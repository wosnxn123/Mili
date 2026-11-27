package me.earthme.luminol.utils;

import com.mojang.logging.LogUtils;
import fun.bm.mili.update.MiliUpdater;
import io.papermc.paper.ServerBuildInfo;
import org.slf4j.Logger;

import java.io.InterruptedIOException;
import java.time.Duration;
import java.time.LocalTime;
import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

public final class AutoUpdateHelper {
    private static final Logger LOGGER = LogUtils.getClassLogger();
    private final AtomicBoolean running = new AtomicBoolean();
    private ScheduledExecutorService scheduler;

    public synchronized void load(List<String> checkTimes, boolean applyOnRestart, boolean replaceLauncher, String targetJarPath) {
        shutdown();
        if ((applyOnRestart && replaceLauncher) || (replaceLauncher && targetJarPath.isBlank())) {
            LOGGER.warn("Invalid Mili auto-update configuration. Use one activation mode and specify the launcher path for direct replacement.");
            return;
        }
        if (checkTimes.isEmpty()) {
            LOGGER.warn("Mili auto-update is enabled, but misc.auto_update.check_times is empty.");
            return;
        }

        final ScheduledExecutorService session = Executors.newSingleThreadScheduledExecutor(task -> {
            final Thread thread = new Thread(task, "Mili Auto Update Scheduler");
            thread.setDaemon(true);
            return thread;
        });
        scheduler = session;
        for (String time : checkTimes) {
            if (!time.matches("(?:[01][0-9]|2[0-3]):[0-5][0-9]")) {
                LOGGER.warn("Invalid Mili auto-update time ignored: {}", time);
                continue;
            }
            Duration delay = Duration.between(LocalTime.now(), LocalTime.parse(time));
            if (delay.isNegative()) delay = delay.plusDays(1);
            session.scheduleAtFixedRate(
                    () -> check(session, applyOnRestart, replaceLauncher ? targetJarPath : ""),
                    delay.toMillis(), TimeUnit.DAYS.toMillis(1), TimeUnit.MILLISECONDS);
        }
    }

    public synchronized void shutdown() {
        if (scheduler != null) {
            scheduler.shutdownNow();
            scheduler = null;
        }
    }

    private void check(ScheduledExecutorService session, boolean applyOnRestart, String targetJarPath) {
        if (!running.compareAndSet(false, true)) return;
        try {
            final ServerBuildInfo build = ServerBuildInfo.buildInfo();
            if (!"luminolmc:mili".equals(build.brandId().asString()) || !"26.3".equals(build.minecraftVersionId())) {
                LOGGER.warn("Mili auto-update only supports Mili builds for Minecraft 26.3.");
                return;
            }
            final String result = MiliUpdater.check(build.gitCommit().orElse(""), applyOnRestart, targetJarPath, action -> {
                synchronized (this) {
                    if (scheduler != session || Thread.currentThread().isInterrupted()) {
                        throw new InterruptedIOException("Mili auto-update configuration changed");
                    }
                    action.run();
                }
            });
            LOGGER.info("{}", result);
        } catch (Exception e) {
            if (!session.isShutdown()) LOGGER.warn("Mili auto-update could not safely complete.", e);
        } finally {
            running.set(false);
        }
    }
}

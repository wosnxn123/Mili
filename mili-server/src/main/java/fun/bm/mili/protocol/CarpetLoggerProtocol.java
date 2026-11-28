package fun.bm.mili.protocol;

import fun.bm.mili.carpet.config.modules.GeneralCompatConfig;
import io.papermc.paper.adventure.PaperAdventure;
import io.papermc.paper.threadedregions.RegionizedWorldData;
import io.papermc.paper.threadedregions.TickRegionScheduler;
import net.kyori.adventure.text.Component;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.protocol.game.ClientboundTabListPacket;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.ServerTickRateManager;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.NaturalSpawner;
import org.jetbrains.annotations.NotNull;
import org.leavesmc.leaves.util.HopperCounter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;

public final class CarpetLoggerProtocol {
    private static final Logger LOGGER = LoggerFactory.getLogger("CarpetLoggerProtocol");
    private static volatile Map<String, String> configuredSubscriptions = Map.of();

    private CarpetLoggerProtocol() {}

    public static void refreshConfiguredDefaults() {
        configuredSubscriptions = parseConfiguredDefaults(GeneralCompatConfig.defaultLoggers);
    }

    public static String serializeConfiguredDefaults(List<String> configuredLoggers) {
        List<String> serialized = new ArrayList<>();
        if (configuredLoggers != null) {
            for (String entry : configuredLoggers) {
                if (entry == null) {
                    continue;
                }
                String trimmed = entry.trim();
                if (!trimmed.isEmpty()) {
                    serialized.add(trimmed);
                }
            }
        }
        return serialized.isEmpty() ? "none" : String.join(",", serialized);
    }

    /** 每秒在本人区域取一次快照，不保存玩家引用，也不接收客户端请求。 */
    public static boolean tickHud(MinecraftServer server, ServerPlayer player, boolean wasVisible) {
        var subscriptions = configuredSubscriptions;
        if (subscriptions.isEmpty() && !wasVisible) return false;
        if (!ca.spottedleaf.moonrise.common.util.TickThread.isTickThreadFor(player) || player.connection == null || player.hasDisconnected()) return false;
        List<net.minecraft.network.chat.Component> lines = new ArrayList<>();
        subscriptions.forEach((loggerName, option) -> {
            switch (loggerName) {
                case "tps" -> lines.add(buildTpsLine(server));
                case "mobcaps" -> {
                    net.minecraft.network.chat.Component line = buildMobcapsLine(player, option);
                    if (line != null) {
                        lines.add(line);
                    }
                }
                case "counter" -> {
                    if (HopperCounter.isEnabled() && player.getBukkitEntity().hasPermission("mili.commands.counter")) {
                        DyeColor color = DyeColor.byName(option, null);
                        if (color != null) lines.add(net.minecraft.network.chat.Component.literal(color.getName() + ": " + HopperCounter.getCounter(color).getTotalItems()));
                    }
                }
                case "xcounter" -> {
                    if (player.getBukkitEntity().hasPermission("mili.commands.counter")) {
                        lines.addAll(fun.bm.mili.carpet.CollectionGameplayRules.xpHud(player.level(), option));
                    }
                }
                default -> {
                }
            }
        });

        if (lines.isEmpty() && !wasVisible) return false;
        var bukkit = player.getBukkitEntity();
        var originalHeader = bukkit.playerListHeader();
        var originalFooter = bukkit.playerListFooter();
        var header = originalHeader == null ? net.minecraft.network.chat.Component.empty() : PaperAdventure.asVanilla(originalHeader);
        MutableComponent footer = net.minecraft.network.chat.Component.empty();
        if (originalFooter != null) footer.append(PaperAdventure.asVanilla(originalFooter));
        boolean separator = originalFooter != null && !Component.empty().equals(originalFooter);
        for (var line : lines) {
            if (separator) footer.append("\n");
            footer.append(line);
            separator = true;
        }
        player.connection.send(new ClientboundTabListPacket(header, footer));
        return !lines.isEmpty();
    }

    private static net.minecraft.network.chat.Component buildTpsLine(MinecraftServer server) {
        ServerTickRateManager tickManager = server.tickRateManager();
        var region = TickRegionScheduler.getCurrentRegion();
        var tickData = region == null ? null : region.getData().getRegionSchedulingHandle().getTickReport5s(System.nanoTime());
        if (tickData == null) return net.minecraft.network.chat.Component.literal("TPS: unavailable");
        final double tps = tickData.tpsData().segmentAll().average();
        final double mspt = tickData.timePerTickData().segmentAll().average() / 1.0E6;

        ChatFormatting color = heatmapColor(mspt, tickManager.millisecondsPerTick());
        return net.minecraft.network.chat.Component.empty()
                .append(net.minecraft.network.chat.Component.literal("TPS: ").withStyle(ChatFormatting.GRAY))
                .append(net.minecraft.network.chat.Component.literal(String.format(Locale.US, "%.1f", tps)).withStyle(color))
                .append(net.minecraft.network.chat.Component.literal("  MSPT: ").withStyle(ChatFormatting.GRAY))
                .append(net.minecraft.network.chat.Component.literal(String.format(Locale.US, "%.1f", mspt)).withStyle(color));
    }

    private static net.minecraft.network.chat.Component buildMobcapsLine(ServerPlayer player, String option) {
        final ServerLevel level = resolveMobcapsLevel(player, option);
        if (level == null) {
            return null;
        }

        final RegionizedWorldData data = level.getCurrentWorldData();
        if (data == null) {
            return null;
        }

        final NaturalSpawner.SpawnState spawnState = data.lastSpawnState;
        if (spawnState == null) {
            return net.minecraft.network.chat.Component.literal("Mobcaps: unavailable").withStyle(ChatFormatting.DARK_GRAY);
        }

        int chunks = spawnState.getSpawnableChunkCount();
        var counts = spawnState.getMobCategoryCounts();
        MutableComponent line = net.minecraft.network.chat.Component.literal("Mobcaps").withStyle(ChatFormatting.GRAY);
        for (MobCategory category : MobCategory.values()) {
            if (category == MobCategory.MISC) {
                continue;
            }
            int current = counts.getOrDefault(category, 0);
            int limit = NaturalSpawner.globalLimitForCategory(level, category, chunks);
            line.append(net.minecraft.network.chat.Component.literal("  " + shortName(category) + " ").withStyle(ChatFormatting.DARK_GRAY));
            line.append(net.minecraft.network.chat.Component.literal(current + "/" + limit).withStyle(categoryColor(current, limit)));
        }
        return line;
    }

    private static ServerLevel resolveMobcapsLevel(ServerPlayer player, String option) {
        if (option == null || option.isBlank() || option.equalsIgnoreCase("dynamic")) {
            return player.level();
        }
        return switch (option.toLowerCase(Locale.ROOT)) {
            case "overworld" -> player.level().dimension() == Level.OVERWORLD ? player.level() : null;
            case "nether" -> player.level().dimension() == Level.NETHER ? player.level() : null;
            case "end" -> player.level().dimension() == Level.END ? player.level() : null;
            default -> player.level();
        };
    }

    private static ChatFormatting heatmapColor(double actual, double reference) {
        if (actual > reference) {
            return ChatFormatting.LIGHT_PURPLE;
        }
        if (actual > 0.8D * reference) {
            return ChatFormatting.RED;
        }
        if (actual > 0.5D * reference) {
            return ChatFormatting.YELLOW;
        }
        if (actual >= 0.0D) {
            return ChatFormatting.DARK_GREEN;
        }
        return ChatFormatting.GRAY;
    }

    private static ChatFormatting categoryColor(int current, int limit) {
        if (limit <= 0) {
            return ChatFormatting.DARK_GRAY;
        }
        double ratio = current / (double) limit;
        if (ratio >= 1.0D) {
            return ChatFormatting.RED;
        }
        if (ratio >= 0.8D) {
            return ChatFormatting.YELLOW;
        }
        return ChatFormatting.GREEN;
    }

    private static String shortName(MobCategory category) {
        return switch (category) {
            case MONSTER -> "M";
            case CREATURE -> "C";
            case AMBIENT -> "A";
            case AXOLOTLS -> "Ax";
            case UNDERGROUND_WATER_CREATURE -> "UWC";
            case WATER_CREATURE -> "WC";
            case WATER_AMBIENT -> "WA";
            case MISC -> "X";
        };
    }

    private static Map<String, String> parseConfiguredDefaults(List<String> configuredLoggers) {
        LinkedHashMap<String, String> subscriptions = new LinkedHashMap<>();
        if (configuredLoggers == null) return Map.of();
        for (String entry : configuredLoggers) {
            if (entry == null) {
                continue;
            }
            for (String chunk : entry.split(",")) {
                String token = chunk.trim();
                if (token.isEmpty() || token.equalsIgnoreCase("none")) {
                    continue;
                }
                String[] parts = token.split("\\s+", 2);
                String loggerName = parts[0].toLowerCase(Locale.ROOT);
                if (loggerName.equals("mob_caps")) loggerName = "mobcaps";
                if (!isSupported(loggerName)) {
                    LOGGER.debug("Ignoring unsupported Carpet default logger '{}'", loggerName);
                    continue;
                }
                String option = parts.length == 1 ? defaultOption(loggerName) : parts[1].trim();
                if (option.isEmpty()) {
                    option = defaultOption(loggerName);
                }
                subscriptions.put(loggerName, option);
            }
        }
        return Collections.unmodifiableMap(subscriptions);
    }

    private static boolean isSupported(String loggerName) {
        return Arrays.asList("tps", "mobcaps", "counter", "xcounter").contains(loggerName);
    }

    private static @NotNull String defaultOption(String loggerName) {
        return switch (loggerName) {
            case "mobcaps" -> "dynamic";
            case "counter", "xcounter" -> "white";
            default -> "";
        };
    }
}

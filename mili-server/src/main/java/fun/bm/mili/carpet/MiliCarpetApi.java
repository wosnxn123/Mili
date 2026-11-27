package fun.bm.mili.carpet;

import fun.bm.mili.api.carpet.CarpetApi;
import fun.bm.mili.api.carpet.CarpetRuleChangeEvent;
import fun.bm.mili.api.carpet.CarpetRuleUpdate;
import fun.bm.mili.carpet.config.modules.PluginRuleAccessConfig;
import io.papermc.paper.threadedregions.RegionizedServer;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import me.earthme.luminol.config.ConfigManager;
import org.bukkit.Bukkit;
import org.bukkit.plugin.Plugin;
import org.leavesmc.leaves.protocol.CarpetServerProtocol;
import org.leavesmc.leaves.protocol.CarpetServerProtocol.CarpetRule;

/** 读取不可变快照；只允许获准的、仍然启用的本地插件通过全局区域写入。 */
public final class MiliCarpetApi implements CarpetApi {
    public static final CarpetApi INSTANCE = new MiliCarpetApi();

    private MiliCarpetApi() {}

    private static Rule snapshot(CarpetRule rule) {
        return new Rule(rule.identifier(), rule.name(), rule.value());
    }

    @Override
    public List<Rule> rules() {
        return CarpetServerProtocol.CarpetRules.snapshot().stream().map(MiliCarpetApi::snapshot)
            .sorted(Comparator.comparing(Rule::name)).toList();
    }

    @Override
    public CompletableFuture<CarpetRuleUpdate> setRule(Plugin owner, String name, String value, boolean persist) {
        Objects.requireNonNull(owner); Objects.requireNonNull(name); Objects.requireNonNull(value);
        // 不能仅相信调用方传入的 Plugin 对象；它必须来自直接调用者的实际插件加载器。
        Class<?> caller = StackWalker.getInstance(StackWalker.Option.RETAIN_CLASS_REFERENCE).getCallerClass();
        try {
            if (org.bukkit.plugin.java.JavaPlugin.getProvidingPlugin(caller) != owner) {
                return CompletableFuture.failedFuture(new SecurityException("不能代表其他插件修改 Carpet 规则"));
            }
        } catch (IllegalArgumentException | IllegalStateException error) {
            return CompletableFuture.failedFuture(new SecurityException("规则写入需要由已加载的插件直接发起", error));
        }
        String pluginName = owner.getName();
        if (Bukkit.getServer() == null || !PluginRuleAccessConfig.allowedRuleWriters.contains(pluginName)) {
            return CompletableFuture.failedFuture(new SecurityException("该插件没有 Carpet 规则写入权限"));
        }
        CompletableFuture<CarpetRuleUpdate> result = new CompletableFuture<>();
        // 总是排入全局队列；事件监听器内发起的后续变更不会递归写入或阻塞区域线程。
        RegionizedServer.getInstance().addTask(() -> {
            if (result.isDone()) return;
            try {
                if (!PluginRuleAccessConfig.allowedRuleWriters.contains(pluginName) || !owner.isEnabled()
                    || Bukkit.getPluginManager().getPlugin(pluginName) != owner) {
                    throw new SecurityException("插件已停用、被替换或不再获准写规则");
                }
                Rule before = rule(name).orElseThrow(() -> new IllegalArgumentException("未公布的规则：" + name));
                var config = ConfigManager.getConfigs("carpet");
                if (config == null || !config.alreadyInit) throw new IllegalStateException("Carpet 配置尚未初始化");
                CarpetRuleUpdate change = CarpetRuleMutations.apply(before, value, persist, config::setConfig);
                if (change.status() == CarpetRuleUpdate.Status.UPDATED) {
                    var now = change.rule();
                    CarpetServerProtocol.CarpetRules.register(CarpetRule.of(now.manager(), now.name(), now.value()), pluginName);
                }
                result.complete(change);
            } catch (RuntimeException error) {
                result.completeExceptionally(error);
            }
        });
        return result.orTimeout(30, TimeUnit.SECONDS);
    }

    public static void published(Map<String, CarpetRule> before, Map<String, CarpetRule> after, String pluginName) {
        if (before.isEmpty() || Bukkit.getServer() == null || before.equals(after)) return;
        var cause = pluginName == null ? CarpetRuleChangeEvent.Cause.CONFIGURATION : CarpetRuleChangeEvent.Cause.API;
        List<CarpetRuleChangeEvent> events = new ArrayList<>();
        for (var entry : after.entrySet()) {
            CarpetRule old = before.get(entry.getKey());
            if (old != null && !old.equals(entry.getValue())) {
                events.add(new CarpetRuleChangeEvent(snapshot(old), snapshot(entry.getValue()), cause, pluginName));
            }
        }
        Runnable notify = () -> {
            if (CarpetRuleChangeEvent.getHandlerList().getRegisteredListeners().length != 0) {
                events.forEach(CarpetRuleChangeEvent::callEvent);
            }
            CarpetServerProtocol.refreshSubscribers();
        };
        if (RegionizedServer.isGlobalTickThread()) notify.run();
        else RegionizedServer.getInstance().addTask(notify);
    }
}

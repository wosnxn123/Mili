package fun.bm.mili.api.carpet;

import java.util.Objects;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/** 原生规则的实时值已经改变。事件在 Folia 全局区域派发，不授予实体所有权。 */
public final class CarpetRuleChangeEvent extends Event {
    private static final HandlerList HANDLERS = new HandlerList();
    public enum Cause { API, CONFIGURATION }
    private final CarpetApi.Rule previous;
    private final CarpetApi.Rule current;
    private final Cause cause;
    private final @Nullable String pluginName;

    public CarpetRuleChangeEvent(CarpetApi.Rule previous, CarpetApi.Rule current, Cause cause, @Nullable String pluginName) {
        this.previous = Objects.requireNonNull(previous);
        this.current = Objects.requireNonNull(current);
        this.cause = Objects.requireNonNull(cause);
        this.pluginName = pluginName;
    }

    public CarpetApi.Rule getPreviousRule() { return previous; }
    public CarpetApi.Rule getRule() { return current; }
    public Cause getCause() { return cause; }
    public @Nullable String getPluginName() { return pluginName; }
    @Override public @NotNull HandlerList getHandlers() { return HANDLERS; }
    public static @NotNull HandlerList getHandlerList() { return HANDLERS; }
}

package fun.bm.mili.api.carpet;

import java.util.Objects;

/** 已接受规则更新的结果。仅重启生效时，rule 仍是当前实际值。 */
public record CarpetRuleUpdate(CarpetApi.Rule rule, String requestedValue, Status status, boolean persisted) {
    public CarpetRuleUpdate {
        Objects.requireNonNull(rule);
        Objects.requireNonNull(requestedValue);
        Objects.requireNonNull(status);
    }

    public enum Status { UPDATED, UNCHANGED, SAVED_FOR_RESTART }
}

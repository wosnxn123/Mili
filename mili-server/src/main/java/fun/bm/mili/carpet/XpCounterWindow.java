/* SPDX-License-Identifier: LGPL-3.0-or-later
 * Carpet TIS Addition, Fallen_Breath and contributors, via Lophine 7a51f49a.
 */
package fun.bm.mili.carpet;

import java.util.Comparator;
import java.util.Locale;
import java.util.Map;
import java.util.TreeMap;

/** One measurement since the first accepted orb. Callers hold the per-world map lock. */
final class XpCounterWindow {
    private static final int MAX_DENOMINATIONS = 64;
    private final long startTick, startNanos;
    private final Map<Integer, Long> byValue = new TreeMap<>(Comparator.reverseOrder());
    private long other, total;

    XpCounterWindow(long tick, long nanos) {
        this.startTick = tick;
        this.startNanos = nanos;
    }

    void record(int value, int count) {
        if (value <= 0 || count <= 0) return;
        long amount = (long)value * count;
        total = CollectionGameplayRules.addWithoutOverflow(total, amount);
        if (byValue.size() < MAX_DENOMINATIONS || byValue.containsKey(value)) {
            byValue.merge(value, amount, CollectionGameplayRules::addWithoutOverflow);
        } else other = CollectionGameplayRules.addWithoutOverflow(other, amount);
    }

    record Snapshot(long total, double ticks, Map<Integer, Long> byValue, long other) {
        double hourly(long xp) { return xp * 72000.0D / ticks; }
        String summary(String name, boolean realtime) {
            return String.format(Locale.ROOT, "%s: %d XP, %.2f/h over %.2f min%s", name, total, hourly(total), ticks / 1200.0D, realtime ? " (real time)" : "");
        }
    }

    Snapshot snapshot(long tick, long nanos, boolean realtime, boolean detailed) {
        double elapsed = realtime ? (nanos - startNanos) / 50_000_000.0D : tick - startTick;
        return new Snapshot(total, Math.max(1.0D, elapsed), detailed ? new TreeMap<>(byValue) : Map.of(), other);
    }
}

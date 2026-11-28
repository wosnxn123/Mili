package fun.bm.mili.carpet.timing;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/** A single watch's bounded, in-order buffer. The watched position's owner is the only writer/reader. */
final class TraceJournal {
    static final int CAPACITY = 128;
    private static final int MAX_DETAIL = 256;
    private final List<Row> rows = new ArrayList<>();
    private int nextId;
    private int pending;
    private long omitted;

    int reserve(final int slots) {
        if (this.rows.size() + this.pending + slots <= CAPACITY) {
            return 1;
        }
        if (this.omitted < Long.MAX_VALUE) {
            ++this.omitted;
        }
        return 0;
    }

    void point(final long tick, final String phase, final String event, final String detail) {
        this.rows.add(new Row(tick, phase, event, bounded(detail), 0, ' '));
    }

    int begin(final long tick, final String phase, final String event, final String detail) {
        final int id = ++this.nextId;
        ++this.pending;
        this.rows.add(new Row(tick, phase, event, bounded(detail), id, '+'));
        return id;
    }

    void end(final long tick, final String phase, final String event, final int id, final String detail) {
        --this.pending;
        this.rows.add(new Row(tick, phase, event, bounded(detail), id, '-'));
    }

    private static String bounded(final String detail) {
        return detail.length() <= MAX_DETAIL ? detail : detail.substring(0, MAX_DETAIL - 3) + "...";
    }

    Snapshot snapshot(final View view) {
        final List<String> lines = new ArrayList<>();
        final Set<Row> seen = view == View.UNIQUE ? new HashSet<>() : Set.of();
        for (int i = 0; i < this.rows.size(); ++i) {
            final Row row = this.rows.get(i);
            if (view == View.UNIQUE && row.id == 0 && !seen.add(row)) {
                continue;
            }
            int repetitions = 1;
            if (view == View.MERGED && row.id == 0) {
                while (i + 1 < this.rows.size() && row.equals(this.rows.get(i + 1))) {
                    ++i;
                    ++repetitions;
                }
            }
            lines.add(row.format() + (repetitions == 1 ? "" : " ×" + repetitions));
        }
        return new Snapshot(List.copyOf(lines), this.omitted);
    }

    enum View { ALL, MERGED, UNIQUE }

    record Snapshot(List<String> lines, long omitted) {}

    private record Row(long tick, String phase, String event, String detail, int id, char boundary) {
        String format() {
            return this.tick + " [" + this.phase + "] " + this.event
                + (this.id == 0 ? "" : " " + this.boundary + "#" + this.id)
                + (this.detail.isEmpty() ? "" : " " + this.detail);
        }
    }
}

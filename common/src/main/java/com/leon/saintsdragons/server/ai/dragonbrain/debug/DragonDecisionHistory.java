package com.leon.saintsdragons.server.ai.dragonbrain.debug;

import java.util.ArrayList;
import java.util.List;

public final class DragonDecisionHistory {
    private static final int CAPACITY = 24;
    private static final int COALESCE_TICKS = 20;
    private final List<Entry> entries = new ArrayList<>();

    public void record(long tick, String subject, String reason, int targetId) {
        for (int i = entries.size() - 1; i >= 0; i--) {
            Entry entry = entries.get(i);
            if (tick - entry.lastTick > COALESCE_TICKS) {
                break;
            }
            if (entry.subject.equals(subject) && entry.reason.equals(reason) && entry.targetId == targetId) {
                entry.lastTick = tick;
                entry.count = Math.min(Integer.MAX_VALUE - 1, entry.count) + 1;
                entries.remove(i);
                entries.add(entry);
                return;
            }
        }
        if (entries.size() == CAPACITY) {
            entries.remove(0);
        }
        entries.add(new Entry(tick, subject, reason, targetId));
    }

    public List<String> describe(long tick) {
        List<String> result = new ArrayList<>();
        for (int i = entries.size() - 1; i >= 0 && result.size() < 8; i--) {
            Entry entry = entries.get(i);
            result.add(Math.max(0L, tick - entry.lastTick) + "t ago " + entry.subject
                    + ":" + entry.reason + " target=" + entry.targetId
                    + (entry.count > 1 ? " x" + entry.count : ""));
        }
        return List.copyOf(result);
    }

    private static final class Entry {
        private long lastTick;
        private final String subject;
        private final String reason;
        private final int targetId;
        private int count = 1;

        private Entry(long tick, String subject, String reason, int targetId) {
            this.lastTick = tick;
            this.subject = subject;
            this.reason = reason;
            this.targetId = targetId;
        }
    }
}

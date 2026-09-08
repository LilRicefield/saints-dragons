package com.leon.saintsdragons.server.ai.navigation.async;

import net.minecraft.server.MinecraftServer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

public final class DragonPathPerformance {
    private static final Logger LOGGER = LoggerFactory.getLogger(DragonPathPerformance.class);
    private static final ConcurrentHashMap<MinecraftServer, Window> WINDOWS = new ConcurrentHashMap<>();

    private DragonPathPerformance() {
    }

    public static void tick(MinecraftServer server, boolean enabled) {
        if (!enabled) {
            stop(server);
            return;
        }
        Window current = WINDOWS.computeIfAbsent(server, ignored -> new Window(server.getTickCount()));
        if (server.getTickCount() - current.startedTick >= 100) {
            synchronized (current) {
                log(current, server.getTickCount());
                current.reset(server.getTickCount());
            }
        }
    }

    public static void stop(MinecraftServer server) {
        Window window = WINDOWS.remove(server);
        if (window != null) {
            log(window, server.getTickCount());
        }
    }

    static Window current(MinecraftServer server) {
        return WINDOWS.get(server);
    }

    private static void log(Window window, int tick) {
        LOGGER.info("[Dragon Path Performance] ticks={} {}", Math.max(1, tick - window.startedTick), window.summary());
    }

    static final class Window {
        private static final int MAX_SECTION_KEYS = 65536;
        private int startedTick;
        private final Set<SectionKey> sections = new HashSet<>();
        private boolean overlapCapped;
        private long captures, copies, repeatedCopies, dynamicShapes, captureNanos, maxCaptureNanos;
        private int captureTick = Integer.MIN_VALUE;
        private long tickCaptureNanos, peakTickCaptureNanos;
        private long started, searches, completed, cancelled, rejected, empty, failed;
        private long queueNanos, maxQueueNanos, searchNanos, maxSearchNanos;
        private long totalNanos, maxTotalNanos, deliveryNanos, maxDeliveryNanos;

        Window(int tick) {
            startedTick = tick;
        }

        synchronized void reset(int tick) {
            startedTick = tick;
            sections.clear();
            overlapCapped = false;
            captures = copies = repeatedCopies = dynamicShapes = captureNanos = maxCaptureNanos = 0;
            captureTick = Integer.MIN_VALUE;
            tickCaptureNanos = peakTickCaptureNanos = 0;
            started = searches = completed = cancelled = rejected = empty = failed = 0;
            queueNanos = maxQueueNanos = searchNanos = maxSearchNanos = 0;
            totalNanos = maxTotalNanos = deliveryNanos = maxDeliveryNanos = 0;
        }

        synchronized void capture(int tick, String dimension, Set<Long> copiedSections,
                                  int shapes, long nanos) {
            captures++;
            copies += copiedSections.size();
            dynamicShapes += shapes;
            captureNanos += nanos;
            maxCaptureNanos = Math.max(maxCaptureNanos, nanos);
            if (captureTick != tick) {
                captureTick = tick;
                tickCaptureNanos = 0;
            }
            tickCaptureNanos += nanos;
            peakTickCaptureNanos = Math.max(peakTickCaptureNanos, tickCaptureNanos);
            for (long section : copiedSections) {
                SectionKey key = new SectionKey(dimension, section);
                if (sections.contains(key)) {
                    repeatedCopies++;
                } else if (sections.size() < MAX_SECTION_KEYS) {
                    sections.add(key);
                } else {
                    overlapCapped = true;
                }
            }
        }

        synchronized void workerStarted(long wait) {
            started++;
            queueNanos += wait;
            maxQueueNanos = Math.max(maxQueueNanos, wait);
        }

        synchronized void searched(long nanos) {
            searches++;
            searchNanos += nanos;
            maxSearchNanos = Math.max(maxSearchNanos, nanos);
        }

        synchronized void delivered(long total, long delivery, boolean noPath) {
            completed++;
            if (noPath) empty++;
            totalNanos += total;
            maxTotalNanos = Math.max(maxTotalNanos, total);
            if (delivery >= 0) {
                deliveryNanos += delivery;
                maxDeliveryNanos = Math.max(maxDeliveryNanos, delivery);
            }
        }

        synchronized void cancelled() { cancelled++; }
        synchronized void rejected() { rejected++; }
        synchronized void failed() { failed++; }

        synchronized String summary() {
            return "captures=" + captures + " sectionCopies=" + copies + " repeatedSectionCopies=" + repeatedCopies
                    + " overlapCapped=" + overlapCapped + " dynamicShapes=" + dynamicShapes
                    + " captureTotalMicros=" + captureNanos / 1000 + " captureMaxMicros=" + maxCaptureNanos / 1000
                    + " capturePeakTickMicros=" + peakTickCaptureNanos / 1000
                    + " workersStarted=" + started + " searches=" + searches + " completed=" + completed
                    + " cancelled=" + cancelled + " rejected=" + rejected + " empty=" + empty + " failed=" + failed
                    + " queueTotalMicros=" + queueNanos / 1000 + " queueMaxMicros=" + maxQueueNanos / 1000
                    + " searchTotalMicros=" + searchNanos / 1000 + " searchMaxMicros=" + maxSearchNanos / 1000
                    + " deliveryTotalMicros=" + deliveryNanos / 1000 + " deliveryMaxMicros=" + maxDeliveryNanos / 1000
                    + " requestTotalMicros=" + totalNanos / 1000 + " requestMaxMicros=" + maxTotalNanos / 1000;
        }
    }

    private record SectionKey(String dimension, long section) {
    }
}

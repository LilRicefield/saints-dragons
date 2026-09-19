package com.leon.saintsdragons.client.particle;

import java.util.function.BiConsumer;

import static com.leon.saintsdragons.client.particle.VolitansBreathParticle.Kind;

final class VolitansBreathEmission {
    private final double[] nextBirth = new double[Kind.values().length];
    private double lastFrame = Double.NaN;
    private boolean poison;

    void reset() {
        lastFrame = Double.NaN;
    }

    void emitUntil(double now, boolean poison, BiConsumer<Kind, Double> emit) {
        if (Double.isNaN(lastFrame) || this.poison != poison || now < lastFrame || now - lastFrame > 2.0D) {
            java.util.Arrays.fill(nextBirth, now);
        }
        this.poison = poison;
        lastFrame = now;
        for (Kind kind : Kind.values()) {
            int count = particlesPerSection(kind, poison);
            if (count == 0) continue;
            int index = kind.ordinal();
            double interval = 2.0D / count;
            while (nextBirth[index] <= now) {
                emit.accept(kind, nextBirth[index]);
                nextBirth[index] += interval;
            }
        }
    }

    private static int particlesPerSection(Kind kind, boolean poison) {
        return switch (kind) {
            case WATER -> poison ? 0 : 32;
            case POISON, POISON_FLAME -> poison ? 8 : 0;
            case POISON_SKULL -> poison ? 4 : 0;
            case BUBBLES -> poison ? 0 : 4;
            case EMITTER, STAR -> poison ? 0 : 16;
        };
    }
}

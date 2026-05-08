package com.leon.saintsdragons.server.entity.component;

import net.minecraft.util.Mth;

public final class DragonBreathComponent {
    private DragonBreathComponent() {
    }

    public interface Gauge {
        float getEnergy();

        void setEnergyRaw(float energy);

        boolean isDepleted();

        void setDepleted(boolean depleted);
    }

    public static void setEnergy(Gauge gauge, float energy, float rearmThreshold) {
        float clamped = Mth.clamp(energy, 0.0F, 1.0F);
        gauge.setEnergyRaw(clamped);
        if (clamped >= rearmThreshold && gauge.isDepleted()) {
            gauge.setDepleted(false);
        }
    }

    public static boolean canUse(Gauge gauge, float depletedThreshold) {
        return gauge.getEnergy() > depletedThreshold && !gauge.isDepleted();
    }

    public static boolean drain(Gauge gauge, float amount, float depletedThreshold, float rearmThreshold) {
        float drain = Math.max(0.0F, amount);
        if (drain > 0.0F) {
            setEnergy(gauge, gauge.getEnergy() - drain, rearmThreshold);
        }
        if (gauge.getEnergy() <= depletedThreshold) {
            gauge.setDepleted(true);
            return false;
        }
        return true;
    }

    public static void regen(Gauge gauge, float amount, float rearmThreshold) {
        if (gauge.getEnergy() >= 1.0F) {
            return;
        }
        float regen = Math.max(0.0F, amount);
        if (regen > 0.0F) {
            setEnergy(gauge, gauge.getEnergy() + regen, rearmThreshold);
        }
    }
}

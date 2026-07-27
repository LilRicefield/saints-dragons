package com.leon.saintsdragons.server.entity.ability.abilities.atroxiia;

import com.leon.saintsdragons.common.config.dragon.DragonAttributeConfigLoader;
import com.leon.saintsdragons.server.entity.dragons.atroxiia.Atroxiia;
import com.leon.saintsdragons.server.entity.dragons.cindervane.Cindervane;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.Vec3;

final class AtroxiiaFrostImpact {
    private static final int SLOWNESS_AMPLIFIER = 6;
    private static final int WEAKNESS_AMPLIFIER = 1;
    private static final int SNOW_BURST_PARTICLES = 28;
    private static final int CINDERVANE_FIRE_BODY_SUPPRESSION_TICKS = 5 * 20;

    private AtroxiiaFrostImpact() {
    }

    static void apply(Atroxiia dragon, LivingEntity target, int stunTicks) {
        boolean enabled = DragonAttributeConfigLoader.getInstance()
                .getConfig(DragonAttributeConfigLoader.ATROXIIA_ID)
                .abilityEnabled("frost_impact", true);
        if (!enabled) {
            return;
        }
        int duration = Math.max(1, stunTicks);
        target.addEffect(new MobEffectInstance(
                MobEffects.MOVEMENT_SLOWDOWN, duration, SLOWNESS_AMPLIFIER, false, true
        ));
        target.addEffect(new MobEffectInstance(
                MobEffects.WEAKNESS, duration, WEAKNESS_AMPLIFIER, false, true
        ));
        if (target.canFreeze()) {
            int frozenTicks = target.getTicksRequiredToFreeze() + duration * 2;
            target.setTicksFrozen(Math.max(target.getTicksFrozen(), frozenTicks));
        }
        spawnSnowBurst(dragon, target);

        if (target instanceof Cindervane cindervane) {
            cindervane.suppressFireBody(CINDERVANE_FIRE_BODY_SUPPRESSION_TICKS);
        }
    }

    private static void spawnSnowBurst(Atroxiia dragon, LivingEntity target) {
        if (!(target.level() instanceof ServerLevel server)) {
            return;
        }

        RandomSource random = dragon.getRandom();
        double surfaceRadius = Math.max(0.25D, target.getBbWidth() * 0.3D);
        for (int i = 0; i < SNOW_BURST_PARTICLES; i++) {
            double angle = random.nextDouble() * Math.PI * 2.0D;
            double vertical = random.nextDouble() * Math.PI * 0.25D;
            Vec3 direction = new Vec3(Math.cos(angle), vertical, Math.sin(angle)).normalize();
            double speed = 0.12D + random.nextDouble() * 0.28D;
            double particleY = target.getY()
                    + target.getBbHeight() * (0.25D + random.nextDouble() * 0.5D);

            server.sendParticles(
                    ParticleTypes.SNOWFLAKE,
                    target.getX() + direction.x * surfaceRadius,
                    particleY,
                    target.getZ() + direction.z * surfaceRadius,
                    0,
                    direction.x,
                    direction.y,
                    direction.z,
                    speed
            );
        }
    }
}

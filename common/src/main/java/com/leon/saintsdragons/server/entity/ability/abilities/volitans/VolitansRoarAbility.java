package com.leon.saintsdragons.server.entity.ability.abilities.volitans;

import com.leon.saintsdragons.common.registry.ModSounds;
import com.leon.saintsdragons.server.entity.effect.volitans.VolitansSpineEntity;
import com.leon.saintsdragons.server.entity.ability.DragonAbility;
import com.leon.saintsdragons.server.entity.ability.DragonAbilitySection;
import com.leon.saintsdragons.server.entity.ability.DragonAbilityType;
import com.leon.saintsdragons.server.entity.dragons.volitans.Volitans;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static com.leon.saintsdragons.server.entity.ability.DragonAbilitySection.AbilitySectionDuration;
import static com.leon.saintsdragons.server.entity.ability.DragonAbilitySection.AbilitySectionType.RECOVERY;
import static com.leon.saintsdragons.server.entity.ability.DragonAbilitySection.AbilitySectionType.STARTUP;

public class VolitansRoarAbility extends DragonAbility<Volitans> {
    private static final int STARTUP_TICKS = 8;
    private static final int ACTIVE_TICKS = 46;
    private static final int RECOVERY_TICKS = 10;
    private static final int ROAR_ANIM_TOTAL_TICKS = STARTUP_TICKS + ACTIVE_TICKS + RECOVERY_TICKS; // 64 ticks (~3.2083s)
    private static final int SOUND_DURATION_TICKS = 100;
    private static final int AIR_WATER_ROAR_TICKS = 33; // 1.6667s
    private static final int AIR_WATER_ROAR_SOUND_TICKS = 60;
    private static final int GROUNDED_ROAR_TAKEOFF_BLOCK_BUFFER_TICKS = 10;
    private static final int AIR_WATER_SPINE_START_DELAY_TICKS = 5;
    private static final int ROAR_EFFECT_START_TICK = 23;
    private static final int ROAR_EFFECT_DURATION_TICKS = 40;
    private static final int ROAR_SPINE_PULSE_INTERVAL_TICKS = 6;
    private static final float GROUNDED_ROAR_DAMAGE = 10.0F;
    private static final float AIR_WATER_ROAR_DAMAGE = 7.0F;
    private static final float ROAR_SHAKE_INTENSITY = 0.85F;
    private static final int GROUNDED_POISON_DURATION_TICKS = 1200;
    private static final int GROUNDED_POISON_LEVEL = 3;
    private static final int AIR_WATER_POISON_DURATION_TICKS = 200;
    private static final int AIR_WATER_POISON_LEVEL = 2;
    private static final int GROUNDED_STUN_TICKS = 40;
    private static final int AIR_WATER_STUN_TICKS = 20;
    private static final double GROUNDED_HIT_RADIUS = 20.0D;
    private static final double AIR_WATER_HIT_RADIUS = 12.0D;

    private static final DragonAbilitySection[] TRACK = new DragonAbilitySection[] {
            new AbilitySectionDuration(STARTUP, STARTUP_TICKS),
            new AbilitySectionDuration(DragonAbilitySection.AbilitySectionType.ACTIVE, ACTIVE_TICKS),
            new AbilitySectionDuration(RECOVERY, RECOVERY_TICKS)
    };

    private final Set<Integer> hitTargetIds = new HashSet<>();
    private boolean shakeTriggered;
    private boolean airOrWaterRoar;
    private boolean groundedRoar;

    public VolitansRoarAbility(DragonAbilityType<Volitans, VolitansRoarAbility> type, Volitans user) {
        super(type, user, TRACK, 30);
    }

    @Override
    protected void beginSection(DragonAbilitySection section) {
        if (section == null) {
            return;
        }
        if (section.sectionType == STARTUP) {
            Volitans dragon = getUser();
            airOrWaterRoar = dragon.isFlying() || dragon.isInWaterOrBubble();
            groundedRoar = !airOrWaterRoar;
            dragon.triggerAnim("instant", airOrWaterRoar ? "roar_air_water" : "roar");
            if (!airOrWaterRoar) {
                dragon.lockRiderControls(ROAR_ANIM_TOTAL_TICKS);
                dragon.blockTakeoffInput(ROAR_ANIM_TOTAL_TICKS + GROUNDED_ROAR_TAKEOFF_BLOCK_BUFFER_TICKS);
                dragon.setGoingUp(false);
                dragon.setGoingDown(false);
            }
            hitTargetIds.clear();
            shakeTriggered = false;

            if (!dragon.level().isClientSide) {
                if (airOrWaterRoar) {
                    dragon.getSoundHandler().playMovingEntitySound(ModSounds.VOLITANS_ROAR_AIR_WATER.get(), 1.6f, 1.0f, AIR_WATER_ROAR_SOUND_TICKS);
                } else {
                    dragon.getSoundHandler().playMovingEntitySound(ModSounds.VOLITANS_ROAR.get(), 1.6f, 1.0f, SOUND_DURATION_TICKS);
                }
            }
        }
    }

    @Override
    public void tickUsing() {
        DragonAbilitySection section = getCurrentSection();
        if (section == null || getUser().level().isClientSide) {
            return;
        }

        int ticksInUse = getTicksInUse();
        if (airOrWaterRoar) {
            if (ticksInUse >= AIR_WATER_ROAR_TICKS) {
                end();
                return;
            }
            if (ticksInUse >= AIR_WATER_SPINE_START_DELAY_TICKS
                    && ticksInUse < AIR_WATER_ROAR_TICKS
                    && ((ticksInUse - AIR_WATER_SPINE_START_DELAY_TICKS) % ROAR_SPINE_PULSE_INTERVAL_TICKS == 0)) {
                int pulseIndex = (ticksInUse - AIR_WATER_SPINE_START_DELAY_TICKS) / ROAR_SPINE_PULSE_INTERVAL_TICKS;
                spawnRoarSpinesAirWater(pulseIndex);
            }
            applyRoarPulse();
            return;
        }

        boolean inEffectWindow = ticksInUse >= ROAR_EFFECT_START_TICK
                && ticksInUse < ROAR_EFFECT_START_TICK + ROAR_EFFECT_DURATION_TICKS;
        if (inEffectWindow) {
            Volitans dragon = getUser();
            if (!shakeTriggered) {
                dragon.triggerScreenShake(ROAR_SHAKE_INTENSITY, ROAR_EFFECT_DURATION_TICKS);
                shakeTriggered = true;
            }
            int effectTick = ticksInUse - ROAR_EFFECT_START_TICK;
            if (effectTick % ROAR_SPINE_PULSE_INTERVAL_TICKS == 0) {
                spawnRoarSpines(effectTick / ROAR_SPINE_PULSE_INTERVAL_TICKS);
            }
            applyRoarPulse();
        }
    }

    @Override
    public void interrupt() {
        Volitans dragon = getUser();
        if (groundedRoar) {
            dragon.blockTakeoffInput(GROUNDED_ROAR_TAKEOFF_BLOCK_BUFFER_TICKS);
            dragon.setGoingUp(false);
            dragon.setGoingDown(false);
        }
        groundedRoar = false;
        super.interrupt();
    }

    @Override
    public void end() {
        Volitans dragon = getUser();
        if (groundedRoar) {
            dragon.blockTakeoffInput(GROUNDED_ROAR_TAKEOFF_BLOCK_BUFFER_TICKS);
            dragon.setGoingUp(false);
            dragon.setGoingDown(false);
        }
        groundedRoar = false;
        super.end();
    }

    private void applyRoarPulse() {
        Volitans dragon = getUser();
        Vec3 origin = dragon.position();
        double hitRadius = airOrWaterRoar ? AIR_WATER_HIT_RADIUS : GROUNDED_HIT_RADIUS;
        float roarDamage = airOrWaterRoar
                ? dragon.getConfiguredAbilityDamage("roar_air_water", AIR_WATER_ROAR_DAMAGE)
                : dragon.getConfiguredAbilityDamage("roar_ground", GROUNDED_ROAR_DAMAGE);
        int poisonDuration = Math.max(0, (int) Math.round(dragon.getConfiguredExtra(
                airOrWaterRoar ? "roar_air_water_poison_duration_ticks" : "roar_ground_poison_duration_ticks",
                airOrWaterRoar ? AIR_WATER_POISON_DURATION_TICKS : GROUNDED_POISON_DURATION_TICKS
        )));
        int poisonAmplifier = dragon.getConfiguredPoisonAmplifier(
                airOrWaterRoar ? "roar_air_water_poison_level" : "roar_ground_poison_level",
                airOrWaterRoar ? AIR_WATER_POISON_LEVEL : GROUNDED_POISON_LEVEL
        );
        int stunTicks = airOrWaterRoar ? AIR_WATER_STUN_TICKS : GROUNDED_STUN_TICKS;
        AABB hitBox = new AABB(
                origin.x - hitRadius,
                origin.y - hitRadius,
                origin.z - hitRadius,
                origin.x + hitRadius,
                origin.y + hitRadius,
                origin.z + hitRadius
        );
        DamageSource source = dragon.level().damageSources().mobAttack(dragon);

        List<LivingEntity> targets = dragon.level().getEntitiesOfClass(LivingEntity.class, hitBox,
                entity -> entity != dragon
                        && entity.isAlive()
                        && entity.attackable()
                        && !dragon.isAlly(entity)
                        && entity.distanceToSqr(dragon) <= (hitRadius * hitRadius));

        for (LivingEntity target : targets) {
            if (!hitTargetIds.add(target.getId())) {
                continue;
            }
            target.hurt(source, roarDamage);
            if (poisonDuration > 0 && poisonAmplifier >= 0) {
                target.addEffect(new MobEffectInstance(MobEffects.POISON, poisonDuration, poisonAmplifier));
            }
            applyStun(target, stunTicks);
        }
    }

    private static void applyStun(LivingEntity target, int durationTicks) {
        if (!(target instanceof Mob mob)) {
            return;
        }
        mob.getNavigation().stop();
        target.setDeltaMovement(Vec3.ZERO);
        target.hurtMarked = true;
        target.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, durationTicks, 6, false, true));
        target.addEffect(new MobEffectInstance(MobEffects.WEAKNESS, durationTicks, 1, false, true));
        target.addEffect(new MobEffectInstance(MobEffects.BLINDNESS, Math.min(durationTicks, 20), 0, false, true));
    }

    private void spawnRoarSpines(int pulseIndex) {
        Volitans dragon = getUser();
        Vec3 center = dragon.position().add(0.0D, dragon.getBbHeight() * 0.55D, 0.0D);
        double spawnRadius = Math.max(1.25D, dragon.getBbWidth() * 0.65D);
        double angleOffset = (pulseIndex % 2 == 0) ? 0.0D : (Math.PI / 12.0D);

        spawnSpineRing(dragon, center, spawnRadius, 10, angleOffset, 0.22D, 1.20F);
        spawnSpineRing(dragon, center, spawnRadius * 0.85D, 8, angleOffset + (Math.PI / 8.0D), 0.05D, 1.45F);

    }

    private void spawnRoarSpinesAirWater(int pulseIndex) {
        Volitans dragon = getUser();
        Vec3 center = dragon.position().add(0.0D, dragon.getBbHeight() * 0.55D, 0.0D);
        double spawnRadius = Math.max(1.1D, dragon.getBbWidth() * 0.58D);
        double angleOffset = (pulseIndex % 2 == 0) ? 0.0D : (Math.PI / 12.0D);

        // Lighter visual density for air/underwater roar.
        spawnSpineRing(dragon, center, spawnRadius, 5, angleOffset, 0.18D, 1.05F);
        spawnSpineRing(dragon, center, spawnRadius * 0.82D, 4, angleOffset + (Math.PI / 8.0D), 0.04D, 1.25F);
    }

    private void spawnSpineRing(Volitans dragon, Vec3 center, double radius, int count, double angleOffset, double verticalBias, float speed) {
        for (int i = 0; i < count; i++) {
            double angle = angleOffset + (Math.PI * 2.0D * i) / (double) count;
            double jitterX = (dragon.getRandom().nextDouble() - 0.5D) * 0.08D;
            double jitterY = (dragon.getRandom().nextDouble() - 0.5D) * 0.06D;
            double jitterZ = (dragon.getRandom().nextDouble() - 0.5D) * 0.08D;
            Vec3 direction = new Vec3(Math.cos(angle) + jitterX, verticalBias + jitterY, Math.sin(angle) + jitterZ).normalize();
            float variedSpeed = speed + (dragon.getRandom().nextFloat() - 0.5F) * 0.16F;
            spawnSpine(dragon, center, direction, radius, variedSpeed);
        }
    }

    private void spawnSpine(Volitans dragon, Vec3 center, Vec3 direction, double radius, float speed) {
        VolitansSpineEntity spine = new VolitansSpineEntity(dragon.level(), dragon);
        Vec3 horizontal = new Vec3(direction.x, 0.0D, direction.z);
        if (horizontal.lengthSqr() < 1.0E-4D) {
            horizontal = new Vec3(1.0D, 0.0D, 0.0D);
        } else {
            horizontal = horizontal.normalize();
        }
        // Keep origins on a flat ring so spikes do not spawn above/below the dragon.
        Vec3 spawnPos = center.add(horizontal.scale(radius));
        spine.setPos(spawnPos.x, spawnPos.y, spawnPos.z);
        spine.shoot(direction.x, direction.y, direction.z, speed, 0.0F);
        spine.pickup = net.minecraft.world.entity.projectile.AbstractArrow.Pickup.DISALLOWED;
        dragon.level().addFreshEntity(spine);
    }
}

package com.leon.saintsdragons.server.entity.ability.abilities.volitans;

import com.leon.saintsdragons.common.registry.ModSounds;
import com.leon.saintsdragons.server.entity.ability.DragonAbility;
import com.leon.saintsdragons.server.entity.ability.DragonAbilitySection;
import com.leon.saintsdragons.server.entity.ability.DragonAbilityType;
import com.leon.saintsdragons.server.entity.dragons.volitans.handlers.VolitansAnimationHandler;
import com.leon.saintsdragons.util.animation.DragonFlightAnimationHelper;
import com.leon.saintsdragons.server.entity.dragons.volitans.Volitans;
import com.leon.saintsdragons.server.entity.dragons.util.DragonElementalImmunity;
import com.leon.saintsdragons.server.entity.ability.DragonAbilitySection.AbilitySectionDuration;

import static com.leon.saintsdragons.server.entity.ability.DragonAbilitySection.AbilitySectionType.*;
import com.leon.saintsdragons.server.entity.effect.ImpactRingEntity;
import com.leon.saintsdragons.server.entity.effect.stegonaut.StegonautGroundCrackEntity;
import com.leon.saintsdragons.server.entity.effect.volitans.VolitansSpineEntity;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import java.util.List;


public class VolitansUltimateAbility extends DragonAbility<Volitans> {
    private static final int SLAMMING_ANIM_TICKS = 25;
    private static final int SLAM_MAX_TICKS = 12000; // Failsafe timeout - usually ends on ground impact
    private static final int RECOVERY_TICKS = 20;
    private static final int COOLDOWN_TICKS = 40;
    private static final int POST_IMPACT_TAKEOFF_BLOCK_TICKS = 8;

    private static final double SLAM_INITIAL_SPEED = -1.5D;
    private static final double SLAM_EXTRA_PULL_PER_TICK = 0.15D;
    private static final double HORIZONTAL_DAMPING = 0.78D;

    private static final float BASE_DAMAGE = 24.0F;
    private static final double IMPACT_RADIUS = 20.0D;
    private static final float IMPACT_SCREEN_SHAKE = 1.2F;
    private static final int IMPACT_SHAKE_TICKS = 12;
    private static final int POISON_DURATION_TICKS = 20 * 30; // 30 seconds
    private static final int POISON_AMPLIFIER = 1;
    private static final int STUN_TICKS = 40;
    private static final int SLAMMING_SOUND_TICKS = 90;
    private static final int IMPACT_SPINE_WAVES = 3;

    private static final DragonAbilitySection[] TRACK = new DragonAbilitySection[]{
            new AbilitySectionDuration(STARTUP, SLAMMING_ANIM_TICKS),
            new AbilitySectionDuration(ACTIVE, SLAM_MAX_TICKS),
            new AbilitySectionDuration(RECOVERY, RECOVERY_TICKS)
    };

    private boolean impactApplied;
    private double forcedSlamSpeed;
    private boolean wasAirborne;

    public VolitansUltimateAbility(DragonAbilityType<Volitans, VolitansUltimateAbility> type, Volitans user) {
        super(type, user, TRACK, COOLDOWN_TICKS);
    }

    @Override
    public boolean tryAbility() {
        Volitans dragon = getUser();
        if (dragon == null || dragon.isBaby() || dragon.areRiderControlsLocked()) {
            return false;
        }
        if (!dragon.isFlying() || dragon.onGround()) {
            return false;
        }
        if (dragon.getControllingPassenger() instanceof net.minecraft.world.entity.player.Player rider) {
            return dragon.isTame() && dragon.isOwnedBy(rider) && super.tryAbility();
        }
        return !dragon.isVehicle() && dragon.isTargetValid(dragon.getTarget()) && super.tryAbility();
    }

    @Override
    protected void beginSection(DragonAbilitySection section) {
        if (section == null) {
            return;
        }
        Volitans dragon = getUser();
        if (section.sectionType == STARTUP) {
            impactApplied = false;
            wasAirborne = true;
            dragon.startUltimateSlamMovement();
            dragon.lockRiderControls(SLAMMING_ANIM_TICKS + SLAM_MAX_TICKS + RECOVERY_TICKS);
            dragon.setGoingUp(false);
            dragon.setGoingDown(false);
            forcedSlamSpeed = 0.0D;
            if (!dragon.level().isClientSide) {
                dragon.triggerAnim(DragonFlightAnimationHelper.CONTROLLER, "slamming");
                dragon.getSoundHandler().playMovingEntitySound(
                        ModSounds.VOLITANS_SLAMMING.get(),
                        1.6f,
                        1.0f,
                        SLAMMING_SOUND_TICKS
                );
            }
            Vec3 current = dragon.getDeltaMovement();
            dragon.setDeltaMovement(current.x * HORIZONTAL_DAMPING, 0.0D, current.z * HORIZONTAL_DAMPING);
            dragon.hasImpulse = true;
        } else if (section.sectionType == ACTIVE) {
            dragon.setGoingUp(false);
            dragon.setGoingDown(true);
            forcedSlamSpeed = SLAM_INITIAL_SPEED;
            Vec3 current = dragon.getDeltaMovement();
            dragon.setDeltaMovement(current.x * HORIZONTAL_DAMPING, forcedSlamSpeed, current.z * HORIZONTAL_DAMPING);
            dragon.hasImpulse = true;
        }
    }

    @Override
    public void tickUsing() {
        Volitans dragon = getUser();
        DragonAbilitySection section = getCurrentSection();
        if (section == null) {
            return;
        }

        if (section.sectionType == STARTUP) {
            dragon.setGoingUp(false);
            dragon.setGoingDown(false);
            Vec3 current = dragon.getDeltaMovement();
            dragon.setDeltaMovement(current.x * HORIZONTAL_DAMPING, 0.0D, current.z * HORIZONTAL_DAMPING);
            dragon.hasImpulse = true;
            return;
        }

        if (section.sectionType != ACTIVE) {
            return;
        }

        dragon.setGoingUp(false);
        dragon.setGoingDown(true);
        if (!dragon.onGround()) {
            wasAirborne = true;
        }

        forcedSlamSpeed -= SLAM_EXTRA_PULL_PER_TICK;
        Vec3 current = dragon.getDeltaMovement();
        dragon.setDeltaMovement(current.x, forcedSlamSpeed, current.z);
        dragon.hasImpulse = true;

        if (dragon.level().isClientSide) {
            return;
        }
        boolean impacted = dragon.onGround() && wasAirborne;

        if (impacted && !impactApplied) {
            impactApplied = true;
            dragon.setOnGround(true);
            applyImpact(dragon);
            spawnImpactVisuals(dragon);
            spawnImpactSpines(dragon);
            dragon.triggerScreenShake(IMPACT_SCREEN_SHAKE, IMPACT_SHAKE_TICKS);
            dragon.markLandedNow();
            dragon.blockTakeoffInput(RECOVERY_TICKS + POST_IMPACT_TAKEOFF_BLOCK_TICKS);
            dragon.setGoingUp(false);
            dragon.setGoingDown(false);
            dragon.setDeltaMovement(0.0D, 0.0D, 0.0D);
            if (!dragon.level().isClientSide) {
                dragon.triggerAnim(DragonFlightAnimationHelper.CONTROLLER, "slammed");
                dragon.playSound(ModSounds.VOLITANS_SLAMMED.get(), 1.9f, 1.0f);
            }

            nextSection();
        }
    }

    @Override
    protected void endSection(DragonAbilitySection section) {
        if (section != null && section.sectionType == RECOVERY) {
            getUser().stopUltimateSlamMovement();
            getUser().clearRiderControlLock();
            getUser().setGoingUp(false);
            getUser().setGoingDown(false);
        }
    }

    @Override
    public void end() {
        getUser().stopUltimateSlamMovement();
        getUser().clearRiderControlLock();
        getUser().setGoingUp(false);
        getUser().setGoingDown(false);
        super.end();
    }

    @Override
    public void interrupt() {
        getUser().stopUltimateSlamMovement();
        getUser().clearRiderControlLock();
        getUser().setGoingUp(false);
        getUser().setGoingDown(false);
        super.interrupt();
    }

    private void applyImpact(Volitans dragon) {
        Vec3 origin = dragon.position();
        AABB hitBox = new AABB(
                origin.x - IMPACT_RADIUS,
                origin.y - IMPACT_RADIUS,
                origin.z - IMPACT_RADIUS,
                origin.x + IMPACT_RADIUS,
                origin.y + IMPACT_RADIUS,
                origin.z + IMPACT_RADIUS
        );

        DamageSource source = dragon.level().damageSources().mobAttack(dragon);
        float damage = BASE_DAMAGE;
        boolean poisonActive = !dragon.isVenomNeutralized();

        List<LivingEntity> targets = dragon.level().getEntitiesOfClass(LivingEntity.class, hitBox,
                entity -> entity != dragon
                        && entity.isAlive()
                        && entity.attackable()
                        && !dragon.isAlly(entity)
                        && (!poisonActive || !DragonElementalImmunity.isPoisonImmune(entity))
                        && entity.distanceToSqr(dragon) <= (IMPACT_RADIUS * IMPACT_RADIUS));

        for (LivingEntity target : targets) {
            target.hurt(source, damage);
            if (poisonActive) {
                target.addEffect(new MobEffectInstance(MobEffects.POISON, POISON_DURATION_TICKS, POISON_AMPLIFIER));
            }
            applyStun(target);
            Vec3 push = target.position().subtract(origin);
            if (push.lengthSqr() < 1.0E-4) {
                push = new Vec3(0.0D, 0.0D, 1.0D);
            }
            push = push.normalize().scale(1.0D);
            target.push(push.x, 0.45D, push.z);
            target.hasImpulse = true;
        }
    }

    private static void applyStun(LivingEntity target) {
        if (!(target instanceof Mob mob)) {
            return;
        }
        mob.getNavigation().stop();
        target.setDeltaMovement(Vec3.ZERO);
        target.hurtMarked = true;
        target.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, STUN_TICKS, 6, false, true));
        target.addEffect(new MobEffectInstance(MobEffects.WEAKNESS, STUN_TICKS, 1, false, true));
        target.addEffect(new MobEffectInstance(MobEffects.BLINDNESS, 20, 0, false, true));
    }

    private void spawnImpactVisuals(Volitans dragon) {
        if (!(dragon.level() instanceof ServerLevel server)) {
            return;
        }
        Vec3 ringPos = dragon.position().add(0.0D, 0.08D, 0.0D);
        double crackY = dragon.getBoundingBox().minY + 0.02D;
        server.addFreshEntity(new ImpactRingEntity(server, ringPos));
        server.addFreshEntity(new StegonautGroundCrackEntity(server, new Vec3(dragon.getX(), crackY, dragon.getZ()), dragon.getYRot()));
    }

    private void spawnImpactSpines(Volitans dragon) {
        Vec3 center = dragon.position().add(0.0D, dragon.getBbHeight() * 0.55D, 0.0D);
        double spawnRadius = Math.max(1.25D, dragon.getBbWidth() * 0.65D);
        for (int wave = 0; wave < IMPACT_SPINE_WAVES; wave++) {
            double waveRadius = spawnRadius * (1.0D + (wave * 0.35D));
            double angleOffset = (Math.PI / 10.0D) * wave;
            float ringSpeed = 1.20F + (wave * 0.25F);
            double verticalBias = 0.14D + (wave * 0.07D);
            int ringCount = 10 + (wave * 2);

            spawnSpineRing(dragon, center, waveRadius, ringCount, angleOffset, verticalBias, ringSpeed);
        }
    }

    private void spawnSpineRing(Volitans dragon, Vec3 center, double radius, int count, double angleOffset, double verticalBias, float speed) {
        for (int i = 0; i < count; i++) {
            double angle = angleOffset + (Math.PI * 2.0D * i) / (double) count;
            double jitterX = (dragon.getRandom().nextDouble() - 0.5D) * 0.10D;
            double jitterY = (dragon.getRandom().nextDouble() - 0.5D) * 0.06D;
            double jitterZ = (dragon.getRandom().nextDouble() - 0.5D) * 0.10D;
            Vec3 direction = new Vec3(Math.cos(angle) + jitterX, verticalBias + jitterY, Math.sin(angle) + jitterZ).normalize();
            float variedSpeed = speed + (dragon.getRandom().nextFloat() - 0.5F) * 0.18F;
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
        Vec3 spawnPos = center.add(horizontal.scale(radius));
        spine.setPos(spawnPos.x, spawnPos.y, spawnPos.z);
        spine.shoot(direction.x, direction.y, direction.z, speed, 0.0F);
        spine.pickup = net.minecraft.world.entity.projectile.AbstractArrow.Pickup.DISALLOWED;
        dragon.level().addFreshEntity(spine);
    }
}

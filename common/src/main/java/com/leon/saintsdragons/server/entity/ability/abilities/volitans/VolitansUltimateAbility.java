package com.leon.saintsdragons.server.entity.ability.abilities.volitans;

import com.leon.saintsdragons.common.registry.ModSounds;
import com.leon.saintsdragons.server.entity.ability.DragonAbility;
import com.leon.saintsdragons.server.entity.ability.DragonAbilitySection;
import com.leon.saintsdragons.server.entity.ability.DragonAbilityType;
import com.leon.saintsdragons.server.entity.dragons.volitans.Volitans;
import com.leon.saintsdragons.server.entity.effect.volitans.VolitansSpineEntity;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import java.util.List;

import static com.leon.saintsdragons.server.entity.ability.DragonAbilitySection.AbilitySectionDuration;
import static com.leon.saintsdragons.server.entity.ability.DragonAbilitySection.AbilitySectionType.ACTIVE;
import static com.leon.saintsdragons.server.entity.ability.DragonAbilitySection.AbilitySectionType.RECOVERY;
import static com.leon.saintsdragons.server.entity.ability.DragonAbilitySection.AbilitySectionType.STARTUP;

public class VolitansUltimateAbility extends DragonAbility<Volitans> {
    private static final int SLAMMING_ANIM_TICKS = 25;
    private static final int SLAM_MAX_TICKS = 12000; // Failsafe timeout - usually ends on ground impact
    private static final int RECOVERY_TICKS = 20;
    private static final int COOLDOWN_TICKS = 40;

    private static final double SLAM_INITIAL_SPEED = -1.5D;
    private static final double SLAM_EXTRA_PULL_PER_TICK = 0.15D;
    private static final double HORIZONTAL_DAMPING = 0.78D;

    private static final float BASE_DAMAGE = 24.0F;
    private static final double IMPACT_RADIUS = 20.0D;
    private static final float IMPACT_SCREEN_SHAKE = 1.2F;
    private static final int IMPACT_SHAKE_TICKS = 12;
    private static final int POISON_DURATION_TICKS = 20 * 30; // 30 seconds
    private static final int POISON_AMPLIFIER = 1;
    private static final int SLAMMING_SOUND_TICKS = 90;

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
        if (!(dragon.getControllingPassenger() instanceof net.minecraft.world.entity.player.Player rider)) {
            return false;
        }
        // Only works when flying/airborne
        if (!dragon.isFlying() || dragon.onGround()) {
            return false;
        }
        return dragon.isTame() && dragon.isOwnedBy(rider) && super.tryAbility();
    }

    @Override
    protected void beginSection(DragonAbilitySection section) {
        if (section == null) {
            return;
        }
        Volitans dragon = getUser();
        if (section.sectionType == STARTUP) {
            impactApplied = false;
            wasAirborne = true; // Already airborne (enforced by tryAbility)
            dragon.startUltimateSlamMovement();
            dragon.setGoingUp(false);
            dragon.setGoingDown(false);
            forcedSlamSpeed = 0.0D;

            // Trigger slamming animation (1.67 seconds)
            if (!dragon.level().isClientSide) {
                dragon.triggerAnim("instant", "slamming");
                dragon.getSoundHandler().playMovingEntitySound(
                        ModSounds.VOLITANS_SLAMMING.get(),
                        1.6f,
                        1.0f,
                        SLAMMING_SOUND_TICKS
                );
            }

            // Hold in-air pose during slamming animation.
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

        // Track if we've been airborne (to avoid false ground detection)
        if (!dragon.onGround()) {
            wasAirborne = true;
        }

        // Accelerate downward using velocity
        forcedSlamSpeed -= SLAM_EXTRA_PULL_PER_TICK;
        // Preserve horizontal movement, only control vertical
        Vec3 current = dragon.getDeltaMovement();
        dragon.setDeltaMovement(current.x, forcedSlamSpeed, current.z);
        dragon.hasImpulse = true;

        if (dragon.level().isClientSide) {
            return;
        }

        // Only trigger impact when we actually hit the ground after being airborne
        boolean impacted = dragon.onGround() && wasAirborne;

        if (impacted && !impactApplied) {
            impactApplied = true;
            dragon.setOnGround(true);
            applyImpact(dragon);
            spawnImpactSpines(dragon);
            dragon.triggerScreenShake(IMPACT_SCREEN_SHAKE, IMPACT_SHAKE_TICKS);
            dragon.markLandedNow();
            dragon.clearRiderControlLock();
            dragon.setGoingUp(false);
            dragon.setGoingDown(false);
            dragon.setDeltaMovement(0.0D, 0.0D, 0.0D);

            // Trigger slammed animation (1 second)
            if (!dragon.level().isClientSide) {
                dragon.triggerAnim("instant", "slammed");
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

        List<LivingEntity> targets = dragon.level().getEntitiesOfClass(LivingEntity.class, hitBox,
                entity -> entity != dragon
                        && entity.isAlive()
                        && entity.attackable()
                        && !dragon.isAlly(entity)
                        && entity.distanceToSqr(dragon) <= (IMPACT_RADIUS * IMPACT_RADIUS));

        for (LivingEntity target : targets) {
            target.hurt(source, damage);
            target.addEffect(new MobEffectInstance(MobEffects.POISON, POISON_DURATION_TICKS, POISON_AMPLIFIER));
            Vec3 push = target.position().subtract(origin);
            if (push.lengthSqr() < 1.0E-4) {
                push = new Vec3(0.0D, 0.0D, 1.0D);
            }
            push = push.normalize().scale(1.0D);
            target.push(push.x, 0.45D, push.z);
            target.hasImpulse = true;
        }
    }

    private void spawnImpactSpines(Volitans dragon) {
        Vec3 center = dragon.position().add(0.0D, dragon.getBbHeight() * 0.55D, 0.0D);
        double spawnRadius = Math.max(1.25D, dragon.getBbWidth() * 0.65D);

        for (int i = 0; i < 10; i++) {
            Vec3 direction;
            if (i < 8) {
                double angle = (Math.PI * 2.0D * i) / 8.0D;
                direction = new Vec3(Math.cos(angle), 0.18D, Math.sin(angle)).normalize();
            } else if (i == 8) {
                direction = new Vec3(0.0D, 1.0D, 0.0D);
            } else {
                direction = new Vec3(0.0D, -0.65D, 0.0D).normalize();
            }

            VolitansSpineEntity spine = new VolitansSpineEntity(dragon.level(), dragon);
            Vec3 spawnPos = center.add(direction.scale(spawnRadius));
            spine.setPos(spawnPos.x, spawnPos.y, spawnPos.z);
            spine.shoot(direction.x, direction.y, direction.z, 1.35F, 0.0F);
            spine.pickup = net.minecraft.world.entity.projectile.AbstractArrow.Pickup.DISALLOWED;
            dragon.level().addFreshEntity(spine);
        }
    }
}

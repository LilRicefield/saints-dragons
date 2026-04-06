package com.leon.saintsdragons.server.entity.ability.abilities.volitans;

import com.leon.saintsdragons.common.registry.ModSounds;
import com.leon.saintsdragons.server.entity.ability.DragonAbility;
import com.leon.saintsdragons.server.entity.ability.DragonAbilitySection;
import com.leon.saintsdragons.server.entity.ability.DragonAbilityType;
import com.leon.saintsdragons.server.entity.dragons.volitans.Volitans;
import com.leon.saintsdragons.server.entity.effect.volitans.VolitansPoisonBallEntity;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;

import static com.leon.saintsdragons.server.entity.ability.DragonAbilitySection.AbilitySectionInfinite;
import static com.leon.saintsdragons.server.entity.ability.DragonAbilitySection.AbilitySectionType.ACTIVE;

/**
 * Hold-to-charge poison ball for Volitans:
 * ready (0.625s) -> hold (loop) -> release to shoot (0.4167s).
 */
public class VolitansPoisonBallAbility extends DragonAbility<Volitans> {
    private static final DragonAbilitySection[] TRACK = new DragonAbilitySection[] {
            new AbilitySectionInfinite(ACTIVE)
    };

    private static final int COOLDOWN_TICKS = 20;
    private static final int READY_TICKS = 13; // 0.625s
    private static final int SHOOT_RELEASE_TICKS = 8; // 0.4167s
    private static final int RELEASE_TAKEOFF_BLOCK_TICKS = 16;
    private static final int READY_SOUND_TICKS = 25; // 1.25s
    private static final int SHOOT_SOUND_TICKS = 52; // 2.60s

    private static final int PROJECTILE_LIFETIME_TICKS = 200;
    private static final double PROJECTILE_SPEED = 3.5D;
    private static final float PROJECTILE_SCALE = 2.2F;
    private static final double IMPACT_RADIUS = 5.0D;
    private static final float IMPACT_DAMAGE = 12.0F;
    private static final int POISON_DURATION_TICKS = 120;
    private static final int POISON_AMPLIFIER = 0;
    private static final double TARGET_LEAD_FACTOR = 0.6D;

    private int chargeTicks = 0;
    private boolean holdLoopActive = false;
    private boolean releaseRequested = false;
    private boolean shootAnimTriggered = false;
    private int releaseTicks = 0;
    private boolean resolved = false;

    public VolitansPoisonBallAbility(DragonAbilityType<Volitans, VolitansPoisonBallAbility> type, Volitans user) {
        super(type, user, TRACK, COOLDOWN_TICKS);
    }

    @Override
    protected void beginSection(DragonAbilitySection section) {
        if (section == null || section.sectionType != ACTIVE) {
            return;
        }
        chargeTicks = 0;
        holdLoopActive = false;
        releaseRequested = false;
        shootAnimTriggered = false;
        releaseTicks = 0;
        resolved = false;
        getUser().triggerAnim("actions", "poison_ball_ready");
        if (!getUser().level().isClientSide) {
            getUser().getSoundHandler().playMovingEntitySound(
                    ModSounds.VOLITANS_POISON_BALL_READY.get(),
                    2.0f,
                    1.0f,
                    READY_SOUND_TICKS
            );
        }
    }

    @Override
    protected boolean canContinueUsing() {
        Volitans dragon = getUser();
        return dragon.isAlive() && !dragon.isRemoved();
    }

    @Override
    public void tickUsing() {
        if (resolved) {
            return;
        }

        if (!releaseRequested) {
            chargeTicks++;
            if (!holdLoopActive && chargeTicks >= READY_TICKS) {
                holdLoopActive = true;
                getUser().triggerAnim("actions", "poison_ball_hold");
            }
            return;
        }

        if (chargeTicks < READY_TICKS) {
            chargeTicks++;
            return;
        }

        if (!shootAnimTriggered) {
            shootAnimTriggered = true;
            releaseTicks = 0;
            getUser().triggerAnim("actions", "poison_ball_shoot");
            if (!getUser().level().isClientSide) {
                getUser().getSoundHandler().playMovingEntitySound(
                        ModSounds.VOLITANS_POISON_BALL_SHOOT.get(),
                        2.0f,
                        1.0f,
                        SHOOT_SOUND_TICKS
                );
            }
        }

        releaseTicks++;
        if (releaseTicks >= SHOOT_RELEASE_TICKS) {
            firePoisonBall();
            resolved = true;
            end();
        }
    }

    @Override
    public void interrupt() {
        Volitans dragon = getUser();
        dragon.blockTakeoffInput(RELEASE_TAKEOFF_BLOCK_TICKS);
        dragon.setGoingUp(false);
        dragon.setGoingDown(false);
        resetState();
        super.interrupt();
    }

    @Override
    public void end() {
        Volitans dragon = getUser();
        dragon.blockTakeoffInput(RELEASE_TAKEOFF_BLOCK_TICKS);
        dragon.setGoingUp(false);
        dragon.setGoingDown(false);
        resetState();
        super.end();
    }

    public void requestRelease() {
        if (resolved || releaseRequested) {
            return;
        }
        releaseRequested = true;
        releaseTicks = 0;
        Volitans dragon = getUser();
        dragon.blockTakeoffInput(SHOOT_RELEASE_TICKS + RELEASE_TAKEOFF_BLOCK_TICKS);
        dragon.setGoingUp(false);
        dragon.setGoingDown(false);
    }

    private void firePoisonBall() {
        Volitans dragon = getUser();
        if (!(dragon.level() instanceof ServerLevel server)) {
            return;
        }

        Vec3 direction = getAimDirection(dragon);
        Vec3 spawnPos = dragon.getMouthPosition();

        int poisonDurationTicks = (int) Math.round(dragon.getConfiguredExtra("poison_ball_poison_duration_ticks", POISON_DURATION_TICKS));
        int poisonAmplifier = dragon.getConfiguredPoisonAmplifier("poison_ball_poison_level", POISON_AMPLIFIER + 1);
        VolitansPoisonBallEntity projectile = new VolitansPoisonBallEntity(
                server, spawnPos, dragon,
                IMPACT_RADIUS, dragon.getConfiguredAbilityDamage("poison_ball", IMPACT_DAMAGE),
                Math.max(0, poisonDurationTicks), Math.max(-1, poisonAmplifier), PROJECTILE_LIFETIME_TICKS
        );
        projectile.setVisualScale(PROJECTILE_SCALE);
        projectile.setDeltaMovement(direction.scale(PROJECTILE_SPEED));
        projectile.hasImpulse = true;
        server.addFreshEntity(projectile);
    }

    private Vec3 getAimDirection(Volitans dragon) {
        Entity rider = dragon.getControllingPassenger();
        if (rider instanceof Player player) {
            Vec3 view = player.getViewVector(1.0F);
            if (view.lengthSqr() > 1.0E-6) {
                return view.normalize();
            }
        }

        if (dragon.getTarget() != null) {
            var target = dragon.getTarget();
            Vec3 targetPos = target.getEyePosition();
            Vec3 lead = target.getDeltaMovement().scale(TARGET_LEAD_FACTOR);
            Vec3 aimPoint = targetPos.add(lead);
            Vec3 dir = aimPoint.subtract(dragon.getMouthPosition());
            if (dir.lengthSqr() > 1.0E-6) {
                return dir.normalize();
            }
        }

        Vec3 look = dragon.getLookAngle();
        return look.lengthSqr() > 1.0E-6 ? look.normalize() : new Vec3(0.0D, 0.0D, 1.0D);
    }

    private void resetState() {
        chargeTicks = 0;
        holdLoopActive = false;
        releaseRequested = false;
        shootAnimTriggered = false;
        releaseTicks = 0;
        resolved = false;
    }
}

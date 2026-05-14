package com.leon.saintsdragons.server.entity.ability.abilities.volitans;

import com.leon.saintsdragons.common.registry.ModSounds;
import com.leon.saintsdragons.server.entity.ability.DragonAbility;
import com.leon.saintsdragons.server.entity.ability.DragonAbilitySection;
import com.leon.saintsdragons.server.entity.ability.DragonAbilityType;
import com.leon.saintsdragons.server.entity.dragons.volitans.Volitans;
import com.leon.saintsdragons.server.entity.dragons.volitans.handlers.VolitansAnimationHandler;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import java.util.List;

import static com.leon.saintsdragons.server.entity.ability.DragonAbilitySection.AbilitySectionDuration;
import static com.leon.saintsdragons.server.entity.ability.DragonAbilitySection.AbilitySectionType.ACTIVE;
import static com.leon.saintsdragons.server.entity.ability.DragonAbilitySection.AbilitySectionType.RECOVERY;
import static com.leon.saintsdragons.server.entity.ability.DragonAbilitySection.AbilitySectionType.STARTUP;

/**
 * Burrow-phase ability: visual underground phase with collision bypass + burst exit.
 */
public class VolitansBurrowAbility extends DragonAbility<Volitans> {
    private static final int STARTUP_TICKS = 14;
    private static final int ACTIVE_TICKS_MAX = 20 * 15; // 15 seconds
    private static final int EXIT_TICKS = 52; // 2.5833s
    private static final int EXIT_BURST_DELAY_TICKS = 28; // ~1.4s into exit anim
    private static final int ENTER_BURROW_SOUND_TICKS = 60; // 3.0s
    private static final int EXIT_BURROW_SOUND_TICKS = 100; // 5.0s
    private static final int COOLDOWN_TICKS = 50;
    private static final float EXIT_DAMAGE = 30.0F; // 15 hearts
    private static final double EXIT_RADIUS = 12.0D;
    private static final double EXIT_UPWARD_KNOCK = 1.0D;

    private static final DragonAbilitySection[] TRACK = new DragonAbilitySection[] {
            new AbilitySectionDuration(STARTUP, STARTUP_TICKS),
            new AbilitySectionDuration(ACTIVE, ACTIVE_TICKS_MAX),
            new AbilitySectionDuration(RECOVERY, EXIT_TICKS)
    };
    private boolean exitRequested;
    private boolean applyBurstOnExit;
    private boolean burstApplied;

    public VolitansBurrowAbility(DragonAbilityType<Volitans, VolitansBurrowAbility> type, Volitans user) {
        super(type, user, TRACK, COOLDOWN_TICKS);
    }

    @Override
    public boolean tryAbility() {
        Volitans dragon = getUser();
        if (dragon == null || !dragon.isAlive() || dragon.isDying() || dragon.isBaby()) {
            return false;
        }
        if (dragon.isFlying() || dragon.isInWaterOrBubble() || dragon.isUnderWater()) {
            return false;
        }
        if (dragon.getControllingPassenger() instanceof Player rider) {
            return dragon.isTame() && dragon.isOwnedBy(rider) && super.tryAbility();
        }
        if (dragon.isVehicle()) {
            return false;
        }
        return dragon.isTargetValid(dragon.getTarget()) && super.tryAbility();
    }

    @Override
    protected void beginSection(DragonAbilitySection section) {
        if (section == null) {
            return;
        }
        Volitans dragon = getUser();
        if (section.sectionType == STARTUP) {
            dragon.triggerAnim(VolitansAnimationHandler.FAST_ACTION_CONTROLLER, "enter_burrow");
            playEnterBurrowSound(dragon);
            dragon.setBurrowing(false);
            exitRequested = false;
            applyBurstOnExit = true;
            burstApplied = false;
            return;
        }
        if (section.sectionType == ACTIVE) {
            dragon.setBurrowing(true);
            return;
        }
        if (section.sectionType == RECOVERY) {
            dragon.setBurrowing(false);
            dragon.triggerAnim(VolitansAnimationHandler.ACTION_CONTROLLER, "burrow_exit");
            playExitBurrowSound(dragon);
            dragon.grantTemporaryInvulnerability(EXIT_TICKS);
            dragon.lockRiderControls(EXIT_TICKS);
            dragon.blockTakeoffAfterBurrowExit(EXIT_TICKS);
            dragon.setGoingUp(false);
            dragon.setGoingDown(false);
        }
    }

    @Override
    protected void endSection(DragonAbilitySection section) {
        if (section != null && (section.sectionType == ACTIVE || section.sectionType == RECOVERY)) {
            Volitans dragon = getUser();
            dragon.setBurrowing(false);
            dragon.setGoingUp(false);
            dragon.setGoingDown(false);
        }
    }

    @Override
    public void tickUsing() {
        DragonAbilitySection section = getCurrentSection();
        if (section == null) {
            return;
        }
        Volitans dragon = getUser();
        if (section.sectionType == RECOVERY) {
            if (applyBurstOnExit && !burstApplied && getTicksInSection() >= EXIT_BURST_DELAY_TICKS) {
                applyExitBurst(dragon);
                burstApplied = true;
            }
            return;
        }
        if (section.sectionType != ACTIVE) {
            return;
        }

        // Water cancels burrow phase and exits cleanly without burst damage.
        if (dragon.isInWaterOrBubble() || dragon.isUnderWater()) {
            requestExit(false);
        }
        if (exitRequested) {
            nextSection();
        }
    }

    @Override
    public void interrupt() {
        Volitans dragon = getUser();
        dragon.setBurrowing(false);
        dragon.setGoingUp(false);
        dragon.setGoingDown(false);
        dragon.clearRiderControlLock();
        dragon.blockTakeoffAfterBurrowExit(8);
        super.interrupt();
    }

    public void requestExit(boolean withBurst) {
        DragonAbilitySection section = getCurrentSection();
        if (section == null || section.sectionType != ACTIVE) {
            return;
        }
        exitRequested = true;
        applyBurstOnExit = withBurst;
    }

    private void applyExitBurst(Volitans dragon) {
        if (dragon.level().isClientSide) {
            return;
        }
        Vec3 origin = dragon.position();
        AABB hitBox = new AABB(
                origin.x - EXIT_RADIUS,
                origin.y - EXIT_RADIUS,
                origin.z - EXIT_RADIUS,
                origin.x + EXIT_RADIUS,
                origin.y + EXIT_RADIUS,
                origin.z + EXIT_RADIUS
        );
        DamageSource source = dragon.level().damageSources().mobAttack(dragon);
        float damage = dragon.getConfiguredAbilityDamage("burrow", EXIT_DAMAGE);
        List<LivingEntity> targets = dragon.level().getEntitiesOfClass(
                LivingEntity.class,
                hitBox,
                entity -> entity != dragon
                        && entity.isAlive()
                        && entity.attackable()
                        && !dragon.isAlly(entity)
                        && entity.distanceToSqr(dragon) <= EXIT_RADIUS * EXIT_RADIUS
        );
        for (LivingEntity target : targets) {
            target.hurt(source, damage);
            Vec3 motion = target.getDeltaMovement();
            target.setDeltaMovement(motion.x * 0.35D, Math.max(motion.y, EXIT_UPWARD_KNOCK), motion.z * 0.35D);
            target.hurtMarked = true;
        }
    }

    private void playEnterBurrowSound(Volitans dragon) {
        if (dragon.level().isClientSide) {
            return;
        }
        float pitch = 0.96f + dragon.getRandom().nextFloat() * 0.08f;
        dragon.getSoundHandler().playMovingEntitySound(
                ModSounds.VOLITANS_ENTER_BURROW.get(),
                2.0f,
                pitch,
                ENTER_BURROW_SOUND_TICKS
        );
    }

    private void playExitBurrowSound(Volitans dragon) {
        if (dragon.level().isClientSide) {
            return;
        }
        float pitch = 0.96f + dragon.getRandom().nextFloat() * 0.08f;
        dragon.getSoundHandler().playMovingEntitySound(
                ModSounds.VOLITANS_BURROW_EXIT.get(),
                2.0f,
                pitch,
                EXIT_BURROW_SOUND_TICKS
        );
    }
}

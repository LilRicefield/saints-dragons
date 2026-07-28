package com.leon.saintsdragons.server.entity.ability.abilities.atroxiia;

import com.leon.saintsdragons.common.config.dragon.DragonAttributeConfigLoader;
import com.leon.saintsdragons.common.registry.ModSounds;
import com.leon.saintsdragons.server.entity.ability.DragonAbility;
import com.leon.saintsdragons.server.entity.ability.DragonAbilitySection;
import com.leon.saintsdragons.server.entity.ability.DragonAbilityType;
import com.leon.saintsdragons.server.entity.ability.DragonMeleeGeometry;
import com.leon.saintsdragons.server.entity.dragons.atroxiia.Atroxiia;
import com.leon.saintsdragons.util.animation.AnimationHelper;
import net.minecraft.util.Mth;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.Vec3;

import java.util.List;

import static com.leon.saintsdragons.server.entity.ability.DragonAbilitySection.AbilitySectionDuration;
import static com.leon.saintsdragons.server.entity.ability.DragonAbilitySection.AbilitySectionType.ACTIVE;
import static com.leon.saintsdragons.server.entity.ability.DragonAbilitySection.AbilitySectionType.RECOVERY;
import static com.leon.saintsdragons.server.entity.ability.DragonAbilitySection.AbilitySectionType.STARTUP;

public class AtroxiiaPreciseStrikeAbility extends DragonAbility<Atroxiia> {
    private static final float BASE_DAMAGE = 10.0F;
    private static final int ANIMATION_TICKS = 100;
    private static final int RECOVERY_TICKS = 8;
    private static final int FIRST_NUDGE_AND_DAMAGE_TICK = 11;
    private static final int PULL_TARGETS_TICK = 25;
    private static final int SECOND_NUDGE_TICK = 39;
    private static final int SECOND_DAMAGE_TICK = 45;
    private static final int THIRD_NUDGE_TICK = 53;
    private static final int THIRD_DAMAGE_TICK = 64;
    private static final int NUDGE_TICKS = 5;
    private static final double NUDGE_DISTANCE = 5.0D;
    private static final double RANGE = 7.5D;
    private static final double SWEEP_HORIZONTAL = 7.5D;
    private static final double SWEEP_VERTICAL = 7.5D;
    private static final double ANGLE_DEG = 180.0D;
    private static final double PULL_STRENGTH = 2.0D;
    private static final double DAMAGE_KNOCKBACK = 0.75D;
    private static final double DAMAGE_KNOCKBACK_Y = 0.16D;
    private static final int POST_COMBO_STUN_TICKS = 40;
    private static final float THIRD_STRIKE_SCREEN_SHAKE = 0.75F;
    private static final int THIRD_STRIKE_SCREEN_SHAKE_TICKS = 8;
    private static final float AI_STEERING_DEGREES_PER_TICK = 10.0F;

    private static final DragonAbilitySection[] TRACK = new DragonAbilitySection[] {
            new AbilitySectionDuration(STARTUP, 1),
            new AbilitySectionDuration(ACTIVE, ANIMATION_TICKS),
            new AbilitySectionDuration(RECOVERY, RECOVERY_TICKS)
    };

    public AtroxiiaPreciseStrikeAbility(DragonAbilityType<Atroxiia, AtroxiiaPreciseStrikeAbility> type, Atroxiia user) {
        super(type, user, TRACK, 30);
    }

    @Override
    public boolean tryAbility() {
        return getUser().canUseGroundCombatAbility();
    }

    @Override
    protected void beginSection(DragonAbilitySection section) {
        if (section != null && section.sectionType == STARTUP) {
            Atroxiia dragon = getUser();
            dragon.triggerAnim(AnimationHelper.MOVEMENT_CONTROLLER, "precise_strike");
            dragon.getSoundHandler().playMovingEntitySound(ModSounds.ATROXIIA_PRECISE_STRIKE.get(), 1.0f, 1.0f, 120);
        }
    }

    @Override
    public void tickUsing() {
        DragonAbilitySection section = getCurrentSection();
        if (section == null || section.sectionType != ACTIVE) {
            return;
        }

        steerTowardAiTarget();

        int tick = getTicksInUse();
        if (tick == FIRST_NUDGE_AND_DAMAGE_TICK) {
            nudgeForward();
            damageTargets();
        } else if (tick == PULL_TARGETS_TICK) {
            pullTargetsBack();
        } else if (tick == SECOND_NUDGE_TICK) {
            nudgeForward();
        } else if (tick == SECOND_DAMAGE_TICK) {
            damageTargets();
        } else if (tick == THIRD_NUDGE_TICK) {
            nudgeForward();
        } else if (tick == THIRD_DAMAGE_TICK) {
            getUser().triggerScreenShake(THIRD_STRIKE_SCREEN_SHAKE, THIRD_STRIKE_SCREEN_SHAKE_TICKS);
            damageTargets();
        }
    }

    private void steerTowardAiTarget() {
        Atroxiia dragon = getUser();
        if (dragon.level().isClientSide || dragon.getControllingPassenger() != null) {
            return;
        }

        LivingEntity target = dragon.getTarget();
        if (!dragon.isTargetValid(target)) {
            return;
        }

        double dx = target.getX() - dragon.getX();
        double dz = target.getZ() - dragon.getZ();
        Vec3 horizontalDirection = new Vec3(dx, 0.0D, dz);
        if (horizontalDirection.lengthSqr() < 1.0E-6D) {
            return;
        }

        float targetYaw = (float)(Mth.atan2(dz, dx) * Mth.RAD_TO_DEG) - 90.0F;
        float steeredYaw = Mth.approachDegrees(
                dragon.getYRot(),
                targetYaw,
                AI_STEERING_DEGREES_PER_TICK
        );
        dragon.setYRot(steeredYaw);
        dragon.yBodyRot = steeredYaw;
        dragon.yHeadRot = Mth.approachDegrees(
                dragon.yHeadRot,
                targetYaw,
                AI_STEERING_DEGREES_PER_TICK * 1.5F
        );
        dragon.getLookControl().setLookAt(target, 30.0F, 30.0F);
        dragon.steerPreciseStrikeNudge(Vec3.directionFromRotation(0.0F, steeredYaw));
    }

    private void nudgeForward() {
        Atroxiia dragon = getUser();
        if (dragon.isGroundedForAction()) {
            dragon.beginPreciseStrikeNudge(NUDGE_TICKS, NUDGE_DISTANCE);
        }
    }

    private void damageTargets() {
        Atroxiia dragon = getUser();
        List<LivingEntity> targets = findTargets();
        for (LivingEntity target : targets) {
            float damage = (float) DragonAttributeConfigLoader.getInstance()
                    .getConfig(DragonAttributeConfigLoader.ATROXIIA_ID)
                    .abilityDamage("precise_strike", BASE_DAMAGE);
            damage *= dragon.getHungerMeleeDamageMultiplier();

            DamageSource source = dragon.level().damageSources().mobAttack(dragon);
            target.hurt(source, damage);

            applyComboStun(target);
            knockTargetBack(target);
        }
    }

    private void applyComboStun(LivingEntity target) {
        int postComboStunTicks = DragonAttributeConfigLoader.getInstance()
                .getConfig(DragonAttributeConfigLoader.ATROXIIA_ID)
                .abilityStunDurationTicks("precise_strike", POST_COMBO_STUN_TICKS);
        int stunTicks = Math.max(1, ANIMATION_TICKS + RECOVERY_TICKS - getTicksInUse() + postComboStunTicks);
        AtroxiiaFrostImpact.apply(getUser(), target, stunTicks);
    }

    private void knockTargetBack(LivingEntity target) {
        Atroxiia dragon = getUser();
        double knockback = DragonAttributeConfigLoader.getInstance()
                .getConfig(DragonAttributeConfigLoader.ATROXIIA_ID)
                .abilityKnockback("precise_strike", DAMAGE_KNOCKBACK);
        Vec3 forward = DragonMeleeGeometry.forwardAttack(dragon).forward();
        Vec3 horizontalForward = new Vec3(forward.x, 0.0D, forward.z);
        if (horizontalForward.lengthSqr() < 1.0E-6D) {
            horizontalForward = Vec3.directionFromRotation(0.0F, dragon.getYRot());
        }

        Vec3 push = horizontalForward.normalize().scale(knockback);
        target.push(push.x, DAMAGE_KNOCKBACK_Y, push.z);
        target.hurtMarked = true;
    }

    private void pullTargetsBack() {
        Atroxiia dragon = getUser();
        Vec3 pullOrigin = dragon.position().add(0.0D, dragon.getBbHeight() * 0.45D, 0.0D);
        List<LivingEntity> targets = findTargets();
        for (LivingEntity target : targets) {
            Vec3 pull = pullOrigin.subtract(target.position());
            Vec3 horizontalPull = new Vec3(pull.x, 0.0D, pull.z);
            if (horizontalPull.lengthSqr() < 1.0E-6D) {
                continue;
            }
            Vec3 motion = horizontalPull.normalize().scale(PULL_STRENGTH);
            target.push(motion.x, 0.12D, motion.z);
            target.hurtMarked = true;
        }
    }

    private List<LivingEntity> findTargets() {
        Atroxiia dragon = getUser();
        return DragonMeleeGeometry.findForwardTargets(
                dragon,
                RANGE,
                SWEEP_HORIZONTAL,
                SWEEP_VERTICAL,
                ANGLE_DEG,
                RANGE * 0.45D,
                entity -> !dragon.isAlly(entity)
        );
    }
}

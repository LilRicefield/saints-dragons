package com.leon.saintsdragons.server.entity.ability.abilities.atroxiia;

import com.leon.saintsdragons.common.config.dragon.DragonAttributeConfigLoader;
import com.leon.saintsdragons.common.registry.ModSounds;
import com.leon.saintsdragons.server.entity.ability.DragonAbility;
import com.leon.saintsdragons.server.entity.ability.DragonAbilitySection;
import com.leon.saintsdragons.server.entity.ability.DragonAbilityType;
import com.leon.saintsdragons.server.entity.ability.DragonMeleeGeometry;
import com.leon.saintsdragons.server.entity.dragons.atroxiia.Atroxiia;
import com.leon.saintsdragons.util.animation.AnimationHelper;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.Vec3;

import java.util.List;

import static com.leon.saintsdragons.server.entity.ability.DragonAbilitySection.AbilitySectionDuration;
import static com.leon.saintsdragons.server.entity.ability.DragonAbilitySection.AbilitySectionType.ACTIVE;
import static com.leon.saintsdragons.server.entity.ability.DragonAbilitySection.AbilitySectionType.RECOVERY;
import static com.leon.saintsdragons.server.entity.ability.DragonAbilitySection.AbilitySectionType.STARTUP;

public class AtroxiiaPreciseStrikeAbility extends DragonAbility<Atroxiia> {
    private static final float BASE_DAMAGE = 9.0F;
    private static final int ANIMATION_TICKS = 100;
    private static final int RECOVERY_TICKS = 8;
    private static final int FIRST_NUDGE_AND_DAMAGE_TICK = 11;
    private static final int PULL_TARGETS_TICK = 25;
    private static final int SECOND_NUDGE_TICK = 39;
    private static final int SECOND_DAMAGE_TICK = 45;
    private static final int THIRD_NUDGE_TICK = 53;
    private static final int THIRD_DAMAGE_TICK = 64;
    private static final int NUDGE_TICKS = 5;
    private static final double NUDGE_DISTANCE = 4.0D;
    private static final double RANGE = 6.5D;
    private static final double SWEEP_HORIZONTAL = 6.5D;
    private static final double SWEEP_VERTICAL = 6.5D;
    private static final double ANGLE_DEG = 130.0D;
    private static final double PULL_STRENGTH = 0.75D;
    private static final double DAMAGE_KNOCKBACK = 0.75D;
    private static final double DAMAGE_KNOCKBACK_Y = 0.16D;
    private static final int STUN_SLOWNESS_AMPLIFIER = 6;
    private static final int STUN_WEAKNESS_AMPLIFIER = 1;

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
        Atroxiia dragon = getUser();
        return dragon.getControllingPassenger() != null && dragon.onGround() && !dragon.isBaby();
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
            damageTargets();
        }
    }

    private void nudgeForward() {
        Atroxiia dragon = getUser();
        if (dragon.onGround()) {
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
        int stunTicks = Math.max(1, ANIMATION_TICKS + RECOVERY_TICKS - getTicksInUse());
        target.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, stunTicks, STUN_SLOWNESS_AMPLIFIER, false, true));
        target.addEffect(new MobEffectInstance(MobEffects.WEAKNESS, stunTicks, STUN_WEAKNESS_AMPLIFIER, false, true));
    }

    private void knockTargetBack(LivingEntity target) {
        Atroxiia dragon = getUser();
        Vec3 forward = DragonMeleeGeometry.forwardAttack(dragon).forward();
        Vec3 horizontalForward = new Vec3(forward.x, 0.0D, forward.z);
        if (horizontalForward.lengthSqr() < 1.0E-6D) {
            horizontalForward = Vec3.directionFromRotation(0.0F, dragon.getYRot());
        }

        Vec3 push = horizontalForward.normalize().scale(DAMAGE_KNOCKBACK);
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

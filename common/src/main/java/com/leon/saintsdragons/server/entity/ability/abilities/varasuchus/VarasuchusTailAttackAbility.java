package com.leon.saintsdragons.server.entity.ability.abilities.varasuchus;

import com.leon.saintsdragons.common.config.dragon.DragonAttributeConfigLoader;
import com.leon.saintsdragons.common.registry.ModSounds;
import com.leon.saintsdragons.server.entity.ability.DragonAbility;
import com.leon.saintsdragons.server.entity.ability.DragonAbilitySection;
import com.leon.saintsdragons.server.entity.ability.DragonAbilityType;
import com.leon.saintsdragons.server.entity.ability.DragonMeleeGeometry;
import com.leon.saintsdragons.server.entity.dragons.varasuchus.Varasuchus;
import com.leon.saintsdragons.server.entity.dragons.varasuchus.handlers.VarasuchusAnimationHandler;
import net.minecraft.util.Mth;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.phys.Vec3;

import java.util.List;

import static com.leon.saintsdragons.server.entity.ability.DragonAbilitySection.AbilitySectionDuration;
import static com.leon.saintsdragons.server.entity.ability.DragonAbilitySection.AbilitySectionType.ACTIVE;
import static com.leon.saintsdragons.server.entity.ability.DragonAbilitySection.AbilitySectionType.RECOVERY;
import static com.leon.saintsdragons.server.entity.ability.DragonAbilitySection.AbilitySectionType.STARTUP;


public class VarasuchusTailAttackAbility extends DragonAbility<Varasuchus> {
    private static final float DEFAULT_DAMAGE = 8.0f;
    private static final double RANGE = 9.8;
    private static final double TAIL_ANGLE_DEG = 170.0;
    private static final double TAIL_SWIPE_HORIZONTAL = 4.0;
    private static final double TAIL_SWIPE_VERTICAL = 7.0;
    private static final double AI_DIRECT_EXTRA_REACH = 5.0;
    private static final double KNOCKBACK_STRENGTH = 1.4;
    private static final int CONTROL_LOCK_TICKS = (int) Math.round(1.4583 * 20);
    private static final int TAIL_ATTACK_SOUND_TICKS = 50;
    private static final DragonAbilitySection[] TRACK = new DragonAbilitySection[] {
            new AbilitySectionDuration(STARTUP, 12),
            new AbilitySectionDuration(ACTIVE, 2),
            new AbilitySectionDuration(RECOVERY, 15)
    };

    private boolean appliedHit;
    private final boolean useLeftTail;

    public VarasuchusTailAttackAbility(DragonAbilityType<Varasuchus, VarasuchusTailAttackAbility> type,
                                       Varasuchus user) {
        super(type, user, TRACK, 5);
        this.useLeftTail = user.shouldUseLeftTailAttack();
        user.toggleTailAttackSide();
    }

    @Override
    public boolean tryAbility() {
        Varasuchus dragon = getUser();
        return !dragon.isPhaseTwoActive() && !dragon.isSwimming() && !dragon.isInWaterOrBubble();
    }

    @Override
    protected void beginSection(DragonAbilitySection section) {
        if (section == null) {
            return;
        }

        if (section.sectionType == STARTUP) {
            Varasuchus dragon = getUser();
            dragon.lockRiderControls(CONTROL_LOCK_TICKS);
            String animName = useLeftTail ? "tail_attack_left" : "tail_attack_right";
            dragon.triggerAnim(VarasuchusAnimationHandler.FAST_ACTION_CONTROLLER, animName);
            if (!dragon.level().isClientSide) {
                dragon.getSoundHandler().playMovingEntitySound(ModSounds.VARASUCHUS_TAIL_ATTACK.get(), 1.0f, 1.0f, TAIL_ATTACK_SOUND_TICKS);
            }
            appliedHit = false;
        }
    }

    @Override
    public void tickUsing() {
        DragonAbilitySection section = getCurrentSection();
        if (section == null) {
            return;
        }

        if (section.sectionType == ACTIVE && !appliedHit) {
            Varasuchus dragon = getUser();
            List<LivingEntity> targets = findAllTargetsInCone();
            for (LivingEntity target : targets) {
                applyHit(dragon, target);
            }
            appliedHit = true;
        }
    }

    private void applyHit(Varasuchus dragon, LivingEntity target) {
        float desiredDamage = resolveBaseDamage() * dragon.getHungerMeleeDamageMultiplier();
        float armor = (float) target.getAttributeValue(Attributes.ARMOR);
        float toughness = (float) target.getAttributeValue(Attributes.ARMOR_TOUGHNESS);
        float rawToDeal = solveRawDamageForPostArmor(desiredDamage, armor, toughness);

        DamageSource source = dragon.level().damageSources().mobAttack(dragon);
        target.hurt(source, rawToDeal);

        Vec3 knockbackDir = target.position().subtract(dragon.position()).normalize();
        Vec3 push = knockbackDir.scale(KNOCKBACK_STRENGTH);
        target.push(push.x, 0.2, push.z);
        target.hurtMarked = true;
    }

    private float resolveBaseDamage() {
        return (float) DragonAttributeConfigLoader.getInstance()
                .getConfig(DragonAttributeConfigLoader.VARASUCHUS_ID)
                .abilityDamage("tail_attack", DEFAULT_DAMAGE);
    }

    private List<LivingEntity> findAllTargetsInCone() {
        Varasuchus dragon = getUser();
        boolean ridden = dragon.getControllingPassenger() != null;
        double effectiveRange = RANGE;

        if (!ridden) {
            LivingEntity target = dragon.getTarget();
            if (DragonMeleeGeometry.isDirectAiTargetValid(dragon, target, AI_DIRECT_EXTRA_REACH)) {
                return java.util.List.of(target);
            }
            return java.util.List.of();
        }

        return DragonMeleeGeometry.findForwardTargets(
                dragon,
                effectiveRange,
                TAIL_SWIPE_HORIZONTAL,
                TAIL_SWIPE_VERTICAL,
                TAIL_ANGLE_DEG,
                effectiveRange * 0.4D,
                entity -> !dragon.isAlly(entity)
        );
    }

    private static float damageAfterArmor(float damage, float armor, float toughness) {
        float f = 2.0F + toughness / 4.0F;
        float reduction = Mth.clamp(armor - damage / f, armor * 0.2F, 20.0F);
        float mult = 1.0F - reduction / 25.0F;
        return damage * mult;
    }

    private static float solveRawDamageForPostArmor(float desiredPostArmor, float armor, float toughness) {
        float lo = desiredPostArmor;
        float hi = desiredPostArmor;
        for (int i = 0; i < 8 && damageAfterArmor(hi, armor, toughness) < desiredPostArmor; i++) {
            hi *= 2.0F;
        }
        for (int i = 0; i < 10; i++) {
            float mid = (lo + hi) * 0.5F;
            float val = damageAfterArmor(mid, armor, toughness);
            if (val < desiredPostArmor) {
                lo = mid;
            } else {
                hi = mid;
            }
        }
        return hi;
    }
}

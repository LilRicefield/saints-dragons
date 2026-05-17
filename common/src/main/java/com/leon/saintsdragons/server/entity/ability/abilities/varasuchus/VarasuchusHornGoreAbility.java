package com.leon.saintsdragons.server.entity.ability.abilities.varasuchus;

import com.leon.saintsdragons.common.config.dragon.DragonAttributeConfigLoader;
import com.leon.saintsdragons.common.registry.ModSounds;
import com.leon.saintsdragons.server.entity.ability.DragonAbility;
import com.leon.saintsdragons.server.entity.ability.DragonAbilitySection;
import com.leon.saintsdragons.server.entity.ability.DragonAbilityType;
import com.leon.saintsdragons.server.entity.ability.DragonMeleeGeometry;
import com.leon.saintsdragons.server.entity.dragons.varasuchus.Varasuchus;
import com.leon.saintsdragons.server.entity.dragons.varasuchus.handlers.VarasuchusAnimationHandler;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.Vec3;

import java.util.List;

import static com.leon.saintsdragons.server.entity.ability.DragonAbilitySection.*;


public class VarasuchusHornGoreAbility extends DragonAbility<Varasuchus> {
    private static final float DEFAULT_PHASE1_DAMAGE = 16.0f;
    private static final float DEFAULT_PHASE2_DAMAGE = 20.8f;
    private static final float DEFAULT_ATTACK_DAMAGE = 10.0f;
    private static final double GORE_RANGE = 5.0;
    private static final double GORE_ANGLE_DEG = 90.0;
    private static final double AI_DIRECT_EXTRA_REACH = GORE_RANGE;
    private static final DragonAbilitySection[] TRACK = new DragonAbilitySection[] {
            new AbilitySectionDuration(AbilitySectionType.STARTUP, 5),
            new AbilitySectionDuration(AbilitySectionType.ACTIVE, 6),
            new AbilitySectionDuration(AbilitySectionType.RECOVERY, 6)
    };

    private final java.util.Set<Integer> hitIdsThisUse = new java.util.HashSet<>();

    public VarasuchusHornGoreAbility(DragonAbilityType<Varasuchus, VarasuchusHornGoreAbility> type, Varasuchus user) {
        super(type, user, TRACK, 3);
    }

    @Override
    protected void beginSection(DragonAbilitySection section) {
        if (section == null) return;
        if (section.sectionType == AbilitySectionType.STARTUP) {
            getUser().triggerAnim(VarasuchusAnimationHandler.ACTION_CONTROLLER, "horn_gore");
            if (!getUser().level().isClientSide) {
                getUser().getSoundHandler().playMovingEntitySound(ModSounds.VARASUCHUS_HORNGORE.get(), 1.0f, 1.0f, 24);
            }
            hitIdsThisUse.clear();
        }
    }

    @Override
    public void tickUsing() {
        DragonAbilitySection section = getCurrentSection();
        if (section == null) return;
        if (section.sectionType != AbilitySectionType.ACTIVE) return;
        java.util.List<LivingEntity> candidates = findTargets();
        java.util.List<LivingEntity> newHits = new java.util.ArrayList<>();
        for (LivingEntity le : candidates) {
            if (hitIdsThisUse.add(le.getId())) {
                newHits.add(le);
            }
        }
        if (!newHits.isEmpty()) {
            for (LivingEntity le : newHits) {
                applyGore(le);
            }
        }
    }

    private java.util.List<LivingEntity> findTargets() {
        Varasuchus dragon = getUser();
        boolean ridden = dragon.getControllingPassenger() != null;
        double range = GORE_RANGE;

        if (!ridden) {
            LivingEntity target = dragon.getTarget();
            if (DragonMeleeGeometry.isDirectAiTargetValid(dragon, target, AI_DIRECT_EXTRA_REACH)) {
                return java.util.List.of(target);
            }
            return java.util.List.of();
        }

        return DragonMeleeGeometry.findForwardTargets(
                dragon,
                range,
                range,
                range,
                GORE_ANGLE_DEG,
                range * 0.6D,
                entity -> !isAllied(dragon, entity)
        );
    }

    private void applyGore(LivingEntity target) {
        Varasuchus dragon = getUser();
        DamageSource src = dragon.level().damageSources().mobAttack(dragon);

        boolean phaseTwo = dragon.isPhaseTwoActive();
        float configuredDamage = resolveBaseDamage(phaseTwo);
        configuredDamage *= dragon.getHungerMeleeDamageMultiplier();
        AttributeInstance attackAttr = dragon.getAttribute(Attributes.ATTACK_DAMAGE);
        if (attackAttr != null && DEFAULT_ATTACK_DAMAGE > 0.0f) {
            double value = attackAttr.getValue();
            if (value > 0) {
                configuredDamage *= value / DEFAULT_ATTACK_DAMAGE;
            }
        }
        float armorPenetration = phaseTwo ? 5.0f : 3.0f;
        float armor = (float) target.getAttributeValue(Attributes.ARMOR);
        float toughness = (float) target.getAttributeValue(Attributes.ARMOR_TOUGHNESS);
        float desiredPostArmor = damageAfterArmor(configuredDamage, Math.max(0f, armor - armorPenetration), toughness);
        float rawToDeal = solveRawDamageForPostArmor(desiredPostArmor, armor, toughness);
        target.hurt(src, rawToDeal);
        Vec3 look = dragon.getLookAngle().normalize();
        double strength = phaseTwo ? 3.5 : 2.0;
        target.knockback((float) strength, -look.x, -look.z);
        Vec3 dv = target.getDeltaMovement();
        float verticalLift = phaseTwo ? 0.9f : 0.5f;
        target.setDeltaMovement(dv.x, Math.max(dv.y, verticalLift), dv.z);

    }

    private static float damageAfterArmor(float damage, float armor, float toughness) {
        float f = 2.0F + toughness / 4.0F;
        float reduction = Mth.clamp(armor - damage / f, armor * 0.2F, 20.0F);
        return damage * (1.0F - reduction / 25.0F);
    }

    private static float solveRawDamageForPostArmor(float desiredPostArmor, float armor, float toughness) {
        float lo = 0.0f;
        float hi = Math.max(desiredPostArmor + 16.0f, 16.0f);
        for (int i = 0; i < 8 && damageAfterArmor(hi, armor, toughness) < desiredPostArmor; i++) {
            hi *= 2.0f;
        }
        for (int it = 0; it < 20; it++) {
            float mid = (lo + hi) * 0.5f;
            float val = damageAfterArmor(mid, armor, toughness);
            if (val < desiredPostArmor) lo = mid; else hi = mid;
        }
        return (lo + hi) * 0.5f;
    }

    private boolean isAllied(Varasuchus dragon, Entity other) {
        return dragon.isAlly(other);
    }

    private float resolveBaseDamage(boolean phaseTwo) {
        String key = phaseTwo ? "horn_gore_phase2" : "horn_gore_phase1";
        float fallback = phaseTwo ? DEFAULT_PHASE2_DAMAGE : DEFAULT_PHASE1_DAMAGE;
        return (float) DragonAttributeConfigLoader.getInstance()
                .getConfig(DragonAttributeConfigLoader.VARASUCHUS_ID)
                .abilityDamage(key, fallback);
    }
}

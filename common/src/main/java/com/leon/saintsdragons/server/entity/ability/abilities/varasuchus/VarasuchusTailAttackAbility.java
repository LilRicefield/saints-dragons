package com.leon.saintsdragons.server.entity.ability.abilities.varasuchus;

import com.leon.saintsdragons.common.config.dragon.DragonAttributeConfigLoader;
import com.leon.saintsdragons.common.registry.ModSounds;
import com.leon.saintsdragons.server.entity.ability.DragonAbility;
import com.leon.saintsdragons.server.entity.ability.DragonAbilitySection;
import com.leon.saintsdragons.server.entity.ability.DragonAbilityType;
import com.leon.saintsdragons.server.entity.dragons.varasuchus.Varasuchus;
import net.minecraft.util.Mth;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import java.util.List;

import static com.leon.saintsdragons.server.entity.ability.DragonAbilitySection.AbilitySectionDuration;
import static com.leon.saintsdragons.server.entity.ability.DragonAbilitySection.AbilitySectionType.ACTIVE;
import static com.leon.saintsdragons.server.entity.ability.DragonAbilitySection.AbilitySectionType.RECOVERY;
import static com.leon.saintsdragons.server.entity.ability.DragonAbilitySection.AbilitySectionType.STARTUP;

/**
 * Phase 1 tail attack for the Varasuchus. Standing attack that alternates left/right.
 */
public class VarasuchusTailAttackAbility extends DragonAbility<Varasuchus> {
    // 8 HP = 8 damage points (4 hearts)
    private static final float DEFAULT_DAMAGE = 8.0f;

    // Based on bite range, widened slightly.
    private static final double BASE_RANGE = 9.0;
    private static final double RIDDEN_RANGE_BONUS = 0.8;
    private static final double TAIL_ANGLE_DEG = 170.0;
    private static final double TAIL_SWIPE_HORIZONTAL = 9.0;
    private static final double TAIL_SWIPE_HORIZONTAL_RIDDEN = 4.0;
    private static final double TAIL_SWIPE_VERTICAL = 7.0;
    private static final double KNOCKBACK_STRENGTH = 1.4;

    // Animation timing: 1.4583 seconds = 29 ticks total
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
        // Phase 1 only, and disallow underwater usage (no G underwater in phase 1).
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
            dragon.triggerAnim("instant", animName);
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

    private double getEffectiveRange() {
        Varasuchus dragon = getUser();
        double range = BASE_RANGE;
        if (dragon.getControllingPassenger() != null) {
            range += RIDDEN_RANGE_BONUS;
        }
        return range;
    }

    private List<LivingEntity> findAllTargetsInCone() {
        Varasuchus dragon = getUser();
        Vec3 mouth = dragon.getMouthPosition();
        Vec3 look = dragon.getLookAngle().normalize();

        boolean ridden = dragon.getControllingPassenger() != null;
        double effectiveRange = getEffectiveRange();

        if (!ridden) {
            LivingEntity target = dragon.getTarget();
            if (isDirectTargetValid(dragon, target, effectiveRange)) {
                return java.util.List.of(target);
            }
            return java.util.List.of();
        }

        double horizontalInflate = ridden ? TAIL_SWIPE_HORIZONTAL_RIDDEN : TAIL_SWIPE_HORIZONTAL;
        AABB forwardSweep = new AABB(mouth, mouth.add(look.scale(effectiveRange)))
                .inflate(horizontalInflate, TAIL_SWIPE_VERTICAL, horizontalInflate);

        List<LivingEntity> candidates = dragon.level().getEntitiesOfClass(LivingEntity.class, forwardSweep,
                e -> e != dragon && e.isAlive() && e.attackable() && !dragon.isAlly(e));

        double cosLimit = Math.cos(Math.toRadians(TAIL_ANGLE_DEG));
        List<LivingEntity> validTargets = new java.util.ArrayList<>();

        for (LivingEntity e : candidates) {
            double distToAabb = distancePointToAABB(mouth, e.getBoundingBox());
            if (distToAabb > effectiveRange + 0.5) continue;

            Vec3 toward = closestPointOnAABB(mouth, e.getBoundingBox()).subtract(mouth);
            double len = toward.length();
            if (len <= 0.0001) continue;
            Vec3 dir = toward.scale(1.0 / len);
            double dot = dir.dot(look);

            if (dot <= 0.0) continue;

            boolean veryClose = distToAabb < (effectiveRange * 0.4);
            boolean goodAngle = dot >= cosLimit;
            if (ridden) {
                goodAngle = goodAngle || dot >= (cosLimit * 0.75);
            }
            if (!(veryClose || goodAngle)) continue;

            validTargets.add(e);
        }

        return validTargets;
    }

    private boolean isDirectTargetValid(Varasuchus dragon, LivingEntity target, double range) {
        if (target == null || !target.isAlive() || !target.attackable() || dragon.isAlly(target) || !dragon.isTargetValid(target)) {
            return false;
        }
        double widthReach = dragon.getBbWidth() + target.getBbWidth() + 1.5D;
        return dragon.distanceTo(target) <= Math.max(range, widthReach);
    }

    private static double distancePointToAABB(Vec3 p, AABB box) {
        double dx = Math.max(Math.max(box.minX - p.x, 0.0), p.x - box.maxX);
        double dy = Math.max(Math.max(box.minY - p.y, 0.0), p.y - box.maxY);
        double dz = Math.max(Math.max(box.minZ - p.z, 0.0), p.z - box.maxZ);
        return Math.sqrt(dx * dx + dy * dy + dz * dz);
    }

    private static Vec3 closestPointOnAABB(Vec3 p, AABB box) {
        double cx = Mth.clamp(p.x, box.minX, box.maxX);
        double cy = Mth.clamp(p.y, box.minY, box.maxY);
        double cz = Mth.clamp(p.z, box.minZ, box.maxZ);
        return new Vec3(cx, cy, cz);
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

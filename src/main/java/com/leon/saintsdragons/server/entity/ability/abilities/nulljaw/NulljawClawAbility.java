package com.leon.saintsdragons.server.entity.ability.abilities.nulljaw;

import com.leon.saintsdragons.server.entity.ability.DragonAbility;
import com.leon.saintsdragons.server.entity.ability.DragonAbilitySection;
import com.leon.saintsdragons.server.entity.ability.DragonAbilityType;
import com.leon.saintsdragons.server.entity.dragons.nulljaw.Nulljaw;
import net.minecraft.util.Mth;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import java.util.List;

import static com.leon.saintsdragons.server.entity.ability.DragonAbilitySection.AbilitySectionDuration;
import static com.leon.saintsdragons.server.entity.ability.DragonAbilitySection.AbilitySectionType.ACTIVE;
import static com.leon.saintsdragons.server.entity.ability.DragonAbilitySection.AbilitySectionType.RECOVERY;
import static com.leon.saintsdragons.server.entity.ability.DragonAbilitySection.AbilitySectionType.STARTUP;

/**
 * Phase 2 claw attack for the Rift Drake. Alternates between left and right claw swipes.
 */
public class NulljawClawAbility extends DragonAbility<Nulljaw> {
    private static final float BASE_DAMAGE = 12.0f;
    private static final double BASE_RANGE = 5.0;
    private static final double RIDDEN_RANGE_BONUS = 1.5;
    private static final double CLAW_ANGLE_DEG = 100.0;     // Wider cone than bite
    private static final double CLAW_SWIPE_HORIZONTAL = 4.0;
    private static final double CLAW_SWIPE_HORIZONTAL_RIDDEN = 3.0;
    private static final double CLAW_SWIPE_VERTICAL = 4.0;

    private static final DragonAbilitySection[] TRACK = new DragonAbilitySection[] {
            new AbilitySectionDuration(STARTUP, 1),
            new AbilitySectionDuration(ACTIVE, 2),
            new AbilitySectionDuration(RECOVERY, 2)
    };

    private boolean appliedHit;
    private final boolean useLeftClaw;

    public NulljawClawAbility(DragonAbilityType<Nulljaw, NulljawClawAbility> type,
                              Nulljaw user) {
        super(type, user, TRACK, 3);
        // Determine which claw to use based on entity's toggle state
        this.useLeftClaw = user.shouldUseLeftClaw();
        // Toggle for next time
        user.toggleClawSide();
    }

    @Override
    public boolean tryAbility() {
        // Only allow in phase 2
        return getUser().isPhaseTwoActive();
    }

    @Override
    public boolean isOverlayAbility() {
        // Claw can run concurrently with bite2 for aggressive combos
        return true;
    }

    @Override
    protected void beginSection(DragonAbilitySection section) {
        if (section == null) {
            return;
        }

        if (section.sectionType == STARTUP) {
            Nulljaw dragon = getUser();
            // Trigger the appropriate claw animation
            String animName = useLeftClaw ? "claw_left" : "claw_right";
            dragon.triggerAnim("action", animName);
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
            Nulljaw dragon = getUser();

            // Hit ALL targets in range (multi-target attack)
            List<LivingEntity> targets = findAllTargets();

            for (LivingEntity target : targets) {
                applyHit(dragon, target);
            }

            appliedHit = true;
        }
    }

    private void applyHit(Nulljaw dragon, LivingEntity target) {
        float damage = BASE_DAMAGE;
        AttributeInstance attackAttr = dragon.getAttribute(Attributes.ATTACK_DAMAGE);
        if (attackAttr != null) {
            double value = attackAttr.getValue();
            if (value > 0) {
                damage = (float) (value * 1.2); // 20% more damage than bite
            }
        }

        DamageSource source = dragon.level().damageSources().mobAttack(dragon);
        target.hurt(source, damage);

        // Stronger knockback than bite
        Vec3 push = dragon.getLookAngle().scale(0.5);
        target.push(push.x, 0.15, push.z);
    }

    // ===== Range calculation =====

    private double getEffectiveRange() {
        Nulljaw dragon = getUser();
        double range = BASE_RANGE;

        if (dragon.getControllingPassenger() != null) {
            range += RIDDEN_RANGE_BONUS;
        }

        return range;
    }

    // ===== Multi-target finding =====

    private List<LivingEntity> findAllTargets() {
        Nulljaw dragon = getUser();
        Vec3 mouth = dragon.getMouthPosition();
        Vec3 look = dragon.getLookAngle().normalize();

        boolean ridden = dragon.getControllingPassenger() != null;
        double effectiveRange = getEffectiveRange();

        // Wide forward sweep for claw attacks
        double horizontalInflate = ridden ? CLAW_SWIPE_HORIZONTAL_RIDDEN : CLAW_SWIPE_HORIZONTAL;
        AABB forwardSweep = new AABB(mouth, mouth.add(look.scale(effectiveRange)))
                .inflate(horizontalInflate, CLAW_SWIPE_VERTICAL, horizontalInflate);

        List<LivingEntity> candidates = dragon.level().getEntitiesOfClass(LivingEntity.class, forwardSweep,
                e -> e != dragon && e.isAlive() && e.attackable() && !dragon.isAlly(e));

        double cosLimit = Math.cos(Math.toRadians(CLAW_ANGLE_DEG));
        List<LivingEntity> validTargets = new java.util.ArrayList<>();

        for (LivingEntity e : candidates) {
            // Compute closest point on target's AABB to the mouth
            double distToAabb = distancePointToAABB(mouth, e.getBoundingBox());
            if (distToAabb > effectiveRange + 0.5) continue;

            // Direction toward the closest point for angle test
            Vec3 toward = closestPointOnAABB(mouth, e.getBoundingBox()).subtract(mouth);
            double len = toward.length();
            if (len <= 0.0001) continue;
            Vec3 dir = toward.scale(1.0 / len);
            double dot = dir.dot(look);

            // Require forward projection
            if (dot <= 0.0) continue;

            // Wide cone for sweeping claws
            boolean veryClose = distToAabb < (effectiveRange * 0.4);
            boolean goodAngle = dot >= cosLimit;
            if (ridden) {
                goodAngle = goodAngle || dot >= (cosLimit * 0.7);
            }
            if (!(veryClose || goodAngle)) continue;

            validTargets.add(e);
        }
        return validTargets;
    }

    // ===== Geometry helpers =====

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
}

package com.leon.saintsdragons.server.entity.ability.abilities.stegonaut;

import com.leon.saintsdragons.common.registry.ModSounds;
import com.leon.saintsdragons.server.entity.ability.DragonAbility;
import com.leon.saintsdragons.server.entity.ability.DragonAbilitySection;
import com.leon.saintsdragons.server.entity.ability.DragonAbilityType;
import com.leon.saintsdragons.server.entity.dragons.stegonaut.Stegonaut;
import net.minecraft.util.Mth;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import java.util.Comparator;
import java.util.List;

import static com.leon.saintsdragons.server.entity.ability.DragonAbilitySection.AbilitySectionDuration;
import static com.leon.saintsdragons.server.entity.ability.DragonAbilitySection.AbilitySectionType.ACTIVE;
import static com.leon.saintsdragons.server.entity.ability.DragonAbilitySection.AbilitySectionType.RECOVERY;
import static com.leon.saintsdragons.server.entity.ability.DragonAbilitySection.AbilitySectionType.STARTUP;

public class StegonautBiteAbility extends DragonAbility<Stegonaut> {
    private static final float BASE_DAMAGE = 5.0f;
    private static final int HIT_TICK = 13;
    private static final double BASE_RANGE = 3.0;
    private static final double RIDDEN_RANGE_BONUS = 1.0;
    private static final double BITE_ANGLE_DEG = 80.0;
    private static final double POINT_BLANK_HIT_DISTANCE = 1.1;
    private static final double SWEEP_HORIZONTAL = 2.5;
    private static final double SWEEP_VERTICAL = 2.5;

    private static final DragonAbilitySection[] TRACK = new DragonAbilitySection[] {
            new AbilitySectionDuration(STARTUP, 7),
            new AbilitySectionDuration(ACTIVE, 3),
            new AbilitySectionDuration(RECOVERY, 11)
    };

    private boolean appliedHit;

    public StegonautBiteAbility(DragonAbilityType<Stegonaut, StegonautBiteAbility> type, Stegonaut user) {
        super(type, user, TRACK, 16);
    }

    @Override
    protected void beginSection(DragonAbilitySection section) {
        if (section == null) {
            return;
        }
        if (section.sectionType == STARTUP) {
            getUser().triggerAnim("action", "bite");
            if (!getUser().level().isClientSide) {
                getUser().getSoundHandler().playMovingEntitySound(ModSounds.STEGONAUT_BITE.get(), 1.0f, getUser().isBaby() ? 1.6f : 1.0f, 59);
            }
            appliedHit = false;
        }
    }

    @Override
    public void tickUsing() {
        if (!appliedHit && getTicksInUse() >= HIT_TICK) {
            List<LivingEntity> targets = findTargets();
            for (LivingEntity target : targets) {
                applyHit(target);
            }
            appliedHit = true;
        }
    }

    private void applyHit(LivingEntity target) {
        Stegonaut dragon = getUser();
        float damage = BASE_DAMAGE * dragon.getHungerMeleeDamageMultiplier();
        DamageSource source = dragon.level().damageSources().mobAttack(dragon);
        target.hurt(source, damage);

        Vec3 push = dragon.getLookAngle().scale(0.25);
        target.push(push.x, 0.06, push.z);
    }

    private List<LivingEntity> findTargets() {
        Stegonaut dragon = getUser();
        Vec3 mouth = dragon.getMouthPosition();
        Vec3 look = dragon.getLookAngle().normalize();

        double range = BASE_RANGE + (dragon.getControllingPassenger() != null ? RIDDEN_RANGE_BONUS : 0.0);
        AABB sweep = new AABB(mouth, mouth.add(look.scale(range)))
                .inflate(SWEEP_HORIZONTAL, SWEEP_VERTICAL, SWEEP_HORIZONTAL);

        double cosLimit = Math.cos(Math.toRadians(BITE_ANGLE_DEG));
        List<LivingEntity> candidates = dragon.level().getEntitiesOfClass(LivingEntity.class, sweep,
                e -> e != dragon && e.isAlive() && e.attackable() && !dragon.isAlly(e));

        return candidates.stream()
                .filter(e -> isValidForwardTarget(mouth, look, range, cosLimit, e))
                .sorted(Comparator.comparingDouble(e -> distancePointToAABB(mouth, e.getBoundingBox())))
                .toList();
    }

    private static boolean isValidForwardTarget(Vec3 origin, Vec3 look, double range, double cosLimit, LivingEntity target) {
        double dist = distancePointToAABB(origin, target.getBoundingBox());
        if (dist > range + 0.35) {
            return false;
        }

        // Guarantee point-blank connections even if the mouth origin is inside/against the target hitbox.
        if (dist <= POINT_BLANK_HIT_DISTANCE) {
            return true;
        }

        Vec3 toward = closestPointOnAABB(origin, target.getBoundingBox()).subtract(origin);
        double len = toward.length();
        if (len <= 0.0001) {
            return true;
        }
        Vec3 dir = toward.scale(1.0 / len);
        double dot = dir.dot(look);
        if (dot <= 0.0) {
            return false;
        }
        return dist < (range * 0.35) || dot >= cosLimit;
    }

    private static double distancePointToAABB(Vec3 p, AABB box) {
        double dx = Math.max(Math.max(box.minX - p.x, 0.0), p.x - box.maxX);
        double dy = Math.max(Math.max(box.minY - p.y, 0.0), p.y - box.maxY);
        double dz = Math.max(Math.max(box.minZ - p.z, 0.0), p.z - box.maxZ);
        return Math.sqrt(dx * dx + dy * dy + dz * dz);
    }

    private static Vec3 closestPointOnAABB(Vec3 p, AABB box) {
        return new Vec3(
                Mth.clamp(p.x, box.minX, box.maxX),
                Mth.clamp(p.y, box.minY, box.maxY),
                Mth.clamp(p.z, box.minZ, box.maxZ)
        );
    }
}

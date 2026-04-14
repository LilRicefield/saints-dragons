package com.leon.saintsdragons.server.entity.ability.abilities.varasuchus;

import com.leon.saintsdragons.common.registry.ModSounds;
import com.leon.saintsdragons.server.entity.ability.DragonAbility;
import com.leon.saintsdragons.server.entity.ability.DragonAbilitySection;
import com.leon.saintsdragons.server.entity.ability.DragonAbilityType;
import com.leon.saintsdragons.server.entity.dragons.varasuchus.Varasuchus;
import net.minecraft.util.Mth;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.List;

import static com.leon.saintsdragons.server.entity.ability.DragonAbilitySection.AbilitySectionDuration;
import static com.leon.saintsdragons.server.entity.ability.DragonAbilitySection.AbilitySectionType.ACTIVE;

public class VarasuchusSlashBarrageAbility extends DragonAbility<Varasuchus> {
    private static final DragonAbilitySection[] TRACK = new DragonAbilitySection[] {
            new AbilitySectionDuration(ACTIVE, 93)
    };

    private static final int TOTAL_TICKS = 93;
    private static final int SOUND_TICKS = 140;
    private static final int[] HIT_TICKS = new int[] {2, 8, 14, 20, 24, 30, 36, 41, 46, 51, 61, 71, 78, 82};
    private static final float HIT_DAMAGE = 15.0F;
    private static final double CLAW_RANGE = 5.0;
    private static final double CLAW_RANGE_RIDDEN_BONUS = 1.5;
    private static final double CLAW_HORIZONTAL = 4.0;
    private static final double CLAW_HORIZONTAL_RIDDEN = 3.0;
    private static final double CLAW_VERTICAL = 4.0;
    private static final double CLAW_ANGLE_DEG = 100.0;

    private final boolean[] hitsApplied = new boolean[HIT_TICKS.length];

    public VarasuchusSlashBarrageAbility(DragonAbilityType<Varasuchus, VarasuchusSlashBarrageAbility> type, Varasuchus user) {
        super(type, user, TRACK, 16);
    }

    @Override
    public boolean tryAbility() {
        return getUser().isPhaseTwoActive();
    }

    @Override
    protected void beginSection(DragonAbilitySection section) {
        if (section == null) {
            return;
        }

        if (section.sectionType == ACTIVE) {
            Varasuchus dragon = getUser();
            dragon.triggerAnim("instant", "slash_barrage");
            dragon.lockAbilities(TOTAL_TICKS);
            if (!dragon.level().isClientSide) {
                dragon.getSoundHandler().playMovingEntitySound(ModSounds.VARASUCHUS_SLASH_BARRAGE.get(), 1.0f, 1.0f, SOUND_TICKS);
            }
            enforceWalkOnly(dragon);
        }
    }

    @Override
    public void tickUsing() {
        DragonAbilitySection section = getCurrentSection();
        if (section == null || section.sectionType != ACTIVE || getUser().level().isClientSide) {
            return;
        }

        Varasuchus dragon = getUser();
        int ticks = getTicksInSection();
        enforceWalkOnly(dragon);

        for (int i = 0; i < HIT_TICKS.length; i++) {
            if (!hitsApplied[i] && ticks >= HIT_TICKS[i]) {
                applyHit(dragon);
                hitsApplied[i] = true;
            }
        }
    }

    private void enforceWalkOnly(Varasuchus dragon) {
        dragon.setAccelerating(false);

        if (dragon.isVehicle()) {
            boolean moving = Math.abs(dragon.getLastRiderForward()) > 0.05F || Math.abs(dragon.getLastRiderStrafe()) > 0.05F;
            dragon.setGroundMoveStateFromRider(moving ? 1 : 0);
            return;
        }

        LivingEntity target = dragon.getTarget();
        if (target != null && target.isAlive()) {
            dragon.getNavigation().moveTo(target, 1.0D);
            dragon.setGroundMoveStateFromAI(1);
        } else {
            dragon.setGroundMoveStateFromAI(0);
        }
    }

    private void applyHit(Varasuchus dragon) {
        List<LivingEntity> targets = findClawTargets(dragon);
        if (targets.isEmpty()) {
            return;
        }

        float damage = HIT_DAMAGE * dragon.getHungerMeleeDamageMultiplier();
        DamageSource source = dragon.level().damageSources().mobAttack(dragon);
        Vec3 push = dragon.getLookAngle().scale(0.25);

        for (LivingEntity target : targets) {
            target.hurt(source, damage);
            target.setDeltaMovement(Vec3.ZERO);
            target.push(push.x * 0.1D, 0.0D, push.z * 0.1D);
            target.hurtMarked = true;
            target.hasImpulse = true;
        }
    }

    private List<LivingEntity> findClawTargets(Varasuchus dragon) {
        Vec3 origin = dragon.getMouthPosition();
        Vec3 forward = dragon.getLookAngle().normalize();
        boolean ridden = dragon.getControllingPassenger() != null;

        double range = CLAW_RANGE + (ridden ? CLAW_RANGE_RIDDEN_BONUS : 0.0);

        if (!ridden) {
            LivingEntity target = dragon.getTarget();
            if (isDirectTargetValid(dragon, target, range)) {
                return List.of(target);
            }
            return List.of();
        }

        double horizontal = ridden ? CLAW_HORIZONTAL_RIDDEN : CLAW_HORIZONTAL;

        AABB sweep = new AABB(origin, origin.add(forward.scale(range)))
                .inflate(horizontal, CLAW_VERTICAL, horizontal);

        List<LivingEntity> candidates = dragon.level().getEntitiesOfClass(LivingEntity.class, sweep,
                entity -> entity != dragon && entity.isAlive() && entity.attackable() && !dragon.isAlly(entity));

        double cosLimit = Math.cos(Math.toRadians(CLAW_ANGLE_DEG));
        List<LivingEntity> valid = new ArrayList<>();

        for (LivingEntity candidate : candidates) {
            double distance = distancePointToAABB(origin, candidate.getBoundingBox());
            if (distance > range + 0.5) {
                continue;
            }

            Vec3 toward = closestPointOnAABB(origin, candidate.getBoundingBox()).subtract(origin);
            double len = toward.length();
            if (len <= 1.0e-4) {
                continue;
            }

            Vec3 dir = toward.scale(1.0 / len);
            double dot = dir.dot(forward);
            if (dot <= 0.0) {
                continue;
            }

            boolean veryClose = distance < (range * 0.4);
            boolean goodAngle = dot >= cosLimit;
            if (ridden) {
                goodAngle = goodAngle || dot >= (cosLimit * 0.7);
            }

            if (veryClose || goodAngle) {
                valid.add(candidate);
            }
        }

        return valid;
    }

    private boolean isDirectTargetValid(Varasuchus dragon, LivingEntity target, double range) {
        if (target == null || !target.isAlive() || !target.attackable() || dragon.isAlly(target) || !dragon.isTargetValid(target)) {
            return false;
        }
        double widthReach = dragon.getBbWidth() + target.getBbWidth() + 1.5D;
        return dragon.distanceTo(target) <= Math.max(range, widthReach);
    }

    private static double distancePointToAABB(Vec3 point, AABB box) {
        double dx = Math.max(Math.max(box.minX - point.x, 0.0), point.x - box.maxX);
        double dy = Math.max(Math.max(box.minY - point.y, 0.0), point.y - box.maxY);
        double dz = Math.max(Math.max(box.minZ - point.z, 0.0), point.z - box.maxZ);
        return Math.sqrt(dx * dx + dy * dy + dz * dz);
    }

    private static Vec3 closestPointOnAABB(Vec3 point, AABB box) {
        double cx = Mth.clamp(point.x, box.minX, box.maxX);
        double cy = Mth.clamp(point.y, box.minY, box.maxY);
        double cz = Mth.clamp(point.z, box.minZ, box.maxZ);
        return new Vec3(cx, cy, cz);
    }
}

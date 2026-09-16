package com.leon.saintsdragons.server.entity.component;

import com.leon.saintsdragons.common.registry.ModAbilities;
import com.leon.saintsdragons.server.ai.DragonTargetingHelper;
import com.leon.saintsdragons.server.ai.dragonbrain.learning.DragonCombatLearning;
import com.leon.saintsdragons.server.entity.dragons.volitans.Volitans;
import net.minecraft.core.BlockPos;
import net.minecraft.tags.FluidTags;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.Vec3;

import java.util.UUID;

public final class VolitansWaterCombatMovement {
    private final Volitans dragon;
    private UUID opponent;
    private UUID mount;
    private Vec3 destination;
    private Vec3 observedCenter;
    private long nextDecision;
    private long committedUntil;
    private long meleeUntil;
    private long separateUntil;
    private boolean wasMelee;
    private boolean wasBreath;
    private int side;
    private String decision = "idle";

    public VolitansWaterCombatMovement(Volitans dragon) { this.dragon = dragon; }

    public void reset() {
        opponent = null;
        mount = null;
        destination = null;
        observedCenter = null;
        nextDecision = committedUntil = meleeUntil = separateUntil = 0;
        wasMelee = wasBreath = false;
        decision = "idle";
    }

    public boolean holdForMelee() {
        boolean melee = dragon.isAbilityActive(ModAbilities.VOLITANS_BITE)
                || dragon.isAbilityActive(ModAbilities.VOLITANS_CLAW)
                || dragon.isAbilityActive(ModAbilities.VOLITANS_HORN_GORE);
        if (melee) {
            wasMelee = true;
            decision = "melee-committed";
        }
        return melee;
    }

    public boolean makingSpace(LivingEntity target) {
        return dragon.level().getGameTime() < separateUntil
                && !DragonTargetingHelper.isBiteOnlyPreyTarget(dragon, target);
    }

    public Vec3 destination(LivingEntity target) {
        var anchor = DragonTargetingHelper.movementAnchor(target);
        long now = dragon.level().getGameTime();
        if (!target.getUUID().equals(opponent) || !anchor.getUUID().equals(mount)) {
            reset();
            opponent = target.getUUID();
            mount = anchor.getUUID();
            side = dragon.getRandom().nextBoolean() ? 1 : -1;
            meleeUntil = now + 24;
        }
        if (!dragon.getSensing().hasLineOfSight(target)) {
            decision = "remembered-course";
            return destination != null ? destination : observedCenter != null ? observedCenter : dragon.position();
        }
        Vec3 center = anchor.getBoundingBox().getCenter();
        if (observedCenter != null && observedCenter.distanceToSqr(center) > 32 * 32) destination = null;
        observedCenter = center;
        if (!DragonTargetingHelper.isMovementAnchorInWater(target)) {
            destination = null;
            decision = "shore-pursuit";
            return center;
        }
        boolean breath = dragon.isAbilityActive(ModAbilities.VOLITANS_BREATH);
        if (wasMelee || (wasBreath && !breath)) {
            destination = null;
            nextDecision = 0;
            meleeUntil = now + 40;
            if (wasMelee) separateUntil = now + 30;
        }
        wasMelee = false;
        wasBreath = breath;
        boolean reached = destination != null && dragon.position().distanceToSqr(destination) < 16;
        if (destination != null && now < nextDecision) return destination;
        if (destination != null && now < committedUntil && !reached
                && !dragon.horizontalCollision && !dragon.getAiSwimController().hasReachedPathEnd()) return destination;
        nextDecision = now + 20;
        committedUntil = now + 40 + dragon.getRandom().nextInt(21);
        Vec3 predicted = dragon.getCombatLearning().predictCenter(target, 4, 4);
        if (predicted != null) center = predicted;
        Vec3 self = dragon.getBoundingBox().getCenter();
        Vec3 away = self.subtract(center).multiply(1, 0, 1);
        away = away.lengthSqr() < 1.0E-6 ? Vec3.directionFromRotation(0, dragon.getYRot()) : away.normalize();
        double radii = (dragon.getBbWidth() + anchor.getBbWidth()) * 0.5;
        double gap = Math.max(0, self.distanceTo(center) - radii);
        boolean ranged = breath || dragon.getBreathCombat().ready(target);
        boolean approachMelee = DragonTargetingHelper.isBiteOnlyPreyTarget(dragon, target)
                || (!ranged && now >= meleeUntil);
        if (dragon.getRandom().nextFloat() < 0.25F) side = -side;
        double spacing = dragon.getCombatLearning().expectation(target, DragonCombatLearning.Attack.BREATH, false).spacingBonus() * 0.5;
        double desiredRange = approachMelee ? radii + 2.5 : radii + 10 + spacing;
        // Cut across a broad arc so the next leg faces into the target's area for a breath pass.
        double sweep = Math.toRadians((ranged ? 75 : 45) * side);
        if (gap > 30) sweep = Math.toRadians(15 * side);
        for (int attempt = 0; attempt < 6; attempt++) {
            double turn = attempt % 2 == 0 ? sweep : -sweep;
            double horizontalScale = attempt < 4 ? 1 : 0.65;
            Vec3 radial = new Vec3(away.x * Math.cos(turn) - away.z * Math.sin(turn), 0,
                    away.x * Math.sin(turn) + away.z * Math.cos(turn));
            double vertical = attempt < 2 ? (dragon.getRandom().nextDouble() - 0.5) * 4
                    : attempt < 4 ? -Math.min(3, dragon.getBbHeight()) : 0;
            Vec3 candidate = center.add(radial.scale(desiredRange * horizontalScale))
                    .add(0, vertical - dragon.getBbHeight() * 0.5, 0);
            if (!canOccupy(candidate)) continue;
            destination = candidate;
            if (attempt % 2 != 0) side = -side;
            if (approachMelee) meleeUntil = now + 50;
            decision = approachMelee ? "melee-approach" : breath ? "breath-pass" : gap < 7 ? "make-space" : "swim-flank";
            return destination;
        }
        // Let the existing async pathfinder project and validate a direct pursuit in confined water.
        destination = center.add(0, -dragon.getBbHeight() * 0.5, 0);
        committedUntil = nextDecision;
        decision = "confined-pursuit";
        return destination;
    }

    private boolean canOccupy(Vec3 feet) {
        var box = dragon.getBoundingBox().move(feet.subtract(dragon.position()));
        for (double x : new double[] {box.minX + 0.05, box.maxX - 0.05}) {
            for (double y : new double[] {box.minY + 0.05, box.maxY - 0.05}) {
                for (double z : new double[] {box.minZ + 0.05, box.maxZ - 0.05}) {
                    BlockPos pos = BlockPos.containing(x, y, z);
                    if (!dragon.level().hasChunkAt(pos) || !dragon.level().getFluidState(pos).is(FluidTags.WATER)) return false;
                }
            }
        }
        return dragon.level().noCollision(dragon, box);
    }

    public String debugSummary() { return decision; }
}

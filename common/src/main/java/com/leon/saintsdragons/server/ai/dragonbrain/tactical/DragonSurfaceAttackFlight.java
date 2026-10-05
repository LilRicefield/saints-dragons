package com.leon.saintsdragons.server.ai.dragonbrain.tactical;

import com.leon.saintsdragons.server.ai.DragonTargetingHelper;
import com.leon.saintsdragons.server.ai.dragonbrain.DragonBrainContext;
import com.leon.saintsdragons.server.ai.dragonbrain.DragonMemories;
import com.leon.saintsdragons.server.ai.dragonbrain.DragonMovementIntent;
import com.leon.saintsdragons.server.ai.navigation.async.DragonFlightRequest;
import com.leon.saintsdragons.server.entity.base.RideableFlyingDragon;
import net.minecraft.core.BlockPos;
import net.minecraft.tags.FluidTags;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

import java.util.UUID;
import java.util.function.BooleanSupplier;

public final class DragonSurfaceAttackFlight {
    private static final double SURFACE_CLEARANCE = 0.75D;
    private static final int PASS_TIMEOUT = 120;
    private UUID targetId;
    private long nextBite;
    private long nextPosition;
    private long phaseSince;
    private long nextValidation;
    private int side;
    private Phase phase = Phase.REPOSITION;
    private Vec3 destination;
    private Vec3 strike;
    private Vec3 exit;
    private Vec3 passDirection;
    private boolean bitten;
    private String decision = "inactive";

    public boolean reset() {
        boolean active = targetId != null;
        targetId = null;
        destination = strike = exit = passDirection = null;
        phase = Phase.REPOSITION;
        decision = "inactive";
        return active;
    }

    public void tick(DragonBrainContext<? extends RideableFlyingDragon> context, LivingEntity target,
                     DragonWaterCombatProfile profile, BooleanSupplier rangedAttack, BooleanSupplier biteAttack) {
        RideableFlyingDragon dragon = context.dragon();
        long now = context.gameTime();
        if (!target.getUUID().equals(targetId)) {
            reset();
            targetId = target.getUUID();
            nextBite = now + (profile.favorsRanged() ? profile.biteIntervalTicks() : 20);
            nextPosition = 0;
            side = dragon.getRandom().nextBoolean() ? 1 : -1;
        }
        // Do not substitute the live position of an unseen swimmer for the perception system's evidence.
        if (!context.memories().get(DragonMemories.TARGET_VISIBLE).orElse(false)) return;
        Entity anchor = DragonTargetingHelper.movementAnchor(target);
        double surface = surfaceHeight(dragon.level(), anchor.blockPosition());
        boolean surfaceKnown = Double.isFinite(surface);
        // A deeply submerged target must not pull the flier down while its surface is outside our local scan.
        if (!surfaceKnown) surface = dragon.getY() - profile.attackHeight();
        dragon.getLookControl().setLookAt(target, 30.0F, 30.0F);
        Vec3 focus = new Vec3(anchor.getX(), surface, anchor.getZ());
        var space = dragon.getAIMovement().flightSpace();
        if (phase != Phase.REPOSITION) {
            boolean expired = now - phaseSince >= PASS_TIMEOUT || dragon.getAIMovement().hasFailed();
            boolean moved = strike != null && focus.subtract(strike).horizontalDistanceSqr() > 64;
            boolean blocked = false;
            if (now >= nextValidation) {
                nextValidation = now + 10;
                blocked = destination == null || !space.corridorClear(dragon.position(), destination);
            }
            if (expired || moved || !surfaceKnown || target.getEyeY() < surface - 1.0D
                    || blocked) {
                finishPass(now, profile, "pass-aborted");
            }
        }
        if (phase == Phase.REPOSITION) {
            boolean ranged = rangedAttack.getAsBoolean();
            if (ranged && profile.favorsRanged()) nextBite = now + profile.biteIntervalTicks();
            if (!ranged && dragon.getActiveAbility() == null && now >= nextBite) {
                nextBite = now + profile.biteIntervalTicks();
                if (surfaceKnown && target.getEyeY() >= surface - 1.0D && planPass(dragon, target, focus, profile)) {
                    phase = Phase.APPROACH;
                    phaseSince = now;
                    nextValidation = now + 10;
                    bitten = false;
                    decision = "bite-approach";
                }
            }
            if (phase == Phase.REPOSITION && (now >= nextPosition
                    || destination != null && dragon.position().distanceToSqr(destination) < 16)) {
                nextPosition = now + 20;
                Vec3 radial = horizontal(dragon.position().subtract(focus), dragon.getLookAngle());
                Vec3 tangent = new Vec3(-radial.z, 0, radial.x);
                destination = null;
                for (int turn : new int[]{side, -side}) {
                    Vec3 preferred = focus.add(radial.scale(profile.attackRadius() * 0.7D))
                            .add(tangent.scale(turn * profile.attackRadius() * 0.7D))
                            .add(0, profile.attackHeight(), 0);
                    Vec3 fitted = space.fitDestination(preferred);
                    if (fitted != null && fitted.y >= surface + SURFACE_CLEARANCE
                            && space.corridorClear(dragon.position(), fitted)) {
                        destination = fitted;
                        side = turn;
                        break;
                    }
                }
                decision = destination == null ? "no-clear-surface-lane" : ranged ? "ranged-pass" : "reposition";
            }
        }
        if (phase == Phase.APPROACH && dragon.position().distanceToSqr(destination) < 9) {
            phase = Phase.STRIKE;
            phaseSince = now;
            destination = strike;
            decision = "bite-pass";
        }
        if (phase == Phase.STRIKE) {
            double gap = Math.max(0, dragon.distanceTo(target) - (dragon.getBbWidth() + target.getBbWidth()) * 0.5D);
            Vec3 toward = target.getBoundingBox().getCenter().subtract(dragon.getBoundingBox().getCenter()).normalize();
            if (!bitten && gap <= profile.biteRange() && dragon.getLookAngle().dot(toward) > 0.1D) {
                bitten = biteAttack.getAsBoolean();
            }
            if (dragon.position().distanceToSqr(strike) < 4
                    || dragon.position().subtract(strike).dot(passDirection) >= 0) {
                phase = Phase.EGRESS;
                phaseSince = now;
                destination = exit;
                decision = "bite-pull-away";
            }
        }
        if (phase == Phase.EGRESS && dragon.position().distanceToSqr(exit) < 9) {
            finishPass(now, profile, "pass-complete");
        }
        // Maintain forward motion while abilities play; never park in bite range.
        double blocksPerTick = phase == Phase.STRIKE ? 0.45D : 0.7D;
        double speed = blocksPerTick / Math.max(0.01D, dragon.getFlightSpeed());
        context.memories().set(DragonMemories.MOVEMENT_INTENT, destination == null
                ? DragonMovementIntent.holdPosition()
                : DragonMovementIntent.flight(DragonFlightRequest.maneuver(destination, speed, 1.5D,
                        DragonFlightRequest.Arrival.PASS_THROUGH)));
    }

    private boolean planPass(RideableFlyingDragon dragon, LivingEntity target, Vec3 focus,
                             DragonWaterCombatProfile profile) {
        var space = dragon.getAIMovement().flightSpace();
        Vec3 heading = horizontal(focus.subtract(dragon.position()), dragon.getLookAngle());
        Vec3 contact = focus.add(0, SURFACE_CLEARANCE, 0);
        double reach = profile.biteRange() + (dragon.getBbWidth() + target.getBbWidth()) * 0.5D;
        if (contact.distanceToSqr(target.position()) > reach * reach) return false;
        Vec3 entry = contact.subtract(heading.scale(profile.passLength())).add(0, profile.attackHeight(), 0);
        Vec3 away = contact.add(heading.scale(profile.passLength())).add(0, profile.attackHeight(), 0);
        if (!space.fits(contact) || !space.fits(entry) || !space.fits(away)
                || !space.corridorClear(dragon.position(), entry)
                || !space.corridorClear(entry, contact) || !space.corridorClear(contact, away)) return false;
        destination = entry;
        strike = contact;
        exit = away;
        passDirection = heading;
        return true;
    }

    private void finishPass(long now, DragonWaterCombatProfile profile, String reason) {
        phase = Phase.REPOSITION;
        destination = strike = exit = passDirection = null;
        nextPosition = 0;
        nextBite = now + profile.biteIntervalTicks();
        decision = reason;
    }

    private static Vec3 horizontal(Vec3 direction, Vec3 fallback) {
        Vec3 flat = direction.multiply(1, 0, 1);
        if (flat.lengthSqr() < 0.001D) flat = fallback.multiply(1, 0, 1);
        return flat.lengthSqr() < 0.001D ? new Vec3(0, 0, 1) : flat.normalize();
    }

    /** Read only the loaded water column; no heightmap of the ocean floor or chunk loading. */
    public static double surfaceHeight(Level level, BlockPos origin) {
        BlockPos.MutableBlockPos cursor = origin.mutable();
        if (!level.hasChunkAt(cursor)) return Double.NaN;
        if (!level.getFluidState(cursor).is(FluidTags.WATER)) cursor.move(0, -1, 0);
        if (!level.getFluidState(cursor).is(FluidTags.WATER)) return Double.NaN;
        for (int i = 0; i < 32 && cursor.getY() < level.getMaxBuildHeight() - 1; i++) {
            if (!level.hasChunkAt(cursor)) return Double.NaN;
            if (!level.getFluidState(cursor).is(FluidTags.WATER)) return cursor.getY();
            cursor.move(0, 1, 0);
        }
        return Double.NaN;
    }

    public String summary() { return phase.name().toLowerCase() + ":" + decision; }

    private enum Phase { REPOSITION, APPROACH, STRIKE, EGRESS }
}

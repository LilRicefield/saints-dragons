package com.leon.saintsdragons.server.ai.goals.base;

import com.leon.saintsdragons.server.entity.interfaces.DragonFlightCapable;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.phys.Vec3;

import java.util.EnumSet;

/**
 * Universal water escape goal for ALL dragons.
 * Handles both flying dragons (fly to shore) and ground dragons (walk to shore).
 * No async, no complex pathfinding - just finds nearest shore and goes there.
 *
 * When a dragon falls in water:
 * 1. Finds the nearest shore via spiral search
 * 2. Flying dragons: force takeoff and fly there
 * 3. Ground dragons: use navigation to walk there
 * 4. Stops when out of water
 */
public class DragonWaterEscapeGoal extends Goal {
    private final Mob mob;
    private final boolean canFly;
    private final DragonFlightCapable flyingDragon; // Null for ground dragons
    private Vec3 escapeTarget;
    private int recheckCooldown;

    // Constructor for flying dragons
    public DragonWaterEscapeGoal(DragonFlightCapable dragon) {
        this.mob = (Mob) dragon;
        this.canFly = true;
        this.flyingDragon = dragon;
        this.setFlags(EnumSet.of(Flag.MOVE));
    }

    // Constructor for ground dragons
    public DragonWaterEscapeGoal(Mob mob) {
        this.mob = mob;
        this.canFly = false;
        this.flyingDragon = null;
        this.setFlags(EnumSet.of(Flag.MOVE));
    }

    @Override
    public boolean canUse() {
        // Only activate if in water
        if (!mob.isInWater()) {
            return false;
        }

        // Flying dragons: don't activate if already escaping via flight
        if (canFly && flyingDragon.isFlying()) {
            return false;
        }

        // CRITICAL: Babies can't fly! Treat them as ground dragons
        if (canFly && mob.isBaby()) {
            // Use navigation like ground dragons instead of flight
            if (recheckCooldown > 0) {
                recheckCooldown--;
                return escapeTarget != null && mob.isInWater();
            }
            escapeTarget = findNearestShore();
            recheckCooldown = 40;
            if (escapeTarget != null) {
                mob.getNavigation().moveTo(escapeTarget.x, escapeTarget.y, escapeTarget.z, 1.5);
            }
            return escapeTarget != null;
        }

        // Don't interfere with riding
        if (mob.isVehicle()) {
            return false;
        }

        // Don't interfere with sitting (if mob is tameable)
        if (mob instanceof net.minecraft.world.entity.TamableAnimal tameable && tameable.isOrderedToSit()) {
            return false;
        }

        // Find shore every 2 seconds to avoid constant searching
        if (recheckCooldown > 0) {
            recheckCooldown--;
            return escapeTarget != null;
        }

        escapeTarget = findNearestShore();
        recheckCooldown = 40; // 2 seconds
        return escapeTarget != null;
    }

    @Override
    public boolean canContinueToUse() {
        // Stop when out of water or reached shore
        if (!mob.isInWater()) {
            return false;
        }

        if (escapeTarget == null) {
            return false;
        }

        // Stop when close to shore
        return mob.distanceToSqr(escapeTarget) > 4.0;
    }

    @Override
    public void start() {
        if (canFly && flyingDragon != null && !mob.isBaby()) {
            // Flying dragons: force takeoff (but NOT babies - they can't fly!)
            flyingDragon.setFlying(true);
            flyingDragon.setTakeoff(true);
            flyingDragon.setLanding(false);
        } else {
            // Ground dragons AND baby flying dragons: use navigation
            if (escapeTarget != null) {
                mob.getNavigation().moveTo(escapeTarget.x, escapeTarget.y, escapeTarget.z, 1.5); // Panic speed
            }
        }
    }

    @Override
    public void tick() {
        if (escapeTarget == null) {
            return;
        }

        if (canFly && flyingDragon != null && !mob.isBaby()) {
            // Flying dragons: fly directly to shore (but NOT babies!)
            mob.getMoveControl().setWantedPosition(
                escapeTarget.x,
                escapeTarget.y,
                escapeTarget.z,
                flyingDragon.getFlightSpeed() * 1.5 // Panic speed!
            );
        } else {
            // Ground dragons AND baby flying dragons: keep navigating
            if (mob.getNavigation().isDone()) {
                mob.getNavigation().moveTo(escapeTarget.x, escapeTarget.y, escapeTarget.z, 1.5);
            }
        }
    }

    @Override
    public void stop() {
        escapeTarget = null;
        recheckCooldown = 0;
        // Stop navigation for ground dragons AND babies
        if (!canFly || mob.isBaby()) {
            mob.getNavigation().stop();
        }
    }

    /**
     * Finds nearest shore via simple spiral search.
     */
    private Vec3 findNearestShore() {
        BlockPos start = mob.blockPosition();
        int maxRadius = canFly ? 32 : 48; // Ground dragons need more search range

        // Spiral search outward
        for (int radius = 4; radius <= maxRadius; radius += 4) {
            for (int angle = 0; angle < 360; angle += 15) {
                double rad = Math.toRadians(angle);
                int x = start.getX() + (int)(Math.cos(rad) * radius);
                int z = start.getZ() + (int)(Math.sin(rad) * radius);

                // Check if this position is on land (not water)
                BlockPos checkPos = new BlockPos(x, start.getY(), z);
                if (!mob.level().getFluidState(checkPos).isEmpty()) {
                    continue; // Still water, keep searching
                }

                // Find ground level at this position
                int groundY = mob.level().getHeight(Heightmap.Types.WORLD_SURFACE, x, z);
                BlockPos groundPos = new BlockPos(x, groundY, z);

                // Verify it's actually solid ground (not just air above water)
                if (mob.level().getBlockState(groundPos).isSolid() ||
                    mob.level().getBlockState(groundPos.below()).isSolid()) {
                    // Found shore!
                    if (canFly) {
                        return new Vec3(x + 0.5, groundY + 2, z + 0.5); // Fly slightly above ground
                    } else {
                        return new Vec3(x + 0.5, groundY, z + 0.5); // Walk to ground level
                    }
                }
            }
        }

        // Fallback
        if (canFly) {
            return mob.position().add(0, 10, 0); // Fly up
        } else {
            // Try to move in any direction away from water center
            double angle = mob.getRandom().nextDouble() * Math.PI * 2;
            double distance = 16.0;
            return mob.position().add(Math.cos(angle) * distance, 0, Math.sin(angle) * distance);
        }
    }
}

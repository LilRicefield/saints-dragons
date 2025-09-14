package com.leon.saintsdragons.server.entity.controller;

import com.leon.saintsdragons.server.entity.dragons.lightningdragon.LightningDragonEntity;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.MoverType;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Handles all riding mechanics for the Lightning Dragon
 */
public record DragonRiderController(LightningDragonEntity dragon) {
    // ===== SEAT TUNING CONSTANTS =====
    // Baseline vertical offset relative to dragon height
    private static final double SEAT_BASE_FACTOR = 0.50D; // 0.0..1.0 of bbHeight
    // Additional vertical lift to avoid clipping
    private static final double SEAT_LIFT = 0.1D;
    // Forward/back relative to body (blocks). +forward = toward head, - = toward tail
    private static final double SEAT_FORWARD = 3.5D;
    // Sideways relative to body (blocks). +side = to the dragon's right, - = left
    private static final double SEAT_SIDE = 0.00D;

    // No head-follow offsets; keep static body seat
    // ===== FLIGHT VERTICAL RATES =====
    // Up/down rates while flying controlled by keybinds
    private static final double ASCEND_RATE = 0.15D;
    private static final double DESCEND_RATE = 0.5D;

    // ===== RIDING UTILITIES =====

    @Nullable
    public Player getRidingPlayer() {
        if (dragon.getControllingPassenger() instanceof Player player) {
            return player;
        }
        return null;
    }

    // (isBeingRidden and getFlightSpeedModifier removed; not referenced)

    // ===== RIDER INPUT PROCESSING =====

    /**
     * Processes rider input and converts to movement vector
     */
    public Vec3 getRiddenInput(Player player, @SuppressWarnings("unused") Vec3 deltaIn) {
        float f = player.zza < 0.0F ? 0.5F : 1.0F;

        if (dragon.isFlying()) {
            // Flying movement – NO pitch-based vertical movement.
            // Vertical is controlled exclusively by ascend/descend keybinds.
            return new Vec3(player.xxa * 0.4F, 0.0F, player.zza * 1.0F * f);
        } else {
            // Ground movement - no vertical component, responsive controls
            return new Vec3(player.xxa * 0.5F, 0.0D, player.zza * 0.9F * f);
        }
    }

    /**
     * Main rider tick method - handles rotation and banking
     */
    public void tickRidden(Player player, @SuppressWarnings("unused") Vec3 travelVector) {
        // Prevent accidental rider fall damage while mounted
        player.fallDistance = 0.0F;
        dragon.fallDistance = 0.0F;
        // Clear target when being ridden to prevent AI interference
        dragon.setTarget(null);

        // Make dragon responsive to player look direction
        if (dragon.isFlying()) {
            // Flying: BYPASS THE SLOW MATH! Nearly instant response!
            float targetYaw = player.getYRot();
            float targetPitch = player.getXRot() * 0.5f; // Scale down pitch for stability

            // DIRECT ASSIGNMENT with tiny smoothing - no more slow curves!
            float currentYaw = dragon.getYRot();
            float yawDiff = Mth.wrapDegrees(targetYaw - currentYaw);
            float newYaw = currentYaw + (yawDiff * 0.85f); // 85% instant response
            dragon.setYRot(newYaw);

            float currentPitch = dragon.getXRot();
            float pitchDiff = targetPitch - currentPitch;
            float newPitch = currentPitch + (pitchDiff * 0.8f); // 80% instant response
            dragon.setXRot(newPitch);

            // Banking currently disabled for stability
        } else {
            // Ground: Also direct response 
            float yawDiff = Math.abs(player.getYRot() - dragon.getYRot());
            if (player.zza != 0 || player.xxa != 0 || yawDiff > 5.0f) {
                float currentYaw = dragon.getYRot();
                float targetYaw = player.getYRot();
                float rawDiff = Mth.wrapDegrees(targetYaw - currentYaw);
                float newYaw = currentYaw + (rawDiff * 0.75f); // 75% instant response
                dragon.setYRot(newYaw);
                dragon.setXRot(0); // Keep level on ground
            }
        }

        // Update body and head rotation
        dragon.yBodyRot = dragon.getYRot();
        dragon.yHeadRot = dragon.getYRot();

        // Extra safety: if we just touched ground, ensure rider has no fall damage
        if (dragon.onGround()) {
            player.fallDistance = 0.0F;
            dragon.fallDistance = 0.0F;
        }
    }

    /**
     * Calculate riding speed based on current state
     */
    public float getRiddenSpeed(@SuppressWarnings("unused") Player rider) {
        if (dragon.isFlying()) {
            // Flying speed - use ONLY the attributed flying speed, no modifiers
            return (float) dragon.getAttributeValue(Attributes.FLYING_SPEED);
        } else {
            // Ground speed - use movement speed attribute with acceleration multipliers
            float baseSpeed = (float) dragon.getAttributeValue(Attributes.MOVEMENT_SPEED);

            if (dragon.isAccelerating()) {
                // L-Ctrl pressed - trigger run animation and boost speed
                dragon.setRunning(true);
                return baseSpeed * 0.7F;
            } else {
                // Normal ground speed - use walk animation, stop running
                dragon.setRunning(false);
                return baseSpeed * 0.5F;
            }
        }
    }

    // ===== RIDING MOVEMENT =====

    /**
     * Handle rider movement - called from travel() method
     */
    public void handleRiderMovement(Player player, Vec3 motion) {
        // Clear any AI navigation when being ridden
        if (dragon.getNavigation().getPath() != null) {
            dragon.getNavigation().stop();
        }

        if (dragon.isFlying()) {
            dragon.moveRelative(getRiddenSpeed(player), motion);
            Vec3 delta = dragon.getDeltaMovement();

            // Handle vertical movement from rider input
            if (dragon.isGoingUp()) {
                delta = delta.add(0, ASCEND_RATE, 0);
            } else if (dragon.isGoingDown()) {
                delta = delta.add(0, -DESCEND_RATE, 0);
            }

            dragon.move(MoverType.SELF, delta);
            // Less friction for more responsive flight
            dragon.setDeltaMovement(delta.scale(0.91D));
            dragon.calculateEntityAnimation(true);

            // While airborne and ridden, continuously clear fall distance
            player.fallDistance = 0.0F;
            dragon.fallDistance = 0.0F;
        }
    }

    // ===== RIDING SUPPORT =====
    
    public double getPassengersRidingOffset() {
        return (double) dragon.getBbHeight() * SEAT_BASE_FACTOR;
    }
    
    public void positionRider(@NotNull Entity passenger, Entity.@NotNull MoveFunction moveFunction) {
        if (!dragon.hasPassenger(passenger)) return;
        
        // Simple vanilla positioning - let the render layer handle bone positioning
        double offsetY = getPassengersRidingOffset() + SEAT_LIFT;
        double forward = SEAT_FORWARD;
        double side = SEAT_SIDE;
        double rad = Math.toRadians(dragon.yBodyRot);
        double dx = -Math.sin(rad) * forward + Math.cos(rad) * side;
        double dz =  Math.cos(rad) * forward + Math.sin(rad) * side;
        moveFunction.accept(passenger, dragon.getX() + dx, dragon.getY() + offsetY, dragon.getZ() + dz);
    }
    
    public @NotNull Vec3 getDismountLocationForPassenger(@NotNull LivingEntity passenger) {
        passenger.fallDistance = 0.0F;
        var level = dragon.level();
        Vec3 base = dragon.position();

        // Sample radial candidates around the dragon for a safe dismount
        double[] radii = new double[] { 2.5, 3.5, 1.8 };
        int[] angles = new int[] { 0, 30, -30, 60, -60, 90, -90, 150, -150, 180 };

        for (double r : radii) {
            for (int a : angles) {
                double rad = Math.toRadians(dragon.getYRot() + a);
                double cx = base.x + Math.cos(rad) * r;
                double cz = base.z + Math.sin(rad) * r;

                // Project down to find ground up to 6 blocks below current Y
                int startY = (int) Math.floor(base.y + 1.0);
                for (int dy = 0; dy <= 6; dy++) {
                    int y = startY - dy;
                    var pos = new net.minecraft.core.BlockPos((int) Math.floor(cx), y, (int) Math.floor(cz));
                    var below = pos.below();
                    var bsBelow = level.getBlockState(below);
                    var bsAt = level.getBlockState(pos);
                    boolean solidBelow = !bsBelow.isAir() && !bsBelow.getCollisionShape(level, below).isEmpty();
                    boolean spaceFree = bsAt.getCollisionShape(level, pos).isEmpty();
                    boolean fluidOk = bsAt.getFluidState().isEmpty();
                    if (solidBelow && spaceFree && fluidOk) {
                        // Found a safe spot; return precise center with a slight lift
                        return new Vec3(pos.getX() + 0.5, pos.getY() + 0.05, pos.getZ() + 0.5);
                    }
                }
            }
        }

        // Fallback: ahead of dragon
        Vec3 direction = dragon.getViewVector(1.0F);
        return base.add(direction.scale(2.0));
    }
    
    @Nullable 
    public LivingEntity getControllingPassenger() {
        Entity entity = dragon.getFirstPassenger();
        if (entity instanceof Player player && dragon.isTame() && dragon.isOwnedBy(player)) {
            return player;
        }
        return null;
    }
    
    /**
     * Forces the dragon to take off when being ridden. Called when player presses Space while on ground.
     */
    public void requestRiderTakeoff() {
        if (!dragon.isTame() || getRidingPlayer() == null || dragon.isFlying()) return;
        // Block takeoff if locked (e.g., during roar)
        if (dragon.isTakeoffLocked()) return;
        // Gentle cooldown after landing to prevent spam takeoff/land jitter
        long now = dragon.level().getGameTime();
        long lastLand = dragon.getLastLandingGameTime();
        if (lastLand != Long.MIN_VALUE && (now - lastLand) < 20L) { // ~1s cooldown
            return;
        }
        
        // Reset all flight states for a fresh takeoff
        dragon.timeFlying = 0;
        dragon.landingFlag = false;
        dragon.landingTimer = 0;
        
        // Initiate takeoff sequence
        dragon.setFlying(true);
        dragon.setTakeoff(true);
        dragon.setHovering(false);
        dragon.setLanding(false);
        // Keep takeoff active for a brief window so server flight logic applies upward force
        dragon.setRiderTakeoffTicks(30);
    }
}

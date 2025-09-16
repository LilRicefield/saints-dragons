package com.leon.saintsdragons.server.entity.ability.abilities.lightningdragon;

import com.leon.saintsdragons.server.entity.ability.DragonAbility;
import com.leon.saintsdragons.server.entity.ability.DragonAbilitySection;
import com.leon.saintsdragons.server.entity.ability.DragonAbilityType;
import com.leon.saintsdragons.server.entity.dragons.lightningdragon.LightningDragonEntity;
import static com.leon.saintsdragons.server.entity.ability.DragonAbilitySection.*;

/**
 * Hold-to-fire lightning beam ability.
 * No charge: starts immediately, remains active until interrupted.
 * Initial version: only toggles beaming state; damage/VFX added later.
 */
public class LightningDragonBeamAbility extends DragonAbility<LightningDragonEntity> {

    // Unified beam animation: 1s delay (startup), ~3.03s active beaming, no explicit recovery
    // 1s  = 20 ticks; 3.03s ≈ 61 ticks
    private static final DragonAbilitySection[] TRACK = new DragonAbilitySection[] {
            new AbilitySectionDuration(AbilitySectionType.STARTUP, 20),
            new AbilitySectionDuration(AbilitySectionType.ACTIVE, 61)
    };
    
    private boolean hasBeamFired = false; // Track if beam has been fired this activation

    public LightningDragonBeamAbility(DragonAbilityType<LightningDragonEntity, LightningDragonBeamAbility> type, LightningDragonEntity user) {
        super(type, user, TRACK, 0); // No cooldown; gated by input
    }

    @Override
    protected void beginSection(DragonAbilitySection section) {
        if (section == null) return;
        if (section.sectionType == AbilitySectionType.STARTUP) {
            // Reset beam fired flag; play unified animation once at start
            hasBeamFired = false;
            getUser().setBeaming(false);
            getUser().triggerAnim("action", "lightning_beam");
        } else if (section.sectionType == AbilitySectionType.ACTIVE) {
            // Enter beaming window; visuals/damage enabled during ACTIVE only
            getUser().setBeaming(true);
            // Initial tick damage alignment (optional single pulse at start)
            if (!hasBeamFired) {
                fireBeamOnce();
                hasBeamFired = true;
            }
        }
    }

    @Override
    protected void endSection(DragonAbilitySection section) {
        // When leaving ACTIVE (by interrupt/complete), clear beaming; unified anim handles visuals
        if (section != null && section.sectionType == AbilitySectionType.ACTIVE) {
            getUser().setBeaming(false);
        }
    }

    @Override
    public void interrupt() {
        // Ensure beaming flag cleared even if interrupted mid-active
        getUser().setBeaming(false);
        hasBeamFired = false; // Reset for next use
        super.interrupt();
    }

    @Override
    public void tickUsing() {
        var section = getCurrentSection();
        if (section == null || section.sectionType != AbilitySectionType.ACTIVE) return;

        LightningDragonEntity dragon = getUser();
        if (dragon.level().isClientSide) return; // server-side authority only

        // Update beam visual positions every tick
        updateBeamPositions(dragon);
        // Actively align body toward target while beaming so the whole dragon faces the enemy
        var tgt = dragon.getTarget();
        if (tgt != null && tgt.isAlive()) {
            double dx = tgt.getX() - dragon.getX();
            double dz = tgt.getZ() - dragon.getZ();
            float targetYaw = (float)(Math.atan2(dz, dx) * (180.0 / Math.PI)) - 90.0F;
            float currentYaw = dragon.getYRot();
            float yawErr = net.minecraft.util.Mth.degreesDifference(currentYaw, targetYaw);
            // Slightly larger deadzone and adaptive soft approach to reduce jitter
            if (Math.abs(yawErr) > 3.5f) {
                // Adaptive turn speed: faster for large errors, softer when close
                float base = 2.5f;
                float scale = 0.10f; // per-degree contribution
                float max = dragon.isFlying() ? 7.0f : 6.0f; // cap
                float step = net.minecraft.util.Mth.clamp(base + Math.abs(yawErr) * scale, base, max);
                float newYaw = net.minecraft.util.Mth.approachDegrees(currentYaw, targetYaw, step);
                dragon.setYRot(newYaw);
                dragon.yBodyRot = dragon.getYRot();
            }
        }
        
        // Deal continuous damage every tick while beam is active
        var start = dragon.getBeamStartPosition();
        var end = dragon.getBeamEndPosition();
        if (start != null && end != null) {
            damageAlongBeam(dragon, start, end);
        }
    }

    private void fireBeamOnce() {
        LightningDragonEntity dragon = getUser();
        updateBeamPositions(dragon);
        
        var start = dragon.getBeamStartPosition();
        var end = dragon.getBeamEndPosition();
        if (start != null && end != null) {
            damageAlongBeam(dragon, start, end);
        }
    }
    
    private void updateBeamPositions(LightningDragonEntity dragon) {
        // Compute mouth origin from head yaw/pitch each tick and sync to clients
        var start = dragon.computeHeadMouthOrigin(1.0f);
        dragon.setBeamStartPosition(start);

        // Aim preference: rider look (if mounted) -> target center -> head-based look
        net.minecraft.world.entity.Entity cp = dragon.getControllingPassenger();
        net.minecraft.world.phys.Vec3 aimDir;

        if (cp instanceof net.minecraft.world.entity.LivingEntity rider) {
            aimDir = rider.getLookAngle().normalize();
        } else {
            net.minecraft.world.entity.LivingEntity tgt = dragon.getTarget();
            if (tgt != null && tgt.isAlive()) {
                // Aim at target's mid/eye height from the muzzle
                var aimPoint = tgt.getEyePosition().add(0, -0.25, 0);
                aimDir = aimPoint.subtract(start).normalize();
            } else {
                // Fallback to dragon's head-based look (use head yaw for alignment)
                float yaw = dragon.yHeadRot;
                float pitch = dragon.getXRot();
                aimDir = net.minecraft.world.phys.Vec3.directionFromRotation(pitch, yaw).normalize();
            }
        }

        // Clamp aim to neck capability so beam cannot bend further than neck can
        aimDir = clampAimToNeck(dragon, aimDir);

        // Raycast along aim to determine endpoint
        final double MAX_DISTANCE = 32.0; // blocks
        var tentativeEnd = start.add(aimDir.scale(MAX_DISTANCE));

        var hit = dragon.level().clip(new net.minecraft.world.level.ClipContext(
                start,
                tentativeEnd,
                net.minecraft.world.level.ClipContext.Block.COLLIDER,
                net.minecraft.world.level.ClipContext.Fluid.NONE,
                dragon
        ));
        var end = hit.getType() != net.minecraft.world.phys.HitResult.Type.MISS ? hit.getLocation() : tentativeEnd;
        dragon.setBeamEndPosition(end);
    }

    private static net.minecraft.world.phys.Vec3 clampAimToNeck(LightningDragonEntity dragon, net.minecraft.world.phys.Vec3 desiredDir) {
        if (desiredDir.lengthSqr() < 1.0e-6) return desiredDir;
        desiredDir = desiredDir.normalize();

        float desiredYawDeg = (float)(Math.atan2(-desiredDir.x, desiredDir.z) * (180.0 / Math.PI));
        float desiredPitchDeg = (float)(-Math.atan2(desiredDir.y, Math.sqrt(desiredDir.x * desiredDir.x + desiredDir.z * desiredDir.z)) * (180.0 / Math.PI));

        float headYaw = dragon.yHeadRot;
        float headPitch = dragon.getXRot();

        float yawErrDeg = net.minecraft.util.Mth.degreesDifference(headYaw, desiredYawDeg);
        float pitchErrDeg = desiredPitchDeg - headPitch;

        float TOTAL_MAX_YAW_DEG = (float)Math.toDegrees(0.70f * (0.18f + 0.22f + 0.26f + 0.30f));
        float TOTAL_MAX_PITCH_DEG = (float)Math.toDegrees(0.90f * (0.18f + 0.22f + 0.26f + 0.30f));

        float clampedYawErr = net.minecraft.util.Mth.clamp(yawErrDeg, -TOTAL_MAX_YAW_DEG, TOTAL_MAX_YAW_DEG);
        float clampedPitchErr = net.minecraft.util.Mth.clamp(pitchErrDeg, -TOTAL_MAX_PITCH_DEG, TOTAL_MAX_PITCH_DEG);

        float finalYaw = headYaw + clampedYawErr;
        float finalPitch = headPitch + clampedPitchErr;
        return net.minecraft.world.phys.Vec3.directionFromRotation(finalPitch, finalYaw).normalize();
    }
    
    private void damageAlongBeam(LightningDragonEntity dragon, net.minecraft.world.phys.Vec3 start, net.minecraft.world.phys.Vec3 end) {
        if (!(dragon.level() instanceof net.minecraft.server.level.ServerLevel server)) return;

        final double STEP = 1.0;         // sample spacing
        final double RADIUS = 1.2;       // affect radius around beam core
        final float DAMAGE = 35.0f;       // Per-tick damage (35 damage per tick for 40 ticks = 1400 total damage OH MY GOODNESS)

        var delta = end.subtract(start);
        double len = delta.length();
        if (len < 0.0001) return;
        var dir = delta.scale(1.0 / len);

        java.util.HashSet<net.minecraft.world.entity.LivingEntity> hitThisBeam = new java.util.HashSet<>();

        for (double d = 0; d <= len; d += STEP) {
            var p = start.add(dir.scale(d));
            var aabb = new net.minecraft.world.phys.AABB(p, p).inflate(RADIUS);
            var list = server.getEntitiesOfClass(net.minecraft.world.entity.LivingEntity.class, aabb,
                    e -> e != dragon && e.isAlive() && e.attackable() && !isAllied(dragon, e));
            for (var le : list) {
                if (hitThisBeam.add(le)) {
                    le.hurt(dragon.level().damageSources().lightningBolt(), DAMAGE);
                    // Stronger knockback for single hit
                    var away = le.position().subtract(p).normalize();
                    le.push(away.x * 0.15, 0.08, away.z * 0.15);
                    // no-op beyond damage and push
                }
            }
        }
    }

    private boolean isAllied(LightningDragonEntity dragon, net.minecraft.world.entity.Entity other) {
        if (other instanceof LightningDragonEntity od) {
            return dragon.isTame() && od.isTame() && dragon.getOwner() != null && dragon.getOwner().equals(od.getOwner());
        }
        if (other instanceof net.minecraft.world.entity.LivingEntity le) {
            if (dragon.isTame() && le.equals(dragon.getOwner())) return true;
            return dragon.isAlliedTo(le);
        }
        return false;
    }
}

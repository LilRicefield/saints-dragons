package com.leon.saintsdragons.server.entity.ability.abilities.raevyx;

import com.leon.saintsdragons.server.entity.ability.DragonAbility;
import com.leon.saintsdragons.server.entity.ability.DragonAbilitySection;
import com.leon.saintsdragons.server.entity.ability.DragonAbilityType;
import com.leon.saintsdragons.server.entity.dragons.raevyx.Raevyx;
import static com.leon.saintsdragons.server.entity.ability.DragonAbilitySection.*;

/**
 * Hold-to-fire lightning beam ability.
 * No charge: starts immediately, remains active until interrupted.
 * Initial version: only toggles beaming state; damage/VFX added later.
 */
public class RaevyxBeamAbility extends DragonAbility<Raevyx> {

    // Beam timeline: 1s startup (20 ticks) then 400 ticks of active beaming.
    // Separate start/loop/stop animations handle the visuals.
    private static final DragonAbilitySection[] TRACK = new DragonAbilitySection[] {
            new AbilitySectionDuration(AbilitySectionType.STARTUP, 20),
            new AbilitySectionDuration(AbilitySectionType.ACTIVE, 400)
    };
    
    private boolean hasBeamFired = false; // Track if beam has been fired this activation
    private boolean beamStartPlayed = false;
    private boolean beamLoopActive = false;

    public RaevyxBeamAbility(DragonAbilityType<Raevyx, RaevyxBeamAbility> type, Raevyx user) {
        super(type, user, TRACK, 0); // No cooldown; gated by input
    }

    @Override
    protected void beginSection(DragonAbilitySection section) {
        if (section == null) return;

        if (section.sectionType == AbilitySectionType.STARTUP) {
            // Reset state and kick off the beam start animation
            hasBeamFired = false;
            beamLoopActive = false;
            beamStartPlayed = true;
            Raevyx wyvern = getUser();
            wyvern.setBeaming(false);
            wyvern.triggerAnim("action", "lightning_beam_start");
        } else if (section.sectionType == AbilitySectionType.ACTIVE) {
            // Enter beaming window; visuals/damage enabled during ACTIVE only
            Raevyx wyvern = getUser();
            wyvern.setBeaming(true);
            wyvern.triggerAnim("action", "lightning_beaming");
            beamLoopActive = true;
            // Initial tick damage alignment (optional single pulse at start)
            if (!hasBeamFired) {
                fireBeamOnce();
                hasBeamFired = true;
            }
        }
    }

    @Override
    protected void endSection(DragonAbilitySection section) {
        if (section == null) {
            return;
        }

        if (section.sectionType == AbilitySectionType.ACTIVE) {
            Raevyx wyvern = getUser();
            wyvern.setBeaming(false);
            triggerBeamStop(wyvern);
            hasBeamFired = false;
        }
    }

    @Override
    public void interrupt() {
        // Ensure beaming visuals stop even if interrupted mid-startup or active
        Raevyx wyvern = getUser();
        wyvern.setBeaming(false);
        triggerBeamStop(wyvern);
        hasBeamFired = false; // Reset for next use
        super.interrupt();
    }

    @Override
    public void tickUsing() {
        var section = getCurrentSection();
        if (section == null || section.sectionType != AbilitySectionType.ACTIVE) return;

        Raevyx wyvern = getUser();
        if (wyvern.level().isClientSide) return; // server-side authority only

        // Update beam visual positions every tick
        updateBeamPositions(wyvern);
        // Actively align body toward target while beaming so the whole wyvern faces the enemy
        var tgt = wyvern.getTarget();
        if (tgt != null && tgt.isAlive()) {
            double dx = tgt.getX() - wyvern.getX();
            double dz = tgt.getZ() - wyvern.getZ();
            float targetYaw = (float)(Math.atan2(dz, dx) * (180.0 / Math.PI)) - 90.0F;
            float currentYaw = wyvern.getYRot();
            float yawErr = net.minecraft.util.Mth.degreesDifference(currentYaw, targetYaw);
            // Slightly larger deadzone and adaptive soft approach to reduce jitter
            if (Math.abs(yawErr) > 3.5f) {
                // Adaptive turn speed: faster for large errors, softer when close
                float base = 2.5f;
                float scale = 0.10f; // per-degree contribution
                float max = wyvern.isFlying() ? 7.0f : 6.0f; // cap
                float step = net.minecraft.util.Mth.clamp(base + Math.abs(yawErr) * scale, base, max);
                float newYaw = net.minecraft.util.Mth.approachDegrees(currentYaw, targetYaw, step);
                wyvern.setYRot(newYaw);
                wyvern.yBodyRot = wyvern.getYRot();
            }
        }
        
        // Deal continuous damage every tick while beam is active
        var start = wyvern.getBeamStartPosition();
        var end = wyvern.getBeamEndPosition();
        if (start != null && end != null) {
            damageAlongBeam(wyvern, start, end);
        }
    }

    private void triggerBeamStop(Raevyx wyvern) {
        if (beamLoopActive || beamStartPlayed) {
            wyvern.triggerAnim("action", "lightning_beam_stop");
        }
        beamLoopActive = false;
        beamStartPlayed = false;
    }

    private void fireBeamOnce() {
        Raevyx wyvern = getUser();
        updateBeamPositions(wyvern);
        
        var start = wyvern.getBeamStartPosition();
        var end = wyvern.getBeamEndPosition();
        if (start != null && end != null) {
            damageAlongBeam(wyvern, start, end);
        }
    }
    
    private void updateBeamPositions(Raevyx wyvern) {
        // Compute mouth origin from head yaw/pitch each tick and sync to clients
        var start = wyvern.computeHeadMouthOrigin(1.0f);
        wyvern.setBeamStartPosition(start);

        // Reuse the entity's beaming aim direction so visuals, neck, and damage align.
        net.minecraft.world.phys.Vec3 aimDir = wyvern.refreshBeamAimDirection(start, true);
        if (aimDir == null) {
            aimDir = net.minecraft.world.phys.Vec3.directionFromRotation(wyvern.getXRot(), wyvern.yHeadRot).normalize();
        }

        // Raycast along aim to determine endpoint
        final double MAX_DISTANCE = 128.0; // blocks
        var tentativeEnd = start.add(aimDir.scale(MAX_DISTANCE));

        var hit = wyvern.level().clip(new net.minecraft.world.level.ClipContext(
                start,
                tentativeEnd,
                net.minecraft.world.level.ClipContext.Block.COLLIDER,
                net.minecraft.world.level.ClipContext.Fluid.NONE,
                wyvern
        ));
        var end = hit.getType() != net.minecraft.world.phys.HitResult.Type.MISS ? hit.getLocation() : tentativeEnd;
        wyvern.setBeamEndPosition(end);
    }

    private void damageAlongBeam(Raevyx wyvern, net.minecraft.world.phys.Vec3 start, net.minecraft.world.phys.Vec3 end) {
        if (!(wyvern.level() instanceof net.minecraft.server.level.ServerLevel server)) return;

        final double STEP = 1.0;         // sample spacing
        final double BASE_RADIUS = 1.2;  // base affect radius around beam core
        final float BASE_DAMAGE = 35.0f;  // base per-tick damage
        
        // Apply water conductivity bonuses
        var conductivity = wyvern.getConductivityState();
        final double RADIUS = BASE_RADIUS * conductivity.rangeMultiplier();
        final float DAMAGE = BASE_DAMAGE * conductivity.damageMultiplier() * wyvern.getDamageMultiplier();

        var delta = end.subtract(start);
        double len = delta.length();
        if (len < 0.0001) return;
        var dir = delta.scale(1.0 / len);

        java.util.HashSet<net.minecraft.world.entity.LivingEntity> hitThisBeam = new java.util.HashSet<>();

        for (double d = 0; d <= len; d += STEP) {
            var p = start.add(dir.scale(d));
            var aabb = new net.minecraft.world.phys.AABB(p, p).inflate(RADIUS);
            var list = server.getEntitiesOfClass(net.minecraft.world.entity.LivingEntity.class, aabb,
                    e -> e != wyvern && e.isAlive() && e.attackable() && !isAllied(wyvern, e));
            for (var le : list) {
                if (hitThisBeam.add(le)) {
                    le.hurt(wyvern.level().damageSources().lightningBolt(), DAMAGE);
                    // Stronger knockback for single hit
                    var away = le.position().subtract(p).normalize();
                    le.push(away.x * 0.15, 0.08, away.z * 0.15);
                    // no-op beyond damage and push
                }
            }
        }
    }

    private boolean isAllied(Raevyx wyvern, net.minecraft.world.entity.Entity other) {
        // Use the comprehensive ally system from DragonEntity
        return wyvern.isAlly(other);
    }
}

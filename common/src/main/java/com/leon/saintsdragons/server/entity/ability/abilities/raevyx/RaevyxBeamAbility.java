package com.leon.saintsdragons.server.entity.ability.abilities.raevyx;

import com.leon.saintsdragons.common.config.dragon.DragonAttributeConfigLoader;
import com.leon.saintsdragons.common.registry.ModSounds;
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

    // Beam timeline: 1s startup (20 ticks) then variable active duration.
    // Rider-controlled: 400 ticks (~20 seconds)
    // AI-controlled: 80 ticks (4 seconds)
    private static final DragonAbilitySection[] RIDER_TRACK = new DragonAbilitySection[] {
            new AbilitySectionDuration(AbilitySectionType.STARTUP, 20),
            new AbilitySectionDuration(AbilitySectionType.ACTIVE, 400)
    };
    private static final DragonAbilitySection[] AI_TRACK = new DragonAbilitySection[] {
            new AbilitySectionDuration(AbilitySectionType.STARTUP, 20),
            new AbilitySectionDuration(AbilitySectionType.ACTIVE, 80) // 4 seconds for AI
    };
    private static final double MAX_BEAM_RANGE = 64;
    private static final float DEFAULT_BEAM_DAMAGE = 20.0f; // Reduced from 35.0f for balance

    // Beam energy system constants
    private static final float ENERGY_COST_PER_TICK = 0.014f; // Depletes in ~71 ticks (3.55 seconds) when ridden
    private static final float MIN_ENERGY_TO_START = 0.01f; // Can start beam with any remaining energy

    private boolean hasBeamFired = false; // Track if beam has been fired this activation
    private boolean beamStartPlayed = false;
    private boolean beamLoopActive = false;

    public RaevyxBeamAbility(DragonAbilityType<Raevyx, RaevyxBeamAbility> type, Raevyx user) {
        // Choose track based on whether user has a controlling passenger
        super(type, user, user.getControllingPassenger() != null ? RIDER_TRACK : AI_TRACK, 0);
    }

    @Override
    protected void beginSection(DragonAbilitySection section) {
        if (section == null) return;

        if (section.sectionType == AbilitySectionType.STARTUP) {
            Raevyx wyvern = getUser();

            // Check if beam can be used (has energy AND not locked from depletion)
            if (!wyvern.canUseBeam()) {
                interrupt();
                return;
            }

            // Reset state and kick off the beam start animation
            hasBeamFired = false;
            beamLoopActive = false;
            beamStartPlayed = true;
            wyvern.setBeamGlowActive(true);
            wyvern.setBeaming(false);
            wyvern.triggerAnim("action", "lightning_beam_start");
            if (!wyvern.level().isClientSide) {
                float pitch = 0.9f + wyvern.getRandom().nextFloat() * 0.2f;
                wyvern.getSoundHandler().playMovingEntitySound(ModSounds.RAEVYX_LIGHTNING_BEAM_START.get(), 1.8f, pitch, 28);
            }
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
            wyvern.setBeamGlowActive(false);
            wyvern.clearBeamPath();
            triggerBeamStop(wyvern);
            hasBeamFired = false;
        }
    }

    @Override
    public void interrupt() {
        // Ensure beaming visuals stop even if interrupted mid-startup or active
        Raevyx wyvern = getUser();
        wyvern.setBeaming(false);
        wyvern.setBeamGlowActive(false);
        wyvern.clearBeamPath();
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

        // Consume beam energy each tick
        float energyDrain = (float) DragonAttributeConfigLoader.getInstance()
                .getConfig(DragonAttributeConfigLoader.RAEVYX_ID)
                .extraDouble("beam_drain_per_tick", ENERGY_COST_PER_TICK);
        energyDrain = Math.max(0.0f, energyDrain);
        if (energyDrain > 0.0f) {
            wyvern.consumeBeamEnergy(energyDrain);
        }

        // Interrupt if out of energy
        if (!wyvern.hasBeamEnergy()) {
            // Set depletion lock - must fully recharge before using again
            wyvern.setBeamDepleted(true);
            interrupt();
            return;
        }

        // Check if target is still valid - interrupt beam if not

        var tgt = wyvern.getTarget();
        if (!wyvern.isTame() && wyvern.getControllingPassenger() == null) {
            if (!isValidTarget(wyvern.getTarget())) {
                interrupt();
                return;
            }
        }

        BeamPath path = computeBeamPath(wyvern);
        if (path == null) {
            return;
        }

        // Actively align body toward target while beaming so the whole wyvern faces the enemy
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
        damageAlongBeam(wyvern, path.origin(), path.impact());
    }

    private void triggerBeamStop(Raevyx wyvern) {
        if (beamLoopActive || beamStartPlayed) {
            wyvern.triggerAnim("action", "lightning_beam_stop");
            if (!wyvern.level().isClientSide) {
                float pitch = 0.95f + wyvern.getRandom().nextFloat() * 0.15f;
                wyvern.getSoundHandler().playMovingEntitySound(ModSounds.RAEVYX_LIGHTNING_BEAM_STOP.get(), 1.6f, pitch, 34);
            }
        }
        beamLoopActive = false;
        beamStartPlayed = false;
    }

    private void fireBeamOnce() {
        Raevyx wyvern = getUser();
        BeamPath path = computeBeamPath(wyvern);
        if (path != null) {
            damageAlongBeam(wyvern, path.origin(), path.impact());
        }
    }
    
    private BeamPath computeBeamPath(Raevyx wyvern) {
        net.minecraft.world.phys.Vec3 origin = wyvern.getBeamStartAnchor(1.0f);
        if (origin == null) {
            wyvern.clearBeamPath();
            return null;
        }

        net.minecraft.world.phys.Vec3 aimDir = wyvern.refreshBeamAimDirection(origin, false);
        if (aimDir == null || aimDir.lengthSqr() < 1.0E-6) {
            wyvern.clearBeamPath();
            return null;
        }

        net.minecraft.world.phys.Vec3 impact = traceBeamImpact(wyvern, origin, aimDir);
        wyvern.syncBeamPath(origin, impact);
        return new BeamPath(origin, impact);
    }

    private net.minecraft.world.phys.Vec3 traceBeamImpact(Raevyx wyvern,
                                                         net.minecraft.world.phys.Vec3 origin,
                                                         net.minecraft.world.phys.Vec3 aimDir) {
        var reach = origin.add(aimDir.scale(MAX_BEAM_RANGE));
        var context = new net.minecraft.world.level.ClipContext(
                origin,
                reach,
                net.minecraft.world.level.ClipContext.Block.COLLIDER,
                net.minecraft.world.level.ClipContext.Fluid.NONE,
                wyvern
        );
        var hit = wyvern.level().clip(context);
        if (hit == null || hit.getType() == net.minecraft.world.phys.HitResult.Type.MISS) {
            return reach;
        }
        return hit.getLocation();
    }

    private void damageAlongBeam(Raevyx wyvern, net.minecraft.world.phys.Vec3 start, net.minecraft.world.phys.Vec3 end) {
        if (!(wyvern.level() instanceof net.minecraft.server.level.ServerLevel server)) return;

        final double BASE_RADIUS = 1.2;  // base affect radius around beam core
        final float configuredBaseDamage = (float) DragonAttributeConfigLoader.getInstance()
                .getConfig(DragonAttributeConfigLoader.RAEVYX_ID)
                .abilityDamage("lightning_beam", DEFAULT_BEAM_DAMAGE);

        // Apply water conductivity bonuses
        var conductivity = wyvern.getConductivityState();
        final double RADIUS = BASE_RADIUS * conductivity.rangeMultiplier();
        final float DAMAGE = configuredBaseDamage * conductivity.damageMultiplier() * wyvern.getDamageMultiplier();

        // Create a bounding box that encompasses the entire beam path, inflated by radius
        var beamAABB = new net.minecraft.world.phys.AABB(start, end).inflate(RADIUS);

        // Get all potential targets in the beam's area
        var potentialTargets = server.getEntitiesOfClass(net.minecraft.world.entity.LivingEntity.class, beamAABB,
                e -> e != wyvern && wyvern.isTargetValid(e) && e.attackable() && !isAllied(wyvern, e));

        for (var target : potentialTargets) {
            // Check if the beam actually intersects the entity's bounding box (inflated by radius)
            var targetAABB = target.getBoundingBox().inflate(RADIUS);
            var hit = targetAABB.clip(start, end);
            boolean pointBlankOverlap = targetAABB.contains(start) || targetAABB.contains(end);

            if (hit.isPresent() || pointBlankOverlap) {
                var hitPos = hit.orElse(start);
                target.hurt(resolveBeamDamageSource(wyvern, target), DAMAGE);
                // Knockback away from hit position
                var away = target.position().subtract(hitPos).normalize();
                target.push(away.x * 0.15, 0.08, away.z * 0.15);
            }
        }
    }

    private boolean isAllied(Raevyx wyvern, net.minecraft.world.entity.Entity other) {
        // Use the comprehensive ally system from DragonEntity
        return wyvern.isAlly(other);
    }

    /**
     * Checks if the target is valid for continued beaming.
     * Beam stops if target is null, dead, removed, or in creative mode.
     */
    private boolean isValidTarget(net.minecraft.world.entity.LivingEntity target) {
        if (target == null) return false;
        if (!getUser().isTargetValid(target)) return false;
        if (target.isRemoved()) return false;

        // Stop beaming if target switches to creative mode
        if (target instanceof net.minecraft.world.entity.player.Player player) {
            if (player.isCreative() || player.isSpectator()) {
                return false;
            }
        }

        return true;
    }

    private net.minecraft.world.damagesource.DamageSource resolveBeamDamageSource(Raevyx wyvern, net.minecraft.world.entity.LivingEntity target) {
        if (target instanceof com.leon.saintsdragons.server.entity.base.DragonEntity) {
            // Dragon-vs-dragon beam should bypass lightning-tag immunities and use direct attacker damage.
            return wyvern.level().damageSources().mobAttack(wyvern);
        }
        if (isIafLightningDragon(target)) {
            return wyvern.level().damageSources().mobAttack(wyvern);
        }
        return wyvern.level().damageSources().lightningBolt();
    }

    private boolean isIafLightningDragon(net.minecraft.world.entity.LivingEntity entity) {
        String className = entity.getClass().getName();
        if ("com.github.alexthe666.iceandfire.entity.EntityLightningDragon".equals(className)
                || "com.iafenvoy.iceandfire.entity.LightningDragonEntity".equals(className)) {
            return true;
        }

        // Fallback for forks/remapped classes: detect by entity type id.
        net.minecraft.resources.ResourceLocation id =
                net.minecraft.core.registries.BuiltInRegistries.ENTITY_TYPE.getKey(entity.getType());
        if (id == null) {
            return false;
        }

        String namespace = id.getNamespace();
        String path = id.getPath();
        boolean iafNamespace = "iceandfire".equals(namespace) || "ice_and_fire".equals(namespace);
        boolean looksLikeLightningDragon = path != null
                && path.contains("lightning")
                && path.contains("dragon");
        return iafNamespace && looksLikeLightningDragon;
    }

    private record BeamPath(net.minecraft.world.phys.Vec3 origin, net.minecraft.world.phys.Vec3 impact) {}
}

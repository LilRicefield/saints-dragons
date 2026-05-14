package com.leon.saintsdragons.server.entity.ability.abilities.raevyx;

import com.leon.saintsdragons.common.config.dragon.DragonAttributeConfigLoader;
import com.leon.saintsdragons.common.registry.ModSounds;
import com.leon.saintsdragons.server.entity.ability.DragonAbility;
import com.leon.saintsdragons.server.entity.ability.DragonAbilitySection;
import com.leon.saintsdragons.server.entity.ability.DragonAbilityType;
import com.leon.saintsdragons.server.entity.base.DragonEntity;
import com.leon.saintsdragons.server.entity.ability.DragonAbilitySection.*;
import com.leon.saintsdragons.server.entity.dragons.raevyx.Raevyx;
import com.leon.saintsdragons.server.entity.dragons.raevyx.handlers.RaevyxAnimationHandler;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

public class RaevyxBeamAbility extends DragonAbility<Raevyx> {
    private static final double AI_TARGET_HIT_RADIUS = 0.55D;
    private static final double RIDER_BEAM_RADIUS = 1.2D;
    private static final double AI_BEAM_RADIUS = 0.75D;
    private static final DragonAbilitySection[] RIDER_TRACK = new DragonAbilitySection[] {
            new AbilitySectionDuration(AbilitySectionType.STARTUP, 20),
            new AbilitySectionDuration(AbilitySectionType.ACTIVE, 400)
    };
    private static final DragonAbilitySection[] AI_TRACK = new DragonAbilitySection[] {
            new AbilitySectionDuration(AbilitySectionType.STARTUP, 20),
            new AbilitySectionDuration(AbilitySectionType.ACTIVE, 80)
    };
    private static final float DEFAULT_BEAM_DAMAGE = 20.0f;
    private static final float ENERGY_COST_PER_TICK = 0.014f;
    private boolean hasBeamFired = false;
    private boolean beamStartPlayed = false;
    private boolean beamLoopActive = false;
    public RaevyxBeamAbility(DragonAbilityType<Raevyx, RaevyxBeamAbility> type, Raevyx user) {
        super(type, user, user.getControllingPassenger() != null ? RIDER_TRACK : AI_TRACK, 0);
    }

    @Override
    protected void beginSection(DragonAbilitySection section) {
        if (section == null) return;

        if (section.sectionType == AbilitySectionType.STARTUP) {
            Raevyx wyvern = getUser();
            if (!wyvern.canUseBeam()) {
                interrupt();
                return;
            }

            hasBeamFired = false;
            beamLoopActive = false;
            beamStartPlayed = true;
            wyvern.setBeamGlowActive(true);
            wyvern.setBeaming(false);
            wyvern.triggerAnim(RaevyxAnimationHandler.FAST_ACTION_CONTROLLER, "lightning_beam_start");
            if (!wyvern.level().isClientSide) {
                float pitch = 0.9f + wyvern.getRandom().nextFloat() * 0.2f;
                wyvern.getSoundHandler().playMovingEntitySound(ModSounds.RAEVYX_LIGHTNING_BEAM_START.get(), 1.8f, pitch, 28);
            }
        } else if (section.sectionType == AbilitySectionType.ACTIVE) {
            Raevyx wyvern = getUser();
            wyvern.setBeaming(true);
            wyvern.triggerAnim(RaevyxAnimationHandler.FAST_ACTION_CONTROLLER, "lightning_beaming");
            beamLoopActive = true;
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
        Raevyx wyvern = getUser();
        wyvern.setBeaming(false);
        wyvern.setBeamGlowActive(false);
        wyvern.clearBeamPath();
        triggerBeamStop(wyvern);
        hasBeamFired = false;
        super.interrupt();
    }

    @Override
    public void tickUsing() {
        var section = getCurrentSection();
        if (section == null || section.sectionType != AbilitySectionType.ACTIVE) return;

        Raevyx wyvern = getUser();
        if (wyvern.level().isClientSide) return;
        float energyDrain = (float) DragonAttributeConfigLoader.getInstance()
                .getConfig(DragonAttributeConfigLoader.RAEVYX_ID)
                .extraDouble("beam_drain_per_tick", ENERGY_COST_PER_TICK);
        energyDrain = Math.max(0.0f, energyDrain);
        if (energyDrain > 0.0f) {
            wyvern.consumeBeamEnergy(energyDrain);
        }
        if (!wyvern.hasBeamEnergy()) {
            wyvern.setBeamDepleted(true);
            interrupt();
            return;
        }

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
        damageAlongBeam(wyvern, path.origin(), path.impact());
    }

    private void triggerBeamStop(Raevyx wyvern) {
        if (beamLoopActive || beamStartPlayed) {
            wyvern.triggerAnim(RaevyxAnimationHandler.FAST_ACTION_CONTROLLER, "lightning_beam_stop");
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
        if (!wyvern.updateBeamPathFromAim()) {
            return null;
        }

        Vec3 origin = wyvern.getBeamStartPosition();
        Vec3 impact = wyvern.getBeamEndPosition();
        if (origin == null || impact == null) {
            return null;
        }
        return new BeamPath(origin, impact);
    }

    private void damageAlongBeam(Raevyx wyvern, Vec3 start, Vec3 end) {
        if (!(wyvern.level() instanceof ServerLevel server)) return;

        boolean riderControlled = wyvern.getControllingPassenger() != null;
        final float configuredBaseDamage = (float) DragonAttributeConfigLoader.getInstance()
                .getConfig(DragonAttributeConfigLoader.RAEVYX_ID)
                .abilityDamage("lightning_beam", DEFAULT_BEAM_DAMAGE);
        final double radiusBase = riderControlled ? RIDER_BEAM_RADIUS : AI_BEAM_RADIUS;
        final double RADIUS = radiusBase;
        final float DAMAGE = configuredBaseDamage * wyvern.getDamageMultiplier();

        if (!riderControlled) {
            damageAiBeamTargetOnly(wyvern, start, end, DAMAGE, RADIUS);
            return;
        }

        var beamAABB = new AABB(start, end).inflate(RADIUS);
        var potentialTargets = server.getEntitiesOfClass(LivingEntity.class, beamAABB, e -> e != wyvern && wyvern.isTargetValid(e) && e.attackable() && !isAllied(wyvern, e));
        for (var target : potentialTargets) {
            var targetAABB = target.getBoundingBox().inflate(RADIUS);
            var hit = targetAABB.clip(start, end);
            boolean pointBlankOverlap = targetAABB.contains(start) || targetAABB.contains(end);

            if (hit.isPresent() || pointBlankOverlap) {
                var hitPos = hit.orElse(start);
                target.hurt(resolveBeamDamageSource(wyvern, target), DAMAGE);
                var away = target.position().subtract(hitPos).normalize();
                target.push(away.x * 0.15, 0.08, away.z * 0.15);
            }
        }
    }

    private void damageAiBeamTargetOnly(Raevyx wyvern, Vec3 start, Vec3 end, float damage, double radius) {
       LivingEntity target = wyvern.getTarget();
        if (!isValidTarget(target) || !wyvern.isTargetValid(target) || isAllied(wyvern, target)) {
            return;
        }

        var targetAABB = target.getBoundingBox().inflate(Math.min(radius, AI_TARGET_HIT_RADIUS));
        var hit = targetAABB.clip(start, end);
        boolean pointBlankOverlap = targetAABB.contains(start) || targetAABB.contains(end);
        if (hit.isEmpty() && !pointBlankOverlap) {
            return;
        }

        var hitPos = hit.orElse(start);
        target.hurt(resolveBeamDamageSource(wyvern, target), damage);
        var away = target.position().subtract(hitPos).normalize();
        target.push(away.x * 0.15, 0.08, away.z * 0.15);
    }

    private boolean isAllied(Raevyx wyvern, Entity other) {
        return wyvern.isAlly(other);
    }

    private boolean isValidTarget(LivingEntity target) {
        if (target == null) return false;
        if (!getUser().isTargetValid(target)) return false;
        if (target.isRemoved()) return false;
        if (target instanceof Player player) {
            if (player.isCreative() || player.isSpectator()) {
                return false;
            }
        }

        return true;
    }

    private DamageSource resolveBeamDamageSource(Raevyx wyvern, LivingEntity target) {
        if (target instanceof DragonEntity) {
            return wyvern.level().damageSources().mobAttack(wyvern);
        }
        if (isIafLightningDragon(target)) {
            return wyvern.level().damageSources().mobAttack(wyvern);
        }
        return wyvern.level().damageSources().lightningBolt();
    }

    private boolean isIafLightningDragon(LivingEntity entity) {
        String className = entity.getClass().getName();
        if ("com.github.alexthe666.iceandfire.entity.EntityLightningDragon".equals(className)
                || "com.iafenvoy.iceandfire.entity.LightningDragonEntity".equals(className)) {
            return true;
        }
        ResourceLocation id = BuiltInRegistries.ENTITY_TYPE.getKey(entity.getType());
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

    private record BeamPath(Vec3 origin, Vec3 impact) {}
}

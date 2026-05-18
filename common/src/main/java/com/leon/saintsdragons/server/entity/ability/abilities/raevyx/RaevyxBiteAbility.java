package com.leon.saintsdragons.server.entity.ability.abilities.raevyx;

import com.leon.saintsdragons.common.config.dragon.DragonAttributeConfigLoader;
import com.leon.saintsdragons.common.registry.ModSounds;
import com.leon.saintsdragons.server.ai.goals.base.DragonTargetingHelper;
import com.leon.saintsdragons.server.entity.effect.raevyx.RaevyxGroundRendTrailEntity;
import com.leon.saintsdragons.server.entity.dragons.raevyx.Raevyx;
import com.leon.saintsdragons.server.entity.dragons.raevyx.handlers.RaevyxAnimationHandler;
import com.leon.saintsdragons.server.entity.dragons.util.DragonElementalImmunity;
import com.leon.saintsdragons.server.entity.ability.DragonAbility;
import com.leon.saintsdragons.server.entity.ability.DragonAbilitySection;
import com.leon.saintsdragons.server.entity.ability.DragonAbilityType;
import com.leon.saintsdragons.server.entity.ability.DragonMeleeGeometry;
import com.leon.saintsdragons.server.entity.ability.debug.DragonAbilityDebug;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.projectile.ProjectileUtil;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import java.util.*;

import com.leon.saintsdragons.server.entity.ability.DragonAbilitySection.*;

public class RaevyxBiteAbility extends DragonAbility<Raevyx> {
    private static final float BASE_DAMAGE = 15.0f;
    private static final double RANGE = 6.0;
    private static final double HITBOX_HALF_WIDTH = 3.75;
    private static final double HITBOX_HALF_HEIGHT = 3.4;
    private static final double HITBOX_FORWARD_OFFSET = 2.0;
    private static final int DEBUG_COLOR = 0x33D1FF;
    private static final int DEBUG_TICKS = 20;
    private static final float CHAIN_DAMAGE_BASE = 10.0f;
    private static final double CHAIN_RADIUS = 7.0;
    private static final int CHAIN_JUMPS = 5;
    private static final float CHAIN_FALLOFF = 0.75f;
    private static final int CHAIN_VISUAL_LIFETIME = 5;

    private static final DragonAbilitySection[] TRACK = new DragonAbilitySection[] {
            new AbilitySectionDuration(AbilitySectionType.STARTUP, 3),
            new AbilitySectionDuration(AbilitySectionType.ACTIVE, 2),
            new AbilitySectionDuration(AbilitySectionType.RECOVERY, 3)
    };

    private boolean didHitThisActive = false;

    public RaevyxBiteAbility(DragonAbilityType<Raevyx, RaevyxBiteAbility> type, Raevyx user) {
        super(type, user, TRACK, 3);
    }

    @Override
    protected void beginSection(DragonAbilitySection section) {
        if (section == null) return;
        if (section.sectionType == AbilitySectionType.STARTUP) {
            getUser().triggerAnim(RaevyxAnimationHandler.FAST_ACTION_CONTROLLER, "lightning_bite");
            if (!getUser().level().isClientSide) {
                float pitch = 0.95f + getUser().getRandom().nextFloat() * 0.10f;
                getUser().getSoundHandler().playMovingEntitySound(ModSounds.RAEVYX_BITE.get(), 1.0f, pitch, 50);
            }
            didHitThisActive = false;
        }
    }

    @Override
    public void tickUsing() {
        DragonAbilitySection section = getCurrentSection();
        if (section == null) return;


        if (section.sectionType == AbilitySectionType.ACTIVE && !didHitThisActive) {
            LivingEntity primary = findPrimaryTarget();
            boolean ridden = getUser().getControllingPassenger() != null;
            if (primary == null && !ridden) {
                LivingEntity t = getUser().getTarget();
                if (t != null && t.isAlive()) {
                    double d = t.distanceTo(getUser());
                    if (d <= 5.2) {
                        primary = t;
                    }
                }
            }
            if (primary == null && !ridden) {
                primary = raycastTargetAlongMouth(ridden ? 7.5 : 5.5, ridden ? 2.0 : 1.0);
            }
            if (primary != null) {
                bitePrimary(primary);
                if (!DragonTargetingHelper.isBiteOnlyPreyTarget(primary)) {
                    chainFrom(primary);
                }
            }
            didHitThisActive = true;
        }
    }

    private LivingEntity findPrimaryTarget() {
        Raevyx wyvern = getUser();
        boolean ridden = wyvern.getControllingPassenger() != null;

        if (!ridden) {
            LivingEntity target = wyvern.getTarget();
            if (DragonMeleeGeometry.isDirectAiTargetValid(wyvern, target, 1.5D)) {
                sendDebugBox(wyvern);
                return target;
            }
        }

        List<LivingEntity> candidates = DragonMeleeGeometry.findBodySweepTargets(
                wyvern,
                RANGE,
                HITBOX_HALF_WIDTH,
                HITBOX_HALF_HEIGHT,
                HITBOX_FORWARD_OFFSET,
                entity -> !isAllied(wyvern, entity)
        );
        sendDebugBox(wyvern);

        return candidates.isEmpty() ? null : candidates.get(0);
    }

    private void sendDebugBox(Raevyx wyvern) {
        if (wyvern.level().isClientSide) {
            return;
        }
        DragonMeleeGeometry.ForwardAttack attack = DragonMeleeGeometry.bodyForwardAttack(wyvern).offset(HITBOX_FORWARD_OFFSET);
        DragonAbilityDebug.sendBox(wyvern, attack.sweep(RANGE, HITBOX_HALF_WIDTH, HITBOX_HALF_HEIGHT), DEBUG_COLOR, DEBUG_TICKS);
    }

    private void bitePrimary(LivingEntity primary) {
        Raevyx wyvern = getUser();
        DamageSource src = wyvern.level().damageSources().mobAttack(wyvern);
        float mult = wyvern.getDamageMultiplier() * wyvern.getHungerMeleeDamageMultiplier();
        primary.hurt(src, resolveBiteDamage() * mult);
        wyvern.noteAggroFrom(primary);
    }

    private float resolveBiteDamage() {
        return (float) DragonAttributeConfigLoader.getInstance()
                .getConfig(DragonAttributeConfigLoader.RAEVYX_ID)
                .abilityDamage("bite", BASE_DAMAGE);
    }

    private void chainFrom(LivingEntity start) {
        Raevyx wyvern = getUser();
        Set<LivingEntity> hit = new HashSet<>();
        hit.add(start);

        LivingEntity current = start;
        float damage = CHAIN_DAMAGE_BASE;

        for (int i = 0; i < CHAIN_JUMPS; i++) {
            LivingEntity next = findNearestChainTarget(current, hit);
            if (next == null) break;

            float mult = wyvern.getDamageMultiplier();
            DamageSource chainSource = next instanceof com.leon.saintsdragons.server.entity.base.DragonEntity
                    ? wyvern.level().damageSources().mobAttack(wyvern)
                    : wyvern.level().damageSources().lightningBolt();
            if (!DragonElementalImmunity.isElectricityImmune(next)) {
                next.hurt(chainSource, damage * mult);
            }
            wyvern.noteAggroFrom(next);
            spawnArc(current.position().add(0, current.getBbHeight() * 0.5, 0),
                    next.position().add(0, next.getBbHeight() * 0.5, 0));

            hit.add(next);
            current = next;
            damage *= CHAIN_FALLOFF;
        }
    }

    private LivingEntity raycastTargetAlongMouth(double maxDistance, double inflateRadius) {
        Raevyx wyvern = getUser();
        DragonMeleeGeometry.ForwardAttack attack = DragonMeleeGeometry.forwardAttack(wyvern);
        Vec3 start = attack.origin();
        Vec3 end = start.add(attack.forward().scale(maxDistance));

        AABB sweep = new AABB(start, end).inflate(inflateRadius);
        var hit = ProjectileUtil.getEntityHitResult(wyvern.level(), wyvern, start, end, sweep,
                e -> e instanceof LivingEntity le && e != wyvern && e.isAlive() && le.attackable() && !isAllied(wyvern, e),
                (float)(maxDistance * maxDistance));
        if (hit != null && hit.getEntity() instanceof LivingEntity le) {
            return le;
        }
        return null;
    }

    private LivingEntity findNearestChainTarget(LivingEntity origin, Set<LivingEntity> exclude) {
        Raevyx wyvern = getUser();
        double range = CHAIN_RADIUS;
        List<LivingEntity> nearby = origin.level().getEntitiesOfClass(
                LivingEntity.class,
                origin.getBoundingBox().inflate(range, range, range),
                entity -> entity != origin && origin.distanceTo(entity) <= range + entity.getBbWidth() / 2.0F
        );
        LivingEntity best = null;
        double bestDist = Double.MAX_VALUE;
        for (LivingEntity e : nearby) {
            if (e == wyvern
                    || exclude.contains(e)
                    || !e.isAlive()
                    || !e.attackable()
                    || isAllied(wyvern, e)
                    || DragonElementalImmunity.isElectricityImmune(e)) {
                continue;
            }
            double d = e.distanceToSqr(origin);
            if (d < bestDist) {
                if (!wyvern.getSensing().hasLineOfSight(e)) continue;
                bestDist = d;
                best = e;
            }
        }
        return best;
    }

    private boolean isAllied(Raevyx wyvern, Entity other) {
        return wyvern.isAlly(other);
    }

    private void spawnArc(Vec3 from, Vec3 to) {
        if (!(getLevel() instanceof ServerLevel server)) return;

        server.addFreshEntity(new RaevyxGroundRendTrailEntity(server, from, to, 1.0F, CHAIN_VISUAL_LIFETIME, server.random.nextLong()));

        Vec3 midpoint = from.lerp(to, 0.5D);
        server.sendParticles(
                ParticleTypes.ELECTRIC_SPARK,
                midpoint.x,
                midpoint.y,
                midpoint.z,
                2,
                0.06D,
                0.06D,
                0.06D,
                0.0D
        );
    }
}

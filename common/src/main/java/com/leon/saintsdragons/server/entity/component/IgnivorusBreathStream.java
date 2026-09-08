package com.leon.saintsdragons.server.entity.component;

import com.leon.saintsdragons.common.config.dragon.DragonAttributeConfigLoader;
import com.leon.saintsdragons.common.particle.ExpandingBreathSection;
import com.leon.saintsdragons.common.particle.FireBreathParticleData;
import com.leon.saintsdragons.server.entity.dragons.ignivorus.Ignivorus;
import com.leon.saintsdragons.server.entity.dragons.util.DragonDestructionManager;
import com.leon.saintsdragons.server.entity.dragons.util.DragonElementalImmunity;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

public final class IgnivorusBreathStream {
    static final int DAMAGE_INTERVAL = 10;
    private final Ignivorus dragon;
    private final List<ExpandingBreathSection> sections = new ArrayList<>();
    private final HitCadence hits = new HitCadence();
    private final Map<BlockPos, Long> recentImpacts = new HashMap<>();
    private int lastEmissionTick = Integer.MIN_VALUE;

    public IgnivorusBreathStream(Ignivorus dragon) {
        this.dragon = dragon;
    }

    public void emit(Vec3 origin, Vec3 direction) {
        if (!(dragon.level() instanceof ServerLevel level) || !dragon.isAlive()
                || lastEmissionTick == dragon.tickCount || direction.lengthSqr() < 1.0E-8) return;
        lastEmissionTick = dragon.tickCount;
        Vec3 velocity = direction.normalize().scale(ExpandingBreathSection.DEFAULT_SPEED);
        ExpandingBreathSection section = new ExpandingBreathSection(origin, velocity,
                ExpandingBreathSection.DEFAULT_RANGE);
        if (sections.size() >= ExpandingBreathSection.MAX_TICKS) sections.remove(0);
        sections.add(section);

        FireBreathParticleData particle = new FireBreathParticleData((float) section.range(), 1.0F);
        AABB visibleArea = new AABB(origin, origin.add(direction.normalize().scale(section.range()))).inflate(64);
        for (ServerPlayer viewer : level.players()) {
            if (visibleArea.contains(viewer.position())) {
                level.sendParticles(viewer, particle, true, origin.x, origin.y, origin.z, 0,
                        velocity.x, velocity.y, velocity.z, 1);
            }
        }
    }

    public void tick() {
        if (!(dragon.level() instanceof ServerLevel level)) return;
        if (!dragon.isAlive() || dragon.isRemoved()) {
            clear();
            return;
        }
        long now = level.getGameTime();
        hits.expire(now);
        recentImpacts.values().removeIf(expiry -> expiry <= now);
        if (sections.isEmpty()) return;
        var config = DragonAttributeConfigLoader.getInstance().getConfig(DragonAttributeConfigLoader.IGNIVORUS_ID);
        float damage = (float) Math.max(0, config.abilityDamage("fire_breath", 80)) * DAMAGE_INTERVAL / 20.0F;
        Set<UUID> attempted = new HashSet<>();
        List<Vec3> impacts = new ArrayList<>();
        for (ExpandingBreathSection section : sections) {
            AABB bounds = section.nextBounds();
            List<ExpandingBreathSection.Sweep> sweeps = section.advance(level);
            if (sweeps.isEmpty()) continue;
            List<LivingEntity> targets = level.getEntitiesOfClass(LivingEntity.class, bounds, this::canHit);
            for (LivingEntity target : targets) {
                UUID id = target.getUUID();
                if (!hits.ready(id, now) || attempted.contains(id)) continue;
                for (ExpandingBreathSection.Sweep sweep : sweeps) {
                    if (!sweep.hits(target.getBoundingBox())) continue;
                    attempted.add(id);
                    // Preserve vanilla hurt immunity, shields, armor and damage-event cancellation.
                    if (damage > 0 && target.hurt(level.damageSources().mobAttack(dragon), damage)) {
                        hits.record(id, now);
                        target.setSecondsOnFire(3);
                    }
                    break;
                }
            }
            for (ExpandingBreathSection.Sweep sweep : sweeps) {
                Vec3 impact = sweep.blockImpact();
                if (impact != null
                        && recentImpacts.putIfAbsent(BlockPos.containing(impact), now + DAMAGE_INTERVAL) == null) {
                    impacts.add(impact);
                }
            }
        }
        sections.removeIf(ExpandingBreathSection::finished);
        // Apply terrain effects after all collision queries, so one impact cannot alter another's trace.
        for (Vec3 impact : impacts) {
            DragonDestructionManager.applyFlameImpact(level, dragon, impact, 1.2);
        }
    }

    private boolean canHit(LivingEntity target) {
        return target.isAlive() && !target.isRemoved() && target != dragon
                && !dragon.hasIndirectPassenger(target) && !dragon.isAlly(target)
                && !(target instanceof Ignivorus baby && baby.isBaby())
                && !(target instanceof Player player && (player.isCreative() || player.isSpectator()))
                && !DragonElementalImmunity.isFireImmune(target);
    }

    public void clear() {
        sections.clear();
        hits.clear();
        recentImpacts.clear();
    }

    static final class HitCadence {
        private final Map<UUID, Long> nextHits = new HashMap<>();

        boolean ready(UUID target, long tick) {
            return nextHits.getOrDefault(target, Long.MIN_VALUE) <= tick;
        }

        void record(UUID target, long tick) {
            nextHits.put(target, tick + DAMAGE_INTERVAL);
        }

        void expire(long tick) {
            nextHits.values().removeIf(expiry -> expiry <= tick);
        }

        void clear() {
            nextHits.clear();
        }
    }
}

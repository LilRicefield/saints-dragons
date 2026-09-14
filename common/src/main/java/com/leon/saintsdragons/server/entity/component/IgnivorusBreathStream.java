package com.leon.saintsdragons.server.entity.component;

import com.leon.saintsdragons.common.config.dragon.DragonAttributeConfigLoader;
import com.leon.saintsdragons.common.particle.ExpandingBreathSection;
import com.leon.saintsdragons.common.particle.FireBreathParticleData;
import com.leon.saintsdragons.common.registry.ModParticles;
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
import net.minecraft.world.level.block.state.BlockState;
import java.util.LinkedHashMap;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

public final class IgnivorusBreathStream {
    static final int DAMAGE_INTERVAL = 10;
    private static final double BACKBLAST_SIDE_OFFSET = 0.75;
    private final Ignivorus dragon;
    private final SweptBreathStream<FireSection> stream = new SweptBreathStream<>(DAMAGE_INTERVAL, ExpandingBreathSection.MAX_TICKS);

    private static final class FireSection {
        boolean breaking;
        final long learningTrial;

        FireSection(long learningTrial) { this.learningTrial = learningTrial; }
    }
    private final IgnivorusBreathTerrain terrain = new IgnivorusBreathTerrain();
    private final Map<BlockPos, Long> recentImpacts = new HashMap<>();
    private int lastEmissionTick = Integer.MIN_VALUE;

    public IgnivorusBreathStream(Ignivorus dragon) {
        this.dragon = dragon;
    }

    public void emit(Vec3 origin, Vec3 direction, boolean canBreakBlocks) {
        emit(origin, direction, canBreakBlocks, 0);
    }

    public void emit(Vec3 origin, Vec3 direction, boolean canBreakBlocks, long learningTrial) {
        if (!(dragon.level() instanceof ServerLevel level) || !dragon.isAlive()
                || lastEmissionTick == dragon.tickCount || direction.lengthSqr() < 1.0E-8) return;
        lastEmissionTick = dragon.tickCount;
        Vec3 velocity = direction.normalize().scale(ExpandingBreathSection.DEFAULT_SPEED);
        ExpandingBreathSection section = new ExpandingBreathSection(origin, velocity,
                ExpandingBreathSection.DEFAULT_RANGE);
        stream.emit(section, new FireSection(learningTrial));
        // At the six-second mark, fire already in flight becomes destructive too.
        if (canBreakBlocks) stream.forEachPayload(payload -> payload.breaking = true);

        FireBreathParticleData particle = new FireBreathParticleData((float) section.range(), 1.0F);
        AABB visibleArea = new AABB(origin, origin.add(direction.normalize().scale(section.range()))).inflate(64);
        Vec3 bodyForward = Vec3.directionFromRotation(0, dragon.yBodyRot);
        Vec3 bodyRight = bodyForward.cross(new Vec3(0, 1, 0)).normalize();
        Vec3 backblastStart = origin.subtract(bodyForward.scale(0.35));
        Vec3 leftStart = backblastStart.subtract(bodyRight.scale(BACKBLAST_SIDE_OFFSET));
        Vec3 rightStart = backblastStart.add(bodyRight.scale(BACKBLAST_SIDE_OFFSET));
        double backblastAngle = Math.toRadians(30);
        Vec3 backward = bodyForward.scale(-Math.cos(backblastAngle));
        Vec3 sideways = bodyRight.scale(Math.sin(backblastAngle));
        Vec3 leftLaunch = backward.subtract(sideways);
        Vec3 rightLaunch = backward.add(sideways);
        for (ServerPlayer viewer : level.players()) {
            if (visibleArea.contains(viewer.position())) {
                level.sendParticles(viewer, particle, true, origin.x, origin.y, origin.z, 0,
                        velocity.x, velocity.y, velocity.z, 1);
                level.sendParticles(viewer, ModParticles.FIRE_BREATH_BACKBLAST.get(), true,
                        leftStart.x, leftStart.y, leftStart.z, 0,
                        leftLaunch.x, leftLaunch.y, leftLaunch.z, 1);
                level.sendParticles(viewer, ModParticles.FIRE_BREATH_BACKBLAST.get(), true,
                        rightStart.x, rightStart.y, rightStart.z, 0,
                        rightLaunch.x, rightLaunch.y, rightLaunch.z, 1);
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
        recentImpacts.values().removeIf(expiry -> expiry <= now);
        var config = DragonAttributeConfigLoader.getInstance().getConfig(DragonAttributeConfigLoader.IGNIVORUS_ID);
        float damage = (float) Math.max(0, config.abilityDamage("fire_breath", 80)) * DAMAGE_INTERVAL / 20.0F;
        List<Vec3> impacts = new ArrayList<>();
        Map<BlockPos, BlockState> blockHits = new LinkedHashMap<>();
        Set<BlockPos> cookingHits = new HashSet<>();
        stream.tick(level, (payload, target) -> canHit(target), (payload, target, sweep) -> {
            if (DragonElementalImmunity.isFireImmune(target)) {
                dragon.getCombatLearning().recordContact(payload.learningTrial, target);
                return false;
            }
            // Preserve vanilla hurt immunity, shields, armor and damage-event cancellation.
            if (damage > 0 && target.hurt(level.damageSources().mobAttack(dragon), damage)) {
                dragon.getCombatLearning().recordHit(payload.learningTrial, target);
                target.setSecondsOnFire(3);
                return true;
            }
            dragon.getCombatLearning().recordContact(payload.learningTrial, target);
            return false;
        }, (payload, sweep) -> {
            Vec3 impact = sweep.blockImpact();
            BlockPos blockPos = sweep.blockPos();
            if (blockPos != null) cookingHits.add(blockPos);
            if (blockPos != null && payload.breaking) {
                blockHits.computeIfAbsent(blockPos, level::getBlockState);
            }
            if (impact != null
                    && recentImpacts.putIfAbsent(blockPos != null ? blockPos : BlockPos.containing(impact), now + DAMAGE_INTERVAL) == null) {
                impacts.add(impact);
            }
        });
        // Apply terrain effects after all collision queries, so one impact cannot alter another's trace.
        terrain.tick(level, dragon, blockHits);
        for (BlockPos pos : cookingHits) {
            DragonDestructionManager.applyFlameCookingHit(level, dragon, pos);
        }
        for (Vec3 impact : impacts) {
            DragonDestructionManager.applyFlameIgnition(level, impact, 1.2);
        }
    }

    private boolean canHit(LivingEntity target) {
        return target.isAlive() && !target.isRemoved() && target != dragon
                && !dragon.hasIndirectPassenger(target) && !dragon.isAlly(target)
                && !(target instanceof Ignivorus baby && baby.isBaby())
                && !(target instanceof Player player && (player.isCreative() || player.isSpectator()));
    }

    public void clear() {
        stream.clear();
        recentImpacts.clear();
    }
}

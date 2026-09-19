package com.leon.saintsdragons.server.entity.component;

import com.leon.saintsdragons.common.particle.ExpandingBreathSection;
import com.leon.saintsdragons.server.entity.dragons.util.DragonElementalImmunity;
import com.leon.saintsdragons.server.entity.dragons.util.DragonUtilities;
import com.leon.saintsdragons.server.entity.dragons.volitans.Volitans;
import com.leon.saintsdragons.common.particle.VolitansBreathParticleData;
import static com.leon.saintsdragons.common.particle.VolitansBreathMotion.*;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.FluidTags;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.LevelEvent;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import java.util.HashSet;
import java.util.Set;

public final class VolitansBreathStream {
    private static final ExpandingBreathSection.Profile WATER = profile(false);
    private static final ExpandingBreathSection.Profile POISON = profile(true);
    private final Volitans dragon;
    private final SweptBreathStream<Payload> stream = new SweptBreathStream<>(2, LIFETIME);
    private int lastEmissionTick = Integer.MIN_VALUE;

    public VolitansBreathStream(Volitans dragon) {
        this.dragon = dragon;
    }

    static ExpandingBreathSection.Profile profile(boolean poison) {
        return new ExpandingBreathSection.Profile(0.8, 0.8 + RANGE * SPREAD * 0.5,
                SPREAD * 0.5, LIFETIME - 1, DRAG, poison ? null : FluidTags.LAVA);
    }

    public void emit(Vec3 origin, Vec3 direction) {
        if (!(dragon.level() instanceof ServerLevel level) || !dragon.isAlive()
                || lastEmissionTick == dragon.tickCount || direction.lengthSqr() < 1.0E-8) return;
        lastEmissionTick = dragon.tickCount;
        boolean poison = dragon.isPoisonBreathMode();
        Payload payload = new Payload(dragon.getBreathCombat().learningTrial(), poison,
                dragon.getConfiguredAbilityDamage(poison ? "poison_breath" : "water_breath", poison ? 1.4F : 1.8F),
                poison ? 0.0F : 0.14F,
                Math.max(0, (int) Math.round(dragon.getConfiguredExtra("poison_breath_poison_duration_ticks", 80))),
                dragon.getConfiguredPoisonAmplifier("poison_breath_poison_level", 1));
        stream.emit(new ExpandingBreathSection(origin, direction.normalize().scale(SPEED), RANGE,
                poison ? POISON : WATER), payload);

        Vec3 velocity = direction.normalize().scale(SPEED);
        AABB visibleArea = new AABB(origin, origin.add(direction.normalize().scale(RANGE))).inflate(64);
        var particle = new VolitansBreathParticleData(dragon.getId());
        for (var viewer : level.players()) {
            if (visibleArea.contains(viewer.position())) {
                level.sendParticles(viewer, particle, true, origin.x, origin.y, origin.z,
                        0, velocity.x, velocity.y, velocity.z, 1.0);
            }
        }
    }

    public static ExpandingBreathSection.Profile collisionProfile(boolean poison) {
        return poison ? POISON : WATER;
    }

    public void tick() {
        if (!(dragon.level() instanceof ServerLevel level)) return;
        if (!dragon.isAlive() || dragon.isRemoved() || dragon.isDying()) {
            clear();
            return;
        }
        Set<BlockPos> waterContacts = new HashSet<>();
        stream.tick(level, (payload, target) -> canHit(target), (payload, target, sweep) -> {
            dragon.getCombatLearning().recordContact(payload.learningTrial(), target);
            if (payload.poison() && !dragon.isVenomNeutralized() && DragonElementalImmunity.isPoisonImmune(target)) {
                return false;
            }
            if (payload.damage() > 0 && target.hurt(level.damageSources().mobAttack(dragon), payload.damage())) {
                dragon.getCombatLearning().recordHit(payload.learningTrial(), target);
            }
            if (payload.poison() && !dragon.isVenomNeutralized()
                    && payload.poisonTicks() > 0 && payload.poisonAmplifier() >= 0) {
                target.addEffect(new MobEffectInstance(MobEffects.POISON, payload.poisonTicks(), payload.poisonAmplifier()));
            }
            if (payload.push() > 0) {
                Vec3 push = target.position().subtract(sweep.start());
                if (push.lengthSqr() < 1.0E-6) push = sweep.end().subtract(sweep.start());
                push = push.normalize().scale(payload.push());
                target.push(push.x, 0.04, push.z);
                target.hasImpulse = true;
            }
            return true;
        }, (payload, sweep) -> {
            if (sweep.blockPos() != null) dragon.getCombatLearning().recordContact(payload.learningTrial());
            if (payload.poison()) return;
            if (sweep.blockPos() != null) waterContacts.add(sweep.blockPos());
            Vec3 extent = sweep.toHalf();
            AABB bounds = new AABB(sweep.start(), sweep.visibleEnd()).inflate(extent.x, extent.y, extent.z);
            for (BlockPos pos : BlockPos.betweenClosed(BlockPos.containing(bounds.minX, bounds.minY, bounds.minZ),
                    BlockPos.containing(bounds.maxX, bounds.maxY, bounds.maxZ))) {
                if (sweep.hits(new AABB(pos))) waterContacts.add(pos.immutable());
            }
        });
        boolean extinguished = false;
        for (BlockPos pos : waterContacts) {
            if (!level.isLoaded(pos)) continue;
            if (level.getFluidState(pos).is(FluidTags.LAVA)) {
                level.setBlock(pos, Blocks.COBBLESTONE.defaultBlockState(), 3);
                level.levelEvent(LevelEvent.LAVA_FIZZ, pos, 0);
            } else {
                Vec3 center = Vec3.atCenterOf(pos);
                extinguished |= DragonUtilities.extinguishFire(level, center, center, 0.0);
            }
        }
        if (extinguished) {
            var player = DragonUtilities.resolveResponsiblePlayer(dragon);
            if (player != null) DragonUtilities.awardAdvancement(player, "fire_hydrant", "fire_hydrant");
        }
    }

    private boolean canHit(LivingEntity target) {
        return target.isAlive() && !target.isRemoved() && target != dragon
                && !dragon.hasIndirectPassenger(target) && !dragon.isAlly(target) && !dragon.isAlliedTo(target)
                && !(target instanceof Player player && (player.isCreative() || player.isSpectator()));
    }

    public void clear() {
        stream.clear();
    }

    private record Payload(long learningTrial, boolean poison, float damage, float push, int poisonTicks, int poisonAmplifier) {}
}

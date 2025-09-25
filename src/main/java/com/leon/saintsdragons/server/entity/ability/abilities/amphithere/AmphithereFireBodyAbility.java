package com.leon.saintsdragons.server.entity.ability.abilities.amphithere;

import com.leon.saintsdragons.server.entity.ability.DragonAbility;
import com.leon.saintsdragons.server.entity.ability.DragonAbilitySection;
import com.leon.saintsdragons.server.entity.ability.DragonAbilityType;
import com.leon.saintsdragons.server.entity.dragons.amphithere.AmphithereEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import java.util.HashSet;
import java.util.Set;

import static com.leon.saintsdragons.server.entity.ability.DragonAbilitySection.AbilitySectionDuration;
import static com.leon.saintsdragons.server.entity.ability.DragonAbilitySection.AbilitySectionType.ACTIVE;
import static com.leon.saintsdragons.server.entity.ability.DragonAbilitySection.AbilitySectionType.STARTUP;

/**
 * Hold-to-cast fire breath ability for the Amphithere.
 */
public class AmphithereFireBodyAbility extends DragonAbility<AmphithereEntity> {
    private static final DragonAbilitySection[] TRACK = new DragonAbilitySection[] {
            new AbilitySectionDuration(STARTUP, 1),
            new AbilitySectionDuration(ACTIVE, 1000)
    };

    private static final double MAX_DISTANCE = 12.0D;
    private static final double STEP = 1.0D;
    private static final double BASE_RADIUS = 1.2D;
    private static final double RADIUS_GROWTH = 0.35D;
    private static final double MAX_SPREAD_RADIANS = Math.toRadians(40.0D);
    private static final float BASE_DAMAGE = 1.8F;
    private static final float EDGE_DAMAGE_FACTOR = 0.35F;
    private static final int FIRE_SECONDS = 4;
    private static final float DISTANCE_DAMAGE_FACTOR = 0.45F;

    private int activeTicks;

    public AmphithereFireBodyAbility(DragonAbilityType<AmphithereEntity, AmphithereFireBodyAbility> type,
                                     AmphithereEntity user) {
        super(type, user, TRACK, 40);
    }

    @Override
    protected void beginSection(DragonAbilitySection section) {
        if (section == null) {
            return;
        }
        if (section.sectionType == STARTUP) {
            activeTicks = 0;
            getUser().setBreathingFire(true);
            Level level = getLevel();
            level.playSound(null, getUser().blockPosition(), SoundEvents.FIRECHARGE_USE, getUser().getSoundSource(), 1.2F, 1.0F + getUser().getRandom().nextFloat() * 0.2F);
        } else if (section.sectionType == ACTIVE) {
            getUser().setBreathingFire(true);
        }
    }

    @Override
    protected void endSection(DragonAbilitySection section) {
        if (section != null && section.sectionType == ACTIVE) {
            getUser().setBreathingFire(false);
        }
    }

    @Override
    public void interrupt() {
        getUser().setBreathingFire(false);
        super.interrupt();
    }

    @Override
    protected boolean canContinueUsing() {
        AmphithereEntity dragon = getUser();
        if (!dragon.isAlive() || dragon.isRemoved()) {
            return false;
        }
        if (dragon.isInWaterOrBubble()) {
            return false;
        }
        return true;
    }

    @Override
    public void tickUsing() {
        AmphithereEntity dragon = getUser();
        Level level = dragon.level();

        if (dragon.getControllingPassenger() instanceof Player rider) {
            alignToRider(dragon, rider);
        }

        if (!level.isClientSide) {
            activeTicks++;
            sprayCone((ServerLevel) level, dragon);
            if (activeTicks % 20 == 0) {
                level.playSound(null, dragon.blockPosition(), SoundEvents.BLAZE_SHOOT, dragon.getSoundSource(), 0.6F, 0.9F + dragon.getRandom().nextFloat() * 0.2F);
            }
        }
    }

    private void alignToRider(AmphithereEntity dragon, Player rider) {
        float clampPitch = Mth.clamp(rider.getXRot(), -50F, 35F);
        dragon.setYRot(rider.getYRot());
        dragon.yBodyRot = dragon.getYRot();
        dragon.yHeadRot = rider.getYRot();
        dragon.setXRot(clampPitch);
    }

    private void sprayCone(ServerLevel level, AmphithereEntity dragon) {
        Vec3 mouth = dragon.getMouthPosition();
        Vec3 forward = Vec3.directionFromRotation(dragon.getXRot(), dragon.getYHeadRot()).normalize();
        mouth = mouth.add(forward.scale(0.6D));

        Set<LivingEntity> hitThisTick = new HashSet<>();

        for (double dist = 0.75D; dist <= MAX_DISTANCE; dist += STEP) {
            double radius = BASE_RADIUS + dist * RADIUS_GROWTH;
            Vec3 sample = mouth.add(forward.scale(dist));

            spawnParticles(level, sample, radius);
            maybeIgnite(level, sample, dragon);

            AABB aabb = new AABB(sample, sample).inflate(radius);
            for (LivingEntity target : level.getEntitiesOfClass(LivingEntity.class, aabb,
                    e -> e != dragon && e.isAlive() && e.attackable() && !dragon.isAlly(e))) {
                if (!hitThisTick.add(target)) {
                    continue;
                }
                Vec3 toTarget = target.getBoundingBox().getCenter().subtract(mouth);
                double distance = toTarget.length();
                if (distance > MAX_DISTANCE || distance < 0.01D) {
                    continue;
                }
                Vec3 direction = toTarget.normalize();
                double angle = Math.acos(Mth.clamp(forward.dot(direction), -1.0D, 1.0D));
                if (angle > MAX_SPREAD_RADIANS) {
                    continue;
                }
                float angleFactor = (float) (angle / MAX_SPREAD_RADIANS);
                float damage = BASE_DAMAGE * Mth.lerp(angleFactor, 1.0F, EDGE_DAMAGE_FACTOR);
                damage *= distanceFalloff(distance);
                target.hurt(level.damageSources().dragonBreath(), damage);
                target.setSecondsOnFire(FIRE_SECONDS);
                Vec3 push = forward.scale(0.1D);
                target.push(push.x, 0.02D, push.z);
            }
        }
    }

    private float distanceFalloff(double distance) {
        double normalized = Mth.clamp(distance / MAX_DISTANCE, 0.0D, 1.0D);
        return (float) Mth.lerp(normalized, 1.0D, DISTANCE_DAMAGE_FACTOR);
    }

    private void spawnParticles(ServerLevel level, Vec3 sample, double radius) {
        double spread = Math.max(0.15D, radius * 0.35D);
        int flameCount = Math.max(8, (int) Math.ceil(radius * 10.0D));
        int emberCount = Math.max(6, (int) Math.ceil(radius * 7.0D));
        int smokeCount = Math.max(4, (int) Math.ceil(radius * 5.0D));

        level.sendParticles(ParticleTypes.FLAME, sample.x, sample.y, sample.z, flameCount,
                spread, spread * 0.8D, spread, 0.05D);
        level.sendParticles(ParticleTypes.SMALL_FLAME, sample.x, sample.y, sample.z, emberCount,
                spread * 0.6D, spread * 0.4D, spread * 0.6D, 0.025D);
        level.sendParticles(ParticleTypes.LAVA, sample.x, sample.y, sample.z, Math.max(2, emberCount / 4),
                spread * 0.25D, spread * 0.25D, spread * 0.25D, 0.08D);
        level.sendParticles(ParticleTypes.LARGE_SMOKE, sample.x, sample.y, sample.z, smokeCount,
                spread, spread * 0.5D, spread, 0.0D);
    }

    private void maybeIgnite(ServerLevel level, Vec3 sample, AmphithereEntity dragon) {
        if (dragon.getRandom().nextFloat() > 0.12F) {
            return;
        }
        BlockPos pos = BlockPos.containing(sample.x, sample.y - 0.5D, sample.z);
        if (!level.isLoaded(pos) || !level.isEmptyBlock(pos)) {
            return;
        }
        BlockPos below = pos.below();
        BlockState belowState = level.getBlockState(below);
        if (belowState.isAir()) {
            return;
        }
        if (Blocks.FIRE.defaultBlockState().canSurvive(level, pos) && belowState.isFaceSturdy(level, below, Direction.UP)) {
            level.setBlock(pos, Blocks.FIRE.defaultBlockState(), 11);
        }
    }
}

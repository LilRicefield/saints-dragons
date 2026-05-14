package com.leon.saintsdragons.server.entity.ability.abilities.ignivorus;

import com.leon.saintsdragons.common.config.dragon.DragonAttributeConfigLoader;
import com.leon.saintsdragons.common.registry.ModEntities;
import com.leon.saintsdragons.common.registry.ModSounds;
import com.leon.saintsdragons.server.entity.ability.DragonAbility;
import com.leon.saintsdragons.server.entity.ability.DragonAbilitySection;
import com.leon.saintsdragons.server.entity.ability.DragonAbilityType;
import com.leon.saintsdragons.server.entity.dragons.ignivorus.Ignivorus;
import com.leon.saintsdragons.server.entity.dragons.ignivorus.handlers.IgnivorusAnimationHandler;
import com.leon.saintsdragons.server.entity.effect.VisualFallingBlockEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.BlockParticleOption;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.List;

import static com.leon.saintsdragons.server.entity.ability.DragonAbilitySection.AbilitySectionDuration;
import static com.leon.saintsdragons.server.entity.ability.DragonAbilitySection.AbilitySectionType.ACTIVE;
import static com.leon.saintsdragons.server.entity.ability.DragonAbilitySection.AbilitySectionType.RECOVERY;
import static com.leon.saintsdragons.server.entity.ability.DragonAbilitySection.AbilitySectionType.STARTUP;

public class IgnivorusBodySlamAbility extends DragonAbility<Ignivorus> {
    private static final int STARTUP_TICKS = 15;
    private static final int ACTIVE_TICKS = 7;
    private static final int RECOVERY_TICKS = 8;
    private static final int CONTROL_LOCK_TICKS = 29;
    private static final int COOLDOWN_TICKS = 20;

    private static final float BASE_DAMAGE = 40.0f;
    private static final double PUSH_STRENGTH = 1.1D;
    private static final double LIFT_FORCE = 0.6D;

    private static final DragonAbilitySection[] TRACK = new DragonAbilitySection[]{
            new AbilitySectionDuration(STARTUP, STARTUP_TICKS),
            new AbilitySectionDuration(ACTIVE, ACTIVE_TICKS),
            new AbilitySectionDuration(RECOVERY, RECOVERY_TICKS)
    };

    private boolean impactApplied;

    public IgnivorusBodySlamAbility(DragonAbilityType<Ignivorus, IgnivorusBodySlamAbility> type, Ignivorus user) {
        super(type, user, TRACK, COOLDOWN_TICKS);
    }

    @Override
    protected void beginSection(DragonAbilitySection section) {
        if (section == null) {
            return;
        }
        Ignivorus dragon = getUser();
        if (section.sectionType == STARTUP) {
            impactApplied = false;
            dragon.triggerAnim(IgnivorusAnimationHandler.ACTION_CONTROLLER, "body_slam");
            if (!dragon.level().isClientSide) {
                dragon.getSoundHandler().playMovingEntitySound(ModSounds.IGNIVORUS_BODY_SLAM.get(), 1.0f, 1.0f, 53);
            }
            dragon.lockRiderControls(CONTROL_LOCK_TICKS);
        }
    }

    @Override
    public void tickUsing() {
        DragonAbilitySection section = getCurrentSection();
        if (section == null) {
            return;
        }

        Ignivorus dragon = getUser();
        if (dragon == null) {
            return;
        }

        // Abort if the dragon somehow takes off mid-ability
        if (dragon.isFlying()) {
            interrupt();
            return;
        }

        if (section.sectionType == ACTIVE && !impactApplied) {
            impactApplied = true;
            applySlam(dragon);
        }
    }

    private void applySlam(Ignivorus dragon) {
        Level level = dragon.level();
        if (!(level instanceof ServerLevel server)) {
            return;
        }

        double inflateXZ = Math.max(1.5D, dragon.getBbWidth()) * 1.75D;
        double inflateY = Math.max(1.5D, dragon.getBbHeight() * 1.75D);
        AABB slamArea = dragon.getBoundingBox().inflate(inflateXZ, inflateY, inflateXZ);

        // Spawn visual effects - blocks and particles
        spawnBodySlamBlockEffect(server, dragon, inflateXZ);
        spawnBodySlamDirtParticles(server, dragon, inflateXZ);

        List<LivingEntity> targets = server.getEntitiesOfClass(LivingEntity.class, slamArea,
                entity -> entity != dragon && entity.isAlive() && entity.attackable() && !dragon.isAlly(entity));

        if (targets.isEmpty()) {
            return;
        }

        float damage = computeDamage() * dragon.getHungerMeleeDamageMultiplier();
        DamageSource source = server.damageSources().mobAttack(dragon);

        for (LivingEntity target : targets) {
            target.hurt(source, damage);

            Vec3 push = target.position().subtract(dragon.position());
            if (push.lengthSqr() < 1.0E-4) {
                push = new Vec3(0, 0, 1);
            }
            push = push.normalize();
            double scaledPush = PUSH_STRENGTH + dragon.getBbWidth() * 0.15D;
            target.push(push.x * scaledPush, LIFT_FORCE, push.z * scaledPush);
            target.hasImpulse = true;
        }
    }

    private static float computeDamage() {
        return resolveBaseDamage();
    }

    private static float resolveBaseDamage() {
        return (float) DragonAttributeConfigLoader.getInstance()
                .getConfig(DragonAttributeConfigLoader.IGNIVORUS_ID)
                .abilityDamage("body_slam", BASE_DAMAGE);
    }

    @Override
    public boolean tryAbility() {
        Ignivorus dragon = getUser();
        if (dragon == null || dragon.isBaby() || dragon.isFlying() || !dragon.onGround() || dragon.areRiderControlsLocked()) {
            return false;
        }
        return super.tryAbility();
    }

    @Override
    protected void endSection(DragonAbilitySection section) {
        if (section != null && section.sectionType == RECOVERY) {
            impactApplied = false;
        }
    }

    @Override
    public void interrupt() {
        impactApplied = false;
        super.interrupt();
    }

    /**
     * Spawns visual falling blocks in rings around the body slam impact
     */
    private void spawnBodySlamBlockEffect(ServerLevel level, Ignivorus dragon, double radius) {
        RandomSource random = dragon.getRandom();
        BlockPos dragonPos = dragon.blockPosition();
        List<BlockPos> blockPositions = new ArrayList<>();

        // Scale rings based on dragon size
        int maxRadius = (int) Math.ceil(radius);
        int midRadius = maxRadius * 2 / 3;
        int minRadius = maxRadius / 3;

        // Outer ring
        addRingPositions(blockPositions, dragonPos, (int)(maxRadius * 0.8), maxRadius, random, 20);

        // Middle ring
        addRingPositions(blockPositions, dragonPos, (int)(midRadius * 0.6), (int)(midRadius * 1.2), random, 15);

        // Inner ring
        addRingPositions(blockPositions, dragonPos, minRadius, (int)(minRadius * 1.5), random, 10);

        // Spawn falling blocks
        for (BlockPos pos : blockPositions) {
            spawnBodySlamFallingBlock(level, dragon, pos, random);
        }
    }

    /**
     * Spawns dirt particles in expanding rings around the body slam
     */
    private void spawnBodySlamDirtParticles(ServerLevel level, Ignivorus dragon, double radius) {
        RandomSource random = dragon.getRandom();
        Vec3 dragonPos = dragon.position();
        BlockParticleOption dirtParticles = new BlockParticleOption(ParticleTypes.BLOCK, Blocks.DIRT.defaultBlockState());

        int maxRadius = (int) Math.ceil(radius);
        int midRadius = maxRadius * 2 / 3;
        int minRadius = maxRadius / 3;

        // Inner ring
        spawnParticleRing(level, dragonPos, dirtParticles, minRadius, (int)(minRadius * 1.5), 30, random, dragon);

        // Middle ring
        spawnParticleRing(level, dragonPos, dirtParticles, (int)(midRadius * 0.6), (int)(midRadius * 1.2), 40, random, dragon);

        // Outer ring
        spawnParticleRing(level, dragonPos, dirtParticles, (int)(maxRadius * 0.8), maxRadius, 50, random, dragon);
    }

    private void addRingPositions(List<BlockPos> positions, BlockPos center, int minRadius, int maxRadius,
                                   RandomSource random, int count) {
        for (int i = 0; i < count; i++) {
            double angle = random.nextDouble() * Math.PI * 2;
            double radius = minRadius + random.nextDouble() * (maxRadius - minRadius);
            int xOffset = (int) Math.round(Math.cos(angle) * radius);
            int zOffset = (int) Math.round(Math.sin(angle) * radius);
            BlockPos targetPos = center.offset(xOffset, 0, zOffset);
            positions.add(targetPos);
        }
    }

    private void spawnBodySlamFallingBlock(ServerLevel level, Ignivorus dragon, BlockPos pos, RandomSource random) {
        BlockPos groundPos = findGroundLevel(level, dragon, pos);
        if (groundPos == null) {
            return;
        }

        BlockState groundState = level.getBlockState(groundPos);
        if (groundState.isAir() || groundState.liquid() || groundState.is(Blocks.BEDROCK)) {
            return;
        }

        double startX = groundPos.getX() + 0.5;
        double startY = groundPos.getY() + 0.5;
        double startZ = groundPos.getZ() + 0.5;

        VisualFallingBlockEntity fallingBlock = new VisualFallingBlockEntity(
            ModEntities.VISUAL_FALLING_BLOCK.get(),
            level,
            startX,
            startY,
            startZ,
            groundState,
            200
        );

        double upwardVelocity = 0.4 + random.nextDouble() * 0.6;
        fallingBlock.setDeltaMovement(0, upwardVelocity, 0);
        level.addFreshEntity(fallingBlock);
    }

    private void spawnParticleRing(ServerLevel level, Vec3 center, BlockParticleOption particleType,
                                    int minRadius, int maxRadius, int count, RandomSource random, Ignivorus dragon) {
        for (int i = 0; i < count; i++) {
            double angle = random.nextDouble() * Math.PI * 2;
            double radius = minRadius + random.nextDouble() * (maxRadius - minRadius);
            double x = center.x + Math.cos(angle) * radius;
            double z = center.z + Math.sin(angle) * radius;

            BlockPos groundPos = findGroundLevel(level, dragon, new BlockPos((int)x, (int)center.y, (int)z));
            if (groundPos == null) {
                continue;
            }

            BlockState groundState = level.getBlockState(groundPos);
            if (groundState.isAir() || groundState.liquid()) {
                continue;
            }

            double particleY = groundPos.getY() + 1.02;
            int burstCount = 5;
            for (int j = 0; j < burstCount; j++) {
                double velX = (random.nextDouble() - 0.5) * 0.8;
                double velY = 0.2 + random.nextDouble() * 0.7;
                double velZ = (random.nextDouble() - 0.5) * 0.8;
                level.sendParticles(particleType, x, particleY, z, 0, velX, velY, velZ, 1.0);
            }
        }
    }

    private BlockPos findGroundLevel(ServerLevel level, Ignivorus dragon, BlockPos startPos) {
        int dragonY = dragon.blockPosition().getY();
        for (int y = dragonY; y > level.getMinBuildHeight(); y--) {
            BlockPos checkPos = new BlockPos(startPos.getX(), y, startPos.getZ());
            BlockState state = level.getBlockState(checkPos);
            if (!state.isAir() && !state.liquid() && state.isSolidRender(level, checkPos)) {
                return checkPos;
            }
        }
        return null;
    }
}

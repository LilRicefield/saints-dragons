package com.leon.saintsdragons.server.entity.ability.abilities.volitans;

import com.leon.saintsdragons.common.registry.ModEntities;
import com.leon.saintsdragons.common.registry.ModParticles;
import com.leon.saintsdragons.common.registry.ModSounds;
import com.leon.saintsdragons.server.entity.ability.DragonAbility;
import com.leon.saintsdragons.server.entity.ability.DragonAbilitySection;
import com.leon.saintsdragons.server.entity.ability.DragonAbilityType;
import com.leon.saintsdragons.server.entity.dragons.volitans.Volitans;
import com.leon.saintsdragons.server.entity.dragons.volitans.handlers.VolitansAnimationHandler;
import com.leon.saintsdragons.server.entity.effect.VisualFallingBlockEntity;
import com.leon.saintsdragons.server.entity.effect.volitans.VolitansBurrowMoundEntity;
import com.leon.saintsdragons.server.entity.effect.volitans.VolitansGroundChunkEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.BlockParticleOption;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import java.util.List;

import static com.leon.saintsdragons.server.entity.ability.DragonAbilitySection.AbilitySectionDuration;
import static com.leon.saintsdragons.server.entity.ability.DragonAbilitySection.AbilitySectionType.ACTIVE;
import static com.leon.saintsdragons.server.entity.ability.DragonAbilitySection.AbilitySectionType.RECOVERY;
import static com.leon.saintsdragons.server.entity.ability.DragonAbilitySection.AbilitySectionType.STARTUP;

public class VolitansBurrowAbility extends DragonAbility<Volitans> {
    private static final int STARTUP_TICKS = 14;
    private static final int ACTIVE_TICKS_MAX = 20 * 15;
    private static final int EXIT_TICKS = 52;
    private static final int EXIT_BURST_DELAY_TICKS = 28;
    private static final int EXIT_LATE_DUST_DELAY_TICKS = 50;
    private static final int ENTER_BURROW_MOUND_DELAY_TICKS = 13;
    private static final int EXIT_BURROW_MOUND_DELAY_TICKS = 26;
    private static final int ENTER_BURROW_SOUND_TICKS = 60;
    private static final int EXIT_BURROW_SOUND_TICKS = 100;
    private static final int GROUND_CHUNK_INTERVAL_TICKS = 6;
    private static final double GROUND_CHUNK_MIN_DISTANCE_SQR = 2.25D;
    private static final double BURROW_MOVEMENT_PARTICLE_SPEED_SQR = 0.015D;
    private static final int COOLDOWN_TICKS = 50;
    private static final float EXIT_DAMAGE = 30.0F;
    private static final double EXIT_RADIUS = 12.0D;
    private static final double EXIT_UPWARD_KNOCK = 1.0D;
    private static final double EXIT_DUST_COLUMN_HEIGHT = 6.0D;
    private static final int EXIT_DUST_COLUMN_STEPS = 10;
    private static final int EXIT_DUST_COLUMN_PARTICLES_PER_STEP = 5;
    private static final double EXIT_DUST_COLUMN_SPEED = 0.32D;
    private static final int EXIT_LATE_DUST_COUNT = 42;
    private static final double EXIT_LATE_DUST_RADIUS = 4.2D;
    private static final double EXIT_LATE_DUST_SPEED = 0.18D;
    private static final int EXIT_BLOCK_PARTICLE_COUNT = 120;
    private static final int EXIT_BLOCK_TRAIL_PARTICLE_COUNT = 96;
    private static final int EXIT_FALLING_BLOCK_COUNT = 16;
    private static final int EXIT_FALLING_BLOCK_LIFETIME = 48;
    private static final double EXIT_FALLING_BLOCK_RADIUS = 3.8D;
    private static final double EXIT_FALLING_BLOCK_SPEED_MIN = 0.32D;
    private static final double EXIT_FALLING_BLOCK_SPEED_MAX = 0.62D;
    private static final double EXIT_FALLING_BLOCK_UP_SPEED_MIN = 0.32D;
    private static final double EXIT_FALLING_BLOCK_UP_SPEED_MAX = 0.58D;

    private static final DragonAbilitySection[] TRACK = new DragonAbilitySection[] {
            new AbilitySectionDuration(STARTUP, STARTUP_TICKS),
            new AbilitySectionDuration(ACTIVE, ACTIVE_TICKS_MAX),
            new AbilitySectionDuration(RECOVERY, EXIT_TICKS)
    };
    private boolean exitRequested;
    private boolean applyBurstOnExit;
    private boolean burstApplied;
    private boolean lateExitDustSpawned;
    private boolean enterMoundSpawned;
    private boolean exitMoundSpawned;
    private Vec3 lastGroundChunkPos;
    private Vec3 lastParticlePos;

    public VolitansBurrowAbility(DragonAbilityType<Volitans, VolitansBurrowAbility> type, Volitans user) {
        super(type, user, TRACK, COOLDOWN_TICKS);
    }

    @Override
    public boolean tryAbility() {
        Volitans dragon = getUser();
        if (dragon == null || !dragon.isAlive() || dragon.isDying() || dragon.isBaby()) {
            return false;
        }
        if (dragon.isFlying() || dragon.isInWaterOrBubble() || dragon.isUnderWater()) {
            return false;
        }
        if (dragon.getControllingPassenger() instanceof Player rider) {
            return dragon.isTame() && dragon.isOwnedBy(rider) && super.tryAbility();
        }
        if (dragon.isVehicle()) {
            return false;
        }
        return dragon.isTargetValid(dragon.getTarget()) && super.tryAbility();
    }

    @Override
    protected void beginSection(DragonAbilitySection section) {
        if (section == null) {
            return;
        }
        Volitans dragon = getUser();
        if (section.sectionType == STARTUP) {
            dragon.triggerAnim(VolitansAnimationHandler.ACTION_CONTROLLER, "enter_burrow");
            playEnterBurrowSound(dragon);
            dragon.setBurrowing(false);
            exitRequested = false;
            applyBurstOnExit = true;
            burstApplied = false;
            lateExitDustSpawned = false;
            enterMoundSpawned = false;
            exitMoundSpawned = false;
            lastGroundChunkPos = null;
            lastParticlePos = null;
            spawnEnterBurrowMoundIfReady(dragon);
            return;
        }
        if (section.sectionType == ACTIVE) {
            dragon.setBurrowing(true);
            return;
        }
        if (section.sectionType == RECOVERY) {
            dragon.setBurrowing(false);
            dragon.triggerAnim(VolitansAnimationHandler.ACTION_CONTROLLER, "burrow_exit");
            playExitBurrowSound(dragon);
            dragon.grantTemporaryInvulnerability(EXIT_TICKS);
            dragon.lockRiderControls(EXIT_TICKS);
            dragon.blockTakeoffAfterBurrowExit(EXIT_TICKS);
            dragon.setGoingUp(false);
            dragon.setGoingDown(false);
            exitMoundSpawned = false;
            spawnExitBurrowMoundIfReady(dragon);
        }
    }

    @Override
    protected void endSection(DragonAbilitySection section) {
        if (section != null && (section.sectionType == ACTIVE || section.sectionType == RECOVERY)) {
            Volitans dragon = getUser();
            dragon.setBurrowing(false);
            dragon.setGoingUp(false);
            dragon.setGoingDown(false);
            lateExitDustSpawned = false;
            enterMoundSpawned = false;
            exitMoundSpawned = false;
            lastGroundChunkPos = null;
            lastParticlePos = null;
        }
    }

    @Override
    public void tickUsing() {
        DragonAbilitySection section = getCurrentSection();
        if (section == null) {
            return;
        }
        Volitans dragon = getUser();
        if (section.sectionType == STARTUP) {
            spawnEnterBurrowMoundIfReady(dragon);
            return;
        }
        if (section.sectionType == RECOVERY) {
            spawnExitBurrowMoundIfReady(dragon);
            if (applyBurstOnExit && !burstApplied && getTicksInSection() >= EXIT_BURST_DELAY_TICKS) {
                applyExitBurst(dragon);
                burstApplied = true;
            }
            if (!lateExitDustSpawned && getTicksInSection() >= EXIT_LATE_DUST_DELAY_TICKS) {
                spawnLateExitDust(dragon);
                lateExitDustSpawned = true;
            }
            return;
        }
        if (section.sectionType != ACTIVE) {
            return;
        }

        Vec3 burrowMovement = getBurrowMovementDelta(dragon);
        if (burrowMovement != null) {
            if (getTicksInSection() % GROUND_CHUNK_INTERVAL_TICKS == 0) {
                spawnGroundChunks(dragon, burrowMovement);
            }
            spawnMovementParticles(dragon, burrowMovement);
        }

        // Water cancels burrow phase and exits cleanly without burst damage.
        if (dragon.isInWaterOrBubble() || dragon.isUnderWater()) {
            requestExit(false);
        }
        if (exitRequested) {
            nextSection();
        }
    }

    @Override
    public void interrupt() {
        Volitans dragon = getUser();
        dragon.setBurrowing(false);
        dragon.setGoingUp(false);
        dragon.setGoingDown(false);
        dragon.clearRiderControlLock();
        dragon.blockTakeoffAfterBurrowExit(8);
        lateExitDustSpawned = false;
        enterMoundSpawned = false;
        exitMoundSpawned = false;
        lastGroundChunkPos = null;
        lastParticlePos = null;
        super.interrupt();
    }

    public void requestExit(boolean withBurst) {
        DragonAbilitySection section = getCurrentSection();
        if (section == null || section.sectionType != ACTIVE) {
            return;
        }
        exitRequested = true;
        applyBurstOnExit = withBurst;
    }

    private void applyExitBurst(Volitans dragon) {
        if (dragon.level().isClientSide) {
            return;
        }
        spawnExitBurstVisuals(dragon);
        Vec3 origin = dragon.position();
        AABB hitBox = new AABB(
                origin.x - EXIT_RADIUS,
                origin.y - EXIT_RADIUS,
                origin.z - EXIT_RADIUS,
                origin.x + EXIT_RADIUS,
                origin.y + EXIT_RADIUS,
                origin.z + EXIT_RADIUS
        );
        DamageSource source = dragon.level().damageSources().mobAttack(dragon);
        float damage = dragon.getConfiguredAbilityDamage("burrow", EXIT_DAMAGE);
        List<LivingEntity> targets = dragon.level().getEntitiesOfClass(
                LivingEntity.class,
                hitBox,
                entity -> entity != dragon
                        && entity.isAlive()
                        && entity.attackable()
                        && !dragon.isAlly(entity)
                        && entity.distanceToSqr(dragon) <= EXIT_RADIUS * EXIT_RADIUS
        );
        for (LivingEntity target : targets) {
            target.hurt(source, damage);
            Vec3 motion = target.getDeltaMovement();
            target.setDeltaMovement(motion.x * 0.35D, Math.max(motion.y, EXIT_UPWARD_KNOCK), motion.z * 0.35D);
            target.hurtMarked = true;
        }
    }

    private void spawnExitBurstVisuals(Volitans dragon) {
        if (!(dragon.level() instanceof ServerLevel server)) {
            return;
        }
        BlockPos groundPos = findGroundPos(dragon);
        BlockState state = normalizeBurrowBlock(server.getBlockState(groundPos));
        Vec3 origin = VolitansGroundChunkEntity.surfacePosition(groundPos);
        RandomSource random = dragon.getRandom();

        spawnExitDustColumn(server, origin, random);
        spawnExitBlockParticles(server, origin, state, random);
        spawnExitBlockParticleTrails(server, origin, state, random);
        spawnExitFallingBlocks(server, origin, state, random);
    }

    private void spawnExitDustColumn(ServerLevel server, Vec3 origin, RandomSource random) {
        for (int step = 0; step < EXIT_DUST_COLUMN_STEPS; step++) {
            double progress = step / (double) Math.max(EXIT_DUST_COLUMN_STEPS - 1, 1);
            double y = origin.y + progress * EXIT_DUST_COLUMN_HEIGHT;
            double radius = 0.18D + progress * 0.75D;
            for (int i = 0; i < EXIT_DUST_COLUMN_PARTICLES_PER_STEP; i++) {
                double angle = random.nextDouble() * Math.PI * 2.0D;
                double distance = random.nextDouble() * radius;
                double x = origin.x + Math.cos(angle) * distance;
                double z = origin.z + Math.sin(angle) * distance;
                double ySpeed = EXIT_DUST_COLUMN_SPEED + random.nextDouble() * 0.18D;
                server.sendParticles(ModParticles.DRAGON_DUST.get(),
                        x, y, z, 0,
                        Math.cos(angle) * 0.035D, ySpeed, Math.sin(angle) * 0.035D,
                        1.0D);
            }
        }
    }

    private void spawnLateExitDust(Volitans dragon) {
        if (!(dragon.level() instanceof ServerLevel server)) {
            return;
        }
        BlockPos groundPos = findGroundPos(dragon);
        Vec3 origin = VolitansGroundChunkEntity.surfacePosition(groundPos);
        RandomSource random = dragon.getRandom();
        for (int i = 0; i < EXIT_LATE_DUST_COUNT; i++) {
            double angle = random.nextDouble() * Math.PI * 2.0D;
            double distance = random.nextDouble() * EXIT_LATE_DUST_RADIUS;
            double x = origin.x + Math.cos(angle) * distance;
            double z = origin.z + Math.sin(angle) * distance;
            double y = origin.y + 0.08D + random.nextDouble() * 0.28D;
            double speed = 0.04D + random.nextDouble() * EXIT_LATE_DUST_SPEED;
            server.sendParticles(ModParticles.DRAGON_DUST.get(),
                    x, y, z,
                    0,
                    Math.cos(angle) * speed,
                    0.04D + random.nextDouble() * 0.12D,
                    Math.sin(angle) * speed,
                    1.0D);
        }
    }

    private void spawnExitBlockParticles(ServerLevel server, Vec3 origin, BlockState state, RandomSource random) {
        for (int i = 0; i < EXIT_BLOCK_PARTICLE_COUNT; i++) {
            double angle = random.nextDouble() * Math.PI * 2.0D;
            double distance = random.nextDouble() * 2.8D;
            double x = origin.x + Math.cos(angle) * distance;
            double z = origin.z + Math.sin(angle) * distance;
            double speed = 0.12D + random.nextDouble() * 0.24D;
            server.sendParticles(new BlockParticleOption(ParticleTypes.BLOCK, state),
                    x, origin.y + 0.12D + random.nextDouble() * 0.35D, z,
                    0,
                    Math.cos(angle) * speed,
                    0.03D + random.nextDouble() * 0.05D,
                    Math.sin(angle) * speed,
                    1.0D);
        }
    }

    private void spawnExitBlockParticleTrails(ServerLevel server, Vec3 origin, BlockState state, RandomSource random) {
        BlockParticleOption particle = new BlockParticleOption(ParticleTypes.BLOCK, state);
        for (int i = 0; i < EXIT_BLOCK_TRAIL_PARTICLE_COUNT; i++) {
            double angle = random.nextDouble() * Math.PI * 2.0D;
            double distance = 0.35D + random.nextDouble() * 5.2D;
            double sideJitter = (random.nextDouble() - 0.5D) * 0.55D;
            double sideZ = Math.cos(angle);
            double dirZ = Math.sin(angle);
            double sideX = -dirZ;
            double x = origin.x + sideZ * distance + sideX * sideJitter;
            double z = origin.z + dirZ * distance + sideZ * sideJitter;
            double y = origin.y + 0.04D + random.nextDouble() * 0.14D;
            double speed = 0.02D + random.nextDouble() * 0.07D;
            server.sendParticles(particle,
                    x, y, z,
                    0,
                    sideZ * speed,
                    0.015D + random.nextDouble() * 0.035D,
                    dirZ * speed,
                    1.0D);
        }
    }

    private void spawnExitFallingBlocks(ServerLevel server, Vec3 origin, BlockState state, RandomSource random) {
        if (state.isAir() || state.liquid() || state.is(Blocks.BEDROCK)) {
            return;
        }
        for (int i = 0; i < EXIT_FALLING_BLOCK_COUNT; i++) {
            double angle = random.nextDouble() * Math.PI * 2.0D;
            double radius = 0.6D + random.nextDouble() * EXIT_FALLING_BLOCK_RADIUS;
            double x = origin.x + Math.cos(angle) * radius;
            double z = origin.z + Math.sin(angle) * radius;
            BlockPos samplePos = findGroundPos(server, x, origin.y, z);
            BlockState sampledState = normalizeBurrowBlock(server.getBlockState(samplePos));
            if (sampledState.isAir() || sampledState.liquid() || sampledState.is(Blocks.BEDROCK)) {
                sampledState = state;
            }

            VisualFallingBlockEntity block = new VisualFallingBlockEntity(
                    ModEntities.VISUAL_FALLING_BLOCK.get(),
                    server,
                    samplePos.getX() + 0.5D,
                    samplePos.getY() + 1.05D,
                    samplePos.getZ() + 0.5D,
                    sampledState,
                    EXIT_FALLING_BLOCK_LIFETIME
            );
            double speed = EXIT_FALLING_BLOCK_SPEED_MIN
                    + random.nextDouble() * (EXIT_FALLING_BLOCK_SPEED_MAX - EXIT_FALLING_BLOCK_SPEED_MIN);
            double upSpeed = EXIT_FALLING_BLOCK_UP_SPEED_MIN
                    + random.nextDouble() * (EXIT_FALLING_BLOCK_UP_SPEED_MAX - EXIT_FALLING_BLOCK_UP_SPEED_MIN);
            block.setNoGravity(false);
            block.setDeltaMovement(Math.cos(angle) * speed, upSpeed, Math.sin(angle) * speed);
            block.hasImpulse = true;
            server.addFreshEntity(block);
        }
    }

    private void spawnGroundChunks(Volitans dragon, Vec3 movement) {
        if (!(dragon.level() instanceof ServerLevel server)) {
            return;
        }

        BlockPos groundPos = findGroundPos(dragon);
        Vec3 spawnPos = VolitansGroundChunkEntity.surfacePosition(groundPos);
        if (lastGroundChunkPos != null && lastGroundChunkPos.distanceToSqr(spawnPos) < GROUND_CHUNK_MIN_DISTANCE_SQR) {
            return;
        }
        BlockState state = normalizeBurrowBlock(server.getBlockState(groundPos));
        float yaw = getGroundChunkYaw(dragon, movement);
        VolitansGroundChunkEntity chunks = new VolitansGroundChunkEntity(
                server,
                spawnPos,
                yaw,
                state
        );
        server.addFreshEntity(chunks);
        lastGroundChunkPos = spawnPos;
    }

    private void spawnBurrowMound(Volitans dragon) {
        if (!(dragon.level() instanceof ServerLevel server)) {
            return;
        }
        BlockPos groundPos = findGroundPos(dragon);
        Vec3 spawnPos = VolitansBurrowMoundEntity.surfacePosition(groundPos);
        BlockState state = normalizeBurrowBlock(server.getBlockState(groundPos));
        server.addFreshEntity(new VolitansBurrowMoundEntity(server, spawnPos, dragon.yBodyRot, state));
    }

    private void spawnEnterBurrowMoundIfReady(Volitans dragon) {
        if (enterMoundSpawned || getTicksInSection() < ENTER_BURROW_MOUND_DELAY_TICKS) {
            return;
        }
        spawnBurrowMound(dragon);
        enterMoundSpawned = true;
    }

    private void spawnExitBurrowMoundIfReady(Volitans dragon) {
        if (exitMoundSpawned || getTicksInSection() < EXIT_BURROW_MOUND_DELAY_TICKS) {
            return;
        }
        spawnBurrowMound(dragon);
        exitMoundSpawned = true;
    }

    private Vec3 getBurrowMovementDelta(Volitans dragon) {
        Vec3 currentPos = dragon.position();
        if (lastParticlePos == null) {
            lastParticlePos = currentPos;
            return null;
        }
        Vec3 movement = currentPos.subtract(lastParticlePos);
        lastParticlePos = currentPos;
        Vec3 horizontal = new Vec3(movement.x, 0.0D, movement.z);
        if (horizontal.lengthSqr() < BURROW_MOVEMENT_PARTICLE_SPEED_SQR) {
            return null;
        }
        return horizontal;
    }

    private void spawnMovementParticles(Volitans dragon, Vec3 horizontal) {
        if (!(dragon.level() instanceof ServerLevel server)) {
            return;
        }
        double movementSqr = horizontal.lengthSqr();

        BlockPos groundPos = findGroundPos(dragon);
        BlockState state = normalizeBurrowBlock(server.getBlockState(groundPos));
        Vec3 center = VolitansGroundChunkEntity.surfacePosition(groundPos);
        Vec3 forward = horizontal.normalize();
        Vec3 side = new Vec3(-forward.z, 0.0D, forward.x);
        double speed = Math.min(0.35D, Math.sqrt(movementSqr) * 0.08D);

        for (int i = 0; i < 12; i++) {
            double sideOffset = (server.random.nextDouble() - 0.5D) * 4.8D;
            double backOffset = server.random.nextDouble() * -3.2D;
            double x = center.x + side.x * sideOffset + forward.x * backOffset;
            double z = center.z + side.z * sideOffset + forward.z * backOffset;
            double y = center.y + 0.08D + server.random.nextDouble() * 0.2D;
            double vx = -forward.x * speed + side.x * (server.random.nextDouble() - 0.5D) * 0.2D;
            double vz = -forward.z * speed + side.z * (server.random.nextDouble() - 0.5D) * 0.2D;
            server.sendParticles(new BlockParticleOption(ParticleTypes.BLOCK, state),
                    x, y, z, 1, vx, 0.08D + server.random.nextDouble() * 0.14D, vz, 0.0D);
        }

        server.sendParticles(ModParticles.DRAGON_DUST.get(),
                center.x, center.y + 0.08D, center.z,
                8, 2.1D, 0.12D, 2.1D, 0.03D);
    }

    private float getGroundChunkYaw(Volitans dragon, Vec3 movement) {
        if (movement.lengthSqr() > 1.0E-6D) {
            return (float) (Math.toDegrees(Math.atan2(movement.z, movement.x)) - 90.0D);
        }
        return dragon.yBodyRot;
    }

    private BlockPos findGroundPos(Volitans dragon) {
        BlockPos origin = BlockPos.containing(dragon.getX(), dragon.getBoundingBox().minY - 0.05D, dragon.getZ());
        for (int offset = 0; offset <= 4; offset++) {
            BlockPos pos = origin.below(offset);
            BlockState state = dragon.level().getBlockState(pos);
            if (!state.isAir() && state.getFluidState().isEmpty()) {
                return pos;
            }
        }
        return origin.below();
    }

    private BlockPos findGroundPos(ServerLevel server, double x, double y, double z) {
        BlockPos origin = BlockPos.containing(x, y + 0.35D, z);
        for (int offset = 0; offset <= 5; offset++) {
            BlockPos pos = origin.below(offset);
            BlockState state = server.getBlockState(pos);
            if (!state.isAir() && state.getFluidState().isEmpty()) {
                return pos;
            }
        }
        return origin.below();
    }

    private BlockState normalizeBurrowBlock(BlockState state) {
        if (state.isAir() || !state.getFluidState().isEmpty()) {
            return Blocks.DIRT.defaultBlockState();
        }
        if (state.is(Blocks.GRASS_BLOCK) || state.is(Blocks.DIRT_PATH) || state.is(Blocks.PODZOL) || state.is(Blocks.MYCELIUM)) {
            return Blocks.DIRT.defaultBlockState();
        }
        if (state.is(Blocks.SAND)) {
            return Blocks.SANDSTONE.defaultBlockState();
        }
        if (state.is(Blocks.RED_SAND)) {
            return Blocks.RED_SANDSTONE.defaultBlockState();
        }
        if (state.is(Blocks.SOUL_SAND)) {
            return Blocks.SOUL_SOIL.defaultBlockState();
        }
        return state;
    }

    private void playEnterBurrowSound(Volitans dragon) {
        if (dragon.level().isClientSide) {
            return;
        }
        float pitch = 0.96f + dragon.getRandom().nextFloat() * 0.08f;
        dragon.getSoundHandler().playMovingEntitySound(
                ModSounds.VOLITANS_ENTER_BURROW.get(),
                2.0f,
                pitch,
                ENTER_BURROW_SOUND_TICKS
        );
    }

    private void playExitBurrowSound(Volitans dragon) {
        if (dragon.level().isClientSide) {
            return;
        }
        float pitch = 0.96f + dragon.getRandom().nextFloat() * 0.08f;
        dragon.getSoundHandler().playMovingEntitySound(
                ModSounds.VOLITANS_BURROW_EXIT.get(),
                2.0f,
                pitch,
                EXIT_BURROW_SOUND_TICKS
        );
    }
}

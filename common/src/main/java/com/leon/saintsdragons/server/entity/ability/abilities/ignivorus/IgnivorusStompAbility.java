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
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

import static com.leon.saintsdragons.server.entity.ability.DragonAbilitySection.AbilitySectionDuration;
import static com.leon.saintsdragons.server.entity.ability.DragonAbilitySection.AbilitySectionType.ACTIVE;
import static com.leon.saintsdragons.server.entity.ability.DragonAbilitySection.AbilitySectionType.RECOVERY;
import static com.leon.saintsdragons.server.entity.ability.DragonAbilitySection.AbilitySectionType.STARTUP;


public class IgnivorusStompAbility extends DragonAbility<Ignivorus> {
    private static final float DEFAULT_DAMAGE = 18.0f;
    private static final double AOE_RADIUS = 18.0;
    private static final double UPWARD_FORCE = 0.75;

    private static final DragonAbilitySection[] TRACK = new DragonAbilitySection[]{
            new AbilitySectionDuration(STARTUP, 20),
            new AbilitySectionDuration(ACTIVE, 2),
            new AbilitySectionDuration(RECOVERY, 7)
    };

    private boolean appliedHit;
    private final List<VisualFallingBlockEntity> spawnedBlocks = new ArrayList<>();
    private int blockEffectTicks = 0;

    public IgnivorusStompAbility(DragonAbilityType<Ignivorus, IgnivorusStompAbility> type,
                                 Ignivorus user) {
        super(type, user, TRACK, 6);
    }

    @Override
    public boolean tryAbility() {

        return getUser().isPhase2Active() && !getUser().isFlying();
    }

    @Override
    protected void beginSection(DragonAbilitySection section) {
        if (section == null) {
            return;
        }

        if (section.sectionType == STARTUP) {
            Ignivorus dragon = getUser();
            dragon.lockRiderControls(29);
            boolean useRight = dragon.shouldUseRightWingSwipe();
            String animationName = useRight ? "stomp_right" : "stomp_left";
            dragon.triggerAnim(IgnivorusAnimationHandler.FAST_ACTION_CONTROLLER, animationName);
            if (!dragon.level().isClientSide) {
                dragon.getSoundHandler().playMovingEntitySound(ModSounds.IGNIVORUS_STOMP.get(), 1.0f, 1.0f, 68);
            }
            dragon.toggleWingSwipeSide();
            appliedHit = false;
            spawnedBlocks.clear();
        }
    }

    @Override
    public void tickUsing() {
        DragonAbilitySection section = getCurrentSection();
        if (section == null) {
            return;
        }

        if (section.sectionType == ACTIVE && !appliedHit) {
            Ignivorus dragon = getUser();
            List<LivingEntity> targets = selectTargets();
            for (LivingEntity target : targets) {
                applyHit(dragon, target);
            }
            spawnBlockLiftEffect(dragon);
            spawnDirtParticles(dragon);
            blockEffectTicks = 0;

            appliedHit = true;
        }
        if (!spawnedBlocks.isEmpty()) {
            blockEffectTicks++;
            if (blockEffectTicks >= 100) {
                spawnedBlocks.clear();
            }
        }
    }

    private void applyHit(Ignivorus dragon, LivingEntity target) {
        DamageSource physicalSource = dragon.level().damageSources().mobAttack(dragon);
        target.hurt(physicalSource, resolveDamage() * dragon.getHungerMeleeDamageMultiplier());
        target.push(0, UPWARD_FORCE, 0);
        target.hurtMarked = true; // Force velocity sync to client
    }

    private float resolveDamage() {
        return (float) DragonAttributeConfigLoader.getInstance()
                .getConfig(DragonAttributeConfigLoader.IGNIVORUS_ID)
                .abilityDamage("stomp", DEFAULT_DAMAGE);
    }

    private List<LivingEntity> selectTargets() {
        Ignivorus dragon = getUser();
        Vec3 dragonPos = dragon.position();
        AABB detectionBox = new AABB(dragonPos, dragonPos).inflate(AOE_RADIUS);
        List<LivingEntity> candidates = dragon.level().getEntitiesOfClass(LivingEntity.class, detectionBox,
                entity -> {
                    if (entity == dragon || !entity.isAlive() || !entity.attackable() || dragon.isAlly(entity)) {
                        return false;
                    }

                    Vec3 entityCenter = entity.getBoundingBox().getCenter();
                    double distSqr = entityCenter.distanceToSqr(dragonPos);
                    return distSqr <= (AOE_RADIUS * AOE_RADIUS);
                });
        candidates.sort(Comparator.comparingDouble(e ->
                e.getBoundingBox().getCenter().distanceToSqr(dragonPos)
        ));

        return candidates;
    }


    private void spawnBlockLiftEffect(Ignivorus dragon) {
        RandomSource random = dragon.getRandom();
        BlockPos dragonBlockPos = dragon.blockPosition();
        List<BlockPos> blockPositions = new ArrayList<>();
        addRingPositions(blockPositions, dragonBlockPos, 12, 16, random, 20);
        addRingPositions(blockPositions, dragonBlockPos, 8, 11, random, 15);
        addRingPositions(blockPositions, dragonBlockPos, 4, 7, random, 10);

        for (BlockPos pos : blockPositions) {
            spawnFallingBlockAt(dragon, pos, random);
        }
    }

    private void spawnDirtParticles(Ignivorus dragon) {
        if (!(dragon.level() instanceof ServerLevel serverLevel)) {
            return;
        }
        RandomSource random = dragon.getRandom();
        Vec3 dragonPos = dragon.position();
        spawnParticleRing(serverLevel, dragonPos, 4, 8, 30, random);
        spawnParticleRing(serverLevel, dragonPos, 8, 12, 50, random);
        spawnParticleRing(serverLevel, dragonPos, 12, 18, 70, random);
    }


    private void spawnParticleRing(ServerLevel level, Vec3 center, int minRadius, int maxRadius, int count, RandomSource random) {
        BlockParticleOption dirtParticles = new BlockParticleOption(ParticleTypes.BLOCK, Blocks.DIRT.defaultBlockState());

        for (int i = 0; i < count; i++) {
            double angle = random.nextDouble() * Math.PI * 2;
            double radius = minRadius + random.nextDouble() * (maxRadius - minRadius);
            double x = center.x + Math.cos(angle) * radius;
            double z = center.z + Math.sin(angle) * radius;
            BlockPos groundPos = findGroundLevel(getUser(), new BlockPos((int) x, (int) center.y, (int) z));
            if (groundPos == null) {
                continue;
            }

            BlockState groundState = level.getBlockState(groundPos);
            if (groundState.isAir() || groundState.liquid()) {
                continue;
            }
            double particleY = groundPos.getY() + 1.02;

            int burstCount = 6;
            for (int j = 0; j < burstCount; j++) {
                double velX = (random.nextDouble() - 0.5) * 0.9;
                double velY = 0.25 + random.nextDouble() * 0.85;
                double velZ = (random.nextDouble() - 0.5) * 0.9;
                level.sendParticles(dirtParticles, x, particleY, z, 0, velX, velY, velZ, 1.0);
            }
        }
    }


    private void addRingPositions(List<BlockPos> positions, BlockPos center, int minRadius, int maxRadius, RandomSource random, int count) {
        for (int i = 0; i < count; i++) {
            double angle = random.nextDouble() * Math.PI * 2;
            double radius = minRadius + random.nextDouble() * (maxRadius - minRadius);
            int xOffset = (int) Math.round(Math.cos(angle) * radius);
            int zOffset = (int) Math.round(Math.sin(angle) * radius);
            BlockPos targetPos = center.offset(xOffset, 0, zOffset);
            positions.add(targetPos);
        }
    }


    private void spawnFallingBlockAt(Ignivorus dragon, BlockPos pos, RandomSource random) {
        BlockPos groundPos = findGroundLevel(dragon, pos);
        if (groundPos == null) {
            return;
        }
        BlockState groundState = dragon.level().getBlockState(groundPos);
        if (groundState.isAir() || groundState.liquid() || groundState.is(Blocks.BEDROCK)) {
            return;
        }
        double startX = groundPos.getX() + 0.5;
        double startY = groundPos.getY() + 0.5;
        double startZ = groundPos.getZ() + 0.5;
        VisualFallingBlockEntity fallingBlock = new VisualFallingBlockEntity(
                ModEntities.VISUAL_FALLING_BLOCK.get(),
                dragon.level(),
                startX,
                startY,
                startZ,
                groundState,
                200
        );

        double upwardVelocity = 0.5 + random.nextDouble() * 0.7;
        fallingBlock.setDeltaMovement(0, upwardVelocity, 0);
        dragon.level().addFreshEntity(fallingBlock);
        spawnedBlocks.add(fallingBlock);
    }

    private BlockPos findGroundLevel(Ignivorus dragon, BlockPos startPos) {
        int dragonY = dragon.blockPosition().getY();

        for (int y = dragonY; y > dragon.level().getMinBuildHeight(); y--) {
            BlockPos checkPos = new BlockPos(startPos.getX(), y, startPos.getZ());
            BlockState state = dragon.level().getBlockState(checkPos);
            if (!state.isAir() && !state.liquid() && state.isSolidRender(dragon.level(), checkPos)) {
                return checkPos;
            }
        }

        return null;
    }
}

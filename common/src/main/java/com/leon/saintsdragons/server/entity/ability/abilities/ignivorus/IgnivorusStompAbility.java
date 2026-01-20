package com.leon.saintsdragons.server.entity.ability.abilities.ignivorus;

import com.leon.saintsdragons.common.config.dragon.DragonAttributeConfigLoader;
import com.leon.saintsdragons.common.registry.ModEntities;
import com.leon.saintsdragons.server.entity.ability.DragonAbility;
import com.leon.saintsdragons.server.entity.ability.DragonAbilitySection;
import com.leon.saintsdragons.server.entity.ability.DragonAbilityType;
import com.leon.saintsdragons.server.entity.dragons.ignivorus.Ignivorus;
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

/**
 * Ground stomp attack for Ignivorus Phase 2.
 * Deals heavy damage in a sphere around the dragon.
 * Alternates between left and right foot stomp.
 */
public class IgnivorusStompAbility extends DragonAbility<Ignivorus> {
    // Heavy stomp damage
    private static final float DEFAULT_DAMAGE = 18.0f;

    // Broad sphere radius for stomp damage
    private static final double AOE_RADIUS = 18.0;

    // Strong upward launch force from stomp
    private static final double UPWARD_FORCE = 0.75;

    // Animation timing: 1.46 seconds = 29 ticks total
    // Damage lands at 1.0 seconds (20 ticks)
    private static final DragonAbilitySection[] TRACK = new DragonAbilitySection[] {
            new AbilitySectionDuration(STARTUP, 20),   // Windup (1.0s - damage lands here)
            new AbilitySectionDuration(ACTIVE, 2),     // Hit window (0.1s)
            new AbilitySectionDuration(RECOVERY, 7)    // Recovery (0.35s)
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
        // Stomp only works in Phase 2 while grounded
        // When flying, falls back to bite attack
        return getUser().isPhase2Active() && !getUser().isFlying();
    }

    @Override
    protected void beginSection(DragonAbilitySection section) {
        if (section == null) {
            return;
        }

        if (section.sectionType == STARTUP) {
            Ignivorus dragon = getUser();

            // Lock controls for the full animation duration (1.46 seconds = 29 ticks)
            dragon.lockRiderControls(29);

            // Alternate between left and right stomp
            boolean useRight = dragon.shouldUseRightWingSwipe();
            String animationName = useRight ? "stomp_right" : "stomp_left";

            // Trigger stomp animation via GeckoLib action controller
            dragon.triggerAnim("action", animationName);

            // Toggle for next time
            dragon.toggleWingSwipeSide();

            appliedHit = false;
            spawnedBlocks.clear(); // Clear any leftover blocks from previous use
        }
    }

    @Override
    public void tickUsing() {
        DragonAbilitySection section = getCurrentSection();
        if (section == null) {
            return;
        }

        // Apply damage during ACTIVE window (hit frame at 1 second)
        if (section.sectionType == ACTIVE && !appliedHit) {
            Ignivorus dragon = getUser();

            List<LivingEntity> targets = selectTargets();

            // Apply stomp damage to all valid targets
            for (LivingEntity target : targets) {
                applyHit(dragon, target);
            }

            // Spawn visual effects - launch blocks into the air and particles
            spawnBlockLiftEffect(dragon);
            spawnDirtParticles(dragon);
            blockEffectTicks = 0; // Start tracking block lifetime

            appliedHit = true;
        }
        if (!spawnedBlocks.isEmpty()) {
            blockEffectTicks++;
            if (blockEffectTicks >= 100) {
                // Just clear the list - the entities will despawn on their own
                spawnedBlocks.clear();
            }
        }
    }

    private void applyHit(Ignivorus dragon, LivingEntity target) {
        // Apply damage as a direct melee hit
        DamageSource physicalSource = dragon.level().damageSources().mobAttack(dragon);
        target.hurt(physicalSource, resolveDamage());

        // Launch enemies upward instead of away - stomp creates an upward shockwave
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

        // Get dragon center position at ground level for stomp
        Vec3 dragonPos = dragon.position();

        // Create sphere detection area around the dragon
        AABB detectionBox = new AABB(dragonPos, dragonPos).inflate(AOE_RADIUS);

        // Find all targets in sphere radius
        List<LivingEntity> candidates = dragon.level().getEntitiesOfClass(LivingEntity.class, detectionBox,
                entity -> {
                    if (entity == dragon || !entity.isAlive() || !entity.attackable() || dragon.isAlly(entity)) {
                        return false;
                    }

                    Vec3 entityCenter = entity.getBoundingBox().getCenter();

                    // Check if within sphere radius
                    double distSqr = entityCenter.distanceToSqr(dragonPos);
                    return distSqr <= (AOE_RADIUS * AOE_RADIUS);
                });

        // Sort by distance (closest first) for consistent behavior
        candidates.sort(Comparator.comparingDouble(e ->
            e.getBoundingBox().getCenter().distanceToSqr(dragonPos)
        ));

        return candidates;
    }

    /**
     * Spawns falling blocks in a ring pattern that get lifted up by the stomp
     */
    private void spawnBlockLiftEffect(Ignivorus dragon) {
        RandomSource random = dragon.getRandom();
        BlockPos dragonBlockPos = dragon.blockPosition();

        // Collect block positions in rings around the dragon
        List<BlockPos> blockPositions = new ArrayList<>();

        // Outer ring - radius of 12-16 blocks
        addRingPositions(blockPositions, dragonBlockPos, 12, 16, random, 20); // ~20 blocks in outer ring

        // Middle ring - radius of 8-11 blocks
        addRingPositions(blockPositions, dragonBlockPos, 8, 11, random, 15); // ~15 blocks in middle ring

        // Inner ring - radius of 4-7 blocks
        addRingPositions(blockPositions, dragonBlockPos, 4, 7, random, 10); // ~10 blocks in inner ring

        // For each position, spawn a falling block
        for (BlockPos pos : blockPositions) {
            spawnFallingBlockAt(dragon, pos, random);
        }
    }

    /**
     * Spawns dirt/dust particles in expanding rings around the stomp impact
     */
    private void spawnDirtParticles(Ignivorus dragon) {
        if (!(dragon.level() instanceof ServerLevel serverLevel)) {
            return;
        }

        RandomSource random = dragon.getRandom();
        Vec3 dragonPos = dragon.position();

        // Spawn particles in expanding rings for a shockwave effect
        // More particles in outer rings for dramatic effect

        // Inner ring - 4-8 block radius, 30 particles
        spawnParticleRing(serverLevel, dragonPos, 4, 8, 30, random);

        // Middle ring - 8-12 block radius, 50 particles
        spawnParticleRing(serverLevel, dragonPos, 8, 12, 50, random);

        // Outer ring - 12-18 block radius, 70 particles
        spawnParticleRing(serverLevel, dragonPos, 12, 18, 70, random);
    }

    /**
     * Spawns a ring of dirt particles at ground level.
     */
    private void spawnParticleRing(ServerLevel level, Vec3 center, int minRadius, int maxRadius, int count, RandomSource random) {
        BlockParticleOption dirtParticles = new BlockParticleOption(ParticleTypes.BLOCK, Blocks.DIRT.defaultBlockState());

        for (int i = 0; i < count; i++) {
            // Random angle around the circle
            double angle = random.nextDouble() * Math.PI * 2;

            // Random radius within the ring
            double radius = minRadius + random.nextDouble() * (maxRadius - minRadius);

            // Calculate position
            double x = center.x + Math.cos(angle) * radius;
            double z = center.z + Math.sin(angle) * radius;

            // Find ground level at this position
            BlockPos groundPos = findGroundLevel(getUser(), new BlockPos((int)x, (int)center.y, (int)z));
            if (groundPos == null) {
                continue;
            }

            BlockState groundState = level.getBlockState(groundPos);
            if (groundState.isAir() || groundState.liquid()) {
                continue;
            }

            // Spawn slightly above ground so it doesn't clip into the block top face.
            double particleY = groundPos.getY() + 1.02;

            // Use count=0 so dx/dy/dz are treated as velocity (more visible + directional).
            int burstCount = 6;
            for (int j = 0; j < burstCount; j++) {
                double velX = (random.nextDouble() - 0.5) * 0.9;
                double velY = 0.25 + random.nextDouble() * 0.85;
                double velZ = (random.nextDouble() - 0.5) * 0.9;
                level.sendParticles(dirtParticles, x, particleY, z, 0, velX, velY, velZ, 1.0);
            }
        }
    }

    /**
     * Adds random block positions in a ring pattern
     */
    private void addRingPositions(List<BlockPos> positions, BlockPos center, int minRadius, int maxRadius, RandomSource random, int count) {
        for (int i = 0; i < count; i++) {
            // Random angle around the circle
            double angle = random.nextDouble() * Math.PI * 2;

            // Random radius within the ring
            double radius = minRadius + random.nextDouble() * (maxRadius - minRadius);

            // Calculate x and z offset
            int xOffset = (int) Math.round(Math.cos(angle) * radius);
            int zOffset = (int) Math.round(Math.sin(angle) * radius);

            // Find the surface block at this position
            BlockPos targetPos = center.offset(xOffset, 0, zOffset);
            positions.add(targetPos);
        }
    }

    /**
     * Spawns a falling block at the given position with upward velocity
     * This creates a VISUAL COPY of the ground block without removing it from the world
     */
    private void spawnFallingBlockAt(Ignivorus dragon, BlockPos pos, RandomSource random) {
        // Find the ground level at this position
        BlockPos groundPos = findGroundLevel(dragon, pos);
        if (groundPos == null) {
            return; // No valid ground found
        }

        BlockState groundState = dragon.level().getBlockState(groundPos);

        // Skip air, liquids, and bedrock
        if (groundState.isAir() || groundState.liquid() || groundState.is(Blocks.BEDROCK)) {
            return;
        }

        // Position it at ground level (centered on block)
        double startX = groundPos.getX() + 0.5;
        double startY = groundPos.getY() + 0.5; // Half a block above ground so it's clearly airborne
        double startZ = groundPos.getZ() + 0.5;

        // Create our custom visual falling block entity
        // This doesn't remove or modify any blocks in the world
        VisualFallingBlockEntity fallingBlock = new VisualFallingBlockEntity(
            ModEntities.VISUAL_FALLING_BLOCK.get(),
            dragon.level(),
            startX,
            startY,
            startZ,
            groundState,
            200
        );

        // Give it strong upward velocity to overcome gravity
        // Gravity applies -0.04 per tick, so we need stronger upward force
        double upwardVelocity = 0.5 + random.nextDouble() * 0.7; // 0.5 to 1.2 blocks/tick
        fallingBlock.setDeltaMovement(0, upwardVelocity, 0);

        // Spawn the entity
        dragon.level().addFreshEntity(fallingBlock);

        // Track it so we can remove it later
        spawnedBlocks.add(fallingBlock);
    }

    /**
     * Finds the ground level at a given XZ position (searches downward from dragon height)
     */
    private BlockPos findGroundLevel(Ignivorus dragon, BlockPos startPos) {
        int dragonY = dragon.blockPosition().getY();

        // Search downward from dragon's Y position
        for (int y = dragonY; y > dragon.level().getMinBuildHeight(); y--) {
            BlockPos checkPos = new BlockPos(startPos.getX(), y, startPos.getZ());
            BlockState state = dragon.level().getBlockState(checkPos);

            // Found solid ground
            if (!state.isAir() && !state.liquid() && state.isSolidRender(dragon.level(), checkPos)) {
                return checkPos;
            }
        }

        return null; // No ground found
    }
}

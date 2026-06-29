package com.leon.saintsdragons.server.entity.ability.abilities.stegonaut;

import com.leon.saintsdragons.common.config.dragon.DragonAttributeConfigLoader;
import com.leon.saintsdragons.common.registry.ModParticles;
import com.leon.saintsdragons.common.registry.ModSounds;
import com.leon.saintsdragons.server.entity.ability.DragonAbility;
import com.leon.saintsdragons.server.entity.ability.DragonAbilitySection;
import com.leon.saintsdragons.server.entity.ability.DragonAbilityType;
import com.leon.saintsdragons.server.entity.dragons.stegonaut.Stegonaut;
import com.leon.saintsdragons.server.entity.dragons.stegonaut.handlers.StegonautAnimationHandler;
import com.leon.saintsdragons.server.entity.effect.stegonaut.StegonautAmethystPillarEntity;
import com.leon.saintsdragons.server.entity.effect.GroundCrackEntity;
import com.leon.saintsdragons.server.entity.effect.ImpactRingEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.BlockParticleOption;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import java.util.List;

import static com.leon.saintsdragons.server.entity.ability.DragonAbilitySection.AbilitySectionInfinite;
import static com.leon.saintsdragons.server.entity.ability.DragonAbilitySection.AbilitySectionType.ACTIVE;

public class StegonautGroundSlamAbility extends DragonAbility<Stegonaut> {
    private static final DragonAbilitySection[] TRACK = new DragonAbilitySection[] {
            new AbilitySectionInfinite(ACTIVE)
    };

    private static final int COOLDOWN_TICKS = 60;
    private static final int FIRST_SLAM_TICKS = (int) Math.round(0.875D * 20.0D) + 5;
    private static final int SECOND_SLAM_TICKS = (int) Math.round(1.25D * 20.0D);
    private static final int FIRST_HIT_TICK = 18;
    private static final int PILLAR_SPAWN_TICK = 25;
    private static final float DEFAULT_SLAM_DAMAGE = 20.0F;
    private static final float DEFAULT_PILLAR_DAMAGE = 10.0F;
    private static final double SLAM_RADIUS = 10.0D;
    private static final double SLAM_VERTICAL = 3.0D;
    private static final double DEFAULT_SLAM_KNOCKBACK = 1.35D;
    private static final float DEFAULT_SLAM2_DAMAGE = 25.0F;
    private static final double SLAM2_RADIUS = 20.0D;
    private static final double SLAM2_VERTICAL = 6.0D;
    private static final double DEFAULT_SLAM2_KNOCKBACK = 1.8D;
    private static final double DEFAULT_PILLAR_KNOCKBACK = 0.9D;
    private static final int PILLARS_PER_AXIS = 4;
    private static final double PILLAR_SPACING = 4.5D;
    private static final float PILLAR_SCALE = 2.0F;
    private static final double DUST_VIEW_DISTANCE = 56.0D;

    private Phase phase = Phase.FIRST_SLAM;
    private int phaseTicks;
    private boolean releaseRequested;
    private boolean firstHitApplied;
    private boolean pillarsSpawned;

    private enum Phase {
        FIRST_SLAM,
        SECOND_SLAM
    }

    public StegonautGroundSlamAbility(DragonAbilityType<Stegonaut, StegonautGroundSlamAbility> type,
                                      Stegonaut user) {
        super(type, user, TRACK, COOLDOWN_TICKS);
    }

    @Override
    public boolean tryAbility() {
        Stegonaut dragon = getUser();
        return dragon.isAlive() && !dragon.isBaby() && !dragon.isInWaterOrBubble();
    }

    @Override
    protected void beginSection(DragonAbilitySection section) {
        if (section == null || section.sectionType != ACTIVE) {
            return;
        }
        phase = Phase.FIRST_SLAM;
        phaseTicks = 0;
        releaseRequested = false;
        firstHitApplied = false;
        pillarsSpawned = false;
        Stegonaut dragon = getUser();
        dragon.lockRiderControls(3);
        dragon.triggerAnim(StegonautAnimationHandler.MOVEMENT_CONTROLLER, "ground_slam");
        playSlamSound(false);
    }

    @Override
    public void tickUsing() {
        Stegonaut dragon = getUser();
        dragon.lockRiderControls(3);

        switch (phase) {
            case FIRST_SLAM -> tickFirstSlam();
            case SECOND_SLAM -> tickSecondSlam();
        }
    }

    public void requestRelease() {
        releaseRequested = true;
    }

    private void tickFirstSlam() {
        phaseTicks++;
        if (!firstHitApplied && phaseTicks >= FIRST_HIT_TICK) {
            applyFirstSlam();
            playImpactEffects(false);
            firstHitApplied = true;
        }
        if (phaseTicks >= FIRST_SLAM_TICKS) {
            if (releaseRequested) {
                end();
            } else {
                beginSecondSlam();
            }
        }
    }

    private void beginSecondSlam() {
        phase = Phase.SECOND_SLAM;
        phaseTicks = 0;
        pillarsSpawned = false;
        getUser().triggerAnim(StegonautAnimationHandler.MOVEMENT_CONTROLLER, "ground_slam2");
        playSlamSound(true);
    }

    private void tickSecondSlam() {
        phaseTicks++;
        if (!pillarsSpawned && phaseTicks >= PILLAR_SPAWN_TICK) {
            applySecondSlam();
            spawnPillars();
            spawnImpactRing();
            spawnGroundCrack();
            playImpactEffects(true);
            pillarsSpawned = true;
        }
        if (phaseTicks >= SECOND_SLAM_TICKS) {
            end();
        }
    }

    private void applyFirstSlam() {
        Stegonaut dragon = getUser();
        if (!(dragon.level() instanceof ServerLevel server)) {
            return;
        }

        Entity rider = dragon.getControllingPassenger();
        AABB area = dragon.getBoundingBox().inflate(SLAM_RADIUS, SLAM_VERTICAL, SLAM_RADIUS);
        List<LivingEntity> targets = server.getEntitiesOfClass(
                LivingEntity.class,
                area,
                target -> target != dragon && target != rider && target.isAlive() && target.attackable() && !dragon.isAlly(target)
        );
        float damage = resolveSlamDamage() * dragon.getHungerMeleeDamageMultiplier();
        double knockback = resolveSlamKnockback();
        for (LivingEntity target : targets) {
            if (target.distanceToSqr(dragon) > SLAM_RADIUS * SLAM_RADIUS) {
                continue;
            }
            target.hurt(server.damageSources().mobAttack(dragon), damage);
            Vec3 direction = target.position().subtract(dragon.position());
            direction = new Vec3(direction.x, 0.0D, direction.z);
            if (direction.lengthSqr() < 1.0E-4D) {
                direction = dragon.getLookAngle();
            }
            Vec3 push = direction.normalize().scale(knockback);
            target.push(push.x, 0.38D, push.z);
            target.hurtMarked = true;
        }
    }

    private void spawnPillars() {
        Stegonaut dragon = getUser();
        if (!(dragon.level() instanceof ServerLevel server)) {
            return;
        }

        float damage = resolvePillarDamage() * dragon.getHungerMeleeDamageMultiplier();
        double knockback = resolvePillarKnockback();
        Vec3 forward = dragon.getLookAngle();
        forward = new Vec3(forward.x, 0.0D, forward.z);
        if (forward.lengthSqr() < 1.0E-4D) {
            forward = Vec3.directionFromRotation(0.0F, dragon.getYRot());
            forward = new Vec3(forward.x, 0.0D, forward.z);
        }
        forward = forward.normalize();
        Vec3 right = new Vec3(-forward.z, 0.0D, forward.x);
        float forwardYaw = (float) Math.toDegrees(Math.atan2(-forward.x, forward.z));
        float rightYaw = (float) Math.toDegrees(Math.atan2(-right.x, right.z));

        for (int step = 1; step <= PILLARS_PER_AXIS; step++) {
            double distance = PILLAR_SPACING * step;
            spawnPillar(server, dragon, forward.scale(distance), forwardYaw, damage, knockback);
            spawnPillar(server, dragon, forward.scale(-distance), forwardYaw + 180.0F, damage, knockback);
            spawnPillar(server, dragon, right.scale(distance), rightYaw, damage, knockback);
            spawnPillar(server, dragon, right.scale(-distance), rightYaw + 180.0F, damage, knockback);
        }
    }

    private void applySecondSlam() {
        Stegonaut dragon = getUser();
        if (!(dragon.level() instanceof ServerLevel server)) {
            return;
        }

        Entity rider = dragon.getControllingPassenger();
        AABB area = dragon.getBoundingBox().inflate(SLAM2_RADIUS, SLAM2_VERTICAL, SLAM2_RADIUS);
        List<LivingEntity> targets = server.getEntitiesOfClass(
                LivingEntity.class,
                area,
                target -> target != dragon && target != rider && target.isAlive() && target.attackable() && !dragon.isAlly(target)
        );
        float damage = resolveSlam2Damage() * dragon.getHungerMeleeDamageMultiplier();
        double knockback = resolveSlam2Knockback();
        double radiusSqr = SLAM2_RADIUS * SLAM2_RADIUS;
        for (LivingEntity target : targets) {
            if (target.distanceToSqr(dragon) > radiusSqr) {
                continue;
            }
            target.hurt(server.damageSources().mobAttack(dragon), damage);
            Vec3 direction = target.position().subtract(dragon.position());
            direction = new Vec3(direction.x, 0.0D, direction.z);
            if (direction.lengthSqr() < 1.0E-4D) {
                direction = dragon.getLookAngle();
            }
            Vec3 push = direction.normalize().scale(knockback);
            target.push(push.x, 0.5D, push.z);
            target.hurtMarked = true;
        }
    }

    private void spawnImpactRing() {
        Stegonaut dragon = getUser();
        if (dragon.level() instanceof ServerLevel server) {
            server.addFreshEntity(new ImpactRingEntity(server, dragon.position().add(0.0D, 0.08D, 0.0D)));
        }
    }

    private void spawnGroundCrack() {
        Stegonaut dragon = getUser();
        if (dragon.level() instanceof ServerLevel server) {
            double y = dragon.getBoundingBox().minY + 0.02D;
            server.addFreshEntity(new GroundCrackEntity(server, new Vec3(dragon.getX(), y, dragon.getZ()), dragon.getYRot()));
        }
    }

    private void spawnPillar(ServerLevel server, Stegonaut dragon, Vec3 offset, float yaw, float damage, double knockback) {
        Vec3 pos = dragon.position().add(offset);
        BlockPos ground = BlockPos.containing(pos.x, dragon.getY() - 0.05D, pos.z);
        StegonautAmethystPillarEntity pillar = new StegonautAmethystPillarEntity(
                server,
                new Vec3(pos.x, ground.getY() + 1.0D, pos.z),
                dragon,
                yaw,
                damage,
                knockback
        );
        pillar.setVisualScale(PILLAR_SCALE);
        server.addFreshEntity(pillar);
    }

    private void playImpactEffects(boolean slam2) {
        Stegonaut dragon = getUser();
        if (!(dragon.level() instanceof ServerLevel server)) {
            return;
        }

        dragon.triggerScreenShake(slam2 ? 1.35F : 0.75F, slam2 ? 10 : 6);
        spawnDustBurst(server, dragon, slam2);
    }

    private void playSlamSound(boolean slam2) {
        Stegonaut dragon = getUser();
        if (!(dragon.level() instanceof ServerLevel server)) {
            return;
        }
        server.playSound(
                null,
                dragon.blockPosition(),
                slam2 ? ModSounds.STEGONAUT_SLAM2.get() : ModSounds.STEGONAUT_SLAM.get(),
                SoundSource.HOSTILE,
                slam2 ? 1.75F : 1.35F,
                dragon.isBaby() ? 1.6F : 1.0F
        );
    }

    private void spawnDustBurst(ServerLevel server, Stegonaut dragon, boolean slam2) {
        RandomSource random = dragon.getRandom();
        double radius = slam2 ? 7.0D : 5.0D;
        int dustCount = slam2 ? 70 : 42;
        int dirtCount = slam2 ? 38 : 0;
        double y = dragon.getBoundingBox().minY + 0.08D;

        for (int i = 0; i < dustCount; i++) {
            double angle = random.nextDouble() * Math.PI * 2.0D;
            double distance = radius * (0.25D + random.nextDouble() * 0.85D);
            double x = dragon.getX() + Math.cos(angle) * distance;
            double z = dragon.getZ() + Math.sin(angle) * distance;
            double speed = (slam2 ? 0.36D : 0.24D) + random.nextDouble() * 0.28D;
            sendNearbyDragonDustParticle(
                    server,
                    dragon,
                    x,
                    y,
                    z,
                    Math.cos(angle) * speed,
                    0.08D + random.nextDouble() * 0.16D,
                    Math.sin(angle) * speed
            );
        }

        if (dirtCount <= 0) {
            return;
        }
        BlockState dirt = Blocks.DIRT.defaultBlockState();
        for (int i = 0; i < dirtCount; i++) {
            double angle = random.nextDouble() * Math.PI * 2.0D;
            double distance = radius * random.nextDouble();
            server.sendParticles(
                    new BlockParticleOption(ParticleTypes.BLOCK, dirt),
                    dragon.getX() + Math.cos(angle) * distance,
                    y + 0.12D,
                    dragon.getZ() + Math.sin(angle) * distance,
                    1,
                    0.0D,
                    0.0D,
                    0.0D,
                    0.18D + random.nextDouble() * 0.12D
            );
        }
    }

    private void sendNearbyDragonDustParticle(ServerLevel server, Stegonaut dragon, double x, double y, double z,
                                              double xSpeed, double ySpeed, double zSpeed) {
        double maxDistanceSqr = DUST_VIEW_DISTANCE * DUST_VIEW_DISTANCE;
        for (ServerPlayer player : server.players()) {
            if (player.distanceToSqr(x, y, z) <= maxDistanceSqr || player.distanceToSqr(dragon) <= maxDistanceSqr) {
                server.sendParticles(player, ModParticles.DRAGON_DUST.get(), true,
                        x, y, z, 0, xSpeed, ySpeed, zSpeed, 1.0D);
            }
        }
    }

    private float resolveSlamDamage() {
        return (float) DragonAttributeConfigLoader.getInstance()
                .getConfig(DragonAttributeConfigLoader.STEGONAUT_ID)
                .abilityDamage("ground_slam", DEFAULT_SLAM_DAMAGE);
    }

    private double resolveSlamKnockback() {
        return DragonAttributeConfigLoader.getInstance()
                .getConfig(DragonAttributeConfigLoader.STEGONAUT_ID)
                .extraDouble("ground_slam_knockback", DEFAULT_SLAM_KNOCKBACK);
    }

    private float resolvePillarDamage() {
        return (float) DragonAttributeConfigLoader.getInstance()
                .getConfig(DragonAttributeConfigLoader.STEGONAUT_ID)
                .abilityDamage("ground_slam_pillar", DEFAULT_PILLAR_DAMAGE);
    }

    private double resolvePillarKnockback() {
        return DragonAttributeConfigLoader.getInstance()
                .getConfig(DragonAttributeConfigLoader.STEGONAUT_ID)
                .extraDouble("ground_slam_pillar_knockback", DEFAULT_PILLAR_KNOCKBACK);
    }

    private float resolveSlam2Damage() {
        return (float) DragonAttributeConfigLoader.getInstance()
                .getConfig(DragonAttributeConfigLoader.STEGONAUT_ID)
                .abilityDamage("ground_slam2", DEFAULT_SLAM2_DAMAGE);
    }

    private double resolveSlam2Knockback() {
        return DragonAttributeConfigLoader.getInstance()
                .getConfig(DragonAttributeConfigLoader.STEGONAUT_ID)
                .extraDouble("ground_slam2_knockback", DEFAULT_SLAM2_KNOCKBACK);
    }

    @Override
    public void end() {
        super.end();
        phase = Phase.FIRST_SLAM;
        phaseTicks = 0;
        releaseRequested = false;
        firstHitApplied = false;
        pillarsSpawned = false;
    }
}

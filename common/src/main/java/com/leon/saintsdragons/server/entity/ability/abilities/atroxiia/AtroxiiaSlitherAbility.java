package com.leon.saintsdragons.server.entity.ability.abilities.atroxiia;

import com.leon.saintsdragons.common.config.dragon.DragonAttributeConfigLoader;
import com.leon.saintsdragons.common.registry.ModParticles;
import com.leon.saintsdragons.common.registry.ModSounds;
import com.leon.saintsdragons.server.entity.ability.DragonAbility;
import com.leon.saintsdragons.server.entity.ability.DragonAbilitySection;
import com.leon.saintsdragons.server.entity.ability.DragonAbilityType;
import com.leon.saintsdragons.server.entity.dragons.atroxiia.Atroxiia;
import net.minecraft.core.particles.BlockParticleOption;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import java.util.HashMap;
import java.util.Map;

import static com.leon.saintsdragons.server.entity.ability.DragonAbilitySection.AbilitySectionDuration;
import static com.leon.saintsdragons.server.entity.ability.DragonAbilitySection.AbilitySectionType.STARTUP;

public final class AtroxiiaSlitherAbility extends DragonAbility<Atroxiia> {
    private static final int ANIMATION_TICKS = 75;
    private static final int NUDGE_TICK = 7;
    private static final int AUTO_MOVE_TICK = 22;
    private static final int STOP_TICK = 67;
    private static final float DEFAULT_CONTACT_DAMAGE = 5.0F;
    private static final double CONTACT_KNOCKBACK = 0.35D;
    private static final int CONTACT_HIT_INTERVAL_TICKS = 10;
    private static final double CONTACT_HORIZONTAL_INFLATE = 0.75D;
    private static final double CONTACT_VERTICAL_INFLATE = 0.35D;
    private static final int DUST_PARTICLES_PER_TICK = 2;
    private static final int DIRT_PARTICLES_PER_TICK = 4;
    private static final BlockParticleOption DIRT_PARTICLE =
            new BlockParticleOption(ParticleTypes.BLOCK, Blocks.DIRT.defaultBlockState());

    private static final DragonAbilitySection[] TRACK = {
            new AbilitySectionDuration(STARTUP, ANIMATION_TICKS)
    };

    private boolean nudged;
    private boolean autoMoving;
    private boolean stopped;
    private final Map<Integer, Integer> contactHitCooldowns = new HashMap<>();

    public AtroxiiaSlitherAbility(
            DragonAbilityType<Atroxiia, AtroxiiaSlitherAbility> type,
            Atroxiia user
    ) {
        super(type, user, TRACK, 30);
    }

    @Override
    public boolean tryAbility() {
        Atroxiia dragon = getUser();
        return dragon.getControllingPassenger() instanceof Player rider
                && dragon.isTame()
                && dragon.isOwnedBy(rider)
                && dragon.canUseGroundCombatAbility();
    }

    @Override
    protected boolean canContinueUsing() {
        Atroxiia dragon = getUser();
        return dragon.getControllingPassenger() instanceof Player rider
                && dragon.isTame()
                && dragon.isOwnedBy(rider)
                && !dragon.isBaby()
                && !dragon.isDying()
                && !dragon.isTamingStunned()
                && !dragon.isInWaterOrBubble()
                && !dragon.areRiderControlsLocked();
    }

    @Override
    protected void beginSection(DragonAbilitySection section) {
        if (section == null || section.sectionType != STARTUP) {
            return;
        }

        nudged = false;
        autoMoving = false;
        stopped = false;
        contactHitCooldowns.clear();
        Atroxiia dragon = getUser();
        dragon.startSlitherAnimation();
        if (!dragon.level().isClientSide) {
            dragon.getSoundHandler().playMovingEntitySound(
                    ModSounds.ATROXIIA_SLITHER.get(), 1.4F, 1.0F, 80
            );
        }
    }

    @Override
    public void tickUsing() {
        Atroxiia dragon = getUser();
        if (dragon.level().isClientSide) {
            return;
        }

        int tick = getTicksInUse();
        if (!nudged && tick >= NUDGE_TICK) {
            dragon.startSlitherNudge(AUTO_MOVE_TICK - NUDGE_TICK);
            nudged = true;
        }
        if (!autoMoving && tick >= AUTO_MOVE_TICK) {
            dragon.startSlitherAutoMove(STOP_TICK - AUTO_MOVE_TICK);
            autoMoving = true;
        }
        tickContactCooldowns();
        if (autoMoving && !stopped && tick < STOP_TICK) {
            damageContactTargets(dragon);
            spawnMovingParticles(dragon);
        }
        if (!stopped && tick >= STOP_TICK) {
            dragon.stopSlitherMovement();
            stopped = true;
        }
    }

    private void tickContactCooldowns() {
        contactHitCooldowns.entrySet().removeIf(entry -> {
            int remaining = entry.getValue() - 1;
            if (remaining <= 0) {
                return true;
            }
            entry.setValue(remaining);
            return false;
        });
    }

    private void damageContactTargets(Atroxiia dragon) {
        float contactDamage = (float) DragonAttributeConfigLoader.getInstance()
                .getConfig(DragonAttributeConfigLoader.ATROXIIA_ID)
                .abilityDamage("slither", DEFAULT_CONTACT_DAMAGE);
        AABB contactArea = dragon.getBoundingBox().inflate(
                CONTACT_HORIZONTAL_INFLATE,
                CONTACT_VERTICAL_INFLATE,
                CONTACT_HORIZONTAL_INFLATE
        );
        LivingEntity rider = dragon.getControllingPassenger();
        for (LivingEntity target : dragon.level().getEntitiesOfClass(
                LivingEntity.class,
                contactArea,
                entity -> entity != dragon
                        && entity != rider
                        && entity.isAlive()
                        && entity.attackable()
                        && !dragon.isAlly(entity)
        )) {
            if (contactHitCooldowns.containsKey(target.getId())) {
                continue;
            }
            if (!target.hurt(dragon.level().damageSources().mobAttack(dragon), contactDamage)) {
                continue;
            }

            applyContactKnockback(dragon, target);
            contactHitCooldowns.put(target.getId(), CONTACT_HIT_INTERVAL_TICKS);
        }
    }

    private void applyContactKnockback(Atroxiia dragon, LivingEntity target) {
        double dx = target.getX() - dragon.getX();
        double dz = target.getZ() - dragon.getZ();
        double horizontalDistance = Math.sqrt(dx * dx + dz * dz);
        if (horizontalDistance < 1.0E-6D) {
            Vec3 backward = Vec3.directionFromRotation(0.0F, dragon.getYRot()).scale(-1.0D);
            dx = backward.x;
            dz = backward.z;
            horizontalDistance = 1.0D;
        }
        target.knockback(
                CONTACT_KNOCKBACK,
                -dx / horizontalDistance,
                -dz / horizontalDistance
        );
    }

    private void spawnMovingParticles(Atroxiia dragon) {
        if (!(dragon.level() instanceof ServerLevel server)) {
            return;
        }

        RandomSource random = dragon.getRandom();
        Vec3 forward = Vec3.directionFromRotation(0.0F, dragon.getYRot()).normalize();
        Vec3 right = new Vec3(-forward.z, 0.0D, forward.x);
        double footprintWidth = Math.max(1.0D, dragon.getBbWidth() * 0.8D);
        double trailLength = Math.max(1.0D, dragon.getBbWidth() * 0.55D);
        double baseY = dragon.getBoundingBox().minY + 0.08D;

        for (int i = 0; i < DUST_PARTICLES_PER_TICK; i++) {
            spawnTrailParticle(server, dragon, random, forward, right, footprintWidth, trailLength, baseY, true);
        }
        for (int i = 0; i < DIRT_PARTICLES_PER_TICK; i++) {
            spawnTrailParticle(server, dragon, random, forward, right, footprintWidth, trailLength, baseY, false);
        }
    }

    private void spawnTrailParticle(ServerLevel server,
                                    Atroxiia dragon,
                                    RandomSource random,
                                    Vec3 forward,
                                    Vec3 right,
                                    double footprintWidth,
                                    double trailLength,
                                    double baseY,
                                    boolean dragonDust) {
        double lateral = (random.nextDouble() - 0.5D) * footprintWidth;
        double trailing = random.nextDouble() * trailLength;
        Vec3 position = dragon.position()
                .add(right.scale(lateral))
                .subtract(forward.scale(trailing));
        double outward = Math.copySign(
                0.05D + random.nextDouble() * 0.10D,
                lateral == 0.0D ? (random.nextBoolean() ? 1.0D : -1.0D) : lateral
        );
        Vec3 velocity = right.scale(outward)
                .subtract(forward.scale(0.08D + random.nextDouble() * 0.08D))
                .add(0.0D, 0.06D + random.nextDouble() * 0.10D, 0.0D);

        if (dragonDust) {
            server.sendParticles(
                    ModParticles.DRAGON_DUST.get(),
                    position.x, baseY, position.z,
                    0,
                    velocity.x, velocity.y, velocity.z,
                    1.0D
            );
        } else {
            server.sendParticles(
                    DIRT_PARTICLE,
                    position.x, baseY, position.z,
                    0,
                    velocity.x, velocity.y, velocity.z,
                    1.0D
            );
        }
    }

    @Override
    public void end() {
        contactHitCooldowns.clear();
        getUser().finishSlitherAnimation();
        super.end();
    }
}

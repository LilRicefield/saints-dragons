package com.leon.saintsdragons.common.item.tools;

import com.leon.saintsdragons.common.config.ToolsArmorConfig;
import com.leon.saintsdragons.common.network.BloodTempestAfterimageProfile;
import com.leon.saintsdragons.common.network.MessageBloodTempestAfterimage;
import com.leon.saintsdragons.common.network.NetworkHandler;
import com.leon.saintsdragons.common.registry.ModItems;
import com.leon.saintsdragons.common.registry.ModParticles;
import com.leon.saintsdragons.common.registry.ModSounds;
import com.leon.saintsdragons.server.entity.ability.abilities.raevyx.RaevyxChainLightningAbility;
import com.leon.saintsdragons.server.entity.effect.LightningVisualEntity;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import java.util.List;

public final class BloodTempestKatanaAbility {
    private static final double COLLISION_STEP = 0.25D;
    private static final double MIN_DISTANCE = 0.75D;
    private static final double SWEEP_INFLATION = 1.0D;
    private static final int RESIDUE_ARC_COUNT = 9;
    private static final int RESIDUE_LIFETIME_TICKS = 13;
    private static final int SLASH_LINE_LIFETIME_TICKS = 14;
    private static final int STORM_TRAIL_LIFETIME_TICKS = 16;
    private static final double SLASH_LINE_HEIGHT = 0.85D;
    private static final double DUST_SAMPLE_SPACING = 0.42D;
    private static final double DUST_START_HALF_WIDTH = 5.5D;
    private static final double DUST_LOWER_START_HEIGHT = 0.08D;
    private static final double DUST_UPPER_START_HEIGHT = 1.62D;
    private static final double DUST_DRIFT_SPEED = 0.045D;

    private BloodTempestKatanaAbility() {
    }

    public static void onSuccessfulKatanaHit(ServerPlayer player, LivingEntity target) {
        if (player.getMainHandItem().is(ModItems.BLOOD_TEMPEST_KATANA.get())
                && SwordAbilityTargeting.canChainFromSuccessfulHit(player, target)) {
            RaevyxChainLightningAbility.chainFromKatana(player, target);
        }
    }

    public static boolean tryUse(ServerPlayer player) {
        if (!canStart(player)) {
            return false;
        }

        Vec3 look = player.getLookAngle();
        Vec3 direction = new Vec3(look.x, 0.0D, look.z);
        if (direction.lengthSqr() < 1.0E-4D) {
            return false;
        }
        direction = direction.normalize();

        Vec3 origin = player.position();
        Vec3 destination = findDestination(player, direction);
        if (destination.distanceToSqr(origin) < MIN_DISTANCE * MIN_DISTANCE) {
            return false;
        }

        float damage = (float) (player.getAttributeValue(Attributes.ATTACK_DAMAGE)
                * ToolsArmorConfig.BLOOD_TEMPEST_KATANA_ABILITY_DAMAGE_MULTIPLIER.get());
        List<LivingEntity> targets = collectTargets(player, origin, destination);

        Item sword = ModItems.BLOOD_TEMPEST_KATANA.get();
        player.getCooldowns().addCooldown(sword, ToolsArmorConfig.BLOOD_TEMPEST_KATANA_ABILITY_COOLDOWN_TICKS.get());
        player.resetAttackStrengthTicker();
        player.teleportTo(destination.x, destination.y, destination.z);
        player.setDeltaMovement(Vec3.ZERO);
        player.hasImpulse = true;
        player.hurtMarked = true;

        strikeTargets(player, targets, direction, damage);
        spawnLightningResidue(player.serverLevel(), origin, destination);
        spawnConvergingDust(player.serverLevel(), origin, destination);
        spawnConvergingStormTrails(player.serverLevel(), origin, destination);
        spawnSlashLine(player.serverLevel(), origin, destination);
        player.level().playSound(
                null,
                player.blockPosition(),
                ModSounds.BLOOD_TEMPEST_ARMOR_ABILITY.get(),
                SoundSource.PLAYERS,
                1.0F,
                1.0F
        );

        MessageBloodTempestAfterimage effect = new MessageBloodTempestAfterimage(
                player.getId(), BloodTempestAfterimageProfile.KATANA_DASH, origin, destination);
        NetworkHandler.sendToTracking(player, effect);
        NetworkHandler.sendToPlayer(player, effect);
        return true;
    }

    private static void spawnLightningResidue(ServerLevel level, Vec3 origin, Vec3 destination) {
        Vec3 travel = destination.subtract(origin);
        if (travel.lengthSqr() < 1.0E-6D) {
            return;
        }

        Vec3 direction = travel.normalize();
        Vec3 side = new Vec3(-direction.z, 0.0D, direction.x);
        for (int index = 0; index < RESIDUE_ARC_COUNT; index++) {
            double baseProgress = 0.14D + index * (0.72D / (RESIDUE_ARC_COUNT - 1));
            double progress = Mth.clamp(baseProgress + signed(level) * 0.035D, 0.08D, 0.92D);
            double lateral = Math.copySign(0.55D + level.random.nextDouble() * 1.25D, signed(level));
            Vec3 center = origin.lerp(destination, progress)
                    .add(side.scale(lateral))
                    .add(0.0D, 0.55D + level.random.nextDouble() * 1.45D, 0.0D);

            Vec3 arcDirection = direction.scale(signed(level) * 0.22D)
                    .add(side.scale(signed(level)))
                    .add(0.0D, signed(level) * 0.85D, 0.0D);
            if (arcDirection.lengthSqr() < 1.0E-6D) {
                arcDirection = side;
            }
            arcDirection = arcDirection.normalize();
            double arcLength = 1.5D + level.random.nextDouble() * 2.15D;
            Vec3 arcStart = center.subtract(arcDirection.scale(arcLength * 0.45D));
            Vec3 arcEnd = center.add(arcDirection.scale(arcLength * 0.55D));
            spawnResidueArc(level, arcStart, arcEnd, 0.58F + level.random.nextFloat() * 0.24F);

            if ((index & 1) == 1) {
                Vec3 branchStart = center.lerp(arcEnd, 0.22D);
                Vec3 branchDirection = arcDirection.scale(0.18D)
                        .add(side.scale(signed(level)))
                        .add(0.0D, 0.35D + level.random.nextDouble() * 0.75D, 0.0D)
                        .normalize();
                Vec3 branchEnd = branchStart.add(branchDirection.scale(0.85D + level.random.nextDouble() * 1.1D));
                spawnResidueArc(level, branchStart, branchEnd, 0.42F + level.random.nextFloat() * 0.18F);
            }
        }
    }

    private static void spawnConvergingDust(ServerLevel level, Vec3 origin, Vec3 destination) {
        ConvergingTrails trails = createConvergingTrails(origin, destination);
        if (trails == null) {
            return;
        }

        Vec3 lowerDrift = trails.meetingPoint().subtract(trails.lowerStart())
                .normalize().scale(DUST_DRIFT_SPEED);
        Vec3 upperDrift = trails.meetingPoint().subtract(trails.upperStart())
                .normalize().scale(DUST_DRIFT_SPEED);
        int samples = Math.max(2, Mth.ceil(origin.distanceTo(destination) / DUST_SAMPLE_SPACING));

        for (int sample = 0; sample <= samples; sample++) {
            double progress = sample / (double)samples;
            spawnDust(level, trails.lowerStart().lerp(trails.meetingPoint(), progress), lowerDrift);
            if (sample < samples) {
                spawnDust(level, trails.upperStart().lerp(trails.meetingPoint(), progress), upperDrift);
            }
        }
    }

    private static void spawnConvergingStormTrails(ServerLevel level, Vec3 origin, Vec3 destination) {
        ConvergingTrails trails = createConvergingTrails(origin, destination);
        if (trails == null) {
            return;
        }

        spawnStormTrail(level, trails.lowerStart(), trails.meetingPoint());
        spawnStormTrail(level, trails.upperStart(), trails.meetingPoint());
    }

    private static ConvergingTrails createConvergingTrails(Vec3 origin, Vec3 destination) {
        Vec3 travel = destination.subtract(origin);
        double distance = travel.length();
        if (distance < 1.0E-4D) {
            return null;
        }

        Vec3 direction = travel.scale(1.0D / distance);
        Vec3 side = new Vec3(-direction.z, 0.0D, direction.x);
        Vec3 meetingPoint = destination.add(0.0D, SLASH_LINE_HEIGHT, 0.0D);
        Vec3 lowerStart = origin.add(side.scale(DUST_START_HALF_WIDTH))
                .add(0.0D, DUST_LOWER_START_HEIGHT, 0.0D);
        Vec3 upperStart = origin.subtract(side.scale(DUST_START_HALF_WIDTH))
                .add(0.0D, DUST_UPPER_START_HEIGHT, 0.0D);
        return new ConvergingTrails(lowerStart, upperStart, meetingPoint);
    }

    private static void spawnDust(ServerLevel level, Vec3 position, Vec3 drift) {
        level.sendParticles(ModParticles.DRAGON_DUST.get(),
                position.x, position.y, position.z,
                0, drift.x, drift.y, drift.z, 1.0D);
    }

    private static void spawnResidueArc(ServerLevel level, Vec3 start, Vec3 end, float scale) {
        level.addFreshEntity(new LightningVisualEntity(
                level,
                start,
                end,
                scale,
                RESIDUE_LIFETIME_TICKS,
                level.random.nextLong(),
                LightningVisualEntity.VisualStyle.BLOOD_TEMPEST
        ));
    }

    private static void spawnStormTrail(ServerLevel level, Vec3 start, Vec3 end) {
        level.addFreshEntity(new LightningVisualEntity(
                level,
                start,
                end,
                1.0F,
                STORM_TRAIL_LIFETIME_TICKS,
                level.random.nextLong(),
                LightningVisualEntity.VisualStyle.BLOOD_TEMPEST_STORM
        ));
    }

    private static void spawnSlashLine(ServerLevel level, Vec3 origin, Vec3 destination) {
        level.addFreshEntity(new LightningVisualEntity(
                level,
                origin.add(0.0D, SLASH_LINE_HEIGHT, 0.0D),
                destination.add(0.0D, SLASH_LINE_HEIGHT, 0.0D),
                1.0F,
                SLASH_LINE_LIFETIME_TICKS,
                level.random.nextLong(),
                LightningVisualEntity.VisualStyle.BLOOD_TEMPEST_SLASH
        ));
    }

    private static double signed(ServerLevel level) {
        return level.random.nextDouble() * 2.0D - 1.0D;
    }

    private record ConvergingTrails(Vec3 lowerStart, Vec3 upperStart, Vec3 meetingPoint) {
    }

    private static boolean canStart(ServerPlayer player) {
        if (!ToolsArmorConfig.BLOOD_TEMPEST_KATANA_ABILITY_ENABLED.get()
                || player == null
                || !player.isAlive()
                || player.isSpectator()
                || player.isPassenger()
                || player.getAbilities().flying
                || player.isFallFlying()
                || player.onClimbable()
                || player.isInWaterOrBubble()) {
            return false;
        }

        Item sword = ModItems.BLOOD_TEMPEST_KATANA.get();
        return player.getMainHandItem().is(sword) && !player.getCooldowns().isOnCooldown(sword);
    }

    private static Vec3 findDestination(ServerPlayer player, Vec3 direction) {
        Level level = player.level();
        AABB originBounds = player.getBoundingBox();
        Vec3 origin = player.position();
        Vec3 destination = origin;

        double maxDistance = ToolsArmorConfig.BLOOD_TEMPEST_KATANA_ABILITY_MAX_DISTANCE.get();
        for (double distance = COLLISION_STEP; distance <= maxDistance; distance += COLLISION_STEP) {
            Vec3 offset = direction.scale(distance);
            AABB candidateBounds = originBounds.move(offset);
            if (!level.noCollision(player, candidateBounds)
                    || !level.getWorldBorder().isWithinBounds(candidateBounds)) {
                break;
            }
            destination = origin.add(offset);
        }

        return destination;
    }

    private static List<LivingEntity> collectTargets(ServerPlayer player, Vec3 origin, Vec3 destination) {
        Vec3 travel = destination.subtract(origin);
        AABB sweep = player.getBoundingBox()
                .expandTowards(travel)
                .inflate(SWEEP_INFLATION);
        return player.level().getEntitiesOfClass(
                LivingEntity.class,
                sweep,
                target -> canHit(player, target)
        );
    }

    private static void strikeTargets(ServerPlayer player, List<LivingEntity> targets,
                                      Vec3 direction, float damage) {
        boolean chained = false;
        for (LivingEntity target : targets) {
            if (target.hurt(player.damageSources().playerAttack(player), damage)) {
                if (!chained) {
                    onSuccessfulKatanaHit(player, target);
                    chained = true;
                }
                target.knockback(0.25D, -direction.x, -direction.z);
                ItemStack katana = player.getMainHandItem();
                katana.hurtAndBreak(1, player,
                        wearer -> wearer.broadcastBreakEvent(EquipmentSlot.MAINHAND));
            }
        }
    }

    private static boolean canHit(Player player, LivingEntity target) {
        return SwordAbilityTargeting.canDamage(player, target)
                && player.hasLineOfSight(target);
    }
}

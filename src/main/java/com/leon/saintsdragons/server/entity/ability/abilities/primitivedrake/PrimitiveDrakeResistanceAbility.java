package com.leon.saintsdragons.server.entity.ability.abilities.primitivedrake;

import com.leon.saintsdragons.server.entity.base.DragonEntity;
import com.leon.saintsdragons.server.entity.dragons.primitivedrake.PrimitiveDrakeEntity;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

/**
 * Ability for Primitive Drakes to provide resistance buffs to nearby players and allies.
 * This makes the drake a valuable companion that protects its friends.
 */
public class PrimitiveDrakeResistanceAbility {
    private final PrimitiveDrakeEntity drake;
    private final Level level;

    // Buff parameters
    private static final double BUFF_RANGE = 8.0; // 8 block radius
    private static final int BUFF_DURATION_TICKS = 40; // Short duration so we can expire when leaving range
    private static final int BUFF_AMPLIFIER = 0; // Resistance I (20% damage reduction)

    // Performance optimization
    private int tickCounter = 0;
    private static final int UPDATE_INTERVAL = 20; // Update every second

    // Track entities that currently have the resistance buff
    private Set<UUID> buffedEntityIds = new HashSet<>();

    public PrimitiveDrakeResistanceAbility(PrimitiveDrakeEntity drake) {
        this.drake = drake;
        this.level = drake.level();
    }

    /**
     * Called every tick to apply resistance buffs to nearby entities
     */
    public void tick() {
        if (!(level instanceof ServerLevel serverLevel)) {
            return;
        }

        if (++tickCounter < UPDATE_INTERVAL) {
            return;
        }
        tickCounter = 0;

        if (!drake.isAlive() || drake.isPlayingDead()) {
            clearTrackedBuffs(serverLevel);
            return;
        }

        applyResistanceBuffs(serverLevel);
    }

    /**
     * Apply resistance buffs to all eligible entities within range
     */
    private void applyResistanceBuffs(ServerLevel serverLevel) {
        List<LivingEntity> nearbyEntities = serverLevel.getEntitiesOfClass(
            LivingEntity.class,
            drake.getBoundingBox().inflate(BUFF_RANGE),
            this::isEligibleForBuff
        );

        Set<UUID> currentNearby = new HashSet<>();

        for (LivingEntity entity : nearbyEntities) {
            applyResistanceToEntity(entity);
            currentNearby.add(entity.getUUID());
        }

        buffedEntityIds = currentNearby;
    }

    /**
     * Check if an entity is eligible for the resistance buff
     */
    private boolean isEligibleForBuff(LivingEntity entity) {
        if (entity == drake || !entity.isAlive()) {
            return false;
        }

        if (entity instanceof Player player) {
            return isPlayerEligible(player);
        }

        if (entity instanceof DragonEntity dragon) {
            return isDragonEligible(dragon);
        }

        return false;
    }

    private boolean isPlayerEligible(Player player) {
        if (!drake.isTame()) {
            return false;
        }

        LivingEntity owner = drake.getOwner();
        if (!(owner instanceof Player ownerPlayer)) {
            return false;
        }

        if (ownerPlayer.getUUID().equals(player.getUUID())) {
            return true;
        }

        return drake.allyManager.isAlly(player);
    }

    private boolean isDragonEligible(DragonEntity dragon) {
        if (!drake.isTame()) {
            return false;
        }

        LivingEntity owner = drake.getOwner();
        if (!(owner instanceof Player ownerPlayer)) {
            return false;
        }

        if (dragon.isOwnedBy(ownerPlayer)) {
            return true;
        }

        return dragon instanceof PrimitiveDrakeEntity alliedDrake && alliedDrake.allyManager.isAlly(ownerPlayer);
    }

    /**
     * Apply resistance buff to a specific entity
     */
    private void applyResistanceToEntity(LivingEntity entity) {
        MobEffectInstance resistanceEffect = new MobEffectInstance(
            MobEffects.DAMAGE_RESISTANCE,
            BUFF_DURATION_TICKS,
            BUFF_AMPLIFIER,
            false,
            false,
            true
        );

        entity.addEffect(resistanceEffect);
    }

    /**
     * Clean up all buffed entities when the drake is removed
     */
    public void cleanup() {
        if (level instanceof ServerLevel serverLevel) {
            clearTrackedBuffs(serverLevel);
        } else {
            buffedEntityIds.clear();
        }
    }

    private void clearTrackedBuffs(ServerLevel serverLevel) {
        for (UUID uuid : buffedEntityIds) {
            Entity entity = serverLevel.getEntity(uuid);
            if (entity instanceof LivingEntity livingEntity) {
                livingEntity.removeEffect(MobEffects.DAMAGE_RESISTANCE);
            }
        }
        buffedEntityIds.clear();
    }

    /**
     * Get the buff range for display purposes
     */
    public static double getBuffRange() {
        return BUFF_RANGE;
    }

    /**
     * Get the buff amplifier level
     */
    public static int getBuffAmplifier() {
        return BUFF_AMPLIFIER;
    }
}

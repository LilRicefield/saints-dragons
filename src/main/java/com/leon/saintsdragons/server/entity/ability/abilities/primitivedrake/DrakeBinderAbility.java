package com.leon.saintsdragons.server.entity.ability.abilities.primitivedrake;

import com.leon.saintsdragons.common.item.DrakeBinderItem;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.OwnableEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/**
 * Handles portable resistance buffs from bound Drake Binders.
 * When a player carries a bound Drake Binder anywhere in their inventory,
 * they and nearby allies get resistance buffs.
 */
public final class DrakeBinderAbility {

    // Buff parameters
    private static final double BUFF_RANGE = 8.0; // Same range as the drake's original ability
    private static final int BUFF_DURATION_TICKS = 40; // Short duration so we can expire it when binder removed
    private static final int BUFF_AMPLIFIER = 0; // Resistance I
    private static final int ABSORPTION_AMPLIFIER = 0; // Absorption I

    // Track which entities are currently buffed per dimension so effects can be cleaned up
    private static final Map<ResourceKey<Level>, Set<UUID>> ACTIVE_BUFF_TARGETS = new HashMap<>();

    private DrakeBinderAbility() {
        // Utility class
    }

    /**
     * Update the portable resistance buffs for all players carrying bound Drake Binders
     */
    public static void updateAllPortableBuffs(ServerLevel level) {
        Set<UUID> currentTargets = new HashSet<>();

        for (Player player : level.players()) {
            if (!player.isAlive()) {
                continue;
            }

            if (!hasBoundDrakeBinder(player)) {
                continue;
            }

            applyPortableResistanceBuffs(level, player, currentTargets);
        }

        Set<UUID> previousTargets = ACTIVE_BUFF_TARGETS.getOrDefault(level.dimension(), Collections.emptySet());
        if (!previousTargets.isEmpty()) {
            for (UUID uuid : previousTargets) {
                if (currentTargets.contains(uuid)) {
                    continue;
                }
                Entity entity = level.getEntity(uuid);
                if (entity instanceof LivingEntity livingEntity) {
                    livingEntity.removeEffect(MobEffects.DAMAGE_RESISTANCE);
                    livingEntity.removeEffect(MobEffects.ABSORPTION);
                }
            }
        }

        ACTIVE_BUFF_TARGETS.put(level.dimension(), currentTargets);
    }

    /**
     * Apply portable resistance buffs to the player and nearby allies
     */
    private static void applyPortableResistanceBuffs(ServerLevel level, Player player, Set<UUID> currentTargets) {
        applyBuffsToEntity(player, currentTargets);

        var nearbyEntities = level.getEntitiesOfClass(
            LivingEntity.class,
            player.getBoundingBox().inflate(BUFF_RANGE),
            entity -> entity != player && isEligibleForPortableBuff(entity, player)
        );

        for (LivingEntity entity : nearbyEntities) {
            applyBuffsToEntity(entity, currentTargets);
        }
    }

    /**
     * Check if the player has a bound Drake Binder anywhere in their inventory
     */
    private static boolean hasBoundDrakeBinder(Player player) {
        if (findBoundDrakeBinderInList(player.getInventory().items)) {
            return true;
        }
        if (findBoundDrakeBinderInList(player.getInventory().offhand)) {
            return true;
        }
        return findBoundDrakeBinderInList(player.getInventory().armor);
    }

    private static boolean findBoundDrakeBinderInList(net.minecraft.core.NonNullList<ItemStack> stacks) {
        for (ItemStack item : stacks) {
            if (item.isEmpty()) {
                continue;
            }
            if (item.getItem() instanceof DrakeBinderItem && DrakeBinderItem.isBound(item)) {
                return DrakeBinderItem.getBoundDrakeUUID(item) != null;
            }
        }
        return false;
    }

    /**
     * Check if an entity is eligible for portable resistance buff
     */
    private static boolean isEligibleForPortableBuff(LivingEntity entity, Player player) {
        if (!entity.isAlive()) {
            return false;
        }

        if (entity instanceof Player) {
            return true;
        }

        if (entity instanceof com.leon.saintsdragons.server.entity.base.DragonEntity dragon) {
            return dragon.isTame() && dragon.isOwnedBy(player);
        }

        if (entity instanceof com.leon.saintsdragons.server.entity.dragons.primitivedrake.PrimitiveDrakeEntity drake) {
            return drake.isTame() && drake.allyManager.isAlly(player);
        }

        if (entity instanceof OwnableEntity ownable) {
            LivingEntity owner = ownable.getOwner();
            if (owner instanceof Player ownerPlayer) {
                return ownerPlayer.getUUID().equals(player.getUUID());
            }
        }

        return false;
    }

    /**
     * Apply resistance effect to an entity and record it for cleanup tracking
     */
    private static void applyBuffsToEntity(LivingEntity entity, Set<UUID> currentTargets) {
        MobEffectInstance resistanceEffect = new MobEffectInstance(
            MobEffects.DAMAGE_RESISTANCE,
            BUFF_DURATION_TICKS,
            BUFF_AMPLIFIER,
            false,
            false,
            false
        );
        MobEffectInstance absorptionEffect = new MobEffectInstance(
            MobEffects.ABSORPTION,
            BUFF_DURATION_TICKS,
            ABSORPTION_AMPLIFIER,
            false,
            false,
            false
        );
        entity.addEffect(resistanceEffect);
        entity.addEffect(absorptionEffect);
        currentTargets.add(entity.getUUID());
    }
}

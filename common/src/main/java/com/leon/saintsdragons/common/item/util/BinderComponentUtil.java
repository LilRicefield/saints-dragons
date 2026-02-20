package com.leon.saintsdragons.common.item.util;

import com.leon.saintsdragons.common.item.CindervaneBinderItem;
import com.leon.saintsdragons.common.item.IgnivorusBinderItem;
import com.leon.saintsdragons.common.item.NulljawBinderItem;
import com.leon.saintsdragons.common.item.RaevyxBinderItem;
import com.leon.saintsdragons.common.item.StegonautBinderItem;
import com.leon.saintsdragons.server.data.DragonCodexSavedData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;

import java.util.UUID;

public final class BinderComponentUtil {
    private static final String BOUND_OWNER_UUID = "BoundOwnerUUID";

    private BinderComponentUtil() {
    }

    public static UUID getBoundDragonUuid(ItemStack stack) {
        if (stack == null || stack.isEmpty()) {
            return null;
        }
        if (stack.getItem() instanceof RaevyxBinderItem) {
            return RaevyxBinderItem.getBoundDragonUUID(stack);
        }
        if (stack.getItem() instanceof CindervaneBinderItem) {
            return CindervaneBinderItem.getBoundAmphithereUUID(stack);
        }
        if (stack.getItem() instanceof NulljawBinderItem) {
            return NulljawBinderItem.getBoundRiftDrakeUUID(stack);
        }
        if (stack.getItem() instanceof IgnivorusBinderItem) {
            return IgnivorusBinderItem.getBoundIgnivorusUUID(stack);
        }
        if (stack.getItem() instanceof StegonautBinderItem) {
            return StegonautBinderItem.getBoundDrakeUUID(stack);
        }
        return null;
    }

    public static boolean containsDragonUuid(ItemStack stack, UUID dragonId) {
        if (dragonId == null) {
            return false;
        }
        UUID boundUuid = getBoundDragonUuid(stack);
        return dragonId.equals(boundUuid);
    }

    public static boolean playerHasBoundDragon(ServerPlayer player, UUID dragonId) {
        if (player == null || dragonId == null) {
            return false;
        }

        for (ItemStack stack : player.getInventory().items) {
            if (containsDragonUuid(stack, dragonId)) {
                return true;
            }
        }
        for (ItemStack stack : player.getInventory().offhand) {
            if (containsDragonUuid(stack, dragonId)) {
                return true;
            }
        }
        for (ItemStack stack : player.getInventory().armor) {
            if (containsDragonUuid(stack, dragonId)) {
                return true;
            }
        }

        var enderChest = player.getEnderChestInventory();
        for (int i = 0; i < enderChest.getContainerSize(); i++) {
            if (containsDragonUuid(enderChest.getItem(i), dragonId)) {
                return true;
            }
        }

        return false;
    }

    public static UUID getBoundOwnerUuid(ItemStack stack) {
        if (stack == null || stack.isEmpty() || !stack.hasTag()) {
            return null;
        }
        var tag = stack.getTag();
        if (tag == null || !tag.hasUUID(BOUND_OWNER_UUID)) {
            return null;
        }
        return tag.getUUID(BOUND_OWNER_UUID);
    }

    /**
     * Called when a binder item entity is destroyed (lava, cactus, despawn, etc.).
     * If it contains a bound dragon, remove that dragon from codex so stale entries
     * don't survive after the binder is gone permanently.
     */
    public static void handleDestroyedBoundBinder(ItemEntity itemEntity) {
        if (itemEntity == null || itemEntity.level().isClientSide()) {
            return;
        }
        if (!(itemEntity.level() instanceof ServerLevel serverLevel)) {
            return;
        }

        ItemStack stack = itemEntity.getItem();
        UUID dragonId = getBoundDragonUuid(stack);
        if (dragonId == null) {
            return;
        }

        UUID ownerId = getBoundOwnerUuid(stack);
        if (ownerId == null) {
            return;
        }

        DragonCodexSavedData.get(serverLevel).removeDragon(ownerId, dragonId);
    }

    /**
     * Returns true if a bound binder for this dragon exists in loaded server state
     * (any online player's inventory/ender chest or any loaded dropped item entity).
     */
    public static boolean isDragonBoundInLoadedWorld(ServerLevel originLevel, UUID dragonId) {
        if (originLevel == null || originLevel.getServer() == null || dragonId == null) {
            return false;
        }

        for (ServerPlayer serverPlayer : originLevel.getServer().getPlayerList().getPlayers()) {
            if (playerHasBoundDragon(serverPlayer, dragonId)) {
                return true;
            }
        }

        for (ServerLevel level : originLevel.getServer().getAllLevels()) {
            for (var entity : level.getAllEntities()) {
                if (entity instanceof ItemEntity itemEntity && containsDragonUuid(itemEntity.getItem(), dragonId)) {
                    return true;
                }
            }
        }

        return false;
    }
}

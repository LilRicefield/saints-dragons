package com.leon.saintsdragons.common.item.util;

import com.leon.saintsdragons.common.item.CindervaneBinderItem;
import com.leon.saintsdragons.common.item.IgnivorusBinderItem;
import com.leon.saintsdragons.common.item.NulljawBinderItem;
import com.leon.saintsdragons.common.item.RaevyxBinderItem;
import com.leon.saintsdragons.common.item.StegonautBinderItem;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;

import java.util.UUID;

public final class BinderComponentUtil {
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
}

package com.leon.saintsdragons.common.item.tools;

import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.OwnableEntity;
import net.minecraft.world.entity.TamableAnimal;
import net.minecraft.world.entity.player.Player;

public final class SwordAbilityTargeting {
    private SwordAbilityTargeting() {
    }

    public static boolean canDamage(Player player, LivingEntity target) {
        if (target == null
                || target == player
                || !target.isAlive()
                || !target.attackable()
                || player.isAlliedTo(target)
                || target.isAlliedTo(player)) {
            return false;
        }

        if (target instanceof TamableAnimal tamable && tamable.isTame()) {
            return false;
        }

        return !(target instanceof OwnableEntity ownable) || ownable.getOwnerUUID() == null;
    }
}

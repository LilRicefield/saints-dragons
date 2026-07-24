package com.leon.saintsdragons.common.item;

import com.leon.saintsdragons.common.registry.ModArmors;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;

public final class DraconianArmorSet {
    private DraconianArmorSet() {
    }

    public static boolean pacifiesSwarm(LivingEntity entity) {
        return entity instanceof Player
                && entity.getItemBySlot(EquipmentSlot.HEAD).is(ModArmors.DRACONIAN_HELMET.get())
                && entity.getItemBySlot(EquipmentSlot.CHEST).is(ModArmors.DRACONIAN_CHESTPLATE.get())
                && entity.getItemBySlot(EquipmentSlot.LEGS).is(ModArmors.DRACONIAN_LEGGINGS.get())
                && entity.getItemBySlot(EquipmentSlot.FEET).is(ModArmors.DRACONIAN_BOOTS.get());
    }
}

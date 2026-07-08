package com.leon.saintsdragons.client.renderer.armor;

import net.minecraft.client.model.HumanoidModel;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;

public final class BloodTempestRenderProvider {
    private static BloodTempestArmorRenderer renderer;

    private BloodTempestRenderProvider() {
    }

    public static Object getHumanoidArmorModel(Object livingEntity, Object itemStack, Object equipmentSlot, Object original) {
        BloodTempestArmorRenderer armorRenderer = renderer();
        armorRenderer.prepForRender(
                (LivingEntity) livingEntity,
                (ItemStack) itemStack,
                (EquipmentSlot) equipmentSlot,
                (HumanoidModel<?>) original
        );
        return armorRenderer;
    }

    private static BloodTempestArmorRenderer renderer() {
        if (renderer == null) {
            renderer = new BloodTempestArmorRenderer();
        }
        return renderer;
    }
}

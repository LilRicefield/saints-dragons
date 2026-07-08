package com.leon.saintsdragons.client.renderer.armor;

import net.minecraft.client.model.HumanoidModel;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;

public final class DragonlordArmorRenderProvider {
    private static DragonlordArmorRenderer renderer;

    private DragonlordArmorRenderProvider() {
    }

    public static Object getHumanoidArmorModel(Object entity, Object stack, Object slot, Object original) {
        DragonlordArmorRenderer armorRenderer = renderer();
        armorRenderer.prepForRender((LivingEntity) entity, (ItemStack) stack, (EquipmentSlot) slot,
                (HumanoidModel<?>) original);
        return armorRenderer;
    }

    private static DragonlordArmorRenderer renderer() {
        if (renderer == null) {
            renderer = new DragonlordArmorRenderer();
        }
        return renderer;
    }
}

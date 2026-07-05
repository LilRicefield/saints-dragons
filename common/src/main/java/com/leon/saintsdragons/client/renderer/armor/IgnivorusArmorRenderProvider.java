package com.leon.saintsdragons.client.renderer.armor;

import net.minecraft.client.model.HumanoidModel;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;

public final class IgnivorusArmorRenderProvider {
    private static IgnivorusArmorRenderer renderer;

    private IgnivorusArmorRenderProvider() {
    }

    public static Object getHumanoidArmorModel(Object entity, Object stack, Object slot, Object original) {
        IgnivorusArmorRenderer armorRenderer = renderer();
        armorRenderer.prepForRender((LivingEntity) entity, (ItemStack) stack, (EquipmentSlot) slot,
                (HumanoidModel<?>) original);
        return armorRenderer;
    }

    private static IgnivorusArmorRenderer renderer() {
        if (renderer == null) {
            renderer = new IgnivorusArmorRenderer();
        }
        return renderer;
    }
}

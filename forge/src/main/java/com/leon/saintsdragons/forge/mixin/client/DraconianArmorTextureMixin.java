package com.leon.saintsdragons.forge.mixin.client;

import com.leon.saintsdragons.client.renderer.armor.DraconianArmorTextures;
import com.leon.saintsdragons.common.registry.ModArmorMaterials;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.renderer.entity.layers.HumanoidArmorLayer;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ArmorItem;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(HumanoidArmorLayer.class)
public abstract class DraconianArmorTextureMixin<
        T extends LivingEntity,
        M extends HumanoidModel<T>,
        A extends HumanoidModel<T>> {

    @Inject(method = "getArmorResource", at = @At("HEAD"), cancellable = true, remap = false)
    private void saintsdragons$useAnimatedDraconianTexture(
            Entity entity,
            ItemStack stack,
            EquipmentSlot slot,
            @Nullable String type,
            CallbackInfoReturnable<ResourceLocation> callback
    ) {
        if (type == null
                && stack.getItem() instanceof ArmorItem armor
                && armor.getMaterial() == ModArmorMaterials.DRACONIAN_FLESH) {
            callback.setReturnValue(DraconianArmorTextures.texture(slot == EquipmentSlot.LEGS));
        }
    }
}

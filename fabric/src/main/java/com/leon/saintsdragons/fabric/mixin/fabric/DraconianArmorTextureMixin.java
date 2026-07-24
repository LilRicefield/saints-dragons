package com.leon.saintsdragons.fabric.mixin.fabric;

import com.leon.saintsdragons.client.renderer.armor.DraconianArmorTextures;
import com.leon.saintsdragons.common.registry.ModArmorMaterials;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.renderer.entity.layers.HumanoidArmorLayer;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ArmorItem;
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

    @Inject(method = "getArmorLocation", at = @At("HEAD"), cancellable = true)
    private void saintsdragons$useAnimatedDraconianTexture(
            ArmorItem armor,
            boolean innerLayer,
            @Nullable String type,
            CallbackInfoReturnable<ResourceLocation> callback
    ) {
        if (type == null && armor.getMaterial() == ModArmorMaterials.DRACONIAN_FLESH) {
            callback.setReturnValue(DraconianArmorTextures.texture(innerLayer));
        }
    }
}

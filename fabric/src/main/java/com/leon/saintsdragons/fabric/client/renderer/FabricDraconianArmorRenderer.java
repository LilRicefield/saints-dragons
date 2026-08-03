package com.leon.saintsdragons.fabric.client.renderer;

import com.leon.saintsdragons.client.renderer.armor.DraconianArmorTextures;
import com.leon.saintsdragons.common.registry.ModArmors;
import com.mojang.blaze3d.vertex.PoseStack;
import net.fabricmc.fabric.api.client.rendering.v1.ArmorRenderer;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.model.geom.builders.CubeDeformation;
import net.minecraft.client.model.geom.builders.LayerDefinition;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;

public final class FabricDraconianArmorRenderer {
    private static final HumanoidModel<LivingEntity> INNER_MODEL = createModel(0.5F);
    private static final HumanoidModel<LivingEntity> OUTER_MODEL = createModel(1.0F);

    private FabricDraconianArmorRenderer() {
    }

    public static void register() {
        ArmorRenderer.register(
                FabricDraconianArmorRenderer::render,
                ModArmors.DRACONIAN_HELMET.get(),
                ModArmors.DRACONIAN_CHESTPLATE.get(),
                ModArmors.DRACONIAN_LEGGINGS.get(),
                ModArmors.DRACONIAN_BOOTS.get()
        );
    }

    private static void render(
            PoseStack poseStack,
            MultiBufferSource bufferSource,
            ItemStack stack,
            LivingEntity entity,
            EquipmentSlot slot,
            int packedLight,
            HumanoidModel<LivingEntity> contextModel
    ) {
        boolean innerLayer = slot == EquipmentSlot.LEGS;
        HumanoidModel<LivingEntity> armorModel = innerLayer ? INNER_MODEL : OUTER_MODEL;
        contextModel.copyPropertiesTo(armorModel);
        setPartVisibility(armorModel, slot);
        ArmorRenderer.renderPart(
                poseStack,
                bufferSource,
                packedLight,
                stack,
                armorModel,
                DraconianArmorTextures.texture(innerLayer)
        );
    }

    private static HumanoidModel<LivingEntity> createModel(float deformation) {
        return new HumanoidModel<>(LayerDefinition.create(
                HumanoidModel.createMesh(new CubeDeformation(deformation), 0.0F),
                64,
                32
        ).bakeRoot());
    }

    private static void setPartVisibility(HumanoidModel<LivingEntity> model, EquipmentSlot slot) {
        model.setAllVisible(false);

        switch (slot) {
            case HEAD -> {
                model.head.visible = true;
                model.hat.visible = true;
            }
            case CHEST -> {
                model.body.visible = true;
                model.rightArm.visible = true;
                model.leftArm.visible = true;
            }
            case LEGS -> {
                model.body.visible = true;
                model.rightLeg.visible = true;
                model.leftLeg.visible = true;
            }
            case FEET -> {
                model.rightLeg.visible = true;
                model.leftLeg.visible = true;
            }
            default -> {
            }
        }
    }
}

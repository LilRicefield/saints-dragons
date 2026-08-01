package com.leon.saintsdragons.client.ui.codex;

import com.leon.saintsdragons.common.SaintsDragonsCommon;
import com.leon.saintsdragons.client.ui.DraconicCodexScreen;
import com.leon.saintsdragons.common.registry.ModEntities;
import com.leon.saintsdragons.server.entity.base.DragonEntity;
import com.leon.saintsdragons.server.entity.base.DragonGender;
import com.leon.saintsdragons.server.entity.variant.SaintsDragonVariantRegistry;
import com.mojang.blaze3d.platform.Lighting;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.InventoryScreen;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.Entity;
import org.joml.Matrix4f;
import org.joml.Quaternionf;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class CodexDragonRenderer {
    private static final int IGNIVORUS_SCALE = 8;
    private static final int RAEVYX_SCALE = 13;
    private static final int ATROXIIA_SCALE = 13;
    private static final int VOLITANS_SCALE = 13;
    private static final int VARASUCHUS_SCALE = 15;
    private static final int CINDERVANE_SCALE = 12;
    private static final int STEGONAUT_SCALE = 23;
    private static final int IGNIVORUS_OFFSET_X = 0;
    private static final int IGNIVORUS_OFFSET_Y = 0;
    private static final int IGNIVORUS_BABY_SCALE_ADJUSTMENT = 30;
    private static final int IGNIVORUS_BABY_OFFSET_X = 0;
    private static final int IGNIVORUS_BABY_OFFSET_Y = -20;
    private static final int RAEVYX_OFFSET_X = 0;
    private static final int RAEVYX_OFFSET_Y = -5;
    private static final int RAEVYX_BABY_SCALE_ADJUSTMENT = 30;
    private static final int RAEVYX_BABY_OFFSET_X = 0;
    private static final int RAEVYX_BABY_OFFSET_Y = -20;
    private static final int ATROXIIA_OFFSET_X = 0;
    private static final int ATROXIIA_OFFSET_Y = 0;
    private static final int ATROXIIA_BABY_SCALE_ADJUSTMENT = 30;
    private static final int ATROXIIA_BABY_OFFSET_X = 0;
    private static final int ATROXIIA_BABY_OFFSET_Y = -30;
    private static final int VOLITANS_OFFSET_X = 0;
    private static final int VOLITANS_OFFSET_Y = -5;
    private static final int VOLITANS_BABY_SCALE_ADJUSTMENT = 30;
    private static final int VOLITANS_BABY_OFFSET_X = 0;
    private static final int VOLITANS_BABY_OFFSET_Y = -5;
    private static final int VARASUCHUS_OFFSET_X = 0;
    private static final int VARASUCHUS_OFFSET_Y = -10;
    private static final int VARASUCHUS_BABY_SCALE_ADJUSTMENT = 30;
    private static final int VARASUCHUS_BABY_OFFSET_X = 0;
    private static final int VARASUCHUS_BABY_OFFSET_Y = -10;
    private static final int CINDERVANE_OFFSET_X = 0;
    private static final int CINDERVANE_OFFSET_Y = -15;
    private static final int CINDERVANE_BABY_SCALE_ADJUSTMENT = 30;
    private static final int CINDERVANE_BABY_OFFSET_X = 0;
    private static final int CINDERVANE_BABY_OFFSET_Y = 0;
    private static final int STEGONAUT_OFFSET_X = 0;
    private static final int STEGONAUT_OFFSET_Y = -15;
    private static final int STEGONAUT_BABY_SCALE_ADJUSTMENT = 30;
    private static final int STEGONAUT_BABY_OFFSET_X = 0;
    private static final int STEGONAUT_BABY_OFFSET_Y = -10;
    private static final int NULLJAW_SCALE = 30;
    private static final int NULLJAW_OFFSET_X = 0;
    private static final int NULLJAW_OFFSET_Y = 0;
    private static final int NULLJAW_BABY_SCALE_ADJUSTMENT = 30;
    private static final int NULLJAW_BABY_OFFSET_X = 0;
    private static final int NULLJAW_BABY_OFFSET_Y = -30;
    private static final Map<String, PortraitDefinition> PORTRAIT_DEFINITIONS = Map.of(
            "ignivorus", portraitDefinition("ignivorus", IGNIVORUS_SCALE, IGNIVORUS_OFFSET_X, IGNIVORUS_OFFSET_Y,
                    IGNIVORUS_BABY_SCALE_ADJUSTMENT, IGNIVORUS_BABY_OFFSET_X, IGNIVORUS_BABY_OFFSET_Y),
            "raevyx", portraitDefinition("raevyx", RAEVYX_SCALE, RAEVYX_OFFSET_X, RAEVYX_OFFSET_Y,
                    RAEVYX_BABY_SCALE_ADJUSTMENT, RAEVYX_BABY_OFFSET_X, RAEVYX_BABY_OFFSET_Y),
            "atroxiia", portraitDefinition("atroxiia", ATROXIIA_SCALE, ATROXIIA_OFFSET_X, ATROXIIA_OFFSET_Y,
                    ATROXIIA_BABY_SCALE_ADJUSTMENT, ATROXIIA_BABY_OFFSET_X, ATROXIIA_BABY_OFFSET_Y),
            "volitans", portraitDefinition("volitans", VOLITANS_SCALE, VOLITANS_OFFSET_X, VOLITANS_OFFSET_Y,
                    VOLITANS_BABY_SCALE_ADJUSTMENT, VOLITANS_BABY_OFFSET_X, VOLITANS_BABY_OFFSET_Y),
            "varasuchus", portraitDefinition("varasuchus", VARASUCHUS_SCALE, VARASUCHUS_OFFSET_X, VARASUCHUS_OFFSET_Y,
                    VARASUCHUS_BABY_SCALE_ADJUSTMENT, VARASUCHUS_BABY_OFFSET_X, VARASUCHUS_BABY_OFFSET_Y),
            "cindervane", portraitDefinition("cindervane", CINDERVANE_SCALE, CINDERVANE_OFFSET_X, CINDERVANE_OFFSET_Y,
                    CINDERVANE_BABY_SCALE_ADJUSTMENT, CINDERVANE_BABY_OFFSET_X, CINDERVANE_BABY_OFFSET_Y),
            "stegonaut", portraitDefinition("stegonaut", STEGONAUT_SCALE, STEGONAUT_OFFSET_X, STEGONAUT_OFFSET_Y,
                    STEGONAUT_BABY_SCALE_ADJUSTMENT, STEGONAUT_BABY_OFFSET_X, STEGONAUT_BABY_OFFSET_Y),
            "nulljaw", portraitDefinition("nulljaw", NULLJAW_SCALE, NULLJAW_OFFSET_X, NULLJAW_OFFSET_Y,
                    NULLJAW_BABY_SCALE_ADJUSTMENT, NULLJAW_BABY_OFFSET_X, NULLJAW_BABY_OFFSET_Y)
    );
    private final CodexStaticPortraitRenderer staticPortraitRenderer = new CodexStaticPortraitRenderer();
    private final Map<UUID, CodexStaticPortraitRenderer.Portrait> staticPortraits = new HashMap<>();

    public void drawDragonPortrait(GuiGraphics guiGraphics, Minecraft minecraft, CodexDragonEntry selected,
                                   int leftPos, int topPos, int mouseX, int mouseY) {
        if (minecraft == null || minecraft.level == null || selected == null || selected.entityId() == null) {
            return;
        }

        DragonEntity dragon = findDragonEntity(minecraft, selected.entityId());
        if (dragon == null) {
            drawStaticPortrait(guiGraphics, minecraft, selected, leftPos, topPos, mouseX, mouseY);
            return;
        }

        int boxX = leftPos + CodexLayout.DRAGON_RENDER_BOX_X;
        int boxY = topPos + CodexLayout.DRAGON_RENDER_BOX_Y;
        int centerX = boxX + CodexLayout.DRAGON_RENDER_BOX_SIZE / 2 + getDragonOffsetX(dragon);
        int centerY = boxY + CodexLayout.DRAGON_RENDER_BOX_SIZE + getDragonOffsetY(dragon);

        int size = getDragonScale(dragon);

        guiGraphics.enableScissor(boxX, boxY,
                boxX + CodexLayout.DRAGON_RENDER_BOX_SIZE,
                boxY + CodexLayout.DRAGON_RENDER_BOX_SIZE);

        DraconicCodexScreen.RENDERING_IN_GUI.set(true);
        try {
            InventoryScreen.renderEntityInInventoryFollowsMouse(
                    guiGraphics,
                    centerX,
                    centerY,
                    size,
                    (float) (centerX - mouseX),
                    (float) (centerY - CodexLayout.DRAGON_RENDER_BOX_SIZE - mouseY),
                    dragon
            );
        } finally {
            DraconicCodexScreen.RENDERING_IN_GUI.set(false);
        }

        guiGraphics.disableScissor();
    }

    private int getDragonScale(DragonEntity dragon) {
        if (dragon.getType() == ModEntities.IGNIVORUS.get()) {
            return IGNIVORUS_SCALE + babyAdjustment(dragon, IGNIVORUS_BABY_SCALE_ADJUSTMENT);
        } else if (dragon.getType() == ModEntities.RAEVYX.get()) {
            return RAEVYX_SCALE + babyAdjustment(dragon, RAEVYX_BABY_SCALE_ADJUSTMENT);
        } else if (dragon.getType() == ModEntities.ATROXIIA.get()) {
            return ATROXIIA_SCALE + babyAdjustment(dragon, ATROXIIA_BABY_SCALE_ADJUSTMENT);
        } else if (dragon.getType() == ModEntities.VOLITANS.get()) {
            return VOLITANS_SCALE + babyAdjustment(dragon, VOLITANS_BABY_SCALE_ADJUSTMENT);
        } else if (dragon.getType() == ModEntities.VARASUCHUS.get()) {
            return VARASUCHUS_SCALE + babyAdjustment(dragon, VARASUCHUS_BABY_SCALE_ADJUSTMENT);
        } else if (dragon.getType() == ModEntities.CINDERVANE.get()) {
            return CINDERVANE_SCALE + babyAdjustment(dragon, CINDERVANE_BABY_SCALE_ADJUSTMENT);
        } else if (dragon.getType() == ModEntities.STEGONAUT.get()) {
            return STEGONAUT_SCALE + babyAdjustment(dragon, STEGONAUT_BABY_SCALE_ADJUSTMENT);
        } else if (dragon.getType() == ModEntities.NULLJAW.get()) {
            return NULLJAW_SCALE + babyAdjustment(dragon, NULLJAW_BABY_SCALE_ADJUSTMENT);
        }
        return 30;
    }

    private int getDragonOffsetX(DragonEntity dragon) {
        if (dragon.getType() == ModEntities.IGNIVORUS.get()) {
            return IGNIVORUS_OFFSET_X + babyAdjustment(dragon, IGNIVORUS_BABY_OFFSET_X);
        } else if (dragon.getType() == ModEntities.RAEVYX.get()) {
            return RAEVYX_OFFSET_X + babyAdjustment(dragon, RAEVYX_BABY_OFFSET_X);
        } else if (dragon.getType() == ModEntities.ATROXIIA.get()) {
            return ATROXIIA_OFFSET_X + babyAdjustment(dragon, ATROXIIA_BABY_OFFSET_X);
        } else if (dragon.getType() == ModEntities.VOLITANS.get()) {
            return VOLITANS_OFFSET_X + babyAdjustment(dragon, VOLITANS_BABY_OFFSET_X);
        } else if (dragon.getType() == ModEntities.VARASUCHUS.get()) {
            return VARASUCHUS_OFFSET_X + babyAdjustment(dragon, VARASUCHUS_BABY_OFFSET_X);
        } else if (dragon.getType() == ModEntities.CINDERVANE.get()) {
            return CINDERVANE_OFFSET_X + babyAdjustment(dragon, CINDERVANE_BABY_OFFSET_X);
        } else if (dragon.getType() == ModEntities.STEGONAUT.get()) {
            return STEGONAUT_OFFSET_X + babyAdjustment(dragon, STEGONAUT_BABY_OFFSET_X);
        } else if (dragon.getType() == ModEntities.NULLJAW.get()) {
            return NULLJAW_OFFSET_X + babyAdjustment(dragon, NULLJAW_BABY_OFFSET_X);
        }
        return 0;
    }

    private int getDragonOffsetY(DragonEntity dragon) {
        if (dragon.getType() == ModEntities.IGNIVORUS.get()) {
            return IGNIVORUS_OFFSET_Y + babyAdjustment(dragon, IGNIVORUS_BABY_OFFSET_Y);
        } else if (dragon.getType() == ModEntities.RAEVYX.get()) {
            return RAEVYX_OFFSET_Y + babyAdjustment(dragon, RAEVYX_BABY_OFFSET_Y);
        } else if (dragon.getType() == ModEntities.ATROXIIA.get()) {
            return ATROXIIA_OFFSET_Y + babyAdjustment(dragon, ATROXIIA_BABY_OFFSET_Y);
        } else if (dragon.getType() == ModEntities.VOLITANS.get()) {
            return VOLITANS_OFFSET_Y + babyAdjustment(dragon, VOLITANS_BABY_OFFSET_Y);
        } else if (dragon.getType() == ModEntities.VARASUCHUS.get()) {
            return VARASUCHUS_OFFSET_Y + babyAdjustment(dragon, VARASUCHUS_BABY_OFFSET_Y);
        } else if (dragon.getType() == ModEntities.CINDERVANE.get()) {
            return CINDERVANE_OFFSET_Y + babyAdjustment(dragon, CINDERVANE_BABY_OFFSET_Y);
        } else if (dragon.getType() == ModEntities.STEGONAUT.get()) {
            return STEGONAUT_OFFSET_Y + babyAdjustment(dragon, STEGONAUT_BABY_OFFSET_Y);
        } else if (dragon.getType() == ModEntities.NULLJAW.get()) {
            return NULLJAW_OFFSET_Y + babyAdjustment(dragon, NULLJAW_BABY_OFFSET_Y);
        }
        return 0;
    }

    private DragonEntity findDragonEntity(Minecraft minecraft, UUID dragonId) {
        for (Entity entity : minecraft.level.entitiesForRendering()) {
            if (entity instanceof DragonEntity dragon && dragon.getUUID().equals(dragonId)) {
                return dragon;
            }
        }
        return null;
    }

    private void drawStaticPortrait(GuiGraphics guiGraphics, Minecraft minecraft, CodexDragonEntry selected,
                                    int leftPos, int topPos, int mouseX, int mouseY) {
        PortraitDefinition definition = PORTRAIT_DEFINITIONS.get(selected.dragonType());
        if (definition == null) {
            return;
        }

        ResourceLocation model = definition.model(selected.isBaby());
        ResourceLocation texture = selected.isBaby()
                ? dragonTexture(selected.dragonType(), "baby_" + selected.dragonType(),
                        DragonGender.fromId(selected.genderId()) == DragonGender.FEMALE)
                : resolveAdultTexture(selected, definition);
        CodexStaticPortraitRenderer.Portrait portrait = staticPortraits.compute(selected.entityId(), (dragonId, current) ->
                current != null && current.matches(model, texture)
                        ? current
                        : new CodexStaticPortraitRenderer.Portrait(dragonId, model, texture));
        int boxX = leftPos + CodexLayout.DRAGON_RENDER_BOX_X;
        int boxY = topPos + CodexLayout.DRAGON_RENDER_BOX_Y;
        int centerX = boxX + CodexLayout.DRAGON_RENDER_BOX_SIZE / 2 + definition.offsetX(selected.isBaby());
        int centerY = boxY + CodexLayout.DRAGON_RENDER_BOX_SIZE + definition.offsetY(selected.isBaby());
        float yaw = (float) Math.atan((centerX - mouseX) / 40.0F);
        float pitch = (float) Math.atan((centerY - CodexLayout.DRAGON_RENDER_BOX_SIZE - mouseY) / 40.0F);
        PoseStack poseStack = guiGraphics.pose();

        guiGraphics.enableScissor(boxX, boxY,
                boxX + CodexLayout.DRAGON_RENDER_BOX_SIZE,
                boxY + CodexLayout.DRAGON_RENDER_BOX_SIZE);
        poseStack.pushPose();
        try {
            poseStack.translate(centerX, centerY, 50.0D);
            int scale = definition.scale(selected.isBaby());
            poseStack.mulPoseMatrix(new Matrix4f().scaling(scale, scale, -scale));
            poseStack.mulPose(new Quaternionf()
                    .rotateZ((float) Math.PI)
                    .rotateX(pitch * 20.0F * ((float) Math.PI / 180.0F))
                    .rotateY(-yaw * 20.0F * ((float) Math.PI / 180.0F)));

            Lighting.setupForEntityInInventory();
            MultiBufferSource.BufferSource buffers = minecraft.renderBuffers().bufferSource();
            RenderType renderType = RenderType.entityCutoutNoCull(texture);
            staticPortraitRenderer.render(poseStack, portrait, buffers, renderType,
                    buffers.getBuffer(renderType), LightTexture.FULL_BRIGHT);
            guiGraphics.flush();
        } finally {
            poseStack.popPose();
            Lighting.setupFor3DItems();
            guiGraphics.disableScissor();
        }
    }

    private ResourceLocation resolveAdultTexture(CodexDragonEntry selected, PortraitDefinition definition) {
        ResourceLocation variantId = parseVariantId(selected.variantResourceId());
        boolean female = DragonGender.fromId(selected.genderId()) == DragonGender.FEMALE;
        int legacyId = SaintsDragonVariantRegistry.variantIdToLegacy(definition.dragonId(), variantId);
        if (legacyId == 1) {
            ResourceLocation legacyTexture = resolveLegacyVariantTexture(selected.dragonType(), female);
            if (legacyTexture != null) {
                return legacyTexture;
            }
        }
        if ("varasuchus".equals(selected.dragonType()) && "void_kissed".equals(variantId.getPath())) {
            return dragonTexture("varasuchus", "varasuchus_void_kissed", female);
        }
        if (SaintsDragonVariantRegistry.isLegacyVariant(definition.dragonId(), variantId)) {
            return dragonTexture(selected.dragonType(), selected.dragonType(), female);
        }
        return SaintsDragonVariantRegistry.adultTexture(definition.dragonId(), variantId, female);
    }

    private ResourceLocation resolveLegacyVariantTexture(String dragonType, boolean female) {
        return switch (dragonType) {
            case "ignivorus" -> dragonTexture(dragonType, "crimson_ignivorus", female);
            case "raevyx" -> dragonTexture(dragonType, "raevyx_night_gold", female);
            case "volitans" -> dragonTexture(dragonType, "volitans_bloodshot", female);
            case "cindervane" -> dragonTexture(dragonType, "cindervane_albino", female);
            default -> null;
        };
    }

    private static int babyAdjustment(DragonEntity dragon, int adjustment) {
        return dragon.isBaby() ? adjustment : 0;
    }

    private static PortraitDefinition portraitDefinition(String dragonType, int scale, int offsetX, int offsetY,
                                                         int babyScaleAdjustment, int babyOffsetX, int babyOffsetY) {
        return new PortraitDefinition(
                SaintsDragonsCommon.rl(dragonType),
                SaintsDragonsCommon.rl("geo/entity/" + dragonType + "_baked.geo.json"),
                SaintsDragonsCommon.rl("geo/entity/baby_" + dragonType + "_baked.geo.json"),
                scale,
                offsetX,
                offsetY,
                babyScaleAdjustment,
                babyOffsetX,
                babyOffsetY
        );
    }

    private static ResourceLocation dragonTexture(String dragonType, String textureName, boolean female) {
        return SaintsDragonsCommon.rl("textures/entity/" + dragonType + "/" + textureName
                + (female ? "_female" : "") + ".png");
    }

    private ResourceLocation parseVariantId(String id) {
        try {
            return new ResourceLocation(id);
        } catch (Exception ignored) {
            return com.leon.saintsdragons.server.entity.variant.SaintsDragonVariantRegistry.DEFAULT_VARIANT_ID;
        }
    }

    private record PortraitDefinition(ResourceLocation dragonId, ResourceLocation adultModel, ResourceLocation babyModel,
                                      int adultScale, int adultOffsetX, int adultOffsetY,
                                      int babyScaleAdjustment, int babyOffsetX, int babyOffsetY) {
        private ResourceLocation model(boolean baby) {
            return baby ? babyModel : adultModel;
        }

        private int scale(boolean baby) {
            return adultScale + (baby ? babyScaleAdjustment : 0);
        }

        private int offsetX(boolean baby) {
            return adultOffsetX + (baby ? babyOffsetX : 0);
        }

        private int offsetY(boolean baby) {
            return adultOffsetY + (baby ? babyOffsetY : 0);
        }
    }
}

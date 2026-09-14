package com.leon.saintsdragons.client.renderer.vfx;

import com.leon.saintsdragons.common.SaintsDragonsCommon;
import com.leon.saintsdragons.server.entity.ability.abilities.raevyx.RaevyxBeamAbility;
import com.leon.saintsdragons.server.entity.dragons.raevyx.Raevyx;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec3;

public final class RaevyxBeamIntroRenderer {
    private static final ResourceLocation WIDE_STAR = SaintsDragonsCommon.rl("textures/particle/shared/stars/wider_star.png");
    private static final ResourceLocation[] MORE_SWIRL = frames("raevyx/beam/more_swirl/more_swirl", 25);
    private static final ResourceLocation[] STEAM = frames("shared/explosions/steamy_explosion/steamy_explosion", 16);
    private static final ResourceLocation[] SHARP = frames("shared/explosions/sharp_explosion/sharp_explosion", 8);
    private static final ResourceLocation[] GLASS = {SaintsDragonsCommon.rl("textures/particle/shared/misc/glass_shatter.png")};
    private static final float GLASS_TICKS = 5.0F;
    private static final float GLASS_START_HALF_SIZE = 4.0F;
    private static final float GLASS_END_HALF_SIZE = 5.0F;
    private static final float GLASS_ALPHA = 0.85F;
    private static final float FRAME_TICKS = 0.5F;
    private static final float SWIRL_FRAME_TICKS = 0.5F;
    private static final float SWIRL_HALF_SIZE = 4.0F;
    private static final float SWIRL_ANCHOR_U = 0.565F;
    private static final float STEAM_FRAME_TICKS = 0.25F;
    private static final float FLASH_TICKS = 2.0F;
    private static final float FLASH_MAX_HALF_SIZE = 4.0F;
    private static final float STEAM_HALF_SIZE = 2.0F;
    private static final float FORWARD_OFFSET = 1.5F;
    private static final float STEAM_START = RaevyxBeamAbility.STARTUP_TICKS - STEAM.length * STEAM_FRAME_TICKS;
    private static final float FLASH_START = STEAM_START - FLASH_TICKS;
    private static final float SWIRL_START = FLASH_START - MORE_SWIRL.length * SWIRL_FRAME_TICKS;
    private static final AttachedPlaneFlipbookRenderer.Style SHARP_STYLE =
            new AttachedPlaneFlipbookRenderer.Style(5.0F, 1.0F, FRAME_TICKS, 1.0F, 0.0F);

    private RaevyxBeamIntroRenderer() {
    }

    public static boolean isActive(Raevyx entity, float partialTick) {
        float chargeAge = entity.getClientBeamChargeAge(partialTick);
        float fireAge = entity.getClientBeamFireAge(partialTick);
        return (chargeAge >= SWIRL_START && chargeAge < FLASH_START)
                || (chargeAge >= FLASH_START && chargeAge < FLASH_START + FLASH_TICKS)
                || (chargeAge >= STEAM_START && chargeAge < RaevyxBeamAbility.STARTUP_TICKS)
                || (fireAge >= 0.0F && fireAge < Math.max(SHARP.length * FRAME_TICKS, GLASS_TICKS));
    }

    public static void render(Raevyx entity, PoseStack poseStack, MultiBufferSource buffers,
                              float partialTick, Vec3 mouth, Vec3 direction) {
        if (!isActive(entity, partialTick)) {
            return;
        }
        Vec3 forward = direction.lengthSqr() > 1.0E-6 ? direction.normalize() : entity.getViewVector(partialTick);
        Vec3 origin = mouth.add(forward.scale(FORWARD_OFFSET));
        float x = (float) origin.x;
        float y = (float) origin.y;
        float z = (float) origin.z;
        boolean gold = entity.getTextureVariant() == Raevyx.VARIANT_NIGHT_GOLD;
        float green = gold ? 0.75F : 0.0F;
        float blue = gold ? 0.15F : 0.0F;
        float swirlAge = entity.getClientBeamChargeAge(partialTick) - SWIRL_START;
        if (swirlAge >= 0.0F && swirlAge < MORE_SWIRL.length * SWIRL_FRAME_TICKS) {
            BillboardFlashRenderer.renderAnchoredQuad(poseStack, buffers,
                    MORE_SWIRL[Mth.floor(swirlAge / SWIRL_FRAME_TICKS)],
                    x, y, z, SWIRL_HALF_SIZE, 0.0F, 1.0F, green, blue, 1.0F,
                    SWIRL_ANCHOR_U, 0.5F);
        }
        float introAge = entity.getClientBeamChargeAge(partialTick) - FLASH_START;
        if (introAge >= 0.0F && introAge < FLASH_TICKS) {
            float life = introAge / FLASH_TICKS;
            float sizeEnvelope = life < 0.3F ? smoothStep(life / 0.3F)
                    : 1.0F - smoothStep((life - 0.3F) / 0.7F);
            BillboardFlashRenderer.renderQuad(poseStack, buffers, WIDE_STAR, x, y, z,
                    FLASH_MAX_HALF_SIZE * sizeEnvelope, 0.0F, 1.0F, green, blue, 1.0F);
        }
        float steamAge = entity.getClientBeamChargeAge(partialTick) - STEAM_START;
        if (steamAge >= 0.0F && steamAge < STEAM.length * STEAM_FRAME_TICKS) {
            BillboardFlashRenderer.renderQuad(poseStack, buffers, STEAM[Mth.floor(steamAge / STEAM_FRAME_TICKS)],
                    x, y, z, STEAM_HALF_SIZE, 0.0F, 1.0F, green, blue, 1.0F);
        }
        float fireAge = entity.getClientBeamFireAge(partialTick);
        if (fireAge >= 0.0F && fireAge < Math.max(SHARP.length * FRAME_TICKS, GLASS_TICKS)) {
            poseStack.pushPose();
            try {
                poseStack.translate(x, y, z);
                float xRot = (float) Math.acos(Mth.clamp(forward.y, -1.0D, 1.0D));
                float yRot = (float) Math.atan2(forward.z, forward.x);
                poseStack.mulPose(Axis.YP.rotation(Mth.PI / 2.0F - yRot));
                poseStack.mulPose(Axis.XP.rotation(-Mth.PI / 2.0F + xRot));
                AttachedPlaneFlipbookRenderer.renderOnce(poseStack, buffers, SHARP, SHARP_STYLE,
                        fireAge, 0.0F, 1.0F, green, blue);
                if (fireAge < GLASS_TICKS) {
                    float progress = smoothStep(fireAge / GLASS_TICKS);
                    float size = Mth.lerp(progress, GLASS_START_HALF_SIZE, GLASS_END_HALF_SIZE);
                    float alpha = GLASS_ALPHA * (1.0F - progress);
                    AttachedPlaneFlipbookRenderer.Style glassStyle = new AttachedPlaneFlipbookRenderer.Style(
                            size, alpha, GLASS_TICKS, GLASS_TICKS, 0.02F);
                    AttachedPlaneFlipbookRenderer.renderOnce(poseStack, buffers, GLASS, glassStyle,
                            fireAge, 0.0F, 1.0F, 1.0F, 1.0F);
                }
            } finally {
                poseStack.popPose();
            }
        }
    }

    private static ResourceLocation[] frames(String prefix, int count) {
        ResourceLocation[] textures = new ResourceLocation[count];
        for (int i = 0; i < count; i++) {
            textures[i] = SaintsDragonsCommon.rl("textures/particle/" + prefix + i + ".png");
        }
        return textures;
    }

    private static float smoothStep(float value) {
        float t = Mth.clamp(value, 0.0F, 1.0F);
        return t * t * (3.0F - 2.0F * t);
    }
}

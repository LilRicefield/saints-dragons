package com.leon.saintsdragons.client.renderer.vfx;

import com.leon.saintsdragons.common.SaintsDragonsCommon;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec3;

public final class RaevyxBeamImpactRenderer {
    private static final ResourceLocation[] CIRCLE = frames("shared/rings/circle_thinning/circle_thinning", 12);
    private static final ResourceLocation[] ZAP = frames("shared/lightning/lightning_zap/lightning_zap", 16);
    private static final ResourceLocation STAR = SaintsDragonsCommon.rl("textures/particle/shared/stars/star.png");
    private static final float CIRCLE_FRAME_TICKS = 0.25F;
    private static final float ZAP_FRAME_TICKS = 1.0F;
    private static final float CIRCLE_HALF_SIZE = 4.0F;
    private static final float ZAP_HALF_SIZE = 5.0F;
    private static final BillboardFlashRenderer.Style STAR_STYLE =
            new BillboardFlashRenderer.Style(7.5F, 4.0F, 1.0F, 0.85F, 0.3F);
    private static final long STAR_SEED_SALT = 0x6A09E667F3BCC909L;
    private static final double SURFACE_OFFSET = 0.05D;

    private RaevyxBeamImpactRenderer() {
    }

    public static void render(PoseStack entityPose, MultiBufferSource buffers, Vec3 tip,
                              Vec3 direction, float age, boolean gold, long seed) {
        if (direction.lengthSqr() < 1.0E-6D || age < 0.0F) {
            return;
        }
        Vec3 forward = direction.normalize();
        Vec3 origin = tip.subtract(forward.scale(SURFACE_OFFSET));
        float green = gold ? 0.75F : 0.0F;
        float blue = gold ? 0.15F : 0.0F;
        float circleLifetime = CIRCLE.length * CIRCLE_FRAME_TICKS;
        float circleAge = age % circleLifetime;
        int circleFrame = Mth.floor(circleAge / CIRCLE_FRAME_TICKS);
        float life = circleAge / circleLifetime;
        float circleAlpha = 1.0F - life * life * (3.0F - 2.0F * life);
        BillboardFlashRenderer.renderQuad(entityPose, buffers, CIRCLE[circleFrame],
                (float) origin.x, (float) origin.y, (float) origin.z,
                CIRCLE_HALF_SIZE, 0.0F, 1.0F, green, blue, circleAlpha);
        int zapFrame = Mth.floor((age % (ZAP.length * ZAP_FRAME_TICKS)) / ZAP_FRAME_TICKS);
        BillboardFlashRenderer.renderQuad(entityPose, buffers, ZAP[zapFrame],
                (float) origin.x, (float) origin.y, (float) origin.z,
                ZAP_HALF_SIZE, 0.0F, 1.0F, green, blue, 1.0F);
        BillboardFlashRenderer.render(entityPose, buffers, STAR,
                (float) origin.x, (float) origin.y, (float) origin.z,
                1.0F, age, seed ^ STAR_SEED_SALT, STAR_STYLE, 1.0F, green, blue);
    }

    private static ResourceLocation[] frames(String prefix, int count) {
        ResourceLocation[] textures = new ResourceLocation[count];
        for (int i = 0; i < count; i++) {
            textures[i] = SaintsDragonsCommon.rl("textures/particle/" + prefix + i + ".png");
        }
        return textures;
    }
}

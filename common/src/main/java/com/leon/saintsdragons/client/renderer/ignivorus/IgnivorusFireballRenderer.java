package com.leon.saintsdragons.client.renderer.ignivorus;

import com.leon.saintsdragons.client.renderer.ShaderPassCompatibility;
import com.leon.saintsdragons.client.model.ignivorus.IgnivorusFireballModel;
import com.leon.saintsdragons.client.renderer.vfx.ScrollingFireballVertexConsumer;
import com.leon.saintsdragons.client.renderer.vfx.BillboardFlashRenderer;
import com.leon.saintsdragons.client.renderer.vfx.BeamRenderTypes;
import com.leon.saintsdragons.common.SaintsDragonsCommon;
import net.minecraft.resources.ResourceLocation;
import com.leon.saintsdragons.server.entity.effect.ignivorus.IgnivorusFireballEntity;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.world.phys.Vec3;
import software.bernie.geckolib.cache.object.BakedGeoModel;
import software.bernie.geckolib.renderer.GeoEntityRenderer;

public class IgnivorusFireballRenderer extends GeoEntityRenderer<IgnivorusFireballEntity> {
    private static final float FRAME_TICKS = 2.0F;
    private static final int FRAME_COUNT = 2;
    private static final ResourceLocation STAR = SaintsDragonsCommon.rl("textures/particle/shared/stars/star.png");
    private static final BillboardFlashRenderer.Style STAR_STYLE =
            new BillboardFlashRenderer.Style(7.5F, 4.0F, 1.0F, 0.85F, 0.3F);

    @Override
    public void render(IgnivorusFireballEntity entity, float entityYaw, float partialTick,
                       PoseStack poses, MultiBufferSource buffers, int packedLight) {
        if (ShaderPassCompatibility.isIrisShadowPass()) return;
        super.render(entity, entityYaw, partialTick, poses, buffers, LightTexture.FULL_BRIGHT);
        if (entity.getVisualScale() >= 8.0F) {
            MultiBufferSource compatible = ignored -> buffers.getBuffer(BeamRenderTypes.translucent(STAR));
            BillboardFlashRenderer.render(poses, compatible, STAR,
                    0, entity.getBbHeight() * 0.5F, 0, 1.0F, entity.getVisualAge(partialTick),
                    entity.getUUID().getLeastSignificantBits() ^ 0x6A09E667F3BCC909L,
                    STAR_STYLE, 147 / 255.0F, 110 / 255.0F, 1.0F);
        }
    }

    public IgnivorusFireballRenderer(EntityRendererProvider.Context context) {
        super(context, new IgnivorusFireballModel());
        this.shadowRadius = 0.0F;
    }

    @Override
    protected void applyRotations(IgnivorusFireballEntity entity, PoseStack poses,
                                  float ageInTicks, float rotationYaw, float partialTick) {
        Vec3 velocity = entity.getDeltaMovement();
        double horizontal = velocity.horizontalDistance();
        float yaw = horizontal > 1.0E-6
                ? (float) Math.toDegrees(Math.atan2(-velocity.x, velocity.z)) : entity.getYRot();
        float pitch = (float) Math.toDegrees(Math.atan2(-velocity.y, horizontal));
        poses.translate(0.0, entity.getBbHeight() * 0.5, 0.0);
        poses.mulPose(Axis.YP.rotationDegrees(180.0F - yaw));
        poses.mulPose(Axis.XP.rotationDegrees(-pitch));
        if (entity.getVisualScale() >= 6.0F) {
            float scale = entity.getVisualScale() >= 8.0F ? 3.0F : 2.4F;
            poses.scale(scale, scale, scale);
            poses.translate(0.0D, -7.0D / 16.0D, 0.0D);
        } else {
            float scale = entity.getVisualScale() * 0.35F;
            poses.scale(scale, scale, scale);
            poses.translate(0.0, -0.25, 0.0);
        }
    }

    @Override
    public void actuallyRender(PoseStack poses, IgnivorusFireballEntity entity, BakedGeoModel model,
                               RenderType renderType, MultiBufferSource buffers, VertexConsumer buffer,
                               boolean isReRender, float partialTick, int packedLight, int packedOverlay,
                               float red, float green, float blue, float alpha) {
        float age = entity.getVisualAge(partialTick);
        int frame = (int) (age / FRAME_TICKS) % FRAME_COUNT;
        float offset = frame / (float) FRAME_COUNT;
        super.actuallyRender(poses, entity, model, renderType, buffers,
                new ScrollingFireballVertexConsumer(buffer, offset), isReRender, partialTick,
                LightTexture.FULL_BRIGHT, packedOverlay, red, green, blue, alpha);
    }
}

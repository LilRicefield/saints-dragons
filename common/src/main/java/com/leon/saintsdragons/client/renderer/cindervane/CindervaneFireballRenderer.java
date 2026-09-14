package com.leon.saintsdragons.client.renderer.cindervane;

import com.leon.saintsdragons.common.SaintsDragonsCommon;
import com.leon.saintsdragons.client.renderer.ShaderPassCompatibility;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.client.renderer.texture.OverlayTexture;
import com.leon.saintsdragons.client.model.cindervane.CindervaneFireballModel;
import com.leon.saintsdragons.client.renderer.vfx.ScrollingFireballVertexConsumer;
import com.leon.saintsdragons.server.entity.effect.cindervane.CindervaneFireballEntity;
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

public class CindervaneFireballRenderer extends GeoEntityRenderer<CindervaneFireballEntity> {
    private static final float FRAME_TICKS = 2.0F;
    private static final int FRAME_COUNT = 2;
    private static final ResourceLocation[] FIRE = frames("shared/fire/fire/fire", 17);
    private static final ResourceLocation[] SPEC = frames("shared/fire/fire_spec/fire_spec", 12);
    private static final ResourceLocation[] EMBER = { SaintsDragonsCommon.rl("textures/particle/shared/emitters/ember.png") };
    private static final ResourceLocation[] EMITTER = { SaintsDragonsCommon.rl("textures/particle/shared/emitters/glowing_emitter.png") };

    private static ResourceLocation[] frames(String prefix, int count) {
        ResourceLocation[] frames = new ResourceLocation[count];
        for (int i = 0; i < count; i++) {
            frames[i] = SaintsDragonsCommon.rl("textures/particle/" + prefix + i + ".png");
        }
        return frames;
    }

    @Override
    public void render(CindervaneFireballEntity entity, float entityYaw, float partialTick,
                       PoseStack poses, MultiBufferSource buffers, int packedLight) {
        if (ShaderPassCompatibility.isIrisShadowPass()) return;
        super.render(entity, entityYaw, partialTick, poses, buffers, LightTexture.FULL_BRIGHT);
        float age = entity.tickCount + partialTick;
        Vec3 tail = entity.getDeltaMovement().normalize().scale(-0.35);
        poses.pushPose();
        poses.translate(0.0, entity.getBbHeight() * 0.5, 0.0);
        poses.translate(tail.x, tail.y, tail.z);
        poses.mulPose(this.entityRenderDispatcher.cameraOrientation());
        layer(poses, buffers, FIRE, age, 0.5F, 0.62F, -0.40F, -0.12F, 0.0F, 0.48F, 0.08F, 0.85F);
        float specProgress = (age % (SPEC.length * 0.5F)) / (SPEC.length * 0.5F);
        float specFade = Math.min(1.0F, Math.max(0.0F, (specProgress - 0.5F) * 2.0F));
        float specAlpha = 0.9F * (1.0F - specFade * specFade * (3.0F - 2.0F * specFade));
        layer(poses, buffers, SPEC, age, 0.5F, 0.46F, 0.45F, 0.25F, 0.015F, 0.78F, 0.28F, specAlpha);
        poses.popPose();
        renderAttachedSparks(entity, poses, buffers, age);
    }

    private void renderAttachedSparks(CindervaneFireballEntity entity, PoseStack poses,
                                      MultiBufferSource buffers, float age) {
        for (int i = 0; i < 17; i++) {
            float time = age + i * 2.75F;
            int cycle = (int) (time / 16.0F);
            float progress = (time % 16.0F) / 16.0F;
            var random = net.minecraft.util.RandomSource.create(
                    entity.getUUID().getLeastSignificantBits() ^ (i * 73428767L) ^ (cycle * 912931L));
            double angle = random.nextDouble() * Math.PI * 2.0;
            double height = random.nextDouble() * 2.0 - 1.0;
            double horizontal = Math.sqrt(1.0 - height * height);
            double radius = 0.5 + progress * 0.45;
            float pulse = (float) Math.sin(progress * Math.PI);
            boolean ember = i < 5;
            float grow = smoothStep(progress / 0.15F);
            float shrink = 1.0F - smoothStep((progress - 0.60F) / 0.40F);
            float halfSize = ember
                    ? (0.045F + random.nextFloat() * 0.025F) * grow * shrink
                    : 0.055F * pulse;
            poses.pushPose();
            poses.translate(Math.cos(angle) * horizontal * radius,
                    entity.getBbHeight() * 0.5 + height * radius + progress * 0.15,
                    Math.sin(angle) * horizontal * radius);
            poses.mulPose(this.entityRenderDispatcher.cameraOrientation());
            if (ember) {
                poses.mulPose(Axis.ZP.rotation((float) angle + progress * 0.8F));
                poses.scale(0.5F, 1.0F, 1.0F);
            }
            layer(poses, buffers, ember ? EMBER : EMITTER, 0.0F, 1.0F,
                    halfSize, 0.0F, 0.0F, 0.0F,
                    ember ? 0.48F : 0.72F, ember ? 0.055F : 0.16F,
                    ember ? 0.95F * shrink : pulse * 0.9F);
            poses.popPose();
        }
    }

    private static float smoothStep(float value) {
        float t = Math.max(0.0F, Math.min(1.0F, value));
        return t * t * (3.0F - 2.0F * t);
    }

    private static void layer(PoseStack poses, MultiBufferSource buffers, ResourceLocation[] frames,
                              float age, float ticksPerFrame, float halfSize, float x, float y, float z,
                              float green, float blue, float alpha) {
        ResourceLocation texture = frames[(int) (age / ticksPerFrame) % frames.length];
        RenderType type = ShaderPassCompatibility.isShaderPackInUse()
                ? RenderType.entityTranslucent(texture) : RenderType.entityTranslucentEmissive(texture);
        VertexConsumer vertices = buffers.getBuffer(type);
        vertex(vertices, poses.last(), x - halfSize, y - halfSize, z, 0, 1, green, blue, alpha);
        vertex(vertices, poses.last(), x + halfSize, y - halfSize, z, 1, 1, green, blue, alpha);
        vertex(vertices, poses.last(), x + halfSize, y + halfSize, z, 1, 0, green, blue, alpha);
        vertex(vertices, poses.last(), x - halfSize, y + halfSize, z, 0, 0, green, blue, alpha);
    }

    private static void vertex(VertexConsumer vertices, PoseStack.Pose pose, float x, float y, float z,
                               float u, float v, float green, float blue, float alpha) {
        vertices.vertex(pose.pose(), x, y, z).color(1.0F, green, blue, alpha).uv(u, v)
                .overlayCoords(OverlayTexture.NO_OVERLAY).uv2(LightTexture.FULL_BRIGHT)
                .normal(pose.normal(), 0.0F, 0.0F, 1.0F).endVertex();
    }

    public CindervaneFireballRenderer(EntityRendererProvider.Context context) {
        super(context, new CindervaneFireballModel());
        this.shadowRadius = 0.0F;
    }

    @Override
    protected void applyRotations(CindervaneFireballEntity entity, PoseStack poses,
                                  float ageInTicks, float rotationYaw, float partialTick) {
        Vec3 velocity = entity.getDeltaMovement();
        double horizontal = velocity.horizontalDistance();
        float yaw = horizontal > 1.0E-6
                ? (float) Math.toDegrees(Math.atan2(-velocity.x, velocity.z)) : entity.getYRot();
        float pitch = (float) Math.toDegrees(Math.atan2(-velocity.y, horizontal));
        poses.translate(0.0, entity.getBbHeight() * 0.5, 0.0);
        // The model's nose faces -Z, with its tail extending toward +Z.
        poses.mulPose(Axis.YP.rotationDegrees(180.0F - yaw));
        poses.mulPose(Axis.XP.rotationDegrees(-pitch));
        poses.translate(0.0, -0.25, 0.0);
    }

    @Override
    public void actuallyRender(PoseStack poses, CindervaneFireballEntity entity, BakedGeoModel model,
                               RenderType renderType, MultiBufferSource buffers, VertexConsumer buffer,
                               boolean isReRender, float partialTick, int packedLight, int packedOverlay,
                               float red, float green, float blue, float alpha) {
        float age = entity.tickCount + partialTick;
        int frame = (int) (age / FRAME_TICKS) % FRAME_COUNT;
        float offset = frame / (float) FRAME_COUNT;
        super.actuallyRender(poses, entity, model, renderType, buffers,
                new ScrollingFireballVertexConsumer(buffer, offset), isReRender, partialTick,
                LightTexture.FULL_BRIGHT, packedOverlay, red, green, blue, alpha);
    }
}

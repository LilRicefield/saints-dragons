package com.leon.saintsdragons.client.renderer.vfx;

import com.leon.saintsdragons.client.renderer.ShaderPassCompatibility;
import com.leon.saintsdragons.common.SaintsDragonsCommon;
import com.leon.saintsdragons.server.entity.ability.abilities.raevyx.RaevyxSummonStormAbility;
import com.leon.saintsdragons.server.entity.dragons.raevyx.Raevyx;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import org.joml.Quaternionf;

public final class RaevyxSummonStormRenderer {
    public static final float DURATION_TICKS = RaevyxSummonStormAbility.GROUND_INTRO_TICKS;
    private static final float ZAP_FRAME_TICKS = 0.5F;
    private static final float RING_FRAME_TICKS = 1.0F;
    private static final float RING_RADIUS = 8.0F;
    private static final int OUTWARD_BOLT_COUNT = 6;
    private static final float OUTWARD_BOLT_TICKS = 4.0F;
    private static final ResourceLocation[] RINGS = frames("shared/rings/second_impact_ring/second_impact_ring", 8);
    private static final ResourceLocation[] ZAPS = frames("shared/lightning/lightning_zap/lightning_zap", 16);
    private static final ResourceLocation[] BURST = frames("shared/lightning/lightning_burst/lightning_burst", 8);
    private static final ResourceLocation[] BURST_GROUND = frames("shared/lightning/lightning_burst/lightning_burst_ground", 10);
    private static final ResourceLocation[] BURST_SURROUND = frames("shared/lightning/lightning_burst/lightning_burst_surround", 6);
    private static final float BURST_FRAME_TICKS = (float) RaevyxSummonStormAbility.GROUND_BURST_TICKS / BURST_GROUND.length;
    public static final float TOTAL_DURATION_TICKS = RaevyxSummonStormAbility.GROUND_SUPERCHARGE_START_TICKS;
    private static final ResourceLocation GLOW = SaintsDragonsCommon.rl("textures/particle/shared/emitters/glowing_emitter.png");

    private RaevyxSummonStormRenderer() {}

    public static void render(Raevyx dragon, Vec3 bodyWorld, PoseStack poses,
                              MultiBufferSource buffers, float partialTick) {
        float age = dragon.getGroundStormVisualAge(partialTick);
        if (age < 0 || age >= TOTAL_DURATION_TICKS
                || dragon.isBaby() || ShaderPassCompatibility.isIrisShadowPass()) return;
        boolean gold = dragon.getTextureVariant() == Raevyx.VARIANT_NIGHT_GOLD;
        float red = 1.0F;
        float green = gold ? 0.78F : 0.06F;
        float blue = gold ? 0.08F : 0.025F;
        Vec3 center = bodyWorld == null ? new Vec3(0, dragon.getBbHeight() * 0.6D, 0)
                : bodyWorld.subtract(dragon.position());
        if (age >= DURATION_TICKS) {
            float burstAge = age - DURATION_TICKS;
            renderGroundTexture(dragon, poses, buffers, BURST_GROUND[Mth.floor(burstAge / BURST_FRAME_TICKS)],
                    partialTick, 10.0F, red, green, blue, 1.0F);
            renderBurstBillboard(BURST_SURROUND, burstAge, center, poses, buffers, 10.0F, red, green, blue);
            renderBurstBillboard(BURST, burstAge, center, poses, buffers, 8.0F, red, green, blue);
            return;
        }
        float visibility = Mth.clamp(age / 2.0F, 0, 1)
                * Mth.clamp((DURATION_TICKS - age) / 5.0F, 0, 1);

        renderGroundRing(dragon, poses, buffers, age, partialTick, red, green, blue, visibility);
        renderOutwardLightning(dragon, poses, buffers, age, red, green, blue, visibility);
        ResourceLocation zap = ZAPS[Mth.floor(age / ZAP_FRAME_TICKS) % ZAPS.length];
        MultiBufferSource lightningBuffers = ignored -> buffers.getBuffer(BeamRenderTypes.translucent(zap));
        BillboardFlashRenderer.renderQuad(poses, lightningBuffers, zap,
                (float) center.x, (float) center.y, (float) center.z,
                8.0F, 0, red, green, blue, visibility);
        renderAbsorption(dragon, center, poses, buffers, age, red, green, blue);
    }

    private static void renderBurstBillboard(ResourceLocation[] frames, float age, Vec3 center,
                                             PoseStack poses, MultiBufferSource buffers, float size,
                                             float red, float green, float blue) {
        int frame = Mth.floor(age / BURST_FRAME_TICKS);
        if (frame >= frames.length) return;
        ResourceLocation texture = frames[frame];
        MultiBufferSource burstBuffers = ignored -> buffers.getBuffer(BeamRenderTypes.translucent(texture));
        BillboardFlashRenderer.renderQuad(poses, burstBuffers, texture,
                (float) center.x, (float) center.y, (float) center.z, size, 0, red, green, blue, 1.0F);
    }

    private static void renderOutwardLightning(Raevyx dragon, PoseStack poses, MultiBufferSource buffers,
                                               float age, float red, float green, float blue, float visibility) {
        var bounds = dragon.getBoundingBox().move(dragon.position().scale(-1));
        Vec3 center = bounds.getCenter();
        double halfX = bounds.getXsize() * 0.5D;
        double halfY = bounds.getYsize() * 0.5D;
        double halfZ = bounds.getZsize() * 0.5D;
        var vertices = buffers.getBuffer(RenderType.lightning());
        for (int slot = 0; slot < OUTWARD_BOLT_COUNT; slot++) {
            float time = age - slot * 0.45F;
            if (time < 0) continue;
            int cycle = Mth.floor(time / OUTWARD_BOLT_TICKS);
            float boltAge = time % OUTWARD_BOLT_TICKS;
            long seed = dragon.getUUID().getLeastSignificantBits()
                    ^ (slot * 73428767L) ^ (cycle * 912931L);
            RandomSource random = RandomSource.create(seed);
            double angle = random.nextDouble() * Math.PI * 2;
            double up = 0.1D + random.nextDouble() * 0.65D;
            double horizontal = Math.sqrt(1 - up * up);
            Vec3 direction = new Vec3(Math.cos(angle) * horizontal, up, Math.sin(angle) * horizontal);
            double edge = Math.min(halfY / up, Math.min(
                    halfX / Math.max(1.0E-6, Math.abs(direction.x)),
                    halfZ / Math.max(1.0E-6, Math.abs(direction.z))));
            Vec3 start = center.add(direction.scale(edge));
            float reach = 3.0F + random.nextFloat() * 4.0F;
            float length = reach * Mth.clamp(boltAge / 0.45F, 0, 1);
            if (length < 0.05F) continue;
            float alpha = visibility * Mth.clamp(boltAge / 0.15F, 0, 1)
                    * (1 - boltAge / OUTWARD_BOLT_TICKS);
            poses.pushPose();
            try {
                poses.translate(start.x, start.y, start.z);
                poses.mulPose(new Quaternionf().rotationTo(0, 0, 1,
                        (float) direction.x, (float) direction.y, (float) direction.z));
                ProceduralBeamLightningRenderer.emitBolt(vertices, poses.last().pose(), length, seed,
                        0.75F, alpha, red, green, blue);
            } finally {
                poses.popPose();
            }
        }
    }

    private static void renderGroundRing(Raevyx dragon, PoseStack poses, MultiBufferSource buffers,
                                          float age, float partialTick, float red, float green, float blue, float alpha) {
        ResourceLocation texture = RINGS[Mth.floor(age / RING_FRAME_TICKS) % RINGS.length];
        renderGroundTexture(dragon, poses, buffers, texture, partialTick, RING_RADIUS, red, green, blue, alpha);
    }

    private static void renderGroundTexture(Raevyx dragon, PoseStack poses, MultiBufferSource buffers,
                                             ResourceLocation texture, float partialTick, float radius,
                                             float red, float green, float blue, float alpha) {
        Vec3 origin = dragon.getPosition(partialTick);
        var hit = dragon.level().clip(new ClipContext(origin.add(0, 0.5D, 0), origin.add(0, -6, 0),
                ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, dragon));
        if (hit.getType() != HitResult.Type.BLOCK) return;
        poses.pushPose();
        try {
            poses.translate(0, hit.getLocation().y - origin.y + 0.04D, 0);
            poses.mulPose(Axis.XP.rotationDegrees(-90));
            MultiBufferSource ringBuffers = ignored -> buffers.getBuffer(BeamRenderTypes.translucent(texture));
            AttachedPlaneFlipbookRenderer.renderOnce(poses, ringBuffers, new ResourceLocation[] {texture},
                    new AttachedPlaneFlipbookRenderer.Style(radius, alpha, 1, 1, 0),
                    0, 0, red, green, blue);
        } finally {
            poses.popPose();
        }
    }

    private static void renderAbsorption(Raevyx dragon, Vec3 center, PoseStack poses,
                                         MultiBufferSource buffers, float age, float red, float green, float blue) {
        MultiBufferSource glowBuffers = ignored -> buffers.getBuffer(BeamRenderTypes.translucent(GLOW));
        // Deterministic paths give smooth attraction without spawning particles every render frame.
        for (int i = 0; i < 48; i++) {
            float localAge = age - i * 0.15F;
            if (localAge < 0) continue;
            int cycle = Mth.floor(localAge / 12.0F);
            float progress = (localAge % 12.0F) / 12.0F;
            RandomSource random = RandomSource.create(dragon.getUUID().getLeastSignificantBits()
                    ^ (i * 73428767L) ^ (cycle * 912931L));
            double angle = random.nextDouble() * Math.PI * 2;
            double vertical = random.nextDouble() * 2 - 1;
            double horizontal = Math.sqrt(1 - vertical * vertical);
            double radius = (5 + random.nextDouble() * 5) * (1 - progress * progress);
            Vec3 point = center.add(Math.cos(angle) * horizontal * radius,
                    vertical * radius * 0.6D, Math.sin(angle) * horizontal * radius);
            // Keep attraction above the ground instead of wasting half the glows below it.
            float y = (float) Math.max(0.12D, point.y);
            float alpha = Mth.clamp(progress / 0.12F, 0, 1)
                    * Mth.clamp((1 - progress) / 0.15F, 0, 1)
                    * Mth.clamp((DURATION_TICKS - age) / 4.0F, 0, 1);
            BillboardFlashRenderer.renderQuad(poses, glowBuffers, GLOW, (float) point.x, y, (float) point.z,
                    (0.10F + random.nextFloat() * 0.12F) * (1 - progress * 0.5F), 0,
                    red, green, blue, alpha);
        }
    }

    private static ResourceLocation[] frames(String prefix, int count) {
        ResourceLocation[] frames = new ResourceLocation[count];
        for (int i = 0; i < count; i++) frames[i] = SaintsDragonsCommon.rl("textures/particle/" + prefix + i + ".png");
        return frames;
    }
}

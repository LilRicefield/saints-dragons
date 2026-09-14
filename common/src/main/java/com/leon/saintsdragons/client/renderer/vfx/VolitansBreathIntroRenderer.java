package com.leon.saintsdragons.client.renderer.vfx;

import com.leon.saintsdragons.common.SaintsDragonsCommon;
import com.leon.saintsdragons.server.entity.dragons.volitans.Volitans;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec3;

public final class VolitansBreathIntroRenderer {
    private static final ResourceLocation[] START = frames("volitans/water/water_breath_start/water_breath_start", 17);
    private static final ResourceLocation[] IMPACT = frames("volitans/water/water_breathing_impact/water_breathing_impact", 6);
    private static final float START_FRAME_TICKS = 0.75F;
    private static final float IMPACT_FRAME_TICKS = 1.0F;
    private static final float START_HALF_SIZE = 2.5F;
    private static final float IMPACT_HALF_SIZE = 2.5F;
    private static final double FORWARD_OFFSET = 0.6D;

    private VolitansBreathIntroRenderer() {}

    public static void render(Volitans dragon, PoseStack poses, MultiBufferSource buffers, float partialTick) {
        if (!dragon.isAlive() || dragon.isPoisonBreathMode()) return;
        float introAge = dragon.getBreathIntroAge(partialTick);
        float fireAge = dragon.getBreathFireAge(partialTick);
        boolean intro = introAge >= 0 && introAge < START.length * START_FRAME_TICKS;
        boolean impact = fireAge >= 0 && fireAge < IMPACT.length * IMPACT_FRAME_TICKS;
        if (!intro && !impact) return;

        Vec3 mouth = dragon.getClientLocatorPosition("breathBoneOrigin");
        if (mouth == null) return;
        var rider = dragon.getControllingPassenger();
        Vec3 forward = rider != null ? rider.getViewVector(partialTick) : dragon.getViewVector(partialTick);
        Vec3 renderOrigin = new Vec3(Mth.lerp(partialTick, dragon.xOld, dragon.getX()),
                Mth.lerp(partialTick, dragon.yOld, dragon.getY()),
                Mth.lerp(partialTick, dragon.zOld, dragon.getZ()));
        Vec3 anchor = mouth.add(forward.scale(FORWARD_OFFSET)).subtract(renderOrigin);
        ResourceLocation texture = intro ? START[Mth.floor(introAge / START_FRAME_TICKS)]
                : IMPACT[Mth.floor(fireAge / IMPACT_FRAME_TICKS)];
        BillboardFlashRenderer.renderQuad(poses, buffers, texture,
                (float) anchor.x, (float) anchor.y, (float) anchor.z,
                intro ? START_HALF_SIZE : IMPACT_HALF_SIZE, 0.0F, 1.0F, 1.0F, 1.0F, 1.0F);
    }

    private static ResourceLocation[] frames(String prefix, int count) {
        ResourceLocation[] textures = new ResourceLocation[count];
        for (int i = 0; i < count; i++) {
            textures[i] = SaintsDragonsCommon.rl("textures/particle/" + prefix + i + ".png");
        }
        return textures;
    }
}

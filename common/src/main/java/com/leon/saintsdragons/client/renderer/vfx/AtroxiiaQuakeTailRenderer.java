package com.leon.saintsdragons.client.renderer.vfx;

import com.leon.saintsdragons.client.renderer.ShaderPassCompatibility;
import com.leon.saintsdragons.common.SaintsDragonsCommon;
import com.leon.saintsdragons.common.registry.ModParticles;
import com.leon.saintsdragons.server.entity.dragons.atroxiia.Atroxiia;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec3;
import java.util.Map;
import java.util.WeakHashMap;

public final class AtroxiiaQuakeTailRenderer {
    private static final float FLASH_TICKS = 10.0F;
    private static final float FADE_IN_TICKS = 0.5F;
    private static final ResourceLocation STAR = SaintsDragonsCommon.rl("textures/particle/shared/stars/wider_star.png");
    private static final Map<Atroxiia, Integer> LAST_BURST = new WeakHashMap<>();

    public static void render(Atroxiia dragon, Vec3 tip, PoseStack poses, MultiBufferSource buffers, float partialTick) {
        if (tip == null || !dragon.isAlive() || ShaderPassCompatibility.isIrisShadowPass()) return;
        int eventTick = dragon.getClientQuakeTailFlashTick();
        float age = dragon.tickCount - eventTick + partialTick;
        if (eventTick < 0 || age < 0 || age >= FLASH_TICKS) return;
        Integer previous = LAST_BURST.get(dragon);
        if (previous == null || previous != eventTick) {
            LAST_BURST.put(dragon, eventTick);
            var random = dragon.getRandom();
            for (int i = 0; i < 96; i++) {
                double angle = random.nextDouble() * Math.PI * 2;
                double vertical = random.nextDouble() * 2 - 1;
                double horizontal = Math.sqrt(Math.max(0, 1 - vertical * vertical));
                double speed = 0.35 + random.nextDouble() * 1.1;
                var emitter = Minecraft.getInstance().particleEngine.createParticle(ModParticles.CINDERVANE_MOUTH_EMITTER.get(),
                        tip.x, tip.y, tip.z, Math.cos(angle) * horizontal * speed,
                        vertical * speed, Math.sin(angle) * horizontal * speed);
                if (emitter != null) emitter.setColor(0.8F, 0.93F, 1);
            }
        }
        Vec3 origin = new Vec3(Mth.lerp(partialTick, dragon.xOld, dragon.getX()),
                Mth.lerp(partialTick, dragon.yOld, dragon.getY()), Mth.lerp(partialTick, dragon.zOld, dragon.getZ()));
        Vec3 offset = tip.subtract(origin);
        float fadeIn = Mth.clamp(age / FADE_IN_TICKS, 0, 1);
        float fadeOut = Mth.clamp((FLASH_TICKS - age) / (FLASH_TICKS - FADE_IN_TICKS), 0, 1);
        float alpha = fadeIn * fadeIn * (3 - 2 * fadeIn) * fadeOut * fadeOut * fadeOut;
        BillboardFlashRenderer.renderAnchoredQuad(poses, buffers, STAR,
                (float) offset.x, (float) offset.y, (float) offset.z,
                5.0F, 0, 0.8F, 0.93F, 1, alpha, 0.5F, 0.5F);
    }
}

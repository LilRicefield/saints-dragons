package com.leon.saintsdragons.client.camera;

import com.leon.saintsdragons.server.entity.ability.abilities.ignivorus.IgnivorusUltimateAbility;
import com.leon.saintsdragons.server.entity.dragons.ignivorus.Ignivorus;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.util.Mth;

public final class IgnivorusSkyfallScreenEffects {
    private IgnivorusSkyfallScreenEffects() {}

    private static float[] sample(float partialTick) {
        Minecraft mc = Minecraft.getInstance();
        float fov = 0.0F, flash = 0.0F, afterglow = 0.0F;
        if (mc.level == null || mc.player == null || mc.screen != null) return new float[]{0, 0, 0};
        for (var entity : mc.level.entitiesForRendering()) {
            if (!(entity instanceof Ignivorus dragon) || !dragon.isAlive()) continue;
            float elapsed = dragon.getSkyfallElapsedTicks(partialTick);
            if (elapsed < 0.0F) continue;
            float time = elapsed - IgnivorusUltimateAbility.SKYFALL_EXPLOSION_TICK;
            float strength = mc.player.getVehicle() == dragon ? 1.0F
                    : 1.0F - smooth((float) (mc.gameRenderer.getMainCamera().getPosition().distanceTo(dragon.position()) / 80.0D));
            if (time < 0.0F) {
                float build = smooth((time + 60.0F) / 52.0F);
                float reset = smooth((-time - 2.0F) / 6.0F);
                fov = Math.max(fov, 0.18F * build * reset * strength);
            } else {
                fov = Math.max(fov, 0.50F * smooth(time / 2.0F) * (1.0F - smooth((time - 2.0F) / 22.0F)) * strength);
                flash = Math.max(flash, 0.9F * (1.0F - smooth(time / 5.0F)) * strength);
                float settle = smooth(time / 4.0F) * (1.0F - smooth((time - 4.0F) / 16.0F));
                afterglow = Math.max(afterglow, settle * strength);
            }
        }
        return new float[]{fov, flash, afterglow};
    }

    public static double fovMultiplier(float partialTick) {
        return 1.0D + sample(partialTick)[0];
    }

    public static void renderFlash(float partialTick) {
        Minecraft mc = Minecraft.getInstance();
        float[] effects = sample(partialTick);
        int alpha = (int) (effects[1] * 255.0F);
        float afterglow = effects[2];
        if (alpha <= 0 && afterglow <= 0.001F) return;
        GuiGraphics graphics = new GuiGraphics(mc, mc.renderBuffers().bufferSource());
        graphics.pose().translate(0.0D, 0.0D, 1000.0D);
        int width = mc.getWindow().getGuiScaledWidth();
        int height = mc.getWindow().getGuiScaledHeight();
        if (afterglow > 0.001F) {
            int glowAlpha = (int) (afterglow * 0.08F * 255.0F);
            graphics.fill(0, 0, width, height, (glowAlpha << 24) | 0xFFFFFF);
            int bands = 48;
            for (int band = 0; band < bands; band++) {
                float outer = band / (float) bands;
                float inner = (band + 1) / (float) bands;
                int x0 = (int) (width * 0.25F * outer);
                int y0 = (int) (height * 0.25F * outer);
                int x1 = (int) (width * 0.25F * inner);
                int y1 = (int) (height * 0.25F * inner);
                int shade = (int) (afterglow * 0.38F * (1.0F - smooth((outer + inner) * 0.5F)) * 255.0F) << 24;
                graphics.fill(x0, y0, width - x0, y1, shade);
                graphics.fill(x0, height - y1, width - x0, height - y0, shade);
                graphics.fill(x0, y1, x1, height - y1, shade);
                graphics.fill(width - x1, y1, width - x0, height - y1, shade);
            }
        }
        if (alpha > 0) graphics.fill(0, 0, width, height, (alpha << 24) | 0xFFFFFF);
        graphics.flush();
    }

    private static float smooth(float value) {
        value = Mth.clamp(value, 0.0F, 1.0F);
        return value * value * (3.0F - 2.0F * value);
    }
}

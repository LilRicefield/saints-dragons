package com.leon.saintsdragons.client.ui;

import com.leon.saintsdragons.client.camera.DragonDiveEffectIntensity;
import com.leon.saintsdragons.client.camera.BloodTempestKatanaVisuals;
import com.mojang.blaze3d.systems.RenderSystem;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.Entity;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

@Environment(EnvType.CLIENT)
public final class SpeedLineOverlay {
    private static final int MAX_LINES = 52;
    private static final float BASE_SPAWN_CHANCE = 0.38F;
    private static final float SPAWN_CHANCE_SCALE = 0.92F;
    private static final float EDGE_BAND = 0.18F;
    private static final int LINE_COLOR = 0xFFFFFFFF;

    private final List<Line> lines = new ArrayList<>();
    private final RandomSource random = RandomSource.create();

    public void render(GuiGraphics graphics, int width, int height, float partialTicks) {
        Entity player = Minecraft.getInstance().player;
        Entity vehicle = player == null ? null : player.getVehicle();
        float intensity = Math.max(
                DragonDiveEffectIntensity.get(vehicle),
                BloodTempestKatanaVisuals.getSpeedLineIntensity(partialTicks));
        if (intensity <= 0.0F) {
            lines.clear();
            return;
        }

        double screenScale = Minecraft.getInstance().options.screenEffectScale().get();
        intensity = (float) (intensity * screenScale);
        if (intensity <= 0.0F) {
            lines.clear();
            return;
        }

        spawnLines(width, height, intensity);
        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
        for (Iterator<Line> iterator = lines.iterator(); iterator.hasNext(); ) {
            Line line = iterator.next();
            if (!line.tick()) {
                iterator.remove();
                continue;
            }
            renderLine(graphics, line, intensity);
        }
        RenderSystem.disableBlend();
    }

    private void spawnLines(int width, int height, float intensity) {
        int attempts = 2 + Mth.floor(intensity * 7.0F);
        float chance = BASE_SPAWN_CHANCE + intensity * SPAWN_CHANCE_SCALE;
        for (int i = 0; i < attempts && lines.size() < MAX_LINES; i++) {
            if (random.nextFloat() > chance) {
                continue;
            }
            lines.add(Line.create(random, width, height, intensity));
        }
    }

    private void renderLine(GuiGraphics graphics, Line line, float diveIntensity) {
        float life = line.age / (float) line.lifetime;
        float fade = life < 0.25F ? life / 0.25F : 1.0F - ((life - 0.25F) / 0.75F);
        int alpha = Mth.clamp(Mth.floor(210.0F * fade * line.alpha * (0.35F + diveIntensity * 0.65F)), 0, 210);
        if (alpha <= 0) {
            return;
        }

        int color = (alpha << 24) | (LINE_COLOR & 0x00FFFFFF);
        graphics.pose().pushPose();
        graphics.pose().translate(line.x, line.y, 0.0F);
        graphics.pose().mulPose(com.mojang.math.Axis.ZP.rotationDegrees(line.angle));
        graphics.fill(0, -line.thickness / 2, line.length, line.thickness / 2, color);
        graphics.pose().popPose();
    }

    private static final class Line {
        private final int lifetime;
        private final int length;
        private final int thickness;
        private final float angle;
        private final float alpha;
        private final float velocityX;
        private final float velocityY;
        private int age;
        private float x;
        private float y;

        private Line(float x, float y, float angle, int length, int thickness, int lifetime, float alpha, float velocityX, float velocityY) {
            this.x = x;
            this.y = y;
            this.angle = angle;
            this.length = length;
            this.thickness = thickness;
            this.lifetime = lifetime;
            this.alpha = alpha;
            this.velocityX = velocityX;
            this.velocityY = velocityY;
        }

        private static Line create(RandomSource random, int width, int height, float intensity) {
            float centerX = width * 0.5F;
            float centerY = height * 0.5F;
            int edge = random.nextInt(4);
            float x;
            float y;
            if (edge == 0) {
                x = random.nextFloat() * width;
                y = random.nextFloat() * height * EDGE_BAND;
            } else if (edge == 1) {
                x = random.nextFloat() * width;
                y = height - random.nextFloat() * height * EDGE_BAND;
            } else if (edge == 2) {
                x = random.nextFloat() * width * EDGE_BAND;
                y = random.nextFloat() * height;
            } else {
                x = width - random.nextFloat() * width * EDGE_BAND;
                y = random.nextFloat() * height;
            }

            float dx = x - centerX;
            float dy = y - centerY;
            float angle = (float) Math.toDegrees(Math.atan2(dy, dx));
            float distance = Mth.sqrt(dx * dx + dy * dy);
            float push = distance > 0.0F ? 1.0F / distance : 0.0F;
            float speed = 0.8F + random.nextFloat() * 2.2F + intensity * 2.2F;
            int length = Mth.floor(28.0F + random.nextFloat() * 54.0F + intensity * 34.0F);
            int thickness = random.nextFloat() < 0.82F ? 1 : 2;
            int lifetime = 5 + random.nextInt(6);
            float alpha = 0.45F + random.nextFloat() * 0.45F;
            return new Line(x, y, angle, length, thickness, lifetime, alpha, dx * push * speed, dy * push * speed);
        }

        private boolean tick() {
            age++;
            x += velocityX;
            y += velocityY;
            return age < lifetime;
        }
    }
}

package com.leon.saintsdragons.forge.client.event;

import com.leon.saintsdragons.client.debug.DragonAbilityDebugClient;
import com.leon.saintsdragons.common.SaintsDragonsCommon;
import com.mojang.blaze3d.platform.InputConstants;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.network.chat.Component;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RegisterKeyMappingsEvent;
import net.minecraftforge.client.event.RenderLevelStageEvent;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid = SaintsDragonsCommon.MOD_ID, value = Dist.CLIENT, bus = Mod.EventBusSubscriber.Bus.FORGE)
public final class DragonAbilityDebugForgeHandler {
    private static final String KEY_CATEGORY = "key.categories.saintsdragons";

    public static final KeyMapping TOGGLE_ABILITY_DEBUG = new KeyMapping(
            "key.saintsdragons.toggle_ability_debug",
            InputConstants.Type.KEYSYM,
            InputConstants.KEY_F7,
            KEY_CATEGORY
    );

    private DragonAbilityDebugForgeHandler() {
    }

    @Mod.EventBusSubscriber(modid = SaintsDragonsCommon.MOD_ID, value = Dist.CLIENT, bus = Mod.EventBusSubscriber.Bus.MOD)
    public static final class ModEvents {
        private ModEvents() {
        }

        @SubscribeEvent
        public static void registerKeys(RegisterKeyMappingsEvent event) {
            event.register(TOGGLE_ABILITY_DEBUG);
        }
    }

    @SubscribeEvent
    public static void onClientTick(TickEvent.ClientTickEvent event) {
        if (event.phase != TickEvent.Phase.END) {
            return;
        }

        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.player == null) {
            return;
        }

        while (TOGGLE_ABILITY_DEBUG.consumeClick()) {
            boolean enabled = DragonAbilityDebugClient.toggle();
            minecraft.player.displayClientMessage(
                    Component.literal("Dragon ability debug: " + (enabled ? "ON" : "OFF")),
                    true
            );
        }

        DragonAbilityDebugClient.tick();
    }

    @SubscribeEvent
    public static void onRenderLevelStage(RenderLevelStageEvent event) {
        if (!DragonAbilityDebugClient.isEnabled()
                || event.getStage() != RenderLevelStageEvent.Stage.AFTER_PARTICLES) {
            return;
        }

        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.level == null) {
            return;
        }

        var boxes = DragonAbilityDebugClient.getBoxes();
        if (boxes.isEmpty()) {
            return;
        }

        PoseStack poseStack = event.getPoseStack();
        Vec3 cameraPos = event.getCamera().getPosition();
        MultiBufferSource.BufferSource bufferSource = minecraft.renderBuffers().bufferSource();
        VertexConsumer vertexConsumer = bufferSource.getBuffer(RenderType.lines());

        poseStack.pushPose();
        poseStack.translate(-cameraPos.x, -cameraPos.y, -cameraPos.z);
        for (var box : boxes) {
            LevelRenderer.renderLineBox(
                    poseStack,
                    vertexConsumer,
                    box.box(),
                    box.red(),
                    box.green(),
                    box.blue(),
                    1.0F
            );
        }
        poseStack.popPose();

        bufferSource.endBatch(RenderType.lines());
    }
}

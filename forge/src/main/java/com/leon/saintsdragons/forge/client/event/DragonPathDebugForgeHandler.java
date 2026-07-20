package com.leon.saintsdragons.forge.client.event;

import com.leon.saintsdragons.client.debug.DragonPathDebugClient;
import com.leon.saintsdragons.client.debug.DragonPathDebugRenderer;
import com.leon.saintsdragons.client.debug.DragonBrainDebugClient;
import com.leon.saintsdragons.client.debug.DragonBrainDebugHud;
import com.leon.saintsdragons.client.debug.DragonBrainDebugRenderer;
import com.leon.saintsdragons.common.SaintsDragonsCommon;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.world.item.Items;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.InputEvent;
import net.minecraftforge.client.event.RenderLevelStageEvent;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid = SaintsDragonsCommon.MOD_ID, value = Dist.CLIENT, bus = Mod.EventBusSubscriber.Bus.FORGE)
public final class DragonPathDebugForgeHandler {
    private DragonPathDebugForgeHandler() {
    }

    @SubscribeEvent
    public static void onClientTick(TickEvent.ClientTickEvent event) {
        if (event.phase != TickEvent.Phase.END) {
            return;
        }

        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.player == null || minecraft.level == null) {
            DragonPathDebugClient.clear();
            DragonBrainDebugClient.clear();
            return;
        }
        DragonPathDebugClient.tick();
        DragonBrainDebugClient.tick();
    }

    @SubscribeEvent
    public static void onMouseScroll(InputEvent.MouseScrollingEvent event) {
        Minecraft minecraft = Minecraft.getInstance();
        if (event.getScrollDelta() == 0.0D
                || minecraft.player == null
                || minecraft.screen != null
                || !Screen.hasShiftDown()
                || (!minecraft.player.getMainHandItem().is(Items.DEBUG_STICK)
                && !minecraft.player.getOffhandItem().is(Items.DEBUG_STICK))) {
            return;
        }

        int direction = event.getScrollDelta() < 0.0D ? 1 : -1;
        if (DragonBrainDebugHud.cyclePage(direction)) {
            event.setCanceled(true);
        }
    }

    @SubscribeEvent
    public static void onRenderLevelStage(RenderLevelStageEvent event) {
        if (event.getStage() != RenderLevelStageEvent.Stage.AFTER_PARTICLES
                || (DragonPathDebugClient.getSnapshot() == null
                && DragonBrainDebugClient.getSnapshot() == null)) {
            return;
        }

        if (DragonPathDebugClient.getSnapshot() != null) {
            DragonPathDebugRenderer.render(event.getPoseStack(), event.getCamera().getPosition());
        }
        if (DragonBrainDebugClient.getSnapshot() != null) {
            DragonBrainDebugRenderer.render(event.getPoseStack(), event.getCamera().getPosition());
        }
    }
}

package com.leon.saintsdragons.client;

import com.leon.saintsdragons.SaintsDragons;
import com.leon.saintsdragons.server.entity.dragons.lightningdragon.LightningDragonEntity;
import com.mojang.blaze3d.platform.InputConstants;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.AABB;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RegisterKeyMappingsEvent;
import net.minecraftforge.client.event.RenderGuiEvent;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraft.client.KeyMapping;

import java.util.Comparator;
import java.util.List;

@Mod.EventBusSubscriber(modid = SaintsDragons.MOD_ID, value = Dist.CLIENT, bus = Mod.EventBusSubscriber.Bus.FORGE)
public class DebugHud {
    private static boolean show = false;

    // Toggle key (F7)
    public static final KeyMapping TOGGLE_DEBUG = new KeyMapping(
            "key.lightningdragon.debug_toggle",
            InputConstants.Type.KEYSYM,
            InputConstants.KEY_F7,
            "key.categories.lightningdragon"
    );

    @Mod.EventBusSubscriber(modid = SaintsDragons.MOD_ID, value = Dist.CLIENT, bus = Mod.EventBusSubscriber.Bus.MOD)
    public static class ModKeys {
        @SubscribeEvent
        public static void onRegisterKeys(RegisterKeyMappingsEvent e) {
            e.register(TOGGLE_DEBUG);
        }
    }

    @SubscribeEvent
    public static void onClientTick(TickEvent.ClientTickEvent e) {
        if (e.phase != TickEvent.Phase.END) return;
        if (TOGGLE_DEBUG.consumeClick()) {
            show = !show;
            var mc = Minecraft.getInstance();
            if (mc.player != null) {
                mc.player.displayClientMessage(Component.literal("LightningDragon debug " + (show ? "ON" : "OFF")), true);
            }
        }
    }

    @SubscribeEvent
    public static void onRenderHud(RenderGuiEvent.Post e) {
        if (!show) return;
        var mc = Minecraft.getInstance();
        LocalPlayer player = mc.player;
        if (player == null || mc.level == null) return;

        LightningDragonEntity target = currentDragonTarget(player);
        if (target == null) return;

        GuiGraphics g = e.getGuiGraphics();
        int x = 6;
        int y = 6;
        int dy = 10;

        g.drawString(mc.font, Component.literal("[LightningDragon Debug]"), x, y, 0xFFFF55, false); y += dy;
        g.drawString(mc.font, Component.literal("EntityId: " + target.getId()), x, y, 0xFFFFFF, false); y += dy;

        int ground = target.getEffectiveGroundState();
        int flight = target.getEffectiveFlightMode();
        boolean walking = target.isWalking();
        boolean running = target.isActuallyRunning();
        boolean flying = target.isFlying();
        boolean takeoff = target.isTakeoff();
        boolean landing = target.isLanding();
        boolean hovering = target.isHovering();
        boolean orderedSit = target.isOrderedToSit();
        boolean sittingPose = target.isInSittingPose();
        float sitProg = target.getEntityData().get(LightningDragonEntity.DATA_SIT_PROGRESS);

        g.drawString(mc.font, Component.literal("groundState: " + ground + " (walk=" + walking + ", run=" + running + ")"), x, y, 0xFFFFFF, false); y += dy;
        g.drawString(mc.font, Component.literal("flightMode: " + flight + " (flying=" + flying + ", takeoff=" + takeoff + ", hover=" + hovering + ", landing=" + landing + ")"), x, y, 0xFFFFFF, false); y += dy;
        g.drawString(mc.font, Component.literal(String.format("sit: ordered=%s, pose=%s, progress=%.1f", orderedSit, sittingPose, sitProg)), x, y, 0xFFFFFF, false); y += dy;

        var vel = target.getDeltaMovement();
        double yDelta = target.getYDelta();
        g.drawString(mc.font, Component.literal(String.format("vel2=%.4f, velY=%.3f, yΔ=%.3f", vel.horizontalDistanceSqr(), vel.y, yDelta)), x, y, 0xAAAAFF, false); y += dy;

        float gFrac = target.getGlidingFraction();
        float fFrac = target.getFlappingFraction();
        float hFrac = target.getHoveringFraction();
        g.drawString(mc.font, Component.literal(String.format("fractions: glide=%.2f flap=%.2f hover=%.2f", gFrac, fFrac, hFrac)), x, y, 0xA0FFA0, false); y += dy;

        var rider = target.getControllingPassenger();
        g.drawString(mc.font, Component.literal("riddenBy: " + (rider != null ? rider.getName().getString() : "<none>")), x, y, 0xFFFFFF, false); y += dy;
    }

    private static LightningDragonEntity currentDragonTarget(LocalPlayer player) {
        Entity veh = player.getVehicle();
        if (veh instanceof LightningDragonEntity d) return d;
        // Nearest within 20 blocks
        var aabb = new AABB(player.blockPosition()).inflate(20);
        List<LightningDragonEntity> list = player.level().getEntitiesOfClass(LightningDragonEntity.class, aabb);
        return list.stream()
                .min(Comparator.comparingDouble(e -> e.distanceToSqr(player)))
                .orElse(null);
    }
}

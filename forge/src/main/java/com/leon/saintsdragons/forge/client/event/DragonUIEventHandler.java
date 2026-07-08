package com.leon.saintsdragons.forge.client.event;

import com.leon.saintsdragons.client.ui.DragonRideHealthBar;
import com.leon.saintsdragons.client.ui.DragonDiveSpeedLineOverlay;
import com.leon.saintsdragons.client.ui.DragonUIRegistry;
import com.leon.saintsdragons.client.ui.FireballChargeIndicator;
import com.leon.saintsdragons.client.ui.IgnivorusFireBreathMeterIndicator;
import com.leon.saintsdragons.client.ui.MeleeModeNotification;
import com.leon.saintsdragons.client.ui.RaevyxBeamMeterIndicator;
import com.leon.saintsdragons.client.ui.SwarmWaveBarOverlay;
import com.leon.saintsdragons.client.ui.VolitansBreathMeterIndicator;
import com.leon.saintsdragons.common.SaintsDragonsCommon;
import com.leon.saintsdragons.server.entity.base.DragonEntity;
import com.leon.saintsdragons.server.entity.base.RideableGroundDragon;
import com.leon.saintsdragons.server.entity.dragons.ignivorus.Ignivorus;
import com.leon.saintsdragons.server.entity.dragons.raevyx.Raevyx;
import com.leon.saintsdragons.server.entity.dragons.volitans.Volitans;
import com.leon.saintsdragons.forge.platform.ForgeClientConfig;
import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.PlayerRideableJumping;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RenderGuiOverlayEvent;
import net.minecraftforge.client.gui.overlay.VanillaGuiOverlay;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid = SaintsDragonsCommon.MOD_ID, bus = Mod.EventBusSubscriber.Bus.FORGE, value = Dist.CLIENT)
public class DragonUIEventHandler {
    private static final MeleeModeNotification meleeModeNotification = new MeleeModeNotification();
    private static final FireballChargeIndicator fireballChargeIndicator = new FireballChargeIndicator();
    private static final RaevyxBeamMeterIndicator raevyxBeamMeterIndicator = new RaevyxBeamMeterIndicator();
    private static final IgnivorusFireBreathMeterIndicator ignivorusFireBreathMeterIndicator = new IgnivorusFireBreathMeterIndicator();
    private static final VolitansBreathMeterIndicator volitansBreathMeterIndicator = new VolitansBreathMeterIndicator();
    private static final DragonRideHealthBar rideHealthBar = new DragonRideHealthBar();
    private static final DragonDiveSpeedLineOverlay diveSpeedLineOverlay = new DragonDiveSpeedLineOverlay();

    static {
        DragonUIRegistry.init(meleeModeNotification);
    }

    @SubscribeEvent
    public static void onRenderGuiOverlayPre(RenderGuiOverlayEvent.Pre event) {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.player == null || !(minecraft.player.getVehicle() instanceof DragonEntity)) {
            return;
        }

        if (minecraft.player.getVehicle() instanceof RideableGroundDragon
                && (event.getOverlay() == VanillaGuiOverlay.EXPERIENCE_BAR.type()
                || event.getOverlay() == VanillaGuiOverlay.JUMP_BAR.type())) {
            event.setCanceled(true);
            return;
        }

        if (!DragonUIRegistry.isUIVisible()) {
            return;
        }

        if (event.getOverlay() == VanillaGuiOverlay.PLAYER_HEALTH.type()) {
            event.setCanceled(true);
        } else if (event.getOverlay() == VanillaGuiOverlay.ARMOR_LEVEL.type()) {
            event.setCanceled(true);
        } else if (event.getOverlay() == VanillaGuiOverlay.EXPERIENCE_BAR.type()
                && !(minecraft.player.getVehicle() instanceof PlayerRideableJumping)) {
            event.setCanceled(true);
        } else if (event.getOverlay() == VanillaGuiOverlay.MOUNT_HEALTH.type()) {
            event.setCanceled(true);
        } else if (event.getOverlay() == VanillaGuiOverlay.FOOD_LEVEL.type()) {
            event.setCanceled(true);
        } else if (event.getOverlay() == VanillaGuiOverlay.AIR_LEVEL.type()) {
            event.setCanceled(true);
        }
    }

    @SubscribeEvent
    public static void onRenderGuiOverlay(RenderGuiOverlayEvent.Post event) {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.player == null) {
            return;
        }

        int screenWidth = minecraft.getWindow().getGuiScaledWidth();
        int screenHeight = minecraft.getWindow().getGuiScaledHeight();

        if (event.getOverlay() == VanillaGuiOverlay.HOTBAR.type()) {
            SwarmWaveBarOverlay.render(event.getGuiGraphics(), screenWidth, event.getPartialTick());
        }

        if (event.getOverlay() == VanillaGuiOverlay.HOTBAR.type()
                && ForgeClientConfig.DIVE_SPEED_LINES_ENABLED.get()) {
            diveSpeedLineOverlay.render(event.getGuiGraphics(), screenWidth, screenHeight, event.getPartialTick());
        }

        if (event.getOverlay() == VanillaGuiOverlay.HOTBAR.type()
                && minecraft.player.getVehicle() instanceof RideableGroundDragon groundDragon) {
            int x = screenWidth / 2 - 91;
            if (minecraft.player.getJumpRidingScale() > 0.0F) {
                minecraft.gui.renderJumpMeter(groundDragon, event.getGuiGraphics(), x);
            } else if (!DragonUIRegistry.isUIVisible()
                    && minecraft.gameMode != null
                    && minecraft.gameMode.hasExperience()) {
                minecraft.gui.renderExperienceBar(event.getGuiGraphics(), x);
            }
        }

        DragonEntity currentDragon = null;
        if (minecraft.player.getVehicle() instanceof DragonEntity dragon) {
            currentDragon = dragon;
            rideHealthBar.setDragon(dragon);
        }
        meleeModeNotification.render(event.getGuiGraphics(), screenWidth, screenHeight);

        if (!DragonUIRegistry.isUIVisible()) {
            return;
        }

        if (currentDragon instanceof Ignivorus ignivorus) {
            fireballChargeIndicator.setChargeLevel(ignivorus.getFireballChargeLevel());
            fireballChargeIndicator.render(event.getGuiGraphics(), screenWidth, screenHeight, event.getPartialTick());
            ignivorusFireBreathMeterIndicator.setBreathEnergy(ignivorus.getFireBreathEnergy());
            ignivorusFireBreathMeterIndicator.setBreathing(ignivorus.isBreathingFire());
            ignivorusFireBreathMeterIndicator.render(event.getGuiGraphics(), screenWidth, screenHeight, event.getPartialTick());
        } else if (currentDragon instanceof Raevyx raevyx) {
            raevyxBeamMeterIndicator.setBeamEnergy(raevyx.getBeamEnergy());
            raevyxBeamMeterIndicator.setBeaming(raevyx.isBeaming());
            raevyxBeamMeterIndicator.render(event.getGuiGraphics(), screenWidth, screenHeight, event.getPartialTick());
        } else if (currentDragon instanceof Volitans volitans) {
            volitansBreathMeterIndicator.setWaterEnergy(volitans.getWaterBreathEnergy());
            volitansBreathMeterIndicator.setPoisonEnergy(volitans.getPoisonBreathEnergy());
            volitansBreathMeterIndicator.setBreathMode(volitans.getBreathMode());
            volitansBreathMeterIndicator.setBreathing(volitans.isBreathing());
            volitansBreathMeterIndicator.render(event.getGuiGraphics(), screenWidth, screenHeight, event.getPartialTick());
        }

        if (currentDragon != null) {
            rideHealthBar.render(event.getGuiGraphics(), screenWidth, screenHeight, event.getPartialTick());
        }
    }

    @SubscribeEvent
    public static void onClientTick(TickEvent.ClientTickEvent event) {
        if (event.phase == TickEvent.Phase.END) {
            DragonUIKeybinds.handleKeybinds();
            meleeModeNotification.tick();
            fireballChargeIndicator.tick();
            raevyxBeamMeterIndicator.tick();
            ignivorusFireBreathMeterIndicator.tick();
            volitansBreathMeterIndicator.tick();
        }
    }
}

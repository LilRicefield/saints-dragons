package com.leon.saintsdragons.client.event;

import com.leon.saintsdragons.SaintsDragons;
import com.leon.saintsdragons.server.entity.dragons.cindervane.Cindervane;
import com.leon.saintsdragons.server.entity.dragons.raevyx.Raevyx;
import com.leon.saintsdragons.server.entity.dragons.nulljaw.Nulljaw;
import com.leon.saintsdragons.server.entity.interfaces.ShakesScreen;
import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.phys.AABB;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.ViewportEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid = SaintsDragons.MOD_ID, value = Dist.CLIENT, bus = Mod.EventBusSubscriber.Bus.FORGE)
public class ClientEventHandler {
    private static final double[] randomTremorOffsets = new double[3];

    // Raevyx takeoff camera zoom transition
    private static float raevyxCameraZoom = 10F; // Base zoom
    private static float raevyxCameraZoomTarget = 10F;

    // Cindervane takeoff camera zoom transition
    private static float cindervaneCameraZoom = 15F; // Base zoom
    private static float cindervaneCameraZoomTarget = 15F;

    @SubscribeEvent
    public static void onComputeCamera(ViewportEvent.ComputeCameraAngles event) {
        Entity player = Minecraft.getInstance().getCameraEntity();
        if (player == null) return;
        
        
        // Dragon riding camera adjustments
        if (player.isPassenger() && player.getVehicle() instanceof Raevyx raevyx && event.getCamera().isDetached()) {
            // Determine target zoom based on flight state
            boolean isFlying = raevyx.isFlying();

            // Flying: zoom to 18F, grounded: 10F base
            raevyxCameraZoomTarget = isFlying ? 18F : 10F;

            // Smooth transition (0.15 = gentle blend rate)
            float blendRate = 0.15F;
            raevyxCameraZoom += (raevyxCameraZoomTarget - raevyxCameraZoom) * blendRate;

            // Apply the smoothed zoom
            event.getCamera().move(-event.getCamera().getMaxZoom(raevyxCameraZoom), 0, 0);
        } else {
            // Reset zoom when not riding Raevyx
            raevyxCameraZoom = 10F;
            raevyxCameraZoomTarget = 10F;
        }

        // Cindervane camera zoom adjustments
        if (player.isPassenger() && player.getVehicle() instanceof Cindervane cindervane && event.getCamera().isDetached()) {
            // Determine target zoom based on flight state
            boolean isFlying = cindervane.isFlying();

            // Flying: zoom to 25F, grounded: 10F base
            cindervaneCameraZoomTarget = isFlying ? 30F : 15F;

            // Smooth transition (0.15 = gentle blend rate)
            float blendRate = 0.15F;
            cindervaneCameraZoom += (cindervaneCameraZoomTarget - cindervaneCameraZoom) * blendRate;

            // Apply the smoothed zoom
            event.getCamera().move(-event.getCamera().getMaxZoom(cindervaneCameraZoom), 0, 0);
        } else if (!(player.getVehicle() instanceof Cindervane)) {
            // Reset zoom when not riding Cindervane
            cindervaneCameraZoom = 15F;
            cindervaneCameraZoomTarget = 15F;
        }

        if (player.isPassenger() && player.getVehicle() instanceof Nulljaw && event.getCamera().isDetached()) {
            event.getCamera().move(-event.getCamera().getMaxZoom(15F), 0, 0);
        }
        
        // Screen shake detection and application
        double shakeDistanceScale = 64.0;
        double distance = Double.MAX_VALUE;
        // Screen shake system
        float tremorAmount = 0.0F; // Reset tremor amount each frame

        AABB aabb = player.getBoundingBox().inflate(shakeDistanceScale);
        var level = Minecraft.getInstance().level;
        if (level == null) return;
        
        for (Mob screenShaker : level.getEntitiesOfClass(Mob.class, aabb, (mob -> mob instanceof ShakesScreen))) {
            ShakesScreen shakesScreen = (ShakesScreen) screenShaker;
            if (shakesScreen.canFeelShake(player) && screenShaker.distanceTo(player) < distance) {
                distance = screenShaker.distanceTo(player);
                float shakeAmount = shakesScreen.getScreenShakeAmount((float) event.getPartialTick());
                tremorAmount = Math.min((1F - (float) Math.min(1, distance / shakesScreen.getShakeDistance())) * Math.max(shakeAmount, 0F), 2.0F);
            }
        }
        
        if (tremorAmount > 0) {
            // Generate random offsets for camera movement
            double intensity = tremorAmount * Minecraft.getInstance().options.screenEffectScale().get();
            event.getCamera().move(randomTremorOffsets[0] * 0.2F * intensity, 
                                  randomTremorOffsets[1] * 0.2F * intensity, 
                                  randomTremorOffsets[2] * 0.5F * intensity);

            // Update random offsets for next frame
            randomTremorOffsets[0] = (Math.random() - 0.5) * 2.0;
            randomTremorOffsets[1] = (Math.random() - 0.5) * 2.0;
            randomTremorOffsets[2] = (Math.random() - 0.5) * 2.0;
        }
    }

    // DISABLED: Let vanilla render the passenger normally for now
    // We'll handle bone-based positioning purely through the render layer
    public static boolean allowRaevyxPassengerRender = false;

    // @SubscribeEvent
    // public static void suppressVanillaPassengerRendering(RenderLivingEvent.Pre<?, ?> event) {
    //     Entity entity = event.getEntity();
    //     if (entity == null) return;
    //     Entity vehicle = entity.getVehicle();
    //     if (vehicle instanceof Raevyx && !allowRaevyxPassengerRender) {
    //         event.setCanceled(true);
    //     }
    // }
}

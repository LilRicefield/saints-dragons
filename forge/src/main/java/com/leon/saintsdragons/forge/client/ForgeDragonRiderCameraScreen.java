package com.leon.saintsdragons.forge.client;

import com.leon.saintsdragons.client.camera.DragonRideCameraTuning;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

import java.util.List;

@OnlyIn(Dist.CLIENT)
public final class ForgeDragonRiderCameraScreen extends ForgePagedConfigScreen {
    public ForgeDragonRiderCameraScreen(Screen parent) {
        super(parent, Component.translatable("saintsdragons.config_screen.dragon_rider_camera"));
    }

    @Override
    protected void buildEntries(List<ConfigEntry> entries) {
        DragonRideCameraTuning.bootstrap();
        for (String dragonKey : DragonRideCameraTuning.getConfigurableProfileKeys()) {
            entries.add(new SectionEntry(Component.translatable(
                    "config.saintsdragons.attributes." + dragonKey)));
            entries.add(new DoubleEntry(
                    Component.translatable("saintsdragons.config_screen.dragon_rider_camera.grounded_distance"),
                    () -> DragonRideCameraTuning.getProfile(dragonKey).groundedDistance(),
                    value -> DragonRideCameraTuning.setGroundedDistance(dragonKey, value),
                    null,
                    () -> DragonRideCameraTuning.getDefaultProfile(dragonKey).groundedDistance(),
                    DragonRideCameraTuning.MIN_CAMERA_DISTANCE,
                    DragonRideCameraTuning.MAX_CAMERA_DISTANCE
            ));
            entries.add(new DoubleEntry(
                    Component.translatable("saintsdragons.config_screen.dragon_rider_camera.air_or_water_distance"),
                    () -> DragonRideCameraTuning.getProfile(dragonKey).airOrWaterDistance(),
                    value -> DragonRideCameraTuning.setAirOrWaterDistance(dragonKey, value),
                    null,
                    () -> DragonRideCameraTuning.getDefaultProfile(dragonKey).airOrWaterDistance(),
                    DragonRideCameraTuning.MIN_CAMERA_DISTANCE,
                    DragonRideCameraTuning.MAX_CAMERA_DISTANCE
            ));
        }
    }

    @Override
    protected void onSave() {
        DragonRideCameraTuning.save();
    }
}

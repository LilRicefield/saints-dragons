package com.leon.saintsdragons.forge.client;

import com.leon.saintsdragons.forge.platform.ForgeClientConfig;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

import java.util.List;

@OnlyIn(Dist.CLIENT)
public final class ForgeClientCommonScreen extends ForgePagedConfigScreen {
    public ForgeClientCommonScreen(Screen parent) {
        super(parent, Component.translatable("saintsdragons.config_screen.client_common"));
    }

    @Override
    protected void buildEntries(List<ConfigEntry> entries) {
        entries.add(new SectionEntry(Component.translatable("saintsdragons.config_screen.client.camera_riding")));
        entries.add(new BooleanEntry(
                Component.translatable("saintsdragons.config_screen.others.first_person_banking_camera"),
                ForgeClientConfig.FIRST_PERSON_BANKING_CAMERA_ENABLED::get,
                ForgeClientConfig.FIRST_PERSON_BANKING_CAMERA_ENABLED::set,
                ForgeClientConfig.CLIENT_SPEC::save
        ));
        entries.add(new BooleanEntry(
                Component.translatable("saintsdragons.config_screen.others.third_person_banking_camera"),
                ForgeClientConfig.THIRD_PERSON_BANKING_CAMERA_ENABLED::get,
                ForgeClientConfig.THIRD_PERSON_BANKING_CAMERA_ENABLED::set,
                ForgeClientConfig.CLIENT_SPEC::save
        ));
        entries.add(new BooleanEntry(
                Component.translatable("saintsdragons.config_screen.others.dive_camera_wobble"),
                ForgeClientConfig.DIVE_CAMERA_WOBBLE_ENABLED::get,
                ForgeClientConfig.DIVE_CAMERA_WOBBLE_ENABLED::set,
                ForgeClientConfig.CLIENT_SPEC::save
        ));

        entries.add(new SectionEntry(Component.translatable("saintsdragons.config_screen.client.visual_effects")));
        entries.add(new BooleanEntry(
                Component.translatable("saintsdragons.config_screen.others.dive_speed_lines"),
                ForgeClientConfig.DIVE_SPEED_LINES_ENABLED::get,
                ForgeClientConfig.DIVE_SPEED_LINES_ENABLED::set,
                ForgeClientConfig.CLIENT_SPEC::save
        ));

        entries.add(new SectionEntry(Component.translatable("saintsdragons.config_screen.client.audio")));
        entries.add(new BooleanEntry(
                Component.translatable("saintsdragons.config_screen.others.generic_dive_loop"),
                ForgeClientConfig.GENERIC_DIVE_LOOP_ENABLED::get,
                ForgeClientConfig.GENERIC_DIVE_LOOP_ENABLED::set,
                ForgeClientConfig.CLIENT_SPEC::save
        ));
        entries.add(new IntSliderEntry(
                Component.translatable("saintsdragons.config_screen.others.swarm_battle_music_volume"),
                ForgeClientConfig.SWARM_BATTLE_MUSIC_VOLUME::get,
                ForgeClientConfig.SWARM_BATTLE_MUSIC_VOLUME::set,
                ForgeClientConfig.CLIENT_SPEC::save,
                0,
                100,
                100,
                value -> Component.literal(value + "%")
        ));
    }

    @Override
    protected void onSave() {
        ForgeClientConfig.CLIENT_SPEC.save();
    }
}

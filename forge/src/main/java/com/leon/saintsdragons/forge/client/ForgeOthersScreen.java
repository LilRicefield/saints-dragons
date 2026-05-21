package com.leon.saintsdragons.forge.client;

import com.leon.saintsdragons.common.config.SaintsDragonsConfig;
import com.leon.saintsdragons.forge.platform.ForgeClientConfig;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

import java.util.List;

/**
 * Forge config screen for miscellaneous settings (NPCs, etc.)
 */
@OnlyIn(Dist.CLIENT)
public final class ForgeOthersScreen extends ForgePagedConfigScreen {

    public ForgeOthersScreen(Screen parent) {
        super(parent, Component.translatable("saintsdragons.config_screen.others"));
    }

    @Override
    protected void buildEntries(List<ConfigEntry> entries) {
        SaintsDragonsConfig.bootstrap();
        boolean remoteServer = isRemoteServerSession();

        if (remoteServer) {
            entries.add(new SectionEntry(Component.literal("Server settings are controlled by the dedicated server TOML files.")));
        } else {
            entries.add(new SectionEntry(Component.translatable("saintsdragons.config_screen.others.dragon_griefing.section")));
            entries.add(new BooleanEntry(
                    Component.translatable("saintsdragons.config_screen.others.dragon_griefing"),
                    SaintsDragonsConfig.DRAGON_GRIEFING_ENABLED::get,
                    SaintsDragonsConfig.DRAGON_GRIEFING_ENABLED::set,
                    SaintsDragonsConfig.DRAGON_GRIEFING_ENABLED::save
            ));
            entries.add(new BooleanEntry(
                    Component.translatable("saintsdragons.config_screen.others.screen_shake"),
                    SaintsDragonsConfig.SCREEN_SHAKE_ENABLED::get,
                    SaintsDragonsConfig.SCREEN_SHAKE_ENABLED::set,
                    SaintsDragonsConfig.SCREEN_SHAKE_ENABLED::save
            ));
            entries.add(new BooleanEntry(
                    Component.translatable("saintsdragons.config_screen.others.barrel_roll"),
                    SaintsDragonsConfig.BARREL_ROLL_ENABLED::get,
                    SaintsDragonsConfig.BARREL_ROLL_ENABLED::set,
                    SaintsDragonsConfig.BARREL_ROLL_ENABLED::save
            ));
            entries.add(new BooleanEntry(
                    Component.translatable("saintsdragons.config_screen.others.dragon_breeding"),
                    SaintsDragonsConfig.DRAGON_BREEDING_ENABLED::get,
                    SaintsDragonsConfig.DRAGON_BREEDING_ENABLED::set,
                    SaintsDragonsConfig.DRAGON_BREEDING_ENABLED::save
            ));
        }

        entries.add(new SectionEntry(Component.literal("Client")));
        entries.add(new BooleanEntry(
                Component.translatable("saintsdragons.config_screen.others.first_person_banking_camera"),
                () -> ForgeClientConfig.FIRST_PERSON_BANKING_CAMERA_ENABLED.get(),
                ForgeClientConfig.FIRST_PERSON_BANKING_CAMERA_ENABLED::set,
                ForgeClientConfig.CLIENT_SPEC::save
        ));
        entries.add(new BooleanEntry(
                Component.translatable("saintsdragons.config_screen.others.dive_camera_wobble"),
                () -> ForgeClientConfig.DIVE_CAMERA_WOBBLE_ENABLED.get(),
                ForgeClientConfig.DIVE_CAMERA_WOBBLE_ENABLED::set,
                ForgeClientConfig.CLIENT_SPEC::save
        ));

        if (!remoteServer) {
            entries.add(new BooleanEntry(
                    Component.translatable("saintsdragons.config_screen.others.hunger_decay"),
                    SaintsDragonsConfig.HUNGER_DECAY_ENABLED::get,
                    SaintsDragonsConfig.HUNGER_DECAY_ENABLED::set,
                    SaintsDragonsConfig.HUNGER_DECAY_ENABLED::save
            ));
            entries.add(new BooleanEntry(
                    Component.translatable("saintsdragons.config_screen.others.happiness_decay"),
                    SaintsDragonsConfig.HAPPINESS_DECAY_ENABLED::get,
                    SaintsDragonsConfig.HAPPINESS_DECAY_ENABLED::set,
                    SaintsDragonsConfig.HAPPINESS_DECAY_ENABLED::save
            ));

            // Ivy the Dragon Merchant
            entries.add(new SectionEntry(Component.translatable("saintsdragons.config_screen.others.ivy")));
            entries.add(new BooleanEntry(
                    Component.translatable("saintsdragons.config_screen.others.ivy.enabled"),
                    SaintsDragonsConfig.IVY_HOUSE_ENABLED::get,
                    SaintsDragonsConfig.IVY_HOUSE_ENABLED::set,
                    SaintsDragonsConfig.IVY_HOUSE_ENABLED::save
            ));
            entries.add(new IntEntry(
                    Component.translatable("saintsdragons.config_screen.others.ivy.restock_interval"),
                    SaintsDragonsConfig.IVY_RESTOCK_INTERVAL::get,
                    SaintsDragonsConfig.IVY_RESTOCK_INTERVAL::set,
                    SaintsDragonsConfig.IVY_RESTOCK_INTERVAL::save
            ));
        }
    }

    @Override
    protected void onSave() {
        ForgeClientConfig.CLIENT_SPEC.save();
    }

    private boolean isRemoteServerSession() {
        return minecraft != null && minecraft.level != null && minecraft.getSingleplayerServer() == null;
    }
}

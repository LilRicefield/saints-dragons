package com.leon.saintsdragons.forge.client;

import com.leon.saintsdragons.forge.SaintsDragonsForge;
import com.leon.saintsdragons.forge.platform.ForgeDragonAttributesConfig;
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
        entries.add(new SectionEntry(Component.translatable("saintsdragons.config_screen.others.dragon_griefing.section")));
        entries.add(new BooleanEntry(
                Component.translatable("saintsdragons.config_screen.others.dragon_griefing"),
                () -> ForgeDragonAttributesConfig.DRAGON_GRIEFING_ENABLED.get(),
                ForgeDragonAttributesConfig.DRAGON_GRIEFING_ENABLED::set,
                ForgeDragonAttributesConfig.ATTRIBUTES_SPEC::save
        ));
        entries.add(new BooleanEntry(
                Component.translatable("saintsdragons.config_screen.others.screen_shake"),
                () -> ForgeDragonAttributesConfig.SCREEN_SHAKE_ENABLED.get(),
                ForgeDragonAttributesConfig.SCREEN_SHAKE_ENABLED::set,
                ForgeDragonAttributesConfig.ATTRIBUTES_SPEC::save
        ));
        entries.add(new BooleanEntry(
                Component.translatable("saintsdragons.config_screen.others.barrel_roll"),
                () -> ForgeDragonAttributesConfig.BARREL_ROLL_ENABLED.get(),
                ForgeDragonAttributesConfig.BARREL_ROLL_ENABLED::set,
                ForgeDragonAttributesConfig.ATTRIBUTES_SPEC::save
        ));
        entries.add(new BooleanEntry(
                Component.translatable("saintsdragons.config_screen.others.first_person_banking_camera"),
                () -> ForgeDragonAttributesConfig.FIRST_PERSON_BANKING_CAMERA_ENABLED.get(),
                val -> ForgeDragonAttributesConfig.FIRST_PERSON_BANKING_CAMERA_ENABLED.set(val),
                ForgeDragonAttributesConfig.ATTRIBUTES_SPEC::save
        ));
        entries.add(new BooleanEntry(
                Component.translatable("saintsdragons.config_screen.others.hunger_decay"),
                () -> ForgeDragonAttributesConfig.HUNGER_DECAY_ENABLED.get(),
                ForgeDragonAttributesConfig.HUNGER_DECAY_ENABLED::set,
                ForgeDragonAttributesConfig.ATTRIBUTES_SPEC::save
        ));
        entries.add(new BooleanEntry(
                Component.translatable("saintsdragons.config_screen.others.happiness_decay"),
                () -> ForgeDragonAttributesConfig.HAPPINESS_DECAY_ENABLED.get(),
                ForgeDragonAttributesConfig.HAPPINESS_DECAY_ENABLED::set,
                ForgeDragonAttributesConfig.ATTRIBUTES_SPEC::save
        ));

        // Ivy the Dragon Merchant
        entries.add(new SectionEntry(Component.translatable("saintsdragons.config_screen.others.ivy")));
        entries.add(new BooleanEntry(
                Component.translatable("saintsdragons.config_screen.others.ivy.enabled"),
                () -> ForgeDragonAttributesConfig.IVY_HOUSE_ENABLED.get(),
                ForgeDragonAttributesConfig.IVY_HOUSE_ENABLED::set,
                ForgeDragonAttributesConfig.ATTRIBUTES_SPEC::save
        ));
        entries.add(new IntEntry(
                Component.translatable("saintsdragons.config_screen.others.ivy.restock_interval"),
                () -> ForgeDragonAttributesConfig.IVY_RESTOCK_INTERVAL.get(),
                val -> ForgeDragonAttributesConfig.IVY_RESTOCK_INTERVAL.set(val),
                null
        ));
    }

    @Override
    protected void onSave() {
        ForgeDragonAttributesConfig.ATTRIBUTES_SPEC.save();
        SaintsDragonsForge.syncRuntimeOthersFromForgeConfig();
    }
}

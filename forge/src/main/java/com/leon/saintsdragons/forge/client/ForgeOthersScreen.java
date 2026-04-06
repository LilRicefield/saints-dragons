package com.leon.saintsdragons.forge.client;

import com.leon.saintsdragons.common.config.SaintsDragonsConfig;
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
                () -> SaintsDragonsConfig.DRAGON_GRIEFING_ENABLED.get(),
                val -> SaintsDragonsConfig.DRAGON_GRIEFING_ENABLED.set(val),
                SaintsDragonsConfig.DRAGON_GRIEFING_ENABLED::save
        ));
        entries.add(new BooleanEntry(
                Component.translatable("saintsdragons.config_screen.others.screen_shake"),
                () -> SaintsDragonsConfig.SCREEN_SHAKE_ENABLED.get(),
                val -> SaintsDragonsConfig.SCREEN_SHAKE_ENABLED.set(val),
                SaintsDragonsConfig.SCREEN_SHAKE_ENABLED::save
        ));
        entries.add(new BooleanEntry(
                Component.translatable("saintsdragons.config_screen.others.barrel_roll"),
                () -> SaintsDragonsConfig.BARREL_ROLL_ENABLED.get(),
                val -> SaintsDragonsConfig.BARREL_ROLL_ENABLED.set(val),
                SaintsDragonsConfig.BARREL_ROLL_ENABLED::save
        ));

        // Ivy the Dragon Merchant
        entries.add(new SectionEntry(Component.translatable("saintsdragons.config_screen.others.ivy")));
        entries.add(new IntEntry(
                Component.translatable("saintsdragons.config_screen.others.ivy.restock_interval"),
                () -> ForgeDragonAttributesConfig.IVY_RESTOCK_INTERVAL.get(),
                val -> ForgeDragonAttributesConfig.IVY_RESTOCK_INTERVAL.set(val),
                null
        ));
    }

    @Override
    protected void onSave() {
        SaintsDragonsConfig.DRAGON_GRIEFING_ENABLED.save();
        SaintsDragonsConfig.SCREEN_SHAKE_ENABLED.save();
        SaintsDragonsConfig.BARREL_ROLL_ENABLED.save();
        ForgeDragonAttributesConfig.ATTRIBUTES_SPEC.save();
    }
}

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
        ForgeDragonAttributesConfig.ATTRIBUTES_SPEC.save();
    }
}

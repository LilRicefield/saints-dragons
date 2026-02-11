package com.leon.saintsdragons.forge.client;

import com.leon.saintsdragons.common.config.dragon.DragonAttributeConfigLoader;
import com.leon.saintsdragons.forge.platform.ForgeDragonAttributesConfig;
import com.leon.saintsdragons.server.entity.base.DragonEntity;
import com.leon.saintsdragons.server.entity.dragons.cindervane.Cindervane;
import com.leon.saintsdragons.server.entity.dragons.ignivorus.Ignivorus;
import com.leon.saintsdragons.server.entity.dragons.nulljaw.Nulljaw;
import com.leon.saintsdragons.server.entity.dragons.raevyx.Raevyx;
import com.leon.saintsdragons.server.entity.dragons.stegonaut.Stegonaut;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.phys.AABB;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.server.ServerLifecycleHooks;

import java.util.List;

@OnlyIn(Dist.CLIENT)
public final class ForgeDragonAttributesScreen extends ForgePagedConfigScreen {
    private enum Section {
        CINDERVANE,
        STEGONAUT,
        RAEVYX,
        NULLJAW,
        IGNIVORUS
    }

    private Section section = Section.CINDERVANE;

    public ForgeDragonAttributesScreen(Screen parent) {
        super(parent, Component.translatable("saintsdragons.config_screen.attributes"));
    }

    @Override
    protected void buildEntries(List<ConfigEntry> entries) {
        switch (section) {
            case CINDERVANE -> addCindervaneEntries(entries);
            case STEGONAUT -> addStegonautEntries(entries);
            case RAEVYX -> addRaevyxEntries(entries);
            case NULLJAW -> addNulljawEntries(entries);
            case IGNIVORUS -> addIgnivorusEntries(entries);
        }
    }

    @Override
    protected void addHeaderButtons() {
        int buttonWidth = Math.min(90, (width - 60) / 5);
        int spacing = 6;
        int totalWidth = buttonWidth * 5 + spacing * 4;
        int startX = (width - totalWidth) / 2;
        int y = 32;

        addRenderableWidget(net.minecraft.client.gui.components.Button.builder(Component.translatable("config.saintsdragons.attributes.cindervane"), button -> {
            if (section != Section.CINDERVANE) {
                section = Section.CINDERVANE;
                rebuildWidgets();
            }
        }).bounds(startX, y, buttonWidth, 20).build());

        addRenderableWidget(net.minecraft.client.gui.components.Button.builder(Component.translatable("config.saintsdragons.attributes.stegonaut"), button -> {
            if (section != Section.STEGONAUT) {
                section = Section.STEGONAUT;
                rebuildWidgets();
            }
        }).bounds(startX + (buttonWidth + spacing), y, buttonWidth, 20).build());

        addRenderableWidget(net.minecraft.client.gui.components.Button.builder(Component.translatable("config.saintsdragons.attributes.raevyx"), button -> {
            if (section != Section.RAEVYX) {
                section = Section.RAEVYX;
                rebuildWidgets();
            }
        }).bounds(startX + (buttonWidth + spacing) * 2, y, buttonWidth, 20).build());

        addRenderableWidget(net.minecraft.client.gui.components.Button.builder(Component.translatable("config.saintsdragons.attributes.nulljaw"), button -> {
            if (section != Section.NULLJAW) {
                section = Section.NULLJAW;
                rebuildWidgets();
            }
        }).bounds(startX + (buttonWidth + spacing) * 3, y, buttonWidth, 20).build());

        addRenderableWidget(net.minecraft.client.gui.components.Button.builder(Component.translatable("config.saintsdragons.attributes.ignivorus"), button -> {
            if (section != Section.IGNIVORUS) {
                section = Section.IGNIVORUS;
                rebuildWidgets();
            }
        }).bounds(startX + (buttonWidth + spacing) * 4, y, buttonWidth, 20).build());

        addRenderableWidget(net.minecraft.client.gui.components.Button.builder(Component.translatable("saintsdragons.config_screen.reset"), button -> {
            resetSection();
            rebuildWidgets();
        }).bounds(width / 2 - 150, height - 28, 60, 20).build());
    }

    @Override
    protected int getPanelTop() {
        return 60;
    }

    @Override
    protected void onSave() {
        ForgeDragonAttributesConfig.ATTRIBUTES_SPEC.save();
        DragonAttributeConfigLoader.getInstance().refreshFromForgeConfig();
        applyAttributesToLoadedDragons();
    }

    private void applyAttributesToLoadedDragons() {
        var server = ServerLifecycleHooks.getCurrentServer();
        if (server == null) {
            return;
        }

        for (var level : server.getAllLevels()) {
            AABB bounds = new AABB(
                    level.getWorldBorder().getMinX(),
                    level.getMinBuildHeight(),
                    level.getWorldBorder().getMinZ(),
                    level.getWorldBorder().getMaxX(),
                    level.getMaxBuildHeight(),
                    level.getWorldBorder().getMaxZ()
            );

            for (DragonEntity dragon : level.getEntitiesOfClass(DragonEntity.class, bounds)) {
                if (dragon instanceof Cindervane cindervane) {
                    cindervane.applyConfiguredAttributes();
                } else if (dragon instanceof Stegonaut stegonaut) {
                    stegonaut.applyConfiguredAttributes();
                } else if (dragon instanceof Raevyx raevyx) {
                    raevyx.applyConfiguredAttributes();
                } else if (dragon instanceof Nulljaw nulljaw) {
                    nulljaw.applyConfiguredAttributes();
                } else if (dragon instanceof Ignivorus ignivorus) {
                    ignivorus.applyConfiguredAttributes();
                }
            }
        }
    }

    private void addCindervaneEntries(List<ConfigEntry> entries) {
        entries.add(new SectionEntry(Component.translatable("config.saintsdragons.attributes.cindervane")));
        entries.add(new DoubleEntry(Component.translatable("config.saintsdragons.attributes.cindervane.max_health"),
                ForgeDragonAttributesConfig.CINDERVANE_MAX_HEALTH::get,
                ForgeDragonAttributesConfig.CINDERVANE_MAX_HEALTH::set,
                null));
        entries.add(new DoubleEntry(Component.translatable("config.saintsdragons.attributes.cindervane.armor"),
                ForgeDragonAttributesConfig.CINDERVANE_ARMOR::get,
                ForgeDragonAttributesConfig.CINDERVANE_ARMOR::set,
                null));
        entries.add(new DoubleEntry(Component.translatable("config.saintsdragons.attributes.cindervane.flying_speed"),
                ForgeDragonAttributesConfig.CINDERVANE_FLYING_SPEED::get,
                ForgeDragonAttributesConfig.CINDERVANE_FLYING_SPEED::set,
                null));
        entries.add(new DoubleEntry(Component.translatable("config.saintsdragons.attributes.cindervane.wild_flying_speed_multiplier"),
                ForgeDragonAttributesConfig.CINDERVANE_WILD_FLYING_SPEED_MULTIPLIER::get,
                ForgeDragonAttributesConfig.CINDERVANE_WILD_FLYING_SPEED_MULTIPLIER::set,
                null));
        entries.add(new DoubleEntry(Component.translatable("config.saintsdragons.attributes.cindervane.bite_damage"),
                ForgeDragonAttributesConfig.CINDERVANE_BITE_DAMAGE::get,
                ForgeDragonAttributesConfig.CINDERVANE_BITE_DAMAGE::set,
                null));
        entries.add(new DoubleEntry(Component.translatable("config.saintsdragons.attributes.cindervane.volley_damage"),
                ForgeDragonAttributesConfig.CINDERVANE_MAGMA_VOLLEY_DAMAGE::get,
                ForgeDragonAttributesConfig.CINDERVANE_MAGMA_VOLLEY_DAMAGE::set,
                null));
        entries.add(new DoubleEntry(Component.translatable("config.saintsdragons.attributes.cindervane.taming_base"),
                ForgeDragonAttributesConfig.CINDERVANE_TAMING_CHANCE_BASE::get,
                ForgeDragonAttributesConfig.CINDERVANE_TAMING_CHANCE_BASE::set,
                null));
        entries.add(new DoubleEntry(Component.translatable("config.saintsdragons.attributes.cindervane.taming_chicken"),
                ForgeDragonAttributesConfig.CINDERVANE_TAMING_CHANCE_CHICKEN::get,
                ForgeDragonAttributesConfig.CINDERVANE_TAMING_CHANCE_CHICKEN::set,
                null));
        entries.add(new DoubleEntry(Component.translatable("config.saintsdragons.attributes.cindervane.taming_hearty"),
                ForgeDragonAttributesConfig.CINDERVANE_TAMING_CHANCE_HEARTY::get,
                ForgeDragonAttributesConfig.CINDERVANE_TAMING_CHANCE_HEARTY::set,
                null));
        entries.add(new DoubleEntry(Component.translatable("config.saintsdragons.attributes.cindervane.egg_hatch_chance_normal"),
                ForgeDragonAttributesConfig.CINDERVANE_EGG_HATCH_CHANCE_NORMAL::get,
                ForgeDragonAttributesConfig.CINDERVANE_EGG_HATCH_CHANCE_NORMAL::set,
                null));
        entries.add(new DoubleEntry(Component.translatable("config.saintsdragons.attributes.cindervane.egg_drop_chance"),
                ForgeDragonAttributesConfig.CINDERVANE_EGG_DROP_CHANCE::get,
                ForgeDragonAttributesConfig.CINDERVANE_EGG_DROP_CHANCE::set,
                null));
        entries.add(new DoubleEntry(Component.translatable("config.saintsdragons.attributes.cindervane.fire_body_explosion_damage"),
                ForgeDragonAttributesConfig.CINDERVANE_FIRE_BODY_EXPLOSION_DAMAGE::get,
                ForgeDragonAttributesConfig.CINDERVANE_FIRE_BODY_EXPLOSION_DAMAGE::set,
                null));
        entries.add(new DoubleEntry(Component.translatable("config.saintsdragons.attributes.cindervane.fire_body_self_damage_on_crash"),
                ForgeDragonAttributesConfig.CINDERVANE_FIRE_BODY_SELF_DAMAGE_ON_CRASH::get,
                ForgeDragonAttributesConfig.CINDERVANE_FIRE_BODY_SELF_DAMAGE_ON_CRASH::set,
                null));
        entries.add(new BooleanEntry(Component.translatable("config.saintsdragons.attributes.cindervane.aggressive_wild"),
                () -> ForgeDragonAttributesConfig.CINDERVANE_AGGRESSIVE_WILD.get(),
                ForgeDragonAttributesConfig.CINDERVANE_AGGRESSIVE_WILD::set,
                null));
        entries.add(new BooleanEntry(Component.translatable("config.saintsdragons.attributes.reactive_terrain_clearing_on_damage"),
                () -> ForgeDragonAttributesConfig.CINDERVANE_REACTIVE_TERRAIN_CLEARING_ON_DAMAGE.get(),
                ForgeDragonAttributesConfig.CINDERVANE_REACTIVE_TERRAIN_CLEARING_ON_DAMAGE::set,
                null));
        entries.add(new BooleanEntry(Component.translatable("config.saintsdragons.attributes.reactive_terrain_clearing_on_damage_tamed"),
                () -> ForgeDragonAttributesConfig.CINDERVANE_REACTIVE_TERRAIN_CLEARING_ON_DAMAGE_TAMED.get(),
                ForgeDragonAttributesConfig.CINDERVANE_REACTIVE_TERRAIN_CLEARING_ON_DAMAGE_TAMED::set,
                null));
    }

    private void addStegonautEntries(List<ConfigEntry> entries) {
        entries.add(new SectionEntry(Component.translatable("config.saintsdragons.attributes.stegonaut")));
        entries.add(new DoubleEntry(Component.translatable("config.saintsdragons.attributes.stegonaut.max_health"),
                ForgeDragonAttributesConfig.STEGONAUT_MAX_HEALTH::get,
                ForgeDragonAttributesConfig.STEGONAUT_MAX_HEALTH::set,
                null));
        entries.add(new DoubleEntry(Component.translatable("config.saintsdragons.attributes.stegonaut.armor"),
                ForgeDragonAttributesConfig.STEGONAUT_ARMOR::get,
                ForgeDragonAttributesConfig.STEGONAUT_ARMOR::set,
                null));
        entries.add(new DoubleEntry(Component.translatable("config.saintsdragons.attributes.stegonaut.bite_damage"),
                ForgeDragonAttributesConfig.STEGONAUT_BITE_DAMAGE::get,
                ForgeDragonAttributesConfig.STEGONAUT_BITE_DAMAGE::set,
                null));
        entries.add(new DoubleEntry(Component.translatable("config.saintsdragons.attributes.stegonaut.chin_slam_damage"),
                ForgeDragonAttributesConfig.STEGONAUT_CHIN_SLAM_DAMAGE::get,
                ForgeDragonAttributesConfig.STEGONAUT_CHIN_SLAM_DAMAGE::set,
                null));
        entries.add(new DoubleEntry(Component.translatable("config.saintsdragons.attributes.stegonaut.ground_eating_damage"),
                ForgeDragonAttributesConfig.STEGONAUT_GROUND_EATING_DAMAGE::get,
                ForgeDragonAttributesConfig.STEGONAUT_GROUND_EATING_DAMAGE::set,
                null));
        entries.add(new DoubleEntry(Component.translatable("config.saintsdragons.attributes.stegonaut.taming_base"),
                ForgeDragonAttributesConfig.STEGONAUT_TAMING_CHANCE_BASE::get,
                ForgeDragonAttributesConfig.STEGONAUT_TAMING_CHANCE_BASE::set,
                null));
        entries.add(new DoubleEntry(Component.translatable("config.saintsdragons.attributes.stegonaut.taming_hearty"),
                ForgeDragonAttributesConfig.STEGONAUT_TAMING_CHANCE_HEARTY::get,
                ForgeDragonAttributesConfig.STEGONAUT_TAMING_CHANCE_HEARTY::set,
                null));
        entries.add(new DoubleEntry(Component.translatable("config.saintsdragons.attributes.stegonaut.egg_hatch_chance_normal"),
                ForgeDragonAttributesConfig.STEGONAUT_EGG_HATCH_CHANCE_NORMAL::get,
                ForgeDragonAttributesConfig.STEGONAUT_EGG_HATCH_CHANCE_NORMAL::set,
                null));
        entries.add(new DoubleEntry(Component.translatable("config.saintsdragons.attributes.stegonaut.egg_drop_chance"),
                ForgeDragonAttributesConfig.STEGONAUT_EGG_DROP_CHANCE::get,
                ForgeDragonAttributesConfig.STEGONAUT_EGG_DROP_CHANCE::set,
                null));
        entries.add(new BooleanEntry(Component.translatable("config.saintsdragons.attributes.reactive_terrain_clearing_on_damage"),
                () -> ForgeDragonAttributesConfig.STEGONAUT_REACTIVE_TERRAIN_CLEARING_ON_DAMAGE.get(),
                ForgeDragonAttributesConfig.STEGONAUT_REACTIVE_TERRAIN_CLEARING_ON_DAMAGE::set,
                null));
        entries.add(new BooleanEntry(Component.translatable("config.saintsdragons.attributes.reactive_terrain_clearing_on_damage_tamed"),
                () -> ForgeDragonAttributesConfig.STEGONAUT_REACTIVE_TERRAIN_CLEARING_ON_DAMAGE_TAMED.get(),
                ForgeDragonAttributesConfig.STEGONAUT_REACTIVE_TERRAIN_CLEARING_ON_DAMAGE_TAMED::set,
                null));
    }

    private void addRaevyxEntries(List<ConfigEntry> entries) {
        entries.add(new SectionEntry(Component.translatable("config.saintsdragons.attributes.raevyx")));
        entries.add(new DoubleEntry(Component.translatable("config.saintsdragons.attributes.raevyx.max_health"),
                ForgeDragonAttributesConfig.RAEVYX_MAX_HEALTH::get,
                ForgeDragonAttributesConfig.RAEVYX_MAX_HEALTH::set,
                null));
        entries.add(new DoubleEntry(Component.translatable("config.saintsdragons.attributes.raevyx.armor"),
                ForgeDragonAttributesConfig.RAEVYX_ARMOR::get,
                ForgeDragonAttributesConfig.RAEVYX_ARMOR::set,
                null));
        entries.add(new DoubleEntry(Component.translatable("config.saintsdragons.attributes.raevyx.flying_speed"),
                ForgeDragonAttributesConfig.RAEVYX_FLYING_SPEED::get,
                ForgeDragonAttributesConfig.RAEVYX_FLYING_SPEED::set,
                null));
        entries.add(new DoubleEntry(Component.translatable("config.saintsdragons.attributes.raevyx.wild_flying_speed_multiplier"),
                ForgeDragonAttributesConfig.RAEVYX_WILD_FLYING_SPEED_MULTIPLIER::get,
                ForgeDragonAttributesConfig.RAEVYX_WILD_FLYING_SPEED_MULTIPLIER::set,
                null));
        entries.add(new DoubleEntry(Component.translatable("config.saintsdragons.attributes.raevyx.bite_damage"),
                ForgeDragonAttributesConfig.RAEVYX_BITE_DAMAGE::get,
                ForgeDragonAttributesConfig.RAEVYX_BITE_DAMAGE::set,
                null));
        entries.add(new DoubleEntry(Component.translatable("config.saintsdragons.attributes.raevyx.beam_damage"),
                ForgeDragonAttributesConfig.RAEVYX_LIGHTNING_BEAM_DAMAGE::get,
                ForgeDragonAttributesConfig.RAEVYX_LIGHTNING_BEAM_DAMAGE::set,
                null));
        entries.add(new DoubleEntry(Component.translatable("config.saintsdragons.attributes.raevyx.horn_damage"),
                ForgeDragonAttributesConfig.RAEVYX_HORN_GORE_DAMAGE::get,
                ForgeDragonAttributesConfig.RAEVYX_HORN_GORE_DAMAGE::set,
                null));
        entries.add(new DoubleEntry(Component.translatable("config.saintsdragons.attributes.raevyx.beam_drain_per_tick"),
                ForgeDragonAttributesConfig.RAEVYX_BEAM_DRAIN_PER_TICK::get,
                ForgeDragonAttributesConfig.RAEVYX_BEAM_DRAIN_PER_TICK::set,
                null));
        entries.add(new DoubleEntry(Component.translatable("config.saintsdragons.attributes.raevyx.beam_regen_per_tick"),
                ForgeDragonAttributesConfig.RAEVYX_BEAM_REGEN_PER_TICK::get,
                ForgeDragonAttributesConfig.RAEVYX_BEAM_REGEN_PER_TICK::set,
                null));
        entries.add(new DoubleEntry(Component.translatable("config.saintsdragons.attributes.raevyx.summon_storm_cooldown_ticks"),
                ForgeDragonAttributesConfig.RAEVYX_SUMMON_STORM_COOLDOWN_TICKS::get,
                ForgeDragonAttributesConfig.RAEVYX_SUMMON_STORM_COOLDOWN_TICKS::set,
                null));
        entries.add(new DoubleEntry(Component.translatable("config.saintsdragons.attributes.raevyx.summon_storm_supercharge_ticks"),
                ForgeDragonAttributesConfig.RAEVYX_SUMMON_STORM_SUPERCHARGE_TICKS::get,
                ForgeDragonAttributesConfig.RAEVYX_SUMMON_STORM_SUPERCHARGE_TICKS::set,
                null));
        entries.add(new DoubleEntry(Component.translatable("config.saintsdragons.attributes.raevyx.summon_storm_supercharge_damage_multiplier"),
                ForgeDragonAttributesConfig.RAEVYX_SUMMON_STORM_SUPERCHARGE_DAMAGE_MULTIPLIER::get,
                ForgeDragonAttributesConfig.RAEVYX_SUMMON_STORM_SUPERCHARGE_DAMAGE_MULTIPLIER::set,
                null));
        entries.add(new DoubleEntry(Component.translatable("config.saintsdragons.attributes.raevyx.summon_storm_duration_ticks"),
                ForgeDragonAttributesConfig.RAEVYX_SUMMON_STORM_DURATION_TICKS::get,
                ForgeDragonAttributesConfig.RAEVYX_SUMMON_STORM_DURATION_TICKS::set,
                null));
        entries.add(new DoubleEntry(Component.translatable("config.saintsdragons.attributes.raevyx.taming_base"),
                ForgeDragonAttributesConfig.RAEVYX_TAMING_CHANCE_BASE::get,
                ForgeDragonAttributesConfig.RAEVYX_TAMING_CHANCE_BASE::set,
                null));
        entries.add(new DoubleEntry(Component.translatable("config.saintsdragons.attributes.raevyx.taming_hearty"),
                ForgeDragonAttributesConfig.RAEVYX_TAMING_CHANCE_HEARTY::get,
                ForgeDragonAttributesConfig.RAEVYX_TAMING_CHANCE_HEARTY::set,
                null));
        entries.add(new DoubleEntry(Component.translatable("config.saintsdragons.attributes.raevyx.taming_stun_health"),
                ForgeDragonAttributesConfig.RAEVYX_TAMING_STUN_HEALTH::get,
                ForgeDragonAttributesConfig.RAEVYX_TAMING_STUN_HEALTH::set,
                null));
        entries.add(new BooleanEntry(Component.translatable("config.saintsdragons.attributes.raevyx.legacy_taming"),
                () -> ForgeDragonAttributesConfig.RAEVYX_LEGACY_TAMING.get(),
                ForgeDragonAttributesConfig.RAEVYX_LEGACY_TAMING::set,
                null));
        entries.add(new DoubleEntry(Component.translatable("config.saintsdragons.attributes.raevyx.egg_hatch_chance_normal"),
                ForgeDragonAttributesConfig.RAEVYX_EGG_HATCH_CHANCE_NORMAL::get,
                ForgeDragonAttributesConfig.RAEVYX_EGG_HATCH_CHANCE_NORMAL::set,
                null));
        entries.add(new DoubleEntry(Component.translatable("config.saintsdragons.attributes.raevyx.egg_hatch_chance_thunder"),
                ForgeDragonAttributesConfig.RAEVYX_EGG_HATCH_CHANCE_THUNDER::get,
                ForgeDragonAttributesConfig.RAEVYX_EGG_HATCH_CHANCE_THUNDER::set,
                null));
        entries.add(new DoubleEntry(Component.translatable("config.saintsdragons.attributes.raevyx.egg_storm_instant_chance"),
                ForgeDragonAttributesConfig.RAEVYX_EGG_STORM_INSTANT_CHANCE::get,
                ForgeDragonAttributesConfig.RAEVYX_EGG_STORM_INSTANT_CHANCE::set,
                null));
        entries.add(new DoubleEntry(Component.translatable("config.saintsdragons.attributes.raevyx.egg_loot_pillager_outpost"),
                ForgeDragonAttributesConfig.RAEVYX_EGG_LOOT_PILLAGER_OUTPOST::get,
                ForgeDragonAttributesConfig.RAEVYX_EGG_LOOT_PILLAGER_OUTPOST::set,
                null));
        entries.add(new DoubleEntry(Component.translatable("config.saintsdragons.attributes.raevyx.egg_loot_shipwreck_treasure"),
                ForgeDragonAttributesConfig.RAEVYX_EGG_LOOT_SHIPWRECK_TREASURE::get,
                ForgeDragonAttributesConfig.RAEVYX_EGG_LOOT_SHIPWRECK_TREASURE::set,
                null));
        entries.add(new DoubleEntry(Component.translatable("config.saintsdragons.attributes.raevyx.egg_loot_ancient_city"),
                ForgeDragonAttributesConfig.RAEVYX_EGG_LOOT_ANCIENT_CITY::get,
                ForgeDragonAttributesConfig.RAEVYX_EGG_LOOT_ANCIENT_CITY::set,
                null));
        entries.add(new DoubleEntry(Component.translatable("config.saintsdragons.attributes.raevyx.egg_drop_chance"),
                ForgeDragonAttributesConfig.RAEVYX_EGG_DROP_CHANCE::get,
                ForgeDragonAttributesConfig.RAEVYX_EGG_DROP_CHANCE::set,
                null));
        entries.add(new BooleanEntry(Component.translatable("config.saintsdragons.attributes.raevyx.aggressive_wild"),
                () -> ForgeDragonAttributesConfig.RAEVYX_AGGRESSIVE_WILD.get(),
                ForgeDragonAttributesConfig.RAEVYX_AGGRESSIVE_WILD::set,
                null));
        entries.add(new BooleanEntry(Component.translatable("config.saintsdragons.attributes.reactive_terrain_clearing_on_damage"),
                () -> ForgeDragonAttributesConfig.RAEVYX_REACTIVE_TERRAIN_CLEARING_ON_DAMAGE.get(),
                ForgeDragonAttributesConfig.RAEVYX_REACTIVE_TERRAIN_CLEARING_ON_DAMAGE::set,
                null));
        entries.add(new BooleanEntry(Component.translatable("config.saintsdragons.attributes.reactive_terrain_clearing_on_damage_tamed"),
                () -> ForgeDragonAttributesConfig.RAEVYX_REACTIVE_TERRAIN_CLEARING_ON_DAMAGE_TAMED.get(),
                ForgeDragonAttributesConfig.RAEVYX_REACTIVE_TERRAIN_CLEARING_ON_DAMAGE_TAMED::set,
                null));
    }

    private void addNulljawEntries(List<ConfigEntry> entries) {
        entries.add(new SectionEntry(Component.translatable("config.saintsdragons.attributes.nulljaw")));
        entries.add(new DoubleEntry(Component.translatable("config.saintsdragons.attributes.nulljaw.max_health"),
                ForgeDragonAttributesConfig.NULLJAW_MAX_HEALTH::get,
                ForgeDragonAttributesConfig.NULLJAW_MAX_HEALTH::set,
                null));
        entries.add(new DoubleEntry(Component.translatable("config.saintsdragons.attributes.nulljaw.armor"),
                ForgeDragonAttributesConfig.NULLJAW_ARMOR::get,
                ForgeDragonAttributesConfig.NULLJAW_ARMOR::set,
                null));
        entries.add(new DoubleEntry(Component.translatable("config.saintsdragons.attributes.nulljaw.swim_speed"),
                ForgeDragonAttributesConfig.NULLJAW_SWIM_SPEED::get,
                ForgeDragonAttributesConfig.NULLJAW_SWIM_SPEED::set,
                null));
        entries.add(new DoubleEntry(Component.translatable("config.saintsdragons.attributes.nulljaw.bite_phase1"),
                ForgeDragonAttributesConfig.NULLJAW_BITE_PHASE1_DAMAGE::get,
                ForgeDragonAttributesConfig.NULLJAW_BITE_PHASE1_DAMAGE::set,
                null));
        entries.add(new DoubleEntry(Component.translatable("config.saintsdragons.attributes.nulljaw.bite_phase2"),
                ForgeDragonAttributesConfig.NULLJAW_BITE_PHASE2_DAMAGE::get,
                ForgeDragonAttributesConfig.NULLJAW_BITE_PHASE2_DAMAGE::set,
                null));
        entries.add(new DoubleEntry(Component.translatable("config.saintsdragons.attributes.nulljaw.horn_phase1"),
                ForgeDragonAttributesConfig.NULLJAW_HORN_GORE_PHASE1_DAMAGE::get,
                ForgeDragonAttributesConfig.NULLJAW_HORN_GORE_PHASE1_DAMAGE::set,
                null));
        entries.add(new DoubleEntry(Component.translatable("config.saintsdragons.attributes.nulljaw.horn_phase2"),
                ForgeDragonAttributesConfig.NULLJAW_HORN_GORE_PHASE2_DAMAGE::get,
                ForgeDragonAttributesConfig.NULLJAW_HORN_GORE_PHASE2_DAMAGE::set,
                null));
        entries.add(new DoubleEntry(Component.translatable("config.saintsdragons.attributes.nulljaw.taming_chance"),
                ForgeDragonAttributesConfig.NULLJAW_TAMING_CHANCE::get,
                ForgeDragonAttributesConfig.NULLJAW_TAMING_CHANCE::set,
                null));
        entries.add(new DoubleEntry(Component.translatable("config.saintsdragons.attributes.nulljaw.taming_tropical"),
                ForgeDragonAttributesConfig.NULLJAW_TAMING_CHANCE_TROPICAL::get,
                ForgeDragonAttributesConfig.NULLJAW_TAMING_CHANCE_TROPICAL::set,
                null));
        entries.add(new BooleanEntry(Component.translatable("config.saintsdragons.attributes.nulljaw.legacy_taming"),
                () -> ForgeDragonAttributesConfig.NULLJAW_LEGACY_TAMING.get(),
                ForgeDragonAttributesConfig.NULLJAW_LEGACY_TAMING::set,
                null));
        entries.add(new DoubleEntry(Component.translatable("config.saintsdragons.attributes.nulljaw.egg_hatch_chance_normal"),
                ForgeDragonAttributesConfig.NULLJAW_EGG_HATCH_CHANCE_NORMAL::get,
                ForgeDragonAttributesConfig.NULLJAW_EGG_HATCH_CHANCE_NORMAL::set,
                null));
        entries.add(new DoubleEntry(Component.translatable("config.saintsdragons.attributes.nulljaw.egg_drop_chance"),
                ForgeDragonAttributesConfig.NULLJAW_EGG_DROP_CHANCE::get,
                ForgeDragonAttributesConfig.NULLJAW_EGG_DROP_CHANCE::set,
                null));
        entries.add(new BooleanEntry(Component.translatable("config.saintsdragons.attributes.nulljaw.aggressive_wild"),
                () -> ForgeDragonAttributesConfig.NULLJAW_AGGRESSIVE_WILD.get(),
                ForgeDragonAttributesConfig.NULLJAW_AGGRESSIVE_WILD::set,
                null));
        entries.add(new BooleanEntry(Component.translatable("config.saintsdragons.attributes.reactive_terrain_clearing_on_damage"),
                () -> ForgeDragonAttributesConfig.NULLJAW_REACTIVE_TERRAIN_CLEARING_ON_DAMAGE.get(),
                ForgeDragonAttributesConfig.NULLJAW_REACTIVE_TERRAIN_CLEARING_ON_DAMAGE::set,
                null));
        entries.add(new BooleanEntry(Component.translatable("config.saintsdragons.attributes.reactive_terrain_clearing_on_damage_tamed"),
                () -> ForgeDragonAttributesConfig.NULLJAW_REACTIVE_TERRAIN_CLEARING_ON_DAMAGE_TAMED.get(),
                ForgeDragonAttributesConfig.NULLJAW_REACTIVE_TERRAIN_CLEARING_ON_DAMAGE_TAMED::set,
                null));
    }

    private void addIgnivorusEntries(List<ConfigEntry> entries) {
        entries.add(new SectionEntry(Component.translatable("config.saintsdragons.attributes.ignivorus")));
        entries.add(new DoubleEntry(Component.translatable("config.saintsdragons.attributes.ignivorus.max_health"),
                ForgeDragonAttributesConfig.IGNIVORUS_MAX_HEALTH::get,
                ForgeDragonAttributesConfig.IGNIVORUS_MAX_HEALTH::set,
                null));
        entries.add(new DoubleEntry(Component.translatable("config.saintsdragons.attributes.ignivorus.armor"),
                ForgeDragonAttributesConfig.IGNIVORUS_ARMOR::get,
                ForgeDragonAttributesConfig.IGNIVORUS_ARMOR::set,
                null));
        entries.add(new DoubleEntry(Component.translatable("config.saintsdragons.attributes.ignivorus.flying_speed"),
                ForgeDragonAttributesConfig.IGNIVORUS_FLYING_SPEED::get,
                ForgeDragonAttributesConfig.IGNIVORUS_FLYING_SPEED::set,
                null));
        entries.add(new DoubleEntry(Component.translatable("config.saintsdragons.attributes.ignivorus.wild_flying_speed_multiplier"),
                ForgeDragonAttributesConfig.IGNIVORUS_WILD_FLYING_SPEED_MULTIPLIER::get,
                ForgeDragonAttributesConfig.IGNIVORUS_WILD_FLYING_SPEED_MULTIPLIER::set,
                null));
        entries.add(new DoubleEntry(Component.translatable("config.saintsdragons.attributes.ignivorus.bite_damage"),
                ForgeDragonAttributesConfig.IGNIVORUS_BITE_DAMAGE::get,
                ForgeDragonAttributesConfig.IGNIVORUS_BITE_DAMAGE::set,
                null));
        entries.add(new DoubleEntry(Component.translatable("config.saintsdragons.attributes.ignivorus.body_slam_damage"),
                ForgeDragonAttributesConfig.IGNIVORUS_BODY_SLAM_DAMAGE::get,
                ForgeDragonAttributesConfig.IGNIVORUS_BODY_SLAM_DAMAGE::set,
                null));
        entries.add(new DoubleEntry(Component.translatable("config.saintsdragons.attributes.ignivorus.fire_breath_damage"),
                ForgeDragonAttributesConfig.IGNIVORUS_FIRE_BREATH_DAMAGE::get,
                ForgeDragonAttributesConfig.IGNIVORUS_FIRE_BREATH_DAMAGE::set,
                null));
        entries.add(new DoubleEntry(Component.translatable("config.saintsdragons.attributes.ignivorus.fireball_damage"),
                ForgeDragonAttributesConfig.IGNIVORUS_FIREBALL_DAMAGE::get,
                ForgeDragonAttributesConfig.IGNIVORUS_FIREBALL_DAMAGE::set,
                null));
        entries.add(new DoubleEntry(Component.translatable("config.saintsdragons.attributes.ignivorus.wing_swipe_damage"),
                ForgeDragonAttributesConfig.IGNIVORUS_WING_SWIPE_DAMAGE::get,
                ForgeDragonAttributesConfig.IGNIVORUS_WING_SWIPE_DAMAGE::set,
                null));
        entries.add(new DoubleEntry(Component.translatable("config.saintsdragons.attributes.ignivorus.stomp_damage"),
                ForgeDragonAttributesConfig.IGNIVORUS_STOMP_DAMAGE::get,
                ForgeDragonAttributesConfig.IGNIVORUS_STOMP_DAMAGE::set,
                null));
        entries.add(new DoubleEntry(Component.translatable("config.saintsdragons.attributes.ignivorus.ultimate_damage"),
                ForgeDragonAttributesConfig.IGNIVORUS_ULTIMATE_DAMAGE::get,
                ForgeDragonAttributesConfig.IGNIVORUS_ULTIMATE_DAMAGE::set,
                null));
        entries.add(new DoubleEntry(Component.translatable("config.saintsdragons.attributes.ignivorus.ultimate_penalty"),
                ForgeDragonAttributesConfig.IGNIVORUS_ULTIMATE_PENALTY_HEALTH::get,
                ForgeDragonAttributesConfig.IGNIVORUS_ULTIMATE_PENALTY_HEALTH::set,
                null));
        entries.add(new DoubleEntry(Component.translatable("config.saintsdragons.attributes.ignivorus.fire_breath_drain_per_tick"),
                ForgeDragonAttributesConfig.IGNIVORUS_FIRE_BREATH_DRAIN_PER_TICK::get,
                ForgeDragonAttributesConfig.IGNIVORUS_FIRE_BREATH_DRAIN_PER_TICK::set,
                null));
        entries.add(new DoubleEntry(Component.translatable("config.saintsdragons.attributes.ignivorus.fire_breath_regen_per_tick"),
                ForgeDragonAttributesConfig.IGNIVORUS_FIRE_BREATH_REGEN_PER_TICK::get,
                ForgeDragonAttributesConfig.IGNIVORUS_FIRE_BREATH_REGEN_PER_TICK::set,
                null));
        entries.add(new DoubleEntry(Component.translatable("config.saintsdragons.attributes.ignivorus.fire_breath_flame_spawn_multiplier"),
                ForgeDragonAttributesConfig.IGNIVORUS_FIRE_BREATH_FLAME_SPAWN_MULTIPLIER::get,
                ForgeDragonAttributesConfig.IGNIVORUS_FIRE_BREATH_FLAME_SPAWN_MULTIPLIER::set,
                null));
        entries.add(new DoubleEntry(Component.translatable("config.saintsdragons.attributes.ignivorus.fire_breath_flame_speed_multiplier"),
                ForgeDragonAttributesConfig.IGNIVORUS_FIRE_BREATH_FLAME_SPEED_MULTIPLIER::get,
                ForgeDragonAttributesConfig.IGNIVORUS_FIRE_BREATH_FLAME_SPEED_MULTIPLIER::set,
                null));
        entries.add(new DoubleEntry(Component.translatable("config.saintsdragons.attributes.ignivorus.fire_breath_flame_lifetime_multiplier"),
                ForgeDragonAttributesConfig.IGNIVORUS_FIRE_BREATH_FLAME_LIFETIME_MULTIPLIER::get,
                ForgeDragonAttributesConfig.IGNIVORUS_FIRE_BREATH_FLAME_LIFETIME_MULTIPLIER::set,
                null));
        entries.add(new DoubleEntry(Component.translatable("config.saintsdragons.attributes.ignivorus.fire_breath_ignite_block_chance"),
                ForgeDragonAttributesConfig.IGNIVORUS_FIRE_BREATH_IGNITE_BLOCK_CHANCE::get,
                ForgeDragonAttributesConfig.IGNIVORUS_FIRE_BREATH_IGNITE_BLOCK_CHANCE::set,
                null));
        entries.add(new DoubleEntry(Component.translatable("config.saintsdragons.attributes.ignivorus.phase2_toggle_on_chance"),
                ForgeDragonAttributesConfig.IGNIVORUS_PHASE2_TOGGLE_ON_CHANCE::get,
                ForgeDragonAttributesConfig.IGNIVORUS_PHASE2_TOGGLE_ON_CHANCE::set,
                null));
        entries.add(new DoubleEntry(Component.translatable("config.saintsdragons.attributes.ignivorus.phase2_toggle_off_chance"),
                ForgeDragonAttributesConfig.IGNIVORUS_PHASE2_TOGGLE_OFF_CHANCE::get,
                ForgeDragonAttributesConfig.IGNIVORUS_PHASE2_TOGGLE_OFF_CHANCE::set,
                null));
        entries.add(new DoubleEntry(Component.translatable("config.saintsdragons.attributes.ignivorus.phase2_decision_min_ticks"),
                ForgeDragonAttributesConfig.IGNIVORUS_PHASE2_DECISION_MIN_TICKS::get,
                ForgeDragonAttributesConfig.IGNIVORUS_PHASE2_DECISION_MIN_TICKS::set,
                null));
        entries.add(new DoubleEntry(Component.translatable("config.saintsdragons.attributes.ignivorus.phase2_decision_max_ticks"),
                ForgeDragonAttributesConfig.IGNIVORUS_PHASE2_DECISION_MAX_TICKS::get,
                ForgeDragonAttributesConfig.IGNIVORUS_PHASE2_DECISION_MAX_TICKS::set,
                null));
        entries.add(new DoubleEntry(Component.translatable("config.saintsdragons.attributes.ignivorus.taming_base"),
                ForgeDragonAttributesConfig.IGNIVORUS_TAMING_CHANCE_BASE::get,
                ForgeDragonAttributesConfig.IGNIVORUS_TAMING_CHANCE_BASE::set,
                null));
        entries.add(new DoubleEntry(Component.translatable("config.saintsdragons.attributes.ignivorus.taming_beef"),
                ForgeDragonAttributesConfig.IGNIVORUS_TAMING_CHANCE_BEEF::get,
                ForgeDragonAttributesConfig.IGNIVORUS_TAMING_CHANCE_BEEF::set,
                null));
        entries.add(new DoubleEntry(Component.translatable("config.saintsdragons.attributes.ignivorus.taming_hearty"),
                ForgeDragonAttributesConfig.IGNIVORUS_TAMING_CHANCE_HEARTY::get,
                ForgeDragonAttributesConfig.IGNIVORUS_TAMING_CHANCE_HEARTY::set,
                null));
        entries.add(new DoubleEntry(Component.translatable("config.saintsdragons.attributes.ignivorus.taming_stun_health"),
                ForgeDragonAttributesConfig.IGNIVORUS_TAMING_STUN_HEALTH::get,
                ForgeDragonAttributesConfig.IGNIVORUS_TAMING_STUN_HEALTH::set,
                null));
        entries.add(new BooleanEntry(Component.translatable("config.saintsdragons.attributes.ignivorus.legacy_taming"),
                () -> ForgeDragonAttributesConfig.IGNIVORUS_LEGACY_TAMING.get(),
                ForgeDragonAttributesConfig.IGNIVORUS_LEGACY_TAMING::set,
                null));
        entries.add(new DoubleEntry(Component.translatable("config.saintsdragons.attributes.ignivorus.egg_hatch_chance_normal"),
                ForgeDragonAttributesConfig.IGNIVORUS_EGG_HATCH_CHANCE_NORMAL::get,
                ForgeDragonAttributesConfig.IGNIVORUS_EGG_HATCH_CHANCE_NORMAL::set,
                null));
        entries.add(new DoubleEntry(Component.translatable("config.saintsdragons.attributes.ignivorus.egg_loot_bastion_treasure"),
                ForgeDragonAttributesConfig.IGNIVORUS_EGG_LOOT_BASTION_TREASURE::get,
                ForgeDragonAttributesConfig.IGNIVORUS_EGG_LOOT_BASTION_TREASURE::set,
                null));
        entries.add(new DoubleEntry(Component.translatable("config.saintsdragons.attributes.ignivorus.egg_loot_nether_bridge"),
                ForgeDragonAttributesConfig.IGNIVORUS_EGG_LOOT_NETHER_BRIDGE::get,
                ForgeDragonAttributesConfig.IGNIVORUS_EGG_LOOT_NETHER_BRIDGE::set,
                null));
        entries.add(new DoubleEntry(Component.translatable("config.saintsdragons.attributes.ignivorus.egg_loot_ancient_city"),
                ForgeDragonAttributesConfig.IGNIVORUS_EGG_LOOT_ANCIENT_CITY::get,
                ForgeDragonAttributesConfig.IGNIVORUS_EGG_LOOT_ANCIENT_CITY::set,
                null));
        entries.add(new DoubleEntry(Component.translatable("config.saintsdragons.attributes.ignivorus.egg_drop_chance"),
                ForgeDragonAttributesConfig.IGNIVORUS_EGG_DROP_CHANCE::get,
                ForgeDragonAttributesConfig.IGNIVORUS_EGG_DROP_CHANCE::set,
                null));
        entries.add(new BooleanEntry(Component.translatable("config.saintsdragons.attributes.ignivorus.aggressive_wild"),
                () -> ForgeDragonAttributesConfig.IGNIVORUS_AGGRESSIVE_WILD.get(),
                ForgeDragonAttributesConfig.IGNIVORUS_AGGRESSIVE_WILD::set,
                null));
        entries.add(new BooleanEntry(Component.translatable("config.saintsdragons.attributes.reactive_terrain_clearing_on_damage"),
                () -> ForgeDragonAttributesConfig.IGNIVORUS_REACTIVE_TERRAIN_CLEARING_ON_DAMAGE.get(),
                ForgeDragonAttributesConfig.IGNIVORUS_REACTIVE_TERRAIN_CLEARING_ON_DAMAGE::set,
                null));
        entries.add(new BooleanEntry(Component.translatable("config.saintsdragons.attributes.reactive_terrain_clearing_on_damage_tamed"),
                () -> ForgeDragonAttributesConfig.IGNIVORUS_REACTIVE_TERRAIN_CLEARING_ON_DAMAGE_TAMED.get(),
                ForgeDragonAttributesConfig.IGNIVORUS_REACTIVE_TERRAIN_CLEARING_ON_DAMAGE_TAMED::set,
                null));
    }

    private void resetSection() {
        switch (section) {
            case CINDERVANE -> {
                ForgeDragonAttributesConfig.CINDERVANE_MAX_HEALTH.set(ForgeDragonAttributesConfig.CINDERVANE_MAX_HEALTH.getDefault());
                ForgeDragonAttributesConfig.CINDERVANE_ARMOR.set(ForgeDragonAttributesConfig.CINDERVANE_ARMOR.getDefault());
                ForgeDragonAttributesConfig.CINDERVANE_FLYING_SPEED.set(ForgeDragonAttributesConfig.CINDERVANE_FLYING_SPEED.getDefault());
                ForgeDragonAttributesConfig.CINDERVANE_WILD_FLYING_SPEED_MULTIPLIER.set(ForgeDragonAttributesConfig.CINDERVANE_WILD_FLYING_SPEED_MULTIPLIER.getDefault());
                ForgeDragonAttributesConfig.CINDERVANE_BITE_DAMAGE.set(ForgeDragonAttributesConfig.CINDERVANE_BITE_DAMAGE.getDefault());
                ForgeDragonAttributesConfig.CINDERVANE_MAGMA_VOLLEY_DAMAGE.set(ForgeDragonAttributesConfig.CINDERVANE_MAGMA_VOLLEY_DAMAGE.getDefault());
                ForgeDragonAttributesConfig.CINDERVANE_TAMING_CHANCE_BASE.set(ForgeDragonAttributesConfig.CINDERVANE_TAMING_CHANCE_BASE.getDefault());
                ForgeDragonAttributesConfig.CINDERVANE_TAMING_CHANCE_CHICKEN.set(ForgeDragonAttributesConfig.CINDERVANE_TAMING_CHANCE_CHICKEN.getDefault());
                ForgeDragonAttributesConfig.CINDERVANE_TAMING_CHANCE_HEARTY.set(ForgeDragonAttributesConfig.CINDERVANE_TAMING_CHANCE_HEARTY.getDefault());
                ForgeDragonAttributesConfig.CINDERVANE_EGG_HATCH_CHANCE_NORMAL.set(ForgeDragonAttributesConfig.CINDERVANE_EGG_HATCH_CHANCE_NORMAL.getDefault());
                ForgeDragonAttributesConfig.CINDERVANE_EGG_DROP_CHANCE.set(ForgeDragonAttributesConfig.CINDERVANE_EGG_DROP_CHANCE.getDefault());
                ForgeDragonAttributesConfig.CINDERVANE_FIRE_BODY_EXPLOSION_DAMAGE.set(ForgeDragonAttributesConfig.CINDERVANE_FIRE_BODY_EXPLOSION_DAMAGE.getDefault());
                ForgeDragonAttributesConfig.CINDERVANE_FIRE_BODY_SELF_DAMAGE_ON_CRASH.set(ForgeDragonAttributesConfig.CINDERVANE_FIRE_BODY_SELF_DAMAGE_ON_CRASH.getDefault());
                ForgeDragonAttributesConfig.CINDERVANE_AGGRESSIVE_WILD.set(ForgeDragonAttributesConfig.CINDERVANE_AGGRESSIVE_WILD.getDefault());
                ForgeDragonAttributesConfig.CINDERVANE_REACTIVE_TERRAIN_CLEARING_ON_DAMAGE.set(ForgeDragonAttributesConfig.CINDERVANE_REACTIVE_TERRAIN_CLEARING_ON_DAMAGE.getDefault());
                ForgeDragonAttributesConfig.CINDERVANE_REACTIVE_TERRAIN_CLEARING_ON_DAMAGE_TAMED.set(ForgeDragonAttributesConfig.CINDERVANE_REACTIVE_TERRAIN_CLEARING_ON_DAMAGE_TAMED.getDefault());
            }
            case STEGONAUT -> {
                ForgeDragonAttributesConfig.STEGONAUT_MAX_HEALTH.set(ForgeDragonAttributesConfig.STEGONAUT_MAX_HEALTH.getDefault());
                ForgeDragonAttributesConfig.STEGONAUT_ARMOR.set(ForgeDragonAttributesConfig.STEGONAUT_ARMOR.getDefault());
                ForgeDragonAttributesConfig.STEGONAUT_BITE_DAMAGE.set(ForgeDragonAttributesConfig.STEGONAUT_BITE_DAMAGE.getDefault());
                ForgeDragonAttributesConfig.STEGONAUT_CHIN_SLAM_DAMAGE.set(ForgeDragonAttributesConfig.STEGONAUT_CHIN_SLAM_DAMAGE.getDefault());
                ForgeDragonAttributesConfig.STEGONAUT_GROUND_EATING_DAMAGE.set(ForgeDragonAttributesConfig.STEGONAUT_GROUND_EATING_DAMAGE.getDefault());
                ForgeDragonAttributesConfig.STEGONAUT_TAMING_CHANCE_BASE.set(ForgeDragonAttributesConfig.STEGONAUT_TAMING_CHANCE_BASE.getDefault());
                ForgeDragonAttributesConfig.STEGONAUT_TAMING_CHANCE_HEARTY.set(ForgeDragonAttributesConfig.STEGONAUT_TAMING_CHANCE_HEARTY.getDefault());
                ForgeDragonAttributesConfig.STEGONAUT_EGG_HATCH_CHANCE_NORMAL.set(ForgeDragonAttributesConfig.STEGONAUT_EGG_HATCH_CHANCE_NORMAL.getDefault());
                ForgeDragonAttributesConfig.STEGONAUT_EGG_DROP_CHANCE.set(ForgeDragonAttributesConfig.STEGONAUT_EGG_DROP_CHANCE.getDefault());
                ForgeDragonAttributesConfig.STEGONAUT_REACTIVE_TERRAIN_CLEARING_ON_DAMAGE.set(ForgeDragonAttributesConfig.STEGONAUT_REACTIVE_TERRAIN_CLEARING_ON_DAMAGE.getDefault());
                ForgeDragonAttributesConfig.STEGONAUT_REACTIVE_TERRAIN_CLEARING_ON_DAMAGE_TAMED.set(ForgeDragonAttributesConfig.STEGONAUT_REACTIVE_TERRAIN_CLEARING_ON_DAMAGE_TAMED.getDefault());
            }
            case RAEVYX -> {
                ForgeDragonAttributesConfig.RAEVYX_MAX_HEALTH.set(ForgeDragonAttributesConfig.RAEVYX_MAX_HEALTH.getDefault());
                ForgeDragonAttributesConfig.RAEVYX_ARMOR.set(ForgeDragonAttributesConfig.RAEVYX_ARMOR.getDefault());
                ForgeDragonAttributesConfig.RAEVYX_FLYING_SPEED.set(ForgeDragonAttributesConfig.RAEVYX_FLYING_SPEED.getDefault());
                ForgeDragonAttributesConfig.RAEVYX_WILD_FLYING_SPEED_MULTIPLIER.set(ForgeDragonAttributesConfig.RAEVYX_WILD_FLYING_SPEED_MULTIPLIER.getDefault());
                ForgeDragonAttributesConfig.RAEVYX_BITE_DAMAGE.set(ForgeDragonAttributesConfig.RAEVYX_BITE_DAMAGE.getDefault());
                ForgeDragonAttributesConfig.RAEVYX_LIGHTNING_BEAM_DAMAGE.set(ForgeDragonAttributesConfig.RAEVYX_LIGHTNING_BEAM_DAMAGE.getDefault());
                ForgeDragonAttributesConfig.RAEVYX_HORN_GORE_DAMAGE.set(ForgeDragonAttributesConfig.RAEVYX_HORN_GORE_DAMAGE.getDefault());
                ForgeDragonAttributesConfig.RAEVYX_BEAM_DRAIN_PER_TICK.set(ForgeDragonAttributesConfig.RAEVYX_BEAM_DRAIN_PER_TICK.getDefault());
                ForgeDragonAttributesConfig.RAEVYX_BEAM_REGEN_PER_TICK.set(ForgeDragonAttributesConfig.RAEVYX_BEAM_REGEN_PER_TICK.getDefault());
                ForgeDragonAttributesConfig.RAEVYX_SUMMON_STORM_COOLDOWN_TICKS.set(ForgeDragonAttributesConfig.RAEVYX_SUMMON_STORM_COOLDOWN_TICKS.getDefault());
                ForgeDragonAttributesConfig.RAEVYX_SUMMON_STORM_SUPERCHARGE_TICKS.set(ForgeDragonAttributesConfig.RAEVYX_SUMMON_STORM_SUPERCHARGE_TICKS.getDefault());
                ForgeDragonAttributesConfig.RAEVYX_SUMMON_STORM_SUPERCHARGE_DAMAGE_MULTIPLIER.set(ForgeDragonAttributesConfig.RAEVYX_SUMMON_STORM_SUPERCHARGE_DAMAGE_MULTIPLIER.getDefault());
                ForgeDragonAttributesConfig.RAEVYX_SUMMON_STORM_DURATION_TICKS.set(ForgeDragonAttributesConfig.RAEVYX_SUMMON_STORM_DURATION_TICKS.getDefault());
                ForgeDragonAttributesConfig.RAEVYX_TAMING_CHANCE_BASE.set(ForgeDragonAttributesConfig.RAEVYX_TAMING_CHANCE_BASE.getDefault());
                ForgeDragonAttributesConfig.RAEVYX_TAMING_CHANCE_HEARTY.set(ForgeDragonAttributesConfig.RAEVYX_TAMING_CHANCE_HEARTY.getDefault());
                ForgeDragonAttributesConfig.RAEVYX_LEGACY_TAMING.set(ForgeDragonAttributesConfig.RAEVYX_LEGACY_TAMING.getDefault());
                ForgeDragonAttributesConfig.RAEVYX_EGG_HATCH_CHANCE_NORMAL.set(ForgeDragonAttributesConfig.RAEVYX_EGG_HATCH_CHANCE_NORMAL.getDefault());
                ForgeDragonAttributesConfig.RAEVYX_EGG_HATCH_CHANCE_THUNDER.set(ForgeDragonAttributesConfig.RAEVYX_EGG_HATCH_CHANCE_THUNDER.getDefault());
                ForgeDragonAttributesConfig.RAEVYX_EGG_STORM_INSTANT_CHANCE.set(ForgeDragonAttributesConfig.RAEVYX_EGG_STORM_INSTANT_CHANCE.getDefault());
                ForgeDragonAttributesConfig.RAEVYX_EGG_LOOT_PILLAGER_OUTPOST.set(ForgeDragonAttributesConfig.RAEVYX_EGG_LOOT_PILLAGER_OUTPOST.getDefault());
                ForgeDragonAttributesConfig.RAEVYX_EGG_LOOT_SHIPWRECK_TREASURE.set(ForgeDragonAttributesConfig.RAEVYX_EGG_LOOT_SHIPWRECK_TREASURE.getDefault());
                ForgeDragonAttributesConfig.RAEVYX_EGG_LOOT_ANCIENT_CITY.set(ForgeDragonAttributesConfig.RAEVYX_EGG_LOOT_ANCIENT_CITY.getDefault());
                ForgeDragonAttributesConfig.RAEVYX_EGG_DROP_CHANCE.set(ForgeDragonAttributesConfig.RAEVYX_EGG_DROP_CHANCE.getDefault());
                ForgeDragonAttributesConfig.RAEVYX_AGGRESSIVE_WILD.set(ForgeDragonAttributesConfig.RAEVYX_AGGRESSIVE_WILD.getDefault());
                ForgeDragonAttributesConfig.RAEVYX_REACTIVE_TERRAIN_CLEARING_ON_DAMAGE.set(ForgeDragonAttributesConfig.RAEVYX_REACTIVE_TERRAIN_CLEARING_ON_DAMAGE.getDefault());
                ForgeDragonAttributesConfig.RAEVYX_REACTIVE_TERRAIN_CLEARING_ON_DAMAGE_TAMED.set(ForgeDragonAttributesConfig.RAEVYX_REACTIVE_TERRAIN_CLEARING_ON_DAMAGE_TAMED.getDefault());
            }
            case NULLJAW -> {
                ForgeDragonAttributesConfig.NULLJAW_MAX_HEALTH.set(ForgeDragonAttributesConfig.NULLJAW_MAX_HEALTH.getDefault());
                ForgeDragonAttributesConfig.NULLJAW_ARMOR.set(ForgeDragonAttributesConfig.NULLJAW_ARMOR.getDefault());
                ForgeDragonAttributesConfig.NULLJAW_SWIM_SPEED.set(ForgeDragonAttributesConfig.NULLJAW_SWIM_SPEED.getDefault());
                ForgeDragonAttributesConfig.NULLJAW_BITE_PHASE1_DAMAGE.set(ForgeDragonAttributesConfig.NULLJAW_BITE_PHASE1_DAMAGE.getDefault());
                ForgeDragonAttributesConfig.NULLJAW_BITE_PHASE2_DAMAGE.set(ForgeDragonAttributesConfig.NULLJAW_BITE_PHASE2_DAMAGE.getDefault());
                ForgeDragonAttributesConfig.NULLJAW_HORN_GORE_PHASE1_DAMAGE.set(ForgeDragonAttributesConfig.NULLJAW_HORN_GORE_PHASE1_DAMAGE.getDefault());
                ForgeDragonAttributesConfig.NULLJAW_HORN_GORE_PHASE2_DAMAGE.set(ForgeDragonAttributesConfig.NULLJAW_HORN_GORE_PHASE2_DAMAGE.getDefault());
                ForgeDragonAttributesConfig.NULLJAW_TAMING_CHANCE.set(ForgeDragonAttributesConfig.NULLJAW_TAMING_CHANCE.getDefault());
                ForgeDragonAttributesConfig.NULLJAW_TAMING_CHANCE_TROPICAL.set(ForgeDragonAttributesConfig.NULLJAW_TAMING_CHANCE_TROPICAL.getDefault());
                ForgeDragonAttributesConfig.NULLJAW_LEGACY_TAMING.set(ForgeDragonAttributesConfig.NULLJAW_LEGACY_TAMING.getDefault());
                ForgeDragonAttributesConfig.NULLJAW_EGG_HATCH_CHANCE_NORMAL.set(ForgeDragonAttributesConfig.NULLJAW_EGG_HATCH_CHANCE_NORMAL.getDefault());
                ForgeDragonAttributesConfig.NULLJAW_EGG_DROP_CHANCE.set(ForgeDragonAttributesConfig.NULLJAW_EGG_DROP_CHANCE.getDefault());
                ForgeDragonAttributesConfig.NULLJAW_AGGRESSIVE_WILD.set(ForgeDragonAttributesConfig.NULLJAW_AGGRESSIVE_WILD.getDefault());
                ForgeDragonAttributesConfig.NULLJAW_REACTIVE_TERRAIN_CLEARING_ON_DAMAGE.set(ForgeDragonAttributesConfig.NULLJAW_REACTIVE_TERRAIN_CLEARING_ON_DAMAGE.getDefault());
                ForgeDragonAttributesConfig.NULLJAW_REACTIVE_TERRAIN_CLEARING_ON_DAMAGE_TAMED.set(ForgeDragonAttributesConfig.NULLJAW_REACTIVE_TERRAIN_CLEARING_ON_DAMAGE_TAMED.getDefault());
            }
            case IGNIVORUS -> {
                ForgeDragonAttributesConfig.IGNIVORUS_MAX_HEALTH.set(ForgeDragonAttributesConfig.IGNIVORUS_MAX_HEALTH.getDefault());
                ForgeDragonAttributesConfig.IGNIVORUS_ARMOR.set(ForgeDragonAttributesConfig.IGNIVORUS_ARMOR.getDefault());
                ForgeDragonAttributesConfig.IGNIVORUS_FLYING_SPEED.set(ForgeDragonAttributesConfig.IGNIVORUS_FLYING_SPEED.getDefault());
                ForgeDragonAttributesConfig.IGNIVORUS_WILD_FLYING_SPEED_MULTIPLIER.set(ForgeDragonAttributesConfig.IGNIVORUS_WILD_FLYING_SPEED_MULTIPLIER.getDefault());
                ForgeDragonAttributesConfig.IGNIVORUS_BITE_DAMAGE.set(ForgeDragonAttributesConfig.IGNIVORUS_BITE_DAMAGE.getDefault());
                ForgeDragonAttributesConfig.IGNIVORUS_BODY_SLAM_DAMAGE.set(ForgeDragonAttributesConfig.IGNIVORUS_BODY_SLAM_DAMAGE.getDefault());
                ForgeDragonAttributesConfig.IGNIVORUS_FIRE_BREATH_DAMAGE.set(ForgeDragonAttributesConfig.IGNIVORUS_FIRE_BREATH_DAMAGE.getDefault());
                ForgeDragonAttributesConfig.IGNIVORUS_FIREBALL_DAMAGE.set(ForgeDragonAttributesConfig.IGNIVORUS_FIREBALL_DAMAGE.getDefault());
                ForgeDragonAttributesConfig.IGNIVORUS_WING_SWIPE_DAMAGE.set(ForgeDragonAttributesConfig.IGNIVORUS_WING_SWIPE_DAMAGE.getDefault());
                ForgeDragonAttributesConfig.IGNIVORUS_STOMP_DAMAGE.set(ForgeDragonAttributesConfig.IGNIVORUS_STOMP_DAMAGE.getDefault());
                ForgeDragonAttributesConfig.IGNIVORUS_ULTIMATE_DAMAGE.set(ForgeDragonAttributesConfig.IGNIVORUS_ULTIMATE_DAMAGE.getDefault());
                ForgeDragonAttributesConfig.IGNIVORUS_ULTIMATE_PENALTY_HEALTH.set(ForgeDragonAttributesConfig.IGNIVORUS_ULTIMATE_PENALTY_HEALTH.getDefault());
                ForgeDragonAttributesConfig.IGNIVORUS_FIRE_BREATH_DRAIN_PER_TICK.set(ForgeDragonAttributesConfig.IGNIVORUS_FIRE_BREATH_DRAIN_PER_TICK.getDefault());
                ForgeDragonAttributesConfig.IGNIVORUS_FIRE_BREATH_REGEN_PER_TICK.set(ForgeDragonAttributesConfig.IGNIVORUS_FIRE_BREATH_REGEN_PER_TICK.getDefault());
                ForgeDragonAttributesConfig.IGNIVORUS_FIRE_BREATH_FLAME_SPAWN_MULTIPLIER.set(ForgeDragonAttributesConfig.IGNIVORUS_FIRE_BREATH_FLAME_SPAWN_MULTIPLIER.getDefault());
                ForgeDragonAttributesConfig.IGNIVORUS_FIRE_BREATH_FLAME_SPEED_MULTIPLIER.set(ForgeDragonAttributesConfig.IGNIVORUS_FIRE_BREATH_FLAME_SPEED_MULTIPLIER.getDefault());
                ForgeDragonAttributesConfig.IGNIVORUS_FIRE_BREATH_FLAME_LIFETIME_MULTIPLIER.set(ForgeDragonAttributesConfig.IGNIVORUS_FIRE_BREATH_FLAME_LIFETIME_MULTIPLIER.getDefault());
                ForgeDragonAttributesConfig.IGNIVORUS_FIRE_BREATH_IGNITE_BLOCK_CHANCE.set(ForgeDragonAttributesConfig.IGNIVORUS_FIRE_BREATH_IGNITE_BLOCK_CHANCE.getDefault());
                ForgeDragonAttributesConfig.IGNIVORUS_PHASE2_TOGGLE_ON_CHANCE.set(ForgeDragonAttributesConfig.IGNIVORUS_PHASE2_TOGGLE_ON_CHANCE.getDefault());
                ForgeDragonAttributesConfig.IGNIVORUS_PHASE2_TOGGLE_OFF_CHANCE.set(ForgeDragonAttributesConfig.IGNIVORUS_PHASE2_TOGGLE_OFF_CHANCE.getDefault());
                ForgeDragonAttributesConfig.IGNIVORUS_PHASE2_DECISION_MIN_TICKS.set(ForgeDragonAttributesConfig.IGNIVORUS_PHASE2_DECISION_MIN_TICKS.getDefault());
                ForgeDragonAttributesConfig.IGNIVORUS_PHASE2_DECISION_MAX_TICKS.set(ForgeDragonAttributesConfig.IGNIVORUS_PHASE2_DECISION_MAX_TICKS.getDefault());
                ForgeDragonAttributesConfig.IGNIVORUS_TAMING_CHANCE_BASE.set(ForgeDragonAttributesConfig.IGNIVORUS_TAMING_CHANCE_BASE.getDefault());
                ForgeDragonAttributesConfig.IGNIVORUS_TAMING_CHANCE_BEEF.set(ForgeDragonAttributesConfig.IGNIVORUS_TAMING_CHANCE_BEEF.getDefault());
                ForgeDragonAttributesConfig.IGNIVORUS_TAMING_CHANCE_HEARTY.set(ForgeDragonAttributesConfig.IGNIVORUS_TAMING_CHANCE_HEARTY.getDefault());
                ForgeDragonAttributesConfig.IGNIVORUS_TAMING_STUN_HEALTH.set(ForgeDragonAttributesConfig.IGNIVORUS_TAMING_STUN_HEALTH.getDefault());
                ForgeDragonAttributesConfig.IGNIVORUS_LEGACY_TAMING.set(ForgeDragonAttributesConfig.IGNIVORUS_LEGACY_TAMING.getDefault());
                ForgeDragonAttributesConfig.IGNIVORUS_EGG_HATCH_CHANCE_NORMAL.set(ForgeDragonAttributesConfig.IGNIVORUS_EGG_HATCH_CHANCE_NORMAL.getDefault());
                ForgeDragonAttributesConfig.IGNIVORUS_EGG_LOOT_BASTION_TREASURE.set(ForgeDragonAttributesConfig.IGNIVORUS_EGG_LOOT_BASTION_TREASURE.getDefault());
                ForgeDragonAttributesConfig.IGNIVORUS_EGG_LOOT_NETHER_BRIDGE.set(ForgeDragonAttributesConfig.IGNIVORUS_EGG_LOOT_NETHER_BRIDGE.getDefault());
                ForgeDragonAttributesConfig.IGNIVORUS_EGG_LOOT_ANCIENT_CITY.set(ForgeDragonAttributesConfig.IGNIVORUS_EGG_LOOT_ANCIENT_CITY.getDefault());
                ForgeDragonAttributesConfig.IGNIVORUS_EGG_DROP_CHANCE.set(ForgeDragonAttributesConfig.IGNIVORUS_EGG_DROP_CHANCE.getDefault());
                ForgeDragonAttributesConfig.IGNIVORUS_AGGRESSIVE_WILD.set(ForgeDragonAttributesConfig.IGNIVORUS_AGGRESSIVE_WILD.getDefault());
                ForgeDragonAttributesConfig.IGNIVORUS_REACTIVE_TERRAIN_CLEARING_ON_DAMAGE.set(ForgeDragonAttributesConfig.IGNIVORUS_REACTIVE_TERRAIN_CLEARING_ON_DAMAGE.getDefault());
                ForgeDragonAttributesConfig.IGNIVORUS_REACTIVE_TERRAIN_CLEARING_ON_DAMAGE_TAMED.set(ForgeDragonAttributesConfig.IGNIVORUS_REACTIVE_TERRAIN_CLEARING_ON_DAMAGE_TAMED.getDefault());
            }
        }

        ForgeDragonAttributesConfig.ATTRIBUTES_SPEC.save();
        DragonAttributeConfigLoader.getInstance().refreshFromForgeConfig();
        applyAttributesToLoadedDragons();
    }
}

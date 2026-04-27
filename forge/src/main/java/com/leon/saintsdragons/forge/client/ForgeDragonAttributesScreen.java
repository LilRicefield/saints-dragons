package com.leon.saintsdragons.forge.client;

import com.leon.saintsdragons.common.config.SaintsDragonsConfig;
import com.leon.saintsdragons.common.config.dragon.DragonAttributeConfigLoader;
import com.leon.saintsdragons.forge.platform.ForgeDragonAttributesConfig;
import com.leon.saintsdragons.server.entity.base.DragonEntity;
import com.leon.saintsdragons.server.entity.dragons.cindervane.Cindervane;
import com.leon.saintsdragons.server.entity.dragons.ignivorus.Ignivorus;
import com.leon.saintsdragons.server.entity.dragons.nulljaw.Nulljaw;
import com.leon.saintsdragons.server.entity.dragons.varasuchus.Varasuchus;
import com.leon.saintsdragons.server.entity.dragons.raevyx.Raevyx;
import com.leon.saintsdragons.server.entity.dragons.stegonaut.Stegonaut;
import com.leon.saintsdragons.server.entity.dragons.volitans.Volitans;
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
        VARASUCHUS,
        IGNIVORUS,
        VOLITANS,
        NULLJAW
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
            case VARASUCHUS -> addVarasuchusEntries(entries);
            case IGNIVORUS -> addIgnivorusEntries(entries);
            case VOLITANS -> addVolitansEntries(entries);
            case NULLJAW -> addNulljawEntries(entries);
        }
    }

    @Override
    protected void addHeaderButtons() {
        int buttonWidth = Math.min(90, (width - 84) / 7);
        int spacing = 6;
        int totalWidth = buttonWidth * 7 + spacing * 6;
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

        addRenderableWidget(net.minecraft.client.gui.components.Button.builder(Component.translatable("config.saintsdragons.attributes.varasuchus"), button -> {
            if (section != Section.VARASUCHUS) {
                section = Section.VARASUCHUS;
                rebuildWidgets();
            }
        }).bounds(startX + (buttonWidth + spacing) * 3, y, buttonWidth, 20).build());

        addRenderableWidget(net.minecraft.client.gui.components.Button.builder(Component.translatable("config.saintsdragons.attributes.ignivorus"), button -> {
            if (section != Section.IGNIVORUS) {
                section = Section.IGNIVORUS;
                rebuildWidgets();
            }
        }).bounds(startX + (buttonWidth + spacing) * 4, y, buttonWidth, 20).build());

        addRenderableWidget(net.minecraft.client.gui.components.Button.builder(Component.translatable("config.saintsdragons.attributes.volitans"), button -> {
            if (section != Section.VOLITANS) {
                section = Section.VOLITANS;
                rebuildWidgets();
            }
        }).bounds(startX + (buttonWidth + spacing) * 5, y, buttonWidth, 20).build());

        addRenderableWidget(net.minecraft.client.gui.components.Button.builder(Component.translatable("config.saintsdragons.attributes.nulljaw"), button -> {
            if (section != Section.NULLJAW) {
                section = Section.NULLJAW;
                rebuildWidgets();
            }
        }).bounds(startX + (buttonWidth + spacing) * 6, y, buttonWidth, 20).build());

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
                } else if (dragon instanceof Varasuchus varasuchus) {
                    varasuchus.applyConfiguredAttributes();
                } else if (dragon instanceof Ignivorus ignivorus) {
                    ignivorus.applyConfiguredAttributes();
                } else if (dragon instanceof Volitans volitans) {
                    volitans.applyConfiguredAttributes();
                } else if (dragon instanceof Nulljaw nulljaw) {
                    nulljaw.applyConfiguredAttributes();
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
        entries.add(new DoubleEntry(Component.translatable("config.saintsdragons.attributes.cindervane.slash_grab_hit1_damage"),
                ForgeDragonAttributesConfig.CINDERVANE_SLASH_GRAB_HIT1_DAMAGE::get,
                ForgeDragonAttributesConfig.CINDERVANE_SLASH_GRAB_HIT1_DAMAGE::set,
                null));
        entries.add(new DoubleEntry(Component.translatable("config.saintsdragons.attributes.cindervane.slash_grab_hit2_damage"),
                ForgeDragonAttributesConfig.CINDERVANE_SLASH_GRAB_HIT2_DAMAGE::get,
                ForgeDragonAttributesConfig.CINDERVANE_SLASH_GRAB_HIT2_DAMAGE::set,
                null));
        entries.add(new DoubleEntry(Component.translatable("config.saintsdragons.attributes.cindervane.volley_damage"),
                ForgeDragonAttributesConfig.CINDERVANE_MAGMA_VOLLEY_DAMAGE::get,
                ForgeDragonAttributesConfig.CINDERVANE_MAGMA_VOLLEY_DAMAGE::set,
                null));
        entries.add(new DoubleEntry(Component.translatable("config.saintsdragons.attributes.cindervane.fire_body_damage"),
                ForgeDragonAttributesConfig.CINDERVANE_FIRE_BODY_DAMAGE::get,
                ForgeDragonAttributesConfig.CINDERVANE_FIRE_BODY_DAMAGE::set,
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
        entries.add(new DoubleEntry(Component.translatable("config.saintsdragons.attributes.cindervane.egg_hatch_time_ticks_normal"),
                ForgeDragonAttributesConfig.CINDERVANE_EGG_HATCH_CHANCE_NORMAL::get,
                ForgeDragonAttributesConfig.CINDERVANE_EGG_HATCH_CHANCE_NORMAL::set,
                null));
        entries.add(new PercentDoubleEntry(Component.translatable("config.saintsdragons.attributes.cindervane.egg_drop_chance"),
                ForgeDragonAttributesConfig.CINDERVANE_EGG_DROP_CHANCE::get,
                ForgeDragonAttributesConfig.CINDERVANE_EGG_DROP_CHANCE::set,
                null));
        entries.add(new PercentDoubleEntry(Component.translatable("config.saintsdragons.attributes.cindervane.scale_drop_chance_brush"),
                ForgeDragonAttributesConfig.CINDERVANE_SCALE_DROP_CHANCE_BRUSH::get,
                ForgeDragonAttributesConfig.CINDERVANE_SCALE_DROP_CHANCE_BRUSH::set,
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
        entries.add(new DoubleEntry(Component.translatable("config.saintsdragons.attributes.stegonaut.egg_hatch_time_ticks_normal"),
                ForgeDragonAttributesConfig.STEGONAUT_EGG_HATCH_CHANCE_NORMAL::get,
                ForgeDragonAttributesConfig.STEGONAUT_EGG_HATCH_CHANCE_NORMAL::set,
                null));
        entries.add(new PercentDoubleEntry(Component.translatable("config.saintsdragons.attributes.stegonaut.egg_drop_chance"),
                ForgeDragonAttributesConfig.STEGONAUT_EGG_DROP_CHANCE::get,
                ForgeDragonAttributesConfig.STEGONAUT_EGG_DROP_CHANCE::set,
                null));
        entries.add(new PercentDoubleEntry(Component.translatable("config.saintsdragons.attributes.stegonaut.scale_drop_chance_brush"),
                ForgeDragonAttributesConfig.STEGONAUT_SCALE_DROP_CHANCE_BRUSH::get,
                ForgeDragonAttributesConfig.STEGONAUT_SCALE_DROP_CHANCE_BRUSH::set,
                null));
        entries.add(new BooleanEntry(
                Component.translatable("saintsdragons.config_screen.others.stegonaut_buffs"),
                SaintsDragonsConfig.STEGONAUT_BUFFS_ENABLED::get,
                SaintsDragonsConfig.STEGONAUT_BUFFS_ENABLED::set,
                SaintsDragonsConfig.STEGONAUT_BUFFS_ENABLED::save
        ));
        entries.add(new BooleanEntry(Component.translatable("config.saintsdragons.attributes.stegonaut.aggressive_wild"),
                () -> ForgeDragonAttributesConfig.STEGONAUT_AGGRESSIVE_WILD.get(),
                ForgeDragonAttributesConfig.STEGONAUT_AGGRESSIVE_WILD::set,
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
        entries.add(new DoubleEntry(Component.translatable("config.saintsdragons.attributes.raevyx.dash_damage"),
                ForgeDragonAttributesConfig.RAEVYX_DASH_DAMAGE::get,
                ForgeDragonAttributesConfig.RAEVYX_DASH_DAMAGE::set,
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
        entries.add(new DoubleEntry(Component.translatable("config.saintsdragons.attributes.raevyx.egg_hatch_time_ticks_normal"),
                ForgeDragonAttributesConfig.RAEVYX_EGG_HATCH_TIME_TICKS_NORMAL::get,
                ForgeDragonAttributesConfig.RAEVYX_EGG_HATCH_TIME_TICKS_NORMAL::set,
                null));
        entries.add(new DoubleEntry(Component.translatable("config.saintsdragons.attributes.raevyx.egg_hatch_time_ticks_thunder"),
                ForgeDragonAttributesConfig.RAEVYX_EGG_HATCH_TIME_TICKS_THUNDER::get,
                ForgeDragonAttributesConfig.RAEVYX_EGG_HATCH_TIME_TICKS_THUNDER::set,
                null));
        entries.add(new PercentDoubleEntry(Component.translatable("config.saintsdragons.attributes.raevyx.egg_loot_pillager_outpost"),
                ForgeDragonAttributesConfig.RAEVYX_EGG_LOOT_PILLAGER_OUTPOST::get,
                ForgeDragonAttributesConfig.RAEVYX_EGG_LOOT_PILLAGER_OUTPOST::set,
                null));
        entries.add(new PercentDoubleEntry(Component.translatable("config.saintsdragons.attributes.raevyx.egg_loot_ancient_city"),
                ForgeDragonAttributesConfig.RAEVYX_EGG_LOOT_ANCIENT_CITY::get,
                ForgeDragonAttributesConfig.RAEVYX_EGG_LOOT_ANCIENT_CITY::set,
                null));
        entries.add(new PercentDoubleEntry(Component.translatable("config.saintsdragons.attributes.raevyx.egg_drop_chance"),
                ForgeDragonAttributesConfig.RAEVYX_EGG_DROP_CHANCE::get,
                ForgeDragonAttributesConfig.RAEVYX_EGG_DROP_CHANCE::set,
                null));
        entries.add(new PercentDoubleEntry(Component.translatable("config.saintsdragons.attributes.raevyx.scale_drop_chance_brush"),
                ForgeDragonAttributesConfig.RAEVYX_SCALE_DROP_CHANCE_BRUSH::get,
                ForgeDragonAttributesConfig.RAEVYX_SCALE_DROP_CHANCE_BRUSH::set,
                null));
        entries.add(new BooleanEntry(Component.translatable("config.saintsdragons.attributes.raevyx.legacy_taming"),
                () -> ForgeDragonAttributesConfig.RAEVYX_LEGACY_TAMING.get(),
                ForgeDragonAttributesConfig.RAEVYX_LEGACY_TAMING::set,
                null));
        entries.add(new BooleanEntry(Component.translatable("config.saintsdragons.attributes.raevyx.aggressive_wild"),
                () -> ForgeDragonAttributesConfig.RAEVYX_AGGRESSIVE_WILD.get(),
                ForgeDragonAttributesConfig.RAEVYX_AGGRESSIVE_WILD::set,
                null));
    }

    private void addVarasuchusEntries(List<ConfigEntry> entries) {
        entries.add(new SectionEntry(Component.translatable("config.saintsdragons.attributes.varasuchus")));
        entries.add(new DoubleEntry(Component.translatable("config.saintsdragons.attributes.varasuchus.max_health"),
                ForgeDragonAttributesConfig.VARASUCHUS_MAX_HEALTH::get,
                ForgeDragonAttributesConfig.VARASUCHUS_MAX_HEALTH::set,
                null));
        entries.add(new DoubleEntry(Component.translatable("config.saintsdragons.attributes.varasuchus.armor"),
                ForgeDragonAttributesConfig.VARASUCHUS_ARMOR::get,
                ForgeDragonAttributesConfig.VARASUCHUS_ARMOR::set,
                null));
        entries.add(new DoubleEntry(Component.translatable("config.saintsdragons.attributes.varasuchus.swim_speed"),
                ForgeDragonAttributesConfig.VARASUCHUS_SWIM_SPEED::get,
                ForgeDragonAttributesConfig.VARASUCHUS_SWIM_SPEED::set,
                null));
        entries.add(new DoubleEntry(Component.translatable("config.saintsdragons.attributes.varasuchus.bite_phase1"),
                ForgeDragonAttributesConfig.VARASUCHUS_BITE_PHASE1_DAMAGE::get,
                ForgeDragonAttributesConfig.VARASUCHUS_BITE_PHASE1_DAMAGE::set,
                null));
        entries.add(new DoubleEntry(Component.translatable("config.saintsdragons.attributes.varasuchus.bite_phase2"),
                ForgeDragonAttributesConfig.VARASUCHUS_BITE_PHASE2_DAMAGE::get,
                ForgeDragonAttributesConfig.VARASUCHUS_BITE_PHASE2_DAMAGE::set,
                null));
        entries.add(new DoubleEntry(Component.translatable("config.saintsdragons.attributes.varasuchus.tail_attack"),
                ForgeDragonAttributesConfig.VARASUCHUS_TAIL_ATTACK_DAMAGE::get,
                ForgeDragonAttributesConfig.VARASUCHUS_TAIL_ATTACK_DAMAGE::set,
                null));
        entries.add(new DoubleEntry(Component.translatable("config.saintsdragons.attributes.varasuchus.dash_tail_swipe"),
                ForgeDragonAttributesConfig.VARASUCHUS_DASH_TAIL_SWIPE_DAMAGE::get,
                ForgeDragonAttributesConfig.VARASUCHUS_DASH_TAIL_SWIPE_DAMAGE::set,
                null));
        entries.add(new DoubleEntry(Component.translatable("config.saintsdragons.attributes.varasuchus.dash_claw"),
                ForgeDragonAttributesConfig.VARASUCHUS_DASH_CLAW_DAMAGE::get,
                ForgeDragonAttributesConfig.VARASUCHUS_DASH_CLAW_DAMAGE::set,
                null));
        entries.add(new DoubleEntry(Component.translatable("config.saintsdragons.attributes.varasuchus.claw_attack"),
                ForgeDragonAttributesConfig.VARASUCHUS_CLAW_ATTACK_DAMAGE::get,
                ForgeDragonAttributesConfig.VARASUCHUS_CLAW_ATTACK_DAMAGE::set,
                null));
        entries.add(new DoubleEntry(Component.translatable("config.saintsdragons.attributes.varasuchus.horn_phase1"),
                ForgeDragonAttributesConfig.VARASUCHUS_HORN_GORE_PHASE1_DAMAGE::get,
                ForgeDragonAttributesConfig.VARASUCHUS_HORN_GORE_PHASE1_DAMAGE::set,
                null));
        entries.add(new DoubleEntry(Component.translatable("config.saintsdragons.attributes.varasuchus.horn_phase2"),
                ForgeDragonAttributesConfig.VARASUCHUS_HORN_GORE_PHASE2_DAMAGE::get,
                ForgeDragonAttributesConfig.VARASUCHUS_HORN_GORE_PHASE2_DAMAGE::set,
                null));
        entries.add(new DoubleEntry(Component.translatable("config.saintsdragons.attributes.varasuchus.taming_chance"),
                ForgeDragonAttributesConfig.VARASUCHUS_TAMING_CHANCE::get,
                ForgeDragonAttributesConfig.VARASUCHUS_TAMING_CHANCE::set,
                null));
        entries.add(new DoubleEntry(Component.translatable("config.saintsdragons.attributes.varasuchus.taming_tropical"),
                ForgeDragonAttributesConfig.VARASUCHUS_TAMING_CHANCE_TROPICAL::get,
                ForgeDragonAttributesConfig.VARASUCHUS_TAMING_CHANCE_TROPICAL::set,
                null));
        entries.add(new DoubleEntry(Component.translatable("config.saintsdragons.attributes.varasuchus.egg_hatch_time_ticks_normal"),
                ForgeDragonAttributesConfig.VARASUCHUS_EGG_HATCH_CHANCE_NORMAL::get,
                ForgeDragonAttributesConfig.VARASUCHUS_EGG_HATCH_CHANCE_NORMAL::set,
                null));
        entries.add(new PercentDoubleEntry(Component.translatable("config.saintsdragons.attributes.varasuchus.egg_drop_chance"),
                ForgeDragonAttributesConfig.VARASUCHUS_EGG_DROP_CHANCE::get,
                ForgeDragonAttributesConfig.VARASUCHUS_EGG_DROP_CHANCE::set,
                null));
        entries.add(new PercentDoubleEntry(Component.translatable("config.saintsdragons.attributes.varasuchus.scale_drop_chance_brush"),
                ForgeDragonAttributesConfig.VARASUCHUS_SCALE_DROP_CHANCE_BRUSH::get,
                ForgeDragonAttributesConfig.VARASUCHUS_SCALE_DROP_CHANCE_BRUSH::set,
                null));
        entries.add(new BooleanEntry(Component.translatable("config.saintsdragons.attributes.varasuchus.legacy_taming"),
                () -> ForgeDragonAttributesConfig.VARASUCHUS_LEGACY_TAMING.get(),
                ForgeDragonAttributesConfig.VARASUCHUS_LEGACY_TAMING::set,
                null));
        entries.add(new BooleanEntry(Component.translatable("config.saintsdragons.attributes.varasuchus.aggressive_wild"),
                () -> ForgeDragonAttributesConfig.VARASUCHUS_AGGRESSIVE_WILD.get(),
                ForgeDragonAttributesConfig.VARASUCHUS_AGGRESSIVE_WILD::set,
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
        entries.add(new DoubleEntry(Component.translatable("config.saintsdragons.attributes.ignivorus.leap_slam_damage"),
                ForgeDragonAttributesConfig.IGNIVORUS_LEAP_SLAM_DAMAGE::get,
                ForgeDragonAttributesConfig.IGNIVORUS_LEAP_SLAM_DAMAGE::set,
                null));
        entries.add(new DoubleEntry(Component.translatable("config.saintsdragons.attributes.ignivorus.fire_breath_damage"),
                ForgeDragonAttributesConfig.IGNIVORUS_FIRE_BREATH_DAMAGE::get,
                ForgeDragonAttributesConfig.IGNIVORUS_FIRE_BREATH_DAMAGE::set,
                null));
        entries.add(new DoubleEntry(Component.translatable("config.saintsdragons.attributes.ignivorus.fireball_damage"),
                ForgeDragonAttributesConfig.IGNIVORUS_FIREBALL_DAMAGE::get,
                ForgeDragonAttributesConfig.IGNIVORUS_FIREBALL_DAMAGE::set,
                null));
        entries.add(new DoubleEntry(Component.translatable("config.saintsdragons.attributes.ignivorus.magma_pillar_damage"),
                ForgeDragonAttributesConfig.IGNIVORUS_MAGMA_PILLAR_DAMAGE::get,
                ForgeDragonAttributesConfig.IGNIVORUS_MAGMA_PILLAR_DAMAGE::set,
                null));
        entries.add(new DoubleEntry(Component.translatable("config.saintsdragons.attributes.ignivorus.wing_swipe_damage"),
                ForgeDragonAttributesConfig.IGNIVORUS_WING_SWIPE_DAMAGE::get,
                ForgeDragonAttributesConfig.IGNIVORUS_WING_SWIPE_DAMAGE::set,
                null));
        entries.add(new DoubleEntry(Component.translatable("config.saintsdragons.attributes.ignivorus.stomp_damage"),
                ForgeDragonAttributesConfig.IGNIVORUS_STOMP_DAMAGE::get,
                ForgeDragonAttributesConfig.IGNIVORUS_STOMP_DAMAGE::set,
                null));
        entries.add(new DoubleEntry(Component.translatable("config.saintsdragons.attributes.ignivorus.bulldoze_damage"),
                ForgeDragonAttributesConfig.IGNIVORUS_BULLDOZE_DAMAGE::get,
                ForgeDragonAttributesConfig.IGNIVORUS_BULLDOZE_DAMAGE::set,
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
        entries.add(new PercentDoubleEntry(Component.translatable("config.saintsdragons.attributes.ignivorus.fire_breath_ignite_block_chance"),
                ForgeDragonAttributesConfig.IGNIVORUS_FIRE_BREATH_IGNITE_BLOCK_CHANCE::get,
                ForgeDragonAttributesConfig.IGNIVORUS_FIRE_BREATH_IGNITE_BLOCK_CHANCE::set,
                null));
        entries.add(new PercentDoubleEntry(Component.translatable("config.saintsdragons.attributes.ignivorus.phase2_toggle_on_chance"),
                ForgeDragonAttributesConfig.IGNIVORUS_PHASE2_TOGGLE_ON_CHANCE::get,
                ForgeDragonAttributesConfig.IGNIVORUS_PHASE2_TOGGLE_ON_CHANCE::set,
                null));
        entries.add(new PercentDoubleEntry(Component.translatable("config.saintsdragons.attributes.ignivorus.phase2_toggle_off_chance"),
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
        entries.add(new DoubleEntry(Component.translatable("config.saintsdragons.attributes.ignivorus.egg_hatch_time_ticks_normal"),
                ForgeDragonAttributesConfig.IGNIVORUS_EGG_HATCH_CHANCE_NORMAL::get,
                ForgeDragonAttributesConfig.IGNIVORUS_EGG_HATCH_CHANCE_NORMAL::set,
                null));
        entries.add(new PercentDoubleEntry(Component.translatable("config.saintsdragons.attributes.ignivorus.egg_loot_bastion_treasure"),
                ForgeDragonAttributesConfig.IGNIVORUS_EGG_LOOT_BASTION_TREASURE::get,
                ForgeDragonAttributesConfig.IGNIVORUS_EGG_LOOT_BASTION_TREASURE::set,
                null));
        entries.add(new PercentDoubleEntry(Component.translatable("config.saintsdragons.attributes.ignivorus.egg_loot_nether_bridge"),
                ForgeDragonAttributesConfig.IGNIVORUS_EGG_LOOT_NETHER_BRIDGE::get,
                ForgeDragonAttributesConfig.IGNIVORUS_EGG_LOOT_NETHER_BRIDGE::set,
                null));
        entries.add(new PercentDoubleEntry(Component.translatable("config.saintsdragons.attributes.ignivorus.egg_loot_ancient_city"),
                ForgeDragonAttributesConfig.IGNIVORUS_EGG_LOOT_ANCIENT_CITY::get,
                ForgeDragonAttributesConfig.IGNIVORUS_EGG_LOOT_ANCIENT_CITY::set,
                null));
        entries.add(new PercentDoubleEntry(Component.translatable("config.saintsdragons.attributes.ignivorus.egg_drop_chance"),
                ForgeDragonAttributesConfig.IGNIVORUS_EGG_DROP_CHANCE::get,
                ForgeDragonAttributesConfig.IGNIVORUS_EGG_DROP_CHANCE::set,
                null));
        entries.add(new PercentDoubleEntry(Component.translatable("config.saintsdragons.attributes.ignivorus.scale_drop_chance_brush"),
                ForgeDragonAttributesConfig.IGNIVORUS_SCALE_DROP_CHANCE_BRUSH::get,
                ForgeDragonAttributesConfig.IGNIVORUS_SCALE_DROP_CHANCE_BRUSH::set,
                null));
        entries.add(new BooleanEntry(Component.translatable("config.saintsdragons.attributes.ignivorus.legacy_taming"),
                () -> ForgeDragonAttributesConfig.IGNIVORUS_LEGACY_TAMING.get(),
                ForgeDragonAttributesConfig.IGNIVORUS_LEGACY_TAMING::set,
                null));
        entries.add(new BooleanEntry(Component.translatable("config.saintsdragons.attributes.ignivorus.aggressive_wild"),
                () -> ForgeDragonAttributesConfig.IGNIVORUS_AGGRESSIVE_WILD.get(),
                ForgeDragonAttributesConfig.IGNIVORUS_AGGRESSIVE_WILD::set,
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
    }

    private void addVolitansEntries(List<ConfigEntry> entries) {
        entries.add(new SectionEntry(Component.translatable("config.saintsdragons.attributes.volitans")));
        entries.add(new DoubleEntry(Component.translatable("config.saintsdragons.attributes.volitans.max_health"),
                ForgeDragonAttributesConfig.VOLITANS_MAX_HEALTH::get,
                ForgeDragonAttributesConfig.VOLITANS_MAX_HEALTH::set,
                null));
        entries.add(new DoubleEntry(Component.translatable("config.saintsdragons.attributes.volitans.armor"),
                ForgeDragonAttributesConfig.VOLITANS_ARMOR::get,
                ForgeDragonAttributesConfig.VOLITANS_ARMOR::set,
                null));
        entries.add(new DoubleEntry(Component.translatable("config.saintsdragons.attributes.volitans.rider_flying_speed"),
                ForgeDragonAttributesConfig.VOLITANS_FLYING_SPEED::get,
                ForgeDragonAttributesConfig.VOLITANS_FLYING_SPEED::set,
                null));
        entries.add(new DoubleEntry(Component.translatable("config.saintsdragons.attributes.volitans.wild_flying_speed_multiplier"),
                ForgeDragonAttributesConfig.VOLITANS_WILD_FLYING_SPEED_MULTIPLIER::get,
                ForgeDragonAttributesConfig.VOLITANS_WILD_FLYING_SPEED_MULTIPLIER::set,
                null));
        entries.add(new DoubleEntry(Component.translatable("config.saintsdragons.attributes.volitans.bite_damage"),
                ForgeDragonAttributesConfig.VOLITANS_BITE_DAMAGE::get,
                ForgeDragonAttributesConfig.VOLITANS_BITE_DAMAGE::set,
                null));
        entries.add(new DoubleEntry(Component.translatable("config.saintsdragons.attributes.volitans.claw_damage"),
                ForgeDragonAttributesConfig.VOLITANS_CLAW_DAMAGE::get,
                ForgeDragonAttributesConfig.VOLITANS_CLAW_DAMAGE::set,
                null));
        entries.add(new DoubleEntry(Component.translatable("config.saintsdragons.attributes.volitans.horn_gore_damage"),
                ForgeDragonAttributesConfig.VOLITANS_HORN_GORE_DAMAGE::get,
                ForgeDragonAttributesConfig.VOLITANS_HORN_GORE_DAMAGE::set,
                null));
        entries.add(new DoubleEntry(Component.translatable("config.saintsdragons.attributes.volitans.roar_ground_damage"),
                ForgeDragonAttributesConfig.VOLITANS_ROAR_GROUND_DAMAGE::get,
                ForgeDragonAttributesConfig.VOLITANS_ROAR_GROUND_DAMAGE::set,
                null));
        entries.add(new DoubleEntry(Component.translatable("config.saintsdragons.attributes.volitans.roar_air_water_damage"),
                ForgeDragonAttributesConfig.VOLITANS_ROAR_AIR_WATER_DAMAGE::get,
                ForgeDragonAttributesConfig.VOLITANS_ROAR_AIR_WATER_DAMAGE::set,
                null));
        entries.add(new DoubleEntry(Component.translatable("config.saintsdragons.attributes.volitans.burrow_damage"),
                ForgeDragonAttributesConfig.VOLITANS_BURROW_DAMAGE::get,
                ForgeDragonAttributesConfig.VOLITANS_BURROW_DAMAGE::set,
                null));
        entries.add(new DoubleEntry(Component.translatable("config.saintsdragons.attributes.volitans.poison_ball_damage"),
                ForgeDragonAttributesConfig.VOLITANS_POISON_BALL_DAMAGE::get,
                ForgeDragonAttributesConfig.VOLITANS_POISON_BALL_DAMAGE::set,
                null));
        entries.add(new DoubleEntry(Component.translatable("config.saintsdragons.attributes.volitans.water_breath_damage"),
                ForgeDragonAttributesConfig.VOLITANS_WATER_BREATH_DAMAGE::get,
                ForgeDragonAttributesConfig.VOLITANS_WATER_BREATH_DAMAGE::set,
                null));
        entries.add(new DoubleEntry(Component.translatable("config.saintsdragons.attributes.volitans.poison_breath_damage"),
                ForgeDragonAttributesConfig.VOLITANS_POISON_BREATH_DAMAGE::get,
                ForgeDragonAttributesConfig.VOLITANS_POISON_BREATH_DAMAGE::set,
                null));
        entries.add(new DoubleEntry(Component.translatable("config.saintsdragons.attributes.volitans.taming_chance_base"),
                ForgeDragonAttributesConfig.VOLITANS_TAMING_CHANCE_BASE::get,
                ForgeDragonAttributesConfig.VOLITANS_TAMING_CHANCE_BASE::set,
                null));
        entries.add(new DoubleEntry(Component.translatable("config.saintsdragons.attributes.volitans.taming_chance_hearty"),
                ForgeDragonAttributesConfig.VOLITANS_TAMING_CHANCE_HEARTY::get,
                ForgeDragonAttributesConfig.VOLITANS_TAMING_CHANCE_HEARTY::set,
                null));
        entries.add(new DoubleEntry(Component.translatable("config.saintsdragons.attributes.volitans.taming_stun_health"),
                ForgeDragonAttributesConfig.VOLITANS_TAMING_STUN_HEALTH::get,
                ForgeDragonAttributesConfig.VOLITANS_TAMING_STUN_HEALTH::set,
                null));
        entries.add(new DoubleEntry(Component.translatable("config.saintsdragons.attributes.volitans.breath_active_ticks_max"),
                ForgeDragonAttributesConfig.VOLITANS_BREATH_ACTIVE_TICKS_MAX::get,
                ForgeDragonAttributesConfig.VOLITANS_BREATH_ACTIVE_TICKS_MAX::set,
                null));
        entries.add(new DoubleEntry(Component.translatable("config.saintsdragons.attributes.volitans.breath_drain_per_tick"),
                ForgeDragonAttributesConfig.VOLITANS_BREATH_DRAIN_PER_TICK::get,
                ForgeDragonAttributesConfig.VOLITANS_BREATH_DRAIN_PER_TICK::set,
                null));
        entries.add(new DoubleEntry(Component.translatable("config.saintsdragons.attributes.volitans.breath_regen_per_tick"),
                ForgeDragonAttributesConfig.VOLITANS_BREATH_REGEN_PER_TICK::get,
                ForgeDragonAttributesConfig.VOLITANS_BREATH_REGEN_PER_TICK::set,
                null));
        entries.add(new DoubleEntry(Component.translatable("config.saintsdragons.attributes.volitans.breath_projectile_spread"),
                ForgeDragonAttributesConfig.VOLITANS_BREATH_PROJECTILE_SPREAD::get,
                ForgeDragonAttributesConfig.VOLITANS_BREATH_PROJECTILE_SPREAD::set,
                null));
        entries.add(new DoubleEntry(Component.translatable("config.saintsdragons.attributes.volitans.breath_projectile_speed"),
                ForgeDragonAttributesConfig.VOLITANS_BREATH_PROJECTILE_SPEED::get,
                ForgeDragonAttributesConfig.VOLITANS_BREATH_PROJECTILE_SPEED::set,
                null));
        entries.add(new DoubleEntry(Component.translatable("config.saintsdragons.attributes.volitans.breath_projectile_lifetime"),
                ForgeDragonAttributesConfig.VOLITANS_BREATH_PROJECTILE_LIFETIME::get,
                ForgeDragonAttributesConfig.VOLITANS_BREATH_PROJECTILE_LIFETIME::set,
                null));
        entries.add(new DoubleEntry(Component.translatable("config.saintsdragons.attributes.volitans.poison_breath_poison_duration_ticks"),
                ForgeDragonAttributesConfig.VOLITANS_POISON_BREATH_POISON_DURATION_TICKS::get,
                ForgeDragonAttributesConfig.VOLITANS_POISON_BREATH_POISON_DURATION_TICKS::set,
                null));
        entries.add(new DoubleEntry(Component.translatable("config.saintsdragons.attributes.volitans.poison_breath_poison_level"),
                ForgeDragonAttributesConfig.VOLITANS_POISON_BREATH_POISON_LEVEL::get,
                ForgeDragonAttributesConfig.VOLITANS_POISON_BREATH_POISON_LEVEL::set,
                null));
        entries.add(new DoubleEntry(Component.translatable("config.saintsdragons.attributes.volitans.poison_ball_poison_duration_ticks"),
                ForgeDragonAttributesConfig.VOLITANS_POISON_BALL_POISON_DURATION_TICKS::get,
                ForgeDragonAttributesConfig.VOLITANS_POISON_BALL_POISON_DURATION_TICKS::set,
                null));
        entries.add(new DoubleEntry(Component.translatable("config.saintsdragons.attributes.volitans.poison_ball_poison_level"),
                ForgeDragonAttributesConfig.VOLITANS_POISON_BALL_POISON_LEVEL::get,
                ForgeDragonAttributesConfig.VOLITANS_POISON_BALL_POISON_LEVEL::set,
                null));
        entries.add(new DoubleEntry(Component.translatable("config.saintsdragons.attributes.volitans.roar_ground_poison_duration_ticks"),
                ForgeDragonAttributesConfig.VOLITANS_ROAR_GROUND_POISON_DURATION_TICKS::get,
                ForgeDragonAttributesConfig.VOLITANS_ROAR_GROUND_POISON_DURATION_TICKS::set,
                null));
        entries.add(new DoubleEntry(Component.translatable("config.saintsdragons.attributes.volitans.roar_ground_poison_level"),
                ForgeDragonAttributesConfig.VOLITANS_ROAR_GROUND_POISON_LEVEL::get,
                ForgeDragonAttributesConfig.VOLITANS_ROAR_GROUND_POISON_LEVEL::set,
                null));
        entries.add(new DoubleEntry(Component.translatable("config.saintsdragons.attributes.volitans.roar_air_water_poison_duration_ticks"),
                ForgeDragonAttributesConfig.VOLITANS_ROAR_AIR_WATER_POISON_DURATION_TICKS::get,
                ForgeDragonAttributesConfig.VOLITANS_ROAR_AIR_WATER_POISON_DURATION_TICKS::set,
                null));
        entries.add(new DoubleEntry(Component.translatable("config.saintsdragons.attributes.volitans.roar_air_water_poison_level"),
                ForgeDragonAttributesConfig.VOLITANS_ROAR_AIR_WATER_POISON_LEVEL::get,
                ForgeDragonAttributesConfig.VOLITANS_ROAR_AIR_WATER_POISON_LEVEL::set,
                null));
        entries.add(new PercentDoubleEntry(Component.translatable("config.saintsdragons.attributes.volitans.scale_drop_chance_brush"),
                ForgeDragonAttributesConfig.VOLITANS_SCALE_DROP_CHANCE_BRUSH::get,
                ForgeDragonAttributesConfig.VOLITANS_SCALE_DROP_CHANCE_BRUSH::set,
                null));
        entries.add(new PercentDoubleEntry(Component.translatable("config.saintsdragons.attributes.volitans.spine_drop_chance"),
                ForgeDragonAttributesConfig.VOLITANS_SPINE_DROP_CHANCE::get,
                ForgeDragonAttributesConfig.VOLITANS_SPINE_DROP_CHANCE::set,
                null));
        entries.add(new PercentDoubleEntry(Component.translatable("config.saintsdragons.attributes.volitans.fish_drop_chance"),
                ForgeDragonAttributesConfig.VOLITANS_FISH_DROP_CHANCE::get,
                ForgeDragonAttributesConfig.VOLITANS_FISH_DROP_CHANCE::set,
                null));
        entries.add(new DoubleEntry(Component.translatable("config.saintsdragons.attributes.volitans.egg_hatch_time_ticks_normal"),
                ForgeDragonAttributesConfig.VOLITANS_EGG_HATCH_CHANCE_NORMAL::get,
                ForgeDragonAttributesConfig.VOLITANS_EGG_HATCH_CHANCE_NORMAL::set,
                null));
        entries.add(new PercentDoubleEntry(Component.translatable("config.saintsdragons.attributes.volitans.egg_loot_shipwreck_treasure"),
                ForgeDragonAttributesConfig.VOLITANS_EGG_LOOT_SHIPWRECK_TREASURE::get,
                ForgeDragonAttributesConfig.VOLITANS_EGG_LOOT_SHIPWRECK_TREASURE::set,
                null));
        entries.add(new PercentDoubleEntry(Component.translatable("config.saintsdragons.attributes.volitans.egg_drop_chance"),
                ForgeDragonAttributesConfig.VOLITANS_EGG_DROP_CHANCE::get,
                ForgeDragonAttributesConfig.VOLITANS_EGG_DROP_CHANCE::set,
                null));
        entries.add(new BooleanEntry(Component.translatable("config.saintsdragons.attributes.volitans.legacy_taming"),
                ForgeDragonAttributesConfig.VOLITANS_LEGACY_TAMING::get,
                ForgeDragonAttributesConfig.VOLITANS_LEGACY_TAMING::set,
                null));
        entries.add(new BooleanEntry(Component.translatable("config.saintsdragons.attributes.volitans.aggressive_wild"),
                ForgeDragonAttributesConfig.VOLITANS_AGGRESSIVE_WILD::get,
                ForgeDragonAttributesConfig.VOLITANS_AGGRESSIVE_WILD::set,
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
                ForgeDragonAttributesConfig.CINDERVANE_SLASH_GRAB_HIT1_DAMAGE.set(ForgeDragonAttributesConfig.CINDERVANE_SLASH_GRAB_HIT1_DAMAGE.getDefault());
                ForgeDragonAttributesConfig.CINDERVANE_SLASH_GRAB_HIT2_DAMAGE.set(ForgeDragonAttributesConfig.CINDERVANE_SLASH_GRAB_HIT2_DAMAGE.getDefault());
                ForgeDragonAttributesConfig.CINDERVANE_MAGMA_VOLLEY_DAMAGE.set(ForgeDragonAttributesConfig.CINDERVANE_MAGMA_VOLLEY_DAMAGE.getDefault());
                ForgeDragonAttributesConfig.CINDERVANE_FIRE_BODY_DAMAGE.set(ForgeDragonAttributesConfig.CINDERVANE_FIRE_BODY_DAMAGE.getDefault());
                ForgeDragonAttributesConfig.CINDERVANE_TAMING_CHANCE_BASE.set(ForgeDragonAttributesConfig.CINDERVANE_TAMING_CHANCE_BASE.getDefault());
                ForgeDragonAttributesConfig.CINDERVANE_TAMING_CHANCE_CHICKEN.set(ForgeDragonAttributesConfig.CINDERVANE_TAMING_CHANCE_CHICKEN.getDefault());
                ForgeDragonAttributesConfig.CINDERVANE_TAMING_CHANCE_HEARTY.set(ForgeDragonAttributesConfig.CINDERVANE_TAMING_CHANCE_HEARTY.getDefault());
                ForgeDragonAttributesConfig.CINDERVANE_EGG_HATCH_CHANCE_NORMAL.set(ForgeDragonAttributesConfig.CINDERVANE_EGG_HATCH_CHANCE_NORMAL.getDefault());
                ForgeDragonAttributesConfig.CINDERVANE_EGG_DROP_CHANCE.set(ForgeDragonAttributesConfig.CINDERVANE_EGG_DROP_CHANCE.getDefault());
                ForgeDragonAttributesConfig.CINDERVANE_SCALE_DROP_CHANCE_BRUSH.set(ForgeDragonAttributesConfig.CINDERVANE_SCALE_DROP_CHANCE_BRUSH.getDefault());
                ForgeDragonAttributesConfig.CINDERVANE_FIRE_BODY_EXPLOSION_DAMAGE.set(ForgeDragonAttributesConfig.CINDERVANE_FIRE_BODY_EXPLOSION_DAMAGE.getDefault());
                ForgeDragonAttributesConfig.CINDERVANE_FIRE_BODY_SELF_DAMAGE_ON_CRASH.set(ForgeDragonAttributesConfig.CINDERVANE_FIRE_BODY_SELF_DAMAGE_ON_CRASH.getDefault());
                ForgeDragonAttributesConfig.CINDERVANE_AGGRESSIVE_WILD.set(ForgeDragonAttributesConfig.CINDERVANE_AGGRESSIVE_WILD.getDefault());
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
                ForgeDragonAttributesConfig.STEGONAUT_SCALE_DROP_CHANCE_BRUSH.set(ForgeDragonAttributesConfig.STEGONAUT_SCALE_DROP_CHANCE_BRUSH.getDefault());
                ForgeDragonAttributesConfig.STEGONAUT_AGGRESSIVE_WILD.set(ForgeDragonAttributesConfig.STEGONAUT_AGGRESSIVE_WILD.getDefault());
            }
            case RAEVYX -> {
                ForgeDragonAttributesConfig.RAEVYX_MAX_HEALTH.set(ForgeDragonAttributesConfig.RAEVYX_MAX_HEALTH.getDefault());
                ForgeDragonAttributesConfig.RAEVYX_ARMOR.set(ForgeDragonAttributesConfig.RAEVYX_ARMOR.getDefault());
                ForgeDragonAttributesConfig.RAEVYX_FLYING_SPEED.set(ForgeDragonAttributesConfig.RAEVYX_FLYING_SPEED.getDefault());
                ForgeDragonAttributesConfig.RAEVYX_WILD_FLYING_SPEED_MULTIPLIER.set(ForgeDragonAttributesConfig.RAEVYX_WILD_FLYING_SPEED_MULTIPLIER.getDefault());
                ForgeDragonAttributesConfig.RAEVYX_BITE_DAMAGE.set(ForgeDragonAttributesConfig.RAEVYX_BITE_DAMAGE.getDefault());
                ForgeDragonAttributesConfig.RAEVYX_LIGHTNING_BEAM_DAMAGE.set(ForgeDragonAttributesConfig.RAEVYX_LIGHTNING_BEAM_DAMAGE.getDefault());
                ForgeDragonAttributesConfig.RAEVYX_HORN_GORE_DAMAGE.set(ForgeDragonAttributesConfig.RAEVYX_HORN_GORE_DAMAGE.getDefault());
                ForgeDragonAttributesConfig.RAEVYX_DASH_DAMAGE.set(ForgeDragonAttributesConfig.RAEVYX_DASH_DAMAGE.getDefault());
                ForgeDragonAttributesConfig.RAEVYX_BEAM_DRAIN_PER_TICK.set(ForgeDragonAttributesConfig.RAEVYX_BEAM_DRAIN_PER_TICK.getDefault());
                ForgeDragonAttributesConfig.RAEVYX_BEAM_REGEN_PER_TICK.set(ForgeDragonAttributesConfig.RAEVYX_BEAM_REGEN_PER_TICK.getDefault());
                ForgeDragonAttributesConfig.RAEVYX_SUMMON_STORM_COOLDOWN_TICKS.set(ForgeDragonAttributesConfig.RAEVYX_SUMMON_STORM_COOLDOWN_TICKS.getDefault());
                ForgeDragonAttributesConfig.RAEVYX_SUMMON_STORM_SUPERCHARGE_TICKS.set(ForgeDragonAttributesConfig.RAEVYX_SUMMON_STORM_SUPERCHARGE_TICKS.getDefault());
                ForgeDragonAttributesConfig.RAEVYX_SUMMON_STORM_SUPERCHARGE_DAMAGE_MULTIPLIER.set(ForgeDragonAttributesConfig.RAEVYX_SUMMON_STORM_SUPERCHARGE_DAMAGE_MULTIPLIER.getDefault());
                ForgeDragonAttributesConfig.RAEVYX_SUMMON_STORM_DURATION_TICKS.set(ForgeDragonAttributesConfig.RAEVYX_SUMMON_STORM_DURATION_TICKS.getDefault());
                ForgeDragonAttributesConfig.RAEVYX_TAMING_CHANCE_BASE.set(ForgeDragonAttributesConfig.RAEVYX_TAMING_CHANCE_BASE.getDefault());
                ForgeDragonAttributesConfig.RAEVYX_TAMING_CHANCE_HEARTY.set(ForgeDragonAttributesConfig.RAEVYX_TAMING_CHANCE_HEARTY.getDefault());
                ForgeDragonAttributesConfig.RAEVYX_LEGACY_TAMING.set(ForgeDragonAttributesConfig.RAEVYX_LEGACY_TAMING.getDefault());
                ForgeDragonAttributesConfig.RAEVYX_EGG_HATCH_TIME_TICKS_NORMAL.set(ForgeDragonAttributesConfig.RAEVYX_EGG_HATCH_TIME_TICKS_NORMAL.getDefault());
                ForgeDragonAttributesConfig.RAEVYX_EGG_HATCH_TIME_TICKS_THUNDER.set(ForgeDragonAttributesConfig.RAEVYX_EGG_HATCH_TIME_TICKS_THUNDER.getDefault());
                ForgeDragonAttributesConfig.RAEVYX_EGG_LOOT_PILLAGER_OUTPOST.set(ForgeDragonAttributesConfig.RAEVYX_EGG_LOOT_PILLAGER_OUTPOST.getDefault());
                ForgeDragonAttributesConfig.RAEVYX_EGG_LOOT_ANCIENT_CITY.set(ForgeDragonAttributesConfig.RAEVYX_EGG_LOOT_ANCIENT_CITY.getDefault());
                ForgeDragonAttributesConfig.RAEVYX_EGG_DROP_CHANCE.set(ForgeDragonAttributesConfig.RAEVYX_EGG_DROP_CHANCE.getDefault());
                ForgeDragonAttributesConfig.RAEVYX_SCALE_DROP_CHANCE_BRUSH.set(ForgeDragonAttributesConfig.RAEVYX_SCALE_DROP_CHANCE_BRUSH.getDefault());
                ForgeDragonAttributesConfig.RAEVYX_AGGRESSIVE_WILD.set(ForgeDragonAttributesConfig.RAEVYX_AGGRESSIVE_WILD.getDefault());
            }
            case VARASUCHUS -> {
                ForgeDragonAttributesConfig.VARASUCHUS_MAX_HEALTH.set(ForgeDragonAttributesConfig.VARASUCHUS_MAX_HEALTH.getDefault());
                ForgeDragonAttributesConfig.VARASUCHUS_ARMOR.set(ForgeDragonAttributesConfig.VARASUCHUS_ARMOR.getDefault());
                ForgeDragonAttributesConfig.VARASUCHUS_SWIM_SPEED.set(ForgeDragonAttributesConfig.VARASUCHUS_SWIM_SPEED.getDefault());
                ForgeDragonAttributesConfig.VARASUCHUS_BITE_PHASE1_DAMAGE.set(ForgeDragonAttributesConfig.VARASUCHUS_BITE_PHASE1_DAMAGE.getDefault());
                ForgeDragonAttributesConfig.VARASUCHUS_BITE_PHASE2_DAMAGE.set(ForgeDragonAttributesConfig.VARASUCHUS_BITE_PHASE2_DAMAGE.getDefault());
                ForgeDragonAttributesConfig.VARASUCHUS_TAIL_ATTACK_DAMAGE.set(ForgeDragonAttributesConfig.VARASUCHUS_TAIL_ATTACK_DAMAGE.getDefault());
                ForgeDragonAttributesConfig.VARASUCHUS_DASH_TAIL_SWIPE_DAMAGE.set(ForgeDragonAttributesConfig.VARASUCHUS_DASH_TAIL_SWIPE_DAMAGE.getDefault());
                ForgeDragonAttributesConfig.VARASUCHUS_DASH_CLAW_DAMAGE.set(ForgeDragonAttributesConfig.VARASUCHUS_DASH_CLAW_DAMAGE.getDefault());
                ForgeDragonAttributesConfig.VARASUCHUS_CLAW_ATTACK_DAMAGE.set(ForgeDragonAttributesConfig.VARASUCHUS_CLAW_ATTACK_DAMAGE.getDefault());
                ForgeDragonAttributesConfig.VARASUCHUS_HORN_GORE_PHASE1_DAMAGE.set(ForgeDragonAttributesConfig.VARASUCHUS_HORN_GORE_PHASE1_DAMAGE.getDefault());
                ForgeDragonAttributesConfig.VARASUCHUS_HORN_GORE_PHASE2_DAMAGE.set(ForgeDragonAttributesConfig.VARASUCHUS_HORN_GORE_PHASE2_DAMAGE.getDefault());
                ForgeDragonAttributesConfig.VARASUCHUS_TAMING_CHANCE.set(ForgeDragonAttributesConfig.VARASUCHUS_TAMING_CHANCE.getDefault());
                ForgeDragonAttributesConfig.VARASUCHUS_TAMING_CHANCE_TROPICAL.set(ForgeDragonAttributesConfig.VARASUCHUS_TAMING_CHANCE_TROPICAL.getDefault());
                ForgeDragonAttributesConfig.VARASUCHUS_LEGACY_TAMING.set(ForgeDragonAttributesConfig.VARASUCHUS_LEGACY_TAMING.getDefault());
                ForgeDragonAttributesConfig.VARASUCHUS_EGG_HATCH_CHANCE_NORMAL.set(ForgeDragonAttributesConfig.VARASUCHUS_EGG_HATCH_CHANCE_NORMAL.getDefault());
                ForgeDragonAttributesConfig.VARASUCHUS_EGG_DROP_CHANCE.set(ForgeDragonAttributesConfig.VARASUCHUS_EGG_DROP_CHANCE.getDefault());
                ForgeDragonAttributesConfig.VARASUCHUS_SCALE_DROP_CHANCE_BRUSH.set(ForgeDragonAttributesConfig.VARASUCHUS_SCALE_DROP_CHANCE_BRUSH.getDefault());
                ForgeDragonAttributesConfig.VARASUCHUS_AGGRESSIVE_WILD.set(ForgeDragonAttributesConfig.VARASUCHUS_AGGRESSIVE_WILD.getDefault());
            }
            case IGNIVORUS -> {
                ForgeDragonAttributesConfig.IGNIVORUS_MAX_HEALTH.set(ForgeDragonAttributesConfig.IGNIVORUS_MAX_HEALTH.getDefault());
                ForgeDragonAttributesConfig.IGNIVORUS_ARMOR.set(ForgeDragonAttributesConfig.IGNIVORUS_ARMOR.getDefault());
                ForgeDragonAttributesConfig.IGNIVORUS_FLYING_SPEED.set(ForgeDragonAttributesConfig.IGNIVORUS_FLYING_SPEED.getDefault());
                ForgeDragonAttributesConfig.IGNIVORUS_WILD_FLYING_SPEED_MULTIPLIER.set(ForgeDragonAttributesConfig.IGNIVORUS_WILD_FLYING_SPEED_MULTIPLIER.getDefault());
                ForgeDragonAttributesConfig.IGNIVORUS_BITE_DAMAGE.set(ForgeDragonAttributesConfig.IGNIVORUS_BITE_DAMAGE.getDefault());
                ForgeDragonAttributesConfig.IGNIVORUS_BODY_SLAM_DAMAGE.set(ForgeDragonAttributesConfig.IGNIVORUS_BODY_SLAM_DAMAGE.getDefault());
                ForgeDragonAttributesConfig.IGNIVORUS_LEAP_SLAM_DAMAGE.set(ForgeDragonAttributesConfig.IGNIVORUS_LEAP_SLAM_DAMAGE.getDefault());
                ForgeDragonAttributesConfig.IGNIVORUS_FIRE_BREATH_DAMAGE.set(ForgeDragonAttributesConfig.IGNIVORUS_FIRE_BREATH_DAMAGE.getDefault());
                ForgeDragonAttributesConfig.IGNIVORUS_FIREBALL_DAMAGE.set(ForgeDragonAttributesConfig.IGNIVORUS_FIREBALL_DAMAGE.getDefault());
                ForgeDragonAttributesConfig.IGNIVORUS_MAGMA_PILLAR_DAMAGE.set(ForgeDragonAttributesConfig.IGNIVORUS_MAGMA_PILLAR_DAMAGE.getDefault());
                ForgeDragonAttributesConfig.IGNIVORUS_WING_SWIPE_DAMAGE.set(ForgeDragonAttributesConfig.IGNIVORUS_WING_SWIPE_DAMAGE.getDefault());
                ForgeDragonAttributesConfig.IGNIVORUS_STOMP_DAMAGE.set(ForgeDragonAttributesConfig.IGNIVORUS_STOMP_DAMAGE.getDefault());
                ForgeDragonAttributesConfig.IGNIVORUS_BULLDOZE_DAMAGE.set(ForgeDragonAttributesConfig.IGNIVORUS_BULLDOZE_DAMAGE.getDefault());
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
                ForgeDragonAttributesConfig.IGNIVORUS_SCALE_DROP_CHANCE_BRUSH.set(ForgeDragonAttributesConfig.IGNIVORUS_SCALE_DROP_CHANCE_BRUSH.getDefault());
                ForgeDragonAttributesConfig.IGNIVORUS_AGGRESSIVE_WILD.set(ForgeDragonAttributesConfig.IGNIVORUS_AGGRESSIVE_WILD.getDefault());
            }
            case VOLITANS -> {
                ForgeDragonAttributesConfig.VOLITANS_MAX_HEALTH.set(ForgeDragonAttributesConfig.VOLITANS_MAX_HEALTH.getDefault());
                ForgeDragonAttributesConfig.VOLITANS_ARMOR.set(ForgeDragonAttributesConfig.VOLITANS_ARMOR.getDefault());
                ForgeDragonAttributesConfig.VOLITANS_FLYING_SPEED.set(ForgeDragonAttributesConfig.VOLITANS_FLYING_SPEED.getDefault());
                ForgeDragonAttributesConfig.VOLITANS_WILD_FLYING_SPEED_MULTIPLIER.set(ForgeDragonAttributesConfig.VOLITANS_WILD_FLYING_SPEED_MULTIPLIER.getDefault());
                ForgeDragonAttributesConfig.VOLITANS_BITE_DAMAGE.set(ForgeDragonAttributesConfig.VOLITANS_BITE_DAMAGE.getDefault());
                ForgeDragonAttributesConfig.VOLITANS_CLAW_DAMAGE.set(ForgeDragonAttributesConfig.VOLITANS_CLAW_DAMAGE.getDefault());
                ForgeDragonAttributesConfig.VOLITANS_HORN_GORE_DAMAGE.set(ForgeDragonAttributesConfig.VOLITANS_HORN_GORE_DAMAGE.getDefault());
                ForgeDragonAttributesConfig.VOLITANS_ROAR_GROUND_DAMAGE.set(ForgeDragonAttributesConfig.VOLITANS_ROAR_GROUND_DAMAGE.getDefault());
                ForgeDragonAttributesConfig.VOLITANS_ROAR_AIR_WATER_DAMAGE.set(ForgeDragonAttributesConfig.VOLITANS_ROAR_AIR_WATER_DAMAGE.getDefault());
                ForgeDragonAttributesConfig.VOLITANS_BURROW_DAMAGE.set(ForgeDragonAttributesConfig.VOLITANS_BURROW_DAMAGE.getDefault());
                ForgeDragonAttributesConfig.VOLITANS_POISON_BALL_DAMAGE.set(ForgeDragonAttributesConfig.VOLITANS_POISON_BALL_DAMAGE.getDefault());
                ForgeDragonAttributesConfig.VOLITANS_WATER_BREATH_DAMAGE.set(ForgeDragonAttributesConfig.VOLITANS_WATER_BREATH_DAMAGE.getDefault());
                ForgeDragonAttributesConfig.VOLITANS_POISON_BREATH_DAMAGE.set(ForgeDragonAttributesConfig.VOLITANS_POISON_BREATH_DAMAGE.getDefault());
                ForgeDragonAttributesConfig.VOLITANS_TAMING_CHANCE_BASE.set(ForgeDragonAttributesConfig.VOLITANS_TAMING_CHANCE_BASE.getDefault());
                ForgeDragonAttributesConfig.VOLITANS_TAMING_CHANCE_HEARTY.set(ForgeDragonAttributesConfig.VOLITANS_TAMING_CHANCE_HEARTY.getDefault());
                ForgeDragonAttributesConfig.VOLITANS_TAMING_STUN_HEALTH.set(ForgeDragonAttributesConfig.VOLITANS_TAMING_STUN_HEALTH.getDefault());
                ForgeDragonAttributesConfig.VOLITANS_LEGACY_TAMING.set(ForgeDragonAttributesConfig.VOLITANS_LEGACY_TAMING.getDefault());
                ForgeDragonAttributesConfig.VOLITANS_EGG_HATCH_CHANCE_NORMAL.set(ForgeDragonAttributesConfig.VOLITANS_EGG_HATCH_CHANCE_NORMAL.getDefault());
                ForgeDragonAttributesConfig.VOLITANS_EGG_LOOT_SHIPWRECK_TREASURE.set(ForgeDragonAttributesConfig.VOLITANS_EGG_LOOT_SHIPWRECK_TREASURE.getDefault());
                ForgeDragonAttributesConfig.VOLITANS_EGG_DROP_CHANCE.set(ForgeDragonAttributesConfig.VOLITANS_EGG_DROP_CHANCE.getDefault());
                ForgeDragonAttributesConfig.VOLITANS_SCALE_DROP_CHANCE_BRUSH.set(ForgeDragonAttributesConfig.VOLITANS_SCALE_DROP_CHANCE_BRUSH.getDefault());
                ForgeDragonAttributesConfig.VOLITANS_SPINE_DROP_CHANCE.set(ForgeDragonAttributesConfig.VOLITANS_SPINE_DROP_CHANCE.getDefault());
                ForgeDragonAttributesConfig.VOLITANS_FISH_DROP_CHANCE.set(ForgeDragonAttributesConfig.VOLITANS_FISH_DROP_CHANCE.getDefault());
                ForgeDragonAttributesConfig.VOLITANS_BREATH_ACTIVE_TICKS_MAX.set(ForgeDragonAttributesConfig.VOLITANS_BREATH_ACTIVE_TICKS_MAX.getDefault());
                ForgeDragonAttributesConfig.VOLITANS_BREATH_DRAIN_PER_TICK.set(ForgeDragonAttributesConfig.VOLITANS_BREATH_DRAIN_PER_TICK.getDefault());
                ForgeDragonAttributesConfig.VOLITANS_BREATH_REGEN_PER_TICK.set(ForgeDragonAttributesConfig.VOLITANS_BREATH_REGEN_PER_TICK.getDefault());
                ForgeDragonAttributesConfig.VOLITANS_BREATH_PROJECTILE_SPREAD.set(ForgeDragonAttributesConfig.VOLITANS_BREATH_PROJECTILE_SPREAD.getDefault());
                ForgeDragonAttributesConfig.VOLITANS_BREATH_PROJECTILE_SPEED.set(ForgeDragonAttributesConfig.VOLITANS_BREATH_PROJECTILE_SPEED.getDefault());
                ForgeDragonAttributesConfig.VOLITANS_BREATH_PROJECTILE_LIFETIME.set(ForgeDragonAttributesConfig.VOLITANS_BREATH_PROJECTILE_LIFETIME.getDefault());
                ForgeDragonAttributesConfig.VOLITANS_POISON_BREATH_POISON_DURATION_TICKS.set(ForgeDragonAttributesConfig.VOLITANS_POISON_BREATH_POISON_DURATION_TICKS.getDefault());
                ForgeDragonAttributesConfig.VOLITANS_POISON_BREATH_POISON_LEVEL.set(ForgeDragonAttributesConfig.VOLITANS_POISON_BREATH_POISON_LEVEL.getDefault());
                ForgeDragonAttributesConfig.VOLITANS_POISON_BALL_POISON_DURATION_TICKS.set(ForgeDragonAttributesConfig.VOLITANS_POISON_BALL_POISON_DURATION_TICKS.getDefault());
                ForgeDragonAttributesConfig.VOLITANS_POISON_BALL_POISON_LEVEL.set(ForgeDragonAttributesConfig.VOLITANS_POISON_BALL_POISON_LEVEL.getDefault());
                ForgeDragonAttributesConfig.VOLITANS_ROAR_GROUND_POISON_DURATION_TICKS.set(ForgeDragonAttributesConfig.VOLITANS_ROAR_GROUND_POISON_DURATION_TICKS.getDefault());
                ForgeDragonAttributesConfig.VOLITANS_ROAR_GROUND_POISON_LEVEL.set(ForgeDragonAttributesConfig.VOLITANS_ROAR_GROUND_POISON_LEVEL.getDefault());
                ForgeDragonAttributesConfig.VOLITANS_ROAR_AIR_WATER_POISON_DURATION_TICKS.set(ForgeDragonAttributesConfig.VOLITANS_ROAR_AIR_WATER_POISON_DURATION_TICKS.getDefault());
                ForgeDragonAttributesConfig.VOLITANS_ROAR_AIR_WATER_POISON_LEVEL.set(ForgeDragonAttributesConfig.VOLITANS_ROAR_AIR_WATER_POISON_LEVEL.getDefault());
                ForgeDragonAttributesConfig.VOLITANS_AGGRESSIVE_WILD.set(ForgeDragonAttributesConfig.VOLITANS_AGGRESSIVE_WILD.getDefault());
            }
            case NULLJAW -> {
                ForgeDragonAttributesConfig.NULLJAW_MAX_HEALTH.set(ForgeDragonAttributesConfig.NULLJAW_MAX_HEALTH.getDefault());
                ForgeDragonAttributesConfig.NULLJAW_ARMOR.set(ForgeDragonAttributesConfig.NULLJAW_ARMOR.getDefault());
            }
        }

        ForgeDragonAttributesConfig.ATTRIBUTES_SPEC.save();
        DragonAttributeConfigLoader.getInstance().refreshFromForgeConfig();
        applyAttributesToLoadedDragons();
    }
}

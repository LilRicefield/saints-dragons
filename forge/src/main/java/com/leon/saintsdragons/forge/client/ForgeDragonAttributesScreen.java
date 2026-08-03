package com.leon.saintsdragons.forge.client;

import com.leon.saintsdragons.common.config.SaintsDragonsConfig;
import com.leon.saintsdragons.common.config.dragon.DragonAttributeConfigLoader;
import com.leon.saintsdragons.forge.platform.ForgeDragonAttributesConfig;
import com.leon.saintsdragons.server.entity.base.DragonEntity;
import com.leon.saintsdragons.server.entity.dragons.atroxiia.Atroxiia;
import com.leon.saintsdragons.server.entity.dragons.cindervane.Cindervane;
import com.leon.saintsdragons.server.entity.dragons.ignivorus.Ignivorus;
import com.leon.saintsdragons.server.entity.dragons.nulljaw.Nulljaw;
import com.leon.saintsdragons.server.entity.dragons.varasuchus.Varasuchus;
import com.leon.saintsdragons.server.entity.dragons.raevyx.Raevyx;
import com.leon.saintsdragons.server.entity.dragons.stegonaut.Stegonaut;
import com.leon.saintsdragons.server.entity.dragons.volitans.Volitans;
import com.leon.saintsdragons.server.entity.draconianswarm.AbstractDraconianSwarmEntity;
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
        NULLJAW,
        ATROXIIA,
        DRACONIAN_SWARM
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
            case ATROXIIA -> addAtroxiiaEntries(entries);
            case DRACONIAN_SWARM -> addDraconianSwarmEntries(entries);
        }
    }

    @Override
    protected void addHeaderButtons() {
        int buttonWidth = Math.min(80, (width - 84) / 8);
        int spacing = 6;
        int totalWidth = buttonWidth * 8 + spacing * 7;
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

        addRenderableWidget(net.minecraft.client.gui.components.Button.builder(Component.translatable("config.saintsdragons.attributes.draconian_swarm"), button -> {
            if (section != Section.DRACONIAN_SWARM) {
                section = Section.DRACONIAN_SWARM;
                rebuildWidgets();
            }
        }).bounds(startX + (buttonWidth + spacing) * 7, y, buttonWidth, 20).build());

        addRenderableWidget(net.minecraft.client.gui.components.Button.builder(Component.translatable("config.saintsdragons.attributes.atroxiia"), button -> {
            if (section != Section.ATROXIIA) {
                section = Section.ATROXIIA;
                rebuildWidgets();
            }
        }).bounds(width / 2 - 40, y + 22, 80, 20).build());

        addRenderableWidget(net.minecraft.client.gui.components.Button.builder(Component.translatable("saintsdragons.config_screen.reset"), button -> {
            resetSection();
            rebuildWidgets();
        }).bounds(width / 2 - 150, height - 28, 60, 20).build());
    }

    @Override
    protected int getPanelTop() {
        return 82;
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
                } else if (dragon instanceof Atroxiia atroxiia) {
                    atroxiia.applyConfiguredAttributes();
                }
            }
            for (AbstractDraconianSwarmEntity swarm : level.getEntitiesOfClass(AbstractDraconianSwarmEntity.class, bounds)) {
                swarm.applyConfiguredAttributes();
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
        entries.add(new DoubleEntry(Component.translatable("config.saintsdragons.attributes.cindervane.double_bite_damage"),
                ForgeDragonAttributesConfig.CINDERVANE_DOUBLE_BITE_DAMAGE::get,
                ForgeDragonAttributesConfig.CINDERVANE_DOUBLE_BITE_DAMAGE::set,
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
        entries.add(new DoubleEntry(Component.translatable("config.saintsdragons.attributes.stegonaut.ground_slam_damage"),
                ForgeDragonAttributesConfig.STEGONAUT_GROUND_SLAM_DAMAGE::get,
                ForgeDragonAttributesConfig.STEGONAUT_GROUND_SLAM_DAMAGE::set,
                null));
        entries.add(new DoubleEntry(Component.translatable("config.saintsdragons.attributes.stegonaut.ground_slam_knockback"),
                ForgeDragonAttributesConfig.STEGONAUT_GROUND_SLAM_KNOCKBACK::get,
                ForgeDragonAttributesConfig.STEGONAUT_GROUND_SLAM_KNOCKBACK::set,
                null));
        entries.add(new DoubleEntry(Component.translatable("config.saintsdragons.attributes.stegonaut.ground_slam2_damage"),
                ForgeDragonAttributesConfig.STEGONAUT_GROUND_SLAM2_DAMAGE::get,
                ForgeDragonAttributesConfig.STEGONAUT_GROUND_SLAM2_DAMAGE::set,
                null));
        entries.add(new DoubleEntry(Component.translatable("config.saintsdragons.attributes.stegonaut.ground_slam2_knockback"),
                ForgeDragonAttributesConfig.STEGONAUT_GROUND_SLAM2_KNOCKBACK::get,
                ForgeDragonAttributesConfig.STEGONAUT_GROUND_SLAM2_KNOCKBACK::set,
                null));
        entries.add(new DoubleEntry(Component.translatable("config.saintsdragons.attributes.stegonaut.ground_slam_pillar_damage"),
                ForgeDragonAttributesConfig.STEGONAUT_GROUND_SLAM_PILLAR_DAMAGE::get,
                ForgeDragonAttributesConfig.STEGONAUT_GROUND_SLAM_PILLAR_DAMAGE::set,
                null));
        entries.add(new DoubleEntry(Component.translatable("config.saintsdragons.attributes.stegonaut.ground_slam_pillar_knockback"),
                ForgeDragonAttributesConfig.STEGONAUT_GROUND_SLAM_PILLAR_KNOCKBACK::get,
                ForgeDragonAttributesConfig.STEGONAUT_GROUND_SLAM_PILLAR_KNOCKBACK::set,
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
        entries.add(new WarningEntry(Component.translatable("config.saintsdragons.attributes.taming_stun_health.warning")));
        entries.add(new DoubleEntry(Component.translatable("config.saintsdragons.attributes.raevyx.armor"),
                ForgeDragonAttributesConfig.RAEVYX_ARMOR::get,
                ForgeDragonAttributesConfig.RAEVYX_ARMOR::set,
                null));
        entries.add(new DoubleEntry(Component.translatable("config.saintsdragons.attributes.raevyx.flying_speed"),
                ForgeDragonAttributesConfig.RAEVYX_FLYING_SPEED::get,
                ForgeDragonAttributesConfig.RAEVYX_FLYING_SPEED::set,
                null));
        entries.add(new BooleanEntry(Component.translatable("config.saintsdragons.attributes.raevyx.dive_loop"),
                () -> ForgeDragonAttributesConfig.RAEVYX_DIVE_LOOP_ENABLED.get(),
                ForgeDragonAttributesConfig.RAEVYX_DIVE_LOOP_ENABLED::set,
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
        entries.add(new DoubleEntry(Component.translatable("config.saintsdragons.attributes.raevyx.taming_mutton"),
                ForgeDragonAttributesConfig.RAEVYX_TAMING_CHANCE_MUTTON::get,
                ForgeDragonAttributesConfig.RAEVYX_TAMING_CHANCE_MUTTON::set,
                null));
        entries.add(new DoubleEntry(Component.translatable("config.saintsdragons.attributes.raevyx.taming_porkchop"),
                ForgeDragonAttributesConfig.RAEVYX_TAMING_CHANCE_PORKCHOP::get,
                ForgeDragonAttributesConfig.RAEVYX_TAMING_CHANCE_PORKCHOP::set,
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
        entries.add(new DoubleEntry(Component.translatable("config.saintsdragons.attributes.varasuchus.tailguard_parry"),
                ForgeDragonAttributesConfig.VARASUCHUS_TAILGUARD_PARRY_DAMAGE::get,
                ForgeDragonAttributesConfig.VARASUCHUS_TAILGUARD_PARRY_DAMAGE::set,
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
        entries.add(new DoubleEntry(Component.translatable("config.saintsdragons.attributes.varasuchus.taming_beef"),
                ForgeDragonAttributesConfig.VARASUCHUS_TAMING_CHANCE_BEEF::get,
                ForgeDragonAttributesConfig.VARASUCHUS_TAMING_CHANCE_BEEF::set,
                null));
        entries.add(new DoubleEntry(Component.translatable("config.saintsdragons.attributes.varasuchus.taming_tropical"),
                ForgeDragonAttributesConfig.VARASUCHUS_TAMING_CHANCE_TROPICAL::get,
                ForgeDragonAttributesConfig.VARASUCHUS_TAMING_CHANCE_TROPICAL::set,
                null));
        entries.add(new DoubleEntry(Component.translatable("config.saintsdragons.attributes.varasuchus.egg_hatch_time_ticks_normal"),
                ForgeDragonAttributesConfig.VARASUCHUS_EGG_HATCH_CHANCE_NORMAL::get,
                ForgeDragonAttributesConfig.VARASUCHUS_EGG_HATCH_CHANCE_NORMAL::set,
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
        entries.add(new WarningEntry(Component.translatable("config.saintsdragons.attributes.ignivorus.thresholds.warning")));
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
        entries.add(new PercentDoubleEntry(Component.translatable("config.saintsdragons.attributes.ignivorus.ultimate_trigger_health_fraction"),
                ForgeDragonAttributesConfig.IGNIVORUS_ULTIMATE_TRIGGER_HEALTH_FRACTION::get,
                ForgeDragonAttributesConfig.IGNIVORUS_ULTIMATE_TRIGGER_HEALTH_FRACTION::set,
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
        entries.add(new DoubleEntry(Component.translatable("config.saintsdragons.attributes.ignivorus.taming_base"),
                ForgeDragonAttributesConfig.IGNIVORUS_TAMING_CHANCE_BASE::get,
                ForgeDragonAttributesConfig.IGNIVORUS_TAMING_CHANCE_BASE::set,
                null));
        entries.add(new DoubleEntry(Component.translatable("config.saintsdragons.attributes.ignivorus.taming_beef"),
                ForgeDragonAttributesConfig.IGNIVORUS_TAMING_CHANCE_BEEF::get,
                ForgeDragonAttributesConfig.IGNIVORUS_TAMING_CHANCE_BEEF::set,
                null));
        entries.add(new DoubleEntry(Component.translatable("config.saintsdragons.attributes.ignivorus.taming_mutton"),
                ForgeDragonAttributesConfig.IGNIVORUS_TAMING_CHANCE_MUTTON::get,
                ForgeDragonAttributesConfig.IGNIVORUS_TAMING_CHANCE_MUTTON::set,
                null));
        entries.add(new DoubleEntry(Component.translatable("config.saintsdragons.attributes.ignivorus.taming_porkchop"),
                ForgeDragonAttributesConfig.IGNIVORUS_TAMING_CHANCE_PORKCHOP::get,
                ForgeDragonAttributesConfig.IGNIVORUS_TAMING_CHANCE_PORKCHOP::set,
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

    private void addAtroxiiaEntries(List<ConfigEntry> entries) {
        entries.add(new SectionEntry(Component.translatable("config.saintsdragons.attributes.atroxiia")));
        entries.add(new DoubleEntry(Component.translatable("config.saintsdragons.attributes.atroxiia.max_health"),
                ForgeDragonAttributesConfig.ATROXIIA_MAX_HEALTH::get,
                ForgeDragonAttributesConfig.ATROXIIA_MAX_HEALTH::set,
                null));
        entries.add(new DoubleEntry(Component.translatable("config.saintsdragons.attributes.atroxiia.armor"),
                ForgeDragonAttributesConfig.ATROXIIA_ARMOR::get,
                ForgeDragonAttributesConfig.ATROXIIA_ARMOR::set,
                null));
        entries.add(new DoubleEntry(Component.translatable("config.saintsdragons.attributes.atroxiia.slam_damage"),
                ForgeDragonAttributesConfig.ATROXIIA_SLAM_DAMAGE::get,
                ForgeDragonAttributesConfig.ATROXIIA_SLAM_DAMAGE::set,
                null));
        entries.add(new DoubleEntry(Component.translatable("config.saintsdragons.attributes.atroxiia.swipe_damage"),
                ForgeDragonAttributesConfig.ATROXIIA_SWIPE_DAMAGE::get,
                ForgeDragonAttributesConfig.ATROXIIA_SWIPE_DAMAGE::set,
                null));
        entries.add(new DoubleEntry(Component.translatable("config.saintsdragons.attributes.atroxiia.underwater_bite_damage"),
                ForgeDragonAttributesConfig.ATROXIIA_UNDERWATER_BITE_DAMAGE::get,
                ForgeDragonAttributesConfig.ATROXIIA_UNDERWATER_BITE_DAMAGE::set,
                null));
        entries.add(new DoubleEntry(Component.translatable("config.saintsdragons.attributes.atroxiia.gungnir_stab_damage"),
                ForgeDragonAttributesConfig.ATROXIIA_GUNGNIR_STAB_DAMAGE::get,
                ForgeDragonAttributesConfig.ATROXIIA_GUNGNIR_STAB_DAMAGE::set,
                null));
        entries.add(new DoubleEntry(Component.translatable("config.saintsdragons.attributes.atroxiia.precise_strike_damage"),
                ForgeDragonAttributesConfig.ATROXIIA_PRECISE_STRIKE_DAMAGE::get,
                ForgeDragonAttributesConfig.ATROXIIA_PRECISE_STRIKE_DAMAGE::set,
                null));
        entries.add(new DoubleEntry(Component.translatable("config.saintsdragons.attributes.atroxiia.precise_strike_knockback"),
                ForgeDragonAttributesConfig.ATROXIIA_PRECISE_STRIKE_KNOCKBACK::get,
                ForgeDragonAttributesConfig.ATROXIIA_PRECISE_STRIKE_KNOCKBACK::set,
                null));
        entries.add(new DoubleEntry(Component.translatable("config.saintsdragons.attributes.atroxiia.precise_strike_stun_duration_ticks"),
                ForgeDragonAttributesConfig.ATROXIIA_PRECISE_STRIKE_STUN_DURATION_TICKS::get,
                ForgeDragonAttributesConfig.ATROXIIA_PRECISE_STRIKE_STUN_DURATION_TICKS::set,
                null));
        entries.add(new DoubleEntry(Component.translatable("config.saintsdragons.attributes.atroxiia.devastating_sweep_damage"),
                ForgeDragonAttributesConfig.ATROXIIA_DEVASTATING_SWEEP_DAMAGE::get,
                ForgeDragonAttributesConfig.ATROXIIA_DEVASTATING_SWEEP_DAMAGE::set,
                null));
        entries.add(new DoubleEntry(Component.translatable("config.saintsdragons.attributes.atroxiia.devastating_sweep_knockback"),
                ForgeDragonAttributesConfig.ATROXIIA_DEVASTATING_SWEEP_KNOCKBACK::get,
                ForgeDragonAttributesConfig.ATROXIIA_DEVASTATING_SWEEP_KNOCKBACK::set,
                null));
        entries.add(new DoubleEntry(Component.translatable("config.saintsdragons.attributes.atroxiia.helheim_quake_damage"),
                ForgeDragonAttributesConfig.ATROXIIA_HELHEIM_QUAKE_DAMAGE::get,
                ForgeDragonAttributesConfig.ATROXIIA_HELHEIM_QUAKE_DAMAGE::set,
                null));
        entries.add(new DoubleEntry(Component.translatable("config.saintsdragons.attributes.atroxiia.helheim_quake_knockback"),
                ForgeDragonAttributesConfig.ATROXIIA_HELHEIM_QUAKE_KNOCKBACK::get,
                ForgeDragonAttributesConfig.ATROXIIA_HELHEIM_QUAKE_KNOCKBACK::set,
                null));
        entries.add(new DoubleEntry(Component.translatable("config.saintsdragons.attributes.atroxiia.helheim_quake_secondary_knockback"),
                ForgeDragonAttributesConfig.ATROXIIA_HELHEIM_QUAKE_SECONDARY_KNOCKBACK::get,
                ForgeDragonAttributesConfig.ATROXIIA_HELHEIM_QUAKE_SECONDARY_KNOCKBACK::set,
                null));
        entries.add(new DoubleEntry(Component.translatable("config.saintsdragons.attributes.atroxiia.helheim_quake_stun_duration_ticks"),
                ForgeDragonAttributesConfig.ATROXIIA_HELHEIM_QUAKE_STUN_DURATION_TICKS::get,
                ForgeDragonAttributesConfig.ATROXIIA_HELHEIM_QUAKE_STUN_DURATION_TICKS::set,
                null));
        entries.add(new DoubleEntry(Component.translatable("config.saintsdragons.attributes.atroxiia.egg_hatch_time_ticks_normal"),
                ForgeDragonAttributesConfig.ATROXIIA_EGG_HATCH_TIME_TICKS_NORMAL::get,
                ForgeDragonAttributesConfig.ATROXIIA_EGG_HATCH_TIME_TICKS_NORMAL::set,
                null));
        entries.add(new BooleanEntry(Component.translatable("config.saintsdragons.attributes.atroxiia.frost_impact_enabled"),
                ForgeDragonAttributesConfig.ATROXIIA_FROST_IMPACT_ENABLED::get,
                ForgeDragonAttributesConfig.ATROXIIA_FROST_IMPACT_ENABLED::set,
                null));
        entries.add(new BooleanEntry(Component.translatable("config.saintsdragons.attributes.atroxiia.aggressive_wild"),
                ForgeDragonAttributesConfig.ATROXIIA_AGGRESSIVE_WILD::get,
                ForgeDragonAttributesConfig.ATROXIIA_AGGRESSIVE_WILD::set,
                null));
    }

    private void addDraconianSwarmEntries(List<ConfigEntry> entries) {
        entries.add(new SectionEntry(Component.translatable("config.saintsdragons.attributes.draconian_swarm")));
        entries.add(new DoubleEntry(Component.translatable("config.saintsdragons.attributes.draconian_swarm.wave_1_count"),
                () -> ForgeDragonAttributesConfig.SWARM_WAVE_1_COUNT.get().doubleValue(),
                value -> ForgeDragonAttributesConfig.SWARM_WAVE_1_COUNT.set(clampSwarmWaveCount(value)),
                null));
        entries.add(new DoubleEntry(Component.translatable("config.saintsdragons.attributes.draconian_swarm.wave_2_count"),
                () -> ForgeDragonAttributesConfig.SWARM_WAVE_2_COUNT.get().doubleValue(),
                value -> ForgeDragonAttributesConfig.SWARM_WAVE_2_COUNT.set(clampSwarmWaveCount(value)),
                null));
        entries.add(new DoubleEntry(Component.translatable("config.saintsdragons.attributes.draconian_swarm.wave_3_count"),
                () -> ForgeDragonAttributesConfig.SWARM_WAVE_3_COUNT.get().doubleValue(),
                value -> ForgeDragonAttributesConfig.SWARM_WAVE_3_COUNT.set(clampSwarmWaveCount(value)),
                null));
        entries.add(new SectionEntry(Component.translatable("config.saintsdragons.attributes.draconian_swarm.latcher")));
        entries.add(new DoubleEntry(Component.translatable("config.saintsdragons.attributes.draconian_swarm.latcher.max_health"),
                ForgeDragonAttributesConfig.SWARM_LATCHER_MAX_HEALTH::get,
                ForgeDragonAttributesConfig.SWARM_LATCHER_MAX_HEALTH::set,
                null));
        entries.add(new DoubleEntry(Component.translatable("config.saintsdragons.attributes.draconian_swarm.latcher.armor"),
                ForgeDragonAttributesConfig.SWARM_LATCHER_ARMOR::get,
                ForgeDragonAttributesConfig.SWARM_LATCHER_ARMOR::set,
                null));
        entries.add(new DoubleEntry(Component.translatable("config.saintsdragons.attributes.draconian_swarm.latcher.chase_speed"),
                ForgeDragonAttributesConfig.SWARM_LATCHER_CHASE_SPEED::get,
                ForgeDragonAttributesConfig.SWARM_LATCHER_CHASE_SPEED::set,
                null));
        entries.add(new DoubleEntry(Component.translatable("config.saintsdragons.attributes.draconian_swarm.latcher.bite"),
                ForgeDragonAttributesConfig.SWARM_LATCHER_BITE_DAMAGE::get,
                ForgeDragonAttributesConfig.SWARM_LATCHER_BITE_DAMAGE::set,
                null));
        entries.add(new SectionEntry(Component.translatable("config.saintsdragons.attributes.draconian_swarm.winged")));
        entries.add(new DoubleEntry(Component.translatable("config.saintsdragons.attributes.draconian_swarm.winged.max_health"),
                ForgeDragonAttributesConfig.SWARM_WINGED_MAX_HEALTH::get,
                ForgeDragonAttributesConfig.SWARM_WINGED_MAX_HEALTH::set,
                null));
        entries.add(new DoubleEntry(Component.translatable("config.saintsdragons.attributes.draconian_swarm.winged.armor"),
                ForgeDragonAttributesConfig.SWARM_WINGED_ARMOR::get,
                ForgeDragonAttributesConfig.SWARM_WINGED_ARMOR::set,
                null));
        entries.add(new DoubleEntry(Component.translatable("config.saintsdragons.attributes.draconian_swarm.winged.chase_speed"),
                ForgeDragonAttributesConfig.SWARM_WINGED_CHASE_SPEED::get,
                ForgeDragonAttributesConfig.SWARM_WINGED_CHASE_SPEED::set,
                null));
        entries.add(new DoubleEntry(Component.translatable("config.saintsdragons.attributes.draconian_swarm.winged.attack"),
                ForgeDragonAttributesConfig.SWARM_WINGED_HOOK_AND_PULL_DAMAGE::get,
                ForgeDragonAttributesConfig.SWARM_WINGED_HOOK_AND_PULL_DAMAGE::set,
                null));
        entries.add(new DoubleEntry(Component.translatable("config.saintsdragons.attributes.draconian_swarm.winged.attack2"),
                ForgeDragonAttributesConfig.SWARM_WINGED_DIVE_BOMB_DAMAGE::get,
                ForgeDragonAttributesConfig.SWARM_WINGED_DIVE_BOMB_DAMAGE::set,
                null));
        entries.add(new SectionEntry(Component.translatable("config.saintsdragons.attributes.draconian_swarm.whettled")));
        entries.add(new DoubleEntry(Component.translatable("config.saintsdragons.attributes.draconian_swarm.whettled.max_health"),
                ForgeDragonAttributesConfig.SWARM_WHETTLED_MAX_HEALTH::get,
                ForgeDragonAttributesConfig.SWARM_WHETTLED_MAX_HEALTH::set,
                null));
        entries.add(new DoubleEntry(Component.translatable("config.saintsdragons.attributes.draconian_swarm.whettled.armor"),
                ForgeDragonAttributesConfig.SWARM_WHETTLED_ARMOR::get,
                ForgeDragonAttributesConfig.SWARM_WHETTLED_ARMOR::set,
                null));
        entries.add(new DoubleEntry(Component.translatable("config.saintsdragons.attributes.draconian_swarm.whettled.chase_speed"),
                ForgeDragonAttributesConfig.SWARM_WHETTLED_CHASE_SPEED::get,
                ForgeDragonAttributesConfig.SWARM_WHETTLED_CHASE_SPEED::set,
                null));
        entries.add(new DoubleEntry(Component.translatable("config.saintsdragons.attributes.draconian_swarm.whettled.clawattack"),
                ForgeDragonAttributesConfig.SWARM_WHETTLED_CLAW_ATTACK_DAMAGE::get,
                ForgeDragonAttributesConfig.SWARM_WHETTLED_CLAW_ATTACK_DAMAGE::set,
                null));
        entries.add(new DoubleEntry(Component.translatable("config.saintsdragons.attributes.draconian_swarm.whettled.movehornattack"),
                ForgeDragonAttributesConfig.SWARM_WHETTLED_LUNGE_DAMAGE::get,
                ForgeDragonAttributesConfig.SWARM_WHETTLED_LUNGE_DAMAGE::set,
                null));
    }

    private void addVolitansEntries(List<ConfigEntry> entries) {
        entries.add(new SectionEntry(Component.translatable("config.saintsdragons.attributes.volitans")));
        entries.add(new DoubleEntry(Component.translatable("config.saintsdragons.attributes.volitans.max_health"),
                ForgeDragonAttributesConfig.VOLITANS_MAX_HEALTH::get,
                ForgeDragonAttributesConfig.VOLITANS_MAX_HEALTH::set,
                null));
        entries.add(new WarningEntry(Component.translatable("config.saintsdragons.attributes.taming_stun_health.warning")));
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
        entries.add(new DoubleEntry(Component.translatable("config.saintsdragons.attributes.volitans.egg_hatch_time_ticks_normal"),
                ForgeDragonAttributesConfig.VOLITANS_EGG_HATCH_CHANCE_NORMAL::get,
                ForgeDragonAttributesConfig.VOLITANS_EGG_HATCH_CHANCE_NORMAL::set,
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

    private static int clampSwarmWaveCount(double value) {
        return DragonAttributeConfigLoader.clampSwarmWaveCount((int) Math.round(value));
    }

    private void resetSection() {
        switch (section) {
            case CINDERVANE -> {
                ForgeDragonAttributesConfig.CINDERVANE_MAX_HEALTH.set(ForgeDragonAttributesConfig.CINDERVANE_MAX_HEALTH.getDefault());
                ForgeDragonAttributesConfig.CINDERVANE_ARMOR.set(ForgeDragonAttributesConfig.CINDERVANE_ARMOR.getDefault());
                ForgeDragonAttributesConfig.CINDERVANE_FLYING_SPEED.set(ForgeDragonAttributesConfig.CINDERVANE_FLYING_SPEED.getDefault());
                ForgeDragonAttributesConfig.CINDERVANE_WILD_FLYING_SPEED_MULTIPLIER.set(ForgeDragonAttributesConfig.CINDERVANE_WILD_FLYING_SPEED_MULTIPLIER.getDefault());
                ForgeDragonAttributesConfig.CINDERVANE_BITE_DAMAGE.set(ForgeDragonAttributesConfig.CINDERVANE_BITE_DAMAGE.getDefault());
                ForgeDragonAttributesConfig.CINDERVANE_DOUBLE_BITE_DAMAGE.set(ForgeDragonAttributesConfig.CINDERVANE_DOUBLE_BITE_DAMAGE.getDefault());
                ForgeDragonAttributesConfig.CINDERVANE_SLASH_GRAB_HIT1_DAMAGE.set(ForgeDragonAttributesConfig.CINDERVANE_SLASH_GRAB_HIT1_DAMAGE.getDefault());
                ForgeDragonAttributesConfig.CINDERVANE_SLASH_GRAB_HIT2_DAMAGE.set(ForgeDragonAttributesConfig.CINDERVANE_SLASH_GRAB_HIT2_DAMAGE.getDefault());
                ForgeDragonAttributesConfig.CINDERVANE_MAGMA_VOLLEY_DAMAGE.set(ForgeDragonAttributesConfig.CINDERVANE_MAGMA_VOLLEY_DAMAGE.getDefault());
                ForgeDragonAttributesConfig.CINDERVANE_FIRE_BODY_DAMAGE.set(ForgeDragonAttributesConfig.CINDERVANE_FIRE_BODY_DAMAGE.getDefault());
                ForgeDragonAttributesConfig.CINDERVANE_TAMING_CHANCE_BASE.set(ForgeDragonAttributesConfig.CINDERVANE_TAMING_CHANCE_BASE.getDefault());
                ForgeDragonAttributesConfig.CINDERVANE_TAMING_CHANCE_CHICKEN.set(ForgeDragonAttributesConfig.CINDERVANE_TAMING_CHANCE_CHICKEN.getDefault());
                ForgeDragonAttributesConfig.CINDERVANE_TAMING_CHANCE_HEARTY.set(ForgeDragonAttributesConfig.CINDERVANE_TAMING_CHANCE_HEARTY.getDefault());
                ForgeDragonAttributesConfig.CINDERVANE_EGG_HATCH_CHANCE_NORMAL.set(ForgeDragonAttributesConfig.CINDERVANE_EGG_HATCH_CHANCE_NORMAL.getDefault());
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
                ForgeDragonAttributesConfig.STEGONAUT_GROUND_SLAM_DAMAGE.set(ForgeDragonAttributesConfig.STEGONAUT_GROUND_SLAM_DAMAGE.getDefault());
                ForgeDragonAttributesConfig.STEGONAUT_GROUND_SLAM_KNOCKBACK.set(ForgeDragonAttributesConfig.STEGONAUT_GROUND_SLAM_KNOCKBACK.getDefault());
                ForgeDragonAttributesConfig.STEGONAUT_GROUND_SLAM2_DAMAGE.set(ForgeDragonAttributesConfig.STEGONAUT_GROUND_SLAM2_DAMAGE.getDefault());
                ForgeDragonAttributesConfig.STEGONAUT_GROUND_SLAM2_KNOCKBACK.set(ForgeDragonAttributesConfig.STEGONAUT_GROUND_SLAM2_KNOCKBACK.getDefault());
                ForgeDragonAttributesConfig.STEGONAUT_GROUND_SLAM_PILLAR_DAMAGE.set(ForgeDragonAttributesConfig.STEGONAUT_GROUND_SLAM_PILLAR_DAMAGE.getDefault());
                ForgeDragonAttributesConfig.STEGONAUT_GROUND_SLAM_PILLAR_KNOCKBACK.set(ForgeDragonAttributesConfig.STEGONAUT_GROUND_SLAM_PILLAR_KNOCKBACK.getDefault());
                ForgeDragonAttributesConfig.STEGONAUT_TAMING_CHANCE_BASE.set(ForgeDragonAttributesConfig.STEGONAUT_TAMING_CHANCE_BASE.getDefault());
                ForgeDragonAttributesConfig.STEGONAUT_TAMING_CHANCE_HEARTY.set(ForgeDragonAttributesConfig.STEGONAUT_TAMING_CHANCE_HEARTY.getDefault());
                ForgeDragonAttributesConfig.STEGONAUT_EGG_HATCH_CHANCE_NORMAL.set(ForgeDragonAttributesConfig.STEGONAUT_EGG_HATCH_CHANCE_NORMAL.getDefault());
                ForgeDragonAttributesConfig.STEGONAUT_AGGRESSIVE_WILD.set(ForgeDragonAttributesConfig.STEGONAUT_AGGRESSIVE_WILD.getDefault());
            }
            case RAEVYX -> {
                ForgeDragonAttributesConfig.RAEVYX_MAX_HEALTH.set(ForgeDragonAttributesConfig.RAEVYX_MAX_HEALTH.getDefault());
                ForgeDragonAttributesConfig.RAEVYX_ARMOR.set(ForgeDragonAttributesConfig.RAEVYX_ARMOR.getDefault());
                ForgeDragonAttributesConfig.RAEVYX_FLYING_SPEED.set(ForgeDragonAttributesConfig.RAEVYX_FLYING_SPEED.getDefault());
                ForgeDragonAttributesConfig.RAEVYX_DIVE_LOOP_ENABLED.set(ForgeDragonAttributesConfig.RAEVYX_DIVE_LOOP_ENABLED.getDefault());
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
                ForgeDragonAttributesConfig.RAEVYX_TAMING_CHANCE_MUTTON.set(ForgeDragonAttributesConfig.RAEVYX_TAMING_CHANCE_MUTTON.getDefault());
                ForgeDragonAttributesConfig.RAEVYX_TAMING_CHANCE_PORKCHOP.set(ForgeDragonAttributesConfig.RAEVYX_TAMING_CHANCE_PORKCHOP.getDefault());
                ForgeDragonAttributesConfig.RAEVYX_TAMING_CHANCE_HEARTY.set(ForgeDragonAttributesConfig.RAEVYX_TAMING_CHANCE_HEARTY.getDefault());
                ForgeDragonAttributesConfig.RAEVYX_LEGACY_TAMING.set(ForgeDragonAttributesConfig.RAEVYX_LEGACY_TAMING.getDefault());
                ForgeDragonAttributesConfig.RAEVYX_EGG_HATCH_TIME_TICKS_NORMAL.set(ForgeDragonAttributesConfig.RAEVYX_EGG_HATCH_TIME_TICKS_NORMAL.getDefault());
                ForgeDragonAttributesConfig.RAEVYX_EGG_HATCH_TIME_TICKS_THUNDER.set(ForgeDragonAttributesConfig.RAEVYX_EGG_HATCH_TIME_TICKS_THUNDER.getDefault());
                ForgeDragonAttributesConfig.RAEVYX_AGGRESSIVE_WILD.set(ForgeDragonAttributesConfig.RAEVYX_AGGRESSIVE_WILD.getDefault());
            }
            case VARASUCHUS -> {
                ForgeDragonAttributesConfig.VARASUCHUS_MAX_HEALTH.set(ForgeDragonAttributesConfig.VARASUCHUS_MAX_HEALTH.getDefault());
                ForgeDragonAttributesConfig.VARASUCHUS_ARMOR.set(ForgeDragonAttributesConfig.VARASUCHUS_ARMOR.getDefault());
                ForgeDragonAttributesConfig.VARASUCHUS_SWIM_SPEED.set(ForgeDragonAttributesConfig.VARASUCHUS_SWIM_SPEED.getDefault());
                ForgeDragonAttributesConfig.VARASUCHUS_BITE_PHASE1_DAMAGE.set(ForgeDragonAttributesConfig.VARASUCHUS_BITE_PHASE1_DAMAGE.getDefault());
                ForgeDragonAttributesConfig.VARASUCHUS_BITE_PHASE2_DAMAGE.set(ForgeDragonAttributesConfig.VARASUCHUS_BITE_PHASE2_DAMAGE.getDefault());
                ForgeDragonAttributesConfig.VARASUCHUS_TAIL_ATTACK_DAMAGE.set(ForgeDragonAttributesConfig.VARASUCHUS_TAIL_ATTACK_DAMAGE.getDefault());
                ForgeDragonAttributesConfig.VARASUCHUS_TAILGUARD_PARRY_DAMAGE.set(ForgeDragonAttributesConfig.VARASUCHUS_TAILGUARD_PARRY_DAMAGE.getDefault());
                ForgeDragonAttributesConfig.VARASUCHUS_DASH_TAIL_SWIPE_DAMAGE.set(ForgeDragonAttributesConfig.VARASUCHUS_DASH_TAIL_SWIPE_DAMAGE.getDefault());
                ForgeDragonAttributesConfig.VARASUCHUS_DASH_CLAW_DAMAGE.set(ForgeDragonAttributesConfig.VARASUCHUS_DASH_CLAW_DAMAGE.getDefault());
                ForgeDragonAttributesConfig.VARASUCHUS_CLAW_ATTACK_DAMAGE.set(ForgeDragonAttributesConfig.VARASUCHUS_CLAW_ATTACK_DAMAGE.getDefault());
                ForgeDragonAttributesConfig.VARASUCHUS_HORN_GORE_PHASE1_DAMAGE.set(ForgeDragonAttributesConfig.VARASUCHUS_HORN_GORE_PHASE1_DAMAGE.getDefault());
                ForgeDragonAttributesConfig.VARASUCHUS_HORN_GORE_PHASE2_DAMAGE.set(ForgeDragonAttributesConfig.VARASUCHUS_HORN_GORE_PHASE2_DAMAGE.getDefault());
                ForgeDragonAttributesConfig.VARASUCHUS_TAMING_CHANCE.set(ForgeDragonAttributesConfig.VARASUCHUS_TAMING_CHANCE.getDefault());
                ForgeDragonAttributesConfig.VARASUCHUS_TAMING_CHANCE_BEEF.set(ForgeDragonAttributesConfig.VARASUCHUS_TAMING_CHANCE_BEEF.getDefault());
                ForgeDragonAttributesConfig.VARASUCHUS_TAMING_CHANCE_TROPICAL.set(ForgeDragonAttributesConfig.VARASUCHUS_TAMING_CHANCE_TROPICAL.getDefault());
                ForgeDragonAttributesConfig.VARASUCHUS_LEGACY_TAMING.set(ForgeDragonAttributesConfig.VARASUCHUS_LEGACY_TAMING.getDefault());
                ForgeDragonAttributesConfig.VARASUCHUS_EGG_HATCH_CHANCE_NORMAL.set(ForgeDragonAttributesConfig.VARASUCHUS_EGG_HATCH_CHANCE_NORMAL.getDefault());
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
                ForgeDragonAttributesConfig.IGNIVORUS_ULTIMATE_TRIGGER_HEALTH_FRACTION.set(ForgeDragonAttributesConfig.IGNIVORUS_ULTIMATE_TRIGGER_HEALTH_FRACTION.getDefault());
                ForgeDragonAttributesConfig.IGNIVORUS_FIRE_BREATH_DRAIN_PER_TICK.set(ForgeDragonAttributesConfig.IGNIVORUS_FIRE_BREATH_DRAIN_PER_TICK.getDefault());
                ForgeDragonAttributesConfig.IGNIVORUS_FIRE_BREATH_REGEN_PER_TICK.set(ForgeDragonAttributesConfig.IGNIVORUS_FIRE_BREATH_REGEN_PER_TICK.getDefault());
                ForgeDragonAttributesConfig.IGNIVORUS_FIRE_BREATH_FLAME_SPAWN_MULTIPLIER.set(ForgeDragonAttributesConfig.IGNIVORUS_FIRE_BREATH_FLAME_SPAWN_MULTIPLIER.getDefault());
                ForgeDragonAttributesConfig.IGNIVORUS_FIRE_BREATH_FLAME_SPEED_MULTIPLIER.set(ForgeDragonAttributesConfig.IGNIVORUS_FIRE_BREATH_FLAME_SPEED_MULTIPLIER.getDefault());
                ForgeDragonAttributesConfig.IGNIVORUS_FIRE_BREATH_FLAME_LIFETIME_MULTIPLIER.set(ForgeDragonAttributesConfig.IGNIVORUS_FIRE_BREATH_FLAME_LIFETIME_MULTIPLIER.getDefault());
                ForgeDragonAttributesConfig.IGNIVORUS_FIRE_BREATH_IGNITE_BLOCK_CHANCE.set(ForgeDragonAttributesConfig.IGNIVORUS_FIRE_BREATH_IGNITE_BLOCK_CHANCE.getDefault());
                ForgeDragonAttributesConfig.IGNIVORUS_TAMING_CHANCE_BASE.set(ForgeDragonAttributesConfig.IGNIVORUS_TAMING_CHANCE_BASE.getDefault());
                ForgeDragonAttributesConfig.IGNIVORUS_TAMING_CHANCE_BEEF.set(ForgeDragonAttributesConfig.IGNIVORUS_TAMING_CHANCE_BEEF.getDefault());
                ForgeDragonAttributesConfig.IGNIVORUS_TAMING_CHANCE_MUTTON.set(ForgeDragonAttributesConfig.IGNIVORUS_TAMING_CHANCE_MUTTON.getDefault());
                ForgeDragonAttributesConfig.IGNIVORUS_TAMING_CHANCE_PORKCHOP.set(ForgeDragonAttributesConfig.IGNIVORUS_TAMING_CHANCE_PORKCHOP.getDefault());
                ForgeDragonAttributesConfig.IGNIVORUS_TAMING_CHANCE_HEARTY.set(ForgeDragonAttributesConfig.IGNIVORUS_TAMING_CHANCE_HEARTY.getDefault());
                ForgeDragonAttributesConfig.IGNIVORUS_TAMING_STUN_HEALTH.set(ForgeDragonAttributesConfig.IGNIVORUS_TAMING_STUN_HEALTH.getDefault());
                ForgeDragonAttributesConfig.IGNIVORUS_LEGACY_TAMING.set(ForgeDragonAttributesConfig.IGNIVORUS_LEGACY_TAMING.getDefault());
                ForgeDragonAttributesConfig.IGNIVORUS_EGG_HATCH_CHANCE_NORMAL.set(ForgeDragonAttributesConfig.IGNIVORUS_EGG_HATCH_CHANCE_NORMAL.getDefault());
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
            case ATROXIIA -> {
                ForgeDragonAttributesConfig.ATROXIIA_MAX_HEALTH.set(ForgeDragonAttributesConfig.ATROXIIA_MAX_HEALTH.getDefault());
                ForgeDragonAttributesConfig.ATROXIIA_ARMOR.set(ForgeDragonAttributesConfig.ATROXIIA_ARMOR.getDefault());
                ForgeDragonAttributesConfig.ATROXIIA_SLAM_DAMAGE.set(ForgeDragonAttributesConfig.ATROXIIA_SLAM_DAMAGE.getDefault());
                ForgeDragonAttributesConfig.ATROXIIA_SWIPE_DAMAGE.set(ForgeDragonAttributesConfig.ATROXIIA_SWIPE_DAMAGE.getDefault());
                ForgeDragonAttributesConfig.ATROXIIA_UNDERWATER_BITE_DAMAGE.set(ForgeDragonAttributesConfig.ATROXIIA_UNDERWATER_BITE_DAMAGE.getDefault());
                ForgeDragonAttributesConfig.ATROXIIA_GUNGNIR_STAB_DAMAGE.set(ForgeDragonAttributesConfig.ATROXIIA_GUNGNIR_STAB_DAMAGE.getDefault());
                ForgeDragonAttributesConfig.ATROXIIA_PRECISE_STRIKE_DAMAGE.set(ForgeDragonAttributesConfig.ATROXIIA_PRECISE_STRIKE_DAMAGE.getDefault());
                ForgeDragonAttributesConfig.ATROXIIA_PRECISE_STRIKE_KNOCKBACK.set(ForgeDragonAttributesConfig.ATROXIIA_PRECISE_STRIKE_KNOCKBACK.getDefault());
                ForgeDragonAttributesConfig.ATROXIIA_PRECISE_STRIKE_STUN_DURATION_TICKS.set(ForgeDragonAttributesConfig.ATROXIIA_PRECISE_STRIKE_STUN_DURATION_TICKS.getDefault());
                ForgeDragonAttributesConfig.ATROXIIA_DEVASTATING_SWEEP_DAMAGE.set(ForgeDragonAttributesConfig.ATROXIIA_DEVASTATING_SWEEP_DAMAGE.getDefault());
                ForgeDragonAttributesConfig.ATROXIIA_DEVASTATING_SWEEP_KNOCKBACK.set(ForgeDragonAttributesConfig.ATROXIIA_DEVASTATING_SWEEP_KNOCKBACK.getDefault());
                ForgeDragonAttributesConfig.ATROXIIA_HELHEIM_QUAKE_DAMAGE.set(ForgeDragonAttributesConfig.ATROXIIA_HELHEIM_QUAKE_DAMAGE.getDefault());
                ForgeDragonAttributesConfig.ATROXIIA_HELHEIM_QUAKE_KNOCKBACK.set(ForgeDragonAttributesConfig.ATROXIIA_HELHEIM_QUAKE_KNOCKBACK.getDefault());
                ForgeDragonAttributesConfig.ATROXIIA_HELHEIM_QUAKE_SECONDARY_KNOCKBACK.set(ForgeDragonAttributesConfig.ATROXIIA_HELHEIM_QUAKE_SECONDARY_KNOCKBACK.getDefault());
                ForgeDragonAttributesConfig.ATROXIIA_HELHEIM_QUAKE_STUN_DURATION_TICKS.set(ForgeDragonAttributesConfig.ATROXIIA_HELHEIM_QUAKE_STUN_DURATION_TICKS.getDefault());
                ForgeDragonAttributesConfig.ATROXIIA_EGG_HATCH_TIME_TICKS_NORMAL.set(ForgeDragonAttributesConfig.ATROXIIA_EGG_HATCH_TIME_TICKS_NORMAL.getDefault());
                ForgeDragonAttributesConfig.ATROXIIA_FROST_IMPACT_ENABLED.set(ForgeDragonAttributesConfig.ATROXIIA_FROST_IMPACT_ENABLED.getDefault());
                ForgeDragonAttributesConfig.ATROXIIA_AGGRESSIVE_WILD.set(ForgeDragonAttributesConfig.ATROXIIA_AGGRESSIVE_WILD.getDefault());
            }
            case DRACONIAN_SWARM -> {
                ForgeDragonAttributesConfig.SWARM_WAVE_1_COUNT.set(ForgeDragonAttributesConfig.SWARM_WAVE_1_COUNT.getDefault());
                ForgeDragonAttributesConfig.SWARM_WAVE_2_COUNT.set(ForgeDragonAttributesConfig.SWARM_WAVE_2_COUNT.getDefault());
                ForgeDragonAttributesConfig.SWARM_WAVE_3_COUNT.set(ForgeDragonAttributesConfig.SWARM_WAVE_3_COUNT.getDefault());
                ForgeDragonAttributesConfig.SWARM_LATCHER_MAX_HEALTH.set(ForgeDragonAttributesConfig.SWARM_LATCHER_MAX_HEALTH.getDefault());
                ForgeDragonAttributesConfig.SWARM_LATCHER_ARMOR.set(ForgeDragonAttributesConfig.SWARM_LATCHER_ARMOR.getDefault());
                ForgeDragonAttributesConfig.SWARM_LATCHER_CHASE_SPEED.set(ForgeDragonAttributesConfig.SWARM_LATCHER_CHASE_SPEED.getDefault());
                ForgeDragonAttributesConfig.SWARM_LATCHER_BITE_DAMAGE.set(ForgeDragonAttributesConfig.SWARM_LATCHER_BITE_DAMAGE.getDefault());
                ForgeDragonAttributesConfig.SWARM_WINGED_MAX_HEALTH.set(ForgeDragonAttributesConfig.SWARM_WINGED_MAX_HEALTH.getDefault());
                ForgeDragonAttributesConfig.SWARM_WINGED_ARMOR.set(ForgeDragonAttributesConfig.SWARM_WINGED_ARMOR.getDefault());
                ForgeDragonAttributesConfig.SWARM_WINGED_CHASE_SPEED.set(ForgeDragonAttributesConfig.SWARM_WINGED_CHASE_SPEED.getDefault());
                ForgeDragonAttributesConfig.SWARM_WINGED_HOOK_AND_PULL_DAMAGE.set(ForgeDragonAttributesConfig.SWARM_WINGED_HOOK_AND_PULL_DAMAGE.getDefault());
                ForgeDragonAttributesConfig.SWARM_WINGED_DIVE_BOMB_DAMAGE.set(ForgeDragonAttributesConfig.SWARM_WINGED_DIVE_BOMB_DAMAGE.getDefault());
                ForgeDragonAttributesConfig.SWARM_WHETTLED_MAX_HEALTH.set(ForgeDragonAttributesConfig.SWARM_WHETTLED_MAX_HEALTH.getDefault());
                ForgeDragonAttributesConfig.SWARM_WHETTLED_ARMOR.set(ForgeDragonAttributesConfig.SWARM_WHETTLED_ARMOR.getDefault());
                ForgeDragonAttributesConfig.SWARM_WHETTLED_CHASE_SPEED.set(ForgeDragonAttributesConfig.SWARM_WHETTLED_CHASE_SPEED.getDefault());
                ForgeDragonAttributesConfig.SWARM_WHETTLED_CLAW_ATTACK_DAMAGE.set(ForgeDragonAttributesConfig.SWARM_WHETTLED_CLAW_ATTACK_DAMAGE.getDefault());
                ForgeDragonAttributesConfig.SWARM_WHETTLED_LUNGE_DAMAGE.set(ForgeDragonAttributesConfig.SWARM_WHETTLED_LUNGE_DAMAGE.getDefault());
            }
        }

        ForgeDragonAttributesConfig.ATTRIBUTES_SPEC.save();
        DragonAttributeConfigLoader.getInstance().refreshFromForgeConfig();
        applyAttributesToLoadedDragons();
    }
}

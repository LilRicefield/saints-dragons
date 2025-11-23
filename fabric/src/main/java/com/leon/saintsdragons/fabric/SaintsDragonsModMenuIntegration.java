package com.leon.saintsdragons.fabric;

import com.leon.saintsdragons.common.config.dragon.DragonAbilityOverride;
import com.leon.saintsdragons.common.config.dragon.DragonAttributeConfig;
import com.leon.saintsdragons.common.config.dragon.DragonAttributeConfigLoader;
import com.leon.saintsdragons.fabric.config.SaintsDragonsFabricConfig;
import com.terraformersmc.modmenu.api.ConfigScreenFactory;
import com.terraformersmc.modmenu.api.ModMenuApi;
import me.shedaniel.autoconfig.AutoConfig;
import me.shedaniel.autoconfig.ConfigHolder;
import me.shedaniel.clothconfig2.api.AbstractConfigListEntry;
import me.shedaniel.clothconfig2.api.ConfigBuilder;
import me.shedaniel.clothconfig2.api.ConfigCategory;
import me.shedaniel.clothconfig2.api.ConfigEntryBuilder;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;
import java.util.function.IntConsumer;
import java.util.function.IntSupplier;
import java.util.function.Supplier;

/**
 * Custom ModMenu screen wiring spawn tuning and dragon attribute sliders through Cloth Config.
 */
@Environment(EnvType.CLIENT)
public class SaintsDragonsModMenuIntegration implements ModMenuApi {
    private static final Component TITLE = Component.translatable("config.saintsdragons.title");
    private static final Component SPAWN_CATEGORY = Component.translatable("config.saintsdragons.category.spawning");
    private static final Component ATTRIBUTES_CATEGORY = Component.translatable("config.saintsdragons.category.attributes");

    @Override
    public ConfigScreenFactory<?> getModConfigScreenFactory() {
        return this::createScreen;
    }

    private Screen createScreen(Screen parent) {
        ConfigHolder<SaintsDragonsFabricConfig> holder = AutoConfig.getConfigHolder(SaintsDragonsFabricConfig.class);
        SaintsDragonsFabricConfig config = holder.getConfig();

        DragonAttributeConfigLoader loader = DragonAttributeConfigLoader.getInstance();
        DragonAttributeConfig cindervaneCurrent = loader.getConfig(DragonAttributeConfigLoader.CINDERVANE_ID);
        DragonAttributeConfig cindervaneDefaults = loader.getDefaultConfig(DragonAttributeConfigLoader.CINDERVANE_ID);
        CindervaneAttributeBuffer cindervaneBuffer = new CindervaneAttributeBuffer();
        cindervaneBuffer.maxHealth = cindervaneCurrent.maxHealth();
        cindervaneBuffer.armor = cindervaneCurrent.armor();
        cindervaneBuffer.movementSpeed = cindervaneCurrent.movementSpeed();
        cindervaneBuffer.flyingSpeed = cindervaneCurrent.flyingSpeed();
        cindervaneBuffer.biteDamage = cindervaneCurrent.abilityDamage("bite",
                cindervaneDefaults.abilityDamage("bite", 12.0D));
        cindervaneBuffer.volleyDamage = cindervaneCurrent.abilityDamage("magma_volley",
                cindervaneDefaults.abilityDamage("magma_volley", 20.0D));

        DragonAttributeConfig raevyxCurrent = loader.getConfig(DragonAttributeConfigLoader.RAEVYX_ID);
        DragonAttributeConfig raevyxDefaults = loader.getDefaultConfig(DragonAttributeConfigLoader.RAEVYX_ID);
        RaevyxAttributeBuffer raevyxBuffer = new RaevyxAttributeBuffer();
        raevyxBuffer.maxHealth = raevyxCurrent.maxHealth();
        raevyxBuffer.armor = raevyxCurrent.armor();
        raevyxBuffer.movementSpeed = raevyxCurrent.movementSpeed();
        raevyxBuffer.flyingSpeed = raevyxCurrent.flyingSpeed();
        raevyxBuffer.biteDamage = raevyxCurrent.abilityDamage("bite",
                raevyxDefaults.abilityDamage("bite", 15.0D));
        raevyxBuffer.beamDamage = raevyxCurrent.abilityDamage("lightning_beam",
                raevyxDefaults.abilityDamage("lightning_beam", 35.0D));
        raevyxBuffer.hornDamage = raevyxCurrent.abilityDamage("horn_gore",
                raevyxDefaults.abilityDamage("horn_gore", 15.0D));
        raevyxBuffer.tamingChanceBase = raevyxCurrent.extraDouble("taming_chance_base", 5.0);
        raevyxBuffer.tamingChanceHearty = raevyxCurrent.extraDouble("taming_chance_hearty", 3.0);

        DragonAttributeConfig nulljawCurrent = loader.getConfig(DragonAttributeConfigLoader.NULLJAW_ID);
        DragonAttributeConfig nulljawDefaults = loader.getDefaultConfig(DragonAttributeConfigLoader.NULLJAW_ID);
        NulljawAttributeBuffer nulljawBuffer = new NulljawAttributeBuffer();
        nulljawBuffer.maxHealth = nulljawCurrent.maxHealth();
        nulljawBuffer.armor = nulljawCurrent.armor();
        nulljawBuffer.runSpeed = nulljawCurrent.movementSpeed();
        nulljawBuffer.walkSpeed = nulljawCurrent.extraDouble("walk_speed", nulljawBuffer.runSpeed * 0.5D);
        nulljawBuffer.swimSpeed = nulljawCurrent.extraDouble("swim_speed", 1.45D);
        nulljawBuffer.bitePhase1 = nulljawCurrent.abilityDamage("bite_phase1", 40.0D);
        nulljawBuffer.bitePhase2 = nulljawCurrent.abilityDamage("bite_phase2", 50.0D);
        nulljawBuffer.hornPhase1 = nulljawCurrent.abilityDamage("horn_gore_phase1", 16.0D);
        nulljawBuffer.hornPhase2 = nulljawCurrent.abilityDamage("horn_gore_phase2", 20.8D);

        DragonAttributeConfig ignivorusCurrent = loader.getConfig(DragonAttributeConfigLoader.IGNIVORUS_ID);
        DragonAttributeConfig ignivorusDefaults = loader.getDefaultConfig(DragonAttributeConfigLoader.IGNIVORUS_ID);
        IgnivorusAttributeBuffer ignivorusBuffer = new IgnivorusAttributeBuffer();
        ignivorusBuffer.maxHealth = ignivorusCurrent.maxHealth();
        ignivorusBuffer.armor = ignivorusCurrent.armor();
        ignivorusBuffer.movementSpeed = ignivorusCurrent.movementSpeed();
        double ignivorusDefaultWalk = ignivorusDefaults.extraDouble("walk_speed",
                ignivorusDefaults.movementSpeed() * 0.8D);
        ignivorusBuffer.walkSpeed = ignivorusCurrent.extraDouble("walk_speed", ignivorusDefaultWalk);
        ignivorusBuffer.flyingSpeed = ignivorusCurrent.flyingSpeed();
        ignivorusBuffer.baseDamage = ignivorusCurrent.extraDouble("attack_damage",
                ignivorusDefaults.extraDouble("attack_damage", 15.0D));
        ignivorusBuffer.biteDamage = ignivorusCurrent.abilityDamage("bite",
                ignivorusDefaults.abilityDamage("bite", 50.0D));
        ignivorusBuffer.bodySlamDamage = ignivorusCurrent.abilityDamage("body_slam",
                ignivorusDefaults.abilityDamage("body_slam", 40.0D));
        ignivorusBuffer.fireBreathDamage = ignivorusCurrent.abilityDamage("fire_breath",
                ignivorusDefaults.abilityDamage("fire_breath", 4.0D));
        ignivorusBuffer.ultimateDamage = ignivorusCurrent.abilityDamage("ultimate",
                ignivorusDefaults.abilityDamage("ultimate", 200.0D));
        ignivorusBuffer.ultimatePenalty = ignivorusCurrent.extraDouble("ultimate_penalty_health",
                ignivorusDefaults.extraDouble("ultimate_penalty_health", 50.0D));
        ignivorusBuffer.tamingChanceBase = ignivorusCurrent.extraDouble("taming_chance_base", 7.0);
        ignivorusBuffer.tamingChanceHearty = ignivorusCurrent.extraDouble("taming_chance_hearty", 4.0);
        raevyxBuffer.hornDamage = raevyxCurrent.abilityDamage("horn_gore",
                raevyxDefaults.abilityDamage("horn_gore", 15.0D));

        ConfigBuilder builder = ConfigBuilder.create()
                .setParentScreen(parent)
                .setTitle(TITLE);
        builder.setTransparentBackground(true);
        builder.setSavingRunnable(() -> {
            holder.save();
            persistDragonAttributes(cindervaneBuffer, raevyxBuffer, nulljawBuffer, ignivorusBuffer);
        });

        ConfigEntryBuilder entryBuilder = builder.entryBuilder();

        ConfigCategory spawning = builder.getOrCreateCategory(SPAWN_CATEGORY);
        addSpawnEntries(spawning, entryBuilder, Component.translatable("config.saintsdragons.spawn.raevyx"),
                () -> config.raevyxSpawnWeight, value -> config.raevyxSpawnWeight = value,
                () -> config.raevyxMinGroupSize, value -> config.raevyxMinGroupSize = value,
                () -> config.raevyxMaxGroupSize, value -> config.raevyxMaxGroupSize = value,
                () -> config.raevyxAdditionalBiomes, list -> {
                    config.raevyxAdditionalBiomes.clear();
                    config.raevyxAdditionalBiomes.addAll(list);
                },
                1, 1, 2);

        addSpawnEntries(spawning, entryBuilder, Component.translatable("config.saintsdragons.spawn.stegonaut"),
                () -> config.stegonautSpawnWeight, value -> config.stegonautSpawnWeight = value,
                () -> config.stegonautMinGroupSize, value -> config.stegonautMinGroupSize = value,
                () -> config.stegonautMaxGroupSize, value -> config.stegonautMaxGroupSize = value,
                () -> config.stegonautAdditionalBiomes, list -> {
                    config.stegonautAdditionalBiomes.clear();
                    config.stegonautAdditionalBiomes.addAll(list);
                },
                5, 1, 4);

        addSpawnEntries(spawning, entryBuilder, Component.translatable("config.saintsdragons.spawn.cindervane"),
                () -> config.cindervaneSpawnWeight, value -> config.cindervaneSpawnWeight = value,
                () -> config.cindervaneMinGroupSize, value -> config.cindervaneMinGroupSize = value,
                () -> config.cindervaneMaxGroupSize, value -> config.cindervaneMaxGroupSize = value,
                () -> config.cindervaneAdditionalBiomes, list -> {
                    config.cindervaneAdditionalBiomes.clear();
                    config.cindervaneAdditionalBiomes.addAll(list);
                },
                3, 1, 3);

        addSpawnEntries(spawning, entryBuilder, Component.translatable("config.saintsdragons.spawn.nulljaw"),
                () -> config.nulljawSpawnWeight, value -> config.nulljawSpawnWeight = value,
                () -> config.nulljawMinGroupSize, value -> config.nulljawMinGroupSize = value,
                () -> config.nulljawMaxGroupSize, value -> config.nulljawMaxGroupSize = value,
                () -> config.nulljawAdditionalBiomes, list -> {
                    config.nulljawAdditionalBiomes.clear();
                    config.nulljawAdditionalBiomes.addAll(list);
                },
                2, 1, 2);

        addSpawnEntries(spawning, entryBuilder, Component.translatable("config.saintsdragons.spawn.ignivorus"),
                () -> config.ignivorusSpawnWeight, value -> config.ignivorusSpawnWeight = value,
                () -> config.ignivorusMinGroupSize, value -> config.ignivorusMinGroupSize = value,
                () -> config.ignivorusMaxGroupSize, value -> config.ignivorusMaxGroupSize = value,
                () -> config.ignivorusAdditionalBiomes, list -> {
                    config.ignivorusAdditionalBiomes.clear();
                    config.ignivorusAdditionalBiomes.addAll(list);
                },
                1, 1, 2);

        ConfigCategory attributes = builder.getOrCreateCategory(ATTRIBUTES_CATEGORY);
        addCindervaneAttributes(attributes, entryBuilder, cindervaneBuffer, cindervaneDefaults);
        addRaevyxAttributes(attributes, entryBuilder, raevyxBuffer, raevyxDefaults);
        addNulljawAttributes(attributes, entryBuilder, nulljawBuffer, nulljawDefaults);
        addIgnivorusAttributes(attributes, entryBuilder, ignivorusBuffer, ignivorusDefaults);

        return builder.build();
    }

    private void addSpawnEntries(ConfigCategory category,
                                 ConfigEntryBuilder entryBuilder,
                                 Component label,
                                 IntSupplier weightGetter,
                                 IntConsumer weightSetter,
                                 IntSupplier minGetter,
                                 IntConsumer minSetter,
                                 IntSupplier maxGetter,
                                 IntConsumer maxSetter,
                                 Supplier<List<String>> biomesGetter,
                                 Consumer<List<String>> biomesSetter,
                                 int defaultWeight,
                                 int defaultMin,
                                 int defaultMax) {
        List<AbstractConfigListEntry<?>> entries = new ArrayList<>();
        entries.add(entryBuilder.startIntField(Component.translatable("config.saintsdragons.spawn.weight"), weightGetter.getAsInt())
                .setDefaultValue(defaultWeight)
                .setMin(0)
                .setMax(100)
                .setSaveConsumer(weightSetter::accept)
                .build());
        entries.add(entryBuilder.startIntField(Component.translatable("config.saintsdragons.spawn.min_group"), minGetter.getAsInt())
                .setDefaultValue(defaultMin)
                .setMin(1)
                .setMax(10)
                .setSaveConsumer(minSetter::accept)
                .build());
        entries.add(entryBuilder.startIntField(Component.translatable("config.saintsdragons.spawn.max_group"), maxGetter.getAsInt())
                .setDefaultValue(defaultMax)
                .setMin(1)
                .setMax(10)
                .setSaveConsumer(maxSetter::accept)
                .build());
        List<String> listCopy = new ArrayList<>(biomesGetter.get());
        entries.add(entryBuilder.startStrList(Component.translatable("config.saintsdragons.spawn.additional_biomes"), listCopy)
                .setDefaultValue(List.of())
                .setSaveConsumer(values -> biomesSetter.accept(new ArrayList<>(values)))
                .build());
        @SuppressWarnings({"rawtypes", "unchecked"})
        List<AbstractConfigListEntry> rawEntries = (List) entries;
        category.addEntry(entryBuilder.startSubCategory(label, rawEntries).setExpanded(false).build());
    }

    private void addCindervaneAttributes(ConfigCategory category,
                                         ConfigEntryBuilder entryBuilder,
                                         CindervaneAttributeBuffer buffer,
                                         DragonAttributeConfig defaults) {
        List<AbstractConfigListEntry<?>> entries = new ArrayList<>();
        entries.add(entryBuilder.startDoubleField(Component.translatable("config.saintsdragons.attributes.cindervane.max_health"), buffer.maxHealth)
                .setDefaultValue(defaults.maxHealth())
                .setMin(10.0D)
                .setMax(1000.0D)
                .setSaveConsumer(value -> buffer.maxHealth = value)
                .build());
        entries.add(entryBuilder.startDoubleField(Component.translatable("config.saintsdragons.attributes.cindervane.armor"), buffer.armor)
                .setDefaultValue(defaults.armor())
                .setMin(0.0D)
                .setMax(30.0D)
                .setSaveConsumer(value -> buffer.armor = value)
                .build());
        entries.add(entryBuilder.startDoubleField(Component.translatable("config.saintsdragons.attributes.cindervane.movement_speed"), buffer.movementSpeed)
                .setDefaultValue(defaults.movementSpeed())
                .setMin(0.05D)
                .setMax(1.5D)
                .setSaveConsumer(value -> buffer.movementSpeed = value)
                .build());
        entries.add(entryBuilder.startDoubleField(Component.translatable("config.saintsdragons.attributes.cindervane.flying_speed"), buffer.flyingSpeed)
                .setDefaultValue(defaults.flyingSpeed())
                .setMin(0.05D)
                .setMax(2.0D)
                .setSaveConsumer(value -> buffer.flyingSpeed = value)
                .build());
        entries.add(entryBuilder.startDoubleField(Component.translatable("config.saintsdragons.attributes.cindervane.bite_damage"), buffer.biteDamage)
                .setDefaultValue(defaults.abilityDamage("bite", 12.0D))
                .setMin(1.0D)
                .setMax(100.0D)
                .setSaveConsumer(value -> buffer.biteDamage = value)
                .build());
        entries.add(entryBuilder.startDoubleField(Component.translatable("config.saintsdragons.attributes.cindervane.volley_damage"), buffer.volleyDamage)
                .setDefaultValue(defaults.abilityDamage("magma_volley", 20.0D))
                .setMin(1.0D)
                .setMax(200.0D)
                .setSaveConsumer(value -> buffer.volleyDamage = value)
                .build());

        @SuppressWarnings({"rawtypes", "unchecked"})
        List<AbstractConfigListEntry> rawEntries = (List) entries;
        category.addEntry(entryBuilder.startSubCategory(Component.translatable("config.saintsdragons.attributes.cindervane"), rawEntries)
                .setExpanded(false)
                .build());
    }

    private void addRaevyxAttributes(ConfigCategory category,
                                     ConfigEntryBuilder entryBuilder,
                                     RaevyxAttributeBuffer buffer,
                                     DragonAttributeConfig defaults) {
        List<AbstractConfigListEntry<?>> entries = new ArrayList<>();
        entries.add(entryBuilder.startDoubleField(Component.translatable("config.saintsdragons.attributes.raevyx.max_health"), buffer.maxHealth)
                .setDefaultValue(defaults.maxHealth())
                .setMin(10.0D)
                .setMax(2000.0D)
                .setSaveConsumer(value -> buffer.maxHealth = value)
                .build());
        entries.add(entryBuilder.startDoubleField(Component.translatable("config.saintsdragons.attributes.raevyx.armor"), buffer.armor)
                .setDefaultValue(defaults.armor())
                .setMin(0.0D)
                .setMax(40.0D)
                .setSaveConsumer(value -> buffer.armor = value)
                .build());
        entries.add(entryBuilder.startDoubleField(Component.translatable("config.saintsdragons.attributes.raevyx.movement_speed"), buffer.movementSpeed)
                .setDefaultValue(defaults.movementSpeed())
                .setMin(0.05D)
                .setMax(1.5D)
                .setSaveConsumer(value -> buffer.movementSpeed = value)
                .build());
        entries.add(entryBuilder.startDoubleField(Component.translatable("config.saintsdragons.attributes.raevyx.flying_speed"), buffer.flyingSpeed)
                .setDefaultValue(defaults.flyingSpeed())
                .setMin(0.1D)
                .setMax(3.0D)
                .setSaveConsumer(value -> buffer.flyingSpeed = value)
                .build());
        entries.add(entryBuilder.startDoubleField(Component.translatable("config.saintsdragons.attributes.raevyx.bite_damage"), buffer.biteDamage)
                .setDefaultValue(defaults.abilityDamage("bite", 15.0D))
                .setMin(1.0D)
                .setMax(200.0D)
                .setSaveConsumer(value -> buffer.biteDamage = value)
                .build());
        entries.add(entryBuilder.startDoubleField(Component.translatable("config.saintsdragons.attributes.raevyx.beam_damage"), buffer.beamDamage)
                .setDefaultValue(defaults.abilityDamage("lightning_beam", 35.0D))
                .setMin(1.0D)
                .setMax(200.0D)
                .setSaveConsumer(value -> buffer.beamDamage = value)
                .build());
        entries.add(entryBuilder.startDoubleField(Component.translatable("config.saintsdragons.attributes.raevyx.horn_damage"), buffer.hornDamage)
                .setDefaultValue(defaults.abilityDamage("horn_gore", 15.0D))
                .setMin(1.0D)
                .setMax(200.0D)
                .setSaveConsumer(value -> buffer.hornDamage = value)
                .build());
        entries.add(entryBuilder.startDoubleField(Component.translatable("config.saintsdragons.attributes.raevyx.taming_base"), buffer.tamingChanceBase)
                .setDefaultValue(defaults.extraDouble("taming_chance_base", 5.0))
                .setMin(1.0D)
                .setMax(100.0D)
                .setSaveConsumer(value -> buffer.tamingChanceBase = value)
                .build());
        entries.add(entryBuilder.startDoubleField(Component.translatable("config.saintsdragons.attributes.raevyx.taming_hearty"), buffer.tamingChanceHearty)
                .setDefaultValue(defaults.extraDouble("taming_chance_hearty", 3.0))
                .setMin(1.0D)
                .setMax(100.0D)
                .setSaveConsumer(value -> buffer.tamingChanceHearty = value)
                .build());
        @SuppressWarnings({"rawtypes", "unchecked"})
        List<AbstractConfigListEntry> rawEntries = (List) entries;
        category.addEntry(entryBuilder.startSubCategory(Component.translatable("config.saintsdragons.attributes.raevyx"), rawEntries)
                .setExpanded(false)
                .build());
    }

    private void addNulljawAttributes(ConfigCategory category,
                                     ConfigEntryBuilder entryBuilder,
                                     NulljawAttributeBuffer buffer,
                                     DragonAttributeConfig defaults) {
        List<AbstractConfigListEntry<?>> entries = new ArrayList<>();
        entries.add(entryBuilder.startDoubleField(Component.translatable("config.saintsdragons.attributes.nulljaw.max_health"), buffer.maxHealth)
                .setDefaultValue(defaults.maxHealth())
                .setMin(50.0D)
                .setMax(5000.0D)
                .setSaveConsumer(value -> buffer.maxHealth = value)
                .build());
        entries.add(entryBuilder.startDoubleField(Component.translatable("config.saintsdragons.attributes.nulljaw.armor"), buffer.armor)
                .setDefaultValue(defaults.armor())
                .setMin(0.0D)
                .setMax(30.0D)
                .setSaveConsumer(value -> buffer.armor = value)
                .build());
        entries.add(entryBuilder.startDoubleField(Component.translatable("config.saintsdragons.attributes.nulljaw.run_speed"), buffer.runSpeed)
                .setDefaultValue(defaults.movementSpeed())
                .setMin(0.05D)
                .setMax(1.5D)
                .setSaveConsumer(value -> buffer.runSpeed = value)
                .build());
        entries.add(entryBuilder.startDoubleField(Component.translatable("config.saintsdragons.attributes.nulljaw.walk_speed"), buffer.walkSpeed)
                .setDefaultValue(defaults.extraDouble("walk_speed", defaults.movementSpeed() * 0.5D))
                .setMin(0.01D)
                .setMax(1.0D)
                .setSaveConsumer(value -> buffer.walkSpeed = value)
                .build());
        entries.add(entryBuilder.startDoubleField(Component.translatable("config.saintsdragons.attributes.nulljaw.swim_speed"), buffer.swimSpeed)
                .setDefaultValue(defaults.extraDouble("swim_speed", 1.45D))
                .setMin(0.1D)
                .setMax(5.0D)
                .setSaveConsumer(value -> buffer.swimSpeed = value)
                .build());
        entries.add(entryBuilder.startDoubleField(Component.translatable("config.saintsdragons.attributes.nulljaw.bite_phase1"), buffer.bitePhase1)
                .setDefaultValue(defaults.abilityDamage("bite_phase1", 40.0D))
                .setMin(1.0D)
                .setMax(500.0D)
                .setSaveConsumer(value -> buffer.bitePhase1 = value)
                .build());
        entries.add(entryBuilder.startDoubleField(Component.translatable("config.saintsdragons.attributes.nulljaw.bite_phase2"), buffer.bitePhase2)
                .setDefaultValue(defaults.abilityDamage("bite_phase2", 50.0D))
                .setMin(1.0D)
                .setMax(600.0D)
                .setSaveConsumer(value -> buffer.bitePhase2 = value)
                .build());
        entries.add(entryBuilder.startDoubleField(Component.translatable("config.saintsdragons.attributes.nulljaw.horn_phase1"), buffer.hornPhase1)
                .setDefaultValue(defaults.abilityDamage("horn_gore_phase1", 16.0D))
                .setMin(1.0D)
                .setMax(300.0D)
                .setSaveConsumer(value -> buffer.hornPhase1 = value)
                .build());
        entries.add(entryBuilder.startDoubleField(Component.translatable("config.saintsdragons.attributes.nulljaw.horn_phase2"), buffer.hornPhase2)
                .setDefaultValue(defaults.abilityDamage("horn_gore_phase2", 20.8D))
                .setMin(1.0D)
                .setMax(400.0D)
                .setSaveConsumer(value -> buffer.hornPhase2 = value)
                .build());

        @SuppressWarnings({"rawtypes", "unchecked"})
        List<AbstractConfigListEntry> rawEntries = (List) entries;
        category.addEntry(entryBuilder.startSubCategory(Component.translatable("config.saintsdragons.attributes.nulljaw"), rawEntries)
                .setExpanded(false)
                .build());
    }

    private void addIgnivorusAttributes(ConfigCategory category,
                                        ConfigEntryBuilder entryBuilder,
                                        IgnivorusAttributeBuffer buffer,
                                        DragonAttributeConfig defaults) {
        List<AbstractConfigListEntry<?>> entries = new ArrayList<>();
        entries.add(entryBuilder.startDoubleField(Component.translatable("config.saintsdragons.attributes.ignivorus.max_health"), buffer.maxHealth)
                .setDefaultValue(defaults.maxHealth())
                .setMin(50.0D)
                .setMax(2000.0D)
                .setSaveConsumer(value -> buffer.maxHealth = value)
                .build());
        entries.add(entryBuilder.startDoubleField(Component.translatable("config.saintsdragons.attributes.ignivorus.armor"), buffer.armor)
                .setDefaultValue(defaults.armor())
                .setMin(0.0D)
                .setMax(40.0D)
                .setSaveConsumer(value -> buffer.armor = value)
                .build());
        entries.add(entryBuilder.startDoubleField(Component.translatable("config.saintsdragons.attributes.ignivorus.walk_speed"), buffer.walkSpeed)
                .setDefaultValue(defaults.extraDouble("walk_speed", defaults.movementSpeed() * 0.8D))
                .setMin(0.01D)
                .setMax(1.0D)
                .setSaveConsumer(value -> buffer.walkSpeed = value)
                .build());
        entries.add(entryBuilder.startDoubleField(Component.translatable("config.saintsdragons.attributes.ignivorus.movement_speed"), buffer.movementSpeed)
                .setDefaultValue(defaults.movementSpeed())
                .setMin(0.05D)
                .setMax(1.5D)
                .setSaveConsumer(value -> buffer.movementSpeed = value)
                .build());
        entries.add(entryBuilder.startDoubleField(Component.translatable("config.saintsdragons.attributes.ignivorus.flying_speed"), buffer.flyingSpeed)
                .setDefaultValue(defaults.flyingSpeed())
                .setMin(0.1D)
                .setMax(3.0D)
                .setSaveConsumer(value -> buffer.flyingSpeed = value)
                .build());
        entries.add(entryBuilder.startDoubleField(Component.translatable("config.saintsdragons.attributes.ignivorus.base_damage"), buffer.baseDamage)
                .setDefaultValue(defaults.extraDouble("attack_damage", 15.0D))
                .setMin(1.0D)
                .setMax(300.0D)
                .setSaveConsumer(value -> buffer.baseDamage = value)
                .build());
        entries.add(entryBuilder.startDoubleField(Component.translatable("config.saintsdragons.attributes.ignivorus.bite_damage"), buffer.biteDamage)
                .setDefaultValue(defaults.abilityDamage("bite", 50.0D))
                .setMin(1.0D)
                .setMax(500.0D)
                .setSaveConsumer(value -> buffer.biteDamage = value)
                .build());
        entries.add(entryBuilder.startDoubleField(Component.translatable("config.saintsdragons.attributes.ignivorus.body_slam_damage"), buffer.bodySlamDamage)
                .setDefaultValue(defaults.abilityDamage("body_slam", 40.0D))
                .setMin(1.0D)
                .setMax(500.0D)
                .setSaveConsumer(value -> buffer.bodySlamDamage = value)
                .build());
        entries.add(entryBuilder.startDoubleField(Component.translatable("config.saintsdragons.attributes.ignivorus.fire_breath_damage"), buffer.fireBreathDamage)
                .setDefaultValue(defaults.abilityDamage("fire_breath", 4.0D))
                .setMin(0.1D)
                .setMax(100.0D)
                .setSaveConsumer(value -> buffer.fireBreathDamage = value)
                .build());
        entries.add(entryBuilder.startDoubleField(Component.translatable("config.saintsdragons.attributes.ignivorus.ultimate_damage"), buffer.ultimateDamage)
                .setDefaultValue(defaults.abilityDamage("ultimate", 200.0D))
                .setMin(10.0D)
                .setMax(2000.0D)
                .setSaveConsumer(value -> buffer.ultimateDamage = value)
                .build());
        entries.add(entryBuilder.startDoubleField(Component.translatable("config.saintsdragons.attributes.ignivorus.ultimate_penalty"), buffer.ultimatePenalty)
                .setDefaultValue(defaults.extraDouble("ultimate_penalty_health", 50.0D))
                .setMin(1.0D)
                .setMax(500.0D)
                .setSaveConsumer(value -> buffer.ultimatePenalty = value)
                .build());
        entries.add(entryBuilder.startDoubleField(Component.translatable("config.saintsdragons.attributes.ignivorus.taming_base"), buffer.tamingChanceBase)
                .setDefaultValue(defaults.extraDouble("taming_chance_base", 7.0))
                .setMin(1.0D)
                .setMax(100.0D)
                .setSaveConsumer(value -> buffer.tamingChanceBase = value)
                .build());
        entries.add(entryBuilder.startDoubleField(Component.translatable("config.saintsdragons.attributes.ignivorus.taming_hearty"), buffer.tamingChanceHearty)
                .setDefaultValue(defaults.extraDouble("taming_chance_hearty", 4.0))
                .setMin(1.0D)
                .setMax(100.0D)
                .setSaveConsumer(value -> buffer.tamingChanceHearty = value)
                .build());
        @SuppressWarnings({"rawtypes", "unchecked"})
        List<AbstractConfigListEntry> rawEntries = (List) entries;
        category.addEntry(entryBuilder.startSubCategory(Component.translatable("config.saintsdragons.attributes.ignivorus"), rawEntries)
                .setExpanded(false)
                .build());
    }

    private void persistDragonAttributes(CindervaneAttributeBuffer cindervaneBuffer,
                                         RaevyxAttributeBuffer raevyxBuffer,
                                         NulljawAttributeBuffer nulljawBuffer,
                                         IgnivorusAttributeBuffer ignivorusBuffer) {
        DragonAttributeConfigLoader loader = DragonAttributeConfigLoader.getInstance();
        DragonAttributeConfig current = loader.getConfig(DragonAttributeConfigLoader.CINDERVANE_ID);
        Map<String, DragonAbilityOverride> abilities = new HashMap<>(current.abilities());
        abilities.put("bite", DragonAbilityOverride.ofDamage(cindervaneBuffer.biteDamage));
        abilities.put("magma_volley", DragonAbilityOverride.ofDamage(cindervaneBuffer.volleyDamage));
        DragonAttributeConfig updated = new DragonAttributeConfig(
                cindervaneBuffer.maxHealth,
                cindervaneBuffer.armor,
                cindervaneBuffer.movementSpeed,
                cindervaneBuffer.flyingSpeed,
                abilities,
                Map.of()
        );
        loader.overwriteConfig(DragonAttributeConfigLoader.CINDERVANE_ID, updated);

        DragonAttributeConfig raevyxCurrent = loader.getConfig(DragonAttributeConfigLoader.RAEVYX_ID);
        Map<String, DragonAbilityOverride> raevyxAbilities = new HashMap<>(raevyxCurrent.abilities());
        raevyxAbilities.put("bite", DragonAbilityOverride.ofDamage(raevyxBuffer.biteDamage));
        raevyxAbilities.put("lightning_beam", DragonAbilityOverride.ofDamage(raevyxBuffer.beamDamage));
        raevyxAbilities.put("horn_gore", DragonAbilityOverride.ofDamage(raevyxBuffer.hornDamage));
        DragonAttributeConfig updatedRaevyx = new DragonAttributeConfig(
                raevyxBuffer.maxHealth,
                raevyxBuffer.armor,
                raevyxBuffer.movementSpeed,
                raevyxBuffer.flyingSpeed,
                raevyxAbilities,
                Map.of(
                        "taming_chance_base", raevyxBuffer.tamingChanceBase,
                        "taming_chance_hearty", raevyxBuffer.tamingChanceHearty
                )
        );
        loader.overwriteConfig(DragonAttributeConfigLoader.RAEVYX_ID, updatedRaevyx);

        Map<String, DragonAbilityOverride> nulljawAbilities = new HashMap<>();
        nulljawAbilities.put("bite_phase1", DragonAbilityOverride.ofDamage(nulljawBuffer.bitePhase1));
        nulljawAbilities.put("bite_phase2", DragonAbilityOverride.ofDamage(nulljawBuffer.bitePhase2));
        nulljawAbilities.put("horn_gore_phase1", DragonAbilityOverride.ofDamage(nulljawBuffer.hornPhase1));
        nulljawAbilities.put("horn_gore_phase2", DragonAbilityOverride.ofDamage(nulljawBuffer.hornPhase2));
        DragonAttributeConfig updatedNulljaw = new DragonAttributeConfig(
                nulljawBuffer.maxHealth,
                nulljawBuffer.armor,
                nulljawBuffer.runSpeed,
                0.0D,
                nulljawAbilities,
                Map.of(
                        "walk_speed", nulljawBuffer.walkSpeed,
                        "swim_speed", nulljawBuffer.swimSpeed
                )
        );
        loader.overwriteConfig(DragonAttributeConfigLoader.NULLJAW_ID, updatedNulljaw);

        DragonAttributeConfig ignivorusCurrent = loader.getConfig(DragonAttributeConfigLoader.IGNIVORUS_ID);
        Map<String, DragonAbilityOverride> ignivorusAbilities = new HashMap<>(ignivorusCurrent.abilities());
        ignivorusAbilities.put("bite", DragonAbilityOverride.ofDamage(ignivorusBuffer.biteDamage));
        ignivorusAbilities.put("body_slam", DragonAbilityOverride.ofDamage(ignivorusBuffer.bodySlamDamage));
        ignivorusAbilities.put("fire_breath", DragonAbilityOverride.ofDamage(ignivorusBuffer.fireBreathDamage));
        ignivorusAbilities.put("ultimate", DragonAbilityOverride.ofDamage(ignivorusBuffer.ultimateDamage));
        DragonAttributeConfig updatedIgnivorus = new DragonAttributeConfig(
                ignivorusBuffer.maxHealth,
                ignivorusBuffer.armor,
                ignivorusBuffer.movementSpeed,
                ignivorusBuffer.flyingSpeed,
                ignivorusAbilities,
                Map.of(
                        "walk_speed", ignivorusBuffer.walkSpeed,
                        "attack_damage", ignivorusBuffer.baseDamage,
                        "ultimate_penalty_health", ignivorusBuffer.ultimatePenalty,
                        "taming_chance_base", ignivorusBuffer.tamingChanceBase,
                        "taming_chance_hearty", ignivorusBuffer.tamingChanceHearty
                )
        );
        loader.overwriteConfig(DragonAttributeConfigLoader.IGNIVORUS_ID, updatedIgnivorus);
    }

    private static final class CindervaneAttributeBuffer {
        double maxHealth;
        double armor;
        double movementSpeed;
        double flyingSpeed;
        double biteDamage;
        double volleyDamage;
    }

    private static final class RaevyxAttributeBuffer {
        double maxHealth;
        double armor;
        double movementSpeed;
        double flyingSpeed;
        double biteDamage;
        double beamDamage;
        double hornDamage;
        double tamingChanceBase;
        double tamingChanceHearty;
    }

    private static final class NulljawAttributeBuffer {
        double maxHealth;
        double armor;
        double runSpeed;
        double walkSpeed;
        double swimSpeed;
        double bitePhase1;
        double bitePhase2;
        double hornPhase1;
        double hornPhase2;
    }

    private static final class IgnivorusAttributeBuffer {
        double maxHealth;
        double armor;
        double walkSpeed;
        double movementSpeed;
        double flyingSpeed;
        double baseDamage;
        double biteDamage;
        double bodySlamDamage;
        double fireBreathDamage;
        double ultimateDamage;
        double ultimatePenalty;
        double tamingChanceBase;
        double tamingChanceHearty;
    }
}

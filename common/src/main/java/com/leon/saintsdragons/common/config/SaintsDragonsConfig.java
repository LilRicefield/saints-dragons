package com.leon.saintsdragons.common.config;

import com.leon.saintsdragons.platform.ConfigHelper;
import com.leon.saintsdragons.platform.Services;

public final class SaintsDragonsConfig {
    public static final String CONFIG_FOLDER = "saintsdragons";
    public static final String CLIENT_CONFIG_FOLDER = CONFIG_FOLDER + "/client";
    public static final String SERVER_CONFIG_FOLDER = CONFIG_FOLDER + "/server";
    public static final String SPAWNING_CONFIG_FILE = SERVER_CONFIG_FOLDER + "/spawning.toml";
    public static final String SERVER_CONFIG_FILE = SERVER_CONFIG_FOLDER + "/servercommon.toml";

    public static final int SPAWN_WEIGHT_MAX = 5000;
    public static final int RAEVYX_SPAWN_WEIGHT_DEFAULT = 1;
    public static final int RAEVYX_MIN_GROUP_SIZE_DEFAULT = 1;
    public static final int RAEVYX_MAX_GROUP_SIZE_DEFAULT = 2;

    public static final int STEGONAUT_SPAWN_WEIGHT_DEFAULT = 5;
    public static final int STEGONAUT_MIN_GROUP_SIZE_DEFAULT = 1;
    public static final int STEGONAUT_MAX_GROUP_SIZE_DEFAULT = 4;

    public static final int CINDERVANE_SPAWN_WEIGHT_DEFAULT = 3;
    public static final int CINDERVANE_MIN_GROUP_SIZE_DEFAULT = 1;
    public static final int CINDERVANE_MAX_GROUP_SIZE_DEFAULT = 3;

    public static final int ATROXIIA_SPAWN_WEIGHT_DEFAULT = 1;
    public static final int ATROXIIA_MIN_GROUP_SIZE_DEFAULT = 1;
    public static final int ATROXIIA_MAX_GROUP_SIZE_DEFAULT = 1;

    public static final int VOLITANS_SPAWN_WEIGHT_DEFAULT = 1;
    public static final int VOLITANS_MIN_GROUP_SIZE_DEFAULT = 1;
    public static final int VOLITANS_MAX_GROUP_SIZE_DEFAULT = 1;

    public static final int NULLJAW_SPAWN_WEIGHT_DEFAULT = 4;
    public static final int NULLJAW_MIN_GROUP_SIZE_DEFAULT = 4;
    public static final int NULLJAW_MAX_GROUP_SIZE_DEFAULT = 4;

    public static final int MOOP_SPAWN_WEIGHT_DEFAULT = 4;
    public static final int MOOP_MIN_GROUP_SIZE_DEFAULT = 1;
    public static final int MOOP_MAX_GROUP_SIZE_DEFAULT = 1;

    public static final int MOSSBACK_SPAWN_WEIGHT_DEFAULT = 10;
    public static final int MOSSBACK_MIN_GROUP_SIZE_DEFAULT = 1;
    public static final int MOSSBACK_MAX_GROUP_SIZE_DEFAULT = 2;
    public static final boolean RAEVYX_CUSTOM_SPAWNING_ENABLED_DEFAULT = true;
    public static final boolean STEGONAUT_CUSTOM_SPAWNING_ENABLED_DEFAULT = true;
    public static final boolean VOLITANS_CUSTOM_SPAWNING_ENABLED_DEFAULT = true;
    public static final boolean DRAGON_GRIEFING_ENABLED_DEFAULT = true;
    public static final boolean FIRE_DRAGON_BLOCK_IGNITION_ENABLED_DEFAULT = true;
    public static final boolean SCREEN_SHAKE_ENABLED_DEFAULT = true;
    public static final boolean BARREL_ROLL_ENABLED_DEFAULT = true;
    public static final boolean STEGONAUT_BUFFS_ENABLED_DEFAULT = true;
    public static final boolean DRAGON_BREEDING_ENABLED_DEFAULT = true;
    public static final boolean HUNGER_DECAY_ENABLED_DEFAULT = true;
    public static final boolean HAPPINESS_DECAY_ENABLED_DEFAULT = true;
    public static final boolean WIKI_REMINDER_ENABLED_DEFAULT = true;
    public static final int IVY_RESTOCK_INTERVAL_DEFAULT = 24000;

    public static ConfigHelper.IntValue RAEVYX_SPAWN_WEIGHT;
    public static ConfigHelper.IntValue RAEVYX_MIN_GROUP_SIZE;
    public static ConfigHelper.IntValue RAEVYX_MAX_GROUP_SIZE;

    public static ConfigHelper.IntValue STEGONAUT_SPAWN_WEIGHT;
    public static ConfigHelper.IntValue STEGONAUT_MIN_GROUP_SIZE;
    public static ConfigHelper.IntValue STEGONAUT_MAX_GROUP_SIZE;

    public static ConfigHelper.IntValue CINDERVANE_SPAWN_WEIGHT;
    public static ConfigHelper.IntValue CINDERVANE_MIN_GROUP_SIZE;
    public static ConfigHelper.IntValue CINDERVANE_MAX_GROUP_SIZE;

    public static ConfigHelper.IntValue ATROXIIA_SPAWN_WEIGHT;
    public static ConfigHelper.IntValue ATROXIIA_MIN_GROUP_SIZE;
    public static ConfigHelper.IntValue ATROXIIA_MAX_GROUP_SIZE;

    public static ConfigHelper.IntValue VOLITANS_SPAWN_WEIGHT;
    public static ConfigHelper.IntValue VOLITANS_MIN_GROUP_SIZE;
    public static ConfigHelper.IntValue VOLITANS_MAX_GROUP_SIZE;

    public static ConfigHelper.IntValue NULLJAW_SPAWN_WEIGHT;
    public static ConfigHelper.IntValue NULLJAW_MIN_GROUP_SIZE;
    public static ConfigHelper.IntValue NULLJAW_MAX_GROUP_SIZE;

    public static ConfigHelper.IntValue MOOP_SPAWN_WEIGHT;
    public static ConfigHelper.IntValue MOOP_MIN_GROUP_SIZE;
    public static ConfigHelper.IntValue MOOP_MAX_GROUP_SIZE;

    public static ConfigHelper.IntValue MOSSBACK_SPAWN_WEIGHT;
    public static ConfigHelper.IntValue MOSSBACK_MIN_GROUP_SIZE;
    public static ConfigHelper.IntValue MOSSBACK_MAX_GROUP_SIZE;

    public static ConfigHelper.BooleanValue RAEVYX_CUSTOM_SPAWNING_ENABLED;
    public static ConfigHelper.BooleanValue STEGONAUT_CUSTOM_SPAWNING_ENABLED;
    public static ConfigHelper.BooleanValue VOLITANS_CUSTOM_SPAWNING_ENABLED;
    public static ConfigHelper.BooleanValue DRAGON_GRIEFING_ENABLED;
    public static ConfigHelper.BooleanValue FIRE_DRAGON_BLOCK_IGNITION_ENABLED;
    public static ConfigHelper.BooleanValue SCREEN_SHAKE_ENABLED;
    public static ConfigHelper.BooleanValue BARREL_ROLL_ENABLED;
    public static ConfigHelper.BooleanValue STEGONAUT_BUFFS_ENABLED;
    public static ConfigHelper.BooleanValue DRAGON_BREEDING_ENABLED;
    public static ConfigHelper.BooleanValue HUNGER_DECAY_ENABLED;
    public static ConfigHelper.BooleanValue HAPPINESS_DECAY_ENABLED;
    public static ConfigHelper.BooleanValue WIKI_REMINDER_ENABLED;
    public static ConfigHelper.IntValue IVY_RESTOCK_INTERVAL;

    private static volatile boolean initialized = false;

    public static void bootstrap() {
        if (!initialized) {
            synchronized (SaintsDragonsConfig.class) {
                if (!initialized) {
                    initializeConfig();
                    initialized = true;
                }
            }
        }
    }

    private static void initializeConfig() {
        ConfigHelper.ConfigBuilder builder = Services.PLATFORM.getConfigHelper()
                .commonBuilder(SPAWNING_CONFIG_FILE);

        builder.push("spawning");
        builder.comment("Entity spawn configuration - control how often Saint's Dragons creatures spawn");
        builder.comment("Note: spawn weights are relative per biome/category roll.");
        builder.comment("Final spawn frequency also depends on each entity's spawn predicate and placement checks.");
        builder.comment("Natural wild dragon spawns are also filtered by shared density rules to stop creature-category dragons from piling up.");
        builder.comment("Raevyx additionally requires a thunderstorm for natural/chunk-generation spawning.");

        builder.comment("Raevyx spawn settings");
        RAEVYX_CUSTOM_SPAWNING_ENABLED = builder.defineBoolean("raevyxCustomSpawningEnabled", RAEVYX_CUSTOM_SPAWNING_ENABLED_DEFAULT);
        RAEVYX_SPAWN_WEIGHT = builder.defineInt("raevyxSpawnWeight", RAEVYX_SPAWN_WEIGHT_DEFAULT, 0, SPAWN_WEIGHT_MAX);
        RAEVYX_MIN_GROUP_SIZE = builder.defineInt("raevyxMinGroupSize", RAEVYX_MIN_GROUP_SIZE_DEFAULT, 1, 10);
        RAEVYX_MAX_GROUP_SIZE = builder.defineInt("raevyxMaxGroupSize", RAEVYX_MAX_GROUP_SIZE_DEFAULT, 1, 10);

        builder.comment("Stegonaut spawn settings");
        STEGONAUT_CUSTOM_SPAWNING_ENABLED = builder.defineBoolean("stegonautCustomSpawningEnabled", STEGONAUT_CUSTOM_SPAWNING_ENABLED_DEFAULT);
        STEGONAUT_SPAWN_WEIGHT = builder.defineInt("stegonautSpawnWeight", STEGONAUT_SPAWN_WEIGHT_DEFAULT, 0, SPAWN_WEIGHT_MAX);
        STEGONAUT_MIN_GROUP_SIZE = builder.defineInt("stegonautMinGroupSize", STEGONAUT_MIN_GROUP_SIZE_DEFAULT, 1, 10);
        STEGONAUT_MAX_GROUP_SIZE = builder.defineInt("stegonautMaxGroupSize", STEGONAUT_MAX_GROUP_SIZE_DEFAULT, 1, 10);

        builder.comment("Cindervane spawn settings");
        CINDERVANE_SPAWN_WEIGHT = builder.defineInt("cindervaneSpawnWeight", CINDERVANE_SPAWN_WEIGHT_DEFAULT, 0, SPAWN_WEIGHT_MAX);
        CINDERVANE_MIN_GROUP_SIZE = builder.defineInt("cindervaneMinGroupSize", CINDERVANE_MIN_GROUP_SIZE_DEFAULT, 1, 10);
        CINDERVANE_MAX_GROUP_SIZE = builder.defineInt("cindervaneMaxGroupSize", CINDERVANE_MAX_GROUP_SIZE_DEFAULT, 1, 10);

        builder.comment("Atroxiia spawn settings");
        ATROXIIA_SPAWN_WEIGHT = builder.defineInt("atroxiiaSpawnWeight", ATROXIIA_SPAWN_WEIGHT_DEFAULT, 0, SPAWN_WEIGHT_MAX);
        ATROXIIA_MIN_GROUP_SIZE = builder.defineInt("atroxiiaMinGroupSize", ATROXIIA_MIN_GROUP_SIZE_DEFAULT, 1, 10);
        ATROXIIA_MAX_GROUP_SIZE = builder.defineInt("atroxiiaMaxGroupSize", ATROXIIA_MAX_GROUP_SIZE_DEFAULT, 1, 10);

        builder.comment("Volitans spawn settings (custom underwater spawner for ocean/wetland habitats)");
        VOLITANS_CUSTOM_SPAWNING_ENABLED = builder.defineBoolean("volitansCustomSpawningEnabled", VOLITANS_CUSTOM_SPAWNING_ENABLED_DEFAULT);
        VOLITANS_SPAWN_WEIGHT = builder.defineInt("volitansSpawnWeight", VOLITANS_SPAWN_WEIGHT_DEFAULT, 0, SPAWN_WEIGHT_MAX);
        VOLITANS_MIN_GROUP_SIZE = builder.defineInt("volitansMinGroupSize", VOLITANS_MIN_GROUP_SIZE_DEFAULT, 1, 10);
        VOLITANS_MAX_GROUP_SIZE = builder.defineInt("volitansMaxGroupSize", VOLITANS_MAX_GROUP_SIZE_DEFAULT, 1, 10);

        builder.comment("Nulljaw spawn settings (End-floating dragon)");
        NULLJAW_SPAWN_WEIGHT = builder.defineInt("nulljawSpawnWeight", NULLJAW_SPAWN_WEIGHT_DEFAULT, 0, SPAWN_WEIGHT_MAX);
        NULLJAW_MIN_GROUP_SIZE = builder.defineInt("nulljawMinGroupSize", NULLJAW_MIN_GROUP_SIZE_DEFAULT, 1, 10);
        NULLJAW_MAX_GROUP_SIZE = builder.defineInt("nulljawMaxGroupSize", NULLJAW_MAX_GROUP_SIZE_DEFAULT, 1, 10);

        builder.comment("Moop spawn settings");
        MOOP_SPAWN_WEIGHT = builder.defineInt("moopSpawnWeight", MOOP_SPAWN_WEIGHT_DEFAULT, 0, SPAWN_WEIGHT_MAX);
        MOOP_MIN_GROUP_SIZE = builder.defineInt("moopMinGroupSize", MOOP_MIN_GROUP_SIZE_DEFAULT, 1, 10);
        MOOP_MAX_GROUP_SIZE = builder.defineInt("moopMaxGroupSize", MOOP_MAX_GROUP_SIZE_DEFAULT, 1, 10);

        builder.comment("Mossback spawn settings");
        MOSSBACK_SPAWN_WEIGHT = builder.defineInt("mossbackSpawnWeight", MOSSBACK_SPAWN_WEIGHT_DEFAULT, 0, SPAWN_WEIGHT_MAX);
        MOSSBACK_MIN_GROUP_SIZE = builder.defineInt("mossbackMinGroupSize", MOSSBACK_MIN_GROUP_SIZE_DEFAULT, 1, 10);
        MOSSBACK_MAX_GROUP_SIZE = builder.defineInt("mossbackMaxGroupSize", MOSSBACK_MAX_GROUP_SIZE_DEFAULT, 1, 10);

        builder.pop();
        builder.build();

        ConfigHelper.ConfigBuilder serverBuilder = Services.PLATFORM.getConfigHelper()
                .commonBuilder(SERVER_CONFIG_FILE);

        serverBuilder.push("gameplay");
        serverBuilder.comment("Extra Saint's Dragons griefing toggle layered on top of the vanilla mobGriefing gamerule.");
        serverBuilder.comment("If false, dragon-caused block destruction is disabled even when mobGriefing is true.");
        DRAGON_GRIEFING_ENABLED = serverBuilder.defineBoolean("dragonGriefingEnabled", DRAGON_GRIEFING_ENABLED_DEFAULT);
        serverBuilder.comment("Whether Ignivorus and Cindervane attacks may place fire blocks. Also requires dragonGriefingEnabled and the vanilla mobGriefing gamerule.");
        FIRE_DRAGON_BLOCK_IGNITION_ENABLED = serverBuilder.defineBoolean("fireDragonBlockIgnitionEnabled", FIRE_DRAGON_BLOCK_IGNITION_ENABLED_DEFAULT);
        serverBuilder.comment("Global toggle for dragon and ability-driven screen shake effects.");
        SCREEN_SHAKE_ENABLED = serverBuilder.defineBoolean("screenShakeEnabled", SCREEN_SHAKE_ENABLED_DEFAULT);
        serverBuilder.comment("Global toggle for rider-triggered barrel roll on flying dragons.");
        BARREL_ROLL_ENABLED = serverBuilder.defineBoolean("barrelRollEnabled", BARREL_ROLL_ENABLED_DEFAULT);
        serverBuilder.comment("Global toggle for Stegonaut passive aura buffs and portable binder buffs.");
        STEGONAUT_BUFFS_ENABLED = serverBuilder.defineBoolean("stegonautBuffsEnabled", STEGONAUT_BUFFS_ENABLED_DEFAULT);
        serverBuilder.comment("Server-authoritative toggle for dragon breeding. If false, players cannot ready dragons for breeding and dragons cannot produce eggs.");
        DRAGON_BREEDING_ENABLED = serverBuilder.defineBoolean("dragonBreedingEnabled", DRAGON_BREEDING_ENABLED_DEFAULT);
        serverBuilder.comment("Global toggle for tame dragon hunger decay.");
        HUNGER_DECAY_ENABLED = serverBuilder.defineBoolean("hungerDecayEnabled", HUNGER_DECAY_ENABLED_DEFAULT);
        serverBuilder.comment("Global toggle for tame dragon happiness decay.");
        HAPPINESS_DECAY_ENABLED = serverBuilder.defineBoolean("happinessDecayEnabled", HAPPINESS_DECAY_ENABLED_DEFAULT);
        serverBuilder.comment("Whether players receive the one-time Saint's Dragons wiki reminder when joining a world.");
        WIKI_REMINDER_ENABLED = serverBuilder.defineBoolean("wikiReminderEnabled", WIKI_REMINDER_ENABLED_DEFAULT);
        serverBuilder.pop();

        serverBuilder.push("ivy");
        serverBuilder.comment("Ticks between Ivy's trade restocks (20 ticks = 1 second, 24000 = 20 minutes).");
        IVY_RESTOCK_INTERVAL = serverBuilder.defineInt("ivyRestockInterval", IVY_RESTOCK_INTERVAL_DEFAULT, 20, 72000);
        serverBuilder.pop();

        serverBuilder.build();
    }

    public static int getIvyRestockInterval() {
        return IVY_RESTOCK_INTERVAL == null ? IVY_RESTOCK_INTERVAL_DEFAULT : IVY_RESTOCK_INTERVAL.get();
    }

    public static boolean isDragonBreedingEnabled() {
        return DRAGON_BREEDING_ENABLED == null || DRAGON_BREEDING_ENABLED.get();
    }

    public static boolean isFireDragonBlockIgnitionEnabled() {
        return FIRE_DRAGON_BLOCK_IGNITION_ENABLED == null || FIRE_DRAGON_BLOCK_IGNITION_ENABLED.get();
    }

    public static boolean isWikiReminderEnabled() {
        return WIKI_REMINDER_ENABLED == null || WIKI_REMINDER_ENABLED.get();
    }

    public static boolean isRaevyxCustomSpawningEnabled() {
        return RAEVYX_CUSTOM_SPAWNING_ENABLED == null || RAEVYX_CUSTOM_SPAWNING_ENABLED.get();
    }

    public static boolean isStegonautCustomSpawningEnabled() {
        return STEGONAUT_CUSTOM_SPAWNING_ENABLED == null || STEGONAUT_CUSTOM_SPAWNING_ENABLED.get();
    }

    public static boolean isVolitansCustomSpawningEnabled() {
        return VOLITANS_CUSTOM_SPAWNING_ENABLED == null || VOLITANS_CUSTOM_SPAWNING_ENABLED.get();
    }

    private SaintsDragonsConfig() {
    }
}

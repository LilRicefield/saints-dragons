package com.leon.saintsdragons.common.config;

import com.leon.saintsdragons.platform.ConfigHelper;
import com.leon.saintsdragons.platform.Services;

public final class SaintsDragonsConfig {
    public static final String CONFIG_FOLDER = "saintsdragons";
    public static final String CLIENT_CONFIG_FOLDER = CONFIG_FOLDER + "/client";
    public static final String SERVER_CONFIG_FOLDER = CONFIG_FOLDER + "/server";
    public static final String CLIENT_COMMON_CONFIG_FILE = CLIENT_CONFIG_FOLDER + "/clientcommon.toml";
    public static final String DRAGON_RIDER_CAMERA_CONFIG_FOLDER = CLIENT_CONFIG_FOLDER + "/dragon_rider_camera";
    public static final String SPAWNING_CONFIG_FILE = SERVER_CONFIG_FOLDER + "/spawning.toml";
    public static final String SERVER_CONFIG_FILE = SERVER_CONFIG_FOLDER + "/servercommon.toml";
    public static final String DRAGON_ATTRIBUTES_CONFIG_FILE = SERVER_CONFIG_FOLDER + "/attributes.toml";
    public static final String DRAGON_ATTRIBUTES_CONFIG_FOLDER = SERVER_CONFIG_FOLDER + "/dragon_attributes";

    public static final int SPAWN_WEIGHT_MAX = 5000;
    public static final int RAEVYX_SPAWN_WEIGHT_DEFAULT = 6;
    public static final int RAEVYX_MIN_GROUP_SIZE_DEFAULT = 1;
    public static final int RAEVYX_MAX_GROUP_SIZE_DEFAULT = 2;

    public static final int STEGONAUT_SPAWN_WEIGHT_DEFAULT = 10;
    public static final int STEGONAUT_MIN_GROUP_SIZE_DEFAULT = 1;
    public static final int STEGONAUT_MAX_GROUP_SIZE_DEFAULT = 4;

    public static final int CINDERVANE_SPAWN_WEIGHT_DEFAULT = 10;
    public static final int CINDERVANE_MIN_GROUP_SIZE_DEFAULT = 1;
    public static final int CINDERVANE_MAX_GROUP_SIZE_DEFAULT = 3;

    public static final int ATROXIIA_SPAWN_WEIGHT_DEFAULT = 6;
    public static final int ATROXIIA_MIN_GROUP_SIZE_DEFAULT = 1;
    public static final int ATROXIIA_MAX_GROUP_SIZE_DEFAULT = 1;

    public static final int VOLITANS_SPAWN_WEIGHT_DEFAULT = 6;
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
    public static final boolean RAEVYX_SPAWNING_ENABLED_DEFAULT = true;
    public static final boolean STEGONAUT_SPAWNING_ENABLED_DEFAULT = true;
    public static final boolean CINDERVANE_SPAWNING_ENABLED_DEFAULT = true;
    public static final boolean IGNIVORUS_SPAWNING_ENABLED_DEFAULT = true;
    public static final boolean VARASUCHUS_SPAWNING_ENABLED_DEFAULT = true;
    public static final boolean ATROXIIA_SPAWNING_ENABLED_DEFAULT = true;
    public static final boolean VOLITANS_SPAWNING_ENABLED_DEFAULT = true;
    public static final boolean NULLJAW_SPAWNING_ENABLED_DEFAULT = true;
    public static final boolean MOOP_SPAWNING_ENABLED_DEFAULT = true;
    public static final boolean MOSSBACK_SPAWNING_ENABLED_DEFAULT = true;
    public static final boolean IVY_SPAWNING_ENABLED_DEFAULT = true;
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

    public static ConfigHelper.BooleanValue RAEVYX_SPAWNING_ENABLED;
    public static ConfigHelper.BooleanValue STEGONAUT_SPAWNING_ENABLED;
    public static ConfigHelper.BooleanValue CINDERVANE_SPAWNING_ENABLED;
    public static ConfigHelper.BooleanValue IGNIVORUS_SPAWNING_ENABLED;
    public static ConfigHelper.BooleanValue VARASUCHUS_SPAWNING_ENABLED;
    public static ConfigHelper.BooleanValue ATROXIIA_SPAWNING_ENABLED;
    public static ConfigHelper.BooleanValue VOLITANS_SPAWNING_ENABLED;
    public static ConfigHelper.BooleanValue NULLJAW_SPAWNING_ENABLED;
    public static ConfigHelper.BooleanValue MOOP_SPAWNING_ENABLED;
    public static ConfigHelper.BooleanValue MOSSBACK_SPAWNING_ENABLED;
    public static ConfigHelper.BooleanValue IVY_SPAWNING_ENABLED;
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
        builder.comment("With custom spawning enabled, Raevyx spawns are driven by thunderstorms.");
        builder.comment("Disabling custom spawning restores vanilla biome spawning for Raevyx.");
        builder.comment("Each species has an automatic spawning toggle. Disabling it does not remove existing entities or block spawn eggs, commands, or breeding.");
        builder.comment("Disabling Ignivorus, Varasuchus, or Ivy also stops their structures in newly generated chunks. Existing structures are not modified.");
        builder.comment("Restart the client for singleplayer, or the server for multiplayer, after changing a spawning toggle.");
        builder.comment("Raevyx spawn settings");
        RAEVYX_SPAWNING_ENABLED = builder.defineBoolean("raevyxSpawningEnabled", RAEVYX_SPAWNING_ENABLED_DEFAULT);
        RAEVYX_CUSTOM_SPAWNING_ENABLED = builder.defineBoolean("raevyxCustomSpawningEnabled", RAEVYX_CUSTOM_SPAWNING_ENABLED_DEFAULT);
        RAEVYX_SPAWN_WEIGHT = builder.defineInt("raevyxSpawnWeight", RAEVYX_SPAWN_WEIGHT_DEFAULT, 0, SPAWN_WEIGHT_MAX);
        RAEVYX_MIN_GROUP_SIZE = builder.defineInt("raevyxMinGroupSize", RAEVYX_MIN_GROUP_SIZE_DEFAULT, 1, 10);
        RAEVYX_MAX_GROUP_SIZE = builder.defineInt("raevyxMaxGroupSize", RAEVYX_MAX_GROUP_SIZE_DEFAULT, 1, 10);

        builder.comment("Stegonaut spawn settings");
        STEGONAUT_SPAWNING_ENABLED = builder.defineBoolean("stegonautSpawningEnabled", STEGONAUT_SPAWNING_ENABLED_DEFAULT);
        STEGONAUT_CUSTOM_SPAWNING_ENABLED = builder.defineBoolean("stegonautCustomSpawningEnabled", STEGONAUT_CUSTOM_SPAWNING_ENABLED_DEFAULT);
        STEGONAUT_SPAWN_WEIGHT = builder.defineInt("stegonautSpawnWeight", STEGONAUT_SPAWN_WEIGHT_DEFAULT, 0, SPAWN_WEIGHT_MAX);
        STEGONAUT_MIN_GROUP_SIZE = builder.defineInt("stegonautMinGroupSize", STEGONAUT_MIN_GROUP_SIZE_DEFAULT, 1, 10);
        STEGONAUT_MAX_GROUP_SIZE = builder.defineInt("stegonautMaxGroupSize", STEGONAUT_MAX_GROUP_SIZE_DEFAULT, 1, 10);

        builder.comment("Cindervane spawn settings");
        CINDERVANE_SPAWNING_ENABLED = builder.defineBoolean("cindervaneSpawningEnabled", CINDERVANE_SPAWNING_ENABLED_DEFAULT);
        CINDERVANE_SPAWN_WEIGHT = builder.defineInt("cindervaneSpawnWeight", CINDERVANE_SPAWN_WEIGHT_DEFAULT, 0, SPAWN_WEIGHT_MAX);
        CINDERVANE_MIN_GROUP_SIZE = builder.defineInt("cindervaneMinGroupSize", CINDERVANE_MIN_GROUP_SIZE_DEFAULT, 1, 10);
        CINDERVANE_MAX_GROUP_SIZE = builder.defineInt("cindervaneMaxGroupSize", CINDERVANE_MAX_GROUP_SIZE_DEFAULT, 1, 10);

        builder.comment("Ignivorus structure spawn settings");
        IGNIVORUS_SPAWNING_ENABLED = builder.defineBoolean("ignivorusSpawningEnabled", IGNIVORUS_SPAWNING_ENABLED_DEFAULT);

        builder.comment("Varasuchus structure spawn settings");
        VARASUCHUS_SPAWNING_ENABLED = builder.defineBoolean("varasuchusSpawningEnabled", VARASUCHUS_SPAWNING_ENABLED_DEFAULT);

        builder.comment("Atroxiia spawn settings");
        ATROXIIA_SPAWNING_ENABLED = builder.defineBoolean("atroxiiaSpawningEnabled", ATROXIIA_SPAWNING_ENABLED_DEFAULT);
        ATROXIIA_SPAWN_WEIGHT = builder.defineInt("atroxiiaSpawnWeight", ATROXIIA_SPAWN_WEIGHT_DEFAULT, 0, SPAWN_WEIGHT_MAX);
        ATROXIIA_MIN_GROUP_SIZE = builder.defineInt("atroxiiaMinGroupSize", ATROXIIA_MIN_GROUP_SIZE_DEFAULT, 1, 10);
        ATROXIIA_MAX_GROUP_SIZE = builder.defineInt("atroxiiaMaxGroupSize", ATROXIIA_MAX_GROUP_SIZE_DEFAULT, 1, 10);

        builder.comment("Volitans spawn settings (custom underwater spawner for ocean/wetland habitats)");
        VOLITANS_SPAWNING_ENABLED = builder.defineBoolean("volitansSpawningEnabled", VOLITANS_SPAWNING_ENABLED_DEFAULT);
        VOLITANS_CUSTOM_SPAWNING_ENABLED = builder.defineBoolean("volitansCustomSpawningEnabled", VOLITANS_CUSTOM_SPAWNING_ENABLED_DEFAULT);
        VOLITANS_SPAWN_WEIGHT = builder.defineInt("volitansSpawnWeight", VOLITANS_SPAWN_WEIGHT_DEFAULT, 0, SPAWN_WEIGHT_MAX);
        VOLITANS_MIN_GROUP_SIZE = builder.defineInt("volitansMinGroupSize", VOLITANS_MIN_GROUP_SIZE_DEFAULT, 1, 10);
        VOLITANS_MAX_GROUP_SIZE = builder.defineInt("volitansMaxGroupSize", VOLITANS_MAX_GROUP_SIZE_DEFAULT, 1, 10);

        builder.comment("Nulljaw spawn settings (End-floating dragon)");
        NULLJAW_SPAWNING_ENABLED = builder.defineBoolean("nulljawSpawningEnabled", NULLJAW_SPAWNING_ENABLED_DEFAULT);
        NULLJAW_SPAWN_WEIGHT = builder.defineInt("nulljawSpawnWeight", NULLJAW_SPAWN_WEIGHT_DEFAULT, 0, SPAWN_WEIGHT_MAX);
        NULLJAW_MIN_GROUP_SIZE = builder.defineInt("nulljawMinGroupSize", NULLJAW_MIN_GROUP_SIZE_DEFAULT, 1, 10);
        NULLJAW_MAX_GROUP_SIZE = builder.defineInt("nulljawMaxGroupSize", NULLJAW_MAX_GROUP_SIZE_DEFAULT, 1, 10);

        builder.comment("Moop spawn settings");
        MOOP_SPAWNING_ENABLED = builder.defineBoolean("moopSpawningEnabled", MOOP_SPAWNING_ENABLED_DEFAULT);
        MOOP_SPAWN_WEIGHT = builder.defineInt("moopSpawnWeight", MOOP_SPAWN_WEIGHT_DEFAULT, 0, SPAWN_WEIGHT_MAX);
        MOOP_MIN_GROUP_SIZE = builder.defineInt("moopMinGroupSize", MOOP_MIN_GROUP_SIZE_DEFAULT, 1, 10);
        MOOP_MAX_GROUP_SIZE = builder.defineInt("moopMaxGroupSize", MOOP_MAX_GROUP_SIZE_DEFAULT, 1, 10);

        builder.comment("Mossback spawn settings");
        MOSSBACK_SPAWNING_ENABLED = builder.defineBoolean("mossbackSpawningEnabled", MOSSBACK_SPAWNING_ENABLED_DEFAULT);
        MOSSBACK_SPAWN_WEIGHT = builder.defineInt("mossbackSpawnWeight", MOSSBACK_SPAWN_WEIGHT_DEFAULT, 0, SPAWN_WEIGHT_MAX);
        MOSSBACK_MIN_GROUP_SIZE = builder.defineInt("mossbackMinGroupSize", MOSSBACK_MIN_GROUP_SIZE_DEFAULT, 1, 10);
        MOSSBACK_MAX_GROUP_SIZE = builder.defineInt("mossbackMaxGroupSize", MOSSBACK_MAX_GROUP_SIZE_DEFAULT, 1, 10);

        builder.comment("Ivy structure spawn settings");
        IVY_SPAWNING_ENABLED = builder.defineBoolean("ivySpawningEnabled", IVY_SPAWNING_ENABLED_DEFAULT);

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

    public static boolean isRaevyxSpawningEnabled() {
        return RAEVYX_SPAWNING_ENABLED == null || RAEVYX_SPAWNING_ENABLED.get();
    }

    public static boolean isStegonautSpawningEnabled() {
        return STEGONAUT_SPAWNING_ENABLED == null || STEGONAUT_SPAWNING_ENABLED.get();
    }

    public static boolean isCindervaneSpawningEnabled() {
        return CINDERVANE_SPAWNING_ENABLED == null || CINDERVANE_SPAWNING_ENABLED.get();
    }

    public static boolean isIgnivorusSpawningEnabled() {
        return IGNIVORUS_SPAWNING_ENABLED == null || IGNIVORUS_SPAWNING_ENABLED.get();
    }

    public static boolean isVarasuchusSpawningEnabled() {
        return VARASUCHUS_SPAWNING_ENABLED == null || VARASUCHUS_SPAWNING_ENABLED.get();
    }

    public static boolean isAtroxiiaSpawningEnabled() {
        return ATROXIIA_SPAWNING_ENABLED == null || ATROXIIA_SPAWNING_ENABLED.get();
    }

    public static boolean isVolitansSpawningEnabled() {
        return VOLITANS_SPAWNING_ENABLED == null || VOLITANS_SPAWNING_ENABLED.get();
    }

    public static boolean isNulljawSpawningEnabled() {
        return NULLJAW_SPAWNING_ENABLED == null || NULLJAW_SPAWNING_ENABLED.get();
    }

    public static boolean isMoopSpawningEnabled() {
        return MOOP_SPAWNING_ENABLED == null || MOOP_SPAWNING_ENABLED.get();
    }

    public static boolean isMossbackSpawningEnabled() {
        return MOSSBACK_SPAWNING_ENABLED == null || MOSSBACK_SPAWNING_ENABLED.get();
    }

    public static boolean isIvySpawningEnabled() {
        return IVY_SPAWNING_ENABLED == null || IVY_SPAWNING_ENABLED.get();
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

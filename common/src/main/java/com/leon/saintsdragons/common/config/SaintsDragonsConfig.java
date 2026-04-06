package com.leon.saintsdragons.common.config;

import com.leon.saintsdragons.platform.ConfigHelper;
import com.leon.saintsdragons.platform.Services;

import java.util.Collections;

public final class SaintsDragonsConfig {
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

    public static final int VARASUCHUS_SPAWN_WEIGHT_DEFAULT = 2;
    public static final int VARASUCHUS_MIN_GROUP_SIZE_DEFAULT = 1;
    public static final int VARASUCHUS_MAX_GROUP_SIZE_DEFAULT = 2;

    public static final int IGNIVORUS_SPAWN_WEIGHT_DEFAULT = 1;
    public static final int IGNIVORUS_MIN_GROUP_SIZE_DEFAULT = 1;
    public static final int IGNIVORUS_MAX_GROUP_SIZE_DEFAULT = 2;

    public static final int VOLITANS_SPAWN_WEIGHT_DEFAULT = 1;
    public static final int VOLITANS_MIN_GROUP_SIZE_DEFAULT = 1;
    public static final int VOLITANS_MAX_GROUP_SIZE_DEFAULT = 1;
    public static final boolean CINDERVANE_EGG_BLOCK_WORLDGEN_DEFAULT = true;
    public static final boolean VARASUCHUS_EGG_BLOCK_WORLDGEN_DEFAULT = true;
    public static final boolean DRAGON_GRIEFING_ENABLED_DEFAULT = true;
    public static final boolean SCREEN_SHAKE_ENABLED_DEFAULT = true;

    public static ConfigHelper.IntValue RAEVYX_SPAWN_WEIGHT;
    public static ConfigHelper.IntValue RAEVYX_MIN_GROUP_SIZE;
    public static ConfigHelper.IntValue RAEVYX_MAX_GROUP_SIZE;
    public static ConfigHelper.ListValue RAEVYX_ADDITIONAL_BIOMES;
    public static ConfigHelper.ListValue RAEVYX_EXCLUDED_BIOMES;

    public static ConfigHelper.IntValue STEGONAUT_SPAWN_WEIGHT;
    public static ConfigHelper.IntValue STEGONAUT_MIN_GROUP_SIZE;
    public static ConfigHelper.IntValue STEGONAUT_MAX_GROUP_SIZE;
    public static ConfigHelper.ListValue STEGONAUT_ADDITIONAL_BIOMES;
    public static ConfigHelper.ListValue STEGONAUT_EXCLUDED_BIOMES;

    public static ConfigHelper.IntValue CINDERVANE_SPAWN_WEIGHT;
    public static ConfigHelper.IntValue CINDERVANE_MIN_GROUP_SIZE;
    public static ConfigHelper.IntValue CINDERVANE_MAX_GROUP_SIZE;
    public static ConfigHelper.ListValue CINDERVANE_ADDITIONAL_BIOMES;
    public static ConfigHelper.ListValue CINDERVANE_EXCLUDED_BIOMES;
    public static ConfigHelper.BooleanValue CINDERVANE_EGG_BLOCK_WORLDGEN;

    public static ConfigHelper.IntValue VARASUCHUS_SPAWN_WEIGHT;
    public static ConfigHelper.IntValue VARASUCHUS_MIN_GROUP_SIZE;
    public static ConfigHelper.IntValue VARASUCHUS_MAX_GROUP_SIZE;
    public static ConfigHelper.ListValue VARASUCHUS_ADDITIONAL_BIOMES;
    public static ConfigHelper.ListValue VARASUCHUS_EXCLUDED_BIOMES;
    public static ConfigHelper.BooleanValue VARASUCHUS_EGG_BLOCK_WORLDGEN;

    public static ConfigHelper.IntValue IGNIVORUS_SPAWN_WEIGHT;
    public static ConfigHelper.IntValue IGNIVORUS_MIN_GROUP_SIZE;
    public static ConfigHelper.IntValue IGNIVORUS_MAX_GROUP_SIZE;
    public static ConfigHelper.ListValue IGNIVORUS_ADDITIONAL_BIOMES;
    public static ConfigHelper.ListValue IGNIVORUS_EXCLUDED_BIOMES;

    public static ConfigHelper.IntValue VOLITANS_SPAWN_WEIGHT;
    public static ConfigHelper.IntValue VOLITANS_MIN_GROUP_SIZE;
    public static ConfigHelper.IntValue VOLITANS_MAX_GROUP_SIZE;
    public static ConfigHelper.ListValue VOLITANS_ADDITIONAL_BIOMES;
    public static ConfigHelper.ListValue VOLITANS_EXCLUDED_BIOMES;
    public static ConfigHelper.BooleanValue DRAGON_GRIEFING_ENABLED;
    public static ConfigHelper.BooleanValue SCREEN_SHAKE_ENABLED;

    private static volatile boolean initialized = false;

    public static void bootstrap() {
        // Double-checked locking to ensure safe initialization and prevent race conditions
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
                .commonBuilder("saintsdragonsspawning.toml");

        builder.push("spawning");
        builder.comment("Dragon spawn configuration - control where and how often dragons spawn");
        builder.comment("Note: spawn weights are relative per biome/category roll.");
        builder.comment("Final spawn frequency also depends on each dragon's spawn predicate and placement checks.");
        builder.comment("Natural wild spawns are also filtered by shared density rules to stop creature-category dragons from piling up.");
        builder.comment("Raevyx additionally requires a thunderstorm for natural/chunk-generation spawning.");

        builder.comment("Raevyx spawn settings");
        RAEVYX_SPAWN_WEIGHT = builder.defineInt("raevyxSpawnWeight", RAEVYX_SPAWN_WEIGHT_DEFAULT, 0, SPAWN_WEIGHT_MAX);
        RAEVYX_MIN_GROUP_SIZE = builder.defineInt("raevyxMinGroupSize", RAEVYX_MIN_GROUP_SIZE_DEFAULT, 1, 10);
        RAEVYX_MAX_GROUP_SIZE = builder.defineInt("raevyxMaxGroupSize", RAEVYX_MAX_GROUP_SIZE_DEFAULT, 1, 10);
        builder.comment("Additional biome IDs or biome tags where Raevyx can spawn (e.g., \"minecraft:desert\", \"terralith:volcanic_crater\", \"#c:is_overworld\")");
        RAEVYX_ADDITIONAL_BIOMES = builder.defineList("raevyxAdditionalBiomes", Collections.emptyList());
        builder.comment("Biome IDs or biome tags to exclude from default Raevyx spawns (e.g., \"minecraft:plains\", \"#minecraft:is_ocean\")");
        RAEVYX_EXCLUDED_BIOMES = builder.defineList("raevyxExcludedBiomes", Collections.emptyList());

        builder.comment("Stegonaut spawn settings (lush caves by default; broaden via additional biome IDs/tags as needed)");
        STEGONAUT_SPAWN_WEIGHT = builder.defineInt("stegonautSpawnWeight", STEGONAUT_SPAWN_WEIGHT_DEFAULT, 0, SPAWN_WEIGHT_MAX);
        STEGONAUT_MIN_GROUP_SIZE = builder.defineInt("stegonautMinGroupSize", STEGONAUT_MIN_GROUP_SIZE_DEFAULT, 1, 10);
        STEGONAUT_MAX_GROUP_SIZE = builder.defineInt("stegonautMaxGroupSize", STEGONAUT_MAX_GROUP_SIZE_DEFAULT, 1, 10);
        builder.comment("Additional biome IDs or biome tags where Stegonaut can spawn beyond lush caves (e.g., \"minecraft:dripstone_caves\", \"terralith:cave/thermal_caves\", \"#forge:is_cave\")");
        STEGONAUT_ADDITIONAL_BIOMES = builder.defineList("stegonautAdditionalBiomes", Collections.emptyList());
        builder.comment("Biome IDs or biome tags to exclude from default Stegonaut spawns (e.g., \"minecraft:plains\", \"#minecraft:is_ocean\")");
        STEGONAUT_EXCLUDED_BIOMES = builder.defineList("stegonautExcludedBiomes", Collections.emptyList());

        builder.comment("Cindervane spawn settings");
        CINDERVANE_SPAWN_WEIGHT = builder.defineInt("cindervaneSpawnWeight", CINDERVANE_SPAWN_WEIGHT_DEFAULT, 0, SPAWN_WEIGHT_MAX);
        CINDERVANE_MIN_GROUP_SIZE = builder.defineInt("cindervaneMinGroupSize", CINDERVANE_MIN_GROUP_SIZE_DEFAULT, 1, 10);
        CINDERVANE_MAX_GROUP_SIZE = builder.defineInt("cindervaneMaxGroupSize", CINDERVANE_MAX_GROUP_SIZE_DEFAULT, 1, 10);
        builder.comment("Additional biome IDs or biome tags where Cindervane can spawn (e.g., \"minecraft:desert\", \"terralith:volcanic_crater\", \"#c:is_overworld\")");
        CINDERVANE_ADDITIONAL_BIOMES = builder.defineList("cindervaneAdditionalBiomes", Collections.emptyList());
        builder.comment("Biome IDs or biome tags to exclude from default Cindervane spawns (e.g., \"minecraft:plains\", \"#minecraft:is_ocean\")");
        CINDERVANE_EXCLUDED_BIOMES = builder.defineList("cindervaneExcludedBiomes", Collections.emptyList());
        builder.comment("Whether Cindervane egg blocks generate in worldgen feature patches");
        CINDERVANE_EGG_BLOCK_WORLDGEN = builder.defineBoolean("cindervaneEggBlockWorldgen", CINDERVANE_EGG_BLOCK_WORLDGEN_DEFAULT);

        builder.comment("Varasuchus spawn settings (semi-aquatic; biome tags can massively expand coverage)");
        VARASUCHUS_SPAWN_WEIGHT = builder.defineInt("varasuchusSpawnWeight", VARASUCHUS_SPAWN_WEIGHT_DEFAULT, 0, SPAWN_WEIGHT_MAX);
        VARASUCHUS_MIN_GROUP_SIZE = builder.defineInt("varasuchusMinGroupSize", VARASUCHUS_MIN_GROUP_SIZE_DEFAULT, 1, 10);
        VARASUCHUS_MAX_GROUP_SIZE = builder.defineInt("varasuchusMaxGroupSize", VARASUCHUS_MAX_GROUP_SIZE_DEFAULT, 1, 10);
        builder.comment("Additional biome IDs or biome tags where Varasuchus can spawn (e.g., \"minecraft:desert\", \"terralith:volcanic_crater\", \"#c:is_overworld\")");
        VARASUCHUS_ADDITIONAL_BIOMES = builder.defineList("varasuchusAdditionalBiomes", Collections.emptyList());
        builder.comment("Biome IDs or biome tags to exclude from default Varasuchus spawns (e.g., \"minecraft:plains\", \"#minecraft:is_ocean\")");
        VARASUCHUS_EXCLUDED_BIOMES = builder.defineList("varasuchusExcludedBiomes", Collections.emptyList());
        builder.comment("Whether Varasuchus egg blocks generate in worldgen feature patches");
        VARASUCHUS_EGG_BLOCK_WORLDGEN = builder.defineBoolean("varasuchusEggBlockWorldgen", VARASUCHUS_EGG_BLOCK_WORLDGEN_DEFAULT);

        builder.comment("Ignivorus spawn settings");
        IGNIVORUS_SPAWN_WEIGHT = builder.defineInt("ignivorusSpawnWeight", IGNIVORUS_SPAWN_WEIGHT_DEFAULT, 0, SPAWN_WEIGHT_MAX);
        IGNIVORUS_MIN_GROUP_SIZE = builder.defineInt("ignivorusMinGroupSize", IGNIVORUS_MIN_GROUP_SIZE_DEFAULT, 1, 10);
        IGNIVORUS_MAX_GROUP_SIZE = builder.defineInt("ignivorusMaxGroupSize", IGNIVORUS_MAX_GROUP_SIZE_DEFAULT, 1, 10);
        builder.comment("Additional biome IDs or biome tags where Ignivorus can spawn (e.g., \"minecraft:desert\", \"terralith:volcanic_crater\", \"#c:is_overworld\")");
        IGNIVORUS_ADDITIONAL_BIOMES = builder.defineList("ignivorusAdditionalBiomes", Collections.emptyList());
        builder.comment("Biome IDs or biome tags to exclude from default Ignivorus spawns (e.g., \"minecraft:plains\", \"#minecraft:is_ocean\")");
        IGNIVORUS_EXCLUDED_BIOMES = builder.defineList("ignivorusExcludedBiomes", Collections.emptyList());

        builder.comment("Volitans spawn settings (custom underwater spawner for ocean/wetland habitats)");
        VOLITANS_SPAWN_WEIGHT = builder.defineInt("volitansSpawnWeight", VOLITANS_SPAWN_WEIGHT_DEFAULT, 0, SPAWN_WEIGHT_MAX);
        VOLITANS_MIN_GROUP_SIZE = builder.defineInt("volitansMinGroupSize", VOLITANS_MIN_GROUP_SIZE_DEFAULT, 1, 10);
        VOLITANS_MAX_GROUP_SIZE = builder.defineInt("volitansMaxGroupSize", VOLITANS_MAX_GROUP_SIZE_DEFAULT, 1, 10);
        builder.comment("Additional biome IDs or biome tags where Volitans can spawn (e.g., \"minecraft:beach\", \"minecraft:lukewarm_ocean\", \"#minecraft:is_ocean\")");
        VOLITANS_ADDITIONAL_BIOMES = builder.defineList("volitansAdditionalBiomes", Collections.emptyList());
        builder.comment("Biome IDs or biome tags to exclude from default Volitans spawns (e.g., \"minecraft:plains\", \"#minecraft:is_forest\")");
        VOLITANS_EXCLUDED_BIOMES = builder.defineList("volitansExcludedBiomes", Collections.emptyList());

        builder.pop();

        builder.push("gameplay");
        builder.comment("Extra Saints & Dragons griefing toggle layered on top of the vanilla mobGriefing gamerule.");
        builder.comment("If false, dragon-caused block destruction is disabled even when mobGriefing is true.");
        DRAGON_GRIEFING_ENABLED = builder.defineBoolean("dragonGriefingEnabled", DRAGON_GRIEFING_ENABLED_DEFAULT);
        builder.comment("Global toggle for dragon and ability-driven screen shake effects.");
        SCREEN_SHAKE_ENABLED = builder.defineBoolean("screenShakeEnabled", SCREEN_SHAKE_ENABLED_DEFAULT);
        builder.pop();

        builder.build();
    }

    private SaintsDragonsConfig() {
    }
}

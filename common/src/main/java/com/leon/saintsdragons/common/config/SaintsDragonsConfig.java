package com.leon.saintsdragons.common.config;

import com.leon.saintsdragons.platform.ConfigHelper;
import com.leon.saintsdragons.platform.Services;

import java.util.Collections;

public final class SaintsDragonsConfig {
    public static final int RAEVYX_SPAWN_WEIGHT_DEFAULT = 1;
    public static final int RAEVYX_MIN_GROUP_SIZE_DEFAULT = 1;
    public static final int RAEVYX_MAX_GROUP_SIZE_DEFAULT = 2;

    public static final int STEGONAUT_SPAWN_WEIGHT_DEFAULT = 5;
    public static final int STEGONAUT_MIN_GROUP_SIZE_DEFAULT = 1;
    public static final int STEGONAUT_MAX_GROUP_SIZE_DEFAULT = 4;

    public static final int CINDERVANE_SPAWN_WEIGHT_DEFAULT = 3;
    public static final int CINDERVANE_MIN_GROUP_SIZE_DEFAULT = 1;
    public static final int CINDERVANE_MAX_GROUP_SIZE_DEFAULT = 3;

    public static final int NULLJAW_SPAWN_WEIGHT_DEFAULT = 2;
    public static final int NULLJAW_MIN_GROUP_SIZE_DEFAULT = 1;
    public static final int NULLJAW_MAX_GROUP_SIZE_DEFAULT = 2;

    public static final int IGNIVORUS_SPAWN_WEIGHT_DEFAULT = 1;
    public static final int IGNIVORUS_MIN_GROUP_SIZE_DEFAULT = 1;
    public static final int IGNIVORUS_MAX_GROUP_SIZE_DEFAULT = 2;

    public static ConfigHelper.IntValue RAEVYX_SPAWN_WEIGHT;
    public static ConfigHelper.IntValue RAEVYX_MIN_GROUP_SIZE;
    public static ConfigHelper.IntValue RAEVYX_MAX_GROUP_SIZE;
    public static ConfigHelper.ListValue RAEVYX_ADDITIONAL_BIOMES;

    public static ConfigHelper.IntValue STEGONAUT_SPAWN_WEIGHT;
    public static ConfigHelper.IntValue STEGONAUT_MIN_GROUP_SIZE;
    public static ConfigHelper.IntValue STEGONAUT_MAX_GROUP_SIZE;
    public static ConfigHelper.ListValue STEGONAUT_ADDITIONAL_BIOMES;

    public static ConfigHelper.IntValue CINDERVANE_SPAWN_WEIGHT;
    public static ConfigHelper.IntValue CINDERVANE_MIN_GROUP_SIZE;
    public static ConfigHelper.IntValue CINDERVANE_MAX_GROUP_SIZE;
    public static ConfigHelper.ListValue CINDERVANE_ADDITIONAL_BIOMES;

    public static ConfigHelper.IntValue NULLJAW_SPAWN_WEIGHT;
    public static ConfigHelper.IntValue NULLJAW_MIN_GROUP_SIZE;
    public static ConfigHelper.IntValue NULLJAW_MAX_GROUP_SIZE;
    public static ConfigHelper.ListValue NULLJAW_ADDITIONAL_BIOMES;

    public static ConfigHelper.IntValue IGNIVORUS_SPAWN_WEIGHT;
    public static ConfigHelper.IntValue IGNIVORUS_MIN_GROUP_SIZE;
    public static ConfigHelper.IntValue IGNIVORUS_MAX_GROUP_SIZE;
    public static ConfigHelper.ListValue IGNIVORUS_ADDITIONAL_BIOMES;

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

        builder.comment("Raevyx spawn settings");
        RAEVYX_SPAWN_WEIGHT = builder.defineInt("raevyxSpawnWeight", RAEVYX_SPAWN_WEIGHT_DEFAULT, 0, 100);
        RAEVYX_MIN_GROUP_SIZE = builder.defineInt("raevyxMinGroupSize", RAEVYX_MIN_GROUP_SIZE_DEFAULT, 1, 10);
        RAEVYX_MAX_GROUP_SIZE = builder.defineInt("raevyxMaxGroupSize", RAEVYX_MAX_GROUP_SIZE_DEFAULT, 1, 10);
        builder.comment("Additional biomes where Raevyx can spawn (e.g., \"minecraft:desert\", \"terralith:volcanic_crater\")");
        RAEVYX_ADDITIONAL_BIOMES = builder.defineList("raevyxAdditionalBiomes", Collections.emptyList());

        builder.comment("Stegonaut spawn settings");
        STEGONAUT_SPAWN_WEIGHT = builder.defineInt("stegonautSpawnWeight", STEGONAUT_SPAWN_WEIGHT_DEFAULT, 0, 100);
        STEGONAUT_MIN_GROUP_SIZE = builder.defineInt("stegonautMinGroupSize", STEGONAUT_MIN_GROUP_SIZE_DEFAULT, 1, 10);
        STEGONAUT_MAX_GROUP_SIZE = builder.defineInt("stegonautMaxGroupSize", STEGONAUT_MAX_GROUP_SIZE_DEFAULT, 1, 10);
        builder.comment("Additional biomes where Stegonaut can spawn (e.g., \"minecraft:desert\", \"terralith:volcanic_crater\")");
        STEGONAUT_ADDITIONAL_BIOMES = builder.defineList("stegonautAdditionalBiomes", Collections.emptyList());

        builder.comment("Cindervane spawn settings");
        CINDERVANE_SPAWN_WEIGHT = builder.defineInt("cindervaneSpawnWeight", CINDERVANE_SPAWN_WEIGHT_DEFAULT, 0, 100);
        CINDERVANE_MIN_GROUP_SIZE = builder.defineInt("cindervaneMinGroupSize", CINDERVANE_MIN_GROUP_SIZE_DEFAULT, 1, 10);
        CINDERVANE_MAX_GROUP_SIZE = builder.defineInt("cindervaneMaxGroupSize", CINDERVANE_MAX_GROUP_SIZE_DEFAULT, 1, 10);
        builder.comment("Additional biomes where Cindervane can spawn (e.g., \"minecraft:desert\", \"terralith:volcanic_crater\")");
        CINDERVANE_ADDITIONAL_BIOMES = builder.defineList("cindervaneAdditionalBiomes", Collections.emptyList());

        builder.comment("Nulljaw spawn settings");
        NULLJAW_SPAWN_WEIGHT = builder.defineInt("nulljawSpawnWeight", NULLJAW_SPAWN_WEIGHT_DEFAULT, 0, 100);
        NULLJAW_MIN_GROUP_SIZE = builder.defineInt("nulljawMinGroupSize", NULLJAW_MIN_GROUP_SIZE_DEFAULT, 1, 10);
        NULLJAW_MAX_GROUP_SIZE = builder.defineInt("nulljawMaxGroupSize", NULLJAW_MAX_GROUP_SIZE_DEFAULT, 1, 10);
        builder.comment("Additional biomes where Nulljaw can spawn (e.g., \"minecraft:desert\", \"terralith:volcanic_crater\")");
        NULLJAW_ADDITIONAL_BIOMES = builder.defineList("nulljawAdditionalBiomes", Collections.emptyList());

        builder.comment("Ignivorus spawn settings");
        IGNIVORUS_SPAWN_WEIGHT = builder.defineInt("ignivorusSpawnWeight", IGNIVORUS_SPAWN_WEIGHT_DEFAULT, 0, 100);
        IGNIVORUS_MIN_GROUP_SIZE = builder.defineInt("ignivorusMinGroupSize", IGNIVORUS_MIN_GROUP_SIZE_DEFAULT, 1, 10);
        IGNIVORUS_MAX_GROUP_SIZE = builder.defineInt("ignivorusMaxGroupSize", IGNIVORUS_MAX_GROUP_SIZE_DEFAULT, 1, 10);
        builder.comment("Additional biomes where Ignivorus can spawn (e.g., \"minecraft:desert\", \"terralith:volcanic_crater\")");
        IGNIVORUS_ADDITIONAL_BIOMES = builder.defineList("ignivorusAdditionalBiomes", Collections.emptyList());

        builder.pop();
        builder.build();
    }

    private SaintsDragonsConfig() {
    }
}

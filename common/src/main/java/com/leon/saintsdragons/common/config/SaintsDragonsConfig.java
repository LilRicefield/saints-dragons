package com.leon.saintsdragons.common.config;

import com.leon.saintsdragons.platform.ConfigHelper;
import com.leon.saintsdragons.platform.Services;

public final class SaintsDragonsConfig {

    public static final ConfigHelper.IntValue RAEVYX_SPAWN_WEIGHT;
    public static final ConfigHelper.IntValue RAEVYX_MIN_GROUP_SIZE;
    public static final ConfigHelper.IntValue RAEVYX_MAX_GROUP_SIZE;

    public static final ConfigHelper.IntValue STEGONAUT_SPAWN_WEIGHT;
    public static final ConfigHelper.IntValue STEGONAUT_MIN_GROUP_SIZE;
    public static final ConfigHelper.IntValue STEGONAUT_MAX_GROUP_SIZE;

    public static final ConfigHelper.IntValue CINDERVANE_SPAWN_WEIGHT;
    public static final ConfigHelper.IntValue CINDERVANE_MIN_GROUP_SIZE;
    public static final ConfigHelper.IntValue CINDERVANE_MAX_GROUP_SIZE;

    public static final ConfigHelper.IntValue NULLJAW_SPAWN_WEIGHT;
    public static final ConfigHelper.IntValue NULLJAW_MIN_GROUP_SIZE;
    public static final ConfigHelper.IntValue NULLJAW_MAX_GROUP_SIZE;

    static {
        ConfigHelper.ConfigBuilder builder = Services.PLATFORM.getConfigHelper()
                .commonBuilder("saintsdragonsspawning.toml");

        builder.push("spawning");

        RAEVYX_SPAWN_WEIGHT = builder.defineInt("raevyxSpawnWeight", 1, 0, 100);
        RAEVYX_MIN_GROUP_SIZE = builder.defineInt("raevyxMinGroupSize", 1, 1, 10);
        RAEVYX_MAX_GROUP_SIZE = builder.defineInt("raevyxMaxGroupSize", 2, 1, 10);

        STEGONAUT_SPAWN_WEIGHT = builder.defineInt("stegonautSpawnWeight", 5, 0, 100);
        STEGONAUT_MIN_GROUP_SIZE = builder.defineInt("stegonautMinGroupSize", 1, 1, 10);
        STEGONAUT_MAX_GROUP_SIZE = builder.defineInt("stegonautMaxGroupSize", 4, 1, 10);

        CINDERVANE_SPAWN_WEIGHT = builder.defineInt("cindervaneSpawnWeight", 4, 0, 100);
        CINDERVANE_MIN_GROUP_SIZE = builder.defineInt("cindervaneMinGroupSize", 1, 1, 10);
        CINDERVANE_MAX_GROUP_SIZE = builder.defineInt("cindervaneMaxGroupSize", 3, 1, 10);

        NULLJAW_SPAWN_WEIGHT = builder.defineInt("nulljawSpawnWeight", 2, 0, 100);
        NULLJAW_MIN_GROUP_SIZE = builder.defineInt("nulljawMinGroupSize", 1, 1, 10);
        NULLJAW_MAX_GROUP_SIZE = builder.defineInt("nulljawMaxGroupSize", 2, 1, 10);

        builder.pop();
        builder.build();
    }

    public static void bootstrap() {
        // Trigger class loading to ensure platform config registration runs during mod init.
    }

    private SaintsDragonsConfig() {
    }
}

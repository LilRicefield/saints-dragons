package com.leon.saintsdragons.common.config;

import net.minecraftforge.common.ForgeConfigSpec;

public class SaintsDragonsConfig {

    public static final ForgeConfigSpec SPEC;

    // Raevyx (Lightning Dragon)
    public static final ForgeConfigSpec.IntValue RAEVYX_SPAWN_WEIGHT;
    public static final ForgeConfigSpec.IntValue RAEVYX_MIN_GROUP_SIZE;
    public static final ForgeConfigSpec.IntValue RAEVYX_MAX_GROUP_SIZE;

    // Stegonaut (Primitive Drake)
    public static final ForgeConfigSpec.IntValue STEGONAUT_SPAWN_WEIGHT;
    public static final ForgeConfigSpec.IntValue STEGONAUT_MIN_GROUP_SIZE;
    public static final ForgeConfigSpec.IntValue STEGONAUT_MAX_GROUP_SIZE;

    // Cindervane (Amphithere)
    public static final ForgeConfigSpec.IntValue CINDERVANE_SPAWN_WEIGHT;
    public static final ForgeConfigSpec.IntValue CINDERVANE_MIN_GROUP_SIZE;
    public static final ForgeConfigSpec.IntValue CINDERVANE_MAX_GROUP_SIZE;

    // Nulljaw (Rift Drake)
    public static final ForgeConfigSpec.IntValue NULLJAW_SPAWN_WEIGHT;
    public static final ForgeConfigSpec.IntValue NULLJAW_MIN_GROUP_SIZE;
    public static final ForgeConfigSpec.IntValue NULLJAW_MAX_GROUP_SIZE;

    static {
        ForgeConfigSpec.Builder BUILDER = new ForgeConfigSpec.Builder();

        BUILDER.push("spawning");
        BUILDER.comment("Configure spawn rates and group sizes for all dragons");

        // Raevyx
        BUILDER.comment("Raevyx");
        RAEVYX_SPAWN_WEIGHT = BUILDER
                .comment("Spawn weight (0 = disabled, higher = more common)")
                .defineInRange("raevyxSpawnWeight", 1, 0, 100);
        RAEVYX_MIN_GROUP_SIZE = BUILDER
                .comment("Minimum group size")
                .defineInRange("raevyxMinGroupSize", 1, 1, 10);
        RAEVYX_MAX_GROUP_SIZE = BUILDER
                .comment("Maximum group size")
                .defineInRange("raevyxMaxGroupSize", 2, 1, 10);

        // Stegonaut
        BUILDER.comment("Stegonaut");
        STEGONAUT_SPAWN_WEIGHT = BUILDER
                .comment("Spawn weight (0 = disabled, higher = more common)")
                .defineInRange("stegonautSpawnWeight", 5, 0, 100);
        STEGONAUT_MIN_GROUP_SIZE = BUILDER
                .comment("Minimum group size")
                .defineInRange("stegonautMinGroupSize", 1, 1, 10);
        STEGONAUT_MAX_GROUP_SIZE = BUILDER
                .comment("Maximum group size")
                .defineInRange("stegonautMaxGroupSize", 4, 1, 10);

        // Cindervane
        BUILDER.comment("Cindervane");
        CINDERVANE_SPAWN_WEIGHT = BUILDER
                .comment("Spawn weight (0 = disabled, higher = more common)")
                .defineInRange("cindervaneSpawnWeight", 4, 0, 100);
        CINDERVANE_MIN_GROUP_SIZE = BUILDER
                .comment("Minimum group size")
                .defineInRange("cindervaneMinGroupSize", 1, 1, 10);
        CINDERVANE_MAX_GROUP_SIZE = BUILDER
                .comment("Maximum group size")
                .defineInRange("cindervaneMaxGroupSize", 3, 1, 10);

        // Nulljaw
        BUILDER.comment("Nulljaw");
        NULLJAW_SPAWN_WEIGHT = BUILDER
                .comment("Spawn weight (0 = disabled, higher = more common)")
                .defineInRange("nulljawSpawnWeight", 2, 0, 100);
        NULLJAW_MIN_GROUP_SIZE = BUILDER
                .comment("Minimum group size")
                .defineInRange("nulljawMinGroupSize", 1, 1, 10);
        NULLJAW_MAX_GROUP_SIZE = BUILDER
                .comment("Maximum group size")
                .defineInRange("nulljawMaxGroupSize", 2, 1, 10);

        BUILDER.pop();
        SPEC = BUILDER.build();
    }
}

package com.leon.saintsdragons.forge.platform;

import net.minecraftforge.common.ForgeConfigSpec;

public final class ForgeClientConfig {
    public static final ForgeConfigSpec CLIENT_SPEC;

    public static ForgeConfigSpec.BooleanValue FIRST_PERSON_BANKING_CAMERA_ENABLED;
    public static ForgeConfigSpec.BooleanValue DIVE_CAMERA_WOBBLE_ENABLED;
    public static ForgeConfigSpec.BooleanValue DIVE_SPEED_LINES_ENABLED;
    public static ForgeConfigSpec.BooleanValue GENERIC_DIVE_LOOP_ENABLED;
    public static ForgeConfigSpec.IntValue SWARM_BATTLE_MUSIC_VOLUME;

    static {
        ForgeConfigSpec.Builder builder = new ForgeConfigSpec.Builder();

        builder.comment("Local client-only settings").push("client");
        FIRST_PERSON_BANKING_CAMERA_ENABLED = builder
                .comment("Enable first-person banking and barrel-roll camera effects while riding dragons")
                .define("first_person_banking_camera", true);
        DIVE_CAMERA_WOBBLE_ENABLED = builder
                .comment("Enable camera wobble while diving quickly on flying dragons")
                .define("dive_camera_wobble", true);
        DIVE_SPEED_LINES_ENABLED = builder
                .comment("Enable screen edge speed lines while diving quickly on flying dragons")
                .define("dive_speed_lines", true);
        GENERIC_DIVE_LOOP_ENABLED = builder
                .comment("Enable the local generic wind loop while riding diving flying dragons")
                .define("generic_dive_loop", true);
        SWARM_BATTLE_MUSIC_VOLUME = builder
                .comment("Local Draconian Swarm battle music volume, in percent")
                .defineInRange("swarm_battle_music_volume", 100, 0, 100);
        builder.pop();

        CLIENT_SPEC = builder.build();
    }

    private ForgeClientConfig() {
    }
}

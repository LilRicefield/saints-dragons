package com.leon.saintsdragons.forge.platform;

import net.minecraftforge.common.ForgeConfigSpec;

public final class ForgeClientConfig {
    public static final ForgeConfigSpec CLIENT_SPEC;

    public static ForgeConfigSpec.BooleanValue FIRST_PERSON_BANKING_CAMERA_ENABLED;
    public static ForgeConfigSpec.BooleanValue DIVE_CAMERA_WOBBLE_ENABLED;

    static {
        ForgeConfigSpec.Builder builder = new ForgeConfigSpec.Builder();

        builder.comment("Local client-only settings").push("client");
        FIRST_PERSON_BANKING_CAMERA_ENABLED = builder
                .comment("Enable first-person banking and barrel-roll camera effects while riding dragons")
                .define("first_person_banking_camera", true);
        DIVE_CAMERA_WOBBLE_ENABLED = builder
                .comment("Enable camera wobble while diving quickly on flying dragons")
                .define("dive_camera_wobble", true);
        builder.pop();

        CLIENT_SPEC = builder.build();
    }

    private ForgeClientConfig() {
    }
}

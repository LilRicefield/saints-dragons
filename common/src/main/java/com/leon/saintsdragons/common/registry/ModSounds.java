package com.leon.saintsdragons.common.registry;

import com.leon.saintsdragons.common.SaintsDragonsCommon;
import com.leon.saintsdragons.platform.RegistryHelper;
import com.leon.saintsdragons.platform.Services;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.sounds.SoundEvent;

import java.util.function.Supplier;

public final class ModSounds {
    private static final RegistryHelper.RegistryWrapper<SoundEvent> REGISTER =
            Services.PLATFORM.getRegistryHelper()
                    .create(Registries.SOUND_EVENT, () -> BuiltInRegistries.SOUND_EVENT, SaintsDragonsCommon.MOD_ID);

    // Stegonaut
    public static final Supplier<SoundEvent> STEGONAUT_GRUMBLE_1 = registerSound("stegonaut_grumble1");
    public static final Supplier<SoundEvent> STEGONAUT_GRUMBLE_2 = registerSound("stegonaut_grumble2");
    public static final Supplier<SoundEvent> STEGONAUT_GRUMBLE_3 = registerSound("stegonaut_grumble3");
    public static final Supplier<SoundEvent> STEGONAUT_HURT = registerSound("stegonaut_hurt");
    public static final Supplier<SoundEvent> STEGONAUT_DIE = registerSound("stegonaut_die");

    // Raevyx
    public static final Supplier<SoundEvent> RAEVYX_PURR = registerSound("raevyx_purr");
    public static final Supplier<SoundEvent> RAEVYX_SNORT = registerSound("raevyx_snort");
    public static final Supplier<SoundEvent> RAEVYX_CHUFF = registerSound("raevyx_chuff");
    public static final Supplier<SoundEvent> RAEVYX_CONTENT = registerSound("raevyx_content");
    public static final Supplier<SoundEvent> RAEVYX_EXCITED = registerSound("raevyx_excited");
    public static final Supplier<SoundEvent> RAEVYX_ROAR = registerSound("raevyx_roar");
    public static final Supplier<SoundEvent> RAEVYX_SUMMON_STORM_START = registerSound("raevyx_summon_storm_ground_start");
    public static final Supplier<SoundEvent> RAEVYX_SUMMON_STORM = registerSound("raevyx_summon_storm_ground");
    public static final Supplier<SoundEvent> RAEVYX_SUMMON_STORM_END = registerSound("raevyx_summon_storm_ground_end");
    public static final Supplier<SoundEvent> RAEVYX_SUMMON_STORM_AIR_START = registerSound("raevyx_summon_storm_air_start");
    public static final Supplier<SoundEvent> RAEVYX_SUMMON_STORM_AIR = registerSound("raevyx_summon_storm_air");
    public static final Supplier<SoundEvent> RAEVYX_GROWL_WARNING = registerSound("raevyx_growl_warning");
    public static final Supplier<SoundEvent> RAEVYX_WALK = registerSound("raevyx_walk");
    public static final Supplier<SoundEvent> RAEVYX_RUN = registerSound("raevyx_run");
    public static final Supplier<SoundEvent> RAEVYX_HURT = registerSound("raevyx_hurt");
    public static final Supplier<SoundEvent> RAEVYX_BITE = registerSound("raevyx_bite");
    public static final Supplier<SoundEvent> RAEVYX_HORNGORE = registerSound("raevyx_horngore");
    public static final Supplier<SoundEvent> RAEVYX_DIE = registerSound("raevyx_die");
    public static final Supplier<SoundEvent> RAEVYX_FLAP = registerSound("raevyx_flap");
    public static final Supplier<SoundEvent> RAEVYX_GRUMBLE_1 = registerSound("raevyx_grumble_1");
    public static final Supplier<SoundEvent> RAEVYX_GRUMBLE_2 = registerSound("raevyx_grumble_2");
    public static final Supplier<SoundEvent> RAEVYX_GRUMBLE_3 = registerSound("raevyx_grumble_3");
    public static final Supplier<SoundEvent> RAEVYX_TAKEOFF = registerSound("raevyx_takeoff");
    public static final Supplier<SoundEvent> RAEVYX_LIGHTNING_BEAM_START = registerSound("raevyx_lightning_beam_start");
    public static final Supplier<SoundEvent> RAEVYX_LIGHTNING_BEAMING = registerSound("raevyx_lightning_beaming");
    public static final Supplier<SoundEvent> RAEVYX_LIGHTNING_BEAM_STOP = registerSound("raevyx_lightning_beam_stop");

    // Baby Raevyx
    public static final Supplier<SoundEvent> BABY_RAEVYX_HURT = registerSound("baby_raevyx_hurt");
    public static final Supplier<SoundEvent> BABY_RAEVYX_DIE = registerSound("baby_raevyx_die");

    // Cindervane
    public static final Supplier<SoundEvent> CINDERVANE_GRUMBLE_1 = registerSound("cindervane_grumble1");
    public static final Supplier<SoundEvent> CINDERVANE_GRUMBLE_2 = registerSound("cindervane_grumble2");
    public static final Supplier<SoundEvent> CINDERVANE_GRUMBLE_3 = registerSound("cindervane_grumble3");
    public static final Supplier<SoundEvent> CINDERVANE_ROAR = registerSound("cindervane_roar");
    public static final Supplier<SoundEvent> CINDERVANE_HURT = registerSound("cindervane_hurt");
    public static final Supplier<SoundEvent> CINDERVANE_BITE = registerSound("cindervane_bite");
    public static final Supplier<SoundEvent> CINDERVANE_DIE = registerSound("cindervane_die");
    public static final Supplier<SoundEvent> CINDERVANE_WALK = registerSound("cindervane_walk");
    public static final Supplier<SoundEvent> CINDERVANE_RUN = registerSound("cindervane_run");
    public static final Supplier<SoundEvent> CINDERVANE_FLAP = registerSound("cindervane_flap");
    public static final Supplier<SoundEvent> CINDERVANE_TAKEOFF = registerSound("cindervane_takeoff");

    // Nulljaw
    public static final Supplier<SoundEvent> NULLJAW_GRUMBLE_1 = registerSound("nulljaw_grumble1");
    public static final Supplier<SoundEvent> NULLJAW_GRUMBLE_2 = registerSound("nulljaw_grumble2");
    public static final Supplier<SoundEvent> NULLJAW_GRUMBLE_3 = registerSound("nulljaw_grumble3");
    public static final Supplier<SoundEvent> NULLJAW_PHASE1 = registerSound("nulljaw_phase1");
    public static final Supplier<SoundEvent> NULLJAW_PHASE2_START = registerSound("nulljaw_phase2_start");
    public static final Supplier<SoundEvent> NULLJAW_PHASE2 = registerSound("nulljaw_phase2");
    public static final Supplier<SoundEvent> NULLJAW_PHASE2_END = registerSound("nulljaw_phase2_end");
    public static final Supplier<SoundEvent> NULLJAW_ROAR = registerSound("nulljaw_roar");
    public static final Supplier<SoundEvent> NULLJAW_ROARCLAW = registerSound("nulljaw_roarclaw");
    public static final Supplier<SoundEvent> NULLJAW_WALK = registerSound("nulljaw_walk");
    public static final Supplier<SoundEvent> NULLJAW_RUN = registerSound("nulljaw_run");
    public static final Supplier<SoundEvent> NULLJAW_WALK2 = registerSound("nulljaw_walk2");
    public static final Supplier<SoundEvent> NULLJAW_RUN2 = registerSound("nulljaw_run2");
    public static final Supplier<SoundEvent> NULLJAW_CLAW = registerSound("nulljaw_claw");
    public static final Supplier<SoundEvent> NULLJAW_BITE = registerSound("nulljaw_bite");
    public static final Supplier<SoundEvent> NULLJAW_HORNGORE = registerSound("nulljaw_horngore");
    public static final Supplier<SoundEvent> NULLJAW_HURT = registerSound("nulljaw_hurt");
    public static final Supplier<SoundEvent> NULLJAW_DIE = registerSound("nulljaw_die");

    // Ignivorus
    public static final Supplier<SoundEvent> IGNIVORUS_ROAR = registerSound("ignivorus_roar");
    public static final Supplier<SoundEvent> IGNIVORUS_BITE = registerSound("ignivorus_bite");
    public static final Supplier<SoundEvent> IGNIVORUS_WALK = registerSound("ignivorus_walk");
    public static final Supplier<SoundEvent> IGNIVORUS_RUN = registerSound("ignivorus_run");
    public static final Supplier<SoundEvent> IGNIVORUS_MAGMA_PILLAR = registerSound("ignivorus_magma_pillar");
    public static final Supplier<SoundEvent> IGNIVORUS_BODY_SLAM = registerSound("ignivorus_body_slam");
    public static final Supplier<SoundEvent> IGNIVORUS_HURT = registerSound("ignivorus_hurt");
    public static final Supplier<SoundEvent> IGNIVORUS_DIE = registerSound("ignivorus_die");
    public static final Supplier<SoundEvent> IGNIVORUS_FIRE_BREATH_START = registerSound("ignivorus_fire_breath_start");
    public static final Supplier<SoundEvent> IGNIVORUS_FIRE_BREATHING = registerSound("ignivorus_fire_breathing");
    public static final Supplier<SoundEvent> IGNIVORUS_FIRE_BREATH_END = registerSound("ignivorus_fire_breath_end");
    public static final Supplier<SoundEvent> IGNIVORUS_TAKEOFF = registerSound("ignivorus_takeoff");
    public static final Supplier<SoundEvent> IGNIVORUS_FLAP = registerSound("ignivorus_flap");
    public static final Supplier<SoundEvent> IGNIVORUS_GRUMBLE_1 = registerSound("ignivorus_grumble1");
    public static final Supplier<SoundEvent> IGNIVORUS_GRUMBLE_2 = registerSound("ignivorus_grumble2");
    public static final Supplier<SoundEvent> IGNIVORUS_GRUMBLE_3 = registerSound("ignivorus_grumble3");
    public static final Supplier<SoundEvent> IGNIVORUS_ULTIMATE_START = registerSound("ignivorus_ultimate_start");
    public static final Supplier<SoundEvent> IGNIVORUS_ULTIMATE = registerSound("ignivorus_ultimate");
    public static final Supplier<SoundEvent> IGNIVORUS_ULTIMATE_END = registerSound("ignivorus_ultimate_end");

    private ModSounds() {
    }

    private static Supplier<SoundEvent> registerSound(String name) {
        return REGISTER.register(name, () -> SoundEvent.createVariableRangeEvent(SaintsDragonsCommon.rl(name)));
    }

    public static void register() {
        REGISTER.register();
    }
}

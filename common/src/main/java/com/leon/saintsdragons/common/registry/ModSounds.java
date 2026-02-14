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

    public static final Supplier<SoundEvent> IVY_TRADE_START = registerSound("ivy_trade_start");
    public static final Supplier<SoundEvent> IVY_TRADE_STOP = registerSound("ivy_trade_stop");
    public static final Supplier<SoundEvent> BLEEDING_BOLT = registerSound("bleeding_bolt");
    public static final Supplier<SoundEvent> DRACONIC_CODEX_FLIP = registerSound("draconic_codex_flip");

    // Stegonaut
    public static final Supplier<SoundEvent> STEGONAUT_GRUMBLE_1 = registerSound("stegonaut_grumble1");
    public static final Supplier<SoundEvent> STEGONAUT_GRUMBLE_2 = registerSound("stegonaut_grumble2");
    public static final Supplier<SoundEvent> STEGONAUT_GRUMBLE_3 = registerSound("stegonaut_grumble3");
    public static final Supplier<SoundEvent> STEGONAUT_HURT = registerSound("stegonaut_hurt");
    public static final Supplier<SoundEvent> STEGONAUT_DIE = registerSound("stegonaut_die");
    public static final Supplier<SoundEvent> STEGONAUT_EAT = registerSound("stegonaut_eat");
    public static final Supplier<SoundEvent> STEGONAUT_WALK = registerSound("stegonaut_walk");
    public static final Supplier<SoundEvent> STEGONAUT_RUN = registerSound("stegonaut_run");
    public static final Supplier<SoundEvent> STEGONAUT_CHIN_SLAM = registerSound("stegonaut_chin_slam");
    public static final Supplier<SoundEvent> STEGONAUT_BITE = registerSound("stegonaut_bite");
    public static final Supplier<SoundEvent> STEGONAUT_GROUND_EATING_SHOOT = registerSound("stegonaut_ground_eating_shoot");
    public static final Supplier<SoundEvent> STEGONAUT_GROUND_EATING_CANCEL = registerSound("stegonaut_ground_eating_cancel");
    public static final Supplier<SoundEvent> STEGONAUT_GROUND_EATING = registerSound("stegonaut_ground_eating");

    // Raevyx
    public static final Supplier<SoundEvent> RAEVYX_PURR = registerSound("raevyx_purr");
    public static final Supplier<SoundEvent> RAEVYX_CHUFF = registerSound("raevyx_chuff");
    public static final Supplier<SoundEvent> RAEVYX_CONTENT = registerSound("raevyx_content");
    public static final Supplier<SoundEvent> RAEVYX_EXCITED = registerSound("raevyx_excited");
    public static final Supplier<SoundEvent> RAEVYX_ROAR = registerSound("raevyx_roar");
    public static final Supplier<SoundEvent> RAEVYX_SUMMON_STORM = registerSound("raevyx_summon_storm");
    public static final Supplier<SoundEvent> RAEVYX_SUMMON_STORM_AIR = registerSound("raevyx_summon_storm_air");
    public static final Supplier<SoundEvent> RAEVYX_WALK = registerSound("raevyx_walk");
    public static final Supplier<SoundEvent> RAEVYX_RUN = registerSound("raevyx_run");
    public static final Supplier<SoundEvent> RAEVYX_LANDED = registerSound("raevyx_landed");
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
    public static final Supplier<SoundEvent> RAEVYX_EAT = registerSound("raevyx_eat");
    public static final Supplier<SoundEvent> RAEVYX_DODGE = registerSound("raevyx_dodge");
    public static final Supplier<SoundEvent> RAEVYX_DASH = registerSound("raevyx_dash");

    // Cindervane
    public static final Supplier<SoundEvent> CINDERVANE_GRUMBLE_1 = registerSound("cindervane_grumble1");
    public static final Supplier<SoundEvent> CINDERVANE_GRUMBLE_2 = registerSound("cindervane_grumble2");
    public static final Supplier<SoundEvent> CINDERVANE_GRUMBLE_3 = registerSound("cindervane_grumble3");
    public static final Supplier<SoundEvent> CINDERVANE_ROAR = registerSound("cindervane_roar");
    public static final Supplier<SoundEvent> CINDERVANE_HURT = registerSound("cindervane_hurt");
    public static final Supplier<SoundEvent> CINDERVANE_BITE = registerSound("cindervane_bite");
    public static final Supplier<SoundEvent> CINDERVANE_SLASH = registerSound("cindervane_slash");
    public static final Supplier<SoundEvent> CINDERVANE_DIE = registerSound("cindervane_die");
    public static final Supplier<SoundEvent> CINDERVANE_RUN = registerSound("cindervane_run");
    public static final Supplier<SoundEvent> CINDERVANE_FLAP = registerSound("cindervane_flap");
    public static final Supplier<SoundEvent> CINDERVANE_TAKEOFF = registerSound("cindervane_takeoff");
    public static final Supplier<SoundEvent> CINDERVANE_LANDED = registerSound("cindervane_landed");
    public static final Supplier<SoundEvent> CINDERVANE_EAT = registerSound("cindervane_eat");
    public static final Supplier<SoundEvent> CINDERVANE_MAGMA_BLAST = registerSound("cindervane_magma_blast");

    // Nulljaw
    public static final Supplier<SoundEvent> NULLJAW_GRUMBLE_1 = registerSound("nulljaw_grumble1");
    public static final Supplier<SoundEvent> NULLJAW_GRUMBLE_2 = registerSound("nulljaw_grumble2");
    public static final Supplier<SoundEvent> NULLJAW_GRUMBLE_3 = registerSound("nulljaw_grumble3");
    public static final Supplier<SoundEvent> NULLJAW_PHASE1 = registerSound("nulljaw_phase1");
    public static final Supplier<SoundEvent> NULLJAW_PHASE2_START = registerSound("nulljaw_phase2_start");
    public static final Supplier<SoundEvent> NULLJAW_PHASE2 = registerSound("nulljaw_phase2");
    public static final Supplier<SoundEvent> NULLJAW_PHASE2_END = registerSound("nulljaw_phase2_end");
    public static final Supplier<SoundEvent> NULLJAW_ROAR = registerSound("nulljaw_roar");
    public static final Supplier<SoundEvent> NULLJAW_ROAR2 = registerSound("nulljaw_roar2");
    public static final Supplier<SoundEvent> NULLJAW_WALK = registerSound("nulljaw_walk");
    public static final Supplier<SoundEvent> NULLJAW_RUN = registerSound("nulljaw_run");
    public static final Supplier<SoundEvent> NULLJAW_WALK2 = registerSound("nulljaw_walk2");
    public static final Supplier<SoundEvent> NULLJAW_RUN2 = registerSound("nulljaw_run2");
    public static final Supplier<SoundEvent> NULLJAW_CLAW = registerSound("nulljaw_claw");
    public static final Supplier<SoundEvent> NULLJAW_TAIL_SWIPE = registerSound("nulljaw_tail_swipe");
    public static final Supplier<SoundEvent> NULLJAW_TAIL_ATTACK = registerSound("nulljaw_tail_attack");
    public static final Supplier<SoundEvent> NULLJAW_PHASE2_DASH = registerSound("nulljaw_phase2_dash");
    public static final Supplier<SoundEvent> NULLJAW_BITE = registerSound("nulljaw_bite");
    public static final Supplier<SoundEvent> NULLJAW_HORNGORE = registerSound("nulljaw_horngore");
    public static final Supplier<SoundEvent> NULLJAW_HURT = registerSound("nulljaw_hurt");
    public static final Supplier<SoundEvent> NULLJAW_DIE = registerSound("nulljaw_die");
    public static final Supplier<SoundEvent> NULLJAW_EAT = registerSound("nulljaw_eat");

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
    public static final Supplier<SoundEvent> IGNIVORUS_ULTIMATE_START_AIR = registerSound("ignivorus_ultimate_start_air");
    public static final Supplier<SoundEvent> IGNIVORUS_ULTIMATE_AIR = registerSound("ignivorus_ultimate_air");
    public static final Supplier<SoundEvent> IGNIVORUS_ULTIMATE_END_AIR = registerSound("ignivorus_ultimate_end_air");
    public static final Supplier<SoundEvent> IGNIVORUS_LANDED = registerSound("ignivorus_landed");
    public static final Supplier<SoundEvent> IGNIVORUS_PHASE2_LANDED = registerSound("ignivorus_phase2_landed");
    public static final Supplier<SoundEvent> IGNIVORUS_EAT = registerSound("ignivorus_eat");
    public static final Supplier<SoundEvent> IGNIVORUS_BULLDOZER_ENTER = registerSound("ignivorus_bulldozer_enter");
    public static final Supplier<SoundEvent> IGNIVORUS_BULLDOZING = registerSound("ignivorus_bulldozing");
    public static final Supplier<SoundEvent> IGNIVORUS_BULLDOZER_EXIT = registerSound("ignivorus_bulldozer_exit");
    public static final Supplier<SoundEvent> IGNIVORUS_WING_SWIPE = registerSound("ignivorus_wing_swipe");
    public static final Supplier<SoundEvent> IGNIVORUS_PHASE2_ENTER = registerSound("ignivorus_phase2_enter");
    public static final Supplier<SoundEvent> IGNIVORUS_PHASE2_EXIT = registerSound("ignivorus_phase2_exit");
    public static final Supplier<SoundEvent> IGNIVORUS_PHASE2_WALK = registerSound("ignivorus_phase2_walk");
    public static final Supplier<SoundEvent> IGNIVORUS_PHASE2_RUN = registerSound("ignivorus_phase2_run");
    public static final Supplier<SoundEvent> IGNIVORUS_STOMP = registerSound("ignivorus_stomp");
    public static final Supplier<SoundEvent> IGNIVORUS_IMPACT = registerSound("ignivorus_impact");
    public static final Supplier<SoundEvent> IGNIVORUS_LEAP = registerSound("ignivorus_leap");
    public static final Supplier<SoundEvent> IGNIVORUS_LEVEL1_CHARGE = registerSound("ignivorus_level1_charge");
    public static final Supplier<SoundEvent> IGNIVORUS_LEVEL1_SHOOTS = registerSound("ignivorus_level1_shoots");
    public static final Supplier<SoundEvent> IGNIVORUS_LEVEL2_CHARGE = registerSound("ignivorus_level2_charge");
    public static final Supplier<SoundEvent> IGNIVORUS_LEVEL2_SHOOTS = registerSound("ignivorus_level2_shoots");
    public static final Supplier<SoundEvent> IGNIVORUS_LEVEL3_CHARGE = registerSound("ignivorus_level3_charge");
    public static final Supplier<SoundEvent> IGNIVORUS_LEVEL3_SHOOTS = registerSound("ignivorus_level3_shoots");

    private static Supplier<SoundEvent> registerSound(String name) {
        return REGISTER.register(name, () -> SoundEvent.createVariableRangeEvent(SaintsDragonsCommon.rl(name)));
    }

    public static void register() {
        REGISTER.register();
    }
}

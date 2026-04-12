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
    public static final Supplier<SoundEvent> IVY_REACTION_TO_EGG = registerSound("ivy_reaction_to_egg");
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
    public static final Supplier<SoundEvent> RAEVYX_GROUND_REND = registerSound("raevyx_ground_rend");

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

    // Varasuchus
    public static final Supplier<SoundEvent> VARASUCHUS_GRUMBLE_1 = registerSound("varasuchus_grumble1");
    public static final Supplier<SoundEvent> VARASUCHUS_GRUMBLE_2 = registerSound("varasuchus_grumble2");
    public static final Supplier<SoundEvent> VARASUCHUS_GRUMBLE_3 = registerSound("varasuchus_grumble3");
    public static final Supplier<SoundEvent> VARASUCHUS_PHASE1 = registerSound("varasuchus_phase1");
    public static final Supplier<SoundEvent> VARASUCHUS_PHASE1_UNDERWATER = registerSound("varasuchus_phase1_underwater");
    public static final Supplier<SoundEvent> VARASUCHUS_PHASE2 = registerSound("varasuchus_phase2");
    public static final Supplier<SoundEvent> VARASUCHUS_PHASE2_UNDERWATER = registerSound("varasuchus_phase2_underwater");
    public static final Supplier<SoundEvent> VARASUCHUS_ROAR = registerSound("varasuchus_roar");
    public static final Supplier<SoundEvent> VARASUCHUS_WALK = registerSound("varasuchus_walk");
    public static final Supplier<SoundEvent> VARASUCHUS_RUN = registerSound("varasuchus_run");
    public static final Supplier<SoundEvent> VARASUCHUS_WALK2 = registerSound("varasuchus_walk2");
    public static final Supplier<SoundEvent> VARASUCHUS_RUN2 = registerSound("varasuchus_run2");
    public static final Supplier<SoundEvent> VARASUCHUS_BUCKING = registerSound("varasuchus_bucking");
    public static final Supplier<SoundEvent> VARASUCHUSCLAW = registerSound("varasuchus_claw");
    public static final Supplier<SoundEvent> VARASUCHUS_TAIL_SWIPE = registerSound("varasuchus_tail_swipe");
    public static final Supplier<SoundEvent> VARASUCHUS_TAIL_ATTACK = registerSound("varasuchus_tail_attack");
    public static final Supplier<SoundEvent> VARASUCHUS_PHASE2_DASH = registerSound("varasuchus_phase2_dash");
    public static final Supplier<SoundEvent> VARASUCHUS_SLASH_BARRAGE = registerSound("varasuchus_slash_barrage");
    public static final Supplier<SoundEvent> VARASUCHUS_BITE1 = registerSound("varasuchus_bite1");
    public static final Supplier<SoundEvent> VARASUCHUS_BITE2 = registerSound("varasuchus_bite2");
    public static final Supplier<SoundEvent> VARASUCHUS_HORNGORE = registerSound("varasuchus_horngore");
    public static final Supplier<SoundEvent> VARASUCHUS_HURT = registerSound("varasuchus_hurt");
    public static final Supplier<SoundEvent> VARASUCHUS_DIE = registerSound("varasuchus_die");
    public static final Supplier<SoundEvent> VARASUCHUS_EAT = registerSound("varasuchus_eat");

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

    // Volitans
    public static final Supplier<SoundEvent> VOLITANS_ROAR = registerSound("volitans_roar");
    public static final Supplier<SoundEvent> VOLITANS_ROAR_AIR_WATER = registerSound("volitans_roar_air_water");
    public static final Supplier<SoundEvent> VOLITANS_BITE = registerSound("volitans_bite");
    public static final Supplier<SoundEvent> VOLITANS_HORN_GORE = registerSound("volitans_horn_gore");
    public static final Supplier<SoundEvent> VOLITANS_CLAWS = registerSound("volitans_claws");
    public static final Supplier<SoundEvent> VOLITANS_BREATH_START = registerSound("volitans_breath_start");
    public static final Supplier<SoundEvent> VOLITANS_BREATHING = registerSound("volitans_breathing");
    public static final Supplier<SoundEvent> VOLITANS_BREATH_END = registerSound("volitans_breath_end");
    public static final Supplier<SoundEvent> VOLITANS_ENTER_BURROW = registerSound("volitans_enter_burrow");
    public static final Supplier<SoundEvent> VOLITANS_BURROW_IDLE = registerSound("volitans_burrow_idle");
    public static final Supplier<SoundEvent> VOLITANS_BURROW_MOVE = registerSound("volitans_burrow_move");
    public static final Supplier<SoundEvent> VOLITANS_BURROW_EXIT = registerSound("volitans_burrow_exit");
    public static final Supplier<SoundEvent> VOLITANS_GRUMBLE_1 = registerSound("volitans_grumble1");
    public static final Supplier<SoundEvent> VOLITANS_GRUMBLE_2 = registerSound("volitans_grumble2");
    public static final Supplier<SoundEvent> VOLITANS_GRUMBLE_3 = registerSound("volitans_grumble3");
    public static final Supplier<SoundEvent> VOLITANS_FLAP = registerSound("volitans_flap");
    public static final Supplier<SoundEvent> VOLITANS_WALK = registerSound("volitans_walk");
    public static final Supplier<SoundEvent> VOLITANS_RUN = registerSound("volitans_run");
    public static final Supplier<SoundEvent> VOLITANS_EAT = registerSound("volitans_eat");
    public static final Supplier<SoundEvent> VOLITANS_POISON_BALL_READY = registerSound("volitans_poison_ball_ready");
    public static final Supplier<SoundEvent> VOLITANS_POISON_BALL_SHOOT = registerSound("volitans_poison_ball_shoot");
    public static final Supplier<SoundEvent> VOLITANS_DASH_FORWARD = registerSound("volitans_dash_forward");
    public static final Supplier<SoundEvent> VOLITANS_DASH_BACKWARDS = registerSound("volitans_dash_backwards");
    public static final Supplier<SoundEvent> VOLITANS_DODGE = registerSound("volitans_dodge");
    public static final Supplier<SoundEvent> VOLITANS_HURT = registerSound("volitans_hurt");
    public static final Supplier<SoundEvent> VOLITANS_DIE = registerSound("volitans_die");
    public static final Supplier<SoundEvent> VOLITANS_TAKEOFF = registerSound("volitans_takeoff");
    public static final Supplier<SoundEvent> VOLITANS_LANDED = registerSound("volitans_landed");
    public static final Supplier<SoundEvent> VOLITANS_SLAMMING = registerSound("volitans_slamming");
    public static final Supplier<SoundEvent> VOLITANS_SLAMMED = registerSound("volitans_slammed");

    // Nulljaw
    public static final Supplier<SoundEvent> NULLJAW_GRUMBLE_1 = registerSound("nulljaw_grumble1");
    public static final Supplier<SoundEvent> NULLJAW_GRUMBLE_2 = registerSound("nulljaw_grumble2");
    public static final Supplier<SoundEvent> NULLJAW_GRUMBLE_3 = registerSound("nulljaw_grumble3");
    public static final Supplier<SoundEvent> NULLJAW_HURT = registerSound("nulljaw_hurt");
    public static final Supplier<SoundEvent> NULLJAW_DIE = registerSound("nulljaw_die");
    public static final Supplier<SoundEvent> NULLJAW_EAT = registerSound("nulljaw_eat");

    private static Supplier<SoundEvent> registerSound(String name) {
        return REGISTER.register(name, () -> SoundEvent.createVariableRangeEvent(SaintsDragonsCommon.rl(name)));
    }

    public static void register() {
        REGISTER.register();
    }
}

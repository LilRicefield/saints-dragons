package com.leon.saintsdragons.common.registry;

import com.leon.saintsdragons.server.entity.ability.DieAbility;
import com.leon.saintsdragons.server.entity.ability.DragonAbility;
import com.leon.saintsdragons.server.entity.ability.DragonAbilityType;
import com.leon.saintsdragons.server.entity.ability.HurtAbility;
import com.leon.saintsdragons.server.entity.ability.abilities.atroxiia.AtroxiiaDevastatingSweepAbility;
import com.leon.saintsdragons.server.entity.ability.abilities.atroxiia.AtroxiiaHelheimQuakeAbility;
import com.leon.saintsdragons.server.entity.ability.abilities.atroxiia.AtroxiiaPreciseStrikeAbility;
import com.leon.saintsdragons.server.entity.ability.abilities.atroxiia.AtroxiiaSlamAbility;
import com.leon.saintsdragons.server.entity.ability.abilities.atroxiia.AtroxiiaSwipeAbility;
import com.leon.saintsdragons.server.entity.ability.abilities.atroxiia.AtroxiiaUnderwaterBiteAbility;
import com.leon.saintsdragons.server.entity.ability.abilities.cindervane.CindervaneBiteAbility;
import com.leon.saintsdragons.server.entity.ability.abilities.cindervane.CindervaneFireBodyAbility;
import com.leon.saintsdragons.server.entity.ability.abilities.cindervane.CindervaneRoarAbility;
import com.leon.saintsdragons.server.entity.ability.abilities.cindervane.CindervaneSlashGrabAbility;
import com.leon.saintsdragons.server.entity.ability.abilities.cindervane.CindervaneVolleyAbility;
import com.leon.saintsdragons.server.entity.ability.abilities.ignivorus.IgnivorusBiteAbility;
import com.leon.saintsdragons.server.entity.ability.abilities.ignivorus.IgnivorusBodySlamAbility;
import com.leon.saintsdragons.server.entity.ability.abilities.ignivorus.IgnivorusFireBreathAbility;
import com.leon.saintsdragons.server.entity.ability.abilities.ignivorus.IgnivorusFireballAbility;
import com.leon.saintsdragons.server.entity.ability.abilities.ignivorus.IgnivorusRoarAbility;
import com.leon.saintsdragons.server.entity.ability.abilities.ignivorus.IgnivorusStompAbility;
import com.leon.saintsdragons.server.entity.ability.abilities.ignivorus.IgnivorusUltimateAbility;
import com.leon.saintsdragons.server.entity.ability.abilities.ignivorus.IgnivorusWingSwipeAbility;
import com.leon.saintsdragons.server.entity.ability.abilities.nulljaw.NulljawBiteAbility;
import com.leon.saintsdragons.server.entity.ability.abilities.nulljaw.NulljawForwardTeleportAbility;
import com.leon.saintsdragons.server.entity.ability.abilities.raevyx.RaevyxBeamAbility;
import com.leon.saintsdragons.server.entity.ability.abilities.raevyx.RaevyxBiteAbility;
import com.leon.saintsdragons.server.entity.ability.abilities.raevyx.RaevyxGroundRendAbility;
import com.leon.saintsdragons.server.entity.ability.abilities.raevyx.RaevyxHornGoreAbility;
import com.leon.saintsdragons.server.entity.ability.abilities.raevyx.RaevyxRoarAbility;
import com.leon.saintsdragons.server.entity.ability.abilities.raevyx.RaevyxSummonStormAbility;
import com.leon.saintsdragons.server.entity.ability.abilities.stegonaut.StegonautBiteAbility;
import com.leon.saintsdragons.server.entity.ability.abilities.stegonaut.StegonautChinSlamAbility;
import com.leon.saintsdragons.server.entity.ability.abilities.stegonaut.StegonautGroundEatingAbility;
import com.leon.saintsdragons.server.entity.ability.abilities.stegonaut.StegonautGroundSlamAbility;
import com.leon.saintsdragons.server.entity.ability.abilities.varasuchus.VarasuchusBite2Ability;
import com.leon.saintsdragons.server.entity.ability.abilities.varasuchus.VarasuchusBiteAbility;
import com.leon.saintsdragons.server.entity.ability.abilities.varasuchus.VarasuchusClawAbility;
import com.leon.saintsdragons.server.entity.ability.abilities.varasuchus.VarasuchusHornGoreAbility;
import com.leon.saintsdragons.server.entity.ability.abilities.varasuchus.VarasuchusPhaseShiftAbility;
import com.leon.saintsdragons.server.entity.ability.abilities.varasuchus.VarasuchusTailguardAbility;
import com.leon.saintsdragons.server.entity.ability.abilities.varasuchus.VarasuchusSlashBarrageAbility;
import com.leon.saintsdragons.server.entity.ability.abilities.varasuchus.VarasuchusTailAttackAbility;
import com.leon.saintsdragons.server.entity.ability.abilities.volitans.VolitansBiteAbility;
import com.leon.saintsdragons.server.entity.ability.abilities.volitans.VolitansBreathAbility;
import com.leon.saintsdragons.server.entity.ability.abilities.volitans.VolitansBurrowAbility;
import com.leon.saintsdragons.server.entity.ability.abilities.volitans.VolitansClawAbility;
import com.leon.saintsdragons.server.entity.ability.abilities.volitans.VolitansHornGoreAbility;
import com.leon.saintsdragons.server.entity.ability.abilities.volitans.VolitansPoisonBallAbility;
import com.leon.saintsdragons.server.entity.ability.abilities.volitans.VolitansRoarAbility;
import com.leon.saintsdragons.server.entity.ability.abilities.volitans.VolitansUltimateAbility;
import com.leon.saintsdragons.server.entity.dragons.atroxiia.Atroxiia;
import com.leon.saintsdragons.server.entity.dragons.cindervane.Cindervane;
import com.leon.saintsdragons.server.entity.dragons.ignivorus.Ignivorus;
import com.leon.saintsdragons.server.entity.dragons.nulljaw.Nulljaw;
import com.leon.saintsdragons.server.entity.dragons.raevyx.Raevyx;
import com.leon.saintsdragons.server.entity.dragons.stegonaut.Stegonaut;
import com.leon.saintsdragons.server.entity.dragons.varasuchus.Varasuchus;
import com.leon.saintsdragons.server.entity.dragons.volitans.Volitans;
import net.minecraft.world.entity.LivingEntity;

public final class ModAbilities {
    public static final DragonAbilityType<Atroxiia, AtroxiiaSlamAbility> ATROXIIA_SLAM = register("atroxiia_slam", AtroxiiaSlamAbility::new);
    public static final DragonAbilityType<Atroxiia, AtroxiiaSwipeAbility> ATROXIIA_SWIPE = register("atroxiia_swipe", AtroxiiaSwipeAbility::new);
    public static final DragonAbilityType<Atroxiia, AtroxiiaPreciseStrikeAbility> ATROXIIA_PRECISE_STRIKE = register("atroxiia_precise_strike", AtroxiiaPreciseStrikeAbility::new);
    public static final DragonAbilityType<Atroxiia, AtroxiiaDevastatingSweepAbility> ATROXIIA_DEVASTATING_SWEEP = register("atroxiia_devastating_sweep", AtroxiiaDevastatingSweepAbility::new);
    public static final DragonAbilityType<Atroxiia, AtroxiiaHelheimQuakeAbility> ATROXIIA_HELHEIM_QUAKE = register("atroxiia_helheim_quake", AtroxiiaHelheimQuakeAbility::new);
    public static final DragonAbilityType<Atroxiia, AtroxiiaUnderwaterBiteAbility> ATROXIIA_UNDERWATER_BITE = register("atroxiia_underwater_bite", AtroxiiaUnderwaterBiteAbility::new);
    public static final DragonAbilityType<Atroxiia, HurtAbility<Atroxiia>> ATROXIIA_HURT = register("atroxiia_hurt", HurtAbility::new);
    public static final DragonAbilityType<Atroxiia, DieAbility<Atroxiia>> ATROXIIA_DIE = register("atroxiia_die", DieAbility::new);

    public static final DragonAbilityType<Raevyx, RaevyxBiteAbility> RAEVYX_BITE = register("raevyx_bite", RaevyxBiteAbility::new);
    public static final DragonAbilityType<Raevyx, RaevyxHornGoreAbility> RAEVYX_HORN_GORE = register("raevyx_horn_gore", RaevyxHornGoreAbility::new);
    public static final DragonAbilityType<Raevyx, RaevyxBeamAbility> RAEVYX_LIGHTNING_BEAM = register("raevyx_lightning_beam", RaevyxBeamAbility::new);
    public static final DragonAbilityType<Raevyx, RaevyxRoarAbility> RAEVYX_ROAR = register("raevyx_roar", RaevyxRoarAbility::new);
    public static final DragonAbilityType<Raevyx, RaevyxSummonStormAbility> RAEVYX_SUMMON_STORM = register("raevyx_summon_storm", RaevyxSummonStormAbility::new);
    public static final DragonAbilityType<Raevyx, RaevyxGroundRendAbility> RAEVYX_GROUND_REND = register("raevyx_ground_rend", RaevyxGroundRendAbility::new);
    public static final DragonAbilityType<Raevyx, HurtAbility<Raevyx>> RAEVYX_HURT = register("raevyx_hurt", HurtAbility::new);
    public static final DragonAbilityType<Raevyx, DieAbility<Raevyx>> RAEVYX_DIE = register("raevyx_die", DieAbility::new);

    public static final DragonAbilityType<Ignivorus, IgnivorusFireBreathAbility> IGNIVORUS_FIRE_BREATH = register("ignivorus_fire_breath", IgnivorusFireBreathAbility::new);
    public static final DragonAbilityType<Ignivorus, IgnivorusBiteAbility> IGNIVORUS_BITE = register("ignivorus_bite", IgnivorusBiteAbility::new);
    public static final DragonAbilityType<Ignivorus, IgnivorusRoarAbility> IGNIVORUS_ROAR = register("ignivorus_roar", IgnivorusRoarAbility::new);
    public static final DragonAbilityType<Ignivorus, IgnivorusFireballAbility> IGNIVORUS_FIREBALL = register("ignivorus_fireball", IgnivorusFireballAbility::new);
    public static final DragonAbilityType<Ignivorus, IgnivorusBodySlamAbility> IGNIVORUS_BODY_SLAM = register("ignivorus_body_slam", IgnivorusBodySlamAbility::new);
    public static final DragonAbilityType<Ignivorus, IgnivorusWingSwipeAbility> IGNIVORUS_WING_SWIPE = register("ignivorus_wing_swipe", IgnivorusWingSwipeAbility::new);
    public static final DragonAbilityType<Ignivorus, IgnivorusStompAbility> IGNIVORUS_STOMP = register("ignivorus_stomp", IgnivorusStompAbility::new);
    public static final DragonAbilityType<Ignivorus, IgnivorusUltimateAbility> IGNIVORUS_ULTIMATE = register("ignivorus_ultimate", IgnivorusUltimateAbility::new);
    public static final DragonAbilityType<Ignivorus, HurtAbility<Ignivorus>> IGNIVORUS_HURT = register("ignivorus_hurt", HurtAbility::new);
    public static final DragonAbilityType<Ignivorus, DieAbility<Ignivorus>> IGNIVORUS_DIE = register("ignivorus_die", DieAbility::new);

    public static final DragonAbilityType<Cindervane, CindervaneBiteAbility> CINDERVANE_BITE = register("cindervane_bite", CindervaneBiteAbility::new);
    public static final DragonAbilityType<Cindervane, CindervaneSlashGrabAbility> CINDERVANE_SLASH_GRAB = register("cindervane_slash_grab", CindervaneSlashGrabAbility::new);
    public static final DragonAbilityType<Cindervane, CindervaneFireBodyAbility> CINDERVANE_FIRE_BODY = register("cindervane_fire_body", CindervaneFireBodyAbility::new);
    public static final DragonAbilityType<Cindervane, CindervaneRoarAbility> CINDERVANE_ROAR = register("cindervane_roar", CindervaneRoarAbility::new);
    public static final DragonAbilityType<Cindervane, CindervaneVolleyAbility> CINDERVANE_FIRE_BREATH_VOLLEY = register("cindervane_fire_breath_volley", CindervaneVolleyAbility::new);
    public static final DragonAbilityType<Cindervane, HurtAbility<Cindervane>> CINDERVANE_HURT = register("cindervane_hurt", HurtAbility::new);
    public static final DragonAbilityType<Cindervane, DieAbility<Cindervane>> CINDERVANE_DIE = register("cindervane_die", DieAbility::new);

    public static final DragonAbilityType<Varasuchus, VarasuchusBiteAbility> VARASUCHUS_BITE = register("varasuchus_bite", VarasuchusBiteAbility::new);
    public static final DragonAbilityType<Varasuchus, VarasuchusBite2Ability> VARASUCHUS_BITE2 = register("varasuchus_bite2", VarasuchusBite2Ability::new);
    public static final DragonAbilityType<Varasuchus, VarasuchusClawAbility> VARASUCHUS_CLAW = register("varasuchus_claw", VarasuchusClawAbility::new);
    public static final DragonAbilityType<Varasuchus, VarasuchusHornGoreAbility> VARASUCHUS_HORN_GORE = register("varasuchus_horn_gore", VarasuchusHornGoreAbility::new);
    public static final DragonAbilityType<Varasuchus, VarasuchusTailAttackAbility> VARASUCHUS_TAIL_ATTACK = register("varasuchus_tail_attack", VarasuchusTailAttackAbility::new);
    public static final DragonAbilityType<Varasuchus, VarasuchusTailguardAbility> VARASUCHUS_TAILGUARD = register("varasuchus_tailguard", VarasuchusTailguardAbility::new);
    public static final DragonAbilityType<Varasuchus, VarasuchusSlashBarrageAbility> VARASUCHUS_SLASH_BARRAGE = register("varasuchus_slash_barrage", VarasuchusSlashBarrageAbility::new);
    public static final DragonAbilityType<Varasuchus, VarasuchusPhaseShiftAbility> VARASUCHUS_PHASE_SHIFT = register("varasuchus_phase_shift", VarasuchusPhaseShiftAbility::new);
    public static final DragonAbilityType<Varasuchus, HurtAbility<Varasuchus>> VARASUCHUS_HURT = register("varasuchus_hurt", HurtAbility::new);
    public static final DragonAbilityType<Varasuchus, DieAbility<Varasuchus>> VARASUCHUS_DIE = register("varasuchus_die", DieAbility::new);

    public static final DragonAbilityType<Volitans, VolitansBiteAbility> VOLITANS_BITE = register("volitans_bite", VolitansBiteAbility::new);
    public static final DragonAbilityType<Volitans, VolitansClawAbility> VOLITANS_CLAW = register("volitans_claw", VolitansClawAbility::new);
    public static final DragonAbilityType<Volitans, VolitansHornGoreAbility> VOLITANS_HORN_GORE = register("volitans_horn_gore", VolitansHornGoreAbility::new);
    public static final DragonAbilityType<Volitans, VolitansRoarAbility> VOLITANS_ROAR = register("volitans_roar", VolitansRoarAbility::new);
    public static final DragonAbilityType<Volitans, VolitansBurrowAbility> VOLITANS_BURROW = register("volitans_burrow", VolitansBurrowAbility::new);
    public static final DragonAbilityType<Volitans, VolitansPoisonBallAbility> VOLITANS_POISON_BALL = register("volitans_poison_ball", VolitansPoisonBallAbility::new);
    public static final DragonAbilityType<Volitans, VolitansBreathAbility> VOLITANS_BREATH = register("volitans_breath", VolitansBreathAbility::new);
    public static final DragonAbilityType<Volitans, VolitansUltimateAbility> VOLITANS_ULTIMATE = register("volitans_ultimate", VolitansUltimateAbility::new);
    public static final DragonAbilityType<Volitans, HurtAbility<Volitans>> VOLITANS_HURT = register("volitans_hurt", HurtAbility::new);
    public static final DragonAbilityType<Volitans, DieAbility<Volitans>> VOLITANS_DIE = register("volitans_die", DieAbility::new);

    public static final DragonAbilityType<Stegonaut, StegonautBiteAbility> STEGONAUT_BITE = register("stegonaut_bite", StegonautBiteAbility::new);
    public static final DragonAbilityType<Stegonaut, StegonautChinSlamAbility> STEGONAUT_CHIN_SLAM = register("stegonaut_chin_slam", StegonautChinSlamAbility::new);
    public static final DragonAbilityType<Stegonaut, StegonautGroundEatingAbility> STEGONAUT_GROUND_EATING = register("stegonaut_ground_eating", StegonautGroundEatingAbility::new);
    public static final DragonAbilityType<Stegonaut, StegonautGroundSlamAbility> STEGONAUT_GROUND_SLAM = register("stegonaut_ground_slam", StegonautGroundSlamAbility::new);
    public static final DragonAbilityType<Stegonaut, HurtAbility<Stegonaut>> STEGONAUT_HURT = register("stegonaut_hurt", HurtAbility::new);
    public static final DragonAbilityType<Stegonaut, DieAbility<Stegonaut>> STEGONAUT_DIE = register("stegonaut_die", DieAbility::new);

    public static final DragonAbilityType<Nulljaw, NulljawBiteAbility> NULLJAW_BITE = register("nulljaw_bite", NulljawBiteAbility::new);
    public static final DragonAbilityType<Nulljaw, NulljawForwardTeleportAbility> NULLJAW_FORWARD_TELEPORT = register("nulljaw_forward_teleport", NulljawForwardTeleportAbility::new);
    public static final DragonAbilityType<Nulljaw, HurtAbility<Nulljaw>> NULLJAW_HURT = register("nulljaw_hurt", HurtAbility::new);

    private ModAbilities() {
    }

    public static void register() {
    }

    private static <M extends LivingEntity, T extends DragonAbility<M>> DragonAbilityType<M, T> register(
            String name,
            DragonAbilityType.IFactory<M, T> factory
    ) {
        return AbilityRegistry.register(new DragonAbilityType<>(name, factory));
    }
}

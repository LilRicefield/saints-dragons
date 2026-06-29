package com.leon.saintsdragons.common.registry;

import com.leon.saintsdragons.common.SaintsDragonsCommon;
import com.leon.saintsdragons.platform.RegistryHelper;
import com.leon.saintsdragons.platform.Services;
import com.leon.saintsdragons.server.entity.dragons.cindervane.Cindervane;
import com.leon.saintsdragons.server.entity.dragons.ignivorus.Ignivorus;
import com.leon.saintsdragons.server.entity.dragons.nulljaw.Nulljaw;
import com.leon.saintsdragons.server.entity.dragons.varasuchus.Varasuchus;
import com.leon.saintsdragons.server.entity.dragons.raevyx.Raevyx;
import com.leon.saintsdragons.server.entity.dragons.stegonaut.Stegonaut;
import com.leon.saintsdragons.server.entity.dragons.volitans.Volitans;
import com.leon.saintsdragons.server.entity.effect.cindervane.CindervaneMagmaBlockEntity;
import com.leon.saintsdragons.server.entity.effect.DragonWaterSplashEntity;
import com.leon.saintsdragons.server.entity.effect.ignivorus.IgnivorusFlameEntity;
import com.leon.saintsdragons.server.entity.effect.ignivorus.IgnivorusMagmaBlockEntity;
import com.leon.saintsdragons.server.entity.effect.ignivorus.IgnivorusMagmaPillarEntity;
import com.leon.saintsdragons.server.entity.effect.ignivorus.IgnivorusNovaEntity;
import com.leon.saintsdragons.server.entity.effect.ignivorus.IgnivorusNovaRingEntity;
import com.leon.saintsdragons.server.entity.effect.raevyx.RaevyxGroundRendTrailEntity;
import com.leon.saintsdragons.server.entity.effect.raevyx.RaevyxLightningChainEntity;
import com.leon.saintsdragons.server.entity.effect.stegonaut.StegonautAmethystPillarEntity;
import com.leon.saintsdragons.server.entity.effect.GroundCrackEntity;
import com.leon.saintsdragons.server.entity.effect.stegonaut.StegonautGroundChunkEntity;
import com.leon.saintsdragons.server.entity.effect.ImpactRingEntity;
import com.leon.saintsdragons.server.entity.effect.volitans.VolitansPoisonBallEntity;
import com.leon.saintsdragons.server.entity.effect.volitans.ArrowOfVenomEntity;
import com.leon.saintsdragons.server.entity.effect.volitans.VolitansBurrowMoundEntity;
import com.leon.saintsdragons.server.entity.effect.volitans.VolitansGroundChunkEntity;
import com.leon.saintsdragons.server.entity.effect.volitans.VolitansSpineEntity;
import com.leon.saintsdragons.server.entity.effect.volitans.VolitansWaterBreathEntity;
import com.leon.saintsdragons.server.entity.effect.VisualFallingBlockEntity;
import com.leon.saintsdragons.server.entity.npc.IvyTheDragonMerchant;
import com.leon.saintsdragons.server.entity.dragons.Mossback;
import com.leon.saintsdragons.server.entity.otheranimals.Moop;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;

import java.util.function.Supplier;

public final class ModEntities {
    private static final RegistryHelper.RegistryWrapper<EntityType<?>> REGISTER =
            Services.PLATFORM.getRegistryHelper()
                    .create(Registries.ENTITY_TYPE, () -> BuiltInRegistries.ENTITY_TYPE, SaintsDragonsCommon.MOD_ID);

    public static final Supplier<EntityType<Raevyx>> RAEVYX =
            REGISTER.register("raevyx", () -> EntityType.Builder.of(Raevyx::new, MobCategory.CREATURE)
                    .sized(4.5F, 4.5F)
                    .clientTrackingRange(48)
                    .updateInterval(1)
                    .build("raevyx"));

    public static final Supplier<EntityType<Stegonaut>> STEGONAUT =
            REGISTER.register("stegonaut", () -> EntityType.Builder.of(Stegonaut::new, MobCategory.CREATURE)
                    .sized(2.75F, 2.0F)
                    .clientTrackingRange(32)
                    .updateInterval(1)
                    .build("stegonaut"));

    public static final Supplier<EntityType<Cindervane>> CINDERVANE =
            REGISTER.register("cindervane", () -> EntityType.Builder.of(Cindervane::new, MobCategory.CREATURE)
                    .sized(2.5F, 2.5F)
                    .clientTrackingRange(48)
                    .updateInterval(1)
                    .build("cindervane"));

    public static final Supplier<EntityType<Varasuchus>> VARASUCHUS =
            REGISTER.register("varasuchus", () -> EntityType.Builder.of(Varasuchus::new, MobCategory.CREATURE)
                    .sized(4.5F, 3.5F)
                    .clientTrackingRange(48)
                    .updateInterval(1)
                    .build("varasuchus"));

    public static final Supplier<EntityType<Ignivorus>> IGNIVORUS =
            REGISTER.register("ignivorus", () -> EntityType.Builder.of(Ignivorus::new, MobCategory.CREATURE)
                    .sized(8.0F, 6.0F)
                    .clientTrackingRange(64)
                    .updateInterval(1)
                    .build("ignivorus"));

    public static final Supplier<EntityType<Volitans>> VOLITANS =
            REGISTER.register("volitans", () -> EntityType.Builder.of(Volitans::new, MobCategory.CREATURE)
                    .sized(5.0F, 4.0F)
                    .clientTrackingRange(48)
                    .updateInterval(1)
                    .build("volitans"));

    public static final Supplier<EntityType<Nulljaw>> NULLJAW =
            REGISTER.register("nulljaw", () -> EntityType.Builder.of(Nulljaw::new, MobCategory.MONSTER)
                    .sized(1.20F, 1.15F)
                    .clientTrackingRange(48)
                    .updateInterval(1)
                    .build("nulljaw"));

    public static final Supplier<EntityType<Moop>> MOOP =
            REGISTER.register("moop", () -> EntityType.Builder.of(Moop::new, MobCategory.WATER_AMBIENT)
                    .sized(0.7F, 0.35F)
                    .clientTrackingRange(16)
                    .updateInterval(3)
                    .build("moop"));

    public static final Supplier<EntityType<Mossback>> MOSSBACK =
            REGISTER.register("mossback", () -> EntityType.Builder.of(Mossback::new, MobCategory.CREATURE)
                    .sized(0.75F, 0.55F)
                    .clientTrackingRange(24)
                    .updateInterval(2)
                    .build("mossback"));

    public static final Supplier<EntityType<IvyTheDragonMerchant>> IVY_THE_DRAGON_MERCHANT =
            REGISTER.register("ivy_oleander", () -> EntityType.Builder.of(IvyTheDragonMerchant::new, MobCategory.MISC)
                    .sized(0.6F, 1.95F)
                    .clientTrackingRange(32)
                    .updateInterval(2)
                    .build("ivy_oleander"));

    public static final Supplier<EntityType<RaevyxLightningChainEntity>> RAEVYX_LIGHTNING_CHAIN =
            REGISTER.register("raevyx_lightning_chain", () -> EntityType.Builder.<RaevyxLightningChainEntity>of(RaevyxLightningChainEntity::new, MobCategory.MISC)
                    .sized(1.0F, 1.0F)
                    .clientTrackingRange(64)
                    .updateInterval(1)
                    .build("raevyx_lightning_chain"));

    public static final Supplier<EntityType<RaevyxGroundRendTrailEntity>> RAEVYX_GROUND_REND_TRAIL =
            REGISTER.register("raevyx_ground_rend_trail", () -> EntityType.Builder.<RaevyxGroundRendTrailEntity>of(RaevyxGroundRendTrailEntity::new, MobCategory.MISC)
                    .sized(0.25F, 0.25F)
                    .clientTrackingRange(64)
                    .updateInterval(1)
                    .noSummon()
                    .build("raevyx_ground_rend_trail"));

    public static final Supplier<EntityType<CindervaneMagmaBlockEntity>> CINDERVANE_MAGMA_BLOCK =
            REGISTER.register("cindervane_magma_block", () -> EntityType.Builder.<CindervaneMagmaBlockEntity>of(CindervaneMagmaBlockEntity::new, MobCategory.MISC)
                    .sized(0.98F, 0.98F)
                    .clientTrackingRange(32)
                    .updateInterval(1)
                    .fireImmune()
                    .noSummon()
                    .build("cindervane_magma_block"));

    public static final Supplier<EntityType<IgnivorusMagmaBlockEntity>> IGNIVORUS_MAGMA_BLOCK =
            REGISTER.register("ignivorus_magma_block", () -> EntityType.Builder.<IgnivorusMagmaBlockEntity>of(IgnivorusMagmaBlockEntity::new, MobCategory.MISC)
                    .sized(5.0F, 5.0F)
                    .clientTrackingRange(64)
                    .updateInterval(1)
                    .fireImmune()
                    .noSummon()
                    .build("ignivorus_magma_block"));

    public static final Supplier<EntityType<IgnivorusMagmaPillarEntity>> IGNIVORUS_MAGMA_PILLAR =
            REGISTER.register("ignivorus_magma_pillar", () -> EntityType.Builder.<IgnivorusMagmaPillarEntity>of(IgnivorusMagmaPillarEntity::new, MobCategory.MISC)
                    .sized(5.5F, 5.5F)
                    .clientTrackingRange(64)
                    .updateInterval(1)
                    .fireImmune()
                    .noSummon()
                    .build("ignivorus_magma_pillar"));

    public static final Supplier<EntityType<IgnivorusFlameEntity>> IGNIVORUS_FLAME =
            REGISTER.register("ignivorus_flame", () -> EntityType.Builder.<IgnivorusFlameEntity>of(IgnivorusFlameEntity::new, MobCategory.MISC)
                    .sized(0.5F, 0.5F)
                    .clientTrackingRange(64)
                    .updateInterval(1)
                    .fireImmune()
                    .noSummon()
                    .build("ignivorus_flame"));

    public static final Supplier<EntityType<IgnivorusNovaEntity>> IGNIVORUS_NOVA =
            REGISTER.register("ignivorus_nova", () -> EntityType.Builder.<IgnivorusNovaEntity>of(IgnivorusNovaEntity::new, MobCategory.MISC)
                    .sized(1.0F, 1.0F)
                    .clientTrackingRange(128)
                    .updateInterval(1)
                    .fireImmune()
                    .noSummon()
                    .build("ignivorus_nova"));

    public static final Supplier<EntityType<IgnivorusNovaRingEntity>> IGNIVORUS_NOVA_RING =
            REGISTER.register("ignivorus_nova_ring", () -> EntityType.Builder.<IgnivorusNovaRingEntity>of(IgnivorusNovaRingEntity::new, MobCategory.MISC)
                    .sized(1.0F, 0.1F)
                    .clientTrackingRange(128)
                    .updateInterval(1)
                    .fireImmune()
                    .noSummon()
                    .build("ignivorus_nova_ring"));

    public static final Supplier<EntityType<StegonautGroundChunkEntity>> STEGONAUT_GROUND_CHUNK =
            REGISTER.register("stegonaut_ground_chunk", () -> EntityType.Builder.<StegonautGroundChunkEntity>of(StegonautGroundChunkEntity::new, MobCategory.MISC)
                    .sized(1.0F, 1.0F)
                    .clientTrackingRange(64)
                    .updateInterval(1)
                    .noSummon()
                    .build("stegonaut_ground_chunk"));

    public static final Supplier<EntityType<StegonautAmethystPillarEntity>> STEGONAUT_AMETHYST_PILLAR =
            REGISTER.register("stegonaut_amethyst_pillar", () -> EntityType.Builder.<StegonautAmethystPillarEntity>of(StegonautAmethystPillarEntity::new, MobCategory.MISC)
                    .sized(2.0F, 4.0F)
                    .clientTrackingRange(64)
                    .updateInterval(1)
                    .noSummon()
                    .build("stegonaut_amethyst_pillar"));

    public static final Supplier<EntityType<ImpactRingEntity>> STEGONAUT_IMPACT_RING =
            REGISTER.register("stegonaut_impact_ring", () -> EntityType.Builder.<ImpactRingEntity>of(ImpactRingEntity::new, MobCategory.MISC)
                    .sized(1.0F, 0.1F)
                    .clientTrackingRange(64)
                    .updateInterval(1)
                    .noSummon()
                    .build("stegonaut_impact_ring"));

    public static final Supplier<EntityType<GroundCrackEntity>> STEGONAUT_GROUND_CRACK =
            REGISTER.register("stegonaut_ground_crack", () -> EntityType.Builder.<GroundCrackEntity>of(GroundCrackEntity::new, MobCategory.MISC)
                    .sized(1.0F, 0.1F)
                    .clientTrackingRange(64)
                    .updateInterval(1)
                    .noSummon()
                    .build("stegonaut_ground_crack"));

    public static final Supplier<EntityType<DragonWaterSplashEntity>> DRAGON_WATER_SPLASH =
            REGISTER.register("dragon_water_wake", () -> EntityType.Builder.<DragonWaterSplashEntity>of(DragonWaterSplashEntity::new, MobCategory.MISC)
                    .sized(1.0F, 0.1F)
                    .clientTrackingRange(64)
                    .updateInterval(1)
                    .noSummon()
                    .build("dragon_water_wake"));

    public static final Supplier<EntityType<VolitansGroundChunkEntity>> VOLITANS_GROUND_CHUNK =
            REGISTER.register("volitans_ground_chunk", () -> EntityType.Builder.<VolitansGroundChunkEntity>of(VolitansGroundChunkEntity::new, MobCategory.MISC)
                    .sized(9.0F, 2.0F)
                    .clientTrackingRange(64)
                    .updateInterval(1)
                    .noSummon()
                    .build("volitans_ground_chunk"));

    public static final Supplier<EntityType<VolitansBurrowMoundEntity>> VOLITANS_BURROW_MOUND =
            REGISTER.register("volitans_burrow_mound", () -> EntityType.Builder.<VolitansBurrowMoundEntity>of(VolitansBurrowMoundEntity::new, MobCategory.MISC)
                    .sized(9.0F, 4.5F)
                    .clientTrackingRange(64)
                    .updateInterval(1)
                    .noSummon()
                    .build("volitans_burrow_mound"));

    public static final Supplier<EntityType<VisualFallingBlockEntity>> VISUAL_FALLING_BLOCK =
            REGISTER.register("visual_falling_block", () -> EntityType.Builder.<VisualFallingBlockEntity>of(VisualFallingBlockEntity::new, MobCategory.MISC)
                    .sized(0.98F, 0.98F)
                    .clientTrackingRange(64)
                    .updateInterval(1)  // Update every tick for smooth movement
                    .noSummon()
                    .build("visual_falling_block"));

    public static final Supplier<EntityType<VolitansSpineEntity>> VOLITANS_SPINE =
            REGISTER.register("volitans_spine", () -> EntityType.Builder.<VolitansSpineEntity>of(VolitansSpineEntity::new, MobCategory.MISC)
                    .sized(0.5F, 0.5F)
                    .clientTrackingRange(64)
                    .updateInterval(1)
                    .noSummon()
                    .build("volitans_spine"));

    public static final Supplier<EntityType<ArrowOfVenomEntity>> ARROW_OF_VENOM =
            REGISTER.register("arrow_of_venom", () -> EntityType.Builder.<ArrowOfVenomEntity>of(ArrowOfVenomEntity::new, MobCategory.MISC)
                    .sized(0.5F, 0.5F)
                    .clientTrackingRange(64)
                    .updateInterval(1)
                    .noSummon()
                    .build("arrow_of_venom"));

    public static final Supplier<EntityType<VolitansWaterBreathEntity>> VOLITANS_WATER_BREATH =
            REGISTER.register("volitans_water_breath", () -> EntityType.Builder.<VolitansWaterBreathEntity>of(VolitansWaterBreathEntity::new, MobCategory.MISC)
                    .sized(0.5F, 0.5F)
                    .clientTrackingRange(64)
                    .updateInterval(1)
                    .noSummon()
                    .build("volitans_water_breath"));

    public static final Supplier<EntityType<VolitansPoisonBallEntity>> VOLITANS_POISON_BALL =
            REGISTER.register("volitans_poison_ball", () -> EntityType.Builder.<VolitansPoisonBallEntity>of(VolitansPoisonBallEntity::new, MobCategory.MISC)
                    .sized(1.0F, 1.0F)
                    .clientTrackingRange(64)
                    .updateInterval(1)
                    .noSummon()
                    .build("volitans_poison_ball"));

    private ModEntities() {
    }

    public static void register() {
        REGISTER.register();
    }
}

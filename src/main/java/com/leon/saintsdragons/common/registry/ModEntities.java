package com.leon.saintsdragons.common.registry;

import com.leon.saintsdragons.SaintsDragons;
import com.leon.saintsdragons.server.entity.dragons.cindervane.Cindervane;
import com.leon.saintsdragons.server.entity.dragons.raevyx.Raevyx;
import com.leon.saintsdragons.server.entity.dragons.stegonaut.Stegonaut;
import com.leon.saintsdragons.server.entity.effect.raevyx.RaevyxLightningChainEntity;
import com.leon.saintsdragons.server.entity.dragons.nulljaw.Nulljaw;
import com.leon.saintsdragons.server.entity.effect.cindervane.CindervaneMagmaBlockEntity;

import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

/**
 * Central registry for all wyvern entities.
 * Organized by wyvern type for easy management and expansion.
 */
public class ModEntities {
    public static final DeferredRegister<EntityType<?>> REGISTER =
            DeferredRegister.create(ForgeRegistries.ENTITY_TYPES, SaintsDragons.MOD_ID);

    // ===== LIGHTNING DRAGON =====
    public static final RegistryObject<EntityType<Raevyx>> RAEVYX =
            REGISTER.register("raevyx", () -> EntityType.Builder.of(Raevyx::new, MobCategory.CREATURE)
                    .sized(3.5F, 3.0F)
                    .clientTrackingRange(64)
                    .updateInterval(1)
                    .build("raevyx"));

    // ===== STEGONAUT =====
    public static final RegistryObject<EntityType<Stegonaut>> STEGONAUT =
            REGISTER.register("stegonaut", () -> EntityType.Builder.of(Stegonaut::new, MobCategory.CREATURE)
                    .sized(1.5F, 1.0F)  // Smaller than lightning wyvern, cute little drake!
                    .clientTrackingRange(32)
                    .updateInterval(1)
                    .build("stegonaut"));

    // ===== AMPHITHERE =====
    public static final RegistryObject<EntityType<Cindervane>> CINDERVANE =
            REGISTER.register("cindervane", () -> EntityType.Builder.of(Cindervane::new, MobCategory.CREATURE)
                    .sized(4.5F, 6.5F)
                    .clientTrackingRange(48)
                    .updateInterval(1)
                    .build("cindervane"));

    // ===== RIFT DRAKE =====
    public static final RegistryObject<EntityType<Nulljaw>> NULLJAW =
            REGISTER.register("nulljaw", () -> EntityType.Builder.of(Nulljaw::new, MobCategory.CREATURE)
                    .sized(4.5F, 5.0F)
                    .clientTrackingRange(48)
                    .updateInterval(1)
                    .build("nulljaw"));

    // ===== EFFECT ENTITIES =====
    public static final RegistryObject<EntityType<RaevyxLightningChainEntity>> RAEVYX_LIGHTNING_CHAIN =
            REGISTER.register("raevyx_lightning_chain", () -> EntityType.Builder.<RaevyxLightningChainEntity>of(RaevyxLightningChainEntity::new, MobCategory.MISC)
                    .sized(1.0F, 1.0F)
                    .clientTrackingRange(64)
                    .updateInterval(1)
                    .build("raevyx_lightning_chain"));

    public static final RegistryObject<EntityType<CindervaneMagmaBlockEntity>> CINDERVANE_MAGMA_BLOCK =
            REGISTER.register("cindervane_magma_block", () -> EntityType.Builder.<CindervaneMagmaBlockEntity>of(CindervaneMagmaBlockEntity::new, MobCategory.MISC)
                    .sized(0.98F, 0.98F)
                    .clientTrackingRange(32)
                    .updateInterval(1)
                    .fireImmune()
                    .noSummon()
                    .build("cindervane_magma_block"));
}

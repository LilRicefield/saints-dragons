package com.leon.saintsdragons.common.registry;

import com.leon.saintsdragons.SaintsDragons;
import com.leon.saintsdragons.server.entity.dragons.lightningdragon.LightningDragonEntity;
import com.leon.saintsdragons.server.entity.dragons.primitivedrake.PrimitiveDrakeEntity;
import com.leon.saintsdragons.server.entity.effect.lightningdragon.LightningChainEntity;

import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

/**
 * Central registry for all dragon entities.
 * Organized by dragon type for easy management and expansion.
 */
public class ModEntities {
    public static final DeferredRegister<EntityType<?>> REGISTER =
            DeferredRegister.create(ForgeRegistries.ENTITY_TYPES, SaintsDragons.MOD_ID);

    // ===== LIGHTNING DRAGON =====
    public static final RegistryObject<EntityType<LightningDragonEntity>> LIGHTNING_DRAGON =
            REGISTER.register("lightning_dragon", () -> EntityType.Builder.of(LightningDragonEntity::new, MobCategory.CREATURE)
                    .sized(3.5F, 3.0F)
                    .clientTrackingRange(64)
                    .updateInterval(1)
                    .build("lightning_dragon"));

    // ===== PRIMITIVE DRAKE =====
    public static final RegistryObject<EntityType<PrimitiveDrakeEntity>> PRIMITIVE_DRAKE =
            REGISTER.register("primitive_drake", () -> EntityType.Builder.of(PrimitiveDrakeEntity::new, MobCategory.CREATURE)
                    .sized(1.5F, 1.0F)  // Smaller than lightning dragon - cute little drake!
                    .clientTrackingRange(32)
                    .updateInterval(1)
                    .build("primitive_drake"));

    // ===== EFFECT ENTITIES =====
    public static final RegistryObject<EntityType<LightningChainEntity>> LIGHTNING_CHAIN =
            REGISTER.register("lightning_chain", () -> EntityType.Builder.<LightningChainEntity>of(LightningChainEntity::new, MobCategory.MISC)
                    .sized(1.0F, 1.0F)
                    .clientTrackingRange(64)
                    .updateInterval(1)
                    .build("lightning_chain"));

    // ===== FUTURE DRAGON TYPES =====
    // Add new dragon types here as they are implemented
    // Example:
    // public static final RegistryObject<EntityType<FireDragonEntity>> FIRE_DRAGON = ...
    // public static final RegistryObject<EntityType<IceDragonEntity>> ICE_DRAGON = ...
    // public static final RegistryObject<EntityType<EarthDragonEntity>> EARTH_DRAGON = ...
}
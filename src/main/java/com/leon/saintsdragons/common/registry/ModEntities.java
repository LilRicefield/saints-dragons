package com.leon.saintsdragons.common.registry;

import com.leon.saintsdragons.SaintsDragons;
import com.leon.saintsdragons.server.entity.dragons.lightningdragon.LightningDragonEntity;

import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

public class ModEntities {
    public static final DeferredRegister<EntityType<?>> REGISTER =
            DeferredRegister.create(ForgeRegistries.ENTITY_TYPES, SaintsDragons.MOD_ID);

    public static final RegistryObject<EntityType<LightningDragonEntity>> LIGHTNING_DRAGON =
            REGISTER.register("lightning_dragon", () -> EntityType.Builder.of(LightningDragonEntity::new, MobCategory.CREATURE)
                    .sized(4.5F, 4.5F)
                    .clientTrackingRange(64)
                    .updateInterval(1)
                    .build("lightning_dragon"));
}
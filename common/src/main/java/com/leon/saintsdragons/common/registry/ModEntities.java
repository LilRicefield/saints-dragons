package com.leon.saintsdragons.common.registry;

import com.leon.saintsdragons.common.SaintsDragonsCommon;
import com.leon.saintsdragons.platform.RegistryHelper;
import com.leon.saintsdragons.platform.Services;
import com.leon.saintsdragons.server.entity.dragons.cindervane.Cindervane;
import com.leon.saintsdragons.server.entity.dragons.nulljaw.Nulljaw;
import com.leon.saintsdragons.server.entity.dragons.raevyx.Raevyx;
import com.leon.saintsdragons.server.entity.dragons.stegonaut.Stegonaut;
import com.leon.saintsdragons.server.entity.effect.cindervane.CindervaneMagmaBlockEntity;
import com.leon.saintsdragons.server.entity.effect.raevyx.RaevyxLightningChainEntity;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;

import java.util.function.Supplier;

/**
 * Platform-agnostic entity registration.
 */
public final class ModEntities {
    private static final RegistryHelper.RegistryWrapper<EntityType<?>> REGISTER =
            Services.PLATFORM.getRegistryHelper()
                    .create(Registries.ENTITY_TYPE, () -> BuiltInRegistries.ENTITY_TYPE, SaintsDragonsCommon.MOD_ID);

    public static final Supplier<EntityType<Raevyx>> RAEVYX =
            REGISTER.register("raevyx", () -> EntityType.Builder.of(Raevyx::new, MobCategory.CREATURE)
                    .sized(3.5F, 3.0F)
                    .clientTrackingRange(64)
                    .updateInterval(1)
                    .build("raevyx"));

    public static final Supplier<EntityType<Stegonaut>> STEGONAUT =
            REGISTER.register("stegonaut", () -> EntityType.Builder.of(Stegonaut::new, MobCategory.CREATURE)
                    .sized(1.5F, 1.0F)
                    .clientTrackingRange(32)
                    .updateInterval(1)
                    .build("stegonaut"));

    public static final Supplier<EntityType<Cindervane>> CINDERVANE =
            REGISTER.register("cindervane", () -> EntityType.Builder.of(Cindervane::new, MobCategory.CREATURE)
                    .sized(4.5F, 6.5F)
                    .clientTrackingRange(48)
                    .updateInterval(1)
                    .build("cindervane"));

    public static final Supplier<EntityType<Nulljaw>> NULLJAW =
            REGISTER.register("nulljaw", () -> EntityType.Builder.of(Nulljaw::new, MobCategory.CREATURE)
                    .sized(4.5F, 5.0F)
                    .clientTrackingRange(48)
                    .updateInterval(1)
                    .build("nulljaw"));

    public static final Supplier<EntityType<RaevyxLightningChainEntity>> RAEVYX_LIGHTNING_CHAIN =
            REGISTER.register("raevyx_lightning_chain", () -> EntityType.Builder.<RaevyxLightningChainEntity>of(RaevyxLightningChainEntity::new, MobCategory.MISC)
                    .sized(1.0F, 1.0F)
                    .clientTrackingRange(64)
                    .updateInterval(1)
                    .build("raevyx_lightning_chain"));

    public static final Supplier<EntityType<CindervaneMagmaBlockEntity>> CINDERVANE_MAGMA_BLOCK =
            REGISTER.register("cindervane_magma_block", () -> EntityType.Builder.<CindervaneMagmaBlockEntity>of(CindervaneMagmaBlockEntity::new, MobCategory.MISC)
                    .sized(0.98F, 0.98F)
                    .clientTrackingRange(32)
                    .updateInterval(1)
                    .fireImmune()
                    .noSummon()
                    .build("cindervane_magma_block"));

    private ModEntities() {
    }

    public static void register() {
        REGISTER.register();
    }
}

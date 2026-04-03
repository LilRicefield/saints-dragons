package com.leon.saintsdragons.common.registry;

import com.leon.saintsdragons.common.SaintsDragonsCommon;
import com.leon.saintsdragons.common.block.RaevyxEggBlockEntity;
import com.leon.saintsdragons.common.block.IgnivorusEggBlockEntity;
import com.leon.saintsdragons.common.block.CindervaneEggBlockEntity;
import com.leon.saintsdragons.common.block.VarasuchusEggBlockEntity;
import com.leon.saintsdragons.common.block.StegonautEggBlockEntity;
import com.leon.saintsdragons.common.block.VolitansEggBlockEntity;
import com.leon.saintsdragons.platform.RegistryHelper;
import com.leon.saintsdragons.platform.Services;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.level.block.entity.BlockEntityType;

import java.util.function.Supplier;

/**
 * Platform-agnostic block entity registration.
 */
public final class ModBlockEntities {
    private static final RegistryHelper.RegistryWrapper<BlockEntityType<?>> REGISTER =
            Services.PLATFORM.getRegistryHelper()
                    .create(Registries.BLOCK_ENTITY_TYPE, () -> BuiltInRegistries.BLOCK_ENTITY_TYPE, SaintsDragonsCommon.MOD_ID);

    public static final Supplier<BlockEntityType<RaevyxEggBlockEntity>> RAEVYX_EGG =
            REGISTER.register("raevyx_egg", () -> BlockEntityType.Builder.of(
                    RaevyxEggBlockEntity::new,
                    ModBlocks.RAEVYX_EGG.get()
            ).build(null));

    public static final Supplier<BlockEntityType<IgnivorusEggBlockEntity>> IGNIVORUS_EGG =
            REGISTER.register("ignivorus_egg", () -> BlockEntityType.Builder.of(
                    IgnivorusEggBlockEntity::new,
                    ModBlocks.IGNIVORUS_EGG.get()
            ).build(null));

    public static final Supplier<BlockEntityType<CindervaneEggBlockEntity>> CINDERVANE_EGG =
            REGISTER.register("cindervane_egg", () -> BlockEntityType.Builder.of(
                    CindervaneEggBlockEntity::new,
                    ModBlocks.CINDERVANE_EGG.get()
            ).build(null));

    public static final Supplier<BlockEntityType<VarasuchusEggBlockEntity>> VARASUCHUS_EGG =
            REGISTER.register("varasuchus_egg", () -> BlockEntityType.Builder.of(
                    VarasuchusEggBlockEntity::new,
                    ModBlocks.VARASUCHUS_EGG.get()
            ).build(null));

    public static final Supplier<BlockEntityType<StegonautEggBlockEntity>> STEGONAUT_EGG =
            REGISTER.register("stegonaut_egg", () -> BlockEntityType.Builder.of(
                    StegonautEggBlockEntity::new,
                    ModBlocks.STEGONAUT_EGG.get()
            ).build(null));

    public static final Supplier<BlockEntityType<VolitansEggBlockEntity>> VOLITANS_EGG =
            REGISTER.register("volitans_egg", () -> BlockEntityType.Builder.of(
                    VolitansEggBlockEntity::new,
                    ModBlocks.VOLITANS_EGG.get()
            ).build(null));

    public static void register() {
        REGISTER.register();
    }
}

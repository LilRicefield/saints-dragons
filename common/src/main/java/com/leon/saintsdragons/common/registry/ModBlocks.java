package com.leon.saintsdragons.common.registry;

import com.leon.saintsdragons.common.SaintsDragonsCommon;
import com.leon.saintsdragons.common.block.RaevyxEggBlock;
import com.leon.saintsdragons.common.block.IgnivorusEggBlock;
import com.leon.saintsdragons.common.block.CindervaneEggBlock;
import com.leon.saintsdragons.common.block.VarasuchusEggBlock;
import com.leon.saintsdragons.common.block.StegonautEggBlock;
import com.leon.saintsdragons.platform.RegistryHelper;
import com.leon.saintsdragons.platform.Services;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.material.MapColor;

import java.util.function.Supplier;

public class ModBlocks {
    public static final RegistryHelper.RegistryWrapper<Block> REGISTER =
            Services.PLATFORM.getRegistryHelper()
                    .create(Registries.BLOCK, () -> BuiltInRegistries.BLOCK, SaintsDragonsCommon.MOD_ID);

    public static final Supplier<Block> RAEVYX_EGG =
            REGISTER.register("raevyx_egg",
                    () -> new RaevyxEggBlock(BlockBehaviour.Properties.of()
                            .mapColor(MapColor.COLOR_BLACK)
                            .strength(0.5F)
                            .sound(SoundType.METAL)
                            .noOcclusion()
                            .randomTicks()));

    public static final Supplier<Block> IGNIVORUS_EGG =
            REGISTER.register("ignivorus_egg",
                    () -> new IgnivorusEggBlock(BlockBehaviour.Properties.of()
                            .mapColor(MapColor.COLOR_RED)
                            .strength(0.5F)
                            .sound(SoundType.METAL)
                            .noOcclusion()
                            .randomTicks()));

    public static final Supplier<Block> CINDERVANE_EGG =
            REGISTER.register("cindervane_egg",
                    () -> new CindervaneEggBlock(BlockBehaviour.Properties.of()
                            .mapColor(MapColor.COLOR_ORANGE)
                            .strength(0.5F)
                            .sound(SoundType.METAL)
                            .noOcclusion()
                            .randomTicks()));

    public static final Supplier<Block> VARASUCHUS_EGG =
            REGISTER.register("varasuchus_egg",
                    () -> new VarasuchusEggBlock(BlockBehaviour.Properties.of()
                            .mapColor(MapColor.COLOR_BLACK)
                            .strength(0.5F)
                            .sound(SoundType.METAL)
                            .noOcclusion()
                            .randomTicks()));

    public static final Supplier<Block> STEGONAUT_EGG =
            REGISTER.register("stegonaut_egg",
                    () -> new StegonautEggBlock(BlockBehaviour.Properties.of()
                            .mapColor(MapColor.TERRACOTTA_BROWN)
                            .strength(0.5F)
                            .sound(SoundType.METAL)
                            .noOcclusion()
                            .randomTicks()));

    public static void register() {
        REGISTER.register();
    }
}

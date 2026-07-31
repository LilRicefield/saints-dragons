package com.leon.saintsdragons.common.registry;

import com.leon.saintsdragons.common.SaintsDragonsCommon;
import com.leon.saintsdragons.common.block.*;
import com.leon.saintsdragons.platform.RegistryHelper;
import com.leon.saintsdragons.platform.Services;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
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

    public static final Supplier<Block> VOLITANS_EGG =
            REGISTER.register("volitans_egg",
                    () -> new VolitansEggBlock(BlockBehaviour.Properties.of()
                            .mapColor(MapColor.COLOR_LIGHT_BLUE)
                            .strength(0.5F)
                            .sound(SoundType.METAL)
                            .noOcclusion()
                            .randomTicks()));

    public static final Supplier<Block> ATROXIIA_EGG =
            REGISTER.register("atroxiia_egg",
                    () -> new AtroxiiaEggBlock(BlockBehaviour.Properties.of()
                            .mapColor(MapColor.COLOR_LIGHT_BLUE)
                            .strength(0.5F)
                            .sound(SoundType.METAL)
                            .noOcclusion()
                            .randomTicks()));

    public static final Supplier<Block> DRACONIAN_PELLUCIDA =
            REGISTER.register("draconian_pellucida",
                    () -> new Block(BlockBehaviour.Properties.of()
                            .mapColor(MapColor.COLOR_PURPLE)
                            .strength(0.0F)
                            .sound(SoundType.SLIME_BLOCK)
                            .noOcclusion()));

    public static final Supplier<Block> DRACONIAN_NUCLEUS =
            REGISTER.register("draconian_nucleus",
                    () -> new DraconianNucleusBlock(BlockBehaviour.Properties.of()
                            .mapColor(MapColor.COLOR_PURPLE)
                            .strength(0.5F)
                            .sound(SoundType.SLIME_BLOCK)
                            .lightLevel(state -> 8)
                            .noOcclusion()));
    public static final Supplier<Block> DRACONIC_CRUCIBLE =
            REGISTER.register("draconic_crucible",
                    () -> new DraconicCrucibleBlock(BlockBehaviour.Properties.of()
                            .mapColor(MapColor.COLOR_BLACK)
                            .strength(5.0F, 6.0F)
                            .sound(SoundType.METAL)
                            .requiresCorrectToolForDrops()
                            .lightLevel(state -> state.getValue(DraconicCrucibleBlock.LIT) ? 13 : 0)
                            .noOcclusion()));
    public static final Supplier<Block> DRAGONHEART_ALLOY_BLOCK =
            REGISTER.register("dragonheart_alloy_block",
                    () -> new Block(BlockBehaviour.Properties.copy(Blocks.NETHERITE_BLOCK)
                            .mapColor(MapColor.COLOR_RED)
                            .strength(12.0F, 1200.0F)
                            .requiresCorrectToolForDrops()));
    public static final Supplier<Block> DRAGONHEART_BLOCK =
            REGISTER.register("dragonheart_block",
                    () -> new Block(BlockBehaviour.Properties.copy(Blocks.NETHERITE_BLOCK)
                            .mapColor(MapColor.COLOR_RED)
                            .strength(12.0F, 1200.0F)
                            .requiresCorrectToolForDrops()));
    public static final Supplier<Block> DEEPSLATE_WORLDROOT_ORE =
            REGISTER.register("deepslate_worldroot_ore",
                    () -> new Block(BlockBehaviour.Properties.copy(Blocks.DEEPSLATE_DIAMOND_ORE)
                            .mapColor(MapColor.COLOR_LIGHT_BLUE)
                            .requiresCorrectToolForDrops()));
    public static final Supplier<Block> DRAGONHEART_ORE =
            REGISTER.register("dragonheart_ore",
                    () -> new Block(BlockBehaviour.Properties.copy(Blocks.END_STONE)
                            .mapColor(MapColor.COLOR_YELLOW)
                            .requiresCorrectToolForDrops()));
    public static final Supplier<Block> WORLDROOT_BLOCK =
            REGISTER.register("worldroot_block",
                    () -> new Block(BlockBehaviour.Properties.copy(Blocks.IRON_BLOCK)
                            .mapColor(MapColor.COLOR_BROWN)
                            .requiresCorrectToolForDrops()));
    public static final Supplier<Block> RAW_WORLDROOT_BLOCK =
            REGISTER.register("raw_worldroot_block",
                    () -> new Block(BlockBehaviour.Properties.copy(Blocks.RAW_IRON_BLOCK)
                            .mapColor(MapColor.COLOR_BROWN)
                            .requiresCorrectToolForDrops()));
    public static final Supplier<Block> IGNIVORUS_INCUBATOR_BLOCK =
            REGISTER.register("ignivorus_incubator_block",
                    () -> new Block(BlockBehaviour.Properties.copy(Blocks.RAW_IRON_BLOCK)
                            .mapColor(MapColor.COLOR_RED)
                            .requiresCorrectToolForDrops()));

    public static void register() {
        REGISTER.register();
    }
}

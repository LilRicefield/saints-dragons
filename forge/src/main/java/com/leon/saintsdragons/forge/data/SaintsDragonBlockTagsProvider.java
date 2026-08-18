package com.leon.saintsdragons.forge.data;

import com.leon.saintsdragons.common.SaintsDragonsCommon;
import com.leon.saintsdragons.common.registry.ConventionalTags;
import com.leon.saintsdragons.common.registry.ModBlocks;
import com.leon.saintsdragons.common.registry.ModTags;
import net.minecraft.core.HolderLookup;
import net.minecraft.data.PackOutput;
import net.minecraft.tags.BlockTags;
import net.minecraftforge.common.data.BlockTagsProvider;
import net.minecraftforge.common.data.ExistingFileHelper;
import org.jetbrains.annotations.NotNull;

import java.util.concurrent.CompletableFuture;

public final class SaintsDragonBlockTagsProvider extends BlockTagsProvider {
    public SaintsDragonBlockTagsProvider(
            PackOutput output,
            CompletableFuture<HolderLookup.Provider> lookupProvider,
            ExistingFileHelper existingFileHelper
    ) {
        super(output, lookupProvider, SaintsDragonsCommon.MOD_ID, existingFileHelper);
    }

    @Override
    protected void addTags(@NotNull HolderLookup.Provider provider) {
        tag(ModTags.Blocks.ATROXIIA_EGGS).add(ModBlocks.ATROXIIA_EGG.get());
        tag(ModTags.Blocks.CINDERVANE_EGGS).add(ModBlocks.CINDERVANE_EGG.get());
        tag(ModTags.Blocks.IGNIVORUS_EGGS).add(ModBlocks.IGNIVORUS_EGG.get());
        tag(ModTags.Blocks.RAEVYX_EGGS).add(ModBlocks.RAEVYX_EGG.get());
        tag(ModTags.Blocks.STEGONAUT_EGGS).add(ModBlocks.STEGONAUT_EGG.get());
        tag(ModTags.Blocks.VARASUCHUS_EGGS).add(ModBlocks.VARASUCHUS_EGG.get());
        tag(ModTags.Blocks.VOLITANS_EGGS).add(ModBlocks.VOLITANS_EGG.get());

        tag(ModTags.Blocks.DRAGON_EGGS)
                .addTag(ModTags.Blocks.ATROXIIA_EGGS)
                .addTag(ModTags.Blocks.CINDERVANE_EGGS)
                .addTag(ModTags.Blocks.IGNIVORUS_EGGS)
                .addTag(ModTags.Blocks.RAEVYX_EGGS)
                .addTag(ModTags.Blocks.STEGONAUT_EGGS)
                .addTag(ModTags.Blocks.VARASUCHUS_EGGS)
                .addTag(ModTags.Blocks.VOLITANS_EGGS);

        tag(BlockTags.MINEABLE_WITH_PICKAXE)
                .add(ModBlocks.DEEPSLATE_WORLDROOT_ORE.get())
                .add(ModBlocks.WORLDROOT_BLOCK.get())
                .add(ModBlocks.RAW_WORLDROOT_BLOCK.get())
                .add(ModBlocks.DRAGONHEART_ORE.get())
                .add(ModBlocks.DRAGONHEART_ALLOY_BLOCK.get())
                .add(ModBlocks.DRAGONHEART_BLOCK.get())
                .add(ModBlocks.DRACONIC_CRUCIBLE.get());

        tag(BlockTags.NEEDS_DIAMOND_TOOL)
                .add(ModBlocks.DEEPSLATE_WORLDROOT_ORE.get())
                .add(ModBlocks.WORLDROOT_BLOCK.get())
                .add(ModBlocks.RAW_WORLDROOT_BLOCK.get())
                .add(ModBlocks.DRACONIC_CRUCIBLE.get());

        tag(ConventionalTags.Blocks.NEEDS_NETHERITE_TOOL)
                .add(ModBlocks.DRAGONHEART_ORE.get())
                .add(ModBlocks.DRAGONHEART_ALLOY_BLOCK.get())
                .add(ModBlocks.DRAGONHEART_BLOCK.get());
    }
}

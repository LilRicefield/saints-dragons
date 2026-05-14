package com.leon.saintsdragons.forge.data;

import com.leon.saintsdragons.common.SaintsDragonsCommon;
import com.leon.saintsdragons.common.registry.ModItems;
import com.leon.saintsdragons.common.registry.ModTags;
import net.minecraft.core.HolderLookup;
import net.minecraft.data.PackOutput;
import net.minecraft.data.tags.ItemTagsProvider;
import net.minecraft.data.tags.TagsProvider;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Block;
import net.minecraftforge.common.data.ExistingFileHelper;
import org.jetbrains.annotations.NotNull;

import java.util.concurrent.CompletableFuture;

public final class SaintsDragonItemTagsProvider extends ItemTagsProvider {
    public SaintsDragonItemTagsProvider(
            PackOutput output,
            CompletableFuture<HolderLookup.Provider> lookupProvider,
            CompletableFuture<TagsProvider.TagLookup<Block>> blockTags,
            ExistingFileHelper existingFileHelper
    ) {
        super(output, lookupProvider, blockTags);
    }

    @Override
    protected void addTags(@NotNull HolderLookup.Provider provider) {
        copy(ModTags.Blocks.CINDERVANE_EGGS, ModTags.Items.DRAGON_EGGS);
        copy(ModTags.Blocks.IGNIVORUS_EGGS, ModTags.Items.DRAGON_EGGS);
        copy(ModTags.Blocks.RAEVYX_EGGS, ModTags.Items.DRAGON_EGGS);
        copy(ModTags.Blocks.STEGONAUT_EGGS, ModTags.Items.DRAGON_EGGS);
        copy(ModTags.Blocks.VARASUCHUS_EGGS, ModTags.Items.DRAGON_EGGS);
        copy(ModTags.Blocks.VOLITANS_EGGS, ModTags.Items.DRAGON_EGGS);

        tag(ModTags.Items.DRAGON_BINDERS)
                .add(ModItems.CINDERVANE_BINDER.get())
                .add(ModItems.IGNIVORUS_BINDER.get())
                .add(ModItems.RAEVYX_BINDER.get())
                .add(ModItems.STEGONAUT_BINDER.get())
                .add(ModItems.VARASUCHUS_BINDER.get())
                .add(ModItems.VOLITANS_BINDER.get());

        tag(ModTags.Items.DRAGON_BRUSHES)
                .add(ModItems.DRAGON_BRUSH.get())
                .add(ModItems.GOLDEN_DRAGON_BRUSH.get());

        tag(ModTags.Items.DRAGON_SCALES)
                .add(ModItems.CINDERVANE_SCALE.get())
                .add(ModItems.IGNIVORUS_SCALE.get())
                .add(ModItems.RAEVYX_SCALE.get())
                .add(ModItems.STEGONAUT_SCALE.get())
                .add(ModItems.VARASUCHUS_SCALE.get())
                .add(ModItems.VOLITANS_SCALE.get());

        tag(ModTags.Items.DRAGON_TEETH)
                .add(ModItems.IGNIVORUS_TOOTH.get());

        tag(ModTags.Items.DRAGON_HEARTS)
                .add(ModItems.IGNIVORUS_HEART.get());

        tag(ModTags.Items.DRAGON_SPINES)
                .add(ModItems.VOLITANS_SPINE.get());

        tag(ModTags.Items.DRAGON_PARTS)
                .addTag(ModTags.Items.DRAGON_SCALES)
                .addTag(ModTags.Items.DRAGON_TEETH)
                .addTag(ModTags.Items.DRAGON_HEARTS)
                .addTag(ModTags.Items.DRAGON_SPINES);

        tag(ModTags.Items.DRAGON_DROPS)
                .addTag(ModTags.Items.DRAGON_PARTS)
                .addTag(ModTags.Items.DRAGON_EGGS);

        tag(ModTags.Items.DRAGON_SPAWN_EGGS)
                .add(ModItems.CINDERVANE_SPAWN_EGG.get())
                .add(ModItems.IGNIVORUS_SPAWN_EGG.get())
                .add(ModItems.NULLJAW_SPAWN_EGG.get())
                .add(ModItems.RAEVYX_SPAWN_EGG.get())
                .add(ModItems.STEGONAUT_SPAWN_EGG.get())
                .add(ModItems.VARASUCHUS_SPAWN_EGG.get())
                .add(ModItems.VOLITANS_SPAWN_EGG.get());

        tag(ModTags.Items.CINDERVANE_FOODS)
                .add(Items.CHICKEN)
                .add(Items.COD)
                .add(Items.SALMON)
                .add(ModItems.HEARTY_DRAGON_MEAL.get());

        tag(ModTags.Items.IGNIVORUS_FOODS)
                .add(Items.BEEF)
                .add(Items.COD)
                .add(Items.SALMON)
                .add(ModItems.HEARTY_DRAGON_MEAL.get());

        tag(ModTags.Items.NULLJAW_FOODS)
                .add(Items.CHORUS_FRUIT);

        tag(ModTags.Items.RAEVYX_FOODS)
                .add(Items.COD)
                .add(Items.SALMON)
                .add(ModItems.HEARTY_DRAGON_MEAL.get());

        tag(ModTags.Items.STEGONAUT_FOODS)
                .add(Items.BEEF)
                .add(Items.CHICKEN)
                .add(Items.COD)
                .add(Items.MUTTON)
                .add(Items.PORKCHOP)
                .add(Items.SALMON)
                .add(ModItems.HEARTY_DRAGON_MEAL.get());

        tag(ModTags.Items.VARASUCHUS_FOODS)
                .add(Items.COD)
                .add(Items.SALMON)
                .add(Items.TROPICAL_FISH)
                .add(ModItems.HEARTY_DRAGON_MEAL.get());

        tag(ModTags.Items.VOLITANS_FOODS)
                .add(Items.COD)
                .add(Items.PUFFERFISH)
                .add(Items.SALMON)
                .add(Items.TROPICAL_FISH)
                .add(ModItems.HEARTY_DRAGON_MEAL.get());

        tag(ModTags.Items.DRAGON_FOODS)
                .addTag(ModTags.Items.CINDERVANE_FOODS)
                .addTag(ModTags.Items.IGNIVORUS_FOODS)
                .addTag(ModTags.Items.NULLJAW_FOODS)
                .addTag(ModTags.Items.RAEVYX_FOODS)
                .addTag(ModTags.Items.STEGONAUT_FOODS)
                .addTag(ModTags.Items.VARASUCHUS_FOODS)
                .addTag(ModTags.Items.VOLITANS_FOODS);
    }
}

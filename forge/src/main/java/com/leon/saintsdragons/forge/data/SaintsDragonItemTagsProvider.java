package com.leon.saintsdragons.forge.data;

import com.leon.saintsdragons.common.registry.ModItems;
import com.leon.saintsdragons.common.registry.ModTags;
import net.minecraft.core.HolderLookup;
import net.minecraft.data.PackOutput;
import net.minecraft.data.tags.ItemTagsProvider;
import net.minecraft.data.tags.TagsProvider;
import net.minecraft.tags.ItemTags;
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
        tag(ModTags.Items.DRAGON_EGGS)
                .add(ModItems.STEGONAUT_EGG.get())
                .add(ModItems.VARASUCHUS_EGG.get())
                .add(ModItems.IGNIVORUS_EGG.get())
                .add(ModItems.VOLITANS_EGG.get())
                .add(ModItems.CINDERVANE_EGG.get())
                .add(ModItems.RAEVYX_EGG.get());

        tag(ModTags.Items.DRAGON_BINDERS)
                .add(ModItems.CINDERVANE_BINDER.get())
                .add(ModItems.IGNIVORUS_BINDER.get())
                .add(ModItems.RAEVYX_BINDER.get())
                .add(ModItems.STEGONAUT_BINDER.get())
                .add(ModItems.VARASUCHUS_BINDER.get())
                .add(ModItems.VOLITANS_BINDER.get())
                .add(ModItems.NULLJAW_BINDER.get());

        tag(ModTags.Items.DRAGON_BRUSHES)
                .add(ModItems.DRAGON_BRUSH.get())
                .add(ModItems.GOLDEN_DRAGON_BRUSH.get());

        tag(ItemTags.SWORDS)
                .add(ModItems.WORLDROOT_SWORD.get())
                .add(ModItems.BLOOD_TEMPEST_KATANA.get())
                .add(ModItems.DRAGONLORD_SWORD.get());
        tag(ItemTags.PICKAXES)
                .add(ModItems.WORLDROOT_PICKAXE.get());
        tag(ItemTags.AXES)
                .add(ModItems.WORLDROOT_AXE.get());
        tag(ItemTags.SHOVELS)
                .add(ModItems.WORLDROOT_SHOVEL.get());
        tag(ItemTags.HOES)
                .add(ModItems.WORLDROOT_HOE.get());

        tag(ModTags.Items.WORLDROOT_TOOLS)
                .add(ModItems.WORLDROOT_SWORD.get())
                .add(ModItems.WORLDROOT_PICKAXE.get())
                .add(ModItems.WORLDROOT_AXE.get())
                .add(ModItems.WORLDROOT_SHOVEL.get())
                .add(ModItems.WORLDROOT_HOE.get());

        tag(ModTags.Items.DRAGON_PARTS)
                .add(ModItems.CINDERVANE_SCALE.get())
                .add(ModItems.IGNIVORUS_SCALE.get())
                .add(ModItems.RAEVYX_SCALE.get())
                .add(ModItems.STEGONAUT_SCALE.get())
                .add(ModItems.VARASUCHUS_SCALE.get())
                .add(ModItems.VOLITANS_SCALE.get())
                .add(ModItems.IGNIVORUS_TOOTH.get())
                .add(ModItems.IGNIVORUS_HEART.get())
                .add(ModItems.VOLITANS_SPINE.get())
                .add(ModItems.RAEVYX_WING_HIDE.get())
                .add(ModItems.IGNIVORUS_WING_HIDE.get())
                .add(ModItems.RAEVYX_WINGTALON.get());

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
                .add(ModItems.VOLITANS_SPAWN_EGG.get())
                .add(ModItems.MOSSBACK_SPAWN_EGG.get())
                .add(ModItems.ATROXIIA_SPAWN_EGG.get());

        tag(ModTags.Items.ATROXIIA_FOODS)
                .add(Items.BEEF)
                .add(Items.CHICKEN)
                .add(Items.COD)
                .add(Items.MUTTON)
                .add(Items.PORKCHOP)
                .add(Items.SALMON)
                .add(ModItems.HEARTY_DRAGON_MEAL.get());

        tag(ModTags.Items.CINDERVANE_FOODS)
                .add(Items.CHICKEN)
                .add(Items.COD)
                .add(Items.SALMON)
                .add(ModItems.HEARTY_DRAGON_MEAL.get());

        tag(ModTags.Items.IGNIVORUS_FOODS)
                .add(Items.BEEF)
                .add(Items.COD)
                .add(Items.MUTTON)
                .add(Items.PORKCHOP)
                .add(Items.SALMON)
                .add(ModItems.HEARTY_DRAGON_MEAL.get());

        tag(ModTags.Items.NULLJAW_FOODS)
                .add(Items.CHORUS_FRUIT);

        tag(ModTags.Items.RAEVYX_FOODS)
                .add(Items.COD)
                .add(Items.MUTTON)
                .add(Items.PORKCHOP)
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
                .add(Items.BEEF)
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
                .addTag(ModTags.Items.ATROXIIA_FOODS)
                .addTag(ModTags.Items.CINDERVANE_FOODS)
                .addTag(ModTags.Items.IGNIVORUS_FOODS)
                .addTag(ModTags.Items.NULLJAW_FOODS)
                .addTag(ModTags.Items.RAEVYX_FOODS)
                .addTag(ModTags.Items.STEGONAUT_FOODS)
                .addTag(ModTags.Items.VARASUCHUS_FOODS)
                .addTag(ModTags.Items.VOLITANS_FOODS);
    }
}

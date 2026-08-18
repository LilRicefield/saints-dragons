package com.leon.saintsdragons.forge.data;

import com.leon.saintsdragons.common.SaintsDragonsCommon;
import com.leon.saintsdragons.common.registry.Dragons;
import com.leon.saintsdragons.common.registry.ModEntities;
import com.leon.saintsdragons.common.registry.ModTags;
import net.minecraft.core.HolderLookup;
import net.minecraft.data.PackOutput;
import net.minecraft.data.tags.EntityTypeTagsProvider;
import net.minecraft.tags.EntityTypeTags;
import net.minecraft.world.entity.EntityType;
import net.minecraftforge.common.data.ExistingFileHelper;
import org.jetbrains.annotations.NotNull;

import java.util.concurrent.CompletableFuture;

public final class SaintsDragonEntityTypeTagsProvider extends EntityTypeTagsProvider {
    public SaintsDragonEntityTypeTagsProvider(
            PackOutput output,
            CompletableFuture<HolderLookup.Provider> lookupProvider,
            ExistingFileHelper existingFileHelper
    ) {
        super(output, lookupProvider, SaintsDragonsCommon.MOD_ID, existingFileHelper);
    }

    @Override
    protected void addTags(@NotNull HolderLookup.Provider provider) {
        var dragons = tag(ModTags.EntityTypes.DRAGONS);
        for (Dragons dragon : Dragons.values()) {
            dragons.add(dragon.getEntityTypeValue());
        }

        tag(ModTags.EntityTypes.RIDEABLE_DRAGONS).addTag(ModTags.EntityTypes.DRAGONS);
        tag(ModTags.EntityTypes.TAMEABLE_DRAGONS).addTag(ModTags.EntityTypes.DRAGONS);

        tag(ModTags.EntityTypes.FLYING_DRAGONS)
                .add(ModEntities.CINDERVANE.get())
                .add(ModEntities.IGNIVORUS.get())
                .add(ModEntities.NULLJAW.get())
                .add(ModEntities.RAEVYX.get())
                .add(ModEntities.VOLITANS.get());

        tag(ModTags.EntityTypes.GROUNDED_DRAGONS)
                .add(ModEntities.ATROXIIA.get())
                .add(ModEntities.STEGONAUT.get())
                .add(ModEntities.VARASUCHUS.get());

        tag(ModTags.EntityTypes.SWIMMING_DRAGONS)
                .add(ModEntities.VARASUCHUS.get())
                .add(ModEntities.VOLITANS.get());

        tag(ModTags.EntityTypes.CARNIVORE_HUNT_PREY)
                .add(EntityType.CHICKEN)
                .add(EntityType.COW)
                .add(EntityType.PIG)
                .add(EntityType.SHEEP);

        tag(ModTags.EntityTypes.PISCIVORE_HUNT_PREY)
                .add(EntityType.COD)
                .add(EntityType.SALMON)
                .add(EntityType.TROPICAL_FISH)
                .add(EntityType.PUFFERFISH);

        tag(ModTags.EntityTypes.CINDERVANE_HUNT_PREY)
                .add(EntityType.CHICKEN)
                .add(EntityType.COD)
                .add(EntityType.SALMON);

        tag(ModTags.EntityTypes.IGNIVORUS_HUNT_PREY)
                .add(EntityType.COD)
                .add(EntityType.COW)
                .add(EntityType.PIG)
                .add(EntityType.SALMON)
                .add(EntityType.SHEEP);

        tag(ModTags.EntityTypes.RAEVYX_HUNT_PREY)
                .add(EntityType.PIG)
                .add(EntityType.SHEEP);

        tag(ModTags.EntityTypes.VARASUCHUS_HUNT_PREY)
                .add(EntityType.COD)
                .add(EntityType.COW)
                .add(EntityType.SALMON)
                .add(EntityType.TROPICAL_FISH);

        tag(ModTags.EntityTypes.VOLITANS_HUNT_PREY)
                .add(EntityType.COD)
                .add(EntityType.PUFFERFISH)
                .add(EntityType.SALMON)
                .add(EntityType.TROPICAL_FISH);

        tag(ModTags.EntityTypes.IMMUNE_TO_ELECTRICITY);
        tag(ModTags.EntityTypes.IMMUNE_TO_FIRE);
        tag(ModTags.EntityTypes.IMMUNE_TO_POISON);

        tag(EntityTypeTags.POWDER_SNOW_WALKABLE_MOBS)
                .add(ModEntities.ATROXIIA.get());
    }
}

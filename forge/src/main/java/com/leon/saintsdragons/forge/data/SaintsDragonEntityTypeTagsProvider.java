package com.leon.saintsdragons.forge.data;

import com.leon.saintsdragons.common.SaintsDragonsCommon;
import com.leon.saintsdragons.common.registry.ModEntities;
import com.leon.saintsdragons.common.registry.ModTags;
import net.minecraft.core.HolderLookup;
import net.minecraft.data.PackOutput;
import net.minecraft.data.tags.EntityTypeTagsProvider;
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
        super(output, lookupProvider);
    }

    @Override
    protected void addTags(@NotNull HolderLookup.Provider provider) {
        tag(ModTags.EntityTypes.DRAGONS)
                .add(ModEntities.CINDERVANE.get())
                .add(ModEntities.IGNIVORUS.get())
                .add(ModEntities.NULLJAW.get())
                .add(ModEntities.RAEVYX.get())
                .add(ModEntities.STEGONAUT.get())
                .add(ModEntities.VARASUCHUS.get())
                .add(ModEntities.VOLITANS.get());

        tag(ModTags.EntityTypes.RIDEABLE_DRAGONS).addTag(ModTags.EntityTypes.DRAGONS);
        tag(ModTags.EntityTypes.TAMEABLE_DRAGONS).addTag(ModTags.EntityTypes.DRAGONS);

        tag(ModTags.EntityTypes.FLYING_DRAGONS)
                .add(ModEntities.CINDERVANE.get())
                .add(ModEntities.IGNIVORUS.get())
                .add(ModEntities.NULLJAW.get())
                .add(ModEntities.RAEVYX.get())
                .add(ModEntities.VOLITANS.get());

        tag(ModTags.EntityTypes.GROUNDED_DRAGONS)
                .add(ModEntities.STEGONAUT.get())
                .add(ModEntities.VARASUCHUS.get());

        tag(ModTags.EntityTypes.SWIMMING_DRAGONS)
                .add(ModEntities.VARASUCHUS.get())
                .add(ModEntities.VOLITANS.get());

        tag(ModTags.EntityTypes.CINDERVANE_TARGETS)
                .add(EntityType.CHICKEN);

        tag(ModTags.EntityTypes.IGNIVORUS_TARGETS)
                .add(EntityType.SHEEP)
                .add(EntityType.COW)
                .add(EntityType.PIG);

        tag(ModTags.EntityTypes.RAEVYX_TARGETS)
                .add(EntityType.SHEEP)
                .add(EntityType.PIG);

        tag(ModTags.EntityTypes.STEGONAUT_TARGETS);

        tag(ModTags.EntityTypes.VARASUCHUS_TARGETS)
                .add(EntityType.SALMON)
                .add(EntityType.COD)
                .add(EntityType.COW);

        tag(ModTags.EntityTypes.VOLITANS_TARGETS)
                .add(EntityType.DOLPHIN)
                .add(EntityType.SQUID)
                .add(EntityType.GLOW_SQUID)
                .add(EntityType.TROPICAL_FISH)
                .add(EntityType.PUFFERFISH);
    }
}

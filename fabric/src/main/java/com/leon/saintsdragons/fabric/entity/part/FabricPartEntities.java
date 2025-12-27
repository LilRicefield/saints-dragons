package com.leon.saintsdragons.fabric.entity.part;

import com.leon.saintsdragons.common.SaintsDragonsCommon;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;

public final class FabricPartEntities {
    public static final EntityType<FabricDragonPart> DRAGON_PART =
            EntityType.Builder.<FabricDragonPart>of(FabricDragonPart::new, MobCategory.MISC)
                    .sized(0.1F, 0.1F)
                    .clientTrackingRange(0)
                    .updateInterval(1)
                    .noSummon()
                    .build("dragon_part");

    private FabricPartEntities() {
    }

    public static void register() {
        ResourceLocation id = new ResourceLocation(SaintsDragonsCommon.MOD_ID, "dragon_part");
        Registry.register(BuiltInRegistries.ENTITY_TYPE, id, DRAGON_PART);
    }
}

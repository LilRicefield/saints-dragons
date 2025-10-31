package com.leon.saintsdragons.common.world;

import com.leon.saintsdragons.SaintsDragons;
import com.mojang.serialization.Codec;
import net.minecraftforge.common.world.BiomeModifier;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

/**
 * Registry for custom biome modifiers
 */
public class SDWorldRegistry {

    public static final DeferredRegister<Codec<? extends BiomeModifier>> BIOME_MODIFIER_SERIALIZERS =
            DeferredRegister.create(ForgeRegistries.Keys.BIOME_MODIFIER_SERIALIZERS, SaintsDragons.MOD_ID);

    public static final RegistryObject<Codec<AddDragonsBiomeModifier>> ADD_DRAGONS_CODEC =
            BIOME_MODIFIER_SERIALIZERS.register("add_dragons", () -> Codec.unit(AddDragonsBiomeModifier::new));
}

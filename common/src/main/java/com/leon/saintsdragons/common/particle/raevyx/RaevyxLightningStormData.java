package com.leon.saintsdragons.common.particle.raevyx;

import com.leon.saintsdragons.common.registry.ModParticles;
import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.core.particles.ParticleType;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import org.jetbrains.annotations.NotNull;

import java.util.Locale;

/**
 * Server-safe ParticleOptions payload for the lightning_storm particle.
 * Holds a single float parameter: size.
 */
public record RaevyxLightningStormData(float size, boolean female) implements ParticleOptions {
    public static final MapCodec<RaevyxLightningStormData> MAP_CODEC = RecordCodecBuilder.mapCodec(b -> b.group(
            Codec.FLOAT.fieldOf("size").forGetter(RaevyxLightningStormData::size),
            Codec.BOOL.optionalFieldOf("female", false).forGetter(RaevyxLightningStormData::female)
    ).apply(b, RaevyxLightningStormData::new));

    public static final StreamCodec<RegistryFriendlyByteBuf, RaevyxLightningStormData> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.FLOAT, RaevyxLightningStormData::size,
            ByteBufCodecs.BOOL, RaevyxLightningStormData::female,
            RaevyxLightningStormData::new
    );

    public static Codec<RaevyxLightningStormData> CODEC(@SuppressWarnings("unused") ParticleType<RaevyxLightningStormData> type) {
        return MAP_CODEC.codec();
    }

    public @NotNull String writeToString() {
        ParticleType<RaevyxLightningStormData> type = female ? ModParticles.LIGHTNING_STORM_FEMALE.get() : ModParticles.LIGHTNING_STORM.get();
        return String.format(
                Locale.ROOT,
                "%s %.2f %s",
                BuiltInRegistries.PARTICLE_TYPE.getKey(type),
                this.size,
                Boolean.toString(this.female)
        );
    }

    @Override
    public @NotNull ParticleType<RaevyxLightningStormData> getType() {
        return female ? ModParticles.LIGHTNING_STORM_FEMALE.get() : ModParticles.LIGHTNING_STORM.get();
    }
}

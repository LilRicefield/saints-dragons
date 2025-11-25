package com.leon.saintsdragons.common.particle.raevyx;

import com.leon.saintsdragons.common.registry.ModParticles;
import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.core.particles.ParticleType;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import org.jetbrains.annotations.NotNull;

import java.util.Locale;

/**
 * ParticleOptions for the roar-specific lightning arc sprite set.
 */
public record RaevyxLightningArcData(float size, boolean female) implements ParticleOptions {
    public static final MapCodec<RaevyxLightningArcData> MAP_CODEC = RecordCodecBuilder.mapCodec(b -> b.group(
            Codec.FLOAT.fieldOf("size").forGetter(RaevyxLightningArcData::size),
            Codec.BOOL.optionalFieldOf("female", false).forGetter(RaevyxLightningArcData::female)
    ).apply(b, RaevyxLightningArcData::new));

    public static final StreamCodec<RegistryFriendlyByteBuf, RaevyxLightningArcData> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.FLOAT, RaevyxLightningArcData::size,
            ByteBufCodecs.BOOL, RaevyxLightningArcData::female,
            RaevyxLightningArcData::new
    );

    public static Codec<RaevyxLightningArcData> CODEC(@SuppressWarnings("unused") ParticleType<RaevyxLightningArcData> type) {
        return MAP_CODEC.codec();
    }

    public @NotNull String writeToString() {
        return String.format(
                Locale.ROOT,
                "%s %.2f %s",
                BuiltInRegistries.PARTICLE_TYPE.getKey(ModParticles.LIGHTNING_ARC.get()),
                this.size,
                Boolean.toString(this.female)
        );
    }

    @Override
    public @NotNull ParticleType<RaevyxLightningArcData> getType() {
        return ModParticles.LIGHTNING_ARC.get();
    }
}

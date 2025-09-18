package com.leon.saintsdragons.common.particle.lightningdragon;

import com.leon.saintsdragons.common.registry.ModParticles;
import com.mojang.brigadier.StringReader;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.core.particles.ParticleType;
import net.minecraft.network.FriendlyByteBuf;
import org.jetbrains.annotations.NotNull;

import javax.annotation.Nonnull;
import java.util.Locale;

/**
 * Server-safe ParticleOptions payload for the lightning_storm particle.
 * Holds a single float parameter: size.
 */
public record LightningStormData(float size) implements ParticleOptions {
    public static final ParticleOptions.Deserializer<LightningStormData> DESERIALIZER = new ParticleOptions.Deserializer<>() {
        @Override
        public @NotNull LightningStormData fromCommand(@Nonnull ParticleType<LightningStormData> type, @Nonnull StringReader reader) throws CommandSyntaxException {
            reader.expect(' ');
            float size = reader.readFloat();
            return new LightningStormData(size);
        }

        @Override
        public @NotNull LightningStormData fromNetwork(@Nonnull ParticleType<LightningStormData> type, @Nonnull FriendlyByteBuf buf) {
            return new LightningStormData(buf.readFloat());
        }
    };

    public static Codec<LightningStormData> CODEC(@SuppressWarnings("unused") ParticleType<LightningStormData> type) {
        return RecordCodecBuilder.create(b -> b.group(
                Codec.FLOAT.fieldOf("size").forGetter(LightningStormData::size)
        ).apply(b, LightningStormData::new));
    }

    @Override
    public void writeToNetwork(@Nonnull FriendlyByteBuf buf) { buf.writeFloat(this.size); }

    @Override
    public @NotNull String writeToString() {
        return String.format(Locale.ROOT, "%s %.2f", ModParticles.LIGHTNING_STORM.getId(), this.size);
    }

    @Override
    public @NotNull ParticleType<LightningStormData> getType() {
        return ModParticles.LIGHTNING_STORM.get();
    }
}
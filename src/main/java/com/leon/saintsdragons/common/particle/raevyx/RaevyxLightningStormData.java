package com.leon.saintsdragons.common.particle.raevyx;

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
public record RaevyxLightningStormData(float size, boolean female) implements ParticleOptions {
    public static final ParticleOptions.Deserializer<RaevyxLightningStormData> DESERIALIZER = new ParticleOptions.Deserializer<>() {
        @Override
        public @NotNull RaevyxLightningStormData fromCommand(@Nonnull ParticleType<RaevyxLightningStormData> type, @Nonnull StringReader reader) throws CommandSyntaxException {
            reader.expect(' ');
            float size = reader.readFloat();
            boolean female = false;
            if (reader.canRead() && reader.peek() == ' ') {
                reader.expect(' ');
                female = reader.readBoolean();
            }
            return new RaevyxLightningStormData(size, female);
        }

        @Override
        public @NotNull RaevyxLightningStormData fromNetwork(@Nonnull ParticleType<RaevyxLightningStormData> type, @Nonnull FriendlyByteBuf buf) {
            return new RaevyxLightningStormData(buf.readFloat(), buf.readBoolean());
        }
    };

    public static Codec<RaevyxLightningStormData> CODEC(@SuppressWarnings("unused") ParticleType<RaevyxLightningStormData> type) {
        return RecordCodecBuilder.create(b -> b.group(
                Codec.FLOAT.fieldOf("size").forGetter(RaevyxLightningStormData::size),
                Codec.BOOL.optionalFieldOf("female", false).forGetter(RaevyxLightningStormData::female)
        ).apply(b, RaevyxLightningStormData::new));
    }

    @Override
    public void writeToNetwork(@Nonnull FriendlyByteBuf buf) {
        buf.writeFloat(this.size);
        buf.writeBoolean(this.female);
    }

    @Override
    public @NotNull String writeToString() {
        return String.format(Locale.ROOT, "%s %.2f %s", ModParticles.LIGHTNING_STORM.getId(), this.size, Boolean.toString(this.female));
    }

    @Override
    public @NotNull ParticleType<RaevyxLightningStormData> getType() {
        return ModParticles.LIGHTNING_STORM.get();
    }
}

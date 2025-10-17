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
 * ParticleOptions for the roar-specific lightning arc sprite set.
 */
public record RaevyxLightningArcData(float size, boolean female) implements ParticleOptions {
    public static final ParticleOptions.Deserializer<RaevyxLightningArcData> DESERIALIZER = new ParticleOptions.Deserializer<>() {
        @Override
        public @NotNull RaevyxLightningArcData fromCommand(@Nonnull ParticleType<RaevyxLightningArcData> type, @Nonnull StringReader reader) throws CommandSyntaxException {
            reader.expect(' ');
            float size = reader.readFloat();
            boolean female = false;
            if (reader.canRead() && reader.peek() == ' ') {
                reader.expect(' ');
                female = reader.readBoolean();
            }
            return new RaevyxLightningArcData(size, female);
        }

        @Override
        public @NotNull RaevyxLightningArcData fromNetwork(@Nonnull ParticleType<RaevyxLightningArcData> type, @Nonnull FriendlyByteBuf buf) {
            return new RaevyxLightningArcData(buf.readFloat(), buf.readBoolean());
        }
    };

    public static Codec<RaevyxLightningArcData> CODEC(@SuppressWarnings("unused") ParticleType<RaevyxLightningArcData> type) {
        return RecordCodecBuilder.create(b -> b.group(
                Codec.FLOAT.fieldOf("size").forGetter(RaevyxLightningArcData::size),
                Codec.BOOL.optionalFieldOf("female", false).forGetter(RaevyxLightningArcData::female)
        ).apply(b, RaevyxLightningArcData::new));
    }

    @Override
    public void writeToNetwork(@Nonnull FriendlyByteBuf buf) {
        buf.writeFloat(this.size);
        buf.writeBoolean(this.female);
    }

    @Override
    public @NotNull String writeToString() {
        return String.format(Locale.ROOT, "%s %.2f %s", ModParticles.LIGHTNING_ARC.getId(), this.size, Boolean.toString(this.female));
    }

    @Override
    public @NotNull ParticleType<RaevyxLightningArcData> getType() {
        return ModParticles.LIGHTNING_ARC.get();
    }
}

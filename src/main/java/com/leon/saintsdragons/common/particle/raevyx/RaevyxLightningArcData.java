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
public record RaevyxLightningArcData(float size) implements ParticleOptions {
    public static final ParticleOptions.Deserializer<RaevyxLightningArcData> DESERIALIZER = new ParticleOptions.Deserializer<>() {
        @Override
        public @NotNull RaevyxLightningArcData fromCommand(@Nonnull ParticleType<RaevyxLightningArcData> type, @Nonnull StringReader reader) throws CommandSyntaxException {
            reader.expect(' ');
            float size = reader.readFloat();
            return new RaevyxLightningArcData(size);
        }

        @Override
        public @NotNull RaevyxLightningArcData fromNetwork(@Nonnull ParticleType<RaevyxLightningArcData> type, @Nonnull FriendlyByteBuf buf) {
            return new RaevyxLightningArcData(buf.readFloat());
        }
    };

    public static Codec<RaevyxLightningArcData> CODEC(@SuppressWarnings("unused") ParticleType<RaevyxLightningArcData> type) {
        return RecordCodecBuilder.create(b -> b.group(
                Codec.FLOAT.fieldOf("size").forGetter(RaevyxLightningArcData::size)
        ).apply(b, RaevyxLightningArcData::new));
    }

    @Override
    public void writeToNetwork(@Nonnull FriendlyByteBuf buf) { buf.writeFloat(this.size); }

    @Override
    public @NotNull String writeToString() {
        return String.format(Locale.ROOT, "%s %.2f", ModParticles.LIGHTNING_ARC.getId(), this.size);
    }

    @Override
    public @NotNull ParticleType<RaevyxLightningArcData> getType() {
        return ModParticles.LIGHTNING_ARC.get();
    }
}
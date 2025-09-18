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
 * ParticleOptions for the roar-specific lightning arc sprite set.
 */
public record LightningArcData(float size) implements ParticleOptions {
    public static final ParticleOptions.Deserializer<LightningArcData> DESERIALIZER = new ParticleOptions.Deserializer<>() {
        @Override
        public @NotNull LightningArcData fromCommand(@Nonnull ParticleType<LightningArcData> type, @Nonnull StringReader reader) throws CommandSyntaxException {
            reader.expect(' ');
            float size = reader.readFloat();
            return new LightningArcData(size);
        }

        @Override
        public @NotNull LightningArcData fromNetwork(@Nonnull ParticleType<LightningArcData> type, @Nonnull FriendlyByteBuf buf) {
            return new LightningArcData(buf.readFloat());
        }
    };

    public static Codec<LightningArcData> CODEC(@SuppressWarnings("unused") ParticleType<LightningArcData> type) {
        return RecordCodecBuilder.create(b -> b.group(
                Codec.FLOAT.fieldOf("size").forGetter(LightningArcData::size)
        ).apply(b, LightningArcData::new));
    }

    @Override
    public void writeToNetwork(@Nonnull FriendlyByteBuf buf) { buf.writeFloat(this.size); }

    @Override
    public @NotNull String writeToString() {
        return String.format(Locale.ROOT, "%s %.2f", ModParticles.LIGHTNING_ARC.getId(), this.size);
    }

    @Override
    public @NotNull ParticleType<LightningArcData> getType() {
        return ModParticles.LIGHTNING_ARC.get();
    }
}
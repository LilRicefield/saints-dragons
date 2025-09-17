package com.leon.saintsdragons.common.particle.lightningdragon;

import com.leon.saintsdragons.common.registry.ModParticles;
import com.mojang.brigadier.StringReader;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.core.particles.ParticleType;
import net.minecraft.network.FriendlyByteBuf;

import javax.annotation.Nonnull;
import java.util.Locale;

/**
 * ParticleOptions for the roar-specific lightning arc sprite set.
 */
public class LightningArcData implements ParticleOptions {
    public static final ParticleOptions.Deserializer<LightningArcData> DESERIALIZER = new ParticleOptions.Deserializer<>() {
        @Override
        public LightningArcData fromCommand(@Nonnull ParticleType<LightningArcData> type, @Nonnull StringReader reader) throws CommandSyntaxException {
            reader.expect(' ');
            float size = reader.readFloat();
            return new LightningArcData(size);
        }

        @Override
        public LightningArcData fromNetwork(@Nonnull ParticleType<LightningArcData> type, @Nonnull FriendlyByteBuf buf) {
            return new LightningArcData(buf.readFloat());
        }
    };

    public static Codec<LightningArcData> CODEC(ParticleType<LightningArcData> type) {
        return RecordCodecBuilder.create(b -> b.group(
                Codec.FLOAT.fieldOf("size").forGetter(LightningArcData::getSize)
        ).apply(b, LightningArcData::new));
    }

    private final float size;
    public LightningArcData(float size) { this.size = size; }
    public float getSize() { return size; }

    @Override
    public void writeToNetwork(@Nonnull FriendlyByteBuf buf) { buf.writeFloat(this.size); }

    @Override
    public String writeToString() {
        return String.format(Locale.ROOT, "%s %.2f", ModParticles.LIGHTNING_ARC.getId(), this.size);
    }

    @Override
    public ParticleType<LightningArcData> getType() {
        return ModParticles.LIGHTNING_ARC.get();
    }
}


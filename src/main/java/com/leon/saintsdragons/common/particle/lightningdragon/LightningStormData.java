package com.leon.saintsdragons.common.particle.lightningdragon;

import com.leon.saintsdragons.common.registry.ModParticles;
import com.mojang.brigadier.StringReader;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.core.particles.ParticleType;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.FriendlyByteBuf;

import java.util.Locale;

/**
 * Server-safe ParticleOptions payload for the lightning_storm particle.
 * Holds a single float parameter: size.
 */
public class LightningStormData implements ParticleOptions {
    public static final Deserializer<LightningStormData> DESERIALIZER = new Deserializer<>() {
        @Override
        public LightningStormData fromCommand(ParticleType<LightningStormData> type, StringReader reader) throws CommandSyntaxException {
            reader.expect(' ');
            float size = reader.readFloat();
            return new LightningStormData(size);
        }

        @Override
        public LightningStormData fromNetwork(ParticleType<LightningStormData> type, FriendlyByteBuf buf) {
            return new LightningStormData(buf.readFloat());
        }
    };

    public static Codec<LightningStormData> CODEC(ParticleType<LightningStormData> type) {
        return RecordCodecBuilder.create(b -> b.group(
                Codec.FLOAT.fieldOf("size").forGetter(LightningStormData::getSize)
        ).apply(b, LightningStormData::new));
    }

    private final float size;
    public LightningStormData(float size) { this.size = size; }

    public float getSize() { return size; }

    @Override
    public void writeToNetwork(FriendlyByteBuf buf) { buf.writeFloat(this.size); }

    @Override
    public String writeToString() {
        return String.format(Locale.ROOT, "%s %.2f", BuiltInRegistries.PARTICLE_TYPE.getKey(this.getType()), this.size);
    }

    @Override
    public ParticleType<LightningStormData> getType() {
        return ModParticles.LIGHTNING_STORM.get();
    }
}


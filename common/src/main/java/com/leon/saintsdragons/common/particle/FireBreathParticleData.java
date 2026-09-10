package com.leon.saintsdragons.common.particle;

import com.leon.saintsdragons.common.registry.ModParticles;
import com.mojang.brigadier.StringReader;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.core.particles.ParticleType;
import net.minecraft.network.FriendlyByteBuf;
import org.jetbrains.annotations.NotNull;

import java.util.Locale;

public record FireBreathParticleData(float range, float density, float scale) implements ParticleOptions {
    public static final Codec<FireBreathParticleData> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            Codec.FLOAT.fieldOf("range").forGetter(FireBreathParticleData::range),
            Codec.FLOAT.fieldOf("density").forGetter(FireBreathParticleData::density),
            Codec.FLOAT.optionalFieldOf("scale", 1.0F).forGetter(FireBreathParticleData::scale)
    ).apply(instance, FireBreathParticleData::new));

    public FireBreathParticleData(float range, float density) {
        this(range, density, 1.0F);
    }

    public FireBreathParticleData {
        range = Float.isFinite(range) ? Math.max(1, Math.min(96, range)) : (float) ExpandingBreathSection.DEFAULT_RANGE;
        density = Float.isFinite(density) ? Math.max(0, Math.min(5, density)) : 1;
        scale = Float.isFinite(scale) ? Math.max(0.1F, Math.min(5.0F, scale)) : 1.0F;
    }

    public static final Deserializer<FireBreathParticleData> DESERIALIZER = new Deserializer<>() {
        @Override
        public @NotNull FireBreathParticleData fromCommand(@NotNull ParticleType<FireBreathParticleData> type,
                                                          @NotNull StringReader reader) throws CommandSyntaxException {
            reader.expect(' ');
            float range = reader.readFloat();
            reader.expect(' ');
            float density = reader.readFloat();
            reader.expect(' ');
            return new FireBreathParticleData(range, density, reader.readFloat());
        }

        @Override
        public @NotNull FireBreathParticleData fromNetwork(@NotNull ParticleType<FireBreathParticleData> type,
                                                          @NotNull FriendlyByteBuf buffer) {
            return new FireBreathParticleData(buffer.readFloat(), buffer.readFloat(), buffer.readFloat());
        }
    };

    @Override
    public @NotNull ParticleType<FireBreathParticleData> getType() {
        return ModParticles.FIRE_BREATH_FLAME.get();
    }

    @Override
    public void writeToNetwork(@NotNull FriendlyByteBuf buffer) {
        buffer.writeFloat(range);
        buffer.writeFloat(density);
        buffer.writeFloat(scale);
    }

    @Override
    public @NotNull String writeToString() {
        return String.format(Locale.ROOT, "saintsdragons:fire_breath_flame %s %s %s", range, density, scale);
    }
}

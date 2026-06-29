package com.leon.saintsdragons.common.particle.raevyx;

import com.leon.saintsdragons.common.registry.ModParticles;
import com.mojang.brigadier.StringReader;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.core.particles.ParticleType;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.FriendlyByteBuf;
import org.jetbrains.annotations.NotNull;

import javax.annotation.Nonnull;
import java.util.Locale;

public record SonicRingData(float yaw, float pitch, float scale, int duration) implements ParticleOptions {
    public static final ParticleOptions.Deserializer<SonicRingData> DESERIALIZER = new ParticleOptions.Deserializer<>() {
        @Override
        public @NotNull SonicRingData fromCommand(@Nonnull ParticleType<SonicRingData> type, @Nonnull StringReader reader) throws CommandSyntaxException {
            reader.expect(' ');
            float yaw = reader.readFloat();
            reader.expect(' ');
            float pitch = reader.readFloat();
            reader.expect(' ');
            float scale = reader.readFloat();
            reader.expect(' ');
            int duration = reader.readInt();
            return new SonicRingData(yaw, pitch, scale, duration);
        }

        @Override
        public @NotNull SonicRingData fromNetwork(@Nonnull ParticleType<SonicRingData> type, @Nonnull FriendlyByteBuf buf) {
            return new SonicRingData(buf.readFloat(), buf.readFloat(), buf.readFloat(), buf.readInt());
        }
    };

    public static Codec<SonicRingData> CODEC(@SuppressWarnings("unused") ParticleType<SonicRingData> type) {
        return RecordCodecBuilder.create(b -> b.group(
                Codec.FLOAT.fieldOf("yaw").forGetter(SonicRingData::yaw),
                Codec.FLOAT.fieldOf("pitch").forGetter(SonicRingData::pitch),
                Codec.FLOAT.fieldOf("scale").forGetter(SonicRingData::scale),
                Codec.INT.fieldOf("duration").forGetter(SonicRingData::duration)
        ).apply(b, SonicRingData::new));
    }

    @Override
    public void writeToNetwork(@Nonnull FriendlyByteBuf buf) {
        buf.writeFloat(this.yaw);
        buf.writeFloat(this.pitch);
        buf.writeFloat(this.scale);
        buf.writeInt(this.duration);
    }

    @Override
    public @NotNull String writeToString() {
        return String.format(
                Locale.ROOT,
                "%s %.2f %.2f %.2f %d",
                BuiltInRegistries.PARTICLE_TYPE.getKey(getType()),
                this.yaw,
                this.pitch,
                this.scale,
                this.duration
        );
    }

    @Override
    public @NotNull ParticleType<SonicRingData> getType() {
        return ModParticles.RAEVYX_SONIC_RING.get();
    }
}

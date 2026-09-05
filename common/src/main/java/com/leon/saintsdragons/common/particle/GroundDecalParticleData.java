package com.leon.saintsdragons.common.particle;

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

public record GroundDecalParticleData(boolean fissure, float yaw, float scale, int duration)
        implements ParticleOptions {
    public static final double GROUND_OFFSET = 0.04D;
    public static final float NORMAL_SCALE = 7.0F;
    public static final int NORMAL_DURATION = 34;

    public static final ParticleOptions.Deserializer<GroundDecalParticleData> DESERIALIZER =
            new ParticleOptions.Deserializer<>() {
                @Override
                public @NotNull GroundDecalParticleData fromCommand(
                        @Nonnull ParticleType<GroundDecalParticleData> type,
                        @Nonnull StringReader reader) throws CommandSyntaxException {
                    reader.expect(' ');
                    boolean fissure = reader.readBoolean();
                    reader.expect(' ');
                    float yaw = reader.readFloat();
                    reader.expect(' ');
                    float scale = reader.readFloat();
                    reader.expect(' ');
                    int duration = reader.readInt();
                    return new GroundDecalParticleData(fissure, yaw, scale, duration);
                }

                @Override
                public @NotNull GroundDecalParticleData fromNetwork(
                        @Nonnull ParticleType<GroundDecalParticleData> type,
                        @Nonnull FriendlyByteBuf buffer) {
                    return new GroundDecalParticleData(
                            buffer.readBoolean(), buffer.readFloat(), buffer.readFloat(), buffer.readInt());
                }
            };

    public static Codec<GroundDecalParticleData> codec(
            @SuppressWarnings("unused") ParticleType<GroundDecalParticleData> type) {
        return RecordCodecBuilder.create(instance -> instance.group(
                Codec.BOOL.fieldOf("fissure").forGetter(GroundDecalParticleData::fissure),
                Codec.FLOAT.fieldOf("yaw").forGetter(GroundDecalParticleData::yaw),
                Codec.FLOAT.fieldOf("scale").forGetter(GroundDecalParticleData::scale),
                Codec.INT.fieldOf("duration").forGetter(GroundDecalParticleData::duration)
        ).apply(instance, GroundDecalParticleData::new));
    }

    public static GroundDecalParticleData crack(float yaw) {
        return new GroundDecalParticleData(false, yaw, NORMAL_SCALE, NORMAL_DURATION);
    }

    public static GroundDecalParticleData fissure(float yaw, float scale, int duration) {
        return new GroundDecalParticleData(true, yaw, scale, duration);
    }

    @Override
    public void writeToNetwork(@Nonnull FriendlyByteBuf buffer) {
        buffer.writeBoolean(this.fissure);
        buffer.writeFloat(this.yaw);
        buffer.writeFloat(this.scale);
        buffer.writeInt(this.duration);
    }

    @Override
    public @NotNull String writeToString() {
        return String.format(
                Locale.ROOT,
                "%s %s %.2f %.2f %d",
                BuiltInRegistries.PARTICLE_TYPE.getKey(getType()),
                this.fissure,
                this.yaw,
                this.scale,
                this.duration
        );
    }

    @Override
    public @NotNull ParticleType<GroundDecalParticleData> getType() {
        return this.fissure ? ModParticles.GROUND_CRACK_FISSURE.get() : ModParticles.GROUND_CRACK.get();
    }
}

package com.leon.saintsdragons.common.particle.raevyx;

import com.leon.saintsdragons.common.registry.ModParticles;
import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.core.particles.ParticleType;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.NotNull;

import java.util.Locale;

/**
 * ParticleOptions for animated lightning chain effects.
 * Carries start and end positions for the lightning arc.
 */
public record RaevyxLightningChainData(float size, Vec3 startPos, Vec3 endPos, boolean female) implements ParticleOptions {
    public static final MapCodec<RaevyxLightningChainData> MAP_CODEC = RecordCodecBuilder.mapCodec(b -> b.group(
            Codec.FLOAT.fieldOf("size").forGetter(RaevyxLightningChainData::size),
            Vec3.CODEC.fieldOf("startPos").forGetter(RaevyxLightningChainData::startPos),
            Vec3.CODEC.fieldOf("endPos").forGetter(RaevyxLightningChainData::endPos),
            Codec.BOOL.optionalFieldOf("female", false).forGetter(RaevyxLightningChainData::female)
    ).apply(b, RaevyxLightningChainData::new));

    public static final StreamCodec<RegistryFriendlyByteBuf, RaevyxLightningChainData> STREAM_CODEC = StreamCodec.of(
            (buf, data) -> {
                buf.writeFloat(data.size());
                buf.writeDouble(data.startPos().x);
                buf.writeDouble(data.startPos().y);
                buf.writeDouble(data.startPos().z);
                buf.writeDouble(data.endPos().x);
                buf.writeDouble(data.endPos().y);
                buf.writeDouble(data.endPos().z);
                buf.writeBoolean(data.female());
            },
            buf -> {
                float size = buf.readFloat();
                Vec3 start = new Vec3(buf.readDouble(), buf.readDouble(), buf.readDouble());
                Vec3 end = new Vec3(buf.readDouble(), buf.readDouble(), buf.readDouble());
                boolean female = buf.readBoolean();
                return new RaevyxLightningChainData(size, start, end, female);
            }
    );

    public static Codec<RaevyxLightningChainData> CODEC(@SuppressWarnings("unused") ParticleType<RaevyxLightningChainData> type) {
        return MAP_CODEC.codec();
    }

    public @NotNull String writeToString() {
        return String.format(Locale.ROOT, "%s %.2f %.2f %.2f %.2f %.2f %.2f %.2f %s", 
                BuiltInRegistries.PARTICLE_TYPE.getKey(ModParticles.LIGHTNING_CHAIN.get()), this.size,
                this.startPos.x, this.startPos.y, this.startPos.z,
                this.endPos.x, this.endPos.y, this.endPos.z,
                Boolean.toString(this.female));
    }

    @Override
    public @NotNull ParticleType<RaevyxLightningChainData> getType() {
        return ModParticles.LIGHTNING_CHAIN.get();
    }
}

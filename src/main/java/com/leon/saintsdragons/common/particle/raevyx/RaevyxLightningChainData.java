package com.leon.saintsdragons.common.particle.raevyx;

import com.leon.saintsdragons.common.registry.ModParticles;
import com.mojang.brigadier.StringReader;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.core.particles.ParticleType;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.NotNull;

import javax.annotation.Nonnull;
import java.util.Locale;

/**
 * ParticleOptions for animated lightning chain effects.
 * Carries start and end positions for the lightning arc.
 */
public record RaevyxLightningChainData(float size, Vec3 startPos, Vec3 endPos) implements ParticleOptions {
    public static final ParticleOptions.Deserializer<RaevyxLightningChainData> DESERIALIZER = new ParticleOptions.Deserializer<>() {
        @Override
        public @NotNull RaevyxLightningChainData fromCommand(@Nonnull ParticleType<RaevyxLightningChainData> type, @Nonnull StringReader reader) throws CommandSyntaxException {
            reader.expect(' ');
            float size = reader.readFloat();
            reader.expect(' ');
            double startX = reader.readDouble();
            reader.expect(' ');
            double startY = reader.readDouble();
            reader.expect(' ');
            double startZ = reader.readDouble();
            reader.expect(' ');
            double endX = reader.readDouble();
            reader.expect(' ');
            double endY = reader.readDouble();
            reader.expect(' ');
            double endZ = reader.readDouble();
            
            return new RaevyxLightningChainData(size, new Vec3(startX, startY, startZ), new Vec3(endX, endY, endZ));
        }

        @Override
        public @NotNull RaevyxLightningChainData fromNetwork(@Nonnull ParticleType<RaevyxLightningChainData> type, @Nonnull FriendlyByteBuf buf) {
            float size = buf.readFloat();
            double startX = buf.readDouble();
            double startY = buf.readDouble();
            double startZ = buf.readDouble();
            double endX = buf.readDouble();
            double endY = buf.readDouble();
            double endZ = buf.readDouble();
            
            return new RaevyxLightningChainData(size, new Vec3(startX, startY, startZ), new Vec3(endX, endY, endZ));
        }
    };

    public static Codec<RaevyxLightningChainData> CODEC(@SuppressWarnings("unused") ParticleType<RaevyxLightningChainData> type) {
        return RecordCodecBuilder.create(b -> b.group(
                Codec.FLOAT.fieldOf("size").forGetter(RaevyxLightningChainData::size),
                Vec3.CODEC.fieldOf("startPos").forGetter(RaevyxLightningChainData::startPos),
                Vec3.CODEC.fieldOf("endPos").forGetter(RaevyxLightningChainData::endPos)
        ).apply(b, RaevyxLightningChainData::new));
    }

    @Override
    public void writeToNetwork(@Nonnull FriendlyByteBuf buf) {
        buf.writeFloat(this.size);
        buf.writeDouble(this.startPos.x);
        buf.writeDouble(this.startPos.y);
        buf.writeDouble(this.startPos.z);
        buf.writeDouble(this.endPos.x);
        buf.writeDouble(this.endPos.y);
        buf.writeDouble(this.endPos.z);
    }

    @Override
    public @NotNull String writeToString() {
        return String.format(Locale.ROOT, "%s %.2f %.2f %.2f %.2f %.2f %.2f %.2f", 
                ModParticles.LIGHTNING_CHAIN.getId(), this.size,
                this.startPos.x, this.startPos.y, this.startPos.z,
                this.endPos.x, this.endPos.y, this.endPos.z);
    }

    @Override
    public @NotNull ParticleType<RaevyxLightningChainData> getType() {
        return ModParticles.LIGHTNING_CHAIN.get();
    }
}

package com.leon.saintsdragons.common.particle;

import com.leon.saintsdragons.common.registry.ModParticles;
import com.mojang.brigadier.StringReader;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.serialization.Codec;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.core.particles.ParticleType;
import net.minecraft.network.FriendlyByteBuf;
import org.jetbrains.annotations.NotNull;

public record FireBreathBurstData(int dragonId) implements ParticleOptions {
    public static final Codec<FireBreathBurstData> CODEC = Codec.INT.fieldOf("dragon_id")
            .xmap(FireBreathBurstData::new, FireBreathBurstData::dragonId).codec();
    public static final Deserializer<FireBreathBurstData> DESERIALIZER = new Deserializer<>() {
        @Override
        public @NotNull FireBreathBurstData fromCommand(@NotNull ParticleType<FireBreathBurstData> type,
                                                        @NotNull StringReader reader) throws CommandSyntaxException {
            reader.expect(' ');
            return new FireBreathBurstData(reader.readInt());
        }

        @Override
        public @NotNull FireBreathBurstData fromNetwork(@NotNull ParticleType<FireBreathBurstData> type,
                                                        @NotNull FriendlyByteBuf buffer) {
            return new FireBreathBurstData(buffer.readVarInt());
        }
    };

    @Override
    public @NotNull ParticleType<FireBreathBurstData> getType() {
        return ModParticles.FIRE_BREATH_BURST.get();
    }

    @Override
    public void writeToNetwork(@NotNull FriendlyByteBuf buffer) {
        buffer.writeVarInt(dragonId);
    }

    @Override
    public @NotNull String writeToString() {
        return "saintsdragons:fire_breath_burst " + dragonId;
    }
}

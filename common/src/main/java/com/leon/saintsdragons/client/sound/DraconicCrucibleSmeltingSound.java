package com.leon.saintsdragons.client.sound;

import com.leon.saintsdragons.common.block.DraconicCrucibleBlock;
import com.leon.saintsdragons.common.registry.ModSounds;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.resources.sounds.AbstractTickableSoundInstance;
import net.minecraft.client.resources.sounds.SoundInstance;
import net.minecraft.core.BlockPos;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.level.block.state.BlockState;

@Environment(EnvType.CLIENT)
public final class DraconicCrucibleSmeltingSound extends AbstractTickableSoundInstance {
    private final ClientLevel level;
    private final BlockPos pos;

    public DraconicCrucibleSmeltingSound(ClientLevel level, BlockPos pos) {
        super(ModSounds.DRACONIC_CRUCIBLE_SMELTING.get(),
                SoundSource.BLOCKS, SoundInstance.createUnseededRandom());
        this.level = level;
        this.pos = pos.immutable();
        this.looping = true;
        this.delay = 0;
        this.volume = 0.65F;
        this.pitch = 1.0F;
        this.attenuation = Attenuation.LINEAR;
        this.x = pos.getX() + 0.5D;
        this.y = pos.getY() + 0.5D;
        this.z = pos.getZ() + 0.5D;
    }

    @Override
    public void tick() {
        if (Minecraft.getInstance().level != this.level || !isCrucibleSmelting()) {
            stop();
        }
    }

    private boolean isCrucibleSmelting() {
        if (!this.level.hasChunkAt(this.pos)) {
            return false;
        }
        BlockState state = this.level.getBlockState(this.pos);
        return state.getBlock() instanceof DraconicCrucibleBlock
                && state.hasProperty(DraconicCrucibleBlock.LIT)
                && state.getValue(DraconicCrucibleBlock.LIT);
    }
}

package com.leon.saintsdragons.client.sound.raevyx;

import com.leon.saintsdragons.common.registry.ModSounds;
import com.leon.saintsdragons.server.entity.dragons.raevyx.Raevyx;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.resources.sounds.AbstractTickableSoundInstance;
import net.minecraft.client.resources.sounds.SoundInstance;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.phys.Vec3;

/**
 * Looping lightning beam audio that tracks the Raevyx's beam origin.
 */
@Environment(EnvType.CLIENT)
public class RaevyxLightningBeamLoopSound extends AbstractTickableSoundInstance {
    private final Raevyx raevyx;

    public RaevyxLightningBeamLoopSound(Raevyx raevyx) {
        super(ModSounds.RAEVYX_LIGHTNING_BEAMING.get(), SoundSource.NEUTRAL, SoundInstance.createUnseededRandom());
        this.raevyx = raevyx;
        this.looping = true;
        this.delay = 0;
        this.volume = 1.4f;
        this.pitch = 1.0f;
        this.attenuation = Attenuation.LINEAR;
        updatePosition();
    }

    public Raevyx getRaevyx() {
        return raevyx;
    }

    @Override
    public void tick() {
        if (raevyx == null || raevyx.isRemoved() || !raevyx.isAlive()) {
            stop();
            return;
        }
        if (!raevyx.isBeaming()) {
            stop();
            return;
        }
        updatePosition();
    }

    private void updatePosition() {
        Vec3 start = raevyx.getBeamStartPosition();
        if (start == null) {
            start = raevyx.getBeamStartAnchor(1.0f);
        }
        if (start == null) {
            start = raevyx.position().add(0.0D, raevyx.getBbHeight() * 0.65D, 0.0D);
        }
        this.x = start.x;
        this.y = start.y;
        this.z = start.z;

        float sizeScale = (float) Math.max(1.0D, raevyx.getBbWidth());
        this.volume = 1.2f + (sizeScale * 0.15f);
    }
}

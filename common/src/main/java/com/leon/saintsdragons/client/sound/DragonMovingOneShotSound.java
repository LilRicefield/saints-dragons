package com.leon.saintsdragons.client.sound;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.Minecraft;
import net.minecraft.client.resources.sounds.AbstractTickableSoundInstance;
import net.minecraft.client.resources.sounds.SoundInstance;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.Entity;

@Environment(EnvType.CLIENT)
public class DragonMovingOneShotSound extends AbstractTickableSoundInstance {
    private final int entityId;
    private final int maxLifeTicks;
    private int lifeTicks;

    public DragonMovingOneShotSound(int entityId, SoundEvent sound, float volume, float pitch, int durationTicks) {
        super(sound, SoundSource.NEUTRAL, SoundInstance.createUnseededRandom());
        this.entityId = entityId;
        this.maxLifeTicks = Math.max(1, durationTicks);
        this.looping = false;
        this.delay = 0;
        this.volume = volume;
        this.pitch = pitch;
        this.attenuation = Attenuation.LINEAR;
        this.lifeTicks = 0;
        updatePosition();
    }

    @Override
    public boolean canPlaySound() {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.level == null) {
            return false;
        }
        Entity entity = minecraft.level.getEntity(entityId);
        return entity != null && entity.isAlive() && !entity.isSilent();
    }

    @Override
    public boolean canStartSilent() {
        return true;
    }

    @Override
    public void tick() {
        lifeTicks++;
        if (lifeTicks >= maxLifeTicks) {
            stop();
            return;
        }
        if (!updatePosition()) {
            stop();
        }
    }

    private boolean updatePosition() {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.level == null) {
            return false;
        }
        Entity entity = minecraft.level.getEntity(entityId);
        if (entity == null || entity.isRemoved() || !entity.isAlive()) {
            return false;
        }
        this.x = entity.getX();
        this.y = entity.getY() + entity.getBbHeight() * 0.65D;
        this.z = entity.getZ();
        return true;
    }

    public void updateMix(float volume, float pitch) {
        this.volume = volume;
        this.pitch = pitch;
    }
}

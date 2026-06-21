package com.leon.saintsdragons.server.entity.handler;

import com.leon.saintsdragons.server.entity.base.DragonEntity;
import com.leon.saintsdragons.server.entity.base.DragonEntity.VocalEntry;
import com.leon.saintsdragons.server.entity.interfaces.DragonSoundProfile;
import com.leon.saintsdragons.sound.api.DragonSoundSpec;
import com.leon.saintsdragons.sound.server.DragonSoundOrchestrator;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

import java.util.HashMap;
import java.util.Map;

public class DragonSoundHandler {
    private final DragonEntity dragon;
    private final DragonSoundProfile profile;
    private static final int MIN_OVERLAP_GUARD_TICKS = 5;
    private static final Map<String, Integer> GENERIC_VOCAL_WINDOWS = Map.of(
            "hurt", 20,
            "die", 62
    );
    private final Map<String, Integer> vocalCooldowns = new HashMap<>();
    private static final int COOLDOWN_PRUNE_INTERVAL_TICKS = 200;
    private int pruneCooldownCounter = 0;

    public DragonSoundHandler(DragonEntity dragon) {
        this.dragon = dragon;
        DragonSoundProfile providedProfile = dragon.getSoundProfile();
        this.profile = providedProfile != null ? providedProfile : DragonSoundProfile.EMPTY;
    }

    public void tick() {
        if (++pruneCooldownCounter >= COOLDOWN_PRUNE_INTERVAL_TICKS) {
            pruneCooldownCounter = 0;
            vocalCooldowns.entrySet().removeIf(entry -> entry.getValue() <= dragon.tickCount);
        }
    }

    private boolean isInCooldown(String key) {
        Integer cooldown = vocalCooldowns.get(key);
        return cooldown != null && cooldown > dragon.tickCount;
    }

    private void startCooldown(String key) {
        int window = GENERIC_VOCAL_WINDOWS.getOrDefault(key, 20);
        vocalCooldowns.put(key, dragon.tickCount + Math.max(window, MIN_OVERLAP_GUARD_TICKS));
    }

    public void playVocal(String vocalKey) {
        if (dragon.isRemoved() || dragon.isDeadOrDying()) {
            return;
        }
        if (dragon.isBaby() && isGrumbleVocal(vocalKey)) {
            return;
        }
        if (isInCooldown(vocalKey)) {
            return;
        }
        startCooldown(vocalKey);
        DragonSoundProfile profile = dragon.getSoundProfile();
        if (profile != null && profile.handleVocal(this, dragon, vocalKey)) {
            return;
        }

        DragonEntity.VocalEntry entry = resolveVocalEntry(profile, vocalKey);
        if (entry == null || entry.soundSupplier() == null) {
            return;
        }
        if (!entry.allowDuringSleep() && (dragon.isSleeping() || dragon.isSleepTransitioning())) {
            return;
        }
        if (!entry.allowWhenSitting() && dragon.isStayOrSitMuted()) {
            return;
        }

        if (entry.animationId() != null && !entry.animationId().isEmpty()) {
            String controllerId = entry.controllerId() != null && !entry.controllerId().isEmpty() 
                ? entry.controllerId() 
                : "action";
            dragon.triggerAnim(controllerId, vocalKey);
        }

        DragonSoundSpec vocalSpec = profile != null ? profile.getVocalSpec(this, dragon, vocalKey, entry) : null;
        if (vocalSpec != null) {
            DragonSoundOrchestrator.play(dragon, vocalSpec);
        }
    }

    private static boolean isGrumbleVocal(String key) {
        return key != null && key.contains("grumble");
    }

    public void playMovingEntitySound(SoundEvent sound, float volume, float pitch, int durationTicks) {
        if (sound == null) {
            return;
        }
        DragonSoundOrchestrator.play(
                dragon,
                DragonSoundSpec.moving(sound, SoundSource.NEUTRAL, volume, pitch, durationTicks)
        );
    }


    public void playClientSound(DragonEntity dragon, Vec3 position, SoundEvent sound, float volume, float pitch) {
        double x = position != null ? position.x : dragon.getX();
        double y = position != null ? position.y : dragon.getY();
        double z = position != null ? position.z : dragon.getZ();
        Level level = dragon.level();
        if (level.isClientSide) {
            level.playLocalSound(x, y, z, sound, SoundSource.NEUTRAL, volume, pitch, false);
        } else {
            level.playSound(null, x, y, z, sound, SoundSource.NEUTRAL, volume, pitch);
        }
    }

    private DragonEntity.VocalEntry resolveVocalEntry(DragonSoundProfile profile, String key) {
        DragonEntity.VocalEntry entry = dragon.getVocalEntries().get(key);
        if (entry != null) {
            return entry;
        }
        if (profile != null) {
            entry = profile.getFallbackVocalEntry(key);
            if (entry != null) {
                return entry;
            }
        }
        int underscore = key.indexOf('_');
        while (underscore > 0) {
            String suffix = key.substring(underscore + 1);
            entry = dragon.getVocalEntries().get(suffix);
            if (entry != null) {
                return entry;
            }
            if (profile != null) {
                entry = profile.getFallbackVocalEntry(suffix);
                if (entry != null) {
                    return entry;
                }
            }
            underscore = key.indexOf('_', underscore + 1);
        }
        return null;
    }

    public DragonSoundProfile getProfile() {
        return profile;
    }

    public DragonEntity getDragon() {
        return dragon;
    }

}

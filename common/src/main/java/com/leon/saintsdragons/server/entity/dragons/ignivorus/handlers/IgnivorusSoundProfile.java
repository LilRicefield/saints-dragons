package com.leon.saintsdragons.server.entity.dragons.ignivorus.handlers;

import com.leon.saintsdragons.common.registry.ModSounds;
import com.leon.saintsdragons.server.entity.base.DragonEntity;
import com.leon.saintsdragons.server.entity.dragons.ignivorus.Ignivorus;
import com.leon.saintsdragons.server.entity.handler.DragonSoundHandler;
import com.leon.saintsdragons.server.entity.interfaces.DragonSoundProfile;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.phys.Vec3;

import java.util.Map;

/**
 * Ignivorus-specific animation sound routing and locator support.
 */
public final class IgnivorusSoundProfile implements DragonSoundProfile {

    public static final IgnivorusSoundProfile INSTANCE = new IgnivorusSoundProfile();

    private static final Map<String, Integer> VOCAL_WINDOWS = Map.of(
            "ignivorus_roar", 64
    );

    private static final Map<String, DragonEntity.VocalEntry> FALLBACK_VOCALS =
            new DragonEntity.VocalEntryBuilder()
                    .add("ignivorus_roar", "action", "animation.ignivorus.roar",
                            ModSounds.IGNIVORUS_ROAR, 1.8f, 0.85f, 0.15f, false, false, false)
                    .build();

    private IgnivorusSoundProfile() {}

    @Override
    public boolean handleAnimationSound(DragonSoundHandler handler, DragonEntity dragon, String key, String locator) {
        if ("ignivorus_bite".equals(key)) {
            playMouthSound(handler, dragon, locator, ModSounds.IGNIVORUS_BITE.get(), 1.2f, 0.95f, 0.1f);
            return true;
        }
        if ("ignivorus_body_slam".equals(key)) {
            // Use body locator for slam impact
            Vec3 body = handler.resolveLocatorWorldPos(locator != null && !locator.isEmpty() ? locator : "body_locator");
            double x = body != null ? body.x : dragon.getX();
            double y = body != null ? body.y : dragon.getY();
            double z = body != null ? body.z : dragon.getZ();
            float pitch = 0.9f + dragon.getRandom().nextFloat() * 0.15f;
            dragon.level().playLocalSound(x, y, z, ModSounds.IGNIVORUS_BODY_SLAM.get(), SoundSource.NEUTRAL, 2.0f, pitch, false);
            return true;
        }
        if ("ignivorus_walk".equals(key)) {
            playMovementSound(handler, dragon, locator, ModSounds.IGNIVORUS_WALK.get(), 1.0f, 0.85f);
            return true;
        }
        if ("ignivorus_run".equals(key)) {
            playMovementSound(handler, dragon, locator, ModSounds.IGNIVORUS_RUN.get(), 1.1f, 0.9f);
            return true;
        }
        if ("ignivorus_grumble1".equals(key)) {
            playMouthSound(handler, dragon, locator, ModSounds.IGNIVORUS_GRUMBLE_1.get(), 1.1f, 0.95f, 0.05f);
            return true;
        }
        if ("ignivorus_grumble2".equals(key)) {
            playMouthSound(handler, dragon, locator, ModSounds.IGNIVORUS_GRUMBLE_2.get(), 1.15f, 1.0f, 0.05f);
            return true;
        }
        if ("ignivorus_grumble3".equals(key)) {
            playMouthSound(handler, dragon, locator, ModSounds.IGNIVORUS_GRUMBLE_3.get(), 1.2f, 0.9f, 0.05f);
            return true;
        }
        if ("ignivorus_roar".equals(key)) {
            return true; // Roar ability plays the synced audio
        }
        if ("ignivorus_ultimate_start".equals(key)) {
            playMouthSound(handler, dragon, locator, ModSounds.IGNIVORUS_ULTIMATE_START.get(), 2.0f, 1.0f, 0.0f);
            return true;
        }
        if ("ignivorus_ultimate".equals(key)) {
            playMouthSound(handler, dragon, locator, ModSounds.IGNIVORUS_ULTIMATE.get(), 2.5f, 1.0f, 0.0f);
            return true;
        }
        if ("ignivorus_ultimate_end".equals(key)) {
            playMouthSound(handler, dragon, locator, ModSounds.IGNIVORUS_ULTIMATE_END.get(), 2.0f, 1.0f, 0.0f);
            return true;
        }
        if ("ignivorus_fire_breath_start".equals(key)) {
            // Block keyframe sound - fire breath start is handled by IgnivorusFireBreathAbility
            return true;
        }
        if ("ignivorus_fire_breathing".equals(key)) {
            // Block keyframe sound - fire breath loop is handled by IgnivorusFireBreathSoundController
            return true;
        }
        if ("ignivorus_fire_breath_end".equals(key)) {
            // Block keyframe sound - fire breath end is handled by IgnivorusFireBreathAbility
            return true;
        }
        if ("ignivorus_takeoff".equals(key)) {
            // Block keyframe sound - takeoff sound is handled by Ignivorus.setTakeoff()
            return true;
        }
        if ("ignivorus_landed".equals(key)) {
            playLandedSound(handler, dragon, locator);
            return true;
        }
        return false;
    }

    @Override
    public boolean handleWingFlapSound(DragonSoundHandler handler, DragonEntity dragon, String key) {
        float volume = dragon.isBaby() ? 0.7f : 1.2f;
        float pitch = 0.95f + (dragon.getRandom().nextFloat() - 0.5f) * 0.1f;
        dragon.level().playLocalSound(dragon.getX(), dragon.getY(), dragon.getZ(),
                ModSounds.IGNIVORUS_FLAP.get(), SoundSource.NEUTRAL, volume, pitch, false);
        return true;
    }

    @Override
    public DragonEntity.VocalEntry getFallbackVocalEntry(String key) {
        return FALLBACK_VOCALS.get(key);
    }

    @Override
    public int getVocalAnimationWindowTicks(String key) {
        return VOCAL_WINDOWS.getOrDefault(key, -1);
    }

    @Override
    public Vec3 resolveLocator(DragonSoundHandler handler, DragonEntity dragon, String locator) {
        if (dragon instanceof Ignivorus ignivorus) {
            Vec3 cached = ignivorus.getClientLocatorPosition(locator);
            if (cached != null) {
                return cached;
            }
        }
        return DragonSoundProfile.super.resolveLocator(handler, dragon, locator);
    }

    private void playMouthSound(DragonSoundHandler handler, DragonEntity dragon, String locator,
                                net.minecraft.sounds.SoundEvent sound, float volume,
                                float basePitch, float variance) {
        Vec3 mouth = handler.resolveLocatorWorldPos(locator != null && !locator.isEmpty() ? locator : "mouth_origin");
        float pitch = basePitch;
        if (variance != 0f) {
            pitch += dragon.getRandom().nextFloat() * variance;
        }
        double x = mouth != null ? mouth.x : dragon.getX();
        double y = mouth != null ? mouth.y : dragon.getY();
        double z = mouth != null ? mouth.z : dragon.getZ();

        dragon.level().playLocalSound(x, y, z, sound, SoundSource.NEUTRAL, volume, pitch, false);
    }

    private void playMovementSound(DragonSoundHandler handler, DragonEntity dragon, String locator,
                                   SoundEvent sound, float volume, float pitch) {
        // Add cooldown to prevent rapid-fire step sounds during animation transitions
        if (dragon instanceof Ignivorus) {
            long currentTick = dragon.tickCount;
            long tickDiff = currentTick - handler.getLastStepTick();

            if (tickDiff < 5) {
                return; // Blocked by cooldown (minimum 5 ticks = 250ms)
            }
            handler.setLastStepTick(currentTick);
        }

        Vec3 body = handler.resolveLocatorWorldPos(locator != null && !locator.isEmpty() ? locator : "body_locator");
        double x = body != null ? body.x : dragon.getX();
        double y = body != null ? body.y : dragon.getY();
        double z = body != null ? body.z : dragon.getZ();
        float vol = volume * 1.2f;
        float pit = pitch + (dragon.getRandom().nextFloat() - 0.5f) * 0.05f;
        dragon.level().playLocalSound(x, y, z, sound, SoundSource.NEUTRAL, vol, pit, false);
    }

    private void playLandedSound(DragonSoundHandler handler, DragonEntity dragon, String locator) {
        Vec3 at = handler.resolveLocatorWorldPos(
                locator != null && !locator.isEmpty() ? locator : "body_locator"
        );
        double x = at != null ? at.x : dragon.getX();
        double y = at != null ? at.y : dragon.getY();
        double z = at != null ? at.z : dragon.getZ();
        dragon.level().playLocalSound(x, y, z, ModSounds.IGNIVORUS_LANDED.get(), SoundSource.NEUTRAL, 1.5f, 1.0f, false);
    }
}

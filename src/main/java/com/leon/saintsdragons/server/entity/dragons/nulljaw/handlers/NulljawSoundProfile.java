package com.leon.saintsdragons.server.entity.dragons.nulljaw.handlers;

import com.leon.saintsdragons.common.registry.ModSounds;
import com.leon.saintsdragons.server.entity.base.DragonEntity;
import com.leon.saintsdragons.server.entity.handler.DragonSoundHandler;
import com.leon.saintsdragons.server.entity.interfaces.DragonSoundProfile;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.phys.Vec3;

/**
 * Nulljaw-specific animation keyed sounds.
 * Uses direct playSound calls like the working roar ability to bypass handler complexity.
 */
public final class NulljawSoundProfile implements DragonSoundProfile {

    public static final NulljawSoundProfile INSTANCE = new NulljawSoundProfile();

    private NulljawSoundProfile() {}

    @Override
    public boolean handleAnimationSound(DragonSoundHandler handler, DragonEntity dragon, String key, String locator) {
        // Handler already blocks server-side, we're only called on client

        return switch (key) {
            case "nulljaw_grumble1" -> {
                playClientSound(dragon, ModSounds.NULLJAW_GRUMBLE_1.get(), 0.8f, 0.95f, 0.1f);
                yield true;
            }
            case "nulljaw_grumble2" -> {
                playClientSound(dragon, ModSounds.NULLJAW_GRUMBLE_2.get(), 0.8f, 0.95f, 0.1f);
                yield true;
            }
            case "nulljaw_grumble3" -> {
                playClientSound(dragon, ModSounds.NULLJAW_GRUMBLE_3.get(), 0.8f, 0.95f, 0.1f);
                yield true;
            }
            case "nulljaw_phase2" -> {
                playClientSound(dragon, ModSounds.NULLJAW_PHASE2.get(), 2.0f, 0.9f, 0.2f);
                yield true;
            }
            case "nulljaw_phase1" -> {
                playClientSound(dragon, ModSounds.NULLJAW_PHASE1.get(), 1.4f, 0.9f, 0.2f);
                yield true;
            }
            case "nulljaw_step" -> {
                playClientSound(dragon, ModSounds.NULLJAW_STEP.get(), 0.8f, 0.9f, 0.2f);
                yield true;
            }
            case "nulljaw_claw" -> {
                playClientSound(dragon, ModSounds.NULLJAW_CLAW.get(), 1.2f, 0.9f, 0.2f);
                yield true;
            }
            case "nulljaw_bite" -> {
                playClientSound(dragon, ModSounds.NULLJAW_BITE.get(), 1.1f, 0.95f, 0.1f);
                yield true;
            }
            case "nulljaw_roarclaw" -> {
                playClientSound(dragon, ModSounds.NULLJAW_ROARCLAW.get(), 1.3f, 0.9f, 0.2f);
                yield true;
            }
            case "nulljaw_roar" -> {
                // Roar is handled by ability, return false to skip
                yield false;
            }
            default -> false;
        };
    }

    /**
     * Play sound on client side for animation keyframes.
     * GeckoLib fires sound events on client, so we use playLocalSound.
     */
    private void playClientSound(DragonEntity dragon, net.minecraft.sounds.SoundEvent sound,
                                 float volume, float basePitch, float variance) {
        float pitch = basePitch + dragon.getRandom().nextFloat() * variance;
        // Client-side local sound playback
        dragon.level().playLocalSound(
                dragon.getX(),
                dragon.getY(),
                dragon.getZ(),
                sound,
                SoundSource.NEUTRAL,
                volume,
                pitch,
                false  // distanceDelay
        );
    }
}

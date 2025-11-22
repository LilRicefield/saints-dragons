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
                playClientSound(dragon, ModSounds.NULLJAW_GRUMBLE_1.get(), 1.5f, 0.95f, 0.1f);
                yield true;
            }
            case "nulljaw_grumble2" -> {
                playClientSound(dragon, ModSounds.NULLJAW_GRUMBLE_2.get(), 1.5f, 0.95f, 0.1f);
                yield true;
            }
            case "nulljaw_grumble3" -> {
                playClientSound(dragon, ModSounds.NULLJAW_GRUMBLE_3.get(), 1.5f, 0.95f, 0.1f);
                yield true;
            }
            case "nulljaw_phase2_start" -> {
                // Start buildup sound for phase2 transition
                playClientSound(dragon, ModSounds.NULLJAW_PHASE2_START.get(), 2.0f, 0.9f, 0.1f);
                yield true;
            }
            case "nulljaw_phase2" -> {
                // Main phase 2 roar (plays during middle animation)
                playClientSound(dragon, ModSounds.NULLJAW_PHASE2.get(), 2.5f, 0.9f, 0.2f);
                yield true;
            }
            case "nulljaw_phase2_end" -> {
                // Ending sound for phase2 transition (settling down)
                playClientSound(dragon, ModSounds.NULLJAW_PHASE2_END.get(), 1.8f, 0.95f, 0.15f);
                yield true;
            }
            case "nulljaw_phase2_underwater" -> {
                playClientSound(dragon, ModSounds.NULLJAW_PHASE2.get(), 2.5f, 0.9f, 0.2f);
                yield true;
            }
            case "nulljaw_phase1", "nulljaw_phase1_underwater" -> {
                playClientSound(dragon, ModSounds.NULLJAW_PHASE1.get(), 1.4f, 0.9f, 0.2f);
                yield true;
            }
            case "nulljaw_walk" -> {
                playClientSound(dragon, ModSounds.NULLJAW_WALK.get(), 0.8f, 0.9f, 0.2f);
                yield true;
            }
            case "nulljaw_run" -> {
                playClientSound(dragon, ModSounds.NULLJAW_RUN.get(), 0.8f, 0.9f, 0.2f);
                yield true;
            }
            case "nulljaw_walk2" -> {
                playClientSound(dragon, ModSounds.NULLJAW_WALK2.get(), 0.8f, 0.9f, 0.2f);
                yield true;
            }
            case "nulljaw_run2" -> {
                playClientSound(dragon, ModSounds.NULLJAW_RUN2.get(), 0.8f, 0.9f, 0.2f);
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
            case "nulljaw_hurt" -> {
                playClientSound(dragon, ModSounds.NULLJAW_HURT.get(), 1.2f, 0.95f, 0.05f);
                yield true;
            }
            case "nulljaw_die" -> {
                playClientSound(dragon, ModSounds.NULLJAW_DIE.get(), 1.4f, 0.9f, 0.05f);
                yield true;
            }
            case "nulljaw_horngore" -> {
                playClientSound(dragon, ModSounds.NULLJAW_HORNGORE.get(), 1.2f, 0.9f, 0.2f);
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

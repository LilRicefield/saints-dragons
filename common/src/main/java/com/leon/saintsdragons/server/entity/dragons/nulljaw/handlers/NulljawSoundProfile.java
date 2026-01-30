package com.leon.saintsdragons.server.entity.dragons.nulljaw.handlers;

import com.leon.saintsdragons.common.registry.ModSounds;
import com.leon.saintsdragons.server.entity.base.DragonEntity;
import com.leon.saintsdragons.server.entity.handler.DragonSoundHandler;
import com.leon.saintsdragons.server.entity.interfaces.DragonSoundProfile;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.phys.Vec3;

import java.util.Map;

/**
 * Nulljaw-specific vocal timing metadata and animation sound routing.
 * Uses client-side local playback for animation keyframe sounds (more efficient).
 */
public final class NulljawSoundProfile implements DragonSoundProfile {

    public static final NulljawSoundProfile INSTANCE = new NulljawSoundProfile();
    private static final float BABY_PITCH_MULTIPLIER = 1.6f;
    private static final Map<String, Boolean> BABY_ALLOWED_KEYS = Map.ofEntries(
            Map.entry("nulljaw_eat", true),
            Map.entry("nulljaw_hurt", true),
            Map.entry("nulljaw_die", true)
    );

    private static final Map<String, Integer> VOCAL_WINDOWS = Map.ofEntries(
            Map.entry("grumble1", 100),
            Map.entry("grumble2", 120),
            Map.entry("grumble3", 80),
            Map.entry("roar", 140),
            Map.entry("roar2", 160),
            Map.entry("phase1", 85),
            Map.entry("phase2", 180),
            Map.entry("phase2_start", 60),
            Map.entry("phase2_end", 70),
            Map.entry("hurt", 20),
            Map.entry("die", 90)
    );

    private static final Map<String, String> EFFECT_TO_VOCAL_KEY = Map.ofEntries(
            Map.entry("nulljaw_grumble1", "grumble1"),
            Map.entry("nulljaw_grumble2", "grumble2"),
            Map.entry("nulljaw_grumble3", "grumble3"),
            Map.entry("nulljaw_roar", "roar"),
            Map.entry("nulljaw_roar2", "roar2"),
            Map.entry("nulljaw_phase1", "phase1"),
            Map.entry("nulljaw_phase1_underwater", "phase1"),
            Map.entry("nulljaw_phase2", "phase2"),
            Map.entry("nulljaw_phase2_start", "phase2_start"),
            Map.entry("nulljaw_phase2_end", "phase2_end"),
            Map.entry("nulljaw_phase2_underwater", "phase2"),
            Map.entry("nulljaw_hurt", "hurt"),
            Map.entry("nulljaw_die", "die")
    );

    private static final Map<String, DragonEntity.VocalEntry> FALLBACK_VOCALS =
            new DragonEntity.VocalEntryBuilder()
                    .add("grumble1", "action", "animation.nulljaw.grumble1",
                            ModSounds.NULLJAW_GRUMBLE_1, 1.5f, 0.95f, 0.1f, false, false, false)
                    .add("grumble2", "action", "animation.nulljaw.grumble2",
                            ModSounds.NULLJAW_GRUMBLE_2, 1.5f, 0.95f, 0.1f, false, false, false)
                    .add("grumble3", "action", "animation.nulljaw.grumble3",
                            ModSounds.NULLJAW_GRUMBLE_3, 1.5f, 0.95f, 0.1f, false, false, false)
                    .add("roar", "action", "animation.nulljaw.roar",
                            ModSounds.NULLJAW_ROAR, 2.5f, 0.9f, 0.1f, false, false, false)
                    .add("roar2", "action", "animation.nulljaw.roar2",
                            ModSounds.NULLJAW_ROAR2, 2.5f, 0.8f, 0.1f, false, false, false)
                    .add("phase1", "action", "animation.nulljaw.phase1",
                            ModSounds.NULLJAW_PHASE1, 1.4f, 0.9f, 0.2f, false, false, false)
                    .add("phase2", "action", "animation.nulljaw.phase2",
                            ModSounds.NULLJAW_PHASE2, 2.5f, 0.9f, 0.2f, false, false, false)
                    .add("phase2_start", "action", "animation.nulljaw.phase2_start",
                            ModSounds.NULLJAW_PHASE2_START, 2.0f, 0.9f, 0.1f, false, false, false)
                    .add("phase2_end", "action", "animation.nulljaw.phase2_end",
                            ModSounds.NULLJAW_PHASE2_END, 1.8f, 0.95f, 0.15f, false, false, false)
                    .add("hurt", "action", "animation.nulljaw.hurt",
                            ModSounds.NULLJAW_HURT, 1.2f, 0.95f, 0.05f, false, true, true)
                    .add("die", "action", "animation.nulljaw.die",
                            ModSounds.NULLJAW_DIE, 1.4f, 0.9f, 0.05f, false, true, true)
                    .build();

    private NulljawSoundProfile() {}

    @Override
    public boolean handleAnimationSound(DragonSoundHandler handler, DragonEntity dragon, String key, String locator) {
        // Handler already blocks server-side, we're only called on client
        if (dragon.isBaby() && !BABY_ALLOWED_KEYS.containsKey(key)) {
            return true;
        }

        // Check if this is a vocal key that should use the vocal entry system
        String vocalKey = EFFECT_TO_VOCAL_KEY.get(key);
        if (vocalKey != null) {
            playVocalEntry(handler, dragon, vocalKey, locator);
            return true;
        }

        // Handle non-vocal animation sounds
        return switch (key) {
            case "nulljaw_walk" -> {
                playSimpleSoundStereo(handler, dragon, locator,
                        ModSounds.NULLJAW_WALK.get(), ModSounds.NULLJAW_WALK_STEREO.get(), 0.8f, 0.9f, 0.2f);
                yield true;
            }
            case "nulljaw_run" -> {
                playSimpleSoundStereo(handler, dragon, locator,
                        ModSounds.NULLJAW_RUN.get(), ModSounds.NULLJAW_RUN_STEREO.get(), 0.8f, 0.9f, 0.2f);
                yield true;
            }
            case "nulljaw_walk2" -> {
                playSimpleSoundStereo(handler, dragon, locator,
                        ModSounds.NULLJAW_WALK2.get(), ModSounds.NULLJAW_WALK2_STEREO.get(), 0.8f, 0.9f, 0.2f);
                yield true;
            }
            case "nulljaw_run2" -> {
                playSimpleSoundStereo(handler, dragon, locator,
                        ModSounds.NULLJAW_RUN2.get(), ModSounds.NULLJAW_RUN2_STEREO.get(), 0.8f, 0.9f, 0.2f);
                yield true;
            }
            case "nulljaw_claw" -> {
                playSimpleSound(handler, dragon, locator, ModSounds.NULLJAW_CLAW.get(), 1.2f, 0.9f, 0.2f);
                yield true;
            }
            case "nulljaw_tail_swipe" -> {
                playSimpleSoundStereo(handler, dragon, locator,
                        ModSounds.NULLJAW_TAIL_SWIPE.get(), ModSounds.NULLJAW_TAIL_SWIPE_STEREO.get(), 1.2f, 0.9f, 0.2f);
                yield true;
            }
            case "nulljaw_tail_attack" -> {
                playSimpleSound(handler, dragon, locator,
                        ModSounds.NULLJAW_TAIL_ATTACK.get(), 1.2f, 0.9f, 0.2f);
                yield true;
            }
            case "nulljaw_phase2_dash" -> {
                playSimpleSoundStereo(handler, dragon, locator,
                        ModSounds.NULLJAW_PHASE2_DASH.get(), ModSounds.NULLJAW_PHASE2_DASH_STEREO.get(), 1.2f, 0.9f, 0.2f);
                yield true;
            }
            case "nulljaw_bite" -> {
                playSimpleSoundStereo(handler, dragon, locator,
                        ModSounds.NULLJAW_BITE.get(), ModSounds.NULLJAW_BITE_STEREO.get(), 1.1f, 0.95f, 0.1f);
                yield true;
            }
            case "nulljaw_horngore" -> {
                playSimpleSoundStereo(handler, dragon, locator,
                        ModSounds.NULLJAW_HORNGORE.get(), ModSounds.NULLJAW_HORNGORE_STEREO.get(), 1.2f, 0.9f, 0.2f);
                yield true;
            }
            case "nulljaw_eat" -> {
                playEatSound(handler, dragon, locator);
                yield true;
            }
            default -> false;
        };
    }

    @Override
    public DragonEntity.VocalEntry getFallbackVocalEntry(String key) {
        return FALLBACK_VOCALS.get(key);
    }

    @Override
    public int getVocalAnimationWindowTicks(String key) {
        return VOCAL_WINDOWS.getOrDefault(key, -1);
    }

    /**
     * Play vocal entry with proper positioning and pitch variation.
     * Follows Raevyx approach with sleep/sitting state checks.
     */
    private void playVocalEntry(DragonSoundHandler handler, DragonEntity dragon, String vocalKey, String locator) {
        DragonEntity.VocalEntry entry = dragon.getVocalEntries().get(vocalKey);
        if (entry == null) {
            entry = FALLBACK_VOCALS.get(vocalKey);
        }
        if (entry == null) {
            return;
        }

        // Check if allowed during sleep/sitting
        if (!entry.allowDuringSleep() && (dragon.isSleeping() || dragon.isSleepTransitioning())) {
            return;
        }
        if (!entry.allowWhenSitting() && dragon.isStayOrSitMuted()) {
            return;
        }

        // Resolve position (use mouth_origin for vocals, or entity position as fallback)
        Vec3 at = handler.resolveLocatorWorldPos(locator != null && !locator.isEmpty() ? locator : "mouth_origin");

        // Calculate pitch with variance
        float pitch = entry.basePitch();
        if (entry.pitchVariance() != 0f) {
            pitch += dragon.getRandom().nextFloat() * entry.pitchVariance();
        }
        if (dragon.isBaby()) {
            pitch *= BABY_PITCH_MULTIPLIER;
        }

        if ("roar2".equals(vocalKey)) {
            playDualSound(dragon, at, ModSounds.NULLJAW_ROAR2.get(), ModSounds.NULLJAW_ROAR2_STEREO.get(), entry.volume(), pitch);
        } else {
            playClientSound(dragon, at, entry.soundSupplier().get(), entry.volume(), pitch);
        }
    }

    /**
     * Play eat sound with baby pitch adjustment.
     */
    private void playEatSound(DragonSoundHandler handler, DragonEntity dragon, String locator) {
        Vec3 at = handler.resolveLocatorWorldPos(
                locator != null && !locator.isEmpty() ? locator : "mouth_origin"
        );
        float basePitch = 0.8f;
        float pitch = basePitch;
        if (dragon.getRandom().nextFloat() < 0.5f) {
            pitch += dragon.getRandom().nextFloat() * 0.1f;
        }
        if (dragon.isBaby()) {
            pitch *= BABY_PITCH_MULTIPLIER;
        }
        playClientSound(dragon, at, ModSounds.NULLJAW_EAT.get(), 2.5f, pitch);
    }

    /**
     * Play simple sound with locator support and pitch variance.
     */
    private void playSimpleSound(DragonSoundHandler handler, DragonEntity dragon, String locator,
                                  net.minecraft.sounds.SoundEvent sound, float volume, float basePitch, float variance) {
        Vec3 at = handler.resolveLocatorWorldPos(locator != null && !locator.isEmpty() ? locator : "mouth_origin");
        float pitch = basePitch;
        if (variance != 0f) {
            pitch += dragon.getRandom().nextFloat() * variance;
        }

        playClientSound(dragon, at, sound, volume, pitch);
    }

    private void playSimpleSoundStereo(DragonSoundHandler handler, DragonEntity dragon, String locator,
                                       net.minecraft.sounds.SoundEvent monoSound, net.minecraft.sounds.SoundEvent stereoSound,
                                       float volume, float basePitch, float variance) {
        Vec3 at = handler.resolveLocatorWorldPos(locator != null && !locator.isEmpty() ? locator : "mouth_origin");
        float pitch = basePitch;
        if (variance != 0f) {
            pitch += dragon.getRandom().nextFloat() * variance;
        }

        playDualSound(dragon, at, monoSound, stereoSound, volume, pitch);
    }

    /**
     * Play sound on client side using local playback.
     * More efficient than server broadcast for animation keyframe sounds.
     */
    private void playClientSound(DragonEntity dragon, Vec3 position, net.minecraft.sounds.SoundEvent sound,
                                 float volume, float pitch) {
        double x = position != null ? position.x : dragon.getX();
        double y = position != null ? position.y : dragon.getY();
        double z = position != null ? position.z : dragon.getZ();

        dragon.level().playLocalSound(x, y, z, sound, SoundSource.NEUTRAL, volume, pitch, false);
    }

    /**
     * Play dual sound: stereo only for the rider, mono for everyone else.
     */
    private void playDualSound(DragonEntity dragon, Vec3 position, net.minecraft.sounds.SoundEvent monoSound,
                               net.minecraft.sounds.SoundEvent stereoSound, float volume, float pitch) {
        double x = position != null ? position.x : dragon.getX();
        double y = position != null ? position.y : dragon.getY();
        double z = position != null ? position.z : dragon.getZ();

        if (dragon.level().isClientSide) {
            net.minecraft.client.Minecraft mc = net.minecraft.client.Minecraft.getInstance();
            if (mc.player != null) {
                boolean isRiding = mc.player.getVehicle() == dragon;
                if (isRiding) {
                    dragon.level().playLocalSound(x, y, z, stereoSound, SoundSource.NEUTRAL, volume, pitch, false);
                } else {
                    dragon.level().playLocalSound(x, y, z, monoSound, SoundSource.NEUTRAL, volume, pitch, false);
                }
            }
        }
    }
}

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

    private static final Map<String, Integer> VOCAL_WINDOWS = Map.ofEntries(
            Map.entry("roar", 64),
            Map.entry("grumble1", 80),
            Map.entry("grumble2", 85),
            Map.entry("grumble3", 90)
    );

    private static final Map<String, String> EFFECT_TO_VOCAL_KEY = Map.ofEntries(
            Map.entry("ignivorus_roar", "roar"),
            Map.entry("ignivorus_grumble1", "grumble1"),
            Map.entry("ignivorus_grumble2", "grumble2"),
            Map.entry("ignivorus_grumble3", "grumble3")
    );

    private static final Map<String, DragonEntity.VocalEntry> FALLBACK_VOCALS =
            new DragonEntity.VocalEntryBuilder()
                    .add("roar", "action", "animation.ignivorus.roar",
                            ModSounds.IGNIVORUS_ROAR, 1.8f, 0.85f, 0.15f, false, false, false)
                    .add("grumble1", "action", "animation.ignivorus.grumble1",
                            ModSounds.IGNIVORUS_GRUMBLE_1, 1.1f, 0.95f, 0.05f, false, false, false)
                    .add("grumble2", "action", "animation.ignivorus.grumble2",
                            ModSounds.IGNIVORUS_GRUMBLE_2, 1.15f, 1.0f, 0.05f, false, false, false)
                    .add("grumble3", "action", "animation.ignivorus.grumble3",
                            ModSounds.IGNIVORUS_GRUMBLE_3, 1.2f, 0.9f, 0.05f, false, false, false)
                    .build();

    private IgnivorusSoundProfile() {}

    @Override
    public boolean handleAnimationSound(DragonSoundHandler handler, DragonEntity dragon, String key, String locator) {
        // Check if this is a vocal key that should use the vocal entry system
        String vocalKey = EFFECT_TO_VOCAL_KEY.get(key);
        if (vocalKey != null) {
            // Roar sound is handled by ability with precise timing
            if ("roar".equals(vocalKey)) {
                return true;
            }
            playVocalEntry(handler, dragon, vocalKey, locator);
            return true;
        }

        // Handle non-vocal animation sounds
        return switch (key) {
            case "ignivorus_bite" -> {
                playSimpleSound(handler, dragon, "mouth_origin", ModSounds.IGNIVORUS_BITE.get(), 1.2f, 0.95f, 0.1f);
                yield true;
            }
            case "ignivorus_wing_swipe" -> {
                playSimpleSound(handler, dragon, "body_locator", ModSounds.IGNIVORUS_WING_SWIPE.get(), 1.2f, 0.95f, 0.1f);
                yield true;
            }
            case "ignivorus_eat" -> {
                playSimpleSound(handler, dragon, "mouth_origin", ModSounds.IGNIVORUS_EAT.get(), 1.2f, 0.95f, 0.1f);
                yield true;
            }
            case "ignivorus_body_slam" -> {
                playSimpleSound(handler, dragon, "body_locator", ModSounds.IGNIVORUS_BODY_SLAM.get(), 2.0f, 0.9f, 0.15f);
                yield true;
            }
            case "ignivorus_stomp" -> {
                playSimpleSound(handler, dragon, "body_locator", ModSounds.IGNIVORUS_STOMP.get(), 1.5f, 0.9f, 0.15f);
                yield true;
            }
            case "ignivorus_walk" -> {
                playMovementSound(handler, dragon, "body_locator", ModSounds.IGNIVORUS_WALK.get(), 1.0f, 0.85f);
                yield true;
            }
            case "ignivorus_phase2_walk" -> {
                playMovementSound(handler, dragon, "body_locator", ModSounds.IGNIVORUS_PHASE2_WALK.get(), 1.0f, 0.85f);
                yield true;
            }
            case "ignivorus_phase2_enter" -> {
                playMovementSound(handler, dragon, "body_locator", ModSounds.IGNIVORUS_PHASE2_ENTER.get(), 1.0f, 0.85f);
                yield true;
            }
            case "ignivorus_phase2_exit" -> {
                playMovementSound(handler, dragon, "body_locator", ModSounds.IGNIVORUS_PHASE2_EXIT.get(), 1.0f, 0.85f);
                yield true;
            }
            case "ignivorus_phase2_run" -> {
                playMovementSound(handler, dragon, "body_locator", ModSounds.IGNIVORUS_PHASE2_RUN.get(), 1.0f, 0.85f);
                yield true;
            }
            case "ignivorus_run" -> {
                playMovementSound(handler, dragon, "body_locator", ModSounds.IGNIVORUS_RUN.get(), 1.1f, 0.9f);
                yield true;
            }
            case "ignivorus_ultimate_start" -> {
                playSimpleSound(handler, dragon, "mouth_origin", ModSounds.IGNIVORUS_ULTIMATE_START.get(), 2.0f, 1.0f, 0.0f);
                yield true;
            }
            case "ignivorus_ultimate" -> {
                playSimpleSound(handler, dragon, "mouth_origin", ModSounds.IGNIVORUS_ULTIMATE.get(), 2.5f, 1.0f, 0.0f);
                yield true;
            }
            case "ignivorus_ultimate_end" -> {
                playSimpleSound(handler, dragon, "mouth_origin", ModSounds.IGNIVORUS_ULTIMATE_END.get(), 2.0f, 1.0f, 0.0f);
                yield true;
            }
            case "ignivorus_ultimate_start_air" -> {
                playSimpleSound(handler, dragon, "mouth_origin", ModSounds.IGNIVORUS_ULTIMATE_START_AIR.get(), 2.0f, 1.0f, 0.0f);
                yield true;
            }
            case "ignivorus_ultimate_air" -> {
                playSimpleSound(handler, dragon, "mouth_origin", ModSounds.IGNIVORUS_ULTIMATE_AIR.get(), 2.5f, 1.0f, 0.0f);
                yield true;
            }
            case "ignivorus_ultimate_end_air" -> {
                playSimpleSound(handler, dragon, "mouth_origin", ModSounds.IGNIVORUS_ULTIMATE_END_AIR.get(), 2.0f, 1.0f, 0.0f);
                yield true;
            }
            case "ignivorus_fire_breath_start", "ignivorus_fire_breathing", "ignivorus_fire_breath_end" -> {
                // Block keyframe sounds - fire breath is handled by IgnivorusFireBreathAbility/Controller
                yield true;
            }
            case "ignivorus_takeoff" -> {
                // Block keyframe sound - takeoff is handled by Ignivorus.setTakeoff()
                yield true;
            }
            case "ignivorus_landed" -> {
                playSimpleSound(handler, dragon, "body_locator", ModSounds.IGNIVORUS_LANDED.get(), 1.5f, 1.0f, 0.0f);
                yield true;
            }
            case "ignivorus_phase2_landed" -> {
                playSimpleSound(handler, dragon, "body_locator", ModSounds.IGNIVORUS_PHASE2_LANDED.get(), 1.5f, 1.0f, 0.0f);
                yield true;
            }
            case "ignivorus_bulldozer_enter" -> {
                playSimpleSound(handler, dragon, "mouth_origin", ModSounds.IGNIVORUS_BULLDOZER_ENTER.get(), 1.5f, 1.0f, 0.0f);
                yield true;
            }
            case "ignivorus_bulldozing" -> {
                playSimpleSound(handler, dragon, "body_locator", ModSounds.IGNIVORUS_BULLDOZING.get(), 1.5f, 1.0f, 0.0f);
                yield true;
            }
            case "ignivorus_bulldozer_exit" -> {
                playSimpleSound(handler, dragon, "mouth_origin", ModSounds.IGNIVORUS_BULLDOZER_EXIT.get(), 1.5f, 1.0f, 0.0f);
                yield true;
            }
            case "ignivorus_leap" -> {
                playSimpleSound(handler, dragon, "body_locator", ModSounds.IGNIVORUS_LEAP.get(), 2.5f, 1.0f, 0.0f);
                yield true;
            }
            case "ignivorus_impact" -> {
                playSimpleSound(handler, dragon, "body_locator", ModSounds.IGNIVORUS_IMPACT.get(), 2.5f, 1.0f, 0.0f);
                yield true;
            }
            case "ignivorus_level1_charge" -> {
                playSimpleSound(handler, dragon, "body_locator", ModSounds.IGNIVORUS_LEVEL1_CHARGE.get(), 2.5f, 1.0f, 0.0f);
                yield true;
            }
            case "ignivorus_level1_shoots" -> {
                playSimpleSound(handler, dragon, "body_locator", ModSounds.IGNIVORUS_LEVEL1_SHOOTS.get(), 2.5f, 1.0f, 0.0f);
                yield true;
            }
            case "ignivorus_level2_charge" -> {
                playSimpleSound(handler, dragon, "body_locator", ModSounds.IGNIVORUS_LEVEL2_CHARGE.get(), 2.5f, 1.0f, 0.0f);
                yield true;
            }
            case "ignivorus_level2_shoots" -> {
                playSimpleSound(handler, dragon, "body_locator", ModSounds.IGNIVORUS_LEVEL2_SHOOTS.get(), 2.5f, 1.0f, 0.0f);
                yield true;
            }
            case "ignivorus_level3_charge" -> {
                playSimpleSound(handler, dragon, "body_locator", ModSounds.IGNIVORUS_LEVEL3_CHARGE.get(), 2.5f, 1.0f, 0.0f);
                yield true;
            }
            case "ignivorus_level3_shoots" -> {
                playSimpleSound(handler, dragon, "body_locator", ModSounds.IGNIVORUS_LEVEL3_SHOOTS.get(), 2.5f, 1.0f, 0.0f);
                yield true;
            }
            default -> false;
        };
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

        playClientSound(dragon, at, entry.soundSupplier().get(), entry.volume(), pitch);
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

    /**
     * Play movement sound with cooldown to prevent rapid-fire step sounds.
     */
    private void playMovementSound(DragonSoundHandler handler, DragonEntity dragon, String locator,
                                   SoundEvent sound, float volume, float basePitch) {
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
        float vol = volume * 1.2f;
        float pitch = basePitch + (dragon.getRandom().nextFloat() - 0.5f) * 0.05f;

        playClientSound(dragon, body, sound, vol, pitch);
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
}

package com.leon.saintsdragons.server.entity.handler;

import com.leon.saintsdragons.server.entity.base.DragonEntity;
import com.leon.saintsdragons.common.registry.ModSounds;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

/**
 * Handles all sound effects for dragons
 * Separates sound logic from entity class for cleaner organization
 */
public class DragonSoundHandler {
    private final DragonEntity dragon;
    // Step timing control for walk animation to match Blockbench keyframes precisely
    private static final int WALK_STEP_SEPARATION_TICKS = 12; // 0.6s @ 20 TPS
    private static final int RUN_STEP_SEPARATION_TICKS  = 10; // ~0.476s @ 20 TPS (0.5 of 0.9524s)
    private long lastStep1Tick = Long.MIN_VALUE;
    private long lastStep2Tick = Long.MIN_VALUE;

    private static class PendingStep {
        String key;      // "step1"/"step2" or "run_step1"/"run_step2"
        String locator;
        int ticksLeft;
        PendingStep(String key, String locator, int ticksLeft) {
            this.key = key; this.locator = locator; this.ticksLeft = ticksLeft;
        }
    }
    private PendingStep pendingStep1 = null;
    private PendingStep pendingStep2 = null;
    
    public DragonSoundHandler(DragonEntity dragon) {
        this.dragon = dragon;
    }

    /** Call every entity tick to process any pending delayed footsteps */
    public void tick() {
        if (dragon.isDying()) { pendingStep1 = null; pendingStep2 = null; return; }
        if (pendingStep1 != null) {
            if (--pendingStep1.ticksLeft <= 0) {
                actuallyPlayStep(pendingStep1.key, pendingStep1.locator);
                pendingStep1 = null;
            }
        }
        if (pendingStep2 != null) {
            if (--pendingStep2.ticksLeft <= 0) {
                actuallyPlayStep(pendingStep2.key, pendingStep2.locator);
                pendingStep2 = null;
            }
        }
    }
    
    /**
     * Handle keyframe-based sound effects during animations
     * Call this from animation controller sound handlers (legacy support)
     */
    public void handleAnimationSound(DragonEntity entity, Object keyframeData, software.bernie.geckolib.core.animation.AnimationController<?> controller) {
        if (dragon.isDying()) return;
        if (keyframeData == null) return;
        String controllerName = null;
        try {
            if (controller != null && controller.getName() != null) {
                controllerName = controller.getName();
            }
        } catch (Throwable ignored) {}
        // Gate non-action sounds during sit/sleep; allow action controller during sleep transitions so enter/exit can play
        if (dragon.isStayOrSitMuted()) return;
        if (dragon.isSleeping()) return;
        if (dragon.isSleepTransitioning() && (!"action".equals(controllerName))) return;
        String raw = null;
        try {
            // Use reflection to call getSound() method on the keyframe data
            raw = (String) keyframeData.getClass().getMethod("getSound").invoke(keyframeData);
        } catch (Exception e) {
            return; // If we can't get the sound data, skip
        }
        if (raw == null || raw.isEmpty()) return;
        String sound = raw.toLowerCase(java.util.Locale.ROOT);

        // Auto format: namespace:soundid or namespace:soundid|vol|pitch
        if (sound.contains(":")) {
            handleAutoSoundSpec(sound);
            return;
        }
        // Allow flexible keys from animation JSON: flap1, flap_right, step2, step_left, run_step1, etc.
        if (sound.startsWith("flap")) { handleWingFlapSound(sound); return; }
        if (sound.startsWith("step") || sound.startsWith("run_step")) {
            // Only handle footsteps from the movement controller (runs walk/run)
            try {
                if (controller != null && controller.getName() != null) {
                    String ctrl = controller.getName();
                    if (!"movement".equals(ctrl)) return; // ignore non-movement controllers for steps
                }
            } catch (Throwable ignored) {}
            // Use locator position if provided by the keyframe
            String locator = mapStepKeyToLocator(sound);
            handleStepSound(sound, locator);
            return;
        }
        switch (sound) {
            case "wing_flap" -> handleWingFlapSound(sound);
            case "dragon_step" -> handleStepSound(sound, null);
            case "takeoff_whoosh" -> handleTakeoffSound();
            case "landing_thud" -> handleLandingSound();
            default -> handleSoundByName(sound);
        }
    }

    /**
     * Handle sound by name - for direct sound triggering
     */
    public void handleSoundByName(String soundName) {
        if (dragon.isDying()) return;
        if (dragon.isStayOrSitMuted() || dragon.isSleeping() || dragon.isSleepTransitioning()) return; // Suppress during sit/sleep/transition
        if (soundName == null || soundName.isEmpty()) return;
        String key = soundName.toLowerCase(java.util.Locale.ROOT);
        if (key.startsWith("flap")) { handleWingFlapSound(key); return; }
        if (key.startsWith("step") || key.startsWith("run_step")) { handleStepSound(key, null); return; }
        switch (key) {
            case "wing_flap" -> handleWingFlapSound(key);
            case "dragon_step" -> handleStepSound(key, null);
            case "takeoff_whoosh" -> handleTakeoffSound();
            case "landing_thud" -> handleLandingSound();
            default -> {}
        }
    }

    /**
     * Plays a vocal sound and triggers a matching action animation.
     * Expected keys (examples): grumble1, grumble2, grumble3, purr, snort, chuff, content, annoyed,
     * growl_warning, roar, hurt
     */
    public void playVocal(String key) {
        if (key == null || key.isEmpty() || dragon.level().isClientSide) return;
        // Allow hurt and die sounds to play regardless of sleep state
        if (!"hurt".equals(key) && !"amphithere_hurt".equals(key) && !"die".equals(key) && (dragon.isSleeping() || dragon.isSleepTransitioning())) {
            return; // Gate vocals during sleep/transition
        }
        
        // Get mouth position for spatial audio - both entities use mouth_origin locator
        Vec3 mouthPos = resolveLocatorWorldPos("mouth_origin");
        
        switch (key) {
            case "grumble1" -> playRouted(dragon.level(), ModSounds.DRAGON_GRUMBLE_1.get(), 0.8f, 0.95f + dragon.getRandom().nextFloat() * 0.1f, mouthPos, false);
            case "grumble2" -> playRouted(dragon.level(), ModSounds.DRAGON_GRUMBLE_2.get(), 0.8f, 0.95f + dragon.getRandom().nextFloat() * 0.1f, mouthPos, false);
            case "grumble3" -> playRouted(dragon.level(), ModSounds.DRAGON_GRUMBLE_3.get(), 0.8f, 0.95f + dragon.getRandom().nextFloat() * 0.1f, mouthPos, false);
            case "primitivedrake_grumble1" -> playRouted(dragon.level(), ModSounds.PRIMITIVE_DRAKE_GRUMBLE_1.get(), 0.6f, 1.1f + dragon.getRandom().nextFloat() * 0.2f, mouthPos, false);
            case "primitivedrake_grumble2" -> playRouted(dragon.level(), ModSounds.PRIMITIVE_DRAKE_GRUMBLE_2.get(), 0.6f, 1.1f + dragon.getRandom().nextFloat() * 0.2f, mouthPos, false);
            case "primitivedrake_grumble3" -> playRouted(dragon.level(), ModSounds.PRIMITIVE_DRAKE_GRUMBLE_3.get(), 0.6f, 1.1f + dragon.getRandom().nextFloat() * 0.2f, mouthPos, false);
            case "purr"      -> playRouted(dragon.level(), ModSounds.DRAGON_PURR.get(),      0.8f, 1.05f + dragon.getRandom().nextFloat() * 0.05f, mouthPos, true);  // Allow when sitting
            case "snort"     -> playRouted(dragon.level(), ModSounds.DRAGON_SNORT.get(),     0.9f, 0.9f + dragon.getRandom().nextFloat() * 0.2f, mouthPos, false);
            case "chuff"     -> playRouted(dragon.level(), ModSounds.DRAGON_CHUFF.get(),     0.9f, 0.9f + dragon.getRandom().nextFloat() * 0.2f, mouthPos, false);
            case "content"   -> playRouted(dragon.level(), ModSounds.DRAGON_CONTENT.get(),   0.8f, 1.0f + dragon.getRandom().nextFloat() * 0.1f, mouthPos, true);  // Allow when sitting
            case "annoyed"   -> playRouted(dragon.level(), ModSounds.DRAGON_ANNOYED.get(),   1.0f, 0.9f + dragon.getRandom().nextFloat() * 0.2f, mouthPos, false);
            case "growl_warning" -> playRouted(dragon.level(), ModSounds.DRAGON_GROWL_WARNING.get(), 1.2f, 0.8f + dragon.getRandom().nextFloat() * 0.4f, mouthPos, false);
            case "roar"      -> playRouted(dragon.level(), ModSounds.DRAGON_ROAR.get(),      1.4f, 0.9f + dragon.getRandom().nextFloat() * 0.15f, mouthPos, false);
            case "amphithere_roar" -> playRouted(dragon.level(), ModSounds.AMPHITHERE_ROAR.get(), 1.5f, 0.95f + dragon.getRandom().nextFloat() * 0.1f, mouthPos, false);
            case "amphithere_hurt" -> playRouted(dragon.level(), ModSounds.AMPHITHERE_HURT.get(), 1.2f, 0.95f + dragon.getRandom().nextFloat() * 0.1f, mouthPos, false);
            case "hurt"      -> playRouted(dragon.level(), ModSounds.DRAGON_HURT.get(),      1.2f, 0.95f + dragon.getRandom().nextFloat() * 0.1f, mouthPos, false);
            case "die"       -> playRouted(dragon.level(), ModSounds.DRAGON_DIE.get(),       1.5f, 0.95f + dragon.getRandom().nextFloat() * 0.1f, mouthPos, false);
            default -> {
                // Unknown key - do nothing
            }
        }

        // Also trigger a matching vocal animation clip on the action controller.
        // Keep the action window open long enough for the clip to finish.
        int window = getVocalAnimationWindowTicks(key);
        if (!dragon.isSleeping() && !dragon.isSleepTransitioning() && window > 0 && !dragon.level().isClientSide) {
            // Trigger matching action animation clip using native key
            dragon.triggerAnim("action", key);
        }
    }

    /**
     * Returns an appropriate action-controller window length (in ticks) for a vocal animation key.
     * Values mirror the animation lengths defined in lightning_dragon.animation.json (rounded up).
     */
    private static int getVocalAnimationWindowTicks(String key) {
        if (key == null) return 0;
        // Map known keys to their approximate durations (seconds * 20)
        // grumble1: 5.9167s, grumble2: 8.6667s, grumble3: 2.625s
        // content: 4.7083s, purr: 5.2083s, snort: 0.875s, chuff: 1.2083s
        return switch (key) {
            case "grumble1" -> 120;  // ~5.9s
            case "grumble2" -> 180;  // ~8.7s
            case "grumble3" -> 60;   // ~2.6s
            
            // Primitive Drake grumbles - shorter, simpler animations
            case "primitivedrake_grumble1" -> 40;  // ~2s - short cute grumble
            case "primitivedrake_grumble2" -> 60;  // ~3s - medium grumble
            case "primitivedrake_grumble3" -> 30;  // ~1.5s - quick grumble
            case "content"  -> 100;  // ~4.7s
            case "purr"     -> 110;  // ~5.2s
            case "snort"    -> 24;   // ~0.9s
            case "chuff"    -> 28;   // ~1.2s
            case "roar"             -> 69;   // ~3.45s (animation ~3.4167s)
            case "amphithere_roar" -> 45;   // ~2.25s (animation length 2.25s)
            case "hurt"     -> 20;   // ~1.0s one-shot
            case "die"      -> 62;   // ~3.071s one-shot
            // Fallback window for other simple one-shots
            default -> 40;            // ~2s
        };
    }
    
    /**
     * Wing flap sound with dynamic speed variation
     */
    private void handleWingFlapSound(String key) {
        if (dragon.isStayOrSitMuted()) return;
        double flightSpeed = dragon.getCachedHorizontalSpeed();
        float pitch = 1.0f + (float)(flightSpeed * 0.3f); // Higher pitch when flying faster
        float volume = Math.max(0.6f, 0.9f + (float)(flightSpeed * 0.2f));

        // Use custom flap sound (matches Blockbench keyframe label like "flap1")
        playRouted(dragon.level(), ModSounds.FLAP1.get(), volume, pitch);
    }
    
    /**
     * Dragon step sound with weight variation
     */
    private void handleStepSound(String key, String locator) {
        if (dragon.isStayOrSitMuted()) return;
        // Choose step variant based on keyframe name (e.g., "step1" vs "step2")
        // Respect Blockbench spacing for walk and run
        boolean running = dragon.isActuallyRunning() && !dragon.isFlying();
        boolean walking = !running && dragon.isWalking() && !dragon.isFlying();
        long now = dragon.tickCount;
        if (key != null && key.endsWith("2")) {
            // step2: left foot
            if (walking || running) {
                int desired = 0; // step2 is at 0.0 in both cases within the clip window
                int delay = requiredDelayTicks(now - lastStep1Tick, desired);
                String k2 = key.startsWith("run_step") ? "run_step2" : "step2";
                if (delay > 0) {
                    pendingStep2 = new PendingStep(k2, locator, delay);
                } else {
                    actuallyPlayStep(k2, locator);
                }
            } else {
                String k2 = key.startsWith("run_step") ? "run_step2" : "step2";
                actuallyPlayStep(k2, locator);
            }
        } else {
            // step1: right foot
            if (walking || running) {
                int desired = walking ? WALK_STEP_SEPARATION_TICKS : RUN_STEP_SEPARATION_TICKS;
                int delay = requiredDelayTicks(now - lastStep2Tick, desired);
                assert key != null;
                String k1 = key.startsWith("run_step") ? "run_step1" : "step1";
                if (delay > 0) {
                    pendingStep1 = new PendingStep(k1, locator, delay);
                } else {
                    actuallyPlayStep(k1, locator);
                }
            } else {
                assert key != null;
                String k1 = key.startsWith("run_step") ? "run_step1" : "step1";
                actuallyPlayStep(k1, locator);
            }
        }
    }

    private int requiredDelayTicks(long deltaTicks, int desired) {
        if (deltaTicks == Long.MIN_VALUE) return 0; // first time
        if (deltaTicks < desired) return desired - (int)deltaTicks;
        return 0;
    }

    private void actuallyPlayStep(String which, String locator) {
        // Heavier steps when running or carrying rider
        float weight = 1.0f;
        if (dragon.isRunning()) weight *= 1.2f;
        if (dragon.isVehicle()) weight *= 1.1f;
        if (dragon.getHealth() < dragon.getMaxHealth() * 0.5f) weight *= 0.9f;

        float volume = 0.65f * weight;
        float pitch = (0.9f + dragon.getRandom().nextFloat() * 0.2f) / weight;
        Vec3 at = resolveLocatorWorldPos(locator);
        boolean isRun = which != null && which.startsWith("run_step");
        boolean isSecond = which != null && which.endsWith("2");

        if (isRun) {
            if (isSecond) {
                playRouted(dragon.level(), ModSounds.RUN_STEP2.get(), volume, pitch, at);
                lastStep2Tick = dragon.tickCount;
            } else {
                playRouted(dragon.level(), ModSounds.RUN_STEP1.get(), volume, pitch, at);
                lastStep1Tick = dragon.tickCount;
            }
        } else {
            if (isSecond) {
                playRouted(dragon.level(), ModSounds.STEP2.get(), volume, pitch, at);
                lastStep2Tick = dragon.tickCount;
            } else {
                playRouted(dragon.level(), ModSounds.STEP1.get(), volume, pitch, at);
                lastStep1Tick = dragon.tickCount;
            }
        }
    }

    /**
     * Parses and plays sounds specified as namespace:soundid or namespace:soundid|vol|pitch
     */
    private void handleAutoSoundSpec(String spec) {
        if (dragon.isStayOrSitMuted() || dragon.isSleeping()) return;
        if (spec == null) return;
        String[] parts = spec.split("\\|");
        String id = parts[0];
        float vol = 1.0f;
        float pitch = 1.0f;
        try {
            if (parts.length >= 2) vol = Float.parseFloat(parts[1]);
            if (parts.length >= 3) pitch = Float.parseFloat(parts[2]);
        } catch (Exception ignored) {}

        net.minecraft.resources.ResourceLocation rl = net.minecraft.resources.ResourceLocation.tryParse(id);
        if (rl == null) return;
        net.minecraft.sounds.SoundEvent evt = net.minecraftforge.registries.ForgeRegistries.SOUND_EVENTS.getValue(rl);
        if (evt == null) return;
        playRouted(dragon.level(), evt, vol, pitch);
    }

    /**
     * Plays sound properly on both client and server sides
     */
    private void playRouted(Level level, net.minecraft.sounds.SoundEvent sound, float volume, float pitch) {
        playRouted(level, sound, volume, pitch, null, false);
    }

    private void playRouted(Level level, net.minecraft.sounds.SoundEvent sound, float volume, float pitch, Vec3 at) {
        playRouted(level, sound, volume, pitch, at, false);
    }

    private void playRouted(Level level, net.minecraft.sounds.SoundEvent sound, float volume, float pitch, Vec3 at, boolean allowWhenSitting) {
        // Allow hurt and die sounds to play regardless of sleep/sit state
        boolean isHurtOrDieSound = sound == ModSounds.DRAGON_HURT.get() || sound == ModSounds.DRAGON_DIE.get();
        if (!isHurtOrDieSound && dragon.isSleeping()) return;
        if (!allowWhenSitting && !isHurtOrDieSound && dragon.isStayOrSitMuted()) return;
        if (level == null) return;
        double px = dragon.getX();
        double py = dragon.getY();
        double pz = dragon.getZ();
        if (at != null) {
            px = at.x;
            py = at.y;
            pz = at.z;
        }
        if (level.isClientSide) {
            // Client side: play local sound without distance delay
            level.playLocalSound(px, py, pz, sound, SoundSource.NEUTRAL, volume, pitch, false);
        } else {
            // Server side: broadcast to all nearby players
            level.playSound(null, px, py, pz, sound, SoundSource.NEUTRAL, volume, pitch);
        }
    }

    /**
     * In our walk cycle, step1 = right foot, step2 = left foot as authored in the animation JSON.
     * If the GeckoLib event does not expose the locator directly, infer it from the key.
     */
    private String mapStepKeyToLocator(String key) {
        if (key == null) return null;
        String k = key.toLowerCase(java.util.Locale.ROOT).trim();
        if (k.endsWith("1")) return "rightfeetLocator";
        if (k.endsWith("2")) return "leftfeetLocator";
        return null;
    }

    /**
     * Compute an approximate world position for a given locator name from the .geo.
     * This uses static locator offsets and rotates them by the dragon's body yaw.
     * If the locator is unknown or null, returns null to fall back to the entity position.
     */
    private Vec3 resolveLocatorWorldPos(String locator) {
        if (locator == null || locator.isEmpty()) return null;
        // Only compute for known locators. Values taken from lightning_dragon.geo.json
        // "leftfeetLocator":  [ 2.2, 0.05, 2.85]
        // "rightfeetLocator": [-2.2, 0.05, 2.85]
        // "mouth_origin":      [ 0.1, 8.7, -17.4]
        double lx, ly, lz;
        switch (locator) {
            case "leftfeetLocator" -> {
                lx = 2.2; ly = 0.05; lz = 2.85;
            }
            case "rightfeetLocator" -> {
                lx = -2.2; ly = 0.05; lz = 2.85;
            }
            case "mouth_origin" -> {
                Vec3 dynamic = dragon.getMouthPosition();
                if (dynamic != null) {
                    return dynamic;
                }
                // Lightning Dragon mouth position - fallback for both entities
                // Primitive Drake will use renderer-sampled position when available
                lx = 0.1; ly = 8.7; lz = -17.4;
            }
            default -> { return null; }
        }

        // Prefer renderer-sampled exact position if available on client
        Vec3 cached = dragon.getClientLocatorPosition(locator);
        if (cached != null) return cached;

        // Fallback: Convert model-space units (pixels) into world units using dragon's model scale
        double modelScale = dragon.getBbWidth() / 4.5f; // Default dragon width is 4.5f, adjust based on actual size
        double sx = (lx / 16.0) * modelScale;
        double sy = (ly / 16.0) * modelScale;
        double sz = (lz / 16.0) * modelScale;

        // Rotate around Y by the dragon's body yaw
        double yawDeg = dragon.yBodyRot;
        double cy = Math.cos(Math.toRadians(yawDeg));
        double syaw = Math.sin(Math.toRadians(yawDeg));
        double rx = sx * cy - sz * syaw;
        double rz = sx * syaw + sz * cy;

        // Offset from current dragon position
        return new Vec3(dragon.getX() + rx, dragon.getY() + sy, dragon.getZ() + rz);
    }
    
    /**
     * Takeoff sound with urgency variation
     */
    private void handleTakeoffSound() {
        if (dragon.isStayOrSitMuted() || dragon.isSleeping() || dragon.isSleepTransitioning()) return;
        float urgency = dragon.getTarget() != null ? 1.3f : 1.0f;
        // Use custom flap for takeoff to avoid vanilla ENDER_DRAGON_FLAP
        playRouted(dragon.level(), ModSounds.FLAP1.get(), urgency * 1.2f, 0.85f);
    }
    
    /**
     * Landing sound with impact variation
     */
    private void handleLandingSound() {
        if (dragon.isStayOrSitMuted() || dragon.isSleeping()) return;
        double impactSpeed = Math.abs(dragon.getDeltaMovement().y);
        float volume = (float) Math.max(0.8f, 1.0f + impactSpeed * 2.0f);
        float pitch = (float) Math.max(0.7f, 1.0f - impactSpeed * 0.3f);
        
        dragon.playSound(SoundEvents.GENERIC_EXPLODE, volume * 0.6f, pitch);
    }
}


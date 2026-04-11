package com.leon.saintsdragons.server.entity.npc.handlers;

import com.leon.saintsdragons.common.registry.ModSounds;
import com.leon.saintsdragons.server.entity.handler.HumanSoundHandler;
import com.leon.saintsdragons.server.entity.interfaces.HumanSoundProfile;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.phys.Vec3;

public class IvySoundProfile implements HumanSoundProfile {

    private static final int STEP_COOLDOWN = 5; // Prevent rapid-fire steps
    @Override
    public boolean handleSound(HumanSoundHandler handler, Mob entity, String soundKey,
                              String locator, float volume, float pitch) {

        // Handle footstep sounds
        if (soundKey.contains("step")) {
            return handleFootstep(handler, entity, soundKey, locator, volume, pitch);
        }

        // Handle ambient/interaction sounds
        return switch (soundKey) {
            case "greeting" -> playGreeting(handler, volume, pitch);
            case "ivy_trade_start" -> playTradeStart(handler, volume, pitch);
            case "ivy_trade_stop" -> playTradeStop(handler, volume, pitch);
            case "ivy_reaction_to_egg" -> playReactionToEgg(handler, volume, pitch);
            default -> false; // Unknown sound key
        };
    }

    @Override
    public Vec3 resolveLocator(HumanSoundHandler handler, Mob entity, String locator) {
        if (locator == null) {
            return entity.position();
        }

        // Simple locator offsets (since we don't have complex bone data like dragons)
        return switch (locator.toLowerCase()) {
            case "mouth", "head" -> entity.position().add(0, entity.getBbHeight() * 0.85, 0);
            case "feet", "foot", "ground" -> entity.position();
            default -> entity.position();
        };
    }

    // ===== SOUND HANDLERS =====

    private boolean handleFootstep(HumanSoundHandler handler, Mob entity, String soundKey,
                                   String locator, float volume, float pitch) {
        // Resolve foot position (if locator specified, otherwise entity position)
        Vec3 position = handler.resolveLocator(locator);

        // Vary pitch slightly for left/right feet
        float adjustedPitch = pitch;
        if (soundKey.contains("left")) {
            adjustedPitch += 0.05f;
        } else if (soundKey.contains("right")) {
            adjustedPitch -= 0.05f;
        }

        // Play vanilla footstep sound at foot position
        return handler.playSound(
            "step",
            SoundEvents.GRASS_STEP,
            position,
            volume * 0.6f, // Quieter than default
            adjustedPitch + entity.getRandom().nextFloat() * 0.1f,
            STEP_COOLDOWN
        );
    }



    private boolean playGreeting(HumanSoundHandler handler, float volume, float pitch) {
        return false;
    }

    private boolean playTradeStart(HumanSoundHandler handler, float volume, float pitch) {
        return playCustomSound(handler, "ivy_trade_start", ModSounds.IVY_TRADE_START.get(), volume, pitch, 20);
    }

    private boolean playTradeStop(HumanSoundHandler handler, float volume, float pitch) {
        return playCustomSound(handler, "ivy_trade_stop", ModSounds.IVY_TRADE_STOP.get(), volume, pitch, 20);
    }

    private boolean playReactionToEgg(HumanSoundHandler handler, float volume, float pitch) {
        return playCustomSound(handler, "ivy_reaction_to_egg", ModSounds.IVY_REACTION_TO_EGG.get(), volume, pitch, 20);
    }

    /**
     * Helper to play a custom sound (if Ivy gets custom voice lines in the future).
     * For now this is unused, but shows how to integrate ModSounds.
     */
    private boolean playCustomSound(HumanSoundHandler handler, String key, SoundEvent sound,
                                    float volume, float pitch, int cooldown) {
        return handler.playSound(key, sound, volume, pitch, cooldown);
    }
}

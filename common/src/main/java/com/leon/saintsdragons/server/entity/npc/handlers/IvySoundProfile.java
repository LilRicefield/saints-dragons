package com.leon.saintsdragons.server.entity.npc.handlers;

import com.leon.saintsdragons.common.registry.ModSounds;
import com.leon.saintsdragons.server.entity.handler.HumanSoundHandler;
import com.leon.saintsdragons.server.entity.interfaces.HumanSoundProfile;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.phys.Vec3;

public class IvySoundProfile implements HumanSoundProfile {

    @Override
    public boolean handleSound(HumanSoundHandler handler, Mob entity, String soundKey,
                              String locator, float volume, float pitch) {
        return switch (soundKey) {
            case "ivy_trade_start" -> playTradeStart(handler, volume, pitch);
            case "ivy_trade_stop" -> playTradeStop(handler, volume, pitch);
            case "ivy_reaction_to_egg" -> playReactionToEgg(handler, volume, pitch);
            default -> false;
        };
    }

    @Override
    public Vec3 resolveLocator(HumanSoundHandler handler, Mob entity, String locator) {
        if (locator == null) {
            return entity.position();
        }

        return switch (locator.toLowerCase()) {
            case "mouth", "head" -> entity.position().add(0, entity.getBbHeight() * 0.85, 0);
            default -> entity.position();
        };
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

    private boolean playCustomSound(HumanSoundHandler handler, String key, SoundEvent sound,
                                    float volume, float pitch, int cooldown) {
        return handler.playSound(key, sound, volume, pitch, cooldown);
    }
}
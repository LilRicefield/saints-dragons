package com.leon.saintsdragons.server.entity.npc.handlers;

import com.leon.saintsdragons.common.registry.ModSounds;
import com.leon.saintsdragons.server.entity.handler.HumanSoundHandler;
import com.leon.saintsdragons.server.entity.interfaces.HumanSoundProfile;
import net.minecraft.sounds.SoundEvent;
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
            case "ivy_eat" -> playCustomSound(handler, "ivy_eat", ModSounds.IVY_EAT.get(), volume, pitch, 8);
            case "ivy_drink" -> playCustomSound(handler, "ivy_drink", ModSounds.IVY_DRINK.get(), volume, pitch, 8);
            case "ivy_dodge" -> playCustomSound(handler, "ivy_dodge", ModSounds.IVY_DODGE.get(), volume, pitch, 5);
            case "ivy_left_jab" -> playCustomSound(handler, "ivy_left_jab", ModSounds.IVY_LEFT_JAB.get(), volume, pitch, 3);
            case "ivy_right_hook" -> playCustomSound(handler, "ivy_right_hook", ModSounds.IVY_RIGHT_HOOK.get(), volume, pitch, 4);
            case "ivy_jab_jab_hook" -> playCustomSound(handler, "ivy_jab_jab_hook", ModSounds.IVY_JAB_JAB_HOOK.get(), volume, pitch, 5);
            case "ivy_left_jab_right_cross" -> playCustomSound(handler, "ivy_left_jab_right_cross", ModSounds.IVY_LEFT_JAB_RIGHT_CROSS.get(), volume, pitch, 5);
            case "ivy_jab_jab_swing" -> playCustomSound(handler, "ivy_jab_jab_swing", ModSounds.IVY_JAB_JAB_SWING.get(), volume, pitch, 5);
            case "ivy_left_jab_right_swing" -> playCustomSound(handler, "ivy_left_jab_right_swing", ModSounds.IVY_LEFT_JAB_RIGHT_SWING.get(), volume, pitch, 5);
            case "ivy_right_hook_uppercut" -> playCustomSound(handler, "ivy_right_hook_uppercut", ModSounds.IVY_RIGHT_HOOK_UPPERCUT.get(), volume, pitch, 5);
            case "ivy_dodge_liver_shot" -> playCustomSound(handler, "ivy_dodge_liver_shot", ModSounds.IVY_DODGE_LIVER_SHOT.get(), volume, pitch, 5);
            case "ivy_dash_forward_right_cross" -> playCustomSound(handler, "ivy_dash_forward_right_cross", ModSounds.IVY_DASH_FORWARD_RIGHT_CROSS.get(), volume, pitch, 5);
            case "ivy_orthodox_throw_projectiles" -> playCustomSound(handler, "ivy_orthodox_throw_projectiles", ModSounds.IVY_ORTHODOX_THROW_PROJECTILES.get(), volume, pitch, 5);
            case "ivy_orthodox_retreat_to_drink" -> playCustomSound(handler, "ivy_orthodox_retreat_to_drink", ModSounds.IVY_ORTHODOX_RETREAT_TO_DRINK.get(), volume, pitch, 5);
            case "ivy_orthodox_retreat_to_eat" -> playCustomSound(handler, "ivy_orthodox_retreat_to_eat", ModSounds.IVY_ORTHODOX_RETREAT_TO_EAT.get(), volume, pitch, 5);
            case "ivy_sword_brandishes" -> playCustomSound(handler, "ivy_sword_brandishes", ModSounds.IVY_SWORD_BRANDISHES.get(), volume, pitch, 5);
            case "ivy_sword_unsheathe" -> playCustomSound(handler, "ivy_sword_unsheathe", ModSounds.IVY_SWORD_UNSHEATHE.get(), volume, pitch, 5);
            case "ivy_sword_to_orthodox" -> playCustomSound(handler, "ivy_sword_to_orthodox", ModSounds.IVY_SWORD_TO_ORTHODOX.get(), volume, pitch, 5);
            case "ivy_sword_slash_stab_stab" -> playCustomSound(handler, "ivy_sword_slash_stab_stab", ModSounds.IVY_SWORD_SLASH_STAB_STAB.get(), volume, pitch, 5);
            case "ivy_sword_quick_stab" -> playCustomSound(handler, "ivy_sword_quick_stab", ModSounds.IVY_SWORD_QUICK_STAB.get(), volume, pitch, 3);
            case "ivy_sword_swing" -> playCustomSound(handler, "ivy_sword_swing", ModSounds.IVY_SWORD_SWING.get(), volume, pitch, 4);
            case "ivy_sword_swing_slash" -> playCustomSound(handler, "ivy_sword_swing_slash", ModSounds.IVY_SWORD_SWING_SLASH.get(), volume, pitch, 5);
            case "ivy_sword_retreat_to_eat" -> playCustomSound(handler, "ivy_sword_retreat_to_eat", ModSounds.IVY_SWORD_RETREAT_TO_EAT.get(), volume, pitch, 5);
            case "ivy_sword_retreat_to_drink" -> playCustomSound(handler, "ivy_sword_retreat_to_drink", ModSounds.IVY_SWORD_RETREAT_TO_DRINK.get(), volume, pitch, 5);
            case "ivy_sword_dodge_parry" -> playCustomSound(handler, "ivy_sword_dodge_parry", ModSounds.IVY_SWORD_DODGE_PARRY.get(), volume, pitch, 5);
            case "ivy_dash_forward_spin_slash" -> playCustomSound(handler, "ivy_dash_forward_spin_slash", ModSounds.IVY_DASH_FORWARD_SPIN_SLASH.get(), volume, pitch, 5);
            case "ivy_embarrassed" -> playCustomSound(handler, "ivy_embarrassed", ModSounds.IVY_EMBARRASSED.get(), volume, pitch, 20);
            case "ivy_sigh" -> playCustomSound(handler, "ivy_sigh", ModSounds.IVY_SIGH.get(), volume, pitch, 20);
            case "ivy_hmm_trader" -> playCustomSound(handler, "ivy_hmm_trader", ModSounds.IVY_HMM_TRADER.get(), volume, pitch, 20);
            case "ivy_hmm_gardener" -> playCustomSound(handler, "ivy_hmm_gardener", ModSounds.IVY_HMM_GARDENER.get(), volume, pitch, 20);
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

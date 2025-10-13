package com.leon.saintsdragons.server.entity.dragons.nulljaw.handlers;

import com.leon.saintsdragons.common.registry.ModSounds;
import com.leon.saintsdragons.server.entity.base.DragonEntity;
import com.leon.saintsdragons.server.entity.handler.DragonSoundHandler;
import com.leon.saintsdragons.server.entity.interfaces.DragonSoundProfile;
import net.minecraft.world.phys.Vec3;

/**
 * Nulljaw-specific animation keyed sounds.
 */
public final class NulljawSoundProfile implements DragonSoundProfile {

    public static final NulljawSoundProfile INSTANCE = new NulljawSoundProfile();

    private NulljawSoundProfile() {}

    @Override
    public boolean handleAnimationSound(DragonSoundHandler handler, DragonEntity dragon, String key, String locator) {
        return switch (key) {
            case "nulljaw_phase2" -> {
                Vec3 mouth = handler.resolveLocatorWorldPos("mouth_origin");
                float pitch = 0.9f + dragon.getRandom().nextFloat() * 0.2f;
                handler.emitSound(ModSounds.NULLJAW_PHASE2.get(), 2.0f, pitch, mouth, false);
                yield true;
            }
            case "nulljaw_phase1" -> {
                Vec3 mouth = handler.resolveLocatorWorldPos("mouth_origin");
                float pitch = 0.9f + dragon.getRandom().nextFloat() * 0.2f;
                handler.emitSound(ModSounds.NULLJAW_PHASE1.get(), 1.4f, pitch, mouth, false);
                yield true;
            }
            case "nulljaw_step" -> {
                Vec3 pos = handler.resolveLocatorWorldPos(locator);
                float pitch = 0.9f + dragon.getRandom().nextFloat() * 0.2f;
                handler.emitSound(ModSounds.NULLJAW_STEP.get(), 0.8f, pitch, pos, false);
                yield true;
            }
            case "nulljaw_claw" -> {
                Vec3 pos = handler.resolveLocatorWorldPos(locator);
                float pitch = 0.9f + dragon.getRandom().nextFloat() * 0.2f;
                handler.emitSound(ModSounds.NULLJAW_CLAW.get(), 1.2f, pitch, pos, false);
                yield true;
            }
            case "nulljaw_bite" -> {
                Vec3 mouth = handler.resolveLocatorWorldPos("mouth_origin");
                float pitch = 0.95f + dragon.getRandom().nextFloat() * 0.1f;
                handler.emitSound(ModSounds.NULLJAW_BITE.get(), 1.1f, pitch, mouth, false);
                yield true;
            }
            case "nulljaw_roarclaw" -> {
                Vec3 pos = handler.resolveLocatorWorldPos(locator);
                if (pos == null) {
                    pos = handler.resolveLocatorWorldPos("frontLocator");
                }
                float pitch = 0.9f + dragon.getRandom().nextFloat() * 0.2f;
                handler.emitSound(ModSounds.NULLJAW_ROARCLAW.get(), 1.3f, pitch, pos, false);
                yield true;
            }
            default -> false;
        };
    }
}

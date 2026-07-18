package com.leon.saintsdragons.client.camera;

import com.leon.saintsdragons.common.item.DragonlordArmorSetBonus;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.world.phys.Vec3;

@Environment(EnvType.CLIENT)
public final class DragonlordFlightBoostController {
    private static final double TARGET_SPEED = 1.5D;
    private static final double DIRECT_ACCELERATION = 0.1D;
    private static final double VELOCITY_BLEND = 0.5D;

    private static int boostTicks;

    private DragonlordFlightBoostController() {
    }

    public static void startBoost(int durationTicks) {
        boostTicks = Math.max(0, durationTicks);
    }

    public static void tick(Minecraft minecraft) {
        LocalPlayer player = minecraft.player;
        if (player == null || minecraft.level == null) {
            boostTicks = 0;
            return;
        }
        if (minecraft.isPaused() || boostTicks <= 0) {
            return;
        }
        if (!canContinueBoost(player)) {
            boostTicks = 0;
            return;
        }

        Vec3 look = player.getLookAngle().normalize();
        Vec3 motion = player.getDeltaMovement();
        Vec3 target = look.scale(TARGET_SPEED);
        player.setDeltaMovement(motion.add(
                look.x * DIRECT_ACCELERATION + (target.x - motion.x) * VELOCITY_BLEND,
                look.y * DIRECT_ACCELERATION + (target.y - motion.y) * VELOCITY_BLEND,
                look.z * DIRECT_ACCELERATION + (target.z - motion.z) * VELOCITY_BLEND
        ));
        player.hasImpulse = true;
        player.resetFallDistance();
        boostTicks--;
    }

    private static boolean canContinueBoost(LocalPlayer player) {
        return player.isAlive()
                && !player.isSpectator()
                && !player.isPassenger()
                && !player.onGround()
                && !player.onClimbable()
                && !player.isInWaterOrBubble()
                && !player.getAbilities().flying
                && player.isFallFlying()
                && DragonlordArmorSetBonus.isWearingFullSet(player);
    }
}

package com.leon.saintsdragons.client.input;

import com.leon.saintsdragons.server.entity.base.DragonPartEntity;
import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;
import org.jetbrains.annotations.Nullable;

import java.util.function.Predicate;

public final class DragonPartInteractionTargeting {
    private static boolean pickingInteraction;

    private DragonPartInteractionTargeting() {
    }

    public static @Nullable HitResult pick(Minecraft minecraft) {
        HitResult originalHit = minecraft.hitResult;
        if (!(originalHit instanceof EntityHitResult entityHit)
                || !(entityHit.getEntity() instanceof DragonPartEntity)
                || minecraft.level == null || minecraft.player == null || minecraft.gameMode == null
                || minecraft.getCameraEntity() == null || pickingInteraction) {
            return originalHit;
        }

        Entity originalCrosshairEntity = minecraft.crosshairPickEntity;
        try {
            pickingInteraction = true;
            minecraft.gameRenderer.pick(1.0F);
            return minecraft.hitResult;
        } finally {
            pickingInteraction = false;
            minecraft.hitResult = originalHit;
            minecraft.crosshairPickEntity = originalCrosshairEntity;
        }
    }

    public static Predicate<Entity> filter(Predicate<Entity> predicate) {
        return pickingInteraction
                ? predicate.and(entity -> !(entity instanceof DragonPartEntity))
                : predicate;
    }
}

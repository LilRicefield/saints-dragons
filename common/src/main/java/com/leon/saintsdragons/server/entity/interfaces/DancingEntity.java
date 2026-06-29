package com.leon.saintsdragons.server.entity.interfaces;

import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.level.block.entity.JukeboxBlockEntity;
import net.minecraft.world.phys.Vec3;
import software.bernie.geckolib.core.animation.RawAnimation;

public interface DancingEntity {
    int JUKEBOX_DANCE_RANGE = 16;
    int JUKEBOX_DANCE_SCAN_INTERVAL = 10;

    boolean isDancing();

    void setDancing(boolean dancing);

    RawAnimation getDanceAnimation();

    default boolean canDance() {
        return true;
    }

    static boolean shouldScanForJukebox(Entity entity) {
        return !entity.level().isClientSide
                && Math.floorMod(entity.tickCount + entity.getId(), JUKEBOX_DANCE_SCAN_INTERVAL) == 0;
    }

    static boolean hasPlayingJukeboxNearby(Entity entity) {
        BlockPos center = entity.blockPosition();
        BlockPos.MutableBlockPos cursor = new BlockPos.MutableBlockPos();
        int range = JUKEBOX_DANCE_RANGE;

        for (int x = center.getX() - range; x <= center.getX() + range; x++) {
            for (int y = center.getY() - range; y <= center.getY() + range; y++) {
                for (int z = center.getZ() - range; z <= center.getZ() + range; z++) {
                    cursor.set(x, y, z);
                    if (entity.level().getBlockEntity(cursor) instanceof JukeboxBlockEntity jukebox
                            && jukebox.isRecordPlaying()) {
                        return true;
                    }
                }
            }
        }

        return false;
    }

    static void holdStillForDance(Entity entity) {
        if (entity instanceof Mob mob) {
            mob.getNavigation().stop();
            mob.xxa = 0.0F;
            mob.yya = 0.0F;
            mob.zza = 0.0F;
        }

        Vec3 movement = entity.getDeltaMovement();
        if (movement.x != 0.0D || movement.z != 0.0D) {
            entity.setDeltaMovement(0.0D, movement.y, 0.0D);
        }
    }
}

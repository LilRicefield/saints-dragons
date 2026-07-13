package com.leon.saintsdragons.client.renderer.vfx;

import com.leon.saintsdragons.common.particle.SonicRingData;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

@Environment(EnvType.CLIENT)
public final class BloodTempestSonicRingTrail {
    private static final int RING_INTERVAL_TICKS = 1;
    private static final double RING_SPACING = 3.0D;
    private static final int MAX_RINGS = 5;
    private static final double FRONT_OFFSET = -9.0D;
    private static final double FORWARD_STEP = 1.85D;
    private static final float BASE_SCALE = 1.20F;
    private static final float SCALE_VARIANCE = 0.25F;
    private static final int DURATION_TICKS = 10;

    private static final List<Trail> TRAILS = new ArrayList<>();
    private static Level activeLevel;

    private BloodTempestSonicRingTrail() {
    }

    public static void start(int entityId, Vec3 origin, Vec3 destination) {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.level == null || origin == null || destination == null) {
            return;
        }

        Vec3 travel = destination.subtract(origin);
        double distance = travel.length();
        if (distance < 1.0E-4D) {
            return;
        }

        Entity entity = minecraft.level.getEntity(entityId);
        double height = entity != null ? entity.getBbHeight() * 0.5D : 0.9D;
        int ringCount = Math.max(1, Math.min(MAX_RINGS, (int) Math.ceil(distance / RING_SPACING)));
        TRAILS.add(new Trail(destination, travel.normalize(), height, ringCount));
        activeLevel = minecraft.level;
    }

    public static void tick(Minecraft minecraft) {
        if (minecraft.level == null || minecraft.level != activeLevel) {
            TRAILS.clear();
            activeLevel = minecraft.level;
            return;
        }

        Iterator<Trail> trails = TRAILS.iterator();
        while (trails.hasNext()) {
            if (trails.next().tick(minecraft.level)) {
                trails.remove();
            }
        }
    }

    private static final class Trail {
        private final Vec3 destination;
        private final Vec3 direction;
        private final double height;
        private final int ringCount;
        private final float yaw;
        private final float pitch;
        private int nextRing;
        private int cooldown;

        private Trail(Vec3 destination, Vec3 direction, double height, int ringCount) {
            this.destination = destination;
            this.direction = direction;
            this.height = height;
            this.ringCount = ringCount;
            this.yaw = (float) Math.atan2(direction.x, direction.z);
            this.pitch = (float) Math.asin(-direction.y);
        }

        private boolean tick(ClientLevel level) {
            if (this.cooldown > 0) {
                this.cooldown--;
                return false;
            }

            int reverseIndex = this.ringCount - 1 - this.nextRing;
            double forwardDistance = FRONT_OFFSET + reverseIndex * FORWARD_STEP;
            Vec3 position = this.destination
                    .add(this.direction.scale(forwardDistance))
                    .add(0.0D, this.height, 0.0D);
            float scale = BASE_SCALE + level.random.nextFloat() * SCALE_VARIANCE;
            level.addParticle(
                    new SonicRingData(this.yaw, this.pitch, scale, DURATION_TICKS),
                    position.x, position.y, position.z,
                    0.0D, 0.0D, 0.0D
            );

            this.nextRing++;
            this.cooldown = RING_INTERVAL_TICKS - 1;
            return this.nextRing >= this.ringCount;
        }
    }
}

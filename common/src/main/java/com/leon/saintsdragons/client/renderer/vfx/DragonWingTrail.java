package com.leon.saintsdragons.client.renderer.vfx;

import net.minecraft.world.phys.Vec3;

public final class DragonWingTrail {
    private final Vec3[] positions;
    private final float[] alpha;
    private int headIndex;
    private int pointCount;

    public DragonWingTrail(int capacity) {
        this.positions = new Vec3[capacity];
        this.alpha = new float[capacity];
    }

    public void add(Vec3 position, float alphaValue) {
        int targetIndex;
        if (pointCount < positions.length) {
            targetIndex = (headIndex + pointCount) % positions.length;
            pointCount++;
        } else {
            targetIndex = headIndex;
            headIndex = (headIndex + 1) % positions.length;
        }
        positions[targetIndex] = position;
        alpha[targetIndex] = alphaValue;
    }

    public void decay() {
        if (pointCount > 0) {
            headIndex = (headIndex + 1) % positions.length;
            pointCount--;
        }
    }

    public int getPointCount() {
        return pointCount;
    }

    public int getCapacity() {
        return positions.length;
    }

    public int getHeadIndex() {
        return headIndex;
    }

    public Vec3 getPositionAt(int logicalIndex) {
        return positions[(headIndex + logicalIndex) % positions.length];
    }

    public float getAlphaAt(int logicalIndex) {
        return alpha[(headIndex + logicalIndex) % alpha.length];
    }
}

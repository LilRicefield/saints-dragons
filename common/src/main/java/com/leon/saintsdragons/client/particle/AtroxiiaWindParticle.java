package com.leon.saintsdragons.client.particle;

import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.particle.TextureSheetParticle;

abstract class AtroxiiaWindParticle extends TextureSheetParticle {
    private final double swayAxisX;
    private final double swayAxisZ;
    private final double windPhase;
    private final double windFrequency;
    private final double verticalAmplitude;
    private final double swayAmplitude;

    protected AtroxiiaWindParticle(ClientLevel level, double x, double y, double z,
                                   double xSpeed, double ySpeed, double zSpeed,
                                   double verticalAmplitude, double swayAmplitude) {
        super(level, x, y, z, xSpeed, ySpeed, zSpeed);
        this.xd = xSpeed;
        this.yd = ySpeed;
        this.zd = zSpeed;
        this.hasPhysics = false;

        double horizontalSpeed = Math.sqrt(xSpeed * xSpeed + zSpeed * zSpeed);
        if (horizontalSpeed > 1.0E-4D) {
            this.swayAxisX = -zSpeed / horizontalSpeed;
            this.swayAxisZ = xSpeed / horizontalSpeed;
        } else {
            double angle = this.random.nextDouble() * Math.PI * 2.0D;
            this.swayAxisX = Math.cos(angle);
            this.swayAxisZ = Math.sin(angle);
        }

        this.windPhase = this.random.nextDouble() * Math.PI * 2.0D;
        this.windFrequency = 0.28D + this.random.nextDouble() * 0.22D;
        this.verticalAmplitude = verticalAmplitude * (0.65D + this.random.nextDouble() * 0.7D);
        this.swayAmplitude = swayAmplitude * (0.65D + this.random.nextDouble() * 0.7D);
    }

    protected void moveInWind(float horizontalFriction, float verticalFriction, double lift) {
        double time = this.age * this.windFrequency + this.windPhase;
        double verticalFlutter = Math.sin(time) * this.verticalAmplitude;
        double lateralSway = Math.cos(time * 0.73D) * this.swayAmplitude;

        this.move(
                this.xd + this.swayAxisX * lateralSway,
                this.yd + verticalFlutter,
                this.zd + this.swayAxisZ * lateralSway
        );
        this.xd *= horizontalFriction;
        this.yd = this.yd * verticalFriction + lift;
        this.zd *= horizontalFriction;
    }

    protected float fadeAlpha(float maximumAlpha, float fadeInEnd, float fadeOutStart) {
        float progress = this.age / (float) this.lifetime;
        if (progress < fadeInEnd) {
            return maximumAlpha * progress / fadeInEnd;
        }
        if (progress > fadeOutStart) {
            return maximumAlpha * Math.max(0.0F, 1.0F - (progress - fadeOutStart) / (1.0F - fadeOutStart));
        }
        return maximumAlpha;
    }
}

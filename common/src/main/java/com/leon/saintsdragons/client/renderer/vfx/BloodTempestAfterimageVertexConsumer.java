package com.leon.saintsdragons.client.renderer.vfx;

import com.mojang.blaze3d.vertex.VertexConsumer;

public final class BloodTempestAfterimageVertexConsumer implements VertexConsumer {
    private final VertexConsumer delegate;
    private final float red;
    private final float green;
    private final float blue;
    private final float alpha;

    private BloodTempestAfterimageVertexConsumer(VertexConsumer delegate) {
        this.delegate = delegate;
        this.red = BloodTempestAfterimageRenderContext.red();
        this.green = BloodTempestAfterimageRenderContext.green();
        this.blue = BloodTempestAfterimageRenderContext.blue();
        this.alpha = BloodTempestAfterimageRenderContext.alpha();
    }

    public static VertexConsumer wrap(VertexConsumer delegate) {
        return BloodTempestAfterimageRenderContext.isActive()
                ? new BloodTempestAfterimageVertexConsumer(delegate)
                : delegate;
    }

    @Override
    public VertexConsumer vertex(double x, double y, double z) {
        delegate.vertex(x, y, z);
        return this;
    }

    @Override
    public VertexConsumer color(int red, int green, int blue, int alpha) {
        delegate.color(channel(this.red), channel(this.green), channel(this.blue), channel(this.alpha));
        return this;
    }

    @Override
    public VertexConsumer uv(float u, float v) {
        delegate.uv(u, v);
        return this;
    }

    @Override
    public VertexConsumer overlayCoords(int u, int v) {
        delegate.overlayCoords(u, v);
        return this;
    }

    @Override
    public VertexConsumer uv2(int u, int v) {
        delegate.uv2(u, v);
        return this;
    }

    @Override
    public VertexConsumer normal(float x, float y, float z) {
        delegate.normal(x, y, z);
        return this;
    }

    @Override
    public void endVertex() {
        delegate.endVertex();
    }

    @Override
    public void defaultColor(int red, int green, int blue, int alpha) {
        delegate.defaultColor(channel(this.red), channel(this.green), channel(this.blue), channel(this.alpha));
    }

    @Override
    public void unsetDefaultColor() {
        delegate.unsetDefaultColor();
    }

    @Override
    public void vertex(float x, float y, float z, float red, float green, float blue, float alpha,
                       float u, float v, int overlay, int light, float normalX, float normalY, float normalZ) {
        delegate.vertex(x, y, z, this.red, this.green, this.blue, this.alpha,
                u, v, overlay, light, normalX, normalY, normalZ);
    }

    private static int channel(float value) {
        return Math.round(value * 255.0F);
    }
}

package com.leon.saintsdragons.client.renderer.vfx;

import com.mojang.blaze3d.vertex.VertexConsumer;

public final class ScrollingFireballVertexConsumer implements VertexConsumer {
    private final VertexConsumer delegate;
    private final float offset;

    public ScrollingFireballVertexConsumer(VertexConsumer delegate, float offset) {
        this.delegate = delegate;
        this.offset = offset;
    }

    @Override
    public VertexConsumer vertex(double x, double y, double z) {
        delegate.vertex(x, y, z);
        return this;
    }

    @Override
    public VertexConsumer color(int red, int green, int blue, int alpha) {
        delegate.color(red, green, blue, alpha);
        return this;
    }

    @Override
    public VertexConsumer uv(float u, float v) {
        delegate.uv(u, v * 0.5F + offset);
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
        delegate.defaultColor(red, green, blue, alpha);
    }

    @Override
    public void unsetDefaultColor() {
        delegate.unsetDefaultColor();
    }

    @Override
    public void vertex(float x, float y, float z, float red, float green, float blue, float alpha,
                       float u, float v, int overlay, int light, float normalX, float normalY, float normalZ) {
        delegate.vertex(x, y, z, red, green, blue, alpha,
                u, v * 0.5F + offset, overlay, light, normalX, normalY, normalZ);
    }

}

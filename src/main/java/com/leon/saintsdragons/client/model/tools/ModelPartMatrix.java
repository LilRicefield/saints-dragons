package com.leon.saintsdragons.client.model.tools;

import org.joml.Matrix3f;
import org.joml.Matrix4f;

/**
 * Simple wrapper to hold world transformation matrices for bone positioning.
 * Since ModelPart is final in this MC version, we can't extend it.
 */
public class ModelPartMatrix {
    private Matrix4f worldXform;
    private Matrix3f worldNormal;
    private String name;

    public ModelPartMatrix() {
        worldNormal = new Matrix3f();
        worldNormal.identity();
        worldXform = new Matrix4f();
        worldXform.identity();
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getName() {
        return name;
    }

    public Matrix3f getWorldNormal() {
        return worldNormal;
    }

    public void setWorldNormal(Matrix3f worldNormal) {
        this.worldNormal = worldNormal;
    }

    public Matrix4f getWorldXform() {
        return worldXform;
    }

    public void setWorldXform(Matrix4f worldXform) {
        this.worldXform = worldXform;
    }
}

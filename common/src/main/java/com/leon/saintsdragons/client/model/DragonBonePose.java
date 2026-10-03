package com.leon.saintsdragons.client.model;

import software.bernie.geckolib.core.animatable.model.CoreGeoBone;

import java.util.Collection;

/** This will be reusable value snapshot.GeckoLib's baked bones themselves are shared between entities */
public final class DragonBonePose {
    private CoreGeoBone[] bones = new CoreGeoBone[0];
    private float[] values = new float[0];

    public void capture(Collection<? extends CoreGeoBone> source) {
        if (bones.length != source.size() || (!source.isEmpty() && bones[0] != source.iterator().next())) {
            bones = source.toArray(CoreGeoBone[]::new);
            values = new float[bones.length * 14];
        }
        for (int i = 0; i < bones.length; i++) {
            CoreGeoBone bone = bones[i];
            int j = i * 14;
            values[j] = bone.getRotX(); values[j + 1] = bone.getRotY(); values[j + 2] = bone.getRotZ();
            values[j + 3] = bone.getPosX(); values[j + 4] = bone.getPosY(); values[j + 5] = bone.getPosZ();
            values[j + 6] = bone.getScaleX(); values[j + 7] = bone.getScaleY(); values[j + 8] = bone.getScaleZ();
            values[j + 9] = bone.getPivotX(); values[j + 10] = bone.getPivotY(); values[j + 11] = bone.getPivotZ();
            values[j + 12] = bone.isHidden() ? 1 : 0;
            values[j + 13] = bone.isHidingChildren() ? 1 : 0;
        }
    }

    public void restore() {
        for (int i = 0; i < bones.length; i++) {
            CoreGeoBone bone = bones[i];
            int j = i * 14;
            bone.updateRotation(values[j], values[j + 1], values[j + 2]);
            bone.updatePosition(values[j + 3], values[j + 4], values[j + 5]);
            bone.updateScale(values[j + 6], values[j + 7], values[j + 8]);
            bone.updatePivot(values[j + 9], values[j + 10], values[j + 11]);
            bone.setHidden(values[j + 12] != 0);
            bone.setChildrenHidden(values[j + 13] != 0);
        }
    }

    public void resetChanges() {
        for (CoreGeoBone bone : bones) {
            bone.resetStateChanges();
        }
    }
}

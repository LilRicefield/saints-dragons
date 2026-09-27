package com.leon.saintsdragons.server.entity.component;

import com.leon.saintsdragons.server.entity.ability.DragonAimHelper;
import com.leon.saintsdragons.server.entity.base.DragonEntity;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec3;
import org.joml.Quaternionf;
import org.joml.Vector3f;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import javax.annotation.Nullable;

/** Per-dragon aim history, shared by the rendered pose and the server collision rig */
public final class DragonBreathPose {
    public interface Editor {
        @Nullable String parent(String bone);
        float rotation(String bone, int axis);
        void rotate(String bone, int axis, float radians, boolean fromInitial);
    }

    public record Joint(String bone, float weight) {}

    /** Joints are ordered from the neck base to the head while other ancestors are read from the rig. */
    public record Profile(int blendInTicks, int blendOutTicks, float aimResponse,
                          float maxCorrectionDegrees, List<Joint> joints) {
        public Profile {
            joints = List.copyOf(joints);
            if (blendInTicks <= 0 || blendOutTicks <= 0 || aimResponse <= 0 || aimResponse > 1
                    || maxCorrectionDegrees <= 0 || maxCorrectionDegrees > 180 || joints.isEmpty()
                    || joints.stream().anyMatch(joint -> joint.weight() <= 0)) {
                throw new IllegalArgumentException("Invalid breath pose profile");
            }
        }
    }

    private final Profile profile;
    private final float totalWeight;
    private Vec3 previousDirection;
    private Vec3 direction;
    private float previousBlend;
    private float blend;

    public DragonBreathPose(Profile profile) {
        this.profile = profile;
        float total = 0;
        for (Joint joint : profile.joints()) total += joint.weight();
        this.totalWeight = total;
    }

    public void tick(DragonEntity dragon, boolean active, @Nullable Vec3 desired) {
        previousBlend = blend;
        previousDirection = direction;
        desired = desired == null ? null : DragonAimHelper.normalizeOrNull(desired);
        active &= dragon.isAlive() && !dragon.isDeadOrDying()
                && !dragon.isBaby() && !dragon.isScentAssessing() && desired != null;
        if (active) {
            // Use the shot direction, never the previous rendered mouth position
            // Moving the neck must not feed its own visual locator back into next frame's aiming
            if (direction == null || previousBlend == 0.0F) {
                previousDirection = direction = desired;
            } else {
                direction = interpolateDirection(direction, desired, profile.aimResponse());
            }
        }
        blend = Mth.approach(blend, active ? 1.0F : 0.0F,
                1.0F / (active ? profile.blendInTicks() : profile.blendOutTicks()));
    }

    public float weight(float partialTick) {
        float value = Mth.lerp(partialTick, previousBlend, blend);
        return value * value * (3.0F - 2.0F * value);
    }

    public void apply(DragonEntity dragon, float partialTick, Editor editor) {
        float weight = weight(partialTick);
        if (weight <= 0.0F || direction == null) return;
        Vec3 aim = interpolateDirection(previousDirection, direction, partialTick);

        // GeckoLib and CollisionRig both compose local rotations as Z, Y, X
        // Read the current animated pose, including flight pitch or bank and the authored breath bones
        float bodyYaw = Mth.rotLerp(partialTick, dragon.yBodyRotO, dragon.yBodyRot);
        Quaternionf parent = new Quaternionf().rotationY((180.0F - bodyYaw) * Mth.DEG_TO_RAD);
        List<String> chain = new ArrayList<>();
        String headBone = profile.joints().get(profile.joints().size() - 1).bone();
        for (String name = headBone; name != null && !name.isEmpty(); name = editor.parent(name)) {
            chain.add(name);
        }
        Collections.reverse(chain);
        Quaternionf[] rotations = new Quaternionf[chain.size()];
        Quaternionf head = new Quaternionf(parent);
        for (int i = 0; i < chain.size(); i++) {
            rotations[i] = rotation(editor, chain.get(i));
            head.mul(rotations[i]);
        }
        Vector3f forward = head.transform(new Vector3f(0, 0, -1));
        Quaternionf correction = new Quaternionf().rotationTo(forward, aim.toVector3f());
        float angle = 2.0F * (float) Math.acos(Mth.clamp(Math.abs(correction.w), 0.0F, 1.0F));
        float limit = profile.maxCorrectionDegrees() * Mth.DEG_TO_RAD;
        if (angle > limit) weight *= limit / angle;

        for (int i = 0; i < chain.size(); i++) {
            String name = chain.get(i);
            float share = jointWeight(name);
            Quaternionf world = new Quaternionf(parent).mul(rotations[i]);
            if (share == 0.0F) {
                parent.set(world);
                continue;
            }
            Quaternionf step = new Quaternionf().slerp(correction, weight * share / totalWeight);
            Quaternionf adjustedWorld = step.mul(world);
            Quaternionf adjustedLocal = new Quaternionf(parent).invert().mul(adjustedWorld).normalize();
            writeRotation(editor, name, adjustedLocal);
            parent.set(adjustedWorld);
        }
    }

    private float jointWeight(String name) {
        for (Joint joint : profile.joints()) if (joint.bone().equals(name)) return joint.weight();
        return 0.0F;
    }

    private static Vec3 interpolateDirection(Vec3 from, Vec3 to, float fraction) {
        double angle = Math.toDegrees(Math.acos(Mth.clamp(from.dot(to), -1.0D, 1.0D)));
        return DragonAimHelper.turnDirection(from, to, angle * fraction);
    }

    private static Quaternionf rotation(Editor editor, String name) {
        return new Quaternionf().rotationZYX(editor.rotation(name, 2),
                editor.rotation(name, 1), editor.rotation(name, 0));
    }

    private static void writeRotation(Editor editor, String name, Quaternionf q) {
        // Extract ZYX explicitly. JOML 1.10.5's getEulerAnglesZYX has an incorrect denominator for X when Y is nonzero which
        // would introduce a pitch wobble
        float x = (float) Math.atan2(2.0F * (q.w * q.x + q.y * q.z),
                1.0F - 2.0F * (q.x * q.x + q.y * q.y));
        float y = (float) Math.asin(Mth.clamp(2.0F * (q.w * q.y - q.z * q.x), -1.0F, 1.0F));
        float z = (float) Math.atan2(2.0F * (q.w * q.z + q.x * q.y),
                1.0F - 2.0F * (q.y * q.y + q.z * q.z));
        editor.rotate(name, 0, x - editor.rotation(name, 0), false);
        editor.rotate(name, 1, y - editor.rotation(name, 1), false);
        editor.rotate(name, 2, z - editor.rotation(name, 2), false);
    }
}

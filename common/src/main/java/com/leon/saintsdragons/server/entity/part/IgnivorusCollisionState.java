package com.leon.saintsdragons.server.entity.part;

import com.leon.saintsdragons.server.entity.dragons.ignivorus.Ignivorus;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.joml.Matrix4f;

import java.util.*;

public final class IgnivorusCollisionState {
    private record Trigger(String name, int start) {}
    private static final String[] CONTROLLERS = {"movement", "flight", "ignivorusAction", "ignivorusFastAction", "interaction"};
    private final Ignivorus dragon;
    private final Map<String, Trigger> triggers = new HashMap<>();
    private AABB[] bounds;
    private Map<String, Vec3> locators = Map.of();
    private int poseTick = Integer.MIN_VALUE;
    private String baseAnimation = "";
    private int baseStart;
    private AABB[] clientBounds;
    private int clientTick = Integer.MIN_VALUE;

    public IgnivorusCollisionState(Ignivorus dragon) { this.dragon = dragon; }

    private static String animationName(String animation) {
        return switch (animation) {
            case "takeoff", "rider_takeoff" -> "take_off";
            case "sit_down" -> "down";
            case "sit_up" -> "up";
            case "fire_breath_stop" -> "fire_breath_end";
            case "stomp_left" -> "ignivorus_stomp_left";
            case "stomp_right" -> "ignivorus_stomp_right";
            case "leap_impact" -> "ignivorus_impact";
            case "ignivorus_flex" -> "flex";
            case "ignivorus_hurt" -> "hurt";
            default -> animation.replace("_shoot", "_shoots");
        };
    }

    public void trigger(String controller, String animation) {
        String name = animationName(animation);
        if (IgnivorusHitboxes.rig().clip(name) != null) triggers.put(controller, new Trigger(name, dragon.tickCount));
        poseTick = Integer.MIN_VALUE;
    }

    public void stop(String controller, String animation) {
        triggers.entrySet().removeIf(entry -> (controller == null || controller.equals(entry.getKey()))
                && (animation == null || animationName(animation).equals(entry.getValue().name)));
        poseTick = Integer.MIN_VALUE;
    }

    public void captureClientBounds(AABB[] snapshot) {
        clientBounds = snapshot.clone();
        clientTick = dragon.tickCount;
    }

    public void invalidate() {
        poseTick = Integer.MIN_VALUE;
    }

    public void update() {
        if (poseTick == dragon.tickCount) return;
        poseTick = dragon.tickCount;
        CollisionRig rig = IgnivorusHitboxes.rig();
        String base = selectBase();
        if (!base.equals(baseAnimation)) { baseAnimation = base; baseStart = dragon.tickCount; }
        float[] pose = rig.restingPose();
        rig.animate(pose, base, (dragon.tickCount - baseStart) / 20.0);
        boolean skyfall = false;
        for (String controller : CONTROLLERS) {
            Trigger trigger = triggers.get(controller);
            if (trigger == null) continue;
            CollisionRig.Clip clip = rig.clip(trigger.name);
            double age = (dragon.tickCount - trigger.start) / 20.0;
            boolean expired = !clip.loop() && !clip.hold() && age > clip.length();
            if (trigger.name.contains("_charge") && dragon.getFireballChargeLevel() == 0 && age > 0.15) expired = true;
            if (trigger.name.equals("fire_breathing") && !dragon.isBreathingFire()) expired = true;
            if (dragon.isTamingStunned() || dragon.isDeadOrDying()) expired = true;
            if (expired) { triggers.remove(controller); continue; }
            rig.animate(pose, trigger.name, age);
            skyfall |= trigger.name.startsWith("skyfall");
        }
        if (!skyfall) applySteering(rig, pose);
        Map<String, Matrix4f> transforms = rig.matrices(pose, dragon.yBodyRot);
        bounds = new AABB[IgnivorusHitboxes.REGIONS.size()];
        for (int i = 0; i < bounds.length; i++) bounds[i] = rig.bounds(IgnivorusHitboxes.REGIONS.get(i).bones(), transforms);
        Map<String, Vec3> points = new HashMap<>();
        for (CollisionRig.Bone bone : rig.bones()) {
            var point = transforms.get(bone.name()).transformPosition(bone.pivot().toVector3f());
            points.put(bone.name(), new Vec3(point.x, point.y, point.z));
        }
        locators = Map.copyOf(points);
    }

    public AABB bounds(int index) {
        update();
        AABB box = dragon.level().isClientSide && clientBounds != null && dragon.tickCount - clientTick <= 2
                ? clientBounds[index] : bounds[index];
        if (box == null) box = bounds[index];
        return box.move(dragon.position());
    }

    public Vec3 locator(String name) {
        update();
        Vec3 point = locators.get(name.equals("fireBoneOrigin") ? "fireBone" : name);
        return point == null ? null : dragon.position().add(point);
    }

    private String selectBase() {
        if (dragon.isDeadOrDying()) return "die";
        if (dragon.isTamingStunned()) return "stunned";
        if (dragon.isSkyfallIdlePause()) return "idle";
        if (dragon.isSleeping()) return "sleep";
        if (dragon.isInSittingPose()) return "sit";
        if (dragon.isLeaping()) return "ignivorus_leap";
        if (dragon.isTakeoff()) return dragon.isPhase2Active() ? "phase2_takeoff" : "take_off";
        if (dragon.isFlying() || dragon.isLanding() || dragon.isHovering()) {
            return switch (dragon.getVisualFlightState(1.0F)) {
                case SPRINT_FLAP -> "sprint_flap";
                case FLAP -> "flap";
                case FLY_IDLE -> "fly_idle";
                default -> "glide";
            };
        }
        if (dragon.isInWaterOrBubble()) return "swim";
        int movement = dragon.getEntityData().get(Ignivorus.DATA_GROUND_MOVE_STATE);
        if (dragon.isVehicle()) {
            boolean moving = Math.abs(dragon.getEntityData().get(Ignivorus.DATA_RIDER_FORWARD)) > 0.01F
                    || Math.abs(dragon.getEntityData().get(Ignivorus.DATA_RIDER_STRAFE)) > 0.01F;
            movement = !moving ? 0 : dragon.getEntityData().get(Ignivorus.DATA_ACCELERATING) ? 2 : 1;
        }
        if (dragon.isBulldozing()) return movement == 0 ? "bulldozer_idle" : "bulldozing";
        if (dragon.isScentAssessing()) return "investigating";
        return (dragon.isPhase2Active() ? "phase2_" : "") + (movement == 2 ? "run" : movement == 1 ? "walk" : "idle");
    }

    private void applySteering(CollisionRig rig, float[] pose) {
        if (!dragon.isAlive() || dragon.isScentAssessing()) return;
        IgnivorusPoseOffsets.apply(dragon, 1.0F, -dragon.getXRot(),
                -Mth.wrapDegrees(dragon.yHeadRot - dragon.yBodyRot), (name, axis, rotation, fromInitial) -> {
                    int index = rig.index(name);
                    if (index < 0) return;
                    Vec3 initial = rig.bones().get(index).rotation();
                    float base = fromInitial ? (float) (axis == 0 ? initial.x : axis == 1 ? initial.y : initial.z)
                            : pose[index * 9 + axis];
                    pose[index * 9 + axis] = base + (float) Math.toDegrees(rotation) * (axis < 2 ? -1 : 1);
                });
    }
}

package com.leon.saintsdragons.util.animation;

import com.leon.saintsdragons.server.entity.npc.IvyLocomotionBlend;
import com.leon.saintsdragons.server.entity.npc.IvyFallAnimationState;
import com.leon.saintsdragons.server.entity.npc.IvyTheDragonMerchant;
import net.minecraft.util.Mth;
import org.joml.Quaternionf;
import org.joml.Vector3f;
import software.bernie.geckolib.core.animatable.model.CoreGeoBone;
import software.bernie.geckolib.core.animatable.model.CoreGeoModel;
import software.bernie.geckolib.core.animation.Animation;
import software.bernie.geckolib.core.animation.AnimationState;
import software.bernie.geckolib.core.keyframe.BoneAnimation;
import software.bernie.geckolib.core.keyframe.BoneAnimationQueue;
import software.bernie.geckolib.core.state.BoneSnapshot;

import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;

/** Blends the woman */
public final class IvyLocomotionAnimationController extends EntityAnimationController<IvyTheDragonMerchant> {
    private static final String PREFIX = "ivy_oleander.animation.";
    private static final int ROTATION = 1;
    private static final int POSITION = 2;
    private static final int SCALE = 4;
    private static final float ENTRY_TICKS = 3.0F;
    private static final double WALK_RIGHT_CONTACT_SECONDS = 0.00D;
    private static final double WALK_LEFT_CONTACT_SECONDS = 0.46D;
    private static final double RUN_RIGHT_CONTACT_SECONDS = 0.10D;
    private static final double RUN_LEFT_CONTACT_SECONDS = 0.43D;

    private Animation idle;
    private Animation walk;
    private Animation run;
    private Animation falling;
    private Animation landLight;
    private Animation landHeavy;
    private final Map<String, BoneAnimation> idleTracks = new HashMap<>();
    private final Map<String, BoneAnimation> walkTracks = new HashMap<>();
    private final Map<String, BoneAnimation> runTracks = new HashMap<>();
    private final Map<String, BoneAnimation> fallTracks = new HashMap<>();
    private final Map<String, BoneAnimation> lightLandingTracks = new HashMap<>();
    private final Map<String, BoneAnimation> heavyLandingTracks = new HashMap<>();
    private final Set<String> gaitBones = new LinkedHashSet<>();
    private final Map<String, Integer> gaitChannels = new HashMap<>();
    private final Map<String, float[]> entryPose = new HashMap<>();
    private final float[] idlePose = new float[9];
    private final float[] walkPose = new float[9];
    private final float[] runPose = new float[9];
    private final float[] fallPose = new float[9];
    private final float[] lightLandingPose = new float[9];
    private final float[] heavyLandingPose = new float[9];
    private final float[] landingPose = new float[9];
    private final float[] mixedPose = new float[9];
    private final Quaternionf rotation = new Quaternionf();
    private final Quaternionf otherRotation = new Quaternionf();
    private final Vector3f angles = new Vector3f();
    private double entryTime = Double.NaN;
    private double lastSeekTime = Double.NaN;
    private float entryDuration = ENTRY_TICKS;
    private long lastLandingTick = -1L;

    public IvyLocomotionAnimationController(IvyTheDragonMerchant ivy,
                                           AnimationStateHandler<IvyTheDragonMerchant> predicate) {
        super(ivy, "movement", 3, predicate);
    }

    @Override
    public void process(CoreGeoModel<IvyTheDragonMerchant> model, AnimationState<IvyTheDragonMerchant> state,
                        Map<String, CoreGeoBone> bones, Map<String, BoneSnapshot> snapshots,
                        double seekTime, boolean crashWhenCantFindBone) {
        super.process(model, state, bones, snapshots, seekTime, crashWhenCantFindBone);
        if (!blendActive() || !loadClips(model)) {
            clearEntry();
            return;
        }
        IvyFallAnimationState air = animatable.getFallAnimationState();
        long landingTick = air.landingTick();
        boolean newLanding = air.isLanding() && landingTick != lastLandingTick;
        lastLandingTick = landingTick;
        if (Double.isNaN(entryTime) || seekTime < lastSeekTime || seekTime - lastSeekTime > 10.0D) {
            entryTime = seekTime;
            entryDuration = newLanding ? 1.0F : ENTRY_TICKS;
            captureEntryPose(bones, snapshots);
        } else if (newLanding) {
            // Retain the outgoing pose even if a landing packet arrives partway through compression.
            entryTime = seekTime;
            entryDuration = 1.0F;
            captureEntryPose(bones, snapshots);
        }
        lastSeekTime = seekTime;
        float entryWeight = Mth.clamp((float) (seekTime - entryTime) / entryDuration, 0.0F, 1.0F);
        IvyLocomotionBlend blend = animatable.getLocomotionBlend();
        float partialTick = state.getPartialTick();
        float movingWeight = blend.moving(partialTick);
        float runningWeight = blend.running(partialTick);
        double phase = blend.phase(partialTick);
        double walkTime = contactTick(phase, walk.length(), WALK_RIGHT_CONTACT_SECONDS, WALK_LEFT_CONTACT_SECONDS);
        double runTime = contactTick(phase, run.length(), RUN_RIGHT_CONTACT_SECONDS, RUN_LEFT_CONTACT_SECONDS);
        double idleTime = idle.length() > 0.0D ? seekTime % idle.length() : 0.0D;
        float fallWeight = falling != null ? air.falling(partialTick) : 0.0F;
        float landingWeight = landLight != null && landHeavy != null ? air.landingWeight(partialTick) : 0.0F;
        double fallTime = falling != null && falling.length() > 0.0D ? air.fallTime(partialTick) % falling.length() : 0.0D;
        double lightTime = landLight != null ? air.landingSourceTick(partialTick, false, landLight.length()) : 0.0D;
        double heavyTime = landHeavy != null ? air.landingSourceTick(partialTick, true, landHeavy.length()) : 0.0D;

        for (String name : gaitBones) {
            CoreGeoBone bone = bones.get(name);
            if (bone == null) continue;
            DragonAnimationSampler.sample(idleTracks.get(name), idleTime, idleTime, idlePose);
            DragonAnimationSampler.sample(walkTracks.get(name), walkTime, walkTime, walkPose);
            DragonAnimationSampler.sample(runTracks.get(name), runTime, runTime, runPose);
            BoneSnapshot initial = bone.getInitialSnapshot();
            mix(walkPose, runPose, runningWeight, mixedPose, initial);
            mix(idlePose, mixedPose, movingWeight, mixedPose, initial);
            if (fallWeight > 0.0F) {
                DragonAnimationSampler.sample(fallTracks.get(name), fallTime, fallTime, fallPose);
                mix(mixedPose, fallPose, fallWeight, mixedPose, initial);
            }
            if (landingWeight > 0.0F) {
                DragonAnimationSampler.sample(lightLandingTracks.get(name), lightTime, lightTime, lightLandingPose);
                DragonAnimationSampler.sample(heavyLandingTracks.get(name), heavyTime, heavyTime, heavyLandingPose);
                mix(lightLandingPose, heavyLandingPose, air.heavyShare(), landingPose, initial);
                // Let moving feet retain their stride, with more leg authority for a heavy impact.
                float legWeight = name.equals("leftleg") || name.equals("rightleg")
                        ? Mth.lerp(movingWeight, 1.0F, air.movingLegInfluence()) : 1.0F;
                mix(mixedPose, landingPose, landingWeight * legWeight, mixedPose, initial);
            }
            if (entryWeight < 1.0F) mix(entryPose.get(name), mixedPose, entryWeight, mixedPose, initial);
            BoneAnimationQueue queue = getBoneAnimationQueues().computeIfAbsent(name, ignored -> new BoneAnimationQueue(bone));
            writePose(queue, mixedPose, gaitChannels.get(name));
        }
    }

    @Override
    protected void onPlaybackTick(CoreGeoModel<IvyTheDragonMerchant> model, AnimationState<IvyTheDragonMerchant> state) {
        if (!blendActive()) clearEntry();
    }

    private boolean blendActive() {
        return animatable.isLocomotionBlendActive() && !isPlayingTriggeredAnimation()
                && getAnimationState() != State.STOPPED;
    }

    private boolean loadClips(CoreGeoModel<IvyTheDragonMerchant> model) {
        Animation nextIdle = model.getAnimation(animatable, PREFIX + "idle");
        Animation nextWalk = model.getAnimation(animatable, PREFIX + "walk");
        Animation nextRun = model.getAnimation(animatable, PREFIX + "run");
        Animation nextFall = model.getAnimation(animatable, PREFIX + "falling");
        Animation nextLight = model.getAnimation(animatable, PREFIX + "land_light");
        Animation nextHeavy = model.getAnimation(animatable, PREFIX + "land_heavy");
        if (nextIdle == null || nextWalk == null || nextRun == null) return false;
        if (idle != nextIdle || walk != nextWalk || run != nextRun
                || falling != nextFall || landLight != nextLight || landHeavy != nextHeavy) {
            idle = nextIdle;
            walk = nextWalk;
            run = nextRun;
            falling = nextFall;
            landLight = nextLight;
            landHeavy = nextHeavy;
            gaitBones.clear();
            gaitChannels.clear();
            index(idle, idleTracks);
            index(walk, walkTracks);
            index(run, runTracks);
            index(falling, fallTracks);
            index(landLight, lightLandingTracks);
            index(landHeavy, heavyLandingTracks);
            clearEntry();
        }
        return true;
    }

    private void index(Animation animation, Map<String, BoneAnimation> tracks) {
        tracks.clear();
        if (animation == null) return;
        for (BoneAnimation track : animation.boneAnimations()) {
            tracks.put(track.boneName(), track);
            gaitBones.add(track.boneName());
            int channels = (track.rotationKeyFrames().xKeyframes().isEmpty() ? 0 : ROTATION)
                    | (track.positionKeyFrames().xKeyframes().isEmpty() ? 0 : POSITION)
                    | (track.scaleKeyFrames().xKeyframes().isEmpty() ? 0 : SCALE);
            gaitChannels.merge(track.boneName(), channels, (first, second) -> first | second);
        }
    }

    private void clearEntry() {
        entryTime = lastSeekTime = Double.NaN;
        entryPose.clear();
    }

    private void captureEntryPose(Map<String, CoreGeoBone> bones, Map<String, BoneSnapshot> snapshots) {
        entryPose.clear();
        for (String name : gaitBones) {
            CoreGeoBone bone = bones.get(name);
            if (bone == null) continue;
            BoneSnapshot initial = bone.getInitialSnapshot();
            BoneSnapshot pose = snapshots.getOrDefault(name, initial);
            entryPose.put(name, new float[] {
                    pose.getRotX() - initial.getRotX(), pose.getRotY() - initial.getRotY(), pose.getRotZ() - initial.getRotZ(),
                    pose.getOffsetX(), pose.getOffsetY(), pose.getOffsetZ(), pose.getScaleX(), pose.getScaleY(), pose.getScaleZ()});
        }
    }

    private void mix(float[] from, float[] to, float weight, float[] result, BoneSnapshot initial) {
        if (weight <= 0.0F || weight >= 1.0F) {
            System.arraycopy(weight <= 0.0F ? from : to, 0, result, 0, result.length);
            return;
        }
        rotation.rotationZYX(from[2] + initial.getRotZ(), from[1] + initial.getRotY(), from[0] + initial.getRotX());
        otherRotation.rotationZYX(to[2] + initial.getRotZ(), to[1] + initial.getRotY(), to[0] + initial.getRotX());
        rotation.slerp(otherRotation, weight).getEulerAnglesZYX(angles);
        result[0] = angles.x - initial.getRotX();
        result[1] = angles.y - initial.getRotY();
        result[2] = angles.z - initial.getRotZ();
        for (int i = 3; i < result.length; i++) result[i] = Mth.lerp(weight, from[i], to[i]);
    }

    private static void writePose(BoneAnimationQueue queue, float[] pose, int channels) {
        queue.rotationXQueue().clear();
        queue.rotationYQueue().clear();
        queue.rotationZQueue().clear();
        queue.positionXQueue().clear();
        queue.positionYQueue().clear();
        queue.positionZQueue().clear();
        queue.scaleXQueue().clear();
        queue.scaleYQueue().clear();
        queue.scaleZQueue().clear();
        if ((channels & ROTATION) != 0) {
            queue.addRotationXPoint(null, 1, 1, pose[0], pose[0]);
            queue.addRotationYPoint(null, 1, 1, pose[1], pose[1]);
            queue.addRotationZPoint(null, 1, 1, pose[2], pose[2]);
        }
        if ((channels & POSITION) != 0) {
            queue.addPosXPoint(null, 1, 1, pose[3], pose[3]);
            queue.addPosYPoint(null, 1, 1, pose[4], pose[4]);
            queue.addPosZPoint(null, 1, 1, pose[5], pose[5]);
        }
        if ((channels & SCALE) != 0) {
            queue.addScaleXPoint(null, 1, 1, pose[6], pose[6]);
            queue.addScaleYPoint(null, 1, 1, pose[7], pose[7]);
            queue.addScaleZPoint(null, 1, 1, pose[8], pose[8]);
        }
    }

    private static double contactTick(double phase, double length, double rightSeconds, double leftSeconds) {
        if (length <= 0.0D) return 0.0D;
        double rightTick = Mth.clamp(rightSeconds * 20.0D, 0.0D, length);
        double leftTick = Mth.clamp(leftSeconds * 20.0D, rightTick, length);
        double fraction = phase - Math.floor(phase);
        double firstStepTicks = leftTick - rightTick;
        double sourceTick = fraction < 0.5D
                ? rightTick + fraction * 2.0D * firstStepTicks
                : leftTick + (fraction - 0.5D) * 2.0D * (length - firstStepTicks);
        return sourceTick % length;
    }
}

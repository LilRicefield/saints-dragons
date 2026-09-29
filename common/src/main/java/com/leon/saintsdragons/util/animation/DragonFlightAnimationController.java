package com.leon.saintsdragons.util.animation;

import com.leon.saintsdragons.server.entity.base.RideableFlyingDragon;
import com.leon.saintsdragons.server.flight.DragonFlightAnimationProfile;
import com.leon.saintsdragons.server.flight.DragonFlightBlend;
import net.minecraft.util.Mth;
import org.joml.Quaternionf;
import org.joml.Vector3f;
import software.bernie.geckolib.core.animatable.model.CoreGeoBone;
import software.bernie.geckolib.core.animatable.model.CoreGeoModel;
import software.bernie.geckolib.core.animation.Animation;
import software.bernie.geckolib.core.animation.AnimationController;
import software.bernie.geckolib.core.animation.AnimationState;
import software.bernie.geckolib.core.keyframe.BoneAnimation;
import software.bernie.geckolib.core.keyframe.BoneAnimationQueue;
import software.bernie.geckolib.core.state.BoneSnapshot;

import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;

/** Blends only the flight layer, before the model's procedural neck, tail and dive offsets */
public final class DragonFlightAnimationController<T extends RideableFlyingDragon> extends AnimationController<T> {
    private static final int ROTATION = 1;
    private static final int POSITION = 2;
    private static final int SCALE = 4;
    private Animation glide;
    private Animation flap;
    private Animation sprint;
    private Animation idle;
    private DragonFlightAnimationProfile profile;
    private final Map<String, BoneAnimation> glideTracks = new HashMap<>();
    private final Map<String, BoneAnimation> flapTracks = new HashMap<>();
    private final Map<String, BoneAnimation> sprintTracks = new HashMap<>();
    private final Map<String, BoneAnimation> idleTracks = new HashMap<>();
    private final Set<String> flightBones = new LinkedHashSet<>();
    private final Map<String, Integer> flightChannels = new HashMap<>();
    private final Map<String, float[]> entryPose = new HashMap<>();
    private final float[] glidePose = new float[9];
    private final float[] flapPose = new float[9];
    private final float[] sprintPose = new float[9];
    private final float[] idlePose = new float[9];
    private final float[] mixedPose = new float[9];
    private final Quaternionf rotation = new Quaternionf();
    private final Quaternionf otherRotation = new Quaternionf();
    private final Vector3f angles = new Vector3f();
    private double entryTime = Double.NaN;
    private double lastSeekTime = Double.NaN;
    private double lastSoundPhase = Double.NaN;
    private long lastSoundCycle = Long.MIN_VALUE;
    private double flapSoundPhase;
    private double sprintSoundPhase;
    private double idleSoundPhase;

    public DragonFlightAnimationController(T dragon, int transitionTicks,
                                          AnimationStateHandler<T> predicate) {
        super(dragon, AnimationHelper.FLIGHT_CONTROLLER, Math.max(0, transitionTicks), state -> {
            state.getController().transitionLength(Math.max(0, transitionTicks));
            return predicate.handle(state);
        });
    }

    @Override
    public void process(CoreGeoModel<T> model, AnimationState<T> state,
                        Map<String, CoreGeoBone> bones, Map<String, BoneSnapshot> snapshots,
                        double seekTime, boolean crashWhenCantFindBone) {
        super.process(model, state, bones, snapshots, seekTime, crashWhenCantFindBone);
        DragonFlightAnimationProfile nextProfile = animatable.getFlightAnimationProfile();
        if (nextProfile != profile) {
            profile = nextProfile;
            glide = null; // Rebuild clip timing too, even when the animation resources are unchanged.
            entryTime = Double.NaN;
        }
        if (profile == null || !animatable.isFlightBlendActive() || isPlayingTriggeredAnimation()
                || getAnimationState() == State.STOPPED || !loadClips(model)) {
            entryTime = lastSeekTime = lastSoundPhase = Double.NaN;
            entryPose.clear();
            return;
        }
        if (Double.isNaN(entryTime) || seekTime < lastSeekTime || seekTime - lastSeekTime > 10.0D) {
            entryTime = seekTime;
            captureEntryPose(bones, snapshots);
            lastSoundPhase = Double.NaN;
            lastSoundCycle = Long.MIN_VALUE;
        }
        lastSeekTime = seekTime;
        float entryWeight = profile.entryTicks() <= 0 ? 1.0F
                : Mth.clamp((float) ((seekTime - entryTime) / profile.entryTicks()), 0.0F, 1.0F);
        float partialTick = state.getPartialTick();
        DragonFlightBlend blend = animatable.getFlightBlend();
        float flapWeight = blend.flap(partialTick);
        float sprintWeight = blend.sprint(partialTick);
        float idleWeight = blend.idle(partialTick);
        float poweredWeight = Mth.clamp(flapWeight + sprintWeight, 0.0F, 1.0F);
        float sprintShare = poweredWeight > 0.0001F ? sprintWeight / poweredWeight : 0.0F;
        // Dive suppresses the tick-driven flap target, not the current rendered weight.
        // The model scales its existing dive offset by the same gradually increasing glide authority.
        double phase = blend.phase(partialTick);
        double flapSourceTime = profile.clips().flap().sourceTick(phase, flap.length());
        double sprintSourceTime = profile.clips().sprint().sourceTick(phase, sprint.length());
        double idleSourceTime = profile.clips().idle().sourceTick(phase, idle.length());
        double flapTime = loopTick(flapSourceTime, flap.length());
        double sprintTime = loopTick(sprintSourceTime, sprint.length());
        double idleTime = loopTick(idleSourceTime, idle.length());

        // Replace this controller's channels without taking ownership of unauthored channels.
        for (String name : flightBones) {
            CoreGeoBone bone = bones.get(name);
            if (bone == null) continue;
            DragonAnimationSampler.sample(glideTracks.get(name), loopTick(seekTime, glide.length()), seekTime, glidePose);
            DragonAnimationSampler.sample(flapTracks.get(name), flapTime,
                    profile.clips().flap().continuousMolang() ? flapSourceTime : flapTime, flapPose);
            DragonAnimationSampler.sample(sprintTracks.get(name), sprintTime,
                    profile.clips().sprint().continuousMolang() ? sprintSourceTime : sprintTime, sprintPose);
            DragonAnimationSampler.sample(idleTracks.get(name), idleTime,
                    profile.clips().idle().continuousMolang() ? idleSourceTime : idleTime, idlePose);
            BoneSnapshot initial = bone.getInitialSnapshot();
            mix(flapPose, sprintPose, sprintShare, mixedPose, initial);
            mix(glidePose, mixedPose, poweredWeight, mixedPose, initial);
            mix(mixedPose, idlePose, idleWeight, mixedPose, initial);
            if (entryWeight < 1.0F) mix(entryPose.get(name), mixedPose, entryWeight, mixedPose, initial);
            BoneAnimationQueue queue = getBoneAnimationQueues().computeIfAbsent(name, ignored -> new BoneAnimationQueue(bone));
            writePose(queue, mixedPose, flightChannels.get(name));
        }
        float audibleWeight = poweredWeight * (1.0F - idleWeight) + idleWeight;
        float idleSoundShare = audibleWeight > 0.0001F ? idleWeight / audibleWeight : 0.0F;
        playWingbeat(phase, audibleWeight * entryWeight, sprintShare, idleSoundShare);
    }

    private boolean loadClips(CoreGeoModel<T> model) {
        Animation nextGlide = model.getAnimation(animatable, profile.clips().glide());
        Animation nextFlap = model.getAnimation(animatable, profile.clips().flap().name());
        Animation nextSprint = model.getAnimation(animatable, profile.clips().sprint().name());
        Animation nextIdle = model.getAnimation(animatable, profile.clips().idle().name());
        if (nextGlide == null || nextFlap == null || nextSprint == null || nextIdle == null) return false;
        if (nextGlide != glide || nextFlap != flap || nextSprint != sprint || nextIdle != idle) {
            glide = nextGlide;
            flap = nextFlap;
            sprint = nextSprint;
            idle = nextIdle;
            flightBones.clear();
            flightChannels.clear();
            index(glide, glideTracks);
            index(flap, flapTracks);
            index(sprint, sprintTracks);
            index(idle, idleTracks);
            flapSoundPhase = soundPhase(flap, profile.clips().flap(), profile.clips().wingbeatSound());
            sprintSoundPhase = soundPhase(sprint, profile.clips().sprint(), profile.clips().wingbeatSound());
            idleSoundPhase = soundPhase(idle, profile.clips().idle(), profile.clips().wingbeatSound());
            entryTime = Double.NaN;
        }
        return true;
    }

    private void index(Animation animation, Map<String, BoneAnimation> tracks) {
        tracks.clear();
        for (BoneAnimation track : animation.boneAnimations()) {
            tracks.put(track.boneName(), track);
            flightBones.add(track.boneName());
            int channels = (track.rotationKeyFrames().xKeyframes().isEmpty() ? 0 : ROTATION)
                    | (track.positionKeyFrames().xKeyframes().isEmpty() ? 0 : POSITION)
                    | (track.scaleKeyFrames().xKeyframes().isEmpty() ? 0 : SCALE);
            flightChannels.merge(track.boneName(), channels, (first, second) -> first | second);
        }
    }

    private void captureEntryPose(Map<String, CoreGeoBone> bones, Map<String, BoneSnapshot> snapshots) {
        entryPose.clear();
        for (String name : flightBones) {
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
        // Z/Y/X matches GeckoLib's bone transform order. Slerp avoids Euler wrap and twisting artifacts.
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

    private void playWingbeat(double phase, float weight, float sprintShare, float idleShare) {
        double eventPhase = Mth.lerp(idleShare,
                Mth.lerp(sprintShare, flapSoundPhase, sprintSoundPhase), idleSoundPhase);
        long cycle = (long) Math.floor(phase - eventPhase);
        if (!Double.isNaN(lastSoundPhase) && phase >= lastSoundPhase && phase - lastSoundPhase < 0.5D
                && cycle > lastSoundCycle) {
            if (weight >= profile.minimumAudibleWingbeatWeight()) {
                animatable.playAnimationKeyframeSound(profile.clips().wingbeatSound());
            }
        }
        // Changing sprint or idle weights moves the sound marker; never emit twice for the same stroke.
        lastSoundCycle = Math.max(lastSoundCycle, cycle);
        lastSoundPhase = phase;
    }

    private static double loopTick(double tick, double length) {
        return length > 0.0D ? tick % length : tick;
    }

    private static double soundPhase(Animation animation, DragonFlightAnimationProfile.Clip clip, String soundKey) {
        double firstPhase = Double.NaN;
        double x = 0.0D;
        double y = 0.0D;
        // Long procedural clips contain several hand-placed flap effects. Average their stroke phases
        // around the circle so small placement differences (including across phase zero) do not bias timing.
        for (var sound : animation.keyFrames().sounds()) {
            // Species sound profiles also accept numbered flap variants (for example raevyx_flap1).
            if (!sound.getSound().startsWith(soundKey)) continue;
            double phase = clip.soundPhase(sound.getStartTick(), animation.length());
            if (Double.isNaN(firstPhase)) firstPhase = phase;
            x += Math.cos(phase * Math.PI * 2.0D);
            y += Math.sin(phase * Math.PI * 2.0D);
        }
        if (Double.isNaN(firstPhase)) return 0.1D;
        if (x * x + y * y < 1.0E-8D) return firstPhase;
        double mean = Math.atan2(y, x) / (Math.PI * 2.0D);
        return mean - Math.floor(mean);
    }
}

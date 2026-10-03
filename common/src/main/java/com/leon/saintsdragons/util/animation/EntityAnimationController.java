package com.leon.saintsdragons.util.animation;

import net.minecraft.world.entity.LivingEntity;
import software.bernie.geckolib.core.animatable.model.CoreGeoBone;
import software.bernie.geckolib.core.animatable.model.CoreGeoModel;
import software.bernie.geckolib.core.animation.AnimationController;
import software.bernie.geckolib.core.animation.AnimationState;
import software.bernie.geckolib.core.state.BoneSnapshot;

import java.util.Map;

/** Keeps GeckoLib's playback state alive on client ticks without evaluating a hidden skeleton */
public class EntityAnimationController<T extends LivingEntity & TickingGeoEntity> extends AnimationController<T> {
    private int lastPlaybackTick = Integer.MIN_VALUE;
    private Object animationResource;
    private boolean needsRenderSnapshots;

    public EntityAnimationController(T entity, String name, int transitionTicks,
                                     AnimationStateHandler<T> predicate) {
        super(entity, name, transitionTicks, predicate);
    }

    public final void tickPlayback(CoreGeoModel<T> model, AnimationState<T> state, Object resource) {
        if (!animatable.level().isClientSide || lastPlaybackTick == animatable.tickCount) {
            return;
        }
        lastPlaybackTick = animatable.tickCount;
        if (animationResource != null && animationResource != resource) {
            forceAnimationReset();
        }
        animationResource = resource;
        var previousAnimation = currentAnimation;
        isJustStarting = false;

        // In GeckoLib 4.8.4 an empty bone map skips track sampling and bone queue allocation,
        // while still advancing predicates, transitions, stages, loops and event keyframes.
        // Call the base implementation directly: flight blending only runs when posing a model.
        super.process(model, state.withController(this), Map.of(), Map.of(), state.animationTick, false);
        if (currentAnimation != previousAnimation) {
            needsRenderSnapshots = true;
        }
        onPlaybackTick(model, state);
    }

    protected void onPlaybackTick(CoreGeoModel<T> model, AnimationState<T> state) {
    }

    @Override
    public void process(CoreGeoModel<T> model, AnimationState<T> state,
                        Map<String, CoreGeoBone> bones, Map<String, BoneSnapshot> snapshots,
                        double seekTime, boolean crashWhenCantFindBone) {
        // The manager's first *render* is not necessarily the controller's first playback tick.
        // Letting GeckoLib treat it as such would poll an already-consumed animation queue again.
        if (lastPlaybackTick != Integer.MIN_VALUE) {
            isJustStarting = false;
        }
        if (needsRenderSnapshots) {
            needsRenderSnapshots = false;
            if (animationState == State.TRANSITIONING && currentAnimation != null) {
                boneSnapshots.clear();
                for (var track : currentAnimation.animation().boneAnimations()) {
                    BoneSnapshot snapshot = snapshots.get(track.boneName());
                    if (snapshot != null) {
                        boneSnapshots.put(track.boneName(), BoneSnapshot.copy(snapshot));
                    }
                }
            }
        }
        super.process(model, state, bones, snapshots, seekTime, crashWhenCantFindBone);
    }
}

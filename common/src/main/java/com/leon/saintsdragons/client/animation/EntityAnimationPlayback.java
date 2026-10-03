package com.leon.saintsdragons.client.animation;

import com.leon.saintsdragons.util.animation.EntityAnimationController;
import com.leon.saintsdragons.util.animation.TickingGeoEntity;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.LivingEntity;
import software.bernie.geckolib.cache.GeckoLibCache;
import software.bernie.geckolib.constant.DataTickets;
import software.bernie.geckolib.core.animation.AnimatableManager;
import software.bernie.geckolib.core.animation.AnimationState;
import software.bernie.geckolib.core.object.DataTicket;
import software.bernie.geckolib.model.data.EntityModelData;
import software.bernie.geckolib.renderer.GeoEntityRenderer;

/** Client-only playback */
public final class EntityAnimationPlayback {
    private static final DataTicket<Double> LAST_AGED_POSE =
            new DataTicket<>("saintsdragons:last_aged_pose", Double.class);

    private EntityAnimationPlayback() {
    }

    public static void tick(Minecraft minecraft) {
        if (minecraft.level == null || minecraft.isPaused()) {
            return;
        }
        for (var entity : minecraft.level.entitiesForRendering()) {
            if (!(entity instanceof LivingEntity) || !(entity instanceof TickingGeoEntity) || entity.isRemoved()) {
                continue;
            }
            EntityRenderer<?> renderer = minecraft.getEntityRenderDispatcher().getRenderer(entity);
            if (renderer instanceof GeoEntityRenderer<?> geoRenderer) {
                tickEntity((LivingEntity & TickingGeoEntity) entity, geoRenderer);
            }
        }
    }

    @SuppressWarnings("unchecked")
    private static <T extends LivingEntity & TickingGeoEntity> void tickEntity(T entity, GeoEntityRenderer<?> untypedRenderer) {
        GeoEntityRenderer<T> renderer = (GeoEntityRenderer<T>) untypedRenderer;
        var model = renderer.getGeoModel();
        var animations = GeckoLibCache.getBakedAnimations().get(model.getAnimationResource(entity));
        if (animations == null) {
            return; // resources have not finished loading yet
        }
        boolean sitting = entity.isPassenger();
        float bodyYaw = entity.yBodyRotO;
        float headYaw = entity.yHeadRotO;
        if (sitting && entity.getVehicle() instanceof LivingEntity vehicle) {
            bodyYaw = vehicle.yBodyRotO;
            float relativeYaw = Mth.clamp(Mth.wrapDegrees(headYaw - bodyYaw), -85, 85);
            bodyYaw = headYaw - relativeYaw;
            if (relativeYaw * relativeYaw > 2500) {
                bodyYaw += relativeYaw * 0.2F;
            }
        }
        float limbAmount = !sitting && entity.isAlive() ? Math.min(entity.walkAnimation.speed(0), 1) : 0;
        float limbSwing = !sitting && entity.isAlive() ? entity.walkAnimation.position(0) : 0;
        if (entity.isBaby()) {
            limbSwing *= 3;
        }
        var velocity = entity.getDeltaMovement();
        float speed = (float) ((Math.abs(velocity.x) + Math.abs(velocity.z)) / 2);
        AnimationState<T> state = new AnimationState<>(entity, limbSwing, limbAmount, 0,
                speed >= renderer.getMotionAnimThreshold(entity) && limbAmount != 0);
        state.animationTick = entity.getTick(entity);
        state.setData(DataTickets.TICK, state.animationTick);
        state.setData(DataTickets.ENTITY, entity);
        state.setData(DataTickets.ENTITY_MODEL_DATA,
                new EntityModelData(sitting, entity.isBaby(), bodyYaw - headYaw, -entity.xRotO));
        long instanceId = renderer.getInstanceId(entity);
        model.addAdditionalStateData(entity, instanceId, state::setData);
        var manager = entity.getAnimatableInstanceCache().<T>getManagerForId(instanceId);
        ageLastPose(manager, state.animationTick);
        for (var controller : manager.getAnimationControllers().values()) {
            if (controller instanceof EntityAnimationController<T> playback) {
                playback.tickPlayback(model, state, animations);
            }
        }
    }

    public static void ageLastPose(AnimatableManager<?> manager, double tick) {
        double lastPoseTick = manager.getLastUpdateTime();
        Double agedPoseTick = manager.getData(LAST_AGED_POSE);
        if (manager.isFirstTick() || tick - lastPoseTick <= 2
                || (agedPoseTick != null && agedPoseTick == lastPoseTick)) {
            return;
        }
        // Do this once after rendering stops. Expired channels can then return to their
        // rest pose immediately on re-entry. Active channels are overwritten by rendering.
        for (var snapshot : manager.getBoneSnapshotCollection().values()) {
            if (snapshot.isRotAnimInProgress()) snapshot.stopRotAnim(lastPoseTick);
            if (snapshot.isPosAnimInProgress()) snapshot.stopPosAnim(lastPoseTick);
            if (snapshot.isScaleAnimInProgress()) snapshot.stopScaleAnim(lastPoseTick);
        }
        manager.setData(LAST_AGED_POSE, lastPoseTick);
    }
}

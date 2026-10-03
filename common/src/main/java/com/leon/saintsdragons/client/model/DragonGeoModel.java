package com.leon.saintsdragons.client.model;

import com.leon.saintsdragons.common.SaintsDragonsCommon;
import com.leon.saintsdragons.server.entity.base.DragonEntity;
import com.leon.saintsdragons.server.entity.base.RideableDragonBase;
import com.leon.saintsdragons.client.renderer.DragonRiderAttachments;
import com.leon.saintsdragons.client.renderer.ShaderPassCompatibility;
import net.minecraft.resources.ResourceLocation;
import software.bernie.geckolib.cache.object.BakedGeoModel;
import software.bernie.geckolib.core.animation.AnimationState;
import software.bernie.geckolib.model.DefaultedEntityGeoModel;

public abstract class DragonGeoModel<T extends DragonEntity> extends DefaultedEntityGeoModel<T> {
    protected final ResourceLocation model;
    protected final ResourceLocation babyModel;
    protected final ResourceLocation animation;
    protected final ResourceLocation babyAnimation;
    protected final ResourceLocation maleTexture;
    protected final ResourceLocation femaleTexture;
    protected final ResourceLocation babyMaleTexture;
    protected final ResourceLocation babyFemaleTexture;

    private final DragonBonePose displacedPose = new DragonBonePose();
    private boolean hasDisplacedPose;
    private BakedGeoModel attachmentModel;

    @Override
    public BakedGeoModel getBakedModel(ResourceLocation resource) {
        attachmentModel = super.getBakedModel(resource);
        return attachmentModel;
    }

    @Override
    public void handleAnimations(T entity, long instanceId, AnimationState<T> state) {
        RideableDragonBase dragon = entity instanceof RideableDragonBase rideable ? rideable : null;
        boolean cacheable = dragon != null && DragonRiderAttachments.usesPoseCache(dragon);
        var baked = attachmentModel;
        if (cacheable && DragonRiderAttachments.hasAnimation(dragon, state.getPartialTick(), baked)) {
            // GeckoLib tracks the last evaluated instance internally. Restore its original bones
            // before the next real evaluation, even when several dragons share this model.
            if (!hasDisplacedPose) {
                displacedPose.capture(getAnimationProcessor().getRegisteredBones());
                hasDisplacedPose = true;
            }
            DragonRiderAttachments.restoreAnimation(dragon, state.getPartialTick());
            return;
        }
        if (hasDisplacedPose) {
            displacedPose.restore();
            displacedPose.resetChanges();
            hasDisplacedPose = false;
        }
        super.handleAnimations(entity, instanceId, state);
        if (cacheable && !ShaderPassCompatibility.isIrisShadowPass()) {
            DragonRiderAttachments.captureAnimation(dragon, state.getPartialTick(), baked,
                    getAnimationProcessor().getRegisteredBones());
        }
    }

    protected DragonGeoModel(String dragonId) {
        this(dragonId, true);
    }

    protected DragonGeoModel(String dragonId, boolean hasBabyResources) {
        this(SaintsDragonsCommon.rl(dragonId), hasBabyResources);
    }

    protected DragonGeoModel(ResourceLocation dragonId) {
        this(dragonId, true);
    }

    protected DragonGeoModel(ResourceLocation dragonId, boolean hasBabyResources) {
        this(dragonId, Resources.conventional(dragonId, hasBabyResources));
    }

    protected DragonGeoModel(ResourceLocation dragonId, Resources resources) {
        super(dragonId);
        this.model = resources.model();
        this.babyModel = resources.babyModel();
        this.animation = resources.animation();
        this.babyAnimation = resources.babyAnimation();
        this.maleTexture = resources.maleTexture();
        this.femaleTexture = resources.femaleTexture();
        this.babyMaleTexture = resources.babyMaleTexture();
        this.babyFemaleTexture = resources.babyFemaleTexture();
    }

    public record Resources(ResourceLocation model, ResourceLocation babyModel,
                            ResourceLocation animation, ResourceLocation babyAnimation,
                            ResourceLocation maleTexture, ResourceLocation femaleTexture,
                            ResourceLocation babyMaleTexture, ResourceLocation babyFemaleTexture) {
        public Resources {
            java.util.Objects.requireNonNull(model, "model");
            java.util.Objects.requireNonNull(babyModel, "babyModel");
            java.util.Objects.requireNonNull(animation, "animation");
            java.util.Objects.requireNonNull(babyAnimation, "babyAnimation");
            java.util.Objects.requireNonNull(maleTexture, "maleTexture");
            java.util.Objects.requireNonNull(femaleTexture, "femaleTexture");
            java.util.Objects.requireNonNull(babyMaleTexture, "babyMaleTexture");
            java.util.Objects.requireNonNull(babyFemaleTexture, "babyFemaleTexture");
        }

        public static Resources conventional(ResourceLocation id, boolean babies) {
            String path = id.getPath();
            String name = path.substring(path.lastIndexOf('/') + 1);
            ResourceLocation model = resource(id, "geo/entity/" + path + "/" + name + ".geo.json");
            ResourceLocation animation = resource(id, "animations/entity/" + path + "/" + name + ".animation.json");
            ResourceLocation male = resource(id, "textures/entity/" + path + "/" + name + ".png");
            ResourceLocation female = resource(id, "textures/entity/" + path + "/" + name + "_female.png");
            return new Resources(model,
                    babies ? resource(id, "geo/entity/" + path + "/baby_" + name + ".geo.json") : model,
                    animation,
                    babies ? resource(id, "animations/entity/" + path + "/baby_" + name + ".animation.json") : animation,
                    male, female,
                    babies ? resource(id, "textures/entity/" + path + "/baby_" + name + ".png") : male,
                    babies ? resource(id, "textures/entity/" + path + "/baby_" + name + "_female.png") : female);
        }

        private static ResourceLocation resource(ResourceLocation id, String path) {
            return new ResourceLocation(id.getNamespace(), path);
        }
    }

    @Override
    public ResourceLocation getModelResource(T entity) {
        return entity != null && entity.isBaby() ? babyModel : model;
    }

    @Override
    public ResourceLocation getTextureResource(T entity) {
        if (entity == null) {
            return maleTexture;
        }
        if (entity.isBaby()) {
            return getBabyTexture(entity);
        }
        return getAdultTexture(entity);
    }

    @Override
    public ResourceLocation getAnimationResource(T entity) {
        return entity != null && entity.isBaby() ? babyAnimation : animation;
    }

    protected ResourceLocation getAdultTexture(T entity) {
        if (entity.hasCustomTextureVariant()) {
            return entity.getCustomAdultTextureResource(entity.isFemale());
        }
        return entity.isFemale() ? femaleTexture : maleTexture;
    }

    protected ResourceLocation getBabyTexture(T entity) {
        return entity.isFemale() ? babyFemaleTexture : babyMaleTexture;
    }
}

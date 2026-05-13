package com.leon.saintsdragons.client.model;

import com.leon.saintsdragons.common.SaintsDragonsCommon;
import com.leon.saintsdragons.server.entity.base.DragonEntity;
import net.minecraft.resources.ResourceLocation;
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

    protected DragonGeoModel(String dragonId) {
        this(dragonId, true);
    }

    protected DragonGeoModel(String dragonId, boolean hasBabyResources) {
        super(SaintsDragonsCommon.rl(dragonId));
        this.model = SaintsDragonsCommon.rl("geo/entity/" + dragonId + ".geo.json");
        this.animation = SaintsDragonsCommon.rl("animations/entity/" + dragonId + ".animation.json");
        this.maleTexture = SaintsDragonsCommon.rl("textures/entity/" + dragonId + "/" + dragonId + ".png");
        this.femaleTexture = SaintsDragonsCommon.rl("textures/entity/" + dragonId + "/" + dragonId + "_female.png");
        if (hasBabyResources) {
            this.babyModel = SaintsDragonsCommon.rl("geo/entity/baby_" + dragonId + ".geo.json");
            this.babyAnimation = SaintsDragonsCommon.rl("animations/entity/baby_" + dragonId + ".animation.json");
            this.babyMaleTexture = SaintsDragonsCommon.rl("textures/entity/" + dragonId + "/baby_" + dragonId + ".png");
            this.babyFemaleTexture = SaintsDragonsCommon.rl("textures/entity/" + dragonId + "/baby_" + dragonId + "_female.png");
        } else {
            this.babyModel = this.model;
            this.babyAnimation = this.animation;
            this.babyMaleTexture = this.maleTexture;
            this.babyFemaleTexture = this.femaleTexture;
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
        return entity.isFemale() ? femaleTexture : maleTexture;
    }

    protected ResourceLocation getBabyTexture(T entity) {
        return entity.isFemale() ? babyFemaleTexture : babyMaleTexture;
    }
}

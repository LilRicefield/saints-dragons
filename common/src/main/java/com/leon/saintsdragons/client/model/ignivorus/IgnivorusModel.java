package com.leon.saintsdragons.client.model.ignivorus;

import com.leon.saintsdragons.client.model.DragonGeoModel;
import com.leon.saintsdragons.client.ui.DraconicCodexScreen;
import com.leon.saintsdragons.common.SaintsDragonsCommon;
import com.leon.saintsdragons.server.entity.dragons.ignivorus.Ignivorus;
import com.leon.saintsdragons.server.entity.part.IgnivorusPoseOffsets;
import net.minecraft.resources.ResourceLocation;
import software.bernie.geckolib.constant.DataTickets;
import software.bernie.geckolib.core.animation.AnimationState;
import software.bernie.geckolib.model.data.EntityModelData;

public class IgnivorusModel extends DragonGeoModel<Ignivorus> {
    public IgnivorusModel() {
        super("ignivorus");
    }

    private static final ResourceLocation CRIMSON_TEXTURE = SaintsDragonsCommon.rl("textures/entity/ignivorus/crimson_ignivorus.png");
    private static final ResourceLocation CRIMSON_FEMALE_TEXTURE = SaintsDragonsCommon.rl("textures/entity/ignivorus/crimson_ignivorus_female.png");

    @Override
    protected ResourceLocation getAdultTexture(Ignivorus entity) {
        if (entity.hasCustomTextureVariant()) {
            return super.getAdultTexture(entity);
        }
        if (entity.getTextureVariant() == Ignivorus.VARIANT_CRIMSON) {
            return entity.isFemale() ? CRIMSON_FEMALE_TEXTURE : CRIMSON_TEXTURE;
        }
        return super.getAdultTexture(entity);
    }

    @Override
    public void setCustomAnimations(Ignivorus entity, long instanceId, AnimationState<Ignivorus> animationState) {
        super.setCustomAnimations(entity, instanceId, animationState);

        if (DraconicCodexScreen.RENDERING_IN_GUI.get()) {
            return;
        }
        if (entity.isScentAssessing()) {
            return;
        }
        EntityModelData modelData = animationState.getData(DataTickets.ENTITY_MODEL_DATA);
        if (modelData == null) return;

        float partialTick = animationState.getPartialTick();
        if (entity.getSkyfallElapsedTicks(partialTick) >= 0.0F) {
            return;
        }

        if (entity.isAlive()) {
            if (entity.isDeadOrDying()){
                return;
            }
            IgnivorusPoseOffsets.apply(entity, partialTick,
                    modelData.headPitch(), modelData.netHeadYaw(), (name, axis, rotation, fromInitial) -> {
                        getBone(name).ifPresent(bone -> {
                            var initial = bone.getInitialSnapshot();
                            if (axis == 0) bone.setRotX((fromInitial ? initial.getRotX() : bone.getRotX()) + rotation);
                            else if (axis == 1) bone.setRotY((fromInitial ? initial.getRotY() : bone.getRotY()) + rotation);
                            else bone.setRotZ((fromInitial ? initial.getRotZ() : bone.getRotZ()) + rotation);
                        });
                    });
        }
    }

}

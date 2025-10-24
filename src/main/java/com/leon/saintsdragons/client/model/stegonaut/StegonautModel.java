package com.leon.saintsdragons.client.model.stegonaut;

import com.leon.saintsdragons.SaintsDragons;
import com.leon.saintsdragons.server.entity.dragons.stegonaut.Stegonaut;
import net.minecraft.resources.ResourceLocation;
import software.bernie.geckolib.model.DefaultedEntityGeoModel;

public class StegonautModel extends DefaultedEntityGeoModel<Stegonaut> {
    public StegonautModel() {
        super(SaintsDragons.rl( "stegonaut"), "head");
    }
    private static final ResourceLocation MALE_TEXTURE = ResourceLocation.fromNamespaceAndPath("saintsdragons", "textures/entity/stegonaut/stegonaut.png");
    private static final ResourceLocation FEMALE_TEXTURE = ResourceLocation.fromNamespaceAndPath("saintsdragons", "textures/entity/stegonaut/stegonaut_female.png");

    @Override
    public ResourceLocation getTextureResource(Stegonaut entity) {
        // TODO: Add baby texture variant
        return entity.isFemale() ? FEMALE_TEXTURE : MALE_TEXTURE;
    }

}

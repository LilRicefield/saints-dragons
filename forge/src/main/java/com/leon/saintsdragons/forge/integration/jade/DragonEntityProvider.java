package com.leon.saintsdragons.forge.integration.jade;

import com.leon.saintsdragons.server.entity.base.DragonEntity;
import com.leon.saintsdragons.server.entity.base.DragonGender;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import snownee.jade.api.EntityAccessor;
import snownee.jade.api.IEntityComponentProvider;
import snownee.jade.api.IServerDataProvider;
import snownee.jade.api.ITooltip;
import snownee.jade.api.config.IPluginConfig;

//jade but, you've guessed it, froge
public enum DragonEntityProvider implements IEntityComponentProvider, IServerDataProvider<EntityAccessor> {
    INSTANCE;

    @Override
    public void appendTooltip(ITooltip tooltip, EntityAccessor accessor, IPluginConfig config) {
        if (!(accessor.getEntity() instanceof DragonEntity dragon)) {
            return;
        }

        // Get data from server
        CompoundTag serverData = accessor.getServerData();

        // Display gender
        if (serverData.contains("Gender")) {
            String genderName = serverData.getString("Gender");
            Component genderComponent = Component.translatable("saintsdragons.gender." + genderName.toLowerCase());
            tooltip.add(Component.translatable("jade.saintsdragons.gender", genderComponent));
        }

        // Display variant
        if (serverData.contains("VariantName")) {
            String variantName = serverData.getString("VariantName");
            Component variantComponent = Component.translatable("saintsdragons.variant." + variantName);
            tooltip.add(Component.translatable("jade.saintsdragons.variant", variantComponent));
        }
    }

    @Override
    public void appendServerData(CompoundTag tag, EntityAccessor accessor) {
        if (!(accessor.getEntity() instanceof DragonEntity dragon)) {
            return;
        }

        // Send gender to client
        DragonGender gender = dragon.getGender();
        if (gender != null) {
            tag.putString("Gender", gender.name());
        }

        // Send variant name to client
        int variantId = dragon.getCodexTextureVariant();
        String variantName = dragon.getTextureVariantName(variantId);
        tag.putString("VariantName", variantName);
    }

    @Override
    public ResourceLocation getUid() {
        return new ResourceLocation("saintsdragons", "dragon_info");
    }
}

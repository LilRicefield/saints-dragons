package com.leon.saintsdragons.fabric.integration.jade;

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

//jade for fabcir
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

        if (serverData.contains("VariantTranslationKey")) {
            String key = serverData.getString("VariantTranslationKey");
            String fallback = serverData.contains("VariantFallbackName")
                    ? serverData.getString("VariantFallbackName")
                    : serverData.getString("VariantResourceId");
            Component variantComponent = Component.translatableWithFallback(key, fallback);
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

        ResourceLocation variantId = dragon.getCodexTextureVariantId();
        tag.putString("VariantResourceId", variantId.toString());
        tag.putString("VariantTranslationKey", dragon.getTextureVariantTranslationKey(variantId));
        tag.putString("VariantFallbackName", formatFallbackName(dragon.getTextureVariantName(variantId)));
    }

    @Override
    public ResourceLocation getUid() {
        return new ResourceLocation("saintsdragons", "dragon_info");
    }

    private static String formatFallbackName(String name) {
        if (name == null || name.isBlank()) {
            return "Default";
        }
        String[] words = name.replace('-', '_').split("_");
        StringBuilder builder = new StringBuilder();
        for (String word : words) {
            if (word.isBlank()) {
                continue;
            }
            if (builder.length() > 0) {
                builder.append(' ');
            }
            builder.append(Character.toUpperCase(word.charAt(0)));
            if (word.length() > 1) {
                builder.append(word.substring(1));
            }
        }
        return builder.length() == 0 ? name : builder.toString();
    }
}

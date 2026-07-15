package com.leon.saintsdragons.forge;

import com.leon.saintsdragons.client.init.CommonClientModEvents;
import com.leon.saintsdragons.common.SaintsDragonsCommon;
import com.leon.saintsdragons.common.registry.ModBlocks;
import com.leon.saintsdragons.common.registry.ModBlockEntities;
import com.leon.saintsdragons.common.registry.ModRecipes;
import com.leon.saintsdragons.client.model.block.DraconianNucleusModel;
import com.leon.saintsdragons.client.renderer.block.DraconianNucleusRenderer;
import com.leon.saintsdragons.client.model.block.DraconicCrucibleEntity;
import com.leon.saintsdragons.client.renderer.block.DraconicCrucibleRenderer;
import net.minecraft.client.renderer.ItemBlockRenderTypes;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.RecipeBookCategories;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.EntityRenderersEvent;
import net.minecraftforge.client.event.RegisterRecipeBookCategoriesEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.lifecycle.FMLClientSetupEvent;

@Mod.EventBusSubscriber(modid = SaintsDragonsCommon.MOD_ID, bus = Mod.EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
public final class SaintsDragonsForgeClient {

    @SubscribeEvent
    public static void onRegisterRenderers(EntityRenderersEvent.RegisterRenderers event) {
        CommonClientModEvents.registerEntityRenderers(event::registerEntityRenderer);
        event.registerBlockEntityRenderer(ModBlockEntities.DRACONIAN_NUCLEUS.get(), DraconianNucleusRenderer::new);
        event.registerBlockEntityRenderer(ModBlockEntities.DRACONIC_CRUCIBLE.get(), DraconicCrucibleRenderer::new);
    }

    @SubscribeEvent
    public static void onRegisterLayerDefinitions(EntityRenderersEvent.RegisterLayerDefinitions event) {
        event.registerLayerDefinition(DraconianNucleusModel.LAYER_LOCATION, DraconianNucleusModel::createBodyLayer);
        event.registerLayerDefinition(DraconicCrucibleEntity.LAYER_LOCATION, DraconicCrucibleEntity::createBodyLayer);
    }

    @SubscribeEvent
    public static void onRegisterRecipeBookCategories(RegisterRecipeBookCategoriesEvent event) {
        event.registerRecipeCategoryFinder(
                ModRecipes.DRACONIC_CRUCIBLE_SHAPED_TYPE.get(),
                recipe -> RecipeBookCategories.UNKNOWN);
        event.registerRecipeCategoryFinder(
                ModRecipes.DRACONIC_CRUCIBLE_SMELTING_TYPE.get(),
                recipe -> RecipeBookCategories.UNKNOWN);
    }

    @SubscribeEvent
    public static void onClientSetup(FMLClientSetupEvent event) {
        event.enqueueWork(() -> {
            CommonClientModEvents.registerMenuScreens();
            ItemBlockRenderTypes.setRenderLayer(ModBlocks.DRACONIAN_PELLUCIDA.get(), RenderType.translucent());
            ItemBlockRenderTypes.setRenderLayer(ModBlocks.DRACONIAN_NUCLEUS.get(), RenderType.translucent());
        });
    }
}

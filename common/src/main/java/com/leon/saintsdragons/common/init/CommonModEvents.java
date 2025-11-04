package com.leon.saintsdragons.common.init;

import com.leon.saintsdragons.common.registry.ModEntities;
import com.leon.saintsdragons.common.registry.ModItems;
import com.leon.saintsdragons.server.command.DragonAllyCommand;
import com.leon.saintsdragons.server.entity.dragons.cindervane.Cindervane;
import com.leon.saintsdragons.server.entity.dragons.nulljaw.Nulljaw;
import com.leon.saintsdragons.server.entity.dragons.raevyx.Raevyx;
import com.leon.saintsdragons.server.entity.dragons.stegonaut.Stegonaut;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.SpawnPlacements;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.CreativeModeTabs;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.levelgen.Heightmap;

import com.mojang.brigadier.CommandDispatcher;

import java.util.function.BiConsumer;
import java.util.function.Supplier;

/**
 * Loader-neutral wiring for gameplay events that need platform specific dispatchers.
 */
public final class CommonModEvents {
    private CommonModEvents() {
    }

    public static void registerEntityAttributes(
            BiConsumer<EntityType<? extends LivingEntity>, AttributeSupplier.Builder> registrar
    ) {
        registrar.accept(ModEntities.RAEVYX.get(), Raevyx.createAttributes());
        registrar.accept(ModEntities.STEGONAUT.get(), Stegonaut.createAttributes());
        registrar.accept(ModEntities.CINDERVANE.get(), Cindervane.createAttributes());
        registrar.accept(ModEntities.NULLJAW.get(), Nulljaw.createAttributes());
    }

    public static void registerCreativeTabEntries(CreativeTabRegistrar registrar) {
        registrar.accept(CreativeModeTabs.SPAWN_EGGS, ModItems.RAEVYX_SPAWN_EGG);
        registrar.accept(CreativeModeTabs.SPAWN_EGGS, ModItems.STEGONAUT_SPAWN_EGG);
        registrar.accept(CreativeModeTabs.SPAWN_EGGS, ModItems.CINDERVANE_SPAWN_EGG);
        registrar.accept(CreativeModeTabs.SPAWN_EGGS, ModItems.NULLJAW_SPAWN_EGG);

        registrar.accept(CreativeModeTabs.TOOLS_AND_UTILITIES, ModItems.DRAGON_ALLY_BOOK);
        registrar.accept(CreativeModeTabs.TOOLS_AND_UTILITIES, ModItems.STEGONAUT_BINDER);
        registrar.accept(CreativeModeTabs.TOOLS_AND_UTILITIES, ModItems.RAEVYX_BINDER);
        registrar.accept(CreativeModeTabs.TOOLS_AND_UTILITIES, ModItems.CINDERVANE_BINDER);
        registrar.accept(CreativeModeTabs.TOOLS_AND_UTILITIES, ModItems.NULLJAW_BINDER);
    }

    public static void registerSpawnPlacements(SpawnPlacementRegistrar registrar) {
        registrar.register(
                ModEntities.RAEVYX.get(),
                SpawnPlacements.Type.ON_GROUND,
                Heightmap.Types.MOTION_BLOCKING_NO_LEAVES,
                Raevyx::canSpawnHere
        );
        registrar.register(
                ModEntities.STEGONAUT.get(),
                SpawnPlacements.Type.ON_GROUND,
                Heightmap.Types.MOTION_BLOCKING_NO_LEAVES,
                Stegonaut::canSpawnHere
        );
        registrar.register(
                ModEntities.CINDERVANE.get(),
                SpawnPlacements.Type.ON_GROUND,
                Heightmap.Types.MOTION_BLOCKING_NO_LEAVES,
                Cindervane::canSpawnHere
        );
        registrar.register(
                ModEntities.NULLJAW.get(),
                SpawnPlacements.Type.NO_RESTRICTIONS,
                Heightmap.Types.MOTION_BLOCKING_NO_LEAVES,
                Nulljaw::canSpawnHere
        );
    }

    public static void registerCommands(CommandDispatcher<CommandSourceStack> dispatcher) {
        DragonAllyCommand.register(dispatcher);
    }

    @FunctionalInterface
    public interface CreativeTabRegistrar {
        void accept(ResourceKey<CreativeModeTab> tabKey, Supplier<? extends Item> itemSupplier);
    }

    @FunctionalInterface
    public interface SpawnPlacementRegistrar {
        <T extends Mob> void register(EntityType<T> type,
                                      SpawnPlacements.Type placementType,
                                      Heightmap.Types heightmap,
                                      SpawnPlacements.SpawnPredicate<T> predicate);
    }
}

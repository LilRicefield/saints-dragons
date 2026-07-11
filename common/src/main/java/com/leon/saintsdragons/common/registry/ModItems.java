package com.leon.saintsdragons.common.registry;

import com.leon.saintsdragons.common.item.*;
import com.leon.saintsdragons.common.item.dragonfood.HeartyDragonMealItem;
import com.leon.saintsdragons.common.item.tools.WorldrootTier;
import com.leon.saintsdragons.common.SaintsDragonsCommon;
import com.leon.saintsdragons.platform.RegistryHelper;
import com.leon.saintsdragons.platform.Services;
import net.minecraft.world.food.FoodProperties;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Rarity;
import net.minecraft.world.item.RecordItem;
import net.minecraft.world.item.AxeItem;
import net.minecraft.world.item.HoeItem;
import net.minecraft.world.item.PickaxeItem;
import net.minecraft.world.item.ShovelItem;
import net.minecraft.world.item.SwordItem;
import net.minecraft.world.level.material.Fluids;
import net.minecraft.sounds.SoundEvents;

import java.util.function.Supplier;

public class ModItems {
    public static final RegistryHelper.RegistryWrapper<Item> REGISTER =
            Services.PLATFORM.getRegistryHelper()
                    .create(Registries.ITEM, () -> BuiltInRegistries.ITEM, SaintsDragonsCommon.MOD_ID);

    public static final Supplier<Item> RAEVYX_SPAWN_EGG =
            REGISTER.register("raevyx_spawn_egg",
                    () -> Services.PLATFORM.createSpawnEgg(
                            ModEntities.RAEVYX,
                            0x000000, 0x8B0000,
                            new Item.Properties()
                    ));

    public static final Supplier<Item> STEGONAUT_SPAWN_EGG =
            REGISTER.register("stegonaut_spawn_egg",
                    () -> Services.PLATFORM.createSpawnEgg(
                            ModEntities.STEGONAUT,
                            0x9E8B70, 0x7148AC,
                            new Item.Properties()
                    ));

    public static final Supplier<Item> CINDERVANE_SPAWN_EGG =
            REGISTER.register("cindervane_spawn_egg",
                    () -> Services.PLATFORM.createSpawnEgg(
                            ModEntities.CINDERVANE,
                            0xF88017, 0x414F49,
                            new Item.Properties()
                    ));

    public static final Supplier<Item> VARASUCHUS_SPAWN_EGG =
            REGISTER.register("varasuchus_spawn_egg",
                    () -> Services.PLATFORM.createSpawnEgg(
                            ModEntities.VARASUCHUS,
                            0x849B59, 0xE8CE74,
                            new Item.Properties()
                    ));

    public static final Supplier<Item> IGNIVORUS_SPAWN_EGG =
            REGISTER.register("ignivorus_spawn_egg",
                    () -> Services.PLATFORM.createSpawnEgg(
                            ModEntities.IGNIVORUS,
                            0x0A0A0A, 0x5A5A5A,
                            new Item.Properties()
                    ));

    public static final Supplier<Item> VOLITANS_SPAWN_EGG =
            REGISTER.register("volitans_spawn_egg",
                    () -> Services.PLATFORM.createSpawnEgg(
                            ModEntities.VOLITANS,
                            0x2E6B7A, 0x9AD0D9,
                            new Item.Properties()
                    ));

    public static final Supplier<Item> NULLJAW_SPAWN_EGG =
            REGISTER.register("nulljaw_spawn_egg",
                    () -> Services.PLATFORM.createSpawnEgg(
                            ModEntities.NULLJAW,
                            0x121118, 0x7E8BA6,
                            new Item.Properties()
                    ));
    public static final Supplier<Item> ATROXIIA_SPAWN_EGG =
            REGISTER.register("atroxiia_spawn_egg",
                    () -> Services.PLATFORM.createSpawnEgg(
                            ModEntities.ATROXIIA,
                            0xFFFFFF, 0x808080,
                            new Item.Properties()
                    ));

    public static final Supplier<Item> MOOP_SPAWN_EGG =
            REGISTER.register("moop_spawn_egg",
                    () -> Services.PLATFORM.createSpawnEgg(
                            ModEntities.MOOP,
                            0x8CC7C8, 0xF2E5B8,
                            new Item.Properties()
                    ));

    public static final Supplier<Item> MOSSBACK_SPAWN_EGG =
            REGISTER.register("mossback_spawn_egg",
                    () -> Services.PLATFORM.createSpawnEgg(
                            ModEntities.MOSSBACK,
                            0x4F6F3A, 0xB7C46A,
                            new Item.Properties()
                    ));

    public static final Supplier<Item> LATCHER_SPAWN_EGG =
            REGISTER.register("latcher_spawn_egg",
                    () -> Services.PLATFORM.createSpawnEgg(
                            ModEntities.LATCHER,
                            0x000000, 0xFFFFFF,
                            new Item.Properties()
                    ));

    public static final Supplier<Item> WINGED_SPAWN_EGG =
            REGISTER.register("winged_spawn_egg",
                    () -> Services.PLATFORM.createSpawnEgg(
                            ModEntities.WINGED,
                            0x000000, 0xFFFFFF,
                            new Item.Properties()
                    ));

    public static final Supplier<Item> WHETTLED_SPAWN_EGG =
            REGISTER.register("whettled_spawn_egg",
                    () -> Services.PLATFORM.createSpawnEgg(
                            ModEntities.WHETTLED,
                            0x000000, 0xFFFFFF,
                            new Item.Properties()
                    ));

    public static final Supplier<Item> DRACONIAN_SWARM_SPAWN_EGG =
            REGISTER.register("draconian_swarm_spawn_egg",
                    () -> Services.PLATFORM.createDraconianSwarmSpawnEgg(
                            ModEntities.LATCHER,
                            0x000000, 0xFFFFFF,
                            new Item.Properties()
                    ));

    public static final Supplier<Item> DRACONIAN_FLESH =
            REGISTER.register("draconian_flesh", () -> new Item(new Item.Properties()));

    public static final Supplier<Item> RAW_WORLDROOT =
            REGISTER.register("raw_worldroot", () -> new Item(new Item.Properties()));

    public static final Supplier<Item> DRAGONHEART_CHUNK =
            REGISTER.register("dragonheart_chunk", () -> new Item(new Item.Properties()));

    public static final Supplier<Item> DRAGONHEART_ALLOY =
            REGISTER.register("dragonheart_alloy", () -> new Item(new Item.Properties()));

    public static final Supplier<Item> WORLDROOT_INGOT =
            REGISTER.register("worldroot_ingot", () -> new Item(new Item.Properties()));

    public static final Supplier<Item> WORLDROOT_SWORD =
            REGISTER.register("worldroot_sword",
                    () -> new SwordItem(WorldrootTier.INSTANCE, 3, -2.4F, new Item.Properties()));

    public static final Supplier<Item> WORLDROOT_PICKAXE =
            REGISTER.register("worldroot_pickaxe",
                    () -> new PickaxeItem(WorldrootTier.INSTANCE, 1, -2.8F, new Item.Properties()));

    public static final Supplier<Item> WORLDROOT_AXE =
            REGISTER.register("worldroot_axe",
                    () -> new AxeItem(WorldrootTier.INSTANCE, 5.0F, -3.0F, new Item.Properties()));

    public static final Supplier<Item> WORLDROOT_SHOVEL =
            REGISTER.register("worldroot_shovel",
                    () -> new ShovelItem(WorldrootTier.INSTANCE, 1.5F, -3.0F, new Item.Properties()));

    public static final Supplier<Item> WORLDROOT_HOE =
            REGISTER.register("worldroot_hoe",
                    () -> new HoeItem(WorldrootTier.INSTANCE, -4, 0.0F, new Item.Properties()));

    public static final Supplier<Item> IVY_THE_MERCHANT_SPAWN_EGG =
            REGISTER.register("ivy_the_merchant_spawn_egg",
                    () -> Services.PLATFORM.createSpawnEgg(
                            ModEntities.IVY_THE_DRAGON_MERCHANT,
                            0x6B5B4B, 0xC2A27A,
                            new Item.Properties()
                    ));

    // Block Items
    public static final Supplier<Item> DRACONIAN_PELLUCIDA =
            REGISTER.register("draconian_pellucida",
                    () -> new BlockItem(ModBlocks.DRACONIAN_PELLUCIDA.get(), new Item.Properties()));

    public static final Supplier<Item> DRACONIAN_NUCLEUS =
            REGISTER.register("draconian_nucleus",
                    () -> new BlockItem(ModBlocks.DRACONIAN_NUCLEUS.get(), new Item.Properties()));

    public static final Supplier<Item> DRACONIC_CRUCIBLE =
            REGISTER.register("draconic_crucible",
                    () -> new BlockItem(ModBlocks.DRACONIC_CRUCIBLE.get(), new Item.Properties()));

    public static final Supplier<Item> DRAGONHEART_ORE =
            REGISTER.register("dragonheart_ore",
                    () -> new BlockItem(ModBlocks.DRAGONHEART_ORE.get(), new Item.Properties()));

    public static final Supplier<Item> DRAGONHEART_ALLOY_BLOCK =
            REGISTER.register("dragonheart_alloy_block",
                    () -> new BlockItem(ModBlocks.DRAGONHEART_ALLOY_BLOCK.get(), new Item.Properties()));

    public static final Supplier<Item> DRAGONHEART_BLOCK =
            REGISTER.register("dragonheart_block",
                    () -> new BlockItem(ModBlocks.DRAGONHEART_BLOCK.get(), new Item.Properties()));

    public static final Supplier<Item> DEEPSLATE_WORLDROOT_ORE =
            REGISTER.register("deepslate_worldroot_ore",
                    () -> new BlockItem(ModBlocks.DEEPSLATE_WORLDROOT_ORE.get(), new Item.Properties()));

    public static final Supplier<Item> WORLDROOT_BLOCK =
            REGISTER.register("worldroot_block",
                    () -> new BlockItem(ModBlocks.WORLDROOT_BLOCK.get(), new Item.Properties()));

    public static final Supplier<Item> RAW_WORLDROOT_BLOCK =
            REGISTER.register("raw_worldroot_block",
                    () -> new BlockItem(ModBlocks.RAW_WORLDROOT_BLOCK.get(), new Item.Properties()));

    public static final Supplier<Item> RAEVYX_EGG =
            REGISTER.register("raevyx_egg",
                    () -> new BlockItem(ModBlocks.RAEVYX_EGG.get(),
                            new Item.Properties()));

    public static final Supplier<Item> IGNIVORUS_EGG =
            REGISTER.register("ignivorus_egg",
                    () -> new BlockItem(ModBlocks.IGNIVORUS_EGG.get(),
                            new Item.Properties()));

    public static final Supplier<Item> CINDERVANE_EGG =
            REGISTER.register("cindervane_egg",
                    () -> new BlockItem(ModBlocks.CINDERVANE_EGG.get(),
                            new Item.Properties()));

    public static final Supplier<Item> VARASUCHUS_EGG =
            REGISTER.register("varasuchus_egg",
                    () -> new BlockItem(ModBlocks.VARASUCHUS_EGG.get(),
                            new Item.Properties()));

    public static final Supplier<Item> STEGONAUT_EGG =
            REGISTER.register("stegonaut_egg",
                    () -> new BlockItem(ModBlocks.STEGONAUT_EGG.get(),
                            new Item.Properties()));

    public static final Supplier<Item> VOLITANS_EGG =
            REGISTER.register("volitans_egg",
                    () -> new BlockItem(ModBlocks.VOLITANS_EGG.get(),
                            new Item.Properties()));

    public static final Supplier<Item> DRACONIC_CODEX =
            REGISTER.register("draconic_codex",
                    () -> new DragonAllyBookItem(
                            new Item.Properties()
                                    .stacksTo(1)
                                    .durability(0)
                    ));

    //it's unused but important
    public static final Supplier<Item> DRAGON_ENCOUNTER_ICON =
            REGISTER.register("dragon_encounter_icon",
                    () -> new Item(new Item.Properties()));

    public static final Supplier<Item> DRACONIAN_NUCLEUS_PARTICLE_ICON =
            REGISTER.register("draconian_nucleus_particle_icon",
                    () -> new Item(new Item.Properties()));

    public static final Supplier<Item> DRAGON_BINDER_ICON =
            REGISTER.register("dragon_binder_icon",
                    () -> new Item(new Item.Properties()));

    public static final Supplier<Item> DRAGON_SCALE_ICON =
            REGISTER.register("dragon_scale_icon",
                    () -> new Item(new Item.Properties()));

    public static final Supplier<Item> WATER_SPLASH_ICON =
            REGISTER.register("water_splash_icon",
                    () -> new Item(new Item.Properties()));

    public static final Supplier<Item> DRAGON_BRUSH =
            REGISTER.register("dragon_brush",
                    () -> new DragonBrushItem(
                            new Item.Properties()
                                    .stacksTo(1)
                                    .durability(256)
                    ));
    //end

    public static final Supplier<Item> GOLDEN_DRAGON_BRUSH =
            REGISTER.register("golden_dragon_brush",
                    () -> new DragonBrushItem(
                            new Item.Properties()
                                    .stacksTo(1)
                                    .durability(256)
                    ));

    public static final Supplier<Item> RAW_MOOP =
            REGISTER.register("raw_moop",
                    () -> new Item(
                            new Item.Properties()
                                    .food(new FoodProperties.Builder()
                                            .nutrition(2)
                                            .saturationMod(0.1F)
                                            .build())
                    ));

    public static final Supplier<Item> COOKED_MOOP =
            REGISTER.register("cooked_moop",
                    () -> new Item(
                            new Item.Properties()
                                    .food(new FoodProperties.Builder()
                                            .nutrition(5)
                                            .saturationMod(0.6F)
                                            .build())
                    ));

    public static final Supplier<Item> BUCKET_OF_MOOP =
            REGISTER.register("bucket_of_moop",
                    () -> Services.PLATFORM.createMobBucket(
                            ModEntities.MOOP,
                            Fluids.WATER,
                            SoundEvents.BUCKET_EMPTY_FISH,
                            new Item.Properties()
                                    .stacksTo(1)
                    ));

    public static final Supplier<Item> MOSSBACK =
            REGISTER.register("mossback",
                    () -> new MossbackItem(
                            new Item.Properties()
                                    .stacksTo(16)
                    ));

    public static final Supplier<Item> RAEVYX_SCALE =
            REGISTER.register("raevyx_scale",
                    () -> new Item(
                            new Item.Properties()
                    ));

    public static final Supplier<Item> CINDERVANE_SCALE =
            REGISTER.register("cindervane_scale",
                    () -> new Item(
                            new Item.Properties()
                    ));

    public static final Supplier<Item> IGNIVORUS_SCALE =
            REGISTER.register("ignivorus_scale",
                    () -> new Item(
                            new Item.Properties()
                    ));

    public static final Supplier<Item> IGNIVORUS_HEART =
            REGISTER.register("ignivorus_heart",
                    () -> new Item(
                            new Item.Properties()
                    ));

    public static final Supplier<Item> IGNIVORUS_TOOTH =
            REGISTER.register("ignivorus_tooth",
                    () -> new Item(
                            new Item.Properties()
                    ));

    public static final Supplier<Item> VARASUCHUS_SCALE =
            REGISTER.register("varasuchus_scale",
                    () -> new Item(
                            new Item.Properties()
                    ));

    public static final Supplier<Item> VOLITANS_SCALE =
            REGISTER.register("volitans_scale",
                    () -> new Item(
                            new Item.Properties()
                    ));

    public static final Supplier<Item> VOLITANS_SPINE =
            REGISTER.register("volitans_spine",
                    () -> new Item(
                            new Item.Properties()
                    ));

    public static final Supplier<Item> ARROW_OF_VENOM =
            REGISTER.register("arrow_of_venom",
                    () -> new ArrowOfVenomItem(
                            new Item.Properties()
                    ));

    public static final Supplier<Item> STEGONAUT_SCALE =
            REGISTER.register("stegonaut_scale",
                    () -> new Item(
                            new Item.Properties()
                    ));

    public static final Supplier<Item> STEGONAUT_BINDER =
            REGISTER.register("stegonaut_binder",
                    () -> new StegonautBinderItem(
                            new Item.Properties()
                                    .stacksTo(1)
                                    .durability(0)
                    ));

    public static final Supplier<Item> RAEVYX_BINDER =
            REGISTER.register("raevyx_binder",
                    () -> new RaevyxBinderItem(
                            new Item.Properties()
                                    .stacksTo(1)
                                    .durability(0)
                    ));

    public static final Supplier<Item> CINDERVANE_BINDER =
            REGISTER.register("cindervane_binder",
                    () -> new CindervaneBinderItem(
                            new Item.Properties()
                                    .stacksTo(1)
                                    .durability(0)
                    ));

    public static final Supplier<Item> VARASUCHUS_BINDER =
            REGISTER.register("varasuchus_binder",
                    () -> new VarasuchusBinderItem(
                            new Item.Properties()
                                    .stacksTo(1)
                                    .durability(0)
                    ));
    public static final Supplier<Item> IGNIVORUS_BINDER =
            REGISTER.register("ignivorus_binder",
                    () -> new IgnivorusBinderItem(
                            new Item.Properties()
                                    .stacksTo(1)
                                    .durability(0)
                    ));
    public static final Supplier<Item> VOLITANS_BINDER =
            REGISTER.register("volitans_binder",
                    () -> new VolitansBinderItem(
                            new Item.Properties()
                                    .stacksTo(1)
                                    .durability(0)
                    ));

    public static final Supplier<Item> NULLJAW_BINDER =
            REGISTER.register("nulljaw_binder",
                    () -> new NulljawBinderItem(
                            new Item.Properties()
                                    .stacksTo(1)
                                    .durability(0)
                    ));

    public static final Supplier<Item> HEARTY_DRAGON_MEAL =
            REGISTER.register("hearty_dragon_meal",
                    () -> new HeartyDragonMealItem(
                            new Item.Properties()
                                    .stacksTo(16)
                                    .food(new FoodProperties.Builder()
                                            .nutrition(10)
                                            .saturationMod(1.2f)
                                            .build())
                    ));
    public static final Supplier<Item> DRACONIAN_CONTROLLER =
            REGISTER.register("draconian_controller",
                    () -> new DraconianControllerItem(
                            new Item.Properties()
                                    .stacksTo(1)
                                    .durability(0)
                    ));

    public static final Supplier<Item> BLEEDING_BOLT_MUSIC_DISC =
            REGISTER.register("bleeding_bolt_music_disc",
                    () -> new RecordItem(
                            1,
                            ModSounds.BLEEDING_BOLT.get(),
                            new Item.Properties()
                                    .stacksTo(1)
                                    .rarity(Rarity.RARE),
                            20 * 104
                    ));

    public static boolean isDragonBrush(ItemStack stack) {
        return stack.is(ModTags.Items.DRAGON_BRUSHES);
    }

    public static void register() {
        ModArmors.init();
        ModPotionItems.init();
        REGISTER.register();
    }
}

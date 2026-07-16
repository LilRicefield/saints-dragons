package com.leon.saintsdragons.common.item.tools;

import com.google.common.collect.Multimap;
import com.leon.saintsdragons.common.config.ToolsArmorConfig;
import com.leon.saintsdragons.common.item.ConfiguredItemAttributes;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.item.AxeItem;
import net.minecraft.world.item.HoeItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.PickaxeItem;
import net.minecraft.world.item.ShovelItem;
import net.minecraft.world.item.SwordItem;

public final class ConfiguredWorldrootItems {
    public static final class Sword extends SwordItem {
        public Sword(Item.Properties properties) {
            super(WorldrootTier.INSTANCE, 3, -2.4F, properties);
        }

        @Override
        public Multimap<Attribute, AttributeModifier> getDefaultAttributeModifiers(EquipmentSlot slot) {
            return ConfiguredItemAttributes.weapon(super.getDefaultAttributeModifiers(slot), slot,
                    ToolsArmorConfig.WORLDROOT_SWORD_DAMAGE.get(), ToolsArmorConfig.WORLDROOT_SWORD_SPEED.get());
        }
    }

    public static final class Pickaxe extends PickaxeItem {
        public Pickaxe(Item.Properties properties) {
            super(WorldrootTier.INSTANCE, 1, -2.8F, properties);
        }

        @Override
        public Multimap<Attribute, AttributeModifier> getDefaultAttributeModifiers(EquipmentSlot slot) {
            return ConfiguredItemAttributes.weapon(super.getDefaultAttributeModifiers(slot), slot,
                    ToolsArmorConfig.WORLDROOT_PICKAXE_DAMAGE.get(), ToolsArmorConfig.WORLDROOT_PICKAXE_SPEED.get());
        }
    }

    public static final class Axe extends AxeItem {
        public Axe(Item.Properties properties) {
            super(WorldrootTier.INSTANCE, 5.0F, -3.0F, properties);
        }

        @Override
        public Multimap<Attribute, AttributeModifier> getDefaultAttributeModifiers(EquipmentSlot slot) {
            return ConfiguredItemAttributes.weapon(super.getDefaultAttributeModifiers(slot), slot,
                    ToolsArmorConfig.WORLDROOT_AXE_DAMAGE.get(), ToolsArmorConfig.WORLDROOT_AXE_SPEED.get());
        }
    }

    public static final class Shovel extends ShovelItem {
        public Shovel(Item.Properties properties) {
            super(WorldrootTier.INSTANCE, 1.5F, -3.0F, properties);
        }

        @Override
        public Multimap<Attribute, AttributeModifier> getDefaultAttributeModifiers(EquipmentSlot slot) {
            return ConfiguredItemAttributes.weapon(super.getDefaultAttributeModifiers(slot), slot,
                    ToolsArmorConfig.WORLDROOT_SHOVEL_DAMAGE.get(), ToolsArmorConfig.WORLDROOT_SHOVEL_SPEED.get());
        }
    }

    public static final class Hoe extends HoeItem {
        public Hoe(Item.Properties properties) {
            super(WorldrootTier.INSTANCE, -4, 0.0F, properties);
        }

        @Override
        public Multimap<Attribute, AttributeModifier> getDefaultAttributeModifiers(EquipmentSlot slot) {
            return ConfiguredItemAttributes.weapon(super.getDefaultAttributeModifiers(slot), slot,
                    ToolsArmorConfig.WORLDROOT_HOE_DAMAGE.get(), ToolsArmorConfig.WORLDROOT_HOE_SPEED.get());
        }
    }

    private ConfiguredWorldrootItems() {
    }
}

package com.leon.saintsdragons.common.registry;

import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.item.ArmorItem;
import net.minecraft.world.item.ArmorMaterial;
import net.minecraft.world.item.crafting.Ingredient;

import java.util.function.Supplier;

public enum ModArmorMaterials implements ArmorMaterial {
    DRAGONHEART_CHUNK(
            "dragonheart_chunk",
            45,
            4,
            9,
            7,
            4,
            18,
            SoundEvents.ARMOR_EQUIP_NETHERITE,
            4.0F,
            0.15F,
            () -> Ingredient.of(ModItems.DRAGONHEART_CHUNK.get())
    ),
    DRAGONHEART_ALLOY(
            "dragonheart_alloy",
            55,
            5,
            10,
            8,
            5,
            20,
            SoundEvents.ARMOR_EQUIP_NETHERITE,
            5.0F,
            0.0F,
            () -> Ingredient.of(ModItems.DRAGONHEART_ALLOY.get())
    );

    private final String name;
    private final int durabilityMultiplier;
    private final int bootsDefense;
    private final int chestplateDefense;
    private final int leggingsDefense;
    private final int helmetDefense;
    private final int enchantmentValue;
    private final SoundEvent equipSound;
    private final float toughness;
    private final float knockbackResistance;
    private final Supplier<Ingredient> repairIngredient;

    ModArmorMaterials(
            String name,
            int durabilityMultiplier,
            int bootsDefense,
            int chestplateDefense,
            int leggingsDefense,
            int helmetDefense,
            int enchantmentValue,
            SoundEvent equipSound,
            float toughness,
            float knockbackResistance,
            Supplier<Ingredient> repairIngredient
    ) {
        this.name = name;
        this.durabilityMultiplier = durabilityMultiplier;
        this.bootsDefense = bootsDefense;
        this.chestplateDefense = chestplateDefense;
        this.leggingsDefense = leggingsDefense;
        this.helmetDefense = helmetDefense;
        this.enchantmentValue = enchantmentValue;
        this.equipSound = equipSound;
        this.toughness = toughness;
        this.knockbackResistance = knockbackResistance;
        this.repairIngredient = repairIngredient;
    }

    @Override
    public int getDurabilityForType(ArmorItem.Type type) {
        return switch (type) {
            case BOOTS -> 13;
            case LEGGINGS -> 15;
            case CHESTPLATE -> 16;
            case HELMET -> 11;
        } * this.durabilityMultiplier;
    }

    @Override
    public int getDefenseForType(ArmorItem.Type type) {
        return switch (type) {
            case BOOTS -> this.bootsDefense;
            case LEGGINGS -> this.leggingsDefense;
            case CHESTPLATE -> this.chestplateDefense;
            case HELMET -> this.helmetDefense;
        };
    }

    @Override
    public int getEnchantmentValue() {
        return this.enchantmentValue;
    }

    @Override
    public SoundEvent getEquipSound() {
        return this.equipSound;
    }

    @Override
    public Ingredient getRepairIngredient() {
        return this.repairIngredient.get();
    }

    @Override
    public String getName() {
        return "saintsdragons:" + this.name;
    }

    @Override
    public float getToughness() {
        return this.toughness;
    }

    @Override
    public float getKnockbackResistance() {
        return this.knockbackResistance;
    }
}

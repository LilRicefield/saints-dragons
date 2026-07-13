package com.leon.saintsdragons.common.item.tools;

import com.leon.saintsdragons.common.registry.ModItems;
import net.minecraft.world.item.Tier;
import net.minecraft.world.item.crafting.Ingredient;
import org.jetbrains.annotations.NotNull;

import java.util.function.Supplier;

public enum DragonheartWeaponTier implements Tier {
    CHUNK(4, 2600, 5.0F, 5.0F, 18, () -> Ingredient.of(ModItems.DRAGONHEART_CHUNK.get())),
    ALLOY(4, 3400, 10.0F, 7.0F, 20, () -> Ingredient.of(ModItems.DRAGONHEART_ALLOY.get()));

    private final int level;
    private final int uses;
    private final float speed;
    private final float attackDamageBonus;
    private final int enchantmentValue;
    private final Supplier<Ingredient> repairIngredient;

    DragonheartWeaponTier(int level, int uses, float speed, float attackDamageBonus,
                          int enchantmentValue, Supplier<Ingredient> repairIngredient) {
        this.level = level;
        this.uses = uses;
        this.speed = speed;
        this.attackDamageBonus = attackDamageBonus;
        this.enchantmentValue = enchantmentValue;
        this.repairIngredient = repairIngredient;
    }

    @Override
    public int getUses() {
        return this.uses;
    }

    @Override
    public float getSpeed() {
        return this.speed;
    }

    @Override
    public float getAttackDamageBonus() {
        return this.attackDamageBonus;
    }

    @Override
    public int getLevel() {
        return this.level;
    }

    @Override
    public int getEnchantmentValue() {
        return this.enchantmentValue;
    }

    @Override
    public @NotNull Ingredient getRepairIngredient() {
        return this.repairIngredient.get();
    }
}

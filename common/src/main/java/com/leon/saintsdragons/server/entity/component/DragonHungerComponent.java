package com.leon.saintsdragons.server.entity.component;

import com.leon.saintsdragons.server.data.DragonCodexSavedData;
import com.leon.saintsdragons.server.entity.base.DragonEntity;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.util.Mth;

public final class DragonHungerComponent {
    public static final int HUNGER_MAX = 100;
    private static final int HUNGER_DECAY_INTERVAL_TICKS = 7200;
    private static final int HUNGER_DECAY_RIDDEN_TICK_MULT = 2;
    private static final int HUNGER_FEED_AMOUNT = 10;
    private static final int HUNGER_FEED_AMOUNT_HEARTY = 20;
    private static final int HUNGER_DAMAGE_INTERVAL_TICKS = 80;
    private static final float HUNGER_DAMAGE_AMOUNT = 2.0f;

    private final DragonEntity dragon;

    private int hunger = HUNGER_MAX;
    private int hungerDecayTicks = 0;

    public DragonHungerComponent(DragonEntity dragon) {
        this.dragon = dragon;
    }

    public int getHunger() {
        return hunger;
    }

    public int getMaxHunger() {
        return HUNGER_MAX;
    }

    public boolean isHungry() {
        return hunger < HUNGER_MAX;
    }

    public void setHunger(int value) {
        int clamped = Mth.clamp(value, 0, HUNGER_MAX);
        if (this.hunger == clamped) {
            return;
        }
        this.hunger = clamped;
        if (!dragon.level().isClientSide && dragon.isTame() && dragon.getOwnerUUID() != null) {
            net.minecraft.server.level.ServerLevel serverLevel = (net.minecraft.server.level.ServerLevel) dragon.level();
            DragonCodexSavedData.get(serverLevel).updateDragonStats(dragon.getOwnerUUID(), dragon);
        }
    }

    public boolean applyFeeding(boolean heartyMeal) {
        boolean wasHungry = isHungry();
        int amount = heartyMeal ? HUNGER_FEED_AMOUNT_HEARTY : HUNGER_FEED_AMOUNT;
        setHunger(this.hunger + amount);
        return wasHungry;
    }

    public float getMeleeDamageMultiplier() {
        if (hunger > 60) {
            return 1.0f;
        }
        if (hunger <= 30) {
            return 0.25f;
        }
        float ratio = (hunger - 30) / 30.0f; // 30..60 => 0..1
        return 0.25f + (0.25f * ratio);
    }

    public void tick() {
        if (!dragon.isTame()) {
            return;
        }
        int decayStep = 1;
        if (dragon.isVehicle() && dragon.getDeltaMovement().horizontalDistanceSqr() > 0.0025) {
            decayStep = HUNGER_DECAY_RIDDEN_TICK_MULT;
        }
        if (hunger > 0) {
            hungerDecayTicks += decayStep;
            if (hungerDecayTicks >= HUNGER_DECAY_INTERVAL_TICKS) {
                hungerDecayTicks = 0;
                setHunger(hunger - 1);
            }
            return;
        }

        hungerDecayTicks += decayStep;
        if (hungerDecayTicks >= HUNGER_DAMAGE_INTERVAL_TICKS) {
            hungerDecayTicks = 0;
            dragon.hurt(dragon.damageSources().starve(), HUNGER_DAMAGE_AMOUNT);
        }
    }

    public void saveToNBT(CompoundTag tag) {
        tag.putInt("Hunger", this.hunger);
    }

    public void loadFromNBT(CompoundTag tag) {
        this.hunger = tag.contains("Hunger") ? Mth.clamp(tag.getInt("Hunger"), 0, HUNGER_MAX) : HUNGER_MAX;
    }
}

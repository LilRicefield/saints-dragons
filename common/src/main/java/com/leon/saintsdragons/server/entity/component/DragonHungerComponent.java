package com.leon.saintsdragons.server.entity.component;

import com.leon.saintsdragons.server.data.DragonCodexSavedData;
import com.leon.saintsdragons.server.entity.base.DragonEntity;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.Mth;

public final class DragonHungerComponent {
    public static final int HUNGER_MAX = 100;

    private final DragonEntity dragon;

    private int hunger = HUNGER_MAX;
    private int hungerDecayTicks = 0;
    private int hungerHealingTicks = 0;

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
        if (!dragon.isHungerEnabled()) {
            return;
        }
        int clamped = Mth.clamp(value, 0, HUNGER_MAX);
        if (this.hunger == clamped) {
            return;
        }
        this.hunger = clamped;
        if (!dragon.level().isClientSide && dragon.isTame() && dragon.getOwnerUUID() != null) {
            ServerLevel serverLevel = (ServerLevel) dragon.level();
            DragonCodexSavedData.get(serverLevel).updateDragonStats(dragon.getOwnerUUID(), dragon);
        }
    }

    public boolean applyFeeding(boolean heartyMeal) {
        return applyFeeding(dragon.getHungerFeedingAmount(heartyMeal));
    }

    public boolean applyFeeding(int amount) {
        if (!dragon.isHungerEnabled() || amount <= 0) {
            return false;
        }
        boolean wasHungry = isHungry();
        setHunger(this.hunger + Math.min(amount, HUNGER_MAX));
        return wasHungry;
    }

    public float getMeleeDamageMultiplier() {
        if (hunger > 60) {
            return 1.0f;
        }
        if (hunger <= 30) {
            return 0.25f;
        }
        float ratio = (hunger - 30) / 30.0f;
        return 0.25f + (0.75f * ratio);
    }

    public void tick() {
        if (!dragon.isHungerEnabled()) {
            hungerDecayTicks = 0;
            hungerHealingTicks = 0;
            return;
        }

        if (!dragon.shouldTickHunger() || !dragon.isAlive()) {
            return;
        }

        tickDecay();
        tickHealing();
    }

    private void tickDecay() {
        int decayStep = Math.max(0, dragon.getHungerDecayStep());
        hungerDecayTicks = (int) Math.min(Integer.MAX_VALUE, (long) hungerDecayTicks + decayStep);

        if (hunger > 0) {
            int interval = Math.max(1, dragon.getHungerDecayIntervalTicks());
            while (hungerDecayTicks >= interval && hunger > 0) {
                hungerDecayTicks -= interval;
                setHunger(hunger - 1);
            }
            return;
        }

        int interval = Math.max(1, dragon.getHungerStarvationIntervalTicks());
        float damage = dragon.getHungerStarvationDamage();
        if (!Float.isFinite(damage) || damage <= 0.0F) {
            hungerDecayTicks = 0;
            return;
        }
        while (hungerDecayTicks >= interval) {
            hungerDecayTicks -= interval;
            dragon.hurt(dragon.damageSources().starve(), damage);
            if (!dragon.isAlive()) {
                break;
            }
        }
    }

    private void tickHealing() {
        float amount = dragon.getHungerHealingAmount();
        if (!Float.isFinite(amount) || amount <= 0.0F || !dragon.canHealFromHunger()) {
            return;
        }
        int interval = Math.max(1, dragon.getHungerHealingIntervalTicks());
        hungerHealingTicks = (int) Math.min(Integer.MAX_VALUE, (long) hungerHealingTicks + 1);
        if (hungerHealingTicks < interval) {
            return;
        }
        hungerHealingTicks = 0;
        int cost = Mth.clamp(dragon.getHungerHealingCost(), 0, HUNGER_MAX);
        int minimum = Mth.clamp(dragon.getMinimumHungerAfterHealing(), 0, HUNGER_MAX);
        if (hunger - cost < minimum) {
            return;
        }
        setHunger(hunger - cost);
        dragon.heal(amount);
    }

    public void saveToNBT(CompoundTag tag) {
        tag.putInt("Hunger", this.hunger);
        tag.putInt("HungerDecayTicks", this.hungerDecayTicks);
        tag.putInt("HungerHealingTicks", this.hungerHealingTicks);
    }

    public void loadFromNBT(CompoundTag tag) {
        this.hunger = tag.contains("Hunger") ? Mth.clamp(tag.getInt("Hunger"), 0, HUNGER_MAX) : HUNGER_MAX;
        this.hungerDecayTicks = tag.contains("HungerDecayTicks") ? Math.max(0, tag.getInt("HungerDecayTicks")) : 0;
        this.hungerHealingTicks = Math.max(0, tag.getInt("HungerHealingTicks"));
    }
}

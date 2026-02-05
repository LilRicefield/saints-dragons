package com.leon.saintsdragons.server.entity.component;

import com.leon.saintsdragons.server.data.DragonCodexSavedData;
import com.leon.saintsdragons.server.entity.base.DragonEntity;
import java.util.UUID;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;

public final class DragonHappinessComponent {
    public static final int HAPPINESS_MAX = 100;
    // 20 ticks = 1 second, so 9600 ticks = 8 minutes for one decay cycle.
    private static final int HAPPINESS_DECAY_INTERVAL_TICKS = 9600;
    private static final int HAPPINESS_DECAY_AMOUNT = 2;
    private static final int HAPPINESS_DECAY_LOW_HUNGER_TICK_MULT = 2;
    private static final int HAPPINESS_DECAY_CRITICAL_HUNGER_TICK_MULT = 4;
    private static final int HAPPINESS_FEED_AMOUNT = 4;
    private static final int HAPPINESS_FEED_AMOUNT_HEARTY = 8;
    private static final int HAPPINESS_HIT_PENALTY = 2;
    private static final int HAPPINESS_ANGRY_THRESHOLD = 60;
    private static final int HAPPINESS_SLOW_THRESHOLD = 60;
    private static final int HAPPINESS_SPEED_MIN_THRESHOLD = 30;
    private static final float HAPPINESS_SPEED_MIN_MULTIPLIER = 0.5f;
    private static final UUID HAPPINESS_SLOW_GROUND_UUID = UUID.fromString("e4a2e52c-f311-4c35-9cf2-7dd0b6c0c4a8");
    private static final UUID HAPPINESS_SLOW_FLY_UUID = UUID.fromString("b51a7bd2-8c8a-4ea9-9a6f-7f40e1b7b7af");

    private final DragonEntity dragon;
    private final EntityDataAccessor<Integer> dataAccessor;

    private int happiness = HAPPINESS_MAX;
    private int happinessDecayTicks = 0;

    public DragonHappinessComponent(DragonEntity dragon, EntityDataAccessor<Integer> dataAccessor) {
        this.dragon = dragon;
        this.dataAccessor = dataAccessor;
    }

    public int getHappiness() {
        return dragon.level() != null && dragon.level().isClientSide
                ? dragon.getEntityData().get(dataAccessor)
                : happiness;
    }

    public int getMaxHappiness() {
        return HAPPINESS_MAX;
    }

    public void setHappiness(int value) {
        int clamped = Mth.clamp(value, 0, HAPPINESS_MAX);
        if (this.happiness == clamped) {
            return;
        }
        this.happiness = clamped;
        dragon.getEntityData().set(dataAccessor, clamped);
        if (!dragon.level().isClientSide && dragon.isTame() && dragon.getOwnerUUID() != null) {
            net.minecraft.server.level.ServerLevel serverLevel = (net.minecraft.server.level.ServerLevel) dragon.level();
            DragonCodexSavedData.get(serverLevel).updateDragonStats(dragon.getOwnerUUID(), dragon);
        }
    }

    public void applyFeeding(boolean heartyMeal) {
        if (!dragon.isTame()) {
            return;
        }
        int amount = heartyMeal ? HAPPINESS_FEED_AMOUNT_HEARTY : HAPPINESS_FEED_AMOUNT;
        setHappiness(this.happiness + amount);
    }

    public float getSpeedMultiplier() {
        int value = getHappiness();
        if (value > HAPPINESS_SLOW_THRESHOLD) {
            return 1.0f;
        }
        if (value <= HAPPINESS_SPEED_MIN_THRESHOLD) {
            return HAPPINESS_SPEED_MIN_MULTIPLIER;
        }
        float ratio = (value - HAPPINESS_SPEED_MIN_THRESHOLD)
                / (float) (HAPPINESS_SLOW_THRESHOLD - HAPPINESS_SPEED_MIN_THRESHOLD);
        return HAPPINESS_SPEED_MIN_MULTIPLIER + ((1.0f - HAPPINESS_SPEED_MIN_MULTIPLIER) * ratio);
    }

    public void tick(int hunger) {
        if (!dragon.isTame()) {
            return;
        }

        happinessDecayTicks += getDecayStep(hunger);
        while (happinessDecayTicks >= HAPPINESS_DECAY_INTERVAL_TICKS && happiness > 0) {
            happinessDecayTicks -= HAPPINESS_DECAY_INTERVAL_TICKS;
            setHappiness(happiness - HAPPINESS_DECAY_AMOUNT);
        }
    }

    private int getDecayStep(int hunger) {
        if (hunger <= 30) {
            return HAPPINESS_DECAY_CRITICAL_HUNGER_TICK_MULT;
        }
        if (hunger <= 60) {
            return HAPPINESS_DECAY_LOW_HUNGER_TICK_MULT;
        }
        return 1;
    }

    public void applyHitPenalty(net.minecraft.server.level.ServerLevel serverLevel) {
        setHappiness(this.happiness - HAPPINESS_HIT_PENALTY);
        if (this.happiness <= HAPPINESS_ANGRY_THRESHOLD) {
            serverLevel.sendParticles(
                    net.minecraft.core.particles.ParticleTypes.ANGRY_VILLAGER,
                    dragon.getX(),
                    dragon.getY() + dragon.getBbHeight() + 0.3,
                    dragon.getZ(),
                    6,
                    0.3,
                    0.2,
                    0.3,
                    0.0
            );
        }
    }

    public void updateSpeedModifiers() {
        if (dragon.level().isClientSide) {
            return;
        }
        if (!dragon.isTame()) {
            clearSpeedModifiers();
            return;
        }
        float mult = getSpeedMultiplier();
        AttributeInstance move = dragon.getAttribute(Attributes.MOVEMENT_SPEED);
        if (move != null) {
            AttributeModifier existing = move.getModifier(HAPPINESS_SLOW_GROUND_UUID);
            if (mult >= 0.999f) {
                if (existing != null) {
                    move.removeModifier(HAPPINESS_SLOW_GROUND_UUID);
                }
            } else {
                if (existing != null) {
                    move.removeModifier(HAPPINESS_SLOW_GROUND_UUID);
                }
                move.addPermanentModifier(new AttributeModifier(
                        HAPPINESS_SLOW_GROUND_UUID,
                        "Happiness slow (ground)",
                        mult - 1.0,
                        AttributeModifier.Operation.MULTIPLY_TOTAL
                ));
            }
        }

        AttributeInstance fly = dragon.getAttribute(Attributes.FLYING_SPEED);
        if (fly != null) {
            AttributeModifier existing = fly.getModifier(HAPPINESS_SLOW_FLY_UUID);
            if (mult >= 0.999f) {
                if (existing != null) {
                    fly.removeModifier(HAPPINESS_SLOW_FLY_UUID);
                }
            } else {
                if (existing != null) {
                    fly.removeModifier(HAPPINESS_SLOW_FLY_UUID);
                }
                fly.addPermanentModifier(new AttributeModifier(
                        HAPPINESS_SLOW_FLY_UUID,
                        "Happiness slow (fly)",
                        mult - 1.0,
                        AttributeModifier.Operation.MULTIPLY_TOTAL
                ));
            }
        }
    }

    public void clearSpeedModifiers() {
        AttributeInstance move = dragon.getAttribute(Attributes.MOVEMENT_SPEED);
        if (move != null && move.getModifier(HAPPINESS_SLOW_GROUND_UUID) != null) {
            move.removeModifier(HAPPINESS_SLOW_GROUND_UUID);
        }
        AttributeInstance fly = dragon.getAttribute(Attributes.FLYING_SPEED);
        if (fly != null && fly.getModifier(HAPPINESS_SLOW_FLY_UUID) != null) {
            fly.removeModifier(HAPPINESS_SLOW_FLY_UUID);
        }
    }

    public void saveToNBT(CompoundTag tag) {
        tag.putInt("Happiness", this.happiness);
    }

    public void loadFromNBT(CompoundTag tag) {
        this.happiness = tag.contains("Happiness") ? Mth.clamp(tag.getInt("Happiness"), 0, HAPPINESS_MAX) : HAPPINESS_MAX;
        dragon.getEntityData().set(dataAccessor, this.happiness);
    }
}

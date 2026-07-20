package com.leon.saintsdragons.server.ai.dragonbrain.behaviour;

import com.leon.saintsdragons.common.config.SaintsDragonsConfig;
import com.leon.saintsdragons.server.ai.dragonbrain.DragonBehaviour;
import com.leon.saintsdragons.server.ai.dragonbrain.DragonBrainContext;
import com.leon.saintsdragons.server.ai.dragonbrain.DragonMemories;
import com.leon.saintsdragons.server.ai.dragonbrain.DragonMovementIntent;
import com.leon.saintsdragons.server.entity.base.DragonEntity;
import com.leon.saintsdragons.server.entity.base.RideableDragonBase;
import net.minecraft.core.particles.ItemParticleOption;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.ai.behavior.EntityTracker;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;

import java.util.Comparator;
import java.util.Map;

public final class DragonHuntAndEatBehaviour<T extends RideableDragonBase> extends DragonBehaviour<T> {
    private static final int DROP_SEARCH_TICKS = 60;
    private static final int DROP_RESCAN_INTERVAL = 5;
    private static final int MOVE_REFRESH_INTERVAL = 10;
    private static final int EAT_INTERVAL = 20;
    private static final int MAX_FRESH_DROP_AGE = 100;
    private static final double DROP_SEARCH_HORIZONTAL = 8.0D;
    private static final double DROP_SEARCH_VERTICAL = 5.0D;
    private static final double GROUND_MOVE_SPEED = 1.0D;
    private static final double WATER_MOVE_SPEED = 0.25D;
    private static final double LANDING_SPEED = 0.12D;

    @Nullable
    private LivingEntity trackedPrey;
    @Nullable
    private Vec3 killSite;
    @Nullable
    private ItemEntity foodTarget;
    private long dropSearchEndsAt;
    private int dropRescanCooldown;
    private int moveRefreshCooldown;
    private int eatCooldown;
    private String phase = "idle";

    public DragonHuntAndEatBehaviour() {
        super(false);
    }

    public static boolean shouldAcquirePrey(DragonEntity dragon) {
        return !dragon.isHuntFoodPursuitActive() && wantsHuntFood(dragon);
    }

    private static boolean wantsHuntFood(DragonEntity dragon) {
        return SaintsDragonsConfig.HUNGER_DECAY_ENABLED.get() && dragon.isHungry();
    }

    @Override
    protected boolean canStart(DragonBrainContext<T> context) {
        return true;
    }

    @Override
    protected boolean canContinue(DragonBrainContext<T> context) {
        return true;
    }

    @Override
    protected void tick(DragonBrainContext<T> context) {
        T dragon = context.dragon();
        if (eatCooldown > 0) eatCooldown--;
        if (dropRescanCooldown > 0) dropRescanCooldown--;
        if (moveRefreshCooldown > 0) moveRefreshCooldown--;

        LivingEntity attackTarget = context.memories().get(DragonMemories.ATTACK_TARGET).orElse(null);
        updateTrackedPrey(context, attackTarget);

        if (!canSeekFood(dragon)) {
            abandonFoodSearch(context, "idle");
            return;
        }
        if (attackTarget != null
                && attackTarget.isAlive()
                && killSite != null
                && dragon.isPassiveHuntTarget(attackTarget)) {
            clearDeferredPassiveHuntTarget(context, attackTarget);
            attackTarget = null;
        }
        if (attackTarget != null && attackTarget.isAlive()) {
            if (foodTarget != null || killSite != null) {
                abandonFoodSearch(context, "interrupted");
            } else {
                phase = trackedPrey == attackTarget ? "hunting" : "interrupted";
            }
            return;
        }
        if (killSite == null || context.gameTime() > dropSearchEndsAt) {
            abandonFoodSearch(context, "idle");
            return;
        }

        if (!isUsableFoodTarget(dragon, foodTarget)) {
            foodTarget = null;
        }
        if (foodTarget == null && dropRescanCooldown <= 0) {
            foodTarget = findFreshFoodDrop(context);
            dropRescanCooldown = DROP_RESCAN_INTERVAL;
        }
        if (foodTarget == null) {
            phase = "searching_drops";
            return;
        }

        context.memories().set(DragonMemories.LOOK_TARGET, new EntityTracker(foodTarget, true));
        if (isCloseEnoughToEat(dragon, foodTarget)) {
            consumeOne(context, foodTarget);
            return;
        }

        phase = "approaching_food";
        if (moveRefreshCooldown <= 0) {
            requestExistingMovement(context, foodTarget);
            moveRefreshCooldown = MOVE_REFRESH_INTERVAL;
        }
    }

    private void updateTrackedPrey(DragonBrainContext<T> context, @Nullable LivingEntity attackTarget) {
        T dragon = context.dragon();
        if (trackedPrey != null) {
            if (!trackedPrey.isAlive() || trackedPrey.isRemoved()) {
                if (wasKilledByDragon(dragon, trackedPrey)) {
                    killSite = trackedPrey.position();
                    dropSearchEndsAt = context.gameTime() + DROP_SEARCH_TICKS;
                    dropRescanCooldown = 0;
                    moveRefreshCooldown = 0;
                    dragon.setHuntFoodPursuitActive(true);
                    phase = "waiting_for_drops";
                }
                trackedPrey = null;
            } else if (attackTarget != trackedPrey) {
                trackedPrey = null;
                phase = "idle";
            }
        }
        if (killSite != null) {
            return;
        }
        if (attackTarget != null && attackTarget.isAlive() && dragon.isPassiveHuntTarget(attackTarget)) {
            if (trackedPrey != attackTarget) {
                trackedPrey = attackTarget;
                foodTarget = null;
                dragon.setHuntFoodPursuitActive(false);
                phase = "hunting";
            }
        }
    }

    private boolean wasKilledByDragon(T dragon, LivingEntity prey) {
        return prey.getKillCredit() == dragon || prey.getLastHurtByMob() == dragon;
    }

    private boolean canSeekFood(T dragon) {
        return dragon.isAlive()
                && !dragon.isDying()
                && !dragon.isBaby()
                && !dragon.isVehicle()
                && !dragon.isPassenger()
                && !dragon.isOrderedToSit()
                && !dragon.isSleeping()
                && !dragon.isSleepTransitioning()
                && wantsHuntFood(dragon);
    }

    @Nullable
    private ItemEntity findFreshFoodDrop(DragonBrainContext<T> context) {
        if (killSite == null) {
            return null;
        }
        AABB searchArea = new AABB(killSite, killSite).inflate(
                DROP_SEARCH_HORIZONTAL,
                DROP_SEARCH_VERTICAL,
                DROP_SEARCH_HORIZONTAL
        );
        return context.level().getEntitiesOfClass(
                        ItemEntity.class,
                        searchArea,
                        item -> item.tickCount <= MAX_FRESH_DROP_AGE
                                && isUsableFoodTarget(context.dragon(), item)
                ).stream()
                .min(Comparator.comparingDouble(context.dragon()::distanceToSqr))
                .orElse(null);
    }

    private boolean isUsableFoodTarget(T dragon, @Nullable ItemEntity item) {
        return item != null
                && item.isAlive()
                && !item.getItem().isEmpty()
                && dragon.isFood(item.getItem());
    }

    private boolean isCloseEnoughToEat(T dragon, ItemEntity food) {
        double reach = Math.max(1.5D, dragon.getBbWidth() * 0.75D + 0.75D);
        return dragon.distanceToSqr(food) <= reach * reach;
    }

    private void requestExistingMovement(DragonBrainContext<T> context, ItemEntity food) {
        T dragon = context.dragon();
        if (dragon.isAerial() && !food.isInWaterOrBubble()) {
            context.memories().set(
                    DragonMemories.MOVEMENT_INTENT,
                    DragonMovementIntent.landing(food.position(), LANDING_SPEED)
            );
            return;
        }
        double speed = dragon.isInWaterOrBubble() || food.isInWaterOrBubble()
                ? WATER_MOVE_SPEED
                : GROUND_MOVE_SPEED;
        context.memories().set(
                DragonMemories.MOVEMENT_INTENT,
                DragonMovementIntent.auto(food.position(), speed)
        );
    }

    private void consumeOne(DragonBrainContext<T> context, ItemEntity food) {
        T dragon = context.dragon();
        phase = "eating";
        context.memories().set(
                DragonMemories.MOVEMENT_INTENT,
                DragonMovementIntent.stop("hunt-and-eat")
        );
        if (eatCooldown > 0 || dragon.getActiveAbility() != null) {
            return;
        }

        dragon.getLookControl().setLookAt(food, 30.0F, 30.0F);
        dragon.triggerAnim("interaction", "eat");
        dragon.playSound(SoundEvents.GENERIC_EAT, 1.0F, dragon.isBaby() ? 1.4F : 1.0F);
        ItemStack eaten = food.getItem().copy();
        eaten.setCount(1);
        spawnEatingParticles(context, eaten);
        ItemStack remaining = food.getItem().copy();
        remaining.shrink(1);
        dragon.applyFeedingHunger(false);
        eatCooldown = EAT_INTERVAL;

        if (remaining.isEmpty()) {
            food.discard();
            foodTarget = null;
        } else {
            food.setItem(remaining);
        }
        if (!wantsHuntFood(dragon)) {
            abandonFoodSearch(context, "sated");
        }
    }

    private void spawnEatingParticles(DragonBrainContext<T> context, ItemStack food) {
        T dragon = context.dragon();
        AABB bounds = dragon.getBoundingBox();
        double horizontalSpread = Math.max(0.15D,
                Math.min(bounds.getXsize(), bounds.getZsize()) * 0.35D);
        double verticalSpread = Math.max(0.15D, bounds.getYsize() * 0.25D);
        context.level().sendParticles(
                new ItemParticleOption(ParticleTypes.ITEM, food),
                dragon.getX(),
                bounds.minY + bounds.getYsize() * 0.65D,
                dragon.getZ(),
                12,
                horizontalSpread,
                verticalSpread,
                horizontalSpread,
                0.08D
        );
    }

    private void clearDeferredPassiveHuntTarget(DragonBrainContext<T> context, LivingEntity target) {
        T dragon = context.dragon();
        context.memories().erase(DragonMemories.ATTACK_TARGET);
        context.memories().erase(DragonMemories.TARGET_AIRBORNE);
        context.memories().erase(DragonMemories.TARGET_VISIBLE);
        context.memories().erase(DragonMemories.LAST_SEEN_TARGET);
        context.memories().erase(DragonMemories.INVESTIGATION_TARGET);
        context.memories().erase(DragonMemories.HEARD_TARGET);
        if (dragon.getTarget() == target) {
            dragon.setTarget(null);
        }
        dragon.clearPassiveHuntTarget();
    }

    private void abandonFoodSearch(DragonBrainContext<T> context, String nextPhase) {
        if (foodTarget != null || killSite != null) {
            context.memories().erase(DragonMemories.LOOK_TARGET);
        }
        foodTarget = null;
        killSite = null;
        dropSearchEndsAt = 0L;
        dropRescanCooldown = 0;
        moveRefreshCooldown = 0;
        context.dragon().setHuntFoodPursuitActive(false);
        phase = nextPhase;
    }

    @Override
    public Map<String, String> getDragonBrainDebugDetails() {
        return Map.of(
                "phase", phase,
                "prey", trackedPrey == null ? "none" : trackedPrey.getStringUUID(),
                "food", foodTarget == null ? "none" : foodTarget.getItem().getHoverName().getString()
        );
    }
}

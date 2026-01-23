// I know that we are upside down, so hold your tongue and hear me out

package com.leon.saintsdragons.server.entity.npc;

import com.leon.saintsdragons.common.registry.ModItems;
import net.minecraft.world.entity.AgeableMob;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.FloatGoal;
import net.minecraft.world.entity.ai.goal.LookAtPlayerGoal;
import net.minecraft.world.entity.ai.goal.RandomLookAroundGoal;
import net.minecraft.world.entity.ai.goal.RandomStrollGoal;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.ai.control.BodyRotationControl;
import net.minecraft.world.entity.npc.AbstractVillager;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.trading.MerchantOffer;
import net.minecraft.world.item.trading.MerchantOffers;
import net.minecraft.world.entity.npc.VillagerTrades;
import net.minecraft.world.level.Level;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.stats.Stats;
import net.minecraft.util.RandomSource;
import net.minecraft.world.item.enchantment.Enchantments;
import java.util.EnumSet;
import org.jetbrains.annotations.NotNull;
import net.minecraft.util.Mth;
import com.leon.saintsdragons.server.entity.controller.BodyControl;
import com.leon.saintsdragons.server.entity.controller.HumanLookControl;
import com.leon.saintsdragons.util.math.SmoothValue;
import software.bernie.geckolib.animatable.GeoEntity;
import software.bernie.geckolib.core.animatable.instance.AnimatableInstanceCache;
import software.bernie.geckolib.core.animation.AnimatableManager;
import software.bernie.geckolib.core.animation.AnimationController;
import software.bernie.geckolib.core.animation.AnimationState;
import software.bernie.geckolib.core.object.PlayState;
import software.bernie.geckolib.core.animation.RawAnimation;
import software.bernie.geckolib.util.GeckoLibUtil;

import javax.annotation.Nullable;

public class IvyTheDragonMerchant extends AbstractVillager implements GeoEntity {
    private static final int TRADE_START_TICKS = 21;

    private static final VillagerTrades.ItemListing[] TRADES = new VillagerTrades.ItemListing[]{
            (trader, random) -> createIgnivorusEggTrade(random),
            (trader, random) -> createRaevyxEggTrade(random)
    };

    private final AnimatableInstanceCache animationCache = GeckoLibUtil.createInstanceCache(this);
    private TradeAnimState tradeAnimState = TradeAnimState.NONE;
    private int tradeAnimTicks = 0;
    private boolean lastTradingState = false;
    private boolean wasMovementStopped = false;
    private BodyControl bodyControl;
    public final SmoothValue bodyRotDeviation = SmoothValue.rotation(0.0);

    public IvyTheDragonMerchant(EntityType<? extends AbstractVillager> entityType, Level level) {
        super(entityType, level);
        this.lookControl = new HumanLookControl(this);
    }

    public static AttributeSupplier.Builder createAttributes() {
        return Mob.createMobAttributes()
                .add(Attributes.MAX_HEALTH, 20.0)
                .add(Attributes.MOVEMENT_SPEED, 0.35)
                .add(Attributes.FOLLOW_RANGE, 32.0);
    }

    @Override
    protected void registerGoals() {
        goalSelector.addGoal(0, new TradingStillGoal());
        goalSelector.addGoal(1, new FloatGoal(this));
        goalSelector.addGoal(2, new RandomStrollGoal(this, 0.6));
        goalSelector.addGoal(3, new LookAtPlayerGoal(this, Player.class, 6.0f));
        goalSelector.addGoal(4, new RandomLookAroundGoal(this));
    }

    @Override
    protected @NotNull BodyRotationControl createBodyControl() {
        this.bodyControl = new BodyControl(this, 0.45f, 50.0f, 0.22f, 0.12f, 30.0f);
        return this.bodyControl;
    }

    @Override
    public @NotNull MerchantOffers getOffers() {
        if (offers == null) {
            offers = new MerchantOffers();
            updateTrades();
        }
        return offers;
    }

    @Override
    protected void updateTrades() {
        MerchantOffers offers = getOffers();
        if (!offers.isEmpty()) {
            return;
        }
        addOffersFromItemListings(offers, TRADES, TRADES.length);
    }

    @Override
    public @NotNull InteractionResult mobInteract(@NotNull Player player, @NotNull InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        if (stack.is(ModItems.IVY_THE_MERCHANT_SPAWN_EGG.get())) {
            return super.mobInteract(player, hand);
        }
        if (!isAlive()) {
            return InteractionResult.PASS;
        }
        if (isBaby()) {
            return super.mobInteract(player, hand);
        }
        if (!level().isClientSide) {
            setTradingPlayer(player);
            openTradingScreen(player, getDisplayName(), 1);
            player.awardStat(Stats.TALKED_TO_VILLAGER);
        }
        return InteractionResult.sidedSuccess(level().isClientSide);
    }

    @Override
    protected void rewardTradeXp(@NotNull MerchantOffer offer) {
        // No XP rewards for this merchant.
    }

    @Override
    public boolean showProgressBar() {
        return false;
    }

    @Nullable
    @Override
    public AgeableMob getBreedOffspring(net.minecraft.server.level.@NotNull ServerLevel level, @NotNull AgeableMob partner) {
        return null;
    }

    @Override
    public void registerControllers(AnimatableManager.ControllerRegistrar controllers) {
        controllers.add(new AnimationController<>(this, "movement", 4, this::animationPredicate));

        AnimationController<IvyTheDragonMerchant> actionController =
                new AnimationController<>(this, "action", 3, state -> PlayState.CONTINUE);
        setupActionController(actionController);
        controllers.add(actionController);
    }

    private <T extends GeoEntity> PlayState animationPredicate(AnimationState<T> state) {
        if (isTrading() || tradeAnimState != TradeAnimState.NONE) {
            wasMovementStopped = true;
            return PlayState.STOP;
        }

        // Reset animation when resuming from stopped state
        if (wasMovementStopped) {
            state.getController().forceAnimationReset();
            wasMovementStopped = false;
        }

        if (state.isMoving()) {
            state.setAndContinue(RawAnimation.begin().thenLoop("ivy_oleander.animation.walk"));
        } else {
            state.setAndContinue(RawAnimation.begin().thenLoop("ivy_oleander.animation.idle"));
        }
        return PlayState.CONTINUE;
    }

    @Override
    public void setTradingPlayer(@Nullable Player player) {
        super.setTradingPlayer(player);
        if (!level().isClientSide) {
            boolean trading = player != null;
            if (trading && !lastTradingState) {
                startTradingSequence();
            } else if (!trading && lastTradingState) {
                stopTradingSequence();
            }
            lastTradingState = trading;
        }
    }

    @Override
    protected void stopTrading() {
        super.stopTrading();
        if (!level().isClientSide) {
            stopTradingSequence();
            lastTradingState = false;
        }
    }

    @Override
    public void tick() {
        super.tick();
        updateRotationDeviation();
        if (!level().isClientSide) {
            tickTradingAnimation();
            if (bodyControl != null) {
                bodyControl.serverTick();
            }
        }
    }

    private void updateRotationDeviation() {
        float headToBody = (float) (Mth.wrapDegrees(this.yHeadRot - this.yBodyRot) * 0.25);
        bodyRotDeviation.setTo(headToBody);
        bodyRotDeviation.update(0.25f);
    }

    private void startTradingSequence() {
        tradeAnimState = TradeAnimState.START;
        tradeAnimTicks = 0;
        triggerAnim("action", "trade_start");
    }

    private void stopTradingSequence() {
        triggerAnim("action", "trade_stop");
        tradeAnimState = TradeAnimState.NONE;
    }

    private void setupActionController(AnimationController<IvyTheDragonMerchant> controller) {
        controller.triggerableAnim("trade_start",
                RawAnimation.begin().thenPlay("ivy_oleander.animation.trade_start"));
        controller.triggerableAnim("trading",
                RawAnimation.begin().thenLoop("ivy_oleander.animation.trading"));
        controller.triggerableAnim("trade_stop",
                RawAnimation.begin().thenPlay("ivy_oleander.animation.trade_stop"));
    }

    @Override
    public AnimatableInstanceCache getAnimatableInstanceCache() {
        return animationCache;
    }

    private void tickTradingAnimation() {
        if (tradeAnimState != TradeAnimState.START) {
            return;
        }
        tradeAnimTicks++;
        if (tradeAnimTicks >= TRADE_START_TICKS) {
            triggerAnim("action", "trading");
            tradeAnimState = TradeAnimState.LOOP;
        }
    }

    private enum TradeAnimState {
        NONE,
        START,
        LOOP
    }

    private class TradingStillGoal extends Goal {
        TradingStillGoal() {
            setFlags(EnumSet.of(Goal.Flag.MOVE));
        }

        @Override
        public boolean canUse() {
            return isTrading();
        }

        @Override
        public void start() {
            getNavigation().stop();
            setDeltaMovement(0.0, 0.0, 0.0);
        }

        @Override
        public void tick() {
            getNavigation().stop();
            setDeltaMovement(0.0, 0.0, 0.0);
        }
    }

    private static MerchantOffer createIgnivorusEggTrade(RandomSource random) {
        ItemStack result = createIgnivorusReward(random);
        return new MerchantOffer(new ItemStack(ModItems.IGNIVORUS_EGG.get()), result, 3, 5, 0.05f);
    }

    private static ItemStack createIgnivorusReward(RandomSource random) {
        int roll = random.nextInt(8);
        return switch (roll) {
            case 0 -> enchantedNetheriteSword(random);
            case 1 -> enchantedNetheriteAxe(random);
            case 2 -> enchantedNetheritePickaxe(random);
            case 3 -> enchantedNetheriteArmor(random, Items.NETHERITE_CHESTPLATE);
            case 4 -> enchantedNetheriteArmor(random, Items.NETHERITE_LEGGINGS);
            case 5 -> enchantedNetheriteArmor(random, Items.NETHERITE_HELMET);
            case 6 -> enchantedNetheriteArmor(random, Items.NETHERITE_BOOTS);
            default -> enchantedNetheriteHoe(random);
        };
    }

    private static ItemStack enchantedNetheriteSword(RandomSource random) {
        ItemStack stack = new ItemStack(Items.NETHERITE_SWORD);
        stack.enchant(Enchantments.SHARPNESS, 5);
        stack.enchant(Enchantments.UNBREAKING, 3);
        if (random.nextFloat() < 0.35f) {
            stack.enchant(Enchantments.FIRE_ASPECT, 2);
        }
        if (random.nextFloat() < 0.2f) {
            stack.enchant(Enchantments.MOB_LOOTING, 3);
        }
        return stack;
    }

    private static ItemStack enchantedNetheriteAxe(RandomSource random) {
        ItemStack stack = new ItemStack(Items.NETHERITE_AXE);
        stack.enchant(Enchantments.SHARPNESS, 5);
        stack.enchant(Enchantments.UNBREAKING, 3);
        if (random.nextFloat() < 0.4f) {
            stack.enchant(Enchantments.BLOCK_EFFICIENCY, 5);
        }
        return stack;
    }

    private static ItemStack enchantedNetheritePickaxe(RandomSource random) {
        ItemStack stack = new ItemStack(Items.NETHERITE_PICKAXE);
        stack.enchant(Enchantments.BLOCK_EFFICIENCY, 5);
        stack.enchant(Enchantments.UNBREAKING, 3);
        if (random.nextFloat() < 0.35f) {
            stack.enchant(Enchantments.SILK_TOUCH, 1);
        } else {
            stack.enchant(Enchantments.BLOCK_FORTUNE, 3);
        }
        return stack;
    }

    private static ItemStack enchantedNetheriteArmor(RandomSource random, Item item) {
        ItemStack stack = new ItemStack(item);
        stack.enchant(Enchantments.ALL_DAMAGE_PROTECTION, 4);
        stack.enchant(Enchantments.UNBREAKING, 3);
        if (random.nextFloat() < 0.25f) {
            stack.enchant(Enchantments.THORNS, 3);
        }
        return stack;
    }

    private static ItemStack enchantedNetheriteHoe(RandomSource random) {
        ItemStack stack = new ItemStack(Items.NETHERITE_HOE);
        stack.enchant(Enchantments.UNBREAKING, 3);
        if (random.nextFloat() < 0.45f) {
            stack.enchant(Enchantments.BLOCK_EFFICIENCY, 5);
        }
        return stack;
    }

    private static MerchantOffer createRaevyxEggTrade( RandomSource random) {
        ItemStack result = createRaevyxReward(random);
        return new MerchantOffer(new ItemStack(ModItems.RAEVYX_EGG.get()), result, 5, 5, 0.05f);
    }

    private static ItemStack createRaevyxReward(RandomSource random) {
        int roll = random.nextInt(8);
        return switch (roll) {
            case 0 -> enchantedDiamondSword(random);
            case 1 -> enchantedDiamondAxe(random);
            case 2 -> enchantedDiamondPickaxe(random);
            case 3 -> enchantedDiamondArmor(random, Items.DIAMOND_CHESTPLATE);
            case 4 -> enchantedDiamondArmor(random, Items.DIAMOND_LEGGINGS);
            case 5 -> enchantedDiamondArmor(random, Items.DIAMOND_HELMET);
            case 6 -> enchantedDiamondArmor(random, Items.DIAMOND_BOOTS);
            default -> enchantedDiamondHoe(random);
        };
    }

    private static ItemStack enchantedDiamondSword(RandomSource random) {
        ItemStack stack = new ItemStack(Items.DIAMOND_SWORD);
        stack.enchant(Enchantments.SHARPNESS, 4);
        stack.enchant(Enchantments.UNBREAKING, 3);
        if (random.nextFloat() < 0.3f) {
            stack.enchant(Enchantments.FIRE_ASPECT, 2);
        }
        if (random.nextFloat() < 0.2f) {
            stack.enchant(Enchantments.MOB_LOOTING, 2);
        }
        return stack;
    }

    private static ItemStack enchantedDiamondAxe(RandomSource random) {
        ItemStack stack = new ItemStack(Items.DIAMOND_AXE);
        stack.enchant(Enchantments.SHARPNESS, 4);
        stack.enchant(Enchantments.UNBREAKING, 3);
        if (random.nextFloat() < 0.35f) {
            stack.enchant(Enchantments.BLOCK_EFFICIENCY, 4);
        }
        return stack;
    }

    private static ItemStack enchantedDiamondPickaxe(RandomSource random) {
        ItemStack stack = new ItemStack(Items.DIAMOND_PICKAXE);
        stack.enchant(Enchantments.BLOCK_EFFICIENCY, 4);
        stack.enchant(Enchantments.UNBREAKING, 3);
        if (random.nextFloat() < 0.25f) {
            stack.enchant(Enchantments.SILK_TOUCH, 1);
        } else {
            stack.enchant(Enchantments.BLOCK_FORTUNE, 2);
        }
        return stack;
    }

    private static ItemStack enchantedDiamondArmor(RandomSource random, Item item) {
        ItemStack stack = new ItemStack(item);
        stack.enchant(Enchantments.ALL_DAMAGE_PROTECTION, 3);
        stack.enchant(Enchantments.UNBREAKING, 3);
        if (random.nextFloat() < 0.2f) {
            stack.enchant(Enchantments.THORNS, 2);
        }
        return stack;
    }

    private static ItemStack enchantedDiamondHoe(RandomSource random) {
        ItemStack stack = new ItemStack(Items.DIAMOND_HOE);
        stack.enchant(Enchantments.UNBREAKING, 3);
        if (random.nextFloat() < 0.45f) {
            stack.enchant(Enchantments.BLOCK_EFFICIENCY, 4);
        }
        return stack;
    }
}

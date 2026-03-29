// I know that we are upside down, so hold your tongue and hear me out

package com.leon.saintsdragons.server.entity.npc;

import com.leon.saintsdragons.common.registry.ModItems;
import com.leon.saintsdragons.server.entity.handler.HumanSoundHandler;
import com.leon.saintsdragons.server.entity.npc.handlers.IvySoundProfile;
import net.minecraft.world.entity.AgeableMob;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.world.entity.ai.goal.FloatGoal;
import net.minecraft.world.entity.ai.goal.LookAtPlayerGoal;
import net.minecraft.world.entity.ai.goal.RandomLookAroundGoal;
import net.minecraft.world.entity.ai.goal.RandomStrollGoal;
import net.minecraft.world.entity.ai.goal.AvoidEntityGoal;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.monster.Zombie;
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
import software.bernie.geckolib.core.keyframe.event.SoundKeyframeEvent;
import software.bernie.geckolib.util.GeckoLibUtil;

import javax.annotation.Nullable;

public class IvyTheDragonMerchant extends AbstractVillager implements GeoEntity {
    private static final EntityDataAccessor<Boolean> DATA_RUNNING =
            SynchedEntityData.defineId(IvyTheDragonMerchant.class, EntityDataSerializers.BOOLEAN);

    private static final int TRADE_START_TICKS = 29;
    private static final int TRADE_STOP_TICKS = 29;

    private static final VillagerTrades.ItemListing[] TRADES = new VillagerTrades.ItemListing[]{
            (trader, random) -> createIgnivorusEggTrade(random),
            (trader, random) -> createRaevyxEggTrade(random),
            (trader, random) -> createVarasuchusEggTrade(random),
            (trader, random) -> createCindervaneEggTrade(random),
            (trader, random) -> createStegonautEggTrade(random)
    };
    private static final int HEARTY_MEAL_EGG_COUNT = 4;
    private static final int HEARTY_MEAL_SALMON_COUNT = 4;
    private static final int HEARTY_MEAL_OUTPUT_COUNT = 6;
    private static final int HEARTY_MEAL_MAX_USES = 9999;

    private static final int IDLE_VARIANT_DURATION = 66;
    private static final int IDLE_VARIANT_MIN_COOLDOWN = 200;
    private static final int IDLE_VARIANT_MAX_COOLDOWN = 600;

    private final AnimatableInstanceCache animationCache = GeckoLibUtil.createInstanceCache(this);
    private TradeAnimState tradeAnimState = TradeAnimState.NONE;
    private int tradeAnimTicks = 0;
    private boolean lastTradingState = false;
    private boolean wasMovementStopped = false;
    private BodyControl bodyControl;
    public final SmoothValue bodyRotDeviation = SmoothValue.rotation(0.0);

    private boolean playingIdleVariant = false;
    private int idleVariantTicks = 0;
    private int idleVariantCooldown = 0;

    private final HumanSoundHandler soundHandler;

    private int restockTimer = getRestockInterval();

    public IvyTheDragonMerchant(EntityType<? extends AbstractVillager> entityType, Level level) {
        super(entityType, level);
        this.setPersistenceRequired();
        this.lookControl = new HumanLookControl(this);
        this.soundHandler = new HumanSoundHandler(this, new IvySoundProfile());
        this.idleVariantCooldown = IDLE_VARIANT_MIN_COOLDOWN + random.nextInt(IDLE_VARIANT_MAX_COOLDOWN - IDLE_VARIANT_MIN_COOLDOWN);
    }

    @Override
    public boolean removeWhenFarAway(double distanceToClosestPlayer) {
        return false;
    }

    @Override
    public boolean requiresCustomPersistence() {
        return true;
    }

    @Override
    protected void defineSynchedData() {
        super.defineSynchedData();
        this.entityData.define(DATA_RUNNING, false);
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
        goalSelector.addGoal(2, new FleeFromZombiesGoal());
        goalSelector.addGoal(3, new RandomStrollGoal(this, 0.6));
        goalSelector.addGoal(4, new LookAtPlayerGoal(this, Player.class, 6.0f));
        goalSelector.addGoal(5, new RandomLookAroundGoal(this));
    }

    @Override
    protected @NotNull BodyRotationControl createBodyControl() {
        this.bodyControl = new BodyControl(this, 0.45f, 50.0f, 0.22f, 0.12f, 30.0f);
        return this.bodyControl;
    }


    public void setRunning(boolean running) {
        this.entityData.set(DATA_RUNNING, running);
    }

    public boolean isRunning() {
        return this.entityData.get(DATA_RUNNING);
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
        addFixedOffers(offers);
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
    }

    @Override
    public boolean showProgressBar() {
        return false;
    }

    @Override
    public net.minecraft.sounds.SoundEvent getNotifyTradeSound() {
        return null;
    }

    @Override
    public void notifyTrade(@NotNull MerchantOffer offer) {
        offer.increaseUses();
    }

    @Override
    public void playCelebrateSound() {
    }

    @Override
    public void overrideOffers(@NotNull MerchantOffers offers) {}

    @Override
    public void playSound(@NotNull net.minecraft.sounds.SoundEvent sound, float volume, float pitch) {
        if (sound == net.minecraft.sounds.SoundEvents.VILLAGER_YES ||
            sound == net.minecraft.sounds.SoundEvents.VILLAGER_NO ||
            sound == net.minecraft.sounds.SoundEvents.VILLAGER_AMBIENT ||
            sound == net.minecraft.sounds.SoundEvents.VILLAGER_TRADE ||
            sound == net.minecraft.sounds.SoundEvents.VILLAGER_CELEBRATE) {
            return;
        }
        super.playSound(sound, volume, pitch);
    }

    @Override
    protected net.minecraft.sounds.SoundEvent getAmbientSound() {
        return null;
    }

    @Override
    protected net.minecraft.sounds.SoundEvent getHurtSound(@NotNull net.minecraft.world.damagesource.DamageSource source) {
        return net.minecraft.sounds.SoundEvents.PLAYER_HURT;
    }

    @Override
    protected net.minecraft.sounds.SoundEvent getDeathSound() {
        return net.minecraft.sounds.SoundEvents.PLAYER_DEATH;
    }

    @Nullable
    @Override
    public AgeableMob getBreedOffspring(net.minecraft.server.level.@NotNull ServerLevel level, @NotNull AgeableMob partner) {
        return null;
    }

    @Override
    public void registerControllers(AnimatableManager.ControllerRegistrar controllers) {
        AnimationController<IvyTheDragonMerchant> movementController =
                new AnimationController<>(this, "movement", 4, this::animationPredicate);
        movementController.setSoundKeyframeHandler(this::handleSoundKeyframe);
        controllers.add(movementController);
        AnimationController<IvyTheDragonMerchant> actionController =
                new AnimationController<>(this, "action", 3, state -> PlayState.CONTINUE);
        actionController.setSoundKeyframeHandler(this::handleSoundKeyframe);
        setupActionController(actionController);
        controllers.add(actionController);
    }

    private void handleSoundKeyframe(SoundKeyframeEvent<IvyTheDragonMerchant> event) {
        soundHandler.handleAnimationSound(event.getKeyframeData(), event.getController());
    }

    private <T extends GeoEntity> PlayState animationPredicate(AnimationState<T> state) {
        if (isTrading() || tradeAnimState != TradeAnimState.NONE || playingIdleVariant) {
            wasMovementStopped = true;
            return PlayState.STOP;
        }

        if (wasMovementStopped) {
            state.getController().forceAnimationReset();
            wasMovementStopped = false;
        }

        if (state.isMoving()) {
            if (isRunning()) {
                state.setAndContinue(RawAnimation.begin().thenLoop("ivy_oleander.animation.run"));
            } else {
                state.setAndContinue(RawAnimation.begin().thenLoop("ivy_oleander.animation.walk"));
            }
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
            tickIdleVariant();
            tickRestocking();
            if (bodyControl != null) {
                bodyControl.serverTick();
            }
        }
    }

    private void tickRestocking() {
        int interval = Math.max(1, getRestockInterval());
        if (restockTimer > interval) {
            restockTimer = interval;
        }
        restockTimer--;
        if (restockTimer <= 0) {
            this.offers = new MerchantOffers();
            addFixedOffers(this.offers);
            for (VillagerTrades.ItemListing listing : TRADES) {
                MerchantOffer offer = listing.getOffer(this, this.random);
                if (offer != null) {
                    this.offers.add(offer);
                }
            }
            restockTimer = interval;
        }
    }

    private static void addFixedOffers(MerchantOffers offers) {
        if (!hasHeartyMealOffer(offers)) {
            offers.add(createHeartyMealOffer());
        }
    }

    private static boolean hasHeartyMealOffer(MerchantOffers offers) {
        for (MerchantOffer offer : offers) {
            ItemStack costA = offer.getBaseCostA();
            ItemStack costB = offer.getCostB();
            ItemStack result = offer.getResult();
            if (costA.is(ModItems.STEGONAUT_EGG.get())
                    && costA.getCount() == HEARTY_MEAL_EGG_COUNT
                    && costB.is(Items.SALMON)
                    && costB.getCount() == HEARTY_MEAL_SALMON_COUNT
                    && result.is(ModItems.HEARTY_DRAGON_MEAL.get())
                    && result.getCount() == HEARTY_MEAL_OUTPUT_COUNT) {
                return true;
            }
        }
        return false;
    }

    private static MerchantOffer createHeartyMealOffer() {
        ItemStack eggs = new ItemStack(Items.EGG, HEARTY_MEAL_EGG_COUNT);
        ItemStack salmon = new ItemStack(Items.SALMON, HEARTY_MEAL_SALMON_COUNT);
        ItemStack result = new ItemStack(ModItems.HEARTY_DRAGON_MEAL.get(), HEARTY_MEAL_OUTPUT_COUNT);
        return new MerchantOffer(eggs, salmon, result, HEARTY_MEAL_MAX_USES, 0, 0.0f);
    }
    private static int getRestockInterval() {
        try {
            Class<?> forgeConfig = Class.forName("com.leon.saintsdragons.forge.platform.ForgeDragonAttributesConfig");
            java.lang.reflect.Field field = forgeConfig.getField("IVY_RESTOCK_INTERVAL");
            Object configValue = field.get(null);
            java.lang.reflect.Method getMethod = configValue.getClass().getMethod("get");
            Object value = getMethod.invoke(configValue);
            return value instanceof Number ? ((Number) value).intValue() : 24000;
        } catch (Exception ignored) {
        }

        try {
            Class<?> fabricConfig = Class.forName("com.leon.saintsdragons.fabric.config.SaintsDragonsFabricConfig");
            Class<?> autoConfig = Class.forName("me.shedaniel.autoconfig.AutoConfig");
            java.lang.reflect.Method getConfigHolder = autoConfig.getMethod("getConfigHolder", Class.class);
            Object holder = getConfigHolder.invoke(null, fabricConfig);
            java.lang.reflect.Method getConfig = holder.getClass().getMethod("getConfig");
            Object instance = getConfig.invoke(holder);
            java.lang.reflect.Field field = instance.getClass().getField("ivyRestockInterval");
            Object value = field.get(instance);
            return value instanceof Number ? ((Number) value).intValue() : 24000;
        } catch (Exception ignored) {
        }

        return 24000; // 20 minutes
    }

    private void tickIdleVariant() {
        if (isTrading() || tradeAnimState != TradeAnimState.NONE || getDeltaMovement().horizontalDistanceSqr() > 0.001) {
            return;
        }

        if (playingIdleVariant) {
            idleVariantTicks++;
            if (idleVariantTicks >= IDLE_VARIANT_DURATION) {
                playingIdleVariant = false;
                idleVariantTicks = 0;
                idleVariantCooldown = IDLE_VARIANT_MIN_COOLDOWN + random.nextInt(IDLE_VARIANT_MAX_COOLDOWN - IDLE_VARIANT_MIN_COOLDOWN);
            }
        } else if (idleVariantCooldown > 0) {
            idleVariantCooldown--;
        } else if (random.nextInt(100) < 5) {
            startIdleVariant();
        }
    }

    private void startIdleVariant() {
        playingIdleVariant = true;
        idleVariantTicks = 0;
        triggerAnim("action", "idle_variant1");
    }

    private void updateRotationDeviation() {
        float headToBody = (float) (Mth.wrapDegrees(this.yHeadRot - this.yBodyRot) * 0.25);
        bodyRotDeviation.setTo(headToBody);
        bodyRotDeviation.update(0.25f);
    }

    private void startTradingSequence() {
        tradeAnimState = TradeAnimState.START;
        tradeAnimTicks = TRADE_START_TICKS;
        triggerAnim("action", "trade_start");
    }

    private void stopTradingSequence() {
        tradeAnimState = TradeAnimState.STOP;
        tradeAnimTicks = TRADE_STOP_TICKS;
        triggerAnim("action", "trade_stop");
    }

    private void setupActionController(AnimationController<IvyTheDragonMerchant> controller) {
        controller.triggerableAnim("trade_start",
                RawAnimation.begin().thenPlay("ivy_oleander.animation.trade_start"));
        controller.triggerableAnim("trading",
                RawAnimation.begin().thenLoop("ivy_oleander.animation.trading"));
        controller.triggerableAnim("trade_stop",
                RawAnimation.begin().thenPlay("ivy_oleander.animation.trade_stop"));
        controller.triggerableAnim("idle_variant1",
                RawAnimation.begin().thenPlay("ivy_oleander.animation.idle_variant1"));
    }

    @Override
    public AnimatableInstanceCache getAnimatableInstanceCache() {
        return animationCache;
    }

    private void tickTradingAnimation() {
        if (tradeAnimState == TradeAnimState.START) {
            if (tradeAnimTicks > 0) {
                tradeAnimTicks--;

                if (tradeAnimTicks == 3) {
                    triggerAnim("action", "trading");
                }
                if (tradeAnimTicks > 0) {
                    return;
                }
            }
            tradeAnimState = TradeAnimState.LOOP;
        } else if (tradeAnimState == TradeAnimState.STOP) {
            if (tradeAnimTicks > 0) {
                tradeAnimTicks--;
                if (tradeAnimTicks > 0) {
                    return;
                }
            }
            tradeAnimState = TradeAnimState.NONE;
        }
    }

    public HumanSoundHandler getSoundHandler() {
        return soundHandler;
    }

    private enum TradeAnimState {
        NONE,
        START,
        LOOP,
        STOP
    }

    private class TradingStillGoal extends Goal {
        TradingStillGoal() {
            setFlags(EnumSet.of(Goal.Flag.MOVE));
        }

        @Override
        public boolean canUse() {
            return isTrading() || playingIdleVariant;
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

    private class FleeFromZombiesGoal extends AvoidEntityGoal<Zombie> {
        FleeFromZombiesGoal() {
            super(IvyTheDragonMerchant.this, Zombie.class, 8.0F, 0.8, 1.2);
        }

        @Override
        public void start() {
            setRunning(true);
            super.start();
        }

        @Override
        public void stop() {
            setRunning(false);
            super.stop();
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

    private static MerchantOffer createCindervaneEggTrade(RandomSource random) {
        ItemStack result = createCindervaneReward(random);
        return new MerchantOffer(new ItemStack(ModItems.CINDERVANE_EGG.get()), result, 8, 5, 0.05f);
    }

    private static ItemStack createCindervaneReward(RandomSource random) {
        int roll = random.nextInt(12);
        return switch (roll) {
            case 0 -> enchantedIronSword(random);
            case 1 -> enchantedIronAxe(random);
            case 2 -> enchantedIronPickaxe(random);
            case 3 -> enchantedIronArmor(random, Items.IRON_CHESTPLATE);
            case 4 -> enchantedIronArmor(random, Items.IRON_LEGGINGS);
            case 5 -> enchantedIronArmor(random, Items.IRON_HELMET);
            case 6 -> enchantedIronArmor(random, Items.IRON_BOOTS);
            case 7 -> new ItemStack(Items.BLAZE_ROD, 4 + random.nextInt(5));
            case 8 -> new ItemStack(Items.MAGMA_CREAM, 8 + random.nextInt(9));
            case 9 -> new ItemStack(Items.FIRE_CHARGE, 12 + random.nextInt(13));
            case 10 -> new ItemStack(Items.ENDER_PEARL, 4 + random.nextInt(5));
            default -> enchantedIronHoe(random);
        };
    }

    private static ItemStack enchantedIronSword(RandomSource random) {
        ItemStack stack = new ItemStack(Items.IRON_SWORD);
        stack.enchant(Enchantments.SHARPNESS, 3);
        stack.enchant(Enchantments.UNBREAKING, 2);
        if (random.nextFloat() < 0.3f) {
            stack.enchant(Enchantments.FIRE_ASPECT, 1);
        }
        if (random.nextFloat() < 0.15f) {
            stack.enchant(Enchantments.MOB_LOOTING, 2);
        }
        return stack;
    }

    private static ItemStack enchantedIronAxe(RandomSource random) {
        ItemStack stack = new ItemStack(Items.IRON_AXE);
        stack.enchant(Enchantments.SHARPNESS, 3);
        stack.enchant(Enchantments.UNBREAKING, 2);
        if (random.nextFloat() < 0.35f) {
            stack.enchant(Enchantments.BLOCK_EFFICIENCY, 3);
        }
        return stack;
    }

    private static ItemStack enchantedIronPickaxe(RandomSource random) {
        ItemStack stack = new ItemStack(Items.IRON_PICKAXE);
        stack.enchant(Enchantments.BLOCK_EFFICIENCY, 3);
        stack.enchant(Enchantments.UNBREAKING, 2);
        if (random.nextFloat() < 0.3f) {
            stack.enchant(Enchantments.SILK_TOUCH, 1);
        } else if (random.nextFloat() < 0.4f) {
            stack.enchant(Enchantments.BLOCK_FORTUNE, 2);
        }
        return stack;
    }

    private static ItemStack enchantedIronArmor(RandomSource random, Item item) {
        ItemStack stack = new ItemStack(item);
        stack.enchant(Enchantments.ALL_DAMAGE_PROTECTION, 2);
        stack.enchant(Enchantments.UNBREAKING, 2);
        if (random.nextFloat() < 0.2f) {
            stack.enchant(Enchantments.THORNS, 1);
        }
        return stack;
    }

    private static ItemStack enchantedIronHoe(RandomSource random) {
        ItemStack stack = new ItemStack(Items.IRON_HOE);
        stack.enchant(Enchantments.UNBREAKING, 2);
        if (random.nextFloat() < 0.4f) {
            stack.enchant(Enchantments.BLOCK_EFFICIENCY, 3);
        }
        return stack;
    }

    private static MerchantOffer createVarasuchusEggTrade(RandomSource random) {
        ItemStack result = createVarasuchusReward(random);
        return new MerchantOffer(new ItemStack(ModItems.VARASUCHUS_EGG.get()), result, 6, 5, 0.05f);
    }

    private static ItemStack createVarasuchusReward(RandomSource random) {
        int roll = random.nextInt(11);
        return switch (roll) {
            case 0 -> new ItemStack(Items.DIAMOND_SWORD);
            case 1 -> new ItemStack(Items.DIAMOND_AXE);
            case 2 -> new ItemStack(Items.DIAMOND_PICKAXE);
            case 3 -> new ItemStack(Items.DIAMOND_SHOVEL);
            case 4 -> new ItemStack(Items.DIAMOND_CHESTPLATE);
            case 5 -> new ItemStack(Items.DIAMOND_LEGGINGS);
            case 6 -> new ItemStack(Items.DIAMOND_HELMET);
            case 7 -> new ItemStack(Items.DIAMOND_BOOTS);
            case 8 -> new ItemStack(Items.DIAMOND, 3 + random.nextInt(4));
            case 9 -> new ItemStack(Items.TRIDENT);
            default -> enchantedTrident(random);
        };
    }

    private static ItemStack enchantedTrident(RandomSource random) {
        ItemStack stack = new ItemStack(Items.TRIDENT);
        int enchantRoll = random.nextInt(4);
        switch (enchantRoll) {
            case 0 -> stack.enchant(Enchantments.IMPALING, 3);
            case 1 -> stack.enchant(Enchantments.LOYALTY, 2);
            case 2 -> stack.enchant(Enchantments.RIPTIDE, 2);
            default -> stack.enchant(Enchantments.UNBREAKING, 2);
        }
        return stack;
    }

    private static MerchantOffer createStegonautEggTrade(RandomSource random) {
        ItemStack result = createStegonautReward(random);
        return new MerchantOffer(new ItemStack(ModItems.STEGONAUT_EGG.get()), result, 12, 5, 0.05f);
    }

    private static ItemStack createStegonautReward(RandomSource random) {
        int roll = random.nextInt(12);
        return switch (roll) {
            case 0 -> new ItemStack(Items.IRON_INGOT, 16 + random.nextInt(17));
            case 1 -> new ItemStack(Items.COAL, 16 + random.nextInt(33));
            case 2 -> new ItemStack(Items.GOLD_INGOT, 8 + random.nextInt(9));
            case 3 -> new ItemStack(Items.REDSTONE, 16 + random.nextInt(9));
            case 4 -> new ItemStack(Items.LAPIS_LAZULI, 8 + random.nextInt(9));
            case 5 -> new ItemStack(Items.COOKED_BEEF, 16 + random.nextInt(17));
            case 6 -> new ItemStack(Items.GOLDEN_CARROT, 8 + random.nextInt(9));
            case 7 -> new ItemStack(Items.BREAD, 24 + random.nextInt(25));
            case 8 -> new ItemStack(Items.ARROW, 32 + random.nextInt(33));
            case 9 -> new ItemStack(Items.TORCH, 1 + random.nextInt(64));
            case 10 -> new ItemStack(Items.SCAFFOLDING, 32 + random.nextInt(33));
            default -> new ItemStack(Items.EMERALD, 4 + random.nextInt(5));
        };
    }
}

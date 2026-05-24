// I know that we are upside down, so hold your tongue and hear me out

package com.leon.saintsdragons.server.entity.npc;

import com.leon.saintsdragons.common.config.SaintsDragonsConfig;
import com.leon.saintsdragons.common.registry.ModItems;
import com.leon.saintsdragons.server.entity.dragons.util.DragonUtilities;
import com.leon.saintsdragons.util.animation.MovementAnimationHelper;
import com.leon.saintsdragons.server.entity.handler.HumanSoundHandler;
import com.leon.saintsdragons.server.entity.npc.handlers.IvySoundProfile;
import com.leon.saintsdragons.server.entity.npc.trade.IvyTradeRegistry;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.damagesource.DamageSource;
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
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.trading.MerchantOffer;
import net.minecraft.world.item.trading.MerchantOffers;
import net.minecraft.world.level.Level;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.stats.Stats;
import net.minecraft.world.item.Item;
import java.util.EnumSet;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;
import org.jetbrains.annotations.NotNull;
import net.minecraft.util.Mth;
import com.leon.saintsdragons.server.entity.controller.BodyControl;
import com.leon.saintsdragons.server.entity.controller.GenericLookControl;
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
    private static final EntityDataAccessor<Integer> DATA_TRADE_ANIM_STATE =
            SynchedEntityData.defineId(IvyTheDragonMerchant.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Boolean> DATA_IDLE_VARIANT_ACTIVE =
            SynchedEntityData.defineId(IvyTheDragonMerchant.class, EntityDataSerializers.BOOLEAN);
    private static final int TRADE_START_TICKS = 29;
    private static final int TRADE_STOP_TICKS = 29;
    private static final int IDLE_VARIANT_DURATION = 66;
    private static final int IDLE_VARIANT_MIN_COOLDOWN = 200;
    private static final int IDLE_VARIANT_MAX_COOLDOWN = 600;
    private static final int GREETING_DURATION_TICKS = 54;
    private static final int EGG_REACTION_MIN_LOOK_TICKS = 20;
    private static final int EGG_REACTION_MAX_LOOK_TICKS = 40;
    private static final int EGG_REACTION_DURATION_TICKS = 66;
    private static final int EGG_REACTION_COOLDOWN_TICKS = 20;
    private static final double EGG_REACTION_RANGE = 6.0;
    private static final float EGG_REACTION_LOOK_YAW_SPEED = 30.0f;
    private static final float EGG_REACTION_LOOK_PITCH_SPEED = 20.0f;
    private static final float EGG_REACTION_FACE_THRESHOLD = 15.0f;
    private static final RawAnimation TRADE_START = RawAnimation.begin().thenPlay("ivy_oleander.animation.trade_start");
    private static final RawAnimation TRADING = RawAnimation.begin().thenLoop("ivy_oleander.animation.trading");
    private static final RawAnimation TRADE_STOP = RawAnimation.begin().thenPlay("ivy_oleander.animation.trade_stop");
    private static final RawAnimation IDLE = RawAnimation.begin().thenLoop("ivy_oleander.animation.idle");
    private static final RawAnimation WALK = RawAnimation.begin().thenLoop("ivy_oleander.animation.walk");
    private static final RawAnimation RUN = RawAnimation.begin().thenLoop("ivy_oleander.animation.run");
    private static final String GREETED_PLAYERS_TAG = "GreetedPlayers";
    private static final String UNLOCKED_TRADERS_TAG = "UnlockedTraders";
    private static final String UNLOCKED_TRADER_UUID_TAG = "UUID";
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
    private int greetingTicks = 0;
    private EggReactionState eggReactionState = EggReactionState.NONE;
    private int eggReactionTicks = 0;
    private int eggReactionCooldown = 0;
    private int eggReactionTargetId = -1;
    private UUID pendingUnlockPlayerUuid;
    private boolean openTradeAfterReaction;
    private final Set<UUID> greetedPlayerUuids = new HashSet<>();
    private final Set<UUID> unlockedTraderUuids = new HashSet<>();
    private final HumanSoundHandler soundHandler;
    private final int restockInterval;
    private int restockTimer;

    public IvyTheDragonMerchant(EntityType<? extends AbstractVillager> entityType, Level level) {
        super(entityType, level);
        this.setPersistenceRequired();
        this.lookControl = new GenericLookControl(this);
        this.soundHandler = new HumanSoundHandler(this, new IvySoundProfile());
        this.restockInterval = Math.max(1, resolveRestockInterval());
        this.restockTimer = this.restockInterval;
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
        this.entityData.define(DATA_TRADE_ANIM_STATE, TradeAnimState.NONE.id);
        this.entityData.define(DATA_IDLE_VARIANT_ACTIVE, false);
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

    private TradeAnimState getTradeAnimState() {
        return TradeAnimState.byId(this.entityData.get(DATA_TRADE_ANIM_STATE));
    }

    private void setTradeAnimState(TradeAnimState state) {
        this.tradeAnimState = state;
        this.entityData.set(DATA_TRADE_ANIM_STATE, state.id);
    }

    private boolean isIdleVariantActive() {
        return this.entityData.get(DATA_IDLE_VARIANT_ACTIVE);
    }

    private void setIdleVariantActive(boolean active) {
        this.playingIdleVariant = active;
        this.entityData.set(DATA_IDLE_VARIANT_ACTIVE, active);
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
        IvyTradeRegistry.fillOffers(this, this.random, offers);
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
        if (level().isClientSide) {
            if (hasTradingAccess(player) || isReactionEgg(stack)) {
                return InteractionResult.sidedSuccess(true);
            }
            return InteractionResult.CONSUME;
        }
        if (isTrading() || getTradeAnimState() != TradeAnimState.NONE || eggReactionState != EggReactionState.NONE || greetingTicks > 0) {
            return InteractionResult.CONSUME;
        }
        if (player instanceof ServerPlayer serverPlayer) {
            DragonUtilities.awardAdvancement(serverPlayer, "meet_ivy", "meet_ivy");
        }
        if (hasTradingAccess(player)) {
            openTradingFor(player);
            return InteractionResult.CONSUME;
        }
        if (isReactionEgg(stack)) {
            startEggReaction(player, true);
            return InteractionResult.CONSUME;
        }
        if (!hasSeenGreeting(player)) {
            startGreeting(player);
        }
        player.displayClientMessage(Component.translatable("entity.saintsdragons.ivy.first_encounter"), false);
        return InteractionResult.CONSUME;
    }

    @Override
    protected void rewardTradeXp(@NotNull MerchantOffer offer) {
    }

    @Override
    public boolean showProgressBar() {
        return false;
    }

    @Override
    public SoundEvent getNotifyTradeSound() {
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
    public void playSound(@NotNull SoundEvent sound, float volume, float pitch) {
        if (sound == SoundEvents.VILLAGER_YES ||
            sound == SoundEvents.VILLAGER_NO ||
            sound == SoundEvents.VILLAGER_AMBIENT ||
            sound == SoundEvents.VILLAGER_TRADE ||
            sound == SoundEvents.VILLAGER_CELEBRATE) {
            return;
        }
        super.playSound(sound, volume, pitch);
    }

    @Override
    protected SoundEvent getAmbientSound() {
        return null;
    }

    @Override
    protected SoundEvent getHurtSound(@NotNull DamageSource source) {
        return SoundEvents.PLAYER_HURT;
    }

    @Override
    protected SoundEvent getDeathSound() {
        return SoundEvents.PLAYER_DEATH;
    }

    @Nullable
    @Override
    public AgeableMob getBreedOffspring(@NotNull ServerLevel level, @NotNull AgeableMob partner) {
        return null;
    }

    @Override
    public void registerControllers(AnimatableManager.ControllerRegistrar controllers) {
        AnimationController<IvyTheDragonMerchant> movementController =
                new AnimationController<>(this, "movement", 4, this::animationPredicate);
        movementController.setSoundKeyframeHandler(this::handleSoundKeyframe);
        controllers.add(movementController);
        AnimationController<IvyTheDragonMerchant> actionController =
                new AnimationController<>(this, "action", 3, this::actionPredicate);
        actionController.setSoundKeyframeHandler(this::handleSoundKeyframe);
        setupActionController(actionController);
        controllers.add(actionController);
    }

    private void handleSoundKeyframe(SoundKeyframeEvent<IvyTheDragonMerchant> event) {
        soundHandler.handleAnimationSound(event.getKeyframeData(), event.getController());
    }

    private <T extends GeoEntity> PlayState animationPredicate(AnimationState<T> state) {
        if (isTrading() || getTradeAnimState() != TradeAnimState.NONE || isIdleVariantActive() || greetingTicks > 0 || eggReactionState != EggReactionState.NONE) {
            wasMovementStopped = true;
            return PlayState.STOP;
        }

        if (wasMovementStopped) {
            state.getController().forceAnimationReset();
            wasMovementStopped = false;
        }

        if (state.isMoving()) {
            if (isRunning()) {
                MovementAnimationHelper.setAndContinue(state, RUN);
            } else {
                MovementAnimationHelper.setAndContinue(state, WALK);
            }
        } else {
            MovementAnimationHelper.setAndContinue(state, IDLE);
        }
        return PlayState.CONTINUE;
    }

    private PlayState actionPredicate(AnimationState<IvyTheDragonMerchant> state) {
        state.getController().transitionLength(3);
        TradeAnimState tradeState = getTradeAnimState();
        if (tradeState == TradeAnimState.LOOP) {
            MovementAnimationHelper.setAndContinue(state, TRADING);
            return PlayState.CONTINUE;
        }
        if (tradeState == TradeAnimState.START || tradeState == TradeAnimState.STOP) {
            return PlayState.CONTINUE;
        }
        if (isIdleVariantActive()) {
            return PlayState.CONTINUE;
        }
        return PlayState.STOP;
    }

    @Override
    public void setTradingPlayer(@Nullable Player player) {
        super.setTradingPlayer(player);
        if (!level().isClientSide) {
            boolean trading = player != null;
            if (trading && !lastTradingState) {
                resetEggReaction();
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
            tickGreeting();
            tickEggReaction();
            tickIdleVariant();
            tickRestocking();
            if (bodyControl != null) {
                bodyControl.serverTick();
            }
        }
    }

    @Override
    public void addAdditionalSaveData(@NotNull CompoundTag tag) {
        super.addAdditionalSaveData(tag);
        ListTag greetedList = new ListTag();
        for (UUID uuid : greetedPlayerUuids) {
            CompoundTag greetedTag = new CompoundTag();
            greetedTag.putUUID(UNLOCKED_TRADER_UUID_TAG, uuid);
            greetedList.add(greetedTag);
        }
        tag.put(GREETED_PLAYERS_TAG, greetedList);
        ListTag unlockedList = new ListTag();
        for (UUID uuid : unlockedTraderUuids) {
            CompoundTag traderTag = new CompoundTag();
            traderTag.putUUID(UNLOCKED_TRADER_UUID_TAG, uuid);
            unlockedList.add(traderTag);
        }
        tag.put(UNLOCKED_TRADERS_TAG, unlockedList);
    }

    @Override
    public void readAdditionalSaveData(@NotNull CompoundTag tag) {
        super.readAdditionalSaveData(tag);
        greetedPlayerUuids.clear();
        if (tag.contains(GREETED_PLAYERS_TAG, Tag.TAG_LIST)) {
            ListTag greetedList = tag.getList(GREETED_PLAYERS_TAG, Tag.TAG_COMPOUND);
            for (int i = 0; i < greetedList.size(); i++) {
                CompoundTag greetedTag = greetedList.getCompound(i);
                if (greetedTag.hasUUID(UNLOCKED_TRADER_UUID_TAG)) {
                    greetedPlayerUuids.add(greetedTag.getUUID(UNLOCKED_TRADER_UUID_TAG));
                }
            }
        }
        unlockedTraderUuids.clear();
        if (!tag.contains(UNLOCKED_TRADERS_TAG, Tag.TAG_LIST)) {
            return;
        }
        ListTag unlockedList = tag.getList(UNLOCKED_TRADERS_TAG, Tag.TAG_COMPOUND);
        for (int i = 0; i < unlockedList.size(); i++) {
            CompoundTag traderTag = unlockedList.getCompound(i);
            if (traderTag.hasUUID(UNLOCKED_TRADER_UUID_TAG)) {
                unlockedTraderUuids.add(traderTag.getUUID(UNLOCKED_TRADER_UUID_TAG));
            }
        }
    }

    private void tickRestocking() {
        int interval = this.restockInterval;
        if (restockTimer > interval) {
            restockTimer = interval;
        }
        restockTimer--;
        if (restockTimer <= 0) {
            this.offers = new MerchantOffers();
            IvyTradeRegistry.fillOffers(this, this.random, this.offers);
            restockTimer = interval;
        }
    }

    private static int resolveRestockInterval() {
        return SaintsDragonsConfig.getIvyRestockInterval();
    }

    private void tickIdleVariant() {
        if (isTrading() || getTradeAnimState() != TradeAnimState.NONE || eggReactionState != EggReactionState.NONE || greetingTicks > 0 || getDeltaMovement().horizontalDistanceSqr() > 0.001) {
            return;
        }

        if (playingIdleVariant) {
            idleVariantTicks++;
            if (idleVariantTicks >= IDLE_VARIANT_DURATION) {
                setIdleVariantActive(false);
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
        setIdleVariantActive(true);
        idleVariantTicks = 0;
        triggerAnim("action", "idle_variant1");
    }

    private void tickGreeting() {
        if (greetingTicks <= 0) {
            return;
        }
        greetingTicks--;
        getNavigation().stop();
        setDeltaMovement(0.0, 0.0, 0.0);
    }

    private void tickEggReaction() {
        if (eggReactionState == EggReactionState.NONE && eggReactionCooldown > 0) {
            eggReactionCooldown--;
        }

        if (isTrading() || getTradeAnimState() != TradeAnimState.NONE) {
            if (eggReactionState != EggReactionState.NONE) {
                resetEggReaction();
            }
            return;
        }

        if (eggReactionState == EggReactionState.NONE) {
            return;
        }

        Player target = getEggReactionTarget();
        if (target == null) {
            resetEggReaction();
            return;
        }

        getNavigation().stop();
        setDeltaMovement(0.0, 0.0, 0.0);
        lookAtReactionTarget(target);

        if (eggReactionState == EggReactionState.PREPARE) {
            if (!isValidEggPresenter(target)) {
                resetEggReaction();
                return;
            }

            eggReactionTicks++;
            if ((eggReactionTicks >= EGG_REACTION_MIN_LOOK_TICKS && isFacingReactionTarget(target))
                    || eggReactionTicks >= EGG_REACTION_MAX_LOOK_TICKS) {
                eggReactionState = EggReactionState.PLAY;
                eggReactionTicks = EGG_REACTION_DURATION_TICKS;
                triggerAnim("action", "reaction_to_egg");
            }
            return;
        }

        if (eggReactionTicks > 0) {
            eggReactionTicks--;
        }

        if (eggReactionTicks <= 0) {
            eggReactionCooldown = EGG_REACTION_COOLDOWN_TICKS;
            completeEggReaction(target);
            resetEggReaction();
        }
    }

    private void startEggReaction(Player player, boolean unlockTradeAfterReaction) {
        setIdleVariantActive(false);
        idleVariantTicks = 0;
        greetingTicks = 0;
        eggReactionState = EggReactionState.PREPARE;
        eggReactionTicks = 0;
        eggReactionTargetId = player.getId();
        pendingUnlockPlayerUuid = player.getUUID();
        openTradeAfterReaction = unlockTradeAfterReaction;
        getNavigation().stop();
        setDeltaMovement(0.0, 0.0, 0.0);
        lookAtReactionTarget(player);
    }

    private void resetEggReaction() {
        eggReactionState = EggReactionState.NONE;
        eggReactionTicks = 0;
        eggReactionTargetId = -1;
        pendingUnlockPlayerUuid = null;
        openTradeAfterReaction = false;
    }

    @Nullable
    private Player getEggReactionTarget() {
        if (eggReactionTargetId < 0) {
            return null;
        }
        var entity = level().getEntity(eggReactionTargetId);
        return entity instanceof Player player ? player : null;
    }

    private boolean isValidEggPresenter(Player player) {
        return player.isAlive()
                && !player.isSpectator()
                && distanceToSqr(player) <= EGG_REACTION_RANGE * EGG_REACTION_RANGE
                && (isReactionEgg(player.getMainHandItem()) || isReactionEgg(player.getOffhandItem()));
    }

    private boolean isReactionEgg(ItemStack stack) {
        if (stack.isEmpty()) {
            return false;
        }
        Item item = stack.getItem();
        return item == ModItems.RAEVYX_EGG.get()
                || item == ModItems.IGNIVORUS_EGG.get()
                || item == ModItems.CINDERVANE_EGG.get()
                || item == ModItems.VARASUCHUS_EGG.get()
                || item == ModItems.STEGONAUT_EGG.get()
                || item == ModItems.VOLITANS_EGG.get();
    }

    private void lookAtReactionTarget(Player player) {
        getLookControl().setLookAt(player, EGG_REACTION_LOOK_YAW_SPEED, EGG_REACTION_LOOK_PITCH_SPEED);
        lookAt(player, EGG_REACTION_LOOK_YAW_SPEED, EGG_REACTION_LOOK_PITCH_SPEED);
    }

    private boolean isFacingReactionTarget(Player player) {
        double dx = player.getX() - getX();
        double dz = player.getZ() - getZ();
        if (dx * dx + dz * dz < 1.0E-4) {
            return true;
        }
        float targetYaw = (float) (Mth.atan2(dz, dx) * (180.0 / Math.PI)) - 90.0f;
        float headDelta = Math.abs(Mth.wrapDegrees(yHeadRot - targetYaw));
        float bodyDelta = Math.abs(Mth.wrapDegrees(yBodyRot - targetYaw));
        return headDelta <= EGG_REACTION_FACE_THRESHOLD || bodyDelta <= EGG_REACTION_FACE_THRESHOLD;
    }

    private boolean hasTradingAccess(Player player) {
        return unlockedTraderUuids.contains(player.getUUID());
    }

    private boolean hasSeenGreeting(Player player) {
        return greetedPlayerUuids.contains(player.getUUID());
    }

    private void startGreeting(Player player) {
        greetedPlayerUuids.add(player.getUUID());
        setIdleVariantActive(false);
        idleVariantTicks = 0;
        greetingTicks = GREETING_DURATION_TICKS;
        getNavigation().stop();
        setDeltaMovement(0.0, 0.0, 0.0);
        lookAtReactionTarget(player);
        triggerAnim("action", "greetings");
    }

    private void completeEggReaction(Player player) {
        if (pendingUnlockPlayerUuid == null || !pendingUnlockPlayerUuid.equals(player.getUUID())) {
            return;
        }
        unlockedTraderUuids.add(player.getUUID());
        player.displayClientMessage(Component.translatable("entity.saintsdragons.ivy.trade_unlocked"), true);
        if (openTradeAfterReaction && player.isAlive() && !player.isSpectator() && distanceToSqr(player) <= EGG_REACTION_RANGE * EGG_REACTION_RANGE) {
            openTradingFor(player);
        }
    }

    private void openTradingFor(Player player) {
        setTradingPlayer(player);
        openTradingScreen(player, getDisplayName(), 1);
        player.awardStat(Stats.TALKED_TO_VILLAGER);
    }

    private void updateRotationDeviation() {
        float headToBody = (float) (Mth.wrapDegrees(this.yHeadRot - this.yBodyRot) * 0.25);
        bodyRotDeviation.setTo(headToBody);
        bodyRotDeviation.update(0.25f);
    }

    private void startTradingSequence() {
        setTradeAnimState(TradeAnimState.START);
        tradeAnimTicks = TRADE_START_TICKS;
        triggerAnim("action", "trade_start");
    }

    private void stopTradingSequence() {
        setTradeAnimState(TradeAnimState.STOP);
        tradeAnimTicks = TRADE_STOP_TICKS;
        triggerAnim("action", "trade_stop");
    }

    private void setupActionController(AnimationController<IvyTheDragonMerchant> controller) {
        controller.triggerableAnim("trade_start", TRADE_START);
        controller.triggerableAnim("trading", TRADING);
        controller.triggerableAnim("trade_stop", TRADE_STOP);
        controller.triggerableAnim("greetings",
                RawAnimation.begin().thenPlay("ivy_oleander.animation.greetings"));
        controller.triggerableAnim("idle_variant1",
                RawAnimation.begin().thenPlay("ivy_oleander.animation.idle_variant1"));
        controller.triggerableAnim("reaction_to_egg",
                RawAnimation.begin().thenPlay("ivy_oleander.animation.reaction_to_egg"));
    }

    @Override
    public AnimatableInstanceCache getAnimatableInstanceCache() {
        return animationCache;
    }

    private void tickTradingAnimation() {
        TradeAnimState tradeState = getTradeAnimState();
        if (tradeState == TradeAnimState.START) {
            if (tradeAnimTicks > 0) {
                tradeAnimTicks--;
                if (tradeAnimTicks > 0) {
                    return;
                }
            }
            setTradeAnimState(TradeAnimState.LOOP);
            triggerAnim("action", "trading");
        } else if (tradeState == TradeAnimState.STOP) {
            if (tradeAnimTicks > 0) {
                tradeAnimTicks--;
                if (tradeAnimTicks > 0) {
                    return;
                }
            }
            setTradeAnimState(TradeAnimState.NONE);
        }
    }

    public HumanSoundHandler getSoundHandler() {
        return soundHandler;
    }

    public boolean shouldApplyHeadTracking() {
        return !isTrading()
                && getTradeAnimState() == TradeAnimState.NONE
                && greetingTicks <= 0
                && !isIdleVariantActive()
                && eggReactionState != EggReactionState.PLAY;
    }

    private enum EggReactionState {
        NONE,
        PREPARE,
        PLAY
    }

    private enum TradeAnimState {
        NONE(0),
        START(1),
        LOOP(2),
        STOP(3);

        private final int id;

        TradeAnimState(int id) {
            this.id = id;
        }

        private static TradeAnimState byId(int id) {
            for (TradeAnimState state : values()) {
                if (state.id == id) {
                    return state;
                }
            }
            return NONE;
        }
    }

    private class TradingStillGoal extends Goal {
        TradingStillGoal() {
            setFlags(EnumSet.of(Goal.Flag.MOVE));
        }

        @Override
        public boolean canUse() {
            return isTrading() || isIdleVariantActive() || greetingTicks > 0 || eggReactionState != EggReactionState.NONE;
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

}

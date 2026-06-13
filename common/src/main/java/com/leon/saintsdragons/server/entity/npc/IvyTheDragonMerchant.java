// I know that we are upside down, so hold your tongue and hear me out

package com.leon.saintsdragons.server.entity.npc;

import com.leon.saintsdragons.common.config.SaintsDragonsConfig;
import com.leon.saintsdragons.common.SaintsDragonsCommon;
import com.leon.saintsdragons.common.registry.ModItems;
import com.leon.saintsdragons.server.entity.dragons.util.DragonUtilities;
import com.leon.saintsdragons.server.entity.handler.HumanSoundHandler;
import com.leon.saintsdragons.server.entity.npc.dialogue.DialogueDefinition;
import com.leon.saintsdragons.server.entity.npc.dialogue.DialogueRegistry;
import com.leon.saintsdragons.server.entity.npc.dialogue.DialogueSessionRegistry;
import com.leon.saintsdragons.server.entity.npc.handlers.IvySoundProfile;
import com.leon.saintsdragons.server.entity.npc.trade.IvyTradeRegistry;
import com.leon.saintsdragons.server.ai.navigation.PathNavigateGround;
import com.leon.saintsdragons.util.animation.AnimationHelper;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.StringTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.AgeableMob;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.OwnableEntity;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.world.entity.ai.goal.FloatGoal;
import net.minecraft.world.entity.ai.goal.LookAtPlayerGoal;
import net.minecraft.world.entity.ai.goal.OpenDoorGoal;
import net.minecraft.world.entity.ai.goal.RandomLookAroundGoal;
import net.minecraft.world.entity.ai.goal.RandomStrollGoal;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.ai.goal.target.HurtByTargetGoal;
import net.minecraft.world.entity.ai.goal.target.NearestAttackableTargetGoal;
import net.minecraft.world.entity.ai.navigation.PathNavigation;
import net.minecraft.world.entity.monster.Evoker;
import net.minecraft.world.entity.monster.Pillager;
import net.minecraft.world.entity.monster.Vindicator;
import net.minecraft.world.entity.monster.Vex;
import net.minecraft.world.entity.monster.Witch;
import net.minecraft.world.entity.monster.Zombie;
import net.minecraft.world.entity.ai.control.BodyRotationControl;
import net.minecraft.world.entity.npc.AbstractVillager;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.effect.MobEffectCategory;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.trading.MerchantOffer;
import net.minecraft.world.item.trading.MerchantOffers;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.stats.Stats;
import java.util.EnumSet;
import java.util.HashSet;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import org.jetbrains.annotations.NotNull;
import net.minecraft.util.Mth;
import com.leon.saintsdragons.server.entity.controller.GenericLookControl;
import com.leon.saintsdragons.util.math.SmoothValue;
import software.bernie.geckolib.animatable.GeoEntity;
import software.bernie.geckolib.core.animatable.instance.AnimatableInstanceCache;
import software.bernie.geckolib.core.animation.AnimatableManager;
import software.bernie.geckolib.core.animation.AnimationController;
import software.bernie.geckolib.core.animation.AnimationState;
import software.bernie.geckolib.core.object.PlayState;
import software.bernie.geckolib.core.animation.RawAnimation;
import software.bernie.geckolib.core.keyframe.event.ParticleKeyframeEvent;
import software.bernie.geckolib.core.keyframe.event.SoundKeyframeEvent;
import software.bernie.geckolib.util.GeckoLibUtil;

import javax.annotation.Nullable;
import java.util.ArrayList;

public class IvyTheDragonMerchant extends AbstractVillager implements GeoEntity, OwnableEntity {
    static final int RECOVERY_NONE = 0;
    static final int RECOVERY_DRINK = 1;
    static final int RECOVERY_EAT = 2;
    private static final ResourceLocation FIRST_MEETING_DIALOGUE = SaintsDragonsCommon.rl("ivy/first_meeting");
    private static final ResourceLocation KNOWN_GREETING_DIALOGUE = SaintsDragonsCommon.rl("ivy/known_greeting");
    private static final ResourceLocation TRESPASSER_KNOWN_GREETING_DIALOGUE = SaintsDragonsCommon.rl("ivy/known_greeting_trespasser");
    private static final ResourceLocation RUDE_KNOWN_GREETING_DIALOGUE = SaintsDragonsCommon.rl("ivy/known_greeting_rude");
    private static final ResourceLocation WARES_KNOWN_GREETING_DIALOGUE = SaintsDragonsCommon.rl("ivy/known_greeting_wares");
    private static final ResourceLocation RECRUITED_GREETING_DIALOGUE = SaintsDragonsCommon.rl("ivy/recruited_greeting");
    private static final EntityDataAccessor<Boolean> DATA_RUNNING =
            SynchedEntityData.defineId(IvyTheDragonMerchant.class, EntityDataSerializers.BOOLEAN);
    private static final EntityDataAccessor<Boolean> DATA_TAME =
            SynchedEntityData.defineId(IvyTheDragonMerchant.class, EntityDataSerializers.BOOLEAN);
    private static final EntityDataAccessor<Optional<UUID>> DATA_OWNER_UUID =
            SynchedEntityData.defineId(IvyTheDragonMerchant.class, EntityDataSerializers.OPTIONAL_UUID);
    private static final EntityDataAccessor<Integer> DATA_COMMAND =
            SynchedEntityData.defineId(IvyTheDragonMerchant.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<String> DATA_IDLE_CHATTER =
            SynchedEntityData.defineId(IvyTheDragonMerchant.class, EntityDataSerializers.STRING);
    private static final EntityDataAccessor<Integer> DATA_TRADE_ANIM_STATE =
            SynchedEntityData.defineId(IvyTheDragonMerchant.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Boolean> DATA_IDLE_VARIANT_ACTIVE =
            SynchedEntityData.defineId(IvyTheDragonMerchant.class, EntityDataSerializers.BOOLEAN);
    private static final EntityDataAccessor<Boolean> DATA_BOXING_STANCE =
            SynchedEntityData.defineId(IvyTheDragonMerchant.class, EntityDataSerializers.BOOLEAN);
    private static final EntityDataAccessor<Boolean> DATA_BOXING_BACKING_UP =
            SynchedEntityData.defineId(IvyTheDragonMerchant.class, EntityDataSerializers.BOOLEAN);
    private static final EntityDataAccessor<Boolean> DATA_BOXING_FAST =
            SynchedEntityData.defineId(IvyTheDragonMerchant.class, EntityDataSerializers.BOOLEAN);
    private static final EntityDataAccessor<Integer> DATA_BOXING_ACTION_TICKS =
            SynchedEntityData.defineId(IvyTheDragonMerchant.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Boolean> DATA_BOXING_EXITING =
            SynchedEntityData.defineId(IvyTheDragonMerchant.class, EntityDataSerializers.BOOLEAN);
    private static final EntityDataAccessor<Integer> DATA_BOXING_RECOVERY_ACTION =
            SynchedEntityData.defineId(IvyTheDragonMerchant.class, EntityDataSerializers.INT);
    private static final int TRADE_START_TICKS = 29;
    private static final int TRADE_STOP_TICKS = 29;
    private static final int IDLE_CHATTER_DURATION = 90;
    private static final int IDLE_CHATTER_MIN_COOLDOWN = 600;
    private static final int IDLE_CHATTER_MAX_COOLDOWN = 1400;
    private static final double IDLE_CHATTER_OWNER_DISTANCE_SQR = 100.0D;
    private static final String[] IDLE_CHATTER_LINES = {
            "Ow... something bit me.",
            "Quiet day. Suspiciously quiet.",
            "Try not to wander into anything awful.",
            "I could use a cup of coffee, preferably black.",
            "What a world.",
            "I thought I heard wings",
            "Remind me why we left the house again?",
            "I swear that rock moved.",
            "If something screams, we are not investigating immediately."
    };
    private static final RawAnimation TRADE_START = RawAnimation.begin().thenPlay("ivy_oleander.animation.trade_start");
    private static final RawAnimation TRADING = RawAnimation.begin().thenLoop("ivy_oleander.animation.trading");
    private static final RawAnimation TRADE_STOP = RawAnimation.begin().thenPlay("ivy_oleander.animation.trade_stop");
    private static final RawAnimation IDLE = RawAnimation.begin().thenLoop("ivy_oleander.animation.idle");
    private static final RawAnimation WALK = RawAnimation.begin().thenLoop("ivy_oleander.animation.walk");
    private static final RawAnimation RUN = RawAnimation.begin().thenLoop("ivy_oleander.animation.run");
    private static final String KNOWN_DIALOGUE_PLAYERS_TAG = "KnownDialoguePlayers";
    private static final String KNOWN_DIALOGUE_UUID_TAG = "UUID";
    private static final String REMEMBERED_NAME_TAG = "Name";
    private static final String REMEMBERED_IMPRESSION_TAG = "Impression";
    private static final String REMEMBERED_DIALOGUE_FLAGS_TAG = "DialogueFlags";
    private static final String TAME_TAG = "Tame";
    private static final String OWNER_UUID_TAG = "OwnerUUID";
    private static final String COMMAND_TAG = "Command";
    private static final String TRESPASSER_IMPRESSION = "trespasser";
    private static final String RUDE_IMPRESSION = "rude";
    private static final String WARES_IMPRESSION = "wares";
    private final AnimatableInstanceCache animationCache = GeckoLibUtil.createInstanceCache(this);
    private int tradeAnimTicks = 0;
    private boolean lastTradingState = false;
    private boolean wasMovementStopped = false;
    private IvyBodyControl bodyControl;
    private IvyCompanionController companionController;
    public final SmoothValue bodyRotDeviation = SmoothValue.rotation(0.0);
    private int idleChatterTicks = 0;
    private int idleChatterCooldown;
    private final Map<UUID, String> rememberedDialogueNames = new HashMap<>();
    private final Map<UUID, String> rememberedDialogueImpressions = new HashMap<>();
    private final Map<UUID, Set<String>> rememberedDialogueFlags = new HashMap<>();
    private final HumanSoundHandler soundHandler;
    private IvyCombatBrain boxingCombat;
    private final int restockInterval;
    private int restockTimer;
    private boolean clientRecoveryItemVisible;
    private UUID pendingDialogueTradePlayerUuid;
    private ResourceLocation pendingDialogueTradeId;
    private String pendingDialogueTradeNodeId;
    private int pendingDialogueTradeResumeTicks;

    public IvyTheDragonMerchant(EntityType<? extends AbstractVillager> entityType, Level level) {
        super(entityType, level);
        this.setPersistenceRequired();
        this.lookControl = new GenericLookControl(this);
        this.soundHandler = new HumanSoundHandler(this, new IvySoundProfile());
        this.restockInterval = Math.max(1, resolveRestockInterval());
        this.restockTimer = this.restockInterval;
        this.idleChatterCooldown = nextIdleChatterCooldown();
    }

    @Override
    public boolean removeWhenFarAway(double distanceToClosestPlayer) {
        return false;
    }

    @Override
    public boolean requiresCustomPersistence() {
        return true;
    }

    public boolean isTame() {
        return this.entityData.get(DATA_TAME);
    }

    public void setTame(boolean tame) {
        this.entityData.set(DATA_TAME, tame);
        if (!tame) {
            setOwnerUUID(null);
        }
    }

    public void recruitAsCompanion(Player player) {
        setTame(true);
        setOwnerUUID(player.getUUID());
        setCommand(CompanionCommand.FOLLOW.id);
        this.setPersistenceRequired();
    }

    public int getCommand() {
        return this.entityData.get(DATA_COMMAND);
    }

    public void setCommand(int command) {
        this.entityData.set(DATA_COMMAND, CompanionCommand.byId(command).id);
        if (getCommand() == CompanionCommand.STAY.id) {
            getNavigation().stop();
            setDeltaMovement(0.0, getDeltaMovement().y, 0.0);
        }
    }

    public CompanionCommand getCompanionCommand() {
        return CompanionCommand.byId(getCommand());
    }

    public void setCompanionCommand(CompanionCommand command) {
        setCommand(command.id);
    }

    public int cycleCompanionCommand() {
        int next = (getCommand() + 1) % CompanionCommand.values().length;
        setCommand(next);
        return getCommand();
    }

    public String getIdleChatterText() {
        return this.entityData.get(DATA_IDLE_CHATTER);
    }

    public boolean isOwnedBy(LivingEntity entity) {
        return entity != null && entity.getUUID().equals(getOwnerUUID());
    }

    @Nullable
    @Override
    public UUID getOwnerUUID() {
        return this.entityData.get(DATA_OWNER_UUID).orElse(null);
    }

    public void setOwnerUUID(@Nullable UUID ownerUuid) {
        this.entityData.set(DATA_OWNER_UUID, Optional.ofNullable(ownerUuid));
    }

    @Nullable
    @Override
    public LivingEntity getOwner() {
        UUID ownerUuid = getOwnerUUID();
        if (ownerUuid == null) {
            return null;
        }
        if (level() instanceof ServerLevel serverLevel) {
            Entity owner = serverLevel.getEntity(ownerUuid);
            return owner instanceof LivingEntity living ? living : null;
        }
        return level().getPlayerByUUID(ownerUuid);
    }

    @Override
    protected void defineSynchedData() {
        super.defineSynchedData();
        this.entityData.define(DATA_RUNNING, false);
        this.entityData.define(DATA_TAME, false);
        this.entityData.define(DATA_OWNER_UUID, Optional.empty());
        this.entityData.define(DATA_COMMAND, CompanionCommand.WANDER.id);
        this.entityData.define(DATA_IDLE_CHATTER, "");
        this.entityData.define(DATA_TRADE_ANIM_STATE, TradeAnimState.NONE.id);
        this.entityData.define(DATA_IDLE_VARIANT_ACTIVE, false);
        this.entityData.define(DATA_BOXING_STANCE, false);
        this.entityData.define(DATA_BOXING_BACKING_UP, false);
        this.entityData.define(DATA_BOXING_FAST, false);
        this.entityData.define(DATA_BOXING_ACTION_TICKS, 0);
        this.entityData.define(DATA_BOXING_EXITING, false);
        this.entityData.define(DATA_BOXING_RECOVERY_ACTION, RECOVERY_NONE);
    }

    public static AttributeSupplier.Builder createAttributes() {
        return Mob.createMobAttributes()
                .add(Attributes.MAX_HEALTH, 20.0)
                .add(Attributes.MOVEMENT_SPEED, 0.35)
                .add(Attributes.ATTACK_DAMAGE, 4.0)
                .add(Attributes.FOLLOW_RANGE, 32.0);
    }

    @Override
    protected void registerGoals() {
        goalSelector.addGoal(0, new DialogueTalkGoal());
        goalSelector.addGoal(1, new TradingStillGoal());
        goalSelector.addGoal(1, new FloatGoal(this));
        goalSelector.addGoal(2, new OpenDoorGoal(this, true));
        goalSelector.addGoal(2, getBoxingCombat().createGoal());
        goalSelector.addGoal(3, getCompanionController().createStayGoal());
        goalSelector.addGoal(4, getCompanionController().createFollowOwnerGoal());
        goalSelector.addGoal(5, new RandomStrollGoal(this, 0.6) {
            @Override
            public boolean canUse() {
                return IvyTheDragonMerchant.this.getTarget() == null
                        && (!IvyTheDragonMerchant.this.isTame()
                        || IvyTheDragonMerchant.this.getCompanionCommand() == CompanionCommand.WANDER)
                        && !IvyTheDragonMerchant.this.isInDialogue()
                        && !IvyTheDragonMerchant.this.isBoxingCombatActive()
                        && super.canUse();
            }

            @Override
            public boolean canContinueToUse() {
                return IvyTheDragonMerchant.this.getTarget() == null
                        && (!IvyTheDragonMerchant.this.isTame()
                        || IvyTheDragonMerchant.this.getCompanionCommand() == CompanionCommand.WANDER)
                        && !IvyTheDragonMerchant.this.isInDialogue()
                        && !IvyTheDragonMerchant.this.isBoxingCombatActive()
                        && super.canContinueToUse();
            }
        });
        goalSelector.addGoal(6, new LookAtPlayerGoal(this, Player.class, 6.0f) {
            @Override
            public boolean canUse() {
                return !IvyTheDragonMerchant.this.isInDialogue()
                        && !IvyTheDragonMerchant.this.isBoxingCombatActive()
                        && super.canUse();
            }

            @Override
            public boolean canContinueToUse() {
                return !IvyTheDragonMerchant.this.isInDialogue()
                        && !IvyTheDragonMerchant.this.isBoxingCombatActive()
                        && super.canContinueToUse();
            }
        });
        goalSelector.addGoal(7, new RandomLookAroundGoal(this) {
            @Override
            public boolean canUse() {
                return !IvyTheDragonMerchant.this.isInDialogue()
                        && !IvyTheDragonMerchant.this.isBoxingCombatActive()
                        && super.canUse();
            }

            @Override
            public boolean canContinueToUse() {
                return !IvyTheDragonMerchant.this.isInDialogue()
                        && !IvyTheDragonMerchant.this.isBoxingCombatActive()
                        && super.canContinueToUse();
            }
        });
        targetSelector.addGoal(0, new HurtByTargetGoal(this));
        targetSelector.addGoal(1, getCompanionController().createOwnerDefenseGoal());
        targetSelector.addGoal(2, new NearestAttackableTargetGoal<>(this, Evoker.class, true));
        targetSelector.addGoal(3, new NearestAttackableTargetGoal<>(this, Pillager.class, true));
        targetSelector.addGoal(4, new NearestAttackableTargetGoal<>(this, Vex.class, true));
        targetSelector.addGoal(5, new NearestAttackableTargetGoal<>(this, Vindicator.class, true));
        targetSelector.addGoal(6, new NearestAttackableTargetGoal<>(this, Witch.class, true));
        targetSelector.addGoal(7, new NearestAttackableTargetGoal<>(this, Zombie.class, true));
    }

    @Override
    protected @NotNull BodyRotationControl createBodyControl() {
        this.bodyControl = new IvyBodyControl(this);
        return this.bodyControl;
    }

    @Override
    protected @NotNull PathNavigation createNavigation(@NotNull Level level) {
        PathNavigateGround navigation = new PathNavigateGround(this, level);
        navigation.setCanOpenDoors(true);
        navigation.setCanPassDoors(true);
        return navigation;
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
        this.entityData.set(DATA_TRADE_ANIM_STATE, state.id);
    }

    private boolean isIdleVariantActive() {
        return this.entityData.get(DATA_IDLE_VARIANT_ACTIVE);
    }

    private void setIdleVariantActive(boolean active) {
        this.entityData.set(DATA_IDLE_VARIANT_ACTIVE, active);
    }

    boolean isBoxingStance() {
        return this.entityData.get(DATA_BOXING_STANCE);
    }

    void setBoxingStance(boolean boxing) {
        this.entityData.set(DATA_BOXING_STANCE, boxing);
        if (!boxing) {
            this.entityData.set(DATA_BOXING_BACKING_UP, false);
            this.entityData.set(DATA_BOXING_FAST, false);
            this.entityData.set(DATA_BOXING_ACTION_TICKS, 0);
            this.entityData.set(DATA_BOXING_EXITING, false);
            this.entityData.set(DATA_BOXING_RECOVERY_ACTION, RECOVERY_NONE);
        }
    }

    boolean isBoxingExiting() {
        return this.entityData.get(DATA_BOXING_EXITING);
    }

    void setBoxingExiting(boolean exiting) {
        this.entityData.set(DATA_BOXING_EXITING, exiting);
    }

    boolean isBoxingRecovering() {
        return this.entityData.get(DATA_BOXING_RECOVERY_ACTION) != RECOVERY_NONE;
    }

    boolean isBoxingDrinking() {
        return this.entityData.get(DATA_BOXING_RECOVERY_ACTION) == RECOVERY_DRINK;
    }

    boolean isBoxingEating() {
        return this.entityData.get(DATA_BOXING_RECOVERY_ACTION) == RECOVERY_EAT;
    }

    void setBoxingRecoveryAction(int action) {
        this.entityData.set(DATA_BOXING_RECOVERY_ACTION, action);
        if (action == RECOVERY_NONE) {
            this.clientRecoveryItemVisible = false;
        }
    }

    public ItemStack getRecoveryItemForRender() {
        if (!clientRecoveryItemVisible) {
            return ItemStack.EMPTY;
        }
        if (isBoxingDrinking()) {
            return new ItemStack(Items.MILK_BUCKET);
        }
        if (isBoxingEating()) {
            return new ItemStack(Items.COOKED_BEEF);
        }
        return ItemStack.EMPTY;
    }

    void setBoxingMovement(boolean backingUp, boolean fast) {
        this.entityData.set(DATA_BOXING_BACKING_UP, backingUp);
        this.entityData.set(DATA_BOXING_FAST, fast);
    }

    boolean isBoxingBackingUp() {
        return this.entityData.get(DATA_BOXING_BACKING_UP);
    }

    boolean isBoxingFast() {
        return this.entityData.get(DATA_BOXING_FAST);
    }

    int getBoxingActionTicks() {
        return this.entityData.get(DATA_BOXING_ACTION_TICKS);
    }

    void setBoxingActionTicks(int ticks) {
        this.entityData.set(DATA_BOXING_ACTION_TICKS, Math.max(0, ticks));
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
            return InteractionResult.CONSUME;
        }
        clearInvalidDialogueBlockingTarget();
        if (isTrading()
                || isInDialogue()
                || getTradeAnimState() != TradeAnimState.NONE
                || isBoxingStance()) {
            return InteractionResult.CONSUME;
        }
        if (player instanceof ServerPlayer serverPlayer) {
            if (player.isCrouching() && isTame() && isOwnedBy(player)) {
                if (hand == InteractionHand.MAIN_HAND) {
                    int command = cycleCompanionCommand();
                    player.displayClientMessage(Component.translatable("entity.saintsdragons.all.command_" + command, getDisplayName()), true);
                }
                return InteractionResult.CONSUME;
            }
            DragonUtilities.awardAdvancement(serverPlayer, "meet_ivy", "meet_ivy");
            openDialogue(serverPlayer);
            return InteractionResult.CONSUME;
        }
        return InteractionResult.CONSUME;
    }

    private void openDialogue(ServerPlayer player) {
        clearInvalidDialogueBlockingTarget();
        ResourceLocation dialogueId = getDialogueIdFor(player);
        DialogueDefinition definition = DialogueRegistry.get(dialogueId);
        if (definition == null || definition.startNode() == null) {
            player.displayClientMessage(Component.literal("Ivy has nothing to say right now."), false);
            return;
        }
        DialogueSessionRegistry.start(player, this, definition, getRememberedDialogueName(player));
    }

    private void clearInvalidDialogueBlockingTarget() {
        LivingEntity target = getTarget();
        if (target != null && (!target.isAlive() || target.isRemoved())) {
            setTarget(null);
        }
    }

    private boolean hasLiveTarget() {
        LivingEntity target = getTarget();
        return target != null && target.isAlive() && !target.isRemoved();
    }

    private ResourceLocation getDialogueIdFor(ServerPlayer player) {
        if (!hasRememberedDialogueName(player)) {
            return FIRST_MEETING_DIALOGUE;
        }
        if (isTame() && isOwnedBy(player)) {
            return RECRUITED_GREETING_DIALOGUE;
        }
        String impression = rememberedDialogueImpressions.get(player.getUUID());
        if (TRESPASSER_IMPRESSION.equals(impression)) {
            return pickRememberedGreeting(TRESPASSER_KNOWN_GREETING_DIALOGUE);
        }
        if (RUDE_IMPRESSION.equals(impression)) {
            return pickRememberedGreeting(RUDE_KNOWN_GREETING_DIALOGUE);
        }
        if (WARES_IMPRESSION.equals(impression)) {
            return pickRememberedGreeting(WARES_KNOWN_GREETING_DIALOGUE);
        }
        return KNOWN_GREETING_DIALOGUE;
    }

    private ResourceLocation pickRememberedGreeting(ResourceLocation rememberedGreeting) {
        return random.nextBoolean() ? rememberedGreeting : KNOWN_GREETING_DIALOGUE;
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
    public void overrideOffers(MerchantOffers offers) {}

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
        movementController.setParticleKeyframeHandler(this::handleParticleKeyframe);
        setupMovementController(movementController);
        getBoxingCombat().setupMovementController(movementController);
        controllers.add(movementController);
    }

    private void handleSoundKeyframe(SoundKeyframeEvent<IvyTheDragonMerchant> event) {
        soundHandler.handleAnimationSound(event.getKeyframeData(), event.getController());
    }

    private void handleParticleKeyframe(ParticleKeyframeEvent<IvyTheDragonMerchant> event) {
        String effect = event.getKeyframeData().getEffect();
        if (!isBoxingRecovering()) {
            return;
        }
        if ("spawn".equals(effect) || "spawn_milk_bucket".equals(effect)) {
            this.clientRecoveryItemVisible = true;
        } else if ("despawn".equals(effect) || "despawn_milk_bucket".equals(effect)) {
            this.clientRecoveryItemVisible = false;
        }
    }

    private <T extends GeoEntity> PlayState animationPredicate(AnimationState<T> state) {
        TradeAnimState tradeState = getTradeAnimState();
        if (tradeState == TradeAnimState.LOOP) {
            AnimationHelper.setAndContinue(state, TRADING);
            return PlayState.CONTINUE;
        }
        if (tradeState == TradeAnimState.START
                || tradeState == TradeAnimState.STOP
                || isIdleVariantActive()) {
            return PlayState.CONTINUE;
        }

        if (wasMovementStopped) {
            state.getController().forceAnimationReset();
            wasMovementStopped = false;
        }

        if (getBoxingCombat().isActive()) {
            state.getController().transitionLength(1);
        }

        if (getBoxingCombat().applyMovementAnimation(state)) {
            return PlayState.CONTINUE;
        }

        state.getController().transitionLength(4);
        if (state.isMoving()) {
            if (isRunning()) {
                AnimationHelper.setAndContinue(state, RUN);
            } else {
                AnimationHelper.setAndContinue(state, WALK);
            }
        } else {
            AnimationHelper.setAndContinue(state, IDLE);
        }
        return PlayState.CONTINUE;
    }

    @Override
    public boolean hurt(@NotNull DamageSource source, float amount) {
        if (isBlockedHostileDamage(source)) {
            getBoxingCombat().dodgeBlockedHit(source);
            return false;
        }
        if (getBoxingCombat().tryDodgeOnHit(source, amount)) {
            return false;
        }
        boolean holdGround = source.getEntity() instanceof Player && getBoxingCombat().shouldHoldGroundAgainstKnockback();
        Vec3 preHitMotion = holdGround ? getDeltaMovement() : Vec3.ZERO;
        boolean hurt = super.hurt(source, amount);
        if (hurt && holdGround) {
            setDeltaMovement(preHitMotion);
            hasImpulse = true;
        }
        getBoxingCombat().onHurt(source, hurt);
        return hurt;
    }

    @Override
    public void setTarget(@Nullable LivingEntity target) {
        if (isTame() && isOwnedBy(target)) {
            super.setTarget(null);
            return;
        }
        super.setTarget(target);
    }

    private boolean isBlockedHostileDamage(DamageSource source) {
        return !isBoxingRecovering()
                && (source.getEntity() instanceof Zombie
                || source.getEntity() instanceof Pillager
                || source.getEntity() instanceof Vindicator
                || source.getEntity() instanceof Evoker
                || source.getEntity() instanceof Vex
                || source.getEntity() instanceof Witch);
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
                scheduleDialogueResumeAfterTrade();
            }
            lastTradingState = trading;
        }
    }

    @Override
    protected void stopTrading() {
        Player previousTradingPlayer = getTradingPlayer();
        super.stopTrading();
        if (!level().isClientSide) {
            stopTradingSequence();
            lastTradingState = false;
            scheduleDialogueResumeAfterTrade(previousTradingPlayer);
        }
    }

    @Override
    public void tick() {
        super.tick();
        updateRotationDeviation();
        if (level().isClientSide) {
            if (!isBoxingRecovering()) {
                this.clientRecoveryItemVisible = false;
            }
            return;
        }
        tickTradingAnimation();
        tickDialogueTradeResume();
        tickIdleChatter();
        getCompanionController().tick();
        clearInvalidDialogueBlockingTarget();
        if (hasLiveTarget() && isInDialogue()) {
            DialogueSessionRegistry.endForEntity(this);
        }
        getBoxingCombat().tryStartRetreatRecovery();
        getBoxingCombat().tick();
        tickRestocking();
        if (bodyControl != null) {
            if (getBoxingCombat().isActive()) {
                getBoxingCombat().tickRotationLock();
            } else {
                bodyControl.serverTick();
            }
        }
    }

    boolean hasHarmfulEffect() {
        return getActiveEffects().stream()
                .anyMatch(effect -> effect.getEffect().getCategory() == MobEffectCategory.HARMFUL);
    }

    boolean needsRecoveryFood() {
        return getHealth() > 0.0F && getHealth() <= getMaxHealth() * 0.45F;
    }

    void drinkMilkForRecovery() {
        for (var effect : new ArrayList<>(getActiveEffects())) {
            if (effect.getEffect().getCategory() == MobEffectCategory.HARMFUL) {
                removeEffect(effect.getEffect());
            }
        }
    }

    void eatFoodForRecovery() {
        heal(8.0F);
    }

    void lockBoxingBodyToYaw(float yaw, float turnSpeed) {
        if (bodyControl != null) {
            bodyControl.lockBodyToYaw(yaw, turnSpeed);
        }
    }

    boolean isBoxingCombatActive() {
        return getBoxingCombat().isActive();
    }

    boolean isCompanionAiBlocked() {
        return isTrading()
                || isInDialogue()
                || getTradeAnimState() != TradeAnimState.NONE
                || isIdleVariantActive()
                || getBoxingCombat().isActive()
                || isBoxingRecovering();
    }

    @Override
    public void addAdditionalSaveData(@NotNull CompoundTag tag) {
        super.addAdditionalSaveData(tag);
        tag.putBoolean(TAME_TAG, isTame());
        UUID ownerUuid = getOwnerUUID();
        if (ownerUuid != null) {
            tag.putUUID(OWNER_UUID_TAG, ownerUuid);
        }
        tag.putInt(COMMAND_TAG, getCommand());
        ListTag knownDialogueList = new ListTag();
        for (Map.Entry<UUID, String> entry : rememberedDialogueNames.entrySet()) {
            CompoundTag knownTag = new CompoundTag();
            knownTag.putUUID(KNOWN_DIALOGUE_UUID_TAG, entry.getKey());
            knownTag.putString(REMEMBERED_NAME_TAG, entry.getValue());
            String impression = rememberedDialogueImpressions.get(entry.getKey());
            if (impression != null && !impression.isBlank()) {
                knownTag.putString(REMEMBERED_IMPRESSION_TAG, impression);
            }
            Set<String> flags = rememberedDialogueFlags.get(entry.getKey());
            if (flags != null && !flags.isEmpty()) {
                ListTag flagsTag = new ListTag();
                flags.stream()
                        .filter(flag -> flag != null && !flag.isBlank())
                        .sorted()
                        .forEach(flag -> flagsTag.add(StringTag.valueOf(flag)));
                knownTag.put(REMEMBERED_DIALOGUE_FLAGS_TAG, flagsTag);
            }
            knownDialogueList.add(knownTag);
        }
        tag.put(KNOWN_DIALOGUE_PLAYERS_TAG, knownDialogueList);
    }

    @Override
    public void readAdditionalSaveData(@NotNull CompoundTag tag) {
        super.readAdditionalSaveData(tag);
        setTame(tag.getBoolean(TAME_TAG));
        if (tag.hasUUID(OWNER_UUID_TAG)) {
            setOwnerUUID(tag.getUUID(OWNER_UUID_TAG));
            if (getOwnerUUID() != null) {
                this.entityData.set(DATA_TAME, true);
            }
        } else if (!isTame()) {
            setOwnerUUID(null);
        }
        setCommand(tag.contains(COMMAND_TAG, Tag.TAG_INT) ? tag.getInt(COMMAND_TAG) : CompanionCommand.WANDER.id);
        rememberedDialogueNames.clear();
        rememberedDialogueImpressions.clear();
        rememberedDialogueFlags.clear();
        if (tag.contains(KNOWN_DIALOGUE_PLAYERS_TAG, Tag.TAG_LIST)) {
            ListTag knownDialogueList = tag.getList(KNOWN_DIALOGUE_PLAYERS_TAG, Tag.TAG_COMPOUND);
            for (int i = 0; i < knownDialogueList.size(); i++) {
                CompoundTag knownTag = knownDialogueList.getCompound(i);
                if (knownTag.hasUUID(KNOWN_DIALOGUE_UUID_TAG) && knownTag.contains(REMEMBERED_NAME_TAG, Tag.TAG_STRING)) {
                    UUID uuid = knownTag.getUUID(KNOWN_DIALOGUE_UUID_TAG);
                    rememberedDialogueNames.put(uuid, knownTag.getString(REMEMBERED_NAME_TAG));
                    if (knownTag.contains(REMEMBERED_IMPRESSION_TAG, Tag.TAG_STRING)) {
                        rememberedDialogueImpressions.put(uuid, knownTag.getString(REMEMBERED_IMPRESSION_TAG));
                    }
                    if (knownTag.contains(REMEMBERED_DIALOGUE_FLAGS_TAG, Tag.TAG_LIST)) {
                        Set<String> flags = new HashSet<>();
                        ListTag flagsTag = knownTag.getList(REMEMBERED_DIALOGUE_FLAGS_TAG, Tag.TAG_STRING);
                        for (int flagIndex = 0; flagIndex < flagsTag.size(); flagIndex++) {
                            String flag = flagsTag.getString(flagIndex);
                            if (!flag.isBlank()) {
                                flags.add(flag);
                            }
                        }
                        if (!flags.isEmpty()) {
                            rememberedDialogueFlags.put(uuid, flags);
                        }
                    }
                }
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

    private void tickIdleChatter() {
        if (idleChatterTicks > 0) {
            idleChatterTicks--;
            if (idleChatterTicks == 0) {
                this.entityData.set(DATA_IDLE_CHATTER, "");
            }
            return;
        }
        if (!getIdleChatterText().isEmpty()) {
            this.entityData.set(DATA_IDLE_CHATTER, "");
        }
        if (!canIdleChatter()) {
            idleChatterCooldown = Math.max(idleChatterCooldown, 80);
            return;
        }
        if (idleChatterCooldown > 0) {
            idleChatterCooldown--;
            return;
        }
        speakIdleChatter();
    }

    private boolean canIdleChatter() {
        LivingEntity owner = getOwner();
        return isTame()
                && owner != null
                && isOwnedBy(owner)
                && distanceToSqr(owner) <= IDLE_CHATTER_OWNER_DISTANCE_SQR
                && getTarget() == null
                && !isTrading()
                && !isInDialogue()
                && getTradeAnimState() == TradeAnimState.NONE
                && !getBoxingCombat().isActive()
                && !isBoxingRecovering()
                && isAlive();
    }

    private void speakIdleChatter() {
        String line = IDLE_CHATTER_LINES[random.nextInt(IDLE_CHATTER_LINES.length)];
        this.entityData.set(DATA_IDLE_CHATTER, line);
        idleChatterTicks = IDLE_CHATTER_DURATION;
        idleChatterCooldown = nextIdleChatterCooldown();
    }

    private int nextIdleChatterCooldown() {
        return IDLE_CHATTER_MIN_COOLDOWN + random.nextInt(IDLE_CHATTER_MAX_COOLDOWN - IDLE_CHATTER_MIN_COOLDOWN + 1);
    }

    public boolean hasRememberedDialogueName(Player player) {
        return rememberedDialogueNames.containsKey(player.getUUID());
    }

    @Nullable
    public String getRememberedDialogueName(Player player) {
        return rememberedDialogueNames.get(player.getUUID());
    }

    public void rememberDialogueName(Player player, String name) {
        if (name == null || name.isBlank()) {
            rememberedDialogueNames.remove(player.getUUID());
            return;
        }
        rememberedDialogueNames.put(player.getUUID(), name);
    }

    public void rememberDialogueImpression(Player player, String impression) {
        if (impression == null || impression.isBlank()) {
            rememberedDialogueImpressions.remove(player.getUUID());
            return;
        }
        rememberedDialogueImpressions.put(player.getUUID(), impression);
    }

    public Set<String> getRememberedDialogueFlags(Player player) {
        Set<String> flags = rememberedDialogueFlags.get(player.getUUID());
        return flags == null ? Set.of() : Set.copyOf(flags);
    }

    public void rememberDialogueFlag(Player player, String flag) {
        if (flag == null || flag.isBlank()) {
            return;
        }
        rememberedDialogueFlags.computeIfAbsent(player.getUUID(), uuid -> new HashSet<>()).add(flag);
    }

    private void openTradingFor(Player player) {
        setTradingPlayer(player);
        openTradingScreen(player, getDisplayName(), 1);
        player.awardStat(Stats.TALKED_TO_VILLAGER);
    }

    public void openDialogueTrade(ServerPlayer player, ResourceLocation dialogueId, String resumeNodeId) {
        pendingDialogueTradePlayerUuid = player.getUUID();
        pendingDialogueTradeId = dialogueId;
        pendingDialogueTradeNodeId = resumeNodeId;
        pendingDialogueTradeResumeTicks = 0;
        openTradingFor(player);
    }

    private void scheduleDialogueResumeAfterTrade(@Nullable Player player) {
        if (!(player instanceof ServerPlayer serverPlayer)
                || pendingDialogueTradePlayerUuid == null
                || pendingDialogueTradeId == null
                || pendingDialogueTradeNodeId == null
                || !pendingDialogueTradePlayerUuid.equals(serverPlayer.getUUID())) {
            scheduleDialogueResumeAfterTrade();
            return;
        }
        pendingDialogueTradeResumeTicks = 3;
    }

    private void scheduleDialogueResumeAfterTrade() {
        if (pendingDialogueTradePlayerUuid == null || pendingDialogueTradeId == null || pendingDialogueTradeNodeId == null) {
            clearPendingDialogueTrade();
            return;
        }
        pendingDialogueTradeResumeTicks = 3;
    }

    private void tickDialogueTradeResume() {
        if (pendingDialogueTradeResumeTicks <= 0) {
            return;
        }
        pendingDialogueTradeResumeTicks--;
        if (pendingDialogueTradeResumeTicks > 0) {
            return;
        }
        resumeDialogueAfterTrade();
    }

    private void resumeDialogueAfterTrade() {
        if (pendingDialogueTradePlayerUuid == null || pendingDialogueTradeId == null || pendingDialogueTradeNodeId == null) {
            clearPendingDialogueTrade();
            return;
        }
        ServerPlayer serverPlayer = level().getServer() == null
                ? null
                : level().getServer().getPlayerList().getPlayer(pendingDialogueTradePlayerUuid);
        if (serverPlayer == null) {
            clearPendingDialogueTrade();
            return;
        }
        ResourceLocation dialogueId = pendingDialogueTradeId;
        String nodeId = pendingDialogueTradeNodeId;
        clearPendingDialogueTrade();
        DialogueSessionRegistry.resume(serverPlayer, this, dialogueId, nodeId);
    }

    private void clearPendingDialogueTrade() {
        pendingDialogueTradePlayerUuid = null;
        pendingDialogueTradeId = null;
        pendingDialogueTradeNodeId = null;
        pendingDialogueTradeResumeTicks = 0;
    }

    private void updateRotationDeviation() {
        float headToBody = (float) (Mth.wrapDegrees(this.yHeadRot - this.yBodyRot) * 0.25);
        bodyRotDeviation.setTo(headToBody);
        bodyRotDeviation.update(0.25f);
    }

    private void startTradingSequence() {
        setTradeAnimState(TradeAnimState.START);
        tradeAnimTicks = TRADE_START_TICKS;
        triggerAnim("movement", "trade_start");
    }

    private void stopTradingSequence() {
        setTradeAnimState(TradeAnimState.STOP);
        tradeAnimTicks = TRADE_STOP_TICKS;
        triggerAnim("movement", "trade_stop");
    }

    private void setupMovementController(AnimationController<IvyTheDragonMerchant> controller) {
        controller.triggerableAnim("trade_start", TRADE_START);
        controller.triggerableAnim("trading", TRADING);
        controller.triggerableAnim("trade_stop", TRADE_STOP);
        controller.triggerableAnim("greetings",
                RawAnimation.begin().thenPlay("ivy_oleander.animation.greetings"));
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
            triggerAnim("movement", "trading");
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
                && !isInDialogue()
                && getTradeAnimState() == TradeAnimState.NONE
                && !isIdleVariantActive()
                && !getBoxingCombat().isActive();
    }

    void cancelPassiveAnimationsForCombat() {
        setIdleVariantActive(false);
    }

    boolean isReadyForCombatAnimation() {
        return getTradeAnimState() == TradeAnimState.NONE;
    }

    private boolean isInDialogue() {
        return !level().isClientSide && DialogueSessionRegistry.hasSpeaker(this);
    }

    @Nullable
    private ServerPlayer getDialogueSpeaker() {
        if (level().isClientSide) {
            return null;
        }
        return DialogueSessionRegistry.getSpeaker(this);
    }

    private IvyCombatBrain getBoxingCombat() {
        if (boxingCombat == null) {
            boxingCombat = new IvyCombatBrain(this);
        }
        return boxingCombat;
    }

    private IvyCompanionController getCompanionController() {
        if (companionController == null) {
            companionController = new IvyCompanionController(this);
        }
        return companionController;
    }

    public enum CompanionCommand {
        FOLLOW(0),
        STAY(1),
        WANDER(2);

        private final int id;

        CompanionCommand(int id) {
            this.id = id;
        }

        private static CompanionCommand byId(int id) {
            for (CompanionCommand command : values()) {
                if (command.id == id) {
                    return command;
                }
            }
            return WANDER;
        }
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
            return isTrading() || isIdleVariantActive();
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


    private class DialogueTalkGoal extends Goal {
        private static final float TALK_LOOK_YAW_SPEED = 45.0f;
        private static final float TALK_LOOK_PITCH_SPEED = 30.0f;

        DialogueTalkGoal() {
            setFlags(EnumSet.of(Goal.Flag.MOVE, Goal.Flag.LOOK));
        }

        @Override
        public boolean canUse() {
            return getTarget() == null
                    && !getBoxingCombat().isActive()
                    && getDialogueSpeaker() != null;
        }

        @Override
        public boolean canContinueToUse() {
            return getTarget() == null
                    && !getBoxingCombat().isActive()
                    && getDialogueSpeaker() != null;
        }

        @Override
        public void start() {
            stopPassiveDialogueConflicts();
            stopMovement();
        }

        @Override
        public void stop() {
            stopMovement();
            if (getTarget() != null || getBoxingCombat().isActive()) {
                DialogueSessionRegistry.endForEntity(IvyTheDragonMerchant.this);
            }
        }

        @Override
        public void tick() {
            ServerPlayer speaker = getDialogueSpeaker();
            if (speaker == null) {
                return;
            }
            stopPassiveDialogueConflicts();
            stopMovement();
            getLookControl().setLookAt(speaker, TALK_LOOK_YAW_SPEED, TALK_LOOK_PITCH_SPEED);
            lookAt(speaker, TALK_LOOK_YAW_SPEED, TALK_LOOK_PITCH_SPEED);
        }

        private void stopPassiveDialogueConflicts() {
            setIdleVariantActive(false);
        }

        private void stopMovement() {
            getNavigation().stop();
            setDeltaMovement(0.0, getDeltaMovement().y, 0.0);
        }
    }

}

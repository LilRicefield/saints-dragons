// I know that we are upside down, so hold your tongue and hear me out

package com.leon.saintsdragons.server.entity.npc;

import com.leon.saintsdragons.common.config.SaintsDragonsConfig;
import com.leon.saintsdragons.common.SaintsDragonsCommon;
import com.leon.saintsdragons.common.registry.ModItems;
import com.leon.saintsdragons.server.entity.dragons.util.DragonUtilities;
import com.leon.saintsdragons.server.entity.dragons.cindervane.Cindervane;
import com.leon.saintsdragons.server.entity.handler.HumanSoundHandler;
import com.leon.saintsdragons.server.entity.npc.dialogue.DialogueDefinition;
import com.leon.saintsdragons.server.entity.npc.dialogue.DialogueRegistry;
import com.leon.saintsdragons.server.entity.npc.dialogue.DialogueSessionRegistry;
import com.leon.saintsdragons.server.entity.npc.chatter.IvyChatterRegistry;
import com.leon.saintsdragons.server.entity.npc.handlers.IvySoundProfile;
import com.leon.saintsdragons.server.entity.npc.trade.IvyTradeRegistry;
import com.leon.saintsdragons.server.ai.goals.base.GenericSwimSteeringController;
import com.leon.saintsdragons.server.ai.navigation.async.AsyncSwimController;
import com.leon.saintsdragons.server.ai.navigation.PathNavigateGround;
import com.leon.saintsdragons.server.menu.IvyInventoryMenu;
import com.leon.saintsdragons.util.animation.AnimationHelper;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.StringTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.chat.Component;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.tags.FluidTags;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.CombatRules;
import net.minecraft.world.entity.AgeableMob;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.ExperienceOrb;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.OwnableEntity;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.world.entity.ai.memory.MemoryModuleType;
import net.minecraft.world.entity.ai.goal.LookAtPlayerGoal;
import net.minecraft.world.entity.ai.goal.OpenDoorGoal;
import net.minecraft.world.entity.ai.goal.RandomLookAroundGoal;
import net.minecraft.world.entity.ai.goal.RandomStrollGoal;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.ai.goal.target.HurtByTargetGoal;
import net.minecraft.world.entity.ai.goal.target.NearestAttackableTargetGoal;
import net.minecraft.world.entity.ai.navigation.PathNavigation;
import net.minecraft.world.level.pathfinder.BlockPathTypes;
import net.minecraft.world.entity.monster.Evoker;
import net.minecraft.world.entity.monster.Pillager;
import net.minecraft.world.entity.monster.Vindicator;
import net.minecraft.world.entity.monster.Vex;
import net.minecraft.world.entity.monster.Witch;
import net.minecraft.world.entity.monster.Zombie;
import net.minecraft.world.entity.ai.control.BodyRotationControl;
import net.minecraft.world.entity.npc.AbstractVillager;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.vehicle.Boat;
import net.minecraft.world.effect.MobEffectCategory;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.Container;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.SimpleMenuProvider;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.trading.MerchantOffer;
import net.minecraft.world.item.trading.MerchantOffers;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.stats.Stats;
import net.minecraft.world.item.ArmorItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraft.world.item.enchantment.Enchantments;
import com.google.common.collect.Multimap;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.HashSet;
import java.util.HashMap;
import java.util.List;
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

public class IvyTheDragonMerchant extends AbstractVillager implements GeoEntity, OwnableEntity {
    static final int RECOVERY_NONE = 0;
    static final int RECOVERY_DRINK = 1;
    static final int RECOVERY_EAT = 2;
    private static final int PASSIVE_USE_NONE = 0;
    private static final int PASSIVE_USE_DRINK = 1;
    private static final int PASSIVE_USE_EAT = 2;
    private static final int PASSIVE_USE_WATER_CLUTCH = 3;
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
    private static final EntityDataAccessor<String> DATA_IDLE_CHATTER_NAME =
            SynchedEntityData.defineId(IvyTheDragonMerchant.class, EntityDataSerializers.STRING);
    private static final EntityDataAccessor<Integer> DATA_TRADE_ANIM_STATE =
            SynchedEntityData.defineId(IvyTheDragonMerchant.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Boolean> DATA_IDLE_VARIANT_ACTIVE =
            SynchedEntityData.defineId(IvyTheDragonMerchant.class, EntityDataSerializers.BOOLEAN);
    private static final EntityDataAccessor<Boolean> DATA_CLIMBING_LADDER =
            SynchedEntityData.defineId(IvyTheDragonMerchant.class, EntityDataSerializers.BOOLEAN);
    private static final EntityDataAccessor<Boolean> DATA_BOXING_STANCE =
            SynchedEntityData.defineId(IvyTheDragonMerchant.class, EntityDataSerializers.BOOLEAN);
    private static final EntityDataAccessor<Boolean> DATA_BOXING_BACKING_UP =
            SynchedEntityData.defineId(IvyTheDragonMerchant.class, EntityDataSerializers.BOOLEAN);
    private static final EntityDataAccessor<Boolean> DATA_BOXING_FAST =
            SynchedEntityData.defineId(IvyTheDragonMerchant.class, EntityDataSerializers.BOOLEAN);
    private static final EntityDataAccessor<Integer> DATA_BOXING_ACTION_TICKS =
            SynchedEntityData.defineId(IvyTheDragonMerchant.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<String> DATA_BOXING_ANIMATION =
            SynchedEntityData.defineId(IvyTheDragonMerchant.class, EntityDataSerializers.STRING);
    private static final EntityDataAccessor<Boolean> DATA_BOXING_EXITING =
            SynchedEntityData.defineId(IvyTheDragonMerchant.class, EntityDataSerializers.BOOLEAN);
    private static final EntityDataAccessor<Integer> DATA_BOXING_RECOVERY_ACTION =
            SynchedEntityData.defineId(IvyTheDragonMerchant.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Boolean> DATA_BOXING_SWORD_STYLE =
            SynchedEntityData.defineId(IvyTheDragonMerchant.class, EntityDataSerializers.BOOLEAN);
    private static final EntityDataAccessor<Boolean> DATA_HIDE_COMBAT_SWORD =
            SynchedEntityData.defineId(IvyTheDragonMerchant.class, EntityDataSerializers.BOOLEAN);
    private static final EntityDataAccessor<Integer> DATA_PASSIVE_USE_ACTION =
            SynchedEntityData.defineId(IvyTheDragonMerchant.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Boolean> DATA_DOWNED =
            SynchedEntityData.defineId(IvyTheDragonMerchant.class, EntityDataSerializers.BOOLEAN);
    private static final EntityDataAccessor<Integer> DATA_DEATH_ANIMATION =
            SynchedEntityData.defineId(IvyTheDragonMerchant.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Integer> DATA_DOWNED_ARISE_TICKS =
            SynchedEntityData.defineId(IvyTheDragonMerchant.class, EntityDataSerializers.INT);
    private static final int TRADE_START_TICKS = 29;
    private static final int TRADE_STOP_TICKS = 29;
    private static final int PASSIVE_USE_TICKS = 20;
    private static final int PASSIVE_USE_CONSUME_TICKS = 14;
    private static final int PASSIVE_USE_COOLDOWN_TICKS = 100;
    private static final int WATER_CLUTCH_ACTION_TICKS = 14;
    private static final int WATER_CLUTCH_COOLDOWN_TICKS = 80;
    private static final int WATER_CLUTCH_CLEAR_TICKS = 22;
    private static final float WATER_CLUTCH_MIN_FALL_DISTANCE = 5.0F;
    private static final double WATER_CLUTCH_MIN_FALL_SPEED = -0.55D;
    private static final int WATER_CLUTCH_GROUND_SCAN_BLOCKS = 5;
    private static final int DOWNED_BLEED_OUT_TICKS = 20 * 90;
    private static final int DOWNED_FINISH_HITS = 3;
    private static final int DOWNED_REVIVE_REQUIRED_TICKS = 60;
    private static final int DOWNED_REVIVE_INTERACTION_PROGRESS = 5;
    private static final int DOWNED_REVIVE_INTERACTION_GRACE_TICKS = 8;
    private static final int DOWNED_ARISE_TICKS = 30;
    private static final int DOWNED_AGGRO_CLEAR_INTERVAL_TICKS = 10;
    private static final double DOWNED_AGGRO_CLEAR_RADIUS = 32.0D;
    private static final int RECRUITED_DEATH_ANIMATION_TICKS = 32;
    private static final int WILD_DEATH_ANIMATION_TICKS = 76;
    private static final float RECRUITED_COMBAT_FOOD_HEALTH = 12.0F;
    private static final float RECRUITED_PASSIVE_FOOD_HEALTH = 20.0F;
    private static final int IDLE_CHATTER_DURATION = 90;
    private static final int IDLE_CHATTER_MIN_COOLDOWN = 600;
    private static final int IDLE_CHATTER_MAX_COOLDOWN = 1400;
    private static final double IDLE_CHATTER_OWNER_DISTANCE_SQR = 100.0D;
    private static final String INSTANT_CONTROLLER = "instant";
    private static final String DOWNED_CONTROLLER = "downed_state";
    private static final RawAnimation TRADE_START = RawAnimation.begin().thenPlay("ivy_oleander.animation.trade_start");
    private static final RawAnimation TRADING = RawAnimation.begin().thenLoop("ivy_oleander.animation.trading");
    private static final RawAnimation TRADE_STOP = RawAnimation.begin().thenPlay("ivy_oleander.animation.trade_stop");
    private static final RawAnimation IDLE = RawAnimation.begin().thenLoop("ivy_oleander.animation.idle");
    private static final RawAnimation WALK = RawAnimation.begin().thenLoop("ivy_oleander.animation.walk");
    private static final RawAnimation RUN = RawAnimation.begin().thenLoop("ivy_oleander.animation.run");
    private static final RawAnimation FALLING = RawAnimation.begin().thenLoop("ivy_oleander.animation.falling");
    private static final RawAnimation SWIM_IDLE = RawAnimation.begin().thenLoop("ivy_oleander.animation.swim_idle");
    private static final RawAnimation SWIM = RawAnimation.begin().thenLoop("ivy_oleander.animation.swim");
    private static final RawAnimation SWIM_FAST = RawAnimation.begin().thenLoop("ivy_oleander.animation.swim_fast");
    private static final RawAnimation WATER_WADE_IDLE = RawAnimation.begin().thenLoop("ivy_oleander.animation.water_wade_idle");
    private static final RawAnimation WATER_WADING = RawAnimation.begin().thenLoop("ivy_oleander.animation.water_wading");
    private static final RawAnimation CLIMBING = RawAnimation.begin().thenLoop("ivy_oleander.animation.climbing");
    private static final RawAnimation CLIMB_IDLE = RawAnimation.begin().thenLoop("ivy_oleander.animation.climb_idle");
    private static final RawAnimation MOUNTING = RawAnimation.begin().thenLoop("ivy_oleander.animation.mounting");
    private static final RawAnimation ABOUT_TO_DIE = RawAnimation.begin().thenPlayAndHold("ivy_oleander.animation.about_to_die");
    private static final RawAnimation DIE = RawAnimation.begin().thenPlay("ivy_oleander.animation.die");
    private static final RawAnimation ARISE = RawAnimation.begin().thenPlay("ivy_oleander.animation.arise");
    private static final RawAnimation ACTUALLY_DIE = RawAnimation.begin().thenPlay("ivy_oleander.animation.actually_die");
    private static final RawAnimation EMBARRASSED = RawAnimation.begin().thenPlay("ivy_oleander.animation.embarrassed");
    private static final RawAnimation SIGH = RawAnimation.begin().thenPlay("ivy_oleander.animation.sigh");
    private static final RawAnimation HMM_TRADER = RawAnimation.begin().thenPlayAndHold("ivy_oleander.animation.hmm_trader");
    private static final RawAnimation HMM_GARDENER = RawAnimation.begin().thenPlayAndHold("ivy_oleander.animation.hmm_gardener");
    private static final RawAnimation HMM_DRAGON_ADVICE = RawAnimation.begin().thenPlayAndHold("ivy_oleander.animation.hmm_dragon_advice");
    private static final RawAnimation HMM_TRADER_EXIT_TO_IDLE = RawAnimation.begin().thenPlay("ivy_oleander.animation.hmm_trader_exit_to_idle");
    private static final RawAnimation HMM_GARDENER_EXIT_TO_IDLE = RawAnimation.begin().thenPlay("ivy_oleander.animation.hmm_garderner_exit_to_idle");
    private static final RawAnimation HMM_DRAGON_ADVICE_EXIT_TO_IDLE = RawAnimation.begin().thenPlay("ivy_oleander.animation.hmm_dragon_advice_exit_to_idle");
    private static final String PASSIVE_USE_CONTROLLER = "passive_use";
    private static final RawAnimation MAIN_HAND_EAT = RawAnimation.begin().thenPlay("ivy_oleander.animation.main_hand_eat");
    private static final RawAnimation MAIN_HAND_DRINK = RawAnimation.begin().thenPlay("ivy_oleander.animation.main_hand_drink");
    private static final RawAnimation MAIN_HAND_INTERACT = RawAnimation.begin().thenPlay("ivy_oleander.animation.main_hand_interact");
    private static final String KNOWN_DIALOGUE_PLAYERS_TAG = "KnownDialoguePlayers";
    private static final String KNOWN_DIALOGUE_UUID_TAG = "UUID";
    private static final String REMEMBERED_NAME_TAG = "Name";
    private static final String REMEMBERED_IMPRESSION_TAG = "Impression";
    private static final String REMEMBERED_DIALOGUE_FLAGS_TAG = "DialogueFlags";
    private static final String TAME_TAG = "Tame";
    private static final String OWNER_UUID_TAG = "OwnerUUID";
    private static final String COMMAND_TAG = "Command";
    private static final String IVY_INVENTORY_TAG = "IvyInventory";
    private static final String IVY_INVENTORY_SLOT_TAG = "Slot";
    private static final String DOWNED_TAG = "Downed";
    private static final String DOWNED_BLEED_TICKS_TAG = "DownedBleedTicks";
    private static final String DOWNED_FINISH_HITS_TAG = "DownedFinishHits";
    private static final String DOWNED_REVIVE_PROGRESS_TAG = "DownedReviveProgress";
    private static final String TRESPASSER_IMPRESSION = "trespasser";
    private static final String RUDE_IMPRESSION = "rude";
    private static final String WARES_IMPRESSION = "wares";
    private static final double XP_PICKUP_RADIUS = 1.0D;
    private static final int XP_PICKUP_INTERVAL = 4;
    private final AnimatableInstanceCache animationCache = GeckoLibUtil.createInstanceCache(this);
    private int tradeAnimTicks = 0;
    private boolean lastTradingState = false;
    private boolean wasMovementStopped = false;
    private boolean wasDownedOrArisingAnimation = false;
    private boolean wasBoxingAnimation = false;
    private final IvyMovementVisualState movementVisualState = new IvyMovementVisualState();
    private final GenericSwimSteeringController swimSteering;
    private final AsyncSwimController asyncSwimController;
    private IvyBodyControl bodyControl;
    private IvyCompanionController companionController;
    public final SmoothValue bodyRotDeviation = SmoothValue.rotation(0.0);
    private int idleChatterTicks = 0;
    private int idleChatterCooldown;
    private int lastCombatChatterTargetId = -1;
    private final Map<UUID, String> rememberedDialogueNames = new HashMap<>();
    private final Map<UUID, String> rememberedDialogueImpressions = new HashMap<>();
    private final Map<UUID, Set<String>> rememberedDialogueFlags = new HashMap<>();
    private final HumanSoundHandler soundHandler;
    private final SimpleContainer ivyInventory = new SimpleContainer(IvyInventoryMenu.IVY_SLOT_COUNT);
    private IvyCombatBrain boxingCombat;
    private final int restockInterval;
    private int restockTimer;
    private boolean clientRecoveryItemVisible;
    private UUID pendingDialogueTradePlayerUuid;
    private ResourceLocation pendingDialogueTradeId;
    private String pendingDialogueTradeNodeId;
    private int pendingDialogueTradeResumeTicks;
    private int passiveUseTicks;
    private int passiveUseConsumeTicks;
    private int passiveUseCooldown;
    private int waterClutchCooldown;
    private int waterClutchWaterTicks;
    private int xpPickupCooldown;
    private int downedBleedTicks;
    private int downedFinishHits;
    private int downedReviveProgress;
    private int downedReviveInteractionGraceTicks;
    private int downedAggroClearCooldown;
    private boolean finishingDownedDeath;
    @Nullable
    private BlockPos waterClutchWaterPos;
    @Nullable
    private String holdingDialogueExpressionTrigger;

    public IvyTheDragonMerchant(EntityType<? extends AbstractVillager> entityType, Level level) {
        super(entityType, level);
        this.setPersistenceRequired();
        this.lookControl = new GenericLookControl(this);
        this.swimSteering = new GenericSwimSteeringController(this);
        this.asyncSwimController = new AsyncSwimController(this, this.swimSteering);
        this.setPathfindingMalus(BlockPathTypes.WATER, 0.0F);
        this.setPathfindingMalus(BlockPathTypes.WATER_BORDER, 0.0F);
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

    public boolean isDowned() {
        return this.entityData.get(DATA_DOWNED);
    }

    private void setDowned(boolean downed) {
        this.entityData.set(DATA_DOWNED, downed);
    }

    private int getDeathAnimation() {
        return this.entityData.get(DATA_DEATH_ANIMATION);
    }

    private void setDeathAnimation(int animation) {
        this.entityData.set(DATA_DEATH_ANIMATION, animation);
    }

    private int getDownedAriseTicks() {
        return this.entityData.get(DATA_DOWNED_ARISE_TICKS);
    }

    private void setDownedAriseTicks(int ticks) {
        int clampedTicks = Math.max(0, ticks);
        this.entityData.set(DATA_DOWNED_ARISE_TICKS, clampedTicks);
    }

    public int cycleCompanionCommand() {
        int next = (getCommand() + 1) % CompanionCommand.values().length;
        setCommand(next);
        return getCommand();
    }

    public String getIdleChatterText() {
        return this.entityData.get(DATA_IDLE_CHATTER);
    }

    public String getIdleChatterName() {
        return this.entityData.get(DATA_IDLE_CHATTER_NAME);
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
        this.entityData.define(DATA_IDLE_CHATTER_NAME, "");
        this.entityData.define(DATA_TRADE_ANIM_STATE, TradeAnimState.NONE.id);
        this.entityData.define(DATA_IDLE_VARIANT_ACTIVE, false);
        this.entityData.define(DATA_CLIMBING_LADDER, false);
        this.entityData.define(DATA_BOXING_STANCE, false);
        this.entityData.define(DATA_BOXING_BACKING_UP, false);
        this.entityData.define(DATA_BOXING_FAST, false);
        this.entityData.define(DATA_BOXING_ACTION_TICKS, 0);
        this.entityData.define(DATA_BOXING_ANIMATION, "");
        this.entityData.define(DATA_BOXING_EXITING, false);
        this.entityData.define(DATA_BOXING_RECOVERY_ACTION, RECOVERY_NONE);
        this.entityData.define(DATA_BOXING_SWORD_STYLE, false);
        this.entityData.define(DATA_HIDE_COMBAT_SWORD, false);
        this.entityData.define(DATA_PASSIVE_USE_ACTION, PASSIVE_USE_NONE);
        this.entityData.define(DATA_DOWNED, false);
        this.entityData.define(DATA_DEATH_ANIMATION, 0);
        this.entityData.define(DATA_DOWNED_ARISE_TICKS, 0);
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
        goalSelector.addGoal(1, IvySwimGoal.breathe(this));
        goalSelector.addGoal(2, new OpenDoorGoal(this, true));
        goalSelector.addGoal(2, getBoxingCombat().createGoal());
        goalSelector.addGoal(2, getCompanionController().createBoardOwnerVehicleGoal());
        goalSelector.addGoal(3, IvySwimGoal.target(this));
        goalSelector.addGoal(3, getCompanionController().createStayGoal());
        goalSelector.addGoal(4, IvySwimGoal.followOwner(this));
        goalSelector.addGoal(4, getCompanionController().createLadderClimbGoal());
        goalSelector.addGoal(5, getCompanionController().createFollowOwnerGoal());
        goalSelector.addGoal(6, new RandomStrollGoal(this, 0.6) {
            @Override
            public boolean canUse() {
                return IvyTheDragonMerchant.this.getTarget() == null
                        && (!IvyTheDragonMerchant.this.isInWaterOrBubble()
                        || IvyTheDragonMerchant.this.isInShallowWaterForWading())
                        && (!IvyTheDragonMerchant.this.isTame()
                        || IvyTheDragonMerchant.this.getCompanionCommand() == CompanionCommand.WANDER)
                        && !IvyTheDragonMerchant.this.isInDialogue()
                        && !IvyTheDragonMerchant.this.isBoxingCombatActive()
                        && super.canUse();
            }

            @Override
            public boolean canContinueToUse() {
                return IvyTheDragonMerchant.this.getTarget() == null
                        && (!IvyTheDragonMerchant.this.isInWaterOrBubble()
                        || IvyTheDragonMerchant.this.isInShallowWaterForWading())
                        && (!IvyTheDragonMerchant.this.isTame()
                        || IvyTheDragonMerchant.this.getCompanionCommand() == CompanionCommand.WANDER)
                        && !IvyTheDragonMerchant.this.isInDialogue()
                        && !IvyTheDragonMerchant.this.isBoxingCombatActive()
                        && super.canContinueToUse();
            }
        });
        goalSelector.addGoal(7, new LookAtPlayerGoal(this, Player.class, 6.0f) {
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
        goalSelector.addGoal(8, new RandomLookAroundGoal(this) {
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

    boolean isInShallowWaterForWading() {
        if (!isInWaterOrBubble() || isUnderWater()) {
            return false;
        }
        BlockPos feet = BlockPos.containing(getX(), getBoundingBox().minY, getZ());
        if (!level().getFluidState(feet).is(FluidTags.WATER)) {
            return false;
        }
        return onGround() && getFluidDepthUp(feet) <= 1 && getFluidDepthDown(feet) <= 1;
    }

    boolean isCombatBlockedByWater() {
        return isUnderWater() || (isInWaterOrBubble() && !isInShallowWaterForWading());
    }

    boolean isRidingCompanionVehicle() {
        return getVehicle() instanceof Boat || getVehicle() instanceof Cindervane;
    }

    int getFluidDepthUp() {
        return getFluidDepthUp(BlockPos.containing(getX(), getBoundingBox().minY, getZ()));
    }

    int getFluidDepthDown() {
        return getFluidDepthDown(BlockPos.containing(getX(), getBoundingBox().minY, getZ()));
    }

    private int getFluidDepthDown(BlockPos start) {
        BlockPos cursor = start;
        if (!level().getFluidState(cursor).is(FluidTags.WATER)) {
            return 0;
        }
        int startY = start.getY();
        int worldBottom = level().getMinBuildHeight();
        while (cursor.getY() > worldBottom && level().getFluidState(cursor).is(FluidTags.WATER)) {
            cursor = cursor.below();
        }
        return startY - cursor.getY();
    }

    private int getFluidDepthUp(BlockPos start) {
        BlockPos cursor = start;
        int worldTop = level().getMaxBuildHeight();
        while (cursor.getY() < worldTop && level().getFluidState(cursor).is(FluidTags.WATER)) {
            cursor = cursor.above();
        }
        return cursor.getY() - start.getY();
    }

    AsyncSwimController getAsyncSwimController() {
        return asyncSwimController;
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
            this.entityData.set(DATA_BOXING_ANIMATION, "");
            this.entityData.set(DATA_BOXING_EXITING, false);
            this.entityData.set(DATA_BOXING_RECOVERY_ACTION, RECOVERY_NONE);
            this.entityData.set(DATA_BOXING_SWORD_STYLE, false);
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
        int passiveUseAction = this.entityData.get(DATA_PASSIVE_USE_ACTION);
        if (passiveUseAction == PASSIVE_USE_WATER_CLUTCH) {
            return new ItemStack(Items.WATER_BUCKET);
        }
        if (passiveUseAction == PASSIVE_USE_DRINK) {
            return new ItemStack(Items.MILK_BUCKET);
        }
        if (passiveUseAction == PASSIVE_USE_EAT) {
            return getRecoveryFoodForRender();
        }
        if (!clientRecoveryItemVisible) {
            return ItemStack.EMPTY;
        }
        if (isBoxingDrinking()) {
            return new ItemStack(Items.MILK_BUCKET);
        }
        if (isBoxingEating()) {
            return getRecoveryFoodForRender();
        }
        return ItemStack.EMPTY;
    }

    private ItemStack getRecoveryFoodForRender() {
        ItemStack food = getFirstRecoveryFoodStack();
        if (food.isEmpty()) {
            return new ItemStack(Items.COOKED_BEEF);
        }
        ItemStack copy = food.copy();
        copy.setCount(1);
        return copy;
    }

    public boolean shouldRenderSword() {
        return isBoxingSwordStyle()
                && getBoxingCombat().isActive()
                && !isCombatSwordHidden()
                && !isBoxingRecovering()
                && this.entityData.get(DATA_PASSIVE_USE_ACTION) == PASSIVE_USE_NONE
                && hasEquippedSword();
    }

    public ItemStack getSwordForRender() {
        return shouldRenderSword() ? getEquippedSword().copy() : ItemStack.EMPTY;
    }

    boolean hasEquippedSword() {
        return isWeaponStack(getEquippedSword());
    }

    ItemStack getEquippedSword() {
        return ivyInventory.getItem(IvyInventoryMenu.SWORD_SLOT);
    }

    float getEquippedSwordDamageAgainst(LivingEntity target) {
        ItemStack sword = getEquippedSword();
        if (!isWeaponStack(sword)) {
            return (float) getAttributeValue(Attributes.ATTACK_DAMAGE);
        }

        double damage = 1.0D;
        Multimap<net.minecraft.world.entity.ai.attributes.Attribute, AttributeModifier> modifiers =
                sword.getAttributeModifiers(EquipmentSlot.MAINHAND);
        for (AttributeModifier modifier : modifiers.get(Attributes.ATTACK_DAMAGE)) {
            if (modifier.getOperation() == AttributeModifier.Operation.ADDITION) {
                damage += modifier.getAmount();
            } else if (modifier.getOperation() == AttributeModifier.Operation.MULTIPLY_BASE) {
                damage += 1.0D * modifier.getAmount();
            } else if (modifier.getOperation() == AttributeModifier.Operation.MULTIPLY_TOTAL) {
                damage *= 1.0D + modifier.getAmount();
            }
        }
        damage += EnchantmentHelper.getDamageBonus(sword, target.getMobType());
        return (float) Math.max(0.0D, damage);
    }

    int getEquippedSwordKnockbackBonus() {
        ItemStack sword = getEquippedSword();
        return sword.isEmpty() ? 0 : EnchantmentHelper.getItemEnchantmentLevel(Enchantments.KNOCKBACK, sword);
    }

    void applyEquippedSwordPostHit(LivingEntity target) {
        ItemStack sword = getEquippedSword();
        if (!isWeaponStack(sword)) {
            return;
        }

        int fireAspect = EnchantmentHelper.getItemEnchantmentLevel(Enchantments.FIRE_ASPECT, sword);
        if (fireAspect > 0) {
            target.setSecondsOnFire(fireAspect * 4);
        }
        sword.getItem().hurtEnemy(sword, target, this);
    }

    void handleCombatKill(LivingEntity target) {
        if (!(level() instanceof ServerLevel server) || target.isAlive() || !(target instanceof Mob mob)) {
            return;
        }
        int xp = mob.getExperienceReward();
        if (xp <= 0) {
            return;
        }
        int remainingXp = isTame() ? applyMendingFromExperience(xp) : xp;
        if (remainingXp > 0) {
            ExperienceOrb.award(server, target.position(), remainingXp);
        }
    }

    private void tickExperiencePickup() {
        if (!isTame() || !(level() instanceof ServerLevel server)) {
            return;
        }
        if (xpPickupCooldown > 0) {
            xpPickupCooldown--;
            return;
        }
        xpPickupCooldown = XP_PICKUP_INTERVAL;

        List<ExperienceOrb> orbs = level().getEntitiesOfClass(ExperienceOrb.class,
                getBoundingBox().inflate(XP_PICKUP_RADIUS),
                orb -> orb.isAlive() && orb.tickCount > 2 && orb.getValue() > 0);
        for (ExperienceOrb orb : orbs) {
            pickupExperienceOrb(server, orb);
        }
    }

    private void pickupExperienceOrb(ServerLevel server, ExperienceOrb orb) {
        int originalValue = orb.getValue();
        int remainingValue = applyMendingFromExperience(originalValue);
        if (remainingValue == originalValue) {
            return;
        }

        take(orb, 1);
        playSound(SoundEvents.EXPERIENCE_ORB_PICKUP, 0.1F,
                (getRandom().nextFloat() - getRandom().nextFloat()) * 0.35F + 0.9F);
        if (remainingValue <= 0) {
            orb.discard();
        } else {
            Vec3 pos = orb.position();
            orb.discard();
            ExperienceOrb.award(server, pos, remainingValue);
        }
    }

    private int applyMendingFromExperience(int xp) {
        int remainingXp = xp;
        while (remainingXp > 0) {
            ItemStack stack = getRandomDamagedMendingItem();
            if (stack.isEmpty()) {
                break;
            }
            int repairedDurability = Math.min(remainingXp * 2, stack.getDamageValue());
            if (repairedDurability <= 0) {
                break;
            }
            stack.setDamageValue(stack.getDamageValue() - repairedDurability);
            remainingXp -= repairedDurability / 2;
        }
        if (remainingXp != xp) {
            ivyInventory.setChanged();
        }
        return Math.max(0, remainingXp);
    }

    private ItemStack getRandomDamagedMendingItem() {
        List<ItemStack> stacks = new ArrayList<>();
        for (int slot = 0; slot < IvyInventoryMenu.IVY_SLOT_COUNT; slot++) {
            ItemStack stack = ivyInventory.getItem(slot);
            if (!stack.isEmpty()
                    && stack.isDamaged()
                    && EnchantmentHelper.getItemEnchantmentLevel(Enchantments.MENDING, stack) > 0) {
                stacks.add(stack);
            }
        }
        return stacks.isEmpty() ? ItemStack.EMPTY : stacks.get(getRandom().nextInt(stacks.size()));
    }

    boolean isBoxingSwordStyle() {
        return this.entityData.get(DATA_BOXING_SWORD_STYLE);
    }

    void setBoxingSwordStyle(boolean swordStyle) {
        this.entityData.set(DATA_BOXING_SWORD_STYLE, swordStyle);
    }

    void setCombatSwordHidden(boolean hidden) {
        this.entityData.set(DATA_HIDE_COMBAT_SWORD, hidden);
    }

    private boolean isCombatSwordHidden() {
        return this.entityData.get(DATA_HIDE_COMBAT_SWORD);
    }

    @Override
    public @NotNull Iterable<ItemStack> getArmorSlots() {
        List<ItemStack> armor = new ArrayList<>(4);
        armor.add(ivyInventory.getItem(IvyInventoryMenu.BOOTS_SLOT));
        armor.add(ivyInventory.getItem(IvyInventoryMenu.LEGGINGS_SLOT));
        armor.add(ivyInventory.getItem(IvyInventoryMenu.CHESTPLATE_SLOT));
        armor.add(ivyInventory.getItem(IvyInventoryMenu.HELMET_SLOT));
        return armor;
    }

    @Override
    public @NotNull ItemStack getItemBySlot(@NotNull EquipmentSlot slot) {
        return switch (slot) {
            case MAINHAND -> getEquippedSword();
            case HEAD -> ivyInventory.getItem(IvyInventoryMenu.HELMET_SLOT);
            case CHEST -> ivyInventory.getItem(IvyInventoryMenu.CHESTPLATE_SLOT);
            case LEGS -> ivyInventory.getItem(IvyInventoryMenu.LEGGINGS_SLOT);
            case FEET -> ivyInventory.getItem(IvyInventoryMenu.BOOTS_SLOT);
            default -> super.getItemBySlot(slot);
        };
    }

    @Override
    public void setItemSlot(@NotNull EquipmentSlot slot, @NotNull ItemStack stack) {
        switch (slot) {
            case MAINHAND -> setIvyEquipmentItem(IvyInventoryMenu.SWORD_SLOT, stack);
            case HEAD -> setIvyEquipmentItem(IvyInventoryMenu.HELMET_SLOT, stack);
            case CHEST -> setIvyEquipmentItem(IvyInventoryMenu.CHESTPLATE_SLOT, stack);
            case LEGS -> setIvyEquipmentItem(IvyInventoryMenu.LEGGINGS_SLOT, stack);
            case FEET -> setIvyEquipmentItem(IvyInventoryMenu.BOOTS_SLOT, stack);
            default -> super.setItemSlot(slot, stack);
        }
    }

    private void setIvyEquipmentItem(int slot, ItemStack stack) {
        if (stack.isEmpty() || canStayInEquipmentSlot(slot, stack)) {
            ivyInventory.setItem(slot, stack);
            ivyInventory.setChanged();
            return;
        }
        addInventoryRemainderOrDrop(stack.copy());
    }

    @Override
    public int getArmorValue() {
        int armor = 0;
        for (ItemStack stack : getArmorSlots()) {
            if (stack.getItem() instanceof ArmorItem armorItem) {
                armor += armorItem.getDefense();
            }
        }
        return armor;
    }

    private float getEquippedArmorToughness() {
        float toughness = 0.0F;
        for (ItemStack stack : getArmorSlots()) {
            if (stack.getItem() instanceof ArmorItem armorItem) {
                toughness += armorItem.getToughness();
            }
        }
        return toughness;
    }

    @Override
    protected float getDamageAfterArmorAbsorb(@NotNull DamageSource source, float amount) {
        if (!source.is(DamageTypeTags.BYPASSES_ARMOR)) {
            hurtArmor(source, amount);
            amount = CombatRules.getDamageAfterAbsorb(amount, getArmorValue(), getEquippedArmorToughness());
        }
        return amount;
    }

    @Override
    public void hurtArmor(@NotNull DamageSource source, float amount) {
        if (amount <= 0.0F) {
            return;
        }
        amount /= 4.0F;
        if (amount < 1.0F) {
            amount = 1.0F;
        }
        hurtArmorStack(source, IvyInventoryMenu.HELMET_SLOT, EquipmentSlot.HEAD, amount);
        hurtArmorStack(source, IvyInventoryMenu.CHESTPLATE_SLOT, EquipmentSlot.CHEST, amount);
        hurtArmorStack(source, IvyInventoryMenu.LEGGINGS_SLOT, EquipmentSlot.LEGS, amount);
        hurtArmorStack(source, IvyInventoryMenu.BOOTS_SLOT, EquipmentSlot.FEET, amount);
    }

    private void hurtArmorStack(DamageSource source, int slot, EquipmentSlot equipmentSlot, float amount) {
        ItemStack stack = ivyInventory.getItem(slot);
        boolean fireResistant = source.is(DamageTypeTags.IS_FIRE) && stack.getItem().isFireResistant();
        if (!fireResistant && stack.getItem() instanceof ArmorItem) {
            stack.hurtAndBreak((int) amount, this, ivy -> ivy.broadcastBreakEvent(equipmentSlot));
        }
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

    String getBoxingAnimation() {
        return this.entityData.get(DATA_BOXING_ANIMATION);
    }

    void setBoxingAnimation(String animation) {
        this.entityData.set(DATA_BOXING_ANIMATION, animation == null ? "" : animation);
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
    public void setCustomName(@Nullable Component name) {
    }

    @Override
    public @Nullable Component getCustomName() {
        return null;
    }

    @Override
    public boolean hasCustomName() {
        return false;
    }

    @Override
    public @NotNull InteractionResult mobInteract(@NotNull Player player, @NotNull InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        if (stack.is(ModItems.IVY_THE_MERCHANT_SPAWN_EGG.get())) {
            return super.mobInteract(player, hand);
        }
        if (stack.is(Items.NAME_TAG) && stack.hasCustomHoverName()) {
            if (!level().isClientSide) {
                refuseRenameAttempt();
            }
            return InteractionResult.sidedSuccess(level().isClientSide);
        }
        if (!isAlive()) {
            return InteractionResult.PASS;
        }
        if (isDowned()) {
            if (!level().isClientSide && player instanceof ServerPlayer serverPlayer && isOwnedBy(player)) {
                handleDownedReviveInteraction(serverPlayer);
            }
            return InteractionResult.sidedSuccess(level().isClientSide);
        }
        if (isBaby()) {
            return super.mobInteract(player, hand);
        }
        if (level().isClientSide) {
            return InteractionResult.CONSUME;
        }
        clearInvalidDialogueBlockingTarget();
        if (isCombatBlockedByWater()) {
            if (player instanceof ServerPlayer) {
                speakUnderwaterChatter(player);
            }
            return InteractionResult.CONSUME;
        }
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
        if (isTame() && isOwnedBy(player)) {
            return RECRUITED_GREETING_DIALOGUE;
        }
        if (!hasRememberedDialogueName(player)) {
            return FIRST_MEETING_DIALOGUE;
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
                new AnimationController<>(this, "movement", 3, this::animationPredicate)
                        .receiveTriggeredAnimations();
        movementController.setSoundKeyframeHandler(this::handleSoundKeyframe);
        movementController.setParticleKeyframeHandler(this::handleParticleKeyframe);
        setupMovementController(movementController);

        AnimationController<IvyTheDragonMerchant> downedController =
                new AnimationController<>(this, DOWNED_CONTROLLER, 1, this::downedAnimationPredicate);
        downedController.setSoundKeyframeHandler(this::handleSoundKeyframe);

        AnimationController<IvyTheDragonMerchant> passiveUseController =
                new AnimationController<>(this, PASSIVE_USE_CONTROLLER, 1, state -> PlayState.STOP);
        passiveUseController.setSoundKeyframeHandler(this::handleSoundKeyframe);
        setupPassiveUseController(passiveUseController);

        AnimationController<IvyTheDragonMerchant> instantController =
                new AnimationController<>(this, INSTANT_CONTROLLER, 1, this::instantAnimationPredicate)
                        .receiveTriggeredAnimations();
        instantController.setSoundKeyframeHandler(this::handleSoundKeyframe);
        setupInstantController(instantController);

        controllers.add(movementController, downedController, passiveUseController, instantController);
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
        if (deathTime > 0 || getDeathAnimation() != 0 || !isAlive()) {
            if (state.getController().isPlayingTriggeredAnimation()) {
                state.getController().forceAnimationReset();
            }
            return PlayState.STOP;
        }
        if (isDowned()) {
            clearMovementTriggerIfNeeded(state);
            wasDownedOrArisingAnimation = true;
            return PlayState.STOP;
        }
        if (getDownedAriseTicks() > 0) {
            clearMovementTriggerIfNeeded(state);
            wasDownedOrArisingAnimation = true;
            return PlayState.STOP;
        }
        if (isRidingCompanionVehicle()) {
            clearMovementTriggerIfNeeded(state);
            AnimationHelper.setAndContinue(state, MOUNTING);
            state.getController().transitionLength(2);
            return PlayState.CONTINUE;
        }
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
        if (!getBoxingCombat().isActive() && state.getController().isPlayingTriggeredAnimation()) {
            return PlayState.CONTINUE;
        }

        if (wasMovementStopped || wasDownedOrArisingAnimation) {
            state.getController().forceAnimationReset();
            wasMovementStopped = false;
            wasDownedOrArisingAnimation = false;
        }

        if (getBoxingCombat().isActive()) {
            state.getController().transitionLength(1);
        }

        if (getBoxingCombat().applyMovementAnimation(state)) {
            wasBoxingAnimation = true;
            return PlayState.CONTINUE;
        }

        if (wasBoxingAnimation) {
            state.getController().forceAnimationReset();
            wasBoxingAnimation = false;
        }

        movementVisualState.apply(state, this, IDLE, WALK, RUN, FALLING, CLIMBING, CLIMB_IDLE,
                SWIM_IDLE, SWIM, SWIM_FAST, WATER_WADE_IDLE, WATER_WADING);
        return PlayState.CONTINUE;
    }

    private void clearMovementTriggerIfNeeded(AnimationState<?> state) {
        if (state.getController().isPlayingTriggeredAnimation()) {
            state.getController().forceAnimationReset();
        }
    }

    private <T extends GeoEntity> PlayState downedAnimationPredicate(AnimationState<T> state) {
        if (deathTime > 0 || getDeathAnimation() != 0 || !isAlive()) {
            if (getDeathAnimation() == 1 || (getDeathAnimation() == 0 && isTame())) {
                AnimationHelper.setAndContinue(state, DIE);
                state.getController().transitionLength(1);
                return PlayState.CONTINUE;
            }
            return PlayState.STOP;
        }
        if (isDowned()) {
            AnimationHelper.setAndContinue(state, ABOUT_TO_DIE);
            state.getController().transitionLength(1);
            return PlayState.CONTINUE;
        }
        if (getDownedAriseTicks() > 0) {
            AnimationHelper.setAndContinue(state, ARISE);
            state.getController().transitionLength(1);
            return PlayState.CONTINUE;
        }
        return PlayState.STOP;
    }

    private <T extends GeoEntity> PlayState instantAnimationPredicate(AnimationState<T> state) {
        if (deathTime > 0 && (getDeathAnimation() == 2 || !isTame())) {
            state.getController().transitionLength(1);
            AnimationHelper.setAndContinue(state, ACTUALLY_DIE);
            return PlayState.CONTINUE;
        }
        return PlayState.STOP;
    }

    @Override
    public boolean hurt(@NotNull DamageSource source, float amount) {
        if (isDowned()) {
            return hurtDowned(source, amount);
        }
        if (source.getEntity() instanceof Player player) {
            boolean hurt = super.hurt(source, amount);
            if (hurt && isTame() && isOwnedBy(player) && isAlive()) {
                speakOwnerHurtChatter(player);
            }
            if (getTarget() instanceof Player) {
                setTarget(null);
            }
            getBoxingCombat().clearPlayerReaction();
            return hurt;
        }
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
        if (isDownedOrArising() || !isAlive()) {
            return hurt;
        }
        getBoxingCombat().onHurt(source, hurt);
        return hurt;
    }

    @Override
    protected void actuallyHurt(@NotNull DamageSource source, float amount) {
        super.actuallyHurt(source, amount);
        if (getHealth() <= 0.0F) {
            if (!tryUseInventoryTotem(source)) {
                if (canEnterDownedState(source)) {
                    enterDownedState();
                } else if (getDeathAnimation() == 0) {
                    setDeathAnimation(isTame() ? 1 : 2);
                }
            }
        }
    }

    private boolean tryUseInventoryTotem(DamageSource source) {
        if (source.is(DamageTypeTags.BYPASSES_INVULNERABILITY)) {
            return false;
        }

        int totemSlot = findInventoryItem(Items.TOTEM_OF_UNDYING);
        if (totemSlot < 0) {
            return false;
        }

        consumeOneInventoryItem(totemSlot, ItemStack.EMPTY);
        setHealth(1.0F);
        removeAllEffects();
        addEffect(new MobEffectInstance(MobEffects.REGENERATION, 900, 1));
        addEffect(new MobEffectInstance(MobEffects.ABSORPTION, 100, 1));
        addEffect(new MobEffectInstance(MobEffects.FIRE_RESISTANCE, 800, 0));
        level().broadcastEntityEvent(this, (byte) 35);
        playSound(SoundEvents.TOTEM_USE, 1.0F, 1.0F);
        return true;
    }

    private boolean canEnterDownedState(DamageSource source) {
        return isTame()
                && !finishingDownedDeath
                && !source.is(DamageTypeTags.BYPASSES_INVULNERABILITY);
    }

    private void enterDownedState() {
        setHealth(1.0F);
        setDowned(true);
        setDeathAnimation(0);
        downedBleedTicks = DOWNED_BLEED_OUT_TICKS;
        downedFinishHits = 0;
        downedReviveProgress = 0;
        downedReviveInteractionGraceTicks = 0;
        setDownedAriseTicks(0);
        downedAggroClearCooldown = 0;
        finishingDownedDeath = false;
        clearDownedActivity();
        speakDownedChatter();
        clearDownedMobAggro();
        setNoAi(true);
    }

    private void clearDownedActivity() {
        lastTradingState = false;
        setTradingPlayer(null);
        setTradeAnimState(TradeAnimState.NONE);
        tradeAnimTicks = 0;
        clearPendingDialogueTrade();
        DialogueSessionRegistry.endForEntity(this);
        getNavigation().stop();
        setTarget(null);
        getBoxingCombat().clear();
        cancelPassiveUse();
        setIdleVariantActive(false);
        setRunning(false);
        setDeltaMovement(0.0D, Math.min(getDeltaMovement().y, 0.0D), 0.0D);
        hasImpulse = true;
    }

    private void holdDownedStill() {
        getNavigation().stop();
        setTarget(null);
        cancelPassiveUse();
        setIdleVariantActive(false);
        setRunning(false);
        setDeltaMovement(0.0D, Math.min(getDeltaMovement().y, 0.0D), 0.0D);
        hasImpulse = true;
    }

    private void clearDownedMobAggro() {
        if (level().isClientSide) {
            return;
        }
        for (Mob mob : level().getEntitiesOfClass(Mob.class,
                getBoundingBox().inflate(DOWNED_AGGRO_CLEAR_RADIUS),
                this::isMobTargetingDownedIvy)) {
            mob.setTarget(null);
            mob.setLastHurtByMob(null);
            mob.setLastHurtMob(null);
            eraseMobBrainMemory(mob, MemoryModuleType.ATTACK_TARGET);
            eraseMobBrainMemory(mob, MemoryModuleType.ANGRY_AT);
            mob.getNavigation().stop();
        }
    }

    private boolean hurtDowned(DamageSource source, float amount) {
        if (amount <= 0.0F) {
            return false;
        }
        if (source.getEntity() instanceof Mob || source.getDirectEntity() instanceof Mob) {
            clearDownedMobAggro();
            return false;
        }
        if (source.is(DamageTypeTags.BYPASSES_INVULNERABILITY)) {
            finishDownedDeath(source);
            return true;
        }
        downedFinishHits++;
        downedBleedTicks = Math.max(0, downedBleedTicks - 20 * 5);
        if (downedFinishHits >= DOWNED_FINISH_HITS) {
            finishDownedDeath(source);
        }
        return true;
    }

    private boolean isMobTargetingDownedIvy(Mob mob) {
        if (mob == this) {
            return false;
        }
        if (mob.getTarget() == this || mob.getLastHurtByMob() == this || mob.getLastHurtMob() == this) {
            return true;
        }
        return mobBrainAttackTargetIsIvy(mob) || mobBrainAngryAtIvy(mob);
    }

    private boolean mobBrainAttackTargetIsIvy(Mob mob) {
        try {
            return mob.getBrain()
                    .getMemory(MemoryModuleType.ATTACK_TARGET)
                    .map(target -> target == this)
                    .orElse(false);
        } catch (IllegalStateException ignored) {
            return false;
        }
    }

    private boolean mobBrainAngryAtIvy(Mob mob) {
        try {
            return mob.getBrain()
                    .getMemory(MemoryModuleType.ANGRY_AT)
                    .map(uuid -> uuid.equals(getUUID()))
                    .orElse(false);
        } catch (IllegalStateException ignored) {
            return false;
        }
    }

    private void eraseMobBrainMemory(Mob mob, MemoryModuleType<?> memoryType) {
        try {
            mob.getBrain().eraseMemory(memoryType);
        } catch (IllegalStateException ignored) {
        }
    }

    private void finishDownedDeath(DamageSource source) {
        if (level().isClientSide || finishingDownedDeath) {
            return;
        }
        finishingDownedDeath = true;
        clearChatter();
        setDowned(false);
        setNoAi(false);
        setDeathAnimation(1);
        setHealth(0.0F);
        die(source);
    }

    private void reviveFromDowned() {
        if (level().isClientSide || !isDowned()) {
            return;
        }
        setDowned(false);
        setNoAi(false);
        downedBleedTicks = 0;
        downedFinishHits = 0;
        downedReviveProgress = 0;
        downedReviveInteractionGraceTicks = 0;
        downedAggroClearCooldown = 0;
        clearChatter();
        setDownedAriseTicks(DOWNED_ARISE_TICKS);
        setHealth(Math.max(6.0F, getMaxHealth() * 0.35F));
        removeEffect(MobEffects.MOVEMENT_SLOWDOWN);
        setNoAi(true);
        getNavigation().stop();
        setTarget(null);
        setRunning(false);
    }

    private void handleDownedReviveInteraction(ServerPlayer player) {
        if (!isDowned() || distanceToSqr(player) > 9.0D) {
            return;
        }
        downedReviveInteractionGraceTicks = DOWNED_REVIVE_INTERACTION_GRACE_TICKS;
        downedReviveProgress = Math.min(DOWNED_REVIVE_REQUIRED_TICKS,
                downedReviveProgress + DOWNED_REVIVE_INTERACTION_PROGRESS);
        player.displayClientMessage(Component.literal("Reviving Ivy... "
                + downedReviveProgress + "/" + DOWNED_REVIVE_REQUIRED_TICKS), true);
        if (downedReviveProgress >= DOWNED_REVIVE_REQUIRED_TICKS) {
            reviveFromDowned();
        }
    }

    private void tickDownedState() {
        if (!isDowned()) {
            int ariseTicks = getDownedAriseTicks();
            if (ariseTicks > 0) {
                setDownedAriseTicks(ariseTicks - 1);
                if (ariseTicks <= 1) {
                    setNoAi(false);
                    getCompanionController().resumeOwnerDefenseAfterRecovery();
                }
            }
            return;
        }
        holdDownedStill();
        if (downedAggroClearCooldown > 0) {
            downedAggroClearCooldown--;
        } else {
            clearDownedMobAggro();
            downedAggroClearCooldown = DOWNED_AGGRO_CLEAR_INTERVAL_TICKS;
        }
        setHealth(Math.max(1.0F, getHealth()));
        fallDistance = 0.0F;
        if (downedReviveInteractionGraceTicks > 0) {
            downedReviveInteractionGraceTicks--;
        } else if (downedReviveProgress > 0) {
            downedReviveProgress--;
        }
        if (downedBleedTicks > 0) {
            downedBleedTicks--;
        } else {
            finishDownedDeath(damageSources().generic());
        }
    }

    @Override
    public void heal(float amount) {
        if (isDowned() && amount > 0.0F) {
            reviveFromDowned();
            return;
        }
        super.heal(amount);
    }

    @Override
    public void die(@NotNull DamageSource cause) {
        if (getDeathAnimation() == 0) {
            setDeathAnimation(isTame() ? 1 : 2);
        }
        if (getDeathAnimation() != 1) {
            triggerAnim(INSTANT_CONTROLLER, "actually_die");
        }
        super.die(cause);
    }

    @Override
    protected void tickDeath() {
        ++this.deathTime;
        int deathDuration = getDeathAnimation() == 1 ? RECRUITED_DEATH_ANIMATION_TICKS : WILD_DEATH_ANIMATION_TICKS;
        if (this.deathTime >= deathDuration && !this.level().isClientSide()) {
            this.level().broadcastEntityEvent(this, (byte) 60);
            this.remove(Entity.RemovalReason.KILLED);
        }
    }

    @Override
    public void setTarget(@Nullable LivingEntity target) {
        if (target != null && (isCombatBlockedByWater() || isRidingCompanionVehicle())) {
            super.setTarget(null);
            getBoxingCombat().clear();
            return;
        }
        if (target instanceof Player || (isTame() && isOwnedBy(target))) {
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
        if (!level().isClientSide && !isDowned()) {
            tickWaterClutchBeforeMovement();
        }
        super.tick();
        updateRotationDeviation();
        if (level().isClientSide) {
            if (!isBoxingRecovering()) {
                this.clientRecoveryItemVisible = false;
            }
            return;
        }
        tickDownedState();
        if (isDowned() || getDownedAriseTicks() > 0) {
            return;
        }
        tickTradingAnimation();
        tickDialogueTradeResume();
        tickWaterCombatBlock();
        tickCombatChatter();
        tickIdleChatter();
        tickExperiencePickup();
        getCompanionController().tick();
        clearInvalidDialogueBlockingTarget();
        if (hasLiveTarget() && isInDialogue()) {
            DialogueSessionRegistry.endForEntity(this);
        }
        getBoxingCombat().tryStartRetreatRecovery();
        getBoxingCombat().tick();
        tickPassiveUse();
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

    boolean needsCombatRecoveryFood() {
        return isTame()
                ? getHealth() > 0.0F && getHealth() < RECRUITED_COMBAT_FOOD_HEALTH
                : needsRecoveryFood();
    }

    private boolean needsPassiveRecoveryFood() {
        return getHealth() > 0.0F && getHealth() < RECRUITED_PASSIVE_FOOD_HEALTH;
    }

    boolean hasRecoveryMilk() {
        return !isTame() || findInventoryItem(Items.MILK_BUCKET) >= 0;
    }

    boolean hasRecoveryFood() {
        return !isTame() || findRecoveryFoodSlot() >= 0;
    }

    void drinkMilkForRecovery() {
        if (isTame()) {
            int slot = findInventoryItem(Items.MILK_BUCKET);
            if (slot < 0) {
                return;
            }
            consumeOneInventoryItem(slot, new ItemStack(Items.BUCKET));
        }
        for (var effect : new ArrayList<>(getActiveEffects())) {
            if (effect.getEffect().getCategory() == MobEffectCategory.HARMFUL) {
                removeEffect(effect.getEffect());
            }
        }
    }

    void eatFoodForRecovery() {
        if (isTame()) {
            int slot = findRecoveryFoodSlot();
            if (slot < 0) {
                return;
            }
            consumeOneInventoryItem(slot, ItemStack.EMPTY);
        }
        heal(8.0F);
    }

    private void tickPassiveUse() {
        if (passiveUseCooldown > 0) {
            passiveUseCooldown--;
        }
        if (this.entityData.get(DATA_PASSIVE_USE_ACTION) == PASSIVE_USE_WATER_CLUTCH) {
            if (passiveUseTicks > 0) {
                passiveUseTicks--;
            }
            if (passiveUseTicks <= 0) {
                setPassiveUseAction(PASSIVE_USE_NONE);
            }
            return;
        }
        if (getBoxingCombat().isActive() || isTrading() || isInDialogue()) {
            cancelPassiveUse();
            return;
        }
        if (passiveUseTicks > 0) {
            passiveUseTicks--;
            if (passiveUseConsumeTicks > 0 && --passiveUseConsumeTicks <= 0) {
                applyPassiveUseConsume();
            }
            if (passiveUseTicks <= 0) {
                setPassiveUseAction(PASSIVE_USE_NONE);
            }
            return;
        }
        if (passiveUseCooldown > 0 || !isTame() || !isAlive() || !isReadyForCombatAnimation()) {
            return;
        }
        if (hasHarmfulEffect() && hasRecoveryMilk()) {
            startPassiveUse(PASSIVE_USE_DRINK);
        } else if (needsPassiveRecoveryFood() && hasRecoveryFood()) {
            startPassiveUse(PASSIVE_USE_EAT);
        }
    }

    private void tickWaterClutchBeforeMovement() {
        if (waterClutchCooldown > 0) {
            waterClutchCooldown--;
        }
        tickPlacedWaterClutchBlock();

        if (!shouldTryWaterClutch()) {
            return;
        }

        if (findInventoryItem(Items.WATER_BUCKET) < 0) {
            return;
        }

        BlockPos clutchPos = findWaterClutchPlacement();
        if (clutchPos == null) {
            return;
        }

        level().setBlock(clutchPos, Blocks.WATER.defaultBlockState(), 11);
        level().playSound(null, clutchPos, SoundEvents.BUCKET_EMPTY, getSoundSource(), 0.85F, 0.95F + random.nextFloat() * 0.1F);
        waterClutchWaterPos = clutchPos.immutable();
        waterClutchWaterTicks = WATER_CLUTCH_CLEAR_TICKS;
        waterClutchCooldown = WATER_CLUTCH_COOLDOWN_TICKS;
        fallDistance = 0.0F;
        setPassiveUseAction(PASSIVE_USE_WATER_CLUTCH);
        passiveUseTicks = WATER_CLUTCH_ACTION_TICKS;
        passiveUseConsumeTicks = 0;
        triggerAnim(PASSIVE_USE_CONTROLLER, "main_hand_interact");
    }

    private boolean shouldTryWaterClutch() {
        return isTame()
                && isAlive()
                && waterClutchCooldown <= 0
                && !onGround()
                && !isInWaterOrBubble()
                && !isInLava()
                && fallDistance >= WATER_CLUTCH_MIN_FALL_DISTANCE
                && getDeltaMovement().y <= WATER_CLUTCH_MIN_FALL_SPEED;
    }

    @Nullable
    private BlockPos findWaterClutchPlacement() {
        BlockPos feet = BlockPos.containing(getX(), getBoundingBox().minY, getZ());
        BlockPos.MutableBlockPos cursor = new BlockPos.MutableBlockPos();
        for (int distance = 1; distance <= WATER_CLUTCH_GROUND_SCAN_BLOCKS; distance++) {
            BlockPos support = feet.below(distance);
            if (!level().hasChunkAt(support)) {
                return null;
            }
            BlockState supportState = level().getBlockState(support);
            if (supportState.getCollisionShape(level(), support).isEmpty()) {
                continue;
            }

            cursor.set(support.getX(), support.getY() + 1, support.getZ());
            BlockState placementState = level().getBlockState(cursor);
            if (placementState.isAir() && level().getFluidState(cursor).isEmpty()) {
                return cursor.immutable();
            }
            return null;
        }
        return null;
    }

    private void tickPlacedWaterClutchBlock() {
        if (waterClutchWaterTicks <= 0 || waterClutchWaterPos == null) {
            return;
        }
        waterClutchWaterTicks--;
        if (waterClutchWaterTicks > 0) {
            return;
        }
        BlockPos pos = waterClutchWaterPos;
        waterClutchWaterPos = null;
        if (level().getBlockState(pos).is(Blocks.WATER)) {
            level().setBlock(pos, Blocks.AIR.defaultBlockState(), 11);
        }
    }

    private void startPassiveUse(int action) {
        setPassiveUseAction(action);
        passiveUseTicks = PASSIVE_USE_TICKS;
        passiveUseConsumeTicks = PASSIVE_USE_CONSUME_TICKS;
        passiveUseCooldown = PASSIVE_USE_COOLDOWN_TICKS;
        triggerAnim(PASSIVE_USE_CONTROLLER, action == PASSIVE_USE_DRINK ? "main_hand_drink" : "main_hand_eat");
    }

    private void cancelPassiveUse() {
        if (passiveUseTicks <= 0 && this.entityData.get(DATA_PASSIVE_USE_ACTION) == PASSIVE_USE_NONE) {
            return;
        }
        passiveUseTicks = 0;
        passiveUseConsumeTicks = 0;
        setPassiveUseAction(PASSIVE_USE_NONE);
    }

    private void setPassiveUseAction(int action) {
        this.entityData.set(DATA_PASSIVE_USE_ACTION, action);
    }

    private void applyPassiveUseConsume() {
        int action = this.entityData.get(DATA_PASSIVE_USE_ACTION);
        if (action == PASSIVE_USE_DRINK) {
            drinkMilkForRecovery();
        } else if (action == PASSIVE_USE_EAT) {
            eatFoodForRecovery();
        }
    }

    private int findInventoryItem(Item item) {
        for (int slot = IvyInventoryMenu.STORAGE_START; slot < IvyInventoryMenu.IVY_SLOT_COUNT; slot++) {
            if (ivyInventory.getItem(slot).is(item)) {
                return slot;
            }
        }
        return -1;
    }

    boolean canThrowVenomProjectiles() {
        return !isTame() || findInventoryItem(ModItems.ARROW_OF_VENOM.get()) >= 0;
    }

    boolean consumeVenomArrowForThrow() {
        if (!isTame()) {
            return true;
        }
        int slot = findInventoryItem(ModItems.ARROW_OF_VENOM.get());
        if (slot < 0) {
            return false;
        }
        consumeOneInventoryItem(slot, ItemStack.EMPTY);
        return true;
    }

    private ItemStack getFirstRecoveryFoodStack() {
        int slot = findRecoveryFoodSlot();
        return slot < 0 ? ItemStack.EMPTY : ivyInventory.getItem(slot);
    }

    private int findRecoveryFoodSlot() {
        for (int slot = IvyInventoryMenu.STORAGE_START; slot < IvyInventoryMenu.IVY_SLOT_COUNT; slot++) {
            if (isCookedRecoveryFood(ivyInventory.getItem(slot))) {
                return slot;
            }
        }
        return -1;
    }

    private static boolean isCookedRecoveryFood(ItemStack stack) {
        if (stack.isEmpty() || !stack.isEdible()) {
            return false;
        }
        ResourceLocation id = BuiltInRegistries.ITEM.getKey(stack.getItem());
        return id != null && id.getPath().contains("cooked");
    }

    private void consumeOneInventoryItem(int slot, ItemStack remainder) {
        ItemStack stack = ivyInventory.getItem(slot);
        if (stack.isEmpty()) {
            return;
        }
        stack.shrink(1);
        if (stack.isEmpty()) {
            ivyInventory.setItem(slot, remainder.copy());
        } else if (!remainder.isEmpty()) {
            addInventoryRemainderOrDrop(remainder.copy());
        }
        ivyInventory.setChanged();
    }

    private void addInventoryRemainderOrDrop(ItemStack remainder) {
        for (int slot = IvyInventoryMenu.STORAGE_START; slot < IvyInventoryMenu.IVY_SLOT_COUNT; slot++) {
            ItemStack stack = ivyInventory.getItem(slot);
            if (stack.isEmpty()) {
                ivyInventory.setItem(slot, remainder);
                ivyInventory.setChanged();
                return;
            }
            if (ItemStack.isSameItemSameTags(stack, remainder) && stack.getCount() < stack.getMaxStackSize()) {
                int moved = Math.min(remainder.getCount(), stack.getMaxStackSize() - stack.getCount());
                stack.grow(moved);
                remainder.shrink(moved);
                ivyInventory.setChanged();
                if (remainder.isEmpty()) {
                    return;
                }
            }
        }
        spawnAtLocation(remainder);
    }

    void lockBoxingBodyToYaw(float yaw, float turnSpeed) {
        if (bodyControl != null) {
            bodyControl.lockBodyToYaw(yaw, turnSpeed);
        }
    }

    boolean isBoxingCombatActive() {
        return getBoxingCombat().isActive();
    }

    boolean isDownedOrArising() {
        return isDowned() || getDownedAriseTicks() > 0;
    }

    boolean isCompanionAiBlocked() {
        return isDownedOrArising()
                || isTrading()
                || isInDialogue()
                || getTradeAnimState() != TradeAnimState.NONE
                || isIdleVariantActive()
                || getBoxingCombat().isActive()
                || isBoxingRecovering();
    }

    boolean isClimbingLadder() {
        return this.entityData.get(DATA_CLIMBING_LADDER);
    }

    void setClimbingLadder(boolean climbing) {
        this.entityData.set(DATA_CLIMBING_LADDER, climbing);
    }

    @Override
    public void addAdditionalSaveData(@NotNull CompoundTag tag) {
        super.addAdditionalSaveData(tag);
        tag.putBoolean(TAME_TAG, isTame());
        tag.put(IVY_INVENTORY_TAG, saveIvyInventory());
        UUID ownerUuid = getOwnerUUID();
        if (ownerUuid != null) {
            tag.putUUID(OWNER_UUID_TAG, ownerUuid);
        }
        tag.putInt(COMMAND_TAG, getCommand());
        tag.putBoolean(DOWNED_TAG, isDowned());
        tag.putInt(DOWNED_BLEED_TICKS_TAG, Math.max(0, downedBleedTicks));
        tag.putInt(DOWNED_FINISH_HITS_TAG, Math.max(0, downedFinishHits));
        tag.putInt(DOWNED_REVIVE_PROGRESS_TAG, Math.max(0, downedReviveProgress));
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
        if (tag.contains(IVY_INVENTORY_TAG, Tag.TAG_LIST)) {
            loadIvyInventory(tag.getList(IVY_INVENTORY_TAG, Tag.TAG_COMPOUND));
            sanitizeIvyInventoryAfterLoad();
        }
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
        setDowned(tag.getBoolean(DOWNED_TAG));
        downedBleedTicks = tag.contains(DOWNED_BLEED_TICKS_TAG, Tag.TAG_INT)
                ? Math.max(0, tag.getInt(DOWNED_BLEED_TICKS_TAG))
                : DOWNED_BLEED_OUT_TICKS;
        downedFinishHits = tag.contains(DOWNED_FINISH_HITS_TAG, Tag.TAG_INT)
                ? Math.max(0, tag.getInt(DOWNED_FINISH_HITS_TAG))
                : 0;
        downedReviveProgress = tag.contains(DOWNED_REVIVE_PROGRESS_TAG, Tag.TAG_INT)
                ? Math.max(0, tag.getInt(DOWNED_REVIVE_PROGRESS_TAG))
                : 0;
        downedReviveInteractionGraceTicks = 0;
        setDownedAriseTicks(0);
        finishingDownedDeath = false;
        if (isDowned()) {
            setHealth(Math.max(1.0F, getHealth()));
            setNoAi(true);
            setDeathAnimation(0);
        }
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

    private ListTag saveIvyInventory() {
        ListTag items = new ListTag();
        for (int slot = 0; slot < IvyInventoryMenu.IVY_SLOT_COUNT; slot++) {
            ItemStack stack = ivyInventory.getItem(slot);
            if (stack.isEmpty()) {
                continue;
            }
            CompoundTag itemTag = stack.save(new CompoundTag());
            itemTag.putByte(IVY_INVENTORY_SLOT_TAG, (byte) slot);
            items.add(itemTag);
        }
        return items;
    }

    private void loadIvyInventory(ListTag items) {
        for (int slot = 0; slot < IvyInventoryMenu.IVY_SLOT_COUNT; slot++) {
            ivyInventory.setItem(slot, ItemStack.EMPTY);
        }
        for (int index = 0; index < items.size(); index++) {
            CompoundTag itemTag = items.getCompound(index);
            int slot = itemTag.contains(IVY_INVENTORY_SLOT_TAG, Tag.TAG_BYTE)
                    ? Byte.toUnsignedInt(itemTag.getByte(IVY_INVENTORY_SLOT_TAG))
                    : index;
            if (slot >= 0 && slot < IvyInventoryMenu.IVY_SLOT_COUNT) {
                ivyInventory.setItem(slot, ItemStack.of(itemTag));
            }
        }
        ivyInventory.setChanged();
    }

    private void sanitizeIvyInventoryAfterLoad() {
        moveInvalidEquipmentSlotToStorage(IvyInventoryMenu.HELMET_SLOT);
        moveInvalidEquipmentSlotToStorage(IvyInventoryMenu.CHESTPLATE_SLOT);
        moveInvalidEquipmentSlotToStorage(IvyInventoryMenu.LEGGINGS_SLOT);
        moveInvalidEquipmentSlotToStorage(IvyInventoryMenu.BOOTS_SLOT);
        moveInvalidEquipmentSlotToStorage(IvyInventoryMenu.SWORD_SLOT);
        ivyInventory.setChanged();
    }

    private void moveInvalidEquipmentSlotToStorage(int slot) {
        ItemStack stack = ivyInventory.getItem(slot);
        if (stack.isEmpty() || canStayInEquipmentSlot(slot, stack)) {
            return;
        }
        ivyInventory.setItem(slot, ItemStack.EMPTY);
        addInventoryRemainderOrDrop(stack);
    }

    private boolean canStayInEquipmentSlot(int slot, ItemStack stack) {
        return switch (slot) {
            case IvyInventoryMenu.HELMET_SLOT -> isArmorOfType(stack, ArmorItem.Type.HELMET);
            case IvyInventoryMenu.CHESTPLATE_SLOT -> isArmorOfType(stack, ArmorItem.Type.CHESTPLATE);
            case IvyInventoryMenu.LEGGINGS_SLOT -> isArmorOfType(stack, ArmorItem.Type.LEGGINGS);
            case IvyInventoryMenu.BOOTS_SLOT -> isArmorOfType(stack, ArmorItem.Type.BOOTS);
            case IvyInventoryMenu.SWORD_SLOT -> isWeaponStack(stack);
            default -> true;
        };
    }

    private static boolean isWeaponStack(ItemStack stack) {
        return !stack.isEmpty()
                && !stack.getAttributeModifiers(EquipmentSlot.MAINHAND).get(Attributes.ATTACK_DAMAGE).isEmpty();
    }

    private boolean isArmorOfType(ItemStack stack, ArmorItem.Type type) {
        return stack.getItem() instanceof ArmorItem armor && armor.getType() == type;
    }

    private void tickWaterCombatBlock() {
        if (!isCombatBlockedByWater()) {
            return;
        }
        boolean hadTarget = getTarget() != null;
        if (hadTarget) {
            super.setTarget(null);
        }
        if (hadTarget || getBoxingCombat().isActive()) {
            getBoxingCombat().clear();
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
                clearChatter();
            }
            return;
        }
        if (!getIdleChatterText().isEmpty()) {
            clearChatter();
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

    private void tickCombatChatter() {
        LivingEntity target = getTarget();
        if (target == null || !target.isAlive() || target.isRemoved()) {
            lastCombatChatterTargetId = -1;
            return;
        }
        if (lastCombatChatterTargetId == target.getId()) {
            return;
        }
        lastCombatChatterTargetId = target.getId();
        if (!canCombatChatter()) {
            return;
        }
        speakCombatChatter();
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

    private boolean canCombatChatter() {
        return isAlive()
                && !isTrading()
                && !isInDialogue()
                && getTradeAnimState() == TradeAnimState.NONE;
    }

    private void speakIdleChatter() {
        setChatter(IvyChatterRegistry.IDLE, getOwner());
        idleChatterTicks = IDLE_CHATTER_DURATION;
        idleChatterCooldown = nextIdleChatterCooldown();
    }

    private void speakCombatChatter() {
        setChatter(IvyChatterRegistry.COMBAT, getOwner());
        idleChatterTicks = IDLE_CHATTER_DURATION;
    }

    private void speakDownedChatter() {
        setChatter(IvyChatterRegistry.DOWNED, getOwner());
        idleChatterTicks = DOWNED_BLEED_OUT_TICKS;
    }

    private void speakOwnerHurtChatter(Player player) {
        setChatter(IvyChatterRegistry.OWNER_HURT, player);
        idleChatterTicks = IDLE_CHATTER_DURATION;
        idleChatterCooldown = Math.max(idleChatterCooldown, 120);
    }

    private void speakUnderwaterChatter(Player player) {
        setChatter(IvyChatterRegistry.UNDERWATER, player);
        idleChatterTicks = IDLE_CHATTER_DURATION;
        idleChatterCooldown = Math.max(idleChatterCooldown, 80);
    }

    public void refuseRenameAttempt() {
        setChatter(IvyChatterRegistry.RENAME_REFUSAL, getOwner());
        idleChatterTicks = IDLE_CHATTER_DURATION;
        idleChatterCooldown = Math.max(idleChatterCooldown, 120);
    }

    private void setChatter(String pool, @Nullable LivingEntity nameSource) {
        List<String> lines = IvyChatterRegistry.get(pool);
        if (lines.isEmpty()) {
            clearChatter();
            return;
        }
        this.entityData.set(DATA_IDLE_CHATTER, lines.get(random.nextInt(lines.size())));
        this.entityData.set(DATA_IDLE_CHATTER_NAME, resolveChatterName(nameSource));
    }

    private String resolveChatterName(@Nullable LivingEntity entity) {
        if (entity instanceof Player player) {
            String rememberedName = getRememberedDialogueName(player);
            if (rememberedName != null && !rememberedName.isBlank()) {
                return rememberedName;
            }
            return player.getName().getString();
        }
        LivingEntity owner = getOwner();
        if (owner instanceof Player player) {
            String rememberedName = getRememberedDialogueName(player);
            if (rememberedName != null && !rememberedName.isBlank()) {
                return rememberedName;
            }
            return player.getName().getString();
        }
        return "";
    }

    private void clearChatter() {
        this.entityData.set(DATA_IDLE_CHATTER, "");
        this.entityData.set(DATA_IDLE_CHATTER_NAME, "");
        idleChatterTicks = 0;
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

    public void openDialogueInventory(ServerPlayer player) {
        if (!isAlive()
                || !isTame()
                || !isOwnedBy(player)
                || player.distanceToSqr(this) > 64.0D) {
            return;
        }
        player.openMenu(new SimpleMenuProvider(
                (containerId, playerInventory, ignored) -> new IvyInventoryMenu(containerId, playerInventory, this),
                getDisplayName()
        ));
    }

    public Container getIvyInventory() {
        return ivyInventory;
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

    public void playDialogueAnimation(String trigger) {
        if (level().isClientSide || getBoxingCombat().isActive()) {
            return;
        }
        setIdleVariantActive(false);
        getNavigation().stop();
        holdingDialogueExpressionTrigger = isHoldingDialogueExpressionTrigger(trigger) ? trigger : null;
        triggerAnim("movement", trigger);
    }

    public void exitDialogueExpressionAnimation() {
        if (level().isClientSide || holdingDialogueExpressionTrigger == null) {
            return;
        }
        String exitTrigger = dialogueExpressionExitTrigger(holdingDialogueExpressionTrigger);
        holdingDialogueExpressionTrigger = null;
        getNavigation().stop();
        if (exitTrigger != null && !getBoxingCombat().isActive()) {
            triggerAnim("movement", exitTrigger);
        }
    }

    private static boolean isHoldingDialogueExpressionTrigger(String trigger) {
        return "hmm_trader".equals(trigger)
                || "hmm_gardener".equals(trigger)
                || "hmm_dragon_advice".equals(trigger);
    }

    @Nullable
    private static String dialogueExpressionExitTrigger(String trigger) {
        return switch (trigger) {
            case "hmm_trader" -> "hmm_trader_exit_to_idle";
            case "hmm_gardener" -> "hmm_gardener_exit_to_idle";
            case "hmm_dragon_advice" -> "hmm_dragon_advice_exit_to_idle";
            default -> null;
        };
    }

    private void setupMovementController(AnimationController<IvyTheDragonMerchant> controller) {
        controller.triggerableAnim("trade_start", TRADE_START);
        controller.triggerableAnim("trading", TRADING);
        controller.triggerableAnim("trade_stop", TRADE_STOP);
        controller.triggerableAnim("greetings",
                RawAnimation.begin().thenPlay("ivy_oleander.animation.greetings"));
        controller.triggerableAnim("reaction_to_egg",
                RawAnimation.begin().thenPlay("ivy_oleander.animation.reaction_to_egg"));
        controller.triggerableAnim("embarrassed", EMBARRASSED);
        controller.triggerableAnim("sigh", SIGH);
        controller.triggerableAnim("hmm_trader", HMM_TRADER);
        controller.triggerableAnim("hmm_gardener", HMM_GARDENER);
        controller.triggerableAnim("hmm_dragon_advice", HMM_DRAGON_ADVICE);
        controller.triggerableAnim("hmm_trader_exit_to_idle", HMM_TRADER_EXIT_TO_IDLE);
        controller.triggerableAnim("hmm_gardener_exit_to_idle", HMM_GARDENER_EXIT_TO_IDLE);
        controller.triggerableAnim("hmm_dragon_advice_exit_to_idle", HMM_DRAGON_ADVICE_EXIT_TO_IDLE);
    }

    private void setupPassiveUseController(AnimationController<IvyTheDragonMerchant> controller) {
        controller.triggerableAnim("main_hand_eat", MAIN_HAND_EAT);
        controller.triggerableAnim("main_hand_drink", MAIN_HAND_DRINK);
        controller.triggerableAnim("main_hand_interact", MAIN_HAND_INTERACT);
    }

    private void setupInstantController(AnimationController<IvyTheDragonMerchant> controller) {
        controller.triggerableAnim("actually_die", ACTUALLY_DIE);
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
        return !isDowned()
                && getDownedAriseTicks() <= 0
                && !isTrading()
                && !isInDialogue()
                && getTradeAnimState() == TradeAnimState.NONE
                && !isIdleVariantActive()
                && !getBoxingCombat().isActive();
    }

    void cancelPassiveAnimationsForCombat() {
        setIdleVariantActive(false);
        cancelPassiveUse();
    }

    boolean isReadyForCombatAnimation() {
        return !isDowned()
                && getDownedAriseTicks() <= 0
                && getTradeAnimState() == TradeAnimState.NONE;
    }

    boolean isInDialogue() {
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

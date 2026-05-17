package com.leon.saintsdragons.server.entity.dragons.stegonaut;

import com.leon.saintsdragons.server.ai.goals.base.*;
import com.leon.saintsdragons.server.ai.goals.stegonaut.*;
import com.leon.saintsdragons.server.ai.navigation.DragonPathNavigateGround;
import com.leon.saintsdragons.server.entity.ability.abilities.stegonaut.StegonautBuffAbility;
import com.leon.saintsdragons.server.entity.ability.abilities.stegonaut.StegonautGroundEatingAbility;
import com.leon.saintsdragons.server.entity.base.RideableGroundDragon;
import com.leon.saintsdragons.server.entity.base.DragonEntity;
import com.leon.saintsdragons.server.entity.base.DragonGender;
import com.leon.saintsdragons.server.entity.base.DragonSitTransitionController;
import com.leon.saintsdragons.server.entity.dragons.handlers.DragonVocalAnimationHelper;
import com.leon.saintsdragons.server.entity.dragons.stegonaut.handlers.StegonautAnimationHandler;
import com.leon.saintsdragons.server.entity.dragons.stegonaut.handlers.StegonautInteractionHandler;
import com.leon.saintsdragons.server.entity.dragons.stegonaut.handlers.StegonautSoundProfile;
import com.leon.saintsdragons.server.entity.dragons.handlers.DragonInteractionAnimationHelper;
import com.leon.saintsdragons.server.entity.dragons.handlers.DragonStateAnimationHelper;
import com.leon.saintsdragons.server.entity.controller.stegonaut.StegonautRiderController;
import com.leon.saintsdragons.server.entity.interfaces.DragonSoundProfile;
import com.leon.saintsdragons.server.entity.interfaces.PackMember;
import com.leon.saintsdragons.server.menu.StegonautInventoryMenu;
import com.leon.saintsdragons.common.network.DragonRiderAction;
import com.leon.saintsdragons.common.registry.ModTags;
import com.leon.saintsdragons.server.world.DragonSpawnRules;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.FloatGoal;
import net.minecraft.world.entity.ai.goal.LookAtPlayerGoal;
import net.minecraft.world.entity.ai.goal.RandomLookAroundGoal;
import net.minecraft.world.entity.ai.goal.target.NearestAttackableTargetGoal;
import net.minecraft.world.entity.ai.goal.target.HurtByTargetGoal;
import net.minecraft.world.entity.ai.navigation.PathNavigation;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.core.BlockPos;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.phys.Vec3;
import net.minecraft.server.level.ServerLevel;
import java.util.Map;
import java.util.UUID;
import java.util.function.Supplier;
import com.leon.saintsdragons.server.entity.ability.DragonAbilityType;
import com.leon.saintsdragons.common.registry.ModEntities;
import com.leon.saintsdragons.common.registry.ModBlocks;
import com.leon.saintsdragons.common.registry.ModItems;
import com.leon.saintsdragons.common.registry.ModSounds;
import com.leon.saintsdragons.common.registry.ModAbilities;
import com.leon.saintsdragons.common.config.dragon.DragonAttributeConfig;
import com.leon.saintsdragons.common.config.dragon.DragonAttributeConfigLoader;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.SimpleMenuProvider;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.Container;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import javax.annotation.Nonnull;
import software.bernie.geckolib.core.animatable.instance.AnimatableInstanceCache;
import software.bernie.geckolib.core.animation.AnimatableManager;
import software.bernie.geckolib.core.animation.AnimationController;
import software.bernie.geckolib.util.GeckoLibUtil;

public class Stegonaut extends RideableGroundDragon implements PackMember<Stegonaut> {
    @Override
    protected ResourceLocation getDragonAttributesId() {
        return DragonAttributeConfigLoader.STEGONAUT_ID;
    }

    private static final EntityDataAccessor<Boolean> DATA_HAS_CHEST =
            SynchedEntityData.defineId(Stegonaut.class, EntityDataSerializers.BOOLEAN);
    private static final EntityDataAccessor<Integer> DATA_FEEDING_COOLDOWN =
            SynchedEntityData.defineId(Stegonaut.class, EntityDataSerializers.INT);
    private static final int MIN_AMBIENT_DELAY = 200;
    private static final int MAX_AMBIENT_DELAY = 600;
    private static final double BREED_PARTNER_RANGE = 20.0D;
    private static final double BREED_DISTANCE_SQR = 2500.0D;
    private static final double BABY_MAX_HEALTH = 50.0D;
    private static final double BABY_ARMOR = 5.0D;
    private static final float BABY_HITBOX_SCALE = 0.65F;
    private static final double RIDER_JUMP_STRENGTH = 0.75D;
    private static final double RIDER_JUMP_FORWARD_BOOST = 0.7D;
    private static final int STEGONAUT_CHEST_SLOTS = 15;
    public static final double RIDER_WALK_SPEED = 0.1D;
    public static final double RIDER_RUN_SPEED = 0.25D;
    private static final int MAX_PACK_SIZE = 6;
    private static final double PACK_SEARCH_RADIUS = 48.0D;
    private static final Map<String, VocalEntry> VOCAL_ENTRIES = new VocalEntryBuilder()
            .add("grumble1", DragonVocalAnimationHelper.CONTROLLER, "animation.stegonaut.grumble1", ModSounds.STEGONAUT_GRUMBLE_1, 0.6f, 1.1f, 0.2f, false, false, true)
            .add("grumble2", DragonVocalAnimationHelper.CONTROLLER, "animation.stegonaut.grumble2", ModSounds.STEGONAUT_GRUMBLE_2, 0.6f, 1.1f, 0.2f, false, false, true)
            .add("grumble3", DragonVocalAnimationHelper.CONTROLLER, "animation.stegonaut.grumble3", ModSounds.STEGONAUT_GRUMBLE_3, 0.6f, 1.1f, 0.2f, false, false, true)
            .add("stegonaut_hurt", DragonInteractionAnimationHelper.CONTROLLER, "animation.stegonaut.hurt", ModSounds.STEGONAUT_HURT, 1.0f, 0.95f, 0.1f, false, true, true)
            .add("stegonaut_die", DragonInteractionAnimationHelper.CONTROLLER, "animation.stegonaut.die", ModSounds.STEGONAUT_DIE, 1.2f, 1.0f, 0.0f, false, true, true)
            .build();
    private boolean suppressSitAnimation = false;
    private boolean boundToBinder = false;
    @Nullable
    private UUID packLeaderUuid;
    private int walkAnimationHoldTicks = 0;
    private int groundStepSoundCooldownTicks = 0;
    private final DragonSitTransitionController sitTransitions = new DragonSitTransitionController(this);
    public AnimatableInstanceCache dragonCache = GeckoLibUtil.createInstanceCache(this);
    private final StegonautAnimationHandler animationController = new StegonautAnimationHandler(this);
    private final StegonautInteractionHandler interactionHandler = new StegonautInteractionHandler(this);
    private final StegonautRiderController riderController = new StegonautRiderController(this);
    private final SimpleContainer stegonautChestInventory = new SimpleContainer(STEGONAUT_CHEST_SLOTS);
    private final StegonautBuffAbility buffAbility = new StegonautBuffAbility(this);

    public Stegonaut(EntityType<? extends Stegonaut> entityType, Level level) {
        super(entityType, level);
        seedAmbientSoundTimer(MIN_AMBIENT_DELAY, MAX_AMBIENT_DELAY, 80);
        if (!level.isClientSide) {
            applyConfiguredAttributes();
            this.setHealth(this.getMaxHealth());
        }
    }
    @Override
    protected void defineSynchedData() {
        super.defineSynchedData();
    }

    @Override
    protected void defineRideableDragonData() {
        this.entityData.define(DATA_HAS_CHEST, false);
        this.entityData.define(DATA_FEEDING_COOLDOWN, 0);
    }
    @Override
    public Map<String, VocalEntry> getVocalEntries() {
        return VOCAL_ENTRIES;
    }
    @Override
    public DragonSoundProfile getSoundProfile() {
        return StegonautSoundProfile.INSTANCE;
    }

    @Override
    protected void registerGoals() {
        this.goalSelector.addGoal(0, new FloatGoal(this));
        this.goalSelector.addGoal(2, new DragonFollowParentGoal<>(this, Stegonaut.class, 0.70D));
        this.goalSelector.addGoal(3, new DragonBreedGoal<>(this, 1.0D, Stegonaut.class, BREED_PARTNER_RANGE, BREED_DISTANCE_SQR));
        this.goalSelector.addGoal(4, new StegonautCombatGoal(this));
        this.goalSelector.addGoal(5, new DragonGroundFollowOwnerGoal<>(this, DragonGroundFollowOwnerGoal.FollowConfig.forStegonaut()));
        this.goalSelector.addGoal(6, new DragonPackFollowLeaderGoal<>(this, Stegonaut.class, 0.75D, 16.0D, 8.0D));
        this.goalSelector.addGoal(7, new DirectSwimWanderGoal(this, 8.0F, 0.12D, 1, true));
        this.goalSelector.addGoal(7, new StegonautGroundWanderGoal(this, 0.35D, 120));
        this.goalSelector.addGoal(8, new LookAtPlayerGoal(this, Player.class, 8.0F));
        this.goalSelector.addGoal(9, new RandomLookAroundGoal(this));
        this.targetSelector.addGoal(1, new DragonProtectBabiesGoal<>(this, Stegonaut.class));
        this.targetSelector.addGoal(2, new DragonOwnerHurtByTargetGoal(this));
        this.targetSelector.addGoal(3, new DragonOwnerHurtTargetGoal(this));
        this.targetSelector.addGoal(4, new DragonPackDefendPackGoal<>(this, Stegonaut.class, 36.0D));
        this.targetSelector.addGoal(5, new HurtByTargetGoal(this));
        this.targetSelector.addGoal(6, new NearestAttackableTargetGoal<>(this, Player.class, 10, true, false, target -> isAggressiveWild()));
        this.targetSelector.addGoal(7, new DragonRandomHuntTargetGoal(this, 80, () -> true,
                target -> DragonTargetingHelper.isTaggedHuntTarget(target, ModTags.EntityTypes.STEGONAUT_TARGETS)));
    }

    @Override
    protected @NotNull PathNavigation createNavigation(@NotNull Level level) {
        return new DragonPathNavigateGround(this, level);
    }

    public static AttributeSupplier.Builder createAttributes() {
        return Mob.createMobAttributes()
                .add(Attributes.MAX_HEALTH, 100.0D)
                .add(Attributes.KNOCKBACK_RESISTANCE, 1.0D)
                .add(Attributes.MOVEMENT_SPEED, 0.50D)
                .add(Attributes.ATTACK_DAMAGE, 2.0D)
                .add(Attributes.ARMOR, 15.0D)
                .add(Attributes.FOLLOW_RANGE, 32.0D);
    }

    @Override
    protected boolean supportsRiderAction(DragonRiderAction action) {
        return switch (action) {
            case ABILITY_USE, ABILITY_STOP, OPEN_INVENTORY -> true;
            default -> super.supportsRiderAction(action);
        };
    }

    @Override
    protected boolean tryReleaseHeldRidingAbility(String abilityName) {
        if (ModAbilities.STEGONAUT_GROUND_EATING.getName().equals(abilityName)) {
            var active = combatManager.getActiveAbility();
            if (active != null && active.getAbilityType() == ModAbilities.STEGONAUT_GROUND_EATING) {
                ((StegonautGroundEatingAbility) active).requestRelease();
                return true;
            }
        }
        return false;
    }

    @Override
    protected void onRiderOpenInventory(Player player) {
        if (player instanceof ServerPlayer serverPlayer) {
            openStegonautInventory(serverPlayer);
        }
    }

    @Override
    protected boolean canGroundDragonJump() {
        return !isOrderedToSit() && getActiveAbility() == null;
    }

    @Override
    protected void onGroundDragonJumped(int jumpPower) {
        super.onGroundDragonJumped(jumpPower);
    }

    @Override
    protected double getRiderJumpStrength() {
        return RIDER_JUMP_STRENGTH;
    }

    @Override
    protected double getRiderJumpForwardBoost() {
        return RIDER_JUMP_FORWARD_BOOST;
    }

    @Override
    protected int getRiderGroundJumpAnimationFallHoldTicks() {
        return 3;
    }

    @Override
    public boolean hasSecondaryMelee() {
        return true;
    }

    @Override
    public boolean isFood(@Nonnull ItemStack stack) {
        return stack.is(ModTags.Items.STEGONAUT_FOODS);
    }

    public boolean canFeed() {
        return this.entityData.get(DATA_FEEDING_COOLDOWN) <= 0;
    }

    public void setFeedingCooldown(int ticks) {
        this.entityData.set(DATA_FEEDING_COOLDOWN, Math.max(0, ticks));
    }

    public Vec3 getGroundEatingProjectileOrigin() {
        return this.position().add(0, this.getBbHeight() * 0.8, 0);
    }

    @Override
    public DragonAbilityType<?, ?> getPrimaryAttackAbility() {
        return getMeleeMode() == 0
                ? ModAbilities.STEGONAUT_BITE
                : ModAbilities.STEGONAUT_CHIN_SLAM;
    }

    public DragonAbilityType<?, ?> getRandomAiAttackAbility() {
        return this.getRandom().nextBoolean()
                ? ModAbilities.STEGONAUT_BITE
                : ModAbilities.STEGONAUT_CHIN_SLAM;
    }

    @Override
    public RiderAbilityBinding getAttackRiderAbility() {
        String abilityId = getMeleeMode() == 0
                ? ModAbilities.STEGONAUT_BITE.getName()
                : ModAbilities.STEGONAUT_CHIN_SLAM.getName();
        return new RiderAbilityBinding(abilityId, RiderAbilityBinding.Activation.PRESS);
    }

    @Override
    public RiderAbilityBinding getTertiaryRiderAbility() {
        return new RiderAbilityBinding(ModAbilities.STEGONAUT_GROUND_EATING.getName(), RiderAbilityBinding.Activation.HOLD);
    }

    @Override
    protected boolean isRidingAbilityAllowed(DragonAbilityType<?, ?> abilityType) {
        return abilityType == ModAbilities.STEGONAUT_BITE
                || abilityType == ModAbilities.STEGONAUT_CHIN_SLAM
                || abilityType == ModAbilities.STEGONAUT_GROUND_EATING;
    }

    @Override
    protected DragonAbilityType<?, ?> getHurtAbilityType() {
        return ModAbilities.STEGONAUT_HURT;
    }

    @Override
    public int getDeathAnimationDurationTicks() {
        return 43;
    }

    @Override
    protected void dropAdditionalDeathLootAfterBase(@NotNull DamageSource source) {
        DragonAttributeConfig config = DragonAttributeConfigLoader.getInstance()
                .getConfig(DragonAttributeConfigLoader.STEGONAUT_ID);
        double eggDropChance = config.extraDouble("egg_drop_chance", 0.12D);
        if (!level().isClientSide && getGender() == DragonGender.FEMALE && this.random.nextDouble() < eggDropChance) {
            this.spawnAtLocation(ModItems.STEGONAUT_EGG.get());
        }
    }

    @Override
    public boolean hurt(@NotNull DamageSource source, float amount) {
        if (isDying()) {
            return false;
        }
        if (!isWildStegonautDamageAllowed(source)) {
            return false;
        }
        if (source.is(DamageTypeTags.IS_FALL)) {
            amount *= 0.3F;
        }

        return super.hurt(source, amount);
    }

    private boolean isWildStegonautDamageAllowed(@NotNull DamageSource source) {
        Entity attacker = source.getEntity();
        if (!(attacker instanceof Stegonaut other)) {
            return true;
        }
        if (this.isTame() || other.isTame()) {
            return true;
        }
        if (this.isBaby() || other.isBaby()) {
            return false;
        }
        return false;
    }

    @Override
    protected double getCullingInflateX() {
        return 6.0D;
    }

    @Override
    protected double getCullingInflateY() {
        return 3.0D;
    }

    @Override
    protected double getCullingInflateZ() {
        return 6.0D;
    }

    @Override
    protected DragonAbilityType<?, ?> getDeathAbilityType() {
        return ModAbilities.STEGONAUT_DIE;
    }

    @Override
    public void registerControllers(AnimatableManager.ControllerRegistrar controllers) {
        AnimationController<Stegonaut> movementController = new AnimationController<>(this, "movement", 1, animationController::handleMovementAnimation);
        movementController.setSoundKeyframeHandler(event -> handleAnimationSound(event.getKeyframeData().getSound()));
        controllers.add(movementController);
        AnimationController<Stegonaut> actionController = new AnimationController<>(this, StegonautAnimationHandler.ACTION_CONTROLLER, 5, animationController::actionPredicate);
        animationController.setupActionController(actionController);
        actionController.setSoundKeyframeHandler(event -> handleAnimationSound(event.getKeyframeData().getSound()));
        controllers.add(actionController);
        AnimationController<Stegonaut> fastActionController = new AnimationController<>(this, StegonautAnimationHandler.FAST_ACTION_CONTROLLER, 1, animationController::fastActionPredicate);
        animationController.setupFastActionController(fastActionController);
        fastActionController.setSoundKeyframeHandler(event -> handleAnimationSound(event.getKeyframeData().getSound()));
        controllers.add(fastActionController);
        AnimationController<Stegonaut> vocalController = new AnimationController<>(this, DragonVocalAnimationHelper.CONTROLLER, 2, DragonVocalAnimationHelper::idle);
        DragonVocalAnimationHelper.registerGrumbles(vocalController, this);
        vocalController.setSoundKeyframeHandler(event -> handleAnimationSound(event.getKeyframeData().getSound()));
        controllers.add(vocalController);
        AnimationController<Stegonaut> interactionController = new AnimationController<>(this, DragonInteractionAnimationHelper.CONTROLLER, 1, DragonInteractionAnimationHelper::idle);
        animationController.setupInteractionController(interactionController);
        interactionController.setSoundKeyframeHandler(event -> handleAnimationSound(event.getKeyframeData().getSound()));
        controllers.add(interactionController);
        AnimationController<Stegonaut> stateController = new AnimationController<>(this, DragonStateAnimationHelper.CONTROLLER, 1, DragonStateAnimationHelper::idle);
        animationController.setupStateController(stateController);
        stateController.setSoundKeyframeHandler(event -> handleAnimationSound(event.getKeyframeData().getSound()));
        controllers.add(stateController);
    }

    @Override
    public AnimatableInstanceCache getAnimatableInstanceCache() {
        return this.dragonCache;
    }

    @Override
    public AgeableMob getBreedOffspring(@Nonnull ServerLevel level, @Nonnull AgeableMob other) {
        return createBreedOffspring(level, other, ModEntities.STEGONAUT.get(), Stegonaut::applyConfiguredAttributes);
    }

    public void applyConfiguredAttributes() {
        DragonAttributeConfig config = getConfiguredDragonAttributes();
        applyConfiguredHealthAndArmor(config, BABY_MAX_HEALTH, BABY_ARMOR);
        clampHealthToMax();
    }

    @Override
    public void ageBoundaryReached() {
        super.ageBoundaryReached();
        applyConfiguredAttributes();
        refreshDimensions();
    }

    @Override
    protected float getBabyHitboxScale() {
        return BABY_HITBOX_SCALE;
    }

    @Override
    public boolean canBreed() {
        return this.isTame() && !this.isBaby() && this.getHealth() >= this.getMaxHealth() && this.isInLove();
    }

    @Override
    protected Supplier<? extends Block> getEggBlock() {
        return ModBlocks.STEGONAUT_EGG;
    }

    public static boolean canSpawnHere(EntityType<? extends Stegonaut> type,
                                       LevelAccessor level,
                                       MobSpawnType spawnType,
                                       BlockPos pos,
                                       RandomSource random) {
        return DragonSpawnRules.hasCaveGroundSpawnSpace(level, pos) && DragonSpawnRules.passesNearbyDragonDensityCheck(level, spawnType, pos, Stegonaut.class);
    }
    @Override
    public @NotNull InteractionResult mobInteract(@NotNull Player player, @NotNull InteractionHand hand) {
        InteractionResult handlerResult = interactionHandler.handleInteraction(player, hand);
        if (handlerResult != InteractionResult.PASS) {
            return handlerResult;
        }
        return super.mobInteract(player, hand);
    }

    private void dropStegonautChestContents() {
        for (int slot = 0; slot < stegonautChestInventory.getContainerSize(); slot++) {
            ItemStack stack = stegonautChestInventory.getItem(slot);
            if (!stack.isEmpty()) {
                this.spawnAtLocation(stack.copy());
                stegonautChestInventory.setItem(slot, ItemStack.EMPTY);
            }
        }
    }

    public void removeStegonautChestAndDropContents() {
        if (this.level().isClientSide || !hasStegonautChest()) {
            return;
        }
        dropStegonautChestContents();
        setStegonautChest(false);
        this.playSound(SoundEvents.DONKEY_CHEST, 1.0F, 1.0F);
    }

    @Override
    protected void clearSittingForMounting() {
        suppressSitAnimation = true;
        setOrderedToSit(false);
        suppressSitAnimation = false;
        sitTransitions.clear();
    }

    private int grumbleCooldown = 0;
    private String selectAmbientGrumble() {
        if (isDying() || getTarget() != null || isBaby()) {
            return null;
        }

        return selectWeightedAmbientVocal("grumble1", 0.4f, "grumble2", 0.7f, "grumble3");
    }
    private void handleAmbientSounds() {
        if (isDying() || isSleeping() || isSleepTransitioning()) {
            return;
        }

        tickAmbientVocalSounds(MIN_AMBIENT_DELAY, MAX_AMBIENT_DELAY, this::selectAmbientGrumble);
    }

    @Override
    public void aiStep() {
        super.aiStep();
        if (!level().isClientSide) {
            if (this.isTame()) {
                this.packLeaderUuid = null;
            }
        }
        tickRiderControlLock();
        tickAnimationStates();
    }


    @Override
    public boolean supportsSleep() {
        return true;
    }

    @Override
    public boolean isSleepSuppressed() {
        return super.isSleepSuppressed() || getTarget() != null || isInWaterOrBubble() || isVehicle();
    }

    @Override
    protected boolean useSleepSitDownTimer() {
        return true;
    }

    @Override
    protected boolean requireSeatedBeforeFallAsleep() {
        return true;
    }

    @Override
    protected boolean sleepForceSitDownOnEnter() {
        return true;
    }

    @Override
    protected int getSleepExitSuppressionTicks() {
        return 0;
    }

    @Override
    protected int getSleepWakeUpSuppressionTicks() {
        return 0;
    }

    @Override
    protected boolean isAlreadySeatedForSleep() {
        return isOrderedToSit() || shouldStaySeatedCommand() || getSitProgress() >= maxSitTicks();
    }

    @Override
    protected boolean shouldStaySeatedAfterWake() {
        return shouldStaySeatedCommand();
    }

    @Override
    protected void onSleepLockCommand(int snapshot) {
    }

    @Override
    protected void onSleepUnlockCommand(int desired) {
    }

    @Override
    protected void onSleepFreezeTick() {
        super.onSleepFreezeTick();
        this.setOrderedToSit(true);
    }

    @Override
    protected void onSleepSitDownAnimation() {
        animationController.triggerSitDownAnimation();
        setOrderedToSit(true);
    }

    @Override
    protected void onSleepFallAsleepAnimation() {
        animationController.triggerFallAsleepAnimation();
    }

    @Override
    protected void onSleepLoopAnimation() {
        animationController.triggerSleepAnimation();
    }

    @Override
    protected void onSleepWakeUpAnimation() {
        animationController.triggerWakeUpAnimation();
        setOrderedToSit(true);
    }

    @Override
    protected void onSleepSitUpAnimation() {
        animationController.triggerSitUpAnimation();
        setOrderedToSit(false);
    }

    @Override
    protected void onSleepExitSeated() {
        setOrderedToSit(true);
        setSitProgress(Math.max(getSitProgress(), maxSitTicks()));
        setGroundMoveStateFromAI(0);
    }

    @Override
    protected void onSleepExitStarted() {
        setOrderedToSit(true);
    }

    @Override
    public DragonEntity.DragonSleepPreferences getSleepPreferences() {
        return DragonEntity.DragonSleepPreferences.NOCTURNAL();
    }

    @Override
    public boolean canSleepNow() {
        return !DragonEntity.DragonSleepPreferences.isNaturalDay(level());
    }

    @Override
    public float maxSitTicks() {
        return getSitDownAnimationTicks();
    }

    private int getSitDownAnimationTicks() {
        return 38;
    }

    private int getSitUpAnimationTicks() {
        return 38;
    }

    private int getFallAsleepAnimationTicks() {
        return 38;
    }

    private int getWakeUpAnimationTicks() {
        return 38;
    }

    @Override
    protected int getSleepSitDownDuration() {
        return getSitDownAnimationTicks();
    }

    @Override
    protected int getSleepSitUpDuration() {
        return getSitUpAnimationTicks();
    }

    @Override
    protected int getSleepFallAsleepDuration() {
        return getFallAsleepAnimationTicks();
    }

    @Override
    protected int getSleepWakeUpDuration() {
        return getWakeUpAnimationTicks();
    }

    @Override
    public void setOrderedToSit(boolean sitting) {
        boolean wasSitting = this.isOrderedToSit();
        super.setOrderedToSit(sitting);
        if (level().isClientSide) {
            return;
        }
        if (wasSitting == sitting) {
            return;
        }
        if (suppressSitAnimation) {
            if (!sitting) {
                sitTransitions.clear();
            }
            return;
        }
        if (isSleeping() || isSleepTransitioning()) {
            return;
        }
        setGroundMoveStateFromAI(0);
    }

    @Override
    public void setGroundMoveStateFromAI(int state) {
        super.setGroundMoveStateFromAI(state);
    }

    @Override
    public void tick() {
        super.tick();

        if (!level().isClientSide) {
            tickFeedingCooldown();
            handleAmbientSounds();
            tickGroundStepAudio();
        }

        if (this.isAlive()) {
            buffAbility.tick();
        } else {
            buffAbility.cleanup();
        }
        if (grumbleCooldown > 0) {
            grumbleCooldown--;
        }
        if (!isSleeping() && !isSleepTransitioning()) {
            sitTransitions.tick(
                    getSitDownAnimationTicks(),
                    getSitUpAnimationTicks(),
                    animationController::triggerSitDownAnimation,
                    animationController::triggerSitUpAnimation
            );
        }
    }

    private void tickFeedingCooldown() {
        int cooldownTicks = this.entityData.get(DATA_FEEDING_COOLDOWN);
        if (cooldownTicks > 0) {
            this.entityData.set(DATA_FEEDING_COOLDOWN, cooldownTicks - 1);
        }
    }

    @Override
    public boolean isInSitTransition() {
        return sitTransitions.isInTransition();
    }

    @Override
    public boolean isSittingDownAnimation() {
        return sitTransitions.isSittingDown();
    }

    @Override
    public boolean isStandingUpAnimation() {
        return sitTransitions.isStandingUp();
    }

    private boolean shouldStaySeatedCommand() {
        return this.isTame() && this.getCommand() == 1;
    }
    private void tickGroundStepAudio() {
        if (groundStepSoundCooldownTicks > 0) {
            groundStepSoundCooldownTicks--;
        }
        if (isBaby() || isSleeping() || isSleepTransitioning() || isOrderedToSit() || areRiderControlsLocked() || !onGround() || isInWaterOrBubble()) {
            groundStepSoundCooldownTicks = 0;
            return;
        }
        int moveState = this.entityData.get(DATA_GROUND_MOVE_STATE);
        if (moveState <= 0) {
            double speedSqr = this.getDeltaMovement().horizontalDistanceSqr();
            if (speedSqr > 0.0064D) {
                moveState = 2;
            } else if (speedSqr > 0.001D) {
                moveState = 1;
            }
        }
        if (moveState <= 0) {
            groundStepSoundCooldownTicks = 0;
            return;
        }
        if (groundStepSoundCooldownTicks > 0) {
            return;
        }
        boolean running = moveState == 2;
        int duration = running ? 27 : 40;
        getSoundHandler().playMovingEntitySound(
                running ? ModSounds.STEGONAUT_RUN.get() : ModSounds.STEGONAUT_WALK.get(),
                1.0f, isBaby() ? 1.6f : 1.0f, duration
        );
        groundStepSoundCooldownTicks = duration;
    }

    public void playEatMovingSound() {
        if (!level().isClientSide) {
            getSoundHandler().playMovingEntitySound(ModSounds.STEGONAUT_EAT.get(), 1.0f, isBaby() ? 1.6f : 1.0f, 22);
        }
    }
    @Override
    public void setRunning(boolean running) {
    }

    @Override
    public void tickAnimationStates() {
        if (this.isVehicle() && this.isOrderedToSit() && getSitProgress() <= 0.01f) {
            if (this.level().isClientSide) {
                this.setOrderedToSit(false);
                forceSitProgress(0f);
            }
        }
        if (isSleeping() || isOrderedToSit()) {
            if (!this.level().isClientSide) {
                this.entityData.set(DATA_GROUND_MOVE_STATE, 0);
                this.syncAnimState(0, getFlightMode());
            }
            walkAnimationHoldTicks = 0;
            return;
        }

        if (getControllingPassenger() != null) {
            super.tickAnimationStates();
            setRunning(isAccelerating());
            return;
        }

        int moveState = 0;

        boolean hasActivePath = this.getNavigation().isInProgress();
        double horizontalSpeed = this.getDeltaMovement().horizontalDistanceSqr();
        boolean isActuallyMoving = horizontalSpeed > 0.001;

        if (hasActivePath || isActuallyMoving) {
            if (horizontalSpeed > 0.0064) {
                moveState = 2;
            } else {
                moveState = 1;
            }
            walkAnimationHoldTicks = 8;
        } else if (walkAnimationHoldTicks > 0) {
            moveState = 1;
            walkAnimationHoldTicks--;
        }
        if (this.entityData.get(DATA_GROUND_MOVE_STATE) != moveState) {
            this.entityData.set(DATA_GROUND_MOVE_STATE, moveState);
            this.syncAnimState(moveState, getFlightMode());
        }
        setRunning(moveState == 2 && !this.isInLove());

    }

    public boolean hasStegonautChest() {
        return this.entityData.get(DATA_HAS_CHEST);
    }

    public void setStegonautChest(boolean value) {
        this.entityData.set(DATA_HAS_CHEST, value);
        if (!value) {
            stegonautChestInventory.clearContent();
        }
    }

    public Container getStegonautChestInventory() {
        return stegonautChestInventory;
    }

    private void openStegonautInventory(ServerPlayer player) {
        if (!this.isAlive() || player.distanceToSqr(this) > 64.0D) {
            return;
        }
        player.openMenu(new SimpleMenuProvider(
                (containerId, playerInventory, ignored) -> new StegonautInventoryMenu(containerId, playerInventory, this),
                this.getDisplayName()
        ));
    }

    @Override
    public void addAdditionalSaveData(@NotNull CompoundTag tag) {
        super.addAdditionalSaveData(tag);
        tag.putInt("StegonautCommand", this.getCommand());
        tag.putBoolean("StegonautOrderedSit", this.isOrderedToSit());
        tag.putInt("GrumbleCooldown", grumbleCooldown);
        tag.putInt("FeedingCooldownTicks", Math.max(0, this.entityData.get(DATA_FEEDING_COOLDOWN)));
        tag.putBoolean("BoundToBinder", boundToBinder);
        if (this.packLeaderUuid != null) {
            tag.putUUID("PackLeaderUuid", this.packLeaderUuid);
        }
        tag.putBoolean("StegonautHasChest", hasStegonautChest());
        if (hasStegonautChest()) {
            tag.put("StegonautChestItems", stegonautChestInventory.createTag());
        }
        saveSitProgress(tag);
        saveRideableData(tag);
    }

    @Override
    public void readAdditionalSaveData(@NotNull CompoundTag tag) {
        super.readAdditionalSaveData(tag);
        loadRideableData(tag);

        int restoredCommand = this.getCommand();
        if (tag.contains("StegonautCommand")) {
            restoredCommand = tag.getInt("StegonautCommand");
            this.setCommand(restoredCommand);
        }
        boolean restoredOrderedSit = tag.contains("StegonautOrderedSit")
                ? tag.getBoolean("StegonautOrderedSit")
                : restoredCommand == 1;
        grumbleCooldown = tag.getInt("GrumbleCooldown");
        if (tag.contains("FeedingCooldownTicks")) {
            this.entityData.set(DATA_FEEDING_COOLDOWN, Math.max(0, tag.getInt("FeedingCooldownTicks")));
        }
        boundToBinder = tag.getBoolean("BoundToBinder");
        this.packLeaderUuid = tag.hasUUID("PackLeaderUuid") ? tag.getUUID("PackLeaderUuid") : null;
        if (this.isTame()) {
            this.packLeaderUuid = null;
        }

        setStegonautChest(tag.getBoolean("StegonautHasChest"));
        if (hasStegonautChest() && tag.contains("StegonautChestItems", Tag.TAG_LIST)) {
            stegonautChestInventory.fromTag(tag.getList("StegonautChestItems", Tag.TAG_COMPOUND));
        }
        if (tag.contains("SitProgress")) {
            setSitProgress(tag.getFloat("SitProgress"));
        }
        refreshCommandState();
        this.setOrderedToSit(restoredOrderedSit);
    }

    @Override
    public @Nullable UUID getPackLeaderUuid() {
        return this.packLeaderUuid;
    }

    @Override
    public void setPackLeaderUuid(@Nullable UUID leaderUuid) {
        this.packLeaderUuid = leaderUuid;
    }

    @Override
    public int getMaxPackSize() {
        return MAX_PACK_SIZE;
    }

    @Override
    public double getPackSearchRadius() {
        return PACK_SEARCH_RADIUS;
    }

    @Override
    public boolean canTarget(Entity entity) {
        if (this.isBaby()) {
            return false;
        }

        if (entity instanceof Stegonaut otherStegonaut) {
            if (otherStegonaut.isBaby()) {
                return false;
            }
            return false;
        }
        if (entity instanceof Player player && !this.isTame()) {
            if (isAggressiveWild()) {
                return !player.isCreative() && !player.isSpectator();
            }
            return this.getLastHurtByMob() == player || this.getTarget() == player;
        }
        return super.canTarget(entity);
    }

    private boolean isAggressiveWild() {
        return DragonAttributeConfigLoader.getInstance()
                .getConfig(DragonAttributeConfigLoader.STEGONAUT_ID)
                .extraBoolean("aggressive_wild", false);
    }

    @Override
    public @NotNull Vec3 getRiddenInput(@NotNull Player player, @NotNull Vec3 deltaIn) {
        if (areRiderControlsLocked()) {
            return Vec3.ZERO;
        }

        Vec3 input = riderController.getRiddenInput(player, deltaIn);
        if (!level().isClientSide) {
            float fwd = (float) Math.max(-1.0D, Math.min(1.0D, input.z));
            float str = (float) Math.max(-1.0D, Math.min(1.0D, input.x));
            setLastRiderForward(Math.abs(fwd) > 0.02f ? fwd : 0f);
            setLastRiderStrafe(Math.abs(str) > 0.02f ? str : 0f);
        }
        return input;
    }

    @Override
    public float getRiddenSpeed(@NotNull Player rider) {
        return riderController.getRiddenSpeed(rider);
    }

    @Override
    public void tickRidden(@NotNull Player player, @NotNull Vec3 travelVec) {
        super.tickRidden(player, travelVec);
        riderController.tickRidden(player, travelVec);
    }

    @Override
    public void travel(@NotNull Vec3 motion) {
        if (this.isVehicle() && this.getControllingPassenger() instanceof Player player) {
            if (areRiderControlsLocked()) {
                this.setDeltaMovement(Vec3.ZERO);
                return;
            }
            if (this.getNavigation().getPath() != null) {
                this.getNavigation().stop();
            }

            setGoingUp(false);
            setGoingDown(false);
            travelRiddenGround(player, getRiddenInput(player, motion), riderController.getRiddenSpeed(player));
            return;
        }

        super.travel(motion);
    }

    @Override
    protected void positionRider(@Nonnull @NotNull Entity passenger,
                                 @Nonnull @NotNull Entity.MoveFunction moveFunction) {
        riderController.positionRider(passenger, moveFunction);
    }

    @Override
    public @NotNull Vec3 getDismountLocationForPassenger(@NotNull LivingEntity passenger) {
        return riderController.getDismountLocationForPassenger(passenger);
    }

    @Override
    public @Nullable LivingEntity getControllingPassenger() {
        return riderController.getControllingPassenger();
    }

    public boolean canBeBound() {
        return !isSleeping() && !isDying();
    }
}

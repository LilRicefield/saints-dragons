package com.leon.saintsdragons.server.entity.dragons.stegonaut;

import com.leon.saintsdragons.server.ai.goals.base.DragonSleepBehavior;
import com.leon.saintsdragons.server.ai.goals.base.DragonBreedGoal;
import com.leon.saintsdragons.server.ai.goals.base.DragonFollowParentGoal;
import com.leon.saintsdragons.server.ai.goals.base.DragonOwnerHurtByTargetGoal;
import com.leon.saintsdragons.server.ai.goals.base.DragonOwnerHurtTargetGoal;
import com.leon.saintsdragons.server.ai.goals.base.DragonPackDefendPackGoal;
import com.leon.saintsdragons.server.ai.goals.base.DragonPackFollowLeaderGoal;
import com.leon.saintsdragons.server.ai.goals.base.DragonProtectBabiesGoal;
import com.leon.saintsdragons.server.ai.goals.stegonaut.*;
import com.leon.saintsdragons.server.ai.navigation.DragonPathNavigateGround;
import com.leon.saintsdragons.server.entity.ability.abilities.stegonaut.StegonautPassiveBuffAbility;
import com.leon.saintsdragons.server.entity.ability.abilities.stegonaut.StegonautGroundEatingAbility;
import com.leon.saintsdragons.server.entity.base.RideableDragonBase;
import com.leon.saintsdragons.server.entity.base.DragonEntity;
import com.leon.saintsdragons.server.entity.base.DragonGender;
import com.leon.saintsdragons.server.entity.dragons.stegonaut.handlers.StegonautAnimationHandler;
import com.leon.saintsdragons.server.entity.dragons.stegonaut.handlers.StegonautSoundProfile;
import com.leon.saintsdragons.server.entity.controller.stegonaut.StegonautRiderController;
import com.leon.saintsdragons.server.entity.handler.DragonSoundHandler;
import com.leon.saintsdragons.server.entity.interfaces.DragonSoundProfile;
import com.leon.saintsdragons.server.entity.interfaces.PackMember;
import com.leon.saintsdragons.server.entity.interfaces.SoundHandledDragon;
import com.leon.saintsdragons.server.menu.StegonautInventoryMenu;
import com.leon.saintsdragons.common.network.DragonRiderAction;
import net.minecraft.core.Direction;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.FloatGoal;
import net.minecraft.world.entity.ai.goal.target.HurtByTargetGoal;
import net.minecraft.world.entity.animal.Animal;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.core.BlockPos;
import net.minecraft.util.RandomSource;
import net.minecraft.world.phys.Vec3;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.AgeableMob;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import com.leon.saintsdragons.server.entity.ability.DragonAbilityType;
import com.leon.saintsdragons.common.registry.ModEntities;
import com.leon.saintsdragons.common.registry.ModBlocks;
import com.leon.saintsdragons.common.registry.ModItems;
import com.leon.saintsdragons.common.registry.ModSounds;
import com.leon.saintsdragons.common.registry.AbilityRegistry;
import com.leon.saintsdragons.common.registry.stegonaut.StegonautAbilities;
import com.leon.saintsdragons.common.config.dragon.DragonAttributeConfig;
import com.leon.saintsdragons.common.config.dragon.DragonAttributeConfigLoader;
import com.leon.saintsdragons.common.block.StegonautEggBlockEntity;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.SimpleMenuProvider;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.Container;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import javax.annotation.Nonnull;
import software.bernie.geckolib.core.animatable.instance.AnimatableInstanceCache;
import software.bernie.geckolib.core.animation.AnimatableManager;
import software.bernie.geckolib.core.animation.AnimationController;
import software.bernie.geckolib.util.GeckoLibUtil;

public class Stegonaut extends RideableDragonBase implements SoundHandledDragon, PackMember<Stegonaut> {

    public AnimatableInstanceCache dragonCache = GeckoLibUtil.createInstanceCache(this);
    private final StegonautAnimationHandler animationController = new StegonautAnimationHandler(this);
    private final DragonSoundHandler soundHandler = new DragonSoundHandler(this);
    private final StegonautRiderController riderController = new StegonautRiderController(this);
    private final SimpleContainer stegonautChestInventory = new SimpleContainer(STEGONAUT_CHEST_SLOTS);
    // Passive aura that applies resistance and absorption to allies
    private final StegonautPassiveBuffAbility passiveBuffAbility =
            new StegonautPassiveBuffAbility(this);

    // ===== CLIENT LOCATOR CACHE (client-side only) =====
    private final Map<String, Vec3> clientLocatorCache = new ConcurrentHashMap<>();

    // ===== AMBIENT SOUND SYSTEM =====
    private int ambientSoundTimer;
    private int nextAmbientSoundDelay;
    private static final int MIN_AMBIENT_DELAY = 200;  // 10 seconds
    private static final int MAX_AMBIENT_DELAY = 600;  // 30 seconds

    // ===== BREEDING CONSTANTS =====
    private static final double BREED_PARTNER_RANGE = 20.0D;
    private static final double BREED_DISTANCE_SQR = 2500.0D;

    // ===== BABY ATTRIBUTES =====
    private static final double BABY_MAX_HEALTH = 50.0D;
    private static final double BABY_ARMOR = 5.0D;
    private static final float BABY_HITBOX_SCALE = 0.65F;

    // ===== VOCAL ENTRIES =====
    // IMPORTANT: Keys MUST match animation trigger names registered in StegonautAnimationHandler
    private static final Map<String, VocalEntry> VOCAL_ENTRIES = new VocalEntryBuilder()
            .add("grumble1", "action", "animation.stegonaut.grumble1", ModSounds.STEGONAUT_GRUMBLE_1, 0.6f, 1.1f, 0.2f, false, false, true)
            .add("grumble2", "action", "animation.stegonaut.grumble2", ModSounds.STEGONAUT_GRUMBLE_2, 0.6f, 1.1f, 0.2f, false, false, true)
            .add("grumble3", "action", "animation.stegonaut.grumble3", ModSounds.STEGONAUT_GRUMBLE_3, 0.6f, 1.1f, 0.2f, false, false, true)
            .add("stegonaut_hurt", "instant", "animation.stegonaut.hurt", ModSounds.STEGONAUT_HURT, 1.0f, 0.95f, 0.1f, false, true, true)
            .add("stegonaut_die", "instant", "animation.stegonaut.die", ModSounds.STEGONAUT_DIE, 1.2f, 1.0f, 0.0f, false, true, true)
            .build();

    @Override
    public Map<String, VocalEntry> getVocalEntries() {
        return VOCAL_ENTRIES;
    }

    @Override
    public DragonSoundProfile getSoundProfile() {
        return StegonautSoundProfile.INSTANCE;
    }

    // Sleep system fields
    private boolean suppressSitAnimation = false;

    // Client-side animation initialization grace period (fixes T-pose on world rejoin with shaders)
    private int clientAnimInitTicks = 0;
    private static final int ANIM_INIT_GRACE_PERIOD = 5; // Wait 5 ticks for entity data sync

    // Synced ground movement state for reliable animation
    private static final net.minecraft.network.syncher.EntityDataAccessor<Integer> DATA_GROUND_MOVE_STATE =
            net.minecraft.network.syncher.SynchedEntityData.defineId(Stegonaut.class, net.minecraft.network.syncher.EntityDataSerializers.INT);

    private static final net.minecraft.network.syncher.EntityDataAccessor<Float> DATA_RIDER_FORWARD =
            net.minecraft.network.syncher.SynchedEntityData.defineId(Stegonaut.class, net.minecraft.network.syncher.EntityDataSerializers.FLOAT);
    private static final net.minecraft.network.syncher.EntityDataAccessor<Float> DATA_RIDER_STRAFE =
            net.minecraft.network.syncher.SynchedEntityData.defineId(Stegonaut.class, net.minecraft.network.syncher.EntityDataSerializers.FLOAT);
    private static final net.minecraft.network.syncher.EntityDataAccessor<Boolean> DATA_ACCELERATING =
            net.minecraft.network.syncher.SynchedEntityData.defineId(Stegonaut.class, net.minecraft.network.syncher.EntityDataSerializers.BOOLEAN);
    private static final net.minecraft.network.syncher.EntityDataAccessor<Integer> DATA_FLIGHT_MODE =
            net.minecraft.network.syncher.SynchedEntityData.defineId(Stegonaut.class, net.minecraft.network.syncher.EntityDataSerializers.INT);
    private static final net.minecraft.network.syncher.EntityDataAccessor<Boolean> DATA_GOING_UP =
            net.minecraft.network.syncher.SynchedEntityData.defineId(Stegonaut.class, net.minecraft.network.syncher.EntityDataSerializers.BOOLEAN);
    private static final net.minecraft.network.syncher.EntityDataAccessor<Boolean> DATA_GOING_DOWN =
            net.minecraft.network.syncher.SynchedEntityData.defineId(Stegonaut.class, net.minecraft.network.syncher.EntityDataSerializers.BOOLEAN);
    private static final net.minecraft.network.syncher.EntityDataAccessor<Boolean> DATA_RUNNING =
            net.minecraft.network.syncher.SynchedEntityData.defineId(Stegonaut.class, net.minecraft.network.syncher.EntityDataSerializers.BOOLEAN);
    private static final net.minecraft.network.syncher.EntityDataAccessor<Boolean> DATA_HAS_CHEST =
            net.minecraft.network.syncher.SynchedEntityData.defineId(Stegonaut.class, net.minecraft.network.syncher.EntityDataSerializers.BOOLEAN);

    private static final int STEGONAUT_CHEST_SLOTS = 15;

    // ===== RIDING SPEED CONSTANTS =====
    public static final double RIDER_WALK_SPEED = 0.1D;
    public static final double RIDER_RUN_SPEED = 0.25D;

    // Binding state for Drake Binder
    private boolean boundToBinder = false;
    @Nullable
    private UUID packLeaderUuid;

    private static final int MAX_PACK_SIZE = 6;
    private static final double PACK_SEARCH_RADIUS = 48.0D;

    // Client-side smoothing for ground movement states to avoid animation flicker when idle
    private static final int CLIENT_GROUND_STATE_STABLE_TICKS = 3;
    private int clientGroundMoveState = 0;
    private int clientGroundMoveTarget = 0;
    private int clientGroundMoveHold = 0;

    // Server-side hold timer to prevent flickering when stopping
    private int walkAnimationHoldTicks = 0;
    private int groundStepSoundCooldownTicks = 0;

    public Stegonaut(EntityType<? extends Stegonaut> entityType, Level level) {
        super(entityType, level);
        // Initialize animation state
        animationController.initializeAnimation();

        // Desynchronize ambient system across instances to avoid synchronized vocals/animations
        RandomSource rng = this.getRandom();
        this.ambientSoundTimer = rng.nextInt(80); // small random offset
        this.nextAmbientSoundDelay = MIN_AMBIENT_DELAY + rng.nextInt(MAX_AMBIENT_DELAY - MIN_AMBIENT_DELAY);

        // Apply baby/adult attributes
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
        this.entityData.define(DATA_GROUND_MOVE_STATE, 0);
        this.entityData.define(DATA_RIDER_FORWARD, 0.0F);
        this.entityData.define(DATA_RIDER_STRAFE, 0.0F);
        this.entityData.define(DATA_ACCELERATING, false);
        this.entityData.define(DATA_FLIGHT_MODE, -1);
        this.entityData.define(DATA_GOING_UP, false);
        this.entityData.define(DATA_GOING_DOWN, false);
        this.entityData.define(DATA_RUNNING, false);
        this.entityData.define(DATA_HAS_CHEST, false);
    }

    @Override
    protected void registerGoals() {
        this.goalSelector.addGoal(0, new FloatGoal(this)); // CRITICAL: Must float in water to not drown!

        // Register unconditionally; goal self-gates to wild babies in canUse().
        this.goalSelector.addGoal(2, new DragonFollowParentGoal<>(this, Stegonaut.class, 0.70D));

        // Removed flee goal - pack animal doesn't run from threats
        // Register unconditionally; breed goal self-gates via canBreed().
        this.goalSelector.addGoal(3, new DragonBreedGoal<>(
                this, 1.0D, Stegonaut.class, BREED_PARTNER_RANGE, BREED_DISTANCE_SQR
        ));

        this.goalSelector.addGoal(4, new StegonautCombatGoal(this));
        this.goalSelector.addGoal(5, new StegonautFollowOwnerGoal(this));
        this.goalSelector.addGoal(6, new DragonPackFollowLeaderGoal<>(this, Stegonaut.class, 0.75D, 16.0D, 8.0D));
        // Idle water behavior when no target: direct swim wandering instead of idling in place.
        this.goalSelector.addGoal(7, new com.leon.saintsdragons.server.ai.goals.base.DirectSwimWanderGoal(this, 8.0F, 0.12D, 1, true));
        this.goalSelector.addGoal(7, new StegonautGroundWanderGoal(this, 0.35D, 120));

        this.goalSelector.addGoal(8, new StegonautLookAtPlayerGoal(this, Player.class, 8.0F));
        this.goalSelector.addGoal(9, new StegonautRandomLookAroundGoal(this));

        // Retaliation/assist targeting only:
        // - owner hurt by target
        // - owner hurt target
        // - stegonaut hurt by target
        this.targetSelector.addGoal(1, new DragonProtectBabiesGoal<>(this, Stegonaut.class));
        this.targetSelector.addGoal(2, new DragonOwnerHurtByTargetGoal(this));
        this.targetSelector.addGoal(3, new DragonOwnerHurtTargetGoal(this));
        this.targetSelector.addGoal(4, new DragonPackDefendPackGoal<>(this, Stegonaut.class, 36.0D));
        this.targetSelector.addGoal(5, new HurtByTargetGoal(this));
    }

    @Override
    protected net.minecraft.world.entity.ai.navigation.@NotNull PathNavigation createNavigation(net.minecraft.world.level.@NotNull Level level) {
        // Use custom DragonPathNavigateGround for smoother pathfinding with shortcutting
        // Prevents jerky stop-at-every-waypoint behavior, especially during flee/wander
        return new DragonPathNavigateGround(this, level);
    }

    public static AttributeSupplier.Builder createAttributes() {
        return Mob.createMobAttributes()
                .add(Attributes.MAX_HEALTH, 100.0D)
                .add(Attributes.KNOCKBACK_RESISTANCE, 1.0D)
                .add(Attributes.MOVEMENT_SPEED, 0.50D) // Increased for pack animal duties
                .add(Attributes.ATTACK_DAMAGE, 2.0D)
                .add(Attributes.ARMOR, 15.0D)
                .add(Attributes.FOLLOW_RANGE, 64.0D);
    }

    @Override
    protected boolean isDragonFlying() {
        return false;
    }

    /**
     * Check if the wyvern is in a muted state (sitting/staying)
     * Used by sound system to prevent ambient sounds
     */
    public boolean isStayOrSitMuted() {
        return this.isOrderedToSit() || this.isInSittingPose();
    }

    /**
     * Primitive Drakes are rideable, but ground-only.
     */

    @Override
    public boolean canOwnerCommand(Player player) {
        // Primitive Drakes respond to commands from their owner without requiring crouching
        return player != null && player.equals(this.getOwner());
    }

    @Override
    protected boolean supportsRiderAction(DragonRiderAction action) {
        return switch (action) {
            case ABILITY_USE, ABILITY_STOP, OPEN_INVENTORY -> true;
            default -> super.supportsRiderAction(action);
        };
    }

    @Override
    protected void onRiderAbilityUse(Player player, String abilityName) {
        if (abilityName != null && !abilityName.isEmpty()) {
            useRidingAbility(abilityName);
        }
    }

    @Override
    protected void onRiderAbilityStop(Player player, String abilityName) {
        if (abilityName != null && !abilityName.isEmpty()) {
            if (StegonautAbilities.STEGONAUT_GROUND_EATING_ID.equals(abilityName)) {
                var active = combatManager.getActiveAbility();
                if (active != null && active.getAbilityType() == StegonautAbilities.STEGONAUT_GROUND_EATING) {
                    ((StegonautGroundEatingAbility) active).requestRelease();
                    return;
                }
            }
            forceEndActiveAbility();
        }
    }

    @Override
    protected void onRiderOpenInventory(Player player) {
        if (player instanceof ServerPlayer serverPlayer) {
            openStegonautInventory(serverPlayer);
        }
    }

    @Override
    public boolean canTakeoff() {
        return false;
    }

    @Override
    public boolean hasSecondaryMelee() {
        return true;
    }

    @Override
    public boolean isFood(@Nonnull net.minecraft.world.item.ItemStack stack) {
        // Simple food - maybe raw meat or fish?
        return stack.is(net.minecraft.world.item.Items.BEEF) ||
                stack.is(net.minecraft.world.item.Items.PORKCHOP) ||
                stack.is(net.minecraft.world.item.Items.CHICKEN) ||
                stack.is(net.minecraft.world.item.Items.MUTTON) ||
                stack.is(net.minecraft.world.item.Items.COD) ||
                stack.is(net.minecraft.world.item.Items.SALMON) ||
                stack.is(com.leon.saintsdragons.common.registry.ModItems.HEARTY_DRAGON_MEAL.get());
    }

    @Override
    public Vec3 getHeadPosition() {
        // Use eye position - more reliable than bone positions
        return this.getEyePosition();
    }

    @Override
    public Vec3 getMouthPosition() {
        // Simple mouth position - just the head area
        return this.position().add(0, this.getBbHeight() * 0.8, 0);
    }

    @Override
    public DragonAbilityType<?, ?> getPrimaryAttackAbility() {
        return getMeleeMode() == 0
                ? StegonautAbilities.STEGONAUT_BITE
                : StegonautAbilities.STEGONAUT_CHIN_SLAM;
    }

    public DragonAbilityType<?, ?> getRandomAiAttackAbility() {
        return this.getRandom().nextBoolean()
                ? StegonautAbilities.STEGONAUT_BITE
                : StegonautAbilities.STEGONAUT_CHIN_SLAM;
    }

    @Override
    public RiderAbilityBinding getAttackRiderAbility() {
        String abilityId = getMeleeMode() == 0
                ? StegonautAbilities.STEGONAUT_BITE_ID
                : StegonautAbilities.STEGONAUT_CHIN_SLAM_ID;
        return new RiderAbilityBinding(abilityId, RiderAbilityBinding.Activation.PRESS);
    }

    @Override
    public RiderAbilityBinding getTertiaryRiderAbility() {
        return new RiderAbilityBinding(StegonautAbilities.STEGONAUT_GROUND_EATING_ID, RiderAbilityBinding.Activation.HOLD);
    }

    public void useRidingAbility(String abilityName) {
        if (abilityName == null || abilityName.isEmpty()) {
            return;
        }
        net.minecraft.world.entity.Entity rider = this.getControllingPassenger();
        if (!(rider instanceof LivingEntity)) {
            return;
        }
        if (this.isTame() && rider instanceof Player player && !this.isOwnedBy(player)) {
            return;
        }
        DragonAbilityType<?, ?> type = AbilityRegistry.get(abilityName);
        if (type == StegonautAbilities.STEGONAUT_BITE
                || type == StegonautAbilities.STEGONAUT_CHIN_SLAM
                || type == StegonautAbilities.STEGONAUT_GROUND_EATING) {
            combatManager.tryUseAbility(type);
        }
    }

    public void forceEndActiveAbility() {
        combatManager.forceEndActiveAbility();
    }

    @Override
    protected DragonAbilityType<?, ?> getHurtAbilityType() {
        return StegonautAbilities.STEGONAUT_HURT;
    }

    @Override
    public int getDeathAnimationDurationTicks() {
        return 41;
    }

    @Override
    protected void dropAllDeathLoot(@NotNull DamageSource source) {
        // Keep loot timing aligned with death animation completion.
        if (deathTime < getDeathAnimationDurationTicks()) {
            return;
        }

        super.dropAllDeathLoot(source);

        DragonAttributeConfig config = DragonAttributeConfigLoader.getInstance()
                .getConfig(DragonAttributeConfigLoader.STEGONAUT_ID);
        double eggDropChance = config.extraDouble("egg_drop_chance", 0.12D);
        if (!level().isClientSide && getGender() == DragonGender.FEMALE && this.random.nextDouble() < eggDropChance) {
            this.spawnAtLocation(ModItems.STEGONAUT_EGG.get());
        }
    }

    @Override
    public boolean hurt(@NotNull DamageSource source, float amount) {
        // During dying sequence, ignore all damage (entity is already dead, playing death animation)
        if (isDying()) {
            return false;
        }
        if (!isWildStegonautDamageAllowed(source)) {
            return false;
        }

        return super.hurt(source, amount);
    }

    @Override
    public boolean fireImmune() {
        return false;
    }

    private boolean isWildStegonautDamageAllowed(@NotNull DamageSource source) {
        Entity attacker = source.getEntity();
        if (!(attacker instanceof Stegonaut other)) {
            return true;
        }

        // Tamed Stegonauts are treated as outsiders for pack systems.
        if (this.isTame() || other.isTame()) {
            return true;
        }

        // Babies are never valid participants in Stegonaut-vs-Stegonaut combat.
        if (this.isBaby() || other.isBaby()) {
            return false;
        }

        // Wild Stegonauts do not damage each other in simplified pack mode.
        return false;
    }

    /**
     * Returns a larger bounding box for frustum culling to prevent the model from
     * disappearing when the entity's collision box is off-screen but the visual model
     * should still be visible.
     */
    @Override
    public net.minecraft.world.phys.AABB getBoundingBoxForCulling() {
        return super.getBoundingBoxForCulling().inflate(6.0, 3.0, 6.0);
    }

    @Override
    protected DragonAbilityType<?, ?> getDeathAbilityType() {
        return StegonautAbilities.STEGONAUT_DIE;
    }

    @Override
    public void registerControllers(AnimatableManager.ControllerRegistrar controllers) {
        // Use the new smooth animation controller
        AnimationController<Stegonaut> movementController = new AnimationController<>(this, "movement", 1, animationController::handleMovementAnimation);
        movementController.setSoundKeyframeHandler(event -> {});
        controllers.add(movementController);

        // Add action controller for grumble animations
        AnimationController<Stegonaut> actionController = new AnimationController<>(this, "action", 5, animationController::actionPredicate);
        animationController.setupActionController(actionController);
        actionController.setSoundKeyframeHandler(event -> {});
        controllers.add(actionController);

        AnimationController<Stegonaut> instantController = new AnimationController<>(this, "instant", 1, animationController::instantActionPredicate);
        animationController.setupInstantActionController(instantController);
        instantController.setSoundKeyframeHandler(event -> {});
        controllers.add(instantController);
    }

    @Override
    public AnimatableInstanceCache getAnimatableInstanceCache() {
        return this.dragonCache;
    }

    @Override
    public AgeableMob getBreedOffspring(@Nonnull ServerLevel level, @Nonnull AgeableMob other) {
        Stegonaut baby = ModEntities.STEGONAUT.get().create(level);
        if (baby != null) {
            // Set gender randomly
            baby.setGender(this.random.nextBoolean() ? DragonGender.FEMALE : DragonGender.MALE);

            // Inherit owner from parent
            java.util.UUID ownerId = this.getOwnerUUID();
            if (ownerId != null) {
                baby.setOwnerUUID(ownerId);
                baby.setTame(true);
            }

            // Set baby attributes
            baby.setAge(-24000); // Baby age
            baby.setBaby(true);
            baby.applyConfiguredAttributes();
            baby.setHealth(baby.getMaxHealth());

            // Position the baby near the parent to prevent Y=0 spawning
            net.minecraft.core.BlockPos safePos = findSafeBabySpawnPos(level, this.blockPosition());
            double spawnY = safePos != null ? safePos.getY() : this.getY();
            baby.moveTo(this.getX(), spawnY, this.getZ(), this.getYRot(), 0.0F);
            registerToOwnerCodex(baby, level);
        }
        return baby;
    }

    // ===== BABY ATTRIBUTE SYSTEM =====

    public void applyConfiguredAttributes() {
        DragonAttributeConfig config = DragonAttributeConfigLoader.getInstance()
                .getConfig(DragonAttributeConfigLoader.STEGONAUT_ID);

        // Apply baby-specific stats or adult stats
        setAttributeBase(Attributes.MAX_HEALTH, isBaby() ? BABY_MAX_HEALTH : config.maxHealth());
        setAttributeBase(Attributes.ARMOR, isBaby() ? BABY_ARMOR : config.armor());

        // Clamp health if it exceeds new max
        double maxHealth = isBaby() ? BABY_MAX_HEALTH : config.maxHealth();
        if (this.getHealth() > maxHealth) {
            this.setHealth((float) maxHealth);
        }
    }

    private void setAttributeBase(net.minecraft.world.entity.ai.attributes.Attribute attribute, double value) {
        net.minecraft.world.entity.ai.attributes.AttributeInstance instance = this.getAttribute(attribute);
        if (instance != null) {
            instance.setBaseValue(value);
        }
    }

    @Override
    public void ageBoundaryReached() {
        super.ageBoundaryReached();
        applyConfiguredAttributes();
        refreshDimensions();
    }

    @Override
    public @NotNull net.minecraft.world.entity.EntityDimensions getDimensions(@NotNull net.minecraft.world.entity.Pose pose) {
        net.minecraft.world.entity.EntityDimensions baseDimensions = super.getDimensions(pose);
        if (isBaby()) {
            return baseDimensions.scale(BABY_HITBOX_SCALE);
        }
        return baseDimensions;
    }

    @Override
    public boolean canBreed() {
        return this.isTame() && !this.isBaby() && this.getHealth() >= this.getMaxHealth() && this.isInLove();
    }

    @Override
    public boolean canMate(@Nonnull Animal otherAnimal) {
        if (!this.canBreed()) {
            return false;
        }

        if (otherAnimal instanceof Stegonaut otherDragon) {
            if (this.isFemale() == otherDragon.isFemale()) {
                return false;
            }
            return otherDragon.canBreed();
        }

        return false;
    }

    @Override
    public net.minecraft.world.level.block.state.BlockState getEggBlockState() {
        return ModBlocks.STEGONAUT_EGG.get().defaultBlockState();
    }

    @Override
    public void configureEggBlockEntity(net.minecraft.world.level.block.entity.BlockEntity blockEntity,
                                        @Nullable DragonEntity partner) {
        if (!(blockEntity instanceof StegonautEggBlockEntity eggEntity)) {
            return;
        }

        java.util.UUID ownerUUID = resolveEggOwnerUUID(partner);
        if (ownerUUID != null) {
            eggEntity.setOwnerUUID(ownerUUID);
        }

        DragonGender babyGender = this.getRandom().nextBoolean() ? DragonGender.FEMALE : DragonGender.MALE;
        eggEntity.setBabyGender(babyGender);
    }

    public static boolean canSpawnHere(EntityType<? extends Stegonaut> type,
                                       LevelAccessor level,
                                       MobSpawnType spawnType,
                                       BlockPos pos,
                                       RandomSource random) {
        if (!Animal.checkAnimalSpawnRules(type, level, spawnType, pos, random)) {
            return false;
        }

        // Reject waterlogged blocks or fluid directly around the spawn
        if (!level.getFluidState(pos).isEmpty()) {
            return false;
        }
        if (!level.getFluidState(pos.below()).isEmpty()) {
            return false;
        }

        // Optional: enforce a sturdy block underneath
        return level.getBlockState(pos.below()).isFaceSturdy(level, pos.below(), Direction.UP);
    }

    // ===== INTERACTION HANDLING =====

    @Override
    public @NotNull InteractionResult mobInteract(@NotNull Player player, @NotNull InteractionHand hand) {
        ItemStack itemstack = player.getItemInHand(hand);
        if (com.leon.saintsdragons.common.registry.ModItems.isDragonBrush(itemstack)) {
            // Match other dragons: acknowledge brush on client so hand swing plays,
            // while the actual grooming logic executes on the server.
            if (this.level().isClientSide) {
                return InteractionResult.sidedSuccess(true);
            }
            boolean brushed = this.tryBrush(player, itemstack);
            return brushed ? InteractionResult.sidedSuccess(false) : InteractionResult.CONSUME;
        }
        if (!this.isTame()) {
            return handleUntamedInteraction(player, hand);
        } else {
            return handleTamedInteraction(player, hand);
        }
    }

    /**
     * Handle interactions with untamed drakes (100% success taming)
     */
    private InteractionResult handleUntamedInteraction(Player player, InteractionHand hand) {
        ItemStack itemstack = player.getItemInHand(hand);

        if (!this.isFood(itemstack)) {
            return InteractionResult.PASS;
        }

        // Taming logic must be server-only to avoid client-only visual state changes
        if (!this.level().isClientSide) {
            if (!player.getAbilities().instabuild) {
                itemstack.shrink(1);
            }

            // Trigger eat animation
            this.triggerAnim("action", "eat");
            playEatMovingSound();

            DragonAttributeConfig config = DragonAttributeConfigLoader.getInstance()
                    .getConfig(DragonAttributeConfigLoader.STEGONAUT_ID);
            boolean hearty = itemstack.is(com.leon.saintsdragons.common.registry.ModItems.HEARTY_DRAGON_MEAL.get());
            double tamingChance = hearty
                    ? config.extraDoubles().getOrDefault("taming_chance_hearty", 1.0)
                    : config.extraDoubles().getOrDefault("taming_chance_base", 1.0);
            int tameRoll = (int) Math.round(tamingChance);

            if (this.getRandom().nextInt(Math.max(1, tameRoll)) == 0) {
                this.tame(player);
                this.setPackLeaderUuid(null); // Tamed Stegonauts leave wild packs immediately.
                this.setOrderedToSit(true);
                this.setCommand(1); // Set command to Sit (1) to match the sitting state

                this.level().broadcastEntityEvent(this, (byte) 7); // Hearts particles

                // Send taming success message
                player.displayClientMessage(
                        Component.translatable("entity.saintsdragons.stegonaut.tamed", this.getName()),
                        true
                );

                // Trigger advancement for taming Primitive Drake
                if (player instanceof net.minecraft.server.level.ServerPlayer serverPlayer) {
                    var advancement = serverPlayer.server.getAdvancements()
                            .getAdvancement(com.leon.saintsdragons.common.SaintsDragonsCommon.rl("tame_stegonaut"));
                    if (advancement != null) {
                        serverPlayer.getAdvancements().award(advancement, "tame_stegonaut");
                    }
                }
            } else {
                this.level().broadcastEntityEvent(this, (byte) 6); // Smoke particles
            }
        }

        return InteractionResult.sidedSuccess(this.level().isClientSide);
    }

    /**
     * Handle interactions with tamed drakes (feeding, commands)
     */
    private InteractionResult handleTamedInteraction(Player player, InteractionHand hand) {
        ItemStack itemstack = player.getItemInHand(hand);

        if (itemstack.getItem() instanceof com.leon.saintsdragons.common.item.StegonautBinderItem) {
            InteractionResult result = itemstack.interactLivingEntity(player, this, hand);
            if (result != InteractionResult.PASS) {
                return result;
            }
        }

        if (itemstack.is(com.leon.saintsdragons.common.registry.ModItems.DRACONIC_CODEX.get())) {
            return InteractionResult.PASS;
        }

        if (player.equals(this.getOwner()) && player.isShiftKeyDown() && this.isFood(itemstack)) {
            return handleBreeding(player, itemstack);
        }

        // Handle feeding for healing
        if (this.isFood(itemstack)) {
            return handleFeeding(player, itemstack);
        }

        // Handle owner commands
        if (player.equals(this.getOwner())) {
            // Command cycling - Shift+Right-click cycles through commands
            if (player.isShiftKeyDown() && this.canOwnerCommand(player) && !this.isFood(itemstack) && hand == InteractionHand.MAIN_HAND) {
                return handleCommandCycling(player);
            }

            if (!player.isShiftKeyDown() && !this.isFood(itemstack) && hand == InteractionHand.MAIN_HAND) {
                return handleMounting(player);
            }
        }

        // Fall back to base implementation for other interactions
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

    private InteractionResult handleMounting(Player player) {
        if (!this.canOwnerMount(player) || this.isVehicle()) {
            return InteractionResult.sidedSuccess(this.level().isClientSide);
        }

        if (!this.level().isClientSide) {
            prepareForMounting();
            player.startRiding(this);
        }

        return InteractionResult.sidedSuccess(this.level().isClientSide);
    }

    private void prepareForMounting() {
        if (this.level().isClientSide) {
            return;
        }

        suppressSitAnimation = true;
        this.setOrderedToSit(false);
        suppressSitAnimation = false;
        if (this.getCommand() == 1) {
            this.setCommand(0);
        }
        forceSitProgress(0f);
        this.setTarget(null);
        if (this.getNavigation().getPath() != null) {
            this.getNavigation().stop();
        }
    }

    private InteractionResult handleBreeding(Player player, ItemStack itemstack) {
        boolean client = this.level().isClientSide;

        if (this.isBaby()) {
            sendStatusMessage(player, "entity.saintsdragons.stegonaut.breeding_too_young");
            return InteractionResult.sidedSuccess(client);
        }

        if (this.getAge() != 0) {
            sendStatusMessage(player, "entity.saintsdragons.stegonaut.breeding_cooling_down");
            return InteractionResult.sidedSuccess(client);
        }

        if (this.isInLove()) {
            sendStatusMessage(player, "entity.saintsdragons.stegonaut.breeding_already_ready");
            return InteractionResult.sidedSuccess(client);
        }

        if (!client) {
            if (!player.getAbilities().instabuild) {
                itemstack.shrink(1);
            }

            this.triggerAnim("action", "eat");
            playEatMovingSound();
            this.setInLove(player);
            sendStatusMessage(player, "entity.saintsdragons.stegonaut.breeding_ready");
        }

        return InteractionResult.sidedSuccess(client);
    }

    /**
     * Handle feeding tamed drakes for healing
     */
    private InteractionResult handleFeeding(Player player, ItemStack itemstack) {
        if (!this.level().isClientSide) {
            if (!player.getAbilities().instabuild) {
                itemstack.shrink(1);
            }

            // Trigger eat animation
            this.triggerAnim("action", "eat");
            playEatMovingSound();

            boolean hearty = itemstack.is(com.leon.saintsdragons.common.registry.ModItems.HEARTY_DRAGON_MEAL.get());
            boolean wasHungry = this.isHungry();

            if (this.isBaby()) {
                // Baby growth logic
                int growthTicks = hearty ? 4800 : 2400; // Hearty meal: 4 minutes vs 2 minutes
                int currentAge = this.getAge();
                int newAge = Math.min(0, currentAge + growthTicks);
                this.setAge(newAge);

                this.level().broadcastEntityEvent(this, (byte) 7); // Hearts

                if (player instanceof net.minecraft.server.level.ServerPlayer serverPlayer) {
                    String messageKey = (newAge == 0)
                            ? "entity.saintsdragons.stegonaut.baby_grown"
                            : "entity.saintsdragons.stegonaut.baby_fed";
                    serverPlayer.displayClientMessage(
                            Component.translatable(messageKey, this.getName()),
                            true
                    );
                }
                this.applyFeedingHunger(hearty);
            } else {
                // Adult healing logic
                float healAmount = hearty ? 18.0f : 8.0f; // Hearty meal: +7 hearts vs +4 hearts
                float oldHealth = this.getHealth();
                float newHealth = Math.min(oldHealth + healAmount, this.getMaxHealth());
                this.setHealth(newHealth);
                this.applyFeedingHunger(hearty);

                // Play eating sound and particles
                this.level().broadcastEntityEvent(this, (byte) 6); // Eating sound
                this.level().broadcastEntityEvent(this, (byte) 7); // Hearts particles

                // Send appropriate feedback message
                String messageKey;
                if (newHealth >= this.getMaxHealth()) {
                    messageKey = wasHungry ? "entity.saintsdragons.dragon.feeding" : "entity.saintsdragons.stegonaut.fed";
                } else {
                    messageKey = "entity.saintsdragons.stegonaut.fed_partial";
                }

                player.displayClientMessage(
                        Component.translatable(messageKey, this.getName()),
                        true
                );
            }
        }

        return InteractionResult.sidedSuccess(this.level().isClientSide);
    }

    /**
     * Handle command cycling (Follow/Sit/Wander)
     */
    private InteractionResult handleCommandCycling(Player player) {
        // Get current command and cycle to next
        int currentCommand = this.getCommand();
        int nextCommand = (currentCommand + 1) % 3; // 0=Follow, 1=Sit, 2=Wander

        // Apply the new command
        this.setCommand(nextCommand);
        applyCommandState(nextCommand);

        // Send feedback message to player (action bar), server-side only to avoid duplicates
        if (!this.level().isClientSide) {
            player.displayClientMessage(
                    Component.translatable(
                            "entity.saintsdragons.all.command_" + nextCommand,
                            this.getName()
                    ),
                    true
            );
        }

        return InteractionResult.SUCCESS;
    }

    private void sendStatusMessage(Player player, String key) {
        if (player instanceof net.minecraft.server.level.ServerPlayer serverPlayer) {
            serverPlayer.displayClientMessage(Component.translatable(key, this.getName()), true);
        }
    }

    /**
     * Apply the command state to the drake
     */
    private void applyCommandState(int command) {
        switch (command) {
            case 0: // Follow
                this.setOrderedToSit(false);
                // Let updateSittingProgress() handle the "up" animation transition naturally
                break;
            case 1: // Sit
                this.setOrderedToSit(true);
                break;
            case 2: // Wander
                this.setOrderedToSit(false);
                // Let updateSittingProgress() handle the "up" animation transition naturally
                break;
        }
    }

    public void refreshCommandState() {
        applyCommandState(this.getCommand());
    }

    // ===== SOUND SYSTEM =====

    private int grumbleCooldown = 0;
    public DragonSoundHandler getSoundHandler() {
        return soundHandler;
    }

    private void playCustomAmbientSound() {
        RandomSource random = getRandom();

        if (isDying() || getTarget() != null || isBaby()) {
            return;
        }

        String vocalKey = null;

        float moodRoll = random.nextFloat();

        if (moodRoll < 0.4f) {
            vocalKey = "grumble1";
        } else if (moodRoll < 0.7f) {
            vocalKey = "grumble2";
        } else {
            vocalKey = "grumble3";
        }

        this.getSoundHandler().playVocal(vocalKey);
    }
    private void handleAmbientSounds() {
        if (isDying() || isSleeping() || isSleepTransitioning()) {
            return;
        }

        ambientSoundTimer++;
        if (ambientSoundTimer >= nextAmbientSoundDelay) {
            playCustomAmbientSound();
            resetAmbientSoundTimer();
        }
    }
    private void resetAmbientSoundTimer() {
        RandomSource random = getRandom();
        ambientSoundTimer = 0;
        nextAmbientSoundDelay = MIN_AMBIENT_DELAY + random.nextInt(MAX_AMBIENT_DELAY - MIN_AMBIENT_DELAY);
    }

    @Override
    public void aiStep() {
        super.aiStep();
        if (!level().isClientSide) {
            if (this.isTame()) {
                // Safety net for migrated saves / runtime state transitions.
                this.packLeaderUuid = null;
            }
        }
        tickRiderControlLock();
        tickAnimationStates();
    }

    // ===== SLEEP SYSTEM IMPLEMENTATION =====

    @Override
    public boolean supportsSleep() {
        return true;
    }

    @Override
    public boolean isSleepSuppressed() {
        // Don't sleep while in combat, in water, or while being ridden
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
        this.getNavigation().stop();
        this.setDeltaMovement(0, this.getDeltaMovement().y, 0);
        this.setOrderedToSit(true);
        setGroundMoveStateFromAI(0);
        setSitProgress(getSitProgress());
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
    public DragonSleepBehavior.DragonSleepPreferences getSleepPreferences() {
        // Stegonaut are nocturnal sleepers (sleep at night, active during day)
        return DragonSleepBehavior.DragonSleepPreferences.NOCTURNAL();
    }

    @Override
    public boolean canSleepNow() {
        return !level().isDay();
    }

    // Sleep animation durations (ticks)
    @Override
    protected int getSleepSitDownDuration() {
        return 38;
    }

    @Override
    protected int getSleepSitUpDuration() {
        return 38;
    }

    @Override
    protected int getSleepFallAsleepDuration() {
        return 38;
    }

    @Override
    protected int getSleepWakeUpDuration() {
        return 38;
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
                forceSitProgress(0f);
            }
            return;
        }
        // During sleep transitions, dedicated logic drives the sit clips
        if (isSleeping() || isSleepTransitioning()) {
            return;
        }
        if (sitting) {
            animationController.triggerSitDownAnimation();
            setGroundMoveStateFromAI(0);
        } else {
            animationController.triggerSitUpAnimation();
            setGroundMoveStateFromAI(0);
        }
    }

    // Allow AI to forcibly clamp ground animation state to idle during sleep transitions
    @Override
    public void setGroundMoveStateFromAI(int state) {
        super.setGroundMoveStateFromAI(state);
    }

    @Override
    public void tick() {
        super.tick();

        // Tick sound handler
        soundHandler.tick();

        // Increment animation initialization counter on client (prevents T-pose on rejoin with shaders)
        if (level().isClientSide && clientAnimInitTicks < ANIM_INIT_GRACE_PERIOD) {
            clientAnimInitTicks++;
        }

        // Handle ambient sounds (server-side only)
        if (!level().isClientSide) {
            handleAmbientSounds();
            tickGroundStepAudio();
        }

        // Tick passive buff ability (only if alive)
        if (this.isAlive()) {
            passiveBuffAbility.tick();
        } else {
            // Clean up resistance buffs when dead
            passiveBuffAbility.cleanup();
        }

        // Handle grumble cooldown (now deprecated - replaced by handleAmbientSounds)
        if (grumbleCooldown > 0) {
            grumbleCooldown--;
        }

        // Handle sit progress animation
        if (!level().isClientSide) {
            float sitProgress = getSitProgress();
            if (this.isOrderedToSit()) {
                if (sitProgress < maxSitTicks()) {
                    sitProgress++;
                    setSitProgress(sitProgress);
                }
            } else if (sitProgress > 0f) {
                sitProgress--;
                if (sitProgress < 0f) sitProgress = 0f;
                setSitProgress(sitProgress);
            }
        }
        // Client-side sit progress is synced centrally.
    }

    private boolean shouldStaySeatedCommand() {
        return this.isTame() && this.getCommand() == 1;
    }
    // Animation initialization system (fixes T-pose on world rejoin with shaders)
    public boolean isClientAnimationReady() {
        return clientAnimInitTicks >= ANIM_INIT_GRACE_PERIOD;
    }

    private void tickGroundStepAudio() {
        if (groundStepSoundCooldownTicks > 0) {
            groundStepSoundCooldownTicks--;
        }
        if (isSleeping() || isSleepTransitioning() || isOrderedToSit() || areRiderControlsLocked() || !onGround() || isInWaterOrBubble()) {
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

    private void playEatMovingSound() {
        if (!level().isClientSide) {
            getSoundHandler().playMovingEntitySound(ModSounds.STEGONAUT_EAT.get(), 1.0f, isBaby() ? 1.6f : 1.0f, 22);
        }
    }

    // ===== CLIENT LOCATOR CACHE METHODS =====

    /**
     * Client-only: stash per-frame sampled locator world positions
     */
    public void setClientLocatorPosition(String name, Vec3 pos) {
        if (level().isClientSide) {
            this.clientLocatorCache.put(name, pos);
        }
    }

    /**
     * Client-only: retrieve cached locator world position
     */
    public Vec3 getClientLocatorPosition(String name) {
        return this.clientLocatorCache.get(name);
    }


    // ===== MOVEMENT STATE METHODS =====

    /**
     * Check if the drake is currently walking
     */
    public boolean isWalking() {
        if (level().isClientSide) {
            int s = getEffectiveGroundState();
            return s == 1; // walking state
        }
        int s = this.entityData.get(DATA_GROUND_MOVE_STATE);
        return s == 1; // walking state
    }

    /**
     * Check if the drake is currently running
     */
    public boolean isRunning() {
        return this.entityData.get(DATA_RUNNING);
    }

    @Override
    public void setRunning(boolean running) {
        this.entityData.set(DATA_RUNNING, running);
    }

    /**
     * Get the effective ground movement state (with client-side prediction)
     */
    public int getEffectiveGroundState() {
        if (level().isClientSide) {
            int syncedState = this.entityData.get(DATA_GROUND_MOVE_STATE);

            if (clientGroundMoveTarget != syncedState) {
                clientGroundMoveTarget = syncedState;
                clientGroundMoveHold = 0;
            } else if (clientGroundMoveState != clientGroundMoveTarget) {
                clientGroundMoveHold++;
                if (clientGroundMoveHold >= CLIENT_GROUND_STATE_STABLE_TICKS) {
                    clientGroundMoveState = clientGroundMoveTarget;
                    clientGroundMoveHold = 0;
                }
            } else {
                clientGroundMoveHold = 0;
            }

            if (isSleeping() || isOrderedToSit()) {
                clientGroundMoveState = 0;
                clientGroundMoveTarget = 0;
                clientGroundMoveHold = 0;
            }

            return clientGroundMoveState;
        }
        return this.entityData.get(DATA_GROUND_MOVE_STATE);
    }

    @Override
    public void tickAnimationStates() {
        if (this.isVehicle() && this.isOrderedToSit() && getSitProgress() <= 0.01f) {
            // Client can get stuck thinking we're sitting after mount; clear it so walk anim can play.
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

        int moveState = 0; // Default to idle

        boolean hasActivePath = this.getNavigation().isInProgress();
        double horizontalSpeed = this.getDeltaMovement().horizontalDistanceSqr();
        boolean isActuallyMoving = horizontalSpeed > 0.001;

        if (hasActivePath || isActuallyMoving) {
            // Determine walk vs run based on speed (run threshold: ~0.08 blocks/tick squared)
            if (horizontalSpeed > 0.0064) {
                moveState = 2; // Running
            } else {
                moveState = 1; // Walking
            }
            walkAnimationHoldTicks = 8; // Hold animation for 8 ticks after stopping
        } else if (walkAnimationHoldTicks > 0) {
            // Keep playing walk animation while decelerating
            moveState = 1;
            walkAnimationHoldTicks--;
        }

        // Update state only if changed to reduce network traffic
        if (this.entityData.get(DATA_GROUND_MOVE_STATE) != moveState) {
            this.entityData.set(DATA_GROUND_MOVE_STATE, moveState);
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

    public int getStegonautChestColumns() {
        return 5;
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

    // ===== SAVE/LOAD DATA =====

    @Override
    public void addAdditionalSaveData(net.minecraft.nbt.@NotNull CompoundTag tag) {
        super.addAdditionalSaveData(tag);

        tag.putInt("StegonautCommand", this.getCommand());
        tag.putBoolean("StegonautOrderedSit", this.isOrderedToSit());

        // Sleep state is ephemeral - not persisted (sleep goal re-evaluates on load)

        // Save grumble cooldown
        tag.putInt("GrumbleCooldown", grumbleCooldown);

        // Save binding state
        tag.putBoolean("BoundToBinder", boundToBinder);
        if (this.packLeaderUuid != null) {
            tag.putUUID("PackLeaderUuid", this.packLeaderUuid);
        }

        tag.putBoolean("StegonautHasChest", hasStegonautChest());
        if (hasStegonautChest()) {
            tag.put("StegonautChestItems", stegonautChestInventory.createTag());
        }

        // Save sit progress for animation state
        saveSitProgress(tag);
        saveRideableData(tag);
    }

    @Override
    public void readAdditionalSaveData(net.minecraft.nbt.@NotNull CompoundTag tag) {
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

        // Sleep state is ephemeral - not loaded (cleaned up below, sleep goal re-evaluates)

        // Load grumble cooldown
        grumbleCooldown = tag.getInt("GrumbleCooldown");

        // Load binding state
        boundToBinder = tag.getBoolean("BoundToBinder");
        this.packLeaderUuid = tag.hasUUID("PackLeaderUuid") ? tag.getUUID("PackLeaderUuid") : null;
        if (this.isTame()) {
            this.packLeaderUuid = null;
        }

        setStegonautChest(tag.getBoolean("StegonautHasChest"));
        if (hasStegonautChest() && tag.contains("StegonautChestItems", net.minecraft.nbt.Tag.TAG_LIST)) {
            stegonautChestInventory.fromTag(tag.getList("StegonautChestItems", net.minecraft.nbt.Tag.TAG_COMPOUND));
        }

        // Load sit progress for animation state prior to command refresh so poses align immediately
        if (tag.contains("SitProgress")) {
            setSitProgress(tag.getFloat("SitProgress"));
        }

        // Align baseline command state before restoring extra behaviors
        refreshCommandState();
        this.setOrderedToSit(restoredOrderedSit);

        // Don't force wake on chunk reload - let sleep behavior re-evaluate naturally (like Naturalist mod)
        // Sleep transition states are ephemeral and will be re-evaluated by DragonSleepBehavior
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
    public boolean canParticipateInPack() {
        if (this.isTame()) {
            return false;
        }
        if (this.isBaby() || this.isDying()) {
            return false;
        }
        if (!this.isAlive() || this.isRemoved()) {
            return false;
        }
        return !this.isOrderedToSit() && this.getCommand() != 1;
    }

    @Override
    public boolean canLeadPack() {
        // Alpha role is male-only by design.
        return canParticipateInPack() && !this.isFemale();
    }

    @Override
    public int getPackLeadershipPriority() {
        // Prefer healthier adults as alpha; deterministic UUID tie-break is handled in goal.
        return (int) Math.round((this.getHealth() / Math.max(1.0F, this.getMaxHealth())) * 100.0F);
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
    public int getPackLeaderRefreshIntervalTicks() {
        return 60;
    }

    @Override
    public boolean canTarget(net.minecraft.world.entity.Entity entity) {
        // Babies should not engage in combat targeting logic.
        if (this.isBaby()) {
            return false;
        }

        if (entity instanceof Stegonaut otherStegonaut) {
            if (otherStegonaut.isBaby()) {
                return false;
            }
            // Simplified pack mode: Stegonauts never target other Stegonauts.
            return false;
        }
        if (entity instanceof Player player && !this.isTame()) {
            // Wild Stegonauts should only start aggro from retaliation, but once locked onto a target,
            // keep pursuing that same player instead of dropping aggro mid-fight.
            return this.getLastHurtByMob() == player || this.getTarget() == player;
        }
        return super.canTarget(entity);
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

        if (!level().isClientSide) {
            float fwd = (float) net.minecraft.util.Mth.clamp(player.zza, -1.0F, 1.0F);
            float str = (float) net.minecraft.util.Mth.clamp(player.xxa, -1.0F, 1.0F);
            setLastRiderForward(Math.abs(fwd) > 0.02f ? fwd : 0f);
            setLastRiderStrafe(Math.abs(str) > 0.02f ? str : 0f);
            int moveState = 0;
            if (Math.abs(fwd) + Math.abs(str) > 0.05f) {
                moveState = this.isAccelerating() ? 2 : 1;
            } else {
                double speedSqr = getDeltaMovement().horizontalDistanceSqr();
                if (speedSqr > 0.005) {
                    moveState = 1;
                }
            }
            setGroundMoveStateFromAI(moveState);
            setRunning(moveState == 2 && !this.isInLove());
        }
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
            this.setSpeed(riderController.getRiddenSpeed(player));
            super.travel(motion);
            return;
        }

        super.travel(motion);
    }

    @Override
    protected void positionRider(@Nonnull @NotNull net.minecraft.world.entity.Entity passenger,
                                 @Nonnull @NotNull net.minecraft.world.entity.Entity.MoveFunction moveFunction) {
        riderController.positionRider(passenger, moveFunction);
    }

    @Override
    public @NotNull Vec3 getDismountLocationForPassenger(@NotNull net.minecraft.world.entity.LivingEntity passenger) {
        return riderController.getDismountLocationForPassenger(passenger);
    }

    @Override
    public @Nullable net.minecraft.world.entity.LivingEntity getControllingPassenger() {
        return riderController.getControllingPassenger();
    }

    @Override
    protected net.minecraft.network.syncher.EntityDataAccessor<Float> getRiderForwardAccessor() {
        return DATA_RIDER_FORWARD;
    }

    @Override
    protected net.minecraft.network.syncher.EntityDataAccessor<Float> getRiderStrafeAccessor() {
        return DATA_RIDER_STRAFE;
    }

    @Override
    protected net.minecraft.network.syncher.EntityDataAccessor<Integer> getGroundMoveStateAccessor() {
        return DATA_GROUND_MOVE_STATE;
    }

    @Override
    protected net.minecraft.network.syncher.EntityDataAccessor<Integer> getFlightModeAccessor() {
        return DATA_FLIGHT_MODE;
    }

    @Override
    protected net.minecraft.network.syncher.EntityDataAccessor<Boolean> getGoingUpAccessor() {
        return DATA_GOING_UP;
    }

    @Override
    protected net.minecraft.network.syncher.EntityDataAccessor<Boolean> getGoingDownAccessor() {
        return DATA_GOING_DOWN;
    }

    @Override
    protected net.minecraft.network.syncher.EntityDataAccessor<Boolean> getAcceleratingAccessor() {
        return DATA_ACCELERATING;
    }

    @Override
    protected int getFlightMode() {
        return -1;
    }

    @Override
    public boolean isTakeoff() {
        return false;
    }

    @Override
    public boolean isLanding() {
        return false;
    }

    @Override
    public boolean isHovering() {
        return false;
    }

    public boolean canBeBound() {
        return !isSleeping() && !isDying();
    }
}

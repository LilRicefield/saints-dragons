package com.leon.saintsdragons.server.entity.dragons.primitivedrake;

import com.leon.saintsdragons.server.ai.goals.primitivedrake.PrimitiveDrakePlayDeadGoal;
import com.leon.saintsdragons.server.entity.ability.abilities.primitivedrake.PrimitiveDrakePassiveBuffAbility;
import com.leon.saintsdragons.server.entity.base.DragonEntity;
import com.leon.saintsdragons.server.entity.dragons.primitivedrake.handlers.PrimitiveDrakeAnimationHandler;
import com.leon.saintsdragons.server.entity.handler.DragonSoundHandler;
import com.leon.saintsdragons.server.entity.interfaces.DragonSleepCapable;
import com.leon.saintsdragons.server.entity.interfaces.SoundHandledDragon;
import net.minecraft.core.Direction;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.FloatGoal;
import net.minecraft.world.entity.animal.Animal;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.core.BlockPos;
import net.minecraft.util.RandomSource;
import net.minecraft.world.phys.Vec3;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.AgeableMob;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import com.leon.saintsdragons.server.entity.ability.DragonAbilityType;
import com.leon.saintsdragons.common.registry.ModEntities;
import com.leon.saintsdragons.common.registry.ModSounds;
import com.leon.saintsdragons.common.registry.primitivedrake.PrimitiveDrakeAbilities;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageTypes;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.ItemStack;
import net.minecraft.network.chat.Component;
import org.jetbrains.annotations.NotNull;
import javax.annotation.Nonnull;
import software.bernie.geckolib.core.animatable.instance.AnimatableInstanceCache;
import software.bernie.geckolib.core.animation.AnimatableManager;
import software.bernie.geckolib.core.animation.AnimationController;
import software.bernie.geckolib.core.keyframe.event.SoundKeyframeEvent;
import software.bernie.geckolib.util.GeckoLibUtil;

/**
 * Primitive Drake - A simple ground drake that wanders around and runs away from players.
 * No complex abilities, just basic AI and cute behavior.
 * Features:
 * - Sleep behavior: Sleeps at night, awake during day. Simple nap system for short rests.
 * - Play dead behavior: Plays dead when lightning dragons are nearby
 * - Protective aura: Grants resistance and absorption to nearby players and allies
 * - NOT rideable: Too small and simple to be a mount
 */
public class PrimitiveDrakeEntity extends DragonEntity implements DragonSleepCapable, SoundHandledDragon {
    
    private final AnimatableInstanceCache cache = GeckoLibUtil.createInstanceCache(this);
    private final PrimitiveDrakeAnimationHandler animationController = new PrimitiveDrakeAnimationHandler(this);
    private final DragonSoundHandler soundHandler = new DragonSoundHandler(this);
    // Passive aura that applies resistance and absorption to allies
    private final PrimitiveDrakePassiveBuffAbility passiveBuffAbility =
            new PrimitiveDrakePassiveBuffAbility(this);
    
    // ===== CLIENT LOCATOR CACHE (client-side only) =====
    private final Map<String, Vec3> clientLocatorCache = new ConcurrentHashMap<>();

    // ===== VOCAL ENTRIES =====
    private static final Map<String, VocalEntry> VOCAL_ENTRIES = new VocalEntryBuilder()
            .add("primitive_drake_hurt", "action", "animation.primitive_drake.hurt", ModSounds.PRIMITIVE_DRAKE_HURT, 1.0f, 0.95f, 0.1f, false, true, true)
            .add("primitive_drake_die", "action", "animation.primitive_drake.die", ModSounds.PRIMITIVE_DRAKE_DIE, 1.2f, 1.0f, 0.0f, false, true, true)
            .build();

    @Override
    public Map<String, VocalEntry> getVocalEntries() {
        return VOCAL_ENTRIES;
    }

    // Sleep system fields
    private boolean sleeping = false;
    private boolean sleepTransitioning = false;
    private int napTicks = 0; // For short naps
    private int napCooldown = 0; // Cooldown between naps
    private boolean dayNapQueued = false;
    
    // Synced sleep state for client-side animation
    private static final net.minecraft.network.syncher.EntityDataAccessor<Boolean> DATA_SLEEPING = 
            net.minecraft.network.syncher.SynchedEntityData.defineId(PrimitiveDrakeEntity.class, net.minecraft.network.syncher.EntityDataSerializers.BOOLEAN);
    
    // Synced play dead state for client-side animation
    public static final net.minecraft.network.syncher.EntityDataAccessor<Boolean> DATA_PLAYING_DEAD = 
            net.minecraft.network.syncher.SynchedEntityData.defineId(PrimitiveDrakeEntity.class, net.minecraft.network.syncher.EntityDataSerializers.BOOLEAN);
    
    // Synced ground movement state for reliable animation
    private static final net.minecraft.network.syncher.EntityDataAccessor<Integer> DATA_GROUND_MOVE_STATE = 
            net.minecraft.network.syncher.SynchedEntityData.defineId(PrimitiveDrakeEntity.class, net.minecraft.network.syncher.EntityDataSerializers.INT);
    
    // Binding state for Drake Binder
    private boolean boundToBinder = false;

    // Client-side smoothing for ground movement states to avoid animation flicker when idle
    private static final int CLIENT_GROUND_STATE_STABLE_TICKS = 3;
    private int clientGroundMoveState = 0;
    private int clientGroundMoveTarget = 0;
    private int clientGroundMoveHold = 0;
    
    public PrimitiveDrakeEntity(EntityType<? extends PrimitiveDrakeEntity> entityType, Level level) {
        super(entityType, level);
        // Initialize animation state
        animationController.initializeAnimation();
    }
    
    @Override
    protected void defineSynchedData() {
        super.defineSynchedData();
        this.entityData.define(DATA_SLEEPING, false);
        this.entityData.define(DATA_PLAYING_DEAD, false);
        this.entityData.define(DATA_GROUND_MOVE_STATE, 0); // 0=idle, 1=walking, 2=running
    }
    
    @Override
    protected void registerGoals() {
        // Basic AI goals - simple and cute!
        this.goalSelector.addGoal(0, new FloatGoal(this));
        this.goalSelector.addGoal(0, new com.leon.saintsdragons.server.ai.goals.primitivedrake.PrimitiveDrakeSleepGoal(this)); // Highest priority - deep slumber takes precedence
        PrimitiveDrakePlayDeadGoal playDeadGoalInstance = new PrimitiveDrakePlayDeadGoal(this); // Second priority - play dead when lightning dragon nearby
        this.playDeadGoal = playDeadGoalInstance;
        this.goalSelector.addGoal(1, playDeadGoalInstance);
        this.goalSelector.addGoal(2, new com.leon.saintsdragons.server.ai.goals.primitivedrake.PrimitiveDrakeFollowOwnerGoal(this));
        this.goalSelector.addGoal(3, new com.leon.saintsdragons.server.ai.goals.primitivedrake.PrimitiveDrakeGroundWanderGoal(this, 0.35D, 120));
        this.goalSelector.addGoal(4, new com.leon.saintsdragons.server.ai.goals.primitivedrake.PrimitiveDrakeLookAtPlayerGoal(this, Player.class, 8.0F));
        this.goalSelector.addGoal(5, new com.leon.saintsdragons.server.ai.goals.primitivedrake.PrimitiveDrakeRandomLookAroundGoal(this));
    }
    
    public static AttributeSupplier.Builder createAttributes() {
        return Mob.createMobAttributes()
                .add(Attributes.MAX_HEALTH, 100.0D)
                .add(Attributes.MOVEMENT_SPEED, 0.40D)
                .add(Attributes.ATTACK_DAMAGE, 2.0D)
                .add(Attributes.FOLLOW_RANGE, 16.0D);
    }
    
    // Ground drake, no flying!
    public boolean isFlying() {
        return false;
    }
    
    /**
     * Check if the dragon is in a muted state (sitting/staying/playing dead)
     * Used by sound system to prevent ambient sounds
     */
    public boolean isStayOrSitMuted() {
        return this.isOrderedToSit() || this.isInSittingPose() || this.isPlayingDead();
    }
    
    /**
     * Primitive Drakes are NOT rideable - they're too small and simple
     */
    
    @Override
    public boolean canOwnerCommand(Player player) {
        // Primitive Drakes respond to commands from their owner without requiring crouching
        return player != null && player.equals(this.getOwner());
    }
    
    public boolean isTameable() {
        return true; // Can be tamed like other dragons
    }
    
    @Override
    public boolean isFood(@Nonnull net.minecraft.world.item.ItemStack stack) {
        // Simple food - maybe raw meat or fish?
        return stack.is(net.minecraft.world.item.Items.BEEF) || 
               stack.is(net.minecraft.world.item.Items.PORKCHOP) ||
               stack.is(net.minecraft.world.item.Items.CHICKEN) ||
               stack.is(net.minecraft.world.item.Items.MUTTON) ||
               stack.is(net.minecraft.world.item.Items.COD) ||
               stack.is(net.minecraft.world.item.Items.SALMON);
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
        // Simple drake has no abilities - just runs away!
        return null;
    }

    @Override
    protected DragonAbilityType<?, ?> getHurtAbilityType() {
        return PrimitiveDrakeAbilities.HURT;
    }

    @Override
    public boolean hurt(@NotNull DamageSource source, float amount) {
        // During dying sequence, ignore all damage except the final generic kill used by DieAbility
        if (isDying()) {
            if (source.is(DamageTypes.GENERIC_KILL)) {
                return super.hurt(source, amount);
            }
            return false;
        }

        // Intercept lethal damage to play custom death ability first
        if (handleLethalDamage(source, amount, PrimitiveDrakeAbilities.DIE)) {
            return true;
        }
        return super.hurt(source, amount);
    }

    @Override
    protected void tickDeath() {
        // Override vanilla death animation to prevent rotation/flop
        // Just increment death time without the rotation animation
        ++this.deathTime;
        if (this.deathTime >= 20) {
            this.remove(net.minecraft.world.entity.Entity.RemovalReason.KILLED);
            this.dropExperience();
        }
    }

    @Override
    public void registerControllers(AnimatableManager.ControllerRegistrar controllers) {
        // Use the new smooth animation controller
        AnimationController<PrimitiveDrakeEntity> movementController = new AnimationController<>(this, "movement", 1, animationController::handleMovementAnimation);
        movementController.setSoundKeyframeHandler(this::onAnimationSound);
        controllers.add(movementController);
        
        // Add action controller for grumble animations
        AnimationController<PrimitiveDrakeEntity> actionController = new AnimationController<>(this, "action", 1, animationController::actionPredicate);
        animationController.setupActionController(actionController);
        actionController.setSoundKeyframeHandler(this::onAnimationSound);
        controllers.add(actionController);
    }
    
    @Override
    public AnimatableInstanceCache getAnimatableInstanceCache() {
        return this.cache;
    }
    
    @Override
    public AgeableMob getBreedOffspring(@Nonnull ServerLevel level, @Nonnull AgeableMob other) {
        // Simple breeding - just spawn a new Primitive Drake
        return ModEntities.PRIMITIVE_DRAKE.get().create(level);
    }

    public static boolean canSpawnHere(EntityType<? extends PrimitiveDrakeEntity> type,
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
        // Prevent any mounting attempts - Primitive Drakes are NOT rideable
        if (hand == InteractionHand.MAIN_HAND && player.getItemInHand(hand).isEmpty()) {
            // If player is trying to mount (shift+right-click), deny it
            if (player.isShiftKeyDown()) {
                return InteractionResult.PASS; // Don't allow mounting
            }
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
            
            // 100% success chance for Primitive Drake!
            this.tame(player);
            this.setOrderedToSit(true);
            this.setCommand(1); // Set command to Sit (1) to match the sitting state
            
            // Re-evaluate play dead state when tamed
            if (this.isPlayingDead()) {
                // Check if there's still a reason to play dead (wild lightning dragon nearby)
                boolean shouldStillPlayDead = false;
                
                // Look for nearby lightning dragons
                var nearbyLightningDragons = this.level().getEntitiesOfClass(
                    com.leon.saintsdragons.server.entity.dragons.lightningdragon.LightningDragonEntity.class,
                    this.getBoundingBox().inflate(8.0), // Same range as play dead detection
                    dragon -> dragon != null && !dragon.isRemoved() && !dragon.isTame() // Only wild lightning dragons
                );
                
                if (!nearbyLightningDragons.isEmpty()) {
                    shouldStillPlayDead = true;
                }
                
                // If no wild lightning dragons nearby, stop playing dead
                if (!shouldStillPlayDead) {
                    this.clearPlayDeadGoal();
                }
                // If wild lightning dragons are nearby, let the goal continue (it will re-evaluate)
            }
            
            this.level().broadcastEntityEvent(this, (byte) 7); // Hearts particles
            
            // Send taming success message
            player.displayClientMessage(
                Component.translatable("entity.saintsdragons.primitive_drake.tamed", this.getName()),
                true
            );
            
            // Trigger advancement for taming Primitive Drake
            if (player instanceof net.minecraft.server.level.ServerPlayer serverPlayer) {
                var advancement = serverPlayer.server.getAdvancements()
                    .getAdvancement(com.leon.saintsdragons.SaintsDragons.rl("tame_primitive_drake"));
                if (advancement != null) {
                    serverPlayer.getAdvancements().award(advancement, "tame_primitive_drake");
                }
            }
        }
        
        return InteractionResult.sidedSuccess(this.level().isClientSide);
    }
    
    /**
     * Handle interactions with tamed drakes (feeding, commands)
     */
    private InteractionResult handleTamedInteraction(Player player, InteractionHand hand) {
        ItemStack itemstack = player.getItemInHand(hand);
        
        // Handle feeding for healing
        if (this.isFood(itemstack)) {
            return handleFeeding(player, itemstack);
        }
        
        // Handle owner commands
        if (player.equals(this.getOwner())) {
            // Command cycling - Shift+Right-click cycles through commands
            if (this.canOwnerCommand(player) && itemstack.isEmpty() && hand == InteractionHand.MAIN_HAND) {
                return handleCommandCycling(player);
            }
        }
        
        // Fall back to base implementation for other interactions
        return super.mobInteract(player, hand);
    }
    
    /**
     * Handle feeding tamed drakes for healing
     */
    private InteractionResult handleFeeding(Player player, ItemStack itemstack) {
        if (!this.level().isClientSide) {
            if (!player.getAbilities().instabuild) {
                itemstack.shrink(1);
            }
            
            // Heal the drake when fed
            float healAmount = 8.0f; // Heal 4 hearts per food item
            float oldHealth = this.getHealth();
            float newHealth = Math.min(oldHealth + healAmount, this.getMaxHealth());
            this.setHealth(newHealth);
            
            // Play eating sound and particles
            this.level().broadcastEntityEvent(this, (byte) 6); // Eating sound
            this.level().broadcastEntityEvent(this, (byte) 7); // Hearts particles
            
            // Send appropriate feedback message
            String messageKey = (newHealth >= this.getMaxHealth()) 
                ? "entity.saintsdragons.primitive_drake.fed"
                : "entity.saintsdragons.primitive_drake.fed_partial";
                
            player.displayClientMessage(
                Component.translatable(messageKey, this.getName()),
                true
            );
        }
        
        return InteractionResult.sidedSuccess(this.level().isClientSide);
    }
    
    /**
     * Handle command cycling (Follow/Sit/Wander)
     */
    private InteractionResult handleCommandCycling(Player player) {
        // Check if drake is playing dead - if so, show special message instead of cycling commands
        if (this.isPlayingDead()) {
            if (!this.level().isClientSide) {
                player.displayClientMessage(
                    Component.translatable("entity.saintsdragons.primitive_drake.busy_playing_dead", this.getName()),
                    true
                );
            }
            return InteractionResult.SUCCESS;
        }
        
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
    
    /**
     * Apply the command state to the drake
     */
    private void applyCommandState(int command) {
        switch (command) {
            case 0: // Follow
                this.setOrderedToSit(false);
                // Immediately reset sit progress when standing up
                this.sitProgress = 0f;
                this.getEntityData().set(DragonEntity.DATA_SIT_PROGRESS, 0f);
                break;
            case 1: // Sit
                this.setOrderedToSit(true);
                break;
            case 2: // Wander
                this.setOrderedToSit(false);
                // Immediately reset sit progress when standing up
                this.sitProgress = 0f;
                this.getEntityData().set(DragonEntity.DATA_SIT_PROGRESS, 0f);
                break;
        }
    }
    
    // ===== SOUND SYSTEM =====
    
    private int grumbleCooldown = 0;
    
    /**
     * Get the sound handler for this drake
     */
    public DragonSoundHandler getSoundHandler() {
        return soundHandler;
    }
    
    
    /**
     * Play random grumble sounds for personality
     */
    public void playRandomGrumble() {
        if (level().isClientSide || isDying()) return;
        
        float grumbleChance = getRandom().nextFloat();
        String vocalKey;
        String animationTrigger;
        
        if (grumbleChance < 0.4f) {
            vocalKey = "primitivedrake_grumble1";
            animationTrigger = "grumble1";
        } else if (grumbleChance < 0.7f) {
            vocalKey = "primitivedrake_grumble2";
            animationTrigger = "grumble2";
        } else {
            vocalKey = "primitivedrake_grumble3";
            animationTrigger = "grumble3";
        }
        
        // Play the grumble sound
        getSoundHandler().playVocal(vocalKey);
        
        // Trigger the corresponding animation
        triggerAnim("action", animationTrigger);
    }
    
    @Override
    public void aiStep() {
        super.aiStep();
        if (!this.level().isClientSide && this.pendingRestorePlayDead && this.playDeadGoal != null) {
            this.pendingRestorePlayDead = false;
            this.playDeadGoal.restorePlayDeadState(this.pendingRestorePlayDeadTicks, this.pendingRestoreCooldownTicks);
            this.pendingRestorePlayDeadTicks = 0;
            this.pendingRestoreCooldownTicks = 0;
        }
        
        // Update animation states
        if (!this.level().isClientSide) {
            tickAnimationStates();
        }
    }

    // ===== SLEEP SYSTEM IMPLEMENTATION =====
    
    @Override
    public boolean isSleeping() {
        // Use synced data for client-side animation detection
        return level().isClientSide ? this.entityData.get(DATA_SLEEPING) : sleeping;
    }
    
    @Override
    public boolean isSleepTransitioning() {
        return sleepTransitioning;
    }
    
    @Override
    public boolean isSleepSuppressed() {
        // Don't sleep while in combat, in water, or while being ridden
        return getTarget() != null || isInWaterOrBubble() || isVehicle();
    }
    
    @Override
    public void startSleepEnter() {
        if (!sleeping && !sleepTransitioning) {
            sleepTransitioning = true;
            sleeping = true;
            // Sync sleep state to client
            this.entityData.set(DATA_SLEEPING, true);
            // Simple sleep - just lie down, no complex transitions
            setOrderedToSit(true);
        }
    }
    
    @Override
    public void startSleepExit() {
        if (sleeping && !sleepTransitioning) {
            sleepTransitioning = true;
            sleeping = false;
            // Sync sleep state to client
            this.entityData.set(DATA_SLEEPING, false);
            // Wake up - stand up
            setOrderedToSit(false);
            sitProgress = 0f;
            getEntityData().set(DragonEntity.DATA_SIT_PROGRESS, 0f);
        }
    }
    
    @Override
    public SleepPreferences getSleepPreferences() {
        // Simple sleep preferences: sleep at night, awake during day
        return new SleepPreferences(
            true,  // canSleepAtNight
            true,  // canSleepDuringDay (for occasional naps)
            false, // requiresShelter (simple drake doesn't need shelter)
            false, // avoidsThunderstorms (not afraid of storms)
            false  // sleepsNearOwner (independent)
        );
    }
    
    @Override
    public boolean canSleepNow() {
        // Can sleep at night or take a nap during day
        if (level().isDay()) {
            // During day, only allow short naps if not on cooldown
            if (napCooldown <= 0 && getRandom().nextFloat() < 0.01f) {
                dayNapQueued = true;
                return true;
            }
            dayNapQueued = false;
            return false;
        } else {
            dayNapQueued = false;
            // At night, always allow sleep
            return true;
        }
    }
    
    @Override
    public void tick() {
        super.tick();
        
        // Tick sound handler
        soundHandler.tick();
        
        // Tick passive buff ability (only if alive)
        if (this.isAlive()) {
            passiveBuffAbility.tick();
        } else {
            // Clean up resistance buffs when dead
            passiveBuffAbility.cleanup();
        }
        
        // Handle sleep transition completion
        if (sleepTransitioning) {
            sleepTransitioning = false;
        }
        
        // Handle nap system
        if (napTicks > 0) {
            napTicks--;
            if (napTicks <= 0) {
                // Nap finished, wake up
                if (sleeping) {
                    startSleepExit();
                }
                napCooldown = 1200 + getRandom().nextInt(1800); // 1-2.5 minute cooldown
            }
        }
        
        // Handle nap cooldown
        if (napCooldown > 0) {
            napCooldown--;
        }
        
        // Handle grumble cooldown
        if (grumbleCooldown > 0) {
            grumbleCooldown--;
        } else if (!level().isClientSide && !isDying() && !isSleeping()) {
            // Random chance to grumble every tick (very low chance)
            if (getRandom().nextFloat() < 0.001f) { // 0.1% chance per tick
                playRandomGrumble();
                grumbleCooldown = 200 + getRandom().nextInt(400); // 10-30 second cooldown
            }
        }

        // Handle sit progress animation
        if (!level().isClientSide) {
            if (this.isOrderedToSit()) {
                if (sitProgress < maxSitTicks()) {
                    sitProgress++;
                    this.entityData.set(DATA_SIT_PROGRESS, sitProgress);
                }
            } else if (sitProgress > 0f) {
                sitProgress--;
                if (sitProgress < 0f) sitProgress = 0f;
                this.entityData.set(DATA_SIT_PROGRESS, sitProgress);
            }
        }

        // Update client-side sit progress from synchronized data
        if (level().isClientSide) {
            prevSitProgress = sitProgress;
            sitProgress = this.entityData.get(DATA_SIT_PROGRESS);
        }
    }
    
    /**
     * Start a short nap (1-2 minutes)
     */
    public void startNap() {
        if (!sleeping && napCooldown <= 0) {
            napTicks = 1200 + getRandom().nextInt(1200); // 1-2 minutes
        }
    }

    public boolean isDayNapQueued() {
        return dayNapQueued;
    }

    public boolean consumeDayNapQueued() {
        boolean queued = dayNapQueued;
        dayNapQueued = false;
        return queued;
    }

    
    // ===== SOUND KEYFRAME HANDLING =====
    
    /**
     * Handle sound keyframes from animations (for grumble sounds)
     */
    public void onAnimationSound(SoundKeyframeEvent<PrimitiveDrakeEntity> event) {
        // Delegate all keyframed sounds to the sound handler
        this.getSoundHandler().handleAnimationSound(this, event.getKeyframeData(), event.getController());
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
    
    // ===== PLAY DEAD BEHAVIOR SUPPORT =====
    
    // Reference to the play dead goal for easy access
    private com.leon.saintsdragons.server.ai.goals.primitivedrake.PrimitiveDrakePlayDeadGoal playDeadGoal;
    private boolean pendingRestorePlayDead = false;
    private int pendingRestorePlayDeadTicks = 0;
    private int pendingRestoreCooldownTicks = 0;
    
    /**
     * Set the play dead goal reference (called by the goal when it starts)
     */
    public void setPlayDeadGoal(com.leon.saintsdragons.server.ai.goals.primitivedrake.PrimitiveDrakePlayDeadGoal goal) {
        this.playDeadGoal = goal;
    }
    
    /**
     * Clear the play dead goal reference (called by the goal when it stops)
     */
    public void clearPlayDeadGoal() {
        if (this.playDeadGoal != null) {
            this.playDeadGoal.markSkipCooldownOnNextStop();
            this.playDeadGoal.stop();
        }
    }
    
    /**
     * Check if this drake is currently playing dead
     */
    public boolean isPlayingDead() {
        return playDeadGoal != null && playDeadGoal.isPlayingDead();
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
        if (level().isClientSide) {
            int s = getEffectiveGroundState();
            return s == 2; // running state
        }
        int s = this.entityData.get(DATA_GROUND_MOVE_STATE);
        return s == 2; // running state
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

            if (isSleeping() || isPlayingDead() || isOrderedToSit()) {
                clientGroundMoveState = 0;
                clientGroundMoveTarget = 0;
                clientGroundMoveHold = 0;
            }

            return clientGroundMoveState;
        }
        return this.entityData.get(DATA_GROUND_MOVE_STATE);
    }
    
    /**
     * Update animation states based on current movement
     */
    private void tickAnimationStates() {
        // Update ground movement state with more sophisticated detection
        int moveState = 0; // idle
        
        if (!isSleeping() && !isPlayingDead() && !isOrderedToSit()) {
            // Ground movement with refined thresholds
            double velSqr = this.getDeltaMovement().horizontalDistanceSqr();
            
            // Adjusted thresholds based on actual movement values observed in debug output
            final double WALK_MIN = 0.0003; // Lower threshold to match actual movement (velSqr ≈ 5.34E-4)
            final double RUN_MIN = 0.0020;  // Higher threshold for running
            
            
            if (velSqr > RUN_MIN) {
                moveState = 2; // running
            } else if (velSqr > WALK_MIN) {
                moveState = 1; // walking
            }
        }
        
        // Update entity data
        if (this.entityData.get(DATA_GROUND_MOVE_STATE) != moveState) {
            this.entityData.set(DATA_GROUND_MOVE_STATE, moveState);
        }
    }
    
    /**
     * Get the remaining play dead duration in ticks
     */
    public int getRemainingPlayDeadTicks() {
        return playDeadGoal != null ? playDeadGoal.getRemainingPlayDeadTicks() : 0;
    }
    
    /**
     * Get the remaining cooldown before can play dead again
     */
    public int getPlayDeadCooldownTicks() {
        return playDeadGoal != null ? playDeadGoal.getRemainingCooldownTicks() : 0;
    }
    
    // ===== SAVE/LOAD DATA =====
    
    @Override
    public void addAdditionalSaveData(net.minecraft.nbt.@NotNull CompoundTag tag) {
        super.addAdditionalSaveData(tag);

        // Save sleep state
        tag.putBoolean("Sleeping", sleeping);
        tag.putBoolean("SleepTransitioning", sleepTransitioning);
        tag.putInt("NapTicks", napTicks);
        tag.putInt("NapCooldown", napCooldown);

        // Save grumble cooldown
        tag.putInt("GrumbleCooldown", grumbleCooldown);

        // Save play dead state
        tag.putBoolean("PlayingDead", isPlayingDead());
        if (playDeadGoal != null) {
            tag.putInt("PlayDeadTicks", playDeadGoal.getRemainingPlayDeadTicks());
            tag.putInt("PlayDeadCooldown", playDeadGoal.getRemainingCooldownTicks());
        }

        // Save binding state
        tag.putBoolean("BoundToBinder", boundToBinder);

        // Save sit progress for animation state
        tag.putFloat("SitProgress", sitProgress);
    }
    
    @Override
    public void readAdditionalSaveData(net.minecraft.nbt.@NotNull CompoundTag tag) {
        super.readAdditionalSaveData(tag);

        // Load sleep state
        sleeping = tag.getBoolean("Sleeping");
        sleepTransitioning = tag.getBoolean("SleepTransitioning");
        napTicks = tag.getInt("NapTicks");
        napCooldown = tag.getInt("NapCooldown");

        // Load grumble cooldown
        grumbleCooldown = tag.getInt("GrumbleCooldown");

        // Load binding state
        boundToBinder = tag.getBoolean("BoundToBinder");

        // Sync sleep state to client
        this.entityData.set(DATA_SLEEPING, sleeping);

        // Load play dead state
        boolean wasPlayingDead = tag.getBoolean("PlayingDead");
        int savedPlayDeadTicks = tag.getInt("PlayDeadTicks");
        int savedCooldownTicks = tag.getInt("PlayDeadCooldown");

        // If we were playing dead when saved, restore that state
        if (wasPlayingDead && savedPlayDeadTicks > 0) {
            if (this.playDeadGoal != null) {
                this.playDeadGoal.restorePlayDeadState(savedPlayDeadTicks, savedCooldownTicks);
                this.pendingRestorePlayDead = false;
            } else {
                this.pendingRestorePlayDead = true;
                this.pendingRestorePlayDeadTicks = savedPlayDeadTicks;
                this.pendingRestoreCooldownTicks = savedCooldownTicks;
            }
        } else {
            this.pendingRestorePlayDead = false;
            this.pendingRestorePlayDeadTicks = 0;
            this.pendingRestoreCooldownTicks = 0;
            this.entityData.set(DATA_PLAYING_DEAD, false);
        }

        // Load sit progress for animation state
        if (tag.contains("SitProgress")) {
            sitProgress = tag.getFloat("SitProgress");
            this.entityData.set(DragonEntity.DATA_SIT_PROGRESS, sitProgress);
        }

        // Apply the loaded command state to ensure sit progress and other state is properly restored
        // This ensures consistency between command state and actual entity behavior
        applyCommandState(this.getCommand());

        // Explicitly enforce the sit state based on command to ensure it persists
        // This bypasses any potential timing issues with the base class
        int loadedCommand = this.getCommand();
        if (loadedCommand == 1) {
            // Command is Sit - explicitly set ordered to sit
            this.setOrderedToSit(true);
        } else {
            // Command is Follow (0) or Wander (2) - explicitly set not sitting
            this.setOrderedToSit(false);
        }
    }
    
    // ===== DRAKE BINDER FUNCTIONALITY =====
    
    /**
     * Check if this drake is bound to a Drake Binder
     */
    public boolean isBoundToBinder() {
        return boundToBinder;
    }
    
    /**
     * Set the binding state for Drake Binder
     */
    public void setBoundToBinder(boolean bound) {
        this.boundToBinder = bound;
    }
    
    /**
     * Check if this drake can be bound (not playing dead, not sleeping, etc.)
     */
    public boolean canBeBound() {
        return !isPlayingDead() && !isSleeping() && !isDying();
    }
}


package com.leon.saintsdragons.server.entity.dragons.primitivedrake;

import com.leon.saintsdragons.server.entity.base.DragonEntity;
import com.leon.saintsdragons.server.entity.dragons.primitivedrake.handlers.PrimitiveDrakeAnimationHandler;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.FloatGoal;
import net.minecraft.world.entity.ai.goal.LookAtPlayerGoal;
import net.minecraft.world.entity.ai.goal.RandomLookAroundGoal;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.core.BlockPos;
import net.minecraft.util.RandomSource;
import net.minecraft.world.phys.Vec3;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.AgeableMob;
import com.leon.saintsdragons.server.entity.ability.DragonAbilityType;
import com.leon.saintsdragons.common.registry.ModEntities;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.ItemStack;
import net.minecraft.network.chat.Component;
import org.jetbrains.annotations.NotNull;
import software.bernie.geckolib.core.animatable.instance.AnimatableInstanceCache;
import software.bernie.geckolib.core.animation.AnimatableManager;
import software.bernie.geckolib.core.animation.AnimationController;
import software.bernie.geckolib.util.GeckoLibUtil;

/**
 * Primitive Drake - A simple ground drake that wanders around and runs away from players.
 * No complex abilities, just basic AI and cute behavior.
 */
public class PrimitiveDrakeEntity extends DragonEntity {
    
    private final AnimatableInstanceCache cache = GeckoLibUtil.createInstanceCache(this);
    private final PrimitiveDrakeAnimationHandler animationController = new PrimitiveDrakeAnimationHandler(this);
    
    public PrimitiveDrakeEntity(EntityType<? extends PrimitiveDrakeEntity> entityType, Level level) {
        super(entityType, level);
        // Initialize animation state
        animationController.initializeAnimation();
    }
    
    @Override
    protected void registerGoals() {
        // Basic AI goals - simple and cute!
        this.goalSelector.addGoal(0, new FloatGoal(this));
        this.goalSelector.addGoal(1, new com.leon.saintsdragons.server.ai.goals.primitivedrake.PrimitiveDrakeFollowOwnerGoal(this));
        this.goalSelector.addGoal(2, new com.leon.saintsdragons.server.ai.goals.primitivedrake.PrimitiveDrakeGroundWanderGoal(this, 0.35D, 120));
        this.goalSelector.addGoal(3, new LookAtPlayerGoal(this, Player.class, 8.0F));
        this.goalSelector.addGoal(4, new RandomLookAroundGoal(this));
    }
    
    public static AttributeSupplier.Builder createAttributes() {
        return Mob.createMobAttributes()
                .add(Attributes.MAX_HEALTH, 20.0D)
                .add(Attributes.MOVEMENT_SPEED, 0.35D)
                .add(Attributes.ATTACK_DAMAGE, 2.0D)
                .add(Attributes.FOLLOW_RANGE, 16.0D);
    }
    
    // Ground drake, no flying!
    public boolean isFlying() {
        return false;
    }

    
    public boolean isTameable() {
        return true; // Can be tamed like other dragons
    }
    
    @Override
    public boolean isFood(@NotNull net.minecraft.world.item.ItemStack stack) {
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
    public void registerControllers(AnimatableManager.ControllerRegistrar controllers) {
        // Use the new smooth animation controller
        controllers.add(new AnimationController<>(this, "movement", 1, animationController::handleMovementAnimation));
    }
    
    @Override
    public AnimatableInstanceCache getAnimatableInstanceCache() {
        return this.cache;
    }
    
    @Override
    public AgeableMob getBreedOffspring(@NotNull ServerLevel level, @NotNull AgeableMob other) {
        // Simple breeding - just spawn a new Primitive Drake
        return ModEntities.PRIMITIVE_DRAKE.get().create(level);
    }
    
    public static boolean canSpawnHere(EntityType<? extends PrimitiveDrakeEntity> entityType, LevelAccessor level, MobSpawnType spawnType, BlockPos pos, RandomSource random) {
        // Use vanilla animal spawn rules for better compatibility
        return net.minecraft.world.entity.animal.Animal.checkAnimalSpawnRules(entityType, level, spawnType, pos, random);
    }
    
    // ===== INTERACTION HANDLING =====
    
    @Override
    public @NotNull InteractionResult mobInteract(@NotNull Player player, @NotNull InteractionHand hand) {
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
            this.level().broadcastEntityEvent(this, (byte) 7); // Hearts particles
            
            // Send taming success message
            player.displayClientMessage(
                Component.translatable("entity.saintsdragons.primitive_drake.tamed", this.getName()),
                true
            );
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
}
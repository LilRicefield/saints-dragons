package com.leon.saintsdragons.server.entity.ability.abilities.stegonaut;

import com.leon.saintsdragons.common.config.dragon.DragonAttributeConfigLoader;
import com.leon.saintsdragons.common.registry.ModSounds;
import com.leon.saintsdragons.server.entity.ability.DragonAbility;
import com.leon.saintsdragons.server.entity.ability.DragonAbilitySection;
import com.leon.saintsdragons.server.entity.ability.DragonAbilityType;
import com.leon.saintsdragons.server.entity.dragons.stegonaut.Stegonaut;
import com.leon.saintsdragons.server.entity.dragons.stegonaut.handlers.StegonautAnimationHandler;
import com.leon.saintsdragons.server.entity.effect.stegonaut.StegonautGroundChunkEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;

import static com.leon.saintsdragons.server.entity.ability.DragonAbilitySection.AbilitySectionInfinite;
import static com.leon.saintsdragons.server.entity.ability.DragonAbilitySection.AbilitySectionType.ACTIVE;

public class StegonautGroundEatingAbility extends DragonAbility<Stegonaut> {
    private static final DragonAbilitySection[] TRACK = new DragonAbilitySection[] {
            new AbilitySectionInfinite(ACTIVE)
    };

    private static final int COOLDOWN_TICKS = 30;
    private static final int CHARGE_ANIM_TICKS = 29; // 1.4583s
    private static final int HOLD_LOOP_TRIGGER_TICKS = 29;
    private static final int FIRE_RELEASE_TICKS = 18;
    private static final double PROJECTILE_SPEED = 2.5D;
    private static final int PROJECTILE_LIFETIME_TICKS = 80;
    private static final float PROJECTILE_SCALE = 1.2F;
    private static final double PROJECTILE_RADIUS = 3.2D;
    private static final float PROJECTILE_DAMAGE = 10.0F;
    private static final double TARGET_LEAD_FACTOR = 0.55D;
    private int chargeTicks = 0;
    private boolean holdLoopActive = false;
    private boolean releaseRequested = false;
    private boolean cancelRequested = false;
    private boolean resolved = false;
    private int releaseTicks = 0;
    private boolean shootAnimTriggered = false;

    public StegonautGroundEatingAbility(DragonAbilityType<Stegonaut, StegonautGroundEatingAbility> type, Stegonaut user) {
        super(type, user, TRACK, COOLDOWN_TICKS);
    }

    @Override
    protected void beginSection(DragonAbilitySection section) {
        if (section == null) {
            return;
        }
        if (section.sectionType == ACTIVE) {
            chargeTicks = 0;
            holdLoopActive = false;
            releaseRequested = false;
            cancelRequested = false;
            resolved = false;
            releaseTicks = 0;
            shootAnimTriggered = false;
            getUser().triggerAnim(StegonautAnimationHandler.ACTION_CONTROLLER, "ground_eating");
            if (!getUser().level().isClientSide) {
                getUser().getSoundHandler().playMovingEntitySound(ModSounds.STEGONAUT_GROUND_EATING.get(), 1.0f, getUser().isBaby() ? 1.6f : 1.0f, 37);
            }
        }
    }

    @Override
    protected boolean canContinueUsing() {
        Stegonaut dragon = getUser();
        return dragon.isAlive() && !dragon.isRemoved() && !dragon.isInWaterOrBubble();
    }

    @Override
    public void tickUsing() {
        if (resolved) {
            return;
        }

        if (releaseRequested) {
            if (cancelRequested) {
                getUser().triggerAnim(StegonautAnimationHandler.ACTION_CONTROLLER, "ground_eating_cancel");
                if (!getUser().level().isClientSide) {
                    getUser().getSoundHandler().playMovingEntitySound(ModSounds.STEGONAUT_GROUND_EATING_CANCEL.get(), 1.0f, getUser().isBaby() ? 1.6f : 1.0f, 22);
                }
                resolved = true;
                end();
                return;
            }

            if (!shootAnimTriggered) {
                getUser().triggerAnim(StegonautAnimationHandler.ACTION_CONTROLLER, "ground_eating_shoot");
                if (!getUser().level().isClientSide) {
                    getUser().getSoundHandler().playMovingEntitySound(ModSounds.STEGONAUT_GROUND_EATING_SHOOT.get(), 1.0f, getUser().isBaby() ? 1.6f : 1.0f, 75);
                }
                shootAnimTriggered = true;
                releaseTicks = 0;
            }

            releaseTicks++;
            if (releaseTicks >= FIRE_RELEASE_TICKS) {
                launchProjectile();
                resolved = true;
                end();
            }
            return;
        }

        chargeTicks++;
        if (!holdLoopActive && chargeTicks >= HOLD_LOOP_TRIGGER_TICKS) {
            getUser().triggerAnim(StegonautAnimationHandler.ACTION_CONTROLLER, "ground_eating_hold");
            holdLoopActive = true;
        }
    }

    @Override
    public void interrupt() {
        resetState();
        super.interrupt();
    }

    @Override
    public void end() {
        resetState();
        super.end();
    }

    public void requestRelease() {
        if (resolved || releaseRequested) {
            return;
        }
        releaseRequested = true;
        cancelRequested = chargeTicks < CHARGE_ANIM_TICKS;
        releaseTicks = 0;
    }

    public int getChargeTicks() {
        return chargeTicks;
    }

    public static int getChargeAnimationTicks() {
        return CHARGE_ANIM_TICKS;
    }

    private void launchProjectile() {
        Stegonaut dragon = getUser();
        if (!(dragon.level() instanceof ServerLevel server)) {
            return;
        }

        Vec3 direction = getAimDirection(dragon);
        Vec3 spawnPos = dragon.getGroundEatingProjectileOrigin().add(direction.scale(1.2D));
        BlockState blockState = resolveGroundBlockState(dragon);

        StegonautGroundChunkEntity projectile = new StegonautGroundChunkEntity(
                server, spawnPos, dragon, blockState, PROJECTILE_RADIUS, resolveDamage(), PROJECTILE_LIFETIME_TICKS
        );
        projectile.setVisualScale(PROJECTILE_SCALE);
        projectile.setDeltaMovement(direction.scale(PROJECTILE_SPEED));
        projectile.hasImpulse = true;
        server.addFreshEntity(projectile);
    }

    private static Vec3 getAimDirection(Stegonaut dragon) {
        Entity rider = dragon.getControllingPassenger();
        if (rider instanceof Player player) {
            Vec3 view = player.getViewVector(1.0f);
            if (view.lengthSqr() > 1.0E-6) {
                return view.normalize();
            }
        }
        if (dragon.getTarget() != null) {
            Vec3 targetPos = dragon.getTarget().getEyePosition();
            Vec3 lead = dragon.getTarget().getDeltaMovement().scale(TARGET_LEAD_FACTOR);
            Vec3 aimPoint = targetPos.add(lead);
            Vec3 dir = aimPoint.subtract(dragon.getGroundEatingProjectileOrigin());
            if (dir.lengthSqr() > 1.0E-6) {
                return dir.normalize();
            }
        }
        Vec3 look = dragon.getLookAngle();
        return look.lengthSqr() > 1.0E-6 ? look.normalize() : new Vec3(0.0D, 0.0D, 1.0D);
    }

    private static BlockState resolveGroundBlockState(Stegonaut dragon) {
        BlockPos below = dragon.blockPosition().below();
        BlockState state = dragon.level().getBlockState(below);
        if (state.isAir()) {
            return Blocks.DIRT.defaultBlockState();
        }
        return state;
    }

    private void resetState() {
        chargeTicks = 0;
        holdLoopActive = false;
        releaseRequested = false;
        cancelRequested = false;
        resolved = false;
        releaseTicks = 0;
        shootAnimTriggered = false;
    }

    private float resolveDamage() {
        return (float) DragonAttributeConfigLoader.getInstance()
                .getConfig(DragonAttributeConfigLoader.STEGONAUT_ID)
                .abilityDamage("ground_eating", PROJECTILE_DAMAGE);
    }
}

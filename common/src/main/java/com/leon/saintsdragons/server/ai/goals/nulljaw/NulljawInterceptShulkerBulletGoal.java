package com.leon.saintsdragons.server.ai.goals.nulljaw;

import com.leon.saintsdragons.server.entity.dragons.nulljaw.Nulljaw;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.monster.Shulker;
import net.minecraft.world.entity.projectile.ShulkerBullet;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;

import java.util.Comparator;
import java.util.EnumSet;

public final class NulljawInterceptShulkerBulletGoal extends Goal {
    private static final double SEARCH_RADIUS = 32.0D;
    private static final double MAX_DEFENDER_DISTANCE_FROM_OWNER_SQR = 48.0D * 48.0D;
    private static final double MAX_CHASE_DISTANCE_FROM_OWNER_SQR = 40.0D * 40.0D;
    private static final double LAST_CHANCE_INTERCEPT_DISTANCE_SQR = 8.0D * 8.0D;
    private static final double EAT_REACH = 1.25D;
    private static final double MOVE_SPEED = 1.35D;
    private static final int SEARCH_INTERVAL_TICKS = 4;
    private static final int MOVEMENT_REFRESH_TICKS = 2;
    private static final int EAT_COOLDOWN_TICKS = 10;

    private final Nulljaw dragon;
    @Nullable
    private ShulkerBullet projectile;
    private int searchCooldown;
    private int movementRefreshCooldown;
    private int eatCooldown;

    public NulljawInterceptShulkerBulletGoal(Nulljaw dragon) {
        this.dragon = dragon;
        this.setFlags(EnumSet.of(Flag.MOVE, Flag.LOOK));
    }

    @Override
    public boolean canUse() {
        if (this.eatCooldown > 0) {
            this.eatCooldown--;
            return false;
        }

        LivingEntity owner = getDefendedOwner();
        if (owner == null) {
            return false;
        }
        if (this.searchCooldown > 0) {
            this.searchCooldown--;
            return false;
        }

        this.searchCooldown = SEARCH_INTERVAL_TICKS;
        this.projectile = findThreateningProjectile(owner);
        return this.projectile != null;
    }

    @Override
    public boolean canContinueToUse() {
        LivingEntity owner = getDefendedOwner();
        return owner != null && isValidProjectile(this.projectile, owner);
    }

    @Override
    public void start() {
        this.movementRefreshCooldown = 0;
        this.dragon.beginAiFlight();
    }

    @Override
    public void tick() {
        ShulkerBullet bullet = this.projectile;
        LivingEntity owner = getDefendedOwner();
        if (!isValidProjectile(bullet, owner)) {
            return;
        }

        this.dragon.getLookControl().setLookAt(bullet, 100.0F, 100.0F);
        if (this.dragon.getBoundingBox().inflate(EAT_REACH).intersects(bullet.getBoundingBox())) {
            this.dragon.getAIMovement().stop();
            this.dragon.consumeShulkerBullet(bullet);
            this.projectile = null;
            this.eatCooldown = EAT_COOLDOWN_TICKS;
            return;
        }

        if (this.movementRefreshCooldown > 0) {
            this.movementRefreshCooldown--;
        }
        if (this.movementRefreshCooldown == 0 || !this.dragon.getAIMovement().isPathing()) {
            this.dragon.beginAiFlight();
            this.dragon.getAIMovement().setAsyncAirWaypoint(bullet.position(), MOVE_SPEED);
            this.movementRefreshCooldown = MOVEMENT_REFRESH_TICKS;
        }
    }

    @Override
    public void stop() {
        this.dragon.getAIMovement().stop();
        this.projectile = null;
        this.movementRefreshCooldown = 0;
    }

    @Nullable
    private LivingEntity getDefendedOwner() {
        LivingEntity owner = this.dragon.getOwner();
        if (!this.dragon.isTame()
                || this.dragon.isBaby()
                || this.dragon.isVehicle()
                || this.dragon.isPassenger()
                || this.dragon.isOrderedToSit()
                || owner == null
                || !owner.isAlive()
                || owner.level() != this.dragon.level()
                || this.dragon.distanceToSqr(owner) > MAX_DEFENDER_DISTANCE_FROM_OWNER_SQR) {
            return null;
        }
        return owner;
    }

    @Nullable
    private ShulkerBullet findThreateningProjectile(LivingEntity owner) {
        AABB searchBounds = owner.getBoundingBox().inflate(SEARCH_RADIUS);
        return this.dragon.level().getEntitiesOfClass(
                        ShulkerBullet.class,
                        searchBounds,
                        bullet -> isThreateningOwner(bullet, owner)
                ).stream()
                .min(Comparator.comparingDouble(bullet -> owner.distanceToSqr(bullet)))
                .orElse(null);
    }

    private boolean isThreateningOwner(ShulkerBullet bullet, LivingEntity owner) {
        if (!isValidProjectile(bullet, owner) || !(bullet.getOwner() instanceof Shulker shulker)) {
            return false;
        }
        if (shulker.getTarget() == owner) {
            return true;
        }

        Vec3 toOwner = owner.getBoundingBox().getCenter().subtract(bullet.position());
        return toOwner.lengthSqr() <= LAST_CHANCE_INTERCEPT_DISTANCE_SQR
                && bullet.getDeltaMovement().dot(toOwner) > 0.0D;
    }

    private boolean isValidProjectile(@Nullable ShulkerBullet bullet, @Nullable LivingEntity owner) {
        return bullet != null
                && owner != null
                && bullet.isAlive()
                && !bullet.isRemoved()
                && bullet.level() == this.dragon.level()
                && bullet.getOwner() instanceof Shulker
                && owner.distanceToSqr(bullet) <= MAX_CHASE_DISTANCE_FROM_OWNER_SQR;
    }
}

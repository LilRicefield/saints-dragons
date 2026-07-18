package com.leon.saintsdragons.server.ai.dragonbrain.behaviour.nulljaw;

import com.leon.saintsdragons.server.ai.dragonbrain.DragonBehaviour;
import com.leon.saintsdragons.server.ai.dragonbrain.DragonBrainContext;
import com.leon.saintsdragons.server.ai.dragonbrain.DragonMemories;
import com.leon.saintsdragons.server.entity.dragons.nulljaw.Nulljaw;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.monster.Shulker;
import net.minecraft.world.entity.projectile.ShulkerBullet;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;

import java.util.Comparator;
import java.util.Map;

public final class NulljawShulkerBulletSensorBehaviour extends DragonBehaviour<Nulljaw> {
    private static final double SEARCH_RADIUS = 32.0D;
    private static final double MAX_DEFENDER_DISTANCE_FROM_OWNER_SQR = 48.0D * 48.0D;
    private static final double MAX_CHASE_DISTANCE_FROM_OWNER_SQR = 40.0D * 40.0D;
    private static final double LAST_CHANCE_INTERCEPT_DISTANCE_SQR = 8.0D * 8.0D;
    private static final int SEARCH_INTERVAL_TICKS = 4;

    private int searchCooldown;
    private String state = "idle";

    public NulljawShulkerBulletSensorBehaviour() {
        super(false);
    }

    @Override
    protected boolean canStart(DragonBrainContext<Nulljaw> context) {
        return true;
    }

    @Override
    protected boolean canContinue(DragonBrainContext<Nulljaw> context) {
        return true;
    }

    @Override
    protected void tick(DragonBrainContext<Nulljaw> context) {
        Nulljaw dragon = context.dragon();
        Entity remembered = context.memories().get(DragonMemories.INTERCEPT_PROJECTILE).orElse(null);
        if (isValidThreat(dragon, remembered)) {
            state = "tracking";
            if (searchCooldown > 0) {
                searchCooldown--;
            }
            return;
        }

        context.memories().erase(DragonMemories.INTERCEPT_PROJECTILE);
        LivingEntity owner = defendedOwner(dragon);
        if (owner == null) {
            state = "no_owner";
            return;
        }
        if (searchCooldown-- > 0) {
            state = "cooldown";
            return;
        }

        searchCooldown = SEARCH_INTERVAL_TICKS;
        ShulkerBullet threat = findThreat(dragon, owner);
        if (threat == null) {
            state = "clear";
            return;
        }
        context.memories().set(DragonMemories.INTERCEPT_PROJECTILE, threat);
        state = "acquired";
    }

    public static boolean isValidThreat(Nulljaw dragon, @Nullable Entity entity) {
        LivingEntity owner = defendedOwner(dragon);
        if (!(entity instanceof ShulkerBullet bullet) || owner == null) {
            return false;
        }
        return bullet.isAlive()
                && !bullet.isRemoved()
                && bullet.level() == dragon.level()
                && bullet.getOwner() instanceof Shulker
                && owner.distanceToSqr(bullet) <= MAX_CHASE_DISTANCE_FROM_OWNER_SQR;
    }

    @Nullable
    private static LivingEntity defendedOwner(Nulljaw dragon) {
        LivingEntity owner = dragon.getOwner();
        if (!dragon.isTame()
                || dragon.isBaby()
                || dragon.isVehicle()
                || dragon.isPassenger()
                || dragon.isOrderedToSit()
                || owner == null
                || !owner.isAlive()
                || owner.level() != dragon.level()
                || dragon.distanceToSqr(owner) > MAX_DEFENDER_DISTANCE_FROM_OWNER_SQR) {
            return null;
        }
        return owner;
    }

    @Nullable
    private static ShulkerBullet findThreat(Nulljaw dragon, LivingEntity owner) {
        AABB bounds = owner.getBoundingBox().inflate(SEARCH_RADIUS);
        return dragon.level().getEntitiesOfClass(
                        ShulkerBullet.class,
                        bounds,
                        bullet -> threatensOwner(dragon, bullet, owner)
                ).stream()
                .min(Comparator.comparingDouble(owner::distanceToSqr))
                .orElse(null);
    }

    private static boolean threatensOwner(Nulljaw dragon, ShulkerBullet bullet, LivingEntity owner) {
        if (!isValidThreat(dragon, bullet) || !(bullet.getOwner() instanceof Shulker shulker)) {
            return false;
        }
        if (shulker.getTarget() == owner) {
            return true;
        }
        Vec3 toOwner = owner.getBoundingBox().getCenter().subtract(bullet.position());
        return toOwner.lengthSqr() <= LAST_CHANCE_INTERCEPT_DISTANCE_SQR
                && bullet.getDeltaMovement().dot(toOwner) > 0.0D;
    }

    @Override
    public Map<String, String> getDragonBrainDebugDetails() {
        return Map.of("state", state, "search_cooldown", Integer.toString(searchCooldown));
    }
}

package com.leon.saintsdragons.server.ai.dragonbrain.behaviour.nulljaw;

import com.leon.saintsdragons.common.registry.ModAbilities;
import com.leon.saintsdragons.server.ai.dragonbrain.DragonBehaviour;
import com.leon.saintsdragons.server.ai.dragonbrain.DragonBrainContext;
import com.leon.saintsdragons.server.ai.dragonbrain.DragonMemories;
import com.leon.saintsdragons.server.ai.dragonbrain.DragonMovementIntent;
import com.leon.saintsdragons.server.entity.dragons.nulljaw.Nulljaw;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.ShulkerBullet;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.Map;

public final class NulljawTacticalCombatBehaviour extends DragonBehaviour<Nulljaw> {
    private static final double EXTRA_BITE_REACH = 1.5D;
    private static final double WAYPOINT_CHANGE_DISTANCE_SQR = 16.0D;
    private static final double PROJECTILE_EAT_REACH = 1.25D;
    private static final double PROJECTILE_CHASE_SPEED = 1.35D;

    private int movementRefreshCooldown;
    @Nullable
    private Vec3 lastWaypoint;
    @Nullable
    private NulljawPackCombatCoordinator.Phase lastPhase;
    @Nullable
    private LivingEntity coordinatedTarget;
    private String mode = "idle";

    @Override
    protected boolean canStart(DragonBrainContext<Nulljaw> context) {
        return hasProjectile(context) || canChase(context.dragon(), attackTarget(context));
    }

    @Override
    protected boolean canContinue(DragonBrainContext<Nulljaw> context) {
        return hasProjectile(context) || canChase(context.dragon(), attackTarget(context));
    }

    @Override
    protected void start(DragonBrainContext<Nulljaw> context) {
        resetMovementTracking();
        context.dragon().beginAiFlight();
    }

    @Override
    protected void tick(DragonBrainContext<Nulljaw> context) {
        Entity projectile = context.memories().get(DragonMemories.INTERCEPT_PROJECTILE).orElse(null);
        if (NulljawShulkerBulletSensorBehaviour.isValidThreat(context.dragon(), projectile)) {
            tickProjectileIntercept(context, (ShulkerBullet) projectile);
            return;
        }
        context.memories().erase(DragonMemories.INTERCEPT_PROJECTILE);
        tickCoordinatedCombat(context, attackTarget(context));
    }

    private void tickProjectileIntercept(DragonBrainContext<Nulljaw> context, ShulkerBullet bullet) {
        Nulljaw dragon = context.dragon();
        if (!"intercept".equals(mode)) {
            leaveFormation(dragon);
            resetMovementTracking();
        }
        mode = "intercept";
        dragon.getLookControl().setLookAt(bullet, 100.0F, 100.0F);
        if (dragon.getBoundingBox().inflate(PROJECTILE_EAT_REACH).intersects(bullet.getBoundingBox())) {
            dragon.getAIMovement().stop();
            dragon.consumeShulkerBullet(bullet);
            context.memories().erase(DragonMemories.INTERCEPT_PROJECTILE);
            return;
        }
        if (movementRefreshCooldown > 0) {
            movementRefreshCooldown--;
        }
        if (movementRefreshCooldown <= 0 || !dragon.getAIMovement().isPathing()) {
            context.memories().set(
                    DragonMemories.MOVEMENT_INTENT,
                    DragonMovementIntent.strictAir(bullet.position(), PROJECTILE_CHASE_SPEED)
            );
            movementRefreshCooldown = 2;
        }
    }

    private void tickCoordinatedCombat(DragonBrainContext<Nulljaw> context,
                                       @Nullable LivingEntity target) {
        Nulljaw dragon = context.dragon();
        if (!canChase(dragon, target)) {
            return;
        }
        if (!"combat".equals(mode)) {
            resetMovementTracking();
        }
        mode = "combat";
        if (target != coordinatedTarget) {
            leaveFormation(dragon);
            coordinatedTarget = target;
            resetMovementTracking();
        }

        dragon.getLookControl().setLookAt(target, 100.0F, 100.0F);
        NulljawPackCombatCoordinator.Directive directive =
                NulljawPackCombatCoordinator.getDirective(dragon, target);
        if (directive.mayBite()
                && inBiteRange(dragon, target)
                && dragon.getSensing().hasLineOfSight(target)
                && dragon.combatManager.tryUseAbility(ModAbilities.NULLJAW_BITE)) {
            NulljawPackCombatCoordinator.markBiteStarted(dragon, target);
        }

        if (movementRefreshCooldown > 0) {
            movementRefreshCooldown--;
        }
        Vec3 waypoint = directive.waypoint();
        boolean phaseChanged = directive.phase() != lastPhase;
        boolean waypointChanged = lastWaypoint == null
                || lastWaypoint.distanceToSqr(waypoint) >= WAYPOINT_CHANGE_DISTANCE_SQR;
        if (phaseChanged
                || movementRefreshCooldown <= 0
                || !dragon.getAIMovement().isPathing()
                || waypointChanged) {
            context.memories().set(
                    DragonMemories.MOVEMENT_INTENT,
                    DragonMovementIntent.strictAir(waypoint, speedFor(directive.phase()))
            );
            lastWaypoint = waypoint;
            lastPhase = directive.phase();
            movementRefreshCooldown = refreshTicksFor(directive.phase());
        }
    }

    @Override
    protected void stop(DragonBrainContext<Nulljaw> context) {
        Nulljaw dragon = context.dragon();
        leaveFormation(dragon);
        context.memories().erase(DragonMemories.MOVEMENT_INTENT);
        dragon.getAIMovement().stop();
        mode = "idle";
        resetMovementTracking();
    }

    @Override
    public List<net.minecraft.world.entity.ai.memory.MemoryModuleType<?>> clearMemoriesWhenStopped() {
        return List.of(DragonMemories.MOVEMENT_INTENT);
    }

    @Nullable
    private LivingEntity attackTarget(DragonBrainContext<Nulljaw> context) {
        return context.memories().get(DragonMemories.ATTACK_TARGET).orElse(null);
    }

    private boolean hasProjectile(DragonBrainContext<Nulljaw> context) {
        Entity projectile = context.memories().get(DragonMemories.INTERCEPT_PROJECTILE).orElse(null);
        return NulljawShulkerBulletSensorBehaviour.isValidThreat(context.dragon(), projectile);
    }

    private boolean canChase(Nulljaw dragon, @Nullable LivingEntity target) {
        if (dragon.isBaby()
                || dragon.isVehicle()
                || dragon.isPassenger()
                || dragon.isOrderedToSit()
                || !validTarget(dragon, target)) {
            return false;
        }
        double range = Math.max(16.0D, dragon.getAttributeValue(Attributes.FOLLOW_RANGE));
        return dragon.distanceToSqr(target) <= range * range;
    }

    private boolean validTarget(Nulljaw dragon, @Nullable LivingEntity target) {
        return target != null
                && target.isAlive()
                && target.level() == dragon.level()
                && target.attackable()
                && !dragon.isAlly(target)
                && (!(target instanceof Player player) || !player.isCreative() && !player.isSpectator());
    }

    private boolean inBiteRange(Nulljaw dragon, LivingEntity target) {
        double reach = dragon.getBbWidth() * 0.5D
                + target.getBbWidth() * 0.5D
                + EXTRA_BITE_REACH;
        return dragon.distanceToSqr(target) <= reach * reach;
    }

    private void leaveFormation(Nulljaw dragon) {
        NulljawPackCombatCoordinator.leave(dragon, coordinatedTarget);
        coordinatedTarget = null;
    }

    private void resetMovementTracking() {
        movementRefreshCooldown = 0;
        lastWaypoint = null;
        lastPhase = null;
    }

    private double speedFor(NulljawPackCombatCoordinator.Phase phase) {
        return switch (phase) {
            case ORBIT -> 0.9D;
            case STAGE -> 1.1D;
            case DIVE -> 1.4D;
            case EGRESS -> 1.3D;
        };
    }

    private int refreshTicksFor(NulljawPackCombatCoordinator.Phase phase) {
        return switch (phase) {
            case ORBIT -> 8;
            case STAGE, EGRESS -> 5;
            case DIVE -> 2;
        };
    }

    @Override
    public Map<String, String> getDragonBrainDebugDetails() {
        return Map.of(
                "mode", mode,
                "target", coordinatedTarget == null ? "none" : coordinatedTarget.getName().getString(),
                "phase", lastPhase == null ? "none" : lastPhase.name(),
                "move_refresh", Integer.toString(movementRefreshCooldown)
        );
    }
}

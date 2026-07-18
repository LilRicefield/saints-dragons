package com.leon.saintsdragons.server.ai.dragonbrain.behaviour.nulljaw;

import com.leon.saintsdragons.server.ai.dragonbrain.DragonBrainContext;
import com.leon.saintsdragons.server.ai.dragonbrain.behaviour.DragonTargetingBehaviour;
import com.leon.saintsdragons.server.entity.dragons.nulljaw.Nulljaw;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.monster.Shulker;
import net.minecraft.world.entity.player.Player;
import org.jetbrains.annotations.Nullable;

import java.util.Comparator;
import java.util.List;

public final class NulljawTargetingBehaviour extends DragonTargetingBehaviour<Nulljaw> {
    private int shulkerPollCooldown;

    @Nullable
    @Override
    protected TargetChoice findPriorityTarget(DragonBrainContext<Nulljaw> context) {
        Nulljaw dragon = context.dragon();
        LivingEntity assigned = dragon.getTarget();
        if (isUsableTarget(dragon, assigned)) {
            return targetChoice(assigned, "assigned", 0);
        }

        if (shulkerPollCooldown-- > 0) {
            return null;
        }
        shulkerPollCooldown = 10;
        double range = followRange(dragon);
        List<Shulker> candidates = context.level().getEntitiesOfClass(
                Shulker.class,
                dragon.getBoundingBox().inflate(range),
                shulker -> isUsableTarget(dragon, shulker)
                        && dragon.getSensing().hasLineOfSight(shulker)
        );
        return candidates.stream()
                .min(Comparator.comparingDouble(dragon::distanceToSqr))
                .map(shulker -> targetChoice(shulker, "shulker_hunt", 1))
                .orElse(null);
    }

    @Override
    protected boolean canAcquireTargets(Nulljaw dragon) {
        return dragon.isAlive()
                && !dragon.isDying()
                && !dragon.isBaby()
                && !dragon.isVehicle()
                && !dragon.isPassenger()
                && !dragon.isOrderedToSit();
    }

    @Override
    protected boolean isUsableTarget(Nulljaw dragon, @Nullable LivingEntity target) {
        if (target == null
                || !target.isAlive()
                || target.level() != dragon.level()
                || !target.attackable()
                || dragon.isAlly(target)) {
            return false;
        }
        return !(target instanceof Player player) || !player.isCreative() && !player.isSpectator();
    }

    @Override
    protected boolean canRetainTarget(Nulljaw dragon, LivingEntity target, String source) {
        double range = followRange(dragon);
        return dragon.distanceToSqr(target) <= range * range;
    }

    private double followRange(Nulljaw dragon) {
        return Math.max(16.0D, dragon.getAttributeValue(Attributes.FOLLOW_RANGE));
    }
}

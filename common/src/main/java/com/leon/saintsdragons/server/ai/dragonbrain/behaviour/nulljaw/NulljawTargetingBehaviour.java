package com.leon.saintsdragons.server.ai.dragonbrain.behaviour.nulljaw;

import com.leon.saintsdragons.server.ai.dragonbrain.DragonBrainContext;
import com.leon.saintsdragons.server.ai.dragonbrain.DragonTargetLifecycle;
import com.leon.saintsdragons.server.ai.dragonbrain.behaviour.DragonTargetingBehaviour;
import com.leon.saintsdragons.server.entity.dragons.nulljaw.Nulljaw;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.monster.Shulker;
import org.jetbrains.annotations.Nullable;


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
        Shulker shulker = dragon.getVision().nearest(Shulker.class, candidate -> isUsableTarget(dragon, candidate));
        return shulker == null ? null : targetChoice(shulker, "shulker_hunt", 1);
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
        if (!DragonTargetLifecycle.isValidTarget(dragon, target)
                || !target.attackable()
                || dragon.isAlly(target)) {
            return false;
        }
        return true;
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

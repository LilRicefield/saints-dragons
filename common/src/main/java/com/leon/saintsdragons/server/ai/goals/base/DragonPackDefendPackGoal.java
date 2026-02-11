package com.leon.saintsdragons.server.ai.goals.base;

import com.leon.saintsdragons.server.entity.base.DragonEntity;
import com.leon.saintsdragons.server.entity.interfaces.PackMember;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.phys.AABB;
import org.jetbrains.annotations.Nullable;

import java.util.EnumSet;
import java.util.List;
import java.util.UUID;

/**
 * Shares retaliation target information across nearby pack members.
 * Alpha and followers can mirror valid threats from one another.
 */
public class DragonPackDefendPackGoal<T extends DragonEntity & PackMember<T>> extends Goal {
    private final T member;
    private final Class<T> memberClass;
    private final double assistRadiusSq;
    private int retargetCooldown;
    @Nullable
    private LivingEntity pendingTarget;

    public DragonPackDefendPackGoal(T member, Class<T> memberClass, double assistRadius) {
        this.member = member;
        this.memberClass = memberClass;
        this.assistRadiusSq = assistRadius * assistRadius;
        this.retargetCooldown = 0;
        this.setFlags(EnumSet.of(Flag.TARGET));
    }

    @Override
    public boolean canUse() {
        if (!canAssistPack()) {
            clearInvalidCombatTarget();
            return false;
        }
        if (retargetCooldown > 0) {
            retargetCooldown--;
            return false;
        }

        pendingTarget = findAssistTarget();
        return member.isTargetValid(pendingTarget) && member.canTarget(pendingTarget);
    }

    @Override
    public void start() {
        if (member.isTargetValid(pendingTarget) && member.canTarget(pendingTarget)) {
            member.setTarget(pendingTarget);
            // Cooldown to prevent target thrashing while the same threat is active.
            retargetCooldown = 20 + member.getRandom().nextInt(20);
        }
    }

    @Override
    public boolean canContinueToUse() {
        if (!canAssistPack()) {
            clearInvalidCombatTarget();
            return false;
        }
        LivingEntity current = member.getTarget();
        if (!member.isTargetValid(current) || !member.canTarget(current)) {
            return false;
        }
        if (member.distanceToSqr(current) > assistRadiusSq * 4.0D) {
            return false;
        }
        return true;
    }

    @Override
    public void stop() {
        clearInvalidCombatTarget();
    }

    private boolean canAssistPack() {
        if (!member.canParticipateInPack()) {
            member.setPackLeaderUuid(null);
            return false;
        }
        if (member.isOrderedToSit() || member.isVehicle() || member.isPassenger()) {
            return false;
        }
        return true;
    }

    @Nullable
    private LivingEntity findAssistTarget() {
        if (!(member.level() instanceof ServerLevel serverLevel)) {
            return null;
        }

        LivingEntity target = firstValidThreat(member.getLastHurtByMob(), member.getTarget());
        if (target != null) {
            return target;
        }

        T leader = resolveLeader(serverLevel);
        if (leader != null) {
            target = firstValidThreat(leader.getTarget(), leader.getLastHurtByMob());
            if (target != null) {
                return target;
            }
        }

        double radius = Math.max(8.0D, member.getPackSearchRadius());
        AABB box = member.getBoundingBox().inflate(radius);
        List<T> nearby = serverLevel.getEntitiesOfClass(memberClass, box, this::isCompatiblePackmate);
        for (T packmate : nearby) {
            target = firstValidThreat(packmate.getTarget(), packmate.getLastHurtByMob());
            if (target != null) {
                return target;
            }
        }

        return null;
    }

    @Nullable
    private T resolveLeader(ServerLevel serverLevel) {
        UUID leaderUuid = member.getPackLeaderUuid();
        if (leaderUuid == null) {
            return null;
        }
        Entity entity = serverLevel.getEntity(leaderUuid);
        if (entity == null || !memberClass.isInstance(entity)) {
            return null;
        }
        T leader = memberClass.cast(entity);
        return isCompatiblePackmate(leader) ? leader : null;
    }

    private boolean isCompatiblePackmate(T other) {
        if (other == null || other == member) {
            return false;
        }
        if (!other.canParticipateInPack()) {
            return false;
        }
        if (member.isTame() != other.isTame()) {
            return false;
        }
        if (member.isTame()) {
            var owner = member.getOwner();
            return owner != null && other.isOwnedBy(owner);
        }
        return true;
    }

    @Nullable
    private LivingEntity firstValidThreat(@Nullable LivingEntity first, @Nullable LivingEntity second) {
        if (member.isTargetValid(first) && member.canTarget(first)) {
            return first;
        }
        if (member.isTargetValid(second) && member.canTarget(second)) {
            return second;
        }
        return null;
    }

    private void clearInvalidCombatTarget() {
        LivingEntity current = member.getTarget();
        if (current == null) {
            return;
        }
        if (!member.isTargetValid(current) || !member.canTarget(current)) {
            member.setTarget(null);
        }
    }
}

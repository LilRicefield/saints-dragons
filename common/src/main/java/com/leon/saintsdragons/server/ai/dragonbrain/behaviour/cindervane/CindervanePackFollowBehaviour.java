package com.leon.saintsdragons.server.ai.dragonbrain.behaviour.cindervane;

import com.leon.saintsdragons.common.config.dragon.profile.CindervaneStatProfile;
import com.leon.saintsdragons.server.ai.dragonbrain.DragonBrainContext;
import com.leon.saintsdragons.server.ai.dragonbrain.DragonMemories;
import com.leon.saintsdragons.server.ai.dragonbrain.behaviour.DragonPackFollowBehaviour;
import com.leon.saintsdragons.server.entity.dragons.cindervane.Cindervane;
import org.jetbrains.annotations.Nullable;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;

public final class CindervanePackFollowBehaviour extends DragonPackFollowBehaviour<Cindervane> {
    @Nullable
    private CindervanePackFlightCoordinator.FollowPlan plan;
    private long nextUpdate;

    public CindervanePackFollowBehaviour() {
        super(Cindervane.class, 1.0D, 20.0D, 10.0D);
    }

    @Override
    protected boolean usableLeader(Cindervane member, @Nullable Cindervane candidate) {
        if (candidate == null || !super.usableLeader(member, candidate)) return false;
        UUID parent = candidate.getPackLeaderUuid();
        return parent == null || parent.equals(candidate.getUUID());
    }

    @Override
    protected boolean maintainsAirFormation(Cindervane member, Cindervane leader) {
        return member.isAerial() && !member.isLanding() && leader.isAerial() && !leader.isLanding();
    }

    @Override
    protected boolean followAirFormation(DragonBrainContext<Cindervane> context,
                                         Cindervane member, Cindervane leader) {
        if (!maintainsAirFormation(member, leader)) {
            plan = null;
            return false;
        }
        if (plan != null && plan.leader().equals(leader.getUUID()) && context.gameTime() < nextUpdate) {
            return true;
        }
        plan = leader.getPackFlightCoordinator().plan(context.level(), leader, member);
        nextUpdate = context.gameTime() + CindervaneStatProfile.PackFlightCoordinator.UPDATE_INTERVAL_TICKS;
        context.memories().erase(DragonMemories.MOVEMENT_INTENT);
        member.setAccelerating(plan.catchUp());
        if (plan.settled()) {
            member.getAIMovement().stop();
        } else {
            member.getAIMovement().requestFlight(plan.request());
        }
        return true;
    }

    @Override
    protected void start(DragonBrainContext<Cindervane> context) {
        super.start(context);
        plan = null;
        nextUpdate = 0L;
    }

    @Override
    protected void stop(DragonBrainContext<Cindervane> context) {
        super.stop(context);
        plan = null;
        nextUpdate = 0L;
    }

    @Override
    public Map<String, String> getDragonBrainDebugDetails() {
        Map<String, String> details = new LinkedHashMap<>(super.getDragonBrainDebugDetails());
        details.put("formation", plan != null && "formation".equals(details.get("mode"))
                ? plan.debugSummary() : "inactive");
        return Map.copyOf(details);
    }
}

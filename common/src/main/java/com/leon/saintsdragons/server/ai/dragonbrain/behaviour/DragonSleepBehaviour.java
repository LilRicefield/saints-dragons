package com.leon.saintsdragons.server.ai.dragonbrain.behaviour;

import com.leon.saintsdragons.server.ai.dragonbrain.DragonBehaviour;
import com.leon.saintsdragons.server.ai.dragonbrain.DragonBrainContext;
import com.leon.saintsdragons.server.entity.base.DragonEntity;

import java.util.LinkedHashMap;
import java.util.Map;

public final class DragonSleepBehaviour<T extends DragonEntity> extends DragonBehaviour<T> {
    private String decision = "unsupported";
    private String disturbanceCause = "none";
    private float pressure;
    private float disturbance;
    private float wakeDisturbance;

    public DragonSleepBehaviour() {
        super(false);
    }

    @Override
    protected boolean canStart(DragonBrainContext<T> context) {
        return true;
    }

    @Override
    protected boolean canContinue(DragonBrainContext<T> context) {
        return true;
    }

    @Override
    protected void start(DragonBrainContext<T> context) {
        update(context);
    }

    @Override
    protected void tick(DragonBrainContext<T> context) {
        update(context);
    }

    private void update(DragonBrainContext<T> context) {
        T dragon = context.dragon();
        dragon.tickBrainSleepBehaviour();
        pressure = dragon.getSleepPressure();
        disturbance = dragon.getSleepDisturbance();
        wakeDisturbance = dragon.getSleepDisturbanceThreshold();
        disturbanceCause = dragon.getSleepDisturbanceCause();
        decision = dragon.getSleepDecision();
    }

    @Override
    public Map<String, String> getDragonBrainDebugDetails() {
        Map<String, String> details = new LinkedHashMap<>();
        details.put("sleepPressure", String.format(java.util.Locale.ROOT, "%.1f", pressure));
        details.put("sleepDisturbance", String.format(
                java.util.Locale.ROOT,
                "%.1f/%.1f",
                disturbance,
                wakeDisturbance
        ));
        details.put("sleepDisturbanceCause", disturbanceCause);
        details.put("sleepDecision", decision);
        return details;
    }
}

package com.leon.saintsdragons.server.ai.dragonbrain;

public final class DragonBrainUtilities {
    private final DragonNearbyEntities nearby = new DragonNearbyEntities();
    private final DragonDestinationMemory destinations = new DragonDestinationMemory();
    private final DragonBehaviourResources resources = new DragonBehaviourResources();

    public DragonNearbyEntities nearby() {
        return nearby;
    }

    public DragonDestinationMemory destinations() {
        return destinations;
    }

    public DragonBehaviourResources resources() {
        return resources;
    }

    public void clear() {
        nearby.invalidate();
        destinations.clear();
        resources.clear();
    }
}

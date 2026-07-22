package com.leon.saintsdragons.server.ai.dragonbrain;

import com.leon.saintsdragons.common.registry.ModMemoryTypes;
import com.leon.saintsdragons.server.entity.base.DragonLocomotionMode;
import com.leon.saintsdragons.server.ai.dragonbrain.perception.DragonSensoryObservation;
import com.leon.saintsdragons.server.ai.dragonbrain.tactical.DragonTacticalCommitment;
import net.minecraft.core.GlobalPos;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.memory.MemoryModuleType;
import net.minecraft.world.entity.ai.memory.WalkTarget;
import net.minecraft.world.entity.ai.behavior.PositionTracker;
import net.minecraft.world.level.pathfinder.Path;
import net.minecraft.world.phys.Vec3;
import java.util.List;

public final class DragonMemories {
    public static final MemoryModuleType<LivingEntity> ATTACK_TARGET = MemoryModuleType.ATTACK_TARGET;
    public static final MemoryModuleType<WalkTarget> WALK_TARGET = MemoryModuleType.WALK_TARGET;
    public static final MemoryModuleType<Path> PATH = MemoryModuleType.PATH;
    public static final MemoryModuleType<Long> CANT_REACH_WALK_TARGET_SINCE = MemoryModuleType.CANT_REACH_WALK_TARGET_SINCE;
    public static final MemoryModuleType<PositionTracker> LOOK_TARGET = MemoryModuleType.LOOK_TARGET;
    public static final MemoryModuleType<GlobalPos> HOME = MemoryModuleType.HOME;
    public static final MemoryModuleType<Boolean> TARGET_AIRBORNE = ModMemoryTypes.TARGET_AIRBORNE.get();
    public static final MemoryModuleType<Boolean> IS_AERIAL = ModMemoryTypes.IS_AERIAL.get();
    public static final MemoryModuleType<Boolean> IS_GROUNDED = ModMemoryTypes.IS_GROUNDED.get();
    public static final MemoryModuleType<DragonLocomotionMode> LOCOMOTION_MODE = ModMemoryTypes.LOCOMOTION_MODE.get();
    public static final MemoryModuleType<Boolean> IS_RIDDEN = ModMemoryTypes.IS_RIDDEN.get();
    public static final MemoryModuleType<Boolean> IN_WATER = ModMemoryTypes.IN_WATER.get();
    public static final MemoryModuleType<Boolean> IN_LAVA = ModMemoryTypes.IN_LAVA.get();
    public static final MemoryModuleType<DragonMovementIntent> MOVEMENT_INTENT = ModMemoryTypes.MOVEMENT_INTENT.get();
    public static final MemoryModuleType<Entity> INTERCEPT_PROJECTILE = ModMemoryTypes.INTERCEPT_PROJECTILE.get();
    public static final MemoryModuleType<Boolean> GROUND_ROUTE_ABANDONED = ModMemoryTypes.GROUND_ROUTE_ABANDONED.get();
    public static final MemoryModuleType<Vec3> TACTICAL_LANDING_POSITION = ModMemoryTypes.TACTICAL_LANDING_POSITION.get();
    public static final MemoryModuleType<GlobalPos> ROOST_SLEEP_POSITION = ModMemoryTypes.ROOST_SLEEP_POSITION.get();
    public static final MemoryModuleType<Boolean> TARGET_VISIBLE = ModMemoryTypes.TARGET_VISIBLE.get();
    public static final MemoryModuleType<DragonSensoryObservation> LAST_SEEN_TARGET =
            ModMemoryTypes.LAST_SEEN_TARGET.get();
    public static final MemoryModuleType<DragonSensoryObservation> INVESTIGATION_TARGET =
            ModMemoryTypes.INVESTIGATION_TARGET.get();
    public static final MemoryModuleType<DragonSensoryObservation> HEARD_STIMULUS =
            ModMemoryTypes.HEARD_STIMULUS.get();
    public static final MemoryModuleType<DragonSensoryObservation> HEARD_TARGET =
            ModMemoryTypes.HEARD_TARGET.get();
    public static final MemoryModuleType<LivingEntity> WAKE_TARGET = ModMemoryTypes.WAKE_TARGET.get();
    public static final MemoryModuleType<Float> SLEEP_PRESSURE = ModMemoryTypes.SLEEP_PRESSURE.get();
    public static final MemoryModuleType<Boolean> SLEEP_INTENT = ModMemoryTypes.SLEEP_INTENT.get();
    public static final MemoryModuleType<LivingEntity> RESCUE_TARGET = ModMemoryTypes.RESCUE_TARGET.get();
    public static final MemoryModuleType<DragonTacticalCommitment> TACTICAL_COMMITMENT =
            ModMemoryTypes.TACTICAL_COMMITMENT.get();

    private DragonMemories() {
    }

    public static List<MemoryModuleType<?>> all() {
        return List.of(
                ATTACK_TARGET,
                WALK_TARGET,
                PATH,
                CANT_REACH_WALK_TARGET_SINCE,
                LOOK_TARGET,
                HOME,
                TARGET_AIRBORNE,
                IS_AERIAL,
                IS_GROUNDED,
                LOCOMOTION_MODE,
                IS_RIDDEN,
                IN_WATER,
                IN_LAVA,
                MOVEMENT_INTENT,
                INTERCEPT_PROJECTILE,
                GROUND_ROUTE_ABANDONED,
                TACTICAL_LANDING_POSITION,
                ROOST_SLEEP_POSITION,
                TARGET_VISIBLE,
                LAST_SEEN_TARGET,
                INVESTIGATION_TARGET,
                HEARD_STIMULUS,
                HEARD_TARGET,
                WAKE_TARGET,
                SLEEP_PRESSURE,
                SLEEP_INTENT,
                RESCUE_TARGET,
                TACTICAL_COMMITMENT
        );
    }
}

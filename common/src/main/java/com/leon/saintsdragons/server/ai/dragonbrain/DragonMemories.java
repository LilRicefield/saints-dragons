package com.leon.saintsdragons.server.ai.dragonbrain;

import com.leon.saintsdragons.common.registry.ModMemoryTypes;
import com.leon.saintsdragons.server.entity.base.DragonLocomotionMode;
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
    public static final MemoryModuleType<Boolean> TARGET_AIRBORNE = ModMemoryTypes.TARGET_AIRBORNE.get();
    public static final MemoryModuleType<Boolean> IS_AERIAL = ModMemoryTypes.IS_AERIAL.get();
    public static final MemoryModuleType<Boolean> IS_GROUNDED = ModMemoryTypes.IS_GROUNDED.get();
    public static final MemoryModuleType<DragonLocomotionMode> LOCOMOTION_MODE = ModMemoryTypes.LOCOMOTION_MODE.get();
    public static final MemoryModuleType<Boolean> IS_RIDDEN = ModMemoryTypes.IS_RIDDEN.get();
    public static final MemoryModuleType<Boolean> IN_WATER = ModMemoryTypes.IN_WATER.get();
    public static final MemoryModuleType<Boolean> IN_LAVA = ModMemoryTypes.IN_LAVA.get();
    public static final MemoryModuleType<DragonMovementIntent> MOVEMENT_INTENT = ModMemoryTypes.MOVEMENT_INTENT.get();
    public static final MemoryModuleType<Boolean> GROUND_ROUTE_ABANDONED = ModMemoryTypes.GROUND_ROUTE_ABANDONED.get();
    public static final MemoryModuleType<Vec3> TACTICAL_LANDING_POSITION = ModMemoryTypes.TACTICAL_LANDING_POSITION.get();

    private DragonMemories() {
    }

    public static List<MemoryModuleType<?>> all() {
        return List.of(
                ATTACK_TARGET,
                WALK_TARGET,
                PATH,
                CANT_REACH_WALK_TARGET_SINCE,
                LOOK_TARGET,
                TARGET_AIRBORNE,
                IS_AERIAL,
                IS_GROUNDED,
                LOCOMOTION_MODE,
                IS_RIDDEN,
                IN_WATER,
                IN_LAVA,
                MOVEMENT_INTENT,
                GROUND_ROUTE_ABANDONED,
                TACTICAL_LANDING_POSITION
        );
    }
}

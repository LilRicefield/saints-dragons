package com.leon.saintsdragons.common.registry;

import com.leon.saintsdragons.common.SaintsDragonsCommon;
import com.leon.saintsdragons.platform.RegistryHelper;
import com.leon.saintsdragons.platform.Services;
import com.leon.saintsdragons.server.ai.dragonbrain.DragonMovementIntent;
import com.leon.saintsdragons.server.ai.dragonbrain.perception.DragonAwarenessMemory;
import com.leon.saintsdragons.server.ai.dragonbrain.perception.DragonSensoryObservation;
import com.leon.saintsdragons.server.ai.dragonbrain.tactical.DragonTacticalCommitment;
import com.leon.saintsdragons.server.entity.base.DragonLocomotionMode;
import net.minecraft.core.GlobalPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.memory.MemoryModuleType;
import net.minecraft.world.phys.Vec3;

import java.util.Optional;
import java.util.function.Supplier;

public final class ModMemoryTypes {
    private static final RegistryHelper.RegistryWrapper<MemoryModuleType<?>> REGISTER =
            Services.PLATFORM.getRegistryHelper()
                    .create(Registries.MEMORY_MODULE_TYPE, () -> BuiltInRegistries.MEMORY_MODULE_TYPE,
                            SaintsDragonsCommon.MOD_ID);

    public static final Supplier<MemoryModuleType<Boolean>> TARGET_AIRBORNE = register("target_airborne");
    public static final Supplier<MemoryModuleType<Boolean>> IS_AERIAL = register("is_aerial");
    public static final Supplier<MemoryModuleType<Boolean>> IS_GROUNDED = register("is_grounded");
    public static final Supplier<MemoryModuleType<DragonLocomotionMode>> LOCOMOTION_MODE = register("locomotion_mode");
    public static final Supplier<MemoryModuleType<Boolean>> IS_RIDDEN = register("is_ridden");
    public static final Supplier<MemoryModuleType<Boolean>> IN_WATER = register("in_water");
    public static final Supplier<MemoryModuleType<Boolean>> IN_LAVA = register("in_lava");
    public static final Supplier<MemoryModuleType<DragonMovementIntent>> MOVEMENT_INTENT = register("movement_intent");
    public static final Supplier<MemoryModuleType<Entity>> INTERCEPT_PROJECTILE = register("intercept_projectile");
    public static final Supplier<MemoryModuleType<Boolean>> GROUND_ROUTE_ABANDONED = register("ground_route_abandoned");
    public static final Supplier<MemoryModuleType<Vec3>> TACTICAL_LANDING_POSITION = register("tactical_landing_position");
    public static final Supplier<MemoryModuleType<GlobalPos>> ROOST_SLEEP_POSITION = REGISTER.register(
            "roost_sleep_position",
            () -> new MemoryModuleType<>(Optional.of(GlobalPos.CODEC))
    );
    public static final Supplier<MemoryModuleType<Boolean>> TARGET_VISIBLE = register("target_visible");
    public static final Supplier<MemoryModuleType<DragonSensoryObservation>> LAST_SEEN_TARGET =
            register("last_seen_target");
    public static final Supplier<MemoryModuleType<DragonSensoryObservation>> HEARD_STIMULUS =
            register("heard_stimulus");
    public static final Supplier<MemoryModuleType<DragonSensoryObservation>> HEARD_TARGET =
            register("heard_target");
    public static final Supplier<MemoryModuleType<DragonAwarenessMemory>> AWARENESS =
            register("awareness");
    public static final Supplier<MemoryModuleType<LivingEntity>> WAKE_TARGET = register("wake_target");
    public static final Supplier<MemoryModuleType<Float>> SLEEP_PRESSURE = register("sleep_pressure");
    public static final Supplier<MemoryModuleType<Boolean>> SLEEP_INTENT = register("sleep_intent");
    public static final Supplier<MemoryModuleType<LivingEntity>> RESCUE_TARGET = register("rescue_target");
    public static final Supplier<MemoryModuleType<DragonTacticalCommitment>> TACTICAL_COMMITMENT =
            register("tactical_commitment");
    public static final Supplier<MemoryModuleType<DragonSensoryObservation>> INVESTIGATION_TARGET =
            register("investigation_target");
    public static final Supplier<MemoryModuleType<DragonSensoryObservation>> SCENT_CANDIDATE =
            register("scent_candidate");
    public static final Supplier<MemoryModuleType<Boolean>> SCENT_COOLDOWN = register("scent_cooldown");

    private ModMemoryTypes() {
    }

    private static <T> Supplier<MemoryModuleType<T>> register(String name) {
        return REGISTER.register(name, () -> new MemoryModuleType<>(Optional.empty()));
    }

    public static void register() {
        REGISTER.register();
    }
}

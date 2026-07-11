package com.leon.saintsdragons.common.registry;

import com.leon.saintsdragons.common.SaintsDragonsCommon;
import com.leon.saintsdragons.platform.RegistryHelper;
import com.leon.saintsdragons.platform.Services;
import com.leon.saintsdragons.server.ai.dragonbrain.sensor.DragonMovementStateSensor;
import com.leon.saintsdragons.server.ai.dragonbrain.sensor.DragonTargetSensor;
import com.leon.saintsdragons.server.entity.base.DragonEntity;
import com.leon.saintsdragons.server.entity.base.RideableDragonBase;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.entity.ai.sensing.SensorType;

import java.util.function.Supplier;

public final class ModSensorTypes {
    private static final RegistryHelper.RegistryWrapper<SensorType<?>> REGISTER =
            Services.PLATFORM.getRegistryHelper()
                    .create(Registries.SENSOR_TYPE, () -> BuiltInRegistries.SENSOR_TYPE,
                            SaintsDragonsCommon.MOD_ID);

    public static final Supplier<SensorType<DragonTargetSensor<DragonEntity>>> DRAGON_TARGET =
            REGISTER.register("dragon_target", () -> new SensorType<>(() -> new DragonTargetSensor<>(2, 2.0D)));
    public static final Supplier<SensorType<DragonMovementStateSensor<RideableDragonBase>>> DRAGON_MOVEMENT_STATE =
            REGISTER.register("dragon_movement_state", () -> new SensorType<>(() -> new DragonMovementStateSensor<>(2)));

    private ModSensorTypes() {
    }

    public static void register() {
        REGISTER.register();
    }
}

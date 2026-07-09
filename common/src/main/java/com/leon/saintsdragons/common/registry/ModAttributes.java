package com.leon.saintsdragons.common.registry;

import com.leon.saintsdragons.common.SaintsDragonsCommon;
import com.leon.saintsdragons.platform.RegistryHelper;
import com.leon.saintsdragons.platform.Services;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.RangedAttribute;

import java.util.function.Supplier;

public final class ModAttributes {
    public static final RegistryHelper.RegistryWrapper<Attribute> REGISTER =
            Services.PLATFORM.getRegistryHelper()
                    .create(Registries.ATTRIBUTE, () -> BuiltInRegistries.ATTRIBUTE, SaintsDragonsCommon.MOD_ID);

    public static final Supplier<Attribute> DOUBLE_JUMP =
            REGISTER.register("double_jump",
                    () -> new RangedAttribute("attribute.name.saintsdragons.double_jump", 0.0D, 0.0D, 1.0D)
                            .setSyncable(true));

    public static final Supplier<Attribute> FIRE_RESISTANCE =
            REGISTER.register("fire_resistance",
                    () -> new RangedAttribute("attribute.name.saintsdragons.fire_resistance", 0.0D, 0.0D, 100.0D)
                            .setSyncable(true));

    public static final Supplier<Attribute> BLAST_RESISTANCE =
            REGISTER.register("blast_resistance",
                    () -> new RangedAttribute("attribute.name.saintsdragons.blast_resistance", 0.0D, 0.0D, 100.0D)
                            .setSyncable(true));

    private ModAttributes() {
    }

    public static void register() {
        REGISTER.register();
    }
}

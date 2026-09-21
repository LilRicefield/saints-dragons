package com.leon.saintsdragons.common.registry;

import com.leon.saintsdragons.common.SaintsDragonsCommon;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageType;
import net.minecraft.world.entity.LivingEntity;

public final class ModDamageTypes {
    public static final ResourceKey<DamageType> RAEVYX_LIGHTNING = ResourceKey.create(
            Registries.DAMAGE_TYPE, SaintsDragonsCommon.rl("raevyx_lightning"));

    private ModDamageTypes() {
    }

    public static DamageSource raevyxLightning(LivingEntity attacker) {
        return new DamageSource(attacker.level().registryAccess()
                .registryOrThrow(Registries.DAMAGE_TYPE).getHolderOrThrow(RAEVYX_LIGHTNING), attacker);
    }
}

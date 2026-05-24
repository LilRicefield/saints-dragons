package com.leon.saintsdragons.common.config.dragon;

import net.minecraft.util.GsonHelper;
import com.google.gson.JsonObject;
import org.jetbrains.annotations.Nullable;

public record DragonAbilityOverride(@Nullable Double damage) {

    public static DragonAbilityOverride ofDamage(double damage) {
        return new DragonAbilityOverride(damage);
    }

    public static DragonAbilityOverride merge(JsonObject json, @Nullable DragonAbilityOverride fallback) {
        Double base = fallback != null ? fallback.damage : null;
        if (json.has("damage")) {
            base = GsonHelper.getAsDouble(json, "damage");
        }
        return new DragonAbilityOverride(base);
    }

    public double damageOr(double fallback) {
        return damage != null ? damage : fallback;
    }
}

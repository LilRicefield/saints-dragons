package com.leon.saintsdragons.common.config.dragon;

import net.minecraft.util.GsonHelper;
import com.google.gson.JsonObject;
import org.jetbrains.annotations.Nullable;

public record DragonAbilityOverride(@Nullable Double damage,
                                    @Nullable Double knockback,
                                    @Nullable Double secondaryKnockback,
                                    @Nullable Double stunDurationTicks,
                                    @Nullable Boolean enabled) {

    public static DragonAbilityOverride ofDamage(double damage) {
        return new DragonAbilityOverride(damage, null, null, null, null);
    }

    public static DragonAbilityOverride ofTuning(@Nullable Double damage,
                                                 @Nullable Double knockback,
                                                 @Nullable Double secondaryKnockback,
                                                 @Nullable Double stunDurationTicks,
                                                 @Nullable Boolean enabled) {
        return new DragonAbilityOverride(damage, knockback, secondaryKnockback, stunDurationTicks, enabled);
    }

    public static DragonAbilityOverride merge(JsonObject json, @Nullable DragonAbilityOverride fallback) {
        Double damage = fallback != null ? fallback.damage : null;
        Double knockback = fallback != null ? fallback.knockback : null;
        Double secondaryKnockback = fallback != null ? fallback.secondaryKnockback : null;
        Double stunDurationTicks = fallback != null ? fallback.stunDurationTicks : null;
        Boolean enabled = fallback != null ? fallback.enabled : null;
        if (json.has("damage")) {
            damage = GsonHelper.getAsDouble(json, "damage");
        }
        if (json.has("knockback")) {
            knockback = GsonHelper.getAsDouble(json, "knockback");
        }
        if (json.has("secondary_knockback")) {
            secondaryKnockback = GsonHelper.getAsDouble(json, "secondary_knockback");
        }
        if (json.has("stun_duration_ticks")) {
            stunDurationTicks = GsonHelper.getAsDouble(json, "stun_duration_ticks");
        }
        if (json.has("enabled")) {
            enabled = GsonHelper.getAsBoolean(json, "enabled");
        }
        return new DragonAbilityOverride(damage, knockback, secondaryKnockback, stunDurationTicks, enabled);
    }

    public double damageOr(double fallback) {
        return damage != null ? damage : fallback;
    }

    public double knockbackOr(double fallback) {
        return knockback != null ? knockback : fallback;
    }

    public double secondaryKnockbackOr(double fallback) {
        return secondaryKnockback != null ? secondaryKnockback : fallback;
    }

    public int stunDurationTicksOr(int fallback) {
        return stunDurationTicks != null ? Math.max(0, (int)Math.round(stunDurationTicks)) : fallback;
    }

    public boolean enabledOr(boolean fallback) {
        return enabled != null ? enabled : fallback;
    }
}

package com.leon.saintsdragons.common.config.dragon;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import net.minecraft.util.GsonHelper;
import org.jetbrains.annotations.Nullable;

import java.util.HashMap;
import java.util.Map;

public record DragonAttributeConfig(double maxHealth, double armor, double flyingSpeed,
                                    Map<String, DragonAbilityOverride> abilities, Map<String, Double> extraDoubles,
                                    Map<String, Boolean> extraBooleans) {
    public static final DragonAttributeConfig EMPTY = new DragonAttributeConfig(
            40.0D,
            0.0D,
            0.3D,
            Map.of(),
            Map.of(),
            Map.of()
    );

    public DragonAttributeConfig(double maxHealth,
                                 double armor,
                                 double flyingSpeed,
                                 Map<String, DragonAbilityOverride> abilities,
                                 Map<String, Double> extraDoubles,
                                 Map<String, Boolean> extraBooleans) {
        this.maxHealth = maxHealth;
        this.armor = armor;
        this.flyingSpeed = flyingSpeed;
        this.abilities = Map.copyOf(abilities);
        Map<String, Double> normalizedExtras = new HashMap<>(extraDoubles);
        Double tamingStunHealth = normalizedExtras.get("taming_stun_health");
        if (tamingStunHealth != null) {
            double maximumThreshold = Math.max(0.0D, maxHealth - 1.0D);

            Double phase2TriggerFraction = normalizedExtras.get("ultimate_trigger_health_fraction");
            if (phase2TriggerFraction != null) {
                double normalizedFraction = Math.max(0.0D, Math.min(phase2TriggerFraction, 1.0D));
                if (normalizedFraction > 0.0D) {
                    // Ignivorus must cross its phase-two boundary before taming stun can intercept damage.
                    maximumThreshold = Math.min(maximumThreshold,
                            Math.max(0.0D, maxHealth * normalizedFraction - 1.0D));
                }
            }

            normalizedExtras.put("taming_stun_health",
                    Math.max(0.0D, Math.min(tamingStunHealth, maximumThreshold)));
        }
        this.extraDoubles = Map.copyOf(normalizedExtras);
        this.extraBooleans = Map.copyOf(extraBooleans);
    }

    public double abilityDamage(String key, double fallback) {
        DragonAbilityOverride override = abilities.get(key);
        return override != null ? override.damageOr(fallback) : fallback;
    }

    public double abilityKnockback(String key, double fallback) {
        DragonAbilityOverride override = abilities.get(key);
        return override != null ? override.knockbackOr(fallback) : fallback;
    }

    public double abilitySecondaryKnockback(String key, double fallback) {
        DragonAbilityOverride override = abilities.get(key);
        return override != null ? override.secondaryKnockbackOr(fallback) : fallback;
    }

    public int abilityStunDurationTicks(String key, int fallback) {
        DragonAbilityOverride override = abilities.get(key);
        return override != null ? override.stunDurationTicksOr(fallback) : fallback;
    }

    public boolean abilityEnabled(String key, boolean fallback) {
        DragonAbilityOverride override = abilities.get(key);
        return override != null ? override.enabledOr(fallback) : fallback;
    }

    public double extraDouble(String key, double fallback) {
        return extraDoubles.getOrDefault(key, fallback);
    }

    public boolean extraBoolean(String key, boolean fallback) {
        return extraBooleans.getOrDefault(key, fallback);
    }

    public static DragonAttributeConfig merge(JsonObject json, @Nullable DragonAttributeConfig fallback) {
        DragonAttributeConfig base = fallback != null ? fallback : EMPTY;

        double maxHealth = GsonHelper.getAsDouble(json, "max_health", base.maxHealth);
        double armor = GsonHelper.getAsDouble(json, "armor", base.armor);
        double flyingSpeed = GsonHelper.getAsDouble(json, "flying_speed", base.flyingSpeed);

        Map<String, DragonAbilityOverride> abilityMap = new HashMap<>(base.abilities);
        if (json.has("abilities")) {
            JsonObject abilitiesJson = GsonHelper.getAsJsonObject(json, "abilities");
            for (Map.Entry<String, JsonElement> entry : abilitiesJson.entrySet()) {
                JsonObject overrideJson = GsonHelper.convertToJsonObject(entry.getValue(), entry.getKey());
                DragonAbilityOverride override = DragonAbilityOverride.merge(
                        overrideJson,
                        abilityMap.get(entry.getKey())
                );
                abilityMap.put(entry.getKey(), override);
            }
        }
        Map<String, Double> extra = new HashMap<>(base.extraDoubles);
        Map<String, Boolean> booleans = new HashMap<>(base.extraBooleans);
        if (json.has("extra")) {
            JsonObject extraJson = GsonHelper.getAsJsonObject(json, "extra");
            for (Map.Entry<String, JsonElement> entry : extraJson.entrySet()) {
                JsonElement value = entry.getValue();
                if (value.isJsonPrimitive()) {
                    var primitive = value.getAsJsonPrimitive();
                    if (primitive.isBoolean()) {
                        booleans.put(entry.getKey(), primitive.getAsBoolean());
                    } else if (primitive.isNumber()) {
                        extra.put(entry.getKey(), primitive.getAsDouble());
                    }
                }
            }
        }
        if (json.has("extra_booleans")) {
            JsonObject booleansJson = GsonHelper.getAsJsonObject(json, "extra_booleans");
            for (Map.Entry<String, JsonElement> entry : booleansJson.entrySet()) {
                booleans.putIfAbsent(entry.getKey(), GsonHelper.convertToBoolean(entry.getValue(), entry.getKey()));
            }
        }

        return new DragonAttributeConfig(maxHealth, armor, flyingSpeed, abilityMap, extra, booleans);
    }
}

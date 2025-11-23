package com.leon.saintsdragons.common.config.dragon;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import net.minecraft.util.GsonHelper;
import org.jetbrains.annotations.Nullable;

import java.util.HashMap;
import java.util.Map;

/**
 * Data-driven attribute bundle for a dragon species.
 */
public final class DragonAttributeConfig {
    public static final DragonAttributeConfig EMPTY = new DragonAttributeConfig(
            40.0D,
            0.0D,
            0.3D,
            0.3D,
            Map.of(),
            Map.of()
    );

    private final double maxHealth;
    private final double armor;
    private final double movementSpeed;
    private final double flyingSpeed;
    private final Map<String, DragonAbilityOverride> abilities;
    private final Map<String, Double> extraDoubles;

    public DragonAttributeConfig(double maxHealth,
                                 double armor,
                                 double movementSpeed,
                                 double flyingSpeed,
                                 Map<String, DragonAbilityOverride> abilities,
                                 Map<String, Double> extraDoubles) {
        this.maxHealth = maxHealth;
        this.armor = armor;
        this.movementSpeed = movementSpeed;
        this.flyingSpeed = flyingSpeed;
        this.abilities = Map.copyOf(abilities);
        this.extraDoubles = Map.copyOf(extraDoubles);
    }

    public double maxHealth() {
        return maxHealth;
    }

    public double armor() {
        return armor;
    }

    public double movementSpeed() {
        return movementSpeed;
    }

    public double flyingSpeed() {
        return flyingSpeed;
    }

    public Map<String, DragonAbilityOverride> abilities() {
        return abilities;
    }

    public double abilityDamage(String key, double fallback) {
        DragonAbilityOverride override = abilities.get(key);
        return override != null ? override.damageOr(fallback) : fallback;
    }

    public Map<String, Double> extraDoubles() {
        return extraDoubles;
    }

    public double extraDouble(String key, double fallback) {
        return extraDoubles.getOrDefault(key, fallback);
    }

    public double groundRunSpeed(double fallback) {
        return extraDoubles.getOrDefault("run_speed", fallback);
    }

    public double groundWalkSpeed(double fallback) {
        return extraDoubles.getOrDefault("walk_speed", fallback);
    }

    public static DragonAttributeConfig merge(JsonObject json, @Nullable DragonAttributeConfig fallback) {
        DragonAttributeConfig base = fallback != null ? fallback : EMPTY;

        double maxHealth = GsonHelper.getAsDouble(json, "max_health", base.maxHealth);
        double armor = GsonHelper.getAsDouble(json, "armor", base.armor);
        double movementSpeed = GsonHelper.getAsDouble(json, "movement_speed", base.movementSpeed);
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
        if (json.has("extra")) {
            JsonObject extraJson = GsonHelper.getAsJsonObject(json, "extra");
            for (Map.Entry<String, JsonElement> entry : extraJson.entrySet()) {
                extra.put(entry.getKey(), GsonHelper.convertToDouble(entry.getValue(), entry.getKey()));
            }
        }

        return new DragonAttributeConfig(maxHealth, armor, movementSpeed, flyingSpeed, abilityMap, extra);
    }
}

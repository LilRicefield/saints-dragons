package com.leon.saintsdragons.server.entity.variant;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.leon.saintsdragons.common.SaintsDragonsCommon;
import com.leon.saintsdragons.common.registry.Dragons;
import com.leon.saintsdragons.server.entity.base.DragonEntity;
import net.minecraft.core.Holder;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.ServerLevelAccessor;
import net.minecraft.world.level.biome.Biome;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public final class SaintsDragonVariantRegistry {
    public static final String SAINTS_DRAGONS = SaintsDragonsCommon.MOD_ID;
    public static final ResourceLocation DEFAULT_VARIANT_ID = SaintsDragonsCommon.rl("default");

    private static final Map<ResourceLocation, List<DragonVariantDefinition>> DEFAULTS = buildDefaults();
    private static volatile Map<ResourceLocation, List<DragonVariantDefinition>> variantsByDragon = DEFAULTS;
    private static volatile Map<ResourceLocation, DragonVariantDefinition> variantsById = index(DEFAULTS);

    private SaintsDragonVariantRegistry() {
    }

    public static void bootstrap() {
    }

    public static ResourceLocation dragonId(DragonEntity dragon) {
        Dragons type = Dragons.fromEntity(dragon);
        return type != null ? type.getConfigId() : SaintsDragonsCommon.rl("unknown");
    }

    public static ResourceLocation defaultVariantId(ResourceLocation dragonId) {
        return DEFAULT_VARIANT_ID;
    }

    public static ResourceLocation legacyToVariantId(ResourceLocation dragonId, int legacyId) {
        for (DragonVariantDefinition definition : getVariants(dragonId)) {
            if (definition.legacyId() == legacyId) {
                return definition.id();
            }
        }
        return defaultVariantId(dragonId);
    }

    public static int variantIdToLegacy(ResourceLocation dragonId, ResourceLocation variantId) {
        DragonVariantDefinition definition = get(dragonId, variantId);
        if (definition != null && definition.dragon().equals(dragonId) && definition.hasLegacyId()) {
            return definition.legacyId();
        }
        return 0;
    }

    public static boolean isLegacyVariant(ResourceLocation dragonId, ResourceLocation variantId) {
        DragonVariantDefinition definition = get(dragonId, variantId);
        return definition != null && definition.dragon().equals(dragonId) && definition.hasLegacyId();
    }

    public static boolean isDefault(ResourceLocation variantId) {
        return DEFAULT_VARIANT_ID.equals(variantId);
    }

    public static DragonVariantDefinition get(ResourceLocation variantId) {
        return variantsById.get(variantId);
    }

    public static DragonVariantDefinition get(ResourceLocation dragonId, ResourceLocation variantId) {
        for (DragonVariantDefinition definition : getVariants(dragonId)) {
            if (definition.id().equals(variantId)) {
                return definition;
            }
        }
        return null;
    }

    public static List<DragonVariantDefinition> getVariants(ResourceLocation dragonId) {
        return variantsByDragon.getOrDefault(dragonId, DEFAULTS.getOrDefault(dragonId, DEFAULTS.get(SaintsDragonsCommon.rl("ignivorus"))));
    }

    public static Map<String, Integer> legacyNameMap(ResourceLocation dragonId) {
        Map<String, Integer> result = new LinkedHashMap<>();
        for (DragonVariantDefinition definition : getVariants(dragonId)) {
            if (definition.hasLegacyId()) {
                result.put(definition.name(), definition.legacyId());
            }
        }
        if (!result.containsKey("default")) {
            result.put("default", 0);
        }
        return result;
    }

    public static Map<String, ResourceLocation> variantNameMap(ResourceLocation dragonId) {
        Map<String, ResourceLocation> result = new LinkedHashMap<>();
        for (DragonVariantDefinition definition : getVariants(dragonId)) {
            result.put(definition.name(), definition.id());
            result.put(definition.id().toString(), definition.id());
        }
        return result;
    }

    public static List<String> commandSuggestions(ResourceLocation dragonId) {
        List<String> result = new ArrayList<>();
        for (DragonVariantDefinition definition : getVariants(dragonId)) {
            if (SAINTS_DRAGONS.equals(definition.id().getNamespace())) {
                result.add(definition.name());
            } else {
                result.add(definition.id().toString());
            }
        }
        return result;
    }

    public static ResourceLocation chooseSpawnVariant(ServerLevelAccessor levelAccessor, DragonEntity entity) {
        ResourceLocation dragonId = dragonId(entity);
        List<DragonVariantDefinition> allowed = new ArrayList<>();
        for (DragonVariantDefinition definition : getVariants(dragonId)) {
            if (definition.weight() <= 0) {
                continue;
            }
            if (!definition.altitudeRestriction().allows(entity.blockPosition().getY())) {
                continue;
            }
            if (definition.hasBannedBiomes() && matchesBiome(definition.bannedBiomes(), levelAccessor, entity.blockPosition())) {
                continue;
            }
            if (!definition.hasAllowedBiomes() || matchesBiome(definition.allowedBiomes(), levelAccessor, entity.blockPosition())) {
                allowed.add(definition);
            }
        }
        return roll(entity.getRandom(), allowed, defaultVariantId(dragonId));
    }

    public static ResourceLocation normalize(ResourceLocation dragonId, @Nullable ResourceLocation variantId) {
        if (variantId == null) {
            return defaultVariantId(dragonId);
        }
        DragonVariantDefinition definition = get(dragonId, variantId);
        if (definition == null || definition.dragon().equals(dragonId)) {
            return variantId;
        }
        return defaultVariantId(dragonId);
    }

    public static void replaceDatapackVariants(Map<ResourceLocation, List<DragonVariantDefinition>> datapackVariants) {
        Map<ResourceLocation, LinkedHashMap<ResourceLocation, DragonVariantDefinition>> merged = new LinkedHashMap<>();
        for (Map.Entry<ResourceLocation, List<DragonVariantDefinition>> entry : DEFAULTS.entrySet()) {
            LinkedHashMap<ResourceLocation, DragonVariantDefinition> byId = new LinkedHashMap<>();
            for (DragonVariantDefinition definition : entry.getValue()) {
                byId.put(definition.id(), definition);
            }
            merged.put(entry.getKey(), byId);
        }
        for (Map.Entry<ResourceLocation, List<DragonVariantDefinition>> entry : datapackVariants.entrySet()) {
            LinkedHashMap<ResourceLocation, DragonVariantDefinition> byId = merged.computeIfAbsent(entry.getKey(), id -> new LinkedHashMap<>());
            for (DragonVariantDefinition definition : entry.getValue()) {
                DragonVariantDefinition existing = byId.get(definition.id());
                byId.put(definition.id(), preserveLegacyId(existing, definition));
            }
        }

        Map<ResourceLocation, List<DragonVariantDefinition>> next = new LinkedHashMap<>();
        for (Map.Entry<ResourceLocation, LinkedHashMap<ResourceLocation, DragonVariantDefinition>> entry : merged.entrySet()) {
            next.put(entry.getKey(), ImmutableList.copyOf(entry.getValue().values()));
        }
        variantsByDragon = ImmutableMap.copyOf(next);
        variantsById = index(variantsByDragon);
        SaintsDragonsCommon.LOGGER.info("Loaded {} dragon variant definition(s)", variantsById.size());
    }

    public static ResourceLocation adultTexture(ResourceLocation dragonId, ResourceLocation variantId, boolean female) {
        String dragonPath = dragonId.getPath();
        String variantPath = variantId.getPath();
        String suffix = female ? "_female" : "";
        return new ResourceLocation(variantId.getNamespace(), "textures/entity/" + dragonPath + "/" + variantPath + suffix + ".png");
    }

    private static ResourceLocation roll(RandomSource random, List<DragonVariantDefinition> variants, ResourceLocation fallback) {
        int total = 0;
        for (DragonVariantDefinition definition : variants) {
            total += definition.weight();
        }
        if (total <= 0) {
            return fallback;
        }
        int roll = random.nextInt(total);
        for (DragonVariantDefinition definition : variants) {
            roll -= definition.weight();
            if (roll < 0) {
                return definition.id();
            }
        }
        return fallback;
    }

    private static DragonVariantDefinition preserveLegacyId(@Nullable DragonVariantDefinition existing,
                                                            DragonVariantDefinition replacement) {
        if (existing == null || replacement.hasLegacyId() || !existing.hasLegacyId()) {
            return replacement;
        }
        return new DragonVariantDefinition(
                replacement.id(),
                replacement.dragon(),
                replacement.name(),
                replacement.weight(),
                existing.legacyId(),
                replacement.allowedBiomes(),
                replacement.bannedBiomes(),
                replacement.altitudeRestriction()
        );
    }

    private static boolean matchesBiome(@Nullable DragonVariantDefinition.BiomeRestrictions restrictions,
                                        LevelAccessor level,
                                        BlockPos pos) {
        if (restrictions == null || !restrictions.hasAny()) {
            return false;
        }
        Holder<Biome> biome = level.getBiome(pos);
        if (restrictions.hasBiomesByIdList()) {
            ResourceLocation biomeId = biome.unwrapKey()
                    .map(key -> key.location())
                    .orElse(null);
            if (biomeId != null && restrictions.biomesById().contains(biomeId)) {
                return true;
            }
        }
        if (restrictions.hasBiomesByTagList()) {
            for (ResourceLocation tagId : restrictions.biomesByTag()) {
                if (biome.is(TagKey.create(Registries.BIOME, tagId))) {
                    return true;
                }
            }
        }
        return false;
    }

    private static Map<ResourceLocation, DragonVariantDefinition> index(Map<ResourceLocation, List<DragonVariantDefinition>> variants) {
        Map<ResourceLocation, DragonVariantDefinition> result = new LinkedHashMap<>();
        for (List<DragonVariantDefinition> definitions : variants.values()) {
            for (DragonVariantDefinition definition : definitions) {
                result.put(definition.id(), definition);
            }
        }
        return ImmutableMap.copyOf(result);
    }

    private static Map<ResourceLocation, List<DragonVariantDefinition>> buildDefaults() {
        Map<ResourceLocation, List<DragonVariantDefinition>> defaults = new LinkedHashMap<>();
        add(defaults, "raevyx", 0, "default", 90);
        add(defaults, "raevyx", 1, "night_gold", 10);
        add(defaults, "cindervane", 0, "default", 85);
        add(defaults, "cindervane", 1, "albino", 15);
        add(defaults, "ignivorus", 0, "default", 95);
        add(defaults, "ignivorus", 1, "crimson", 5);
        add(defaults, "volitans", 0, "default", 85);
        add(defaults, "volitans", 1, "bloodshot", 15);
        add(defaults, "varasuchus", 0, "default", 100);
        addCustom(defaults, "varasuchus", "void_kissed", 0);
        add(defaults, "stegonaut", 0, "default", 100);
        add(defaults, "nulljaw", 0, "default", 100);
        Map<ResourceLocation, List<DragonVariantDefinition>> immutable = new LinkedHashMap<>();
        for (Map.Entry<ResourceLocation, List<DragonVariantDefinition>> entry : defaults.entrySet()) {
            immutable.put(entry.getKey(), ImmutableList.copyOf(entry.getValue()));
        }
        return ImmutableMap.copyOf(immutable);
    }

    private static void add(Map<ResourceLocation, List<DragonVariantDefinition>> defaults,
                            String dragon,
                            int legacyId,
                            String name,
                            int weight) {
        ResourceLocation dragonId = SaintsDragonsCommon.rl(dragon);
        ResourceLocation variantId = SaintsDragonsCommon.rl(name);
        defaults.computeIfAbsent(dragonId, id -> new ArrayList<>()).add(new DragonVariantDefinition(
                variantId,
                dragonId,
                name,
                weight,
                legacyId,
                null,
                null,
                DragonVariantDefinition.AltitudeRestriction.ANY
        ));
    }

    private static void addCustom(Map<ResourceLocation, List<DragonVariantDefinition>> defaults,
                                  String dragon,
                                  String name,
                                  int weight) {
        ResourceLocation dragonId = SaintsDragonsCommon.rl(dragon);
        ResourceLocation variantId = SaintsDragonsCommon.rl(name);
        defaults.computeIfAbsent(dragonId, id -> new ArrayList<>()).add(new DragonVariantDefinition(
                variantId,
                dragonId,
                name,
                weight,
                DragonVariantDefinition.NO_LEGACY_ID,
                null,
                null,
                DragonVariantDefinition.AltitudeRestriction.ANY
        ));
    }
}

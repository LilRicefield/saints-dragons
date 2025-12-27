package com.leon.saintsdragons.fabric.entity.part;

import com.leon.saintsdragons.server.entity.dragons.ignivorus.Ignivorus;
import net.fabricmc.api.EnvType;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.world.phys.Vec3;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Fabric-specific part manager for Ignivorus hitbox parts.
 * Creates hitboxes for each bone that follow the animated model via synced bone positions.
 * Uses elongated hitboxes that stretch toward adjacent bones to eliminate dead zones.
 *
 * Hitbox zones with damage multipliers:
 * - Head: 1.25x (precision bonus)
 * - Neck: 1.1x (slight bonus)
 * - Wings: 0.9x (reduced)
 * - Body/Legs: 1.0x (normal)
 * - Tail: 0.8x (reduced)
 */
public class FabricIgnivorusPartManager {
    private final Ignivorus dragon;
    private final Map<String, PartConfig> partConfigs = new LinkedHashMap<>();
    private final Map<String, FabricDragonPart> parts = new LinkedHashMap<>();
    private FabricDragonPart[] partsArray;

    // How far each hitbox stretches toward its neighbor (0.75 = 75% of the way)
    private static final float STRETCH_FACTOR = 0.75f;

    public FabricIgnivorusPartManager(Ignivorus dragon) {
        this.dragon = dragon;
        initializePartConfigs();
        initializeParts();
        this.partsArray = parts.values().toArray(new FabricDragonPart[0]);
        addClientParts();
    }

    /**
     * Configuration for each hitbox part: bone name, size, damage multiplier, and adjacent bone to stretch toward.
     */
    private record PartConfig(String boneName, float width, float height, float damageMultiplier, String stretchTowardBone) {}

    private void initializePartConfigs() {
        // Head - precision hits deal bonus damage, longer/narrower box that stretches back to neck
        partConfigs.put("head", new PartConfig("headController", 3.0f, 2.5f, 1.25f, "neck3Controller"));

        // Neck - slight bonus, stretches toward body to close the gap
        partConfigs.put("neck", new PartConfig("neck3Controller", 2.5f, 2.5f, 1.1f, "hip"));

        // Body - covers the main torso area, stretches toward tail
        partConfigs.put("body", new PartConfig("hip", 4.5f, 4.0f, 1.0f, "tail1"));

        // Wings - reduced damage, stretch toward wing joints
        partConfigs.put("leftWing", new PartConfig("leftwing", 4.0f, 2.5f, 0.9f, "leftwingjoint"));
        partConfigs.put("rightWing", new PartConfig("rightwing", 4.0f, 2.5f, 0.9f, "rightwingjoint"));
        // Outer wings - much larger standalone hitboxes at the wing joints to cover wing span
        partConfigs.put("leftWingOuter", new PartConfig("leftwingjoint", 8.0f, 3.0f, 0.9f, null));
        partConfigs.put("rightWingOuter", new PartConfig("rightwingjoint", 8.0f, 3.0f, 0.9f, null));

        // Tail segments - reduced damage, each stretches to the next
        partConfigs.put("tail1", new PartConfig("tail1", 2.5f, 2.5f, 0.85f, "tail2"));
        partConfigs.put("tail2", new PartConfig("tail2", 2.0f, 2.0f, 0.8f, "tail3"));
        partConfigs.put("tail3", new PartConfig("tail3", 1.8f, 1.8f, 0.8f, "tail4"));
        partConfigs.put("tail4", new PartConfig("tail4", 3.0f, 2.0f, 0.75f, null));  // Larger tip hitbox

        // Legs - normal damage, no stretching needed
        partConfigs.put("leftFrontLeg", new PartConfig("leftfrontleg", 1.5f, 2.5f, 1.0f, null));
        partConfigs.put("rightFrontLeg", new PartConfig("rightfrontleg", 1.5f, 2.5f, 1.0f, null));
        partConfigs.put("leftBackLeg", new PartConfig("leftbackleg", 1.5f, 2.5f, 1.0f, null));
        partConfigs.put("rightBackLeg", new PartConfig("rightbackleg", 1.5f, 2.5f, 1.0f, null));
    }

    private void initializeParts() {
        for (Map.Entry<String, PartConfig> entry : partConfigs.entrySet()) {
            String partName = entry.getKey();
            PartConfig config = entry.getValue();
            FabricDragonPart part = new FabricDragonPart(dragon, partName, config.width(), config.height());
            part.setDamageMultiplier(config.damageMultiplier());
            parts.put(partName, part);
        }
    }

    private void addClientParts() {
        if (FabricLoader.getInstance().getEnvironmentType() != EnvType.CLIENT) {
            return;
        }

        for (FabricDragonPart part : parts.values()) {
            addClientPartIfMissing(part);
        }
    }

    private void addClientPartIfMissing(FabricDragonPart part) {
        if (!dragon.level().isClientSide) {
            return;
        }
        if (dragon.level().getEntity(part.getId()) != null) {
            return;
        }
        FabricPartClientHooks.addClientPart(dragon.level(), part);
    }

    public void updatePartPositions() {
        if (dragon.isRemoved()) {
            for (FabricDragonPart part : parts.values()) {
                part.remove(net.minecraft.world.entity.Entity.RemovalReason.DISCARDED);
            }
            return;
        }

        boolean isClient = dragon.level().isClientSide;
        Vec3 dragonPos = dragon.position();
        float yawRad = (float) Math.toRadians(dragon.getYRot());
        double cosYaw = Math.cos(yawRad);
        double sinYaw = Math.sin(yawRad);

        for (Map.Entry<String, FabricDragonPart> entry : parts.entrySet()) {
            String partName = entry.getKey();
            FabricDragonPart part = entry.getValue();
            PartConfig config = partConfigs.get(partName);
            if (isClient) {
                addClientPartIfMissing(part);
            }

            // Try to get synced bone position (works on both client and server)
            Vec3 bonePos = dragon.getBonePositionForHitbox(config.boneName());

            if (bonePos != null) {
                // Check if this part should stretch toward an adjacent bone
                if (config.stretchTowardBone() != null) {
                    Vec3 stretchTarget = dragon.getBonePositionForHitbox(config.stretchTowardBone());
                    if (stretchTarget != null) {
                        // Use elongated hitbox that stretches toward the next bone
                        part.updatePositionElongated(
                            bonePos.x, bonePos.y, bonePos.z,
                            stretchTarget.x, stretchTarget.y, stretchTarget.z,
                            STRETCH_FACTOR
                        );
                        continue;
                    }
                }
                // No stretch target or target not available - use standard positioning
                part.updatePosition(bonePos.x, bonePos.y, bonePos.z);
            } else {
                // Fallback: calculate approximate position from dragon rotation
                // This is used when no bone data is available yet
                double forward = 0, up = 2, side = 0;
                switch (partName) {
                    case "head" -> { forward = 6.0; up = 4.0; }
                    case "neck" -> { forward = 4.0; up = 3.5; }
                    case "body" -> { forward = 0.0; up = 3.0; }
                    case "leftWing" -> { forward = 0.0; up = 3.0; side = -4.0; }
                    case "rightWing" -> { forward = 0.0; up = 3.0; side = 4.0; }
                    case "leftWingOuter" -> { forward = 0.0; up = 3.0; side = -8.0; }
                    case "rightWingOuter" -> { forward = 0.0; up = 3.0; side = 8.0; }
                    case "tail1" -> { forward = -3.0; up = 2.0; }
                    case "tail2" -> { forward = -5.0; up = 1.5; }
                    case "tail3" -> { forward = -7.0; up = 1.0; }
                    case "tail4" -> { forward = -9.0; up = 0.5; }
                    case "leftFrontLeg" -> { forward = 2.0; up = 0.0; side = -2.0; }
                    case "rightFrontLeg" -> { forward = 2.0; up = 0.0; side = 2.0; }
                    case "leftBackLeg" -> { forward = -2.0; up = 0.0; side = -2.0; }
                    case "rightBackLeg" -> { forward = -2.0; up = 0.0; side = 2.0; }
                }

                double worldX = dragonPos.x + (cosYaw * forward) - (sinYaw * side);
                double worldY = dragonPos.y + up;
                double worldZ = dragonPos.z + (sinYaw * forward) + (cosYaw * side);

                part.updatePosition(worldX, worldY, worldZ);
            }
        }
    }

    public FabricDragonPart[] getParts() {
        return partsArray;
    }
}

package com.leon.saintsdragons.forge.entity.part;

import com.leon.saintsdragons.server.entity.dragons.ignivorus.Ignivorus;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.entity.PartEntity;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Forge-specific part manager for Ignivorus hitbox parts.
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
public class ForgeIgnivorusPartManager {
    private final Ignivorus dragon;
    private final Map<String, PartConfig> partConfigs = new LinkedHashMap<>();
    private final Map<String, ForgeDragonPart> parts = new LinkedHashMap<>();
    private ForgeDragonPart[] partsArray;

    // How far each hitbox stretches toward its neighbor (0.6 = 60% of the way)
    private static final float STRETCH_FACTOR = 0.6f;

    public ForgeIgnivorusPartManager(Ignivorus dragon) {
        this.dragon = dragon;
        initializePartConfigs();
        initializeParts();
        this.partsArray = parts.values().toArray(new ForgeDragonPart[0]);
    }

    /**
     * Configuration for each hitbox part: bone name, size, damage multiplier, and adjacent bone to stretch toward.
     * @param boneName The GeckoLib bone this hitbox follows
     * @param width Hitbox base width
     * @param height Hitbox base height
     * @param damageMultiplier Damage multiplier for hits on this part
     * @param stretchTowardBone The adjacent bone to stretch toward (null for no stretching)
     */
    private record PartConfig(String boneName, float width, float height, float damageMultiplier, String stretchTowardBone) {}

    private void initializePartConfigs() {
        // Head - precision hits deal bonus damage, stretches toward neck
        partConfigs.put("head", new PartConfig("headController", 2.5f, 2.5f, 1.25f, "neck3Controller"));

        // Neck - slight bonus, stretches toward tail (body connection)
        partConfigs.put("neck", new PartConfig("neck3Controller", 2.0f, 2.0f, 1.1f, "tail1"));

        // Wings - reduced damage (thin targets), no stretching needed
        partConfigs.put("leftWing", new PartConfig("leftwing", 3.5f, 2.0f, 0.9f, null));
        partConfigs.put("rightWing", new PartConfig("rightwing", 3.5f, 2.0f, 0.9f, null));

        // Tail segments - reduced damage, each stretches to the next
        partConfigs.put("tail1", new PartConfig("tail1", 2.0f, 2.0f, 0.85f, "tail2"));
        partConfigs.put("tail2", new PartConfig("tail2", 1.8f, 1.8f, 0.8f, "tail3"));
        partConfigs.put("tail3", new PartConfig("tail3", 1.5f, 1.5f, 0.8f, "tail4"));
        partConfigs.put("tail4", new PartConfig("tail4", 1.2f, 1.2f, 0.75f, null));

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
            ForgeDragonPart part = new ForgeDragonPart(dragon, partName, config.width(), config.height());
            part.setDamageMultiplier(config.damageMultiplier());
            parts.put(partName, part);
        }
    }

    public void updatePartPositions() {
        Vec3 dragonPos = dragon.position();
        float yawRad = (float) Math.toRadians(dragon.getYRot());
        double cosYaw = Math.cos(yawRad);
        double sinYaw = Math.sin(yawRad);

        for (Map.Entry<String, ForgeDragonPart> entry : parts.entrySet()) {
            String partName = entry.getKey();
            ForgeDragonPart part = entry.getValue();
            PartConfig config = partConfigs.get(partName);

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
                    case "leftWing" -> { forward = 0.0; up = 3.0; side = -4.0; }
                    case "rightWing" -> { forward = 0.0; up = 3.0; side = 4.0; }
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

    public PartEntity<?>[] getParts() {
        return partsArray;
    }
}

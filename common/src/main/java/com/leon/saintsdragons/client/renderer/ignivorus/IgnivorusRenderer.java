package com.leon.saintsdragons.client.renderer.ignivorus;

import com.leon.saintsdragons.client.renderer.DragonGeoEntityRenderer;
import com.leon.saintsdragons.client.model.ignivorus.IgnivorusModel;
import com.leon.saintsdragons.client.renderer.layer.ignivorus.IgnivorusGlowLayer;
import com.leon.saintsdragons.client.renderer.layer.ignivorus.IgnivorusMouthSmokeLayer;
import com.leon.saintsdragons.common.network.MessageDragonBonePositions;
import com.leon.saintsdragons.common.network.NetworkHandler;
import com.leon.saintsdragons.server.entity.dragons.ignivorus.Ignivorus;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.world.phys.Vec3;

public class IgnivorusRenderer extends DragonGeoEntityRenderer<Ignivorus> {
    private static final float PASSENGER_X = 0.0f, PASSENGER_Y = -3.0f, PASSENGER_Z = 0.0f;
    private static final String FIRE_BONE = "fireBone";
    private static final String PASSENGER_BONE = "passengerBone";
    private static final String HEAD_BONE = "headController";
    private static final String NECK_BONE = "neck3Controller";
    private static final String HIP_BONE = "hip";
    private static final String LEFT_WING_BONE = "leftwing";
    private static final String RIGHT_WING_BONE = "rightwing";
    private static final String LEFT_WING_JOINT_BONE = "leftwingjoint";
    private static final String RIGHT_WING_JOINT_BONE = "rightwingjoint";
    private static final String TAIL1_BONE = "tail1";
    private static final String TAIL2_BONE = "tail2";
    private static final String TAIL3_BONE = "tail3";
    private static final String TAIL4_BONE = "tail4";
    private static final String LEFT_FRONT_LEG_BONE = "leftfrontleg";
    private static final String RIGHT_FRONT_LEG_BONE = "rightfrontleg";
    private static final String LEFT_BACK_LEG_BONE = "leftbackleg";
    private static final String RIGHT_BACK_LEG_BONE = "rightbackleg";
    private static final int SYNC_INTERVAL_TICKS = 2;
    private static final double SNAPSHOT_PRECISION = 1000.0D;
    private final java.util.Map<Integer, Integer> lastBoneSnapshotHashes = new java.util.HashMap<>();

    public IgnivorusRenderer(EntityRendererProvider.Context renderManager) {
        super(renderManager, new IgnivorusModel());
        this.addRenderLayer(new IgnivorusMouthSmokeLayer());
        this.addRenderLayer(new IgnivorusGlowLayer(this));
    }

    @Override
    protected float getBabyShadowRadius(Ignivorus entity) {
        return 1.5F;
    }

    @Override
    protected float getAdultShadowRadius(Ignivorus entity) {
        return 5.0f;
    }

    @Override
    protected String[] trackedBoneNames() {
        return new String[] {
                PASSENGER_BONE, FIRE_BONE, HEAD_BONE, NECK_BONE, HIP_BONE,
                LEFT_WING_BONE, RIGHT_WING_BONE, LEFT_WING_JOINT_BONE, RIGHT_WING_JOINT_BONE,
                TAIL1_BONE, TAIL2_BONE, TAIL3_BONE, TAIL4_BONE,
                LEFT_FRONT_LEG_BONE, RIGHT_FRONT_LEG_BONE, LEFT_BACK_LEG_BONE, RIGHT_BACK_LEG_BONE
        };
    }

    @Override
    protected LocatorSpec[] locatorSpecs(Ignivorus entity) {
        if (entity.isBaby()) {
            return new LocatorSpec[] {
                    new LocatorSpec(PASSENGER_BONE, PASSENGER_X, PASSENGER_Y, PASSENGER_Z, "passengerLocator")
            };
        }

        return new LocatorSpec[] {
                new LocatorSpec(PASSENGER_BONE, PASSENGER_X, PASSENGER_Y, PASSENGER_Z, "passengerLocator"),
                new LocatorSpec(FIRE_BONE, 0.0f, 0.0f, 0.0f, "fireBoneOrigin"),
                new LocatorSpec(HEAD_BONE, 0.0f, 0.0f, 0.0f, "headController"),
                new LocatorSpec(NECK_BONE, 0.0f, 0.0f, 0.0f, "neck3Controller"),
                new LocatorSpec(HIP_BONE, 0.0f, 0.0f, 0.0f, "hip"),
                new LocatorSpec(LEFT_WING_BONE, 0.0f, 0.0f, 0.0f, "leftwing"),
                new LocatorSpec(RIGHT_WING_BONE, 0.0f, 0.0f, 0.0f, "rightwing"),
                new LocatorSpec(LEFT_WING_JOINT_BONE, 0.0f, 0.0f, 0.0f, "leftwingjoint"),
                new LocatorSpec(RIGHT_WING_JOINT_BONE, 0.0f, 0.0f, 0.0f, "rightwingjoint"),
                new LocatorSpec(TAIL1_BONE, 0.0f, 0.0f, 0.0f, "tail1"),
                new LocatorSpec(TAIL2_BONE, 0.0f, 0.0f, 0.0f, "tail2"),
                new LocatorSpec(TAIL3_BONE, 0.0f, 0.0f, 0.0f, "tail3"),
                new LocatorSpec(TAIL4_BONE, 0.0f, 0.0f, 0.0f, "tail4"),
                new LocatorSpec(LEFT_FRONT_LEG_BONE, 0.0f, 0.0f, 0.0f, "leftfrontleg"),
                new LocatorSpec(RIGHT_FRONT_LEG_BONE, 0.0f, 0.0f, 0.0f, "rightfrontleg"),
                new LocatorSpec(LEFT_BACK_LEG_BONE, 0.0f, 0.0f, 0.0f, "leftbackleg"),
                new LocatorSpec(RIGHT_BACK_LEG_BONE, 0.0f, 0.0f, 0.0f, "rightbackleg")
        };
    }

    @Override
    protected void afterDragonRender(Ignivorus entity, float partialTick) {
        if (!entity.isBaby()) {
            sendBonePositionsToServer(entity);
        }
    }

    private void sendBonePositionsToServer(Ignivorus entity) {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.player == null || !entity.isAlive()) {
            return;
        }
        if (minecraft.player.distanceToSqr(entity) > 96.0D * 96.0D) {
            return;
        }

        if ((entity.tickCount + entity.getId()) % SYNC_INTERVAL_TICKS != 0) {
            return;
        }

        java.util.Map<String, Vec3> positions =
                new java.util.HashMap<>(MessageDragonBonePositions.SYNCED_BONES.length);
        for (String boneName : MessageDragonBonePositions.SYNCED_BONES) {
           Vec3 pos = entity.getClientLocatorPosition(boneName);
            if (pos != null) {
                positions.put(boneName, pos);
            }
        }

        if (positions.isEmpty()) {
            return;
        }

        int snapshotHash = computeSnapshotHash(positions);
        Integer previousHash = lastBoneSnapshotHashes.put(entity.getId(), snapshotHash);
        if (previousHash != null && previousHash == snapshotHash) {
            return;
        }

        if (!positions.isEmpty()) {
            NetworkHandler.sendToServer(
                new MessageDragonBonePositions(entity.getId(), positions)
            );
        }
    }

    private static int computeSnapshotHash(java.util.Map<String, Vec3> positions) {
        int hash = 1;
        for (String boneName : MessageDragonBonePositions.SYNCED_BONES) {
            Vec3 pos = positions.get(boneName);
            if (pos == null) {
                continue;
            }
            hash = 31 * hash + boneName.hashCode();
            hash = 31 * hash + quantize(pos.x);
            hash = 31 * hash + quantize(pos.y);
            hash = 31 * hash + quantize(pos.z);
        }
        return hash;
    }

    private static int quantize(double value) {
        return (int) Math.round(value * SNAPSHOT_PRECISION);
    }

}

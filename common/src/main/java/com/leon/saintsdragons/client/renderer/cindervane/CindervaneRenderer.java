package com.leon.saintsdragons.client.renderer.cindervane;

import com.leon.saintsdragons.client.renderer.RiderConfig;
import com.leon.saintsdragons.client.renderer.DragonGeoEntityRenderer;
import com.leon.saintsdragons.client.model.cindervane.CindervaneModel;
import com.leon.saintsdragons.common.network.MessageDragonBonePositions;
import com.leon.saintsdragons.common.network.NetworkHandler;
import com.leon.saintsdragons.server.entity.dragons.cindervane.Cindervane;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.world.phys.Vec3;

public class CindervaneRenderer extends DragonGeoEntityRenderer<Cindervane> {
    private static final float PASSENGER_SEAT0_X = 0.0f, PASSENGER_SEAT0_Y = -3.0f, PASSENGER_SEAT0_Z = 0.0f;
    private static final float PASSENGER_SEAT1_X = 0.0f, PASSENGER_SEAT1_Y = -3.0f, PASSENGER_SEAT1_Z = 0.0f;
    private static final String AUTO_MOUNT_BONE = "automountBoneRight";
    private static final String AUTO_MOUNT_LOCATOR = "automountBoneRight";
    private static final float AUTO_MOUNT_OFFSET_X = 0.0f;
    private static final float AUTO_MOUNT_OFFSET_Y = 0.0f;
    private static final float AUTO_MOUNT_OFFSET_Z = 0.0f;
    private static final int SYNC_INTERVAL_TICKS = 2;
    private static final double SNAPSHOT_PRECISION = 1000.0;
    private final java.util.Map<Integer, Integer> lastBoneSnapshotHashes = new java.util.HashMap<>();

    public CindervaneRenderer(EntityRendererProvider.Context renderManager) {
        super(renderManager, new CindervaneModel());
    }

    @Override
    protected float getBabyShadowRadius(Cindervane entity) {
        return 0.8f;
    }

    @Override
    protected float getAdultShadowRadius(Cindervane entity) {
        return 2.0f;
    }

    @Override
    protected String[] trackedBoneNames() {
        return new String[] {"passengerBone1", "passengerBone2", AUTO_MOUNT_BONE};
    }

    @Override
    protected LocatorSpec[] locatorSpecs(Cindervane entity) {
        return new LocatorSpec[] {
                new LocatorSpec("passengerBone1", PASSENGER_SEAT0_X, PASSENGER_SEAT0_Y, PASSENGER_SEAT0_Z,
                        "passengerSeat0", "passengerLocator"),
                new LocatorSpec("passengerBone2", PASSENGER_SEAT1_X, PASSENGER_SEAT1_Y, PASSENGER_SEAT1_Z,
                        "passengerSeat1"),
                new LocatorSpec(AUTO_MOUNT_BONE, AUTO_MOUNT_OFFSET_X, AUTO_MOUNT_OFFSET_Y, AUTO_MOUNT_OFFSET_Z,
                        AUTO_MOUNT_LOCATOR)
        };
    }

    @Override
    protected int seatIndexForRiderBone(Cindervane animatable, String boneName, RiderConfig.RiderSpec riderSpec) {
        if (boneName.equals(RiderConfig.getSeatBoneName(animatable, 0))) {
            return 0;
        }
        if (boneName.equals(RiderConfig.getSeatBoneName(animatable, 1))) {
            return 1;
        }
        return -1;
    }

    @Override
    protected void afterDragonRender(Cindervane entity, float partialTick) {
        sendBonePositionsToServer(entity);
    }

    private void sendBonePositionsToServer(Cindervane entity) {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.player == null || entity.getControllingPassenger() != minecraft.player) {
            return;
        }

        if ((entity.tickCount + entity.getId()) % SYNC_INTERVAL_TICKS != 0) {
            return;
        }

        java.util.Map<String, Vec3> positions = new java.util.HashMap<>(1);
        Vec3 autoMount = entity.getClientLocatorPosition(AUTO_MOUNT_LOCATOR);
        if (autoMount != null) {
            positions.put(AUTO_MOUNT_LOCATOR, autoMount);
        }

        if (positions.isEmpty()) {
            return;
        }

        int snapshotHash = computeSnapshotHash(positions);
        Integer previousHash = lastBoneSnapshotHashes.put(entity.getId(), snapshotHash);
        if (previousHash != null && previousHash == snapshotHash) {
            return;
        }

        NetworkHandler.sendToServer(new MessageDragonBonePositions(entity.getId(), positions));
    }

    private static int computeSnapshotHash(java.util.Map<String, Vec3> positions) {
        int hash = 1;
        Vec3 autoMount = positions.get(AUTO_MOUNT_LOCATOR);
        if (autoMount != null) {
            hash = 31 * hash + AUTO_MOUNT_LOCATOR.hashCode();
            hash = 31 * hash + quantize(autoMount.x);
            hash = 31 * hash + quantize(autoMount.y);
            hash = 31 * hash + quantize(autoMount.z);
        }
        return hash;
    }

    private static int quantize(double value) {
        return (int) Math.round(value * SNAPSHOT_PRECISION);
    }

}

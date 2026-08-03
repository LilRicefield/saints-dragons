package com.leon.saintsdragons.client.renderer.cindervane;

import com.leon.saintsdragons.client.renderer.RiderConfig;
import com.leon.saintsdragons.client.renderer.DragonGeoEntityRenderer;
import com.leon.saintsdragons.client.renderer.layer.cindervane.CindervaneNightEmissiveLayer;
import com.leon.saintsdragons.client.renderer.layer.DragonEquipmentLayer;
import com.leon.saintsdragons.client.renderer.vfx.DragonDiveTrailRenderer;
import com.leon.saintsdragons.client.model.cindervane.CindervaneModel;
import com.leon.saintsdragons.common.network.MessageDragonBonePositions;
import com.leon.saintsdragons.common.network.NetworkHandler;
import com.leon.saintsdragons.common.SaintsDragonsCommon;
import com.leon.saintsdragons.server.entity.dragons.cindervane.Cindervane;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.world.phys.Vec3;

import java.util.HashMap;
import java.util.Map;

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
    private final Map<Integer, Integer> lastBoneSnapshotHashes = new HashMap<>();

    public CindervaneRenderer(EntityRendererProvider.Context context) {
        super(context, new CindervaneModel());
        this.addRenderLayer(new DragonEquipmentLayer<>(
                this,
                Cindervane::hasSaddle,
                SaintsDragonsCommon.rl("textures/entity/cindervane/cindervane_saddle_layer.png")
        ));
        this.addRenderLayer(new DragonEquipmentLayer<>(
                this,
                Cindervane::hasCindervaneChest,
                SaintsDragonsCommon.rl("textures/entity/cindervane/cindervane_chest_layer.png")
        ));
        this.addRenderLayer(new CindervaneNightEmissiveLayer(this));
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
        return new String[] {"passengerBone1", "passengerBone2", AUTO_MOUNT_BONE,
                DragonDiveTrailRenderer.LEFT_WING_TRAIL_BONE,
                DragonDiveTrailRenderer.RIGHT_WING_TRAIL_BONE,
                DragonDiveTrailRenderer.TIP_WING_TRAIL_BONE};
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
    protected void afterDragonRender(Cindervane entity, PoseStack poseStack,
                                     MultiBufferSource bufferSource, float partialTick) {
        sendBonePositionsToServer(entity);
        DragonDiveTrailRenderer.render(entity,
                getBoneWorldPosition(DragonDiveTrailRenderer.LEFT_WING_TRAIL_BONE),
                getBoneWorldPosition(DragonDiveTrailRenderer.RIGHT_WING_TRAIL_BONE),
                getBoneWorldPosition(DragonDiveTrailRenderer.TIP_WING_TRAIL_BONE),
                bufferSource,
                poseStack.last());
    }

    private void sendBonePositionsToServer(Cindervane entity) {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.player == null || entity.getControllingPassenger() != minecraft.player) {
            return;
        }

        if ((entity.tickCount + entity.getId()) % SYNC_INTERVAL_TICKS != 0) {
            return;
        }

        Map<String, Vec3> positions = new HashMap<>(1);
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

    private static int computeSnapshotHash(Map<String, Vec3> positions) {
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

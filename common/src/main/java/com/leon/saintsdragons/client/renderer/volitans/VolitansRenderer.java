package com.leon.saintsdragons.client.renderer.volitans;

import com.leon.saintsdragons.client.model.volitans.VolitansModel;
import com.leon.saintsdragons.client.renderer.DragonGeoEntityRenderer;
import com.leon.saintsdragons.common.network.MessageDragonBonePositions;
import com.leon.saintsdragons.common.network.NetworkHandler;
import com.leon.saintsdragons.server.entity.dragons.volitans.Volitans;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.world.phys.Vec3;

@Environment(EnvType.CLIENT)
public class VolitansRenderer extends DragonGeoEntityRenderer<Volitans> {
    private static final float PASSENGER_X = 0.0f;
    private static final float PASSENGER_Y = -3.0f;
    private static final float PASSENGER_Z = 0.0f;
    private static final String PASSENGER_BONE = "passengerBone";
    private static final String BREATH_BONE = "breathBone";
    private static final int SYNC_INTERVAL_TICKS = 2;
    private final java.util.Map<Integer, Integer> lastBreathSnapshotHashes = new java.util.HashMap<>();

    public VolitansRenderer(EntityRendererProvider.Context context) {
        super(context, new VolitansModel());
    }

    @Override
    protected float getBabyShadowRadius(Volitans entity) {
        return 1.1f;
    }

    @Override
    protected float getAdultShadowRadius(Volitans entity) {
        return 2.4f;
    }

    @Override
    protected String[] trackedBoneNames() {
        return new String[] {PASSENGER_BONE, BREATH_BONE};
    }

    @Override
    protected LocatorSpec[] locatorSpecs(Volitans entity) {
        return new LocatorSpec[] {
                new LocatorSpec(PASSENGER_BONE, PASSENGER_X, PASSENGER_Y, PASSENGER_Z,
                        "passengerLocator", "passengerSeat0"),
                new LocatorSpec(BREATH_BONE, 0.0f, 0.0f, 0.0f, "breathBoneOrigin")
        };
    }

    @Override
    protected void afterDragonRender(Volitans entity, float partialTick) {
        sendBreathLocatorToServer(entity);
    }

    private void sendBreathLocatorToServer(Volitans entity) {
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

        java.util.Map<String, Vec3> positions = new java.util.HashMap<>(1);
        Vec3 breath = entity.getClientLocatorPosition("breathBoneOrigin");
        if (breath != null) {
            positions.put("breathBoneOrigin", breath);
        }

        if (positions.isEmpty()) {
            return;
        }

        int snapshotHash = computeSnapshotHash(positions);
        Integer previousHash = lastBreathSnapshotHashes.put(entity.getId(), snapshotHash);
        if (previousHash != null && previousHash == snapshotHash) {
            return;
        }

        NetworkHandler.sendToServer(new MessageDragonBonePositions(entity.getId(), positions));
    }

    private static int computeSnapshotHash(java.util.Map<String, Vec3> positions) {
        int hash = 1;
        for (String boneName : new String[] {"breathBoneOrigin"}) {
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
        return (int) Math.round(value * 1000.0D);
    }
}

package com.leon.saintsdragons.client.renderer.raevyx;

import com.leon.saintsdragons.client.model.raevyx.RaevyxModel;
import com.leon.saintsdragons.client.renderer.DragonGeoEntityRenderer;
import com.leon.saintsdragons.client.renderer.vfx.DragonDiveTrailRenderer;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.culling.Frustum;
import com.leon.saintsdragons.server.entity.dragons.raevyx.Raevyx;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import com.leon.saintsdragons.client.renderer.layer.raevyx.RaevyxLightningBeamLayer;
import com.leon.saintsdragons.client.renderer.layer.raevyx.RaevyxGlowLayer;
import com.leon.saintsdragons.client.renderer.layer.raevyx.RaevyxNightEmissiveLayer;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.NotNull;

public class RaevyxRenderer extends DragonGeoEntityRenderer<Raevyx> {
    private static final double BEAM_CULL_PADDING = 2.0D;
    private static final double BEAM_RENDER_DISTANCE = 256.0D;
    private static final String PASSENGER_BONE = "passengerBone";
    private static final String BEAM_BONE = "beamBone";
    private static final float PASSENGER_X = 0.0f;
    private static final float PASSENGER_Y = -3.0f;
    private static final float PASSENGER_Z = 0.0f;

    public RaevyxRenderer(EntityRendererProvider.Context context) {
        super(context, new RaevyxModel());
        this.addRenderLayer(new RaevyxNightEmissiveLayer(this));
        this.addRenderLayer(new RaevyxGlowLayer(this));
        this.addRenderLayer(new RaevyxLightningBeamLayer());
    }

    @Override
    protected float getBabyShadowRadius(Raevyx entity) {
        return 1.25F;
    }

    @Override
    protected float getAdultShadowRadius(Raevyx entity) {
        return 3.0f;
    }

    @Override
    protected String[] trackedBoneNames() {
        return new String[] {PASSENGER_BONE, BEAM_BONE, DragonDiveTrailRenderer.LEFT_WING_TRAIL_BONE,
                DragonDiveTrailRenderer.RIGHT_WING_TRAIL_BONE, DragonDiveTrailRenderer.TIP_WING_TRAIL_BONE};
    }

    @Override
    protected LocatorSpec[] locatorSpecs(Raevyx entity) {
        return new LocatorSpec[] {
                new LocatorSpec(PASSENGER_BONE, PASSENGER_X, PASSENGER_Y, PASSENGER_Z,
                        "passengerLocator", "passengerSeat0"),
                new LocatorSpec(BEAM_BONE, 0.0f, 0.0f, 0.0f, "beamBoneOrigin")
        };
    }

    @Override
    public boolean shouldRender(@NotNull Raevyx entity, @NotNull Frustum frustum,
                                double camX, double camY, double camZ) {
        if (super.shouldRender(entity, frustum, camX, camY, camZ)) {
            return true;
        }
        if (!entity.isBeaming()) {
            return false;
        }

        double dx = entity.getX() - camX;
        double dy = entity.getY() - camY;
        double dz = entity.getZ() - camZ;
        if (dx * dx + dy * dy + dz * dz > BEAM_RENDER_DISTANCE * BEAM_RENDER_DISTANCE) {
            return false;
        }

        Vec3 end = entity.getBeamEndPosition();
        if (end == null) {
            return false;
        }
        Vec3 start = entity.getBeamStartAnchor(1.0F);
        AABB beamBounds = new AABB(start, end).inflate(BEAM_CULL_PADDING);
        return frustum.isVisible(beamBounds);
    }

    @Override
    protected void afterDragonRender(Raevyx entity, PoseStack poseStack, MultiBufferSource bufferSource, float partialTick) {
        DragonDiveTrailRenderer.render(entity,
                getBoneWorldPosition(DragonDiveTrailRenderer.LEFT_WING_TRAIL_BONE),
                getBoneWorldPosition(DragonDiveTrailRenderer.RIGHT_WING_TRAIL_BONE),
                getBoneWorldPosition(DragonDiveTrailRenderer.TIP_WING_TRAIL_BONE),
                bufferSource,
                poseStack.last());
    }
}

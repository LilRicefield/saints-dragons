package com.leon.saintsdragons.client.camera;

import com.leon.saintsdragons.server.entity.dragons.cindervane.Cindervane;
import com.leon.saintsdragons.server.entity.dragons.ignivorus.Ignivorus;
import com.leon.saintsdragons.server.entity.dragons.varasuchus.Varasuchus;
import com.leon.saintsdragons.server.entity.dragons.raevyx.Raevyx;
import com.leon.saintsdragons.server.entity.dragons.stegonaut.Stegonaut;
import com.leon.saintsdragons.server.entity.dragons.volitans.Volitans;
import net.minecraft.world.entity.Entity;

public final class DragonRideCameraTuning {
    public static final ZoomProfile RAEVYX = new ZoomProfile(15.0f, 13.0f, 5.5f);
    public static final ZoomProfile CINDERVANE = new ZoomProfile(5.0f, 15.0f, 5.5f);
    public static final ZoomProfile IGNIVORUS = new ZoomProfile(15.0f, 30.0f, 6.5f);
    public static final ZoomProfile VARASUCHUS = new ZoomProfile(15.0f, 18.0f, 5.5f);
    public static final ZoomProfile STEGONAUT = new ZoomProfile(8.0f, 8.0f, 0.0f);
    public static final ZoomProfile VOLITANS = new ZoomProfile(15.0f, 20.0f, 5.5f);
    public static final ZoomProfile DEFAULT = new ZoomProfile(15.0f, 15.0f, 5.5f);

    private DragonRideCameraTuning() {
    }

    public static boolean isAirOrWaterMode(Entity vehicle) {
        if (vehicle instanceof Varasuchus varasuchus) {
            return varasuchus.isInWaterOrBubble();
        }
        if (vehicle instanceof Raevyx raevyx) {
            return raevyx.isFlying() || raevyx.isInWaterOrBubble();
        }
        if (vehicle instanceof Cindervane cindervane) {
            return cindervane.isFlying() || cindervane.isInWaterOrBubble();
        }
        if (vehicle instanceof Ignivorus ignivorus) {
            return ignivorus.isFlying() || ignivorus.isInWaterOrBubble();
        }
        if (vehicle instanceof Volitans volitans) {
            return volitans.isFlying() || volitans.isInWaterOrBubble();
        }
        if (vehicle instanceof Stegonaut) {
            return false;
        }
        return false;
    }

    public static float getTargetZoom(Entity vehicle) {
        ZoomProfile profile = getProfile(vehicle);
        return isAirOrWaterMode(vehicle) ? profile.airOrWater() : profile.grounded();
    }

    public static float getGroundedZoom(Entity vehicle) {
        return getProfile(vehicle).grounded();
    }

    public static float getAirOrWaterZoom(Entity vehicle) {
        return getProfile(vehicle).airOrWater();
    }

    public static float getBankShiftMax(Entity vehicle) {
        return getProfile(vehicle).bankShiftMax();
    }

    public static ZoomProfile getProfile(Entity vehicle) {
        if (vehicle instanceof Raevyx) {
            return RAEVYX;
        }
        if (vehicle instanceof Cindervane) {
            return CINDERVANE;
        }
        if (vehicle instanceof Ignivorus) {
            return IGNIVORUS;
        }
        if (vehicle instanceof Varasuchus) {
            return VARASUCHUS;
        }
        if (vehicle instanceof Stegonaut) {
            return STEGONAUT;
        }
        if (vehicle instanceof Volitans) {
            return VOLITANS;
        }
        return DEFAULT;
    }

    public record ZoomProfile(float grounded, float airOrWater, float bankShiftMax) {
    }
}

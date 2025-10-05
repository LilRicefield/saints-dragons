package com.leon.saintsdragons.server.entity.conductivity;

import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.Level;

/**
 * Utility for deriving conductivity multipliers based on the entity's environment.
 */
public final class ElectricalConductivityHelper {

    private ElectricalConductivityHelper() {
    }

    public static ElectricalConductivityState evaluate(ElectricalConductivityCapable subject) {
        LivingEntity entity = subject.asConductiveEntity();
        Level level = entity.level();
        BlockPos pos = entity.blockPosition();

        boolean submerged = entity.isInWaterOrBubble();
        // Rain checks: the vanilla helper does not include thunderstorms when under sky, so ensure we cover both.
        boolean wet = entity.isInWaterRainOrBubble() || level.isRainingAt(pos) || level.isThundering() && level.canSeeSky(pos);

        ElectricalConductivityProfile profile = subject.getConductivityProfile();

        float damage = profile.baseDamageMultiplier();
        double range = profile.baseRangeMultiplier();

        if (submerged) {
            damage += profile.submergedDamageBonus();
            range += profile.submergedRangeBonus();
        } else if (wet) {
            damage += profile.wetDamageBonus();
            range += profile.wetRangeBonus();
        }

        if (damage < 0.0f) {
            damage = 0.0f;
        }
        if (range < 0.0) {
            range = 0.0;
        }

        return new ElectricalConductivityState(submerged, wet, damage, range);
    }
}

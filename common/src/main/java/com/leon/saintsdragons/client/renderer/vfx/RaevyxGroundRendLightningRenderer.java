package com.leon.saintsdragons.client.renderer.vfx;

import com.leon.saintsdragons.client.renderer.ShaderPassCompatibility;
import com.leon.saintsdragons.server.entity.ability.abilities.raevyx.RaevyxGroundRendAbility;
import com.leon.saintsdragons.server.entity.dragons.raevyx.Raevyx;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import org.joml.Quaternionf;

import java.util.Map;
import java.util.WeakHashMap;

public final class RaevyxGroundRendLightningRenderer {
    private static final float STRIKE_TICKS = 4.0F;
    private static final Map<Raevyx, Strike> STRIKES = new WeakHashMap<>();

    private record Strike(long castStart, int cycle, Vec3 from, Vec3 secondFrom, Vec3 ground, long seed) {}

    private RaevyxGroundRendLightningRenderer() {}

    public static void render(Raevyx dragon, PoseStack poses, MultiBufferSource buffers, float partialTick) {
        if (ShaderPassCompatibility.isIrisShadowPass()) return;
        float age = dragon.getGroundRendVisualAge(partialTick);
        if (age < RaevyxGroundRendAbility.FALLING_BLOCK_TICKS
                || age >= RaevyxGroundRendAbility.GROUND_REND_TRAIL_END_TICKS
                || dragon.isFlying() || dragon.isInWaterOrBubble() || dragon.isBaby()) {
            STRIKES.remove(dragon);
            return;
        }
        float time = age - RaevyxGroundRendAbility.FALLING_BLOCK_TICKS;
        float remainingTicks = RaevyxGroundRendAbility.GROUND_REND_TRAIL_END_TICKS - age;
        int cycle = Mth.floor(time / STRIKE_TICKS);
        long castStart = dragon.level().getGameTime() - Mth.floor(age);
        Vec3 origin = dragon.getPosition(partialTick);
        Strike strike = STRIKES.get(dragon);
        // Do not start a pulse that the move's cutoff would truncate.
        if (remainingTicks >= STRIKE_TICKS
                && (strike == null || strike.castStart != castStart || strike.cycle != cycle)) {
            var bounds = dragon.getBoundingBox().move(origin.subtract(dragon.position()));
            Vec3 center = bounds.getCenter();
            // Sample below the body, not from overhead: low ceilings must not become the impact.
            Vec3 probe = new Vec3(center.x, bounds.minY + 0.5D, center.z);
            var hit = dragon.level().clip(new ClipContext(probe, probe.add(0, -6, 0),
                    ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, dragon));
            Vec3 ground = hit.getType() == HitResult.Type.BLOCK && hit.getDirection().getStepY() > 0
                    ? hit.getLocation().add(0, 0.035D, 0) : null;
            long seed = dragon.getUUID().getLeastSignificantBits() ^ castStart ^ (cycle * 73428767L);
            RandomSource random = RandomSource.create(seed);
            double angle = random.nextDouble() * Math.PI * 2;
            double offset = 4.0D + random.nextDouble() * 3.0D;
            Vec3 overhead = new Vec3(center.x, bounds.maxY + 8.0D, center.z);
            Vec3 from = overhead.add(Math.cos(angle) * offset, 0, Math.sin(angle) * offset);
            double secondAngle = angle + Math.PI * (0.65D + random.nextDouble() * 0.7D);
            Vec3 secondFrom = overhead.add(Math.cos(secondAngle) * offset, -1.5D, Math.sin(secondAngle) * offset);
            strike = new Strike(castStart, cycle, from, secondFrom, ground, seed);
            STRIKES.put(dragon, strike);
            if (ground != null) RaevyxStormLightningRenderer.impact(dragon, ground, RandomSource.create(seed), true);
        }
        if (strike == null || strike.castStart != castStart || strike.ground == null) return;
        // Hold each impact in world space while the next strike follows the moving hitbox.
        // Use the saved pulse's age: modulo would revive it when new pulses are suppressed.
        float strikeAge = time - strike.cycle * STRIKE_TICKS;
        float alpha = smoothFade(1 - strikeAge / STRIKE_TICKS)
                * smoothFade(remainingTicks / STRIKE_TICKS);
        if (alpha <= 0.001F) return;
        boolean gold = dragon.getTextureVariant() == Raevyx.VARIANT_NIGHT_GOLD;
        renderBolt(poses, buffers, origin, strike.from, strike.ground, strike.seed, alpha, gold);
        renderBolt(poses, buffers, origin, strike.secondFrom, strike.ground,
                strike.seed ^ 0x9E3779B97F4A7C15L, alpha, gold);
    }

    private static float smoothFade(float value) {
        float t = Mth.clamp(value, 0, 1);
        return t * t * (3 - 2 * t);
    }

    private static void renderBolt(PoseStack poses, MultiBufferSource buffers, Vec3 origin,
                                   Vec3 start, Vec3 ground, long seed, float alpha, boolean gold) {
        Vec3 from = start.subtract(origin);
        Vec3 delta = ground.subtract(start);
        float length = (float) delta.length();
        if (length < 0.05F) return;
        Vec3 direction = delta.scale(1.0D / length);
        poses.pushPose();
        try {
            poses.translate(from.x, from.y, from.z);
            poses.mulPose(new Quaternionf().rotationTo(0, 0, 1,
                    (float) direction.x, (float) direction.y, (float) direction.z));
            ProceduralBeamLightningRenderer.emitBolt(buffers.getBuffer(RenderType.lightning()), poses.last().pose(),
                    length, seed, 1.2F, alpha, 1, gold ? 0.72F : 0.06F, gold ? 0.12F : 0.08F);
        } finally {
            poses.popPose();
        }
    }
}

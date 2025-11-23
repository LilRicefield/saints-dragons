package com.leon.saintsdragons.server.entity.controller.raevyx;

import com.leon.saintsdragons.server.entity.dragons.raevyx.Raevyx;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec3;
/** 
 * Clean physics controller for Raevyx - simple and maintainable
 */
public class RaevyxPhysicsController {
    private final Raevyx wyvern;

    // Physics envelopes for renderer effects
    private final Envelope01 glideEnv = new Envelope01(0.25f, 0.25f);
    private final Envelope01 flapEnv  = new Envelope01(0.25f, 0.18f);
    private final Envelope01 hoverEnv = new Envelope01(0.40f, 0.15f);

    // Animation fraction values for smooth blending
    public float glidingFraction = 0f;
    public float prevGlidingFraction = 0f;
    public float flappingFraction = 0f;
    public float prevFlappingFraction = 0f;
    public float hoveringFraction = 0f;
    public float prevHoveringFraction = 0f;

    // ===== Envelopes and lift model =====
    public static class Envelope01 {
        private float val = 0f;
        private float prev = 0f;
        private final float upRate;
        private final float downRate;
        public Envelope01(float upRate, float downRate) { this.upRate = upRate; this.downRate = downRate; }
        public void tickToward(float target) {
            prev = val;
            float rate = target > val ? upRate : downRate;
            val += (target - val) * rate;
            if (val < 0f) val = 0f; else if (val > 1f) val = 1f;
        }
        public float raw() { return val; }
        public float get(float pt) { return Mth.lerp(pt, prev, val); }
        public void setRaw(float v) { prev = val = Mth.clamp(v, 0f, 1f); }
    }

    // Physics envelopes enabled by default
    private static final float MASS = 1.3f;
    private static final float LIFT_K = 11.0f;
    private static final float CLIMB_COST = 6.0f;
    private static final float RESPONSE = 1.5f;
    public RaevyxPhysicsController(Raevyx wyvern) {
        this.wyvern = wyvern;
    }

    /**
     * Main tick method - call this from your entity's tick()
     */
    public void tick() {
        // Store previous values for interpolation
        prevGlidingFraction = glidingFraction;
        prevFlappingFraction = flappingFraction;
        prevHoveringFraction = hoveringFraction;

        updatePhysicsEnvelopes();
    }

    private void updatePhysicsEnvelopes() {
        Vec3 v = wyvern.getDeltaMovement();
        float vH = (float)Math.hypot(v.x, v.z);
        float vY = (float)v.y;

        float glideLift = LIFT_K * vH * vH;
        float climbNeed = vY > 0 ? (vY * CLIMB_COST) : 0f;
        float need = MASS + climbNeed - glideLift;

        float flapTarget = need <= 0 ? 0f : (need / (need + RESPONSE));
        flapTarget = Mth.clamp(flapTarget, 0f, 1f);

        // Treat hover as a near-stationary state: only when horizontal speed is tiny AND vertical nearly zero,
        // or when explicitly flagged (hovering/landing/beaming)
        float hoverTarget = (
                wyvern.isHovering() || wyvern.isLanding() || wyvern.isBeaming() ||
                (vH < 0.02f && Math.abs(vY) < 0.02f)
        ) ? 1f : 0f;
        float glideTarget = Mth.clamp(1f - flapTarget, 0.15f, 1f);

        // Explicit ascent bias so climbing always triggers visible flaps
        // Rider-controlled ascent
        if (wyvern.isFlying()) {
            if (wyvern.getControllingPassenger() != null && wyvern.isGoingUp()) {
                flapTarget = Math.max(flapTarget, 0.6f);
            } else if (vY > 0.06f) {
                // AI/physics ascent: scale bias by vertical speed
                float ascentBias = Mth.clamp((vY - 0.02f) * 3.0f, 0.2f, 0.8f);
                flapTarget = Math.max(flapTarget, ascentBias);
            }
            // Recompute glide target after bias
            glideTarget = Mth.clamp(1f - flapTarget, 0.15f, 1f);
        }

        flapEnv.tickToward(flapTarget);
        hoverEnv.tickToward(hoverTarget);
        glideEnv.tickToward(glideTarget);

        // Update animation fractions for renderer from envelopes
        glidingFraction = glideEnv.raw();
        flappingFraction = flapEnv.raw();
        hoveringFraction = hoverEnv.raw();
    }

    // ===== SAVE/LOAD SUPPORT =====
    public void writeToNBT(net.minecraft.nbt.CompoundTag tag) {
        // Store envelope values (authoritative for physics system)
        tag.putFloat("GlideVal", glideEnv.raw());
        tag.putFloat("FlapVal", flapEnv.raw());
        tag.putFloat("HoverVal", hoverEnv.raw());
    }

    public void readFromNBT(net.minecraft.nbt.CompoundTag tag) {
        // Restore all animation state after load
        if (tag.contains("GlideVal")) {
            glideEnv.setRaw(tag.getFloat("GlideVal"));
            flapEnv.setRaw(tag.getFloat("FlapVal"));
            hoverEnv.setRaw(tag.getFloat("HoverVal"));
        }

        glidingFraction = glideEnv.raw();
        flappingFraction = flapEnv.raw();
        hoveringFraction = hoverEnv.raw();

        prevGlidingFraction = glidingFraction;
        prevFlappingFraction = flappingFraction;
        prevHoveringFraction = hoveringFraction;
    }
}

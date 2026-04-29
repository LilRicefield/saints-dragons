package com.leon.saintsdragons.server.entity.ability.abilities.ignivorus;

import com.leon.saintsdragons.common.config.dragon.DragonAttributeConfigLoader;
import com.leon.saintsdragons.common.registry.ModSounds;
import com.leon.saintsdragons.server.entity.ability.DragonAbility;
import com.leon.saintsdragons.server.entity.ability.DragonAbilitySection;
import com.leon.saintsdragons.server.entity.ability.DragonAbilityType;
import com.leon.saintsdragons.server.entity.dragons.ignivorus.Ignivorus;
import com.leon.saintsdragons.server.entity.effect.ignivorus.IgnivorusMagmaBlockEntity;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;

import static com.leon.saintsdragons.server.entity.ability.DragonAbilitySection.AbilitySectionInfinite;
import static com.leon.saintsdragons.server.entity.ability.DragonAbilitySection.AbilitySectionType.ACTIVE;

/**
 * Charged fireball ability for Ignivorus.
 * Hold R to charge, release to fire. Longer charge = bigger, more explosive fireball.
 *
 * Charge levels:
 * - Level 1 (0-40 ticks): Base fireball
 * - Level 2 (40-80 ticks): 1.5x damage/radius/scale
 * - Level 3 (80-120 ticks): 2x damage/radius/scale (max)
 */
public class IgnivorusFireballAbility extends DragonAbility<Ignivorus> {
    // Use infinite section - ability continues until key release
    private static final DragonAbilitySection[] TRACK = new DragonAbilitySection[] {
            new AbilitySectionInfinite(ACTIVE)
    };

    private static final int COOLDOWN_TICKS = 20;
    private static final int MAGMA_LIFETIME_TICKS = 200;
    private static final double FIREBALL_SPEED = 3.5D;
    private static final int FIRE_RELEASE_TICKS = 15;

    // Charge thresholds (in ticks, 20 ticks = 1 second)
    private static final int CHARGE_LEVEL_2_TICKS = 25;
    private static final int CHARGE_LEVEL_3_TICKS = 50;
    private static final int MAX_CHARGE_TICKS = 95;
    private static final int MIN_CHARGE_DISPLAY_TICKS = 3;

    // Base stats (Level 1)
    private static final float BASE_SCALE = 4.0F;
    private static final double BASE_IMPACT_RADIUS = 8.0D;
    private static final float DEFAULT_IMPACT_DAMAGE = 70.0F;

    // Multipliers for higher charge levels
    private static final float LEVEL_2_MULTIPLIER = 1.5F;
    private static final float LEVEL_3_MULTIPLIER = 2.0F;

    private int chargeTicks = 0;
    private int lastChargeAnimLevel = 0;
    private boolean hasFired = false;
    private boolean releaseRequested = false;
    private int releaseTicks = 0;
    private int releaseChargeTicks = 0;
    private boolean level3HoldActive = false;

    public IgnivorusFireballAbility(DragonAbilityType<Ignivorus, IgnivorusFireballAbility> type,
                                    Ignivorus user) {
        super(type, user, TRACK, COOLDOWN_TICKS);
    }

    @Override
    public boolean tryAbility() {
        Ignivorus dragon = getUser();
        boolean airborne = dragon.isFlying() || dragon.isTakeoff() || dragon.isLanding() || dragon.isHovering();
        boolean hasRider = dragon.getControllingPassenger() != null;
        boolean aiUse = !dragon.isVehicle() && dragon.getTarget() != null;
        return (dragon.isPhase2Active() || airborne) && (hasRider || aiUse);
    }

    @Override
    protected void beginSection(DragonAbilitySection section) {
        if (section == null) {
            return;
        }
        if (section.sectionType == ACTIVE) {
            // Start charging
            chargeTicks = 0;
            hasFired = false;
            releaseRequested = false;
            releaseTicks = 0;
            releaseChargeTicks = 0;
            level3HoldActive = false;
            lastChargeAnimLevel = 0;
            getUser().setFireballChargeLevel(0);
        }
    }

    @Override
    protected boolean canContinueUsing() {
        Ignivorus dragon = getUser();
        return dragon.isAlive() && !dragon.isRemoved() && !dragon.isInWaterOrBubble();
    }

    @Override
    public void tickUsing() {
        if (hasFired) {
            return;
        }

        if (releaseRequested) {
            releaseTicks++;
            if (releaseTicks >= FIRE_RELEASE_TICKS) {
                fireFireball(releaseChargeTicks);
                hasFired = true;
                end();
            }
            return;
        }

        // Increment charge (cap at max)
        if (chargeTicks < MAX_CHARGE_TICKS) {
            chargeTicks++;
        }

        // Calculate current charge level (1-3)
        int currentChargeLevel = getChargeLevel(chargeTicks);

        boolean displayCharge = chargeTicks >= MIN_CHARGE_DISPLAY_TICKS;
        getUser().setFireballChargeLevel(displayCharge ? currentChargeLevel : 0);

        if (displayCharge && currentChargeLevel > lastChargeAnimLevel) {
            triggerChargeAnimation(currentChargeLevel);
            lastChargeAnimLevel = currentChargeLevel;
        }

        if (chargeTicks >= MAX_CHARGE_TICKS && !level3HoldActive) {
            triggerHoldAnimation();
            level3HoldActive = true;
        }
    }

    @Override
    public void interrupt() {
        resetChargeState();
        super.interrupt();
    }

    @Override
    public void end() {
        resetChargeState();
        super.end();
    }

    public void requestRelease() {
        if (hasFired || releaseRequested) {
            return;
        }
        releaseRequested = true;
        releaseTicks = 0;
        releaseChargeTicks = Math.max(1, chargeTicks);
        getUser().setFireballChargeLevel(0);
        triggerShootAnimation(getChargeLevel(releaseChargeTicks));
    }

    private int getChargeLevel(int ticks) {
        if (ticks >= CHARGE_LEVEL_3_TICKS) {
            return 3;
        } else if (ticks >= CHARGE_LEVEL_2_TICKS) {
            return 2;
        } else {
            return 1;
        }
    }

    private float getChargeMultiplier(int ticks) {
        int level = getChargeLevel(ticks);
        return switch (level) {
            case 3 -> LEVEL_3_MULTIPLIER;
            case 2 -> LEVEL_2_MULTIPLIER;
            default -> 1.0F;
        };
    }

    private void fireFireball(int chargeAtRelease) {
        Ignivorus dragon = getUser();
        if (!(dragon.level() instanceof ServerLevel server)) {
            return;
        }

        Vec3 direction = getAimDirection(dragon);
        Vec3 spawnPos = getFireballOrigin(dragon);

        float multiplier = getChargeMultiplier(chargeAtRelease);
        float damage = resolveImpactDamage() * multiplier;
        double radius = BASE_IMPACT_RADIUS * multiplier;
        float scale = BASE_SCALE * multiplier;

        IgnivorusMagmaBlockEntity fireball = new IgnivorusMagmaBlockEntity(server, spawnPos, dragon,
                radius, damage, MAGMA_LIFETIME_TICKS);
        fireball.setDeltaMovement(direction.scale(FIREBALL_SPEED));
        fireball.setVisualScale(scale);
        fireball.hasImpulse = true;
        server.addFreshEntity(fireball);
    }

    private void resetChargeState() {
        chargeTicks = 0;
        lastChargeAnimLevel = 0;
        hasFired = false;
        releaseRequested = false;
        releaseTicks = 0;
        releaseChargeTicks = 0;
        level3HoldActive = false;
        getUser().setFireballChargeLevel(0);
    }

    private Vec3 getFireballOrigin(Ignivorus dragon) {
        Vec3 origin = dragon.getFireBreathStartAnchor(1.0f);
        return origin != null ? origin : dragon.getEyePosition();
    }

    private Vec3 getAimDirection(Ignivorus dragon) {
        Entity rider = dragon.getControllingPassenger();
        if (rider instanceof Player player) {
            Vec3 view = player.getViewVector(1.0f);
            if (view.lengthSqr() > 1.0E-6) {
                return view.normalize();
            }
        }
        if (dragon.getTarget() != null) {
            // Aim at target's eye position with light leading
            var target = dragon.getTarget();
            Vec3 targetPos = target.getEyePosition();
            Vec3 lead = target.getDeltaMovement().scale(0.6);
            Vec3 aimPoint = targetPos.add(lead);
            Vec3 dir = aimPoint.subtract(getFireballOrigin(dragon));
            if (dir.lengthSqr() > 1.0E-6) {
                return dir.normalize();
            }
        }
        Vec3 look = dragon.getLookAngle();
        return look.lengthSqr() > 1.0E-6 ? look.normalize() : new Vec3(0.0D, 0.0D, 1.0D);
    }

    private float resolveImpactDamage() {
        return (float) DragonAttributeConfigLoader.getInstance()
                .getConfig(DragonAttributeConfigLoader.IGNIVORUS_ID)
                .abilityDamage("fireball", DEFAULT_IMPACT_DAMAGE);
    }

    private void triggerChargeAnimation(int level) {
        Ignivorus dragon = getUser();
        switch (level) {
            case 1 -> {
                dragon.triggerAnim("action", "fireball_level1_charge");
                if (!dragon.level().isClientSide) {
                    dragon.getSoundHandler().playMovingEntitySound(ModSounds.IGNIVORUS_LEVEL1_CHARGE.get(), 1.0f, 1.0f, 54);
                }
            }
            case 2 -> {
                dragon.triggerAnim("action", "fireball_level2_charge");
                if (!dragon.level().isClientSide) {
                    dragon.getSoundHandler().playMovingEntitySound(ModSounds.IGNIVORUS_LEVEL2_CHARGE.get(), 1.0f, 1.0f, 68);
                }
            }
            case 3 -> {
                dragon.triggerAnim("action", "fireball_level3_charge");
                if (!dragon.level().isClientSide) {
                    dragon.getSoundHandler().playMovingEntitySound(ModSounds.IGNIVORUS_LEVEL3_CHARGE.get(), 1.0f, 1.0f, 94);
                }
            }
            default -> { }
        }
    }

    private void triggerHoldAnimation() {
        getUser().triggerAnim("action", "fireball_level3_hold");
    }

    private void triggerShootAnimation(int level) {
        Ignivorus dragon = getUser();
        switch (level) {
            case 1 -> {
                dragon.triggerAnim("action", "fireball_level1_shoot");
                if (!dragon.level().isClientSide) {
                    dragon.getSoundHandler().playMovingEntitySound(ModSounds.IGNIVORUS_LEVEL1_SHOOTS.get(), 1.0f, 1.0f, 66);
                }
            }
            case 2 -> {
                dragon.triggerAnim("action", "fireball_level2_shoot");
                if (!dragon.level().isClientSide) {
                    dragon.getSoundHandler().playMovingEntitySound(ModSounds.IGNIVORUS_LEVEL2_SHOOTS.get(), 1.0f, 1.0f, 76);
                }
            }
            case 3 -> {
                dragon.triggerAnim("action", "fireball_level3_shoot");
                if (!dragon.level().isClientSide) {
                    dragon.getSoundHandler().playMovingEntitySound(ModSounds.IGNIVORUS_LEVEL3_SHOOTS.get(), 1.0f, 1.0f, 45);
                }
            }
            default -> {
                dragon.triggerAnim("action", "fireball_level1_shoot");
                if (!dragon.level().isClientSide) {
                    dragon.getSoundHandler().playMovingEntitySound(ModSounds.IGNIVORUS_LEVEL1_SHOOTS.get(), 1.0f, 1.0f, 66);
                }
            }
        }
    }
}

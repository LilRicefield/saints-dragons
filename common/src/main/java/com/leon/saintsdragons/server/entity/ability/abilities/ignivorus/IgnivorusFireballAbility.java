package com.leon.saintsdragons.server.entity.ability.abilities.ignivorus;

import com.leon.saintsdragons.common.config.dragon.DragonAttributeConfigLoader;
import com.leon.saintsdragons.common.registry.ModSounds;
import com.leon.saintsdragons.server.entity.ability.DragonAbility;
import com.leon.saintsdragons.server.entity.ability.DragonAbilitySection;
import com.leon.saintsdragons.server.entity.ability.DragonAbilityType;
import com.leon.saintsdragons.server.entity.dragons.ignivorus.Ignivorus;
import com.leon.saintsdragons.server.entity.effect.ignivorus.IgnivorusMagmaBlockEntity;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundSource;
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
 * - Level 1 (0-20 ticks): Base fireball
 * - Level 2 (20-40 ticks): 1.5x damage/radius/scale
 * - Level 3 (40-60 ticks): 2x damage/radius/scale (max)
 */
public class IgnivorusFireballAbility extends DragonAbility<Ignivorus> {
    // Use infinite section - ability continues until key release
    private static final DragonAbilitySection[] TRACK = new DragonAbilitySection[] {
            new AbilitySectionInfinite(ACTIVE)
    };

    private static final int COOLDOWN_TICKS = 20;
    private static final int MAGMA_LIFETIME_TICKS = 200;
    private static final double FIREBALL_SPEED = 3.5D;

    // Charge thresholds (in ticks, 20 ticks = 1 second)
    private static final int CHARGE_LEVEL_2_TICKS = 20;
    private static final int CHARGE_LEVEL_3_TICKS = 40;
    private static final int MAX_CHARGE_TICKS = 60;

    // Base stats (Level 1)
    private static final float BASE_SCALE = 4.0F;
    private static final double BASE_IMPACT_RADIUS = 8.0D;
    private static final float DEFAULT_IMPACT_DAMAGE = 70.0F;

    // Multipliers for higher charge levels
    private static final float LEVEL_2_MULTIPLIER = 1.5F;
    private static final float LEVEL_3_MULTIPLIER = 2.0F;

    private int chargeTicks = 0;
    private int lastChargeLevel = 0;
    private boolean hasFired = false;

    public IgnivorusFireballAbility(DragonAbilityType<Ignivorus, IgnivorusFireballAbility> type,
                                    Ignivorus user) {
        super(type, user, TRACK, COOLDOWN_TICKS);
    }

    @Override
    public boolean tryAbility() {
        Ignivorus dragon = getUser();
        boolean airborne = dragon.isFlying() || dragon.isTakeoff() || dragon.isLanding() || dragon.isHovering();
        return (dragon.isPhase2Active() || airborne) && dragon.getControllingPassenger() != null;
    }

    @Override
    protected void beginSection(DragonAbilitySection section) {
        if (section == null) {
            return;
        }
        if (section.sectionType == ACTIVE) {
            // Start charging
            chargeTicks = 0;
            lastChargeLevel = 0;
            hasFired = false;
            getUser().setFireballChargeLevel(0);
            // Play initial charge sound
            playChargeSound(0);
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

        // Increment charge (cap at max)
        if (chargeTicks < MAX_CHARGE_TICKS) {
            chargeTicks++;
        }

        // Calculate current charge level (1-3)
        int currentChargeLevel = getChargeLevel();

        // Update dragon's charge level for UI sync
        getUser().setFireballChargeLevel(currentChargeLevel);

        // Play sound when reaching new charge level
        if (currentChargeLevel > lastChargeLevel) {
            playChargeSound(currentChargeLevel);
            lastChargeLevel = currentChargeLevel;
        }
    }

    @Override
    public void interrupt() {
        // Fire the fireball on key release (interrupt)
        if (!hasFired && chargeTicks > 0) {
            fireFireball();
            hasFired = true;
        }
        // Reset charge level on dragon
        getUser().setFireballChargeLevel(0);
        super.interrupt();
    }

    @Override
    public void end() {
        // Also fire if ability ends naturally
        if (!hasFired && chargeTicks > 0) {
            fireFireball();
            hasFired = true;
        }
        // Reset charge level on dragon
        getUser().setFireballChargeLevel(0);
        super.end();
    }

    private int getChargeLevel() {
        if (chargeTicks >= CHARGE_LEVEL_3_TICKS) {
            return 3;
        } else if (chargeTicks >= CHARGE_LEVEL_2_TICKS) {
            return 2;
        } else {
            return 1;
        }
    }

    private float getChargeMultiplier() {
        int level = getChargeLevel();
        return switch (level) {
            case 3 -> LEVEL_3_MULTIPLIER;
            case 2 -> LEVEL_2_MULTIPLIER;
            default -> 1.0F;
        };
    }

    private void playChargeSound(int level) {
        Ignivorus dragon = getUser();
        if (dragon.level().isClientSide) {
            return;
        }

        // Use existing sounds with different pitches for charge levels
        float pitch = switch (level) {
            case 0 -> 0.5F;  // Initial low rumble
            case 1 -> 0.7F;  // Level 1 reached
            case 2 -> 0.9F;  // Level 2 reached
            case 3 -> 1.2F;  // Max charge reached
            default -> 0.5F;
        };

        float volume = switch (level) {
            case 0 -> 0.6F;
            case 1 -> 0.8F;
            case 2 -> 1.0F;
            case 3 -> 1.2F;
            default -> 0.6F;
        };

        // Use fire breath start sound for charging feedback
        dragon.level().playSound(null, dragon.blockPosition(),
                ModSounds.IGNIVORUS_FIRE_BREATH_START.get(),
                SoundSource.HOSTILE, volume, pitch);
    }

    private void fireFireball() {
        Ignivorus dragon = getUser();
        if (!(dragon.level() instanceof ServerLevel server)) {
            return;
        }

        // Trigger the shoot animation
        dragon.triggerAnim("action", "fireball_shoots");

        // Play shoot sound
        dragon.playSound(ModSounds.IGNIVORUS_FIREBALL_SHOOTS.get(), 1.5F, 0.9F + dragon.getRandom().nextFloat() * 0.2F);

        Vec3 direction = getAimDirection(dragon);
        Vec3 spawnPos = getMouthPosition(dragon);

        float multiplier = getChargeMultiplier();
        float damage = resolveImpactDamage() * multiplier;
        double radius = BASE_IMPACT_RADIUS * multiplier;
        float scale = BASE_SCALE * multiplier;

        IgnivorusMagmaBlockEntity fireball = new IgnivorusMagmaBlockEntity(server, spawnPos, dragon,
                radius, damage, MAGMA_LIFETIME_TICKS);
        fireball.setNoGravity(true);
        fireball.setDeltaMovement(direction.scale(FIREBALL_SPEED));
        fireball.setVisualScale(scale);
        fireball.hasImpulse = true;
        server.addFreshEntity(fireball);
    }

    private Vec3 getMouthPosition(Ignivorus dragon) {
        Vec3 mouth = dragon.getFireBreathStartAnchor(1.0f);
        return mouth != null ? mouth : dragon.getEyePosition();
    }

    private Vec3 getAimDirection(Ignivorus dragon) {
        Entity rider = dragon.getControllingPassenger();
        if (rider instanceof Player player) {
            Vec3 view = player.getViewVector(1.0f);
            if (view.lengthSqr() > 1.0E-6) {
                return view.normalize();
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
}

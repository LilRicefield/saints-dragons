package com.leon.saintsdragons.server.entity.ability.abilities.cindervane;

import com.leon.saintsdragons.common.config.dragon.DragonAttributeConfigLoader;
import com.leon.saintsdragons.common.registry.ModSounds;
import com.leon.saintsdragons.server.entity.ability.DragonAbility;
import com.leon.saintsdragons.server.entity.ability.DragonAbilitySection;
import com.leon.saintsdragons.server.entity.ability.DragonAbilityType;
import com.leon.saintsdragons.server.entity.dragons.cindervane.Cindervane;
import com.leon.saintsdragons.server.entity.dragons.cindervane.handlers.CindervaneAnimationHandler;
import com.leon.saintsdragons.server.entity.effect.cindervane.CindervaneMagmaBlockEntity;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.phys.Vec3;


import static com.leon.saintsdragons.server.entity.ability.DragonAbilitySection.AbilitySectionDuration;
import static com.leon.saintsdragons.server.entity.ability.DragonAbilitySection.AbilitySectionType.ACTIVE;
import static com.leon.saintsdragons.server.entity.ability.DragonAbilitySection.AbilitySectionType.RECOVERY;
import static com.leon.saintsdragons.server.entity.ability.DragonAbilitySection.AbilitySectionType.STARTUP;

public class CindervaneMagmaVolleyAbility extends DragonAbility<Cindervane> {
    private static final int MAX_VOLLEYS = 3;
    private static final int BLOCKS_PER_VOLLEY = 3;
    private static final int VOLLEY_INTERVAL_TICKS = 10;
    private static final int ACTIVE_DURATION_TICKS = VOLLEY_INTERVAL_TICKS * (MAX_VOLLEYS - 1) + 1;
    private static final int COOLDOWN_TICKS = 5;
    private static final int MAGMA_BLOCK_LIFETIME = 200;

    private static final DragonAbilitySection[] TRACK = new DragonAbilitySection[] {
            new AbilitySectionDuration(STARTUP, 3),
            new AbilitySectionDuration(ACTIVE, ACTIVE_DURATION_TICKS),
            new AbilitySectionDuration(RECOVERY, 5)
    };

    private static final double SPAWN_FORWARD_OFFSET = 5.0D;
    private static final double SPAWN_VERTICAL_OFFSET = 1.5D;
    private static final double VELOCITY_DOWN = -0.15D;
    private static final double VELOCITY_FORWARD = 0.55D;
    private static final double MAGMA_IMPACT_RADIUS = 7.0D;
    private static final float DEFAULT_IMPACT_DAMAGE = 20.0F;

    private int ticksSinceVolley;
    private int volleysFired;

    public CindervaneMagmaVolleyAbility(DragonAbilityType<Cindervane, CindervaneMagmaVolleyAbility> type,
                                        Cindervane user) {
        super(type, user, TRACK, COOLDOWN_TICKS);
    }

    @Override
    protected void beginSection(DragonAbilitySection section) {
        if (section == null) {
            return;
        }
        if (section.sectionType == STARTUP) {
            ticksSinceVolley = VOLLEY_INTERVAL_TICKS;
            volleysFired = 0;
            getUser().triggerAnim(CindervaneAnimationHandler.ACTION_CONTROLLER, "magma_volley");
            if (!getUser().level().isClientSide) {
                getUser().getSoundHandler().playMovingEntitySound(ModSounds.CINDERVANE_MAGMA_VOLLEY.get(), 2.0f, 1.0f, 66);
            }
        }
    }

    @Override
    public void tickUsing() {
        DragonAbilitySection section = getCurrentSection();
        if (section == null || section.sectionType != ACTIVE) {
            return;
        }

        ticksSinceVolley++;
        if (ticksSinceVolley >= VOLLEY_INTERVAL_TICKS && volleysFired < MAX_VOLLEYS) {
            ticksSinceVolley = 0;
            fireMagmaVolley();
            volleysFired++;
        }

        if (volleysFired >= MAX_VOLLEYS && ticksSinceVolley >= VOLLEY_INTERVAL_TICKS) {
            nextSection();
        }
    }

    private void fireMagmaVolley() {
        Cindervane dragon = getUser();
        if (!(dragon.level() instanceof ServerLevel server)) {
            return;
        }

        Vec3 origin = getMagmaVolleyOrigin(dragon);
        float baseYaw = dragon.yHeadRot;
        float basePitch = dragon.getXRot();

        for (int i = 0; i < BLOCKS_PER_VOLLEY; i++) {
            float yawOffset = (i - 1) * 9.5F + (dragon.getRandom().nextFloat() - 0.5F) * 6.0F;
            float pitchOffset = (dragon.getRandom().nextFloat() - 0.5F) * 4.0F;
            Vec3 direction = Vec3.directionFromRotation(basePitch + pitchOffset, baseYaw + yawOffset).normalize();
            Vec3 spawnPos = origin.add(direction.scale(SPAWN_FORWARD_OFFSET));

            CindervaneMagmaBlockEntity block = new CindervaneMagmaBlockEntity(server, spawnPos,
                    dragon, MAGMA_IMPACT_RADIUS, resolveImpactDamage(), MAGMA_BLOCK_LIFETIME);
            block.setDeltaMovement(direction.scale(VELOCITY_FORWARD).add(0.0D, VELOCITY_DOWN, 0.0D));
            server.addFreshEntity(block);
        }
    }

    private float resolveImpactDamage() {
        return (float) DragonAttributeConfigLoader.getInstance()
                .getConfig(DragonAttributeConfigLoader.CINDERVANE_ID)
                .abilityDamage("magma_volley", DEFAULT_IMPACT_DAMAGE);
    }

    private Vec3 getMagmaVolleyOrigin(Cindervane dragon) {
        Vec3 forward = dragon.getLookAngle();
        if (forward.lengthSqr() <= 1.0E-6D) {
            forward = new Vec3(0.0D, 0.0D, 1.0D);
        }
        return dragon.getBoundingBox().getCenter()
                .add(forward.normalize().scale(dragon.getBbWidth() * 0.75D))
                .add(0.0D, SPAWN_VERTICAL_OFFSET, 0.0D);
    }

}

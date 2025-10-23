package com.leon.saintsdragons.server.entity.ability.abilities.cindervane;

import com.leon.saintsdragons.server.entity.ability.DragonAbility;
import com.leon.saintsdragons.server.entity.ability.DragonAbilitySection;
import com.leon.saintsdragons.server.entity.ability.DragonAbilityType;
import com.leon.saintsdragons.server.entity.dragons.cindervane.Cindervane;
import com.leon.saintsdragons.server.entity.effect.cindervane.CindervaneMagmaBlockEntity;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;


import static com.leon.saintsdragons.server.entity.ability.DragonAbilitySection.AbilitySectionDuration;
import static com.leon.saintsdragons.server.entity.ability.DragonAbilitySection.AbilitySectionType.ACTIVE;
import static com.leon.saintsdragons.server.entity.ability.DragonAbilitySection.AbilitySectionType.RECOVERY;
import static com.leon.saintsdragons.server.entity.ability.DragonAbilitySection.AbilitySectionType.STARTUP;

public class CindervaneVolleyAbility extends DragonAbility<Cindervane> {
    private static final DragonAbilitySection[] TRACK = new DragonAbilitySection[] {
            new AbilitySectionDuration(STARTUP, 3),
            new AbilitySectionDuration(ACTIVE, 20),
            new AbilitySectionDuration(RECOVERY, 5)
    };

    private static final int MAX_VOLLEYS = 3;
    private static final int BLOCKS_PER_VOLLEY = 3;
    private static final int VOLLEY_INTERVAL_TICKS = 10;
    private static final int COOLDOWN_TICKS = 5;
    private static final int MAGMA_BLOCK_LIFETIME = 200;

    private static final double SPAWN_FORWARD_OFFSET = 5.0D;
    private static final double SPAWN_VERTICAL_OFFSET = 1.5D;
    private static final double VELOCITY_DOWN = -0.15D;
    private static final double VELOCITY_FORWARD = 0.55D;
    private static final double MAGMA_IMPACT_RADIUS = 7.0D;
    private static final float IMPACT_DAMAGE = 20.0F;

    private int ticksSinceVolley;
    private int volleysFired;

    public CindervaneVolleyAbility(DragonAbilityType<Cindervane, CindervaneVolleyAbility> type,
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
            getUser().triggerAnim("actions", "magma_blast");
            Level level = getLevel();
            level.playSound(null, getUser().blockPosition(), SoundEvents.BLAZE_SHOOT, getUser().getSoundSource(), 1.4F, 0.8F + getUser().getRandom().nextFloat() * 0.2F);
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
            fireVolley();
            volleysFired++;
        }

        if (volleysFired >= MAX_VOLLEYS && ticksSinceVolley >= VOLLEY_INTERVAL_TICKS) {
            nextSection();
        }
    }

    private void fireVolley() {
        Cindervane dragon = getUser();
        if (!(dragon.level() instanceof ServerLevel server)) {
            return;
        }

        Vec3 mouth = dragon.computeMouthOrigin(1.0f).add(0.0D, SPAWN_VERTICAL_OFFSET, 0.0D);
        float baseYaw = dragon.yHeadRot;
        float basePitch = dragon.getXRot();

        for (int i = 0; i < BLOCKS_PER_VOLLEY; i++) {
            float yawOffset = (i - 1) * 9.5F + (dragon.getRandom().nextFloat() - 0.5F) * 6.0F;
            float pitchOffset = (dragon.getRandom().nextFloat() - 0.5F) * 4.0F;
            float yaw = baseYaw + yawOffset;
            float pitch = basePitch + pitchOffset;

            Vec3 direction = Vec3.directionFromRotation(pitch, yaw).normalize();
            Vec3 spawnPos = mouth.add(direction.scale(SPAWN_FORWARD_OFFSET));

            CindervaneMagmaBlockEntity block = new CindervaneMagmaBlockEntity(server, spawnPos,
                    dragon, MAGMA_IMPACT_RADIUS, IMPACT_DAMAGE, MAGMA_BLOCK_LIFETIME);
            block.setDeltaMovement(direction.scale(VELOCITY_FORWARD).add(0.0D, VELOCITY_DOWN, 0.0D));
            server.addFreshEntity(block);
        }
    }
}

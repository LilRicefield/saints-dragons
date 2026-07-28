package com.leon.saintsdragons.server.entity.ability.abilities.cindervane;

import com.leon.saintsdragons.common.registry.ModSounds;
import com.leon.saintsdragons.server.entity.ability.DragonAbility;
import com.leon.saintsdragons.server.entity.ability.DragonAbilitySection;
import com.leon.saintsdragons.server.entity.ability.DragonAbilityType;
import com.leon.saintsdragons.server.entity.dragons.cindervane.Cindervane;
import com.leon.saintsdragons.server.entity.dragons.cindervane.handlers.CindervaneAnimationHandler;
import net.minecraft.world.entity.LivingEntity;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static com.leon.saintsdragons.server.entity.ability.DragonAbilitySection.AbilitySectionDuration;
import static com.leon.saintsdragons.server.entity.ability.DragonAbilitySection.AbilitySectionType.RECOVERY;
import static com.leon.saintsdragons.server.entity.ability.DragonAbilitySection.AbilitySectionType.STARTUP;

public class CindervaneDoubleBiteAbility extends DragonAbility<Cindervane> {
    private static final float BASE_DAMAGE_PER_BITE = 15.0F;
    private static final int[] DAMAGE_OUTPUT_TICKS = {4, 11};
    private static final int ANIMATION_TICKS = 16;
    private static final int SOUND_TICKS = 20;

    private static final DragonAbilitySection[] TRACK = {
            new AbilitySectionDuration(STARTUP, ANIMATION_TICKS),
            new AbilitySectionDuration(RECOVERY, 4)
    };

    private final boolean[] appliedHits = new boolean[DAMAGE_OUTPUT_TICKS.length];
    private final Set<Integer> firstBiteTargets = new HashSet<>();

    public CindervaneDoubleBiteAbility(
            DragonAbilityType<Cindervane, CindervaneDoubleBiteAbility> type,
            Cindervane user
    ) {
        super(type, user, TRACK, 20);
    }

    @Override
    protected void beginSection(DragonAbilitySection section) {
        if (section == null || section.sectionType != STARTUP) {
            return;
        }

        Cindervane dragon = getUser();
        String animation = dragon.isAerial() ? "double_bite_air" : "double_bite";
        dragon.triggerAnim(CindervaneAnimationHandler.ACTION_CONTROLLER, animation);
        if (!dragon.level().isClientSide) {
            dragon.getSoundHandler().playMovingEntitySound(
                    ModSounds.CINDERVANE_DOUBLE_BITE.get(), 1.2F, 1.0F, SOUND_TICKS
            );
        }

        for (int i = 0; i < appliedHits.length; i++) {
            appliedHits[i] = false;
        }
        firstBiteTargets.clear();
    }

    @Override
    public void tickUsing() {
        Cindervane dragon = getUser();
        if (dragon.level().isClientSide || getCurrentSection() == null
                || getCurrentSection().sectionType != STARTUP) {
            return;
        }

        int tick = getTicksInUse();
        for (int i = 0; i < DAMAGE_OUTPUT_TICKS.length; i++) {
            if (!appliedHits[i] && tick >= DAMAGE_OUTPUT_TICKS[i]) {
                applyDamageOutput(dragon, i);
                appliedHits[i] = true;
            }
        }
    }

    @Override
    public void end() {
        firstBiteTargets.clear();
        super.end();
    }

    private void applyDamageOutput(Cindervane dragon, int biteIndex) {
        List<LivingEntity> targets = CindervaneBiteAbility.selectTargets(dragon);
        for (LivingEntity target : targets) {
            if (biteIndex > 0 && firstBiteTargets.contains(target.getId())) {
                target.invulnerableTime = 0;
            }
            boolean hurt = CindervaneBiteAbility.applyHit(
                    dragon, target, "double_bite", BASE_DAMAGE_PER_BITE
            );
            if (biteIndex == 0 && hurt) {
                firstBiteTargets.add(target.getId());
            }
        }
    }
}

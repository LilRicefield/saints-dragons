package com.leon.saintsdragons.server.entity.ability.abilities.atroxiia;

import com.leon.saintsdragons.common.config.dragon.DragonAttributeConfigLoader;
import com.leon.saintsdragons.common.registry.ModSounds;
import com.leon.saintsdragons.server.entity.ability.DragonAbility;
import com.leon.saintsdragons.server.entity.ability.DragonAbilitySection;
import com.leon.saintsdragons.server.entity.ability.DragonAbilityType;
import com.leon.saintsdragons.server.entity.ability.DragonMeleeGeometry;
import com.leon.saintsdragons.server.entity.dragons.atroxiia.Atroxiia;
import com.leon.saintsdragons.util.animation.AnimationHelper;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;

import java.util.List;

import static com.leon.saintsdragons.server.entity.ability.DragonAbilitySection.AbilitySectionDuration;
import static com.leon.saintsdragons.server.entity.ability.DragonAbilitySection.AbilitySectionType.STARTUP;

public class AtroxiiaGungnirStabAbility extends DragonAbility<Atroxiia> {
    private static final int ANIMATION_TICKS = 37;
    private static final int FIRST_NUDGE_TICK = 12;
    private static final int SECOND_NUDGE_AND_DAMAGE_TICK = 17;
    private static final int SHORT_NUDGE_TICKS = 4;
    private static final int FAR_NUDGE_TICKS = 7;
    private static final double SHORT_NUDGE_DISTANCE = 2.0D;
    private static final double FAR_NUDGE_DISTANCE = 8.0D;
    private static final double DAMAGE_RANGE = 20.0D;
    private static final double DAMAGE_HORIZONTAL = 3.0D;
    private static final double DAMAGE_VERTICAL = 4.0D;
    private static final float DEFAULT_DAMAGE = 40.0F;
    private static final int POST_HIT_STUN_TICKS = 40;

    private static final DragonAbilitySection[] TRACK = {
            new AbilitySectionDuration(STARTUP, ANIMATION_TICKS)
    };

    private boolean shortNudgeApplied;
    private boolean farNudgeApplied;
    private float committedAiYaw;

    public AtroxiiaGungnirStabAbility(
            DragonAbilityType<Atroxiia, AtroxiiaGungnirStabAbility> type,
            Atroxiia user
    ) {
        super(type, user, TRACK, 30);
    }

    @Override
    public boolean tryAbility() {
        Atroxiia dragon = getUser();
        if (dragon.getControllingPassenger() instanceof Player rider) {
            return dragon.isTame()
                    && dragon.isOwnedBy(rider)
                    && dragon.canUseGroundCombatAbility();
        }
        return !dragon.isVehicle() && dragon.canUseGroundCombatAbility();
    }

    @Override
    protected void beginSection(DragonAbilitySection section) {
        if (section == null || section.sectionType != STARTUP) {
            return;
        }

        shortNudgeApplied = false;
        farNudgeApplied = false;
        Atroxiia dragon = getUser();
        committedAiYaw = dragon.getYRot();
        dragon.triggerAnim(AnimationHelper.MOVEMENT_CONTROLLER, "gungnir_stab");
        if (!dragon.level().isClientSide) {
            dragon.getSoundHandler().playMovingEntitySound(
                    ModSounds.ATROXIIA_GUNGNIR_STAB.get(), 1.0F, 1.0F, 60
            );
        }
    }

    @Override
    public void tickUsing() {
        Atroxiia dragon = getUser();
        if (dragon.level().isClientSide
                || getCurrentSection() == null
                || getCurrentSection().sectionType != STARTUP) {
            return;
        }

        if (!dragon.isVehicle()) {
            dragon.setYRot(committedAiYaw);
            dragon.yBodyRot = committedAiYaw;
            dragon.yHeadRot = committedAiYaw;
        }

        int tick = getTicksInUse();
        if (!shortNudgeApplied && tick >= FIRST_NUDGE_TICK) {
            dragon.beginPreciseStrikeNudge(SHORT_NUDGE_TICKS, SHORT_NUDGE_DISTANCE);
            shortNudgeApplied = true;
        }
        if (!farNudgeApplied && tick >= SECOND_NUDGE_AND_DAMAGE_TICK) {
            dragon.beginPreciseStrikeNudge(FAR_NUDGE_TICKS, FAR_NUDGE_DISTANCE);
            damageTargets(dragon);
            farNudgeApplied = true;
        }
    }

    private void damageTargets(Atroxiia dragon) {
        float damage = (float) DragonAttributeConfigLoader.getInstance()
                .getConfig(DragonAttributeConfigLoader.ATROXIIA_ID)
                .abilityDamage("gungnir_stab", DEFAULT_DAMAGE);
        int stunTicks = Math.max(1, ANIMATION_TICKS - getTicksInUse() + POST_HIT_STUN_TICKS);
        List<LivingEntity> targets = DragonMeleeGeometry.findBodySweepTargets(
                dragon,
                DAMAGE_RANGE,
                DAMAGE_HORIZONTAL,
                DAMAGE_VERTICAL,
                1.0D,
                target -> !dragon.isAlly(target)
        );
        for (LivingEntity target : targets) {
            target.hurt(dragon.level().damageSources().magic(), damage);
            AtroxiiaFrostImpact.apply(dragon, target, stunTicks);
        }
    }
}

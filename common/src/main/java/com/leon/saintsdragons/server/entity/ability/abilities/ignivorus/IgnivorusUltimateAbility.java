package com.leon.saintsdragons.server.entity.ability.abilities.ignivorus;

import com.leon.saintsdragons.server.entity.ability.DragonAbility;
import com.leon.saintsdragons.server.entity.ability.DragonAbilitySection;
import com.leon.saintsdragons.server.entity.ability.DragonAbilityType;
import com.leon.saintsdragons.server.entity.dragons.ignivorus.Ignivorus;
import net.minecraft.world.phys.Vec3;

import static com.leon.saintsdragons.server.entity.ability.DragonAbilitySection.AbilitySectionDuration;
import static com.leon.saintsdragons.server.entity.ability.DragonAbilitySection.AbilitySectionType.ACTIVE;
import static com.leon.saintsdragons.server.entity.ability.DragonAbilitySection.AbilitySectionType.RECOVERY;
import static com.leon.saintsdragons.server.entity.ability.DragonAbilitySection.AbilitySectionType.STARTUP;

/**
 * Cinematic "ultimate" ability: plays a 3-stage animation sequence (start, loop, end) while
 * locking rider controls. Sound effects are triggered via animation keyframes.
 *
 * Animation timing:
 * - ultimate_start: 1.38s (28 ticks)
 * - ultimate: 5.42s (108 ticks)
 * - ultimate_end: 1.38s (28 ticks)
 * Total: ~8.2 seconds
 */
public class IgnivorusUltimateAbility extends DragonAbility<Ignivorus> {
    private static final int ULTIMATE_START_TICKS = 28;      // 1.38s animation.ignivorus.ultimate_start
    private static final int ULTIMATE_LOOP_TICKS = 108;      // 5.42s animation.ignivorus.ultimate
    private static final int ULTIMATE_END_TICKS = 28;        // 1.38s animation.ignivorus.ultimate_end
    private static final int TOTAL_SEQUENCE_TICKS = ULTIMATE_START_TICKS + ULTIMATE_LOOP_TICKS + ULTIMATE_END_TICKS;
    private static final int COOLDOWN_TICKS = 20 * 60; // 60s cooldown

    // Tick thresholds for animation transitions
    @SuppressWarnings("unused")
    private static final int START_END_TICK = ULTIMATE_START_TICKS;
    @SuppressWarnings("unused")
    private static final int LOOP_END_TICK = ULTIMATE_START_TICKS + ULTIMATE_LOOP_TICKS;

    private static final DragonAbilitySection[] TRACK = new DragonAbilitySection[] {
            new AbilitySectionDuration(STARTUP, TOTAL_SEQUENCE_TICKS),
            new AbilitySectionDuration(ACTIVE, 1),
            new AbilitySectionDuration(RECOVERY, 10)
    };

    private boolean lockedControls;
    private boolean startAnimPlayed;
    private boolean loopAnimPlayed;
    private boolean endAnimPlayed;

    public IgnivorusUltimateAbility(DragonAbilityType<Ignivorus, IgnivorusUltimateAbility> type,
                                    Ignivorus user) {
        super(type, user, TRACK, COOLDOWN_TICKS);
    }

    @Override
    public boolean canUse() {
        Ignivorus dragon = getUser();
        if (!dragon.onGround()) {
            return false;
        }
        if (dragon.isFlying() || dragon.isHovering() || dragon.isTakeoff() || dragon.isLanding()) {
            return false;
        }
        return super.canUse();
    }

    @Override
    protected void beginSection(DragonAbilitySection section) {
        if (section == null) {
            return;
        }

        Ignivorus dragon = getUser();

        if (section.sectionType == STARTUP) {
            // Lock controls for the full sequence duration
            dragon.lockRiderControls(TOTAL_SEQUENCE_TICKS);
            lockedControls = true;
            dragon.markLandedNow();
            dragon.setHovering(false);
            dragon.setLanding(false);
            dragon.setTakeoff(false);
            dragon.setDeltaMovement(Vec3.ZERO);

            // Reset animation tracking flags
            startAnimPlayed = false;
            loopAnimPlayed = false;
            endAnimPlayed = false;

            // Play ONLY the first animation (start)
            dragon.triggerAnim("action", "ultimate_start");
            startAnimPlayed = true;
        }
    }

    @Override
    public void tickUsing() {
        DragonAbilitySection section = getCurrentSection();
        if (section == null || section.sectionType != STARTUP) {
            return;
        }

        Ignivorus dragon = getUser();
        int ticks = getTicksInSection();

        // Manually trigger each animation when the previous one finishes
        // This prevents gaps/flickers between animations

        if (!loopAnimPlayed && ticks >= START_END_TICK) {
            dragon.triggerAnim("action", "ultimate");
            loopAnimPlayed = true;
        }

        if (!endAnimPlayed && ticks >= LOOP_END_TICK) {
            dragon.triggerAnim("action", "ultimate_end");
            endAnimPlayed = true;
        }
    }

    @Override
    protected void endSection(DragonAbilitySection section) {
        if (section != null && section.sectionType == STARTUP) {
            releaseLocks();
        }
    }

    @Override
    public void interrupt() {
        releaseLocks();
        super.interrupt();
    }

    @Override
    public void end() {
        releaseLocks();
        super.end();
    }

    private void releaseLocks() {
        if (lockedControls) {
            getUser().clearRiderControlLock();
            lockedControls = false;
        }
    }
}

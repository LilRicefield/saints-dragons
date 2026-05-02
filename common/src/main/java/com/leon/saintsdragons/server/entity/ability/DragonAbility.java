package com.leon.saintsdragons.server.entity.ability;

import com.leon.saintsdragons.server.entity.ability.DragonAbilitySection.*;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.Level;
import software.bernie.geckolib.animatable.GeoEntity;
import software.bernie.geckolib.core.animation.AnimationState;
import software.bernie.geckolib.core.animation.RawAnimation;
import software.bernie.geckolib.core.object.PlayState;

import java.util.Random;


public abstract class DragonAbility<T extends LivingEntity> {
    private final DragonAbilitySection[] sectionTrack;
    protected int cooldownMax;
    private final DragonAbilityType<T, ? extends DragonAbility<T>> abilityType;
    private final T user;
    private int ticksInUse;
    private int ticksInSection;
    private int currentSectionIndex;
    private boolean isUsing;
    private int cooldownTimer;
    protected Random rand;
    protected RawAnimation activeAnimation;

    public DragonAbility(DragonAbilityType<T, ? extends DragonAbility<T>> abilityType, T user,
                         DragonAbilitySection[] sectionTrack, int cooldownMax) {
        this.abilityType = abilityType;
        this.user = user;
        this.sectionTrack = sectionTrack;
        this.cooldownMax = cooldownMax;
        this.rand = new Random();
    }

    public DragonAbility(DragonAbilityType<T, ? extends DragonAbility<T>> abilityType, T user,
                         DragonAbilitySection[] sectionTrack) {
        this(abilityType, user, sectionTrack, 0);
    }



    public void start() {
        if (user instanceof DragonAbilityEntity) {
            ((DragonAbilityEntity) user).setActiveAbility(this);
        }
        ticksInUse = 0;
        ticksInSection = 0;
        currentSectionIndex = 0;
        isUsing = true;
        beginSection(getSectionTrack()[0]);
    }

    public void tick() {
        if (isUsing()) {
            if (getUser().isEffectiveAi() && !canContinueUsing()) {
                interrupt();
                return;
            }

            tickUsing();

            ticksInUse++;
            ticksInSection++;
            DragonAbilitySection section = getCurrentSection();
            if (section instanceof AbilitySectionInstant) {
                nextSection();
            } else if (section instanceof AbilitySectionDuration sectionDuration) {
                if (ticksInSection >= sectionDuration.duration) nextSection();
            }
        } else {
            tickNotUsing();
            if (getCooldownTimer() > 0) cooldownTimer--;
        }
    }

    public void end() {
        ticksInUse = 0;
        ticksInSection = 0;
        isUsing = false;
        cooldownTimer = getMaxCooldown();
        currentSectionIndex = 0;
        if (user instanceof DragonAbilityEntity) {
            ((DragonAbilityEntity) user).setActiveAbility(null);
        }
    }

    public void interrupt() {
        end();
    }

    public void complete() {
        end();
    }

    @SuppressWarnings("unused")
    public void playAnimation(RawAnimation animation) {
        activeAnimation = animation;
        // Delegate to entity so it can trigger a synced GeckoLib animation (server → clients)
        if (user instanceof DragonAbilityEntity) {
            ((DragonAbilityEntity) user).playDragonAnimation(animation);
        }
    }

    @SuppressWarnings("unused")
    public <E extends GeoEntity> PlayState animationPredicate(AnimationState<E> e) {
        if (activeAnimation == null || activeAnimation.getAnimationStages().isEmpty())
            return PlayState.STOP;
        e.getController().setAnimation(activeAnimation);
        return PlayState.CONTINUE;
    }
    public void nextSection() {
        jumpToSection(currentSectionIndex + 1);
    }

    public void jumpToSection(int sectionIndex) {
        endSection(getCurrentSection());
        currentSectionIndex = sectionIndex;
        ticksInSection = 0;
        if (currentSectionIndex >= getSectionTrack().length) {
            complete();
        } else {
            beginSection(getCurrentSection());
        }
    }


    public boolean isOverlayAbility() {
        return false;
    }
    public void tickUsing() {}
    public void tickNotUsing() {}
    protected void beginSection(DragonAbilitySection section) {}
    protected void endSection(DragonAbilitySection section) {}
    public boolean canUse() {
        return !isUsing() && cooldownTimer == 0;
    }
    public boolean tryAbility() {
        return true;
    }
    protected boolean canContinueUsing() {
        return true;
    }

    public int getRecoveryTicks() {
        return 10;
    }

    public int getInterruptRecoveryTicks() {
        return getRecoveryTicks();
    }

    @SuppressWarnings("unused")
    public boolean damageInterrupts() {
        return false;
    }

    public boolean isUsing() {
        return isUsing;
    }

    public T getUser() {
        return user;
    }

    public Level getLevel() {
        return user.level();
    }

    @SuppressWarnings("unused")
    public int getTicksInUse() {
        return ticksInUse;
    }

    public int getTicksInSection() {
        return ticksInSection;
    }

    public int getCooldownTimer() {
        return cooldownTimer;
    }

    public DragonAbilitySection getCurrentSection() {
        if (currentSectionIndex >= getSectionTrack().length) return null;
        return getSectionTrack()[currentSectionIndex];
    }

    @SuppressWarnings("unused")
    public int getCurrentSectionIndex() {
        return currentSectionIndex;
    }

    public DragonAbilitySection[] getSectionTrack() {
        return sectionTrack;
    }

    public int getMaxCooldown() {
        return cooldownMax;
    }

    public DragonAbilityType<T, ? extends DragonAbility<T>> getAbilityType() {
        return abilityType;
    }

    @SuppressWarnings("unused")
    public boolean isAnimating() {
        return isUsing();
    }

    @SuppressWarnings("unused")
    public interface DragonAbilityEntity {
        void setActiveAbility(DragonAbility<?> ability);
        DragonAbility<?> getActiveAbility();
        void playDragonAnimation(RawAnimation animation);
        DragonAbilityType<?, ?>[] getDragonAbilities();
    }
}

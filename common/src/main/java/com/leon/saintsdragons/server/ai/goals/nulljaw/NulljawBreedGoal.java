package com.leon.saintsdragons.server.ai.goals.nulljaw;

import com.leon.saintsdragons.server.ai.goals.base.DragonBreedGoal;
import com.leon.saintsdragons.server.entity.dragons.nulljaw.Nulljaw;
import com.leon.saintsdragons.server.entity.dragons.util.DragonBreedingRules;
import net.minecraft.advancements.CriteriaTriggers;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.stats.Stats;
import net.minecraft.world.entity.AgeableMob;
import net.minecraft.world.entity.ExperienceOrb;
import net.minecraft.world.level.GameRules;

public final class NulljawBreedGoal extends DragonBreedGoal<Nulljaw> {
    private static final double BREED_FLIGHT_SPEED = 0.22D;

    public NulljawBreedGoal(Nulljaw dragon, double speedModifier, double partnerRange, double breedDistanceSqr) {
        super(dragon, speedModifier, Nulljaw.class, partnerRange, breedDistanceSqr);
    }

    @Override
    protected boolean isBreedingAllowed() {
        return DragonBreedingRules.isEnabled() && this.dragon.isInLove();
    }

    @Override
    public void tick() {
        if (this.partner == null) {
            return;
        }

        this.dragon.getLookControl().setLookAt(this.partner, 10.0F, (float) this.dragon.getMaxHeadXRot());
        boolean closeEnough = isCloseEnoughToBreed();
        if (closeEnough) {
            stopBreedingMovement(this.dragon);
        } else {
            this.dragon.beginAiFlight();
            this.dragon.getAIMovement().setWaypoint(this.partner, BREED_FLIGHT_SPEED);
        }

        ++this.loveTime;
        if (this.loveTime >= 60 && closeEnough) {
            handleBreed();
        }
    }

    @Override
    protected void handleBreed() {
        if (!(this.level instanceof ServerLevel serverLevel) || this.partner == null) {
            return;
        }

        if (!DragonBreedingRules.isEnabled()) {
            this.dragon.resetLove();
            this.partner.resetLove();
            return;
        }

        Nulljaw parentForOffspring = this.dragon.getOwnerUUID() != null ? this.dragon : this.partner;
        Nulljaw otherParent = parentForOffspring == this.dragon ? this.partner : this.dragon;
        AgeableMob baby = parentForOffspring.getBreedOffspring(serverLevel, otherParent);
        if (!(baby instanceof Nulljaw babyNulljaw)) {
            return;
        }

        this.dragon.resetLove();
        this.partner.resetLove();
        if (this.dragon.getAge() == 0) {
            this.dragon.setAge(6000);
        }
        if (this.partner.getAge() == 0) {
            this.partner.setAge(6000);
        }

        babyNulljaw.moveTo(
                (this.dragon.getX() + this.partner.getX()) * 0.5D,
                (this.dragon.getY() + this.partner.getY()) * 0.5D,
                (this.dragon.getZ() + this.partner.getZ()) * 0.5D,
                this.dragon.getYRot(),
                0.0F
        );
        serverLevel.addFreshEntityWithPassengers(babyNulljaw);
        this.level.broadcastEntityEvent(this.dragon, (byte) 18);

        if (this.level.getGameRules().getBoolean(GameRules.RULE_DOMOBLOOT)) {
            this.level.addFreshEntity(new ExperienceOrb(
                    this.level,
                    babyNulljaw.getX(),
                    babyNulljaw.getY(),
                    babyNulljaw.getZ(),
                    this.dragon.getRandom().nextInt(7) + 1
            ));
        }

        ServerPlayer serverPlayer = this.dragon.getLoveCause();
        if (serverPlayer == null && this.partner.getLoveCause() != null) {
            serverPlayer = this.partner.getLoveCause();
        }
        if (serverPlayer != null) {
            serverPlayer.awardStat(Stats.ANIMALS_BRED);
            CriteriaTriggers.BRED_ANIMALS.trigger(serverPlayer, this.dragon, this.partner, babyNulljaw);
        }

        stopBreedingMovement(this.dragon);
        stopBreedingMovement(this.partner);
    }
}

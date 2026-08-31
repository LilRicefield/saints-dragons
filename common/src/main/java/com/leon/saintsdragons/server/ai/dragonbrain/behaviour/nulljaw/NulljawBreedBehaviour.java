package com.leon.saintsdragons.server.ai.dragonbrain.behaviour.nulljaw;

import com.leon.saintsdragons.server.ai.dragonbrain.DragonBrainContext;
import com.leon.saintsdragons.server.ai.dragonbrain.DragonMemories;
import com.leon.saintsdragons.server.ai.dragonbrain.DragonMovementIntent;
import com.leon.saintsdragons.server.ai.dragonbrain.behaviour.DragonBreedBehaviour;
import com.leon.saintsdragons.server.data.DragonCodexSavedData;
import com.leon.saintsdragons.server.entity.dragons.nulljaw.Nulljaw;
import com.leon.saintsdragons.server.entity.dragons.util.DragonBreedingRules;
import net.minecraft.advancements.CriteriaTriggers;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.stats.Stats;
import net.minecraft.world.entity.AgeableMob;
import net.minecraft.world.entity.ExperienceOrb;
import net.minecraft.world.level.GameRules;

public final class NulljawBreedBehaviour extends DragonBreedBehaviour<Nulljaw> {
    private static final double BREED_FLIGHT_SPEED = 0.22D;

    public NulljawBreedBehaviour(double speedModifier, double partnerRange, double breedDistanceSqr) {
        super(speedModifier, Nulljaw.class, partnerRange, breedDistanceSqr);
    }

    @Override
    protected boolean breedingAllowed(Nulljaw dragon) {
        return DragonBreedingRules.isEnabled() && dragon.isAlive() && dragon.isInLove();
    }

    @Override
    protected boolean readyToBreed(Nulljaw dragon) {
        return partner != null;
    }

    @Override
    protected void tick(DragonBrainContext<Nulljaw> context) {
        Nulljaw dragon = context.dragon();
        if (partner == null) {
            return;
        }

        dragon.getLookControl().setLookAt(partner, 10.0F, dragon.getMaxHeadXRot());
        boolean close = closeEnough(dragon);
        if (close) {
            context.memories().erase(DragonMemories.MOVEMENT_INTENT);
            stopMovement(dragon);
        } else {
            dragon.beginAiFlight();
            context.memories().set(
                    DragonMemories.MOVEMENT_INTENT,
                    DragonMovementIntent.strictAir(
                            partner.position().add(0.0D, partner.getBbHeight() * 0.5D, 0.0D),
                            BREED_FLIGHT_SPEED
                    )
            );
        }

        loveTime = Math.min(60, loveTime + 1);
        if (loveTime >= 60 && close) {
            breed(context.level(), dragon);
        }
    }

    @Override
    protected void breed(ServerLevel level, Nulljaw dragon) {
        if (partner == null) {
            return;
        }
        if (!DragonBreedingRules.isEnabled()) {
            dragon.resetLove();
            partner.resetLove();
            return;
        }

        Nulljaw parentForOffspring = dragon.getOwnerUUID() != null ? dragon : partner;
        Nulljaw otherParent = parentForOffspring == dragon ? partner : dragon;
        AgeableMob offspring = parentForOffspring.getBreedOffspring(level, otherParent);
        if (!(offspring instanceof Nulljaw baby)) {
            return;
        }

        dragon.resetLove();
        partner.resetLove();
        if (dragon.getAge() == 0) {
            dragon.setAge(6000);
        }
        if (partner.getAge() == 0) {
            partner.setAge(6000);
        }

        baby.moveTo(
                (dragon.getX() + partner.getX()) * 0.5D,
                (dragon.getY() + partner.getY()) * 0.5D,
                (dragon.getZ() + partner.getZ()) * 0.5D,
                dragon.getYRot(),
                0.0F
        );
        if (!level.addFreshEntity(baby)) {
            return;
        }
        if (baby.isTame() && baby.getOwnerUUID() != null) {
            DragonCodexSavedData.get(level).addDragon(baby.getOwnerUUID(), baby);
        }
        level.broadcastEntityEvent(dragon, (byte)18);

        if (level.getGameRules().getBoolean(GameRules.RULE_DOMOBLOOT)) {
            level.addFreshEntity(new ExperienceOrb(
                    level,
                    baby.getX(),
                    baby.getY(),
                    baby.getZ(),
                    dragon.getRandom().nextInt(7) + 1
            ));
        }

        ServerPlayer player = dragon.getLoveCause();
        if (player == null) {
            player = partner.getLoveCause();
        }
        if (player != null) {
            player.awardStat(Stats.ANIMALS_BRED);
            CriteriaTriggers.BRED_ANIMALS.trigger(player, dragon, partner, baby);
        }

        stopMovement(dragon);
        stopMovement(partner);
    }
}

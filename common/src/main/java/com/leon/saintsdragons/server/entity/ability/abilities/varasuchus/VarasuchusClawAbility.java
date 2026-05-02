package com.leon.saintsdragons.server.entity.ability.abilities.varasuchus;

import com.leon.saintsdragons.common.registry.ModSounds;
import com.leon.saintsdragons.server.entity.ability.DragonAbility;
import com.leon.saintsdragons.server.entity.ability.DragonAbilitySection;
import com.leon.saintsdragons.server.entity.ability.DragonAbilityType;
import com.leon.saintsdragons.server.entity.ability.DragonMeleeGeometry;
import com.leon.saintsdragons.server.entity.dragons.varasuchus.Varasuchus;
import com.leon.saintsdragons.server.entity.dragons.util.DragonGriefingRules;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import java.util.List;

import static com.leon.saintsdragons.server.entity.ability.DragonAbilitySection.AbilitySectionDuration;
import static com.leon.saintsdragons.server.entity.ability.DragonAbilitySection.AbilitySectionType.ACTIVE;
import static com.leon.saintsdragons.server.entity.ability.DragonAbilitySection.AbilitySectionType.RECOVERY;
import static com.leon.saintsdragons.server.entity.ability.DragonAbilitySection.AbilitySectionType.STARTUP;

public class VarasuchusClawAbility extends DragonAbility<Varasuchus> {
    private static final float BASE_DAMAGE = 12.0f;
    private static final double RANGE = 6.5;
    private static final double CLAW_ANGLE_DEG = 100.0;
    private static final double CLAW_SWIPE_HORIZONTAL = 3.0;
    private static final double CLAW_SWIPE_VERTICAL = 4.0;
    private static final double BLOCK_BREAK_RANGE = 6.0;
    private static final double BLOCK_BREAK_WIDTH = 3.0;
    private static final double BLOCK_BREAK_HEIGHT = 6.0;

    private static final DragonAbilitySection[] TRACK = new DragonAbilitySection[] {
            new AbilitySectionDuration(STARTUP, 1),
            new AbilitySectionDuration(ACTIVE, 2),
            new AbilitySectionDuration(RECOVERY, 2)
    };

    private boolean appliedHit;
    private final boolean useLeftClaw;

    public VarasuchusClawAbility(DragonAbilityType<Varasuchus, VarasuchusClawAbility> type,
                                 Varasuchus user) {
        super(type, user, TRACK, 3);
        this.useLeftClaw = user.shouldUseLeftClaw();
        user.toggleClawSide();
    }

    @Override
    public boolean tryAbility() {
        return getUser().isPhaseTwoActive();
    }

    @Override
    public boolean isOverlayAbility() {
        return true;
    }

    @Override
    protected void beginSection(DragonAbilitySection section) {
        if (section == null) {
            return;
        }

        if (section.sectionType == STARTUP) {
            Varasuchus dragon = getUser();
            String animName = getAnimationName(dragon);
            dragon.triggerAnim("instant", animName);
            if (!dragon.level().isClientSide) {
                dragon.getSoundHandler().playMovingEntitySound(ModSounds.VARASUCHUSCLAW.get(), 1.0f, 1.0f, 30);
            }
            appliedHit = false;
        }
    }

    @Override
    public void tickUsing() {
        DragonAbilitySection section = getCurrentSection();
        if (section == null) {
            return;
        }

        if (section.sectionType == ACTIVE && !appliedHit) {
            Varasuchus dragon = getUser();
            List<LivingEntity> targets = findAllTargets();

            for (LivingEntity target : targets) {
                applyHit(dragon, target);
            }
            if (dragon.isVehicle()) {
                breakBlocksInPath(dragon);
            }

            appliedHit = true;
        }
    }

    private String getAnimationName(Varasuchus dragon) {
        if (shouldUseRunClawAnimation(dragon)) {
            return useLeftClaw ? "run_and_claw_left" : "run_and_claw_right";
        }
        return useLeftClaw ? "claw_left" : "claw_right";
    }

    private boolean shouldUseRunClawAnimation(Varasuchus dragon) {
        if (dragon.isInWaterOrBubble()) {
            return false;
        }

        if (dragon.getEffectiveGroundState() == 2) {
            return true;
        }

        return dragon.isAggressive() && dragon.getDeltaMovement().horizontalDistanceSqr() > 0.008D;
    }

    private void applyHit(Varasuchus dragon, LivingEntity target) {
        float damage = BASE_DAMAGE;
        AttributeInstance attackAttr = dragon.getAttribute(Attributes.ATTACK_DAMAGE);
        if (attackAttr != null) {
            double value = attackAttr.getValue();
            if (value > 0) {
                damage = (float) (value * 1.2);
            }
        }

        damage *= dragon.getHungerMeleeDamageMultiplier();
        DamageSource source = dragon.level().damageSources().mobAttack(dragon);
        target.hurt(source, damage);
    }


    private void breakBlocksInPath(Varasuchus dragon) {
        if (!(dragon.level() instanceof ServerLevel server)) {
            return;
        }
        if (!DragonGriefingRules.canDestroyBlocks(server)) {
            return;
        }
        DragonMeleeGeometry.ForwardAttack attack = DragonMeleeGeometry.forwardAttack(dragon);
        Vec3 start = attack.origin();
        Vec3 end = start.add(attack.forward().scale(BLOCK_BREAK_RANGE));
        AABB breakArea = new AABB(start, end)
                .inflate(BLOCK_BREAK_WIDTH, 0, BLOCK_BREAK_WIDTH)
                .expandTowards(0, BLOCK_BREAK_HEIGHT, 0);
        BlockPos minPos = new BlockPos(
                (int) Math.floor(breakArea.minX),
                (int) Math.floor(breakArea.minY),
                (int) Math.floor(breakArea.minZ)
        );
        BlockPos maxPos = new BlockPos(
                (int) Math.ceil(breakArea.maxX),
                (int) Math.ceil(breakArea.maxY),
                (int) Math.ceil(breakArea.maxZ)
        );

        BlockPos.MutableBlockPos cursor = new BlockPos.MutableBlockPos();

        double maxForward = BLOCK_BREAK_RANGE + 0.5;
        double maxLateral = BLOCK_BREAK_WIDTH + 0.5;
        double minVertical = -1.0;
        double maxVertical = BLOCK_BREAK_HEIGHT + 0.5;

        for (int x = minPos.getX(); x <= maxPos.getX(); x++) {
            for (int y = minPos.getY(); y <= maxPos.getY(); y++) {
                for (int z = minPos.getZ(); z <= maxPos.getZ(); z++) {
                    cursor.set(x, y, z);

                    if (!server.isLoaded(cursor)) {
                        continue;
                    }

                    Vec3 blockCenter = new Vec3(x + 0.5, y + 0.5, z + 0.5);
                    Vec3 offset = blockCenter.subtract(start);
                    double forward = offset.dot(attack.forward());
                    if (forward < 0.0 || forward > maxForward) {
                        continue;
                    }
                    Vec3 along = attack.forward().scale(forward);
                    double lateralDistance = offset.subtract(along).length();
                    if (lateralDistance > maxLateral) {
                        continue;
                    }

                    if (offset.y < minVertical || offset.y > maxVertical) {
                        continue;
                    }

                    BlockState state = server.getBlockState(cursor);
                    if (state.isAir() || !canBreakBlock(state)) {
                        continue;
                    }

                    server.destroyBlock(cursor, true, dragon);
                }
            }
        }
    }


    private boolean canBreakBlock(BlockState state) {
        if (state.liquid()) {
            return false;
        }

        float hardness = state.getDestroySpeed(getUser().level(), BlockPos.ZERO);
        if (hardness < 0) {
            return false;
        }

        return hardness <= 30.0f;
    }

    private List<LivingEntity> findAllTargets() {
        Varasuchus dragon = getUser();
        boolean ridden = dragon.getControllingPassenger() != null;
        double effectiveRange = RANGE;

        if (!ridden) {
            LivingEntity target = dragon.getTarget();
            if (DragonMeleeGeometry.isDirectAiTargetValid(dragon, target, 1.5D)) {
                return java.util.List.of(target);
            }
            return java.util.List.of();
        }

        return DragonMeleeGeometry.findForwardTargets(
                dragon,
                effectiveRange,
                CLAW_SWIPE_HORIZONTAL,
                CLAW_SWIPE_VERTICAL,
                CLAW_ANGLE_DEG,
                effectiveRange * 0.4D,
                entity -> !dragon.isAlly(entity)
        );
    }

}
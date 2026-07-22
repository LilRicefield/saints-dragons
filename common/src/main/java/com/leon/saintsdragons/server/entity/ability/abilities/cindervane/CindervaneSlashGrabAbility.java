package com.leon.saintsdragons.server.entity.ability.abilities.cindervane;

import com.leon.saintsdragons.common.config.dragon.DragonAttributeConfig;
import com.leon.saintsdragons.common.config.dragon.DragonAttributeConfigLoader;
import com.leon.saintsdragons.common.registry.ModSounds;
import com.leon.saintsdragons.server.entity.ability.DragonAbility;
import com.leon.saintsdragons.server.entity.ability.DragonAbilitySection;
import com.leon.saintsdragons.server.entity.ability.DragonAbilityType;
import com.leon.saintsdragons.server.entity.base.DragonEntity;
import com.leon.saintsdragons.server.entity.dragons.cindervane.Cindervane;
import com.leon.saintsdragons.server.entity.dragons.cindervane.handlers.CindervaneAnimationHandler;
import net.minecraft.util.Mth;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import java.util.Comparator;
import java.util.List;

import static com.leon.saintsdragons.server.entity.ability.DragonAbilitySection.AbilitySectionDuration;
import static com.leon.saintsdragons.server.entity.ability.DragonAbilitySection.AbilitySectionType.RECOVERY;
import static com.leon.saintsdragons.server.entity.ability.DragonAbilitySection.AbilitySectionType.STARTUP;

public class CindervaneSlashGrabAbility extends DragonAbility<Cindervane> {
    private static final float DEFAULT_DAMAGE_HIT_1 = 5.0f;
    private static final float DEFAULT_DAMAGE_HIT_2 = 7.0f;

    private static final int DEFAULT_HIT_1_TICK = 16;
    private static final int DEFAULT_DISMOUNT_TICK = 26;
    private static final int DEFAULT_MOUNT_TICK = 16;
    private static final int DEFAULT_ANIMATION_TICKS = 35;
    private static final double RELEASE_DROP_Y = -0.65D;

    private static final double GRAB_SIDE_OFFSET = 1.60D;
    private static final double GRAB_FORWARD_OFFSET = 5.25D;
    private static final double GRAB_VERTICAL_OFFSET = 1.15D;
    private static final double GRAB_SEARCH_RADIUS = 3.00D;
    private static final double DEFAULT_MAX_TARGET_WIDTH = 4.30D;
    private static final double DEFAULT_MAX_TARGET_HEIGHT = 4.30D;

    private static final DragonAbilitySection[] TRACK = new DragonAbilitySection[] {
            new AbilitySectionDuration(STARTUP, DEFAULT_ANIMATION_TICKS),
            new AbilitySectionDuration(RECOVERY, 5)
    };

    private int grabbedTargetId = -1;
    private int strikeTargetId = -1;
    private boolean hit1Applied;
    private boolean hit2Applied;
    private boolean releasedTarget;
    private boolean mountAttempted;

    public CindervaneSlashGrabAbility(DragonAbilityType<Cindervane, CindervaneSlashGrabAbility> type, Cindervane user) {
        super(type, user, TRACK, 7);
    }

    @Override
    public boolean tryAbility() {
        Cindervane dragon = getUser();
        return super.tryAbility()
                && dragon.isGroundedForAction()
                && !dragon.isFlying()
                && !dragon.isTakeoff()
                && !dragon.isHovering()
                && !dragon.isLanding();
    }

    @Override
    protected void beginSection(DragonAbilitySection section) {
        if (section == null) {
            return;
        }
        if (section.sectionType == STARTUP) {
            Cindervane dragon = getUser();
            dragon.triggerAnim(CindervaneAnimationHandler.MOVEMENT_CONTROLLER, "slash_left");
            if (!dragon.level().isClientSide) {
                dragon.getSoundHandler().playMovingEntitySound(ModSounds.CINDERVANE_SLASH.get(), 1.0f, 1.0f, 40);
            }
            grabbedTargetId = -1;
            strikeTargetId = -1;
            hit1Applied = false;
            hit2Applied = false;
            releasedTarget = false;
            mountAttempted = false;
        }
    }

    @Override
    public void tickUsing() {
        Cindervane dragon = getUser();
        if (dragon.level().isClientSide) {
            return;
        }
        DragonAttributeConfig config = DragonAttributeConfigLoader.getInstance()
                .getConfig(DragonAttributeConfigLoader.CINDERVANE_ID);

        int hit1Tick = Mth.clamp((int) Math.round(config.extraDouble("slash_grab_hit_tick", DEFAULT_HIT_1_TICK)), 1, 80);
        int dismountTick = Mth.clamp((int) Math.round(config.extraDouble("slash_grab_dismount_tick", DEFAULT_DISMOUNT_TICK)), hit1Tick + 1, 100);
        int mountTick = Mth.clamp((int) Math.round(config.extraDouble("slash_grab_mount_tick", DEFAULT_MOUNT_TICK)), 1, dismountTick - 1);
        int tick = getTicksInUse();

        if (getStrikeTarget() == null) {
            LivingEntity strikeCandidate = findStrikeCandidate();
            if (strikeCandidate != null) {
                strikeTargetId = strikeCandidate.getId();
            }
        }

        if (tick >= mountTick && tick < dismountTick && getGrabbedTarget() == null) {
            tryAutoMountTarget();
        }
        if (!mountAttempted && tick >= mountTick) {
            mountAttempted = true;
        }
        if (tick >= mountTick && tick < dismountTick) {
            holdGrabbedTarget();
        }

        if (!hit1Applied && tick >= hit1Tick) {
            LivingEntity target = resolveDamageTarget();
            if (target != null) {
                applyDamage(target, "slash_grab_hit1", DEFAULT_DAMAGE_HIT_1);
            }
            applySecondaryHitDamage("slash_grab_hit1", DEFAULT_DAMAGE_HIT_1, target);
            hit1Applied = true;
        }

        if (!hit2Applied && tick >= dismountTick) {
            LivingEntity target = resolveDamageTarget();
            if (target != null) {
                applyDamage(target, "slash_grab_hit2", DEFAULT_DAMAGE_HIT_2);
                if (target == getGrabbedTarget()) {
                    releaseAndFling(target);
                } else {
                    clearGrabState();
                }
            } else {
                clearGrabState();
            }
            applySecondaryHitDamage("slash_grab_hit2", DEFAULT_DAMAGE_HIT_2, target);
            hit2Applied = true;
            releasedTarget = true;
        }
    }

    @Override
    public void end() {
        clearGrabState();
        super.end();
    }

    private void tryAutoMountTarget() {
        LivingEntity target = findGrabCandidate();
        if (target == null) {
            return;
        }
        grabbedTargetId = target.getId();
    }

    private LivingEntity findGrabCandidate() {
        Cindervane dragon = getUser();
        DragonAttributeConfig config = DragonAttributeConfigLoader.getInstance()
                .getConfig(DragonAttributeConfigLoader.CINDERVANE_ID);
        double maxTargetWidth = Math.max(0.1D, config.extraDouble("slash_grab_max_target_width", DEFAULT_MAX_TARGET_WIDTH));
        double maxTargetHeight = Math.max(0.1D, config.extraDouble("slash_grab_max_target_height", DEFAULT_MAX_TARGET_HEIGHT));
        Vec3 look = dragon.getLookAngle().normalize();
        Vec3 right = new Vec3(-look.z, 0.0D, look.x).normalize();
        Vec3 grabPoint = dragon.position()
                .add(right.scale(GRAB_SIDE_OFFSET))
                .add(look.scale(GRAB_FORWARD_OFFSET))
                .add(0.0D, GRAB_VERTICAL_OFFSET, 0.0D);
        AABB box = new AABB(grabPoint, grabPoint).inflate(GRAB_SEARCH_RADIUS);

        List<LivingEntity> candidates = dragon.level().getEntitiesOfClass(LivingEntity.class, box, entity ->
                entity != dragon
                        && entity.isAlive()
                        && entity.attackable()
                        && !dragon.isAlly(entity)
                        && !(entity instanceof DragonEntity)
                        && entity.getBbWidth() <= maxTargetWidth
                        && entity.getBbHeight() <= maxTargetHeight
                        && !entity.isPassenger()
                        && !entity.isVehicle()
        );

        return candidates.stream()
                .min(Comparator.comparingDouble(e -> e.distanceToSqr(grabPoint)))
                .orElse(null);
    }

    private LivingEntity findStrikeCandidate() {
        List<LivingEntity> candidates = findStrikeCandidates();
        if (candidates.isEmpty()) {
            return null;
        }
        return candidates.get(0);
    }

    private List<LivingEntity> findStrikeCandidates() {
        Cindervane dragon = getUser();
        Vec3 look = dragon.getLookAngle().normalize();
        Vec3 right = new Vec3(-look.z, 0.0D, look.x).normalize();
        Vec3 strikePoint = dragon.position()
                .add(right.scale(GRAB_SIDE_OFFSET))
                .add(look.scale(GRAB_FORWARD_OFFSET))
                .add(0.0D, GRAB_VERTICAL_OFFSET, 0.0D);
        AABB box = new AABB(strikePoint, strikePoint).inflate(GRAB_SEARCH_RADIUS);

        return dragon.level().getEntitiesOfClass(LivingEntity.class, box, entity ->
                entity != dragon
                        && entity.isAlive()
                        && entity.attackable()
                        && !dragon.isAlly(entity)
                        && !entity.isPassenger()
        ).stream()
                .sorted(Comparator.comparingDouble(e -> e.distanceToSqr(strikePoint)))
                .toList();
    }

    private LivingEntity getGrabbedTarget() {
        if (grabbedTargetId < 0) {
            return null;
        }
        Entity entity = getUser().level().getEntity(grabbedTargetId);
        if (entity instanceof LivingEntity living && living.isAlive()) {
            return living;
        }
        return null;
    }

    private LivingEntity getStrikeTarget() {
        if (strikeTargetId < 0) {
            return null;
        }
        Entity entity = getUser().level().getEntity(strikeTargetId);
        if (entity instanceof LivingEntity living && living.isAlive()) {
            return living;
        }
        return null;
    }

    private LivingEntity resolveDamageTarget() {
        LivingEntity grabbed = getGrabbedTarget();
        if (grabbed != null) {
            return grabbed;
        }
        return getStrikeTarget();
    }

    private void applySecondaryHitDamage(String damageKey, float fallback, LivingEntity primaryTarget) {
        int primaryId = primaryTarget != null ? primaryTarget.getId() : -1;
        for (LivingEntity candidate : findStrikeCandidates()) {
            if (candidate.getId() == primaryId) {
                continue;
            }
            applyDamage(candidate, damageKey, fallback);
        }
    }

    private void applyDamage(LivingEntity target, String damageKey, float fallback) {
        Cindervane dragon = getUser();
        float damage = (float) DragonAttributeConfigLoader.getInstance()
                .getConfig(DragonAttributeConfigLoader.CINDERVANE_ID)
                .abilityDamage(damageKey, fallback);
        damage *= dragon.getHungerMeleeDamageMultiplier();
        DamageSource source = dragon.level().damageSources().mobAttack(dragon);
        target.hurt(source, damage);
    }

    private void releaseAndFling(LivingEntity target) {
        // Simple release: drop straight downward only.
        target.setDeltaMovement(0.0D, RELEASE_DROP_Y, 0.0D);
        target.hurtMarked = true;
        target.hasImpulse = true;
        clearGrabState();
    }

    private void clearGrabState() {
        grabbedTargetId = -1;
        strikeTargetId = -1;
    }

    private void holdGrabbedTarget() {
        LivingEntity target = getGrabbedTarget();
        if (target == null) {
            return;
        }
        Cindervane dragon = getUser();
        Vec3 holdPos = dragon.getBonePositionForPassenger("automountBoneRight");
        if (holdPos == null) {
            Vec3 look = dragon.getLookAngle().normalize();
            Vec3 right = new Vec3(-look.z, 0.0D, look.x).normalize();
            holdPos = dragon.position()
                    .add(right.scale(1.2D))
                    .add(look.scale(1.0D))
                    .add(0.0D, 1.0D, 0.0D);
        }
        Vec3 minus = holdPos.subtract(target.position());
        target.setDeltaMovement(minus);
        target.hurtMarked = true;
        target.hasImpulse = true;
        target.fallDistance = 0.0F;
    }
}

package com.leon.saintsdragons.server.entity.ability.abilities.raevyx;

import com.leon.saintsdragons.common.registry.ModParticles;
import com.leon.saintsdragons.common.registry.ModEntities;
import com.leon.saintsdragons.common.registry.ModSounds;
import com.leon.saintsdragons.server.entity.ability.DragonAbility;
import com.leon.saintsdragons.server.entity.ability.DragonAbilitySection;
import com.leon.saintsdragons.server.entity.ability.DragonAbilityType;
import com.leon.saintsdragons.server.entity.ability.DragonMeleeGeometry;
import com.leon.saintsdragons.server.entity.dragons.raevyx.Raevyx;
import com.leon.saintsdragons.server.entity.dragons.raevyx.handlers.RaevyxAnimationHandler;
import com.leon.saintsdragons.server.entity.effect.ImpactRingEntity;
import com.leon.saintsdragons.server.entity.effect.VisualFallingBlockEntity;
import com.leon.saintsdragons.server.entity.ability.DragonAbilitySection.AbilitySectionDuration;
import com.leon.saintsdragons.server.entity.ability.DragonAbilitySection.AbilitySectionType;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import java.util.HashMap;
import java.util.Map;

public class RaevyxGroundRendAbility extends DragonAbility<Raevyx> {
    private static final int STARTUP_TICKS = 20;
    private static final int ACTIVE_TICKS = 30;
    private static final int RECOVERY_TICKS = 20;
    private static final int COOLDOWN_TICKS = 32;
    private static final int GROUND_REND_SOUND_TICKS = 100;
    private static final int SLOWDOWN_START_TICKS = 25;
    private static final int STOP_TICKS = 55;
    private static final int GROUND_REND_TRAIL_END_TICKS = 40;
    private static final int FALLING_BLOCK_TICKS = 23;
    private static final int END_DUST_TICKS = 62;
    private static final double DUST_VIEW_DISTANCE = 128.0D;
    private static final double AI_STEER_BACK_RANGE = 6.0D;
    private static final float RIDER_SURGE_SPEED = 2.0F;
    private static final float RIDER_RECOVERY_END_SPEED = 0.25F;
    private static final double AI_FORWARD_SPEED = 0.9D;
    private static final double AI_RECOVERY_END_SPEED = AI_FORWARD_SPEED * (RIDER_RECOVERY_END_SPEED / RIDER_SURGE_SPEED);
    private static final float HIT_DAMAGE = 5.0F;
    private static final float SUPERCHARGED_HIT_DAMAGE = HIT_DAMAGE * 2.0F;
    private static final double HIT_KNOCKBACK = 0.55D;
    private static final int HIT_COOLDOWN_TICKS = 5;

    private final Map<Integer, Integer> hitCooldowns = new HashMap<>();
    private Vec3 aiGroundRendDir = Vec3.ZERO;
    private boolean spawnedStartDust = false;
    private boolean spawnedStartBlocks = false;
    private boolean spawnedEndDust = false;

    private static final DragonAbilitySection[] TRACK = new DragonAbilitySection[] {
            new AbilitySectionDuration(AbilitySectionType.STARTUP, STARTUP_TICKS),
            new AbilitySectionDuration(AbilitySectionType.ACTIVE, ACTIVE_TICKS),
            new AbilitySectionDuration(AbilitySectionType.RECOVERY, RECOVERY_TICKS)
    };

    public RaevyxGroundRendAbility(DragonAbilityType<Raevyx, RaevyxGroundRendAbility> type, Raevyx user) {
        super(type, user, TRACK, COOLDOWN_TICKS);
    }

    @Override
    public boolean tryAbility() {
        Raevyx wyvern = getUser();
        return !wyvern.isFlying()
                && !wyvern.isTakeoff()
                && !wyvern.isLanding()
                && !wyvern.isHovering()
                && !wyvern.isInWaterOrBubble()
                && wyvern.onGround();
    }

    @Override
    protected void beginSection(DragonAbilitySection section) {
        if (section == null) {
            return;
        }
        if (section.sectionType == AbilitySectionType.STARTUP) {
            getUser().triggerAnim(RaevyxAnimationHandler.MOVEMENT_CONTROLLER, "ground_rend");
            if (!getUser().level().isClientSide) {
                getUser().getSoundHandler().playMovingEntitySound(
                        ModSounds.RAEVYX_GROUND_REND.get(),
                        1.4f,
                        1.0f,
                        GROUND_REND_SOUND_TICKS
                );
            }
            getUser().setAccelerating(false);
            getUser().setGroundRending(true);
            getUser().setGroundRendTravelSpeed(0.0F);
            aiGroundRendDir = getForwardDir(getUser());
            spawnedStartDust = true;
            spawnedStartBlocks = false;
            spawnedEndDust = false;
            spawnGroundRendImpactRing(getUser());
            spawnGroundRendDustBurst(getUser(), 32, 0.95D, 0.28D);
        } else if (section.sectionType == AbilitySectionType.ACTIVE && !spawnedStartBlocks && getOverallTrackTick() >= FALLING_BLOCK_TICKS) {
            spawnedStartBlocks = true;
            spawnGroundRendFallingBlocks(getUser());
        }
    }
    @Override
    public void tickUsing() {
        hitCooldowns.entrySet().removeIf(entry -> {
            int next = entry.getValue() - 1;
            if (next <= 0) {
                return true;
            }
            entry.setValue(next);
            return false;
        });

        DragonAbilitySection section = getCurrentSection();
        if (section == null) {
            return;
        }

        Raevyx wyvern = getUser();
        if (!spawnedStartDust) {
            spawnedStartDust = true;
            spawnGroundRendImpactRing(wyvern);
            spawnGroundRendDustBurst(wyvern, 32, 0.95D, 0.28D);
        }
        if (!spawnedStartBlocks && getOverallTrackTick() >= FALLING_BLOCK_TICKS) {
            spawnedStartBlocks = true;
            spawnGroundRendFallingBlocks(wyvern);
        }
        if (!spawnedEndDust && getOverallTrackTick() >= END_DUST_TICKS) {
            spawnedEndDust = true;
            spawnGroundRendDustBurst(wyvern, 42, 1.15D, 0.34D);
        }

        if (wyvern.isFlying() || wyvern.isInWaterOrBubble()) {
            interrupt();
            return;
        }

        if (section.sectionType == AbilitySectionType.STARTUP && wyvern.isVehicle()) {
            Vec3 current = wyvern.getDeltaMovement();
            wyvern.setDeltaMovement(0.0D, current.y, 0.0D);
            wyvern.setGroundRendVelocity(Vec3.ZERO);
            wyvern.setGroundRendTravelSpeed(0.0F);
            return;
        }

        if (section.sectionType == AbilitySectionType.RECOVERY) {
            if (wyvern.isVehicle()) {
                float speed = getRiddenTravelSpeed();
                wyvern.setGroundRendTravelSpeed(speed);
                Vec3 current = wyvern.getDeltaMovement();
                if (speed <= 0.0F) {
                    wyvern.setGroundRendVelocity(Vec3.ZERO);
                    wyvern.setDeltaMovement(0.0D, current.y, 0.0D);
                    return;
                }
                Vec3 forward = getForwardDir(wyvern).scale(speed);
                wyvern.setGroundRendVelocity(forward);
                applyGroundRendHits(wyvern, getForwardDir(wyvern));
                if (shouldSpawnGroundRendTrailParticles()) {
                    wyvern.spawnGroundRendTrailParticles(getForwardDir(wyvern), speed);
                }
                return;
            }
            double speed = getAiTravelSpeed();
            if (speed <= 0.0D) {
                Vec3 current = wyvern.getDeltaMovement();
                wyvern.setGroundRendVelocity(new Vec3(0.0D, current.y, 0.0D));
                return;
            }
            if (aiGroundRendDir.lengthSqr() > 1.0E-6D) {
                Vec3 current = wyvern.getDeltaMovement();
                Vec3 trailDir = aiGroundRendDir.normalize();
                Vec3 targetVelocity = trailDir.scale(speed);
                wyvern.setGroundRendVelocity(new Vec3(targetVelocity.x, current.y, targetVelocity.z));
                if (shouldSpawnGroundRendTrailParticles()) {
                    wyvern.spawnGroundRendTrailParticles(trailDir, speed);
                }
            }
            return;
        }

        if (section.sectionType == AbilitySectionType.ACTIVE) {
            if (wyvern.isVehicle()) {
                float speed = getRiddenTravelSpeed();
                wyvern.setGroundRendTravelSpeed(speed);
                Vec3 current = wyvern.getDeltaMovement();
                if (speed <= 0.0F) {
                    wyvern.setGroundRendVelocity(Vec3.ZERO);
                    wyvern.setDeltaMovement(0.0D, current.y, 0.0D);
                    return;
                }
                Vec3 forward = getForwardDir(wyvern).scale(speed);
                wyvern.setGroundRendVelocity(forward);
                applyGroundRendHits(wyvern, getForwardDir(wyvern));
                if (shouldSpawnGroundRendTrailParticles()) {
                    wyvern.spawnGroundRendTrailParticles(getForwardDir(wyvern), speed);
                }
                return;
            }

            Vec3 horizontal;
            double speed;
            LivingEntity target = wyvern.getTarget();
            if (wyvern.isTargetValid(target)) {
                double gap = getEdgeGap(wyvern, target);
                if (gap > AI_STEER_BACK_RANGE) {
                    aiGroundRendDir = getDirectionToTarget(wyvern, target);
                }
            }
            horizontal = aiGroundRendDir;
            if (horizontal.lengthSqr() > 1.0E-6D) {
                float targetYaw = (float) (Math.atan2(horizontal.z, horizontal.x) * (180.0D / Math.PI)) - 90.0F;
                wyvern.setYRot(targetYaw);
                wyvern.yBodyRot = targetYaw;
                wyvern.yHeadRot = targetYaw;
            }
            speed = getAiTravelSpeed();

            if (horizontal.lengthSqr() < 1.0E-6) {
                return;
            }
            if (speed <= 0.0D) {
                Vec3 current = wyvern.getDeltaMovement();
                wyvern.setGroundRendVelocity(new Vec3(0.0D, current.y, 0.0D));
                return;
            }

            // Set velocity - entity will apply it in handleGroundRendMovement().
            Vec3 targetVelocity = horizontal.normalize().scale(speed);
            Vec3 current = wyvern.getDeltaMovement();
            wyvern.setGroundRendVelocity(new Vec3(targetVelocity.x, current.y, targetVelocity.z));
            wyvern.getNavigation().stop();
            applyGroundRendHits(wyvern, horizontal.normalize());
            if (shouldSpawnGroundRendTrailParticles()) {
                wyvern.spawnGroundRendTrailParticles(horizontal.normalize(), speed);
            }
        }
    }

    @Override
    public void end() {
        getUser().setGroundRending(false);
        getUser().setGroundRendTravelSpeed(0.0F);
        aiGroundRendDir = Vec3.ZERO;
        spawnedStartDust = false;
        spawnedStartBlocks = false;
        spawnedEndDust = false;
        getUser().clearRiderControlLock();
        hitCooldowns.clear();
        super.end();
    }

    @Override
    public void interrupt() {
        getUser().setGroundRending(false);
        getUser().setGroundRendTravelSpeed(0.0F);
        aiGroundRendDir = Vec3.ZERO;
        spawnedStartDust = false;
        spawnedStartBlocks = false;
        spawnedEndDust = false;
        getUser().clearRiderControlLock();
        hitCooldowns.clear();
        super.interrupt();
    }

    private static Vec3 getForwardDir(Raevyx wyvern) {
        Vec3 look = wyvern.getLookAngle();
        Vec3 horizontal = new Vec3(look.x, 0.0D, look.z);
        if (horizontal.lengthSqr() < 1.0E-6D) {
            return new Vec3(0.0D, 0.0D, 1.0D);
        }
        return horizontal.normalize();
    }

    private int getOverallTrackTick() {
        DragonAbilitySection section = getCurrentSection();
        if (section == null) {
            return 0;
        }
        return switch (section.sectionType) {
            case STARTUP -> getTicksInSection();
            case ACTIVE -> STARTUP_TICKS + getTicksInSection();
            case RECOVERY -> STARTUP_TICKS + ACTIVE_TICKS + getTicksInSection();
            default -> getTicksInSection();
        };
    }

    private boolean shouldSpawnGroundRendTrailParticles() {
        return getOverallTrackTick() < GROUND_REND_TRAIL_END_TICKS;
    }

    private float getRiddenTravelSpeed() {
        if (getOverallTrackTick() >= STOP_TICKS) {
            return 0.0F;
        }
        return Mth.lerp(getSlowdownProgress(), RIDER_SURGE_SPEED, RIDER_RECOVERY_END_SPEED);
    }

    private double getAiTravelSpeed() {
        if (getOverallTrackTick() >= STOP_TICKS) {
            return 0.0D;
        }
        return Mth.lerp(getSlowdownProgress(), AI_FORWARD_SPEED, AI_RECOVERY_END_SPEED);
    }

    private float getSlowdownProgress() {
        return Mth.clamp(
                (float) (getOverallTrackTick() - SLOWDOWN_START_TICKS) / (float) Math.max(1, STOP_TICKS - SLOWDOWN_START_TICKS),
                0.0F,
                1.0F
        );
    }

    private static Vec3 getDirectionToTarget(Raevyx wyvern, LivingEntity target) {
        Vec3 toTarget = target.position().subtract(wyvern.position());
        Vec3 horizontal = new Vec3(toTarget.x, 0.0D, toTarget.z);
        if (horizontal.lengthSqr() < 1.0E-6D) {
            return getForwardDir(wyvern);
        }
        return horizontal.normalize();
    }

    private static double getEdgeGap(Raevyx wyvern, LivingEntity target) {
        double centerDistance = wyvern.distanceTo(target);
        double combinedRadii = (wyvern.getBbWidth() + target.getBbWidth()) * 0.5D;
        return Math.max(0.0D, centerDistance - combinedRadii);
    }

    private void applyGroundRendHits(Raevyx wyvern, Vec3 forwardDir) {
        if (wyvern.level().isClientSide) {
            return;
        }

        Vec3 attackOrigin = DragonMeleeGeometry.forwardAttack(wyvern).origin();
        AABB dragonBox = wyvern.getBoundingBox().inflate(0.9D);
        AABB mouthBox = new AABB(attackOrigin, attackOrigin).inflate(1.35D);
        AABB combinedBox = dragonBox.minmax(mouthBox);

        java.util.List<LivingEntity> targets = wyvern.level().getEntitiesOfClass(
                LivingEntity.class,
                combinedBox,
                entity -> entity != wyvern
                        && entity != wyvern.getControllingPassenger()
                        && entity.isAlive()
                        && entity.attackable()
                        && !wyvern.isAlly(entity));

        for (LivingEntity target : targets) {
            int entityId = target.getId();
            if (hitCooldowns.containsKey(entityId)) {
                continue;
            }

            float armor = (float) target.getAttributeValue(Attributes.ARMOR);
            float toughness = (float) target.getAttributeValue(Attributes.ARMOR_TOUGHNESS);
            float desiredDamage = wyvern.isSupercharged() ? SUPERCHARGED_HIT_DAMAGE : HIT_DAMAGE;
            float rawDamage = rawDamageForDesiredPostArmor(desiredDamage, Math.max(0f, armor), toughness);
            target.hurt(wyvern.damageSources().mobAttack(wyvern), rawDamage);
            if (wyvern.isSupercharged()) {
                spawnSuperchargedGroundRendLightning(wyvern, target);
            }
            wyvern.noteAggroFrom(target);
            target.knockback((float) HIT_KNOCKBACK, -forwardDir.x, -forwardDir.z);
            hitCooldowns.put(entityId, HIT_COOLDOWN_TICKS);
        }
    }

    private void spawnSuperchargedGroundRendLightning(Raevyx wyvern, LivingEntity target) {
        if (!(wyvern.level() instanceof ServerLevel server)) {
            return;
        }

        var bolt = EntityType.LIGHTNING_BOLT.create(server);
        if (bolt == null) {
            return;
        }

        bolt.moveTo(target.getX(), target.getY(), target.getZ());
        bolt.setVisualOnly(true);
        var owner = wyvern.getOwner();
        if (owner instanceof ServerPlayer sp) {
            bolt.setCause(sp);
        }
        server.addFreshEntity(bolt);
    }

    private void spawnGroundRendDustBurst(Raevyx wyvern, int count, double radiusScale, double speedScale) {
        if (!(wyvern.level() instanceof ServerLevel server) || wyvern.isInWaterOrBubble() || wyvern.isInLava()) {
            return;
        }

        RandomSource random = wyvern.getRandom();
        Vec3 forward = getForwardDir(wyvern);
        double radius = Math.max(wyvern.getBbWidth() * radiusScale, 1.4D);
        double y = wyvern.getBoundingBox().minY + 0.08D;
        for (int i = 0; i < count; i++) {
            double angle = random.nextDouble() * Math.PI * 2.0D;
            double distance = radius * (0.2D + random.nextDouble() * 0.9D);
            double x = wyvern.getX() + Math.cos(angle) * distance;
            double z = wyvern.getZ() + Math.sin(angle) * distance;
            double xSpeed = forward.x * (0.12D + random.nextDouble() * speedScale)
                    + Math.cos(angle) * (0.05D + random.nextDouble() * 0.08D);
            double zSpeed = forward.z * (0.12D + random.nextDouble() * speedScale)
                    + Math.sin(angle) * (0.05D + random.nextDouble() * 0.08D);
            double ySpeed = 0.06D + random.nextDouble() * 0.12D;
            sendNearbyDragonDustParticle(server, wyvern, x, y, z, xSpeed, ySpeed, zSpeed);
        }
    }

    private void spawnGroundRendImpactRing(Raevyx wyvern) {
        if (wyvern.level() instanceof ServerLevel server) {
            server.addFreshEntity(new ImpactRingEntity(server,
                    new Vec3(wyvern.getX(), wyvern.getBoundingBox().minY, wyvern.getZ()), 0.35F));
        }
    }

    private void spawnGroundRendFallingBlocks(Raevyx wyvern) {
        if (!(wyvern.level() instanceof ServerLevel server)) {
            return;
        }

        RandomSource random = wyvern.getRandom();
        Vec3 forward = getForwardDir(wyvern);
        Vec3 right = new Vec3(-forward.z, 0.0D, forward.x);
        double radius = Math.max(wyvern.getBbWidth() * 0.55D, 1.15D);
        for (int i = 0; i < 7; i++) {
            double forwardOffset = (random.nextDouble() * 1.35D - 0.25D) * radius;
            double lateralOffset = (random.nextDouble() - 0.5D) * radius * 1.4D;
            Vec3 sample = wyvern.position().add(forward.scale(forwardOffset)).add(right.scale(lateralOffset));
            BlockPos groundPos = findGroundBlock(server, wyvern, sample);
            if (groundPos == null) {
                continue;
            }

            BlockState state = server.getBlockState(groundPos);
            if (state.isAir() || state.liquid() || state.is(Blocks.BEDROCK)) {
                continue;
            }

            VisualFallingBlockEntity block = new VisualFallingBlockEntity(
                    ModEntities.VISUAL_FALLING_BLOCK.get(),
                    server,
                    groundPos.getX() + 0.5D,
                    groundPos.getY() + 0.55D,
                    groundPos.getZ() + 0.5D,
                    state,
                    34
            );
            double outwardX = sample.x - wyvern.getX();
            double outwardZ = sample.z - wyvern.getZ();
            Vec3 outward = new Vec3(outwardX, 0.0D, outwardZ);
            if (outward.lengthSqr() < 1.0E-4D) {
                outward = forward;
            }
            outward = outward.normalize();
            double sideSpeed = 0.08D + random.nextDouble() * 0.16D;
            double upSpeed = 0.42D + random.nextDouble() * 0.28D;
            block.setDeltaMovement(outward.x * sideSpeed, upSpeed, outward.z * sideSpeed);
            server.addFreshEntity(block);
        }
    }

    private BlockPos findGroundBlock(ServerLevel server, Raevyx wyvern, Vec3 sample) {
        int x = Mth.floor(sample.x);
        int z = Mth.floor(sample.z);
        int startY = Mth.floor(wyvern.getBoundingBox().minY + 0.5D);
        int stopY = Math.max(server.getMinBuildHeight(), startY - 5);
        for (int y = startY; y >= stopY; y--) {
            BlockPos pos = new BlockPos(x, y, z);
            BlockState state = server.getBlockState(pos);
            if (!state.isAir() && !state.liquid() && state.isSolidRender(server, pos)) {
                return pos;
            }
        }
        return null;
    }

    private void sendNearbyDragonDustParticle(ServerLevel server, Raevyx wyvern, double x, double y, double z,
                                              double xSpeed, double ySpeed, double zSpeed) {
        double maxDistanceSqr = DUST_VIEW_DISTANCE * DUST_VIEW_DISTANCE;
        for (ServerPlayer player : server.players()) {
            if (player.distanceToSqr(x, y, z) <= maxDistanceSqr || player.distanceToSqr(wyvern) <= maxDistanceSqr) {
                server.sendParticles(player, ModParticles.DRAGON_DUST.get(), true,
                        x, y, z, 0, xSpeed, ySpeed, zSpeed, 1.0D);
            }
        }
    }

    private static float damageAfterArmor(float damage, float armor, float toughness) {
        float f = 2.0F + toughness / 4.0F;
        float reduction = Mth.clamp(armor - damage / f, armor * 0.2F, 20.0F);
        return damage * (1.0F - reduction / 25.0F);
    }

    private static float rawDamageForDesiredPostArmor(float desiredPostArmor, float armor, float toughness) {
        if (desiredPostArmor <= 0f) {
            return 0f;
        }

        float lo = desiredPostArmor;
        float hi = Math.max(desiredPostArmor, 1.0f);
        for (int i = 0; i < 8 && damageAfterArmor(hi, armor, toughness) < desiredPostArmor; i++) {
            hi *= 2.0f;
        }

        for (int i = 0; i < 14; i++) {
            float mid = (lo + hi) * 0.5f;
            float val = damageAfterArmor(mid, armor, toughness);
            if (val < desiredPostArmor) {
                lo = mid;
            } else {
                hi = mid;
            }
        }
        return hi;
    }
}

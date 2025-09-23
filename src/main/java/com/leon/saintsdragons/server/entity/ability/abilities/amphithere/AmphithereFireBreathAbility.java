package com.leon.saintsdragons.server.entity.ability.abilities.amphithere;

import com.leon.saintsdragons.server.entity.ability.DragonAbility;
import com.leon.saintsdragons.server.entity.ability.DragonAbilitySection;
import com.leon.saintsdragons.server.entity.ability.DragonAbilityType;
import com.leon.saintsdragons.server.entity.dragons.amphithere.AmphithereEntity;
import static com.leon.saintsdragons.server.entity.ability.DragonAbilitySection.*;

/**
 * Fire breathing ability for Amphithere.
 * Shorter range, cone-shaped area effect with burning damage over time.
 * More close-combat focused compared to Lightning Dragon's precise beam.
 */
public class AmphithereFireBreathAbility extends DragonAbility<AmphithereEntity> {

    // Fire breath timing: 0.5s startup, 1.5s active breathing
    private static final DragonAbilitySection[] TRACK = new DragonAbilitySection[] {
            new AbilitySectionDuration(AbilitySectionType.STARTUP, 10),  // 0.5 seconds
            new AbilitySectionDuration(AbilitySectionType.ACTIVE, 30)     // 1.5 seconds
    };
    
    private boolean hasFireBreathStarted = false; // Track if fire breath has been initiated this activation

    public AmphithereFireBreathAbility(DragonAbilityType<AmphithereEntity, AmphithereFireBreathAbility> type, AmphithereEntity user) {
        super(type, user, TRACK, 200); // 10 second cooldown (200 ticks)
    }

    @Override
    protected void beginSection(DragonAbilitySection section) {
        if (section == null) return;
        if (section.sectionType == AbilitySectionType.STARTUP) {
            // Reset fire breath flag; play startup animation
            hasFireBreathStarted = false;
            getUser().setFireBreathing(false);
            getUser().triggerAnim("action", "fire_breath");
        } else if (section.sectionType == AbilitySectionType.ACTIVE) {
            // Enter fire breathing window; visuals/damage enabled during ACTIVE only
            getUser().setFireBreathing(true);
            // Initial tick damage alignment
            if (!hasFireBreathStarted) {
                fireBreathOnce();
                hasFireBreathStarted = true;
            }
        }
    }

    @Override
    protected void endSection(DragonAbilitySection section) {
        // When leaving ACTIVE, clear fire breathing state
        if (section != null && section.sectionType == AbilitySectionType.ACTIVE) {
            getUser().setFireBreathing(false);
        }
    }

    @Override
    public void interrupt() {
        // Ensure fire breathing flag cleared even if interrupted mid-active
        getUser().setFireBreathing(false);
        hasFireBreathStarted = false; // Reset for next use
        super.interrupt();
    }

    @Override
    public void tickUsing() {
        var section = getCurrentSection();
        if (section == null || section.sectionType != AbilitySectionType.ACTIVE) return;

        AmphithereEntity dragon = getUser();
        if (dragon.level().isClientSide) return; // server-side authority only

        // Update fire breath visual positions every tick
        updateFireBreathPositions(dragon);
        
        // Slight body alignment toward target while breathing fire
        var tgt = dragon.getTarget();
        if (tgt != null && tgt.isAlive()) {
            double dx = tgt.getX() - dragon.getX();
            double dz = tgt.getZ() - dragon.getZ();
            float targetYaw = (float)(Math.atan2(dz, dx) * (180.0 / Math.PI)) - 90.0F;
            float currentYaw = dragon.getYRot();
            float yawErr = net.minecraft.util.Mth.degreesDifference(currentYaw, targetYaw);
            // Gentler alignment than lightning beam - Amphithere is more agile
            if (Math.abs(yawErr) > 5.0f) {
                float step = net.minecraft.util.Mth.clamp(3.0f + Math.abs(yawErr) * 0.08f, 3.0f, 8.0f);
                float newYaw = net.minecraft.util.Mth.approachDegrees(currentYaw, targetYaw, step);
                dragon.setYRot(newYaw);
                dragon.yBodyRot = dragon.getYRot();
            }
        }
        
        // Deal continuous damage every tick while fire breath is active
        var mouthPos = dragon.getMouthPosition();
        var aimDir = getFireBreathDirection(dragon);
        if (mouthPos != null && aimDir != null) {
            damageInCone(dragon, mouthPos, aimDir);
        }
    }

    private void fireBreathOnce() {
        AmphithereEntity dragon = getUser();
        updateFireBreathPositions(dragon);
        
        var mouthPos = dragon.getMouthPosition();
        var aimDir = getFireBreathDirection(dragon);
        if (mouthPos != null && aimDir != null) {
            damageInCone(dragon, mouthPos, aimDir);
        }
    }
    
    private void updateFireBreathPositions(AmphithereEntity dragon) {
        // Compute mouth origin for fire breath
        var mouthPos = dragon.getMouthPosition();
        dragon.setFireBreathStartPosition(mouthPos);

        // Calculate fire breath direction
        var aimDir = getFireBreathDirection(dragon);
        if (aimDir != null) {
            dragon.setFireBreathDirection(aimDir);
        }
    }
    
    private net.minecraft.world.phys.Vec3 getFireBreathDirection(AmphithereEntity dragon) {
        // Aim preference: rider look (if mounted) -> target center -> head-based look
        net.minecraft.world.entity.Entity cp = dragon.getControllingPassenger();
        net.minecraft.world.phys.Vec3 aimDir;

        if (cp instanceof net.minecraft.world.entity.LivingEntity rider) {
            aimDir = rider.getLookAngle().normalize();
        } else {
            net.minecraft.world.entity.LivingEntity tgt = dragon.getTarget();
            if (tgt != null && tgt.isAlive()) {
                // Aim at target's center from the mouth
                var aimPoint = tgt.getBoundingBox().getCenter();
                var mouthPos = dragon.getMouthPosition();
                aimDir = aimPoint.subtract(mouthPos).normalize();
            } else {
                // Fallback to dragon's head-based look
                float yaw = dragon.yHeadRot;
                float pitch = dragon.getXRot();
                aimDir = net.minecraft.world.phys.Vec3.directionFromRotation(pitch, yaw).normalize();
            }
        }

        return aimDir;
    }
    
    private void damageInCone(AmphithereEntity dragon, net.minecraft.world.phys.Vec3 start, net.minecraft.world.phys.Vec3 direction) {
        if (!(dragon.level() instanceof net.minecraft.server.level.ServerLevel server)) return;

        final double MAX_DISTANCE = 10.0;        // Shorter range than lightning beam
        final double CONE_ANGLE = Math.toRadians(60.0); // 60 degree cone
        final double STEP_DISTANCE = 0.5;        // Sample spacing
        final double STEP_ANGLE = Math.toRadians(10.0); // Angular sampling
        final float BASE_DAMAGE = 10.0f;         // Lower base damage than lightning beam
        
        // Apply damage multiplier if dragon has one
        final float DAMAGE = BASE_DAMAGE; // TODO: Add damage multiplier method to AmphithereEntity

        java.util.HashSet<net.minecraft.world.entity.LivingEntity> hitThisBreath = new java.util.HashSet<>();

        // Spawn fire breath particles (similar to Luxtructosaurus)
        spawnFireBreathParticles(server, start, direction);

        // Sample points in cone
        for (double distance = STEP_DISTANCE; distance <= MAX_DISTANCE; distance += STEP_DISTANCE) {
            double radius = distance * Math.tan(CONE_ANGLE / 2.0);
            
            // Sample points in circle at this distance
            for (double angle = 0; angle < Math.PI * 2; angle += STEP_ANGLE) {
                // Create perpendicular vectors for circle sampling
                net.minecraft.world.phys.Vec3 forward = direction.normalize();
                net.minecraft.world.phys.Vec3 right = forward.cross(new net.minecraft.world.phys.Vec3(0, 1, 0)).normalize();
                net.minecraft.world.phys.Vec3 up = right.cross(forward).normalize();
                
                // Calculate point in cone
                double x = Math.cos(angle) * radius;
                double z = Math.sin(angle) * radius;
                net.minecraft.world.phys.Vec3 offset = right.scale(x).add(up.scale(z));
                net.minecraft.world.phys.Vec3 point = start.add(forward.scale(distance)).add(offset);
                
                // Check for entities at this point
                var aabb = new net.minecraft.world.phys.AABB(point, point).inflate(0.5);
                var list = server.getEntitiesOfClass(net.minecraft.world.entity.LivingEntity.class, aabb,
                        e -> e != dragon && e.isAlive() && e.attackable() && !isAllied(dragon, e));
                
                for (var le : list) {
                    if (hitThisBreath.add(le)) {
                        // Deal fire damage
                        le.hurt(dragon.level().damageSources().onFire(), DAMAGE);
                        
                        // Set on fire for 4 seconds
                        le.setSecondsOnFire(4);
                        
                        // Moderate knockback
                        var away = le.position().subtract(start).normalize();
                        le.push(away.x * 0.1, 0.05, away.z * 0.1);
                    }
                }
            }
        }
    }

    private void spawnFireBreathParticles(net.minecraft.server.level.ServerLevel server, net.minecraft.world.phys.Vec3 start, net.minecraft.world.phys.Vec3 direction) {
        // Spawn flame particles in a cone pattern (similar to Luxtructosaurus spew flames)
        for (int i = -3; i <= 3; i++) {
            // Create flame particles with random spread
            double spreadX = (server.random.nextFloat() - 0.5F) * 0.2F;
            double spreadY = server.random.nextFloat() * 0.1F - 0.05F;
            double spreadZ = server.random.nextFloat() * 0.3F + 0.2F;
            
            // Calculate flame direction with cone spread
            net.minecraft.world.phys.Vec3 flameDir = direction.normalize();
            flameDir = flameDir.add(spreadX, spreadY, spreadZ).normalize();
            
            // Spawn flame particle
            server.sendParticles(
                com.leon.saintsdragons.common.registry.ModParticles.FIRE_BREATH_FLAME.get(),
                start.x + spreadX, start.y + spreadY, start.z + spreadZ,
                1,
                flameDir.x * 0.1, flameDir.y * 0.1, flameDir.z * 0.1,
                0.1
            );
            
            // Spawn smoke particles occasionally
            if (server.random.nextFloat() < 0.3F) {
                server.sendParticles(
                    com.leon.saintsdragons.common.registry.ModParticles.FIRE_BREATH_SMOKE.get(),
                    start.x + spreadX * 0.5, start.y + spreadY * 0.5, start.z + spreadZ * 0.5,
                    1,
                    flameDir.x * 0.05, flameDir.y * 0.05, flameDir.z * 0.05,
                    0.05
                );
            }
        }
    }

    private boolean isAllied(AmphithereEntity dragon, net.minecraft.world.entity.LivingEntity target) {
        // Check if target is allied with the dragon
        if (dragon.getOwner() != null && target == dragon.getOwner()) {
            return true;
        }
        if (target instanceof net.minecraft.world.entity.TamableAnimal tameable && 
            tameable.getOwner() != null && tameable.getOwner() == dragon.getOwner()) {
            return true;
        }
        return false;
    }
}

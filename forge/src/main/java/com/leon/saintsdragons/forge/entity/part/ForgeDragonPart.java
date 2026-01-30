package com.leon.saintsdragons.forge.entity.part;

import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityDimensions;
import net.minecraft.world.entity.Pose;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.entity.PartEntity;
import org.jetbrains.annotations.NotNull;

/**
 * Forge-specific implementation of dragon hitbox parts using Forge's PartEntity system.
 * Supports both standard centered boxes and elongated boxes that stretch toward a target point.
 */
public class ForgeDragonPart extends PartEntity<Entity> {
    public final String partName;
    private EntityDimensions size;
    private float damageMultiplier = 1.0f;
    private float baseWidth;
    private float baseHeight;

    public ForgeDragonPart(Entity parent, String partName, float width, float height) {
        super(parent);
        this.partName = partName;
        this.baseWidth = width;
        this.baseHeight = height;
        this.size = EntityDimensions.scalable(width, height);
        this.refreshDimensions();
    }

    public ForgeDragonPart setDamageMultiplier(float multiplier) {
        this.damageMultiplier = multiplier;
        return this;
    }

    public float getDamageMultiplier() {
        return damageMultiplier;
    }

    public void updatePosition(double x, double y, double z) {
        this.setPos(x, y, z);
        this.xOld = x;
        this.yOld = y;
        this.zOld = z;
        this.setBoundingBox(this.size.makeBoundingBox(x, y, z));
    }

    /**
     * Updates position and creates an elongated bounding box that stretches toward the next bone.
     * This eliminates dead zones between adjacent hitbox segments.
     *
     * @param x Current bone X position
     * @param y Current bone Y position
     * @param z Current bone Z position
     * @param nextX Next bone X position (to stretch toward)
     * @param nextY Next bone Y position (to stretch toward)
     * @param nextZ Next bone Z position (to stretch toward)
     * @param stretchFactor How far toward the next bone to stretch (0.0-1.0, typically 0.5-0.7)
     */
    public void updatePositionElongated(double x, double y, double z,
                                         double nextX, double nextY, double nextZ,
                                         float stretchFactor) {
        this.setPos(x, y, z);
        this.xOld = x;
        this.yOld = y;
        this.zOld = z;

        // Calculate the point we're stretching toward
        double stretchX = x + (nextX - x) * stretchFactor;
        double stretchY = y + (nextY - y) * stretchFactor;
        double stretchZ = z + (nextZ - z) * stretchFactor;

        // Create base box at current position
        AABB baseBox = this.size.makeBoundingBox(x, y, z);

        // Expand the box to include the stretch point with half the base dimensions
        double halfWidth = baseWidth / 2.0;
        double halfHeight = baseHeight / 2.0;

        double minX = Math.min(baseBox.minX, stretchX - halfWidth);
        double minY = Math.min(baseBox.minY, stretchY - halfHeight);
        double minZ = Math.min(baseBox.minZ, stretchZ - halfWidth);
        double maxX = Math.max(baseBox.maxX, stretchX + halfWidth);
        double maxY = Math.max(baseBox.maxY, stretchY + halfHeight);
        double maxZ = Math.max(baseBox.maxZ, stretchZ + halfWidth);

        this.setBoundingBox(new AABB(minX, minY, minZ, maxX, maxY, maxZ));
    }

    @Override
    public boolean isPickable() {
        return true;
    }

    @Override
    public boolean startRiding(@NotNull Entity entity, boolean force) {
        Entity parent = getParent();
        if (parent != null) {
            return entity.startRiding(parent, force);
        }
        return false;
    }

    @Override
    public void addPassenger(@NotNull Entity passenger) {
        Entity parent = getParent();
        if (parent != null) {
            passenger.startRiding(parent, true);
            return;
        }
        super.addPassenger(passenger);
    }

    @Override
    protected boolean canAddPassenger(@NotNull Entity passenger) {
        return false;
    }

    @Override
    public net.minecraft.world.InteractionResult interact(@NotNull Player player,
                                                           @NotNull net.minecraft.world.InteractionHand hand) {
        Entity parent = this.getParent();
        if (parent == null) {
            return net.minecraft.world.InteractionResult.PASS;
        }
        return parent.interact(player, hand);
    }

    @Override
    public net.minecraft.world.InteractionResult interactAt(@NotNull Player player,
                                                             @NotNull Vec3 vec,
                                                             @NotNull net.minecraft.world.InteractionHand hand) {
        Entity parent = this.getParent();
        if (parent == null) {
            return net.minecraft.world.InteractionResult.PASS;
        }
        return parent.interactAt(player, vec, hand);
    }

    @Override
    public boolean hurt(@NotNull DamageSource source, float amount) {
        // Only process damage on server side
        if (this.level().isClientSide) {
            return !this.isInvulnerableTo(source);
        }

        if (this.isInvulnerableTo(source)) {
            return false;
        }

        Entity parent = this.getParent();
        if (parent == null) {
            return false;
        }

        // Apply damage multiplier based on which part was hit
        float adjustedAmount = amount * damageMultiplier;
        return parent.hurt(source, adjustedAmount);
    }

    @Override
    public boolean is(@NotNull Entity entity) {
        return this == entity || this.getParent() == entity;
    }

    @Override
    protected void defineSynchedData() {
    }

    @Override
    protected void readAdditionalSaveData(@NotNull net.minecraft.nbt.CompoundTag tag) {
    }

    @Override
    protected void addAdditionalSaveData(@NotNull net.minecraft.nbt.CompoundTag tag) {
    }

    @Override
    public @NotNull EntityDimensions getDimensions(@NotNull Pose pose) {
        return this.size;
    }

    @Override
    public boolean shouldBeSaved() {
        return false;
    }
}

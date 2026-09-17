package com.leon.saintsdragons.forge.entity.part;

import com.leon.saintsdragons.server.entity.base.DragonPartEntity;
import com.leon.saintsdragons.server.entity.dragons.ignivorus.Ignivorus;
import com.leon.saintsdragons.server.entity.part.IgnivorusHitboxes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityDimensions;
import net.minecraft.world.entity.Pose;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.entity.PartEntity;
import org.jetbrains.annotations.NotNull;

public class ForgeDragonPart extends PartEntity<Entity> implements DragonPartEntity {
    public final String partName;
    private final int partIndex;
    private EntityDimensions size = EntityDimensions.scalable(1.0F, 1.0F);
    private float damageMultiplier = 1.0f;

    public ForgeDragonPart(Entity parent, int index) {
        super(parent);

        this.partIndex = index;
        var region = IgnivorusHitboxes.REGIONS.get(index);
        this.partName = region.name();
        this.damageMultiplier = region.damage();
        this.size = EntityDimensions.scalable(1.0F, 1.0F);
        this.noPhysics = true;
        this.setNoGravity(true);

    }

    @Override
    public float getDamageMultiplier() {
        return damageMultiplier;
    }

    @Override
    public Entity getDragonParent() {
        return getParent();
    }

    @Override public int getPartIndex() { return partIndex; }

    @Override
    public void updateBounds(AABB bounds) {
        xOld = getX(); yOld = getY(); zOld = getZ();
        xo = getX(); yo = getY(); zo = getZ();
        Vec3 center = bounds.getCenter();
        setPos(center.x, bounds.minY, center.z);
        setBoundingBox(bounds);
        size = EntityDimensions.scalable((float) Math.max(bounds.getXsize(), bounds.getZsize()), (float) bounds.getYsize());
    }

    @Override
    public boolean isPickable() {
        return !isRemoved() && getDragonParent() instanceof Ignivorus dragon
                && dragon.isAlive() && !dragon.isBaby();
    }

    @Override public boolean isAlive() {
        return !isRemoved() && getDragonParent() != null && getDragonParent().isAlive();
    }

    @Override
    public Entity getRootVehicle() {
        return getDragonParent() == null ? this : getDragonParent().getRootVehicle();
    }

    @Override
    public boolean startRiding(@NotNull Entity entity, boolean force) {
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
    public InteractionResult interact(@NotNull Player player,
                                                           @NotNull InteractionHand hand) {
        return InteractionResult.PASS;
    }

    @Override
    public InteractionResult interactAt(@NotNull Player player,
                                        @NotNull Vec3 vec,
                                        @NotNull InteractionHand hand) {
        return InteractionResult.PASS;
    }

    @Override
    public boolean hurt(@NotNull DamageSource source, float amount) {
        return isPickable() && hurtParent(source, amount);
    }

    @Override
    public boolean is(@NotNull Entity entity) {
        return this == entity || this.getParent() == entity;
    }

    @Override
    protected void defineSynchedData() {
    }

    @Override
    protected void readAdditionalSaveData(@NotNull CompoundTag tag) {
    }

    @Override
    protected void addAdditionalSaveData(@NotNull CompoundTag tag) {
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

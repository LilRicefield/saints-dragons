package com.leon.saintsdragons.server.entity.component;

import com.leon.saintsdragons.server.entity.base.DragonEntity;
import com.leon.saintsdragons.server.entity.dragons.cindervane.Cindervane;
import com.leon.saintsdragons.server.entity.dragons.ignivorus.Ignivorus;
import com.leon.saintsdragons.server.entity.dragons.nulljaw.Nulljaw;
import com.leon.saintsdragons.server.entity.dragons.raevyx.Raevyx;
import com.leon.saintsdragons.server.entity.dragons.stegonaut.Stegonaut;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

public final class DragonGroomingComponent {
    private static final int BRUSHES_PER_HAPPINESS = 2;
    private static final int BRUSH_COOLDOWN_TICKS = 10;

    private final DragonEntity dragon;
    private int brushProgress = 0;
    private long lastBrushTick = -BRUSH_COOLDOWN_TICKS;

    public DragonGroomingComponent(DragonEntity dragon) {
        this.dragon = dragon;
    }

    public boolean tryBrush(Player player, ItemStack brushStack) {
        if (dragon.level().isClientSide || !dragon.isAlive() || !dragon.isTame()) {
            return false;
        }

        long now = dragon.level().getGameTime();
        if (now - lastBrushTick < BRUSH_COOLDOWN_TICKS) {
            return false;
        }
        lastBrushTick = now;

        brushProgress++;
        if (brushProgress >= BRUSHES_PER_HAPPINESS) {
            brushProgress = 0;
            dragon.setHappiness(dragon.getHappiness() + 1);
        }

        GroomingProfile profile = getProfile(dragon);
        if (dragon.getRandom().nextFloat() <= profile.dropChance()) {
            int amount = Mth.nextInt(dragon.getRandom(), profile.minDrops(), profile.maxDrops());
            dragon.spawnAtLocation(new ItemStack(profile.dropItem(), amount));
        }

        brushStack.hurtAndBreak(1, player, ignored -> {});
        return true;
    }

    public void saveToNBT(CompoundTag tag) {
        tag.putInt("BrushProgress", brushProgress);
    }

    public void loadFromNBT(CompoundTag tag) {
        brushProgress = Mth.clamp(tag.getInt("BrushProgress"), 0, BRUSHES_PER_HAPPINESS - 1);
    }

    private static GroomingProfile getProfile(DragonEntity dragon) {
        if (dragon instanceof Ignivorus) {
            return new GroomingProfile(0.35F, Items.BLAZE_POWDER, 1, 2);
        }
        if (dragon instanceof Raevyx) {
            return new GroomingProfile(0.35F, Items.GLOWSTONE_DUST, 1, 2);
        }
        if (dragon instanceof Nulljaw) {
            return new GroomingProfile(0.30F, Items.PRISMARINE_SHARD, 1, 2);
        }
        if (dragon instanceof Cindervane) {
            return new GroomingProfile(0.30F, Items.FIRE_CHARGE, 1, 1);
        }
        if (dragon instanceof Stegonaut) {
            return new GroomingProfile(0.30F, Items.SCUTE, 1, 2);
        }
        return new GroomingProfile(0.25F, Items.SCUTE, 1, 1);
    }

    private record GroomingProfile(float dropChance, Item dropItem, int minDrops, int maxDrops) {
    }
}

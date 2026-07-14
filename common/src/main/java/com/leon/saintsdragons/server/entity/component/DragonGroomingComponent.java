package com.leon.saintsdragons.server.entity.component;

import com.leon.saintsdragons.common.registry.ModItems;
import com.leon.saintsdragons.server.entity.base.DragonEntity;
import com.leon.saintsdragons.server.entity.dragons.cindervane.Cindervane;
import com.leon.saintsdragons.server.entity.dragons.ignivorus.Ignivorus;
import com.leon.saintsdragons.server.entity.dragons.varasuchus.Varasuchus;
import com.leon.saintsdragons.server.entity.dragons.raevyx.Raevyx;
import com.leon.saintsdragons.server.entity.dragons.stegonaut.Stegonaut;
import com.leon.saintsdragons.server.entity.dragons.volitans.Volitans;
import com.leon.saintsdragons.server.loot.DragonLootTables;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;

public final class DragonGroomingComponent {
    public static final int SCALE_REGROWTH_TICKS = 20 * 60;
    private static final int NORMAL_BRUSH_HAPPINESS = 10;
    private static final int GOLDEN_BRUSH_HAPPINESS = 20;
    private static final int NORMAL_BRUSH_MIN_SCALES = 1;
    private static final int NORMAL_BRUSH_MAX_SCALES = 5;
    private static final int GOLDEN_BRUSH_MIN_SCALES = 6;
    private static final int GOLDEN_BRUSH_MAX_SCALES = 10;
    private static final int BRUSH_COOLDOWN_TICKS = 10;

    private final DragonEntity dragon;
    private int scaleRegrowthTicks;
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

        boolean goldenBrush = brushStack.is(ModItems.GOLDEN_DRAGON_BRUSH.get());
        boolean increasedHappiness = false;
        if (dragon.getHappiness() < dragon.getMaxHappiness()) {
            increaseHappiness(goldenBrush);
            increasedHappiness = true;
        }

        boolean shedScales = false;
        if (isBrushingAvailable()) {
            ResourceLocation groomingLoot = getGroomingLoot(dragon);
            if (groomingLoot != null) {
                int minimum = goldenBrush ? GOLDEN_BRUSH_MIN_SCALES : NORMAL_BRUSH_MIN_SCALES;
                int maximum = goldenBrush ? GOLDEN_BRUSH_MAX_SCALES : NORMAL_BRUSH_MAX_SCALES;
                int scaleCount = Mth.nextInt(dragon.getRandom(), minimum, maximum);
                DragonLootTables.dropGroomingLoot(dragon, player, groomingLoot, scaleCount);
                scaleRegrowthTicks = SCALE_REGROWTH_TICKS;
                dragon.level().playSound(null, dragon.blockPosition(), SoundEvents.SHEEP_SHEAR, SoundSource.PLAYERS,
                        0.7F, 0.9F + dragon.getRandom().nextFloat() * 0.2F);
                shedScales = true;
            }
        }

        if (!increasedHappiness && !shedScales) {
            return false;
        }

        lastBrushTick = now;
        damageBrush(player, brushStack);
        return true;
    }

    public void tick() {
        if (scaleRegrowthTicks > 0) {
            scaleRegrowthTicks--;
        }
    }

    public boolean isBrushingAvailable() {
        return scaleRegrowthTicks <= 0;
    }

    public int getScaleRegrowthTicks() {
        return Math.max(0, scaleRegrowthTicks);
    }

    public int getBrushingProgressPercent() {
        int elapsedTicks = SCALE_REGROWTH_TICKS - getScaleRegrowthTicks();
        return Mth.clamp(elapsedTicks * 100 / SCALE_REGROWTH_TICKS, 0, 100);
    }

    private void increaseHappiness(boolean goldenBrush) {
        int previousHappiness = dragon.getHappiness();
        dragon.setHappiness(previousHappiness
                + (goldenBrush ? GOLDEN_BRUSH_HAPPINESS : NORMAL_BRUSH_HAPPINESS));
        if (dragon.getHappiness() > previousHappiness) {
            dragon.level().playSound(null, dragon.blockPosition(), SoundEvents.EXPERIENCE_ORB_PICKUP, SoundSource.PLAYERS,
                    0.45F, 1.25F + dragon.getRandom().nextFloat() * 0.15F);
        }
    }

    private static void damageBrush(Player player, ItemStack brushStack) {
        brushStack.hurtAndBreak(1, player, ignored -> {});
    }

    public void saveToNBT(CompoundTag tag) {
        tag.putInt("ScaleRegrowthTicks", getScaleRegrowthTicks());
    }

    public void loadFromNBT(CompoundTag tag) {
        scaleRegrowthTicks = Math.max(0, tag.getInt("ScaleRegrowthTicks"));
    }

    private static ResourceLocation getGroomingLoot(DragonEntity dragon) {
        if (dragon instanceof Ignivorus) {
            return DragonLootTables.IGNIVORUS_GROOMING;
        }
        if (dragon instanceof Raevyx) {
            return DragonLootTables.RAEVYX_GROOMING;
        }
        if (dragon instanceof Varasuchus) {
            return DragonLootTables.VARASUCHUS_GROOMING;
        }
        if (dragon instanceof Cindervane) {
            return DragonLootTables.CINDERVANE_GROOMING;
        }
        if (dragon instanceof Stegonaut) {
            return DragonLootTables.STEGONAUT_GROOMING;
        }
        if (dragon instanceof Volitans) {
            return DragonLootTables.VOLITANS_GROOMING;
        }
        return null;
    }
}

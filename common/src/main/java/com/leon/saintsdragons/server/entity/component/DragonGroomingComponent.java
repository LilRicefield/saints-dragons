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
    private static final int BRUSHES_PER_HAPPINESS = 2;
    private static final int NORMAL_BRUSH_HAPPINESS = 1;
    private static final int GOLDEN_BRUSH_HAPPINESS = 5;
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

        boolean canDropRewards = dragon.getHappiness() < dragon.getMaxHappiness();

        long now = dragon.level().getGameTime();
        if (now - lastBrushTick < BRUSH_COOLDOWN_TICKS) {
            return false;
        }
        lastBrushTick = now;

        brushProgress++;
        if (brushProgress >= BRUSHES_PER_HAPPINESS) {
            brushProgress = 0;
            int happinessGain = brushStack.is(ModItems.GOLDEN_DRAGON_BRUSH.get())
                    ? GOLDEN_BRUSH_HAPPINESS
                    : NORMAL_BRUSH_HAPPINESS;
            int previousHappiness = dragon.getHappiness();
            dragon.setHappiness(dragon.getHappiness() + happinessGain);
            if (dragon.getHappiness() > previousHappiness) {
                dragon.level().playSound(null, dragon.blockPosition(), SoundEvents.EXPERIENCE_ORB_PICKUP, SoundSource.PLAYERS,
                        0.45F, 1.25F + dragon.getRandom().nextFloat() * 0.15F);
            }
        }

        ResourceLocation groomingLoot = getGroomingLoot(dragon);
        if (canDropRewards && groomingLoot != null) {
            DragonLootTables.dropGroomingLoot(dragon, player, groomingLoot);
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

package com.leon.saintsdragons.server.entity.component;

import com.leon.saintsdragons.common.config.dragon.DragonAttributeConfigLoader;
import com.leon.saintsdragons.common.registry.ModItems;
import com.leon.saintsdragons.server.entity.base.DragonEntity;
import com.leon.saintsdragons.server.entity.dragons.cindervane.Cindervane;
import com.leon.saintsdragons.server.entity.dragons.ignivorus.Ignivorus;
import com.leon.saintsdragons.server.entity.dragons.varasuchus.Varasuchus;
import com.leon.saintsdragons.server.entity.dragons.raevyx.Raevyx;
import com.leon.saintsdragons.server.entity.dragons.stegonaut.Stegonaut;
import com.leon.saintsdragons.server.entity.dragons.volitans.Volitans;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

public final class DragonGroomingComponent {
    private static final int BRUSHES_PER_HAPPINESS = 2;
    private static final int NORMAL_BRUSH_HAPPINESS = 1;
    private static final int GOLDEN_BRUSH_HAPPINESS = 3;
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
            dragon.setHappiness(dragon.getHappiness() + happinessGain);
        }

        GroomingProfile profile = getProfile(dragon);
        float dropChance = getConfiguredDropChance(profile);
        if (canDropRewards && dragon.getRandom().nextFloat() <= dropChance) {
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
            return new GroomingProfile(DragonAttributeConfigLoader.IGNIVORUS_ID, 0.35F, ModItems.IGNIVORUS_SCALE.get(), 1, 2);
        }
        if (dragon instanceof Raevyx) {
            return new GroomingProfile(DragonAttributeConfigLoader.RAEVYX_ID, 0.35F, ModItems.RAEVYX_SCALE.get(), 1, 2);
        }
        if (dragon instanceof Varasuchus) {
            return new GroomingProfile(DragonAttributeConfigLoader.VARASUCHUS_ID, 0.30F, ModItems.VARASUCHUS_SCALE.get(), 1, 2);
        }
        if (dragon instanceof Cindervane) {
            return new GroomingProfile(DragonAttributeConfigLoader.CINDERVANE_ID, 0.30F, ModItems.CINDERVANE_SCALE.get(), 1, 1);
        }
        if (dragon instanceof Stegonaut) {
            return new GroomingProfile(DragonAttributeConfigLoader.STEGONAUT_ID, 0.30F, ModItems.STEGONAUT_SCALE.get(), 1, 2);
        }
        if (dragon instanceof Volitans) {
            return new GroomingProfile(DragonAttributeConfigLoader.VOLITANS_ID, 0.30F, ModItems.VOLITANS_SCALE.get(), 1, 2);
        }
        return new GroomingProfile(null, 0.25F, Items.SCUTE, 1, 1);
    }

    private static float getConfiguredDropChance(GroomingProfile profile) {
        ResourceLocation configId = profile.configId();
        if (configId == null) {
            return profile.defaultDropChance();
        }
        return (float) DragonAttributeConfigLoader.getInstance()
                .getConfig(configId)
                .extraDouble("scale_drop_chance_brush", profile.defaultDropChance());
    }

    private record GroomingProfile(ResourceLocation configId, float defaultDropChance, Item dropItem, int minDrops, int maxDrops) {
    }
}

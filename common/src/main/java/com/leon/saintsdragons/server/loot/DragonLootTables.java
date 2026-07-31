package com.leon.saintsdragons.server.loot;

import com.leon.saintsdragons.common.SaintsDragonsCommon;
import com.leon.saintsdragons.server.entity.base.DragonEntity;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.storage.loot.LootParams;
import net.minecraft.world.level.storage.loot.LootTable;
import net.minecraft.world.level.storage.loot.parameters.LootContextParamSets;
import net.minecraft.world.level.storage.loot.parameters.LootContextParams;

import java.util.List;

public final class DragonLootTables {
    public static final ResourceLocation ATROXIIA_FEMALE_DEATH = table("gameplay/dragon_drops/atroxiia_female_death");
    public static final ResourceLocation CINDERVANE_FEMALE_DEATH = table("gameplay/dragon_drops/cindervane_female_death");
    public static final ResourceLocation IGNIVORUS_FEMALE_DEATH = table("gameplay/dragon_drops/ignivorus_female_death");
    public static final ResourceLocation RAEVYX_FEMALE_DEATH = table("gameplay/dragon_drops/raevyx_female_death");
    public static final ResourceLocation STEGONAUT_FEMALE_DEATH = table("gameplay/dragon_drops/stegonaut_female_death");
    public static final ResourceLocation VARASUCHUS_FEMALE_DEATH = table("gameplay/dragon_drops/varasuchus_female_death");
    public static final ResourceLocation VOLITANS_FEMALE_DEATH = table("gameplay/dragon_drops/volitans_female_death");

    public static final ResourceLocation IGNIVORUS_HIT = table("gameplay/dragon_drops/ignivorus_hit");
    public static final ResourceLocation VOLITANS_HIT = table("gameplay/dragon_drops/volitans_hit");

    public static final ResourceLocation CINDERVANE_GROOMING = table("gameplay/grooming/cindervane");
    public static final ResourceLocation ATROXIIA_GROOMING = table("gameplay/grooming/atroxiia");
    public static final ResourceLocation IGNIVORUS_GROOMING = table("gameplay/grooming/ignivorus");
    public static final ResourceLocation RAEVYX_GROOMING = table("gameplay/grooming/raevyx");
    public static final ResourceLocation STEGONAUT_GROOMING = table("gameplay/grooming/stegonaut");
    public static final ResourceLocation VARASUCHUS_GROOMING = table("gameplay/grooming/varasuchus");
    public static final ResourceLocation VOLITANS_GROOMING = table("gameplay/grooming/volitans");

    private DragonLootTables() {
    }

    public static boolean dropEntityLoot(DragonEntity dragon, ResourceLocation tableId, DamageSource source) {
        if (!(dragon.level() instanceof ServerLevel serverLevel)) {
            return false;
        }

        LootTable table = serverLevel.getServer().getLootData().getLootTable(tableId);
        LootParams.Builder builder = new LootParams.Builder(serverLevel)
                .withParameter(LootContextParams.THIS_ENTITY, dragon)
                .withParameter(LootContextParams.ORIGIN, dragon.position())
                .withParameter(LootContextParams.DAMAGE_SOURCE, source)
                .withOptionalParameter(LootContextParams.KILLER_ENTITY, source.getEntity())
                .withOptionalParameter(LootContextParams.DIRECT_KILLER_ENTITY, source.getDirectEntity());

        Player player = source.getEntity() instanceof Player sourcePlayer
                ? sourcePlayer
                : dragon.getLastHurtByMob() instanceof Player hurtByPlayer ? hurtByPlayer : null;
        if (player != null) {
            builder.withParameter(LootContextParams.LAST_DAMAGE_PLAYER, player)
                    .withLuck(player.getLuck());
        }

        List<ItemStack> drops = table.getRandomItems(builder.create(LootContextParamSets.ENTITY), dragon.getLootTableSeed());
        drops.forEach(dragon::spawnAtLocation);
        return !drops.isEmpty();
    }

    public static void dropGroomingLoot(DragonEntity dragon, Player player, ResourceLocation tableId, int scaleCount) {
        if (!(dragon.level() instanceof ServerLevel serverLevel)) {
            return;
        }

        LootTable table = serverLevel.getServer().getLootData().getLootTable(tableId);
        LootParams params = new LootParams.Builder(serverLevel)
                .withParameter(LootContextParams.ORIGIN, dragon.position())
                .withOptionalParameter(LootContextParams.THIS_ENTITY, dragon)
                .withLuck(player.getLuck())
                .create(LootContextParamSets.CHEST);
        List<ItemStack> drops = table.getRandomItems(params);
        for (ItemStack drop : drops) {
            if (!drop.isEmpty()) {
                drop.setCount(Math.max(1, scaleCount));
                dragon.spawnAtLocation(drop);
            }
        }
    }

    private static ResourceLocation table(String path) {
        return SaintsDragonsCommon.rl(path);
    }
}

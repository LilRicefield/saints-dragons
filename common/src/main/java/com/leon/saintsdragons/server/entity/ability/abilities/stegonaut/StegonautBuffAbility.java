package com.leon.saintsdragons.server.entity.ability.abilities.stegonaut;

import com.leon.saintsdragons.common.config.SaintsDragonsConfig;
import com.leon.saintsdragons.common.item.StegonautBinderItem;
import com.leon.saintsdragons.server.entity.base.DragonEntity;
import com.leon.saintsdragons.server.entity.dragons.stegonaut.Stegonaut;
import com.leon.saintsdragons.server.entity.npc.IvyTheDragonMerchant;
import net.minecraft.core.NonNullList;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.OwnableEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.Container;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

public class StegonautBuffAbility {
    private static final double BUFF_RANGE = 8.0D;
    private static final int UPDATE_INTERVAL = 20;
    private static final int BUFF_DURATION_TICKS = 40;
    private static final int RESISTANCE_AMPLIFIER = 0;
    private static final int ABSORPTION_AMPLIFIER = 0;

    private static final Map<ResourceKey<Level>, Set<UUID>> PORTABLE_BUFF_TARGETS = new HashMap<>();

    private final Stegonaut stegonaut;
    private final Level level;

    private int tickCounter = 0;
    private Set<UUID> auraTargets = new HashSet<>();

    public StegonautBuffAbility(Stegonaut stegonaut) {
        this.stegonaut = stegonaut;
        this.level = stegonaut.level();
    }

    public void tick() {
        if (!(level instanceof ServerLevel serverLevel)) {
            return;
        }

        if (!SaintsDragonsConfig.STEGONAUT_BUFFS_ENABLED.get() || !stegonaut.isTame()) {
            clearAuraTargets(serverLevel);
            tickCounter = 0;
            return;
        }

        if (++tickCounter < UPDATE_INTERVAL) {
            return;
        }
        tickCounter = 0;

        if (!stegonaut.isAlive()) {
            clearAuraTargets(serverLevel);
            return;
        }

        applyStegonautAura(serverLevel);
    }

    public void cleanup() {
        if (level instanceof ServerLevel serverLevel) {
            clearAuraTargets(serverLevel);
        } else {
            auraTargets.clear();
        }
    }

    public static void updateAllPortableBuffs(ServerLevel level) {
        if (!SaintsDragonsConfig.STEGONAUT_BUFFS_ENABLED.get()) {
            clearPortableTargets(level);
            return;
        }

        Set<UUID> currentTargets = new HashSet<>();

        for (Player player : level.players()) {
            if (player.isAlive() && hasBoundStegonautBinder(player)) {
                applyPortableAura(level, player, currentTargets);
            }
            for (IvyTheDragonMerchant ivy : level.getEntitiesOfClass(
                    IvyTheDragonMerchant.class,
                    player.getBoundingBox().inflate(BUFF_RANGE * 2.0D),
                    ivy -> ivy.isAlive()
                            && ivy.isTame()
                            && ivy.isOwnedBy(player)
                            && hasBoundStegonautBinder(ivy.getIvyInventory())
            )) {
                applyPortableAura(level, ivy, currentTargets);
            }
        }

        clearMissingTargets(level, PORTABLE_BUFF_TARGETS.getOrDefault(level.dimension(), Collections.emptySet()), currentTargets);
        PORTABLE_BUFF_TARGETS.put(level.dimension(), currentTargets);
    }

    private void applyStegonautAura(ServerLevel serverLevel) {
        List<LivingEntity> nearbyEntities = serverLevel.getEntitiesOfClass(
                LivingEntity.class,
                stegonaut.getBoundingBox().inflate(BUFF_RANGE),
                this::isEligibleForStegonautAura
        );

        Set<UUID> currentTargets = new HashSet<>();
        for (LivingEntity entity : nearbyEntities) {
            applyBuffs(entity, true);
            currentTargets.add(entity.getUUID());
        }

        clearMissingTargets(serverLevel, auraTargets, currentTargets);
        auraTargets = currentTargets;
    }

    private static void applyPortableAura(ServerLevel level, Player player, Set<UUID> currentTargets) {
        applyBuffs(player, false);
        currentTargets.add(player.getUUID());

        List<LivingEntity> nearbyEntities = level.getEntitiesOfClass(
                LivingEntity.class,
                player.getBoundingBox().inflate(BUFF_RANGE),
                entity -> entity != player && isEligibleForPortableAura(entity, player)
        );

        for (LivingEntity entity : nearbyEntities) {
            applyBuffs(entity, false);
            currentTargets.add(entity.getUUID());
        }
    }

    private static void applyPortableAura(ServerLevel level, IvyTheDragonMerchant ivy, Set<UUID> currentTargets) {
        applyBuffs(ivy, false);
        currentTargets.add(ivy.getUUID());

        LivingEntity owner = ivy.getOwner();
        if (!(owner instanceof Player ownerPlayer)) {
            return;
        }

        List<LivingEntity> nearbyEntities = level.getEntitiesOfClass(
                LivingEntity.class,
                ivy.getBoundingBox().inflate(BUFF_RANGE),
                entity -> entity != ivy && isEligibleForPortableAura(entity, ownerPlayer)
        );

        for (LivingEntity entity : nearbyEntities) {
            applyBuffs(entity, false);
            currentTargets.add(entity.getUUID());
        }
    }

    private boolean isEligibleForStegonautAura(LivingEntity entity) {
        if (entity == stegonaut || !entity.isAlive()) {
            return false;
        }

        if (entity instanceof Player player) {
            return isPlayerEligibleForStegonautAura(player);
        }

        if (entity instanceof DragonEntity dragon) {
            return isDragonEligibleForStegonautAura(dragon);
        }

        if (entity instanceof OwnableEntity ownable) {
            LivingEntity owner = ownable.getOwner();
            if (owner instanceof Player ownerPlayer) {
                LivingEntity stegonautOwner = stegonaut.getOwner();
                if (stegonautOwner instanceof Player stegonautOwnerPlayer) {
                    return stegonautOwnerPlayer.getUUID().equals(ownerPlayer.getUUID())
                            || stegonaut.allyManager.isAlly(ownerPlayer);
                }
            }
        }

        return false;
    }

    private boolean isPlayerEligibleForStegonautAura(Player player) {
        LivingEntity owner = stegonaut.getOwner();
        if (!(owner instanceof Player ownerPlayer)) {
            return false;
        }

        return ownerPlayer.getUUID().equals(player.getUUID()) || stegonaut.allyManager.isAlly(player);
    }

    private boolean isDragonEligibleForStegonautAura(DragonEntity dragon) {
        LivingEntity owner = stegonaut.getOwner();
        if (!(owner instanceof Player ownerPlayer)) {
            return false;
        }

        return dragon.isOwnedBy(ownerPlayer)
                || dragon instanceof Stegonaut alliedStegonaut && alliedStegonaut.allyManager.isAlly(ownerPlayer);
    }

    private static boolean isEligibleForPortableAura(LivingEntity entity, Player player) {
        if (!entity.isAlive()) {
            return false;
        }

        if (entity instanceof Player) {
            return true;
        }

        if (entity instanceof Stegonaut stegonaut) {
            return stegonaut.isTame() && (stegonaut.isOwnedBy(player) || stegonaut.allyManager.isAlly(player));
        }

        if (entity instanceof DragonEntity dragon) {
            return dragon.isTame() && dragon.isOwnedBy(player);
        }

        if (entity instanceof OwnableEntity ownable) {
            LivingEntity owner = ownable.getOwner();
            return owner instanceof Player ownerPlayer && ownerPlayer.getUUID().equals(player.getUUID());
        }

        return false;
    }

    private static boolean hasBoundStegonautBinder(Player player) {
        return hasBoundStegonautBinder(player.getInventory().items)
                || hasBoundStegonautBinder(player.getInventory().offhand)
                || hasBoundStegonautBinder(player.getInventory().armor);
    }

    private static boolean hasBoundStegonautBinder(NonNullList<ItemStack> stacks) {
        for (ItemStack stack : stacks) {
            if (!stack.isEmpty()
                    && stack.getItem() instanceof StegonautBinderItem
                    && StegonautBinderItem.isBound(stack)
                    && StegonautBinderItem.getBoundStegonautUUID(stack) != null) {
                return true;
            }
        }
        return false;
    }

    public static boolean hasBoundStegonautBinder(Container container) {
        for (int slot = 0; slot < container.getContainerSize(); slot++) {
            ItemStack stack = container.getItem(slot);
            if (!stack.isEmpty()
                    && stack.getItem() instanceof StegonautBinderItem
                    && StegonautBinderItem.isBound(stack)
                    && StegonautBinderItem.getBoundStegonautUUID(stack) != null) {
                return true;
            }
        }
        return false;
    }

    private static void applyBuffs(LivingEntity entity, boolean showIcon) {
        entity.addEffect(new MobEffectInstance(
                MobEffects.DAMAGE_RESISTANCE,
                BUFF_DURATION_TICKS,
                RESISTANCE_AMPLIFIER,
                false,
                false,
                showIcon
        ));
        entity.addEffect(new MobEffectInstance(
                MobEffects.ABSORPTION,
                BUFF_DURATION_TICKS,
                ABSORPTION_AMPLIFIER,
                false,
                false,
                showIcon
        ));
    }

    private void clearAuraTargets(ServerLevel serverLevel) {
        clearTargets(serverLevel, auraTargets);
        auraTargets.clear();
    }

    private static void clearPortableTargets(ServerLevel level) {
        clearTargets(level, PORTABLE_BUFF_TARGETS.getOrDefault(level.dimension(), Collections.emptySet()));
        PORTABLE_BUFF_TARGETS.put(level.dimension(), new HashSet<>());
    }

    private static void clearMissingTargets(ServerLevel level, Set<UUID> previousTargets, Set<UUID> currentTargets) {
        if (previousTargets.isEmpty()) {
            return;
        }

        Set<UUID> missingTargets = new HashSet<>(previousTargets);
        missingTargets.removeAll(currentTargets);
        clearTargets(level, missingTargets);
    }

    private static void clearTargets(ServerLevel level, Set<UUID> targets) {
        for (UUID uuid : targets) {
            Entity entity = level.getEntity(uuid);
            if (entity instanceof LivingEntity livingEntity) {
                livingEntity.removeEffect(MobEffects.DAMAGE_RESISTANCE);
                livingEntity.removeEffect(MobEffects.ABSORPTION);
            }
        }
    }

    public static double getBuffRange() {
        return BUFF_RANGE;
    }

    public static int getResistanceAmplifier() {
        return RESISTANCE_AMPLIFIER;
    }

    public static int getAbsorptionAmplifier() {
        return ABSORPTION_AMPLIFIER;
    }
}

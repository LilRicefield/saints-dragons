package com.leon.saintsdragons.common.item.tools;

import com.leon.saintsdragons.common.config.ToolsArmorConfig;
import com.leon.saintsdragons.common.registry.ModItems;
import com.leon.saintsdragons.common.registry.ModSounds;
import com.leon.saintsdragons.server.entity.effect.ignivorus.IgnivorusMagmaPillarEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.phys.Vec3;

public final class DragonlordSwordAbility {
    private static final int PILLAR_COUNT = 3;
    private static final double BASE_FORWARD_OFFSET = 3.0D;
    private static final double FORWARD_STEP = 6.0D;
    private static final int PILLAR_WARMUP_TICKS = 5;
    private static final int PILLAR_LIFETIME_TICKS = 34;

    private DragonlordSwordAbility() {
    }

    public static void tryUse(ServerPlayer player) {
        if (!ToolsArmorConfig.DRAGONLORD_SWORD_ABILITY_ENABLED.get()
                || player == null || !player.isAlive() || player.isSpectator()) {
            return;
        }

        ItemStack stack = player.getMainHandItem();
        Item sword = ModItems.DRAGONLORD_SWORD.get();
        if (!stack.is(sword) || player.getCooldowns().isOnCooldown(sword)) {
            return;
        }

        Vec3 look = player.getLookAngle();
        Vec3 forward = new Vec3(look.x, 0.0D, look.z);
        if (forward.lengthSqr() < 1.0E-4D) {
            return;
        }
        forward = forward.normalize();

        ServerLevel level = player.serverLevel();
        float pillarYaw = (float) Math.toDegrees(Math.atan2(forward.z, forward.x)) - 90.0F;

        for (int index = 0; index < PILLAR_COUNT; index++) {
            double distance = BASE_FORWARD_OFFSET + index * FORWARD_STEP;
            Vec3 base = player.position().add(forward.scale(distance));
            BlockPos column = BlockPos.containing(base.x, base.y, base.z);
            BlockPos ground = level.getHeightmapPos(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, column);
            Vec3 spawnPos = new Vec3(base.x, ground.getY(), base.z);

            IgnivorusMagmaPillarEntity pillar = new IgnivorusMagmaPillarEntity(
                    level,
                    spawnPos,
                    player,
                    index,
                    pillarYaw,
                    (float) (ToolsArmorConfig.DRAGONLORD_SWORD_ABILITY_BASE_DAMAGE.get()
                            + index * ToolsArmorConfig.DRAGONLORD_SWORD_ABILITY_DAMAGE_PER_PILLAR.get()),
                    ToolsArmorConfig.DRAGONLORD_SWORD_ABILITY_BASE_KNOCKBACK.get()
                            + index * ToolsArmorConfig.DRAGONLORD_SWORD_ABILITY_KNOCKBACK_PER_PILLAR.get(),
                    PILLAR_WARMUP_TICKS,
                    PILLAR_LIFETIME_TICKS
            );
            level.addFreshEntity(pillar);
            level.playSound(
                    null,
                    spawnPos.x,
                    spawnPos.y,
                    spawnPos.z,
                    ModSounds.IGNIVORUS_MAGMA_PILLAR.get(),
                    SoundSource.PLAYERS,
                    1.2F,
                    0.9F + level.random.nextFloat() * 0.2F
            );
        }

        player.getCooldowns().addCooldown(sword, ToolsArmorConfig.DRAGONLORD_SWORD_ABILITY_COOLDOWN_TICKS.get());
    }
}

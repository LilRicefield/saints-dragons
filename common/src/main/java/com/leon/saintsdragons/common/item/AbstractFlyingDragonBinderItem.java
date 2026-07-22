package com.leon.saintsdragons.common.item;

import com.leon.saintsdragons.common.item.util.BinderComponentUtil;
import com.leon.saintsdragons.server.entity.base.RideableFlyingDragon;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.NotNull;

public abstract class AbstractFlyingDragonBinderItem<T extends RideableFlyingDragon>
        extends AbstractDragonBinderItem<T> {
    private static final double AIR_RELEASE_DISTANCE = 5.0D;

    protected AbstractFlyingDragonBinderItem(Properties properties) {
        super(properties);
    }

    @Override
    public @NotNull InteractionResultHolder<ItemStack> use(@NotNull Level level, @NotNull Player player,
                                                           @NotNull InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        if (!BinderComponentUtil.isBound(stack)) {
            return super.use(level, player, hand);
        }
        if (level.isClientSide) {
            return InteractionResultHolder.success(stack);
        }

        Vec3 releasePosition = player.position().add(
                player.getLookAngle().normalize().scale(AIR_RELEASE_DISTANCE)
        );
        boolean released = releaseDragon(stack, player, releasePosition, true);
        if (released) {
            syncPlayerInventory(player);
        }
        return released ? InteractionResultHolder.success(stack) : InteractionResultHolder.fail(stack);
    }

    @Override
    protected void prepareDragonForAirRelease(T dragon, Player player) {
        dragon.getAIMovement().stopAndClearAllMovement();
        dragon.setDeltaMovement(Vec3.ZERO);
        dragon.setOnGround(false);
        dragon.setFlying(true);
        dragon.setTakeoff(false);
        dragon.setLanding(false);
        dragon.setHovering(false);
        dragon.setGoingUp(false);
        dragon.setGoingDown(false);
        dragon.setAccelerating(false);
    }

    @Override
    protected String getBoundReleaseTooltipKey() {
        return "saintsdragons.tooltip.flying_binder.right_click_to_release";
    }
}

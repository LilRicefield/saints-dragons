package com.leon.saintsdragons.common.item;

import com.leon.saintsdragons.common.item.util.BinderComponentUtil;
import com.leon.saintsdragons.common.registry.ModEntities;
import com.leon.saintsdragons.server.entity.dragons.volitans.Volitans;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.Nullable;

import java.util.UUID;

public class VolitansBinderItem extends AbstractDragonBinderItem<Volitans> {

    private static final String DRAGON_DATA_KEY = "VolitansData";

    public VolitansBinderItem(Properties properties) {
        super(properties);
    }

    @Override
    protected Class<Volitans> getDragonClass() {
        return Volitans.class;
    }

    @Override
    protected Volitans createDragon(ServerLevel level) {
        return new Volitans(ModEntities.VOLITANS.get(), level);
    }

    @Override
    protected boolean canBind(Volitans dragon) {
        return dragon.canBeBound();
    }

    @Override
    protected void prepareDragonForCapture(Volitans dragon, Player player) {
        dragon.prepareForBinderStorage();
    }

    @Override
    protected void prepareDragonForRelease(Volitans dragon, Player player) {
        dragon.prepareAfterBinderRelease();
    }

    @Override
    protected String getDragonDataKey() {
        return DRAGON_DATA_KEY;
    }

    @Override
    protected String getTooltipDescriptionKey() {
        return "saintsdragons.tooltip.volitans.description";
    }

    public static boolean isBound(ItemStack stack) {
        return BinderComponentUtil.isBound(stack);
    }

    @Nullable
    public static UUID getBoundVolitansUUID(ItemStack stack) {
        return BinderComponentUtil.getBoundDragonUuid(stack);
    }

    @Nullable
    public static String getBoundVolitansName(ItemStack stack) {
        return BinderComponentUtil.getBoundDragonName(stack);
    }
}

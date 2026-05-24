package com.leon.saintsdragons.common.item;

import com.leon.saintsdragons.common.item.util.BinderComponentUtil;
import com.leon.saintsdragons.common.registry.ModEntities;
import com.leon.saintsdragons.server.entity.dragons.raevyx.Raevyx;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.Nullable;

import java.util.UUID;

public class RaevyxBinderItem extends AbstractDragonBinderItem<Raevyx> {

    private static final String DRAGON_DATA_KEY = "RaevyxData";

    public RaevyxBinderItem(Properties properties) {
        super(properties);
    }

    @Override
    protected Class<Raevyx> getDragonClass() {
        return Raevyx.class;
    }

    @Override
    protected Raevyx createDragon(ServerLevel level) {
        return new Raevyx(ModEntities.RAEVYX.get(), level);
    }

    @Override
    protected boolean canBind(Raevyx dragon) {
        return dragon.canBeBound();
    }

    @Override
    protected String getDragonDataKey() {
        return DRAGON_DATA_KEY;
    }

    @Override
    protected String getTooltipDescriptionKey() {
        return "saintsdragons.tooltip.raevyx_binder.description";
    }

    public static boolean isBound(ItemStack stack) {
        return BinderComponentUtil.isBound(stack);
    }

    @Nullable
    public static UUID getBoundRaevyxUUID(ItemStack stack) {
        return BinderComponentUtil.getBoundDragonUuid(stack);
    }

    @Nullable
    public static String getBoundRaevyxName(ItemStack stack) {
        return BinderComponentUtil.getBoundDragonName(stack);
    }
}

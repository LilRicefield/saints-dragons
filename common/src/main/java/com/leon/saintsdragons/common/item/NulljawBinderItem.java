package com.leon.saintsdragons.common.item;

import com.leon.saintsdragons.common.item.util.BinderComponentUtil;
import com.leon.saintsdragons.common.registry.ModEntities;
import com.leon.saintsdragons.server.entity.dragons.nulljaw.Nulljaw;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.Nullable;

import java.util.UUID;

public class NulljawBinderItem extends AbstractDragonBinderItem<Nulljaw> {
    private static final String DRAGON_DATA_KEY = "NulljawData";

    public NulljawBinderItem(Properties properties) {
        super(properties);
    }

    @Override
    protected Class<Nulljaw> getDragonClass() {
        return Nulljaw.class;
    }

    @Override
    protected Nulljaw createDragon(ServerLevel level) {
        return new Nulljaw(ModEntities.NULLJAW.get(), level);
    }

    @Override
    protected boolean canBind(Nulljaw dragon) {
        return dragon.canBeBound();
    }

    @Override
    protected String getDragonDataKey() {
        return DRAGON_DATA_KEY;
    }

    @Override
    protected String getTooltipDescriptionKey() {
        return "saintsdragons.tooltip.nulljaw_binder.description";
    }

    public static boolean isBound(ItemStack stack) {
        return BinderComponentUtil.isBound(stack);
    }

    @Nullable
    public static UUID getBoundNulljawUUID(ItemStack stack) {
        return BinderComponentUtil.getBoundDragonUuid(stack);
    }

    @Nullable
    public static String getBoundNulljawName(ItemStack stack) {
        return BinderComponentUtil.getBoundDragonName(stack);
    }
}

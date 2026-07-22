package com.leon.saintsdragons.common.item;

import com.leon.saintsdragons.common.item.util.BinderComponentUtil;
import com.leon.saintsdragons.common.registry.ModEntities;
import com.leon.saintsdragons.server.entity.dragons.cindervane.Cindervane;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.Nullable;

import java.util.UUID;

public class CindervaneBinderItem extends AbstractFlyingDragonBinderItem<Cindervane> {

    private static final String DRAGON_DATA_KEY = "CindervaneData";

    public CindervaneBinderItem(Properties properties) {
        super(properties);
    }

    @Override
    protected Class<Cindervane> getDragonClass() {
        return Cindervane.class;
    }

    @Override
    protected Cindervane createDragon(ServerLevel level) {
        return new Cindervane(ModEntities.CINDERVANE.get(), level);
    }

    @Override
    protected boolean canBind(Cindervane dragon) {
        return dragon.canBeBound();
    }

    @Override
    protected String getDragonDataKey() {
        return DRAGON_DATA_KEY;
    }

    @Override
    protected String getTooltipDescriptionKey() {
        return "saintsdragons.tooltip.cindervane_binder.description";
    }

    public static boolean isBound(ItemStack stack) {
        return BinderComponentUtil.isBound(stack);
    }

    @Nullable
    public static UUID getBoundCindervaneUUID(ItemStack stack) {
        return BinderComponentUtil.getBoundDragonUuid(stack);
    }

    @Nullable
    public static String getBoundCindervaneName(ItemStack stack) {
        return BinderComponentUtil.getBoundDragonName(stack);
    }
}

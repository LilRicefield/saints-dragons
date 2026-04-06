package com.leon.saintsdragons.common.item;

import com.leon.saintsdragons.common.item.util.BinderComponentUtil;
import com.leon.saintsdragons.common.registry.ModEntities;
import com.leon.saintsdragons.server.entity.dragons.ignivorus.Ignivorus;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.Nullable;

import java.util.UUID;

public class IgnivorusBinderItem extends AbstractDragonBinderItem<Ignivorus> {

    private static final String DRAGON_DATA_KEY = "IgnivorusData";

    public IgnivorusBinderItem(Properties properties) {
        super(properties);
    }

    @Override
    protected Class<Ignivorus> getDragonClass() {
        return Ignivorus.class;
    }

    @Override
    protected Ignivorus createDragon(ServerLevel level) {
        return new Ignivorus(ModEntities.IGNIVORUS.get(), level);
    }

    @Override
    protected boolean canBind(Ignivorus dragon) {
        return dragon.canBeBound();
    }

    @Override
    protected String getDragonDataKey() {
        return DRAGON_DATA_KEY;
    }

    @Override
    protected String getTooltipDescriptionKey() {
        return "saintsdragons.tooltip.ignivorus_binder.description";
    }

    public static boolean isBound(ItemStack stack) {
        return BinderComponentUtil.isBound(stack);
    }

    @Nullable
    public static UUID getBoundIgnivorusUUID(ItemStack stack) {
        return BinderComponentUtil.getBoundDragonUuid(stack);
    }

    @Nullable
    public static String getBoundIgnivorusName(ItemStack stack) {
        return BinderComponentUtil.getBoundDragonName(stack);
    }
}

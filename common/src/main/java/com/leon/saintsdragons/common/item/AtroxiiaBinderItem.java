package com.leon.saintsdragons.common.item;

import com.leon.saintsdragons.common.item.util.BinderComponentUtil;
import com.leon.saintsdragons.common.registry.ModEntities;
import com.leon.saintsdragons.server.entity.dragons.atroxiia.Atroxiia;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.Nullable;

import java.util.UUID;

public class AtroxiiaBinderItem extends AbstractDragonBinderItem<Atroxiia> {
    private static final String DRAGON_DATA_KEY = "AtroxiiaData";

    public AtroxiiaBinderItem(Properties properties) {
        super(properties);
    }

    @Override
    protected Class<Atroxiia> getDragonClass() {
        return Atroxiia.class;
    }

    @Override
    protected Atroxiia createDragon(ServerLevel level) {
        return new Atroxiia(ModEntities.ATROXIIA.get(), level);
    }

    @Override
    protected boolean canBind(Atroxiia dragon) {
        return dragon.canBeBound();
    }

    @Override
    protected String getDragonDataKey() {
        return DRAGON_DATA_KEY;
    }

    @Override
    protected String getReleaseNotOwnerMessageKey() {
        return "saintsdragons.message.not_dragon_owner";
    }

    @Override
    protected String getTooltipDescriptionKey() {
        return "saintsdragons.tooltip.atroxiia_binder.description";
    }

    public static boolean isBound(ItemStack stack) {
        return BinderComponentUtil.isBound(stack);
    }

    @Nullable
    public static UUID getBoundAtroxiiaUUID(ItemStack stack) {
        return BinderComponentUtil.getBoundDragonUuid(stack);
    }

    @Nullable
    public static String getBoundAtroxiiaName(ItemStack stack) {
        return BinderComponentUtil.getBoundDragonName(stack);
    }
}

package com.leon.saintsdragons.common.item;

import com.leon.saintsdragons.common.item.util.BinderComponentUtil;
import com.leon.saintsdragons.common.registry.ModEntities;
import com.leon.saintsdragons.server.entity.dragons.varasuchus.Varasuchus;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.Nullable;

import java.util.UUID;

public class VarasuchusBinderItem extends AbstractDragonBinderItem<Varasuchus> {

    private static final String DRAGON_DATA_KEY = "VarasuchusData";

    public VarasuchusBinderItem(Properties properties) {
        super(properties);
    }

    @Override
    protected Class<Varasuchus> getDragonClass() {
        return Varasuchus.class;
    }

    @Override
    protected Varasuchus createDragon(ServerLevel level) {
        return new Varasuchus(ModEntities.VARASUCHUS.get(), level);
    }

    @Override
    protected boolean canBind(Varasuchus dragon) {
        return dragon.canBeBound();
    }

    @Override
    protected String getDragonDataKey() {
        return DRAGON_DATA_KEY;
    }

    @Override
    protected String getTooltipDescriptionKey() {
        return "saintsdragons.tooltip.varasuchus.description";
    }

    public static boolean isBound(ItemStack stack) {
        return BinderComponentUtil.isBound(stack);
    }

    @Nullable
    public static UUID getBoundVarasuchusUUID(ItemStack stack) {
        return BinderComponentUtil.getBoundDragonUuid(stack);
    }

    @Nullable
    public static String getBoundVarasuchusName(ItemStack stack) {
        return BinderComponentUtil.getBoundDragonName(stack);
    }
}

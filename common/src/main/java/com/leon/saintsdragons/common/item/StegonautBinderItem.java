package com.leon.saintsdragons.common.item;

import com.leon.saintsdragons.common.item.util.BinderComponentUtil;
import com.leon.saintsdragons.common.registry.ModEntities;
import com.leon.saintsdragons.server.entity.dragons.stegonaut.Stegonaut;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.UUID;

public class StegonautBinderItem extends AbstractDragonBinderItem<Stegonaut> {

    private static final String DRAGON_DATA_KEY = "StegonautData";

    public StegonautBinderItem(Properties properties) {
        super(properties);
    }

    @Override
    protected Class<Stegonaut> getDragonClass() {
        return Stegonaut.class;
    }

    @Override
    protected Stegonaut createDragon(ServerLevel level) {
        return new Stegonaut(ModEntities.STEGONAUT.get(), level);
    }

    @Override
    protected boolean canBind(Stegonaut dragon) {
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
        return "saintsdragons.tooltip.stegonaut_binder.description";
    }

    public static boolean isBound(ItemStack stack) {
        return BinderComponentUtil.isBound(stack);
    }

    @Nullable
    public static UUID getBoundDrakeUUID(ItemStack stack) {
        return BinderComponentUtil.getBoundDragonUuid(stack);
    }

    @Nullable
    public static String getBoundDrakeName(ItemStack stack) {
        return BinderComponentUtil.getBoundDragonName(stack);
    }

    @Override
    @Environment(EnvType.CLIENT)
    public void appendHoverText(@NotNull ItemStack stack, @Nullable Level level, @NotNull List<Component> tooltip,
                                @NotNull TooltipFlag flag) {
        super.appendHoverText(stack, level, tooltip, flag);
    }
}

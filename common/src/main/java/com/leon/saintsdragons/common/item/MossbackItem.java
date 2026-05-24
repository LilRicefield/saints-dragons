package com.leon.saintsdragons.common.item;

import com.leon.saintsdragons.client.renderer.item.MossbackItemRenderer;
import com.leon.saintsdragons.common.registry.ModEntities;
import com.leon.saintsdragons.server.entity.dragons.Mossback;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.NotNull;
import software.bernie.geckolib.animatable.GeoItem;
import software.bernie.geckolib.core.animatable.instance.AnimatableInstanceCache;
import software.bernie.geckolib.core.animation.AnimatableManager;
import software.bernie.geckolib.core.animation.AnimationController;
import software.bernie.geckolib.core.animation.RawAnimation;
import software.bernie.geckolib.core.object.PlayState;
import software.bernie.geckolib.util.GeckoLibUtil;

import java.lang.reflect.Proxy;
import java.util.function.Consumer;
import java.util.function.Supplier;

public class MossbackItem extends Item implements GeoItem {
    private static final RawAnimation IDLE = RawAnimation.begin().thenLoop("animation.mossback.idle");
    private final AnimatableInstanceCache cache = GeckoLibUtil.createInstanceCache(this);
    private final Supplier<Object> renderProvider = this::createFabricRenderProvider;

    public MossbackItem(Properties properties) {
        super(properties);
    }

    @Override
    public @NotNull InteractionResultHolder<ItemStack> use(@NotNull Level level, Player player, @NotNull InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);

        if (level instanceof ServerLevel serverLevel) {
            Mossback mossback = ModEntities.MOSSBACK.get().create(serverLevel);
            if (mossback != null) {
                Vec3 look = player.getLookAngle();
                Vec3 spawn = player.getEyePosition().add(look.scale(0.65D));
                mossback.moveTo(spawn.x, spawn.y - 0.25D, spawn.z, player.getYRot(), 0.0F);
                mossback.setDeltaMovement(look.scale(1.25D).add(0.0D, 0.18D, 0.0D));
                mossback.markThrown();
                serverLevel.addFreshEntity(mossback);
                player.playSound(SoundEvents.SNOWBALL_THROW, 0.6F, 0.85F + player.getRandom().nextFloat() * 0.25F);
                stack.shrink(1);
            }
        }

        return InteractionResultHolder.sidedSuccess(stack, level.isClientSide);
    }

    @Override
    public void registerControllers(AnimatableManager.ControllerRegistrar controllers) {
        controllers.add(new AnimationController<>(this, "idle", 4, state -> {
            state.setAndContinue(IDLE);
            return PlayState.CONTINUE;
        }));
    }

    @Override
    public AnimatableInstanceCache getAnimatableInstanceCache() {
        return cache;
    }

    @Override
    public void createRenderer(Consumer<Object> consumer) {
        Object provider = createFabricRenderProvider();
        if (provider != null) {
            consumer.accept(provider);
        }
    }

    @Override
    public Supplier<Object> getRenderProvider() {
        return renderProvider;
    }

    private Object createFabricRenderProvider() {
        try {
            Class<?> renderProviderClass = Class.forName("software.bernie.geckolib.animatable.client.RenderProvider");
            return Proxy.newProxyInstance(
                    MossbackItem.class.getClassLoader(),
                    new Class<?>[]{renderProviderClass},
                    (proxyInstance, method, args) -> {
                        if ("getCustomRenderer".equals(method.getName())) {
                            return MossbackForgeRendererHolder.renderer();
                        }
                        return null;
                    });
        } catch (ClassNotFoundException ignored) {
            return null;
        }
    }

    public void initializeClient(Consumer<Object> consumer) {
        try {
            Class<?> extensions = Class.forName("net.minecraftforge.client.extensions.common.IClientItemExtensions");
            Object proxy = Proxy.newProxyInstance(
                    MossbackItem.class.getClassLoader(),
                    new Class<?>[]{extensions},
                    (proxyInstance, method, args) -> {
                        if ("getCustomRenderer".equals(method.getName())) {
                            return MossbackForgeRendererHolder.renderer();
                        }
                        return defaultForgeExtensionValue(method.getReturnType());
                    });
            consumer.accept(proxy);
        } catch (ClassNotFoundException ignored) {
        }
    }

    private static Object defaultForgeExtensionValue(Class<?> returnType) {
        if (returnType == Boolean.TYPE) {
            return false;
        }
        if (returnType == Byte.TYPE) {
            return (byte) 0;
        }
        if (returnType == Short.TYPE) {
            return (short) 0;
        }
        if (returnType == Integer.TYPE) {
            return 0;
        }
        if (returnType == Long.TYPE) {
            return 0L;
        }
        if (returnType == Float.TYPE) {
            return 0.0F;
        }
        if (returnType == Double.TYPE) {
            return 0.0D;
        }
        if (returnType == Character.TYPE) {
            return '\0';
        }
        return null;
    }

    private static final class MossbackForgeRendererHolder {
        private static MossbackItemRenderer renderer;

        private static MossbackItemRenderer renderer() {
            if (renderer == null) {
                renderer = new MossbackItemRenderer();
            }
            return renderer;
        }
    }
}

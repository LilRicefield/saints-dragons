package com.leon.saintsdragons.forge.compat;

import com.leon.saintsdragons.client.renderer.vfx.BloodTempestAfterimageRenderContext;
import com.leon.saintsdragons.common.SaintsDragonsCommon;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.eventbus.api.Event;
import net.minecraftforge.eventbus.api.EventPriority;

import java.lang.reflect.Method;

public final class EpicFightRenderCompatibility {
    private static final String PLAYER_RENDER_EVENT =
            "yesman.epicfight.api.client.forgeevent.RenderEpicFightPlayerEvent";

    private static Method setShouldRender;
    private static boolean registered;

    private EpicFightRenderCompatibility() {
    }

    public static void register() {
        if (registered) {
            return;
        }

        try {
            Class<?> rawEventClass = Class.forName(PLAYER_RENDER_EVENT);
            if (!Event.class.isAssignableFrom(rawEventClass)) {
                return;
            }

            setShouldRender = rawEventClass.getMethod("setShouldRender", boolean.class);
            registerListener(rawEventClass.asSubclass(Event.class));
            registered = true;
        } catch (ClassNotFoundException ignored) {
            // Epic Fight is optional.
        } catch (ReflectiveOperationException exception) {
            SaintsDragonsCommon.LOGGER.warn("Could not initialize Epic Fight render compatibility", exception);
        }
    }

    private static <T extends Event> void registerListener(Class<T> eventClass) {
        MinecraftForge.EVENT_BUS.addListener(
                EventPriority.LOWEST,
                false,
                eventClass,
                EpicFightRenderCompatibility::onPlayerRender
        );
    }

    private static void onPlayerRender(Event event) {
        if (!BloodTempestAfterimageRenderContext.isActive() || setShouldRender == null) {
            return;
        }

        try {
            setShouldRender.invoke(event, false);
        } catch (ReflectiveOperationException exception) {
            SaintsDragonsCommon.LOGGER.warn("Could not hand a Blood Tempest afterimage to the vanilla renderer", exception);
            setShouldRender = null;
        }
    }
}

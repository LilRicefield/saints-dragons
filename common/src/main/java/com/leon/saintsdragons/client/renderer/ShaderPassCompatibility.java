package com.leon.saintsdragons.client.renderer;

import java.lang.reflect.Method;

public final class ShaderPassCompatibility {
    private static boolean irisLookupResolved = false;
    private static Method irisGetInstanceMethod;
    private static Method irisShaderPackInUseMethod;
    private static Method irisShadowPassMethod;

    private ShaderPassCompatibility() {
    }

    public static boolean isShaderPackInUse() {
        resolveIrisApi();
        if (irisGetInstanceMethod == null || irisShaderPackInUseMethod == null) return false;
        try {
            return Boolean.TRUE.equals(irisShaderPackInUseMethod.invoke(irisGetInstanceMethod.invoke(null)));
        } catch (ReflectiveOperationException | LinkageError ignored) {
            return false;
        }
    }

    public static boolean isIrisShadowPass() {
        resolveIrisApi();
        if (irisGetInstanceMethod == null || irisShaderPackInUseMethod == null || irisShadowPassMethod == null) {
            return false;
        }

        try {
            Object irisApi = irisGetInstanceMethod.invoke(null);
            if (!(irisShaderPackInUseMethod.invoke(irisApi) instanceof Boolean shadersEnabled) || !shadersEnabled) {
                return false;
            }
            Object shadowPass = irisShadowPassMethod.invoke(irisApi);
            return shadowPass instanceof Boolean active && active;
        } catch (ReflectiveOperationException ignored) {
            return false;
        }
    }

    private static void resolveIrisApi() {
        if (irisLookupResolved) {
            return;
        }
        irisLookupResolved = true;

        for (String apiName : new String[]{"net.irisshaders.iris.api.v0.IrisApi",
                "net.coderbot.iris.api.v0.IrisApi"}) {
            try {
                Class<?> irisApiClass = Class.forName(apiName);
                Method instance = irisApiClass.getMethod("getInstance");
                Method packInUse = irisApiClass.getMethod("isShaderPackInUse");
                irisGetInstanceMethod = instance;
                irisShaderPackInUseMethod = packInUse;
                try {
                    irisShadowPassMethod = irisApiClass.getMethod("isRenderingShadowPass");
                } catch (NoSuchMethodException ignored) {
                    // Pack detection does not depend on shadow-pass API availability.
                }
                return;
            } catch (ReflectiveOperationException | LinkageError ignored) {
                // Older Iris/Oculus releases use the legacy package name.
            }
        }
    }
}

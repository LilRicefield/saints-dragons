package com.leon.saintsdragons.client.renderer;

import java.lang.reflect.Method;

public final class ShaderPassCompatibility {
    private static boolean irisLookupResolved = false;
    private static Method irisGetInstanceMethod;
    private static Method irisShaderPackInUseMethod;
    private static Method irisShadowPassMethod;

    private ShaderPassCompatibility() {
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

        try {
            Class<?> irisApiClass = Class.forName("net.irisshaders.iris.api.v0.IrisApi");
            irisGetInstanceMethod = irisApiClass.getMethod("getInstance");
            irisShaderPackInUseMethod = irisApiClass.getMethod("isShaderPackInUse");
            irisShadowPassMethod = irisApiClass.getMethod("isRenderingShadowPass");
        } catch (ReflectiveOperationException ignored) {
            irisGetInstanceMethod = null;
            irisShaderPackInUseMethod = null;
            irisShadowPassMethod = null;
        }
    }
}

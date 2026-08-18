package com.leon.saintsdragons.forge.datagen;

import java.lang.reflect.InvocationTargetException;

/**
 * Runs Forge data generation behind an explicit JVM exit boundary.
 *
 * <p>Architectury Transformer 5.2.91 leaks non-daemon classpath-reader executor threads. Forge's
 * bootstrap completes normally, but those threads otherwise keep the data generator JVM alive.
 * This launcher is compiled only into the dataGenerator source set and is not shipped with the mod.
 */
public final class ForgeDataGenBootstrap {
    private static final String FORGE_BOOTSTRAP = "cpw.mods.bootstraplauncher.BootstrapLauncher";

    private ForgeDataGenBootstrap() {
    }

    public static void main(String[] args) {
        int exitCode = 0;

        try {
            Class<?> bootstrap = Class.forName(FORGE_BOOTSTRAP);
            bootstrap.getMethod("main", String[].class).invoke(null, (Object) args);
        } catch (InvocationTargetException exception) {
            exitCode = 1;
            Throwable cause = exception.getCause();
            (cause == null ? exception : cause).printStackTrace();
        } catch (Throwable throwable) {
            exitCode = 1;
            throwable.printStackTrace();
        } finally {
            System.exit(exitCode);
        }
    }
}

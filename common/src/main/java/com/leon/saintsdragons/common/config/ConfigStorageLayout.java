package com.leon.saintsdragons.common.config;

import com.leon.saintsdragons.common.SaintsDragonsCommon;
import com.leon.saintsdragons.platform.Services;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.stream.Stream;

/** Keeps the on-disk config tree aligned with the Client/Server UI hierarchy. */
public final class ConfigStorageLayout {
    private static boolean migrated;

    private ConfigStorageLayout() {
    }

    public static synchronized void migrateLegacyFiles() {
        if (migrated) {
            return;
        }
        migrated = true;

        Path configRoot = Services.PLATFORM.getConfigDirectory();
        copyLegacyFile(configRoot.resolve("saintsdragons/client.toml"),
                configRoot.resolve(SaintsDragonsConfig.CLIENT_COMMON_CONFIG_FILE));
        copyLegacyFile(configRoot.resolve("saintsdragons/spawning.toml"),
                configRoot.resolve(SaintsDragonsConfig.SPAWNING_CONFIG_FILE));
        copyLegacyFile(configRoot.resolve("saintsdragons/server.toml"),
                configRoot.resolve(SaintsDragonsConfig.SERVER_CONFIG_FILE));
        copyLegacyFile(configRoot.resolve("saintsdragons/attributes.toml"),
                configRoot.resolve(SaintsDragonsConfig.DRAGON_ATTRIBUTES_CONFIG_FILE));
        copyLegacyFile(configRoot.resolve("saintsdragons/camera_tuning.json"),
                configRoot.resolve(SaintsDragonsConfig.DRAGON_RIDER_CAMERA_CONFIG_FOLDER).resolve("camera_tuning.json"));
        copyLegacyDirectory(configRoot.resolve("saintsdragons/dragon_attributes"),
                configRoot.resolve(SaintsDragonsConfig.DRAGON_ATTRIBUTES_CONFIG_FOLDER));
    }

    private static void copyLegacyFile(Path source, Path target) {
        if (!Files.isRegularFile(source) || Files.exists(target)) {
            return;
        }
        try {
            Files.createDirectories(target.getParent());
            Files.copy(source, target, StandardCopyOption.COPY_ATTRIBUTES);
            SaintsDragonsCommon.LOGGER.info("Copied legacy config {} to {}", source, target);
        } catch (IOException exception) {
            SaintsDragonsCommon.LOGGER.warn("Could not copy legacy config {} to {}", source, target, exception);
        }
    }

    private static void copyLegacyDirectory(Path source, Path target) {
        if (!Files.isDirectory(source)) {
            return;
        }
        try (Stream<Path> paths = Files.walk(source)) {
            paths.filter(Files::isRegularFile).forEach(path ->
                    copyLegacyFile(path, target.resolve(source.relativize(path))));
        } catch (IOException exception) {
            SaintsDragonsCommon.LOGGER.warn("Could not inspect legacy config directory {}", source, exception);
        }
    }
}

package com.pycoder.createprobabilitytuning.client;

import com.mojang.logging.LogUtils;
import org.slf4j.Logger;

import java.awt.Desktop;
import java.io.IOException;
import java.nio.file.Path;

/** Opens the platform's file manager without depending on a shell script. */
public final class ConfigDirectoryOpener {
    private static final Logger LOGGER = LogUtils.getLogger();

    private ConfigDirectoryOpener() {
    }

    public static boolean open(Path directory) {
        try {
            if (!Desktop.isDesktopSupported()) {
                LOGGER.warn("Desktop file manager is not supported; cannot open {}", directory);
                return false;
            }
            Desktop.getDesktop().open(directory.toFile());
            return true;
        } catch (IOException | UnsupportedOperationException | SecurityException exception) {
            LOGGER.warn("Unable to open config directory {}", directory, exception);
            return false;
        }
    }
}

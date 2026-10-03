package com.pycoder.createprobabilitytuning.client;

import com.mojang.logging.LogUtils;
import org.slf4j.Logger;

import java.awt.Desktop;
import java.io.IOException;
import java.nio.file.Path;

/** 打开操作系统的文件管理器，无需依赖 shell 脚本。 */
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

package com.pycoder.createprobabilitytuning.config;

import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/** 管理当前生效的配置，并提供原子化重载边界。 */
public final class ConfigRuntime {
    private final Path path;
    private final ConfigManager manager;
    private volatile ModConfig current = new ModConfig(Map.of(), List.of());

    public ConfigRuntime(Path path, ConfigManager manager) {
        this.path = Objects.requireNonNull(path, "path");
        this.manager = Objects.requireNonNull(manager, "manager");
    }

    public synchronized ModConfig reload() {
        current = manager.reload(path);
        return current;
    }

    public ModConfig current() {
        return current;
    }
}

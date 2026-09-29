package com.pycoder.createprobabilitytuning.config;

import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/** Owns the active configuration and provides an atomic reload boundary. */
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

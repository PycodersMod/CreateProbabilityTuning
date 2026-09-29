package com.pycoder.createprobabilitytuning.config;

import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertTrue;

class ConfigRuntimeTest {
    @Test
    void reloadCreatesAndLoadsTheConfiguredFile() throws Exception {
        Path config = Files.createTempDirectory("cpt-runtime").resolve("create_probability_tuning.json");
        ConfigRuntime runtime = new ConfigRuntime(config, new ConfigManager());

        ModConfig result = runtime.reload();

        assertTrue(Files.exists(config));
        assertTrue(result.recipes().isEmpty());
        assertTrue(result.diagnostics().isEmpty());
        assertTrue(runtime.current() == result);
    }
}

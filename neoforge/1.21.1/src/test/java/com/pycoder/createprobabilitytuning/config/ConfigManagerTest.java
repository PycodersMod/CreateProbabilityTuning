package com.pycoder.createprobabilitytuning.config;

import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ConfigManagerTest {
    private final ConfigManager manager = new ConfigManager();

    @Test
    void createsDefaultConfigWhenFileIsMissing() throws Exception {
        Path config = Files.createTempDirectory("cpt-config").resolve("create_probability_tuning.json");

        manager.ensureDefaultFile(config);

        assertTrue(Files.exists(config));
        assertTrue(Files.readString(config).contains("recipes"));
    }

    @Test
    void loadsDiscreteAndContinuousRules() throws Exception {
        Path config = Files.createTempFile("cpt-config", ".json");
        Files.writeString(config, """
                {
                  "recipes": {
                    "create:discrete_test": {
                      "enabled": true,
                      "initial": 0.2,
                      "mode": "discrete",
                      "change": [0.2, 0.1, 0.05, 0.0],
                      "max_n": 7
                    },
                    "create:continuous_test": {
                      "initial": 0.2,
                      "mode": "continuous",
                      "change": {"formula": "0.05*sin(n)"}
                    }
                  }
                }
                """);

        ModConfig result = manager.load(config);

        assertEquals(2, result.recipes().size());
        assertEquals(RecipeRule.Mode.DISCRETE, result.recipes().get("create:discrete_test").mode());
        assertEquals(4, result.recipes().get("create:discrete_test").changes().size());
        assertEquals(7, result.recipes().get("create:discrete_test").maxN());
        assertEquals("0.05*sin(n)", result.recipes().get("create:continuous_test").formula());
        assertTrue(result.diagnostics().isEmpty());
    }

    @Test
    void rejectsNegativeOrFractionalMaxN() throws Exception {
        Path config = Files.createTempFile("cpt-config", ".json");
        Files.writeString(config, """
                {"recipes": {
                  "create:negative": {"initial": 0.2, "mode": "discrete", "change": [0.1], "max_n": -1},
                  "create:fraction": {"initial": 0.2, "mode": "discrete", "change": [0.1], "max_n": 1.5}
                }}
                """);

        ModConfig result = manager.load(config);

        assertTrue(result.recipes().isEmpty());
        assertEquals(2, result.diagnostics().size());
    }

    @Test
    void acceptsStringChangeAsContinuousFormula() throws Exception {
        Path config = Files.createTempFile("cpt-continuous-string", ".json");
        Files.writeString(config, """
                {"recipes": {
                  "create:string_formula": {"initial": 0.1, "mode": "continuous", "change": "exp(n)/150", "max_n": 5}
                }}
                """);

        ModConfig result = manager.load(config);

        assertTrue(result.diagnostics().isEmpty());
        assertEquals("exp(n)/150", result.recipes().get("create:string_formula").formula());
        assertEquals(5, result.recipes().get("create:string_formula").maxN());
    }

    @Test
    void unboundedContinuousFormulaStillUsesSafeValidationSampling() throws Exception {
        Path config = Files.createTempFile("cpt-unbounded-formula", ".json");
        Files.writeString(config, """
                {"recipes": {
                  "create:unbounded_formula": {"initial": 0.1, "mode": "continuous", "formula": "exp(n)"}
                }}
                """);

        ModConfig result = manager.load(config);

        assertTrue(result.recipes().containsKey("create:unbounded_formula"));
        assertTrue(result.diagnostics().stream().anyMatch(d -> d.recipeId().equals("create:unbounded_formula")
                && d.severity() == ConfigDiagnostic.Severity.WARNING));
    }

    @Test
    void largeFiniteMaxNDoesNotExpandValidationBeyondSafetyLimit() throws Exception {
        Path config = Files.createTempFile("cpt-large-max", ".json");
        Files.writeString(config, """
                {"recipes": {
                  "create:large_max": {"initial": 0.1, "mode": "continuous", "formula": "0.01", "max_n": 2147483647}
                }}
                """);

        ModConfig result = manager.load(config);

        assertTrue(result.recipes().containsKey("create:large_max"));
        assertTrue(result.diagnostics().isEmpty());
    }

    @Test
    void continuousFormulaUsesOnlyNVariable() throws Exception {
        Path config = Files.createTempFile("cpt-x-formula", ".json");
        Files.writeString(config, """
                {"recipes": {
                  "create:x_formula": {"initial": 0.1, "mode": "continuous", "formula": "exp(x)"}
                }}
                """);

        ModConfig result = manager.load(config);

        assertFalse(result.recipes().containsKey("create:x_formula"));
        assertTrue(result.diagnostics().stream().anyMatch(d -> d.recipeId().equals("create:x_formula")
                && d.severity() == ConfigDiagnostic.Severity.ERROR));
    }

    @Test
    void invalidRuleDoesNotRemoveValidRule() throws Exception {
        Path config = Files.createTempFile("cpt-config", ".json");
        Files.writeString(config, """
                {
                  "recipes": {
                    "create:valid": {"initial": 0.2, "mode": "discrete", "change": [0.1]},
                    "not a resource location": {"initial": 0.2, "mode": "discrete", "change": [0.1]}
                  }
                }
                """);

        ModConfig result = manager.load(config);

        assertTrue(result.recipes().containsKey("create:valid"));
        assertEquals(1, result.recipes().size());
        assertTrue(result.diagnostics().stream().anyMatch(d -> d.severity() == ConfigDiagnostic.Severity.ERROR));
    }

    @Test
    void corruptJsonKeepsFileAndReturnsEmptyConfig() throws Exception {
        Path config = Files.createTempFile("cpt-config", ".json");
        String original = "{ not valid json";
        Files.writeString(config, original);

        ModConfig result = manager.load(config);

        assertTrue(result.recipes().isEmpty());
        assertFalse(result.diagnostics().isEmpty());
        assertEquals(original, Files.readString(config));
    }

    @Test
    void invalidContinuousFormulaDisablesOnlyThatRule() throws Exception {
        Path config = Files.createTempFile("cpt-config", ".json");
        Files.writeString(config, """
                {
                  "recipes": {
                    "create:invalid_formula": {"initial": 0.2, "mode": "continuous", "formula": "0.1*m"},
                    "create:valid": {"initial": 0.2, "mode": "discrete", "change": [0.1]}
                  }
                }
                """);

        ModConfig result = manager.load(config);

        assertFalse(result.recipes().containsKey("create:invalid_formula"));
        assertTrue(result.recipes().containsKey("create:valid"));
        assertTrue(result.diagnostics().stream().anyMatch(d -> d.recipeId().equals("create:invalid_formula")
                && d.severity() == ConfigDiagnostic.Severity.ERROR));
    }

    @Test
    void extremeContinuousFormulaIsRetainedWithWarning() throws Exception {
        Path config = Files.createTempFile("cpt-config", ".json");
        Files.writeString(config, """
                {
                  "recipes": {
                    "create:extreme_formula": {"initial": 0.2, "mode": "continuous", "formula": "1000000000001*n"}
                  }
                }
                """);

        ModConfig result = manager.load(config);

        assertTrue(result.recipes().containsKey("create:extreme_formula"));
        assertTrue(result.diagnostics().stream().anyMatch(d -> d.recipeId().equals("create:extreme_formula")
                && d.severity() == ConfigDiagnostic.Severity.WARNING));
    }
}

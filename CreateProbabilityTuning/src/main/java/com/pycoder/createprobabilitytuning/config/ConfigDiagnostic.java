package com.pycoder.createprobabilitytuning.config;

public record ConfigDiagnostic(String recipeId, Severity severity, Type type, String message) {
    public ConfigDiagnostic(String recipeId, Severity severity, String message) {
        this(recipeId, severity, typeFor(severity), message);
    }

    public enum Severity {
        WARNING,
        ERROR
    }

    public enum Type {
        NUMERIC_RISK,
        SYNTAX,
        MISSING_RECIPE,
        IO
    }

    public static ConfigDiagnostic error(String recipeId, String message) {
        return new ConfigDiagnostic(recipeId, Severity.ERROR, Type.SYNTAX, message);
    }

    public static ConfigDiagnostic warning(String recipeId, String message) {
        return new ConfigDiagnostic(recipeId, Severity.WARNING, Type.MISSING_RECIPE, message);
    }

    public static ConfigDiagnostic numericWarning(String recipeId, String message) {
        return new ConfigDiagnostic(recipeId, Severity.WARNING, Type.NUMERIC_RISK, message);
    }

    private static Type typeFor(Severity severity) {
        return severity == Severity.WARNING ? Type.MISSING_RECIPE : Type.SYNTAX;
    }
}

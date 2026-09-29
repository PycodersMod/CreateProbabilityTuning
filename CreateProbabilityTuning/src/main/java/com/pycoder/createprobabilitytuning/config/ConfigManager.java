package com.pycoder.createprobabilitytuning.config;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import com.google.gson.JsonParser;
import com.pycoder.createprobabilitytuning.probability.FormulaValidator;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.regex.Pattern;

public final class ConfigManager {
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final Pattern RECIPE_ID = Pattern.compile("^[a-z0-9_.-]+:[a-z0-9/._-]+$");
    private static final FormulaValidator FORMULA_VALIDATOR = new FormulaValidator();

    public void ensureDefaultFile(Path path) throws IOException {
        if (Files.exists(path)) {
            return;
        }
        Path parent = path.getParent();
        if (parent != null) {
            Files.createDirectories(parent);
        }
        JsonObject root = new JsonObject();
        root.add("recipes", new JsonObject());
        Files.writeString(path, GSON.toJson(root) + System.lineSeparator(), StandardCharsets.UTF_8);
    }

    public ModConfig reload(Path path) {
        return load(path);
    }

    public ModConfig load(Path path) {
        try {
            ensureDefaultFile(path);
            String content = Files.readString(path, StandardCharsets.UTF_8);
            JsonElement parsed = JsonParser.parseString(content);
            if (!parsed.isJsonObject()) {
                return ModConfig.emptyWith(ConfigDiagnostic.error("<root>", "配置根节点必须是 JSON 对象"));
            }
            return parseRoot(parsed.getAsJsonObject());
        } catch (IOException | JsonParseException | IllegalStateException exception) {
            return ModConfig.emptyWith(ConfigDiagnostic.error("<root>", "配置文件无法读取：" + exception.getMessage()));
        }
    }

    private ModConfig parseRoot(JsonObject root) {
        Map<String, RecipeRule> recipes = new LinkedHashMap<>();
        List<ConfigDiagnostic> diagnostics = new ArrayList<>();
        JsonElement recipesElement = root.get("recipes");
        if (recipesElement == null) {
            diagnostics.add(ConfigDiagnostic.warning("<root>", "缺少 recipes，按空配置处理"));
            return new ModConfig(recipes, diagnostics);
        }
        if (!recipesElement.isJsonObject()) {
            diagnostics.add(ConfigDiagnostic.error("<root>", "recipes 必须是 JSON 对象"));
            return new ModConfig(recipes, diagnostics);
        }

        for (Map.Entry<String, JsonElement> entry : recipesElement.getAsJsonObject().entrySet()) {
            String recipeId = entry.getKey();
            try {
                validateRecipeId(recipeId);
                RecipeRule rule = parseRule(recipeId, entry.getValue());
                if (rule.mode() == RecipeRule.Mode.CONTINUOUS) {
                    FormulaValidator.Result validation = FORMULA_VALIDATOR.validate(rule.formula(), rule.maxN());
                    diagnostics.addAll(validation.diagnostics().stream()
                            .map(diagnostic -> new ConfigDiagnostic(recipeId, diagnostic.severity(),
                                    diagnostic.type(), diagnostic.message()))
                            .toList());
                    if (!validation.valid()) {
                        continue;
                    }
                }
                recipes.put(recipeId, rule);
            } catch (ConfigException exception) {
                diagnostics.add(ConfigDiagnostic.error(recipeId, exception.getMessage()));
            }
        }
        return new ModConfig(recipes, diagnostics);
    }

    private RecipeRule parseRule(String recipeId, JsonElement element) {
        if (!element.isJsonObject()) {
            throw new ConfigException("规则必须是 JSON 对象");
        }
        JsonObject object = element.getAsJsonObject();
        boolean enabled = readBoolean(object, "enabled", true, recipeId);
        double initial = readFiniteNumber(object, "initial", recipeId);
        int maxN = readNaturalNumber(object, "max_n", 0, recipeId);
        String modeValue = readString(object, "mode", recipeId).toLowerCase(Locale.ROOT);
        return switch (modeValue) {
            case "discrete" -> new RecipeRule(enabled, initial, RecipeRule.Mode.DISCRETE,
                    readChanges(object, recipeId), "", maxN);
            case "continuous" -> new RecipeRule(enabled, initial, RecipeRule.Mode.CONTINUOUS,
                    List.of(), readFormula(object, recipeId), maxN);
            default -> throw new ConfigException("mode 必须是 discrete 或 continuous");
        };
    }

    private List<Double> readChanges(JsonObject object, String recipeId) {
        JsonElement change = object.get("change");
        if (change == null || !change.isJsonArray()) {
            throw new ConfigException("discrete 模式的 change 必须是数字数组");
        }
        JsonArray array = change.getAsJsonArray();
        if (array.isEmpty()) {
            throw new ConfigException("discrete 模式的 change 不能为空");
        }
        List<Double> changes = new ArrayList<>();
        for (JsonElement value : array) {
            if (!value.isJsonPrimitive() || !value.getAsJsonPrimitive().isNumber()) {
                throw new ConfigException("change 数组必须全部是数字");
            }
            double number = value.getAsDouble();
            if (!Double.isFinite(number)) {
                throw new ConfigException("change 数组不能包含 NaN 或 Infinity");
            }
            changes.add(number);
        }
        return changes;
    }

    private String readFormula(JsonObject object, String recipeId) {
        JsonElement change = object.get("change");
        if (change != null && change.isJsonPrimitive()
                && change.getAsJsonPrimitive().isString()
                && !change.getAsString().isBlank()) {
            return change.getAsString();
        }
        if (change != null && change.isJsonObject() && change.getAsJsonObject().has("formula")) {
            return readString(change.getAsJsonObject(), "formula", recipeId);
        }
        return readString(object, "formula", recipeId);
    }

    private boolean readBoolean(JsonObject object, String key, boolean defaultValue, String recipeId) {
        JsonElement value = object.get(key);
        if (value == null) {
            return defaultValue;
        }
        if (!value.isJsonPrimitive() || !value.getAsJsonPrimitive().isBoolean()) {
            throw new ConfigException(key + " 必须是布尔值");
        }
        return value.getAsBoolean();
    }

    private double readFiniteNumber(JsonObject object, String key, String recipeId) {
        JsonElement value = object.get(key);
        if (value == null || !value.isJsonPrimitive() || !value.getAsJsonPrimitive().isNumber()) {
            throw new ConfigException(key + " 必须是数字");
        }
        double number = value.getAsDouble();
        if (!Double.isFinite(number)) {
            throw new ConfigException(key + " 不能是 NaN 或 Infinity");
        }
        return number;
    }

    private int readNaturalNumber(JsonObject object, String key, int defaultValue, String recipeId) {
        JsonElement value = object.get(key);
        if (value == null) {
            return defaultValue;
        }
        if (!value.isJsonPrimitive() || !value.getAsJsonPrimitive().isNumber()) {
            throw new ConfigException(key + " 必须是自然数");
        }
        double number = value.getAsDouble();
        if (!Double.isFinite(number) || number < 0 || number != Math.rint(number)
                || number > Integer.MAX_VALUE) {
            throw new ConfigException(key + " 必须是 0 或正整数");
        }
        return (int) number;
    }

    private String readString(JsonObject object, String key, String recipeId) {
        JsonElement value = object.get(key);
        if (value == null || !value.isJsonPrimitive() || !value.getAsJsonPrimitive().isString()
                || value.getAsString().isBlank()) {
            throw new ConfigException(key + " 必须是非空字符串");
        }
        return value.getAsString();
    }

    private void validateRecipeId(String recipeId) {
        if (!RECIPE_ID.matcher(recipeId).matches()) {
            throw new ConfigException("不是合法的 Minecraft Recipe ID");
        }
    }

    private static final class ConfigException extends RuntimeException {
        private ConfigException(String message) {
            super(message);
        }
    }
}

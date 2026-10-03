package com.pycoder.createprobabilitytuning.network;

import com.pycoder.createprobabilitytuning.config.ModConfig;
import com.pycoder.createprobabilitytuning.config.RecipeRule;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public final class ClientRuleSync {
    private static volatile long revision = -1;
    private static volatile ModConfig config = new ModConfig(Map.of(), List.of());

    private ClientRuleSync() {
    }

    public static void accept(RuleSyncPayload payload) {
        if (payload.revision() < revision) return;
        Map<String, RecipeRule> rules = new HashMap<>();
        payload.entries().forEach(entry -> rules.put(entry.recipeId(), entry.rule()));
        config = new ModConfig(rules, List.of());
        revision = payload.revision();
    }

    public static ModConfig config() {
        return config;
    }

    public static void clear() {
        revision = -1;
        config = new ModConfig(Map.of(), List.of());
    }
}

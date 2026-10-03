package com.pycoder.createprobabilitytuning.client;

import com.pycoder.createprobabilitytuning.config.ConfigDiagnostic;

import java.util.ArrayDeque;
import java.util.HashSet;
import java.util.List;
import java.util.Queue;
import java.util.Set;

public final class WarningQueue {
    private final Queue<ConfigDiagnostic> pending = new ArrayDeque<>();
    private final Set<String> shown = new HashSet<>();

    public void enqueue(List<ConfigDiagnostic> diagnostics) {
        for (ConfigDiagnostic diagnostic : diagnostics) {
            String key = diagnostic.recipeId() + "\n" + diagnostic.message();
            if (shown.add(key)) {
                pending.add(diagnostic);
            }
        }
    }

    public ConfigDiagnostic poll() {
        return pending.poll();
    }

    public void clear() {
        pending.clear();
        shown.clear();
    }
}

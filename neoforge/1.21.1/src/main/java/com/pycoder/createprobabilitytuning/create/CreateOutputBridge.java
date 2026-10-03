package com.pycoder.createprobabilitytuning.create;

import java.util.List;
import com.pycoder.createprobabilitytuning.recipe.ProcessingDecision;

/** Keeps Create's already rolled outputs when a recipe is not handled by this mod. */
public final class CreateOutputBridge {
    private CreateOutputBridge() {
    }

    public static <T> List<T> resolve(List<T> createRolledOutputs,
                                       com.pycoder.createprobabilitytuning.recipe.ProcessingDecision.Status status,
                                       List<T> handledOutputs) {
        if (status == ProcessingDecision.Status.DISABLED || status == ProcessingDecision.Status.UNAVAILABLE) {
            return List.copyOf(createRolledOutputs);
        }
        if (status == ProcessingDecision.Status.SUCCESS) {
            return List.copyOf(handledOutputs);
        }
        return List.copyOf(handledOutputs);
    }
}

package com.pycoder.createprobabilitytuning.create;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

class CreateOutputBridgeTest {
    @Test
    void disabledDecisionPreservesCreateRolledOutputs() {
        List<String> resolved = CreateOutputBridge.resolve(
                List.of("create-result"), com.pycoder.createprobabilitytuning.recipe.ProcessingDecision.Status.DISABLED,
                List.of());

        assertEquals(List.of("create-result"), resolved);
    }

    @Test
    void handledDecisionReplacesCreateRolledOutputs() {
        List<String> resolved = CreateOutputBridge.resolve(
                List.of("create-result"), com.pycoder.createprobabilitytuning.recipe.ProcessingDecision.Status.FAILURE,
                List.of("mod-result"));

        assertEquals(List.of("mod-result"), resolved);
    }

    @Test
    void successfulDecisionUsesHandledMainAndHandledExtras() {
        List<String> resolved = CreateOutputBridge.resolve(
                List.of("create-final", "create-extra"), com.pycoder.createprobabilitytuning.recipe.ProcessingDecision.Status.SUCCESS,
                List.of("mod-result", "mod-extra"));

        assertEquals(List.of("mod-result", "mod-extra"), resolved);
    }
}

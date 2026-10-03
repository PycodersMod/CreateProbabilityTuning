package com.pycoder.createprobabilitytuning.create;

import java.util.List;
import com.pycoder.createprobabilitytuning.recipe.ProcessingDecision;

/** 配方未由本 Mod 处理时，保留 Create 已经抽取出的输出结果。 */
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

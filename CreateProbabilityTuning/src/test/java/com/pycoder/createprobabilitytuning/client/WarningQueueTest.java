package com.pycoder.createprobabilitytuning.client;

import com.pycoder.createprobabilitytuning.config.ConfigDiagnostic;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

class WarningQueueTest {
    @Test
    void sameDiagnosticIsShownOnlyOnce() {
        WarningQueue queue = new WarningQueue();
        ConfigDiagnostic warning = ConfigDiagnostic.warning("create:test", "extreme formula");
        queue.enqueue(List.of(warning, warning));

        assertEquals(warning, queue.poll());
        assertNull(queue.poll());
    }
}

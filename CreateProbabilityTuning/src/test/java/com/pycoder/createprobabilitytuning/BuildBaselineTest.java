package com.pycoder.createprobabilitytuning;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

class BuildBaselineTest {
    @Test
    void exposesExpectedModId() {
        assertEquals("createprobabilitytuning", CreateProbabilityTuning.MOD_ID);
    }

    @Test
    void createApiIsAvailableOnTestClasspath() throws ClassNotFoundException {
        assertNotNull(Class.forName("com.simibubi.create.Create"));
    }
}

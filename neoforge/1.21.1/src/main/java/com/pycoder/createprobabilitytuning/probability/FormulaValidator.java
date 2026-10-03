package com.pycoder.createprobabilitytuning.probability;

import com.pycoder.createprobabilitytuning.config.ConfigDiagnostic;

import java.util.ArrayList;
import java.util.List;

public final class FormulaValidator {
    private static final int SAMPLE_COUNT = 1000;
    private static final double EXTREME_VALUE = 1.0E12;

    public Result validate(String formula) {
        return validate(formula, 0);
    }

    /** 配方具有有限的 max_n 时，只验证实际可达的范围。 */
    public Result validate(String formula, int maxN) {
        List<ConfigDiagnostic> diagnostics = new ArrayList<>();
        final FormulaExpression expression;
        try {
            expression = FormulaExpression.parse(formula);
        } catch (RuntimeException exception) {
            diagnostics.add(ConfigDiagnostic.error("<formula>", "公式无法解析：" + exception.getMessage()));
            return new Result(false, diagnostics);
        }

        int sampleCount = maxN > 0 ? Math.min(Math.max(0, maxN - 1), SAMPLE_COUNT) : SAMPLE_COUNT;
        for (int index = 1; index <= sampleCount; index++) {
            final double value;
            try {
                value = expression.evaluate(index);
            } catch (RuntimeException exception) {
                diagnostics.add(ConfigDiagnostic.error("<formula>", "公式计算失败：" + exception.getMessage()));
                return new Result(false, diagnostics);
            }
            if (!Double.isFinite(value)) {
                if (index == 1) {
                    diagnostics.add(ConfigDiagnostic.error("<formula>", "公式在 n=" + index + " 时产生非有限值"));
                    return new Result(false, diagnostics);
                }
                diagnostics.add(ConfigDiagnostic.numericWarning("<formula>",
                        "公式在 n=" + index + " 之后可能产生非有限值，运行到该次数时将暂停该配方"));
                break;
            }
            if (Math.abs(value) > EXTREME_VALUE && !hasWarning(diagnostics)) {
                diagnostics.add(ConfigDiagnostic.numericWarning("<formula>",
                        "公式可能产生异常巨大数值，首次超过安全采样阈值的位置为 n=" + index));
            }
        }
        return new Result(true, diagnostics);
    }

    private boolean hasWarning(List<ConfigDiagnostic> diagnostics) {
        return diagnostics.stream().anyMatch(diagnostic -> diagnostic.severity() == ConfigDiagnostic.Severity.WARNING);
    }

    public record Result(boolean valid, List<ConfigDiagnostic> diagnostics) {
        public Result {
            diagnostics = List.copyOf(diagnostics);
        }

        public boolean hasError() {
            return diagnostics.stream().anyMatch(diagnostic -> diagnostic.severity() == ConfigDiagnostic.Severity.ERROR);
        }

        public boolean hasWarning() {
            return diagnostics.stream().anyMatch(diagnostic -> diagnostic.severity() == ConfigDiagnostic.Severity.WARNING);
        }
    }
}

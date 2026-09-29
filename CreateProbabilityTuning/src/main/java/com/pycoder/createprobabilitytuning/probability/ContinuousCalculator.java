package com.pycoder.createprobabilitytuning.probability;

public final class ContinuousCalculator implements ProbabilityCalculator {
    public static final int MAX_RUNTIME_TERMS = 4096;
    private final FormulaExpression expression;

    public ContinuousCalculator(String formula) {
        expression = FormulaExpression.parse(formula);
    }

    @Override
    public synchronized double calculate(double initial, int failures) {
        if (failures < 0) {
            throw new IllegalArgumentException("失败次数不能为负数");
        }
        if (failures > MAX_RUNTIME_TERMS) {
            throw new IllegalArgumentException("连续公式计算次数超过安全上限 " + MAX_RUNTIME_TERMS);
        }
        double probability = initial;
        for (int index = 1; index <= failures; index++) {
            probability += expression.evaluate(index);
        }
        return probability;
    }
}

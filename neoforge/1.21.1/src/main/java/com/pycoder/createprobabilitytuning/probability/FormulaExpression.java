package com.pycoder.createprobabilitytuning.probability;

import java.util.Locale;

/** Dependency-free evaluator for continuous probability formulas. */
final class FormulaExpression {
    private final String source;
    private int index;

    private FormulaExpression(String source) { this.source = source; }

    static FormulaExpression parse(String source) {
        FormulaExpression parser = new FormulaExpression(source);
        parser.parseExpression(0.0);
        parser.skipWhitespace();
        if (parser.index != source.length()) throw parser.error("公式末尾存在无法解析的内容");
        return parser;
    }

    double evaluate(double n) {
        FormulaExpression parser = new FormulaExpression(source);
        double value = parser.parseExpression(n);
        parser.skipWhitespace();
        if (parser.index != source.length()) throw parser.error("公式末尾存在无法解析的内容");
        return value;
    }

    private double parseExpression(double n) {
        double value = parseTerm(n);
        while (true) {
            skipWhitespace();
            if (take('+')) value += parseTerm(n);
            else if (take('-')) value -= parseTerm(n);
            else return value;
        }
    }

    private double parseTerm(double n) {
        double value = parseUnary(n);
        while (true) {
            skipWhitespace();
            if (take('*')) value *= parseUnary(n);
            else if (take('/')) value /= parseUnary(n);
            else return value;
        }
    }

    private double parsePower(double n) {
        double value = parsePrimary(n);
        skipWhitespace();
        return take('^') ? Math.pow(value, parseUnary(n)) : value;
    }

    private double parseUnary(double n) {
        skipWhitespace();
        if (take('+')) return parseUnary(n);
        if (take('-')) return -parseUnary(n);
        return parsePower(n);
    }

    private double parsePrimary(double n) {
        skipWhitespace();
        if (take('(')) {
            double value = parseExpression(n);
            require(')');
            return value;
        }
        if (index < source.length() && (Character.isDigit(source.charAt(index)) || source.charAt(index) == '.')) {
            return parseNumber();
        }
        String name = parseName();
        if (name.isEmpty()) throw error("需要数字、n、常量或函数");
        if (name.equalsIgnoreCase("n")) return n;
        if (name.equalsIgnoreCase("pi")) return Math.PI;
        if (name.equalsIgnoreCase("e")) return Math.E;
        skipWhitespace();
        require('(');
        double first = parseExpression(n);
        skipWhitespace();
        double second = Double.NaN;
        boolean hasSecond = take(',');
        if (hasSecond) second = parseExpression(n);
        require(')');
        if (hasSecond && !name.equalsIgnoreCase("min") && !name.equalsIgnoreCase("max")) {
            throw error(name + " 只需要一个参数");
        }
        return applyFunction(name.toLowerCase(Locale.ROOT), first, second);
    }

    private double parseNumber() {
        int start = index;
        while (index < source.length() && (Character.isDigit(source.charAt(index)) || source.charAt(index) == '.')) index++;
        if (index < source.length() && (source.charAt(index) == 'e' || source.charAt(index) == 'E')) {
            index++;
            if (index < source.length() && (source.charAt(index) == '+' || source.charAt(index) == '-')) index++;
            while (index < source.length() && Character.isDigit(source.charAt(index))) index++;
        }
        try { return Double.parseDouble(source.substring(start, index)); }
        catch (NumberFormatException exception) { throw error("数字格式错误"); }
    }

    private String parseName() {
        int start = index;
        while (index < source.length() && (Character.isLetterOrDigit(source.charAt(index)))) index++;
        return source.substring(start, index);
    }

    private double applyFunction(String name, double first, double second) {
        return switch (name) {
            case "abs" -> Math.abs(first);
            case "acos" -> Math.acos(first);
            case "asin" -> Math.asin(first);
            case "atan" -> Math.atan(first);
            case "ceil" -> Math.ceil(first);
            case "cos" -> Math.cos(first);
            case "exp" -> Math.exp(first);
            case "floor" -> Math.floor(first);
            case "log", "ln" -> Math.log(first);
            case "log10" -> Math.log10(first);
            case "round" -> Math.round(first);
            case "sin" -> Math.sin(first);
            case "sqrt" -> Math.sqrt(first);
            case "tan" -> Math.tan(first);
            case "min" -> twoArgument(name, second, Math.min(first, second));
            case "max" -> twoArgument(name, second, Math.max(first, second));
            default -> throw error("不支持的函数: " + name);
        };
    }

    private double twoArgument(String name, double second, double result) {
        if (Double.isNaN(second)) throw error(name + " 需要两个参数");
        return result;
    }

    private boolean take(char expected) {
        if (index < source.length() && source.charAt(index) == expected) { index++; return true; }
        return false;
    }

    private void require(char expected) {
        skipWhitespace();
        if (!take(expected)) throw error("缺少 '" + expected + "'");
    }

    private void skipWhitespace() { while (index < source.length() && Character.isWhitespace(source.charAt(index))) index++; }
    private IllegalArgumentException error(String message) { return new IllegalArgumentException(message + "，位置 " + index); }
}

package com.leon.saintsdragons.server.entity.part;

import java.util.function.DoubleUnaryOperator;

final class CollisionExpression {
    private final String text;
    private int cursor;

    private CollisionExpression(String text) { this.text = text.replaceAll("\\s+", ""); }

    static DoubleUnaryOperator compile(String text) {
        CollisionExpression parser = new CollisionExpression(text);
        DoubleUnaryOperator result = parser.sum();
        if (parser.cursor != parser.text.length()) throw new IllegalArgumentException("Unsupported collision expression: " + text);
        return result;
    }

    private boolean take(char token) {
        if (cursor < text.length() && text.charAt(cursor) == token) { cursor++; return true; }
        return false;
    }

    private DoubleUnaryOperator sum() {
        DoubleUnaryOperator value = product();
        while (cursor < text.length()) {
            boolean add = take('+');
            if (!add && !take('-')) break;
            DoubleUnaryOperator left = value, right = product();
            value = add ? t -> left.applyAsDouble(t) + right.applyAsDouble(t)
                    : t -> left.applyAsDouble(t) - right.applyAsDouble(t);
        }
        return value;
    }

    private DoubleUnaryOperator product() {
        DoubleUnaryOperator value = atom();
        while (cursor < text.length()) {
            boolean multiply = take('*');
            if (!multiply && !take('/')) break;
            DoubleUnaryOperator left = value, right = atom();
            value = multiply ? t -> left.applyAsDouble(t) * right.applyAsDouble(t)
                    : t -> left.applyAsDouble(t) / right.applyAsDouble(t);
        }
        return value;
    }

    private DoubleUnaryOperator atom() {
        if (take('+')) return atom();
        if (take('-')) { DoubleUnaryOperator value = atom(); return t -> -value.applyAsDouble(t); }
        if (take('(')) { DoubleUnaryOperator value = sum(); close(); return value; }
        if (text.startsWith("query.anim_time", cursor)) { cursor += 15; return t -> t; }
        if (text.startsWith("math.sin(", cursor) || text.startsWith("math.cos(", cursor)) {
            boolean sine = text.startsWith("math.sin(", cursor);
            cursor += 9;
            DoubleUnaryOperator angle = sum();
            close();
            return sine ? t -> Math.sin(Math.toRadians(angle.applyAsDouble(t)))
                    : t -> Math.cos(Math.toRadians(angle.applyAsDouble(t)));
        }
        int start = cursor;
        while (cursor < text.length() && (Character.isDigit(text.charAt(cursor)) || text.charAt(cursor) == '.')) cursor++;
        if (start == cursor) throw new IllegalArgumentException("Unsupported collision expression: " + text);
        double number = Double.parseDouble(text.substring(start, cursor));
        return t -> number;
    }

    private void close() {
        if (!take(')')) throw new IllegalArgumentException("Unclosed collision expression: " + text);
    }
}

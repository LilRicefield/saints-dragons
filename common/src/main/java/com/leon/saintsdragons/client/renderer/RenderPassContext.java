package com.leon.saintsdragons.client.renderer;

import java.util.ArrayDeque;
import java.util.Deque;

public final class RenderPassContext {
    private static final ThreadLocal<Deque<Integer>> EXTRACTION_STACK =
            ThreadLocal.withInitial(ArrayDeque::new);

    private RenderPassContext() {
    }

    public static void beginExtraction(int dragonId) {
        EXTRACTION_STACK.get().push(dragonId);
    }

    public static void endExtraction() {
        Deque<Integer> stack = EXTRACTION_STACK.get();
        if (!stack.isEmpty()) {
            stack.pop();
        }
        if (stack.isEmpty()) {
            EXTRACTION_STACK.remove();
        }
    }

    public static boolean isExtractionAllowed(int dragonId) {
        Deque<Integer> stack = EXTRACTION_STACK.get();
        return !stack.isEmpty()
                && stack.peek() == dragonId
                && !ShaderPassCompatibility.isIrisShadowPass();
    }
}

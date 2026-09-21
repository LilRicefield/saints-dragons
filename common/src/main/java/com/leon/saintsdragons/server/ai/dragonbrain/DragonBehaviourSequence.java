package com.leon.saintsdragons.server.ai.dragonbrain;

import java.util.List;
import java.util.Objects;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import java.util.function.Function;

public final class DragonBehaviourSequence<C> {
    private final List<Step<C>> steps;
    private int index;
    private long stepStartedAt;
    private Result result = Result.IDLE;

    public DragonBehaviourSequence(List<Step<C>> steps) {
        if (steps.isEmpty()) throw new IllegalArgumentException("A sequence requires at least one step");
        this.steps = List.copyOf(steps);
    }

    public void start(C context, long now) {
        cancel(context);
        index = 0;
        result = Result.RUNNING;
        enter(context, now);
    }

    public Result tick(C context, long now) {
        if (result != Result.RUNNING) return result;
        Step<C> step = steps.get(index);
        if (now - stepStartedAt >= step.timeoutTicks) {
            result = Result.TIMED_OUT;
            step.exit.accept(context, result);
            return result;
        }
        Result next = Objects.requireNonNull(step.tick.apply(context));
        if (result != Result.RUNNING || steps.get(index) != step) return result;
        if (next == Result.IDLE) throw new IllegalStateException("A running step cannot return IDLE");
        if (next == Result.RUNNING) return result;
        result = next;
        step.exit.accept(context, next);
        if (next == Result.SUCCESS && index + 1 < steps.size()) {
            index++;
            result = Result.RUNNING;
            enter(context, now);
        }
        return result;
    }

    public void cancel(C context) {
        if (result == Result.RUNNING) {
            result = Result.CANCELLED;
            steps.get(index).exit.accept(context, result);
        }
    }

    public Result result() {
        return result;
    }

    public String stepName() {
        return result == Result.IDLE ? "none" : steps.get(index).name;
    }

    private void enter(C context, long now) {
        stepStartedAt = now;
        steps.get(index).enter.accept(context);
    }

    public record Step<C>(String name, int timeoutTicks, Consumer<C> enter,
                          Function<C, Result> tick, BiConsumer<C, Result> exit) {
        public Step {
            Objects.requireNonNull(name);
            Objects.requireNonNull(enter);
            Objects.requireNonNull(tick);
            Objects.requireNonNull(exit);
            if (timeoutTicks <= 0) throw new IllegalArgumentException("A step requires a positive timeout");
        }

        public static <C> Step<C> of(String name, int timeoutTicks, Function<C, Result> tick) {
            return new Step<>(name, timeoutTicks, context -> {}, tick, (context, result) -> {});
        }
    }

    public enum Result { IDLE, RUNNING, SUCCESS, FAILURE, TIMED_OUT, CANCELLED }
}

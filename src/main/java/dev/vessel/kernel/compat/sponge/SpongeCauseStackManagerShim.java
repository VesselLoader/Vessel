package dev.vessel.kernel.compat.sponge;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.Objects;

/**
 * Shim emulating the Sponge CauseStackManager for tracking event causes and contexts across plugins.
 * Adheres strictly to Rule 17 (Single Responsibility Principle).
 *
 * @version 1.0.0
 */
public final class SpongeCauseStackManagerShim {

    private final ThreadLocal<Deque<Object>> causeStack = ThreadLocal.withInitial(ArrayDeque::new);

    public void pushCause(Object cause) {
        Objects.requireNonNull(cause, "cause cannot be null");
        causeStack.get().push(cause);
    }

    public Object popCause() {
        Deque<Object> stack = causeStack.get();
        return stack.isEmpty() ? null : stack.pop();
    }

    public Object peekCause() {
        Deque<Object> stack = causeStack.get();
        return stack.peek();
    }

    public Object getRootCause() {
        Deque<Object> stack = causeStack.get();
        return stack.isEmpty() ? null : stack.getLast();
    }

    public int getDepth() {
        return causeStack.get().size();
    }

    public void clear() {
        causeStack.get().clear();
    }
}

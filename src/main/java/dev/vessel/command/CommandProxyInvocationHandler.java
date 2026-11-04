package dev.vessel.command;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.util.Objects;

/**
 * Invocation handler proxying cross-ClassLoader Brigadier {@code Command#run(CommandContext)} calls
 * to a target executor instance.
 *
 * <p>Strictly adheres to Rule 17 with zero inner, nested, or anonymous classes.</p>
 *
 * @version 1.0.0
 */
public final class CommandProxyInvocationHandler implements InvocationHandler {

    private final Object delegateExecutor;

    public CommandProxyInvocationHandler(Object delegateExecutor) {
        this.delegateExecutor = Objects.requireNonNull(delegateExecutor, "delegateExecutor must not be null");
    }

    @Override
    public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
        if ("run".equals(method.getName()) && args != null && args.length == 1) {
            for (Method m : delegateExecutor.getClass().getMethods()) {
                if ("run".equals(m.getName()) && m.getParameterCount() == 1) {
                    return m.invoke(delegateExecutor, args[0]);
                }
            }
            return 1;
        }
        if ("toString".equals(method.getName())) {
            return "CommandProxy[" + delegateExecutor.getClass().getSimpleName() + "]";
        }
        if ("hashCode".equals(method.getName())) {
            return System.identityHashCode(this);
        }
        if ("equals".equals(method.getName()) && args != null && args.length == 1) {
            return proxy == args[0];
        }
        return null;
    }
}

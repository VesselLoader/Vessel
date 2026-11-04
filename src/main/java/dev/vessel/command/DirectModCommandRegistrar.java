package dev.vessel.command;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Objects;

/**
 * Direct registrar for known Forge and NeoForge ecosystem mod commands (such as Mekanism and Create)
 * providing resilient registration when declarative event dispatch is deferred or constrained.
 *
 * <p>Strictly adheres to Rule 17 with zero inner, nested, or anonymous classes.</p>
 *
 * @version 1.0.3
 */
public final class DirectModCommandRegistrar {

    private final BrigadierCommandNodeAttacher commandNodeAttacher = new BrigadierCommandNodeAttacher();

    public DirectModCommandRegistrar() {
    }

    /**
     * Attempts to directly bind Mekanism and Create commands into the running Brigadier dispatcher.
     *
     * @param dispatcher Brigadier command dispatcher
     * @param loader     classloader containing active mod classes
     * @return count of mod command trees successfully wired
     */
    @SuppressWarnings("unchecked")
    public int registerDirectModCommands(Object dispatcher, ClassLoader loader) {
        if (dispatcher == null || loader == null) {
            return 0;
        }

        int registered = 0;

        // 1. Mekanism: CommandMek.register() -> LiteralArgumentBuilder
        try {
            Class<?> cmdMekClass = Class.forName("mekanism.common.command.CommandMek", false, loader);
            Method regMethod = cmdMekClass.getMethod("register");
            Object builder = regMethod.invoke(null);
            if (builder != null) {
                if (builder instanceof LiteralArgumentBuilder<?> literalBuilder && literalBuilder.getCommand() == null) {
                    ((LiteralArgumentBuilder) literalBuilder).executes(new MekanismRootCommandExecutor());
                }
                if (dispatcher instanceof CommandDispatcher rawDispatcher && builder instanceof LiteralArgumentBuilder literalBuilder) {
                    rawDispatcher.register(literalBuilder);
                    registered++;
                    System.out.println("[VESSEL] Successfully registered direct command tree: /mek");
                } else {
                    Method regOnDisp = dispatcher.getClass().getMethod("register", builder.getClass());
                    regOnDisp.invoke(dispatcher, builder);
                    registered++;
                    System.out.println("[VESSEL] Successfully registered reflective command tree: /mek");
                }
                commandNodeAttacher.attachFallbackIfUnset(dispatcher, "mek", new MekanismRootCommandExecutor());
            }
        } catch (ClassNotFoundException ignored) {
            // Mekanism not in this loader
        } catch (Throwable t) {
            Throwable root = (t instanceof InvocationTargetException ite && ite.getCause() != null) ? ite.getCause() : t;
            System.err.println("[VESSEL] Notice: Mekanism direct command registration root cause: " + root);
            root.printStackTrace(System.err);
        }

        // 2. Create: AllCommands.register(dispatcher)
        try {
            Class<?> allCmdsClass = Class.forName("com.simibubi.create.infrastructure.command.AllCommands", false, loader);
            for (Method m : allCmdsClass.getMethods()) {
                if (m.getName().equals("register") && m.getParameterCount() == 1) {
                    m.invoke(null, dispatcher);
                    registered++;
                    commandNodeAttacher.attachFallbackIfUnset(dispatcher, "create", new CreateRootCommandExecutor());
                    commandNodeAttacher.attachFallbackIfUnset(dispatcher, "c", new CreateRootCommandExecutor());
                    System.out.println("[VESSEL] Successfully registered direct command tree: /create");
                    break;
                }
            }
        } catch (ClassNotFoundException ignored) {
            // Create not in this loader
        } catch (Throwable t) {
            Throwable root = (t instanceof InvocationTargetException ite && ite.getCause() != null) ? ite.getCause() : t;
            System.err.println("[VESSEL] Notice: Create direct command registration root cause: " + root);
            root.printStackTrace(System.err);
        }

        return registered;
    }
}

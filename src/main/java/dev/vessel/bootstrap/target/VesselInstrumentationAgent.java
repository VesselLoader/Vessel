package dev.vessel.bootstrap.target;

import java.lang.instrument.Instrumentation;

/**
 * JVM Instrumentation Agent entrypoint ({@code Premain-Class} and {@code Agent-Class}) providing
 * class redefinition and bytecode instrumentation hooks for the VESSEL microkernel.
 *
 * <p>Strictly adheres to Rule 17 with zero inner or nested classes.</p>
 *
 * @version 1.0.0
 * @author Shiroz &amp; Antigravity
 * @since 1.0.0 (Pass K-29)
 */
public final class VesselInstrumentationAgent {

    private static volatile Instrumentation activeInstrumentation;
    private static volatile String lastAgentArguments = "";

    /**
     * Standard JVM {@code -javaagent} premain entrypoint invoked prior to {@code main}.
     *
     * @param agentArgs agent option string
     * @param inst      JVM {@link Instrumentation} handle
     */
    public static void premain(String agentArgs, Instrumentation inst) {
        activeInstrumentation = inst;
        lastAgentArguments = agentArgs != null ? agentArgs : "";
    }

    /**
     * Dynamic attach agent entrypoint invoked when attaching to a running JVM.
     *
     * @param agentArgs agent option string
     * @param inst      JVM {@link Instrumentation} handle
     */
    public static void agentmain(String agentArgs, Instrumentation inst) {
        activeInstrumentation = inst;
        lastAgentArguments = agentArgs != null ? agentArgs : "";
    }

    /**
     * Checks whether a live JVM {@link Instrumentation} instance has been installed.
     *
     * @return {@code true} if instrumentation is active
     */
    public boolean isInstrumentationActive() {
        return activeInstrumentation != null || !lastAgentArguments.isEmpty();
    }

    /**
     * Returns the active {@link Instrumentation} handle, or {@code null} if not attached.
     *
     * @return instrumentation instance or {@code null}
     */
    public static Instrumentation getInstrumentation() {
        return activeInstrumentation;
    }
}

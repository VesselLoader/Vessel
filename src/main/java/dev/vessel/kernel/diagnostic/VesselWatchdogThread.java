package dev.vessel.kernel.diagnostic;

import java.lang.management.ManagementFactory;
import java.lang.management.ThreadInfo;
import java.lang.management.ThreadMXBean;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Monitors server tick heartbeats (`VesselWatchdogThread`), detects hung tick loops exceeding the
 * configured timeout threshold, and captures full JVM thread dumps for post-mortem diagnostics.
 *
 * @version 1.0.0
 */
public final class VesselWatchdogThread {

    private final AtomicLong lastHeartbeatMillis = new AtomicLong(0L);

    /**
     * Records a server tick heartbeat at the specified monotonic timestamp in milliseconds.
     *
     * @param currentTimestampMillis the current timestamp in milliseconds
     */
    public void heartbeatTick(long currentTimestampMillis) {
        lastHeartbeatMillis.set(currentTimestampMillis);
    }

    /**
     * Evaluates whether the server tick loop has stalled longer than {@code timeoutMillis} since
     * the last recorded heartbeat.
     *
     * @param currentTimestampMillis the current timestamp in milliseconds
     * @param timeoutMillis          the hang timeout threshold in milliseconds
     * @return {@code true} if the server is hanging
     */
    public boolean isServerHanging(long currentTimestampMillis, long timeoutMillis) {
        long lastBeat = lastHeartbeatMillis.get();
        if (lastBeat <= 0L || timeoutMillis <= 0L) {
            return false;
        }
        return (currentTimestampMillis - lastBeat) > timeoutMillis;
    }

    /**
     * Captures a formatted diagnostic dump of all live JVM threads and their stack traces.
     *
     * @return the formatted diagnostic thread dump string
     */
    public String captureDiagnosticThreadDump() {
        ThreadMXBean bean = ManagementFactory.getThreadMXBean();
        ThreadInfo[] infos = bean.dumpAllThreads(true, true);
        StringBuilder dump = new StringBuilder(2048);
        dump.append("=== VESSEL WATCHDOG THREAD DUMP (Threads: ").append(infos.length).append(") ===\n");
        for (ThreadInfo info : infos) {
            if (info == null) {
                continue;
            }
            dump.append('"')
                    .append(info.getThreadName())
                    .append("\" Id=")
                    .append(info.getThreadId())
                    .append(" ")
                    .append(info.getThreadState())
                    .append('\n');
            StackTraceElement[] stack = info.getStackTrace();
            int limit = Math.min(stack.length, 8);
            for (int i = 0; i < limit; i++) {
                dump.append("    at ").append(stack[i]).append('\n');
            }
        }
        return dump.toString();
    }
}

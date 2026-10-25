package dev.vessel.kernel.sanitizer;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.net.URL;
import java.net.URLClassLoader;
import java.nio.ByteBuffer;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests verifying proactive Metaspace leak cleanup and off-heap direct buffer watchdog deallocations.
 *
 * <p>Strictly adheres to Rule 17 with zero inner, nested, or anonymous classes.</p>
 *
 * @version 1.0.0
 * @author VESSEL Engine Team
 */
public final class MetaspaceAndDirectBufferSanitizerTest {

    @Test
    @DisplayName("Verify Metaspace memory leak sanitizer tracking and cache flushing")
    public void testMetaspaceMemoryLeakSanitization() {
        MetaspaceMemoryLeakSanitizer sanitizer = new MetaspaceMemoryLeakSanitizer();

        URLClassLoader mockLoader = new URLClassLoader(new URL[0], ClassLoader.getPlatformClassLoader());
        sanitizer.trackClassLoader("TestPluginLoader", mockLoader);

        assertThat(sanitizer.getTrackedCount()).isEqualTo(1);

        MetaspaceSanitizeReport report = sanitizer.sanitize();

        assertThat(report.trackedLoadersCount()).isEqualTo(1);
        assertThat(report.durationNanos()).isGreaterThan(0L);
    }

    @Test
    @DisplayName("Verify DirectBufferLeakWatchdog tracking, explicit release, and orphan reclamation")
    public void testDirectBufferWatchdogAndDeallocation() {
        DirectBufferLeakWatchdog watchdog = new DirectBufferLeakWatchdog();

        ByteBuffer directBuffer = ByteBuffer.allocateDirect(1024 * 64); // 64 KB
        watchdog.trackDirectBuffer("KryptonNettyArena", directBuffer);

        assertThat(watchdog.getTrackedCount()).isEqualTo(1);

        // Explicit release
        boolean released = watchdog.releaseDirectBuffer(directBuffer);
        assertThat(released).isTrue();
        assertThat(watchdog.getTrackedCount()).isEqualTo(0);

        // Audit and reclaim check
        DirectBufferAuditReport auditReport = watchdog.auditAndReclaim();
        assertThat(auditReport.activeTrackedBuffers()).isEqualTo(0);
        assertThat(auditReport.currentDirectAllocatedBytes()).isEqualTo(0L);
    }

    @Test
    @DisplayName("Verify DirectBufferDeallocator safely rejects heap buffers")
    public void testDirectBufferDeallocatorRejectsHeapBuffers() {
        ByteBuffer heapBuffer = ByteBuffer.allocate(128);
        boolean deallocated = DirectBufferDeallocator.deallocate(heapBuffer);
        assertThat(deallocated).isFalse();
    }
}

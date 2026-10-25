package dev.vessel.kernel.vfs;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.nio.ByteBuffer;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Unit test suite for {@link ThreadLocalDirectBufferPoolRecycler}.
 *
 * @version 1.0.0
 * @author VESSEL Engine Team
 */
class ThreadLocalDirectBufferPoolRecyclerTest {

    @AfterEach
    void tearDown() {
        ThreadLocalDirectBufferPoolRecycler.clearCurrentThread();
    }

    @Test
    @DisplayName("Acquires and recycles DirectByteBuffer through pool lifecycle")
    void testAcquireAndReleaseLifecycle() {
        ByteBuffer buf1 = ThreadLocalDirectBufferPoolRecycler.acquire(4096);
        assertThat(buf1.isDirect()).isTrue();
        assertThat(buf1.capacity()).isGreaterThanOrEqualTo(4096);
        assertThat(buf1.position()).isEqualTo(0);

        // Dirty the buffer
        buf1.putInt(12345);
        assertThat(buf1.position()).isEqualTo(4);

        // Recycle the buffer
        ThreadLocalDirectBufferPoolRecycler.release(buf1);
        assertThat(ThreadLocalDirectBufferPoolRecycler.currentThreadRetainedBytes()).isGreaterThan(0);

        // Re-acquire should yield a cleared buffer
        ByteBuffer buf2 = ThreadLocalDirectBufferPoolRecycler.acquire(4096);
        assertThat(buf2).isSameAs(buf1);
        assertThat(buf2.position()).isEqualTo(0);

        ThreadLocalDirectBufferPoolRecycler.release(buf2);
    }

    @Test
    @DisplayName("Selects the appropriate tiered capacity matching requested minimum size")
    void testTierSelection() {
        ByteBuffer bufSmall = ThreadLocalDirectBufferPoolRecycler.acquire(2048);
        assertThat(bufSmall.capacity()).isEqualTo(4096); // 4 KB tier
        ThreadLocalDirectBufferPoolRecycler.release(bufSmall);

        ByteBuffer bufMedium = ThreadLocalDirectBufferPoolRecycler.acquire(10000);
        assertThat(bufMedium.capacity()).isEqualTo(16384); // 16 KB tier
        ThreadLocalDirectBufferPoolRecycler.release(bufMedium);

        ByteBuffer bufLarge = ThreadLocalDirectBufferPoolRecycler.acquire(50000);
        assertThat(bufLarge.capacity()).isEqualTo(65536); // 64 KB tier
        ThreadLocalDirectBufferPoolRecycler.release(bufLarge);
    }

    @Test
    @DisplayName("Ensures complete thread isolation of ThreadLocal buffers across workers")
    void testThreadIsolation() throws InterruptedException {
        ExecutorService executor = Executors.newFixedThreadPool(2);
        CountDownLatch latch = new CountDownLatch(2);
        AtomicBoolean success = new AtomicBoolean(true);

        for (int i = 0; i < 2; i++) {
            executor.submit(() -> {
                try {
                    ByteBuffer buf = ThreadLocalDirectBufferPoolRecycler.acquire(4096);
                    if (!buf.isDirect() || buf.capacity() < 4096) {
                        success.set(false);
                    }
                    ThreadLocalDirectBufferPoolRecycler.release(buf);
                    ThreadLocalDirectBufferPoolRecycler.clearCurrentThread();
                } catch (Exception e) {
                    success.set(false);
                } finally {
                    latch.countDown();
                }
            });
        }

        boolean finished = latch.await(5, TimeUnit.SECONDS);
        executor.shutdown();

        assertThat(finished).isTrue();
        assertThat(success.get()).isTrue();
    }

    @Test
    @DisplayName("Clears current thread direct buffers and resets retained byte counter")
    void testClearCurrentThread() {
        ByteBuffer buf = ThreadLocalDirectBufferPoolRecycler.acquire(16384);
        ThreadLocalDirectBufferPoolRecycler.release(buf);

        assertThat(ThreadLocalDirectBufferPoolRecycler.currentThreadRetainedBytes()).isGreaterThan(0);

        ThreadLocalDirectBufferPoolRecycler.clearCurrentThread();
        assertThat(ThreadLocalDirectBufferPoolRecycler.currentThreadRetainedBytes()).isEqualTo(0);
    }
}

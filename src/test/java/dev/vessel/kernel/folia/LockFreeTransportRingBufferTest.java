package dev.vessel.kernel.folia;

import dev.vessel.kernel.folia.transport.LockFreeTransportRingBuffer;
import dev.vessel.kernel.folia.transport.TransportPacket;
import dev.vessel.kernel.folia.transport.TransportResourceType;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Unit tests for {@link LockFreeTransportRingBuffer}.
 * <p>
 * Verifies lock-free MPSC enqueue, drain, power-of-two capacity sizing,
 * and high-concurrency cross-thread data integrity.
 * </p>
 *
 * @version 1.0.0
 * @author VESSEL Engine Team
 */
public class LockFreeTransportRingBufferTest {

    @Test
    public void testCapacityRoundingAndInitialState() {
        LockFreeTransportRingBuffer<TransportPacket> buffer = new LockFreeTransportRingBuffer<>(100);
        assertThat(buffer.capacity()).isEqualTo(128);
        assertThat(buffer.isEmpty()).isTrue();
        assertThat(buffer.isFull()).isFalse();
        assertThat(buffer.size()).isEqualTo(0);
        assertThat(buffer.poll()).isNull();
    }

    @Test
    public void testCapacityBoundsValidation() {
        assertThatThrownBy(() -> new LockFreeTransportRingBuffer<>(8))
                .isInstanceOf(IllegalArgumentException.class);

        assertThatThrownBy(() -> new LockFreeTransportRingBuffer<>((1 << 20) + 1))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    public void testOfferPollAndDrainSingleThread() {
        LockFreeTransportRingBuffer<TransportPacket> buffer = new LockFreeTransportRingBuffer<>(16);

        TransportPacket packet1 = new TransportPacket(
                1L, 2L, 10, 64, 20, TransportResourceType.ENERGY, 500L, null
        );
        TransportPacket packet2 = new TransportPacket(
                1L, 2L, 11, 64, 21, TransportResourceType.FLUID, 1000L, null
        );

        assertThat(buffer.offer(packet1)).isTrue();
        assertThat(buffer.offer(packet2)).isTrue();
        assertThat(buffer.size()).isEqualTo(2);
        assertThat(buffer.isEmpty()).isFalse();

        TransportPacket polled = buffer.poll();
        assertThat(polled).isNotNull();
        assertThat(polled.resourceType()).isEqualTo(TransportResourceType.ENERGY);
        assertThat(polled.amount()).isEqualTo(500L);

        List<TransportPacket> drained = new ArrayList<>();
        int count = buffer.drainTo(drained::add);

        assertThat(count).isEqualTo(1);
        assertThat(drained.get(0).resourceType()).isEqualTo(TransportResourceType.FLUID);
        assertThat(buffer.isEmpty()).isTrue();
    }

    @Test
    public void testBufferSaturationAndRejection() {
        LockFreeTransportRingBuffer<TransportPacket> buffer = new LockFreeTransportRingBuffer<>(16);

        for (int i = 0; i < 16; i++) {
            TransportPacket packet = new TransportPacket(
                    1L, 2L, i, 64, 0, TransportResourceType.ITEM, 1L, null
            );
            boolean enqueued = buffer.offer(packet);
            assertThat(enqueued).isTrue();
        }

        assertThat(buffer.isFull()).isTrue();
        assertThat(buffer.size()).isEqualTo(16);

        TransportPacket overflow = new TransportPacket(
                1L, 2L, 99, 64, 99, TransportResourceType.ITEM, 1L, null
        );
        assertThat(buffer.offer(overflow)).isFalse();

        // Clear and verify recovered state
        buffer.clear();
        assertThat(buffer.isEmpty()).isTrue();
        assertThat(buffer.size()).isEqualTo(0);
        assertThat(buffer.offer(overflow)).isTrue();
    }

    @Test
    public void testMultiProducerSingleConsumerConcurrency() throws InterruptedException {
        int producerCount = 4;
        int itemsPerProducer = 1000;
        int totalExpected = producerCount * itemsPerProducer;

        LockFreeTransportRingBuffer<TransportPacket> buffer = new LockFreeTransportRingBuffer<>(4096);
        ExecutorService producerPool = Executors.newFixedThreadPool(producerCount);
        CountDownLatch startLatch = new CountDownLatch(1);
        CountDownLatch doneLatch = new CountDownLatch(producerCount);

        for (int p = 0; p < producerCount; p++) {
            final int producerId = p;
            producerPool.submit(() -> {
                try {
                    startLatch.await();
                    for (int i = 0; i < itemsPerProducer; i++) {
                        TransportPacket packet = new TransportPacket(
                                producerId, 100L, i, 64, 0,
                                TransportResourceType.ENERGY, 10L, null
                        );
                        while (!buffer.offer(packet)) {
                            Thread.yield();
                        }
                    }
                } catch (InterruptedException ignored) {
                    Thread.currentThread().interrupt();
                } finally {
                    doneLatch.countDown();
                }
            });
        }

        startLatch.countDown();

        AtomicLong totalEnergyReceived = new AtomicLong(0L);
        int receivedCount = 0;

        while (receivedCount < totalExpected) {
            TransportPacket packet = buffer.poll();
            if (packet != null) {
                totalEnergyReceived.addAndGet(packet.amount());
                receivedCount++;
            } else {
                Thread.yield();
            }
        }

        doneLatch.await(5, TimeUnit.SECONDS);
        producerPool.shutdown();

        assertThat(receivedCount).isEqualTo(totalExpected);
        assertThat(totalEnergyReceived.get()).isEqualTo(totalExpected * 10L);
        assertThat(buffer.isEmpty()).isTrue();
    }
}

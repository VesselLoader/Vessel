package dev.vessel.kernel.adapter.fabric;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Unit tests validating Fabric and Quilt mod initializers and DAG array-backed event dispatching.
 *
 * <p>Validates elimination of: ERR-35, ERR-57, ECO-051..080.</p>
 * <p>Strictly adheres to Rule 17 (zero inner, nested, or anonymous classes).</p>
 *
 * @version 1.0.0
 * @author VESSEL Engine Team
 */
public final class FabricAndQuiltInitializerTest {

    private FabricModInitializerInvoker fabricInvoker;
    private QuiltModInitializerInvoker quiltInvoker;
    private ClassLoader classLoader;

    @BeforeEach
    public void setUp() {
        fabricInvoker = new FabricModInitializerInvoker();
        quiltInvoker = new QuiltModInitializerInvoker();
        classLoader = getClass().getClassLoader();
        MockFabricModInitializer.initialized = false;
        MockFabricServerInitializer.initialized = false;
        MockQuiltModInitializer.initialized = false;
        MockQuiltModInitializer.passedContainer = null;
        MockQuiltZeroArgInitializer.initialized = false;
    }

    @Test
    @DisplayName("FabricModInitializerInvoker: Successfully invokes main entrypoint")
    public void testFabricMainInvocation() {
        FabricInvocationResult result = fabricInvoker.invokeMainEntrypoint(
                "test-fabric-mod",
                MockFabricModInitializer.class.getName(),
                classLoader
        );

        assertThat(result.status()).isEqualTo(FabricInvocationStatus.SUCCESS);
        assertThat(result.phase()).isEqualTo(FabricModInitializerInvoker.PHASE_MAIN);
        assertThat(MockFabricModInitializer.initialized).isTrue();
        assertThat(fabricInvoker.getInvocationHistory()).hasSize(1);
    }

    @Test
    @DisplayName("FabricModInitializerInvoker: Successfully invokes server entrypoint")
    public void testFabricServerInvocation() {
        FabricInvocationResult result = fabricInvoker.invokeServerEntrypoint(
                "test-fabric-mod",
                MockFabricServerInitializer.class.getName(),
                classLoader
        );

        assertThat(result.status()).isEqualTo(FabricInvocationStatus.SUCCESS);
        assertThat(result.phase()).isEqualTo(FabricModInitializerInvoker.PHASE_SERVER);
        assertThat(MockFabricServerInitializer.initialized).isTrue();
    }

    @Test
    @DisplayName("FabricModInitializerInvoker: Client entrypoint is safely skipped on server")
    public void testFabricClientSkipped() {
        FabricInvocationResult result = fabricInvoker.invokeClientEntrypoint(
                "test-fabric-mod",
                "com.example.ClientInit"
        );

        assertThat(result.status()).isEqualTo(FabricInvocationStatus.SKIPPED);
        assertThat(result.phase()).isEqualTo(FabricModInitializerInvoker.PHASE_CLIENT);
    }

    @Test
    @DisplayName("FabricModInitializerInvoker: Faulting entrypoint captures root exception without crashing")
    public void testFabricFaultHandling() {
        FabricInvocationResult result = fabricInvoker.invokeMainEntrypoint(
                "test-failing-mod",
                MockFailingInitializer.class.getName(),
                classLoader
        );

        assertThat(result.status()).isEqualTo(FabricInvocationStatus.FAILED);
        assertThat(result.getOptionalError()).isPresent();
        assertThat(result.getOptionalError().get()).isInstanceOf(IllegalStateException.class);
    }

    @Test
    @DisplayName("FabricModInitializerInvoker: Non-existent class returns NOT_FOUND")
    public void testFabricClassNotFound() {
        FabricInvocationResult result = fabricInvoker.invokeMainEntrypoint(
                "test-missing-mod",
                "dev.vessel.nonexistent.Class",
                classLoader
        );

        assertThat(result.status()).isEqualTo(FabricInvocationStatus.NOT_FOUND);
        assertThat(result.getOptionalError()).isPresent();
    }

    @Test
    @DisplayName("QuiltModInitializerInvoker: Successfully passes container to Quilt initializer")
    public void testQuiltContainerInvocation() {
        String dummyContainer = "MockModContainerToken";
        QuiltInvocationResult result = quiltInvoker.invokeQuiltModInitializer(
                "test-quilt-mod",
                MockQuiltModInitializer.class.getName(),
                dummyContainer,
                classLoader
        );

        assertThat(result.status()).isEqualTo(QuiltInvocationStatus.SUCCESS);
        assertThat(MockQuiltModInitializer.initialized).isTrue();
        assertThat(MockQuiltModInitializer.passedContainer).isEqualTo(dummyContainer);
        assertThat(quiltInvoker.getInvocationHistory()).hasSize(1);
    }

    @Test
    @DisplayName("QuiltModInitializerInvoker: Fallback to zero-arg initialization when method has no parameter")
    public void testQuiltZeroArgFallback() {
        QuiltInvocationResult result = quiltInvoker.invokeQuiltModInitializer(
                "test-quilt-mod",
                MockQuiltZeroArgInitializer.class.getName(),
                "IgnoredContainer",
                classLoader
        );

        assertThat(result.status()).isEqualTo(QuiltInvocationStatus.SUCCESS);
        assertThat(MockQuiltZeroArgInitializer.initialized).isTrue();
    }

    @Test
    @DisplayName("DagEventArrayBackedDispatcher: Correctly orders listeners based on topological constraints")
    public void testDagEventTopologicalOrdering() {
        DagEventArrayBackedDispatcher<List<String>> dispatcher = new DagEventArrayBackedDispatcher<>();
        List<String> executionLog = new ArrayList<>();

        // Register in arbitrary order: C, A, B. Constraints: A must run before B, B before C
        dispatcher.register("node-C", log -> log.add("C"), Set.of(), Set.of("node-B"));
        dispatcher.register("node-A", log -> log.add("A"), Set.of("node-B"), Set.of());
        dispatcher.register("node-B", log -> log.add("B"), Set.of("node-C"), Set.of("node-A"));

        assertThat(dispatcher.getListenerCount()).isEqualTo(3);

        dispatcher.dispatch(executionLog);

        assertThat(executionLog).containsExactly("A", "B", "C");
    }

    @Test
    @DisplayName("DagEventArrayBackedDispatcher: Detects cyclic dependencies and throws DagCycleDetectedException")
    public void testDagEventCycleDetection() {
        DagEventArrayBackedDispatcher<String> dispatcher = new DagEventArrayBackedDispatcher<>();

        // A before B, B before A -> Direct cycle
        dispatcher.register("node-A", ignored -> {}, Set.of("node-B"), Set.of());

        assertThatThrownBy(() -> {
            dispatcher.register("node-B", ignored -> {}, Set.of("node-A"), Set.of());
        }).isInstanceOf(DagCycleDetectedException.class);
    }

    @Test
    @DisplayName("DagEventArrayBackedDispatcher: Halts execution immediately upon event cancellation")
    public void testDagEventCancellation() {
        DagEventArrayBackedDispatcher<MockCancellableEvent> dispatcher = new DagEventArrayBackedDispatcher<>();
        List<String> visited = new ArrayList<>();

        dispatcher.register("first", event -> {
            visited.add("first");
            event.setCancelled(true);
        }, Set.of("second"), Set.of());

        dispatcher.register("second", event -> {
            visited.add("second");
        }, Set.of(), Set.of("first"));

        MockCancellableEvent event = new MockCancellableEvent("TestPayload");
        dispatcher.dispatch(event);

        assertThat(visited).containsExactly("first");
        assertThat(visited).doesNotContain("second");
    }
}

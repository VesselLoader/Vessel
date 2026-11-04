package dev.vessel.bootstrap.mojang;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.List;
import org.junit.jupiter.api.Test;

/**
 * Unit tests verifying {@link TransformerFailureDiagnosticLogger} and its integration
 * with {@link TierOneGameTransformingClassLoader}.
 *
 * <p>Strictly adheres to Rule 17 with zero inner, nested, or anonymous classes.</p>
 *
 * @version 1.0.0
 */
public final class TransformerFailureDiagnosticLoggerTest {

    @Test
    public void testDefaultConstructor() {
        TransformerFailureDiagnosticLogger logger = new TransformerFailureDiagnosticLogger();
        assertThat(logger.getFailureCount()).isZero();
        assertThat(logger.getFailureHistory()).isEmpty();
    }

    @Test
    public void testCustomPrintStreamAndLogging() {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        PrintStream printStream = new PrintStream(baos, true, StandardCharsets.UTF_8);
        TransformerFailureDiagnosticLogger logger = new TransformerFailureDiagnosticLogger(printStream);

        Exception testException = new IllegalStateException("Corrupted class bytes");
        logger.logFailure("net.minecraft.server.MinecraftServer", "VesselServerBrandTransformer", testException);

        assertThat(logger.getFailureCount()).isEqualTo(1);
        List<String> history = logger.getFailureHistory();
        assertThat(history).hasSize(1);
        assertThat(history.get(0)).contains("VesselServerBrandTransformer")
                .contains("net.minecraft.server.MinecraftServer")
                .contains("IllegalStateException: Corrupted class bytes")
                .contains("[VESSEL/WARN]");

        String streamOutput = baos.toString(StandardCharsets.UTF_8);
        assertThat(streamOutput).contains("Corrupted class bytes");
        assertThat(streamOutput).contains("IllegalStateException");
    }

    @Test
    public void testNullArgumentValidations() {
        assertThatThrownBy(() -> new TransformerFailureDiagnosticLogger(null))
                .isInstanceOf(NullPointerException.class)
                .hasMessageContaining("outStream must not be null");

        TransformerFailureDiagnosticLogger logger = new TransformerFailureDiagnosticLogger();
        assertThatThrownBy(() -> logger.logFailure(null, "TestTransformer", new RuntimeException()))
                .isInstanceOf(NullPointerException.class)
                .hasMessageContaining("className must not be null");

        assertThatThrownBy(() -> logger.logFailure("SomeClass", null, new RuntimeException()))
                .isInstanceOf(NullPointerException.class)
                .hasMessageContaining("transformerName must not be null");
    }

    @Test
    public void testResetClearsHistoryAndCount() {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        PrintStream printStream = new PrintStream(baos, true, StandardCharsets.UTF_8);
        TransformerFailureDiagnosticLogger logger = new TransformerFailureDiagnosticLogger(printStream);

        logger.logFailure("ClassA", "TransA", new RuntimeException("err1"));
        logger.logFailure("ClassB", "TransB", new RuntimeException("err2"));
        assertThat(logger.getFailureCount()).isEqualTo(2);
        assertThat(logger.getFailureHistory()).hasSize(2);

        logger.reset();
        assertThat(logger.getFailureCount()).isZero();
        assertThat(logger.getFailureHistory()).isEmpty();
    }

    @Test
    public void testTierOneGameTransformingClassLoaderIntegration() {
        TransformerFailureDiagnosticLogger diagnosticLogger = new TransformerFailureDiagnosticLogger();
        try (TierOneGameTransformingClassLoader loader = new TierOneGameTransformingClassLoader(
                "test-tier-one-loader",
                new URL[0],
                ClassLoader.getPlatformClassLoader(),
                diagnosticLogger)) {

            assertThat(loader.getDiagnosticLogger()).isSameAs(diagnosticLogger);
            assertThat(loader.getDiagnosticLogger().getFailureCount()).isZero();
        } catch (Exception e) {
            throw new AssertionError("Failed to close test class loader", e);
        }
    }

    @Test
    public void testTierOneGameTransformingClassLoaderDefaultLogger() {
        try (TierOneGameTransformingClassLoader loader = new TierOneGameTransformingClassLoader(
                "test-default-loader",
                new URL[0],
                ClassLoader.getPlatformClassLoader())) {

            assertThat(loader.getDiagnosticLogger()).isNotNull();
            assertThat(loader.getDiagnosticLogger().getFailureCount()).isZero();
        } catch (Exception e) {
            throw new AssertionError("Failed to close test class loader", e);
        }
    }
}

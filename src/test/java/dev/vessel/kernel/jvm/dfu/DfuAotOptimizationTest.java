package dev.vessel.kernel.jvm.dfu;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Unit tests for {@link DfuAotOptimizationPath} and {@link DfuSchemaBinaryFlattener}.
 *
 * @version 1.0.0
 * @author VESSEL Engine Team
 */
public class DfuAotOptimizationTest {

    @Test
    @DisplayName("Bypass: Directly bypasses DFU processing when incoming data version matches server target")
    void testDirectBypass() {
        DfuAotOptimizationPath path = new DfuAotOptimizationPath(DfuAotOptimizationPath.CURRENT_DATA_VERSION);

        DfuOptimizationStatus status = path.evaluate(
                DfuAotOptimizationPath.CURRENT_DATA_VERSION,
                DfuPayloadType.CHUNK
        );

        assertThat(status).isEqualTo(DfuOptimizationStatus.BYPASS);
        assertThat(path.canBypass(DfuAotOptimizationPath.CURRENT_DATA_VERSION)).isTrue();
    }

    @Test
    @DisplayName("AOT: Resolves flattened binary transformation rules for legacy data versions")
    void testFlattenedBinaryRuleEvaluation() {
        DfuAotOptimizationPath path = new DfuAotOptimizationPath(DfuAotOptimizationPath.CURRENT_DATA_VERSION);
        path.prewarmAotSchemas();

        DfuOptimizationStatus status = path.evaluate(
                DfuAotOptimizationPath.DATA_VERSION_1_12_2,
                DfuPayloadType.CHUNK
        );

        assertThat(status).isEqualTo(DfuOptimizationStatus.AOT_FLAT_TABLE);

        long ruleMask = path.getFlattener().lookupRuleBitmask(
                DfuAotOptimizationPath.DATA_VERSION_1_12_2,
                DfuPayloadType.CHUNK
        );
        assertThat(ruleMask & DfuAotOptimizationPath.RULE_FLAG_BLOCK_STATE_FLATTEN).isNotZero();
    }

    @Test
    @DisplayName("Latency: AOT table pre-warming completes under 800 milliseconds")
    void testAotPrewarmLatency() {
        DfuAotOptimizationPath path = new DfuAotOptimizationPath();
        path.prewarmAotSchemas();

        long bootDurationNanos = path.getBootDurationNanos();
        long maxAllowedNanos = 800_000_000L; // 800ms

        assertThat(bootDurationNanos).isLessThan(maxAllowedNanos);
        assertThat(path.isInitialized()).isTrue();
    }

    @Test
    @DisplayName("Flattener: Sorts and indexes rules linearly for cache-friendly binary access")
    void testSchemaFlattenerOrdering() {
        DfuSchemaBinaryFlattener flattener = new DfuSchemaBinaryFlattener();

        flattener.addRule(new DfuFlatRuleEntry(2586, 3837, DfuPayloadType.CHUNK, 4L, 2));
        flattener.addRule(new DfuFlatRuleEntry(1343, 1628, DfuPayloadType.ITEM_STACK, 2L, 0));
        flattener.flatten();

        assertThat(flattener.getRuleCount()).isEqualTo(2);
        assertThat(flattener.getFlattenedEntries().get(0).fromVersion()).isEqualTo(1343);
    }
}

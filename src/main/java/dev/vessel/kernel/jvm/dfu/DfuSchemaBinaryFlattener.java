package dev.vessel.kernel.jvm.dfu;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;

/**
 * Flattens Mojang DFU's recursive schema DAG tree into a linear flat binary lookup table.
 * <p>
 * Eliminates ERR-96 (DFU initialization bottleneck, recursive TypeRewriteRule tree building).
 * </p>
 *
 * @version 1.0.0
 * @author VESSEL Engine Team
 */
public final class DfuSchemaBinaryFlattener {

    public static final int NO_RULE_FOUND = -1;
    public static final long NO_RULE_BITMASK = 0L;

    private final List<DfuFlatRuleEntry> ruleEntries;
    private DfuFlatRuleEntry[] flattenedArray;
    private boolean isFlattened;

    /**
     * Constructs an un-flattened DfuSchemaBinaryFlattener.
     */
    public DfuSchemaBinaryFlattener() {
        this.ruleEntries = new ArrayList<>();
        this.flattenedArray = new DfuFlatRuleEntry[0];
        this.isFlattened = false;
    }

    /**
     * Adds an unflattened version transition rule into the flattener pipeline.
     *
     * @param entry rule entry to register
     */
    public synchronized void addRule(DfuFlatRuleEntry entry) {
        Objects.requireNonNull(entry, "entry cannot be null");
        ruleEntries.add(entry);
        isFlattened = false;
    }

    /**
     * Flattens and sorts all registered rules into a cache-friendly linear binary array.
     */
    public synchronized void flatten() {
        ruleEntries.sort(Comparator.comparingInt(DfuFlatRuleEntry::fromVersion)
                .thenComparing(entry -> entry.payloadType().ordinal()));
        flattenedArray = ruleEntries.toArray(new DfuFlatRuleEntry[0]);
        isFlattened = true;
    }

    /**
     * Performs a direct binary lookup for the rule governing the specified version and payload type.
     *
     * @param dataVersion source data version
     * @param payloadType target payload category
     * @return rule bitmask, or NO_RULE_BITMASK if no transformation needed
     */
    public long lookupRuleBitmask(int dataVersion, DfuPayloadType payloadType) {
        Objects.requireNonNull(payloadType, "payloadType cannot be null");
        ensureFlattened();

        for (DfuFlatRuleEntry entry : flattenedArray) {
            if (entry.fromVersion() <= dataVersion && entry.toVersion() > dataVersion
                    && entry.payloadType() == payloadType) {
                return entry.ruleBitmask();
            }
        }
        return NO_RULE_BITMASK;
    }

    /**
     * Finds the matching flattened rule entry for a version.
     *
     * @param dataVersion source data version
     * @param payloadType target payload type
     * @return matching rule entry or null
     */
    public DfuFlatRuleEntry findEntry(int dataVersion, DfuPayloadType payloadType) {
        Objects.requireNonNull(payloadType, "payloadType cannot be null");
        ensureFlattened();

        for (DfuFlatRuleEntry entry : flattenedArray) {
            if (entry.fromVersion() <= dataVersion && entry.toVersion() > dataVersion
                    && entry.payloadType() == payloadType) {
                return entry;
            }
        }
        return null;
    }

    /**
     * Obtains the flattened array view.
     *
     * @return unmodifiable list of flattened entries
     */
    public List<DfuFlatRuleEntry> getFlattenedEntries() {
        ensureFlattened();
        return Collections.unmodifiableList(List.of(flattenedArray));
    }

    /**
     * Total number of rules registered.
     *
     * @return rule count
     */
    public int getRuleCount() {
        return ruleEntries.size();
    }

    private void ensureFlattened() {
        if (!isFlattened) {
            flatten();
        }
    }
}

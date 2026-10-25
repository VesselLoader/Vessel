package dev.vessel.kernel.vstm.parser;

import dev.vessel.kernel.vstm.engine.VstmClassTransmutationEntry;
import dev.vessel.kernel.vstm.engine.VstmDialectMask;
import dev.vessel.kernel.vstm.engine.VstmFieldTransmutationEntry;
import dev.vessel.kernel.vstm.engine.VstmMethodTransmutationEntry;
import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * Harmonizes and unifies disparate upstream mapping dialect streams into canonical VSTM transmutation matrices.
 *
 * <p>Integrates outputs from Mojang ProGuard, Fabric Tiny v2, NeoForge TSRG v2, and Spigot CSRG.
 * Deduplicates isomorphic mappings by merging dialect bitmasks and enforces deterministic
 * natural sorting across all entries for reproducible off-heap hash table generation (Catalog ref: TRN-0025).</p>
 *
 * @version 1.0.0
 * @since 1.0.0 (Wave 2)
 */
public final class UpstreamDialectHarmonizer {

    private final MojangProGuardParser mojangParser;
    private final FabricTinyV2Parser fabricParser;
    private final NeoForgeTsrg2Parser neoForgeParser;
    private final SpigotCsrgParser spigotParser;

    /**
     * Constructs a default upstream dialect harmonizer with clean-room parsers.
     */
    public UpstreamDialectHarmonizer() {
        this.mojangParser = new MojangProGuardParser();
        this.fabricParser = new FabricTinyV2Parser();
        this.neoForgeParser = new NeoForgeTsrg2Parser();
        this.spigotParser = new SpigotCsrgParser();
    }

    /**
     * Harmonizes multiple parsed dialect results into a unified transmutation matrix.
     *
     * @param results array of parsed dialect results
     * @return unified harmonized transmutation matrix
     */
    public HarmonizedTransmutationMatrix harmonize(ParsedDialectResult... results) {
        if (results == null || results.length == 0) {
            return new HarmonizedTransmutationMatrix(List.of(), List.of(), List.of(), 0);
        }
        return harmonize(List.of(results));
    }

    /**
     * Harmonizes a list of parsed dialect results into a unified transmutation matrix.
     *
     * @param results collection of parsed dialect results
     * @return unified harmonized transmutation matrix
     */
    public HarmonizedTransmutationMatrix harmonize(List<ParsedDialectResult> results) {
        Objects.requireNonNull(results, "results must not be null");

        Map<String, ParsedClassEntryAccumulator> classMap = new LinkedHashMap<>();
        Map<String, ParsedFieldEntryAccumulator> fieldMap = new LinkedHashMap<>();
        Map<String, ParsedMethodEntryAccumulator> methodMap = new LinkedHashMap<>();

        int combinedMask = 0;

        for (ParsedDialectResult result : results) {
            if (result == null) {
                continue;
            }
            combinedMask |= result.dialectMask();

            // Accumulate class mappings
            for (ParsedClassMapping c : result.classes()) {
                String key = c.sourceClass() + "->" + c.targetClass();
                ParsedClassEntryAccumulator acc = classMap.get(key);
                if (acc == null) {
                    classMap.put(key, new ParsedClassEntryAccumulator(c.sourceClass(), c.targetClass(), c.dialectMask()));
                } else {
                    acc.mergeMask(c.dialectMask());
                }
            }

            // Accumulate field mappings
            for (ParsedFieldMapping f : result.fields()) {
                String key = f.ownerClass() + "." + f.sourceFieldName() + ":" + f.sourceDescriptor() + "->" + f.targetFieldName();
                ParsedFieldEntryAccumulator acc = fieldMap.get(key);
                if (acc == null) {
                    fieldMap.put(key, new ParsedFieldEntryAccumulator(
                            f.ownerClass(),
                            f.sourceFieldName(),
                            f.sourceDescriptor(),
                            f.targetFieldName(),
                            f.dialectMask()
                    ));
                } else {
                    acc.mergeMask(f.dialectMask());
                }
            }

            // Accumulate method mappings
            for (ParsedMethodMapping m : result.methods()) {
                String key = m.ownerClass() + "." + m.sourceMethodName() + m.sourceDescriptor() + "->" + m.targetMethodName() + m.targetDescriptor();
                ParsedMethodEntryAccumulator acc = methodMap.get(key);
                if (acc == null) {
                    methodMap.put(key, new ParsedMethodEntryAccumulator(
                            m.ownerClass(),
                            m.sourceMethodName(),
                            m.sourceDescriptor(),
                            m.targetMethodName(),
                            m.targetDescriptor(),
                            m.dialectMask()
                    ));
                } else {
                    acc.mergeMask(m.dialectMask());
                }
            }
        }

        // Convert and sort classes
        List<VstmClassTransmutationEntry> finalClasses = new ArrayList<>(classMap.size());
        for (ParsedClassEntryAccumulator acc : classMap.values()) {
            finalClasses.add(VstmClassTransmutationEntry.of(acc.sourceClass(), acc.targetClass(), acc.dialectMask()));
        }
        Collections.sort(finalClasses);

        // Convert and sort fields
        List<VstmFieldTransmutationEntry> finalFields = new ArrayList<>(fieldMap.size());
        for (ParsedFieldEntryAccumulator acc : fieldMap.values()) {
            finalFields.add(VstmFieldTransmutationEntry.of(
                    acc.ownerClass(),
                    acc.sourceFieldName(),
                    acc.sourceDescriptor(),
                    acc.targetFieldName(),
                    acc.dialectMask()
            ));
        }
        Collections.sort(finalFields);

        // Convert and sort methods
        List<VstmMethodTransmutationEntry> finalMethods = new ArrayList<>(methodMap.size());
        for (ParsedMethodEntryAccumulator acc : methodMap.values()) {
            finalMethods.add(VstmMethodTransmutationEntry.of(
                    acc.ownerClass(),
                    acc.sourceMethodName(),
                    acc.sourceDescriptor(),
                    acc.targetMethodName(),
                    acc.targetDescriptor(),
                    acc.dialectMask()
            ));
        }
        Collections.sort(finalMethods);

        return new HarmonizedTransmutationMatrix(finalClasses, finalFields, finalMethods, combinedMask);
    }

    /**
     * Parses and harmonizes all four major upstream dialect sources in a single pass.
     *
     * @param mojangPath   path to Mojang client or server ProGuard mappings
     * @param fabricPath   path to Fabric Yarn Tiny v2 mappings
     * @param neoForgePath path to NeoForge TSRG v2 mappings
     * @param spigotPath   path to Spigot CSRG mappings
     * @return unified harmonized transmutation matrix
     * @throws IOException             if reading any upstream file fails
     * @throws UpstreamParserException if any file contains malformed syntax
     */
    public HarmonizedTransmutationMatrix harmonizeAll(
            Path mojangPath,
            Path fabricPath,
            Path neoForgePath,
            Path spigotPath
    ) throws IOException {
        List<ParsedDialectResult> results = new ArrayList<>(4);

        if (mojangPath != null) {
            results.add(mojangParser.parse(mojangPath, VstmDialectMask.MOJMAP));
        }
        if (fabricPath != null) {
            results.add(fabricParser.parse(fabricPath, VstmDialectMask.YARN));
        }
        if (neoForgePath != null) {
            results.add(neoForgeParser.parse(neoForgePath, VstmDialectMask.SRG));
        }
        if (spigotPath != null) {
            results.add(spigotParser.parse(spigotPath, VstmDialectMask.SPIGOT_NMS));
        }

        return harmonize(results);
    }

    public MojangProGuardParser getMojangParser() {
        return mojangParser;
    }

    public FabricTinyV2Parser getFabricParser() {
        return fabricParser;
    }

    public NeoForgeTsrg2Parser getNeoForgeParser() {
        return neoForgeParser;
    }

    public SpigotCsrgParser getSpigotParser() {
        return spigotParser;
    }
}

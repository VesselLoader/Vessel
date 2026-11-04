package dev.vessel.kernel.mixin.remap;

import dev.vessel.kernel.vstm.engine.VstmTransmutationFastLookup;

import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;

/**
 * In-memory batch translator for Mixin refmap tokens utilizing VSTM fast symbol tables.
 *
 * <p>Translates Intermediary and SRG method/field names directly into official MojMap symbols
 * to prevent Mixin injection misses on the VESSEL Sovereign Server runtime.</p>
 *
 * <p>Strictly adheres to Rule 17: Single Responsibility, zero inner, nested, or anonymous classes.</p>
 *
 * @version 1.0.1
 * @since 1.0.0
 */
public final class RefmapVstmBatchTranslator {

    private final RefmapMethodDescriptorScanner scanner;
    private final Map<String, String> translationOverrides;
    private volatile VstmTransmutationFastLookup fastLookup;

    /**
     * Constructs a batch translator.
     *
     * @param scanner token scanner
     */
    public RefmapVstmBatchTranslator(RefmapMethodDescriptorScanner scanner) {
        this.scanner = Objects.requireNonNull(scanner, "scanner cannot be null");
        this.translationOverrides = new ConcurrentHashMap<>();
        this.fastLookup = null;
    }

    /**
     * Sets optional VSTM off-heap memory lookup engine.
     *
     * @param fastLookup VSTM fast lookup instance
     */
    public void setFastLookup(VstmTransmutationFastLookup fastLookup) {
        this.fastLookup = fastLookup;
    }

    /**
     * Registers a known method or field translation override.
     *
     * @param sourceSymbol obfuscated symbol (e.g. {@code "m_46700_"} or {@code "method_5678"})
     * @param targetSymbol official MojMap symbol
     */
    public void registerOverride(String sourceSymbol, String targetSymbol) {
        Objects.requireNonNull(sourceSymbol, "sourceSymbol cannot be null");
        Objects.requireNonNull(targetSymbol, "targetSymbol cannot be null");
        translationOverrides.put(sourceSymbol, targetSymbol);
    }

    /**
     * Translates a raw refmap signature string into official MojMap.
     *
     * @param rawSignature signature string (e.g. {@code "Lnet/minecraft/world/level/Level;m_46700_()V"})
     * @return translated signature string
     */
    public String translateSignature(String rawSignature) {
        if (rawSignature == null || rawSignature.isEmpty()) {
            return "";
        }

        List<String> tokens = scanner.parseSignatureTokens(rawSignature);
        if (tokens.size() < 3) {
            return rawSignature;
        }

        String owner = tokens.get(0);
        String name = tokens.get(1);
        String desc = tokens.get(2);

        String remappedName = translationOverrides.getOrDefault(name, name);
        if (remappedName.equals(name) && fastLookup != null) {
            String vstmLookup = fastLookup.lookupMethod(owner, name, desc, 0);
            if (vstmLookup != null && !vstmLookup.isEmpty()) {
                remappedName = vstmLookup;
            }
        }

        return "L" + owner + ";" + remappedName + desc;
    }
}

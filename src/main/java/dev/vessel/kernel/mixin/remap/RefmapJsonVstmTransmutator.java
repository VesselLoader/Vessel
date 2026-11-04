package dev.vessel.kernel.mixin.remap;

import java.nio.file.Path;
import java.util.Objects;
import java.util.Optional;

/**
 * Top-level coordinator for translating Mixin refmaps from legacy/SRG/Intermediary to MojMap.
 *
 * <p>Strictly adheres to Rule 17: Single Responsibility, zero inner, nested, or anonymous classes.</p>
 *
 * @version 1.0.0
 * @since 1.0.0
 */
public final class RefmapJsonVstmTransmutator {

    private final RefmapVstmBatchTranslator batchTranslator;
    private final RefmapDiskCacheCoordinator cacheCoordinator;

    /**
     * Constructs a refmap transmutator.
     *
     * @param batchTranslator  translator
     * @param cacheCoordinator disk cache coordinator
     */
    public RefmapJsonVstmTransmutator(
            RefmapVstmBatchTranslator batchTranslator,
            RefmapDiskCacheCoordinator cacheCoordinator
    ) {
        this.batchTranslator = Objects.requireNonNull(batchTranslator, "batchTranslator cannot be null");
        this.cacheCoordinator = Objects.requireNonNull(cacheCoordinator, "cacheCoordinator cannot be null");
    }

    /**
     * Resolves the remapped refmap JSON for a given mod jar and raw JSON content.
     *
     * @param modSha1Hash SHA-1 hash of the mod JAR
     * @param rawJson     original refmap JSON text
     * @return remapped refmap JSON text
     */
    public String transmuteRefmap(String modSha1Hash, String rawJson) {
        Objects.requireNonNull(modSha1Hash, "modSha1Hash cannot be null");
        if (rawJson == null || rawJson.isEmpty()) {
            return "{}";
        }

        // 1. Check disk cache
        Optional<String> cached = cacheCoordinator.readCachedRefmap(modSha1Hash);
        if (cached.isPresent()) {
            return cached.get();
        }

        // 2. Perform transmutation
        String transmuted = rawJson;
        // Basic line-by-line token replacement for refmap targets
        StringBuilder sb = new StringBuilder(rawJson.length());
        String[] lines = rawJson.split("\n");
        for (String line : lines) {
            int targetIdx = line.indexOf("\"Lnet/minecraft/");
            if (targetIdx != -1) {
                int endQuote = line.indexOf('"', targetIdx + 1);
                if (endQuote != -1) {
                    String targetSig = line.substring(targetIdx + 1, endQuote);
                    String remappedSig = batchTranslator.translateSignature(targetSig);
                    line = line.substring(0, targetIdx + 1) + remappedSig + line.substring(endQuote);
                }
            }
            sb.append(line).append("\n");
        }
        transmuted = sb.toString();

        // 3. Write back to disk cache
        cacheCoordinator.writeCachedRefmap(modSha1Hash, transmuted);

        return transmuted;
    }
}

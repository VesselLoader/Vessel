package dev.vessel.kernel.asset.stream;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * High-speed dual-generation compiler and resource pack builder.
 * Generates both legacy CustomModelData (1.20.1 - 1.20.4) overrides and modern
 * minecraft:item_model (1.20.5+) definitions from a unified canonical source.
 * Extinguishes AST-MOD by unifying multi-version client model compatibility.
 *
 * @version 1.0.0
 * @author VESSEL Engine Team
 */
public final class ClientAssetDualGenerationPackBuilder {

    public static final String PACK_MCMETA_PATH = "pack.mcmeta";
    public static final String DEFAULT_DESCRIPTION = "VESSEL Sovereign Dual-Generation Resource Pack";

    private final List<ItemModelSourceDefinition> definitions = new ArrayList<>();
    private final LegacyCustomModelDataCompiler legacyCompiler;
    private final ModernItemModelDefinitionCompiler modernCompiler;
    private final PackMetaManifestBuilder manifestBuilder;

    /**
     * Constructs the dual pack builder with internal compilers.
     */
    public ClientAssetDualGenerationPackBuilder() {
        this.legacyCompiler = new LegacyCustomModelDataCompiler();
        this.modernCompiler = new ModernItemModelDefinitionCompiler();
        this.manifestBuilder = new PackMetaManifestBuilder();
    }

    /**
     * Registers a universal item model definition into the builder queue.
     *
     * @param definition canonical item model definition
     * @return this builder for fluent chaining
     */
    public ClientAssetDualGenerationPackBuilder addModelDefinition(ItemModelSourceDefinition definition) {
        Objects.requireNonNull(definition, "definition cannot be null");
        this.definitions.add(definition);
        return this;
    }

    /**
     * Registers multiple item model definitions.
     *
     * @param defs list of canonical definitions
     * @return this builder
     */
    public ClientAssetDualGenerationPackBuilder addAllModelDefinitions(List<ItemModelSourceDefinition> defs) {
        Objects.requireNonNull(defs, "defs cannot be null");
        this.definitions.addAll(defs);
        return this;
    }

    /**
     * Compiles all queued definitions into a DualGenerationPackArtifact containing
     * both legacy and modern file trees alongside a combined unified tree.
     *
     * @param description pack description string
     * @return compiled artifact
     */
    public DualGenerationPackArtifact compileDualPack(String description) {
        String desc = description != null && !description.isBlank() ? description : DEFAULT_DESCRIPTION;

        List<ItemModelSourceDefinition> snapshot = Collections.unmodifiableList(new ArrayList<>(definitions));
        Map<String, String> legacyFiles = legacyCompiler.compileLegacyPack(snapshot);
        Map<String, String> modernFiles = modernCompiler.compileModernPack(snapshot);

        Map<String, String> combinedFiles = new HashMap<>();
        combinedFiles.putAll(legacyFiles);
        combinedFiles.putAll(modernFiles);

        String metaJson = manifestBuilder.buildPackMetaJson(TargetPackFormat.UNIVERSAL_DUAL, desc);
        combinedFiles.put(PACK_MCMETA_PATH, metaJson);

        return new DualGenerationPackArtifact(legacyFiles, modernFiles, combinedFiles);
    }

    /**
     * Compiles and packages the combined dual pack into an ultra-compressed ZIP archive.
     *
     * @param description pack description
     * @param compressor lossless compressor instance
     * @return byte array of compressed ZIP file
     * @throws IOException if zip generation fails
     */
    public byte[] compileAndPackageZip(String description, ZstdLosslessPackCompressor compressor) throws IOException {
        Objects.requireNonNull(compressor, "compressor cannot be null");

        DualGenerationPackArtifact artifact = compileDualPack(description);
        Map<String, byte[]> binaryTree = new HashMap<>();

        for (Map.Entry<String, String> entry : artifact.combinedFiles().entrySet()) {
            binaryTree.put(entry.getKey(), entry.getValue().getBytes(StandardCharsets.UTF_8));
        }

        return compressor.compressResourcePackZip(binaryTree);
    }

    /**
     * Compiles, packages, and mounts the resource pack directly into the HTTP Range streaming server.
     *
     * @param description pack description
     * @param mountUriPath virtual URI path (e.g. "/assets/pack.zip")
     * @param streamServer HTTP streaming server instance
     * @param compressor lossless pack compressor
     * @return mounted StreamableAssetResource metadata
     * @throws IOException if packaging fails
     */
    public StreamableAssetResource compileAndMount(
            String description,
            String mountUriPath,
            HttpRangeChunkStreamServer streamServer,
            ZstdLosslessPackCompressor compressor
    ) throws IOException {
        Objects.requireNonNull(mountUriPath, "mountUriPath cannot be null");
        Objects.requireNonNull(streamServer, "streamServer cannot be null");
        Objects.requireNonNull(compressor, "compressor cannot be null");

        byte[] zipBytes = compileAndPackageZip(description, compressor);
        String sha1 = ZstdLosslessPackCompressor.computeSha1Hex(zipBytes);

        StreamableAssetResource resource = new StreamableAssetResource(
                mountUriPath,
                "application/zip",
                zipBytes,
                sha1,
                System.currentTimeMillis()
        );

        streamServer.registerAsset(resource);
        return resource;
    }

    /**
     * Returns the current count of queued model definitions.
     *
     * @return queued count
     */
    public int queuedDefinitionCount() {
        return definitions.size();
    }

    /**
     * Clears all queued definitions from the builder.
     */
    public void clear() {
        definitions.clear();
    }
}

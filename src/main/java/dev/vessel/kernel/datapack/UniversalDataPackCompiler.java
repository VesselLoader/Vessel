package dev.vessel.kernel.datapack;

import dev.vessel.kernel.vfs.InMemoryVirtualFileSystemMount;
import dev.vessel.kernel.vfs.VirtualFileSystemMount;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * In-memory universal datapack compilation engine for the Startup Build Stage (Decision 1, Decision 19).
 * <p>
 * Consolidates datapack resources across Fabric, Forge, NeoForge, and Bukkit plugins
 * into an in-memory Virtual File System ({@link InMemoryVirtualFileSystemMount}).
 * Resolves tag namespace conflicts ({@code #c:}, {@code #forge:}, {@code #minecraft:})
 * via {@link ConventionalTagHarmonizer}, synthesizes canonical {@code pack.mcmeta},
 * and harmonizes recipes and loot tables before world loading.
 *
 * @version 1.0.1
 * @author VESSEL Engine Team
 */
public final class UniversalDataPackCompiler {

    private static final String DEFAULT_MOUNT_POINT = "/datapacks/vessel-universal";

    private final ConventionalTagHarmonizer tagHarmonizer;
    private final TagRegistryIndex tagRegistryIndex;

    // Parsed tags: normalizedTagPath -> DatapackTagDefinition
    private final Map<String, DatapackTagDefinition> aggregatedTags;
    // Recipes: normalizedRecipePath -> RecipeData
    private final Map<String, byte[]> aggregatedRecipes;
    // Loot tables: normalizedLootTablePath -> LootTableData
    private final Map<String, byte[]> aggregatedLootTables;
    // Other miscellaneous data resources (advancements, functions, damage types, etc.)
    private final Map<String, byte[]> generalDataResources;

    /**
     * Constructs a universal datapack compiler with default tag harmonizer and index.
     */
    public UniversalDataPackCompiler() {
        this(new ConventionalTagHarmonizer(), new TagRegistryIndex());
    }

    /**
     * Constructs a universal datapack compiler with specified harmonizer and index.
     *
     * @param tagHarmonizer    conventional tag harmonizer
     * @param tagRegistryIndex tag registry index
     */
    public UniversalDataPackCompiler(
            ConventionalTagHarmonizer tagHarmonizer,
            TagRegistryIndex tagRegistryIndex) {
        this.tagHarmonizer = Objects.requireNonNull(tagHarmonizer, "tagHarmonizer cannot be null");
        this.tagRegistryIndex = Objects.requireNonNull(tagRegistryIndex, "tagRegistryIndex cannot be null");

        this.aggregatedTags = new ConcurrentHashMap<>();
        this.aggregatedRecipes = new ConcurrentHashMap<>();
        this.aggregatedLootTables = new ConcurrentHashMap<>();
        this.generalDataResources = new ConcurrentHashMap<>();
    }

    /**
     * Ingests a raw resource file from an active loader module.
     *
     * @param loader       source loader identifier ("fabric", "forge", "neoforge", "bukkit")
     * @param modId        source module identifier
     * @param relativePath path inside module jar (e.g. "data/c/tags/items/iron_ingots.json")
     * @param content      raw file content
     */
    public void addSourceResource(String loader, String modId, String relativePath, byte[] content) {
        Objects.requireNonNull(relativePath, "relativePath cannot be null");
        Objects.requireNonNull(content, "content cannot be null");

        String normPath = normalizePath(relativePath);
        if (!normPath.startsWith("data/")) {
            return; // Datapack compiler only processes resources inside data/
        }

        if (normPath.contains("/tags/")) {
            ingestTagResource(normPath, content);
        } else if (normPath.contains("/recipes/")) {
            aggregatedRecipes.put(normPath, content.clone());
        } else if (normPath.contains("/loot_tables/")) {
            aggregatedLootTables.put(normPath, content.clone());
        } else {
            generalDataResources.put(normPath, content.clone());
        }
    }

    /**
     * Recursively traverses and ingests all datapack resources from a virtual file system mount.
     *
     * @param loader   source loader
     * @param modId    module id
     * @param vfsMount mounted VFS container
     * @throws IOException if resource reading fails
     */
    public void addSourceModuleVfs(String loader, String modId, VirtualFileSystemMount vfsMount) throws IOException {
        Objects.requireNonNull(vfsMount, "vfsMount cannot be null");
        scanAndIngestVfsDirectory(loader, modId, vfsMount, "data");
    }

    private void scanAndIngestVfsDirectory(
            String loader,
            String modId,
            VirtualFileSystemMount mount,
            String currentDir) throws IOException {
        Set<String> entries = mount.listResources(currentDir);
        for (String entry : entries) {
            String path = currentDir.isEmpty() ? entry : currentDir + "/" + entry;
            Optional<byte[]> res = mount.readResource(path);
            if (res.isPresent()) {
                addSourceResource(loader, modId, path, res.get());
            } else {
                // Directory: descend
                scanAndIngestVfsDirectory(loader, modId, mount, path);
            }
        }
    }

    /**
     * Ingests and merges a tag JSON file using the dedicated {@link DatapackTagJsonParser}.
     */
    private void ingestTagResource(String normPath, byte[] content) {
        DatapackTagDefinition parsed = DatapackTagJsonParser.parseTag(content);
        DatapackTagDefinition existing = aggregatedTags.computeIfAbsent(normPath, k -> new DatapackTagDefinition());
        synchronized (existing) {
            if (parsed.isReplace()) {
                existing.setReplace(true);
                existing.clearValues();
            }
            existing.addAllValues(parsed.getValues());
        }
    }

    /**
     * Compiles all aggregated datapack assets into an in-memory VFS mount.
     *
     * @return compiled virtual filesystem mount containing pack.mcmeta and harmonized data/
     */
    public InMemoryVirtualFileSystemMount compile() {
        return compile(DEFAULT_MOUNT_POINT);
    }

    /**
     * Compiles all aggregated datapack assets into a specified mount point.
     *
     * @param mountPoint mount point prefix (e.g. "/datapacks/vessel-universal")
     * @return compiled virtual filesystem mount
     */
    public InMemoryVirtualFileSystemMount compile(String mountPoint) {
        Objects.requireNonNull(mountPoint, "mountPoint cannot be null");
        InMemoryVirtualFileSystemMount mount = new InMemoryVirtualFileSystemMount(mountPoint, false);

        // 1. Synthesize pack.mcmeta
        byte[] manifest = DatapackMetadataGenerator.generateMetadataBytes(
                DatapackMetadataGenerator.DEFAULT_PACK_FORMAT,
                DatapackMetadataGenerator.DEFAULT_DESCRIPTION);
        mount.putResource("pack.mcmeta", manifest);

        // 2. Aggregate equivalence groups to unify cross-loader tag values
        Map<String, Set<String>> canonicalToValues = new HashMap<>();
        Map<String, String> canonicalToType = new HashMap<>();

        for (Map.Entry<String, DatapackTagDefinition> entry : aggregatedTags.entrySet()) {
            String path = entry.getKey();
            DatapackTagDefinition def = entry.getValue();
            String tagId = extractTagIdentifier(path);
            String tagType = extractTagType(path);
            if (tagId != null) {
                String canonical = tagHarmonizer.getCanonicalTag(tagId);
                canonicalToValues.computeIfAbsent(canonical, k -> new LinkedHashSet<>()).addAll(def.getValues());
                canonicalToType.putIfAbsent(canonical, tagType);
            }
        }

        // 3. Write compiled tags to VFS with unified cross-loader member sets
        Set<String> writtenPaths = new HashSet<>();

        for (Map.Entry<String, DatapackTagDefinition> entry : aggregatedTags.entrySet()) {
            String path = entry.getKey();
            DatapackTagDefinition data = entry.getValue();
            String tagId = extractTagIdentifier(path);

            Set<String> effectiveValues = new LinkedHashSet<>(data.getValues());
            if (tagId != null) {
                String canonical = tagHarmonizer.getCanonicalTag(tagId);
                Set<String> groupValues = canonicalToValues.get(canonical);
                if (groupValues != null) {
                    effectiveValues.addAll(groupValues);
                }
            }

            String tagJson = serializeTagJson(data.isReplace(), effectiveValues);
            mount.putResource(path, tagJson.getBytes(StandardCharsets.UTF_8));
            writtenPaths.add(path);
        }

        // 4. Synthesize missing cross-loader conventional tag alias files
        for (Map.Entry<String, Set<String>> groupEntry : canonicalToValues.entrySet()) {
            String canonical = groupEntry.getKey();
            Set<String> values = groupEntry.getValue();
            String tagType = canonicalToType.getOrDefault(canonical, "items");

            Set<String> equivalentTags = tagHarmonizer.getEquivalentTags(canonical);
            for (String eqTag : equivalentTags) {
                String aliasPath = buildTagPath(eqTag, tagType);
                if (aliasPath != null && !writtenPaths.contains(aliasPath)) {
                    String aliasJson = serializeTagJson(false, values);
                    mount.putResource(aliasPath, aliasJson.getBytes(StandardCharsets.UTF_8));
                    writtenPaths.add(aliasPath);
                }
            }
        }

        // 5. Compile recipes
        for (Map.Entry<String, byte[]> entry : aggregatedRecipes.entrySet()) {
            mount.putResource(entry.getKey(), entry.getValue());
        }

        // 6. Compile loot tables
        for (Map.Entry<String, byte[]> entry : aggregatedLootTables.entrySet()) {
            mount.putResource(entry.getKey(), entry.getValue());
        }

        // 7. Compile general data resources
        for (Map.Entry<String, byte[]> entry : generalDataResources.entrySet()) {
            mount.putResource(entry.getKey(), entry.getValue());
        }

        return mount;
    }

    private static String serializeTagJson(boolean replace, Collection<String> values) {
        StringBuilder sb = new StringBuilder();
        sb.append("{\n");
        sb.append("  \"replace\": ").append(replace).append(",\n");
        sb.append("  \"values\": [\n");
        int count = 0;
        int size = values.size();
        for (String val : values) {
            count++;
            sb.append("    \"").append(val).append("\"");
            if (count < size) {
                sb.append(",");
            }
            sb.append("\n");
        }
        sb.append("  ]\n");
        sb.append("}\n");
        return sb.toString();
    }

    private static String extractTagIdentifier(String path) {
        if (!path.startsWith("data/") || !path.contains("/tags/")) {
            return null;
        }
        String[] parts = path.split("/");
        if (parts.length < 5) {
            return null;
        }
        String namespace = parts[1];
        int tagsIdx = -1;
        for (int i = 0; i < parts.length; i++) {
            if ("tags".equals(parts[i])) {
                tagsIdx = i;
                break;
            }
        }
        if (tagsIdx == -1 || tagsIdx + 2 >= parts.length) {
            return null;
        }

        StringBuilder nameBuilder = new StringBuilder();
        for (int i = tagsIdx + 2; i < parts.length; i++) {
            if (i > tagsIdx + 2) {
                nameBuilder.append("/");
            }
            nameBuilder.append(parts[i]);
        }
        String tagName = nameBuilder.toString();
        if (tagName.endsWith(".json")) {
            tagName = tagName.substring(0, tagName.length() - 5);
        }
        return namespace + ":" + tagName;
    }

    private static String extractTagType(String path) {
        String[] parts = path.split("/");
        for (int i = 0; i < parts.length - 1; i++) {
            if ("tags".equals(parts[i])) {
                return parts[i + 1];
            }
        }
        return "items";
    }

    private static String buildTagPath(String fullTagId, String tagType) {
        int colonIdx = fullTagId.indexOf(':');
        if (colonIdx == -1) {
            return null;
        }
        String namespace = fullTagId.substring(0, colonIdx);
        String name = fullTagId.substring(colonIdx + 1);
        return "data/" + namespace + "/tags/" + tagType + "/" + name + ".json";
    }

    private static String normalizePath(String path) {
        String p = path.replace('\\', '/').trim();
        while (p.startsWith("/")) {
            p = p.substring(1);
        }
        return p;
    }

    /**
     * Total number of distinct tag files compiled.
     *
     * @return count
     */
    public int getCompiledTagCount() {
        return aggregatedTags.size();
    }

    /**
     * Total number of recipes compiled.
     *
     * @return count
     */
    public int getCompiledRecipeCount() {
        return aggregatedRecipes.size();
    }

    /**
     * Total number of loot tables compiled.
     *
     * @return count
     */
    public int getCompiledLootTableCount() {
        return aggregatedLootTables.size();
    }

    /**
     * Total number of all datapack files compiled.
     *
     * @return count
     */
    public int getTotalCompiledResourceCount() {
        return aggregatedTags.size() + aggregatedRecipes.size() + aggregatedLootTables.size() + generalDataResources.size();
    }

    /**
     * Returns the associated conventional tag harmonizer.
     *
     * @return harmonizer instance
     */
    public ConventionalTagHarmonizer getTagHarmonizer() {
        return tagHarmonizer;
    }

    /**
     * Returns the associated tag registry index.
     *
     * @return tag index instance
     */
    public TagRegistryIndex getTagRegistryIndex() {
        return tagRegistryIndex;
    }
}

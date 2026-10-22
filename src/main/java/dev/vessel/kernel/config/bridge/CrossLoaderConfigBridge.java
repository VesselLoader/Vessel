package dev.vessel.kernel.config.bridge;

import dev.vessel.kernel.config.HierarchicalConfigNode;
import dev.vessel.kernel.identity.SourceLoaderKind;

import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;

/**
 * High-performance configuration translation bridge across Forge NightConfig (TOML),
 * Fabric/Quilt Config (JSON5), and Spigot/Bukkit (YAML).
 * <p>
 * Provides single-cycle hash lookup to map conventional settings (view distance, entity activation,
 * network tick buffers, thread pool limits) between loader-specific naming conventions without data loss.
 * </p>
 *
 * @version 1.0.0
 * @author VESSEL Engine Team
 */
public final class CrossLoaderConfigBridge {

    private final Map<String, ConfigKeyMapping> canonicalIndex = new ConcurrentHashMap<>();
    private final Map<String, ConfigKeyMapping> forgeIndex = new ConcurrentHashMap<>();
    private final Map<String, ConfigKeyMapping> fabricIndex = new ConcurrentHashMap<>();
    private final Map<String, ConfigKeyMapping> bukkitIndex = new ConcurrentHashMap<>();

    /**
     * Constructs a CrossLoaderConfigBridge pre-populated with standard conventions.
     */
    public CrossLoaderConfigBridge() {
        registerDefaultConventions();
    }

    /**
     * Registers a cross-loader configuration key mapping.
     *
     * @param mapping key mapping descriptor
     */
    public void registerMapping(ConfigKeyMapping mapping) {
        Objects.requireNonNull(mapping, "mapping cannot be null");
        canonicalIndex.put(mapping.canonicalPath(), mapping);

        if (mapping.forgePath() != null) {
            forgeIndex.put(mapping.forgePath(), mapping);
        }
        if (mapping.fabricPath() != null) {
            fabricIndex.put(mapping.fabricPath(), mapping);
        }
        if (mapping.bukkitPath() != null) {
            bukkitIndex.put(mapping.bukkitPath(), mapping);
        }
    }

    /**
     * Maps a key from a source loader convention to a destination loader convention.
     *
     * @param fromLoader source loader format
     * @param key        source key path
     * @param toLoader   target loader format
     * @return translated key path or original key if no mapping exists
     */
    public String mapKey(SourceLoaderKind fromLoader, String key, SourceLoaderKind toLoader) {
        if (key == null || fromLoader == null || toLoader == null || fromLoader == toLoader) {
            return key;
        }

        ConfigKeyMapping mapping = findMapping(fromLoader, key);
        if (mapping != null) {
            return mapping.getPathForLoader(toLoader);
        }
        return key;
    }

    /**
     * Resolves the canonical VESSEL key for any loader-specific key path.
     *
     * @param loader source loader
     * @param key    loader-specific key path
     * @return canonical key path
     */
    public String resolveCanonicalKey(SourceLoaderKind loader, String key) {
        if (key == null || loader == null) {
            return key;
        }
        ConfigKeyMapping mapping = findMapping(loader, key);
        return (mapping != null) ? mapping.canonicalPath() : key;
    }

    /**
     * Translates an entire configuration tree from one loader format into another.
     *
     * @param sourceRoot source configuration root
     * @param fromLoader source loader
     * @param toLoader   target loader
     * @return newly translated hierarchical configuration tree
     */
    public HierarchicalConfigNode translateTree(
            HierarchicalConfigNode sourceRoot,
            SourceLoaderKind fromLoader,
            SourceLoaderKind toLoader
    ) {
        Objects.requireNonNull(sourceRoot, "sourceRoot cannot be null");
        Objects.requireNonNull(fromLoader, "fromLoader cannot be null");
        Objects.requireNonNull(toLoader, "toLoader cannot be null");

        HierarchicalConfigNode targetRoot = new HierarchicalConfigNode("");
        translateRecursive(sourceRoot, targetRoot, "", fromLoader, toLoader);
        return targetRoot;
    }

    /**
     * Synchronizes canonical configuration values into a target loader tree.
     *
     * @param canonicalRoot source canonical tree
     * @param targetRoot    target loader tree to update
     * @param targetLoader  target loader format
     */
    public void synchronize(
            HierarchicalConfigNode canonicalRoot,
            HierarchicalConfigNode targetRoot,
            SourceLoaderKind targetLoader
    ) {
        Objects.requireNonNull(canonicalRoot, "canonicalRoot cannot be null");
        Objects.requireNonNull(targetRoot, "targetRoot cannot be null");
        Objects.requireNonNull(targetLoader, "targetLoader cannot be null");

        for (ConfigKeyMapping mapping : canonicalIndex.values()) {
            HierarchicalConfigNode sourceNode = canonicalRoot.getNode(mapping.canonicalPath());
            if (sourceNode != null && sourceNode.getValue() != null) {
                String targetPath = mapping.getPathForLoader(targetLoader);
                HierarchicalConfigNode destNode = targetRoot.getOrCreateNode(targetPath);
                destNode.setValue(sourceNode.getValue());
                if (sourceNode.getComment() != null) {
                    destNode.setComment(sourceNode.getComment());
                }
            }
        }
    }

    private void translateRecursive(
            HierarchicalConfigNode source,
            HierarchicalConfigNode targetRoot,
            String currentPath,
            SourceLoaderKind fromLoader,
            SourceLoaderKind toLoader
    ) {
        for (Map.Entry<String, HierarchicalConfigNode> entry : source.getChildren().entrySet()) {
            String key = entry.getKey();
            HierarchicalConfigNode child = entry.getValue();
            String fullPath = currentPath.isEmpty() ? key : currentPath + "." + key;

            if (child.getChildren().isEmpty()) {
                // Leaf node
                String targetPath = mapKey(fromLoader, fullPath, toLoader);
                HierarchicalConfigNode destNode = targetRoot.getOrCreateNode(targetPath);
                destNode.setValue(child.getValue());
                if (child.getComment() != null) {
                    destNode.setComment(child.getComment());
                }
            } else {
                translateRecursive(child, targetRoot, fullPath, fromLoader, toLoader);
            }
        }
    }

    private ConfigKeyMapping findMapping(SourceLoaderKind loader, String key) {
        return switch (loader) {
            case FORGE, NEOFORGE -> forgeIndex.get(key);
            case FABRIC, QUILT -> fabricIndex.get(key);
            case BUKKIT, SPIGOT, PAPER, FOLIA -> bukkitIndex.get(key);
            case VESSEL_NATIVE -> canonicalIndex.get(key);
        };
    }

    private void registerDefaultConventions() {
        registerMapping(new ConfigKeyMapping(
                "server.port",
                "server.listen_port",
                "serverPort",
                "server-port",
                25565
        ));
        registerMapping(new ConfigKeyMapping(
                "world.view_distance",
                "world.view_distance",
                "viewDistance",
                "view-distance",
                10
        ));
        registerMapping(new ConfigKeyMapping(
                "world.simulation_distance",
                "world.simulation_distance",
                "simulationDistance",
                "simulation-distance",
                8
        ));
        registerMapping(new ConfigKeyMapping(
                "performance.async_chunks",
                "performance.async_chunk_gen",
                "asyncChunkLoading",
                "async-chunks",
                true
        ));
        registerMapping(new ConfigKeyMapping(
                "performance.entity_activation_range",
                "entities.activation_range",
                "entityActivationRange",
                "entity-activation-range",
                32
        ));
        registerMapping(new ConfigKeyMapping(
                "network.max_players",
                "server.max_players",
                "maxPlayers",
                "max-players",
                20
        ));
        registerMapping(new ConfigKeyMapping(
                "world.difficulty",
                "world.game_difficulty",
                "difficulty",
                "difficulty",
                "normal"
        ));
    }
}

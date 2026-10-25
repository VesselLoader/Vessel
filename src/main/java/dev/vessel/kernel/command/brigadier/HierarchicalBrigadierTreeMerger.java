package dev.vessel.kernel.command.brigadier;

import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Sovereign hierarchical Brigadier tree merger that unifies command registries from Bukkit,
 * Forge, NeoForge, Fabric, and Minecraft Vanilla under Decision 38 and ECO-015.
 *
 * <p>Detects root command collisions (such as {@code /spawn}, {@code /home}, {@code /tpa}),
 * synthesizes namespaced aliases (e.g. {@code /essentials:spawn} vs {@code /mod:spawn}), and prevents
 * server crashes caused by conflicting root Brigadier literal registrations.</p>
 *
 * <p>Strictly adheres to Rule 17 with zero inner or nested classes.</p>
 *
 * @author VESSEL Engine Team
 * @version 1.0.0
 */
public final class HierarchicalBrigadierTreeMerger {

    private static final Logger LOGGER = LoggerFactory.getLogger(HierarchicalBrigadierTreeMerger.class);

    private static final String NAMESPACE_SEPARATOR = ":";
    private static final String COMMAND_PREFIX = "/";

    private final ReentrantReadWriteLock lock = new ReentrantReadWriteLock();
    private final Map<String, BrigadierCommandNode> rootNodes = new ConcurrentHashMap<>();
    private final Map<String, BrigadierCommandNode> namespacedNodes = new ConcurrentHashMap<>();
    private final Map<String, CommandRegistrationSource> nodeSources = new ConcurrentHashMap<>();

    private final AtomicInteger conflictsCounter = new AtomicInteger(0);
    private final AtomicInteger aliasCounter = new AtomicInteger(0);

    private final CommandConflictResolutionStrategy defaultStrategy;

    /**
     * Constructs a tree merger using {@link CommandConflictResolutionStrategy#PRIORITY_BASED}.
     */
    public HierarchicalBrigadierTreeMerger() {
        this(CommandConflictResolutionStrategy.PRIORITY_BASED);
    }

    /**
     * Constructs a tree merger with an explicit conflict resolution strategy.
     *
     * @param defaultStrategy the default resolution strategy for collisions
     */
    public HierarchicalBrigadierTreeMerger(CommandConflictResolutionStrategy defaultStrategy) {
        this.defaultStrategy = Objects.requireNonNull(defaultStrategy, "defaultStrategy cannot be null");
    }

    /**
     * Registers a Brigadier command node originating from a specified subsystem.
     *
     * @param source the originating subsystem and namespace metadata
     * @param node   the root command node to register
     * @return {@code true} if registration was processed successfully
     */
    public boolean registerCommand(CommandRegistrationSource source, BrigadierCommandNode node) {
        return registerCommand(source, node, defaultStrategy);
    }

    /**
     * Registers a Brigadier command node with an explicit conflict resolution strategy.
     *
     * @param source   originating subsystem metadata
     * @param node     root command node
     * @param strategy resolution strategy to apply in case of collision
     * @return {@code true} if registration succeeded
     */
    public boolean registerCommand(
            CommandRegistrationSource source,
            BrigadierCommandNode node,
            CommandConflictResolutionStrategy strategy
    ) {
        Objects.requireNonNull(source, "source cannot be null");
        Objects.requireNonNull(node, "node cannot be null");
        Objects.requireNonNull(strategy, "strategy cannot be null");

        String literal = node.getName().toLowerCase();
        String qualifiedName = source.namespace() + NAMESPACE_SEPARATOR + literal;

        lock.writeLock().lock();
        try {
            // Always map the namespaced variant: namespace:command
            namespacedNodes.put(qualifiedName, node);
            aliasCounter.incrementAndGet();

            for (String alias : node.getAliases()) {
                String namespacedAlias = source.namespace() + NAMESPACE_SEPARATOR + alias.toLowerCase();
                namespacedNodes.put(namespacedAlias, node);
                aliasCounter.incrementAndGet();
            }

            BrigadierCommandNode existingRoot = rootNodes.get(literal);
            if (existingRoot == null) {
                // Clean un-namespaced registration
                rootNodes.put(literal, node);
                nodeSources.put(literal, source);
                registerAliasesAsRoot(source, node);
                LOGGER.debug("Registered root command '{}' under namespace '{}'", literal, source.namespace());
                return true;
            }

            // Conflict detected
            conflictsCounter.incrementAndGet();
            CommandRegistrationSource existingSource = nodeSources.get(literal);
            LOGGER.warn("Brigadier root command conflict detected for '/{}' between [{} ({})] and [{} ({})]",
                    literal,
                    existingSource != null ? existingSource.subsystemName() : "UNKNOWN",
                    existingSource != null ? existingSource.namespace() : "unknown",
                    source.subsystemName(),
                    source.namespace());

            resolveConflict(literal, existingSource, existingRoot, source, node, strategy);
            return true;
        } finally {
            lock.writeLock().unlock();
        }
    }

    private void registerAliasesAsRoot(CommandRegistrationSource source, BrigadierCommandNode node) {
        for (String alias : node.getAliases()) {
            String lowerAlias = alias.toLowerCase();
            if (!rootNodes.containsKey(lowerAlias)) {
                rootNodes.put(lowerAlias, node);
                nodeSources.put(lowerAlias, source);
            }
        }
    }

    private void resolveConflict(
            String literal,
            CommandRegistrationSource existingSource,
            BrigadierCommandNode existingNode,
            CommandRegistrationSource newSource,
            BrigadierCommandNode newNode,
            CommandConflictResolutionStrategy strategy
    ) {
        switch (strategy) {
            case OVERWRITE -> {
                rootNodes.put(literal, newNode);
                nodeSources.put(literal, newSource);
                LOGGER.info("Resolved collision for '/{}' by OVERWRITING with namespace '{}'",
                        literal, newSource.namespace());
            }
            case PRESERVE_EXISTING -> {
                LOGGER.info("Resolved collision for '/{}' by PRESERVING existing namespace '{}'",
                        literal, existingSource != null ? existingSource.namespace() : "unknown");
            }
            case REQUIRE_NAMESPACE_ON_COLLISION -> {
                rootNodes.remove(literal);
                nodeSources.remove(literal);
                LOGGER.info("Resolved collision for '/{}' by stripping un-namespaced root to require explicit namespace",
                        literal);
            }
            case PRIORITY_BASED -> {
                int existingPriority = existingSource != null ? existingSource.priorityWeight() : 0;
                int newPriority = newSource.priorityWeight();

                if (newPriority > existingPriority) {
                    rootNodes.put(literal, newNode);
                    nodeSources.put(literal, newSource);
                    LOGGER.info("Resolved collision for '/{}': newcomer '{}' (priority {}) took precedence over '{}' (priority {})",
                            literal, newSource.namespace(), newPriority,
                            existingSource != null ? existingSource.namespace() : "unknown", existingPriority);
                } else {
                    LOGGER.info("Resolved collision for '/{}': existing '{}' (priority {}) retained precedence over '{}' (priority {})",
                            literal, existingSource != null ? existingSource.namespace() : "unknown", existingPriority,
                            newSource.namespace(), newPriority);
                }
            }
        }
    }

    /**
     * Resolves a command string (e.g. "/spawn", "/essentials:spawn") to its active Brigadier node.
     *
     * @param commandLine input command line including optional leading slash
     * @return optional containing the target node if found
     */
    public Optional<BrigadierCommandNode> resolveCommand(String commandLine) {
        if (commandLine == null || commandLine.isBlank()) {
            return Optional.empty();
        }

        String cleaned = commandLine.trim();
        if (cleaned.startsWith(COMMAND_PREFIX)) {
            cleaned = cleaned.substring(1);
        }

        int spaceIndex = cleaned.indexOf(' ');
        String commandLiteral = (spaceIndex > 0 ? cleaned.substring(0, spaceIndex) : cleaned).toLowerCase();

        lock.readLock().lock();
        try {
            if (commandLiteral.contains(NAMESPACE_SEPARATOR)) {
                return Optional.ofNullable(namespacedNodes.get(commandLiteral));
            }
            return Optional.ofNullable(rootNodes.get(commandLiteral));
        } finally {
            lock.readLock().unlock();
        }
    }

    /**
     * Returns an unmodifiable view of all active root command nodes.
     *
     * @return map of root nodes
     */
    public Map<String, BrigadierCommandNode> getRootNodes() {
        lock.readLock().lock();
        try {
            return Collections.unmodifiableMap(rootNodes);
        } finally {
            lock.readLock().unlock();
        }
    }

    /**
     * Returns an unmodifiable view of all namespaced command nodes.
     *
     * @return map of namespaced nodes
     */
    public Map<String, BrigadierCommandNode> getNamespacedNodes() {
        lock.readLock().lock();
        try {
            return Collections.unmodifiableMap(namespacedNodes);
        } finally {
            lock.readLock().unlock();
        }
    }

    /**
     * Summarizes current merger state into an immutable diagnostic record.
     *
     * @return snapshot of merge metrics
     */
    public BrigadierTreeMergeResult getMergeMetrics() {
        lock.readLock().lock();
        try {
            Set<String> literals = new LinkedHashSet<>(rootNodes.keySet());
            literals.addAll(namespacedNodes.keySet());
            return new BrigadierTreeMergeResult(
                    rootNodes.size(),
                    conflictsCounter.get(),
                    aliasCounter.get(),
                    literals,
                    true
            );
        } finally {
            lock.readLock().unlock();
        }
    }
}

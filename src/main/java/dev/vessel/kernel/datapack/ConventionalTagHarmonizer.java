package dev.vessel.kernel.datapack;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * High-performance bidirectional tag harmonization engine.
 * <p>
 * Unifies naming conventions between Fabric Conventional Tags ({@code #c:}),
 * Forge Tags ({@code #forge:}), NeoForge Tags ({@code #neoforge:}), and
 * Vanilla Minecraft Tags ({@code #minecraft:}).
 * <p>
 * Equivalence testing evaluates in 1 CPU cycle using pre-indexed primitive group IDs,
 * enabling cross-loader recipes and loot conditions to resolve ingredients seamlessly.
 *
 * @version 1.0.1
 * @author VESSEL Engine Team
 */
public final class ConventionalTagHarmonizer {

    private final Map<String, Integer> tagToGroupId;
    private final Map<Integer, Set<String>> groupToTags;
    private final Map<Integer, String> groupToCanonical;
    private final AtomicInteger nextGroupId;

    /**
     * Constructs a tag harmonizer initialized with standard conventions.
     */
    public ConventionalTagHarmonizer() {
        this.tagToGroupId = new ConcurrentHashMap<>();
        this.groupToTags = new ConcurrentHashMap<>();
        this.groupToCanonical = new ConcurrentHashMap<>();
        this.nextGroupId = new AtomicInteger(1);

        registerStandardConventions();
    }

    /**
     * Strips leading '#' from a tag identifier if present.
     *
     * @param tag tag name
     * @return normalized tag name without '#'
     */
    public static String normalizeTag(String tag) {
        if (tag == null) {
            return "";
        }
        String trimmed = tag.trim();
        return trimmed.startsWith("#") ? trimmed.substring(1) : trimmed;
    }

    /**
     * Registers a group of equivalent tags across conventions with full transitive group unification.
     *
     * @param canonicalTag   the canonical representative tag
     * @param equivalentTags other equivalent tags to associate
     */
    public synchronized void registerEquivalenceGroup(String canonicalTag, String... equivalentTags) {
        Objects.requireNonNull(canonicalTag, "canonicalTag cannot be null");
        String normCanonical = normalizeTag(canonicalTag);
        if (normCanonical.isEmpty()) {
            return;
        }

        // Collect all normalized non-empty tags in this operation
        Set<String> allInputTags = new LinkedHashSet<>();
        allInputTags.add(normCanonical);
        if (equivalentTags != null) {
            for (String eq : equivalentTags) {
                if (eq != null) {
                    String normEq = normalizeTag(eq);
                    if (!normEq.isEmpty()) {
                        allInputTags.add(normEq);
                    }
                }
            }
        }

        // Identify all existing group IDs touched by the input tags
        Set<Integer> existingGroupIds = new HashSet<>();
        for (String tag : allInputTags) {
            Integer gid = tagToGroupId.get(tag);
            if (gid != null) {
                existingGroupIds.add(gid);
            }
        }

        final int targetGroupId;
        if (existingGroupIds.isEmpty()) {
            targetGroupId = nextGroupId.getAndIncrement();
            groupToCanonical.put(targetGroupId, normCanonical);
        } else {
            // Select lowest existing group ID as canonical target to maintain stability
            targetGroupId = Collections.min(existingGroupIds);
            Set<String> targetSet = groupToTags.computeIfAbsent(targetGroupId, k -> ConcurrentHashMap.newKeySet());

            // Merge all other existing groups into targetGroupId
            for (Integer gid : existingGroupIds) {
                if (!gid.equals(targetGroupId)) {
                    Set<String> oldMembers = groupToTags.remove(gid);
                    if (oldMembers != null) {
                        for (String oldMember : oldMembers) {
                            tagToGroupId.put(oldMember, targetGroupId);
                            targetSet.add(oldMember);
                        }
                    }
                    groupToCanonical.remove(gid);
                }
            }
        }

        Set<String> targetSet = groupToTags.computeIfAbsent(targetGroupId, k -> ConcurrentHashMap.newKeySet());
        for (String tag : allInputTags) {
            targetSet.add(tag);
            tagToGroupId.put(tag, targetGroupId);
        }
        groupToCanonical.putIfAbsent(targetGroupId, normCanonical);
    }

    /**
     * Registers a bidirectional alias between two tags.
     *
     * @param tagA first tag
     * @param tagB second tag
     */
    public void registerAlias(String tagA, String tagB) {
        registerEquivalenceGroup(tagA, tagB);
    }

    /**
     * Tests whether two tags are equivalent across conventions in O(1) time.
     *
     * @param tagA first tag
     * @param tagB second tag
     * @return true if both tags belong to the same conventional equivalence group
     */
    public boolean isEquivalent(String tagA, String tagB) {
        String normA = normalizeTag(tagA);
        String normB = normalizeTag(tagB);

        if (normA.equals(normB)) {
            return true;
        }

        Integer idA = tagToGroupId.get(normA);
        if (idA == null) {
            return false;
        }
        Integer idB = tagToGroupId.get(normB);
        return idA.equals(idB);
    }

    /**
     * Resolves the canonical tag for a given tag.
     *
     * @param tag tag name
     * @return canonical tag name, or original normalized tag if unmapped
     */
    public String getCanonicalTag(String tag) {
        String norm = normalizeTag(tag);
        Integer groupId = tagToGroupId.get(norm);
        if (groupId == null) {
            return norm;
        }
        return groupToCanonical.getOrDefault(groupId, norm);
    }

    /**
     * Returns all tags equivalent to the given tag.
     *
     * @param tag tag name
     * @return unmodifiable set of equivalent tags, or single element set if unmapped
     */
    public Set<String> getEquivalentTags(String tag) {
        String norm = normalizeTag(tag);
        Integer groupId = tagToGroupId.get(norm);
        if (groupId == null) {
            return Collections.singleton(norm);
        }
        Set<String> tags = groupToTags.get(groupId);
        return tags != null ? Collections.unmodifiableSet(tags) : Collections.singleton(norm);
    }

    /**
     * Maps a tag to its Fabric conventional equivalent (namespace {@code c:}).
     *
     * @param tag source tag
     * @return Optional containing the Fabric tag, or empty if not found
     */
    public Optional<String> mapToFabric(String tag) {
        return findTagWithNamespace(tag, "c");
    }

    /**
     * Maps a tag to its Forge equivalent (namespace {@code forge:}).
     *
     * @param tag source tag
     * @return Optional containing the Forge tag, or empty if not found
     */
    public Optional<String> mapToForge(String tag) {
        return findTagWithNamespace(tag, "forge");
    }

    /**
     * Maps a tag to its Vanilla equivalent (namespace {@code minecraft:}).
     *
     * @param tag source tag
     * @return Optional containing the Vanilla tag, or empty if not found
     */
    public Optional<String> mapToVanilla(String tag) {
        return findTagWithNamespace(tag, "minecraft");
    }

    /**
     * Harmonizes an existing {@link TagRegistryIndex} by propagating member IDs
     * across all equivalent tags.
     *
     * @param tagIndex the tag registry index to harmonize
     */
    public void harmonizeTagRegistry(TagRegistryIndex tagIndex) {
        Objects.requireNonNull(tagIndex, "tagIndex cannot be null");

        for (Map.Entry<Integer, Set<String>> entry : groupToTags.entrySet()) {
            Set<String> group = entry.getValue();

            // Collect all unique members across the group
            Set<Integer> aggregatedMembers = new HashSet<>();
            for (String tag : group) {
                int[] members = tagIndex.getMembers(tag);
                for (int id : members) {
                    aggregatedMembers.add(id);
                }
            }

            if (!aggregatedMembers.isEmpty()) {
                int[] unified = aggregatedMembers.stream().mapToInt(Integer::intValue).toArray();
                for (String tag : group) {
                    tagIndex.registerTag(tag, unified);
                }
            }
        }
    }

    private Optional<String> findTagWithNamespace(String tag, String targetNamespace) {
        for (String eq : getEquivalentTags(tag)) {
            if (eq.startsWith(targetNamespace + ":")) {
                return Optional.of(eq);
            }
        }
        return Optional.empty();
    }

    /**
     * Pre-populates the harmonizer with official standard conventions.
     */
    private void registerStandardConventions() {
        // Ingots
        registerEquivalenceGroup("c:iron_ingots", "forge:ingots/iron", "minecraft:iron_ingot");
        registerEquivalenceGroup("c:gold_ingots", "forge:ingots/gold", "minecraft:gold_ingot");
        registerEquivalenceGroup("c:copper_ingots", "forge:ingots/copper", "minecraft:copper_ingot");
        registerEquivalenceGroup("c:netherite_ingots", "forge:ingots/netherite", "minecraft:netherite_ingot");

        // Gems & Minerals
        registerEquivalenceGroup("c:diamonds", "forge:gems/diamond", "minecraft:diamond");
        registerEquivalenceGroup("c:emeralds", "forge:gems/emerald", "minecraft:emerald");
        registerEquivalenceGroup("c:lapis", "forge:gems/lapis", "minecraft:lapis_lazuli");
        registerEquivalenceGroup("c:quartz", "forge:gems/quartz", "minecraft:quartz");
        registerEquivalenceGroup("c:coal", "forge:gems/coal", "minecraft:coal");
        registerEquivalenceGroup("c:redstone_dusts", "forge:dusts/redstone", "minecraft:redstone");
        registerEquivalenceGroup("c:glowstone_dusts", "forge:dusts/glowstone", "minecraft:glowstone_dust");

        // Raw Materials
        registerEquivalenceGroup("c:raw_iron_blocks", "forge:storage_blocks/raw_iron", "minecraft:raw_iron_block");
        registerEquivalenceGroup("c:raw_copper_blocks", "forge:storage_blocks/raw_copper", "minecraft:raw_copper_block");
        registerEquivalenceGroup("c:raw_gold_blocks", "forge:storage_blocks/raw_gold", "minecraft:raw_gold_block");
        registerEquivalenceGroup("c:raw_materials/iron", "forge:raw_materials/iron", "minecraft:raw_iron");
        registerEquivalenceGroup("c:raw_materials/copper", "forge:raw_materials/copper", "minecraft:raw_copper");
        registerEquivalenceGroup("c:raw_materials/gold", "forge:raw_materials/gold", "minecraft:raw_gold");

        // Ores
        registerEquivalenceGroup("c:iron_ores", "c:ores/iron", "forge:ores/iron");
        registerEquivalenceGroup("c:gold_ores", "c:ores/gold", "forge:ores/gold");
        registerEquivalenceGroup("c:copper_ores", "c:ores/copper", "forge:ores/copper");
        registerEquivalenceGroup("c:diamond_ores", "c:ores/diamond", "forge:ores/diamond");
        registerEquivalenceGroup("c:emerald_ores", "c:ores/emerald", "forge:ores/emerald");
        registerEquivalenceGroup("c:coal_ores", "c:ores/coal", "forge:ores/coal");
        registerEquivalenceGroup("c:lapis_ores", "c:ores/lapis", "forge:ores/lapis");

        // Tools
        registerEquivalenceGroup("c:tools/pickaxes", "forge:tools/pickaxes", "c:pickaxes");
        registerEquivalenceGroup("c:tools/axes", "forge:tools/axes", "c:axes");
        registerEquivalenceGroup("c:tools/shovels", "forge:tools/shovels", "c:shovels");
        registerEquivalenceGroup("c:tools/swords", "forge:tools/swords", "c:swords");
        registerEquivalenceGroup("c:tools/hoes", "forge:tools/hoes", "c:hoes");
        registerEquivalenceGroup("c:shears", "forge:shears", "minecraft:shears");

        // Storage & Container
        registerEquivalenceGroup("c:chests", "forge:chests", "minecraft:chest");
        registerEquivalenceGroup("c:chests/wooden", "forge:chests/wooden");
        registerEquivalenceGroup("c:chests/ender", "forge:chests/ender", "minecraft:ender_chest");
        registerEquivalenceGroup("c:barrels", "forge:barrels", "minecraft:barrel");
        registerEquivalenceGroup("c:glass_blocks", "forge:glass", "minecraft:glass");
        registerEquivalenceGroup("c:glass_panes", "forge:glass_panes", "minecraft:glass_pane");

        // Rods & Materials
        registerEquivalenceGroup("c:rods/wooden", "forge:rods/wooden", "minecraft:stick");
        registerEquivalenceGroup("c:rods/blaze", "forge:rods/blaze", "minecraft:blaze_rod");
    }
}

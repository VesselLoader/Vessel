package dev.vessel.kernel.datapack;

import java.util.Arrays;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;

/**
 * High-performance tag registry and spatial membership index.
 * <p>
 * Bridges conventional tags across loaders (#minecraft:, #forge:, #c:, #quilt:)
 * into pre-sorted primitive integer arrays. Membership testing ({@link #isMember(String, int)})
 * is evaluated via branch-optimized binary search with zero heap allocations.
 *
 * @version 1.0.0
 */
public final class TagRegistryIndex {

    private static final int[] EMPTY_INTS = new int[0];
    private final Map<String, int[]> tagMembers;

    /**
     * Constructs a new tag registry index.
     */
    public TagRegistryIndex() {
        this.tagMembers = new ConcurrentHashMap<>();
    }

    /**
     * Registers or replaces a tag with its primitive integer member IDs.
     *
     * @param tagName   canonical tag name (e.g., "c:iron_ores", "minecraft:logs")
     * @param memberIds array of member block or item IDs
     */
    public void registerTag(String tagName, int[] memberIds) {
        Objects.requireNonNull(tagName, "tagName cannot be null");
        Objects.requireNonNull(memberIds, "memberIds cannot be null");

        int[] sorted = Arrays.copyOf(memberIds, memberIds.length);
        Arrays.sort(sorted);
        tagMembers.put(tagName, sorted);
    }

    /**
     * Tests if the given item/block ID is a member of the specified tag.
     *
     * @param tagName  canonical tag name
     * @param memberId member ID
     * @return true if member belongs to tag
     */
    public boolean isMember(String tagName, int memberId) {
        int[] members = tagMembers.get(tagName);
        if (members == null || members.length == 0) {
            return false;
        }
        return Arrays.binarySearch(members, memberId) >= 0;
    }

    /**
     * Returns an array of member IDs belonging to the given tag.
     *
     * @param tagName canonical tag name
     * @return sorted array of member IDs
     */
    public int[] getMembers(String tagName) {
        int[] members = tagMembers.get(tagName);
        if (members == null) {
            return EMPTY_INTS;
        }
        return Arrays.copyOf(members, members.length);
    }

    /**
     * Returns the number of distinct tags registered.
     *
     * @return tag count
     */
    public int getTagCount() {
        return tagMembers.size();
    }

    /**
     * Returns the number of members in a specific tag.
     *
     * @param tagName tag name
     * @return member count
     */
    public int getMemberCount(String tagName) {
        int[] members = tagMembers.get(tagName);
        return (members != null) ? members.length : 0;
    }

    /**
     * Clears all registered tags.
     */
    public void clear() {
        tagMembers.clear();
    }
}

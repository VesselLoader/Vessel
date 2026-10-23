package dev.vessel.kernel.remap;

import java.util.Objects;

/**
 * Immutable lookup result returned by {@link CompactMappingDictionary} when querying class,
 * method, or field symbol translations.
 *
 * @param found              {@code true} if an explicit mapping entry matched the query
 * @param remappedName       translated symbol name, or original name when unmapped
 * @param remappedDescriptor translated JVM descriptor, or original descriptor when unmapped
 * @version 1.0.1
 * @since 1.0.0 (Pass K-27)
 */
public record MappingLookupResult(
        boolean found,
        String remappedName,
        String remappedDescriptor
) {

    public MappingLookupResult {
        Objects.requireNonNull(remappedName, "remappedName cannot be null");
        Objects.requireNonNull(remappedDescriptor, "remappedDescriptor cannot be null");
    }

    /**
     * Creates a fallback result representing an unmapped symbol that retains its original name
     * and descriptor.
     *
     * @param originalName       original internal class, method, or field name
     * @param originalDescriptor original JVM descriptor (may be empty for class lookups)
     * @return unmapped lookup result with {@code found = false}
     */
    public static MappingLookupResult unmapped(String originalName, String originalDescriptor) {
        Objects.requireNonNull(originalName, "originalName cannot be null");
        String safeDesc = originalDescriptor != null ? originalDescriptor : "";
        return new MappingLookupResult(false, originalName, safeDesc);
    }
}

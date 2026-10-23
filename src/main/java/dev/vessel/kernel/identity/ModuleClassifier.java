package dev.vessel.kernel.identity;

import java.util.Objects;

/**
 * Enumeration representing the structural classification and functional role of a module.
 *
 * <p>Enables fine-grained resolution of multi-jar packages where library, API, or compatibility
 * bridges accompany the primary mod jar.</p>
 *
 * @version 1.0.1
 * @since 1.0.0 (Pass K-3)
 */
public enum ModuleClassifier {

    /** Primary executable mod or plugin payload. */
    MAIN("main"),

    /** Public interface or API-only library layer. */
    API("api"),

    /** Compatibility bridge or adapter module targeting a secondary ecosystem. */
    COMPAT_BRIDGE("compat"),

    /** Embedded dependency or runtime helper packaged inside a Jar-in-Jar. */
    RUNTIME_EMBEDDED("embedded");

    private final String tag;

    ModuleClassifier(String tag) {
        this.tag = Objects.requireNonNull(tag, "tag must not be null");
    }

    /**
     * Returns the serialized tag representing this classifier.
     *
     * @return the classifier tag
     */
    public String getTag() {
        return tag;
    }

    /**
     * Resolves a module classifier from a string tag (case-insensitive).
     *
     * @param tag the tag to parse
     * @return the matched ModuleClassifier, defaults to MAIN if empty
     */
    public static ModuleClassifier fromTag(String tag) {
        if (tag == null || tag.isBlank()) {
            return MAIN;
        }
        String normalized = tag.trim().toLowerCase();
        for (ModuleClassifier classifier : values()) {
            if (classifier.tag.equals(normalized)) {
                return classifier;
            }
        }
        throw new IllegalArgumentException("Unknown ModuleClassifier tag: " + tag);
    }
}

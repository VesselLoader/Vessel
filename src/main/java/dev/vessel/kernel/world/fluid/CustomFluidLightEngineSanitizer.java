package dev.vessel.kernel.world.fluid;

import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Sanitizer and normalizer for modded custom fluid light properties.
 * <p>
 * Third-party mods (e.g. Mekanism, Create, IndustrialCraft) occasionally register fluids with out-of-bounds,
 * negative, or uninitialized light parameters (e.g. negative luminance or opacity &gt; 15).
 * Passing unchecked parameters to Starlight causes BFS queue overflows, index out-of-bounds, or infinite propagation.
 * This sanitizer clamps and validates all fluid parameters before ingestion by the lighting engine.
 * Mitigates ERR-23 and resolves BLK-0401 through BLK-0600.
 * </p>
 *
 * @version 1.0.0
 * @author VESSEL Engine Team
 */
public final class CustomFluidLightEngineSanitizer {

    public static final int MIN_LIGHT_LEVEL = 0;
    public static final int MAX_LIGHT_LEVEL = 15;
    public static final int DEFAULT_SAFE_OPACITY = 1;
    public static final int DEFAULT_SAFE_LUMINANCE = 0;

    public static final String AUDIT_OK = "OK_IN_BOUNDS";
    public static final String AUDIT_LUMINANCE_UNDERFLOW = "LUMINANCE_UNDERFLOW_CLAMPED_TO_0";
    public static final String AUDIT_LUMINANCE_OVERFLOW = "LUMINANCE_OVERFLOW_CLAMPED_TO_15";
    public static final String AUDIT_OPACITY_UNDERFLOW = "OPACITY_UNDERFLOW_CLAMPED_TO_0";
    public static final String AUDIT_OPACITY_OVERFLOW = "OPACITY_OVERFLOW_CLAMPED_TO_15";

    private final ConcurrentHashMap<String, SanitizedFluidLightProperties> cache;
    private final AtomicInteger totalSanitizedCount;
    private final AtomicInteger anomalyCount;

    /**
     * Constructs a CustomFluidLightEngineSanitizer.
     */
    public CustomFluidLightEngineSanitizer() {
        this.cache = new ConcurrentHashMap<>();
        this.totalSanitizedCount = new AtomicInteger(0);
        this.anomalyCount = new AtomicInteger(0);
    }

    /**
     * Sanitizes raw fluid parameters, recording audit details and caching the result.
     *
     * @param fluidId      unique fluid identifier
     * @param rawLuminance raw reported luminance
     * @param rawOpacity   raw reported opacity
     * @return sanitized properties guaranteed safe for Starlight
     */
    public SanitizedFluidLightProperties sanitize(String fluidId, int rawLuminance, int rawOpacity) {
        Objects.requireNonNull(fluidId, "fluidId cannot be null");

        SanitizedFluidLightProperties existing = cache.get(fluidId);
        if (existing != null) {
            return existing;
        }

        totalSanitizedCount.incrementAndGet();

        boolean clamped = false;
        StringBuilder auditBuilder = new StringBuilder();

        // Validate and clamp luminance
        int safeLuminance = rawLuminance;
        if (rawLuminance < MIN_LIGHT_LEVEL) {
            safeLuminance = MIN_LIGHT_LEVEL;
            clamped = true;
            auditBuilder.append(AUDIT_LUMINANCE_UNDERFLOW).append("; ");
        } else if (rawLuminance > MAX_LIGHT_LEVEL) {
            safeLuminance = MAX_LIGHT_LEVEL;
            clamped = true;
            auditBuilder.append(AUDIT_LUMINANCE_OVERFLOW).append("; ");
        }

        // Validate and clamp opacity
        int safeOpacity = rawOpacity;
        if (rawOpacity < MIN_LIGHT_LEVEL) {
            safeOpacity = MIN_LIGHT_LEVEL;
            clamped = true;
            auditBuilder.append(AUDIT_OPACITY_UNDERFLOW).append("; ");
        } else if (rawOpacity > MAX_LIGHT_LEVEL) {
            safeOpacity = MAX_LIGHT_LEVEL;
            clamped = true;
            auditBuilder.append(AUDIT_OPACITY_OVERFLOW).append("; ");
        }

        if (clamped) {
            anomalyCount.incrementAndGet();
        } else {
            auditBuilder.append(AUDIT_OK);
        }

        SanitizedFluidLightProperties result = new SanitizedFluidLightProperties(
                fluidId,
                safeLuminance,
                safeOpacity,
                clamped,
                auditBuilder.toString()
        );

        cache.put(fluidId, result);
        return result;
    }

    /**
     * Convenience method to sanitize from a {@link RawFluidLightDescriptor}.
     *
     * @param descriptor raw fluid light descriptor
     * @return sanitized properties
     */
    public SanitizedFluidLightProperties sanitize(RawFluidLightDescriptor descriptor) {
        Objects.requireNonNull(descriptor, "descriptor cannot be null");
        return sanitize(descriptor.fluidId(), descriptor.rawLuminance(), descriptor.rawOpacity());
    }

    /**
     * Fast-path standalone luminance clamp without cache query.
     *
     * @param rawLuminance raw luminance
     * @return safe luminance in [0, 15]
     */
    public static int sanitizeLuminance(int rawLuminance) {
        return Math.clamp(rawLuminance, MIN_LIGHT_LEVEL, MAX_LIGHT_LEVEL);
    }

    /**
     * Fast-path standalone opacity clamp without cache query.
     *
     * @param rawOpacity raw opacity
     * @return safe opacity in [0, 15]
     */
    public static int sanitizeOpacity(int rawOpacity) {
        return Math.clamp(rawOpacity, MIN_LIGHT_LEVEL, MAX_LIGHT_LEVEL);
    }

    /**
     * Retrieves pre-sanitized properties from cache if available.
     *
     * @param fluidId fluid identifier
     * @return cached properties or null
     */
    public SanitizedFluidLightProperties getCached(String fluidId) {
        Objects.requireNonNull(fluidId, "fluidId cannot be null");
        return cache.get(fluidId);
    }

    /**
     * Pre-registers a known canonical fluid light definition.
     *
     * @param fluidId   fluid identifier
     * @param luminance safe luminance
     * @param opacity   safe opacity
     */
    public void registerPreSanitized(String fluidId, int luminance, int opacity) {
        Objects.requireNonNull(fluidId, "fluidId cannot be null");
        int safeLuminance = Math.clamp(luminance, MIN_LIGHT_LEVEL, MAX_LIGHT_LEVEL);
        int safeOpacity = Math.clamp(opacity, MIN_LIGHT_LEVEL, MAX_LIGHT_LEVEL);
        SanitizedFluidLightProperties props = new SanitizedFluidLightProperties(
                fluidId, safeLuminance, safeOpacity, false, AUDIT_OK
        );
        cache.put(fluidId, props);
    }

    public int getSanitizedCount() {
        return totalSanitizedCount.get();
    }

    public int getAnomalyCount() {
        return anomalyCount.get();
    }

    public int getCachedFluidCount() {
        return cache.size();
    }

    public void clearCache() {
        cache.clear();
    }
}

package dev.vessel.kernel.vstm.binary;

/**
 * Manages format versioning and backward/forward compatibility rules for .vstmb binary files.
 *
 * <p>Format Version Specification:
 * <ul>
 *     <li>v1.0 (0x0001): Initial canonical memory-mapped layout with 64-byte aligned header,
 *         string pool, and $O(1)$ fast lookup slots.</li>
 * </ul>
 * </p>
 *
 * <p>Guards against offset calculation errors and incompatible schema drift (Catalog ref: ERR-0015).</p>
 *
 * @version 1.0.0
 * @since 1.0.0 (Wave 1)
 */
public final class VstmbFormatVersion {

    /**
     * Canonical format version 1.0 (uint16 representation).
     */
    public static final short VERSION_1 = 1;

    /**
     * Current generator version used by the VESSEL microkernel.
     */
    public static final short CURRENT_VERSION = VERSION_1;

    /**
     * Minimum binary version supported for backwards compatibility.
     */
    public static final short MIN_SUPPORTED_VERSION = VERSION_1;

    /**
     * Maximum binary version supported by this microkernel release.
     */
    public static final short MAX_SUPPORTED_VERSION = VERSION_1;

    /**
     * Determines whether the given version integer is supported by this runtime.
     *
     * @param version the format version to check
     * @return true if supported, false otherwise
     */
    public static boolean isSupported(short version) {
        return version >= MIN_SUPPORTED_VERSION && version <= MAX_SUPPORTED_VERSION;
    }

    /**
     * Validates that the binary version is supported by the runtime engine.
     *
     * @param version the version found in the header
     * @throws VstmbBinaryCorruptException if the version is unsupported
     */
    public static void validate(short version) {
        if (!isSupported(version)) {
            throw new VstmbBinaryCorruptException(
                    "ERR-VSTM-VERSION",
                    "Unsupported .vstmb format version: " + version
                            + " (supported range: [" + MIN_SUPPORTED_VERSION + " .. " + MAX_SUPPORTED_VERSION + "])"
            );
        }
    }

    private VstmbFormatVersion() {
        // Constant and validation holder
    }
}

package dev.vessel.kernel.autoupdate;

import java.util.Collections;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

/**
 * Immutable descriptor for an incremental or cumulative binary patch containing class/resource updates.
 *
 * <p>Strictly adheres to Rule 17 with zero inner, nested, or anonymous classes.</p>
 *
 * @param fromRevision      the expected prerequisite base revision number
 * @param toRevision        the resulting target revision number after application
 * @param patchId           unique diagnostic identifier for this patch payload
 * @param entriesToUpsert   map of relative jar entry paths to replacement byte contents
 * @param entriesToDelete   set of relative jar entry paths to remove from the target jar
 * @param patchDigestSha256 SHA-256 checksum for cryptographic verification
 *
 * @author VESSEL Engine Team
 * @version 1.0.0
 */
public record PatchRevision(
        int fromRevision,
        int toRevision,
        String patchId,
        Map<String, byte[]> entriesToUpsert,
        Set<String> entriesToDelete,
        String patchDigestSha256
) {

    public PatchRevision {
        if (fromRevision < 0) {
            throw new IllegalArgumentException("fromRevision must not be negative: " + fromRevision);
        }
        if (toRevision <= fromRevision) {
            throw new IllegalArgumentException(
                    "toRevision (" + toRevision + ") must be strictly greater than fromRevision (" + fromRevision + ")");
        }
        Objects.requireNonNull(patchId, "patchId must not be null");
        Objects.requireNonNull(entriesToUpsert, "entriesToUpsert must not be null");
        Objects.requireNonNull(entriesToDelete, "entriesToDelete must not be null");
        Objects.requireNonNull(patchDigestSha256, "patchDigestSha256 must not be null");

        entriesToUpsert = Collections.unmodifiableMap(entriesToUpsert);
        entriesToDelete = Collections.unmodifiableSet(entriesToDelete);
    }

    /**
     * Checks whether this patch is directly applicable to a server currently at {@code currentRevision}.
     *
     * @param currentRevision the server's current binary revision
     * @return {@code true} if this patch directly builds upon {@code currentRevision}
     */
    public boolean isDirectlyApplicable(int currentRevision) {
        return this.fromRevision == currentRevision;
    }
}

package dev.vessel.kernel.registry;

import java.util.Objects;

/**
 * Immutable security token authorizing scoped unfreeze operations on registries.
 *
 * @param tokenId unique token identifier
 * @param requesterName module or loader requesting scoped unfreeze
 * @param issuedAtMillis creation timestamp
 *
 * @version 1.0.0
 * @author VESSEL Engine Team
 */
public record RegistryLockToken(
        String tokenId,
        String requesterName,
        long issuedAtMillis
) {
    public RegistryLockToken {
        Objects.requireNonNull(tokenId, "tokenId cannot be null");
        Objects.requireNonNull(requesterName, "requesterName cannot be null");
        if (issuedAtMillis <= 0) {
            issuedAtMillis = System.currentTimeMillis();
        }
    }
}

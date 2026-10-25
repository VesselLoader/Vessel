package dev.vessel.kernel.adapter.forge;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Pre-registration scanner identifying duplicate registry key claims across competing mod distributions.
 * <p>
 * Eliminates ERR-92 by surfacing duplicate keys before registration commits to prevent silent overrides.
 * </p>
 *
 * @version 1.0.0
 * @author VESSEL Engine Team
 */
public final class ModDuplicateRegistryKeyScanner {

    private static final Logger LOGGER = LoggerFactory.getLogger(ModDuplicateRegistryKeyScanner.class);

    private static final String ANSI_RED = "\u001B[31m";
    private static final String ANSI_RESET = "\u001B[0m";

    /**
     * Constructs a mod duplicate registry key scanner.
     */
    public ModDuplicateRegistryKeyScanner() {
    }

    /**
     * Inspects registry claims and validates that no two distinct mods declare the same key in any registry.
     *
     * @param claims list of prospective key claims
     * @return clean collision report
     * @throws DuplicateRegistryKeyException if any collision is encountered
     */
    public RegistryCollisionReport scanAndValidate(List<RegistryKeyClaim> claims) {
        RegistryCollisionReport report = scanOnly(claims);
        if (report.hasCollisions()) {
            emitRedCollisionBanner(report);
            RegistryKeyCollision first = report.collisions().get(0);
            throw new DuplicateRegistryKeyException(
                    first.registryIdentifier(),
                    first.namespacedKey(),
                    first.firstModId(),
                    first.secondModId(),
                    "Pre-flight registry key validation rejected conflicting registration."
            );
        }
        return report;
    }

    /**
     * Inspects registry claims without throwing an exception, returning a detailed collision report.
     *
     * @param claims list of prospective key claims
     * @return collision report
     */
    public RegistryCollisionReport scanOnly(List<RegistryKeyClaim> claims) {
        Objects.requireNonNull(claims, "claims must not be null");

        // Map key format: registry:namespacedKey
        Map<String, RegistryKeyClaim> seenClaims = new HashMap<>();
        List<RegistryKeyCollision> collisions = new ArrayList<>();

        for (RegistryKeyClaim claim : claims) {
            String compositeKey = claim.registryIdentifier().trim().toLowerCase(Locale.ROOT)
                    + "/" + claim.namespacedKey().trim().toLowerCase(Locale.ROOT);

            RegistryKeyClaim existing = seenClaims.get(compositeKey);
            if (existing != null) {
                // If two claims originate from different mod IDs, record collision
                if (!existing.ownerModId().equalsIgnoreCase(claim.ownerModId())) {
                    collisions.add(new RegistryKeyCollision(
                            claim.registryIdentifier(),
                            claim.namespacedKey(),
                            existing.ownerModId(),
                            claim.ownerModId(),
                            existing.declarationSource(),
                            claim.declarationSource()
                    ));
                }
            } else {
                seenClaims.put(compositeKey, claim);
            }
        }

        return new RegistryCollisionReport(
                !collisions.isEmpty(),
                collisions.size(),
                Collections.unmodifiableList(collisions)
        );
    }

    private void emitRedCollisionBanner(RegistryCollisionReport report) {
        StringBuilder sb = new StringBuilder();
        sb.append(ANSI_RED);
        sb.append("\n================================================================================\n");
        sb.append(" [VESSEL REGISTRY ERROR] DUPLICATE REGISTRY KEY COLLISION DETECTED (ERR-92)\n");
        sb.append("================================================================================\n");
        for (RegistryKeyCollision col : report.collisions()) {
            sb.append(String.format(" Registry: %s | Key: %s\n", col.registryIdentifier(), col.namespacedKey()));
            sb.append(String.format("   - Primary Owner   : mod '%s' (%s)\n", col.firstModId(), col.firstSource()));
            sb.append(String.format("   - Conflicting Mod : mod '%s' (%s)\n", col.secondModId(), col.secondSource()));
        }
        sb.append("================================================================================\n");
        sb.append(ANSI_RESET);

        String banner = sb.toString();
        LOGGER.error("{}", banner);
        System.err.println(banner);
    }
}

package dev.vessel.kernel.autoupdate;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;
import java.util.jar.JarOutputStream;

/**
 * Applies cumulative and incremental patch sequences across multiple successive revisions (e.g. rev100 -> rev105)
 * without re-downloading complete 100MB+ server distribution binaries.
 *
 * <p>Consolidates delta entries, deletes superseded classes, and commits the target revision directly
 * into the server binary artifact atomically.</p>
 *
 * <p>Strictly adheres to Rule 17 with zero inner, nested, or anonymous classes.</p>
 *
 * @author VESSEL Engine Team
 * @version 1.0.0
 */
public final class CumulativePatchRollforwardApplier {

    public static final String REVISION_METADATA_ENTRY = "META-INF/VESSEL-REVISION";
    public static final String ROLLFORWARD_TEMP_PREFIX = ".vessel-rollforward-tmp-";
    public static final int BUFFER_SIZE = 8192;

    /**
     * Reads the current numeric revision marker from inside the given server jar artifact.
     * Defaults to {@code 0} if the revision entry is absent or unparseable.
     *
     * @param jarPath path to the server jar
     * @return parsed revision number, or {@code 0}
     */
    public int readRevisionFromJar(Path jarPath) {
        Objects.requireNonNull(jarPath, "jarPath must not be null");
        if (!Files.isRegularFile(jarPath)) {
            return 0;
        }
        try (JarFile jarFile = new JarFile(jarPath.toFile())) {
            JarEntry entry = jarFile.getJarEntry(REVISION_METADATA_ENTRY);
            if (entry == null) {
                return 0;
            }
            try (InputStream is = jarFile.getInputStream(entry)) {
                String raw = new String(is.readAllBytes(), StandardCharsets.UTF_8).trim();
                return Integer.parseInt(raw);
            }
        } catch (Exception ignored) {
            return 0;
        }
    }

    /**
     * Validates that the provided sequence of patches forms a contiguous, monotonic, and gap-free
     * rollforward chain starting from {@code expectedStartingRevision}.
     *
     * @param expectedStartingRevision prerequisite base revision
     * @param patchChain               ordered list of candidate patches
     * @throws PatchChainValidationException if the chain contains gaps, reversed revisions, or is empty
     */
    public void validatePatchChain(int expectedStartingRevision, List<PatchRevision> patchChain) {
        Objects.requireNonNull(patchChain, "patchChain must not be null");
        if (patchChain.isEmpty()) {
            throw new PatchChainValidationException("Patch sequence is empty; at least one patch is required");
        }

        int current = expectedStartingRevision;
        for (int i = 0; i < patchChain.size(); i++) {
            PatchRevision patch = patchChain.get(i);
            if (patch == null) {
                throw new PatchChainValidationException("Patch at index " + i + " must not be null");
            }
            if (patch.fromRevision() != current) {
                throw new PatchChainValidationException(
                        "Revision continuity broken at index " + i + ". Expected fromRevision: "
                                + current + ", but encountered: " + patch.fromRevision()
                );
            }
            if (patch.toRevision() <= patch.fromRevision()) {
                throw new PatchChainValidationException(
                        "Invalid patch revision range at index " + i + ": from "
                                + patch.fromRevision() + " to " + patch.toRevision()
                );
            }
            current = patch.toRevision();
        }
    }

    /**
     * Applies a contiguous chain of patches onto {@code baseJarPath}, writing the resulting updated binary
     * to {@code outputJarPath}.
     *
     * @param baseJarPath   path to the existing source server jar
     * @param outputJarPath destination path for the updated server jar
     * @param patchChain    ordered sequence of incremental patches
     * @return {@link RollforwardApplicationResult} documenting the rollforward transformation
     * @throws IOException if reading base jar or writing updated jar fails
     */
    public RollforwardApplicationResult applyCumulativeRollforward(
            Path baseJarPath,
            Path outputJarPath,
            List<PatchRevision> patchChain) throws IOException {
        Objects.requireNonNull(baseJarPath, "baseJarPath must not be null");
        Objects.requireNonNull(outputJarPath, "outputJarPath must not be null");
        Objects.requireNonNull(patchChain, "patchChain must not be null");

        int initialRevision = readRevisionFromJar(baseJarPath);
        validatePatchChain(initialRevision, patchChain);

        int finalRevision = patchChain.get(patchChain.size() - 1).toRevision();

        // Consolidate patch operations into net upserts and net deletes
        Map<String, byte[]> consolidatedUpserts = new HashMap<>();
        Set<String> consolidatedDeletes = new HashSet<>();

        for (PatchRevision patch : patchChain) {
            for (String deleteKey : patch.entriesToDelete()) {
                consolidatedDeletes.add(deleteKey);
                consolidatedUpserts.remove(deleteKey);
            }
            for (Map.Entry<String, byte[]> upsertEntry : patch.entriesToUpsert().entrySet()) {
                consolidatedUpserts.put(upsertEntry.getKey(), upsertEntry.getValue());
                consolidatedDeletes.remove(upsertEntry.getKey());
            }
        }

        // Always update or add the REVISION_METADATA_ENTRY to reflect the final revision
        consolidatedUpserts.put(
                REVISION_METADATA_ENTRY,
                String.valueOf(finalRevision).getBytes(StandardCharsets.UTF_8)
        );
        consolidatedDeletes.remove(REVISION_METADATA_ENTRY);

        Path outputDir = outputJarPath.getParent();
        if (outputDir != null) {
            Files.createDirectories(outputDir);
        }

        Path tempOutputFile = (outputDir != null ? outputDir : Path.of(".")).resolve(
                ROLLFORWARD_TEMP_PREFIX + System.currentTimeMillis() + ".jar"
        );

        try {
            try (JarFile baseJar = new JarFile(baseJarPath.toFile());
                 JarOutputStream jos = new JarOutputStream(Files.newOutputStream(tempOutputFile))) {

                Set<String> writtenEntryNames = new HashSet<>();
                Enumeration<JarEntry> entries = baseJar.entries();

                while (entries.hasMoreElements()) {
                    JarEntry entry = entries.nextElement();
                    String entryName = entry.getName();

                    // If marked for deletion, omit from new jar
                    if (consolidatedDeletes.contains(entryName)) {
                        continue;
                    }

                    // If this entry has a consolidated upsert replacement
                    if (consolidatedUpserts.containsKey(entryName)) {
                        byte[] replacementData = consolidatedUpserts.remove(entryName);
                        writeSingleJarEntry(jos, entryName, replacementData);
                        writtenEntryNames.add(entryName);
                        continue;
                    }

                    // Otherwise copy original entry directly
                    JarEntry newEntry = new JarEntry(entryName);
                    newEntry.setTime(entry.getTime());
                    jos.putNextEntry(newEntry);
                    try (InputStream entryStream = baseJar.getInputStream(entry)) {
                        entryStream.transferTo(jos);
                    }
                    jos.closeEntry();
                    writtenEntryNames.add(entryName);
                }

                // Write remaining new upserts that were not present in the base jar
                for (Map.Entry<String, byte[]> remainingEntry : consolidatedUpserts.entrySet()) {
                    String entryName = remainingEntry.getKey();
                    if (!writtenEntryNames.contains(entryName)) {
                        writeSingleJarEntry(jos, entryName, remainingEntry.getValue());
                        writtenEntryNames.add(entryName);
                    }
                }
            }

            // Atomically replace outputJarPath with the completed temp file
            try {
                Files.move(tempOutputFile, outputJarPath, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE);
            } catch (AtomicMoveNotSupportedException e) {
                Files.move(tempOutputFile, outputJarPath, StandardCopyOption.REPLACE_EXISTING);
            }

            return RollforwardApplicationResult.successful(
                    initialRevision,
                    finalRevision,
                    patchChain.size(),
                    outputJarPath,
                    "Successfully applied " + patchChain.size() + " cumulative patches from rev"
                            + initialRevision + " to rev" + finalRevision
            );
        } finally {
            try {
                Files.deleteIfExists(tempOutputFile);
            } catch (IOException ignored) {
            }
        }
    }

    private void writeSingleJarEntry(JarOutputStream jos, String entryName, byte[] data) throws IOException {
        JarEntry newEntry = new JarEntry(entryName);
        newEntry.setTime(System.currentTimeMillis());
        jos.putNextEntry(newEntry);
        jos.write(data);
        jos.closeEntry();
    }
}

package dev.vessel.bootstrap.mojang;

import java.io.File;
import java.nio.file.Path;
import java.util.Enumeration;
import java.util.LinkedHashSet;
import java.util.Objects;
import java.util.Set;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;

/**
 * Scans mod JARs in the server's {@code mods/} directory to discover all declared block identifiers
 * by inspecting blockstates, block models, and block textures.
 *
 * <p>Strictly adheres to Rule 17 with zero inner, nested, or anonymous classes.</p>
 *
 * @version 1.0.0
 */
public final class VesselBlockIdentifierScanner {

    public VesselBlockIdentifierScanner() {
    }

    /**
     * Scans all {@code .jar} files in {@code modsDir} and returns all discovered block identifiers.
     *
     * @param modsDir directory containing mod JARs
     * @return set of namespaced block identifiers (e.g. {@code "create:cogwheel"})
     */
    public Set<String> scanBlockIdentifiers(Path modsDir) {
        Objects.requireNonNull(modsDir, "modsDir must not be null");
        Set<String> result = new LinkedHashSet<>();
        File[] files = modsDir.toFile().listFiles();
        if (files == null) {
            return result;
        }

        for (File file : files) {
            if (!file.isFile() || !file.getName().toLowerCase().endsWith(".jar")) {
                continue;
            }
            try (JarFile jar = new JarFile(file)) {
                Enumeration<JarEntry> entries = jar.entries();
                while (entries.hasMoreElements()) {
                    JarEntry entry = entries.nextElement();
                    String name = entry.getName();
                    if (!name.startsWith("assets/")) {
                        continue;
                    }

                    // 1. Primary: assets/<namespace>/blockstates/<block>.json (1.8 – 26.3+)
                    if (name.endsWith(".json")) {
                        int bsIdx = name.indexOf("/blockstates/");
                        if (bsIdx > 7) {
                            String namespace = name.substring(7, bsIdx);
                            String blockPath = name.substring(bsIdx + "/blockstates/".length(), name.length() - 5);
                            if (isValidIdentifierPart(namespace) && !blockPath.isBlank()) {
                                result.add(namespace + ":" + blockPath);
                                int lastSlash = blockPath.lastIndexOf('/');
                                if (lastSlash >= 0) {
                                    result.add(namespace + ":" + blockPath.substring(lastSlash + 1));
                                }
                            }
                        }

                        // 2. Secondary: assets/<namespace>/models/block/<block>.json
                        int mbIdx = name.indexOf("/models/block/");
                        if (mbIdx > 7) {
                            String namespace = name.substring(7, mbIdx);
                            String blockPath = name.substring(mbIdx + "/models/block/".length(), name.length() - 5);
                            if (isValidIdentifierPart(namespace) && !blockPath.isBlank()) {
                                result.add(namespace + ":" + blockPath);
                                int lastSlash = blockPath.lastIndexOf('/');
                                if (lastSlash >= 0) {
                                    result.add(namespace + ":" + blockPath.substring(lastSlash + 1));
                                }
                            }
                        }
                    }

                    // 3. Legacy textures: assets/<namespace>/textures/blocks/<block>.png (1.7.10 – 1.12.2)
                    if (name.endsWith(".png")) {
                        int tbIdx = name.indexOf("/textures/blocks/");
                        if (tbIdx > 7) {
                            String namespace = name.substring(7, tbIdx);
                            String blockPath = name.substring(tbIdx + "/textures/blocks/".length(), name.length() - 4);
                            if (isValidIdentifierPart(namespace) && !blockPath.isBlank()) {
                                result.add(namespace + ":" + blockPath);
                                int lastSlash = blockPath.lastIndexOf('/');
                                if (lastSlash >= 0) {
                                    result.add(namespace + ":" + blockPath.substring(lastSlash + 1));
                                }
                            }
                        }

                        // Modern textures: assets/<namespace>/textures/block/<block>.png (1.13+)
                        int tSingleIdx = name.indexOf("/textures/block/");
                        if (tSingleIdx > 7) {
                            String namespace = name.substring(7, tSingleIdx);
                            String blockPath = name.substring(tSingleIdx + "/textures/block/".length(), name.length() - 4);
                            if (isValidIdentifierPart(namespace) && !blockPath.isBlank()) {
                                result.add(namespace + ":" + blockPath);
                                int lastSlash = blockPath.lastIndexOf('/');
                                if (lastSlash >= 0) {
                                    result.add(namespace + ":" + blockPath.substring(lastSlash + 1));
                                }
                            }
                        }
                    }
                }
            } catch (Exception ignored) {
            }
        }

        return result;
    }

    private static boolean isValidIdentifierPart(String s) {
        return s != null && !s.isBlank() && !s.contains("/");
    }
}

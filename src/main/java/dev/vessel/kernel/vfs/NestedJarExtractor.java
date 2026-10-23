package dev.vessel.kernel.vfs;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

/**
 * Extracts nested Jar-in-Jar dependencies into memory mounts without disk extraction.
 * <p>
 * Supports standard nested locations across loader ecosystems:
 * <ul>
 *   <li>Fabric / Quilt: {@code META-INF/jars/*.jar}</li>
 *   <li>Forge / NeoForge: {@code META-INF/jarjar/*.jar}</li>
 * </ul>
 * </p>
 *
 * @version 1.0.1
 * @since 1.0.0 (Pass K-4)
 */
public class NestedJarExtractor {

    private static final String FABRIC_JARS_PREFIX = "META-INF/jars/";
    private static final String FORGE_JARJAR_PREFIX = "META-INF/jarjar/";

    /**
     * Extracts all nested JAR archives from a parent JAR input stream.
     *
     * @param parentJarStream stream of the parent JAR archive
     * @return unmodifiable map of relative jar entry path to its byte content
     * @throws IOException if stream reading fails
     */
    public Map<String, byte[]> extractNestedJars(InputStream parentJarStream) throws IOException {
        Objects.requireNonNull(parentJarStream, "parentJarStream cannot be null");
        Map<String, byte[]> nestedJars = new HashMap<>();

        try (ZipInputStream zip = new ZipInputStream(parentJarStream)) {
            ZipEntry entry;
            while ((entry = zip.getNextEntry()) != null) {
                if (!entry.isDirectory()) {
                    String name = entry.getName().replace('\\', '/');
                    if (isNestedJar(name)) {
                        byte[] content = readAllBytes(zip);
                        nestedJars.put(name, content);
                    }
                }
                zip.closeEntry();
            }
        }

        return Collections.unmodifiableMap(nestedJars);
    }

    /**
     * Extracts nested JARs and mounts them into a target virtual file system mount.
     *
     * @param parentJarStream stream of the parent JAR
     * @param targetMount destination in-memory virtual mount
     * @throws IOException if extraction fails
     */
    public void extractAndMount(InputStream parentJarStream, InMemoryVirtualFileSystemMount targetMount) throws IOException {
        Objects.requireNonNull(targetMount, "targetMount cannot be null");
        Map<String, byte[]> extracted = extractNestedJars(parentJarStream);
        for (Map.Entry<String, byte[]> e : extracted.entrySet()) {
            targetMount.putResource(e.getKey(), e.getValue());
        }
    }

    private static boolean isNestedJar(String name) {
        if (!name.endsWith(".jar")) {
            return false;
        }
        return name.startsWith(FABRIC_JARS_PREFIX) || name.startsWith(FORGE_JARJAR_PREFIX);
    }

    private static byte[] readAllBytes(InputStream in) throws IOException {
        ByteArrayOutputStream buffer = new ByteArrayOutputStream();
        byte[] data = new byte[8192];
        int nRead;
        while ((nRead = in.read(data, 0, data.length)) != -1) {
            buffer.write(data, 0, nRead);
        }
        return buffer.toByteArray();
    }
}

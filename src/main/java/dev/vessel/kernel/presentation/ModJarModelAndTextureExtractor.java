package dev.vessel.kernel.presentation;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;

/**
 * Extracts models, textures, blockstates, and lang files from mod JARs into staging directories.
 * Adheres strictly to Rule 17 (Single Responsibility Principle).
 *
 * @version 1.0.0
 */
public final class ModJarModelAndTextureExtractor {

    public int extractAssetsFromModJar(Path modJarPath, Path targetAssetsDir) throws IOException {
        Objects.requireNonNull(modJarPath, "modJarPath cannot be null");
        Objects.requireNonNull(targetAssetsDir, "targetAssetsDir cannot be null");
        if (!Files.isRegularFile(modJarPath)) {
            return 0;
        }

        AtomicInteger extractedCount = new AtomicInteger(0);
        try (JarFile jar = new JarFile(modJarPath.toFile())) {
            var entries = jar.entries();
            while (entries.hasMoreElements()) {
                JarEntry entry = entries.nextElement();
                String name = entry.getName();
                if (name.startsWith("assets/") && !entry.isDirectory() && isAssetPayload(name)) {
                    Path destPath = targetAssetsDir.resolve(name);
                    Files.createDirectories(destPath.getParent());
                    try (InputStream is = jar.getInputStream(entry)) {
                        Files.copy(is, destPath, StandardCopyOption.REPLACE_EXISTING);
                        extractedCount.incrementAndGet();
                    }
                }
            }
        }
        return extractedCount.get();
    }

    public boolean isAssetPayload(String entryName) {
        if (entryName == null) {
            return false;
        }
        return entryName.endsWith(".png")
                || entryName.endsWith(".json")
                || entryName.endsWith(".mcmeta")
                || entryName.endsWith(".ogg");
    }
}

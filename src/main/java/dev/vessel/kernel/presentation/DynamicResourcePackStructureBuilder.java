package dev.vessel.kernel.presentation;

import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Objects;
import java.util.zip.ZipOutputStream;

/**
 * Builds the dynamic client resource pack with server-version-aware pack_format in pack.mcmeta.
 * Adheres strictly to Rule 17 (Single Responsibility Principle - zero anonymous classes).
 *
 * @version 1.0.1
 */
public final class DynamicResourcePackStructureBuilder {

    public int resolvePackFormatForServerVersion(String serverMcVersion) {
        if (serverMcVersion == null || serverMcVersion.isBlank()) {
            return 15; // Safe default for 1.20.1
        }
        if (serverMcVersion.startsWith("1.20.1")) {
            return 15;
        } else if (serverMcVersion.startsWith("1.20.2")) {
            return 18;
        } else if (serverMcVersion.startsWith("1.20.4")) {
            return 22;
        } else if (serverMcVersion.startsWith("1.20.6")) {
            return 32;
        } else if (serverMcVersion.startsWith("1.21.1")) {
            return 34;
        } else if (serverMcVersion.startsWith("26.")) {
            return 46;
        }
        return 15;
    }

    public String generatePackMcMetaJson(int packFormat, String description) {
        String safeDesc = (description != null && !description.isBlank()) ? description : "VESSEL Universal Resource Pack";
        return "{\n"
                + "  \"pack\": {\n"
                + "    \"pack_format\": " + packFormat + ",\n"
                + "    \"description\": \"" + safeDesc + "\"\n"
                + "  }\n"
                + "}";
    }

    public void packageDirectoryToZip(Path sourceDirectory, Path targetZipFile) throws IOException {
        Objects.requireNonNull(sourceDirectory, "sourceDirectory cannot be null");
        Objects.requireNonNull(targetZipFile, "targetZipFile cannot be null");

        if (targetZipFile.getParent() != null) {
            Files.createDirectories(targetZipFile.getParent());
        }

        try (ZipOutputStream zos = new ZipOutputStream(new FileOutputStream(targetZipFile.toFile()))) {
            ResourcePackFileZipVisitor visitor = new ResourcePackFileZipVisitor(sourceDirectory, zos);
            Files.walkFileTree(sourceDirectory, visitor);
        }
    }
}

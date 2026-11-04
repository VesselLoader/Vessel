package dev.vessel.kernel.presentation;

import java.io.IOException;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.Objects;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

/**
 * File visitor that streams visited directory files into a ZipOutputStream.
 * Adheres strictly to Rule 17 (Single Responsibility Principle - zero anonymous classes).
 *
 * @version 1.0.0
 */
public final class ResourcePackFileZipVisitor extends SimpleFileVisitor<Path> {

    private final Path rootDirectory;
    private final ZipOutputStream zipOutputStream;

    public ResourcePackFileZipVisitor(Path rootDirectory, ZipOutputStream zipOutputStream) {
        this.rootDirectory = Objects.requireNonNull(rootDirectory, "rootDirectory cannot be null");
        this.zipOutputStream = Objects.requireNonNull(zipOutputStream, "zipOutputStream cannot be null");
    }

    @Override
    public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
        String relativePath = rootDirectory.relativize(file).toString().replace('\\', '/');
        zipOutputStream.putNextEntry(new ZipEntry(relativePath));
        Files.copy(file, zipOutputStream);
        zipOutputStream.closeEntry();
        return FileVisitResult.CONTINUE;
    }
}

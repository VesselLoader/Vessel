package dev.vessel.kernel.test;

import dev.vessel.kernel.vfs.InMemoryVirtualFileSystemMount;
import dev.vessel.kernel.vfs.JarContentHashVerifier;
import dev.vessel.kernel.vfs.NestedJarExtractor;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Unit verification suite for Virtual File System (VFS), Hash Verification, and Nested Jar Extraction.
 *
 * @version 1.0.0
 * @author Shiroz &amp; Antigravity
 * @since 1.0.0 (Pass K-4)
 */
public class VfsTest {

    @Test
    @DisplayName("InMemoryVirtualFileSystemMount stores, retrieves, and lists resources")
    void testInMemoryVfsBasicOperations() throws IOException {
        try (InMemoryVirtualFileSystemMount mount = new InMemoryVirtualFileSystemMount("/mods/sodium.jar")) {
            assertThat(mount.mountPoint()).isEqualTo("/mods/sodium.jar");
            assertThat(mount.isReadOnly()).isFalse();

            byte[] jsonBytes = "{\"id\":\"sodium\"}".getBytes(StandardCharsets.UTF_8);
            mount.putResource("fabric.mod.json", jsonBytes);

            byte[] classBytes = new byte[]{ (byte) 0xCA, (byte) 0xFE, (byte) 0xBA, (byte) 0xBE };
            mount.putResource("me/jellysquid/mods/sodium/Sodium.class", classBytes);

            assertThat(mount.hasResource("fabric.mod.json")).isTrue();
            assertThat(mount.hasResource("non_existent.txt")).isFalse();

            Optional<byte[]> readJson = mount.readResource("fabric.mod.json");
            assertThat(readJson).isPresent();
            assertThat(readJson.get()).isEqualTo(jsonBytes);

            Set<String> rootListing = mount.listResources("");
            assertThat(rootListing).contains("fabric.mod.json", "me");

            Set<String> subListing = mount.listResources("me/jellysquid/mods/sodium");
            assertThat(subListing).contains("Sodium.class");
        }
    }

    @Test
    @DisplayName("ReadOnly mount rejects writes and closed mount rejects operations")
    void testVfsReadOnlyAndClosedGuards() throws IOException {
        InMemoryVirtualFileSystemMount roMount = new InMemoryVirtualFileSystemMount("/read-only.jar", true);
        assertThatThrownBy(() -> roMount.putResource("test.txt", new byte[]{1, 2, 3}))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("Cannot write to read-only mount");

        InMemoryVirtualFileSystemMount mount = new InMemoryVirtualFileSystemMount("/temp.jar");
        mount.putResource("foo.txt", new byte[]{1});
        mount.close();

        assertThatThrownBy(() -> mount.readResource("foo.txt"))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("already closed");
    }

    @Test
    @DisplayName("JarContentHashVerifier computes and verifies SHA-256 integrity hashes")
    void testSha256Verification() {
        JarContentHashVerifier verifier = new JarContentHashVerifier();
        byte[] payload = "VESSEL_SOVEREIGN_MICROKERNEL_2026".getBytes(StandardCharsets.UTF_8);

        String hash = verifier.computeSha256(payload);
        assertThat(hash).isNotNull().hasSize(64);

        assertThat(verifier.verify(payload, hash)).isTrue();
        assertThat(verifier.verify(payload, hash.toUpperCase())).isTrue();
        assertThat(verifier.verify(payload, "0000000000000000000000000000000000000000000000000000000000000000")).isFalse();
    }

    @Test
    @DisplayName("NestedJarExtractor extracts Jar-in-Jar archives directly into memory")
    void testNestedJarExtraction() throws IOException {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        try (ZipOutputStream zos = new ZipOutputStream(baos)) {
            // Root file
            zos.putNextEntry(new ZipEntry("fabric.mod.json"));
            zos.write("{\"id\":\"root_mod\"}".getBytes(StandardCharsets.UTF_8));
            zos.closeEntry();

            // Nested Jar 1: Fabric format
            zos.putNextEntry(new ZipEntry("META-INF/jars/fabric-api-base-0.4.31.jar"));
            zos.write(new byte[]{ (byte) 0x50, (byte) 0x4B, (byte) 0x03, (byte) 0x04 });
            zos.closeEntry();

            // Nested Jar 2: Forge Jar-in-Jar format
            zos.putNextEntry(new ZipEntry("META-INF/jarjar/sub-library-1.0.jar"));
            zos.write(new byte[]{ (byte) 0x50, (byte) 0x4B, (byte) 0x03, (byte) 0x04 });
            zos.closeEntry();

            // Unrelated text file in META-INF
            zos.putNextEntry(new ZipEntry("META-INF/MANIFEST.MF"));
            zos.write("Manifest-Version: 1.0\n".getBytes(StandardCharsets.UTF_8));
            zos.closeEntry();
        }

        byte[] syntheticParentJar = baos.toByteArray();
        NestedJarExtractor extractor = new NestedJarExtractor();

        Map<String, byte[]> nested = extractor.extractNestedJars(new ByteArrayInputStream(syntheticParentJar));
        assertThat(nested).hasSize(2);
        assertThat(nested).containsKey("META-INF/jars/fabric-api-base-0.4.31.jar");
        assertThat(nested).containsKey("META-INF/jarjar/sub-library-1.0.jar");

        try (InMemoryVirtualFileSystemMount targetMount = new InMemoryVirtualFileSystemMount("/mods/root_mod.jar")) {
            extractor.extractAndMount(new ByteArrayInputStream(syntheticParentJar), targetMount);
            assertThat(targetMount.hasResource("META-INF/jars/fabric-api-base-0.4.31.jar")).isTrue();
            assertThat(targetMount.hasResource("META-INF/jarjar/sub-library-1.0.jar")).isTrue();
            assertThat(targetMount.hasResource("META-INF/MANIFEST.MF")).isFalse();
        }
    }
}

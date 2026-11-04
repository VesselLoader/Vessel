package dev.vessel.kernel.test;

import dev.vessel.kernel.resourcepack.PackTargetPlatform;
import dev.vessel.kernel.resourcepack.VesselDeterministicZipEntry;
import dev.vessel.kernel.resourcepack.VesselDeterministicZipPacker;
import dev.vessel.kernel.resourcepack.VirtualResourcePack;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Verification test suite for {@link VesselDeterministicZipPacker}.
 *
 * @version 1.0.0
 * @author VESSEL Engine Team
 */
public class VesselDeterministicZipPackerTest {

    @Test
    @DisplayName("Deterministic packing produces bit-identical byte output regardless of input insertion order")
    void testDeterministicByteOutput() {
        VesselDeterministicZipPacker packer = new VesselDeterministicZipPacker();

        Map<String, byte[]> map1 = new LinkedHashMap<>();
        map1.put("pack.mcmeta", "{\"pack\":{\"pack_format\":15}}".getBytes(StandardCharsets.UTF_8));
        map1.put("assets/techmod/textures/item/wrench.png", new byte[]{1, 2, 3, 4});
        map1.put("assets/techmod/models/item/wrench.json", "{\"parent\":\"item/generated\"}".getBytes(StandardCharsets.UTF_8));
        map1.put("pack.png", new byte[]{5, 6, 7, 8});

        Map<String, byte[]> map2 = new LinkedHashMap<>();
        // Insert in reverse order
        map2.put("pack.png", new byte[]{5, 6, 7, 8});
        map2.put("assets/techmod/models/item/wrench.json", "{\"parent\":\"item/generated\"}".getBytes(StandardCharsets.UTF_8));
        map2.put("assets/techmod/textures/item/wrench.png", new byte[]{1, 2, 3, 4});
        map2.put("pack.mcmeta", "{\"pack\":{\"pack_format\":15}}".getBytes(StandardCharsets.UTF_8));

        byte[] zip1 = packer.pack(map1);
        byte[] zip2 = packer.pack(map2);

        assertThat(zip1).isNotNull().isNotEmpty();
        assertThat(zip2).isEqualTo(zip1); // 100% byte identical

        String hash1 = VesselDeterministicZipPacker.computeSha1Hex(zip1);
        String hash2 = VesselDeterministicZipPacker.computeSha1Hex(zip2);
        assertThat(hash1).isEqualTo(hash2);
    }

    @Test
    @DisplayName("Deterministic packing produces bit-identical output regardless of JVM default TimeZone")
    void testDeterministicAcrossTimeZones() {
        VesselDeterministicZipPacker packer = new VesselDeterministicZipPacker();
        Map<String, byte[]> entries = new LinkedHashMap<>();
        entries.put("assets/minecraft/textures/block/stone.png", new byte[]{1, 2, 3});
        entries.put("pack.mcmeta", "{\"pack\":{\"pack_format\":15}}".getBytes(StandardCharsets.UTF_8));

        java.util.TimeZone defaultTz = java.util.TimeZone.getDefault();
        try {
            java.util.TimeZone.setDefault(java.util.TimeZone.getTimeZone("UTC"));
            byte[] zipUtc = packer.pack(entries);

            java.util.TimeZone.setDefault(java.util.TimeZone.getTimeZone("Asia/Ho_Chi_Minh"));
            byte[] zipVn = packer.pack(entries);

            java.util.TimeZone.setDefault(java.util.TimeZone.getTimeZone("America/New_York"));
            byte[] zipNy = packer.pack(entries);

            java.util.TimeZone.setDefault(java.util.TimeZone.getTimeZone("Europe/London"));
            byte[] zipLondon = packer.pack(entries);

            assertThat(zipVn).isEqualTo(zipUtc);
            assertThat(zipNy).isEqualTo(zipUtc);
            assertThat(zipLondon).isEqualTo(zipUtc);
        } finally {
            java.util.TimeZone.setDefault(defaultTz);
        }
    }

    @Test
    @DisplayName("Duplicate entry paths or normalized path collisions are handled deterministically without crashing or ordering drift")
    void testDuplicatePathsHandledDeterministically() {
        VesselDeterministicZipPacker packer = new VesselDeterministicZipPacker();
        List<VesselDeterministicZipEntry> entries1 = List.of(
                new VesselDeterministicZipEntry("assets/item/apple.png", new byte[]{1, 2}),
                new VesselDeterministicZipEntry("/assets/item/apple.png", new byte[]{3, 4})
        );
        List<VesselDeterministicZipEntry> entries2 = List.of(
                new VesselDeterministicZipEntry("/assets/item/apple.png", new byte[]{3, 4}),
                new VesselDeterministicZipEntry("assets/item/apple.png", new byte[]{1, 2})
        );

        byte[] zip1 = packer.pack(entries1);
        byte[] zip2 = packer.pack(entries2);
        assertThat(zip1).isEqualTo(zip2);
    }

    @Test
    @DisplayName("normalizePath rejects directory traversal attempts")
    void testPathTraversalRejected() {
        assertThatThrownBy(() -> VesselDeterministicZipEntry.normalizePath("../evil.txt"))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> VesselDeterministicZipEntry.normalizePath("assets/../../secret"))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    @DisplayName("Zip entries strictly adhere to fixed epoch timestamp (1980-01-01 00:00:00 UTC)")
    void testDeterministicTimestampOnEntries() throws IOException {
        VesselDeterministicZipPacker packer = new VesselDeterministicZipPacker();

        Map<String, byte[]> entries = new HashMap<>();
        entries.put("test_file_a.txt", "Hello World".getBytes(StandardCharsets.UTF_8));
        entries.put("test_file_b.txt", "VESSEL Engine".getBytes(StandardCharsets.UTF_8));

        byte[] zipData = packer.pack(entries);

        try (ZipInputStream zis = new ZipInputStream(new ByteArrayInputStream(zipData))) {
            ZipEntry entry;
            int count = 0;
            while ((entry = zis.getNextEntry()) != null) {
                count++;
                // Verify entry time is set to DOS epoch 1980-01-01 00:00:00
                assertThat(entry.getTimeLocal()).isEqualTo(VesselDeterministicZipPacker.DETERMINISTIC_LOCAL_DATETIME);
                zis.closeEntry();
            }
            assertThat(count).isEqualTo(2);
        }
    }

    @Test
    @DisplayName("Entries inside ZIP archive are sorted in strict alphabetical order")
    void testAlphabeticalEntryOrder() throws IOException {
        VesselDeterministicZipPacker packer = new VesselDeterministicZipPacker();

        Map<String, byte[]> entries = new HashMap<>();
        entries.put("z_last.txt", new byte[]{1});
        entries.put("a_first.txt", new byte[]{2});
        entries.put("m_middle.txt", new byte[]{3});
        entries.put("b_second.txt", new byte[]{4});

        byte[] zipData = packer.pack(entries);

        List<String> entryNamesInOrder = new ArrayList<>();
        try (ZipInputStream zis = new ZipInputStream(new ByteArrayInputStream(zipData))) {
            ZipEntry entry;
            while ((entry = zis.getNextEntry()) != null) {
                entryNamesInOrder.add(entry.getName());
                zis.closeEntry();
            }
        }

        assertThat(entryNamesInOrder).containsExactly(
                "a_first.txt",
                "b_second.txt",
                "m_middle.txt",
                "z_last.txt"
        );
    }

    @Test
    @DisplayName("Path normalization handles Windows backslashes and leading slashes")
    void testPathNormalization() throws IOException {
        VesselDeterministicZipPacker packer = new VesselDeterministicZipPacker();

        Map<String, byte[]> entries = new HashMap<>();
        entries.put("\\assets\\minecraft\\textures\\test.png", new byte[]{1, 2});
        entries.put("/assets/techmod/models/test.json", new byte[]{3, 4});

        byte[] zipData = packer.pack(entries);

        List<String> entryNames = new ArrayList<>();
        try (ZipInputStream zis = new ZipInputStream(new ByteArrayInputStream(zipData))) {
            ZipEntry entry;
            while ((entry = zis.getNextEntry()) != null) {
                entryNames.add(entry.getName());
                zis.closeEntry();
            }
        }

        assertThat(entryNames).containsExactly(
                "assets/minecraft/textures/test.png",
                "assets/techmod/models/test.json"
        );
    }

    @Test
    @DisplayName("packVirtual creates valid VirtualResourcePack container with consistent SHA-1 hash")
    void testPackVirtual() {
        VesselDeterministicZipPacker packer = new VesselDeterministicZipPacker();

        Map<String, byte[]> entries = Collections.singletonMap("manifest.json", "{}".getBytes(StandardCharsets.UTF_8));
        VirtualResourcePack pack = packer.packVirtual("test-pack", "Test Pack", PackTargetPlatform.JAVA_EDITION, entries);

        assertThat(pack.packId()).isEqualTo("test-pack");
        assertThat(pack.name()).isEqualTo("Test Pack");
        assertThat(pack.platform()).isEqualTo(PackTargetPlatform.JAVA_EDITION);
        assertThat(pack.archiveData()).isNotEmpty();
        assertThat(pack.sha1Hex()).hasSize(40);
        assertThat(pack.sha1Hex()).isEqualTo(VesselDeterministicZipPacker.computeSha1Hex(pack.archiveData()));
    }

    @Test
    @DisplayName("VesselDeterministicZipEntry defends immutability")
    void testEntryImmutability() {
        byte[] original = new byte[]{1, 2, 3};
        VesselDeterministicZipEntry entry = new VesselDeterministicZipEntry("test.bin", original);

        // Mutate original array
        original[0] = 99;
        assertThat(entry.data()[0]).isEqualTo((byte) 1);

        // Mutate array returned from getter
        byte[] returned = entry.data();
        returned[0] = 77;
        assertThat(entry.data()[0]).isEqualTo((byte) 1);
    }
}

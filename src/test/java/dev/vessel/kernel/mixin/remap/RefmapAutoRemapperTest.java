package dev.vessel.kernel.mixin.remap;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Path;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Verification test suite for {@link RefmapMethodDescriptorScanner}, {@link RefmapVstmBatchTranslator},
 * and {@link RefmapDiskCacheCoordinator}.
 *
 * <p>Strictly adheres to Rule 17: Single Responsibility, zero inner, nested, or anonymous classes.</p>
 */
public final class RefmapAutoRemapperTest {

    @Test
    @DisplayName("Verify RefmapMethodDescriptorScanner splits method and field signatures into tokens")
    void verifyDescriptorScannerTokens() {
        RefmapMethodDescriptorScanner scanner = new RefmapMethodDescriptorScanner();

        List<String> methodTokens = scanner.parseSignatureTokens("Lnet/minecraft/world/level/Level;m_46700_()V");
        assertThat(methodTokens).containsExactly("net/minecraft/world/level/Level", "m_46700_", "()V");

        List<String> fieldTokens = scanner.parseSignatureTokens("Lnet/minecraft/world/entity/Entity;f_19853_:Lnet/minecraft/world/level/Level;");
        assertThat(fieldTokens).containsExactly("net/minecraft/world/entity/Entity", "f_19853_", "Lnet/minecraft/world/level/Level;");
    }

    @Test
    @DisplayName("Verify RefmapVstmBatchTranslator remaps obfuscated method names to MojMap")
    void verifyBatchTranslator() {
        RefmapMethodDescriptorScanner scanner = new RefmapMethodDescriptorScanner();
        RefmapVstmBatchTranslator translator = new RefmapVstmBatchTranslator(scanner);

        translator.registerOverride("m_46700_", "isClientSide");

        String remapped = translator.translateSignature("Lnet/minecraft/world/level/Level;m_46700_()Z");
        assertThat(remapped).isEqualTo("Lnet/minecraft/world/level/Level;isClientSide()Z");
    }

    @Test
    @DisplayName("Verify RefmapDiskCacheCoordinator persists and retrieves remapped JSON by SHA-1")
    void verifyDiskCacheCoordinator(@TempDir Path tempDir) {
        RefmapDiskCacheCoordinator cache = new RefmapDiskCacheCoordinator(tempDir);

        String sampleJson = "{\"mappings\": {\"Lnet/minecraft/Level;\": \"isClientSide\"}}";
        cache.writeCachedRefmap("abc123sha1", sampleJson);

        assertThat(cache.readCachedRefmap("abc123sha1")).contains(sampleJson);
        assertThat(cache.readCachedRefmap("nonexistent")).isEmpty();
    }
}

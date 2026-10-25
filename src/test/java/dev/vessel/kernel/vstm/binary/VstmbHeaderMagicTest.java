package dev.vessel.kernel.vstm.binary;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Unit tests validating Magic Header integrity, Format Version negotiation,
 * and 64-byte Cache Line alignment rules.
 *
 * @version 1.0.0
 * @since 1.0.0 (Wave 1)
 */
public final class VstmbHeaderMagicTest {

    @Test
    @DisplayName("Verify valid VSTM magic header constant and bytes match ASCII 'VSTM'")
    void testValidMagicHeader() {
        assertThat(VstmbHeaderMagic.MAGIC_INT).isEqualTo(0x5653544D);
        assertThat(VstmbHeaderMagic.MAGIC_BYTES).containsExactly(0x56, 0x53, 0x54, 0x4D);

        assertThat(VstmbHeaderMagic.isValid(0x5653544D)).isTrue();
        assertThat(VstmbHeaderMagic.isValid(new byte[]{0x56, 0x53, 0x54, 0x4D})).isTrue();
        assertThat(VstmbHeaderMagic.isValid(new byte[]{0x56, 0x53, 0x54, 0x4D, 0x00, 0x01})).isTrue();

        assertThatCode(() -> VstmbHeaderMagic.validate(0x5653544D)).doesNotThrowAnyException();
        assertThatCode(() -> VstmbHeaderMagic.validate(new byte[]{0x56, 0x53, 0x54, 0x4D})).doesNotThrowAnyException();
    }

    @Test
    @DisplayName("Verify rejection of corrupted or alien magic headers with domain exception")
    void testCorruptedMagicHeaderRejection() {
        assertThat(VstmbHeaderMagic.isValid(0x12345678)).isFalse();
        assertThat(VstmbHeaderMagic.isValid(new byte[]{0x00, 0x00, 0x00, 0x00})).isFalse();
        assertThat(VstmbHeaderMagic.isValid((byte[]) null)).isFalse();
        assertThat(VstmbHeaderMagic.isValid(new byte[]{0x56, 0x53})).isFalse();

        assertThatThrownBy(() -> VstmbHeaderMagic.validate(0xDEADBEEF))
                .isInstanceOf(VstmbBinaryCorruptException.class)
                .hasMessageContaining("Invalid .vstmb magic header")
                .satisfies(e -> assertThat(((VstmbBinaryCorruptException) e).getErrorCode()).isEqualTo("ERR-VSTM-MAGIC"));

        assertThatThrownBy(() -> VstmbHeaderMagic.validate(new byte[]{'Z', 'I', 'P', '!' }))
                .isInstanceOf(VstmbBinaryCorruptException.class)
                .hasMessageContaining("Invalid .vstmb magic bytes header");
    }

    @Test
    @DisplayName("Verify format version 1.0 support and rejection of out-of-range versions")
    void testFormatVersionValidation() {
        assertThat(VstmbFormatVersion.CURRENT_VERSION).isEqualTo((short) 1);
        assertThat(VstmbFormatVersion.isSupported((short) 1)).isTrue();
        assertThatCode(() -> VstmbFormatVersion.validate((short) 1)).doesNotThrowAnyException();

        assertThat(VstmbFormatVersion.isSupported((short) 0)).isFalse();
        assertThat(VstmbFormatVersion.isSupported((short) 2)).isFalse();

        assertThatThrownBy(() -> VstmbFormatVersion.validate((short) 99))
                .isInstanceOf(VstmbBinaryCorruptException.class)
                .hasMessageContaining("Unsupported .vstmb format version")
                .satisfies(e -> assertThat(((VstmbBinaryCorruptException) e).getErrorCode()).isEqualTo("ERR-VSTM-VERSION"));
    }

    @Test
    @DisplayName("Verify 64-byte cache line alignment and padding calculation")
    void testCacheLineAligner() {
        assertThat(VstmbCacheLineAligner.isAligned64(0L)).isTrue();
        assertThat(VstmbCacheLineAligner.isAligned64(64L)).isTrue();
        assertThat(VstmbCacheLineAligner.isAligned64(128L)).isTrue();
        assertThat(VstmbCacheLineAligner.isAligned64(63L)).isFalse();
        assertThat(VstmbCacheLineAligner.isAligned64(65L)).isFalse();

        assertThat(VstmbCacheLineAligner.align64(0L)).isEqualTo(0L);
        assertThat(VstmbCacheLineAligner.align64(1L)).isEqualTo(64L);
        assertThat(VstmbCacheLineAligner.align64(63L)).isEqualTo(64L);
        assertThat(VstmbCacheLineAligner.align64(64L)).isEqualTo(64L);
        assertThat(VstmbCacheLineAligner.align64(65L)).isEqualTo(128L);

        assertThat(VstmbCacheLineAligner.paddingBytesNeeded(64L)).isEqualTo(0);
        assertThat(VstmbCacheLineAligner.paddingBytesNeeded(60L)).isEqualTo(4);
        assertThat(VstmbCacheLineAligner.paddingBytesNeeded(1L)).isEqualTo(63);
    }
}

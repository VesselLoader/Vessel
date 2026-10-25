package dev.vessel.kernel.identity;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.Random;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Unit test suite for {@link FastUuidHexFormatter}.
 *
 * @version 1.0.0
 * @author VESSEL Engine Team
 */
class FastUuidHexFormatterTest {

    @Test
    @DisplayName("Formats standard dashed UUID string identically to JDK UUID.toString()")
    void testFormatDashedMatchesJdk() {
        UUID uuid = UUID.randomUUID();
        String formatted = FastUuidHexFormatter.format(uuid);
        assertThat(formatted).isEqualTo(uuid.toString());
    }

    @Test
    @DisplayName("Formats undashed 32-character hex string correctly")
    void testFormatUndashed() {
        UUID uuid = UUID.fromString("01234567-89ab-cdef-0123-456789abcdef");
        String undashed = FastUuidHexFormatter.formatUndashed(uuid);
        assertThat(undashed).isEqualTo("0123456789abcdef0123456789abcdef");
    }

    @Test
    @DisplayName("Zero-allocation formatTo into primitive char array matches standard format")
    void testFormatToCharArray() {
        UUID uuid = UUID.randomUUID();
        char[] buffer = new char[FastUuidHexFormatter.DASHED_LENGTH];
        FastUuidHexFormatter.formatTo(uuid, buffer, 0);

        assertThat(new String(buffer)).isEqualTo(uuid.toString());
    }

    @Test
    @DisplayName("Zero-allocation formatUndashedTo into primitive char array")
    void testFormatUndashedToCharArray() {
        UUID uuid = UUID.fromString("11223344-5566-7788-9900-aabbccddeeff");
        char[] buffer = new char[FastUuidHexFormatter.UNDASHED_LENGTH];
        FastUuidHexFormatter.formatUndashedTo(uuid, buffer, 0);

        assertThat(new String(buffer)).isEqualTo("11223344556677889900aabbccddeeff");
    }

    @Test
    @DisplayName("Parses both dashed and undashed strings back into identical UUID")
    void testParseDashedAndUndashed() {
        UUID original = UUID.randomUUID();
        String dashed = original.toString();
        String undashed = dashed.replace("-", "");

        UUID parsedFromDashed = FastUuidHexFormatter.parse(dashed);
        UUID parsedFromUndashed = FastUuidHexFormatter.parse(undashed);

        assertThat(parsedFromDashed).isEqualTo(original);
        assertThat(parsedFromUndashed).isEqualTo(original);
    }

    @Test
    @DisplayName("Parses from char array slice without heap string allocation")
    void testParseFromCharArray() {
        UUID original = UUID.randomUUID();
        char[] buffer = original.toString().toCharArray();

        UUID parsed = FastUuidHexFormatter.parse(buffer, 0, buffer.length);
        assertThat(parsed).isEqualTo(original);
    }

    @Test
    @DisplayName("Validates input and throws IllegalArgumentException on malformed UUID strings")
    void testInvalidUuidParsing() {
        // Wrong length
        assertThatThrownBy(() -> FastUuidHexFormatter.parse("short-uuid"))
                .isInstanceOf(IllegalArgumentException.class);

        // Missing hyphens
        assertThatThrownBy(() -> FastUuidHexFormatter.parse("01234567+89ab+cdef+0123+456789abcdef"))
                .isInstanceOf(IllegalArgumentException.class);

        // Non-hex character
        assertThatThrownBy(() -> FastUuidHexFormatter.parse("01234567-89ab-cdef-0123-456789abcdeg"))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    @DisplayName("Round-trip fuzz test across 250 randomized UUID values")
    void testFuzzRoundTrip() {
        Random random = new Random(42);
        char[] buffer = new char[FastUuidHexFormatter.DASHED_LENGTH];

        for (int i = 0; i < 250; i++) {
            long mostSig = random.nextLong();
            long leastSig = random.nextLong();
            UUID original = new UUID(mostSig, leastSig);

            FastUuidHexFormatter.formatTo(mostSig, leastSig, buffer, 0);
            UUID parsed = FastUuidHexFormatter.parse(buffer, 0, buffer.length);

            assertThat(parsed).isEqualTo(original);
        }
    }
}

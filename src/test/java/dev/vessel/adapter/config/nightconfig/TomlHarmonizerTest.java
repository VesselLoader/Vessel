package dev.vessel.adapter.config.nightconfig;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.lang.invoke.MethodHandle;
import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Verification test suite for {@link NightConfigMethodHandleHarmonizer}, {@link TomlConfigSpecValidator},
 * and {@link FastMemoryTomlParser}.
 *
 * <p>Strictly adheres to Rule 17: Single Responsibility, zero inner, nested, or anonymous classes.</p>
 */
public final class TomlHarmonizerTest {

    @Test
    @DisplayName("Verify NightConfigMethodHandleHarmonizer safely resolves method handles")
    void verifyMethodHandleHarmonizer() throws Throwable {
        NightConfigMethodHandleHarmonizer harmonizer = new NightConfigMethodHandleHarmonizer();

        Optional<MethodHandle> handle = harmonizer.findHandle(String.class, "length", int.class);
        assertThat(handle).isPresent();

        int len = (int) handle.get().invokeExact("VESSEL");
        assertThat(len).isEqualTo(6);

        Optional<MethodHandle> nonExistent = harmonizer.findHandle(String.class, "fakeMethod", void.class);
        assertThat(nonExistent).isEmpty();
    }

    @Test
    @DisplayName("Verify TomlConfigSpecValidator sanitizes malformed trailing commas")
    void verifyTomlSanitizer() {
        TomlConfigSpecValidator validator = new TomlConfigSpecValidator();

        String rawToml = """
                [general]
                items = ["diamond", "emerald",]
                # comment line
                port = 25565
                """;

        String clean = validator.sanitizeToml(rawToml);
        assertThat(clean).doesNotContain(",]");
        assertThat(clean).contains("items = [\"diamond\", \"emerald\"]");
    }

    @Test
    @DisplayName("Verify FastMemoryTomlParser parses key value pairs into map")
    void verifyFastMemoryTomlParser() {
        TomlConfigSpecValidator validator = new TomlConfigSpecValidator();
        FastMemoryTomlParser parser = new FastMemoryTomlParser(validator);

        String sampleToml = """
                [server]
                motd = "Welcome to VESSEL"
                max_players = 100
                [energy]
                fe_per_tick = 500
                """;

        Map<String, String> parsed = parser.parseSimpleKeyValuePairs(sampleToml);
        assertThat(parsed.get("server.motd")).isEqualTo("Welcome to VESSEL");
        assertThat(parsed.get("server.max_players")).isEqualTo("100");
        assertThat(parsed.get("energy.fe_per_tick")).isEqualTo("500");
    }
}

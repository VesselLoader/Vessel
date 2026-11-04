package dev.vessel.kernel.testmatrix;

import java.util.List;
import java.util.Objects;
import java.util.regex.Pattern;

/**
 * Validates and asserts that a specific expected token or regular expression pattern
 * is present within console output lines emitted during live command execution.
 *
 * <p>Strictly adheres to Rule 17 with zero inner, nested, or anonymous classes.</p>
 *
 * @version 1.0.0
 */
public final class ConsoleCommandOutputAssertionEngine {

    public ConsoleCommandOutputAssertionEngine() {
    }

    /**
     * Asserts whether any line in the output collection contains the specified substring.
     *
     * @param lines          emitted console output lines
     * @param expectedToken  substring to find
     * @return {@code true} if matching
     */
    public boolean assertContains(List<String> lines, String expectedToken) {
        Objects.requireNonNull(expectedToken, "expectedToken must not be null");
        if (lines == null || lines.isEmpty()) {
            return false;
        }
        for (String line : lines) {
            if (line != null && line.contains(expectedToken)) {
                return true;
            }
        }
        return false;
    }

    /**
     * Asserts whether any line matches the given regular expression pattern.
     *
     * @param lines        emitted console output lines
     * @param regexPattern regular expression string
     * @return {@code true} if matching
     */
    public boolean assertMatchesRegex(List<String> lines, String regexPattern) {
        Objects.requireNonNull(regexPattern, "regexPattern must not be null");
        if (lines == null || lines.isEmpty()) {
            return false;
        }
        Pattern pattern = Pattern.compile(regexPattern);
        for (String line : lines) {
            if (line != null && pattern.matcher(line).find()) {
                return true;
            }
        }
        return false;
    }
}

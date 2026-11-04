package dev.vessel.kernel.logging;

import java.io.OutputStream;
import java.io.PrintStream;
import java.util.Objects;

/**
 * Custom {@link PrintStream} interceptor that intercepts raw console lines, sanitizes legacy
 * Minecraft codes, and formats lines according to strict tri-color (Red, Yellow, White) ANSI rules.
 *
 * <p>Strictly adheres to Rule 17 with zero inner, nested, or anonymous classes.</p>
 *
 * @version 1.0.0
 */
public final class VesselConsoleOutputStreamInterceptor extends PrintStream {

    private final PrintStream delegateStream;
    private final boolean isErrorStream;
    private final VesselConsoleLogColorSanitizer sanitizer = new VesselConsoleLogColorSanitizer();
    private final VesselTriColorLogFormatter formatter = new VesselTriColorLogFormatter();

    public VesselConsoleOutputStreamInterceptor(PrintStream delegateStream, boolean isErrorStream) {
        super(Objects.requireNonNull(delegateStream, "delegateStream must not be null"), true);
        this.delegateStream = delegateStream;
        this.isErrorStream = isErrorStream;
    }

    @Override
    public void println(String line) {
        if (line == null) {
            delegateStream.println();
            return;
        }
        String clean = sanitizer.sanitize(line);
        String triColored = formatter.formatTriColor(clean, isErrorStream);
        delegateStream.println(triColored);
    }

    @Override
    public void print(String text) {
        if (text == null) {
            delegateStream.print("null");
            return;
        }
        if (text.contains("\n")) {
            String clean = sanitizer.sanitize(text);
            String triColored = formatter.formatTriColor(clean, isErrorStream);
            delegateStream.print(triColored);
        } else {
            delegateStream.print(text);
        }
    }

    @Override
    public void println(Object obj) {
        println(obj != null ? obj.toString() : "null");
    }

    @Override
    public void print(Object obj) {
        print(obj != null ? obj.toString() : "null");
    }

    @Override
    public void flush() {
        super.flush();
        delegateStream.flush();
    }
}

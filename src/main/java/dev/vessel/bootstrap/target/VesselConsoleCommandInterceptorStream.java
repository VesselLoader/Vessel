package dev.vessel.bootstrap.target;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.Locale;
import java.util.Objects;

/**
 * Intercepts server console input on {@link System#in} before Vanilla Minecraft's Brigadier
 * command handler reads it, executing VESSEL's sovereign administrative commands
 * ({@code /vessel}, {@code vessel <subcommand>}, {@code cls}, {@code clear}) directly against
 * {@link VesselServerLauncher} while forwarding all standard Minecraft commands ({@code say},
 * {@code list}, {@code help}, {@code stop}, {@code op}, etc.) transparently to the underlying
 * server console reader thread.
 *
 * <p>Strictly adheres to Rule 17 with zero inner, nested, or anonymous classes.</p>
 *
 * @version 1.0.0
 */
public final class VesselConsoleCommandInterceptorStream extends InputStream {

    private final InputStream delegateStream;
    private final VesselServerLauncher serverLauncher;
    private byte[] pendingPassthroughBuffer = new byte[0];
    private int pendingOffset = 0;
    private boolean endOfStreamReached = false;

    /**
     * Constructs a console command interceptor wrapping {@code delegateStream}.
     *
     * @param delegateStream original console input stream (typically {@link System#in})
     * @param serverLauncher active {@link VesselServerLauncher} instance
     */
    public VesselConsoleCommandInterceptorStream(
            InputStream delegateStream,
            VesselServerLauncher serverLauncher) {
        this.delegateStream = Objects.requireNonNull(delegateStream, "delegateStream must not be null");
        this.serverLauncher = Objects.requireNonNull(serverLauncher, "serverLauncher must not be null");
    }

    /**
     * Returns the underlying wrapped {@link InputStream}.
     *
     * @return wrapped delegate input stream
     */
    public InputStream getDelegateStream() {
        return delegateStream;
    }

    /**
     * Checks whether a raw console command line is a VESSEL sovereign command or console clear
     * instruction that should be intercepted locally rather than sent to Vanilla Brigadier.
     *
     * @param rawLine raw console line typed by the administrator
     * @return {@code true} if intercepted by VESSEL; {@code false} if it should pass through to Minecraft
     */
    public static boolean isVesselInterceptedLine(String rawLine) {
        if (rawLine == null) {
            return false;
        }
        String trimmed = rawLine.trim().toLowerCase(Locale.ROOT);
        if (trimmed.isEmpty()) {
            return false;
        }
        if ("cls".equals(trimmed) || "clear".equals(trimmed)) {
            return true;
        }
        if ("vessel".equals(trimmed) || "/vessel".equals(trimmed)) {
            return true;
        }
        return trimmed.startsWith("vessel ") || trimmed.startsWith("/vessel ");
    }

    @Override
    public synchronized int read() throws IOException {
        if (!ensurePendingBytesAvailable()) {
            return -1;
        }
        return pendingPassthroughBuffer[pendingOffset++] & 0xFF;
    }

    @Override
    public synchronized int read(byte[] b, int off, int len) throws IOException {
        Objects.checkFromIndexSize(off, len, b.length);
        if (len == 0) {
            return 0;
        }
        if (!ensurePendingBytesAvailable()) {
            return -1;
        }
        int available = pendingPassthroughBuffer.length - pendingOffset;
        int toCopy = Math.min(len, available);
        System.arraycopy(pendingPassthroughBuffer, pendingOffset, b, off, toCopy);
        pendingOffset += toCopy;
        return toCopy;
    }

    private boolean ensurePendingBytesAvailable() throws IOException {
        while (pendingOffset >= pendingPassthroughBuffer.length) {
            if (endOfStreamReached) {
                return false;
            }
            ByteArrayOutputStream lineBuffer = new ByteArrayOutputStream(128);
            int ch;
            while ((ch = delegateStream.read()) != -1) {
                lineBuffer.write(ch);
                if (ch == '\n') {
                    break;
                }
            }
            if (ch == -1 && lineBuffer.size() == 0) {
                endOfStreamReached = true;
                return false;
            }
            byte[] rawBytes = lineBuffer.toByteArray();
            String decodedLine = new String(rawBytes, StandardCharsets.UTF_8);
            if (isVesselInterceptedLine(decodedLine)) {
                serverLauncher.executeVesselConsoleCommand(decodedLine);
                if (ch == -1) {
                    endOfStreamReached = true;
                    return false;
                }
                continue;
            }
            pendingPassthroughBuffer = rawBytes;
            pendingOffset = 0;
            if (ch == -1) {
                endOfStreamReached = true;
            }
            return true;
        }
        return true;
    }
}

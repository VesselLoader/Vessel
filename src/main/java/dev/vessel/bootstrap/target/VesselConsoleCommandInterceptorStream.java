package dev.vessel.bootstrap.target;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.Objects;

/**
 * Intercepts server console input on {@link System#in} before Vanilla Minecraft's Brigadier
 * command handler reads it, executing VESSEL's sovereign administrative commands and registered
 * Bukkit plugin commands directly while forwarding standard Minecraft commands transparently
 * to the underlying server console reader thread.
 *
 * <p>Strictly adheres to Rule 17 with zero inner, nested, or anonymous classes.</p>
 *
 * @version 1.0.5
 */
public final class VesselConsoleCommandInterceptorStream extends InputStream {

    private static final VesselConsoleCommandRouteResolver ROUTE_RESOLVER =
            new VesselConsoleCommandRouteResolver();

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
     * Checks whether a raw console command line is a VESSEL sovereign command, Bukkit plugin command,
     * or console clear instruction that should be intercepted locally rather than sent to Vanilla Brigadier.
     *
     * @param rawLine raw console line typed by the administrator
     * @return {@code true} if intercepted by VESSEL; {@code false} if it should pass through to Minecraft
     */
    public static boolean isVesselInterceptedLine(String rawLine) {
        return ROUTE_RESOLVER.shouldIntercept(rawLine);
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
            if (rawBytes.length >= 3
                    && (rawBytes[0] & 0xFF) == 0xEF
                    && (rawBytes[1] & 0xFF) == 0xBB
                    && (rawBytes[2] & 0xFF) == 0xBF) {
                byte[] stripped = new byte[rawBytes.length - 3];
                System.arraycopy(rawBytes, 3, stripped, 0, stripped.length);
                rawBytes = stripped;
            }
            String decodedLine = new String(rawBytes, StandardCharsets.UTF_8).replace("\uFEFF", "");
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

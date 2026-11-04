package dev.vessel.kernel.testmatrix;

import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.nio.charset.StandardCharsets;

/**
 * Lightweight standalone simulated bot client performing canonical Minecraft Java protocol
 * handshake and status query (Server List Ping) against the live server on TCP port 25565.
 *
 * <p>Strictly adheres to Rule 17 with zero inner, nested, or anonymous classes.</p>
 *
 * @version 1.0.0
 */
public final class LiveServerStatusPingBot {

    public LiveServerStatusPingBot() {
    }

    /**
     * Executes a Minecraft Java protocol Server List Ping against the target host and port.
     *
     * @param host      target hostname or IP (e.g. "127.0.0.1")
     * @param port      target TCP port (e.g. 25565)
     * @param timeoutMs connection and read timeout in milliseconds
     * @return {@code true} if server responded with valid status JSON
     */
    public boolean pingServer(String host, int port, int timeoutMs) {
        String effectiveHost = (host == null || host.isBlank()) ? "127.0.0.1" : host;
        int effectivePort = port <= 0 ? 25565 : port;
        int effectiveTimeout = timeoutMs <= 0 ? 5000 : timeoutMs;

        System.out.println("[BOT] Connecting to live server at " + effectiveHost + ":" + effectivePort + "...");
        try (Socket socket = new Socket()) {
            socket.connect(new InetSocketAddress(effectiveHost, effectivePort), effectiveTimeout);
            socket.setSoTimeout(effectiveTimeout);

            OutputStream out = socket.getOutputStream();
            InputStream in = socket.getInputStream();
            DataInputStream dataIn = new DataInputStream(in);

            // 1. Send Handshake packet (Packet ID 0x00, Next state = 1)
            ByteArrayOutputStream handshakePayload = new ByteArrayOutputStream();
            writeVarInt(handshakePayload, 0x00); // Packet ID
            writeVarInt(handshakePayload, 763);  // Protocol version
            writeString(handshakePayload, effectiveHost);
            handshakePayload.write((effectivePort >> 8) & 0xFF);
            handshakePayload.write(effectivePort & 0xFF);
            writeVarInt(handshakePayload, 1);    // Next state: 1 (Status)

            byte[] handshakeBytes = handshakePayload.toByteArray();
            writeVarInt(out, handshakeBytes.length);
            out.write(handshakeBytes);
            out.flush();

            // 2. Send Status Request packet (Length 1, Packet ID 0x00)
            writeVarInt(out, 1);
            writeVarInt(out, 0x00);
            out.flush();

            // 3. Read Status Response packet
            int packetLength = readVarInt(dataIn);
            if (packetLength <= 0) {
                System.err.println("[BOT] Received invalid packet length: " + packetLength);
                return false;
            }

            int packetId = readVarInt(dataIn);
            if (packetId != 0x00) {
                System.err.println("[BOT] Received unexpected packet ID: " + packetId);
                return false;
            }

            int jsonLength = readVarInt(dataIn);
            byte[] jsonBytes = dataIn.readNBytes(jsonLength);
            String responseJson = new String(jsonBytes, StandardCharsets.UTF_8);

            System.out.println("[BOT] Successfully received live status response from server (" + jsonLength + " bytes):");
            System.out.println("[BOT] >>> " + responseJson);
            return true;
        } catch (Throwable t) {
            System.err.println("[BOT] Failed to ping server: " + t.getMessage());
            return false;
        }
    }

    private static void writeVarInt(OutputStream out, int value) throws java.io.IOException {
        int v = value;
        while ((v & 0xFFFFFF80) != 0) {
            out.write((v & 0x7F) | 0x80);
            v >>>= 7;
        }
        out.write(v & 0x7F);
    }

    private static int readVarInt(DataInputStream in) throws java.io.IOException {
        int numRead = 0;
        int result = 0;
        byte read;
        do {
            read = in.readByte();
            int value = (read & 0x7F);
            result |= (value << (7 * numRead));
            numRead++;
            if (numRead > 5) {
                throw new java.io.IOException("VarInt is too big");
            }
        } while ((read & 0x80) != 0);
        return result;
    }

    private static void writeString(OutputStream out, String string) throws java.io.IOException {
        byte[] bytes = string.getBytes(StandardCharsets.UTF_8);
        writeVarInt(out, bytes.length);
        out.write(bytes);
    }

    /**
     * Standalone main runner for bot ping validation.
     *
     * @param args optional args: [host] [port] [timeoutMs]
     */
    public static void main(String[] args) {
        String host = args.length > 0 ? args[0] : "127.0.0.1";
        int port = args.length > 1 ? Integer.parseInt(args[1]) : 25565;
        int timeout = args.length > 2 ? Integer.parseInt(args[2]) : 5000;

        LiveServerStatusPingBot bot = new LiveServerStatusPingBot();
        boolean ok = bot.pingServer(host, port, timeout);
        if (ok) {
            System.out.println("[BOT] Live server handshake test PASSED!");
            System.exit(0);
        } else {
            System.err.println("[BOT] Live server handshake test FAILED!");
            System.exit(1);
        }
    }
}

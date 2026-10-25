package dev.vessel.kernel.network.protocol;

/**
 * Thrown when a variable-length integer (VarInt or VarLong) exceeds the protocol byte limit,
 * defending against malicious packet floods designed to crash Netty event loops.
 *
 * @version 1.0.0
 * @author VESSEL Engine Team
 */
public final class VarIntOverflowException extends RuntimeException {

    private static final long serialVersionUID = 1L;

    private final int bytesRead;
    private final int maxAllowedBytes;

    /**
     * Constructs a VarIntOverflowException with byte count diagnostic details.
     *
     * @param message         error message
     * @param bytesRead       the count of bytes consumed when overflow occurred
     * @param maxAllowedBytes the maximum protocol limit (5 for VarInt, 10 for VarLong)
     */
    public VarIntOverflowException(String message, int bytesRead, int maxAllowedBytes) {
        super(message);
        this.bytesRead = bytesRead;
        this.maxAllowedBytes = maxAllowedBytes;
    }

    /**
     * Returns the number of bytes read before overflow was detected.
     *
     * @return bytes read
     */
    public int getBytesRead() {
        return bytesRead;
    }

    /**
     * Returns the maximum permitted byte length.
     *
     * @return max allowed bytes
     */
    public int getMaxAllowedBytes() {
        return maxAllowedBytes;
    }
}

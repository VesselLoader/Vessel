package dev.vessel.kernel.vstm.parser;

/**
 * Domain exception thrown when parsing upstream mapping dialect definitions fails.
 *
 * <p>Captures the active dialect identifier, line number, and offending raw line
 * content to pinpoint syntax corruptions without compromising memory safety.</p>
 *
 * @version 1.0.0
 * @since 1.0.0 (Wave 2)
 */
public class UpstreamParserException extends RuntimeException {

    private static final long serialVersionUID = 1L;

    private final String dialect;
    private final int lineNumber;
    private final String lineContent;

    /**
     * Constructs a generic parser exception with an explanatory message.
     *
     * @param message human-readable cause of failure
     */
    public UpstreamParserException(String message) {
        super(message);
        this.dialect = "UNKNOWN";
        this.lineNumber = -1;
        this.lineContent = "";
    }

    /**
     * Constructs a generic parser exception with a message and underlying cause.
     *
     * @param message human-readable cause of failure
     * @param cause   underlying exception
     */
    public UpstreamParserException(String message, Throwable cause) {
        super(message, cause);
        this.dialect = "UNKNOWN";
        this.lineNumber = -1;
        this.lineContent = "";
    }

    /**
     * Constructs a parser exception with complete dialect parsing context.
     *
     * @param dialect     the active dialect name (e.g. "MojMap", "TinyV2")
     * @param lineNumber  1-based index of the line where corruption occurred
     * @param lineContent raw text content of the corrupted line
     * @param message     human-readable error description
     */
    public UpstreamParserException(String dialect, int lineNumber, String lineContent, String message) {
        super(formatMessage(dialect, lineNumber, lineContent, message));
        this.dialect = dialect != null ? dialect : "UNKNOWN";
        this.lineNumber = lineNumber;
        this.lineContent = lineContent != null ? lineContent : "";
    }

    /**
     * Constructs a parser exception with complete dialect context and root cause.
     *
     * @param dialect     the active dialect name
     * @param lineNumber  1-based index of the line
     * @param lineContent raw text content of the corrupted line
     * @param message     human-readable error description
     * @param cause       underlying exception
     */
    public UpstreamParserException(String dialect, int lineNumber, String lineContent, String message, Throwable cause) {
        super(formatMessage(dialect, lineNumber, lineContent, message), cause);
        this.dialect = dialect != null ? dialect : "UNKNOWN";
        this.lineNumber = lineNumber;
        this.lineContent = lineContent != null ? lineContent : "";
    }

    private static String formatMessage(String dialect, int lineNumber, String lineContent, String message) {
        return "[" + dialect + ":" + lineNumber + "] " + message + (lineContent.isEmpty() ? "" : " (line: '" + lineContent + "')");
    }

    public String getDialect() {
        return dialect;
    }

    public int getLineNumber() {
        return lineNumber;
    }

    public String getLineContent() {
        return lineContent;
    }
}

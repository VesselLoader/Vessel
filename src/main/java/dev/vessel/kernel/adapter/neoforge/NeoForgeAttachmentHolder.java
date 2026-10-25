package dev.vessel.kernel.adapter.neoforge;

/**
 * Interface implemented by entities, chunks, and world objects that hold NeoForge data attachments.
 *
 * @version 1.0.0
 * @author VESSEL Engine Team
 */
public interface NeoForgeAttachmentHolder {

    /**
     * Unique monotonic or persistent holder identifier (e.g. Entity UUID or Chunk Coord string).
     *
     * @return holder unique ID
     */
    String getAttachmentHolderId();
}

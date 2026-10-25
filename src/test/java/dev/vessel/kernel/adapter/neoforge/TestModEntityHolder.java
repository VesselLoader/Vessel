package dev.vessel.kernel.adapter.neoforge;

import java.util.Objects;

/**
 * Concrete test fixture representing a Minecraft Entity or Chunk for NeoForge attachment host tests.
 *
 * @version 1.0.0
 * @author VESSEL Engine Team
 */
public final class TestModEntityHolder implements NeoForgeAttachmentHolder {

    private final String holderId;

    /**
     * Constructs the test fixture with an explicit ID.
     *
     * @param holderId unique ID
     */
    public TestModEntityHolder(String holderId) {
        this.holderId = Objects.requireNonNull(holderId, "holderId must not be null");
    }

    @Override
    public String getAttachmentHolderId() {
        return holderId;
    }
}

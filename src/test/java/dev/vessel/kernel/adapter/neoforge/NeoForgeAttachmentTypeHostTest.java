package dev.vessel.kernel.adapter.neoforge;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Unit tests verifying NeoForge Data Attachment host operations, fallback default suppliers,
 * and leak prevention via explicit holder lifecycle eviction.
 *
 * @version 1.0.0
 * @author VESSEL Engine Team
 */
public final class NeoForgeAttachmentTypeHostTest {

    private NeoForgeAttachmentTypeHost attachmentHost;

    @BeforeEach
    public void setUp() {
        attachmentHost = new NeoForgeAttachmentTypeHost();
    }

    @Test
    @DisplayName("NeoForge Attachment Host - Stores and retrieves typed attachment data")
    public void testSetAndGetData() {
        NeoForgeAttachmentKey<Integer> manaKey = new NeoForgeAttachmentKey<>(
                "magicmod:mana",
                Integer.class,
                () -> 100
        );
        attachmentHost.registerAttachmentType(manaKey);

        TestModEntityHolder player = new TestModEntityHolder("player-uuid-001");

        // Initially uninitialized, falls back to default 100
        Integer initialMana = attachmentHost.getData(player, manaKey);
        assertThat(initialMana).isEqualTo(100);

        // Update mana
        attachmentHost.setData(player, manaKey, 250);
        assertThat(attachmentHost.getData(player, manaKey)).isEqualTo(250);
        assertThat(attachmentHost.hasData(player, manaKey)).isTrue();

        // Remove mana
        Integer removed = attachmentHost.removeData(player, manaKey);
        assertThat(removed).isEqualTo(250);
        assertThat(attachmentHost.hasData(player, manaKey)).isFalse();
    }

    @Test
    @DisplayName("NeoForge Attachment Host - Rejects type mismatched assignment with ERR-99")
    @SuppressWarnings({"rawtypes", "unchecked"})
    public void testTypeMismatchRejection() {
        NeoForgeAttachmentKey<String> nameKey = new NeoForgeAttachmentKey<>(
                "mod:custom_name",
                String.class,
                null
        );
        attachmentHost.registerAttachmentType(nameKey);

        TestModEntityHolder chunk = new TestModEntityHolder("chunk-x0-z0");

        // Storing invalid raw object manually in container to test type safety guard
        NeoForgeAttachmentContainer container = new NeoForgeAttachmentContainer(chunk.getAttachmentHolderId());
        container.set("mod:custom_name", 12345); // Integer instead of String

        // Manually place in host to trigger getData validation
        // Using reflect or verifying setData rejection
        assertThatThrownBy(() -> {
            // Unchecked cast trigger
            NeoForgeAttachmentKey rawKey = nameKey;
            attachmentHost.setData(chunk, rawKey, 12345);
        }).isInstanceOf(NeoForgeAttachmentException.class)
                .hasMessageContaining("[ERR-99]")
                .hasMessageContaining("is not an instance of");
    }

    @Test
    @DisplayName("NeoForge Attachment Host - Evicts holder containers upon chunk/entity unload (ERR-99 mitigation)")
    public void testHolderUnloadEviction() {
        NeoForgeAttachmentKey<String> markerKey = new NeoForgeAttachmentKey<>(
                "mod:marker",
                String.class,
                null
        );
        attachmentHost.registerAttachmentType(markerKey);

        TestModEntityHolder entity1 = new TestModEntityHolder("entity-1");
        TestModEntityHolder entity2 = new TestModEntityHolder("entity-2");

        attachmentHost.setData(entity1, markerKey, "value1");
        attachmentHost.setData(entity2, markerKey, "value2");

        assertThat(attachmentHost.getTrackedHolderCount()).isEqualTo(2);

        // Evict entity1
        attachmentHost.releaseHolder(entity1);
        assertThat(attachmentHost.getTrackedHolderCount()).isEqualTo(1);
        assertThat(attachmentHost.hasData(entity1, markerKey)).isFalse();
        assertThat(attachmentHost.hasData(entity2, markerKey)).isTrue();

        // Evict entity2 by ID
        attachmentHost.releaseHolderById("entity-2");
        assertThat(attachmentHost.getTrackedHolderCount()).isEqualTo(0);
    }
}

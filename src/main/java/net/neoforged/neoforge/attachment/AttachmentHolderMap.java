package net.neoforged.neoforge.attachment;

import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Thread-safe {@link IAttachmentHolder} backing store that manages NeoForge {@link AttachmentType}
 * instances in a concurrent map.
 *
 * <p>Strictly adheres to Rule 17 with zero inner, nested, or anonymous classes.</p>
 *
 * @version 1.0.0
 */
public final class AttachmentHolderMap implements IAttachmentHolder {

    private final Map<AttachmentType<?>, Object> attachments = new ConcurrentHashMap<>();

    /**
     * Constructs an empty {@code AttachmentHolderMap}.
     */
    public AttachmentHolderMap() {
    }

    @Override
    public <T> boolean hasData(AttachmentType<T> type) {
        return attachments.containsKey(Objects.requireNonNull(type, "type must not be null"));
    }

    @Override
    @SuppressWarnings("unchecked")
    public <T> T getData(AttachmentType<T> type) {
        Objects.requireNonNull(type, "type must not be null");
        Object existing = attachments.get(type);
        if (existing != null) {
            return (T) existing;
        }
        T created = Objects.requireNonNull(
                type.createDefaultValue(), "default attachment value must not be null");
        attachments.put(type, created);
        return created;
    }

    @Override
    public <T> T setData(AttachmentType<T> type, T data) {
        Objects.requireNonNull(type, "type must not be null");
        Objects.requireNonNull(data, "data must not be null");
        attachments.put(type, data);
        return data;
    }

    @Override
    @SuppressWarnings("unchecked")
    public <T> T removeData(AttachmentType<T> type) {
        Objects.requireNonNull(type, "type must not be null");
        return (T) attachments.remove(type);
    }

    /**
     * Returns the number of currently bound attachments in this holder.
     *
     * @return bound attachment count
     */
    public int size() {
        return attachments.size();
    }
}

package net.neoforged.neoforge.attachment;

/**
 * Contract for NeoForge objects capable of holding typed {@link AttachmentType} data instances
 * ({@code net.neoforged.neoforge.attachment.IAttachmentHolder}).
 *
 * <p>Strictly adheres to Rule 17 with zero inner, nested, or anonymous classes.</p>
 *
 * @version 1.0.0
 */
public interface IAttachmentHolder {

    /**
     * Checks whether data is currently present for the specified {@link AttachmentType}.
     *
     * @param type attachment type descriptor
     * @param <T>  attached data value type
     * @return {@code true} if an attachment value is bound
     */
    <T> boolean hasData(AttachmentType<T> type);

    /**
     * Returns the data bound to the specified {@link AttachmentType}, computing and binding its
     * default value if not yet present.
     *
     * @param type attachment type descriptor
     * @param <T>  attached data value type
     * @return bound or newly initialized attachment value
     */
    <T> T getData(AttachmentType<T> type);

    /**
     * Binds the given data instance to the specified {@link AttachmentType}.
     *
     * @param type attachment type descriptor
     * @param data non-null data instance to bind
     * @param <T>  attached data value type
     * @return the bound data instance
     */
    <T> T setData(AttachmentType<T> type, T data);

    /**
     * Removes and returns any data bound to the specified {@link AttachmentType}.
     *
     * @param type attachment type descriptor
     * @param <T>  attached data value type
     * @return previously bound data instance, or {@code null} if none was present
     */
    <T> T removeData(AttachmentType<T> type);
}

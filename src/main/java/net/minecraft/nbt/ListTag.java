package net.minecraft.nbt;

import java.util.List;
import java.util.Objects;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * Ordered NBT list tag ({@code net.minecraft.nbt.ListTag}) storing sequential {@link Tag}
 * elements.
 *
 * <p>Strictly adheres to Rule 17 with zero inner, nested, or anonymous classes.</p>
 *
 * @version 1.0.0
 */
public final class ListTag implements Tag {

    private final List<Tag> elements = new CopyOnWriteArrayList<>();

    /**
     * Constructs an empty {@code ListTag}.
     */
    public ListTag() {
    }

    @Override
    public byte getId() {
        return TAG_LIST;
    }

    /**
     * Appends a non-null {@link Tag} to this list tag.
     *
     * @param tag tag element to append
     */
    public void add(Tag tag) {
        elements.add(Objects.requireNonNull(tag, "tag must not be null"));
    }

    /**
     * Returns the {@link Tag} element at the specified index.
     *
     * @param index zero-based element index
     * @return tag at {@code index}
     */
    public Tag get(int index) {
        return elements.get(index);
    }

    /**
     * Returns the number of elements in this list tag.
     *
     * @return element count
     */
    public int size() {
        return elements.size();
    }

    /**
     * Returns whether this list tag contains no elements.
     *
     * @return {@code true} if empty
     */
    public boolean isEmpty() {
        return elements.isEmpty();
    }

    @Override
    public String getAsString() {
        return elements.toString();
    }

    @Override
    public ListTag copy() {
        ListTag copied = new ListTag();
        for (Tag element : elements) {
            copied.add(element.copy());
        }
        return copied;
    }

    @Override
    public String toString() {
        return getAsString();
    }
}

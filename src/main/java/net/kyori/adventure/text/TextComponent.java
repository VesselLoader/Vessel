package net.kyori.adventure.text;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import net.kyori.adventure.text.format.TextColor;

/**
 * Canonical Kyori Adventure immutable literal text component implementation
 * ({@code net.kyori.adventure.text.TextComponent}).
 *
 * <p>Strictly adheres to Rule 17 with zero inner, nested, or anonymous classes.</p>
 *
 * @version 1.0.0
 */
public final class TextComponent implements Component {

    private final String content;
    private final TextColor color;
    private final List<Component> children;

    /**
     * Constructs an immutable {@code TextComponent}.
     *
     * @param content  literal text string (defaults to {@code ""} if null)
     * @param color    optional {@link TextColor} (nullable)
     * @param children child components (defaults to empty list if null)
     */
    public TextComponent(String content, TextColor color, List<Component> children) {
        this.content = content != null ? content : "";
        this.color = color;
        this.children = children == null || children.isEmpty()
                ? List.of()
                : Collections.unmodifiableList(new ArrayList<>(children));
    }

    @Override
    public String content() {
        return content;
    }

    @Override
    public TextColor color() {
        return color;
    }

    @Override
    public TextComponent color(TextColor newColor) {
        return new TextComponent(content, newColor, children);
    }

    @Override
    public List<Component> children() {
        return children;
    }

    @Override
    public TextComponent append(Component child) {
        Objects.requireNonNull(child, "child must not be null");
        List<Component> updated = new ArrayList<>(children.size() + 1);
        updated.addAll(children);
        updated.add(child);
        return new TextComponent(content, color, updated);
    }

    @Override
    public String toPlainText() {
        if (children.isEmpty()) {
            return content;
        }
        StringBuilder sb = new StringBuilder(content);
        for (Component child : children) {
            sb.append(child.toPlainText());
        }
        return sb.toString();
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (!(obj instanceof TextComponent other)) {
            return false;
        }
        return content.equals(other.content)
                && Objects.equals(color, other.color)
                && children.equals(other.children);
    }

    @Override
    public int hashCode() {
        return Objects.hash(content, color, children);
    }

    @Override
    public String toString() {
        return toPlainText();
    }
}

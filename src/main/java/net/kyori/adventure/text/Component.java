package net.kyori.adventure.text;

import java.util.List;
import net.kyori.adventure.text.format.TextColor;

/**
 * Canonical Kyori Adventure immutable text component tree contract
 * ({@code net.kyori.adventure.text.Component}).
 *
 * <p>Used across Paper, Velocity, and Adventure-enabled plugins for rich chat, titles,
 * action bars, and inventory lore.</p>
 *
 * <p>Strictly adheres to Rule 17 with zero inner, nested, or anonymous classes.</p>
 *
 * @version 1.0.0
 */
public interface Component {

    /**
     * Creates a literal {@link TextComponent} with the specified text content and no explicit color.
     *
     * @param content literal text content
     * @return constructed {@link TextComponent}
     */
    static TextComponent text(String content) {
        return new TextComponent(content, null, List.of());
    }

    /**
     * Creates a literal {@link TextComponent} with the specified text content and {@link TextColor}.
     *
     * @param content literal text content
     * @param color   text color (nullable)
     * @return constructed {@link TextComponent}
     */
    static TextComponent text(String content, TextColor color) {
        return new TextComponent(content, color, List.of());
    }

    /**
     * Returns an empty literal {@link TextComponent}.
     *
     * @return empty {@link TextComponent}
     */
    static TextComponent empty() {
        return new TextComponent("", null, List.of());
    }

    /**
     * Returns the literal text content of this component node.
     *
     * @return text content
     */
    String content();

    /**
     * Returns the optional {@link TextColor} of this component node, or {@code null} if uncolored.
     *
     * @return text color or {@code null}
     */
    TextColor color();

    /**
     * Returns a copy of this component with the specified {@link TextColor}.
     *
     * @param color new text color
     * @return updated component
     */
    Component color(TextColor color);

    /**
     * Returns the immutable list of child components appended to this component.
     *
     * @return child components
     */
    List<Component> children();

    /**
     * Returns a copy of this component with {@code child} appended to its children.
     *
     * @param child non-null child component
     * @return updated component
     */
    Component append(Component child);

    /**
     * Flattens this component and all of its recursive children into a plain text string.
     *
     * @return concatenated plain text representation
     */
    String toPlainText();
}

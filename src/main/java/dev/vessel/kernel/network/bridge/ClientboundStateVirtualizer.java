package dev.vessel.kernel.network.bridge;

import dev.vessel.kernel.registry.CompactCustomBlockStateIndex;
import java.util.Objects;

/**
 * Virtualizes custom modded block states for unmodded Vanilla and Bedrock clients using
 * {@link CompactCustomBlockStateIndex} and classifies custom model complexity according to
 * Decisions 21 and 22 ({@code DISPLAY_BACKEND}, {@code VANILLA_JSON}, or
 * {@code GREY_CASE_WITH_WARNING}).
 *
 * @version 1.0.0
 */
public final class ClientboundStateVirtualizer {

    /**
     * Translates a custom modded block state ID into a safe vanilla network palette ID using the
     * zero-allocation {@link CompactCustomBlockStateIndex}.
     *
     * @param customBlockStateId the server-side custom block state ID
     * @param stateIndex         the compact custom block state index
     * @return the vanilla-safe network block state palette ID
     */
    public int virtualizeCustomBlockStateForVanilla(
            int customBlockStateId,
            CompactCustomBlockStateIndex stateIndex) {
        Objects.requireNonNull(stateIndex, "stateIndex must not be null");
        return stateIndex.getVanillaPaletteId(customBlockStateId);
    }

    /**
     * Classifies custom block/item model rendering complexity into one of the three canonical
     * Decision 22 tiers.
     *
     * @param requiresCodeAtDrawTime whether the model executes custom Java/GL code at draw time
     * @param vanillaJsonExactMatch  whether the model can be represented losslessly as Vanilla JSON
     * @return {@code "DISPLAY_BACKEND"}, {@code "VANILLA_JSON"}, or
     *         {@code "GREY_CASE_WITH_WARNING"}
     */
    public String classifyCustomModelComplexity(
            boolean requiresCodeAtDrawTime,
            boolean vanillaJsonExactMatch) {
        if (vanillaJsonExactMatch && !requiresCodeAtDrawTime) {
            return "VANILLA_JSON";
        }
        if (requiresCodeAtDrawTime && !vanillaJsonExactMatch) {
            return "DISPLAY_BACKEND";
        }
        return "GREY_CASE_WITH_WARNING";
    }
}

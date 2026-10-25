package dev.vessel.kernel.gui;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Unit tests verifying Negative Space Font Glyph UI rendering, shift building,
 * and 20 Hz actionbar animation ticker behavior.
 *
 * @version 1.0.0
 * @author VESSEL Engine Team
 */
public class NegativeSpaceFontGlyphUiTest {

    @Test
    @DisplayName("Should compose optimal negative-space shift sequences")
    public void testShiftBuilderCalculations() {
        // Shift negative 17 pixels -> -16 and -1
        String shift17 = NegativeSpaceGlyphBuilder.buildShiftString(-17);
        assertThat(shift17).isEqualTo("\uF807\uF80B");

        // Shift positive 35 pixels -> +32, +2, +1
        String shift35 = NegativeSpaceGlyphBuilder.buildShiftString(35);
        assertThat(shift35).isEqualTo("\uF826\uF822\uF821");

        // Shift zero pixels -> empty string
        assertThat(NegativeSpaceGlyphBuilder.buildShiftString(0)).isEmpty();
    }

    @Test
    @DisplayName("Should correctly layout and render multi-layered Font Glyph UI")
    public void testUiEngineRendering() {
        NegativeSpaceFontGlyphUiEngine engine = new NegativeSpaceFontGlyphUiEngine("crusher_gui", 256, 166);

        // Add background frame glyph
        engine.addElement(new FontGlyphElement(
                "\uE001",
                0,
                0,
                256,
                GlyphLayerType.MACHINERY_FRAME,
                -1,
                false
        ));

        // Add energy meter glyph
        engine.addElement(new FontGlyphElement(
                "\uE010",
                32,
                16,
                16,
                GlyphLayerType.CONTAINER_SLOTS,
                0xFF0000,
                false
        ));

        RenderedGlyphComponent rendered = engine.render(0);
        assertThat(rendered).isNotNull();
        assertThat(rendered.jsonText()).contains("vessel:gui_screen");
        assertThat(rendered.jsonText()).contains("\uE001");
        assertThat(rendered.jsonText()).contains("\uE010");
        assertThat(rendered.jsonText()).contains("#FF0000");

        // Verify caching idempotency
        RenderedGlyphComponent cached = engine.render(0);
        assertThat(cached.jsonText()).isEqualTo(rendered.jsonText());

        // Test clear
        engine.clearElements();
        assertThat(engine.getElements()).isEmpty();
    }

    @Test
    @DisplayName("Should advance actionbar animation ticker at 20 Hz with pulsing")
    public void testActionbarAnimationTicker() {
        FontGlyphActionbarAnimationTicker ticker = new FontGlyphActionbarAnimationTicker("MANA", 10, ActionBarGlyphTheme.DEFAULT_MANA_THEME);
        ticker.setValues(50.0f, 100.0f);

        ActionBarPulseState state1 = ticker.tick();
        assertThat(state1.currentMana()).isEqualTo(50.0f);
        assertThat(state1.getFillRatio()).isEqualTo(0.5f);
        assertThat(state1.activeSegmentIndex()).isEqualTo(5);

        String json = ticker.renderJson();
        assertThat(json).contains("[MANA]");
        assertThat(json).contains("50%");
        assertThat(json).contains(ActionBarGlyphTheme.DEFAULT_MANA_THEME.filledGlyph());
        assertThat(json).contains(ActionBarGlyphTheme.DEFAULT_MANA_THEME.emptyGlyph());

        // Tick multiple times to test pulse cycle
        for (int i = 0; i < 25; i++) {
            ticker.tick();
        }
        assertThat(ticker.getCurrentTick()).isEqualTo(26);

        // Test invalid inputs
        assertThatThrownBy(() -> ticker.setValues(-5.0f, -10.0f))
                .isInstanceOf(IllegalArgumentException.class);
    }
}

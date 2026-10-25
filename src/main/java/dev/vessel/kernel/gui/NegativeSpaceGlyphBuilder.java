package dev.vessel.kernel.gui;

/**
 * Utility calculator for converting arbitrary horizontal pixel offsets
 * into optimal composite Unicode negative-space and positive-space shift sequences.
 *
 * @version 1.0.0
 * @author VESSEL Engine Team
 */
public final class NegativeSpaceGlyphBuilder {

    private static final NegativeSpaceShiftGlyph[] NEGATIVE_GLYPHS = new NegativeSpaceShiftGlyph[] {
            NegativeSpaceShiftGlyph.SHIFT_NEG_1024,
            NegativeSpaceShiftGlyph.SHIFT_NEG_512,
            NegativeSpaceShiftGlyph.SHIFT_NEG_256,
            NegativeSpaceShiftGlyph.SHIFT_NEG_128,
            NegativeSpaceShiftGlyph.SHIFT_NEG_64,
            NegativeSpaceShiftGlyph.SHIFT_NEG_32,
            NegativeSpaceShiftGlyph.SHIFT_NEG_16,
            NegativeSpaceShiftGlyph.SHIFT_NEG_8,
            NegativeSpaceShiftGlyph.SHIFT_NEG_4,
            NegativeSpaceShiftGlyph.SHIFT_NEG_2,
            NegativeSpaceShiftGlyph.SHIFT_NEG_1
    };

    private static final NegativeSpaceShiftGlyph[] POSITIVE_GLYPHS = new NegativeSpaceShiftGlyph[] {
            NegativeSpaceShiftGlyph.SHIFT_POS_1024,
            NegativeSpaceShiftGlyph.SHIFT_POS_512,
            NegativeSpaceShiftGlyph.SHIFT_POS_256,
            NegativeSpaceShiftGlyph.SHIFT_POS_128,
            NegativeSpaceShiftGlyph.SHIFT_POS_64,
            NegativeSpaceShiftGlyph.SHIFT_POS_32,
            NegativeSpaceShiftGlyph.SHIFT_POS_16,
            NegativeSpaceShiftGlyph.SHIFT_POS_8,
            NegativeSpaceShiftGlyph.SHIFT_POS_4,
            NegativeSpaceShiftGlyph.SHIFT_POS_2,
            NegativeSpaceShiftGlyph.SHIFT_POS_1
    };

    private NegativeSpaceGlyphBuilder() {
    }

    /**
     * Builds an optimal string of shift characters representing the target pixel delta.
     *
     * @param targetDeltaPx target pixel displacement (negative moves cursor left, positive moves right)
     * @return sequence of Unicode shift characters
     */
    public static String buildShiftString(int targetDeltaPx) {
        if (targetDeltaPx == 0) {
            return "";
        }

        StringBuilder sb = new StringBuilder();
        int remaining = targetDeltaPx;

        if (remaining < 0) {
            int absVal = -remaining;
            for (NegativeSpaceShiftGlyph glyph : NEGATIVE_GLYPHS) {
                int step = -glyph.getPixelOffset();
                while (absVal >= step) {
                    sb.append(glyph.getUnicodeChar());
                    absVal -= step;
                }
            }
        } else {
            int val = remaining;
            for (NegativeSpaceShiftGlyph glyph : POSITIVE_GLYPHS) {
                int step = glyph.getPixelOffset();
                while (val >= step) {
                    sb.append(glyph.getUnicodeChar());
                    val -= step;
                }
            }
        }

        return sb.toString();
    }
}

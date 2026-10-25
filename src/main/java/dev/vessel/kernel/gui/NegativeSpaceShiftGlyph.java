package dev.vessel.kernel.gui;

/**
 * Standard Unicode negative-space and positive-space font glyph shift definitions.
 * Maps pixel shift magnitudes to their dedicated private-use-area (PUA) characters
 * in custom Minecraft resource packs, enabling exact sub-pixel and multi-pixel alignment.
 *
 * @version 1.0.0
 * @author VESSEL Engine Team
 */
public enum NegativeSpaceShiftGlyph {

    SHIFT_NEG_1024(-1024, "\uF801"),
    SHIFT_NEG_512(-512, "\uF802"),
    SHIFT_NEG_256(-256, "\uF803"),
    SHIFT_NEG_128(-128, "\uF804"),
    SHIFT_NEG_64(-64, "\uF805"),
    SHIFT_NEG_32(-32, "\uF806"),
    SHIFT_NEG_16(-16, "\uF807"),
    SHIFT_NEG_8(-8, "\uF808"),
    SHIFT_NEG_4(-4, "\uF809"),
    SHIFT_NEG_2(-2, "\uF80A"),
    SHIFT_NEG_1(-1, "\uF80B"),

    SHIFT_POS_1(1, "\uF821"),
    SHIFT_POS_2(2, "\uF822"),
    SHIFT_POS_4(4, "\uF823"),
    SHIFT_POS_8(8, "\uF824"),
    SHIFT_POS_16(16, "\uF825"),
    SHIFT_POS_32(32, "\uF826"),
    SHIFT_POS_64(64, "\uF827"),
    SHIFT_POS_128(128, "\uF828"),
    SHIFT_POS_256(256, "\uF829"),
    SHIFT_POS_512(512, "\uF82A"),
    SHIFT_POS_1024(1024, "\uF82B");

    private final int pixelOffset;
    private final String unicodeChar;

    NegativeSpaceShiftGlyph(int pixelOffset, String unicodeChar) {
        this.pixelOffset = pixelOffset;
        this.unicodeChar = unicodeChar;
    }

    public int getPixelOffset() {
        return pixelOffset;
    }

    public String getUnicodeChar() {
        return unicodeChar;
    }
}

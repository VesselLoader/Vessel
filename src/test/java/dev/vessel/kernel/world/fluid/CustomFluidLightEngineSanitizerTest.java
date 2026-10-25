package dev.vessel.kernel.world.fluid;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Unit test suite verifying CustomFluidLightEngineSanitizer clamping, anomaly detection, and caching.
 *
 * @version 1.0.0
 * @author VESSEL Engine Team
 */
public final class CustomFluidLightEngineSanitizerTest {

    @Test
    @DisplayName("Verify in-bounds normal fluids are passed through without modification")
    public void testInBoundsFluidSanitization() {
        CustomFluidLightEngineSanitizer sanitizer = new CustomFluidLightEngineSanitizer();

        SanitizedFluidLightProperties props = sanitizer.sanitize("minecraft:water", 0, 1);

        assertThat(props.fluidId()).isEqualTo("minecraft:water");
        assertThat(props.luminance()).isEqualTo(0);
        assertThat(props.opacity()).isEqualTo(1);
        assertThat(props.wasClamped()).isFalse();
        assertThat(props.auditReason()).contains(CustomFluidLightEngineSanitizer.AUDIT_OK);
        assertThat(sanitizer.getAnomalyCount()).isEqualTo(0);
    }

    @Test
    @DisplayName("Verify out-of-bounds modded fluids are clamped and logged as anomalies")
    public void testOutOfBoundsFluidSanitization() {
        CustomFluidLightEngineSanitizer sanitizer = new CustomFluidLightEngineSanitizer();

        // Test underflow: negative luminance -5 and negative opacity -2
        SanitizedFluidLightProperties underflow = sanitizer.sanitize("modded:corrupted_cold_fluid", -5, -2);
        assertThat(underflow.luminance()).isEqualTo(0);
        assertThat(underflow.opacity()).isEqualTo(0);
        assertThat(underflow.wasClamped()).isTrue();
        assertThat(underflow.auditReason()).contains(CustomFluidLightEngineSanitizer.AUDIT_LUMINANCE_UNDERFLOW);
        assertThat(underflow.auditReason()).contains(CustomFluidLightEngineSanitizer.AUDIT_OPACITY_UNDERFLOW);

        // Test overflow: excessive luminance 30 and excessive opacity 100
        SanitizedFluidLightProperties overflow = sanitizer.sanitize("modded:super_radioactive_fluid", 30, 100);
        assertThat(overflow.luminance()).isEqualTo(15);
        assertThat(overflow.opacity()).isEqualTo(15);
        assertThat(overflow.wasClamped()).isTrue();
        assertThat(overflow.auditReason()).contains(CustomFluidLightEngineSanitizer.AUDIT_LUMINANCE_OVERFLOW);
        assertThat(overflow.auditReason()).contains(CustomFluidLightEngineSanitizer.AUDIT_OPACITY_OVERFLOW);

        assertThat(sanitizer.getAnomalyCount()).isEqualTo(2);
    }

    @Test
    @DisplayName("Verify caching and pre-registration of fluid properties")
    public void testCachingAndPreRegistration() {
        CustomFluidLightEngineSanitizer sanitizer = new CustomFluidLightEngineSanitizer();

        RawFluidLightDescriptor descriptor = new RawFluidLightDescriptor("create:honey", 0, 2);
        SanitizedFluidLightProperties first = sanitizer.sanitize(descriptor);
        SanitizedFluidLightProperties second = sanitizer.getCached("create:honey");

        assertThat(first).isSameAs(second);
        assertThat(sanitizer.getCachedFluidCount()).isEqualTo(1);

        sanitizer.registerPreSanitized("mekanism:liquid_osmium", 5, 1);
        SanitizedFluidLightProperties osmium = sanitizer.getCached("mekanism:liquid_osmium");
        assertThat(osmium).isNotNull();
        assertThat(osmium.luminance()).isEqualTo(5);
        assertThat(osmium.opacity()).isEqualTo(1);

        // Fast path standalone clamp methods
        assertThat(CustomFluidLightEngineSanitizer.sanitizeLuminance(-10)).isEqualTo(0);
        assertThat(CustomFluidLightEngineSanitizer.sanitizeLuminance(25)).isEqualTo(15);
        assertThat(CustomFluidLightEngineSanitizer.sanitizeOpacity(-1)).isEqualTo(0);
        assertThat(CustomFluidLightEngineSanitizer.sanitizeOpacity(99)).isEqualTo(15);
    }
}

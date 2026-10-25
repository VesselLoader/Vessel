package dev.vessel.kernel.architectury;

/**
 * Concrete synthetic platform implementation backing {@link SampleExpectPlatformTarget}.
 *
 * @version 1.0.0
 * @author VESSEL Engine Team
 */
public final class SampleExpectPlatformTargetImpl {

    public static final String EXPECTED_GREETING = "Hello from Architectury Platform Impl!";

    public SampleExpectPlatformTargetImpl() {
    }

    public static String getGreeting() {
        return EXPECTED_GREETING;
    }
}

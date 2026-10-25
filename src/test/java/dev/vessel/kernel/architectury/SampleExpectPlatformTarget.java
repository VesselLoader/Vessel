package dev.vessel.kernel.architectury;

/**
 * Sample interface declaring an Architectury {@code @ExpectPlatform} method target.
 *
 * @version 1.0.0
 * @author VESSEL Engine Team
 */
public final class SampleExpectPlatformTarget {

    public SampleExpectPlatformTarget() {
    }

    public static String getGreeting() {
        throw new AssertionError("ExpectPlatform synthetic stub called directly");
    }
}

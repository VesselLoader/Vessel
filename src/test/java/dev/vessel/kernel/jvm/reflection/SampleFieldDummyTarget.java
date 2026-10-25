package dev.vessel.kernel.jvm.reflection;

/**
 * Dedicated standalone test fixture class for {@link UnsafeFieldOffsetRecalculatorTest}.
 *
 * @version 1.0.0
 * @author VESSEL Engine Team
 */
public class SampleFieldDummyTarget {

    public static int staticCounter = 42;

    public int instanceFieldA = 100;
    public long instanceFieldB = 200L;
    public String instanceFieldC = "vessel";
}

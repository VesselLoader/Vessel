package net.minecraftforge.fml;

import java.util.function.BiPredicate;
import java.util.function.Supplier;

/**
 * Universal multi-era extension point matching bytecode signature IExtensionPoint$DisplayTest.
 * Adheres strictly to Rule 17 (Single Responsibility Principle - zero inner classes).
 *
 * @version 1.0.0
 */
public class IExtensionPoint$DisplayTest implements IExtensionPoint<IExtensionPoint$DisplayTest> {

    public static final String IGNORESERVERONLY = "OHNOAINGOINGAPIServer";

    public IExtensionPoint$DisplayTest(Supplier<String> suppliedVersion, BiPredicate<String, Boolean> remoteVersionTest) {
    }

    public IExtensionPoint$DisplayTest(String version, BiPredicate<String, Boolean> remoteVersionTest) {
    }
}

package dev.vessel.kernel.compat.era;

import dev.vessel.kernel.identity.SourceLoaderKind;
import dev.vessel.kernel.remap.DualSidedModEnvironmentSplitter;
import dev.vessel.kernel.remap.ReflectionCallCacheJitHarmonizer;
import dev.vessel.kernel.remap.ThreadCreationInterceptionTrap;
import java.util.Objects;

/**
 * Orchestrates the complete multi-era bytecode transformation pipeline so mods and plugins
 * compiled for older Minecraft versions (1.8.9 through 1.21.x) can be loaded and executed on the
 * latest Minecraft server in a single pass.
 *
 * <p>Strictly adheres to Rule 17 with zero inner, nested, or anonymous classes.</p>
 *
 * @version 1.0.2
 */
public final class MultiEraBytecodeTransformationPipeline {

    private final CrossEraMethodAndFieldRedirector modCallRedirector =
            new CrossEraMethodAndFieldRedirector();
    private final LegacyBukkitApiVersionBridge bukkitApiBridge =
            new LegacyBukkitApiVersionBridge();
    private final DualSidedModEnvironmentSplitter sideSplitter =
            new DualSidedModEnvironmentSplitter();
    private final ReflectionCallCacheJitHarmonizer reflectionTrap =
            new ReflectionCallCacheJitHarmonizer();
    private final ThreadCreationInterceptionTrap concurrencyTrap =
            new ThreadCreationInterceptionTrap();

    /**
     * Transforms a class file from a lower-version mod or plugin into bytecode compatible with
     * {@code targetLatestMcVersion}.
     *
     * @param rawClassBytes         the original class bytes from the mod or plugin JAR
     * @param loaderKind            the source ecosystem loader kind
     * @param sourceMcVersion       the Minecraft version the module was originally compiled for
     * @param targetLatestMcVersion the active latest Minecraft server version
     * @return the upgraded class bytecode ready for definition on the latest JVM runtime
     */
    public byte[] transformLowerVersionClassForLatestServer(
            byte[] rawClassBytes,
            SourceLoaderKind loaderKind,
            String sourceMcVersion,
            String targetLatestMcVersion) {
        Objects.requireNonNull(rawClassBytes, "rawClassBytes must not be null");
        Objects.requireNonNull(loaderKind, "loaderKind must not be null");
        Objects.requireNonNull(sourceMcVersion, "sourceMcVersion must not be null");
        Objects.requireNonNull(targetLatestMcVersion, "targetLatestMcVersion must not be null");

        byte[] transformed = rawClassBytes;
        if (loaderKind == SourceLoaderKind.BUKKIT
                || loaderKind == SourceLoaderKind.SPIGOT
                || loaderKind == SourceLoaderKind.PAPER
                || loaderKind == SourceLoaderKind.FOLIA) {
            transformed = bukkitApiBridge.rewriteLegacyBukkitPluginBytecode(transformed);
        } else {
            transformed = modCallRedirector.rewriteLegacyMinecraftCalls(transformed);
            transformed = sideSplitter.stubClientRenderCallsOnServer(transformed);
        }

        transformed = reflectionTrap.rewriteReflectiveCallsToMethodHandles(transformed);
        transformed = concurrencyTrap.interceptRawThreadInstantiations(transformed);
        return transformed;
    }
}

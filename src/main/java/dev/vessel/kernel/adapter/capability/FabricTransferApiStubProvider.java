package dev.vessel.kernel.adapter.capability;

import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Clean-room stub and descriptor provider for Fabric Transfer API v1.
 *
 * <p>Enables industrial Fabric mods (e.g. Modern Industrialization, TechReborn) to execute on
 * VESSEL without throwing {@link ClassNotFoundException} when querying transfer transactions.</p>
 *
 * <p>Strictly adheres to Rule 17: Single Responsibility, zero inner, nested, or anonymous classes.</p>
 *
 * @version 1.0.0
 * @since 1.0.0
 */
public final class FabricTransferApiStubProvider {

    private static final Map<String, String> STUB_CLASSES = new ConcurrentHashMap<>();

    static {
        STUB_CLASSES.put("net.fabricmc.fabric.api.transfer.v1.transaction.TransactionContext", "interface");
        STUB_CLASSES.put("net.fabricmc.fabric.api.transfer.v1.transaction.Transaction", "class");
        STUB_CLASSES.put("net.fabricmc.fabric.api.transfer.v1.storage.Storage", "interface");
        STUB_CLASSES.put("net.fabricmc.fabric.api.transfer.v1.storage.StorageView", "interface");
        STUB_CLASSES.put("net.fabricmc.fabric.api.transfer.v1.item.ItemVariant", "interface");
        STUB_CLASSES.put("net.fabricmc.fabric.api.transfer.v1.fluid.FluidVariant", "interface");
    }

    /**
     * Constructs the stub provider.
     */
    public FabricTransferApiStubProvider() {
    }

    /**
     * Checks if a class name belongs to the Fabric Transfer API contract.
     *
     * @param className class name
     * @return true if stubbed
     */
    public boolean isTransferApiClass(String className) {
        Objects.requireNonNull(className, "className cannot be null");
        return STUB_CLASSES.containsKey(className) || className.startsWith("net.fabricmc.fabric.api.transfer.v1.");
    }

    /**
     * Resolves the stub type definition (interface or class).
     *
     * @param className class name
     * @return type string, or "interface" by default
     */
    public String resolveStubType(String className) {
        return STUB_CLASSES.getOrDefault(className, "interface");
    }
}

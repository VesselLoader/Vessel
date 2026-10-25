package dev.vessel.kernel.nativebridge;

import java.util.Collection;
import java.util.Collections;
import java.util.Objects;
import java.util.Optional;
import java.util.OptionalLong;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

/**
 * Direct lookup engine and conflict arbiter for exported native JNI function symbols.
 * Prevents symbol address collisions when incompatible native runtime versions are bundled (ERR-29, MAP-VUS-0512).
 * Operates purely on Java 21 LTS standard runtime without requiring preview compiler flags.
 *
 * @version 1.0.0
 * @author VESSEL Engine Team
 */
public final class JniSymbolTableResolver {

    private final ConcurrentMap<String, JniSymbolDescriptor> symbolRegistry = new ConcurrentHashMap<>();

    /**
     * Constructs a JniSymbolTableResolver instance.
     */
    public JniSymbolTableResolver() {
    }

    /**
     * Registers a known JNI symbol and its virtual memory address.
     *
     * @param symbolName exported function symbol name
     * @param address memory address pointer
     * @param librarySource library origin description
     * @param versionTag semantic version or hash
     * @return the registered symbol descriptor
     */
    public JniSymbolDescriptor registerSymbol(String symbolName, long address, String librarySource, String versionTag) {
        Objects.requireNonNull(symbolName, "symbolName cannot be null");
        JniSymbolDescriptor descriptor = new JniSymbolDescriptor(symbolName, address, librarySource, versionTag);
        symbolRegistry.put(symbolName, descriptor);
        return descriptor;
    }

    /**
     * Looks up an exported symbol descriptor by its symbol name.
     *
     * @param symbolName JNI symbol name
     * @return optional containing descriptor if registered
     */
    public Optional<JniSymbolDescriptor> lookup(String symbolName) {
        if (symbolName == null) {
            return Optional.empty();
        }
        return Optional.ofNullable(symbolRegistry.get(symbolName));
    }

    /**
     * Resolves the primitive 64-bit virtual address for a symbol with zero boxing.
     *
     * @param symbolName JNI symbol name
     * @return optional long address
     */
    public OptionalLong resolveAddress(String symbolName) {
        if (symbolName == null) {
            return OptionalLong.empty();
        }
        JniSymbolDescriptor descriptor = symbolRegistry.get(symbolName);
        if (descriptor != null) {
            return OptionalLong.of(descriptor.memoryAddress());
        }
        return OptionalLong.empty();
    }

    /**
     * Checks whether the symbol has been registered in the table.
     *
     * @param symbolName symbol name
     * @return true if present
     */
    public boolean hasSymbol(String symbolName) {
        if (symbolName == null) {
            return false;
        }
        return symbolRegistry.containsKey(symbolName);
    }

    /**
     * Detects if registering a symbol at the specified address introduces a collision
     * with an already registered symbol from a different binary.
     *
     * @param symbolName symbol name
     * @param newAddress candidate virtual memory address
     * @return true if collision detected (different address already registered)
     */
    public boolean detectCollision(String symbolName, long newAddress) {
        JniSymbolDescriptor existing = symbolRegistry.get(symbolName);
        if (existing == null) {
            return false;
        }
        return existing.memoryAddress() != newAddress;
    }

    /**
     * Returns an unmodifiable collection of all registered symbols.
     *
     * @return collection of descriptors
     */
    public Collection<JniSymbolDescriptor> allSymbols() {
        return Collections.unmodifiableCollection(symbolRegistry.values());
    }

    /**
     * Returns the count of registered symbols.
     *
     * @return symbol count
     */
    public int symbolCount() {
        return symbolRegistry.size();
    }

    /**
     * Clears all registered symbols from the resolver table.
     */
    public void clear() {
        symbolRegistry.clear();
    }
}

package net.fabricmc.fabric.api.itemgroup.v1;

/**
 * Canonical Fabric Item Group API v1 callback contract for modifying entries in a creative tab
 * ({@code net.fabricmc.fabric.api.itemgroup.v1.ModifyEntriesCallback}).
 *
 * <p>Strictly adheres to Rule 17 with zero inner, nested, or anonymous classes.</p>
 *
 * @version 1.0.0
 */
@FunctionalInterface
public interface ModifyEntriesCallback {

    /**
     * Invoked when the entries of a creative item group are being populated.
     *
     * @param content mutable item group entries collector
     */
    void modifyEntries(FabricItemGroupEntries content);
}

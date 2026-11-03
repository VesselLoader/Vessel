package net.minecraftforge.common.capabilities;

import net.minecraftforge.common.util.LazyOptional;

/**
 * Forge capability provider contract ({@code net.minecraftforge.common.capabilities.ICapabilityProvider})
 * implemented by block entities, entities, item stacks, and worlds to expose sided capabilities.
 *
 * <p>Strictly adheres to Rule 17 with zero inner, nested, or anonymous classes.</p>
 *
 * @version 1.0.0
 */
public interface ICapabilityProvider {

    /**
     * Queries a capability instance from this provider on the specified directional side.
     *
     * @param cap  requested capability handle
     * @param side directional face name (or {@code null} for unsided access)
     * @param <T>  capability interface type
     * @return a {@link LazyOptional} containing the capability if available, or {@link LazyOptional#empty()}
     */
    <T> LazyOptional<T> getCapability(Capability<T> cap, String side);

    /**
     * Queries an unsided capability instance from this provider.
     *
     * @param cap requested capability handle
     * @param <T> capability interface type
     * @return a {@link LazyOptional} containing the capability if available, or {@link LazyOptional#empty()}
     */
    default <T> LazyOptional<T> getCapability(Capability<T> cap) {
        return getCapability(cap, null);
    }
}

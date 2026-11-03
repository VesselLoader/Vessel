package net.minecraftforge.common.capabilities;

import java.util.Objects;
import net.minecraftforge.common.util.LazyOptional;

/**
 * Forge capability handle ({@code net.minecraftforge.common.capabilities.Capability}) representing
 * a registered capability contract and providing type-safe {@link LazyOptional} filtering.
 *
 * <p>Strictly adheres to Rule 17 with zero inner, nested, or anonymous classes.</p>
 *
 * @param <T> capability interface type
 * @version 1.0.0
 */
public final class Capability<T> {

    private final String name;
    private final boolean registered;

    /**
     * Constructs a registered {@code Capability} with the specified canonical name.
     *
     * @param name non-null capability name
     */
    public Capability(String name) {
        this(name, true);
    }

    /**
     * Constructs a {@code Capability} with the specified canonical name and registration state.
     *
     * @param name       non-null capability name
     * @param registered whether the capability has been registered
     */
    public Capability(String name, boolean registered) {
        this.name = Objects.requireNonNull(name, "name must not be null").trim();
        this.registered = registered;
    }

    /**
     * Returns the canonical name of this capability.
     *
     * @return non-null capability name
     */
    public String getName() {
        return name;
    }

    /**
     * Returns {@code true} if this capability is registered in the runtime capability table.
     *
     * @return {@code true} if registered
     */
    public boolean isRegistered() {
        return registered;
    }

    /**
     * Returns {@code value.cast()} if {@code toCheck} matches this capability, or
     * {@link LazyOptional#empty()} otherwise.
     *
     * @param toCheck capability requested by the caller
     * @param value   holder for this capability's implementation
     * @param <R>     requested capability type
     * @return cast {@link LazyOptional} if matching, or {@link LazyOptional#empty()}
     */
    public <R> LazyOptional<R> orEmpty(Capability<R> toCheck, LazyOptional<T> value) {
        Objects.requireNonNull(value, "value must not be null");
        if (this == toCheck || (toCheck != null && this.name.equals(toCheck.name))) {
            return value.cast();
        }
        return LazyOptional.empty();
    }
}

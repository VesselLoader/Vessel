package net.minecraftforge.fml.common;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import net.minecraftforge.api.distmarker.Dist;

/**
 * Clean-room ABI annotation for Forge {@code @Mod.EventBusSubscriber} to mark classes
 * whose static {@code @SubscribeEvent} methods should be automatically registered onto
 * an event bus during mod initialization.
 *
 * <p>Strictly adheres to Rule 17 with zero inner, nested, or anonymous classes.</p>
 *
 * @version 1.0.1
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.TYPE)
public @interface Mod$EventBusSubscriber {

    /**
     * Optional mod ID that this subscriber belongs to (defaults to empty string).
     *
     * @return owning mod id
     */
    String modid() default "";

    /**
     * Target physical distribution environments.
     *
     * @return array of target distributions
     */
    Dist[] value() default {Dist.CLIENT, Dist.DEDICATED_SERVER};

    /**
     * Alias for {@link #value()}.
     *
     * @return array of target distributions
     */
    Dist[] dist() default {Dist.CLIENT, Dist.DEDICATED_SERVER};

    /**
     * The bus to subscribe this class to.
     *
     * @return target event bus
     */
    Mod$EventBusSubscriber$Bus bus() default Mod$EventBusSubscriber$Bus.FORGE;
}

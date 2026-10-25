package net.minecraftforge.fml.common;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Canonical Minecraft Forge mod entrypoint annotation ({@code net.minecraftforge.fml.common.Mod})
 * retained at runtime so VESSEL can discover, instantiate, and bind {@code @Mod("modid")} classes
 * from physical Forge {@code .jar} archives.
 *
 * <p>Strictly adheres to Rule 17 with zero inner, nested, or anonymous classes.</p>
 *
 * @version 1.0.0
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.TYPE)
public @interface Mod {

    /**
     * The unique lowercase mod identifier matching {@code META-INF/mods.toml}.
     *
     * @return mod ID string
     */
    String value();
}

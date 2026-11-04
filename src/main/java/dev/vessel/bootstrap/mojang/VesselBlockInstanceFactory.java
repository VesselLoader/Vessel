package dev.vessel.bootstrap.mojang;

import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.util.Objects;

/**
 * Reflectively constructs Minecraft {@code Block} instances across all Minecraft eras:
 * <ul>
 *   <li>Modern 26.3 / 1.20.5+: {@code new Block(BlockBehaviour.Properties.of().setId(ResourceKey))}</li>
 *   <li>Modern 1.13 – 1.20.4: {@code new Block(BlockBehaviour.Properties.of())}</li>
 *   <li>Legacy 1.7.10 – 1.12.2: {@code new Block(Material.STONE)} with {@code setRegistryName}</li>
 * </ul>
 *
 * <p>Supports instantiating concrete mod-defined {@code Block} subclasses as well as vanilla fallbacks.</p>
 *
 * <p>Strictly adheres to Rule 17 with zero inner, nested, or anonymous classes, and zero lambdas.</p>
 *
 * @version 1.0.0
 */
public final class VesselBlockInstanceFactory {

    private static final String[] BLOCK_CLASS_NAMES = new String[]{
            "net.minecraft.world.level.block.Block",
            "net.minecraft.block.Block"
    };

    private final VesselBlockPropertiesFactory propertiesFactory = new VesselBlockPropertiesFactory();

    public VesselBlockInstanceFactory() {
    }

    /**
     * Resolves the active Minecraft {@code Block} class.
     *
     * @param classLoader active game classloader
     * @return the {@code Block} class, or {@code null} if not found
     */
    public Class<?> resolveBlockClass(ClassLoader classLoader) {
        Objects.requireNonNull(classLoader, "classLoader must not be null");
        for (String name : BLOCK_CLASS_NAMES) {
            try {
                return Class.forName(name, true, classLoader);
            } catch (ClassNotFoundException ignored) {
            }
        }
        return null;
    }

    /**
     * Creates a {@code Block} instance for the specified identifier using vanilla {@code Block} fallback.
     *
     * @param classLoader active game classloader
     * @param idObj       the identifier or resource location object
     * @param blockStr    the namespaced string representation
     * @return a new {@code Block} instance, or {@code null} if creation failed
     */
    public Object createBlockInstance(ClassLoader classLoader, Object idObj, String blockStr) {
        return createBlockInstance(classLoader, idObj, blockStr, null);
    }

    /**
     * Creates a {@code Block} instance, attempting to instantiate {@code targetBlockClass} if non-null,
     * or falling back to the vanilla {@code Block} class.
     *
     * @param classLoader      active game classloader
     * @param idObj            the identifier or resource location object
     * @param blockStr         the namespaced string representation
     * @param targetBlockClass specific mod {@code Block} subclass to construct, or {@code null} for vanilla
     * @return a new {@code Block} instance, or {@code null} if creation failed
     */
    public Object createBlockInstance(
            ClassLoader classLoader,
            Object idObj,
            String blockStr,
            Class<?> targetBlockClass) {
        Objects.requireNonNull(classLoader, "classLoader must not be null");

        Class<?> blockClass = resolveBlockClass(classLoader);
        if (blockClass == null) {
            return null;
        }

        Class<?> effectiveClass = (targetBlockClass != null && blockClass.isAssignableFrom(targetBlockClass))
                ? targetBlockClass
                : blockClass;

        Object props = propertiesFactory.createBlockProperties(classLoader, idObj);
        if (props != null) {
            // 1. Try constructor taking properties/material on target class
            for (Constructor<?> ctor : effectiveClass.getDeclaredConstructors()) {
                if (ctor.getParameterCount() == 1 && ctor.getParameterTypes()[0].isAssignableFrom(props.getClass())) {
                    try {
                        ctor.setAccessible(true);
                        return ctor.newInstance(props);
                    } catch (Exception ignored) {
                    }
                }
            }

            // 2. If target class failed, try vanilla Block constructor taking properties
            if (effectiveClass != blockClass) {
                for (Constructor<?> ctor : blockClass.getDeclaredConstructors()) {
                    if (ctor.getParameterCount() == 1 && ctor.getParameterTypes()[0].isAssignableFrom(props.getClass())) {
                        try {
                            ctor.setAccessible(true);
                            return ctor.newInstance(props);
                        } catch (Exception ignored) {
                        }
                    }
                }
            }
        }

        // 3. Fallback to no-argument constructor
        try {
            Constructor<?> noArgCtor = effectiveClass.getDeclaredConstructor();
            noArgCtor.setAccessible(true);
            Object blockInstance = noArgCtor.newInstance();

            // Set registry name if supported (1.9 – 1.12.2)
            if (idObj != null) {
                try {
                    Method setRegMethod = effectiveClass.getMethod("setRegistryName", idObj.getClass());
                    setRegMethod.invoke(blockInstance, idObj);
                } catch (Exception e1) {
                    try {
                        Method setRegString = effectiveClass.getMethod("setRegistryName", String.class);
                        setRegString.invoke(blockInstance, blockStr);
                    } catch (Exception ignored) {
                    }
                }
            }

            return blockInstance;
        } catch (Exception ignored) {
        }

        // 4. Vanilla no-arg fallback
        if (effectiveClass != blockClass) {
            try {
                Constructor<?> vanillaNoArg = blockClass.getDeclaredConstructor();
                vanillaNoArg.setAccessible(true);
                return vanillaNoArg.newInstance();
            } catch (Exception ignored) {
            }
        }

        return null;
    }
}

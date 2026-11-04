package dev.vessel.bootstrap.mojang;

import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.Objects;

/**
 * Reflectively creates and configures block properties ({@code BlockBehaviour$Properties} or
 * legacy {@code Material}) across Minecraft eras (1.7.10 – 26.3+).
 *
 * <p>Strictly adheres to Rule 17 with zero inner, nested, or anonymous classes.</p>
 *
 * @version 1.0.0
 */
public final class VesselBlockPropertiesFactory {

    private static final String[] BLOCK_PROPERTIES_CLASS_NAMES = new String[]{
            "net.minecraft.world.level.block.state.BlockBehaviour$Properties",
            "net.minecraft.world.level.block.Block$Properties",
            "net.minecraft.block.Block$Properties",
            "net.minecraft.block.state.BlockBehaviour$Properties"
    };

    private static final String[] MATERIAL_CLASS_NAMES = new String[]{
            "net.minecraft.world.level.material.Material",
            "net.minecraft.block.material.Material"
    };

    public VesselBlockPropertiesFactory() {
    }

    /**
     * Resolves the Minecraft block properties class.
     *
     * @param classLoader active game classloader
     * @return the block properties class, or {@code null} if not found
     */
    public Class<?> resolveBlockPropertiesClass(ClassLoader classLoader) {
        Objects.requireNonNull(classLoader, "classLoader must not be null");
        for (String name : BLOCK_PROPERTIES_CLASS_NAMES) {
            try {
                return Class.forName(name, true, classLoader);
            } catch (ClassNotFoundException ignored) {
            }
        }
        return null;
    }

    /**
     * Resolves the legacy Minecraft {@code Material} class.
     *
     * @param classLoader active game classloader
     * @return the {@code Material} class, or {@code null} if not found
     */
    public Class<?> resolveMaterialClass(ClassLoader classLoader) {
        Objects.requireNonNull(classLoader, "classLoader must not be null");
        for (String name : MATERIAL_CLASS_NAMES) {
            try {
                return Class.forName(name, true, classLoader);
            } catch (ClassNotFoundException ignored) {
            }
        }
        return null;
    }

    /**
     * Creates and configures a block properties instance suitable for passing to a Block constructor.
     *
     * @param classLoader active game classloader
     * @param idObj       the identifier or resource location object
     * @return a properties or material instance, or {@code null}
     */
    public Object createBlockProperties(ClassLoader classLoader, Object idObj) {
        Objects.requireNonNull(classLoader, "classLoader must not be null");

        Class<?> propsClass = resolveBlockPropertiesClass(classLoader);
        if (propsClass != null) {
            Object propsInstance = createModernPropertiesInstance(propsClass);
            if (propsInstance != null) {
                configureResourceKeyIfSupported(classLoader, propsInstance, idObj);
                return propsInstance;
            }
        }

        // Fallback for legacy 1.7.10 – 1.12.2: resolve Material.STONE / Material.ROCK
        Class<?> matClass = resolveMaterialClass(classLoader);
        if (matClass != null) {
            String[] commonMaterialFields = new String[]{"STONE", "rock", "IRON", "iron", "AIR", "air"};
            for (String fieldName : commonMaterialFields) {
                try {
                    Field f = matClass.getField(fieldName);
                    Object mat = f.get(null);
                    if (mat != null) {
                        return mat;
                    }
                } catch (Exception ignored) {
                }
            }
        }

        return null;
    }

    private static Object createModernPropertiesInstance(Class<?> propsClass) {
        // 1. Try static of() method
        try {
            Method ofMethod = propsClass.getMethod("of");
            return ofMethod.invoke(null);
        } catch (Exception ignored) {
        }

        // 2. Try no-arg constructor
        try {
            Constructor<?> ctor = propsClass.getDeclaredConstructor();
            ctor.setAccessible(true);
            return ctor.newInstance();
        } catch (Exception ignored) {
        }

        return null;
    }

    private static void configureResourceKeyIfSupported(
            ClassLoader classLoader,
            Object propsInstance,
            Object idObj) {
        if (idObj == null) {
            return;
        }
        try {
            Class<?> resourceKeyClass = Class.forName("net.minecraft.resources.ResourceKey", true, classLoader);
            Class<?> registriesKeyClass = Class.forName("net.minecraft.core.registries.Registries", true, classLoader);
            Field registriesBlockField = registriesKeyClass.getField("BLOCK");
            Object registriesBlockKey = registriesBlockField.get(null);

            Method createResourceKeyMethod = resourceKeyClass.getMethod(
                    "create",
                    resourceKeyClass,
                    idObj.getClass());
            Object blockKey = createResourceKeyMethod.invoke(null, registriesBlockKey, idObj);

            Method setIdMethod = propsInstance.getClass().getMethod("setId", resourceKeyClass);
            setIdMethod.invoke(propsInstance, blockKey);
        } catch (Exception ignored) {
        }
    }
}

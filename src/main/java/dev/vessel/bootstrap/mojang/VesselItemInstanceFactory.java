package dev.vessel.bootstrap.mojang;

import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.Objects;

/**
 * Reflectively constructs Minecraft {@code Item} instances across all Minecraft eras:
 * <ul>
 *   <li>Modern 26.3 / 1.20.5+: {@code new Item(Item.Properties.setId(ResourceKey))}</li>
 *   <li>Modern 1.13 – 1.20.4: {@code new Item(Item.Properties)}</li>
 *   <li>Legacy 1.7.10 – 1.12.2: {@code new Item()} with {@code setRegistryName}</li>
 * </ul>
 *
 * <p>Strictly adheres to Rule 17 with zero inner, nested, or anonymous classes, and zero lambdas.</p>
 *
 * @version 1.0.0
 */
public final class VesselItemInstanceFactory {

    private static final String[] ITEM_CLASS_NAMES = new String[]{
            "net.minecraft.world.item.Item",
            "net.minecraft.item.Item"
    };

    private static final String[] ITEM_PROPERTIES_CLASS_NAMES = new String[]{
            "net.minecraft.world.item.Item$Properties",
            "net.minecraft.item.Item$Properties"
    };

    public VesselItemInstanceFactory() {
    }

    /**
     * Finds the active Minecraft {@code Item} class.
     *
     * @param classLoader active game classloader
     * @return the {@code Item} class, or {@code null} if not found
     */
    public Class<?> resolveItemClass(ClassLoader classLoader) {
        Objects.requireNonNull(classLoader, "classLoader must not be null");
        for (String name : ITEM_CLASS_NAMES) {
            try {
                return Class.forName(name, true, classLoader);
            } catch (ClassNotFoundException ignored) {
            }
        }
        return null;
    }

    /**
     * Finds the active Minecraft {@code Item.Properties} class.
     *
     * @param classLoader active game classloader
     * @return the {@code Item.Properties} class, or {@code null} if not found
     */
    public Class<?> resolveItemPropertiesClass(ClassLoader classLoader) {
        Objects.requireNonNull(classLoader, "classLoader must not be null");
        for (String name : ITEM_PROPERTIES_CLASS_NAMES) {
            try {
                return Class.forName(name, true, classLoader);
            } catch (ClassNotFoundException ignored) {
            }
        }
        return null;
    }

    /**
     * Creates an {@code Item} instance for the specified identifier.
     *
     * @param classLoader active game classloader
     * @param idObj       the identifier or resource location object
     * @param itemStr     the namespaced string representation
     * @return a new {@code Item} instance, or {@code null} if creation failed
     */
    public Object createItemInstance(ClassLoader classLoader, Object idObj, String itemStr) {
        Objects.requireNonNull(classLoader, "classLoader must not be null");

        Class<?> itemClass = resolveItemClass(classLoader);
        if (itemClass == null) {
            return null;
        }

        Class<?> propsClass = resolveItemPropertiesClass(classLoader);
        if (propsClass != null) {
            try {
                Constructor<?> propsCtor = propsClass.getDeclaredConstructor();
                propsCtor.setAccessible(true);
                Object props = propsCtor.newInstance();

                // Check for modern 1.20.5+ / 26.3 props.setId(ResourceKey)
                configureResourceKeyIfSupported(classLoader, props, idObj);

                Constructor<?> itemCtor = itemClass.getDeclaredConstructor(propsClass);
                itemCtor.setAccessible(true);
                return itemCtor.newInstance(props);
            } catch (Exception ignored) {
            }
        }

        // Fallback for pre-1.13: try no-argument constructor
        try {
            Constructor<?> noArgCtor = itemClass.getDeclaredConstructor();
            noArgCtor.setAccessible(true);
            Object itemInstance = noArgCtor.newInstance();

            // Set registry name if supported (1.9 – 1.12.2)
            if (idObj != null) {
                try {
                    Method setRegMethod = itemClass.getMethod("setRegistryName", idObj.getClass());
                    setRegMethod.invoke(itemInstance, idObj);
                } catch (Exception e1) {
                    try {
                        Method setRegString = itemClass.getMethod("setRegistryName", String.class);
                        setRegString.invoke(itemInstance, itemStr);
                    } catch (Exception ignored) {
                    }
                }
            }

            // Set unlocalized name if supported (1.7.10 – 1.12.2)
            try {
                Method setUnlocalizedMethod = itemClass.getMethod("setUnlocalizedName", String.class);
                setUnlocalizedMethod.invoke(itemInstance, itemStr);
            } catch (Exception ignored) {
            }

            return itemInstance;
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
            Field registriesItemField = registriesKeyClass.getField("ITEM");
            Object registriesItemKey = registriesItemField.get(null);

            Method createResourceKeyMethod = resourceKeyClass.getMethod(
                    "create",
                    resourceKeyClass,
                    idObj.getClass());
            Object itemKey = createResourceKeyMethod.invoke(null, registriesItemKey, idObj);

            Method setIdMethod = propsInstance.getClass().getMethod("setId", resourceKeyClass);
            setIdMethod.invoke(propsInstance, itemKey);
        } catch (Exception ignored) {
        }
    }
}

package dev.vessel.bootstrap.mojang;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Reflectively injects custom mod item instances into the native Minecraft item registry across
 * all historical eras (1.7.10 – 26.3+), supporting {@code BuiltInRegistries.ITEM}, {@code Registry.ITEM},
 * {@code Item.REGISTRY}, {@code Item.itemRegistry}, and FML {@code GameRegistry}.
 *
 * <p>Strictly adheres to Rule 17 with zero inner, nested, or anonymous classes, and zero lambdas.</p>
 *
 * @version 1.0.0
 */
public final class VesselItemRegistryInjector {

    private static final AtomicInteger LEGACY_ID_COUNTER = new AtomicInteger(10000);

    public VesselItemRegistryInjector() {
    }

    /**
     * Resolves the active Minecraft Item registry instance across eras.
     *
     * @param classLoader active game classloader
     * @return the item registry instance, or {@code null} if not found
     */
    public Object resolveItemRegistry(ClassLoader classLoader) {
        Objects.requireNonNull(classLoader, "classLoader must not be null");

        // 1. Modern 1.19.3+ BuiltInRegistries.ITEM
        try {
            Class<?> builtIn = Class.forName("net.minecraft.core.registries.BuiltInRegistries", true, classLoader);
            Field f = builtIn.getField("ITEM");
            Object reg = f.get(null);
            if (reg != null) {
                return reg;
            }
        } catch (Exception ignored) {
        }

        // 2. Modern 1.17 – 1.19.2 Registry.ITEM
        try {
            Class<?> registryClass = Class.forName("net.minecraft.core.Registry", true, classLoader);
            Field f = registryClass.getField("ITEM");
            Object reg = f.get(null);
            if (reg != null) {
                return reg;
            }
        } catch (Exception ignored) {
        }

        // 3. Modern 1.14 – 1.16 Registry.ITEM or IRegistry.ITEM
        String[] altRegistryClassNames = new String[]{
                "net.minecraft.util.registry.Registry",
                "net.minecraft.core.IRegistry"
        };
        for (String cName : altRegistryClassNames) {
            try {
                Class<?> c = Class.forName(cName, true, classLoader);
                Field f = c.getField("ITEM");
                Object reg = f.get(null);
                if (reg != null) {
                    return reg;
                }
            } catch (Exception ignored) {
            }
        }

        // 4. Legacy 1.7.10 – 1.13.2 Item.REGISTRY or Item.itemRegistry
        String[] itemClassNames = new String[]{
                "net.minecraft.world.item.Item",
                "net.minecraft.item.Item"
        };
        for (String iName : itemClassNames) {
            try {
                Class<?> itemClass = Class.forName(iName, true, classLoader);
                try {
                    Field f = itemClass.getField("REGISTRY");
                    Object reg = f.get(null);
                    if (reg != null) {
                        return reg;
                    }
                } catch (NoSuchFieldException e1) {
                    try {
                        Field f = itemClass.getField("itemRegistry");
                        Object reg = f.get(null);
                        if (reg != null) {
                            return reg;
                        }
                    } catch (NoSuchFieldException ignored) {
                    }
                }
            } catch (Exception ignored) {
            }
        }

        return null;
    }

    /**
     * Injects an item instance into the active registry.
     *
     * @param classLoader  active game classloader
     * @param itemRegistry active registry object
     * @param idObj        the identifier or resource location object
     * @param itemStr      raw namespaced string representation
     * @param itemInstance the Item instance to register
     * @return {@code true} if registration succeeded
     */
    public boolean injectItem(
            ClassLoader classLoader,
            Object itemRegistry,
            Object idObj,
            String itemStr,
            Object itemInstance) {
        Objects.requireNonNull(classLoader, "classLoader must not be null");
        Objects.requireNonNull(itemInstance, "itemInstance must not be null");

        // Unfreeze registry if needed
        Field frozenField = null;
        if (itemRegistry != null) {
            frozenField = findFieldInHierarchy(itemRegistry.getClass(), "frozen");
            if (frozenField != null) {
                try {
                    frozenField.setAccessible(true);
                    frozenField.set(itemRegistry, false);
                } catch (Exception ignored) {
                }
            }
        }

        boolean injected = false;
        try {
            if (itemRegistry != null) {
                injected = tryRegisterOnRegistry(classLoader, itemRegistry, idObj, itemStr, itemInstance);
            }
            if (!injected) {
                injected = tryRegisterOnItemClass(classLoader, idObj, itemStr, itemInstance);
            }
            if (!injected) {
                injected = tryRegisterOnForge(classLoader, itemStr, itemInstance);
            }
        } finally {
            if (frozenField != null && itemRegistry != null) {
                try {
                    frozenField.set(itemRegistry, true);
                } catch (Exception ignored) {
                }
            }
        }

        return injected;
    }

    /**
     * Checks whether the given registry already contains the identifier.
     *
     * @param registry active item registry
     * @param idObj    identifier object
     * @param itemStr  raw string identifier
     * @return {@code true} if present
     */
    public boolean isAlreadyRegistered(Object registry, Object idObj, String itemStr) {
        if (registry == null) {
            return false;
        }

        try {
            Method containsKeyMethod = registry.getClass().getMethod("containsKey", idObj.getClass());
            return (boolean) containsKeyMethod.invoke(registry, idObj);
        } catch (Exception ignored) {
        }

        try {
            Method getMethod = registry.getClass().getMethod("get", idObj.getClass());
            Object result = getMethod.invoke(registry, idObj);
            if (result != null) {
                return true;
            }
        } catch (Exception ignored) {
        }

        try {
            Method getObjectMethod = registry.getClass().getMethod("getObject", Object.class);
            Object result = getObjectMethod.invoke(registry, idObj);
            if (result != null) {
                return true;
            }
        } catch (Exception ignored) {
        }

        try {
            Method getObjectStringMethod = registry.getClass().getMethod("getObject", String.class);
            Object result = getObjectStringMethod.invoke(registry, itemStr);
            if (result != null) {
                return true;
            }
        } catch (Exception ignored) {
        }

        return false;
    }

    private static boolean tryRegisterOnRegistry(
            ClassLoader classLoader,
            Object itemRegistry,
            Object idObj,
            String itemStr,
            Object itemInstance) {
        // 1. Try static Registry.register(registry, id, item)
        String[] registryClassNames = new String[]{
                "net.minecraft.core.Registry",
                "net.minecraft.core.registries.BuiltInRegistries",
                "net.minecraft.util.registry.Registry"
        };
        for (String cName : registryClassNames) {
            try {
                Class<?> regClass = Class.forName(cName, true, classLoader);
                for (Method m : regClass.getMethods()) {
                    if ("register".equals(m.getName()) && m.getParameterCount() == 3) {
                        try {
                            m.invoke(null, itemRegistry, idObj, itemInstance);
                            return true;
                        } catch (Exception ignored) {
                        }
                    }
                }
            } catch (Exception ignored) {
            }
        }

        // 2. Try instance method register(id, item) or register(int, id, item)
        for (Method m : itemRegistry.getClass().getMethods()) {
            if ("register".equals(m.getName())) {
                if (m.getParameterCount() == 2) {
                    try {
                        m.invoke(itemRegistry, idObj, itemInstance);
                        return true;
                    } catch (Exception ignored) {
                    }
                } else if (m.getParameterCount() == 3) {
                    try {
                        m.invoke(itemRegistry, LEGACY_ID_COUNTER.incrementAndGet(), idObj, itemInstance);
                        return true;
                    } catch (Exception ignored) {
                    }
                }
            }
        }

        // 3. Try legacy putObject(id, item) or putObject(String, item)
        try {
            Method putObjMethod = itemRegistry.getClass().getMethod("putObject", Object.class, Object.class);
            putObjMethod.invoke(itemRegistry, idObj, itemInstance);
            return true;
        } catch (Exception ignored) {
        }

        try {
            Method putStrMethod = itemRegistry.getClass().getMethod("putObject", String.class, Object.class);
            putStrMethod.invoke(itemRegistry, itemStr, itemInstance);
            return true;
        } catch (Exception ignored) {
        }

        return false;
    }

    private static boolean tryRegisterOnItemClass(
            ClassLoader classLoader,
            Object idObj,
            String itemStr,
            Object itemInstance) {
        String[] itemClassNames = new String[]{
                "net.minecraft.world.item.Item",
                "net.minecraft.item.Item"
        };
        for (String iName : itemClassNames) {
            try {
                Class<?> itemClass = Class.forName(iName, true, classLoader);
                for (Method m : itemClass.getMethods()) {
                    if ("registerItem".equals(m.getName())) {
                        if (m.getParameterCount() == 2) {
                            try {
                                m.invoke(null, idObj, itemInstance);
                                return true;
                            } catch (Exception ignored) {
                            }
                        } else if (m.getParameterCount() == 3) {
                            try {
                                m.invoke(null, LEGACY_ID_COUNTER.incrementAndGet(), idObj, itemInstance);
                                return true;
                            } catch (Exception ignored) {
                            }
                        }
                    }
                }
            } catch (Exception ignored) {
            }
        }
        return false;
    }

    private static boolean tryRegisterOnForge(
            ClassLoader classLoader,
            String itemStr,
            Object itemInstance) {
        String[] forgeRegistryClasses = new String[]{
                "net.minecraftforge.fml.common.registry.GameRegistry",
                "cpw.mods.fml.common.registry.GameRegistry"
        };
        for (String fName : forgeRegistryClasses) {
            try {
                Class<?> forgeClass = Class.forName(fName, true, classLoader);
                for (Method m : forgeClass.getMethods()) {
                    if ("register".equals(m.getName()) && m.getParameterCount() == 1) {
                        try {
                            m.invoke(null, itemInstance);
                            return true;
                        } catch (Exception ignored) {
                        }
                    }
                    if ("registerItem".equals(m.getName())) {
                        if (m.getParameterCount() == 2) {
                            try {
                                m.invoke(null, itemInstance, itemStr);
                                return true;
                            } catch (Exception ignored) {
                            }
                        }
                    }
                }
            } catch (Exception ignored) {
            }
        }
        return false;
    }

    private static Field findFieldInHierarchy(Class<?> clazz, String fieldName) {
        Class<?> current = clazz;
        while (current != null && current != Object.class) {
            try {
                return current.getDeclaredField(fieldName);
            } catch (NoSuchFieldException e) {
                current = current.getSuperclass();
            }
        }
        return null;
    }
}

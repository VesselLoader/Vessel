package dev.vessel.bootstrap.mojang;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Reflectively injects custom mod block instances into the native Minecraft block registry across
 * all historical eras (1.7.10 – 26.3+), supporting {@code BuiltInRegistries.BLOCK}, {@code Registry.BLOCK},
 * {@code Block.REGISTRY}, {@code Block.blockRegistry}, and FML {@code GameRegistry}.
 *
 * <p>Strictly adheres to Rule 17 with zero inner, nested, or anonymous classes, and zero lambdas.</p>
 *
 * @version 1.0.0
 */
public final class VesselBlockRegistryInjector {

    private static final AtomicInteger LEGACY_BLOCK_ID_COUNTER = new AtomicInteger(4096);

    public VesselBlockRegistryInjector() {
    }

    /**
     * Resolves the active Minecraft Block registry instance across eras.
     *
     * @param classLoader active game classloader
     * @return the block registry instance, or {@code null} if not found
     */
    public Object resolveBlockRegistry(ClassLoader classLoader) {
        Objects.requireNonNull(classLoader, "classLoader must not be null");

        // 1. Modern 1.19.3+ BuiltInRegistries.BLOCK
        try {
            Class<?> builtIn = Class.forName("net.minecraft.core.registries.BuiltInRegistries", true, classLoader);
            Field f = builtIn.getField("BLOCK");
            Object reg = f.get(null);
            if (reg != null) {
                return reg;
            }
        } catch (Exception ignored) {
        }

        // 2. Modern 1.17 – 1.19.2 Registry.BLOCK
        try {
            Class<?> registryClass = Class.forName("net.minecraft.core.Registry", true, classLoader);
            Field f = registryClass.getField("BLOCK");
            Object reg = f.get(null);
            if (reg != null) {
                return reg;
            }
        } catch (Exception ignored) {
        }

        // 3. Modern 1.14 – 1.16 Registry.BLOCK or IRegistry.BLOCK
        String[] altRegistryClassNames = new String[]{
                "net.minecraft.util.registry.Registry",
                "net.minecraft.core.IRegistry"
        };
        for (String cName : altRegistryClassNames) {
            try {
                Class<?> c = Class.forName(cName, true, classLoader);
                Field f = c.getField("BLOCK");
                Object reg = f.get(null);
                if (reg != null) {
                    return reg;
                }
            } catch (Exception ignored) {
            }
        }

        // 4. Legacy 1.7.10 – 1.13.2 Block.REGISTRY or Block.blockRegistry
        String[] blockClassNames = new String[]{
                "net.minecraft.world.level.block.Block",
                "net.minecraft.block.Block"
        };
        for (String bName : blockClassNames) {
            try {
                Class<?> blockClass = Class.forName(bName, true, classLoader);
                try {
                    Field f = blockClass.getField("REGISTRY");
                    Object reg = f.get(null);
                    if (reg != null) {
                        return reg;
                    }
                } catch (NoSuchFieldException e1) {
                    try {
                        Field f = blockClass.getField("blockRegistry");
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
     * Injects a block instance into the active registry.
     *
     * @param classLoader   active game classloader
     * @param blockRegistry active registry object
     * @param idObj         the identifier or resource location object
     * @param blockStr      raw namespaced string representation
     * @param blockInstance the Block instance to register
     * @return {@code true} if registration succeeded
     */
    public boolean injectBlock(
            ClassLoader classLoader,
            Object blockRegistry,
            Object idObj,
            String blockStr,
            Object blockInstance) {
        Objects.requireNonNull(classLoader, "classLoader must not be null");
        Objects.requireNonNull(blockInstance, "blockInstance must not be null");

        // Unfreeze registry if needed
        Field frozenField = null;
        if (blockRegistry != null) {
            frozenField = findFieldInHierarchy(blockRegistry.getClass(), "frozen");
            if (frozenField != null) {
                try {
                    frozenField.setAccessible(true);
                    frozenField.set(blockRegistry, false);
                } catch (Exception ignored) {
                }
            }
        }

        boolean injected = false;
        try {
            if (blockRegistry != null) {
                injected = tryRegisterOnRegistry(classLoader, blockRegistry, idObj, blockStr, blockInstance);
            }
            if (!injected) {
                injected = tryRegisterOnBlockClass(classLoader, idObj, blockStr, blockInstance);
            }
            if (!injected) {
                injected = tryRegisterOnForge(classLoader, blockStr, blockInstance);
            }
        } finally {
            if (frozenField != null && blockRegistry != null) {
                try {
                    frozenField.set(blockRegistry, true);
                } catch (Exception ignored) {
                }
            }
        }

        return injected;
    }

    /**
     * Checks whether the given registry already contains the identifier.
     *
     * @param registry active block registry
     * @param idObj    identifier object
     * @param blockStr raw string identifier
     * @return {@code true} if present
     */
    public boolean isAlreadyRegistered(Object registry, Object idObj, String blockStr) {
        if (registry == null) {
            return false;
        }

        if (idObj != null) {
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
        }

        if (blockStr != null) {
            try {
                Method getObjectStringMethod = registry.getClass().getMethod("getObject", String.class);
                Object result = getObjectStringMethod.invoke(registry, blockStr);
                if (result != null) {
                    return true;
                }
            } catch (Exception ignored) {
            }
        }

        return false;
    }

    private static boolean tryRegisterOnRegistry(
            ClassLoader classLoader,
            Object blockRegistry,
            Object idObj,
            String blockStr,
            Object blockInstance) {
        // 1. Try static Registry.register(registry, id, block)
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
                            m.invoke(null, blockRegistry, idObj, blockInstance);
                            return true;
                        } catch (Exception ignored) {
                        }
                    }
                }
            } catch (Exception ignored) {
            }
        }

        // 2. Try instance method register(id, block) or register(int, id, block)
        for (Method m : blockRegistry.getClass().getMethods()) {
            if ("register".equals(m.getName())) {
                if (m.getParameterCount() == 2) {
                    try {
                        m.invoke(blockRegistry, idObj, blockInstance);
                        return true;
                    } catch (Exception ignored) {
                    }
                } else if (m.getParameterCount() == 3) {
                    try {
                        m.invoke(blockRegistry, LEGACY_BLOCK_ID_COUNTER.incrementAndGet(), idObj, blockInstance);
                        return true;
                    } catch (Exception ignored) {
                    }
                }
            }
        }

        // 3. Try legacy putObject(id, block) or putObject(String, block)
        try {
            Method putObjMethod = blockRegistry.getClass().getMethod("putObject", Object.class, Object.class);
            putObjMethod.invoke(blockRegistry, idObj, blockInstance);
            return true;
        } catch (Exception ignored) {
        }

        try {
            Method putStrMethod = blockRegistry.getClass().getMethod("putObject", String.class, Object.class);
            putStrMethod.invoke(blockRegistry, blockStr, blockInstance);
            return true;
        } catch (Exception ignored) {
        }

        return false;
    }

    private static boolean tryRegisterOnBlockClass(
            ClassLoader classLoader,
            Object idObj,
            String blockStr,
            Object blockInstance) {
        String[] blockClassNames = new String[]{
                "net.minecraft.world.level.block.Block",
                "net.minecraft.block.Block"
        };
        for (String bName : blockClassNames) {
            try {
                Class<?> blockClass = Class.forName(bName, true, classLoader);
                for (Method m : blockClass.getMethods()) {
                    if ("registerBlock".equals(m.getName())) {
                        if (m.getParameterCount() == 2) {
                            try {
                                m.invoke(null, idObj, blockInstance);
                                return true;
                            } catch (Exception ignored) {
                            }
                        } else if (m.getParameterCount() == 3) {
                            try {
                                m.invoke(null, LEGACY_BLOCK_ID_COUNTER.incrementAndGet(), idObj, blockInstance);
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
            String blockStr,
            Object blockInstance) {
        String[] forgeRegistryClasses = new String[]{
                "net.minecraftforge.fml.common.registry.GameRegistry",
                "cpw.mods.fml.common.registry.GameRegistry"
        };
        for (String fName : forgeRegistryClasses) {
            try {
                Class<?> forgeClass = Class.forName(fName, true, classLoader);
                for (Method m : forgeClass.getMethods()) {
                    if ("registerBlock".equals(m.getName())) {
                        if (m.getParameterCount() == 2) {
                            try {
                                m.invoke(null, blockInstance, blockStr);
                                return true;
                            } catch (Exception ignored) {
                            }
                        } else if (m.getParameterCount() == 1) {
                            try {
                                m.invoke(null, blockInstance);
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

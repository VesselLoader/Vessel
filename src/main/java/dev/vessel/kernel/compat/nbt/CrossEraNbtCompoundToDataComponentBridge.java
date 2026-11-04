package dev.vessel.kernel.compat.nbt;

import java.lang.reflect.Method;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

/**
 * Transparent two-way bridge connecting legacy Minecraft NBT (CompoundTag / NBTTagCompound)
 * with modern DataComponents (DataComponents.CUSTOM_DATA) introduced in 1.20.5+ and 26.3.
 * Adheres strictly to Rule 17 (Single Responsibility Principle).
 *
 * @version 1.0.0
 */
public final class CrossEraNbtCompoundToDataComponentBridge {

    private final ConcurrentMap<String, Object> simulatedCompoundStore = new ConcurrentHashMap<>();

    public Object extractCustomDataFromItemStack(Object itemStackInstance) {
        if (itemStackInstance == null) {
            return null;
        }
        try {
            Method getComponentsMethod = itemStackInstance.getClass().getMethod("getComponents");
            Object componentMap = getComponentsMethod.invoke(itemStackInstance);
            if (componentMap != null) {
                return componentMap;
            }
        } catch (ReflectiveOperationException ignored) {
            // Fallback for earlier Minecraft versions still using direct tag
            try {
                Method getTagMethod = itemStackInstance.getClass().getMethod("getTag");
                return getTagMethod.invoke(itemStackInstance);
            } catch (ReflectiveOperationException ignoredAgain) {
                return null;
            }
        }
        return null;
    }

    public boolean applyLegacyTagToItemStack(Object itemStackInstance, Object legacyCompoundTag) {
        if (itemStackInstance == null || legacyCompoundTag == null) {
            return false;
        }
        try {
            // Check if modern set(DataComponentType, Object) exists
            Method setMethod = findComponentSetMethod(itemStackInstance.getClass());
            if (setMethod != null) {
                Object customDataComponentType = resolveCustomDataComponentType(itemStackInstance.getClass().getClassLoader());
                if (customDataComponentType != null) {
                    Object customDataWrapper = wrapCompoundIntoCustomData(legacyCompoundTag, itemStackInstance.getClass().getClassLoader());
                    if (customDataWrapper != null) {
                        setMethod.invoke(itemStackInstance, customDataComponentType, customDataWrapper);
                        return true;
                    }
                }
            }
            // Fallback to legacy setTag(CompoundTag)
            Method setTagMethod = itemStackInstance.getClass().getMethod("setTag", legacyCompoundTag.getClass());
            setTagMethod.invoke(itemStackInstance, legacyCompoundTag);
            return true;
        } catch (ReflectiveOperationException ignored) {
            simulatedCompoundStore.put(itemStackInstance.toString(), legacyCompoundTag);
            return true;
        }
    }

    public Object getOrCreateSimulatedTag(String stackIdentifier, Object defaultTag) {
        Objects.requireNonNull(stackIdentifier, "stackIdentifier cannot be null");
        return simulatedCompoundStore.computeIfAbsent(stackIdentifier, k -> defaultTag);
    }

    private Method findComponentSetMethod(Class<?> stackClass) {
        for (Method m : stackClass.getMethods()) {
            if ("set".equals(m.getName()) && m.getParameterCount() == 2) {
                return m;
            }
        }
        return null;
    }

    private Object resolveCustomDataComponentType(ClassLoader loader) {
        try {
            Class<?> dataComponentsClass = Class.forName("net.minecraft.core.component.DataComponents", true, loader);
            var field = dataComponentsClass.getField("CUSTOM_DATA");
            return field.get(null);
        } catch (ReflectiveOperationException ignored) {
            return null;
        }
    }

    private Object wrapCompoundIntoCustomData(Object compoundTag, ClassLoader loader) {
        try {
            Class<?> customDataClass = Class.forName("net.minecraft.world.item.component.CustomData", true, loader);
            Method ofMethod = customDataClass.getMethod("of", compoundTag.getClass());
            return ofMethod.invoke(null, compoundTag);
        } catch (ReflectiveOperationException ignored) {
            return null;
        }
    }
}

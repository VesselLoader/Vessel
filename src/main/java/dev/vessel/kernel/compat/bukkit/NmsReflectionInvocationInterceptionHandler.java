package dev.vessel.kernel.compat.bukkit;

import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

/**
 * Intercepts legacy Bukkit NMS reflection lookups (net.minecraft.server.v1_...)
 * and redirects them cleanly to modern unversioned MojMap classes.
 * Adheres strictly to Rule 17 (Single Responsibility Principle).
 *
 * @version 1.0.0
 */
public final class NmsReflectionInvocationInterceptionHandler {

    private final ConcurrentMap<String, String> nmsClassRedirectMap = new ConcurrentHashMap<>();

    public NmsReflectionInvocationInterceptionHandler() {
        populateStandardNmsRedirects();
    }

    public Class<?> redirectClassLookup(String className, ClassLoader targetLoader) throws ClassNotFoundException {
        Objects.requireNonNull(className, "className cannot be null");
        Objects.requireNonNull(targetLoader, "targetLoader cannot be null");
        String redirectedName = resolveRedirectedClassName(className);
        return Class.forName(redirectedName, true, targetLoader);
    }

    public String resolveRedirectedClassName(String rawClassName) {
        if (rawClassName == null) {
            return null;
        }
        for (var entry : nmsClassRedirectMap.entrySet()) {
            if (rawClassName.contains(entry.getKey())) {
                return entry.getValue();
            }
        }
        return rawClassName;
    }

    private void populateStandardNmsRedirects() {
        nmsClassRedirectMap.put("EntityPlayer", "net.minecraft.server.level.ServerPlayer");
        nmsClassRedirectMap.put("WorldServer", "net.minecraft.server.level.ServerLevel");
        nmsClassRedirectMap.put("MinecraftServer", "net.minecraft.server.MinecraftServer");
        nmsClassRedirectMap.put("PlayerConnection", "net.minecraft.server.network.ServerGamePacketListenerImpl");
        nmsClassRedirectMap.put("NetworkManager", "net.minecraft.network.Connection");
        nmsClassRedirectMap.put("ItemStack", "net.minecraft.world.item.ItemStack");
        nmsClassRedirectMap.put("NBTTagCompound", "net.minecraft.nbt.CompoundTag");
        nmsClassRedirectMap.put("BlockPosition", "net.minecraft.core.BlockPos");
    }
}

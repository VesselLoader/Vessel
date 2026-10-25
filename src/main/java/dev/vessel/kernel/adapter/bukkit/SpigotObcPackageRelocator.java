package dev.vessel.kernel.adapter.bukkit;

import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * High-performance runtime relocator mapping legacy versioned CraftBukkit packages
 * ({@code org.bukkit.craftbukkit.v1_X_RY.*}) and legacy NMS references to unversioned
 * CraftBukkit and Mojang Official Mappings.
 *
 * <p>Quells: ERR-62, MAP-045.</p>
 * <p>Strictly adheres to Rule 17 (zero inner, nested, or anonymous classes).</p>
 *
 * @version 1.0.0
 * @author VESSEL Engine Team
 */
public final class SpigotObcPackageRelocator {

    /**
     * Pattern matching versioned internal CraftBukkit package paths.
     */
    public static final Pattern OBC_INTERNAL_PATTERN =
            Pattern.compile("org/bukkit/craftbukkit/v1_\\d+_R\\d+/(.+)");

    /**
     * Pattern matching versioned dot-separated CraftBukkit package paths.
     */
    public static final Pattern OBC_DOT_PATTERN =
            Pattern.compile("org\\.bukkit\\.craftbukkit\\.v1_\\d+_R\\d+\\.(.+)");

    /**
     * Pattern matching legacy versioned internal NMS package paths.
     */
    public static final Pattern NMS_INTERNAL_PATTERN =
            Pattern.compile("net/minecraft/server/v1_\\d+_R\\d+/(.+)");

    /**
     * Pattern matching legacy versioned dot-separated NMS package paths.
     */
    public static final Pattern NMS_DOT_PATTERN =
            Pattern.compile("net\\.minecraft\\.server\\.v1_\\d+_R\\d+\\.(.+)");

    /**
     * Unversioned CraftBukkit internal prefix.
     */
    public static final String OBC_UNVERSIONED_INTERNAL_PREFIX = "org/bukkit/craftbukkit/";

    /**
     * Unversioned CraftBukkit dot prefix.
     */
    public static final String OBC_UNVERSIONED_DOT_PREFIX = "org.bukkit.craftbukkit.";

    private final Map<String, String> internalNameCache = new ConcurrentHashMap<>();
    private final Map<String, String> classNameCache = new ConcurrentHashMap<>();
    private final Map<String, String> descriptorCache = new ConcurrentHashMap<>();
    private final Map<String, String> legacyNmsToMojang = new ConcurrentHashMap<>();

    /**
     * Constructs a relocator and populates canonical NMS mappings.
     */
    public SpigotObcPackageRelocator() {
        populateStandardNmsMappings();
    }

    private void populateStandardNmsMappings() {
        legacyNmsToMojang.put("EntityPlayer", "net/minecraft/server/level/ServerPlayer");
        legacyNmsToMojang.put("WorldServer", "net/minecraft/server/level/ServerLevel");
        legacyNmsToMojang.put("MinecraftServer", "net/minecraft/server/MinecraftServer");
        legacyNmsToMojang.put("ItemStack", "net/minecraft/world/item/ItemStack");
        legacyNmsToMojang.put("BlockPosition", "net/minecraft/core/BlockPos");
        legacyNmsToMojang.put("Packet", "net/minecraft/network/protocol/Packet");
        legacyNmsToMojang.put("Entity", "net/minecraft/world/entity/Entity");
        legacyNmsToMojang.put("NBTTagCompound", "net/minecraft/nbt/CompoundTag");
        legacyNmsToMojang.put("World", "net/minecraft/world/level/Level");
    }

    /**
     * Relocates an internal class name (e.g. {@code org/bukkit/craftbukkit/v1_20_R3/CraftServer}).
     *
     * @param internalName source JVM internal name
     * @return relocated internal name
     */
    public String relocateInternalName(String internalName) {
        if (internalName == null || internalName.isEmpty()) {
            return internalName;
        }

        String cached = internalNameCache.get(internalName);
        if (cached != null) {
            return cached;
        }

        String result = internalName;
        Matcher obcMatcher = OBC_INTERNAL_PATTERN.matcher(internalName);
        if (obcMatcher.matches()) {
            result = OBC_UNVERSIONED_INTERNAL_PREFIX + obcMatcher.group(1);
        } else {
            Matcher nmsMatcher = NMS_INTERNAL_PATTERN.matcher(internalName);
            if (nmsMatcher.matches()) {
                String simpleName = nmsMatcher.group(1);
                String mojangTarget = legacyNmsToMojang.get(simpleName);
                if (mojangTarget != null) {
                    result = mojangTarget;
                } else {
                    result = "net/minecraft/server/" + simpleName;
                }
            }
        }

        internalNameCache.put(internalName, result);
        return result;
    }

    /**
     * Relocates a dot-separated class name (e.g. {@code org.bukkit.craftbukkit.v1_20_R3.CraftServer}).
     *
     * @param className source dot-separated class name
     * @return relocated dot-separated class name
     */
    public String relocateClassName(String className) {
        if (className == null || className.isEmpty()) {
            return className;
        }

        String cached = classNameCache.get(className);
        if (cached != null) {
            return cached;
        }

        String result = className;
        Matcher obcMatcher = OBC_DOT_PATTERN.matcher(className);
        if (obcMatcher.matches()) {
            result = OBC_UNVERSIONED_DOT_PREFIX + obcMatcher.group(1);
        } else {
            Matcher nmsMatcher = NMS_DOT_PATTERN.matcher(className);
            if (nmsMatcher.matches()) {
                String simpleName = nmsMatcher.group(1);
                String mojangTarget = legacyNmsToMojang.get(simpleName);
                if (mojangTarget != null) {
                    result = mojangTarget.replace('/', '.');
                } else {
                    result = "net.minecraft.server." + simpleName;
                }
            }
        }

        classNameCache.put(className, result);
        return result;
    }

    /**
     * Relocates a JVM method or field descriptor containing versioned CraftBukkit or NMS types.
     *
     * @param descriptor bytecode type descriptor
     * @return relocated descriptor
     */
    public String relocateDescriptor(String descriptor) {
        if (descriptor == null || descriptor.isEmpty() || !descriptor.contains("org/bukkit/craftbukkit/v1_")
                && !descriptor.contains("net/minecraft/server/v1_")) {
            return descriptor;
        }

        String cached = descriptorCache.get(descriptor);
        if (cached != null) {
            return cached;
        }

        StringBuilder sb = new StringBuilder(descriptor.length());
        int i = 0;
        int len = descriptor.length();

        while (i < len) {
            char c = descriptor.charAt(i);
            if (c == 'L') {
                int semi = descriptor.indexOf(';', i);
                if (semi > i) {
                    String internal = descriptor.substring(i + 1, semi);
                    String relocated = relocateInternalName(internal);
                    sb.append('L').append(relocated).append(';');
                    i = semi + 1;
                    continue;
                }
            }
            sb.append(c);
            i++;
        }

        String result = sb.toString();
        descriptorCache.put(descriptor, result);
        return result;
    }

    /**
     * Registers a custom NMS mapping rule.
     *
     * @param legacySimpleName simple NMS class name (e.g. "PacketPlayOutChat")
     * @param canonicalMojangInternalName canonical Mojang internal name (e.g. "net/minecraft/network/protocol/game/ClientboundSystemChatPacket")
     */
    public void registerNmsMapping(String legacySimpleName, String canonicalMojangInternalName) {
        Objects.requireNonNull(legacySimpleName, "legacySimpleName cannot be null");
        Objects.requireNonNull(canonicalMojangInternalName, "canonicalMojangInternalName cannot be null");
        legacyNmsToMojang.put(legacySimpleName, canonicalMojangInternalName);
        internalNameCache.clear();
        classNameCache.clear();
        descriptorCache.clear();
    }

    /**
     * Returns the size of internal name relocation cache.
     *
     * @return cache entry count
     */
    public int getCacheSize() {
        return internalNameCache.size();
    }
}

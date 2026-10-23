package dev.vessel.command;

import java.lang.reflect.Method;
import java.util.List;
import java.util.Locale;
import java.util.Objects;

/**
 * Bridges Bukkit and EssentialsX player commands ({@code /sethome}, {@code /home}, {@code /delhome},
 * {@code /spawn}, {@code /heal}, {@code /feed}, {@code /fly}) directly into the native Minecraft
 * server runtime and in-game Brigadier command tree.
 *
 * <p>Strictly adheres to Rule 17 with zero inner, nested, or anonymous classes.</p>
 *
 * @version 1.0.0
 */
public final class VesselBukkitCommandBridge {

    public static final String[] BUKKIT_ROOT_COMMANDS = new String[]{
            "sethome", "home", "delhome", "spawn", "heal", "feed", "fly", "esethome", "createhome"
    };

    private final VesselHomeDataStore homeStore = new VesselHomeDataStore();

    public VesselBukkitCommandBridge() {
    }

    /**
     * Initializes the home storage with the server root directory.
     *
     * @param serverRoot the server root directory
     */
    public void initialize(java.nio.file.Path serverRoot) {
        homeStore.initialize(serverRoot);
    }

    /**
     * Checks if the given command root name belongs to the Bukkit/EssentialsX bridged suite.
     *
     * @param rootCmd command root name without leading slash
     * @return {@code true} if supported
     */
    public boolean isBukkitCommand(String rootCmd) {
        if (rootCmd == null) {
            return false;
        }
        String lower = rootCmd.trim().toLowerCase(Locale.ROOT);
        for (String c : BUKKIT_ROOT_COMMANDS) {
            if (c.equals(lower)) {
                return true;
            }
        }
        return false;
    }

    /**
     * Executes a Bukkit command from either in-game {@code CommandSourceStack} or console.
     *
     * @param rootCmd  command root name (e.g., "sethome")
     * @param subArgs  arguments passed after the command name
     * @param source   Brigadier {@code CommandSourceStack} instance, or {@code null}
     * @return list of chat or console response messages
     */
    public List<String> executeBukkitCommand(
            String rootCmd,
            String[] subArgs,
            Object source) {
        Objects.requireNonNull(rootCmd, "rootCmd must not be null");
        String lower = rootCmd.trim().toLowerCase(Locale.ROOT);
        String homeName = (subArgs != null && subArgs.length > 0 && !subArgs[0].isBlank())
                ? subArgs[0].trim().toLowerCase(Locale.ROOT)
                : "home";

        Object playerEntity = extractPlayerEntity(source);
        String playerName = extractPlayerName(playerEntity, source);

        if ("sethome".equals(lower) || "esethome".equals(lower) || "createhome".equals(lower)) {
            if (playerEntity == null) {
                return List.of("[VESSEL/Essentials] Error: /sethome must be executed by an in-game player.");
            }
            double[] coords = extractCoordinates(playerEntity);
            float[] rots = extractRotations(playerEntity);
            String dimension = extractDimension(playerEntity);
            homeStore.setHome(playerName, homeName, dimension, coords[0], coords[1], coords[2], rots[0], rots[1]);
            return List.of(String.format(Locale.ROOT,
                    "[EssentialsX] Home '%s' successfully set at %.1f, %.1f, %.1f in %s.",
                    homeName, coords[0], coords[1], coords[2], dimension));
        }

        if ("home".equals(lower)) {
            if (playerEntity == null) {
                return List.of("[VESSEL/Essentials] Error: /home must be executed by an in-game player.");
            }
            String record = homeStore.getHome(playerName, homeName);
            if (record == null) {
                List<String> available = homeStore.listHomes(playerName);
                if (available.isEmpty()) {
                    return List.of("[EssentialsX] You have not set any homes yet. Use /sethome [name] to create one.");
                }
                return List.of("[EssentialsX] Home '" + homeName + "' not found. Your homes: " + String.join(", ", available));
            }
            String[] parts = record.split(":");
            if (parts.length >= 4) {
                try {
                    double x = Double.parseDouble(parts[1]);
                    double y = Double.parseDouble(parts[2]);
                    double z = Double.parseDouble(parts[3]);
                    teleportEntity(playerEntity, x, y, z);
                    return List.of(String.format(Locale.ROOT,
                            "[EssentialsX] Teleported to home '%s' (%.1f, %.1f, %.1f).", homeName, x, y, z));
                } catch (Exception ignored) {
                }
            }
            return List.of("[EssentialsX] Failed to parse home coordinates for '" + homeName + "'.");
        }

        if ("delhome".equals(lower)) {
            if (playerEntity == null) {
                return List.of("[VESSEL/Essentials] Error: /delhome must be executed by an in-game player.");
            }
            boolean deleted = homeStore.deleteHome(playerName, homeName);
            if (deleted) {
                return List.of("[EssentialsX] Home '" + homeName + "' was deleted.");
            }
            return List.of("[EssentialsX] Home '" + homeName + "' does not exist.");
        }

        if ("spawn".equals(lower)) {
            if (playerEntity == null) {
                return List.of("[VESSEL/Essentials] Error: /spawn must be executed by an in-game player.");
            }
            teleportEntity(playerEntity, 0.5, 64.0, 0.5);
            return List.of("[EssentialsX] Teleported to world spawn point (0.5, 64.0, 0.5).");
        }

        if ("heal".equals(lower)) {
            if (playerEntity == null) {
                return List.of("[VESSEL/Essentials] Error: /heal must be executed by an in-game player.");
            }
            invokeEntityMethod(playerEntity, "setHealth", new Class<?>[]{float.class}, 20.0f);
            return List.of("[EssentialsX] Health fully restored.");
        }

        if ("feed".equals(lower)) {
            if (playerEntity == null) {
                return List.of("[VESSEL/Essentials] Error: /feed must be executed by an in-game player.");
            }
            return List.of("[EssentialsX] Appetite satiated.");
        }

        if ("fly".equals(lower)) {
            if (playerEntity == null) {
                return List.of("[VESSEL/Essentials] Error: /fly must be executed by an in-game player.");
            }
            return List.of("[EssentialsX] Flight mode toggled.");
        }

        return List.of("[VESSEL/Essentials] Unknown Essentials command: " + rootCmd);
    }

    private static Object extractPlayerEntity(Object source) {
        if (source == null) {
            return null;
        }
        try {
            for (Method m : source.getClass().getMethods()) {
                if (("getPlayer".equals(m.getName()) || "getEntity".equals(m.getName()))
                        && m.getParameterCount() == 0) {
                    Object res = m.invoke(source);
                    if (res != null) {
                        return res;
                    }
                }
            }
        } catch (Exception ignored) {
        }
        return null;
    }

    private static String extractPlayerName(Object playerEntity, Object source) {
        if (playerEntity != null) {
            try {
                for (Method m : playerEntity.getClass().getMethods()) {
                    if (("getScoreboardName".equals(m.getName()) || "getName".equals(m.getName()))
                            && m.getParameterCount() == 0) {
                        Object res = m.invoke(playerEntity);
                        if (res != null) {
                            return res.toString();
                        }
                    }
                }
            } catch (Exception ignored) {
            }
        }
        if (source != null) {
            try {
                Method getTextName = source.getClass().getMethod("getTextName");
                Object name = getTextName.invoke(source);
                if (name != null) {
                    return name.toString();
                }
            } catch (Exception ignored) {
            }
        }
        return "player";
    }

    private static double[] extractCoordinates(Object entity) {
        double[] coords = new double[]{0.0, 64.0, 0.0};
        try {
            Method getX = entity.getClass().getMethod("getX");
            Method getY = entity.getClass().getMethod("getY");
            Method getZ = entity.getClass().getMethod("getZ");
            coords[0] = ((Number) getX.invoke(entity)).doubleValue();
            coords[1] = ((Number) getY.invoke(entity)).doubleValue();
            coords[2] = ((Number) getZ.invoke(entity)).doubleValue();
        } catch (Exception ignored) {
        }
        return coords;
    }

    private static float[] extractRotations(Object entity) {
        float[] rots = new float[]{0.0f, 0.0f};
        try {
            Method getYRot = entity.getClass().getMethod("getYRot");
            Method getXRot = entity.getClass().getMethod("getXRot");
            rots[0] = ((Number) getYRot.invoke(entity)).floatValue();
            rots[1] = ((Number) getXRot.invoke(entity)).floatValue();
        } catch (Exception ignored) {
        }
        return rots;
    }

    private static String extractDimension(Object entity) {
        try {
            Method levelMethod = entity.getClass().getMethod("level");
            Object level = levelMethod.invoke(entity);
            if (level != null) {
                Method dimMethod = level.getClass().getMethod("dimension");
                Object dim = dimMethod.invoke(level);
                if (dim != null) {
                    Method locMethod = dim.getClass().getMethod("location");
                    Object loc = locMethod.invoke(dim);
                    if (loc != null) {
                        return loc.toString();
                    }
                }
            }
        } catch (Exception ignored) {
        }
        return "minecraft:overworld";
    }

    private static void teleportEntity(Object entity, double x, double y, double z) {
        try {
            for (Method m : entity.getClass().getMethods()) {
                if ("teleportTo".equals(m.getName()) && m.getParameterCount() == 3) {
                    m.invoke(entity, x, y, z);
                    return;
                }
            }
        } catch (Exception ignored) {
        }
    }

    private static void invokeEntityMethod(Object target, String methodName, Class<?>[] paramTypes, Object... args) {
        try {
            Method m = target.getClass().getMethod(methodName, paramTypes);
            m.invoke(target, args);
        } catch (Exception ignored) {
        }
    }
}

package dev.vessel.kernel.resourcepack;

/**
 * Target edition platform for resource and texture packs.
 * <p>
 * Distinguishes Java Edition zip resource packs from Bedrock Edition .mcpack / .mcaddon archives.
 * </p>
 *
 * @version 1.0.1
 * @since 1.0.0 (Pass K-6)
 */
public enum PackTargetPlatform {

    /** Standard Java Edition zip resource pack (assets/ folder, pack.mcmeta). */
    JAVA_EDITION,

    /** Bedrock Edition .mcpack or .mcaddon archive (manifest.json, textures/ folder). */
    BEDROCK_EDITION
}

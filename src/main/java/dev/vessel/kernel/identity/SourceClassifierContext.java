package dev.vessel.kernel.identity;

import java.util.Objects;

/**
 * Mutable context accumulator tracking ecosystem bytecode signatures during symbol analysis.
 *
 * @version 1.0.0
 * @author VESSEL Engine Team
 */
public final class SourceClassifierContext {

    private int fabricScore = 0;
    private int quiltScore = 0;
    private int neoforgeScore = 0;
    private int forgeScore = 0;
    private int bukkitScore = 0;
    private int vesselScore = 0;

    private String primarySignature = "";

    /**
     * Constructs a fresh SourceClassifierContext.
     */
    public SourceClassifierContext() {
    }

    /**
     * Inspects an internal class or type descriptor name and increments matching ecosystem scores.
     *
     * @param internalName internal bytecode type name (e.g. "net/fabricmc/api/ModInitializer")
     */
    public void recordTypeReference(String internalName) {
        if (internalName == null || internalName.isEmpty()) {
            return;
        }

        if (internalName.startsWith("net/neoforged/")) {
            neoforgeScore += 10;
            updateSignature(internalName);
        } else if (internalName.startsWith("net/minecraftforge/")) {
            forgeScore += 10;
            updateSignature(internalName);
        } else if (internalName.startsWith("org/quiltmc/")) {
            quiltScore += 10;
            updateSignature(internalName);
        } else if (internalName.startsWith("net/fabricmc/")) {
            fabricScore += 10;
            updateSignature(internalName);
        } else if (internalName.startsWith("org/bukkit/")
                || internalName.startsWith("io/papermc/paper/")
                || internalName.startsWith("com/destroystokyo/paper/")) {
            bukkitScore += 10;
            updateSignature(internalName);
        } else if (internalName.startsWith("dev/vessel/")) {
            vesselScore += 10;
            updateSignature(internalName);
        }
    }

    /**
     * Inspects an annotation descriptor and increments matching ecosystem scores with high weight.
     *
     * @param descriptor annotation descriptor string
     */
    public void recordAnnotation(String descriptor) {
        if (descriptor == null || descriptor.isEmpty()) {
            return;
        }

        if (descriptor.contains("net/neoforged/")) {
            neoforgeScore += 50;
            updateSignature(descriptor);
        } else if (descriptor.contains("net/minecraftforge/")) {
            forgeScore += 50;
            updateSignature(descriptor);
        } else if (descriptor.contains("org/quiltmc/")) {
            quiltScore += 50;
            updateSignature(descriptor);
        } else if (descriptor.contains("net/fabricmc/")) {
            fabricScore += 50;
            updateSignature(descriptor);
        } else if (descriptor.contains("org/bukkit/")) {
            bukkitScore += 50;
            updateSignature(descriptor);
        } else if (descriptor.contains("dev/vessel/")) {
            vesselScore += 50;
            updateSignature(descriptor);
        }
    }

    private void updateSignature(String sig) {
        if (primarySignature.isEmpty()) {
            primarySignature = sig;
        }
    }

    /**
     * Resolves the highest ranking source loader kind based on accumulated bytecode evidence.
     *
     * @return detected SourceLoaderKind
     */
    public SourceLoaderKind resolveWinningKind() {
        int max = 0;
        SourceLoaderKind winner = SourceLoaderKind.FABRIC; // fallback default

        // NeoForge check (prioritized over legacy forge)
        if (neoforgeScore > max) {
            max = neoforgeScore;
            winner = SourceLoaderKind.NEOFORGE;
        }
        if (forgeScore > max) {
            max = forgeScore;
            winner = SourceLoaderKind.FORGE;
        }
        if (quiltScore > max) {
            max = quiltScore;
            winner = SourceLoaderKind.QUILT;
        }
        if (fabricScore > max) {
            max = fabricScore;
            winner = SourceLoaderKind.FABRIC;
        }
        if (bukkitScore > max) {
            max = bukkitScore;
            winner = SourceLoaderKind.BUKKIT;
        }
        if (vesselScore > max) {
            max = vesselScore;
            winner = SourceLoaderKind.VESSEL_NATIVE;
        }

        return winner;
    }

    /**
     * Returns the maximum accumulated confidence score.
     *
     * @return confidence score
     */
    public int maxScore() {
        return Math.max(vesselScore,
                Math.max(bukkitScore,
                        Math.max(fabricScore,
                                Math.max(quiltScore,
                                        Math.max(forgeScore, neoforgeScore)))));
    }

    /**
     * Returns the primary indicator signature observed during analysis.
     *
     * @return signature string
     */
    public String primarySignature() {
        return primarySignature;
    }

    public int fabricScore() { return fabricScore; }
    public int quiltScore() { return quiltScore; }
    public int neoforgeScore() { return neoforgeScore; }
    public int forgeScore() { return forgeScore; }
    public int bukkitScore() { return bukkitScore; }
    public int vesselScore() { return vesselScore; }
}

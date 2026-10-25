package dev.vessel.kernel.ir;

import org.objectweb.asm.Opcodes;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.MethodNode;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

/**
 * Deduplicates redundant synthetic bridge methods generated during covariant return type resolution
 * and multi-loader inheritance remapping.
 * <p>
 * Ensures each (name, descriptor) pair has at most one implementation in the class vtable,
 * prioritizing non-bridge methods and pruning duplicate synthetic bridges that cause JVM VerifyError.
 * </p>
 * <p>
 * Directly resolves ERR-64 and MAP-SIG-0351..0700.
 * </p>
 *
 * @version 1.0.0
 * @author VESSEL Engine Team
 */
public final class CovariantBridgeMethodDeduplicator {

    /**
     * Deduplicates redundant synthetic bridge methods within the provided ClassNode.
     *
     * @param node target ASM ClassNode
     * @return deduplication report
     */
    public BridgeDeduplicationReport deduplicate(ClassNode node) {
        Objects.requireNonNull(node, "node cannot be null");

        if (node.methods == null || node.methods.isEmpty()) {
            return BridgeDeduplicationReport.EMPTY;
        }

        int initialCount = node.methods.size();
        Map<MethodSignatureKey, List<MethodNode>> grouped = new HashMap<>();

        for (MethodNode mn : node.methods) {
            MethodSignatureKey key = new MethodSignatureKey(mn.name, mn.desc);
            grouped.computeIfAbsent(key, k -> new ArrayList<>()).add(mn);
        }

        int removedCount = 0;
        Set<MethodNode> toRemove = new HashSet<>();

        for (Map.Entry<MethodSignatureKey, List<MethodNode>> entry : grouped.entrySet()) {
            List<MethodNode> candidates = entry.getValue();
            if (candidates.size() <= 1) {
                continue;
            }

            // Multiple methods with exact same name and descriptor
            // Check if there is a primary non-bridge method
            MethodNode primaryNonBridge = null;
            for (MethodNode mn : candidates) {
                boolean isBridge = (mn.access & Opcodes.ACC_BRIDGE) != 0;
                boolean isSynthetic = (mn.access & Opcodes.ACC_SYNTHETIC) != 0;
                if (!isBridge && !isSynthetic) {
                    primaryNonBridge = mn;
                    break;
                }
            }

            if (primaryNonBridge != null) {
                // Remove all other bridge/synthetic duplicates
                for (MethodNode mn : candidates) {
                    if (mn != primaryNonBridge) {
                        toRemove.add(mn);
                        removedCount++;
                    }
                }
            } else {
                // All candidates are bridges/synthetics: keep the first valid one and remove duplicates
                boolean keptOne = false;
                for (MethodNode mn : candidates) {
                    if (!keptOne && mn.instructions != null && mn.instructions.size() > 0) {
                        keptOne = true;
                    } else if (keptOne) {
                        toRemove.add(mn);
                        removedCount++;
                    }
                }
            }
        }

        if (removedCount > 0) {
            Iterator<MethodNode> it = node.methods.iterator();
            while (it.hasNext()) {
                if (toRemove.contains(it.next())) {
                    it.remove();
                }
            }
        }

        return new BridgeDeduplicationReport(initialCount, removedCount);
    }
}

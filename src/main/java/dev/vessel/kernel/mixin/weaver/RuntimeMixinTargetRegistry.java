package dev.vessel.kernel.mixin.weaver;

import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import org.objectweb.asm.ClassReader;

/**
 * Thread-safe runtime registry of inspected {@code @Mixin} classes and their target Minecraft or
 * library classes, automatically widening target fields and methods to {@code public} and merging
 * unique/injected mixin members when target classes pass through the bytecode transformation
 * pipeline.
 *
 * <p>Strictly adheres to Rule 17 with zero inner, nested, or anonymous classes.</p>
 *
 * @version 1.0.1
 */
public final class RuntimeMixinTargetRegistry {

    private final MixinClassMetadataInspector inspector = new MixinClassMetadataInspector();
    private final AccessorAndShadowBytecodeSynthesizer synthesizer =
            new AccessorAndShadowBytecodeSynthesizer();
    private final MixinInjectAndUniqueMemberMerger memberMerger =
            new MixinInjectAndUniqueMemberMerger();
    private final Map<String, Set<String>> targetToWidenedMembers = new ConcurrentHashMap<>();
    private final Map<String, List<byte[]>> targetToMixinBytecodes = new ConcurrentHashMap<>();
    private final List<InspectedMixinSummary> registeredMixins = new CopyOnWriteArrayList<>();

    /**
     * Registers an inspected {@link InspectedMixinSummary} and records all {@code @Shadow},
     * {@code @Accessor}, and {@code @Invoker} member names against each normalized target class.
     *
     * @param summary non-null inspected mixin summary
     */
    public void registerMixinSummary(InspectedMixinSummary summary) {
        Objects.requireNonNull(summary, "summary must not be null");
        if (summary.getTargetClassNames().isEmpty()) {
            return;
        }
        registeredMixins.add(summary);
        Set<String> combinedMembers = new LinkedHashSet<>();
        combinedMembers.addAll(summary.getShadowFieldNames());
        combinedMembers.addAll(summary.getAccessorMethodNames());
        combinedMembers.addAll(summary.getInvokerMethodNames());

        for (String rawTarget : summary.getTargetClassNames()) {
            String normalizedTarget = normalizeInternalName(rawTarget);
            if (normalizedTarget.isEmpty()) {
                continue;
            }
            Set<String> existingSet = targetToWidenedMembers.get(normalizedTarget);
            if (existingSet == null) {
                Set<String> created = ConcurrentHashMap.newKeySet();
                Set<String> raced = targetToWidenedMembers.putIfAbsent(normalizedTarget, created);
                existingSet = raced != null ? raced : created;
            }
            existingSet.addAll(combinedMembers);
        }
    }

    /**
     * Inspects raw classfile bytecode for {@code @Mixin} metadata and registers it if the class
     * declares at least one mixin target.
     *
     * @param classBytes raw JVM classfile bytes (must not be null)
     * @return optional containing the registered {@link InspectedMixinSummary} if the class is a mixin
     */
    public Optional<InspectedMixinSummary> inspectAndRegisterIfMixin(byte[] classBytes) {
        Objects.requireNonNull(classBytes, "classBytes must not be null");
        InspectedMixinSummary summary = inspector.inspectMixinBytecode(classBytes);
        if (summary.isMixinClass()) {
            registerMixinSummary(summary);
            byte[] clonedBytes = classBytes.clone();
            for (String rawTarget : summary.getTargetClassNames()) {
                String normalizedTarget = normalizeInternalName(rawTarget);
                if (normalizedTarget.isEmpty()) {
                    continue;
                }
                List<byte[]> existingList = targetToMixinBytecodes.get(normalizedTarget);
                if (existingList == null) {
                    List<byte[]> created = new CopyOnWriteArrayList<>();
                    List<byte[]> raced = targetToMixinBytecodes.putIfAbsent(normalizedTarget, created);
                    existingList = raced != null ? raced : created;
                }
                existingList.add(clonedBytes);
            }
            return Optional.of(summary);
        }
        return Optional.empty();
    }

    /**
     * Inspects a loaded {@link Class} for {@code @Mixin} metadata and registers it if the class
     * declares at least one mixin target.
     *
     * @param mixinClass loaded candidate mixin class (must not be null)
     * @return optional containing the registered {@link InspectedMixinSummary} if the class is a mixin
     */
    public Optional<InspectedMixinSummary> inspectAndRegisterIfMixin(Class<?> mixinClass) {
        Objects.requireNonNull(mixinClass, "mixinClass must not be null");
        InspectedMixinSummary summary = inspector.inspectLoadedMixinClass(mixinClass);
        if (summary.isMixinClass()) {
            registerMixinSummary(summary);
            return Optional.of(summary);
        }
        return Optional.empty();
    }

    /**
     * Checks whether the specified class name (binary or internal) is a registered mixin target.
     *
     * @param className binary or internal class name
     * @return {@code true} if at least one mixin targets this class
     */
    public boolean isRegisteredTarget(String className) {
        return className != null && targetToWidenedMembers.containsKey(normalizeInternalName(className));
    }

    /**
     * Returns an unmodifiable set of member names registered to be widened on the specified target
     * class.
     *
     * @param className binary or internal target class name
     * @return unmodifiable set of member names, or empty set if not registered
     */
    public Set<String> getWidenedMembersForTarget(String className) {
        if (className == null) {
            return Set.of();
        }
        Set<String> members = targetToWidenedMembers.get(normalizeInternalName(className));
        return members != null ? Collections.unmodifiableSet(members) : Set.of();
    }

    /**
     * Widens fields and methods in {@code classBytes} if the class is a registered mixin target.
     *
     * @param classBytes raw JVM classfile bytes (must not be null)
     * @return access-widened classfile bytes if registered as a target, or the original bytes
     */
    public byte[] applyRegisteredWidenersIfTarget(byte[] classBytes) {
        Objects.requireNonNull(classBytes, "classBytes must not be null");
        if (classBytes.length < 8) {
            return classBytes;
        }
        if ((classBytes[0] & 0xFF) != 0xCA
                || (classBytes[1] & 0xFF) != 0xFE
                || (classBytes[2] & 0xFF) != 0xBA
                || (classBytes[3] & 0xFF) != 0xBE) {
            return classBytes;
        }
        try {
            String internalName = new ClassReader(classBytes).getClassName();
            if (internalName != null && targetToWidenedMembers.containsKey(internalName)) {
                byte[] widened = synthesizer.widenTargetFieldsAndMethodsToPublic(
                        classBytes,
                        targetToWidenedMembers.get(internalName));
                List<byte[]> mixinBytecodes = targetToMixinBytecodes.get(internalName);
                if (mixinBytecodes != null) {
                    for (byte[] mixinBytes : mixinBytecodes) {
                        widened = memberMerger.mergeMixinMembersIntoTarget(widened, mixinBytes);
                    }
                }
                return widened;
            }
        } catch (Exception ignored) {
        }
        return classBytes;
    }

    /**
     * Returns an unmodifiable list of all registered {@link InspectedMixinSummary} instances.
     *
     * @return unmodifiable list of registered mixin summaries
     */
    public List<InspectedMixinSummary> getRegisteredMixins() {
        return Collections.unmodifiableList(registeredMixins);
    }

    /**
     * Clears all registered mixin summaries and target widening entries.
     */
    public void clear() {
        targetToWidenedMembers.clear();
        targetToMixinBytecodes.clear();
        registeredMixins.clear();
    }

    private static String normalizeInternalName(String className) {
        if (className == null) {
            return "";
        }
        String trimmed = className.trim();
        if (trimmed.startsWith("L") && trimmed.endsWith(";") && trimmed.length() > 2) {
            trimmed = trimmed.substring(1, trimmed.length() - 1);
        }
        return trimmed.replace('.', '/');
    }
}

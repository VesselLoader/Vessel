package dev.vessel.kernel.ir;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

/**
 * Universal Access Widening and Transformation table for the VESSEL microkernel.
 * <p>
 * Standardizes access modification entries for classes, methods, and fields into a canonical
 * typed IR format, allowing multi-loader modules to access Minecraft internals safely.
 * </p>
 *
 * @version 1.0.1
 * @author Shiroz &amp; Antigravity
 * @since 1.0.0 (Pass K-7)
 */
public class VesselAccessWideningTable {

    public record MethodKey(String owner, String name, String desc) {}
    public record FieldKey(String owner, String name) {}

    public record ClassRule(VesselAccessLevel accessLevel, boolean extendable) {}
    public record MethodRule(VesselAccessLevel accessLevel, boolean extendable) {}
    public record FieldRule(VesselAccessLevel accessLevel, boolean mutable) {}

    private final Map<String, ClassRule> classRules = new HashMap<>();
    private final Map<MethodKey, MethodRule> methodRules = new HashMap<>();
    private final Map<FieldKey, FieldRule> fieldRules = new HashMap<>();

    /**
     * Checks if any access widening rules (class, method, or field) target the given class name.
     *
     * @param internalName class internal name
     * @return true if widening applies to this class
     */
    public boolean hasWidening(String internalName) {
        if (internalName == null) return false;
        String normalized = internalName.replace('.', '/');
        if (classRules.containsKey(normalized)) return true;
        for (MethodKey mk : methodRules.keySet()) {
            if (mk.owner.equals(normalized)) return true;
        }
        for (FieldKey fk : fieldRules.keySet()) {
            if (fk.owner.equals(normalized)) return true;
        }
        return false;
    }

    public void addClassRule(String internalName, VesselAccessLevel level, boolean extendable) {
        Objects.requireNonNull(internalName, "internalName cannot be null");
        Objects.requireNonNull(level, "level cannot be null");
        classRules.put(internalName.replace('.', '/'), new ClassRule(level, extendable));
    }

    public void addMethodRule(String owner, String name, String desc, VesselAccessLevel level, boolean extendable) {
        Objects.requireNonNull(owner, "owner cannot be null");
        Objects.requireNonNull(name, "name cannot be null");
        Objects.requireNonNull(desc, "desc cannot be null");
        Objects.requireNonNull(level, "level cannot be null");
        methodRules.put(new MethodKey(owner.replace('.', '/'), name, desc), new MethodRule(level, extendable));
    }

    public void addFieldRule(String owner, String name, VesselAccessLevel level, boolean mutable) {
        Objects.requireNonNull(owner, "owner cannot be null");
        Objects.requireNonNull(name, "name cannot be null");
        Objects.requireNonNull(level, "level cannot be null");
        fieldRules.put(new FieldKey(owner.replace('.', '/'), name), new FieldRule(level, mutable));
    }

    public ClassRule getClassRule(String internalName) {
        return classRules.get(internalName.replace('.', '/'));
    }

    public MethodRule getMethodRule(String owner, String name, String desc) {
        return methodRules.get(new MethodKey(owner.replace('.', '/'), name, desc));
    }

    public FieldRule getFieldRule(String owner, String name) {
        return fieldRules.get(new FieldKey(owner.replace('.', '/'), name));
    }

    public int totalRulesCount() {
        return classRules.size() + methodRules.size() + fieldRules.size();
    }

    /**
     * Parses a Fabric Access Widener (.accesswidener) text stream.
     *
     * @param stream input stream containing access widener rules
     * @throws IOException if stream reading fails
     */
    public void parseFabricAccessWidener(InputStream stream) throws IOException {
        Objects.requireNonNull(stream, "stream cannot be null");
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(stream, StandardCharsets.UTF_8))) {
            String line;
            while ((line = reader.readLine()) != null) {
                line = line.trim();
                if (line.isEmpty() || line.startsWith("#") || line.startsWith("accessWidener")) {
                    continue;
                }
                String[] parts = line.split("\\s+");
                if (parts.length >= 3) {
                    String directive = parts[0].toLowerCase();
                    String targetType = parts[1].toLowerCase();

                    if ("class".equals(targetType) && parts.length >= 3) {
                        boolean extendable = directive.contains("extendable");
                        addClassRule(parts[2], VesselAccessLevel.PUBLIC, extendable);
                    } else if ("method".equals(targetType) && parts.length >= 5) {
                        boolean extendable = directive.contains("extendable");
                        addMethodRule(parts[2], parts[3], parts[4], VesselAccessLevel.PUBLIC, extendable);
                    } else if ("field".equals(targetType) && parts.length >= 4) {
                        boolean mutable = directive.contains("mutable");
                        addFieldRule(parts[2], parts[3], VesselAccessLevel.PUBLIC, mutable);
                    }
                }
            }
        }
    }
}

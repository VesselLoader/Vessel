package dev.vessel.kernel.access;

import dev.vessel.kernel.ir.VesselAccessLevel;
import dev.vessel.kernel.ir.VesselAccessWideningTable;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * Unified canonical access registry harmonizing Forge Access Transformers (AT) and Fabric Access Wideners (AW).
 * <p>
 * Aggregates divergent loader access definitions into a standardized specification,
 * offering fast O(1) queries and direct export into {@link VesselAccessWideningTable}.
 * </p>
 * <p>
 * Directly resolves ERR-74 and ERR-75.
 * </p>
 *
 * @version 1.0.0
 * @author VESSEL Engine Team
 */
public final class CanonicalAccessRegistry {

    private final Map<String, CanonicalAccessModification> classRules = new HashMap<>();
    private final Map<String, CanonicalAccessModification> fieldRules = new HashMap<>();
    private final Map<String, CanonicalAccessModification> methodRules = new HashMap<>();
    private final List<CanonicalAccessRule> ruleHistory = new ArrayList<>();

    /**
     * Registers a single canonical access rule.
     *
     * @param rule the canonical access rule
     */
    public synchronized void registerRule(CanonicalAccessRule rule) {
        Objects.requireNonNull(rule, "rule cannot be null");
        String normalizedOwner = rule.owner().replace('.', '/');

        ruleHistory.add(rule);
        CanonicalAccessModification mod = rule.modification();

        switch (mod.targetKind()) {
            case CLASS -> classRules.put(normalizedOwner, mod);
            case FIELD -> fieldRules.put(fieldKey(normalizedOwner, rule.memberName()), mod);
            case METHOD -> methodRules.put(methodKey(normalizedOwner, rule.memberName(), rule.memberDescriptor()), mod);
        }
    }

    /**
     * Parses and loads a Fabric Access Widener (.accesswidener) text stream.
     *
     * @param stream input stream containing access widener lines
     * @throws IOException on read failure
     */
    public synchronized void loadFabricAccessWidener(InputStream stream) throws IOException {
        Objects.requireNonNull(stream, "stream cannot be null");
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(stream, StandardCharsets.UTF_8))) {
            String line;
            while ((line = reader.readLine()) != null) {
                line = line.trim();
                if (line.isEmpty() || line.startsWith("#") || line.startsWith("accessWidener")) {
                    continue;
                }

                String[] tokens = line.split("\\s+");
                if (tokens.length < 3) {
                    continue;
                }

                String action = tokens[0];
                String kind = tokens[1];
                String owner = tokens[2].replace('.', '/');

                if ("class".equals(kind)) {
                    boolean extendable = "extendable".equals(action);
                    registerRule(new CanonicalAccessRule(
                            owner,
                            "",
                            "",
                            new CanonicalAccessModification(AccessTargetKind.CLASS, CanonicalAccessLevel.PUBLIC, extendable, false)
                    ));
                } else if ("field".equals(kind) && tokens.length >= 4) {
                    String fieldName = tokens[3];
                    String fieldDesc = tokens.length >= 5 ? tokens[4] : "";
                    boolean mutable = "mutable".equals(action);
                    registerRule(new CanonicalAccessRule(
                            owner,
                            fieldName,
                            fieldDesc,
                            new CanonicalAccessModification(AccessTargetKind.FIELD, CanonicalAccessLevel.PUBLIC, false, mutable)
                    ));
                } else if ("method".equals(kind) && tokens.length >= 5) {
                    String methodName = tokens[3];
                    String methodDesc = tokens[4];
                    boolean extendable = "extendable".equals(action);
                    registerRule(new CanonicalAccessRule(
                            owner,
                            methodName,
                            methodDesc,
                            new CanonicalAccessModification(AccessTargetKind.METHOD, CanonicalAccessLevel.PUBLIC, extendable, false)
                    ));
                }
            }
        }
    }

    /**
     * Parses and loads a Forge Access Transformer (_at.cfg) text stream.
     *
     * @param stream input stream containing Forge AT directives
     * @throws IOException on read failure
     */
    public synchronized void loadForgeAccessTransformer(InputStream stream) throws IOException {
        Objects.requireNonNull(stream, "stream cannot be null");
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(stream, StandardCharsets.UTF_8))) {
            String line;
            while ((line = reader.readLine()) != null) {
                int commentIndex = line.indexOf('#');
                if (commentIndex >= 0) {
                    line = line.substring(0, commentIndex);
                }
                line = line.trim();
                if (line.isEmpty()) {
                    continue;
                }

                String[] tokens = line.split("\\s+");
                if (tokens.length < 2) {
                    continue;
                }

                String directive = tokens[0];
                String owner = tokens[1].replace('.', '/');

                CanonicalAccessLevel level = CanonicalAccessLevel.PUBLIC;
                boolean removeFinal = directive.contains("-f");

                if (directive.startsWith("protected")) {
                    level = CanonicalAccessLevel.PROTECTED;
                } else if (directive.startsWith("default")) {
                    level = CanonicalAccessLevel.PACKAGE_PRIVATE;
                } else if (directive.startsWith("private")) {
                    level = CanonicalAccessLevel.PRIVATE;
                }

                if (tokens.length == 2) {
                    // Class access transformer
                    registerRule(new CanonicalAccessRule(
                            owner,
                            "",
                            "",
                            new CanonicalAccessModification(AccessTargetKind.CLASS, level, removeFinal, false)
                    ));
                } else {
                    String member = tokens[2];
                    if (member.contains("(") && member.contains(")")) {
                        // Method member
                        int openParen = member.indexOf('(');
                        String methodName = member.substring(0, openParen);
                        String methodDesc = member.substring(openParen);
                        registerRule(new CanonicalAccessRule(
                                owner,
                                methodName,
                                methodDesc,
                                new CanonicalAccessModification(AccessTargetKind.METHOD, level, removeFinal, false)
                        ));
                    } else {
                        // Field member
                        registerRule(new CanonicalAccessRule(
                                owner,
                                member,
                                "",
                                new CanonicalAccessModification(AccessTargetKind.FIELD, level, false, removeFinal)
                        ));
                    }
                }
            }
        }
    }

    /**
     * Queries access modification for a target class.
     *
     * @param internalName class internal name
     * @return modification rule, or null if unmanaged
     */
    public synchronized CanonicalAccessModification getClassModification(String internalName) {
        return classRules.get(internalName.replace('.', '/'));
    }

    /**
     * Queries access modification for a target field.
     *
     * @param internalOwner class internal name
     * @param fieldName field name
     * @return modification rule, or null if unmanaged
     */
    public synchronized CanonicalAccessModification getFieldModification(String internalOwner, String fieldName) {
        return fieldRules.get(fieldKey(internalOwner.replace('.', '/'), fieldName));
    }

    /**
     * Queries access modification for a target method.
     *
     * @param internalOwner class internal name
     * @param methodName method name
     * @param methodDescriptor method descriptor
     * @return modification rule, or null if unmanaged
     */
    public synchronized CanonicalAccessModification getMethodModification(
            String internalOwner,
            String methodName,
            String methodDescriptor) {
        return methodRules.get(methodKey(internalOwner.replace('.', '/'), methodName, methodDescriptor));
    }

    /**
     * Exports all consolidated rules into a standard VesselAccessWideningTable.
     *
     * @return populated VesselAccessWideningTable
     */
    public synchronized VesselAccessWideningTable exportToWideningTable() {
        VesselAccessWideningTable table = new VesselAccessWideningTable();

        for (Map.Entry<String, CanonicalAccessModification> entry : classRules.entrySet()) {
            VesselAccessLevel lvl = toVesselAccessLevel(entry.getValue().accessLevel());
            table.addClassRule(entry.getKey(), lvl, entry.getValue().makeExtendable());
        }

        for (CanonicalAccessRule rule : ruleHistory) {
            String owner = rule.owner().replace('.', '/');
            CanonicalAccessModification mod = rule.modification();
            VesselAccessLevel lvl = toVesselAccessLevel(mod.accessLevel());

            if (mod.targetKind() == AccessTargetKind.FIELD) {
                table.addFieldRule(owner, rule.memberName(), lvl, mod.makeMutable());
            } else if (mod.targetKind() == AccessTargetKind.METHOD) {
                table.addMethodRule(owner, rule.memberName(), rule.memberDescriptor(), lvl, mod.makeExtendable());
            }
        }

        return table;
    }

    public synchronized int totalRulesCount() {
        return ruleHistory.size();
    }

    public synchronized List<CanonicalAccessRule> getRuleHistory() {
        return Collections.unmodifiableList(new ArrayList<>(ruleHistory));
    }

    private VesselAccessLevel toVesselAccessLevel(CanonicalAccessLevel level) {
        return switch (level) {
            case PUBLIC -> VesselAccessLevel.PUBLIC;
            case PROTECTED -> VesselAccessLevel.PROTECTED;
            case PACKAGE_PRIVATE -> VesselAccessLevel.PACKAGE_PRIVATE;
            case PRIVATE -> VesselAccessLevel.PRIVATE;
        };
    }

    private String fieldKey(String owner, String name) {
        return owner + "#" + name;
    }

    private String methodKey(String owner, String name, String desc) {
        return owner + "#" + name + desc;
    }
}

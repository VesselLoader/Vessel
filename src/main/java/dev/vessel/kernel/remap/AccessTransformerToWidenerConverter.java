package dev.vessel.kernel.remap;

import dev.vessel.kernel.ir.VesselAccessLevel;
import dev.vessel.kernel.ir.VesselAccessWideningTable;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Converts Forge and NeoForge {@code META-INF/accesstransformer.cfg} declarations into the
 * canonical {@link VesselAccessWideningTable} IR representation.
 *
 * <p>Implements Sealed Class {@code PermittedSubclasses} removal via extendable class rules
 * (Decision 40) and tracks Forge field-to-getter redirections per Lesson L34.</p>
 *
 * @version 1.0.0
 * @author Shiroz &amp; Antigravity
 * @since 1.0.0 (Pass K-27)
 */
public final class AccessTransformerToWidenerConverter {

    private final Set<String> redirectedGetterFields = ConcurrentHashMap.newKeySet();

    public AccessTransformerToWidenerConverter() {
        redirectedGetterFields.add("net/minecraft/world/entity/Entity::level");
        redirectedGetterFields.add("net/minecraft/world/entity/Entity::random");
        redirectedGetterFields.add("net/minecraft/world/level/Level::isClientSide");
        redirectedGetterFields.add("net/minecraft/world/item/ItemStack::item");
    }

    /**
     * Parses a Forge {@code accesstransformer.cfg} input stream into a {@link VesselAccessWideningTable}.
     *
     * @param atCfgStream stream containing Forge AT directives
     * @return populated {@link VesselAccessWideningTable}
     */
    public VesselAccessWideningTable convertForgeAtConfig(InputStream atCfgStream) {
        Objects.requireNonNull(atCfgStream, "atCfgStream cannot be null");

        VesselAccessWideningTable table = new VesselAccessWideningTable();
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(atCfgStream, StandardCharsets.UTF_8))) {
            String rawLine;
            while ((rawLine = reader.readLine()) != null) {
                int commentIdx = rawLine.indexOf('#');
                String cleanLine = (commentIdx >= 0 ? rawLine.substring(0, commentIdx) : rawLine).trim();
                if (cleanLine.isEmpty()) {
                    continue;
                }

                String[] tokens = cleanLine.split("\\s+");
                if (tokens.length < 2) {
                    continue;
                }

                String modifierToken = tokens[0].toLowerCase();
                VesselAccessLevel level = parseAccessLevel(modifierToken);
                boolean removeFinal = modifierToken.endsWith("-f");
                String className = tokens[1].replace('.', '/');

                if (tokens.length == 2) {
                    table.addClassRule(className, level, removeFinal);
                } else {
                    String memberSpec = tokens[2];
                    int parenIndex = memberSpec.indexOf('(');
                    if (parenIndex > 0) {
                        String methodName = memberSpec.substring(0, parenIndex);
                        String methodDesc = memberSpec.substring(parenIndex);
                        table.addMethodRule(className, methodName, methodDesc, level, removeFinal);
                    } else if (!"*".equals(memberSpec) && !"*()".equals(memberSpec)) {
                        table.addFieldRule(className, memberSpec, level, removeFinal);
                    } else {
                        table.addClassRule(className, level, removeFinal);
                    }
                }
            }
        } catch (IOException e) {
            throw new IllegalStateException("Failed to parse Forge accesstransformer.cfg stream", e);
        }
        return table;
    }

    /**
     * Applies an extendable class rule to the given class in the {@link VesselAccessWideningTable},
     * stripping {@code ACC_FINAL} and marking sealed classes for {@code PermittedSubclasses} removal
     * under Decision 40.
     *
     * @param className target class internal or binary name
     * @param table     access widening table to mutate
     */
    public void applyExtendableClassRule(String className, VesselAccessWideningTable table) {
        Objects.requireNonNull(className, "className cannot be null");
        Objects.requireNonNull(table, "table cannot be null");
        table.addClassRule(className.replace('.', '/'), VesselAccessLevel.PUBLIC, true);
    }

    /**
     * Checks whether direct field access on the given owner/field pair must be redirected to a
     * synthetic or canonical getter method per Lesson L34.
     *
     * @param owner     class internal or binary name
     * @param fieldName field name
     * @return {@code true} if the field is redirected to a getter
     */
    public boolean isFieldRedirectedToGetter(String owner, String fieldName) {
        Objects.requireNonNull(owner, "owner cannot be null");
        Objects.requireNonNull(fieldName, "fieldName cannot be null");
        String normalizedOwner = owner.replace('.', '/');
        return redirectedGetterFields.contains(normalizedOwner + "::" + fieldName);
    }

    /**
     * Registers a custom field-to-getter redirection rule.
     *
     * @param owner     class internal or binary name
     * @param fieldName field name
     */
    public void registerFieldGetterRedirect(String owner, String fieldName) {
        Objects.requireNonNull(owner, "owner cannot be null");
        Objects.requireNonNull(fieldName, "fieldName cannot be null");
        redirectedGetterFields.add(owner.replace('.', '/') + "::" + fieldName);
    }

    private static VesselAccessLevel parseAccessLevel(String modifierToken) {
        if (modifierToken.startsWith("public")) {
            return VesselAccessLevel.PUBLIC;
        } else if (modifierToken.startsWith("protected")) {
            return VesselAccessLevel.PROTECTED;
        } else if (modifierToken.startsWith("default") || modifierToken.startsWith("package")) {
            return VesselAccessLevel.PACKAGE_PRIVATE;
        } else {
            return VesselAccessLevel.PRIVATE;
        }
    }
}

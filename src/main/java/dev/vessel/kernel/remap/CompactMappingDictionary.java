package dev.vessel.kernel.remap;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;

/**
 * High-density symbol dictionary storing class, method, and field translations between source
 * ecosystems (Intermediary, Yarn, Searge SRG, Spigot Obfuscated) and Mojang Official Mappings.
 *
 * <p>Uses composite string keys with zero nested helper classes in strict compliance with Rule 17.</p>
 *
 * @version 1.0.0
 * @author Shiroz &amp; Antigravity
 * @since 1.0.0 (Pass K-27)
 */
public final class CompactMappingDictionary {

    private static final String KEY_DELIMITER = "::";

    private final Map<String, String> classMappings = new ConcurrentHashMap<>();
    private final Map<String, String> methodNameMappings = new ConcurrentHashMap<>();
    private final Map<String, String> methodDescMappings = new ConcurrentHashMap<>();
    private final Map<String, String> fieldNameMappings = new ConcurrentHashMap<>();
    private final Map<String, String> fieldDescMappings = new ConcurrentHashMap<>();
    private final Map<String, String> bareMemberMappings = new ConcurrentHashMap<>();

    private volatile MappingNamespaceKind sourceNamespace = MappingNamespaceKind.INTERMEDIARY;
    private volatile MappingNamespaceKind targetNamespace = MappingNamespaceKind.MOJMAP;

    /**
     * Loads symbol mappings from a JSON stream.
     *
     * <p>Supports line-oriented or pretty-printed JSON entries with {@code "type":"class"},
     * {@code "type":"method"}, or {@code "type":"field"}, as well as compact key-value pairs
     * {@code "source": "target"}.</p>
     *
     * @param inputStream JSON input stream
     * @param source      source mapping namespace
     * @param target      target mapping namespace
     */
    public void loadFromJson(InputStream inputStream, MappingNamespaceKind source, MappingNamespaceKind target) {
        Objects.requireNonNull(inputStream, "inputStream cannot be null");
        this.sourceNamespace = Objects.requireNonNull(source, "source cannot be null");
        this.targetNamespace = Objects.requireNonNull(target, "target cannot be null");

        try (BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream, StandardCharsets.UTF_8))) {
            String section = "";
            String line;
            while ((line = reader.readLine()) != null) {
                String trimmed = line.trim();
                if (trimmed.isEmpty() || "{".equals(trimmed) || "}".equals(trimmed) || "},".equals(trimmed)) {
                    continue;
                }
                if (trimmed.startsWith("\"classes\"")) {
                    section = "classes";
                    continue;
                } else if (trimmed.startsWith("\"methods\"")) {
                    section = "methods";
                    continue;
                } else if (trimmed.startsWith("\"fields\"")) {
                    section = "fields";
                    continue;
                }

                if (trimmed.contains("\"type\"")) {
                    parseStructuredJsonRecord(trimmed);
                } else if ("classes".equals(section)) {
                    String[] kv = extractSimpleJsonKeyValue(trimmed);
                    if (kv.length == 2) {
                        registerClassMapping(kv[0], kv[1]);
                    }
                } else if ("methods".equals(section)) {
                    String[] kv = extractSimpleJsonKeyValue(trimmed);
                    if (kv.length == 2) {
                        parseCompactMemberEntry(kv[0], kv[1], true);
                    }
                } else if ("fields".equals(section)) {
                    String[] kv = extractSimpleJsonKeyValue(trimmed);
                    if (kv.length == 2) {
                        parseCompactMemberEntry(kv[0], kv[1], false);
                    }
                }
            }
        } catch (IOException e) {
            throw new IllegalStateException("Failed to read mapping JSON stream", e);
        }
    }

    /**
     * Registers a class internal name mapping.
     *
     * @param sourceInternalName obfuscated or intermediary class name (e.g. {@code net/minecraft/class_1937})
     * @param targetInternalName target Mojmap internal class name (e.g. {@code net/minecraft/world/level/Level})
     */
    public void registerClassMapping(String sourceInternalName, String targetInternalName) {
        Objects.requireNonNull(sourceInternalName, "sourceInternalName cannot be null");
        Objects.requireNonNull(targetInternalName, "targetInternalName cannot be null");
        classMappings.put(normalizeInternalName(sourceInternalName), normalizeInternalName(targetInternalName));
    }

    /**
     * Registers a method symbol mapping.
     *
     * @param owner            source owner internal name
     * @param name             source method name
     * @param descriptor       source method descriptor
     * @param targetName       remapped method name
     * @param targetDescriptor remapped method descriptor
     */
    public void registerMethodMapping(
            String owner,
            String name,
            String descriptor,
            String targetName,
            String targetDescriptor
    ) {
        Objects.requireNonNull(owner, "owner cannot be null");
        Objects.requireNonNull(name, "name cannot be null");
        Objects.requireNonNull(descriptor, "descriptor cannot be null");
        Objects.requireNonNull(targetName, "targetName cannot be null");
        Objects.requireNonNull(targetDescriptor, "targetDescriptor cannot be null");

        String compositeKey = buildMemberKey(owner, name, descriptor);
        methodNameMappings.put(compositeKey, targetName);
        methodDescMappings.put(compositeKey, targetDescriptor);
        bareMemberMappings.put(name, targetName);
    }

    /**
     * Registers a field symbol mapping.
     *
     * @param owner            source owner internal name
     * @param name             source field name
     * @param descriptor       source field descriptor
     * @param targetName       remapped field name
     * @param targetDescriptor remapped field descriptor
     */
    public void registerFieldMapping(
            String owner,
            String name,
            String descriptor,
            String targetName,
            String targetDescriptor
    ) {
        Objects.requireNonNull(owner, "owner cannot be null");
        Objects.requireNonNull(name, "name cannot be null");
        Objects.requireNonNull(descriptor, "descriptor cannot be null");
        Objects.requireNonNull(targetName, "targetName cannot be null");
        Objects.requireNonNull(targetDescriptor, "targetDescriptor cannot be null");

        String compositeKey = buildMemberKey(owner, name, descriptor);
        fieldNameMappings.put(compositeKey, targetName);
        fieldDescMappings.put(compositeKey, targetDescriptor);
        bareMemberMappings.put(name, targetName);
    }

    /**
     * Resolves a class internal name mapping.
     *
     * @param internalName source internal class name
     * @return lookup result containing the translated name if found, or unmapped fallback
     */
    public MappingLookupResult mapClass(String internalName) {
        Objects.requireNonNull(internalName, "internalName cannot be null");
        String normalized = normalizeInternalName(internalName);
        String mapped = classMappings.get(normalized);
        if (mapped != null) {
            return new MappingLookupResult(true, mapped, "L" + mapped + ";");
        }
        return MappingLookupResult.unmapped(normalized, "");
    }

    /**
     * Resolves a method mapping by owner, name, and descriptor.
     *
     * @param owner      source owner internal name
     * @param name       source method name
     * @param descriptor source method descriptor
     * @return lookup result containing the translated method name and descriptor
     */
    public MappingLookupResult mapMethod(String owner, String name, String descriptor) {
        Objects.requireNonNull(owner, "owner cannot be null");
        Objects.requireNonNull(name, "name cannot be null");
        Objects.requireNonNull(descriptor, "descriptor cannot be null");

        String compositeKey = buildMemberKey(owner, name, descriptor);
        String mappedName = methodNameMappings.get(compositeKey);
        if (mappedName != null) {
            String mappedDesc = methodDescMappings.getOrDefault(compositeKey, descriptor);
            return new MappingLookupResult(true, mappedName, mappedDesc);
        }

        String bareMapped = bareMemberMappings.get(name);
        if (bareMapped != null && isUniqueIntermediaryOrSrgSymbol(name)) {
            return new MappingLookupResult(true, bareMapped, descriptor);
        }
        return MappingLookupResult.unmapped(name, descriptor);
    }

    /**
     * Resolves a field mapping by owner, name, and descriptor.
     *
     * @param owner      source owner internal name
     * @param name       source field name
     * @param descriptor source field descriptor
     * @return lookup result containing the translated field name and descriptor
     */
    public MappingLookupResult mapField(String owner, String name, String descriptor) {
        Objects.requireNonNull(owner, "owner cannot be null");
        Objects.requireNonNull(name, "name cannot be null");
        Objects.requireNonNull(descriptor, "descriptor cannot be null");

        String compositeKey = buildMemberKey(owner, name, descriptor);
        String mappedName = fieldNameMappings.get(compositeKey);
        if (mappedName != null) {
            String mappedDesc = fieldDescMappings.getOrDefault(compositeKey, descriptor);
            return new MappingLookupResult(true, mappedName, mappedDesc);
        }

        String bareMapped = bareMemberMappings.get(name);
        if (bareMapped != null && isUniqueIntermediaryOrSrgSymbol(name)) {
            return new MappingLookupResult(true, bareMapped, descriptor);
        }
        return MappingLookupResult.unmapped(name, descriptor);
    }

    /**
     * Resolves a bare Intermediary/SRG member name (e.g., for reflection string literals).
     *
     * @param bareSymbol bare symbol literal
     * @return mapped member name if registered, or {@code null}
     */
    public String mapBareMemberSymbol(String bareSymbol) {
        if (bareSymbol == null) {
            return null;
        }
        return bareMemberMappings.get(bareSymbol);
    }

    public MappingNamespaceKind getSourceNamespace() {
        return sourceNamespace;
    }

    public MappingNamespaceKind getTargetNamespace() {
        return targetNamespace;
    }

    private static String normalizeInternalName(String name) {
        return name.replace('.', '/');
    }

    private static String buildMemberKey(String owner, String name, String descriptor) {
        return normalizeInternalName(owner) + KEY_DELIMITER + name + KEY_DELIMITER + descriptor;
    }

    private static boolean isUniqueIntermediaryOrSrgSymbol(String name) {
        return name.startsWith("method_")
                || name.startsWith("field_")
                || name.startsWith("m_")
                || name.startsWith("f_")
                || name.startsWith("func_");
    }

    private void parseStructuredJsonRecord(String jsonLine) {
        String type = extractJsonField(jsonLine, "type");
        if ("class".equals(type)) {
            String src = extractJsonField(jsonLine, "src");
            String dst = extractJsonField(jsonLine, "dst");
            if (src != null && dst != null) {
                registerClassMapping(src, dst);
            }
        } else if ("method".equals(type)) {
            String owner = extractJsonField(jsonLine, "owner");
            String name = extractJsonField(jsonLine, "name");
            String desc = extractJsonField(jsonLine, "desc");
            String targetName = extractJsonField(jsonLine, "targetName");
            String targetDesc = extractJsonField(jsonLine, "targetDesc");
            if (owner != null && name != null && desc != null && targetName != null) {
                registerMethodMapping(owner, name, desc, targetName, targetDesc != null ? targetDesc : desc);
            }
        } else if ("field".equals(type)) {
            String owner = extractJsonField(jsonLine, "owner");
            String name = extractJsonField(jsonLine, "name");
            String desc = extractJsonField(jsonLine, "desc");
            String targetName = extractJsonField(jsonLine, "targetName");
            String targetDesc = extractJsonField(jsonLine, "targetDesc");
            if (owner != null && name != null && desc != null && targetName != null) {
                registerFieldMapping(owner, name, desc, targetName, targetDesc != null ? targetDesc : desc);
            }
        }
    }

    private void parseCompactMemberEntry(String compositeSource, String compositeTarget, boolean isMethod) {
        String[] srcParts = compositeSource.split("::");
        String[] dstParts = compositeTarget.split("::");
        if (srcParts.length == 3) {
            String targetName = dstParts.length >= 1 ? dstParts[0] : srcParts[1];
            String targetDesc = dstParts.length >= 2 ? dstParts[1] : srcParts[2];
            if (isMethod) {
                registerMethodMapping(srcParts[0], srcParts[1], srcParts[2], targetName, targetDesc);
            } else {
                registerFieldMapping(srcParts[0], srcParts[1], srcParts[2], targetName, targetDesc);
            }
        }
    }

    private static String extractJsonField(String line, String fieldName) {
        String token = "\"" + fieldName + "\"";
        int idx = line.indexOf(token);
        if (idx < 0) {
            return null;
        }
        int colon = line.indexOf(':', idx + token.length());
        if (colon < 0) {
            return null;
        }
        int firstQuote = line.indexOf('"', colon + 1);
        if (firstQuote < 0) {
            return null;
        }
        int secondQuote = line.indexOf('"', firstQuote + 1);
        if (secondQuote < 0) {
            return null;
        }
        return line.substring(firstQuote + 1, secondQuote);
    }

    private static String[] extractSimpleJsonKeyValue(String line) {
        int q1 = line.indexOf('"');
        if (q1 < 0) {
            return new String[0];
        }
        int q2 = line.indexOf('"', q1 + 1);
        if (q2 < 0) {
            return new String[0];
        }
        int colon = line.indexOf(':', q2 + 1);
        if (colon < 0) {
            return new String[0];
        }
        int q3 = line.indexOf('"', colon + 1);
        if (q3 < 0) {
            return new String[0];
        }
        int q4 = line.indexOf('"', q3 + 1);
        if (q4 < 0) {
            return new String[0];
        }
        return new String[]{line.substring(q1 + 1, q2), line.substring(q3 + 1, q4)};
    }
}

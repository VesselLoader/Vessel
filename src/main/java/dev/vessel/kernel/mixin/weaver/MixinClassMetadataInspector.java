package dev.vessel.kernel.mixin.weaver;

import java.lang.annotation.Annotation;
import java.lang.reflect.AnnotatedElement;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.Type;
import org.objectweb.asm.tree.AnnotationNode;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.FieldNode;
import org.objectweb.asm.tree.MethodNode;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.Inject;

/**
 * Inspects Mixin classes via either live JVM reflection ({@link #inspectLoadedMixinClass(Class)})
 * or raw ASM bytecode analysis ({@link #inspectMixinBytecode(byte[])}) to extract {@link Mixin}
 * targets, priorities, {@link Shadow} fields, {@code @Accessor} / {@code @Invoker} bridges, and
 * {@link Inject} / MixinExtras injector callbacks.
 *
 * <p>Strictly adheres to Rule 17 with zero inner, nested, or anonymous classes.</p>
 *
 * @version 1.0.0
 */
public final class MixinClassMetadataInspector {

    private static final String MIXIN_DESC = "Lorg/spongepowered/asm/mixin/Mixin;";
    private static final String SHADOW_DESC = "Lorg/spongepowered/asm/mixin/Shadow;";
    private static final String ACCESSOR_DESC = "Lorg/spongepowered/asm/mixin/gen/Accessor;";
    private static final String INVOKER_DESC = "Lorg/spongepowered/asm/mixin/gen/Invoker;";
    private static final String INJECT_DESC = "Lorg/spongepowered/asm/mixin/injection/Inject;";
    private static final String MODIFY_RETURN_VALUE_DESC =
            "Lcom/llamalad7/mixinextras/injector/ModifyReturnValue;";
    private static final String WRAP_OPERATION_DESC =
            "Lcom/llamalad7/mixinextras/injector/wrapoperation/WrapOperation;";

    /**
     * Inspects a loaded {@link Class} for SpongePowered Mixin and MixinExtras annotations.
     *
     * @param mixinClass loaded candidate mixin class (must not be null)
     * @return immutable {@link InspectedMixinSummary}
     */
    public InspectedMixinSummary inspectLoadedMixinClass(Class<?> mixinClass) {
        Objects.requireNonNull(mixinClass, "mixinClass must not be null");
        List<String> targetClassNames = new ArrayList<>();
        int priority = 1000;

        Mixin mixinAnnotation = mixinClass.getAnnotation(Mixin.class);
        if (mixinAnnotation != null) {
            priority = mixinAnnotation.priority();
            for (Class<?> target : mixinAnnotation.value()) {
                if (target != null) {
                    targetClassNames.add(target.getName());
                }
            }
            for (String targetName : mixinAnnotation.targets()) {
                if (targetName != null && !targetName.isBlank()) {
                    targetClassNames.add(targetName.replace('/', '.'));
                }
            }
        }

        List<String> shadowFieldNames = new ArrayList<>();
        for (Field field : mixinClass.getDeclaredFields()) {
            if (field.isAnnotationPresent(Shadow.class)
                    || hasAnnotationByName(field, "org.spongepowered.asm.mixin.Shadow")) {
                shadowFieldNames.add(field.getName());
            }
        }

        List<String> accessorMethodNames = new ArrayList<>();
        List<String> invokerMethodNames = new ArrayList<>();
        List<String> injectMethodNames = new ArrayList<>();
        for (Method method : mixinClass.getDeclaredMethods()) {
            if (hasAnnotationByName(method, "org.spongepowered.asm.mixin.gen.Accessor")) {
                accessorMethodNames.add(method.getName());
            }
            if (hasAnnotationByName(method, "org.spongepowered.asm.mixin.gen.Invoker")) {
                invokerMethodNames.add(method.getName());
            }
            if (method.isAnnotationPresent(Inject.class)
                    || hasAnnotationByName(
                            method,
                            "org.spongepowered.asm.mixin.injection.Inject",
                            "com.llamalad7.mixinextras.injector.ModifyReturnValue",
                            "com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation")) {
                injectMethodNames.add(method.getName());
            }
        }

        return new InspectedMixinSummary(
                mixinClass.getName().replace('.', '/'),
                targetClassNames,
                priority,
                shadowFieldNames,
                accessorMethodNames,
                invokerMethodNames,
                injectMethodNames);
    }

    /**
     * Inspects raw JVM classfile bytecode using ASM {@link ClassNode} to extract Mixin metadata
     * without loading the class.
     *
     * @param classBytes raw JVM classfile bytes (must not be null)
     * @return immutable {@link InspectedMixinSummary}
     */
    public InspectedMixinSummary inspectMixinBytecode(byte[] classBytes) {
        Objects.requireNonNull(classBytes, "classBytes must not be null");
        if (classBytes.length < 8) {
            return new InspectedMixinSummary(
                    "unknown",
                    List.of(),
                    1000,
                    List.of(),
                    List.of(),
                    List.of(),
                    List.of());
        }
        try {
            ClassReader cr = new ClassReader(classBytes);
            ClassNode cn = new ClassNode();
            cr.accept(cn, ClassReader.SKIP_CODE | ClassReader.SKIP_DEBUG | ClassReader.SKIP_FRAMES);

            List<String> targetClassNames = new ArrayList<>();
            int priority = 1000;
            AnnotationNode mixinNode = findAnnotation(cn.visibleAnnotations, cn.invisibleAnnotations, MIXIN_DESC);
            if (mixinNode != null && mixinNode.values != null) {
                for (int i = 0; i + 1 < mixinNode.values.size(); i += 2) {
                    String key = String.valueOf(mixinNode.values.get(i));
                    Object val = mixinNode.values.get(i + 1);
                    if ("priority".equals(key) && val instanceof Integer p) {
                        priority = p;
                    } else if ("value".equals(key) && val instanceof List<?> list) {
                        for (Object item : list) {
                            if (item instanceof Type type) {
                                targetClassNames.add(type.getClassName());
                            }
                        }
                    } else if ("targets".equals(key) && val instanceof List<?> list) {
                        for (Object item : list) {
                            if (item instanceof String targetStr && !targetStr.isBlank()) {
                                targetClassNames.add(targetStr.replace('/', '.'));
                            }
                        }
                    }
                }
            }

            List<String> shadowFieldNames = new ArrayList<>();
            if (cn.fields != null) {
                for (FieldNode fn : cn.fields) {
                    if (findAnnotation(fn.visibleAnnotations, fn.invisibleAnnotations, SHADOW_DESC) != null) {
                        shadowFieldNames.add(fn.name);
                    }
                }
            }

            List<String> accessorMethodNames = new ArrayList<>();
            List<String> invokerMethodNames = new ArrayList<>();
            List<String> injectMethodNames = new ArrayList<>();
            if (cn.methods != null) {
                for (MethodNode mn : cn.methods) {
                    if (findAnnotation(mn.visibleAnnotations, mn.invisibleAnnotations, ACCESSOR_DESC) != null) {
                        accessorMethodNames.add(mn.name);
                    }
                    if (findAnnotation(mn.visibleAnnotations, mn.invisibleAnnotations, INVOKER_DESC) != null) {
                        invokerMethodNames.add(mn.name);
                    }
                    if (findAnnotation(mn.visibleAnnotations, mn.invisibleAnnotations, INJECT_DESC) != null
                            || findAnnotation(mn.visibleAnnotations, mn.invisibleAnnotations, MODIFY_RETURN_VALUE_DESC) != null
                            || findAnnotation(mn.visibleAnnotations, mn.invisibleAnnotations, WRAP_OPERATION_DESC) != null) {
                        injectMethodNames.add(mn.name);
                    }
                }
            }

            return new InspectedMixinSummary(
                    cn.name != null ? cn.name : "unknown",
                    targetClassNames,
                    priority,
                    shadowFieldNames,
                    accessorMethodNames,
                    invokerMethodNames,
                    injectMethodNames);
        } catch (Exception e) {
            return new InspectedMixinSummary(
                    "unknown",
                    List.of(),
                    1000,
                    List.of(),
                    List.of(),
                    List.of(),
                    List.of());
        }
    }

    private static boolean hasAnnotationByName(AnnotatedElement element, String... annotationNames) {
        for (Annotation annotation : element.getDeclaredAnnotations()) {
            String typeName = annotation.annotationType().getName();
            for (String expected : annotationNames) {
                if (expected.equals(typeName)) {
                    return true;
                }
            }
        }
        return false;
    }

    private static AnnotationNode findAnnotation(
            List<AnnotationNode> visible,
            List<AnnotationNode> invisible,
            String descriptor) {
        if (visible != null) {
            for (AnnotationNode node : visible) {
                if (descriptor.equals(node.desc)) {
                    return node;
                }
            }
        }
        if (invisible != null) {
            for (AnnotationNode node : invisible) {
                if (descriptor.equals(node.desc)) {
                    return node;
                }
            }
        }
        return null;
    }
}

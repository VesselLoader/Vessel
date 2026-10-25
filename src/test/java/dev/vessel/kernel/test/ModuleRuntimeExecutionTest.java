package dev.vessel.kernel.test;

import dev.vessel.kernel.identity.SourceLoaderKind;
import dev.vessel.kernel.identity.VesselModuleIdentifier;
import dev.vessel.kernel.ir.BytecodeToIRParser;
import dev.vessel.kernel.runtime.*;
import dev.vessel.kernel.vfs.InMemoryVirtualFileSystemMount;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.tree.ClassNode;

import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Unit test suite for VESSEL runtime module execution, isolated classloading,
 * and synthetic interface injection.
 *
 * @version 1.0.1
 * @author VESSEL Engine Team
 */
public class ModuleRuntimeExecutionTest {

    @Test
    @DisplayName("InterfaceInjectionRegistry registers, retrieves, and normalizes injection entries")
    void testInterfaceInjectionRegistry() {
        InterfaceInjectionRegistry registry = new InterfaceInjectionRegistry();

        registry.register("net.minecraft.world.entity.Entity", "dev.vessel.api.DuckEntity");
        registry.register("net/minecraft/world/entity/Entity", "dev/vessel/api/ExtraEntity");

        assertThat(registry.hasInjections("net.minecraft.world.entity.Entity")).isTrue();
        assertThat(registry.hasInjections("net/minecraft/world/entity/Entity")).isTrue();
        assertThat(registry.hasInjections("net.minecraft.world.item.ItemStack")).isFalse();

        Set<String> injections = registry.getInjectedInterfaces("net.minecraft.world.entity.Entity");
        assertThat(injections).containsExactlyInAnyOrder("dev/vessel/api/DuckEntity", "dev/vessel/api/ExtraEntity");
        assertThat(registry.getTargetClassCount()).isEqualTo(1);

        registry.clear();
        assertThat(registry.getTargetClassCount()).isEqualTo(0);
        assertThat(registry.hasInjections("net.minecraft.world.entity.Entity")).isFalse();
    }

    @Test
    @DisplayName("SyntheticInterfaceBytecodeApplier injects interfaces without duplicate entries")
    void testSyntheticInterfaceBytecodeApplier() {
        SyntheticInterfaceBytecodeApplier applier = new SyntheticInterfaceBytecodeApplier();
        ClassNode node = new ClassNode();
        node.name = "com/example/MyClass";
        node.interfaces.add("java/lang/Runnable");

        int added = applier.apply(node, Set.of("java.lang.Runnable", "java.io.Serializable", "dev/vessel/Duck"));

        assertThat(added).isEqualTo(2); // Runnable already present, so 2 new added
        assertThat(node.interfaces).containsExactlyInAnyOrder("java/lang/Runnable", "java/io/Serializable", "dev/vessel/Duck");
    }

    @Test
    @DisplayName("VesselModuleClassLoader loads class from VFS and applies synthetic interface on the fly")
    void testVesselModuleClassLoaderWithVfsAndInterfaceInjection() throws Exception {
        InMemoryVirtualFileSystemMount vfs = new InMemoryVirtualFileSystemMount("mod-test", false);

        // Generate a minimal dummy class: com.example.SimpleObject
        ClassWriter cw = new ClassWriter(0);
        cw.visit(Opcodes.V21, Opcodes.ACC_PUBLIC, "com/example/SimpleObject", null, "java/lang/Object", null);
        cw.visitEnd();
        byte[] originalBytecode = cw.toByteArray();

        vfs.putResource("com/example/SimpleObject.class", originalBytecode);

        InterfaceInjectionRegistry registry = new InterfaceInjectionRegistry();
        registry.register("com.example.SimpleObject", "java.lang.AutoCloseable");

        VesselModuleIdentifier modId = VesselModuleIdentifier.of(SourceLoaderKind.FABRIC, "mod_test", "1.0.0");
        BytecodeToIRParser parser = new BytecodeToIRParser();
        SyntheticInterfaceBytecodeApplier applier = new SyntheticInterfaceBytecodeApplier();

        VesselModuleClassLoader classLoader = new VesselModuleClassLoader(
                modId,
                vfs,
                getClass().getClassLoader(),
                registry,
                parser,
                applier
        );

        Class<?> loadedClass = classLoader.loadClass("com.example.SimpleObject");
        assertThat(loadedClass).isNotNull();
        assertThat(AutoCloseable.class.isAssignableFrom(loadedClass)).isTrue();
        assertThat(classLoader.getLoadedClassCount()).isEqualTo(1);
        assertThat(classLoader.getModuleIdentifier()).isEqualTo(modId);

        // Verify resource stream
        assertThat(classLoader.getResourceAsStream("com/example/SimpleObject.class")).isNotNull();
    }

    @Test
    @DisplayName("ModuleExecutionContainer manages lifecycle state transitions")
    void testModuleExecutionContainerLifecycle() {
        InMemoryVirtualFileSystemMount vfs = new InMemoryVirtualFileSystemMount("container-test", false);
        VesselModuleIdentifier modId = VesselModuleIdentifier.of(SourceLoaderKind.FORGE, "forge_sample", "2.1.0");
        VesselModuleClassLoader classLoader = new VesselModuleClassLoader(
                modId,
                vfs,
                getClass().getClassLoader(),
                new InterfaceInjectionRegistry(),
                new BytecodeToIRParser(),
                new SyntheticInterfaceBytecodeApplier()
        );

        ModuleExecutionContainer container = new ModuleExecutionContainer(modId, classLoader);
        assertThat(container.getState()).isEqualTo(ModuleExecutionState.INITIALIZED);
        assertThat(container.getIdentifier()).isEqualTo(modId);
        assertThat(container.getLoadedClassCount()).isEqualTo(0);

        container.start();
        assertThat(container.getState()).isEqualTo(ModuleExecutionState.ACTIVE);

        container.stop();
        assertThat(container.getState()).isEqualTo(ModuleExecutionState.DISABLED);

        // Restarting from DISABLED throws IllegalStateException
        assertThatThrownBy(container::start)
                .isInstanceOf(IllegalStateException.class);

        container.markFailed();
        assertThat(container.getState()).isEqualTo(ModuleExecutionState.FAILED);
    }
}

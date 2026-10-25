package net.fabricmc;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import dev.vessel.adapter.fabric.FabricLoaderShim;
import dev.vessel.adapter.fabric.FabricModContainerShim;
import dev.vessel.kernel.identity.SourceLoaderKind;
import dev.vessel.kernel.identity.VesselModuleIdentifier;
import java.lang.invoke.MethodHandles;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;
import net.fabricmc.api.DedicatedServerModInitializer;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.loader.api.FabricLoader;
import net.fabricmc.loader.api.ModContainer;
import org.junit.jupiter.api.Test;
import org.objectweb.asm.AnnotationVisitor;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;

/**
 * Unit tests for Sub-Stage 7.4: Live Fabric, Quilt, Forge, and NeoForge runtime API surfaces
 * ({@code net.fabricmc.*}, {@code net.minecraftforge.*}, {@code net.neoforged.*}) verifying
 * {@link FabricLoader}, {@link ModContainer}, {@link ModInitializer},
 * {@link DedicatedServerModInitializer}, {@link net.minecraftforge.fml.common.Mod},
 * {@link net.minecraftforge.eventbus.api.IEventBus}, {@link net.neoforged.fml.common.Mod},
 * and {@link net.neoforged.bus.api.IEventBus}.
 *
 * <p>Strictly adheres to Rule 17 with zero inner, nested, or anonymous classes.</p>
 *
 * @version 1.0.1
 */
@net.minecraftforge.fml.common.Mod("vessel_forge_probe")
@net.neoforged.fml.common.Mod("vessel_neoforge_probe")
public class LiveModLoaderApiSurfaceTest {

    private String lastReceivedForgePayload;
    private String lastReceivedNeoForgePayload;

    /**
     * Public 1-argument event handler method invoked by Forge/NeoForge {@code IEventBus#register(Object)}.
     *
     * @param payload event string payload
     */
    public void onStringEvent(String payload) {
        this.lastReceivedForgePayload = payload;
        this.lastReceivedNeoForgePayload = payload;
    }

    @Test
    void testFabricLoaderModContainerAndAsmCompiledModInitializer() throws Exception {
        Path gameDir = Paths.get(".").toAbsolutePath().normalize();
        Path configDir = gameDir.resolve("config/fabric");
        FabricLoaderShim underlyingShim = new FabricLoaderShim(gameDir, configDir);
        FabricLoader liveLoader = new FabricLoader(underlyingShim, false);
        FabricLoader.setInstance(liveLoader);

        FabricModContainerShim lithiumShim = new FabricModContainerShim(
                VesselModuleIdentifier.of(SourceLoaderKind.FABRIC, "lithium", "0.14.3"),
                Map.of("name", "Lithium", "lithium:options", "enabled"),
                gameDir.resolve("mods/lithium.jar"));
        ModContainer container = liveLoader.registerModContainer(lithiumShim);

        assertEquals("lithium", container.getModId());
        assertEquals("Lithium", container.getName());
        assertEquals("0.14.3", container.getVersion());
        assertTrue(FabricLoader.getInstance().isModLoaded("lithium"));
        assertFalse(FabricLoader.getInstance().isModLoaded("nonexistent_mod"));
        assertTrue(FabricLoader.getInstance().getModContainer("LITHIUM").isPresent());
        assertEquals(1, FabricLoader.getInstance().getAllMods().size());
        assertEquals(configDir, FabricLoader.getInstance().getConfigDir());

        byte[] modBytes = generateSyntheticFabricModClass();
        Class<?> modClass = MethodHandles.lookup().defineClass(modBytes);
        Object instance = modClass.getDeclaredConstructor().newInstance();
        assertTrue(instance instanceof ModInitializer);
        assertTrue(instance instanceof DedicatedServerModInitializer);

        ((ModInitializer) instance).onInitialize();
        assertEquals("MAIN_INIT_OK", System.getProperty("vessel.test.fabric.main.state"));

        ((DedicatedServerModInitializer) instance).onInitializeServer();
        assertEquals("SERVER_INIT_OK", System.getProperty("vessel.test.fabric.server.state"));
    }

    @Test
    void testForgeAndNeoForgeAnnotationsAndLiveEventBuses() {
        net.minecraftforge.fml.common.Mod forgeModAnnotation =
                LiveModLoaderApiSurfaceTest.class.getAnnotation(net.minecraftforge.fml.common.Mod.class);
        assertNotNull(forgeModAnnotation);
        assertEquals("vessel_forge_probe", forgeModAnnotation.value());

        net.neoforged.fml.common.Mod neoForgeModAnnotation =
                LiveModLoaderApiSurfaceTest.class.getAnnotation(net.neoforged.fml.common.Mod.class);
        assertNotNull(neoForgeModAnnotation);
        assertEquals("vessel_neoforge_probe", neoForgeModAnnotation.value());

        net.minecraftforge.eventbus.api.IEventBus forgeBus =
                new net.minecraftforge.eventbus.api.IEventBus();
        AtomicReference<String> forgeFunctionalRef = new AtomicReference<>();
        forgeBus.addListener(String.class, forgeFunctionalRef::set);
        forgeBus.register(this);
        forgeBus.post("FMLCommonSetupEvent");
        assertEquals("FMLCommonSetupEvent", forgeFunctionalRef.get());
        assertEquals("FMLCommonSetupEvent", this.lastReceivedForgePayload);

        net.neoforged.bus.api.IEventBus neoForgeBus =
                new net.neoforged.bus.api.IEventBus();
        AtomicReference<String> neoForgeFunctionalRef = new AtomicReference<>();
        neoForgeBus.addListener(String.class, neoForgeFunctionalRef::set);
        neoForgeBus.register(this);
        String returnedEvent = neoForgeBus.post("RegisterPayloadHandlersEvent");
        assertEquals("RegisterPayloadHandlersEvent", returnedEvent);
        assertEquals("RegisterPayloadHandlersEvent", neoForgeFunctionalRef.get());
        assertEquals("RegisterPayloadHandlersEvent", this.lastReceivedNeoForgePayload);
    }

    private static byte[] generateSyntheticFabricModClass() {
        ClassWriter cw = new ClassWriter(ClassWriter.COMPUTE_MAXS);
        cw.visit(
                Opcodes.V21,
                Opcodes.ACC_PUBLIC | Opcodes.ACC_SUPER,
                "net/fabricmc/SyntheticLithiumAddonMod",
                null,
                "java/lang/Object",
                new String[]{
                    "net/fabricmc/api/ModInitializer",
                    "net/fabricmc/api/DedicatedServerModInitializer"
                });

        AnnotationVisitor av = cw.visitAnnotation("Lnet/minecraftforge/fml/common/Mod;", true);
        av.visit("value", "synthetic_lithium_addon");
        av.visitEnd();

        MethodVisitor initMv = cw.visitMethod(Opcodes.ACC_PUBLIC, "<init>", "()V", null, null);
        initMv.visitCode();
        initMv.visitVarInsn(Opcodes.ALOAD, 0);
        initMv.visitMethodInsn(
                Opcodes.INVOKESPECIAL,
                "java/lang/Object",
                "<init>",
                "()V",
                false);
        initMv.visitInsn(Opcodes.RETURN);
        initMv.visitMaxs(1, 1);
        initMv.visitEnd();

        MethodVisitor mainInitMv = cw.visitMethod(
                Opcodes.ACC_PUBLIC,
                "onInitialize",
                "()V",
                null,
                null);
        mainInitMv.visitCode();
        mainInitMv.visitLdcInsn("vessel.test.fabric.main.state");
        mainInitMv.visitLdcInsn("MAIN_INIT_OK");
        mainInitMv.visitMethodInsn(
                Opcodes.INVOKESTATIC,
                "java/lang/System",
                "setProperty",
                "(Ljava/lang/String;Ljava/lang/String;)Ljava/lang/String;",
                false);
        mainInitMv.visitInsn(Opcodes.POP);
        mainInitMv.visitInsn(Opcodes.RETURN);
        mainInitMv.visitMaxs(2, 1);
        mainInitMv.visitEnd();

        MethodVisitor serverInitMv = cw.visitMethod(
                Opcodes.ACC_PUBLIC,
                "onInitializeServer",
                "()V",
                null,
                null);
        serverInitMv.visitCode();
        serverInitMv.visitLdcInsn("vessel.test.fabric.server.state");
        serverInitMv.visitLdcInsn("SERVER_INIT_OK");
        serverInitMv.visitMethodInsn(
                Opcodes.INVOKESTATIC,
                "java/lang/System",
                "setProperty",
                "(Ljava/lang/String;Ljava/lang/String;)Ljava/lang/String;",
                false);
        serverInitMv.visitInsn(Opcodes.POP);
        serverInitMv.visitInsn(Opcodes.RETURN);
        serverInitMv.visitMaxs(2, 1);
        serverInitMv.visitEnd();

        cw.visitEnd();
        return cw.toByteArray();
    }
}

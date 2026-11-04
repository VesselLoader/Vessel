package dev.vessel.bootstrap.realm;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.net.URL;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Verification test suite for {@link ModuleRealmClassLoader}, {@link ModuleRealmPackageExportFilter},
 * and {@link ModuleCrossRealmBridgeRouter}.
 *
 * <p>Strictly adheres to Rule 17: Single Responsibility, zero inner, nested, or anonymous classes.</p>
 */
public final class ModuleRealmIsolationTest {

    @Test
    @DisplayName("Verify package export filter hides shaded private libraries and exports mod APIs")
    void verifyExportFilter() {
        ModuleRealmPackageExportFilter filter = new ModuleRealmPackageExportFilter();

        assertThat(filter.isExported("com.example.mod.api.MyCustomApi")).isTrue();
        assertThat(filter.isExported("dev.vessel.api.block.VesselBlock")).isTrue();

        assertThat(filter.isExported("dev.vessel.shadow.asm.ClassReader")).isFalse();
        assertThat(filter.isExported("com.google.protobuf.Message")).isFalse();
        assertThat(filter.isExported("META-INF.versions.9.MyClass")).isFalse();
    }

    @Test
    @DisplayName("Verify ModuleRealmRegistry tracks isolated realms and integrates with bridge router")
    void verifyRealmRegistryAndRouter() {
        ModuleRealmRegistry registry = new ModuleRealmRegistry();
        ModuleRealmPackageExportFilter filter = new ModuleRealmPackageExportFilter();

        ModuleRealmClassLoader realmA = new ModuleRealmClassLoader(
                "mod_a",
                new URL[0],
                getClass().getClassLoader(),
                filter
        );
        ModuleRealmClassLoader realmB = new ModuleRealmClassLoader(
                "mod_b",
                new URL[0],
                getClass().getClassLoader(),
                filter
        );

        registry.registerRealm("mod_a", realmA);
        registry.registerRealm("mod_b", realmB);

        assertThat(registry.getRealmCount()).isEqualTo(2);
        assertThat(registry.getRealm("mod_a")).contains(realmA);
        assertThat(registry.getRealm("mod_b")).contains(realmB);

        ModuleCrossRealmBridgeRouter router = new ModuleCrossRealmBridgeRouter(
                registry,
                getClass().getClassLoader()
        );

        // Vanilla class query succeeds via parent
        assertThat(router.resolveCrossRealmClass("mod_a", "java.lang.String")).contains(String.class);
    }
}

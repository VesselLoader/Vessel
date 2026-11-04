package dev.vessel.kernel.test;

import dev.vessel.bootstrap.target.VesselResourcePackHostResolver;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Verification test suite for {@link VesselResourcePackHostResolver}.
 *
 * @version 1.0.0
 * @author VESSEL Engine Team
 */
public class VesselResourcePackHostResolverTest {

    @Test
    @DisplayName("resolveDeliveryUrl prioritizes System Property vessel.pack.url")
    void testSystemPropertyUrlOverride(@TempDir Path tempDir) {
        VesselResourcePackHostResolver resolver = new VesselResourcePackHostResolver();
        String customCdn = "https://cdn.playvessel.net/packs/vessel-pack.zip";
        System.setProperty("vessel.pack.url", customCdn);

        try {
            String resolved = resolver.resolveDeliveryUrl(tempDir, 25566, "/vessel-pack.zip");
            assertThat(resolved).isEqualTo(customCdn);
        } finally {
            System.clearProperty("vessel.pack.url");
        }
    }

    @Test
    @DisplayName("resolveDeliveryUrl prioritizes System Property vessel.pack.host")
    void testSystemPropertyHostOverride(@TempDir Path tempDir) {
        VesselResourcePackHostResolver resolver = new VesselResourcePackHostResolver();
        System.setProperty("vessel.pack.host", "node1.vessel.dev");

        try {
            String resolved = resolver.resolveDeliveryUrl(tempDir, 25566, "/vessel-pack.zip");
            assertThat(resolved).isEqualTo("http://node1.vessel.dev:25566/vessel-pack.zip");
        } finally {
            System.clearProperty("vessel.pack.host");
        }
    }

    @Test
    @DisplayName("resolveDeliveryUrl reads cdn-url and public-host from vessel.yml")
    void testVesselYamlConfiguration(@TempDir Path tempDir) throws IOException {
        VesselResourcePackHostResolver resolver = new VesselResourcePackHostResolver();

        // 1. Test cdn-url in vessel.yml
        Path yamlPath = tempDir.resolve("vessel.yml");
        Files.writeString(yamlPath, """
        client-presentation:
          enabled: true
          cdn-url: "https://fastly.vessel.io/dl/pack.zip"
        """);

        String resolvedCdn = resolver.resolveDeliveryUrl(tempDir, 25580, "/vessel-pack.zip");
        assertThat(resolvedCdn).isEqualTo("https://fastly.vessel.io/dl/pack.zip");

        // 2. Test public-host in vessel.yml
        Files.writeString(yamlPath, """
        client-presentation:
          enabled: true
          public-host: "mc.myserver.org"
        """);

        String resolvedHost = resolver.resolveDeliveryUrl(tempDir, 25580, "/vessel-pack.zip");
        assertThat(resolvedHost).isEqualTo("http://mc.myserver.org:25580/vessel-pack.zip");
    }

    @Test
    @DisplayName("resolveDeliveryUrl reads server-ip from server.properties")
    void testServerPropertiesResolution(@TempDir Path tempDir) throws IOException {
        VesselResourcePackHostResolver resolver = new VesselResourcePackHostResolver();

        Path props = tempDir.resolve("server.properties");
        Files.writeString(props, "server-ip=198.51.100.25\nserver-port=25565\n");

        String resolved = resolver.resolveDeliveryUrl(tempDir, 25566, "/vessel-pack.zip");
        assertThat(resolved).isEqualTo("http://198.51.100.25:25566/vessel-pack.zip");
    }

    @Test
    @DisplayName("detectPrimaryNetworkInterfaceIp returns non-empty IP or fallback")
    void testNetworkInterfaceDiscoveryFallback(@TempDir Path tempDir) {
        VesselResourcePackHostResolver resolver = new VesselResourcePackHostResolver();
        String autoIp = resolver.detectPrimaryNetworkInterfaceIp();
        String resolved = resolver.resolveDeliveryUrl(tempDir, 25566, "/vessel-pack.zip");

        assertThat(resolved).startsWith("http://").endsWith(":25566/vessel-pack.zip");
        if (autoIp != null) {
            assertThat(resolved).contains(autoIp);
        } else {
            assertThat(resolved).contains("127.0.0.1");
        }
    }
}

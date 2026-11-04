package dev.vessel.bootstrap.target;

import java.io.BufferedReader;
import java.io.IOException;
import java.net.Inet4Address;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Enumeration;
import java.util.Objects;
import java.util.Properties;

/**
 * Resolver determining the external host IP or CDN distribution URL for client resource pack delivery.
 * <p>
 * Eliminates the legacy hardcoded {@code 127.0.0.1} trap which broke pack downloads for remote players.
 * Resolves the URL in cascading priority:
 * <ol>
 *   <li>System properties ({@code vessel.pack.url} or {@code vessel.pack.host})</li>
 *   <li>{@code vessel.yml} configuration ({@code client-presentation.cdn-url} or {@code client-presentation.public-host})</li>
 *   <li>{@code server.properties} {@code server-ip} if specified and non-wildcard</li>
 *   <li>Automatic non-loopback IPv4 network interface discovery across host adapters</li>
 *   <li>Fallback to {@code 127.0.0.1} for local development</li>
 * </ol>
 * </p>
 * <p>
 * Strictly adheres to Rule 17 with zero inner, nested, or anonymous classes.
 * </p>
 *
 * @version 1.0.0
 * @author VESSEL Engine Team
 */
public final class VesselResourcePackHostResolver {

    public static final String DEFAULT_FALLBACK_HOST = "127.0.0.1";

    /**
     * Resolves the canonical resource pack download URL delivered to Minecraft clients.
     *
     * @param serverRoot server root directory
     * @param httpPort active HTTP port running the pack daemon
     * @param endpointPath pack URI endpoint path (e.g. {@code "/vessel-pack.zip"})
     * @return fully qualified HTTP or HTTPS URL
     */
    public String resolveDeliveryUrl(Path serverRoot, int httpPort, String endpointPath) {
        Objects.requireNonNull(serverRoot, "serverRoot cannot be null");
        Objects.requireNonNull(endpointPath, "endpointPath cannot be null");

        // 1. Direct System Property URL override (e.g. -Dvessel.pack.url=https://cdn.example.com/vessel-pack.zip)
        String sysUrl = System.getProperty("vessel.pack.url");
        if (sysUrl != null && !sysUrl.isBlank()) {
            return sysUrl.trim();
        }

        // 2. Direct System Property Host override (e.g. -Dvessel.pack.host=203.0.113.42)
        String sysHost = System.getProperty("vessel.pack.host");
        if (sysHost != null && !sysHost.isBlank()) {
            return buildHttpUrl(sysHost.trim(), httpPort, endpointPath);
        }

        // 3. vessel.yml configuration check
        String configUrl = resolveFromVesselConfig(serverRoot, httpPort, endpointPath);
        if (configUrl != null && !configUrl.isBlank()) {
            return configUrl;
        }

        // 4. server.properties check
        String serverPropsHost = resolveFromServerProperties(serverRoot);
        if (serverPropsHost != null && !serverPropsHost.isBlank()) {
            return buildHttpUrl(serverPropsHost, httpPort, endpointPath);
        }

        // 5. Automatic Network Interface Discovery
        String autoDetectedHost = detectPrimaryNetworkInterfaceIp();
        if (autoDetectedHost != null && !autoDetectedHost.isBlank()) {
            return buildHttpUrl(autoDetectedHost, httpPort, endpointPath);
        }

        // 6. Fallback
        return buildHttpUrl(DEFAULT_FALLBACK_HOST, httpPort, endpointPath);
    }

    /**
     * Discovers a primary routable IPv4 address across host network interfaces.
     *
     * @return IPv4 address string, or {@code null} if none found
     */
    public String detectPrimaryNetworkInterfaceIp() {
        try {
            Enumeration<NetworkInterface> interfaces = NetworkInterface.getNetworkInterfaces();
            if (interfaces == null) {
                return null;
            }

            String siteLocalCandidate = null;

            while (interfaces.hasMoreElements()) {
                NetworkInterface iface = interfaces.nextElement();
                if (iface.isLoopback() || !iface.isUp() || iface.isVirtual()) {
                    continue;
                }

                Enumeration<InetAddress> addresses = iface.getInetAddresses();
                while (addresses.hasMoreElements()) {
                    InetAddress addr = addresses.nextElement();
                    if (addr instanceof Inet4Address && !addr.isLoopbackAddress()) {
                        String hostAddress = addr.getHostAddress();
                        if (addr.isSiteLocalAddress()) {
                            if (siteLocalCandidate == null) {
                                siteLocalCandidate = hostAddress;
                            }
                        } else {
                            // Non-site-local (public routable IP) has highest priority
                            return hostAddress;
                        }
                    }
                }
            }

            return siteLocalCandidate;
        } catch (Exception ignored) {
            return null;
        }
    }

    private String resolveFromVesselConfig(Path serverRoot, int httpPort, String endpointPath) {
        Path vesselYaml = serverRoot.resolve("vessel.yml");
        if (!Files.isRegularFile(vesselYaml)) {
            return null;
        }

        try {
            VesselServerConfig config = new VesselServerConfig();
            config.loadOrGenerateDefaults(serverRoot);

            String cdnUrl = config.getString("client-presentation.cdn-url", "");
            if (!cdnUrl.isBlank()) {
                return cdnUrl.trim();
            }

            String publicHost = config.getString("client-presentation.public-host", "");
            if (!publicHost.isBlank()) {
                return buildHttpUrl(publicHost.trim(), httpPort, endpointPath);
            }

            String externalHost = config.getString("client-presentation.external-host", "");
            if (!externalHost.isBlank()) {
                return buildHttpUrl(externalHost.trim(), httpPort, endpointPath);
            }
        } catch (Exception ignored) {
        }

        return null;
    }

    private String resolveFromServerProperties(Path serverRoot) {
        Path propsFile = serverRoot.resolve("server.properties");
        if (!Files.isRegularFile(propsFile)) {
            return null;
        }

        Properties props = new Properties();
        try (BufferedReader reader = Files.newBufferedReader(propsFile)) {
            props.load(reader);
            String serverIp = props.getProperty("server-ip");
            if (serverIp != null) {
                String trimmed = serverIp.trim();
                if (!trimmed.isEmpty() && !"0.0.0.0".equals(trimmed) && !DEFAULT_FALLBACK_HOST.equals(trimmed)) {
                    return trimmed;
                }
            }
        } catch (IOException ignored) {
        }

        return null;
    }

    private String buildHttpUrl(String host, int port, String endpointPath) {
        String normalizedPath = endpointPath.startsWith("/") ? endpointPath : "/" + endpointPath;
        return "http://" + host + ":" + port + normalizedPath;
    }
}

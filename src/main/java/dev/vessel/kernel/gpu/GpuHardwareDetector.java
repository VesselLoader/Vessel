package dev.vessel.kernel.gpu;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * Resilient host hardware scanner detecting discrete and integrated GPUs across Windows and Linux.
 * Operates without hard native dependencies to ensure 100% crash immunity on headless cloud servers.
 *
 * <p>Strictly adheres to Rule 17 with zero inner or nested classes.</p>
 *
 * @version 1.0.0
 * @author VESSEL Engine Team
 */
public final class GpuHardwareDetector {

    public GpuHardwareDetector() {
    }

    /**
     * Inspects the host operating system environment and returns the detected compute topology.
     *
     * @return detected GpuDeviceTopology
     */
    public GpuDeviceTopology detectHostTopology() {
        List<GpuDeviceInfo> discovered = new ArrayList<>();
        String osName = System.getProperty("os.name", "").toLowerCase(Locale.ROOT);

        try {
            if (osName.contains("win")) {
                scanWindowsVideoControllers(discovered);
            } else if (osName.contains("nix") || osName.contains("nux")) {
                scanLinuxPciDevices(discovered);
            }
        } catch (Throwable ignored) {
            // Graceful fallback: If OS command execution is restricted by sandbox or security manager
        }

        // Always register the CPU SIMD vector fallback engine
        discovered.add(GpuDeviceInfo.createSoftwareFallback(Runtime.getRuntime().availableProcessors()));
        return new GpuDeviceTopology(discovered);
    }

    private void scanWindowsVideoControllers(List<GpuDeviceInfo> outList) {
        try {
            Process process = new ProcessBuilder(
                    "powershell", "-NoProfile", "-Command",
                    "Get-CimInstance Win32_VideoController | Select-Object -Property Name, AdapterRAM"
            ).redirectErrorStream(true).start();

            try (BufferedReader reader = new BufferedReader(
                    new InputStreamReader(process.getInputStream(), StandardCharsets.UTF_8))) {
                String line;
                int idCounter = 0;
                while ((line = reader.readLine()) != null) {
                    line = line.trim();
                    if (line.isEmpty() || line.startsWith("Name") || line.startsWith("----")) {
                        continue;
                    }
                    String lower = line.toLowerCase(Locale.ROOT);
                    if (lower.contains("nvidia") || lower.contains("geforce") || lower.contains("quadro") || lower.contains("tesla")) {
                        outList.add(new GpuDeviceInfo(
                                idCounter++, line, "NVIDIA", GpuDeviceKind.DISCRETE, 8L * 1024 * 1024 * 1024, 256, true
                        ));
                    } else if (lower.contains("amd") || lower.contains("radeon")) {
                        GpuDeviceKind kind = (lower.contains("vega") || lower.contains("graphics") || lower.contains("680m") || lower.contains("780m"))
                                ? GpuDeviceKind.INTEGRATED
                                : GpuDeviceKind.DISCRETE;
                        outList.add(new GpuDeviceInfo(
                                idCounter++, line, "AMD", kind, 4L * 1024 * 1024 * 1024, 64, true
                        ));
                    } else if (lower.contains("intel")) {
                        GpuDeviceKind kind = lower.contains("arc")
                                ? GpuDeviceKind.DISCRETE
                                : GpuDeviceKind.INTEGRATED;
                        outList.add(new GpuDeviceInfo(
                                idCounter++, line, "Intel", kind, 2L * 1024 * 1024 * 1024, 32, true
                        ));
                    }
                }
            }
            process.waitFor();
        } catch (Throwable ignored) {
        }
    }

    private void scanLinuxPciDevices(List<GpuDeviceInfo> outList) {
        try {
            Process process = new ProcessBuilder(
                    "lspci"
            ).redirectErrorStream(true).start();

            try (BufferedReader reader = new BufferedReader(
                    new InputStreamReader(process.getInputStream(), StandardCharsets.UTF_8))) {
                String line;
                int idCounter = 0;
                while ((line = reader.readLine()) != null) {
                    String lower = line.toLowerCase(Locale.ROOT);
                    if (lower.contains("vga") || lower.contains("3d controller")) {
                        if (lower.contains("nvidia")) {
                            outList.add(new GpuDeviceInfo(
                                    idCounter++, line, "NVIDIA", GpuDeviceKind.DISCRETE, 8L * 1024 * 1024 * 1024, 256, true
                            ));
                        } else if (lower.contains("amd") || lower.contains("ati")) {
                            outList.add(new GpuDeviceInfo(
                                    idCounter++, line, "AMD", GpuDeviceKind.DISCRETE, 4L * 1024 * 1024 * 1024, 64, true
                            ));
                        } else if (lower.contains("intel")) {
                            outList.add(new GpuDeviceInfo(
                                    idCounter++, line, "Intel", GpuDeviceKind.INTEGRATED, 2L * 1024 * 1024 * 1024, 32, true
                            ));
                        }
                    }
                }
            }
            process.waitFor();
        } catch (Throwable ignored) {
        }
    }
}

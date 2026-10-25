package dev.vessel.kernel.vstm.remap;

/**
 * Immutable value holder reporting detailed telemetry from a JAR stream transmutation pass.
 *
 * <p>Tracks counts of rewritten class bytecode entries, preserved assets, and total bytes processed
 * (Catalog ref: USML-0021, FOL-0040).</p>
 *
 * @version 1.0.0
 * @since 1.0.0 (Wave 3)
 */
public final class VstmRemapStatistics {

    private final int classesTransmuted;
    private final int resourcesPreserved;
    private final long bytesProcessed;

    /**
     * Constructs an immutable transmutation statistics record.
     *
     * @param classesTransmuted  total number of .class bytecode files remapped
     * @param resourcesPreserved total number of resource assets copied unchanged
     * @param bytesProcessed     total uncompressed bytes processed through the stream
     */
    public VstmRemapStatistics(int classesTransmuted, int resourcesPreserved, long bytesProcessed) {
        this.classesTransmuted = classesTransmuted;
        this.resourcesPreserved = resourcesPreserved;
        this.bytesProcessed = bytesProcessed;
    }

    /**
     * Retrieves the count of rewritten class files.
     *
     * @return the number of transmuted classes
     */
    public int getClassesTransmuted() {
        return classesTransmuted;
    }

    /**
     * Retrieves the count of preserved resource files.
     *
     * @return the number of preserved assets
     */
    public int getResourcesPreserved() {
        return resourcesPreserved;
    }

    /**
     * Retrieves the total bytes processed across all entries.
     *
     * @return the total bytes processed
     */
    public long getBytesProcessed() {
        return bytesProcessed;
    }

    @Override
    public String toString() {
        return "VstmRemapStatistics[" +
                "classesTransmuted=" + classesTransmuted +
                ", resourcesPreserved=" + resourcesPreserved +
                ", bytesProcessed=" + bytesProcessed +
                ']';
    }
}

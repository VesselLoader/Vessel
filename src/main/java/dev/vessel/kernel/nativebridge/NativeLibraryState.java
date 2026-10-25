package dev.vessel.kernel.nativebridge;

/**
 * State lifecycle indicators for native shared libraries managed by the Root ClassLoader.
 *
 * @version 1.0.0
 * @author VESSEL Engine Team
 */
public enum NativeLibraryState {

    /** Library metadata registered but binary not yet loaded into JVM address space. */
    REGISTERED,

    /** Native binary actively being loaded into JVM address space. */
    LOADING,

    /** Native binary successfully bound and linked. */
    LOADED,

    /** Native library loading failed. */
    FAILED
}

package dev.vessel.kernel.optimization.leak;

/**
 * Defines the lifecycle boundaries at which allocated resources must be disposed.
 * Inspired by Memory Leak Fix mod to prevent static map and listener accumulation.
 *
 * @version 1.0.0
 */
public enum LifecycleDisposalScope {

    /**
     * Resources released when a connected player disconnects from the server.
     */
    PLAYER_DISCONNECT,

    /**
     * Resources released when a chunk section or 16x16 chunk is unloaded from memory.
     */
    CHUNK_UNLOAD,

    /**
     * Resources released when a dimension or world level is unloaded.
     */
    WORLD_UNLOAD,

    /**
     * Resources released only when the entire VESSEL server process shuts down.
     */
    SERVER_STOP
}

package dev.vessel.kernel.registry;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.concurrent.atomic.AtomicBoolean;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Unit tests for {@link DynamicRegistryFreezeSentinel} verifying safe registry freezing and scoped unfreeze.
 *
 * @version 1.0.0
 * @author VESSEL Engine Team
 */
public class DynamicRegistryFreezeSentinelTest {

    @Test
    @DisplayName("Lifecycle: Default registries start in BOOT_PENDING allowing registrations")
    void testInitialRegistryState() {
        DynamicRegistryFreezeSentinel sentinel = new DynamicRegistryFreezeSentinel();

        assertThat(sentinel.getState(DynamicRegistryFreezeSentinel.REGISTRY_BLOCK))
                .isEqualTo(RegistryFreezeState.BOOT_PENDING);
        assertThat(sentinel.canRegister(DynamicRegistryFreezeSentinel.REGISTRY_BLOCK)).isTrue();
        assertThat(sentinel.isFrozen(DynamicRegistryFreezeSentinel.REGISTRY_BLOCK)).isFalse();
    }

    @Test
    @DisplayName("Seal: Freezes registry disallowing normal element registration")
    void testSealRegistry() {
        DynamicRegistryFreezeSentinel sentinel = new DynamicRegistryFreezeSentinel();

        sentinel.seal(DynamicRegistryFreezeSentinel.REGISTRY_ITEM);

        assertThat(sentinel.getState(DynamicRegistryFreezeSentinel.REGISTRY_ITEM))
                .isEqualTo(RegistryFreezeState.SEALED);
        assertThat(sentinel.canRegister(DynamicRegistryFreezeSentinel.REGISTRY_ITEM)).isFalse();
        assertThat(sentinel.isFrozen(DynamicRegistryFreezeSentinel.REGISTRY_ITEM)).isTrue();
    }

    @Test
    @DisplayName("ScopedUnfreeze: Safely executes late mod registration and re-seals registry afterwards")
    void testScopedUnfreeze() {
        DynamicRegistryFreezeSentinel sentinel = new DynamicRegistryFreezeSentinel();
        sentinel.seal(DynamicRegistryFreezeSentinel.REGISTRY_BLOCK);

        RegistryLockToken token = sentinel.issueToken(
                DynamicRegistryFreezeSentinel.REGISTRY_BLOCK,
                "ForgeLateRegistryModule"
        );

        AtomicBoolean registrationExecuted = new AtomicBoolean(false);

        sentinel.executeScopedUnfreeze(DynamicRegistryFreezeSentinel.REGISTRY_BLOCK, token, () -> {
            assertThat(sentinel.canRegister(DynamicRegistryFreezeSentinel.REGISTRY_BLOCK)).isTrue();
            registrationExecuted.set(true);
        });

        assertThat(registrationExecuted.get()).isTrue();
        // Automatically re-sealed after scope completion
        assertThat(sentinel.isFrozen(DynamicRegistryFreezeSentinel.REGISTRY_BLOCK)).isTrue();
    }
}

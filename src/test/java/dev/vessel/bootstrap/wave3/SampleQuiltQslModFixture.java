package dev.vessel.bootstrap.wave3;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import net.fabricmc.loader.api.ModContainer;
import org.quiltmc.qsl.base.api.entrypoint.ModInitializer;
import org.quiltmc.qsl.base.api.entrypoint.server.DedicatedServerModInitializer;

/**
 * Sample Quilt Standard Libraries (QSL) mod fixture implementing both {@link ModInitializer}
 * and {@link DedicatedServerModInitializer} to verify Wave 3 live Quilt entrypoint activation.
 *
 * <p>Strictly adheres to Rule 17 with zero inner, nested, or anonymous classes.</p>
 *
 * @version 1.0.0
 */
public final class SampleQuiltQslModFixture
        implements ModInitializer, DedicatedServerModInitializer {

    public static final List<String> QUILT_LOG = new CopyOnWriteArrayList<>();

    public SampleQuiltQslModFixture() {
    }

    @Override
    public void onInitialize(ModContainer mod) {
        QUILT_LOG.add("QUILT_MAIN:" + mod.getModId());
    }

    @Override
    public void onInitializeServer(ModContainer mod) {
        QUILT_LOG.add("QUILT_SERVER:" + mod.getModId());
    }
}

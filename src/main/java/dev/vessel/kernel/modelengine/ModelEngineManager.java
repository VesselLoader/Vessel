package dev.vessel.kernel.modelengine;

import dev.vessel.kernel.modelengine.bedrock.GeyserEntityRegistrationBridge;
import dev.vessel.kernel.modelengine.bone.BoneHierarchyTree;
import dev.vessel.kernel.modelengine.bone.ResourcePackModelMapping;
import dev.vessel.kernel.modelengine.driver.CustomDropItem;
import dev.vessel.kernel.modelengine.driver.DriverDropSuppressor;
import dev.vessel.kernel.modelengine.driver.DriverHealthScaler;
import dev.vessel.kernel.modelengine.driver.DriverInvisibilityGovernor;
import dev.vessel.kernel.modelengine.driver.DriverMobController;
import dev.vessel.kernel.modelengine.driver.DriverMobState;
import dev.vessel.kernel.modelengine.driver.DroppedItemEntry;
import dev.vessel.kernel.modelengine.hitbox.DamageRelayResult;
import dev.vessel.kernel.modelengine.hitbox.DynamicHitbox;
import dev.vessel.kernel.modelengine.hitbox.HitboxInteractionEvent;
import dev.vessel.kernel.optimization.entity.PlayerSpatialCoordinates;

import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Central Model Engine manager and orchestrator for VESSEL.
 * Governs active model instances, dynamic hitboxes, damage relay, drop suppression,
 * LOD throttling, and GeyserMC crossplay integration.
 *
 * <p>Strictly adheres to Rule 17 with zero inner, nested, or anonymous classes.</p>
 *
 * @version 1.0.0
 * @author VESSEL Engine Team
 */
public final class ModelEngineManager {

    private final Map<Integer, ActiveModelInstance> instancesByDriverId = new ConcurrentHashMap<>();
    private final Map<Integer, ActiveModelInstance> instancesByHitboxId = new ConcurrentHashMap<>();
    private final Map<String, BoneHierarchyTree> modelTemplates = new ConcurrentHashMap<>();

    private final ResourcePackModelMapping resourcePackMapping = new ResourcePackModelMapping();
    private final GeyserEntityRegistrationBridge geyserBridge = new GeyserEntityRegistrationBridge();

    /**
     * Constructs a ModelEngineManager.
     */
    public ModelEngineManager() {
    }

    /**
     * Registers a model template bone hierarchy.
     *
     * @param modelId model identifier
     * @param tree    bone hierarchy template
     */
    public void registerModelTemplate(String modelId, BoneHierarchyTree tree) {
        Objects.requireNonNull(modelId, "modelId cannot be null");
        Objects.requireNonNull(tree, "tree cannot be null");
        modelTemplates.put(modelId, tree);
    }

    /**
     * Spawns and registers an active Model Engine custom entity instance.
     *
     * @param instanceId      unique instance ID
     * @param modelId         model template identifier
     * @param driverEntityId  Minecraft entity ID of the driver mob
     * @param driverUuid      UUID of the driver mob
     * @param mobType         vanilla entity type (e.g. "minecraft:pig")
     * @param x               initial X coordinate
     * @param y               initial Y coordinate
     * @param z               initial Z coordinate
     * @param maxHealth       custom max health (e.g. 100.0f, 200.0f, 500.0f)
     * @param suppressDrops   whether default entity drops should be cancelled
     * @param customDrops     optional custom drop items
     * @return newly spawned ActiveModelInstance
     */
    public ActiveModelInstance spawnInstance(
            String instanceId,
            String modelId,
            int driverEntityId,
            UUID driverUuid,
            String mobType,
            double x, double y, double z,
            float maxHealth,
            boolean suppressDrops,
            List<CustomDropItem> customDrops
    ) {
        BoneHierarchyTree template = modelTemplates.get(modelId);
        if (template == null) {
            throw new IllegalArgumentException("Unknown model template: " + modelId);
        }

        DriverMobState state = new DriverMobState(driverEntityId, driverUuid, mobType);
        state.setPosition(x, y, z);

        DriverHealthScaler healthScaler = new DriverHealthScaler(maxHealth);
        DriverInvisibilityGovernor invisibilityGovernor = new DriverInvisibilityGovernor();
        DriverDropSuppressor dropSuppressor = new DriverDropSuppressor();
        dropSuppressor.setSuppressDefaultDrops(suppressDrops);

        if (customDrops != null) {
            for (int i = 0; i < customDrops.size(); i++) {
                dropSuppressor.addCustomDrop(customDrops.get(i));
            }
        }

        DriverMobController controller = new DriverMobController(
                state,
                healthScaler,
                invisibilityGovernor,
                dropSuppressor
        );

        ActiveModelInstance instance = new ActiveModelInstance(instanceId, modelId, controller, template);

        instancesByDriverId.put(driverEntityId, instance);
        for (DynamicHitbox hitbox : instance.getHitboxes().values()) {
            instancesByHitboxId.put(hitbox.getVirtualEntityId(), instance);
        }

        return instance;
    }

    /**
     * Associates a newly registered hitbox entity ID with its model instance.
     *
     * @param hitboxEntityId entity ID of the hitbox
     * @param instance       associated instance
     */
    public void indexHitbox(int hitboxEntityId, ActiveModelInstance instance) {
        instancesByHitboxId.put(hitboxEntityId, instance);
    }

    /**
     * Executes the periodic engine tick across all active model instances.
     *
     * @param currentTick server tick count
     * @param players     active player spatial coordinates
     */
    public void tick(long currentTick, Collection<PlayerSpatialCoordinates> players) {
        for (ActiveModelInstance instance : instancesByDriverId.values()) {
            instance.tick(currentTick, players);
        }
    }

    /**
     * Intercepts an attack against any dynamic hitbox, routing it to the appropriate model instance.
     *
     * @param hitboxEntityId Minecraft entity ID of the struck hitbox
     * @param rawDamage      raw incoming damage
     * @return outcome record, or null if hitbox not recognized
     */
    public DamageRelayResult relayAttack(int hitboxEntityId, float rawDamage) {
        ActiveModelInstance instance = instancesByHitboxId.get(hitboxEntityId);
        if (instance == null) {
            return null;
        }
        return instance.relayDamage(hitboxEntityId, rawDamage);
    }

    /**
     * Intercepts an interaction against any dynamic hitbox, routing it to the appropriate model instance.
     *
     * @param hitboxEntityId Minecraft entity ID of the interacted hitbox
     * @param playerId       player entity ID
     * @param isMainHand     whether main hand was used
     * @param clickX         click coordinate X
     * @param clickY         click coordinate Y
     * @param clickZ         click coordinate Z
     * @return interaction event record, or null if hitbox not recognized
     */
    public HitboxInteractionEvent relayInteraction(
            int hitboxEntityId,
            int playerId,
            boolean isMainHand,
            double clickX, double clickY, double clickZ
    ) {
        ActiveModelInstance instance = instancesByHitboxId.get(hitboxEntityId);
        if (instance == null) {
            return null;
        }
        return instance.relayInteraction(hitboxEntityId, playerId, isMainHand, clickX, clickY, clickZ);
    }

    /**
     * Handles death of a driver entity, cleaning up hitboxes and generating custom drops.
     *
     * @param driverEntityId driver mob entity ID
     * @param lootingLevel   looting enchantment level
     * @return list of custom drops produced
     */
    public List<DroppedItemEntry> handleDeath(int driverEntityId, int lootingLevel) {
        ActiveModelInstance instance = instancesByDriverId.remove(driverEntityId);
        if (instance == null) {
            return Collections.emptyList();
        }

        // Clean up hitbox indices
        for (DynamicHitbox hitbox : instance.getHitboxes().values()) {
            instancesByHitboxId.remove(hitbox.getVirtualEntityId());
        }

        return instance.onDeath(lootingLevel);
    }

    /**
     * Removes and disposes an active instance.
     *
     * @param driverEntityId driver mob entity ID
     */
    public void removeInstance(int driverEntityId) {
        ActiveModelInstance instance = instancesByDriverId.remove(driverEntityId);
        if (instance != null) {
            for (DynamicHitbox hitbox : instance.getHitboxes().values()) {
                instancesByHitboxId.remove(hitbox.getVirtualEntityId());
            }
        }
    }

    public ActiveModelInstance getInstanceByDriverId(int driverEntityId) {
        return instancesByDriverId.get(driverEntityId);
    }

    public ActiveModelInstance getInstanceByHitboxId(int hitboxEntityId) {
        return instancesByHitboxId.get(hitboxEntityId);
    }

    public ResourcePackModelMapping getResourcePackMapping() {
        return resourcePackMapping;
    }

    public GeyserEntityRegistrationBridge getGeyserBridge() {
        return geyserBridge;
    }

    public int getActiveInstanceCount() {
        return instancesByDriverId.size();
    }
}

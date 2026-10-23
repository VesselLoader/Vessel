package dev.vessel.kernel.modelengine;

import dev.vessel.kernel.display.model.BoneTransformState;
import dev.vessel.kernel.display.model.VirtualDisplayEntityBridge;
import dev.vessel.kernel.modelengine.bone.BoneHierarchyTree;
import dev.vessel.kernel.modelengine.bone.BoneNode;
import dev.vessel.kernel.modelengine.bone.ComputedBoneTransform;
import dev.vessel.kernel.modelengine.bone.ItemDisplayRenderer;
import dev.vessel.kernel.modelengine.driver.DriverMobController;
import dev.vessel.kernel.modelengine.driver.DroppedItemEntry;
import dev.vessel.kernel.modelengine.hitbox.DamageRelayCoordinator;
import dev.vessel.kernel.modelengine.hitbox.DamageRelayResult;
import dev.vessel.kernel.modelengine.hitbox.DynamicHitbox;
import dev.vessel.kernel.modelengine.hitbox.HitboxInteractionEvent;
import dev.vessel.kernel.modelengine.hitbox.HitboxInteractionRelay;
import dev.vessel.kernel.modelengine.hitbox.HitboxSpatialCalculator;
import dev.vessel.kernel.modelengine.hitbox.ModernInteractionHitboxSimulator;
import dev.vessel.kernel.modelengine.lod.ModelAnimationLODGovernor;
import dev.vessel.kernel.modelengine.lod.ModelAnimationLODTier;
import dev.vessel.kernel.modelengine.lod.MovementDeltaCullingFilter;
import dev.vessel.kernel.modelengine.lod.PassengerMountTracker;
import dev.vessel.kernel.optimization.entity.PlayerSpatialCoordinates;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * Runtime active instance of a Model Engine custom entity.
 * Coordinates driver mob AI, bone hierarchy animation, dynamic hitboxes,
 * passenger mounting, delta threshold culling, and activation range LOD.
 *
 * <p>Strictly adheres to Rule 17 with zero inner, nested, or anonymous classes.</p>
 *
 * @version 1.0.0
 * @author VESSEL Engine Team
 */
public final class ActiveModelInstance {

    private final String instanceId;
    private final String modelId;

    private final DriverMobController driverController;
    private final BoneHierarchyTree boneHierarchy;

    private final Map<String, DynamicHitbox> hitboxes = new LinkedHashMap<>();
    private final Map<Integer, DynamicHitbox> hitboxesByEntityId = new LinkedHashMap<>();
    private final Map<String, ModernInteractionHitboxSimulator> interactionSimulators = new LinkedHashMap<>();

    private final Map<String, VirtualDisplayEntityBridge> boneDisplayEntities = new LinkedHashMap<>();
    private final Map<String, ItemDisplayRenderer> itemDisplayRenderers = new LinkedHashMap<>();

    private final MovementDeltaCullingFilter deltaCullingFilter;
    private final ModelAnimationLODGovernor lodGovernor;
    private final PassengerMountTracker passengerTracker;
    private final HitboxSpatialCalculator spatialCalculator;
    private final DamageRelayCoordinator damageRelayCoordinator;
    private final HitboxInteractionRelay interactionRelay;

    private ModelAnimationLODTier activeTier = ModelAnimationLODTier.NEAR;
    private long totalTicksRun = 0;
    private long boneUpdatesRun = 0;

    /**
     * Constructs an ActiveModelInstance.
     *
     * @param instanceId       unique runtime instance ID
     * @param modelId          model definition ID
     * @param driverController driver mob controller
     * @param boneHierarchy    skeletal hierarchy tree
     */
    public ActiveModelInstance(
            String instanceId,
            String modelId,
            DriverMobController driverController,
            BoneHierarchyTree boneHierarchy
    ) {
        this.instanceId = Objects.requireNonNull(instanceId, "instanceId cannot be null");
        this.modelId = Objects.requireNonNull(modelId, "modelId cannot be null");
        this.driverController = Objects.requireNonNull(driverController, "driverController cannot be null");
        this.boneHierarchy = Objects.requireNonNull(boneHierarchy, "boneHierarchy cannot be null");

        this.deltaCullingFilter = new MovementDeltaCullingFilter();
        this.lodGovernor = new ModelAnimationLODGovernor();
        this.passengerTracker = new PassengerMountTracker(driverController.getState().getEntityId());
        this.spatialCalculator = new HitboxSpatialCalculator();
        this.damageRelayCoordinator = new DamageRelayCoordinator();
        this.interactionRelay = new HitboxInteractionRelay();

        // Initialize bone display entities
        initializeBoneDisplayEntities();
    }

    private void initializeBoneDisplayEntities() {
        for (BoneNode bone : boneHierarchy.getAllBones().values()) {
            BoneTransformState transformState = new BoneTransformState(bone.getBoneName());
            VirtualDisplayEntityBridge bridge = new VirtualDisplayEntityBridge(bone.getBoneName(), transformState);
            bridge.setCustomModelData(bone.getCustomModelData());

            boneDisplayEntities.put(bone.getBoneName(), bridge);
            itemDisplayRenderers.put(bone.getBoneName(), new ItemDisplayRenderer(bone, bridge));

            // Mount bone display entity onto root mob as passenger
            passengerTracker.addPassenger(bridge.getVirtualEntityId());
        }
    }

    /**
     * Registers a dynamic hitbox onto this model instance.
     *
     * @param hitbox dynamic hitbox to add
     */
    public synchronized void addHitbox(DynamicHitbox hitbox) {
        Objects.requireNonNull(hitbox, "hitbox cannot be null");
        hitboxes.put(hitbox.getHitboxId(), hitbox);
        hitboxesByEntityId.put(hitbox.getVirtualEntityId(), hitbox);

        interactionSimulators.put(hitbox.getHitboxId(), new ModernInteractionHitboxSimulator(hitbox));

        // Mount hitbox onto vehicle as passenger to follow client movement automatically
        passengerTracker.addPassenger(hitbox.getVirtualEntityId());

        // Initial spatial position calculation
        updateSingleHitboxPosition(hitbox);
    }

    /**
     * Performs an engine game tick on this active model instance.
     * Evaluates LOD, delta threshold culling, bone transforms, and hitbox positions.
     *
     * @param currentTick current server game tick
     * @param players     active player positions in the world
     */
    public synchronized void tick(long currentTick, Collection<PlayerSpatialCoordinates> players) {
        totalTicksRun++;

        if (!driverController.isAlive()) {
            return;
        }

        driverController.tick();

        double currentX = driverController.getState().getPosX();
        double currentY = driverController.getState().getPosY();
        double currentZ = driverController.getState().getPosZ();
        float currentYaw = driverController.getState().getYaw();
        float currentPitch = driverController.getState().getPitch();

        // Step 1: Resolve LOD tier
        this.activeTier = lodGovernor.resolveTier(currentX, currentY, currentZ, players);

        // Step 2: Passenger mount movement tracking
        passengerTracker.recordMovementTick();

        // Step 3: Delta threshold culling check
        boolean positionChanged = deltaCullingFilter.shouldDispatchUpdate(currentX, currentY, currentZ, currentYaw, currentPitch);

        // Step 4: Check if bone animations should update under active LOD
        boolean shouldUpdateBones = activeTier.shouldUpdateAnimation(currentTick, driverController.getState().getEntityId());

        if (shouldUpdateBones) {
            boneUpdatesRun++;
            updateBoneTransforms();
        }

        // Step 5: Update hitbox spatial coordinates if mob moved or bones updated
        if (positionChanged || shouldUpdateBones) {
            updateAllHitboxPositions();
        }
    }

    /**
     * Recomputes global bone transformations and updates ItemDisplay renderers.
     */
    public synchronized void updateBoneTransforms() {
        Map<String, ComputedBoneTransform> transforms = boneHierarchy.computeGlobalTransforms();
        for (Map.Entry<String, ComputedBoneTransform> entry : transforms.entrySet()) {
            ItemDisplayRenderer renderer = itemDisplayRenderers.get(entry.getKey());
            if (renderer != null) {
                renderer.applyTransform(entry.getValue());
            }
        }
    }

    /**
     * Updates spatial coordinates for all registered hitboxes.
     */
    public synchronized void updateAllHitboxPositions() {
        for (DynamicHitbox hitbox : hitboxes.values()) {
            updateSingleHitboxPosition(hitbox);
        }
    }

    private void updateSingleHitboxPosition(DynamicHitbox hitbox) {
        double rootX = driverController.getState().getPosX();
        double rootY = driverController.getState().getPosY();
        double rootZ = driverController.getState().getPosZ();
        float rootYaw = driverController.getState().getYaw();

        String attachedBone = hitbox.getAttachedBone();
        if (attachedBone == null || attachedBone.isEmpty()) {
            spatialCalculator.updateRootHitboxPosition(hitbox, rootX, rootY, rootZ, rootYaw);
        } else {
            VirtualDisplayEntityBridge bridge = boneDisplayEntities.get(attachedBone);
            if (bridge != null) {
                BoneTransformState state = bridge.getTransformState();
                spatialCalculator.updateBoneHitboxPosition(
                        hitbox,
                        rootX, rootY, rootZ,
                        rootYaw,
                        state.getTranslationX(),
                        state.getTranslationY(),
                        state.getTranslationZ()
                );
            } else {
                spatialCalculator.updateRootHitboxPosition(hitbox, rootX, rootY, rootZ, rootYaw);
            }
        }
    }

    /**
     * Relays localized damage from an attacked hitbox to the driver mob.
     *
     * @param hitboxEntityId Minecraft entity ID of the attacked hitbox
     * @param rawDamage      raw incoming damage amount
     * @return outcome of damage relay
     */
    public synchronized DamageRelayResult relayDamage(int hitboxEntityId, float rawDamage) {
        DynamicHitbox hitbox = hitboxesByEntityId.get(hitboxEntityId);
        if (hitbox == null) {
            throw new IllegalArgumentException("No hitbox registered with entity ID: " + hitboxEntityId);
        }
        return damageRelayCoordinator.relayDamage(hitbox, driverController, rawDamage);
    }

    /**
     * Relays a player right-click interaction on a hitbox.
     *
     * @param hitboxEntityId entity ID of the hitbox
     * @param playerId       player entity ID
     * @param isMainHand     whether main hand was used
     * @param clickX         click coordinate X
     * @param clickY         click coordinate Y
     * @param clickZ         click coordinate Z
     * @return interaction event record
     */
    public synchronized HitboxInteractionEvent relayInteraction(
            int hitboxEntityId,
            int playerId,
            boolean isMainHand,
            double clickX, double clickY, double clickZ
    ) {
        DynamicHitbox hitbox = hitboxesByEntityId.get(hitboxEntityId);
        if (hitbox == null) {
            throw new IllegalArgumentException("No hitbox registered with entity ID: " + hitboxEntityId);
        }
        return interactionRelay.relayInteraction(hitbox, driverController, playerId, isMainHand, clickX, clickY, clickZ);
    }

    /**
     * Executes mob death handling and retrieves custom dropped items.
     *
     * @param lootingLevel looting enchantment level
     * @return list of custom items dropped
     */
    public synchronized List<DroppedItemEntry> onDeath(int lootingLevel) {
        return driverController.onDeath(lootingLevel);
    }

    public String getInstanceId() {
        return instanceId;
    }

    public String getModelId() {
        return modelId;
    }

    public DriverMobController getDriverController() {
        return driverController;
    }

    public BoneHierarchyTree getBoneHierarchy() {
        return boneHierarchy;
    }

    public Map<String, DynamicHitbox> getHitboxes() {
        return Collections.unmodifiableMap(new LinkedHashMap<>(hitboxes));
    }

    public DynamicHitbox getHitboxByEntityId(int entityId) {
        return hitboxesByEntityId.get(entityId);
    }

    public Map<String, VirtualDisplayEntityBridge> getBoneDisplayEntities() {
        return Collections.unmodifiableMap(new LinkedHashMap<>(boneDisplayEntities));
    }

    public Map<String, ItemDisplayRenderer> getItemDisplayRenderers() {
        return Collections.unmodifiableMap(new LinkedHashMap<>(itemDisplayRenderers));
    }

    public PassengerMountTracker getPassengerTracker() {
        return passengerTracker;
    }

    public MovementDeltaCullingFilter getDeltaCullingFilter() {
        return deltaCullingFilter;
    }

    public ModelAnimationLODGovernor getLodGovernor() {
        return lodGovernor;
    }

    public ModelAnimationLODTier getActiveTier() {
        return activeTier;
    }

    public long getTotalTicksRun() {
        return totalTicksRun;
    }

    public long getBoneUpdatesRun() {
        return boneUpdatesRun;
    }
}

package dev.vessel.kernel.optimization.physics;

import java.util.Arrays;
import java.util.Objects;

/**
 * Flat Primitive Bounding Volume Hierarchy (BVH) for high-speed broad-phase physics and collision queries.
 * <p>
 * In high-density server environments (such as farms with hundreds of mobs or dynamic contraptions),
 * broad-phase entity collision checks using object-based spatial trees or naive $O(N^2)$ checks
 * saturate the garbage collector and CPU caches.
 * </p>
 * <p>
 * This BVH stores all bounding volume boxes and binary tree topology in flat, contiguous
 * primitive arrays ({@code float[]} and {@code int[]}) with zero heap allocation during ray/box queries.
 * Traversal operations use an internal non-allocating integer stack, executing broad-phase checks in $O(\log N)$ time.
 * </p>
 *
 * @version 1.0.0
 * @author VESSEL Engine Team
 */
public final class SpatialBoundingVolumeHierarchy {

    public static final int NULL_NODE = -1;
    private static final int DEFAULT_INITIAL_LEAF_CAPACITY = 1024;
    private static final int STACK_CAPACITY = 256;

    // Node bounds: 6 floats per node (minX, minY, minZ, maxX, maxY, maxZ)
    private float[] bounds;
    // Node topology: separate primitive arrays for cache alignment
    private int[] leftChild;
    private int[] rightChild;
    private int[] parent;
    private int[] entityId;
    private int[] height;

    // Free list for node recycling
    private int[] freeList;
    private int freeCount;
    private int nodeCount;
    private int maxNodes;
    private int rootNode = NULL_NODE;
    private int leafCount = 0;

    // Open-addressing primitive hash map for entityId -> leafNodeIndex (0 allocation)
    private int[] entityHashKeys;
    private int[] entityHashValues;
    private int entityMapMask;
    private int entityMapCount;

    // Thread-safe / re-entrant stack for traversal
    private final int[] queryStack;

    /**
     * Constructs a SpatialBoundingVolumeHierarchy with default capacity.
     */
    public SpatialBoundingVolumeHierarchy() {
        this(DEFAULT_INITIAL_LEAF_CAPACITY);
    }

    /**
     * Constructs a SpatialBoundingVolumeHierarchy with specified leaf capacity.
     *
     * @param initialLeafCapacity expected number of tracked entities
     */
    public SpatialBoundingVolumeHierarchy(int initialLeafCapacity) {
        int initialLeaves = Math.max(16, initialLeafCapacity);
        this.maxNodes = initialLeaves * 2;

        this.bounds = new float[this.maxNodes * 6];
        this.leftChild = new int[this.maxNodes];
        this.rightChild = new int[this.maxNodes];
        this.parent = new int[this.maxNodes];
        this.entityId = new int[this.maxNodes];
        this.height = new int[this.maxNodes];

        Arrays.fill(this.leftChild, NULL_NODE);
        Arrays.fill(this.rightChild, NULL_NODE);
        Arrays.fill(this.parent, NULL_NODE);
        Arrays.fill(this.entityId, NULL_NODE);

        this.freeList = new int[this.maxNodes];
        this.freeCount = this.maxNodes;
        for (int i = 0; i < this.maxNodes; i++) {
            this.freeList[i] = this.maxNodes - 1 - i;
        }

        // Entity map table (power of two)
        int mapCapacity = 1;
        while (mapCapacity < initialLeaves * 2) {
            mapCapacity <<= 1;
        }
        this.entityHashKeys = new int[mapCapacity];
        this.entityHashValues = new int[mapCapacity];
        Arrays.fill(this.entityHashKeys, -1);
        Arrays.fill(this.entityHashValues, NULL_NODE);
        this.entityMapMask = mapCapacity - 1;
        this.entityMapCount = 0;

        this.queryStack = new int[STACK_CAPACITY];
    }

    /**
     * Inserts an entity bounding box into the hierarchy.
     *
     * @param id   unique entity identifier (>= 0)
     * @param minX minimum X
     * @param minY minimum Y
     * @param minZ minimum Z
     * @param maxX maximum X
     * @param maxY maximum Y
     * @param maxZ maximum Z
     */
    public synchronized void insert(int id, float minX, float minY, float minZ, float maxX, float maxY, float maxZ) {
        if (id < 0) {
            throw new IllegalArgumentException("entityId must be non-negative: " + id);
        }

        int existingLeaf = findLeafNode(id);
        if (existingLeaf != NULL_NODE) {
            update(id, minX, minY, minZ, maxX, maxY, maxZ);
            return;
        }

        int leaf = allocateNode();
        setNodeBounds(leaf, minX, minY, minZ, maxX, maxY, maxZ);
        entityId[leaf] = id;
        leftChild[leaf] = NULL_NODE;
        rightChild[leaf] = NULL_NODE;
        height[leaf] = 0;

        insertLeaf(leaf);
        putEntityMapping(id, leaf);
        leafCount++;
    }

    /**
     * Updates an entity's bounding box in the hierarchy.
     *
     * @param id   entity identifier
     * @param minX minimum X
     * @param minY minimum Y
     * @param minZ minimum Z
     * @param maxX maximum X
     * @param maxY maximum Y
     * @param maxZ maximum Z
     * @return true if entity was found and updated, false if not found
     */
    public synchronized boolean update(int id, float minX, float minY, float minZ, float maxX, float maxY, float maxZ) {
        int leaf = findLeafNode(id);
        if (leaf == NULL_NODE) {
            return false;
        }

        removeLeaf(leaf);
        setNodeBounds(leaf, minX, minY, minZ, maxX, maxY, maxZ);
        insertLeaf(leaf);
        return true;
    }

    /**
     * Removes an entity from the hierarchy.
     *
     * @param id entity identifier
     * @return true if entity was found and removed, false otherwise
     */
    public synchronized boolean remove(int id) {
        int leaf = findLeafNode(id);
        if (leaf == NULL_NODE) {
            return false;
        }

        removeLeaf(leaf);
        removeEntityMapping(id);
        freeNode(leaf);
        leafCount--;
        return true;
    }

    /**
     * Queries all intersecting entity boxes using a zero-allocation callback.
     *
     * @param minX     query volume min X
     * @param minY     query volume min Y
     * @param minZ     query volume min Z
     * @param maxX     query volume max X
     * @param maxY     query volume max Y
     * @param maxZ     query volume max Z
     * @param consumer collision callback
     * @return total number of candidate intersecting entities found
     */
    public synchronized int queryIntersections(
            float minX, float minY, float minZ,
            float maxX, float maxY, float maxZ,
            BvhCollisionConsumer consumer
    ) {
        Objects.requireNonNull(consumer, "consumer cannot be null");
        if (rootNode == NULL_NODE) {
            return 0;
        }

        int hits = 0;
        int stackPtr = 0;
        queryStack[stackPtr++] = rootNode;

        while (stackPtr > 0) {
            int node = queryStack[--stackPtr];
            if (node == NULL_NODE) {
                continue;
            }

            int bIdx = node * 6;
            if (testOverlap(minX, minY, minZ, maxX, maxY, maxZ,
                    bounds[bIdx], bounds[bIdx + 1], bounds[bIdx + 2],
                    bounds[bIdx + 3], bounds[bIdx + 4], bounds[bIdx + 5])) {

                if (leftChild[node] == NULL_NODE) {
                    // Leaf node
                    consumer.onCollision(
                            entityId[node],
                            bounds[bIdx], bounds[bIdx + 1], bounds[bIdx + 2],
                            bounds[bIdx + 3], bounds[bIdx + 4], bounds[bIdx + 5]
                    );
                    hits++;
                } else {
                    if (stackPtr + 2 >= queryStack.length) {
                        // Overflow guard
                        continue;
                    }
                    queryStack[stackPtr++] = leftChild[node];
                    queryStack[stackPtr++] = rightChild[node];
                }
            }
        }

        return hits;
    }

    /**
     * Queries intersecting entities and writes their IDs into a caller-supplied array.
     *
     * @param minX            query volume min X
     * @param minY            query volume min Y
     * @param minZ            query volume min Z
     * @param maxX            query volume max X
     * @param maxY            query volume max Y
     * @param maxZ            query volume max Z
     * @param outEntityIds    caller-allocated output buffer
     * @param maxResultsCount maximum number of results to write
     * @return count of entities written into outEntityIds
     */
    public synchronized int queryIntersections(
            float minX, float minY, float minZ,
            float maxX, float maxY, float maxZ,
            int[] outEntityIds, int maxResultsCount
    ) {
        Objects.requireNonNull(outEntityIds, "outEntityIds cannot be null");
        if (rootNode == NULL_NODE || maxResultsCount <= 0 || outEntityIds.length == 0) {
            return 0;
        }

        int limit = Math.min(maxResultsCount, outEntityIds.length);
        int hits = 0;
        int stackPtr = 0;
        queryStack[stackPtr++] = rootNode;

        while (stackPtr > 0 && hits < limit) {
            int node = queryStack[--stackPtr];
            if (node == NULL_NODE) {
                continue;
            }

            int bIdx = node * 6;
            if (testOverlap(minX, minY, minZ, maxX, maxY, maxZ,
                    bounds[bIdx], bounds[bIdx + 1], bounds[bIdx + 2],
                    bounds[bIdx + 3], bounds[bIdx + 4], bounds[bIdx + 5])) {

                if (leftChild[node] == NULL_NODE) {
                    outEntityIds[hits++] = entityId[node];
                } else {
                    if (stackPtr + 2 < queryStack.length) {
                        queryStack[stackPtr++] = leftChild[node];
                        queryStack[stackPtr++] = rightChild[node];
                    }
                }
            }
        }

        return hits;
    }

    /**
     * Returns the total number of active entity leaves in the BVH.
     *
     * @return leaf count
     */
    public synchronized int getLeafCount() {
        return leafCount;
    }

    /**
     * Returns the root node index or NULL_NODE if empty.
     *
     * @return root node index
     */
    public synchronized int getRootNode() {
        return rootNode;
    }

    /**
     * Clears all entities and resets the hierarchy to empty state.
     */
    public synchronized void clear() {
        rootNode = NULL_NODE;
        leafCount = 0;
        nodeCount = 0;
        freeCount = maxNodes;
        for (int i = 0; i < maxNodes; i++) {
            freeList[i] = maxNodes - 1 - i;
            leftChild[i] = NULL_NODE;
            rightChild[i] = NULL_NODE;
            parent[i] = NULL_NODE;
            entityId[i] = NULL_NODE;
            height[i] = 0;
        }
        Arrays.fill(entityHashKeys, -1);
        Arrays.fill(entityHashValues, NULL_NODE);
        entityMapCount = 0;
    }

    // --- Internal Tree Balancing & Insertion ---

    private void insertLeaf(int leaf) {
        if (rootNode == NULL_NODE) {
            rootNode = leaf;
            parent[rootNode] = NULL_NODE;
            return;
        }

        // Find best sibling using surface area heuristic approximation
        int sibling = rootNode;
        int lIdx = leaf * 6;
        float leafMinX = bounds[lIdx];
        float leafMinY = bounds[lIdx + 1];
        float leafMinZ = bounds[lIdx + 2];
        float leafMaxX = bounds[lIdx + 3];
        float leafMaxY = bounds[lIdx + 4];
        float leafMaxZ = bounds[lIdx + 5];

        while (leftChild[sibling] != NULL_NODE) {
            int left = leftChild[sibling];
            int right = rightChild[sibling];

            float area = combinedPerimeter(sibling, leafMinX, leafMinY, leafMinZ, leafMaxX, leafMaxY, leafMaxZ);
            float cost = 2.0f * area;

            int sIdx = sibling * 6;
            float inheritanceCost = 2.0f * (area - perimeter(bounds[sIdx], bounds[sIdx + 1], bounds[sIdx + 2],
                    bounds[sIdx + 3], bounds[sIdx + 4], bounds[sIdx + 5]));

            float costLeft;
            if (leftChild[left] == NULL_NODE) {
                costLeft = combinedPerimeter(left, leafMinX, leafMinY, leafMinZ, leafMaxX, leafMaxY, leafMaxZ) + inheritanceCost;
            } else {
                int leftIdx = left * 6;
                float oldArea = perimeter(bounds[leftIdx], bounds[leftIdx + 1], bounds[leftIdx + 2],
                        bounds[leftIdx + 3], bounds[leftIdx + 4], bounds[leftIdx + 5]);
                float newArea = combinedPerimeter(left, leafMinX, leafMinY, leafMinZ, leafMaxX, leafMaxY, leafMaxZ);
                costLeft = (newArea - oldArea) + inheritanceCost;
            }

            float costRight;
            if (leftChild[right] == NULL_NODE) {
                costRight = combinedPerimeter(right, leafMinX, leafMinY, leafMinZ, leafMaxX, leafMaxY, leafMaxZ) + inheritanceCost;
            } else {
                int rightIdx = right * 6;
                float oldArea = perimeter(bounds[rightIdx], bounds[rightIdx + 1], bounds[rightIdx + 2],
                        bounds[rightIdx + 3], bounds[rightIdx + 4], bounds[rightIdx + 5]);
                float newArea = combinedPerimeter(right, leafMinX, leafMinY, leafMinZ, leafMaxX, leafMaxY, leafMaxZ);
                costRight = (newArea - oldArea) + inheritanceCost;
            }

            if (cost < costLeft && cost < costRight) {
                break;
            }

            sibling = (costLeft < costRight) ? left : right;
        }

        // Create new internal parent
        int oldParent = parent[sibling];
        int newParent = allocateNode();
        parent[newParent] = oldParent;
        entityId[newParent] = NULL_NODE;

        unionBounds(newParent, leaf, sibling);
        height[newParent] = height[sibling] + 1;

        if (oldParent != NULL_NODE) {
            if (leftChild[oldParent] == sibling) {
                leftChild[oldParent] = newParent;
            } else {
                rightChild[oldParent] = newParent;
            }
            leftChild[newParent] = sibling;
            rightChild[newParent] = leaf;
            parent[sibling] = newParent;
            parent[leaf] = newParent;
        } else {
            leftChild[newParent] = sibling;
            rightChild[newParent] = leaf;
            parent[sibling] = newParent;
            parent[leaf] = newParent;
            rootNode = newParent;
        }

        // Walk back up the tree refitting AABBs
        int index = parent[leaf];
        while (index != NULL_NODE) {
            index = balance(index);
            int child1 = leftChild[index];
            int child2 = rightChild[index];

            height[index] = 1 + Math.max(height[child1], height[child2]);
            unionBounds(index, child1, child2);
            index = parent[index];
        }
    }

    private void removeLeaf(int leaf) {
        if (leaf == rootNode) {
            rootNode = NULL_NODE;
            return;
        }

        int parentNode = parent[leaf];
        int grandParent = parent[parentNode];
        int sibling = (leftChild[parentNode] == leaf) ? rightChild[parentNode] : leftChild[parentNode];

        if (grandParent != NULL_NODE) {
            if (leftChild[grandParent] == parentNode) {
                leftChild[grandParent] = sibling;
            } else {
                rightChild[grandParent] = sibling;
            }
            parent[sibling] = grandParent;
            freeNode(parentNode);

            int index = grandParent;
            while (index != NULL_NODE) {
                index = balance(index);
                int child1 = leftChild[index];
                int child2 = rightChild[index];

                unionBounds(index, child1, child2);
                height[index] = 1 + Math.max(height[child1], height[child2]);
                index = parent[index];
            }
        } else {
            rootNode = sibling;
            parent[sibling] = NULL_NODE;
            freeNode(parentNode);
        }

        parent[leaf] = NULL_NODE;
    }

    private int balance(int iA) {
        if (leftChild[iA] == NULL_NODE || height[iA] < 2) {
            return iA;
        }

        int iB = leftChild[iA];
        int iC = rightChild[iA];
        int balance = height[iC] - height[iB];

        // Rotate C up
        if (balance > 1) {
            int iF = leftChild[iC];
            int iG = rightChild[iC];

            rightChild[iC] = iA;
            parent[iC] = parent[iA];
            parent[iA] = iC;

            if (parent[iC] != NULL_NODE) {
                if (leftChild[parent[iC]] == iA) {
                    leftChild[parent[iC]] = iC;
                } else {
                    rightChild[parent[iC]] = iC;
                }
            } else {
                rootNode = iC;
            }

            // Rotate
            if (height[iF] > height[iG]) {
                rightChild[iA] = iF;
                parent[iF] = iA;
                unionBounds(iA, iB, iF);
                unionBounds(iC, iA, iG);
                height[iA] = 1 + Math.max(height[iB], height[iF]);
                height[iC] = 1 + Math.max(height[iA], height[iG]);
            } else {
                rightChild[iA] = iG;
                parent[iG] = iA;
                unionBounds(iA, iB, iG);
                unionBounds(iC, iA, iF);
                height[iA] = 1 + Math.max(height[iB], height[iG]);
                height[iC] = 1 + Math.max(height[iA], height[iF]);
            }
            return iC;
        }

        // Rotate B up
        if (balance < -1) {
            int iD = leftChild[iB];
            int iE = rightChild[iB];

            leftChild[iB] = iA;
            parent[iB] = parent[iA];
            parent[iA] = iB;

            if (parent[iB] != NULL_NODE) {
                if (leftChild[parent[iB]] == iA) {
                    leftChild[parent[iB]] = iB;
                } else {
                    rightChild[parent[iB]] = iB;
                }
            } else {
                rootNode = iB;
            }

            if (height[iD] > height[iE]) {
                leftChild[iA] = iE;
                parent[iE] = iA;
                unionBounds(iA, iC, iE);
                unionBounds(iB, iA, iD);
                height[iA] = 1 + Math.max(height[iC], height[iE]);
                height[iB] = 1 + Math.max(height[iA], height[iD]);
            } else {
                leftChild[iA] = iD;
                parent[iD] = iA;
                unionBounds(iA, iC, iD);
                unionBounds(iB, iA, iE);
                height[iA] = 1 + Math.max(height[iC], height[iD]);
                height[iB] = 1 + Math.max(height[iA], height[iE]);
            }
            return iB;
        }

        return iA;
    }

    private void unionBounds(int targetNode, int child1, int child2) {
        int tIdx = targetNode * 6;
        int c1Idx = child1 * 6;
        int c2Idx = child2 * 6;

        bounds[tIdx] = Math.min(bounds[c1Idx], bounds[c2Idx]);
        bounds[tIdx + 1] = Math.min(bounds[c1Idx + 1], bounds[c2Idx + 1]);
        bounds[tIdx + 2] = Math.min(bounds[c1Idx + 2], bounds[c2Idx + 2]);
        bounds[tIdx + 3] = Math.max(bounds[c1Idx + 3], bounds[c2Idx + 3]);
        bounds[tIdx + 4] = Math.max(bounds[c1Idx + 4], bounds[c2Idx + 4]);
        bounds[tIdx + 5] = Math.max(bounds[c1Idx + 5], bounds[c2Idx + 5]);
    }

    private float combinedPerimeter(int node, float minX, float minY, float minZ, float maxX, float maxY, float maxZ) {
        int idx = node * 6;
        float uMinX = Math.min(bounds[idx], minX);
        float uMinY = Math.min(bounds[idx + 1], minY);
        float uMinZ = Math.min(bounds[idx + 2], minZ);
        float uMaxX = Math.max(bounds[idx + 3], maxX);
        float uMaxY = Math.max(bounds[idx + 4], maxY);
        float uMaxZ = Math.max(bounds[idx + 5], maxZ);
        return perimeter(uMinX, uMinY, uMinZ, uMaxX, uMaxY, uMaxZ);
    }

    private static float perimeter(float minX, float minY, float minZ, float maxX, float maxY, float maxZ) {
        float dx = maxX - minX;
        float dy = maxY - minY;
        float dz = maxZ - minZ;
        return 2.0f * (dx * dy + dy * dz + dz * dx);
    }

    private static boolean testOverlap(
            float aMinX, float aMinY, float aMinZ, float aMaxX, float aMaxY, float aMaxZ,
            float bMinX, float bMinY, float bMinZ, float bMaxX, float bMaxY, float bMaxZ
    ) {
        return aMinX <= bMaxX && aMaxX >= bMinX &&
               aMinY <= bMaxY && aMaxY >= bMinY &&
               aMinZ <= bMaxZ && aMaxZ >= bMinZ;
    }

    private void setNodeBounds(int node, float minX, float minY, float minZ, float maxX, float maxY, float maxZ) {
        int idx = node * 6;
        bounds[idx] = minX;
        bounds[idx + 1] = minY;
        bounds[idx + 2] = minZ;
        bounds[idx + 3] = maxX;
        bounds[idx + 4] = maxY;
        bounds[idx + 5] = maxZ;
    }

    private int allocateNode() {
        if (freeCount > 0) {
            int node = freeList[--freeCount];
            nodeCount++;
            return node;
        }

        // Expand capacity
        int oldMax = maxNodes;
        int newMax = maxNodes * 2;
        bounds = Arrays.copyOf(bounds, newMax * 6);
        leftChild = Arrays.copyOf(leftChild, newMax);
        rightChild = Arrays.copyOf(rightChild, newMax);
        parent = Arrays.copyOf(parent, newMax);
        entityId = Arrays.copyOf(entityId, newMax);
        height = Arrays.copyOf(height, newMax);

        Arrays.fill(leftChild, oldMax, newMax, NULL_NODE);
        Arrays.fill(rightChild, oldMax, newMax, NULL_NODE);
        Arrays.fill(parent, oldMax, newMax, NULL_NODE);
        Arrays.fill(entityId, oldMax, newMax, NULL_NODE);

        freeList = new int[newMax];
        for (int i = oldMax; i < newMax; i++) {
            freeList[freeCount++] = newMax - 1 - (i - oldMax);
        }
        maxNodes = newMax;
        return freeList[--freeCount];
    }

    private void freeNode(int node) {
        leftChild[node] = NULL_NODE;
        rightChild[node] = NULL_NODE;
        parent[node] = NULL_NODE;
        entityId[node] = NULL_NODE;
        height[node] = 0;
        freeList[freeCount++] = node;
        nodeCount--;
    }

    // --- Flat Hash Map for EntityId -> Leaf Index ---

    private int findLeafNode(int id) {
        int mask = entityMapMask;
        int idx = hash(id) & mask;
        while (entityHashKeys[idx] != -1) {
            if (entityHashKeys[idx] == id) {
                return entityHashValues[idx];
            }
            idx = (idx + 1) & mask;
        }
        return NULL_NODE;
    }

    private void putEntityMapping(int id, int leaf) {
        if ((entityMapCount + 1) * 2 >= entityHashKeys.length) {
            resizeEntityMap();
        }
        int mask = entityMapMask;
        int idx = hash(id) & mask;
        while (entityHashKeys[idx] != -1 && entityHashKeys[idx] != id) {
            idx = (idx + 1) & mask;
        }
        if (entityHashKeys[idx] == -1) {
            entityMapCount++;
        }
        entityHashKeys[idx] = id;
        entityHashValues[idx] = leaf;
    }

    private void removeEntityMapping(int id) {
        int mask = entityMapMask;
        int idx = hash(id) & mask;
        while (entityHashKeys[idx] != -1) {
            if (entityHashKeys[idx] == id) {
                entityHashKeys[idx] = -1;
                entityHashValues[idx] = NULL_NODE;
                entityMapCount--;
                // Rehash cluster
                idx = (idx + 1) & mask;
                while (entityHashKeys[idx] != -1) {
                    int rehashKey = entityHashKeys[idx];
                    int rehashVal = entityHashValues[idx];
                    entityHashKeys[idx] = -1;
                    entityHashValues[idx] = NULL_NODE;
                    entityMapCount--;
                    putEntityMapping(rehashKey, rehashVal);
                    idx = (idx + 1) & mask;
                }
                return;
            }
            idx = (idx + 1) & mask;
        }
    }

    private void resizeEntityMap() {
        int[] oldKeys = entityHashKeys;
        int[] oldVals = entityHashValues;
        int newCap = oldKeys.length * 2;
        entityHashKeys = new int[newCap];
        entityHashValues = new int[newCap];
        Arrays.fill(entityHashKeys, -1);
        Arrays.fill(entityHashValues, NULL_NODE);
        entityMapMask = newCap - 1;
        entityMapCount = 0;
        for (int i = 0; i < oldKeys.length; i++) {
            if (oldKeys[i] != -1) {
                putEntityMapping(oldKeys[i], oldVals[i]);
            }
        }
    }

    private static int hash(int key) {
        key ^= (key >>> 20) ^ (key >>> 12);
        return key ^ (key >>> 7) ^ (key >>> 4);
    }
}

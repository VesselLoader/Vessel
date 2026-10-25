package dev.vessel.kernel.entity.ai;

import java.util.Arrays;
import java.util.Objects;
import java.util.UUID;
import java.util.concurrent.locks.ReentrantLock;
import java.util.function.Consumer;
import java.util.function.Predicate;

/**
 * High-performance Copy-On-Write goal container designed for high-frequency entity AI ticks
 * under Decision 45 and EAI-0401..0800.
 *
 * <p>Completely eradicates {@link java.util.ConcurrentModificationException} when Bukkit plugins
 * or Forge mods mutate mob goals asynchronously or mid-tick. Reads are lock-free and iterate
 * directly over an immutable volatile snapshot array without heap allocations.</p>
 *
 * <p>Strictly adheres to Rule 17 with zero inner or nested classes.</p>
 *
 * @author VESSEL Engine Team
 * @version 1.0.0
 */
public final class CopyOnWriteMobGoalList {

    private static final MobGoalEntry[] EMPTY_ARRAY = new MobGoalEntry[0];

    private final ReentrantLock modificationLock = new ReentrantLock();
    private volatile MobGoalEntry[] elements = EMPTY_ARRAY;

    /**
     * Constructs an empty copy-on-write mob goal list.
     */
    public CopyOnWriteMobGoalList() {
    }

    /**
     * Adds an AI goal to the list and maintains priority ordering.
     *
     * @param goal the goal to insert (non-null)
     * @return {@code true} if the goal was added
     */
    public boolean add(MobGoalEntry goal) {
        Objects.requireNonNull(goal, "goal cannot be null");
        modificationLock.lock();
        try {
            MobGoalEntry[] current = elements;
            int len = current.length;
            MobGoalEntry[] next = new MobGoalEntry[len + 1];
            System.arraycopy(current, 0, next, 0, len);
            next[len] = goal;
            Arrays.sort(next);
            elements = next;
            return true;
        } finally {
            modificationLock.unlock();
        }
    }

    /**
     * Removes a goal by its unique UUID.
     *
     * @param goalId UUID of the goal to remove
     * @return {@code true} if a goal was found and removed
     */
    public boolean remove(UUID goalId) {
        if (goalId == null) {
            return false;
        }
        modificationLock.lock();
        try {
            MobGoalEntry[] current = elements;
            int index = -1;
            for (int i = 0; i < current.length; i++) {
                if (goalId.equals(current[i].getGoalId())) {
                    index = i;
                    break;
                }
            }
            if (index == -1) {
                return false;
            }

            int numMoved = current.length - index - 1;
            MobGoalEntry[] next = new MobGoalEntry[current.length - 1];
            if (index > 0) {
                System.arraycopy(current, 0, next, 0, index);
            }
            if (numMoved > 0) {
                System.arraycopy(current, index + 1, next, index, numMoved);
            }
            elements = next;
            return true;
        } finally {
            modificationLock.unlock();
        }
    }

    /**
     * Removes all goals matching the given predicate.
     *
     * @param filter predicate returning true for elements to remove
     * @return number of elements removed
     */
    public int removeIf(Predicate<MobGoalEntry> filter) {
        Objects.requireNonNull(filter, "filter cannot be null");
        modificationLock.lock();
        try {
            MobGoalEntry[] current = elements;
            int matchCount = 0;
            for (MobGoalEntry entry : current) {
                if (filter.test(entry)) {
                    matchCount++;
                }
            }
            if (matchCount == 0) {
                return 0;
            }

            MobGoalEntry[] next = new MobGoalEntry[current.length - matchCount];
            int destIdx = 0;
            for (MobGoalEntry entry : current) {
                if (!filter.test(entry)) {
                    next[destIdx++] = entry;
                }
            }
            elements = next;
            return matchCount;
        } finally {
            modificationLock.unlock();
        }
    }

    /**
     * Traverses the current snapshot of AI goals without locks or allocations.
     * Guaranteed to never throw {@link java.util.ConcurrentModificationException}.
     *
     * @param consumer action performed on each active goal
     */
    public void forEachGoal(Consumer<MobGoalEntry> consumer) {
        Objects.requireNonNull(consumer, "consumer cannot be null");
        MobGoalEntry[] snapshot = elements;
        for (MobGoalEntry entry : snapshot) {
            consumer.accept(entry);
        }
    }

    /**
     * Finds a goal by name in the snapshot.
     *
     * @param name goal name
     * @return matching entry or null
     */
    public MobGoalEntry findByName(String name) {
        if (name == null) {
            return null;
        }
        MobGoalEntry[] snapshot = elements;
        for (MobGoalEntry entry : snapshot) {
            if (name.equalsIgnoreCase(entry.getName())) {
                return entry;
            }
        }
        return null;
    }

    /**
     * Clears all goals from this container.
     */
    public void clear() {
        modificationLock.lock();
        try {
            elements = EMPTY_ARRAY;
        } finally {
            modificationLock.unlock();
        }
    }

    /**
     * Returns the current number of registered goals.
     *
     * @return element count
     */
    public int size() {
        return elements.length;
    }

    /**
     * Checks if the list is empty.
     *
     * @return {@code true} if size is 0
     */
    public boolean isEmpty() {
        return elements.length == 0;
    }

    /**
     * Returns a direct reference to the immutable snapshot array for zero-alloc hot loops.
     *
     * @return current array snapshot
     */
    public MobGoalEntry[] getSnapshotArray() {
        return elements;
    }
}

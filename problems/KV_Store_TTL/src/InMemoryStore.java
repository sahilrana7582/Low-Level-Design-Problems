import java.time.Duration;
import java.util.HashMap;
import java.util.PriorityQueue;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.ReentrantReadWriteLock;

public class InMemoryStore implements Store{
    private final ReentrantReadWriteLock reentrantReadWriteLock;
    private final Condition sweeperCondition;
    private final PriorityQueue<ExpireKey> ttlExpireKeys;
    private final HashMap<String, DataValue> store;
    private final Thread sweeperThread;
    private volatile boolean running;

    public InMemoryStore(){
        this.reentrantReadWriteLock = new ReentrantReadWriteLock(true);
        this.sweeperCondition = reentrantReadWriteLock.writeLock().newCondition();
        this.ttlExpireKeys = new PriorityQueue<>(
                (a, b) -> Long.compare(a.getExpireAt(), b.getExpireAt())
        );
        this.store = new HashMap<>();

        this.running = true;
        this.sweeperThread = new Thread(this::runSweeper, "ttl-sweeper");
        this.sweeperThread.setDaemon(true);
        this.sweeperThread.start();
    }

    @Override
    public int put(String key, String value) {
        reentrantReadWriteLock.writeLock().lock();
        try {
            DataValue staleValue = store.get(key);
            long keyVersion = staleValue != null ? staleValue.getVersion() + 1 : 1;
            DataValue newValue = new DataValue(key, value, keyVersion, 0L);
            return store.put(key, newValue) != null ? 1 : 0;
        } finally {
            reentrantReadWriteLock.writeLock().unlock();
        }
    }

    @Override
    public int put(String key, String value, Duration ttl) {
        reentrantReadWriteLock.writeLock().lock();
        try {
            DataValue staleValue = store.get(key);
            long keyVersion = staleValue != null ? staleValue.getVersion() + 1 : 1;

            long expireAt = System.currentTimeMillis() + ttl.toMillis();
            DataValue newValue = new DataValue(key, value, keyVersion, expireAt);

            int result = store.put(key, newValue) != null ? 1 : 0;
            ttlExpireKeys.add(new ExpireKey(key, expireAt, keyVersion));
            // Wake the sweeper in case it's parked waiting on a later-expiring
            // head; it will re-peek and recompute its wait on the new entry.
            sweeperCondition.signal();
            return result;
        } finally {
            reentrantReadWriteLock.writeLock().unlock();
        }
    }

    @Override
    public String get(String key) {
        reentrantReadWriteLock.readLock().lock();
        try {
            DataValue dataValue = store.get(key);
            if (dataValue == null) {
                return null;
            }
            if (!dataValue.isExpired()) {
                return dataValue.getValue();
            }
        } finally {
            reentrantReadWriteLock.readLock().unlock();
        }

        // Fast path saw an expired entry. ReentrantReadWriteLock cannot upgrade
        // a held read lock into a write lock (that self-deadlocks), so the read
        // lock above is fully released before the write lock is acquired here.
        reentrantReadWriteLock.writeLock().lock();
        try {
            // Re-check under the write lock: another thread could have already
            // evicted this key, or overwritten it with a fresh value, in the gap
            // between releasing the read lock and acquiring the write lock.
            DataValue dataValue = store.get(key);
            if (dataValue == null) {
                return null;
            }
            if (dataValue.isExpired()) {
                store.remove(key);
                return null;
            }
            return dataValue.getValue();
        } finally {
            reentrantReadWriteLock.writeLock().unlock();
        }
    }

    @Override
    public int delete(String key) {
        reentrantReadWriteLock.writeLock().lock();
        try {
            return store.remove(key) != null ? 1 : 0;
        } finally {
            reentrantReadWriteLock.writeLock().unlock();
        }
    }

    /**
     * Background loop: wait until the earliest-expiring key is due, evict it if
     * the store still holds the exact version that was queued (a version
     * mismatch means the key was overwritten or deleted since this ExpireKey
     * was queued, so the entry is stale and simply dropped), then repeat.
     */
    private void runSweeper() {
        while (running) {
            reentrantReadWriteLock.writeLock().lock();
            try {
                while (running && ttlExpireKeys.isEmpty()) {
                    sweeperCondition.await();
                }
                if (!running) {
                    return;
                }

                ExpireKey head = ttlExpireKeys.peek();
                long remaining = head.getExpireAt() - System.currentTimeMillis();
                if (remaining > 0) {
                    // Released while parked, so put(..., ttl) can signal us
                    // early if it queues an entry earlier than this head.
                    sweeperCondition.await(remaining, TimeUnit.MILLISECONDS);
                    continue;
                }

                ttlExpireKeys.poll();
                DataValue current = store.get(head.getKey());
                if (current != null && current.getVersion() == head.getVersion()) {
                    store.remove(head.getKey());
                }
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                return;
            } finally {
                reentrantReadWriteLock.writeLock().unlock();
            }
        }
    }

    public void shutdown() {
        reentrantReadWriteLock.writeLock().lock();
        try {
            running = false;
            sweeperCondition.signalAll();
        } finally {
            reentrantReadWriteLock.writeLock().unlock();
        }
        try {
            sweeperThread.join();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }
}

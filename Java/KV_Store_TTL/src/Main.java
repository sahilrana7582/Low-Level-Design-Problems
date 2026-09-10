import java.time.Duration;

void main() throws InterruptedException {
    InMemoryStore store = new InMemoryStore();

    IO.println("--- put/get, no TTL ---");
    store.put("name", "sahil");
    IO.println("get(name) = " + store.get("name"));

    IO.println("--- put with TTL(2s), read before expiry ---");
    store.put("session", "token-abc", Duration.ofSeconds(2));
    IO.println("get(session) immediately = " + store.get("session"));

    IO.println("--- overwrite before expiry cancels the old TTL ---");
    store.put("temp", "v1", Duration.ofSeconds(2));
    Thread.sleep(500);
    store.put("temp", "v2"); // no ttl: bumps version, old queued ExpireKey is now stale
    Thread.sleep(2000); // past the original 2s window
    IO.println("get(temp) past original TTL = " + store.get("temp") + " (expect v2, sweeper must not evict it)");

    IO.println("--- sweeper evicts in the background, before get() is even called ---");
    store.put("ghost", "boo", Duration.ofSeconds(1));
    Thread.sleep(1500); // give the sweeper thread time to run on its own
    IO.println("get(ghost) after sweeper had time to run = " + store.get("ghost") + " (expect null)");

    IO.println("--- delete ---");
    store.put("gone", "v");
    IO.println("delete(gone) = " + store.delete("gone"));
    IO.println("get(gone) after delete = " + store.get("gone"));

    IO.println("--- concurrent writers on the same key ---");
    Runnable writer = () -> {
        for (int i = 0; i < 1000; i++) {
            store.put("counter", "v" + i);
        }
    };
    Thread t1 = new Thread(writer);
    Thread t2 = new Thread(writer);
    t1.start();
    t2.start();
    t1.join();
    t2.join();
    IO.println("final get(counter) = " + store.get("counter") + " (no crash, no corruption across two writer threads)");

    store.shutdown();
    IO.println("--- sweeper thread shut down cleanly ---");
}

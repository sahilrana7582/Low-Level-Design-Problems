import entities.Message;
import entities.Subscriber;

import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

public final class TrackingSubscriber implements Subscriber {

    private final String name;
    private final Set<String> receivedPayloads = ConcurrentHashMap.newKeySet();
    private final Map<String, AtomicLong> receivedPerTopic = new ConcurrentHashMap<>();

    public TrackingSubscriber(String name) {
        this.name = name;
    }

    @Override
    public void receive(Message message) {
        String payload = message.payload();
        if (!receivedPayloads.add(payload)) {
            throw new IllegalStateException(name + " received a duplicate message: " + payload);
        }
        String topicName = payload.substring(0, payload.indexOf('|'));
        receivedPerTopic.computeIfAbsent(topicName, t -> new AtomicLong()).incrementAndGet();
    }

    public long receivedFor(String topicName) {
        AtomicLong count = receivedPerTopic.get(topicName);
        return count == null ? 0 : count.get();
    }
}

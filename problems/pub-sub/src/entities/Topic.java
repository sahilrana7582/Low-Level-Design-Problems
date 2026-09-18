package entities;

import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.atomic.AtomicBoolean;

public final class Topic {

    private final String name;
    private final MpscRingBuffer<Message> queue;

    private final CopyOnWriteArrayList<Subscriber> subscribers =
            new CopyOnWriteArrayList<>();

    private final AtomicBoolean broadcasting = new AtomicBoolean(false);

    public Topic(String name, int capacity) {
        if (name == null || name.isBlank()) {
            throw new IllegalArgumentException("Topic name cannot be null or blank");
        }

        if (capacity <= 0) {
            throw new IllegalArgumentException("Capacity must be greater than zero");
        }

        this.name = name;
        this.queue = new MpscRingBuffer<>(capacity);
    }

    public PublishResult publish(Message message) {

        if (message == null) {
            throw new IllegalArgumentException("Message cannot be null");
        }

        if (!queue.offer(message)) {
            return PublishResult.FULL;
        }

        if (broadcasting.compareAndSet(false, true)) {
            broadcast();
        }

        return PublishResult.ACCEPTED;
    }

    public void subscribe(Subscriber subscriber) {

        if (subscriber == null) {
            throw new IllegalArgumentException("Subscriber cannot be null");
        }

        subscribers.addIfAbsent(subscriber);
    }

    public boolean unsubscribe(Subscriber subscriber) {

        if (subscriber == null) {
            return false;
        }

        return subscribers.remove(subscriber);
    }

    private void broadcast() {

        while (true) {
            try {
                Message message;

                while ((message = queue.poll()) != null) {
                    broadcastToSubscribers(message);
                }

            } finally {
                broadcasting.set(false);
            }

            if (queue.isEmpty() || !broadcasting.compareAndSet(false, true)) {
                return;
            }
            // else: re-acquired the flag, loop and drain again instead of recursing
        }
    }

    private void broadcastToSubscribers(Message message) {

        for (Subscriber subscriber : subscribers) {
            try {
                subscriber.receive(message);
            } catch (RuntimeException exception) {
                handleSubscriberFailure(
                        subscriber,
                        message,
                        exception
                );
            }
        }
    }

    private void handleSubscriberFailure(
            Subscriber subscriber,
            Message message,
            RuntimeException exception
    ) {
        System.out.println(
                "subscriber=" + subscriber +
                        ", message=" + message +
                        ", exception=" + exception
        );
    }

    public int pendingMessages() {
        return queue.size();
    }

    public int subscriberCount() {
        return subscribers.size();
    }

    public String name() {
        return name;
    }
}
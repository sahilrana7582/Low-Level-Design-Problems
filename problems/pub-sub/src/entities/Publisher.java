package entities;

import java.util.concurrent.atomic.AtomicLong;

public class Publisher {

    private final String name;
    private final Topic topic;
    private final AtomicLong sequence;

    public Publisher(String name, Topic topic, AtomicLong sequence) {
        this.name = name;
        this.topic = topic;
        this.sequence = sequence;
    }

    public PublishResult publish(String payload) {
        Message message = new Message(sequence.getAndIncrement(), System.nanoTime(), payload);
        return topic.publish(message);
    }

    public String name() {
        return name;
    }
}

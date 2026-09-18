import entities.Publisher;
import entities.PublishResult;

public final class ProducerTask {

    private final String topicName;
    private final String producerName;
    private final Publisher publisher;
    private final int messageCount;

    private long accepted;
    private long rejected;
    private long elapsedNanos;

    public ProducerTask(String topicName, String producerName, Publisher publisher, int messageCount) {
        this.topicName = topicName;
        this.producerName = producerName;
        this.publisher = publisher;
        this.messageCount = messageCount;
    }

    public void run() {
        long startNanos = System.nanoTime();
        for (int i = 0; i < messageCount; i++) {
            String payload = topicName + "|" + producerName + "|" + i;
            PublishResult result = publisher.publish(payload);
            if (result == PublishResult.ACCEPTED) {
                accepted++;
            } else {
                rejected++;
            }
        }
        elapsedNanos = System.nanoTime() - startNanos;
    }

    public String topicName() {
        return topicName;
    }

    public long accepted() {
        return accepted;
    }

    public long rejected() {
        return rejected;
    }

    public long elapsedNanos() {
        return elapsedNanos;
    }
}

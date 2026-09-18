import entities.Publisher;
import entities.Topic;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicLong;

void main() throws InterruptedException {
    String[] topicNames = {"orders", "payments", "shipping", "inventory"};
    int producersPerTopic = 5;
    int messagesPerProducer = 100_000;
    int topicCapacity = 1 << 25;

    Map<String, Topic> topics = new LinkedHashMap<>();
    for (String name : topicNames) {
        topics.put(name, new Topic(name, topicCapacity));
    }

    Map<String, TrackingSubscriber> subscribers = new LinkedHashMap<>();
    Map<String, List<String>> subscriberTopics = new LinkedHashMap<>();
    subscribe("audit-all", topics, subscribers, subscriberTopics, "orders", "payments", "shipping", "inventory");
    subscribe("commerce", topics, subscribers, subscriberTopics, "orders", "payments");
    subscribe("fulfillment", topics, subscribers, subscriberTopics, "shipping", "inventory");
    subscribe("orders-only", topics, subscribers, subscriberTopics, "orders");

    List<ProducerTask> producerTasks = new ArrayList<>();
    for (String topicName : topicNames) {
        Topic topic = topics.get(topicName);
        for (int p = 0; p < producersPerTopic; p++) {
            String producerName = topicName + "-producer-" + p;
            Publisher publisher = new Publisher(producerName, topic, new AtomicLong(0));
            producerTasks.add(new ProducerTask(topicName, producerName, publisher, messagesPerProducer));
        }
    }

    int producerCount = producerTasks.size();
    CountDownLatch ready = new CountDownLatch(producerCount);
    CountDownLatch start = new CountDownLatch(1);
    CountDownLatch finished = new CountDownLatch(producerCount);
    ExecutorService pool = Executors.newFixedThreadPool(producerCount);

    for (ProducerTask task : producerTasks) {
        pool.submit(() -> {
            ready.countDown();
            await(start);
            task.run();
            finished.countDown();
        });
    }

    ready.await();
    long startNanos = System.nanoTime();
    start.countDown();
    finished.await();
    long elapsedNanos = System.nanoTime() - startNanos;
    pool.shutdown();

    // publish() drives broadcast() synchronously on whichever thread wins the
    // drain handoff, so once every producer has returned from run(), every
    // ACCEPTED message for every topic has already been fanned out -- there
    // is no separate "wait for consumers" phase to add here.

    report(topicNames, topics, subscribers, subscriberTopics, producerTasks, elapsedNanos);
}

private static void subscribe(
        String consumerName,
        Map<String, Topic> topics,
        Map<String, TrackingSubscriber> subscribers,
        Map<String, List<String>> subscriberTopics,
        String... topicNames
) {
    TrackingSubscriber subscriber = new TrackingSubscriber(consumerName);
    subscribers.put(consumerName, subscriber);
    subscriberTopics.put(consumerName, List.of(topicNames));
    for (String topicName : topicNames) {
        topics.get(topicName).subscribe(subscriber);
    }
}

private static void report(
        String[] topicNames,
        Map<String, Topic> topics,
        Map<String, TrackingSubscriber> subscribers,
        Map<String, List<String>> subscriberTopics,
        List<ProducerTask> producerTasks,
        long elapsedNanos
) {
    Map<String, Long> acceptedByTopic = new LinkedHashMap<>();
    Map<String, Long> rejectedByTopic = new LinkedHashMap<>();
    for (String topicName : topicNames) {
        acceptedByTopic.put(topicName, 0L);
        rejectedByTopic.put(topicName, 0L);
    }

    long totalAccepted = 0;
    long totalRejected = 0;
    for (ProducerTask task : producerTasks) {
        acceptedByTopic.merge(task.topicName(), task.accepted(), Long::sum);
        rejectedByTopic.merge(task.topicName(), task.rejected(), Long::sum);
        totalAccepted += task.accepted();
        totalRejected += task.rejected();
    }

    System.out.println("=== per-topic producer results ===");
    for (String topicName : topicNames) {
        Topic topic = topics.get(topicName);
        System.out.printf(
                "topic=%-10s accepted=%-8d rejected=%-8d pending=%d%n",
                topicName, acceptedByTopic.get(topicName), rejectedByTopic.get(topicName),
                topic.pendingMessages()
        );
    }

    System.out.println("=== per-topic producer fairness (fastest/slowest producer on that topic) ===");
    for (String topicName : topicNames) {
        long min = Long.MAX_VALUE;
        long max = Long.MIN_VALUE;
        for (ProducerTask task : producerTasks) {
            if (task.topicName().equals(topicName)) {
                min = Math.min(min, task.elapsedNanos());
                max = Math.max(max, task.elapsedNanos());
            }
        }
        System.out.printf(
                "topic=%-10s fastest_producer=%.1fms slowest_producer=%.1fms%n",
                topicName, min / 1_000_000.0, max / 1_000_000.0
        );
    }

    System.out.println("=== subscriber verification ===");
    boolean allPass = true;
    for (Map.Entry<String, TrackingSubscriber> entry : subscribers.entrySet()) {
        String consumerName = entry.getKey();
        TrackingSubscriber subscriber = entry.getValue();
        for (String topicName : subscriberTopics.get(consumerName)) {
            long expected = acceptedByTopic.get(topicName);
            long actual = subscriber.receivedFor(topicName);
            boolean pass = expected == actual;
            allPass &= pass;
            System.out.printf(
                    "consumer=%-12s topic=%-10s expected=%-8d actual=%-8d %s%n",
                    consumerName, topicName, expected, actual, pass ? "PASS" : "FAIL"
            );
        }
    }

    double seconds = elapsedNanos / 1_000_000_000.0;
    System.out.printf(
            "%n=== summary ===%ntotal accepted=%d rejected=%d in %.2fs -> %.0f accepted msgs/sec%n",
            totalAccepted, totalRejected, seconds, totalAccepted / seconds
    );
    System.out.println(allPass
            ? "PASS: every subscriber received exactly what its topics accepted"
            : "FAIL: see mismatches above");

    if (!allPass) {
        throw new AssertionError("simulation correctness check failed");
    }
}

private static void await(CountDownLatch latch) {
    try {
        latch.await();
    } catch (InterruptedException e) {
        Thread.currentThread().interrupt();
    }
}

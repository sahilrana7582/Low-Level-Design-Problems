import java.util.Objects;
import java.util.concurrent.Delayed;
import java.util.concurrent.TimeUnit;

public class DelayMyTask implements Delayed, Runnable {

    private final String taskId;
    private final long executeAt;
    private final Runnable action;

    public DelayMyTask(
            String taskId,
            long delay,
            TimeUnit timeUnit,
            Runnable action
    ) {
        this.taskId = Objects.requireNonNull(taskId);
        this.action = Objects.requireNonNull(action);

        if (delay < 0) {
            throw new IllegalArgumentException("Delay cannot be negative");
        }

        this.executeAt =
                System.nanoTime() + timeUnit.toNanos(delay);
    }

    public String getTaskId() {
        return taskId;
    }

    public Runnable getAction() {
        return action;
    }

    @Override
    public long getDelay(TimeUnit unit) {
        long remainingNanos =
                executeAt - System.nanoTime();

        return unit.convert(
                remainingNanos,
                TimeUnit.NANOSECONDS
        );
    }

    @Override
    public int compareTo(Delayed other) {
        if (this == other) {
            return 0;
        }

        long difference =
                this.getDelay(TimeUnit.NANOSECONDS)
                        - other.getDelay(TimeUnit.NANOSECONDS);

        return Long.compare(difference, 0);
    }

    @Override
    public void run() {
        System.out.printf(
                "Task: %s is running now%n",
                this.taskId
        );

        action.run();
    }
}
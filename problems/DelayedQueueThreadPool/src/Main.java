import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.locks.ReentrantLock;

public class Main {
    public static void main(String[] args) throws Exception {


        AtomicInteger executedTasks = new AtomicInteger();
        AtomicInteger rejectedTask = new AtomicInteger();

        BlockingQueue<Runnable> queue = new ArrayBlockingQueue<>(1000);

        ThreadPoolExecutor pool = new ThreadPoolExecutor(
                100,                      // core threads
                500,                      // max threads
                1, TimeUnit.MINUTES,    // keepAlive
                queue
        );

        for (int i = 1; i <=100000; i++) {
            int id = i;

            DelayMyTask task = new DelayMyTask(
                    "task-" + id,
                    id,                     // delay value (ignored by ThreadPoolExecutor)
                    TimeUnit.SECONDS,
                    () -> {
                        System.out.println("Task " + id + " running on " + Thread.currentThread().getName());
                        try {
                            Thread.sleep(10);
                            executedTasks.addAndGet(1);
                        } catch (InterruptedException e) { }
                        System.out.println("Task " + id + " done");
                    }
            );

            try {
                pool.execute(task);
            }catch (Exception e) {
                rejectedTask.addAndGet(1);
                System.out.println("Exception In pool:  "+ e.getMessage());
            }

            System.out.println("Submitted " + task + " | queue size = " + queue.size());
        }

        pool.shutdown();
        pool.awaitTermination(1, TimeUnit.MINUTES);
        System.out.printf("Rejected Tasks: %s\n", rejectedTask.get());
        System.out.printf("Executed Tasks: %s", executedTasks.get());
    }
}
package intermediate.intermedidiate_prac;

import java.util.concurrent.*;

public class QueueTypeComparison {

    public void compareQueueTypes() {
        System.out.println("=== Thread Pool Queue Types Comparison ===\n");

        // Unbounded queue - tasks never rejected
        ThreadPoolExecutor unbounded = new ThreadPoolExecutor(
                2, 4, 60L, TimeUnit.SECONDS,
                new LinkedBlockingQueue<>() // Unbounded - can grow infinitely
        );

        // Bounded queue - tasks may be rejected when full
        ThreadPoolExecutor bounded = new ThreadPoolExecutor(
                2, 4, 60L, TimeUnit.SECONDS,
                new ArrayBlockingQueue<>(5), // Bounded to 5 tasks
                new ThreadPoolExecutor.CallerRunsPolicy() // Run on caller thread when rejected
        );

        // Direct handoff - no queuing, immediate thread creation or rejection
        ThreadPoolExecutor direct = new ThreadPoolExecutor(
                2, 10, 60L, TimeUnit.SECONDS,
                new SynchronousQueue<>(), // No storage - direct handoff
                new ThreadPoolExecutor.AbortPolicy() // Throw exception when rejected
        );

        // Priority queue - tasks executed by priority
        ThreadPoolExecutor priority = new ThreadPoolExecutor(
                2, 4, 60L, TimeUnit.SECONDS,
                new PriorityBlockingQueue<>()
        );

        // Test each configuration
        testConfiguration("UNBOUNDED", unbounded);
        waitBetweenTests();

        testConfiguration("BOUNDED", bounded);
        waitBetweenTests();

        testConfiguration("DIRECT", direct);
        waitBetweenTests();

        testPriorityConfiguration("PRIORITY", priority);
    }

    private void testConfiguration(String name, ThreadPoolExecutor executor) {
        System.out.println("=== Testing " + name + " Queue Configuration ===");
        System.out.println("Core: " + executor.getCorePoolSize() +
                ", Max: " + executor.getMaximumPoolSize() +
                ", Queue Type: " + executor.getQueue().getClass().getSimpleName());

        for (int i = 1; i <= 15; i++) {
            final int taskId = i;
            try {
                executor.submit(() -> {
                    System.out.println(name + " - Task " + taskId +
                            " STARTED on " + Thread.currentThread().getName() +
                            " [Pool size: " + executor.getPoolSize() +
                            ", Queue: " + executor.getQueue().size() + "]");
                    try {
                        Thread.sleep(1000); // Simulate work
                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                    }
                    System.out.println(name + " - Task " + taskId + " COMPLETED");
                });

                System.out.println(name + " - Task " + taskId + " SUBMITTED" +
                        " [Active: " + executor.getActiveCount() +
                        ", Pool: " + executor.getPoolSize() +
                        ", Queue: " + executor.getQueue().size() + "]");

            } catch (RejectedExecutionException e) {
                System.out.println(name + " - Task " + taskId + " REJECTED: " + e.getMessage());
            }

            // Small delay between submissions to see behavior
            try {
                Thread.sleep(200);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }

        // Wait for tasks to complete
        try {
            Thread.sleep(2000);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }

        System.out.println(name + " - Final stats: " +
                "Completed: " + executor.getCompletedTaskCount() +
                ", Pool size: " + executor.getPoolSize());

        executor.shutdown();
        try {
            executor.awaitTermination(5, TimeUnit.SECONDS);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }

        System.out.println(name + " test completed\n");
    }

    private void waitBetweenTests() {
        try {
            Thread.sleep(3000); // Wait between tests
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    private void testPriorityConfiguration(String name, ThreadPoolExecutor executor) {
        System.out.println("=== Testing " + name + " Queue Configuration ===");
        System.out.println("Core: " + executor.getCorePoolSize() +
                ", Max: " + executor.getMaximumPoolSize() +
                ", Queue Type: " + executor.getQueue().getClass().getSimpleName());

        // Submit tasks with different priorities
        for (int i = 1; i <= 15; i++) {
            int priority = (i % 5) + 1; // Priorities 1-5 (5 = highest)
            try {
                PriorityTask priorityTask = new PriorityTask(i, priority);
                executor.execute(priorityTask);
                System.out.println(name + " - Task " + i + " (Priority " + priority + ") SUBMITTED" +
                        " [Active: " + executor.getActiveCount() +
                        ", Pool: " + executor.getPoolSize() +
                        ", Queue: " + executor.getQueue().size() + "]");

            } catch (RejectedExecutionException e) {
                System.out.println(name + " - Task " + i + " REJECTED: " + e.getMessage());
            }

            try {
                Thread.sleep(200);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }

        // Wait for tasks to complete
        try {
            Thread.sleep(5000); // More time to see priority ordering
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }

        System.out.println(name + " - Final stats: " +
                "Completed: " + executor.getCompletedTaskCount() +
                ", Pool size: " + executor.getPoolSize());

        executor.shutdown();
        try {
            executor.awaitTermination(5, TimeUnit.SECONDS);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }

        System.out.println(name + " test completed\n");
    }
    static class PriorityTask implements Runnable, Comparable<PriorityTask> {
        private final int taskId;
        private final int priority;

        public PriorityTask(int taskId, int priority) {
            this.taskId = taskId;
            this.priority = priority;
        }

        @Override
        public void run() {
            System.out.println("PRIORITY - Task " + taskId + " (Priority: " + priority +
                    ") on " + Thread.currentThread().getName());
            try {
                Thread.sleep(500);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }

        @Override
        public int compareTo(PriorityTask other) {
            return Integer.compare(other.priority, this.priority); // Higher priority first
        }
    }

    public static void main(String[] args) {
        QueueTypeComparison comparison = new QueueTypeComparison();
        comparison.compareQueueTypes();

        System.out.println("\n=== Key Observations ===");
        System.out.println("1. UNBOUNDED: Never rejects, queue grows indefinitely");
        System.out.println("2. BOUNDED: Uses CallerRunsPolicy when queue full");
        System.out.println("3. DIRECT: Creates threads immediately or rejects");
        System.out.println("4. PRIORITY: Executes higher priority tasks first");
    }
}
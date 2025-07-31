package intermediate.intermedidiate_prac;

import java.util.concurrent.*;
import java.util.List;
import java.util.ArrayList;

public class FutureTimeoutExample {
    private final ExecutorService executor = Executors.newCachedThreadPool();

    public void demonstrateTimeoutAndCancellation() throws ExecutionException {
        System.out.println("=== Timeout and Cancellation Demo ===");

        // Create a long-running task
        Future<String> longTask = executor.submit(() -> {
            System.out.println("🚀 Long task started (will take 10 seconds)");
            for (int i = 0; i < 10; i++) {
                // Check if task was interrupted
                if (Thread.currentThread().isInterrupted()) {
                    System.out.println("⚠️  Task detected interruption at step " + i);
                    return "Task interrupted at step " + i;
                }

                try {
                    Thread.sleep(1000);
                    System.out.println("✅ Step " + (i + 1) + "/10 completed");
                } catch (InterruptedException e) {
                    System.out.println("💤 Task interrupted during sleep at step " + (i + 1));
                    Thread.currentThread().interrupt();
                    return "Task interrupted during sleep at step " + (i + 1);
                }
            }
            System.out.println("🎉 Long task completed all steps!");
            return "Task completed successfully";
        });

        try {
            // Try to get result with SHORT timeout (3 seconds)
            System.out.println("⏰ Waiting for result with 3-second timeout...");
            String result = longTask.get(3, TimeUnit.SECONDS);
            System.out.println("🎉 Result: " + result);

        } catch (TimeoutException e) {
            System.out.println("⏰ Task timed out after 3 seconds!");
            System.out.println("🛑 Attempting cancellation...");

            boolean cancelled = longTask.cancel(true); // true = interrupt if running
            System.out.println("Cancellation request sent: " + (cancelled ? "✅ Success" : "❌ Failed"));

            // Wait a bit and check final status
            try {
                System.out.println("⏳ Waiting 2 seconds to see cancellation effect...");
                Thread.sleep(2000);
                System.out.println("📊 Final Status:");
                System.out.println("   - Task cancelled: " + longTask.isCancelled());
                System.out.println("   - Task done: " + longTask.isDone());

                // Try to get result after cancellation (should throw CancellationException)
                try {
                    longTask.get();
                } catch (CancellationException ce) {
                    System.out.println("   - ✅ Confirmed: Task was cancelled");
                }

            } catch (InterruptedException ie) {
                Thread.currentThread().interrupt();
            }

        } catch (InterruptedException e) {
            System.err.println("❌ Main thread was interrupted: " + e.getMessage());
            Thread.currentThread().interrupt();
        } catch (ExecutionException e) {
            System.err.println("❌ Task execution error: " + e.getMessage());
        }

        System.out.println();
    }

    public void demonstrateNonInterruptibleTask() {
        System.out.println("=== Non-Interruptible Task Demo ===");

        Future<String> stubborn = executor.submit(() -> {
            System.out.println("😤 Stubborn task started (ignores interruption)");
            for (int i = 0; i < 5; i++) {
                // This task ignores interruption status
                // In real world: CPU-bound computation, busy waiting, etc.
                long startTime = System.currentTimeMillis();
                while (System.currentTimeMillis() - startTime < 1000) {
                    // Busy wait for 1 second (simulates CPU-bound work)
                    Math.sin(Math.random()); // Some CPU work
                }
                System.out.println("💪 Stubborn step " + (i + 1) + "/5 completed (ignoring interrupts)");
            }
            return "Stubborn task finished";
        });

        try {
            System.out.println("⏰ Waiting with 2-second timeout...");
            String result = stubborn.get(2, TimeUnit.SECONDS);
            System.out.println("Result: " + result);
        } catch (TimeoutException e) {
            System.out.println("⏰ Stubborn task timed out!");
            System.out.println("🛑 Attempting cancellation (won't work!)...");

            boolean cancelled = stubborn.cancel(true);
            System.out.println("Cancellation request: " + (cancelled ? "Sent" : "Failed"));

            try {
                Thread.sleep(4000); // Wait to see if it actually stops
                System.out.println("📊 After 4 seconds:");
                System.out.println("   - Task cancelled: " + stubborn.isCancelled());
                System.out.println("   - Task done: " + stubborn.isDone());
            } catch (InterruptedException ie) {
                Thread.currentThread().interrupt();
            }
        } catch (InterruptedException | ExecutionException e) {
            System.err.println("Error: " + e.getMessage());
        }

        System.out.println();
    }

    public void demonstrateMultipleTasksWithTimeout() {
        System.out.println("=== Multiple Tasks with Timeout Demo ===");

        List<Future<String>> tasks = new ArrayList<>();

        // Submit multiple tasks with different durations
        for (int i = 0; i < 4; i++) {
            final int taskId = i;
            final int duration = (i + 1) * 1000; // 1s, 2s, 3s, 4s

            Future<String> task = executor.submit(() -> {
                try {
                    System.out.println("🏃 Task " + taskId + " started (" + duration + "ms)");
                    Thread.sleep(duration);
                    System.out.println("✅ Task " + taskId + " completed");
                    return "Task " + taskId + " result";
                } catch (InterruptedException e) {
                    System.out.println("⚠️  Task " + taskId + " interrupted");
                    Thread.currentThread().interrupt();
                    return "Task " + taskId + " interrupted";
                }
            });
            tasks.add(task);
        }

        // Try to get all results with 2.5 second timeout
        System.out.println("⏰ Getting results with 2.5-second timeout each...");

        for (int i = 0; i < tasks.size(); i++) {
            Future<String> task = tasks.get(i);
            try {
                String result = task.get(2500, TimeUnit.MILLISECONDS);
                System.out.println("✅ Got result: " + result);
            } catch (TimeoutException e) {
                System.out.println("⏰ Task " + i + " timed out, cancelling...");
                task.cancel(true);
            } catch (InterruptedException | ExecutionException e) {
                System.err.println("❌ Task " + i + " error: " + e.getMessage());
            }
        }

        System.out.println();
    }

    public void shutdown() {
        System.out.println("🔄 Shutting down executor...");
        executor.shutdown();
        try {
            if (!executor.awaitTermination(3, TimeUnit.SECONDS)) {
                System.out.println("⚠️  Some tasks still running, forcing shutdown...");
                executor.shutdownNow();

                if (!executor.awaitTermination(2, TimeUnit.SECONDS)) {
                    System.err.println("❌ Executor did not terminate gracefully");
                } else {
                    System.out.println("✅ Forced shutdown successful");
                }
            } else {
                System.out.println("✅ Graceful shutdown successful");
            }
        } catch (InterruptedException e) {
            System.err.println("❌ Shutdown interrupted");
            executor.shutdownNow();
            Thread.currentThread().interrupt();
        }
    }

    public static void main(String[] args) {
        FutureTimeoutExample example = new FutureTimeoutExample();

        System.out.println("🚀 Starting Future Timeout Demonstrations\n");

        try {
            example.demonstrateTimeoutAndCancellation();
            example.demonstrateNonInterruptibleTask();
            example.demonstrateMultipleTasksWithTimeout();

        } catch (Exception e) {
            System.err.println("❌ Unexpected error: " + e.getMessage());
        } finally {
            example.shutdown();
        }

        System.out.println("🏁 All demonstrations completed!");
    }
}
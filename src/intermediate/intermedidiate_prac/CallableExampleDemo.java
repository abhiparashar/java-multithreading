package intermediate.intermedidiate_prac;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.*;

public class CallableExampleDemo {
    ExecutorService executor = Executors.newFixedThreadPool(4);

    public void basicCallableExample() throws ExecutionException, InterruptedException, TimeoutException {
        System.out.println("=== Basic Callable Example ===");
        Callable<Integer> callable = () -> {
            try {
                Thread.sleep(2000);
                return 42 * 1337;
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                return -1;
            }
        };

        Future<Integer> future = executor.submit(callable);
        try {
            if (!future.isDone()) {
                System.out.println("📋 Computation in progress...");
                System.out.println("🔄 Doing other work while waiting...");
                // Simulate doing other work
                for (int i = 1; i <= 3; i++) {
                    System.out.println("   Other work step " + i);
                    Thread.sleep(600);
                }
            }
            // Blocking call with timeout
            System.out.println("🕐 Now getting result (with 5 second timeout)...");
            Integer result = future.get(5, TimeUnit.SECONDS);
            System.out.println("🎉 Computation result: " + result);

        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        } catch (ExecutionException e) {
            System.err.println("❌ Computation failed: " + e.getCause().getMessage());
        } catch (TimeoutException e) {
            System.err.println("⏰ Computation timed out!");
            future.cancel(true);
        }
    }

    public void multipleCallablesExample() throws ExecutionException, InterruptedException {
        System.out.println("=== Multiple Callable Example ===");
        List<Callable<String>> callableList = new ArrayList<>();

        // Create multiple computational tasks
        for (int i = 0; i < 5; i++) {
            final int taskId = i;
            callableList.add(() -> {
                try {
                    // FIXED: Correct sleep calculation
                    Thread.sleep(1000 + (taskId * 500));
                    if (taskId == 3) {
                        throw new RuntimeException("Task " + taskId + " failed!");
                    }
                    return "Task " + taskId + " completed successfully";
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    throw new RuntimeException("Task interrupted", e);
                }
            });
        }

        // Submit all tasks
        List<Future<String>> futureList = new ArrayList<>();
        for (int i = 0; i < 5; i++) {
            futureList.add(executor.submit(callableList.get(i)));
        }

        // Process results as they become available
        int successCount = 0;
        int failCount = 0;

        for (int i = 0; i < futureList.size(); i++) {
            Future<String> future = futureList.get(i);
            try {
                String result = future.get();
                System.out.println("Success: " + result);
                successCount++;
            } catch (ExecutionException e) {
                // FIXED: Don't interrupt on ExecutionException
                System.err.println("Task " + i + " failed: " + e.getCause().getMessage());
                failCount++;
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                System.err.println("Interrupted while waiting for task " + i);
                break; // Exit loop if interrupted
            }
        }
        System.out.println("\n📊 Summary: " + successCount + " succeeded, " + failCount + " failed");
        // REMOVED: executor.shutdown() - let main method handle it
    }

    public void demonstrateWithTimeout() {
        System.out.println("=== Timeout Demonstration ===");

        Callable<String> slowTask = () -> {
            try {
                System.out.println("🐌 Starting very slow task (5 seconds)...");
                Thread.sleep(5000);
                return "Slow task completed";
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                throw new RuntimeException("Task interrupted", e);
            }
        };

        Future<String> future = executor.submit(slowTask);

        try {
            // Try to get result with short timeout
            System.out.println("⏰ Trying to get result with 2 second timeout...");
            String result = future.get(2, TimeUnit.SECONDS);
            System.out.println("Result: " + result);
        } catch (TimeoutException e) {
            System.out.println("⏰ Task timed out! Cancelling...");
            boolean cancelled = future.cancel(true);
            System.out.println("Cancellation " + (cancelled ? "successful" : "failed"));
        } catch (InterruptedException | ExecutionException e) {
            System.err.println("Error: " + e.getMessage());
        }

        System.out.println();
    }

    public void shutdown() {
        System.out.println("🔄 Shutting down executor...");
        executor.shutdown();
        try {
            // FIXED: Logic was inverted
            if (!executor.awaitTermination(5, TimeUnit.SECONDS)) {
                System.out.println("⚠️  Forcing shutdown...");
                executor.shutdownNow();
                if (!executor.awaitTermination(2, TimeUnit.SECONDS)) {
                    System.err.println("❌ Executor did not terminate gracefully");
                }
            }
            System.out.println("✅ Executor shutdown complete");
        } catch (InterruptedException e) {
            System.err.println("❌ Shutdown interrupted");
            executor.shutdownNow(); // FIXED: Use shutdownNow(), not shutdown()
            Thread.currentThread().interrupt();
        }
    }

    public static void main(String[] args) {
        CallableExampleDemo example = new CallableExampleDemo();
        try {
            // Run all examples
            example.basicCallableExample();
            example.multipleCallablesExample();
            example.demonstrateWithTimeout();

        } catch (InterruptedException | ExecutionException | TimeoutException e) {
            System.err.println("Main thread error: " + e.getMessage());
            Thread.currentThread().interrupt();
        } finally {
            // Always shutdown the executor
            example.shutdown();
        }

        System.out.println("🏁 Program finished");
    }
}
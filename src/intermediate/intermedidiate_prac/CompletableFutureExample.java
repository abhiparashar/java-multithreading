package intermediate.intermedidiate_prac;

import java.util.Arrays;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

public class CompletableFutureExample {
    public void basicCompletableFuture(){
        System.out.println("=== Basic CompletableFuture Example ===");

        // Create and complete a future manually
        CompletableFuture future = new CompletableFuture();

        // Complete it from another thread
        CompletableFuture.runAsync(()->{
            try{
                System.out.println("🔄 Background task starting...");
                Thread.sleep(2000);
                future.complete("Hello from CompletableFuture!");
                System.out.println("✅ Background task completed future manually");
            }catch (InterruptedException e){
                System.err.println("❌ Background task interrupted");
                future.completeExceptionally(e);
            }
        });

        // Do other work while waiting
        System.out.println("📋 Doing other work while future completes...");
        for (int i = 1; i <= 3; i++) {
            System.out.println("   Work step " + i);
            try { Thread.sleep(600); } catch (InterruptedException e) {}
        }

        // Get result
        try {
            System.out.println("🕐 Getting result...");
            String result = (String) future.get(5, TimeUnit.SECONDS);
            System.out.println("🎉 Result: " + result);
        }catch (InterruptedException | ExecutionException | TimeoutException e){
            System.err.println("❌ Future failed: " + e.getMessage());
        }
    };

    public void chainedOperations(){
        System.out.println("=== Chained Operations Example ===");
        CompletableFuture<Integer>future = CompletableFuture
                .supplyAsync(()->{
                    System.out.println("🚀 Step 1: Initial computation");
                    try { Thread.sleep(500); } catch (InterruptedException e) {}
                    return 10;
                })
                .thenApply(result->{
                    System.out.println("⚙️  Step 2: Transform " + result + " → " + (result * 2));
                    return result * 2;
                })
                .thenApply(result -> {
                    System.out.println("⚙️  Step 3: Add to " + result + " → " + (result + 5));
                    return result + 5;
                })
                .thenCompose(result -> {
                    System.out.println("🔗 Step 4: Compose with another async operation (" + result + ")");
                    return CompletableFuture.supplyAsync(() -> {
                        try { Thread.sleep(300); } catch (InterruptedException e) {}
                        int finalResult = result * 10;
                        System.out.println("⚙️  Step 4 completed: " + result + " → " + finalResult);
                        return finalResult;
                    });
                });

        try {
            System.out.println("⏳ Waiting for chain to complete...");
            Integer finalResult = future.get();
            System.out.println("🎉 Final result: " + finalResult);
            System.out.println("📊 Calculation: ((10 * 2) + 5) * 10 = " + finalResult);
        } catch (InterruptedException | ExecutionException e) {
            System.err.println("❌ Chain failed: " + e.getMessage());
        }
    }

    public void combiningFutures(){
        System.out.println("=== Combining Futures Example ===");
        CompletableFuture<String> future1 = CompletableFuture.supplyAsync(() -> {
            try {
                System.out.println("🌟 Task 1 started (1 second)");
                Thread.sleep(1000);
            } catch (InterruptedException e) {}
            System.out.println("✅ Task 1 completed");
            return "Hello";
        });
        CompletableFuture<String> future2 = CompletableFuture.supplyAsync(() -> {
            try {
                System.out.println("🌟 Task 2 started (2 seconds)");
                Thread.sleep(2000);
            } catch (InterruptedException e) {}
            System.out.println("✅ Task 2 completed");
            return "World";
        });

        // Combine two futures - runs in parallel!
        System.out.println("🔄 Starting both tasks in parallel...");
        CompletableFuture<String> combined = future1.thenCombine(future2,
                (result1, result2) -> {
                    String finalResult = result1 + " " + result2 + "!";
                    System.out.println("🔗 Combining: '" + result1 + "' + '" + result2 + "' = '" + finalResult + "'");
                    return finalResult;
                });

        // Handle both success and failure
        combined.whenComplete((result, throwable) -> {
            if (throwable != null) {
                System.err.println("❌ Combined operation failed: " + throwable.getMessage());
            } else {
                System.out.println("🎉 Combined result: " + result);
            }
        });

        try {
            System.out.println("⏳ Waiting for combination...");
            combined.get(); // Wait for completion
        } catch (InterruptedException | ExecutionException e) {
            System.err.println("❌ Error: " + e.getMessage());
        }
    }

    public void exceptionHandling() {
        System.out.println("=== Exception Handling Example ===");

        CompletableFuture<String> future = CompletableFuture
                .supplyAsync(() -> {
                    System.out.println("🎲 Rolling the dice...");
                    double random = Math.random();
                    System.out.println("   Random value: " + String.format("%.2f", random));
                    if (random > 0.5) {
                        System.out.println("💥 Simulating failure!");
                        throw new RuntimeException("Random failure! (value > 0.5)");
                    }
                    System.out.println("✅ Success! (value <= 0.5)");
                    return "Success!";
                })
                .exceptionally(throwable -> {
                    System.err.println("🛡️  Handling exception: " + throwable.getMessage());
                    return "Recovered from failure";
                })
                .thenApply(result -> {
                    System.out.println("⚙️  Processing: " + result);
                    return result.toUpperCase();
                });

        try {
            String result = future.get();
            System.out.println("🎉 Final result: " + result);
        } catch (InterruptedException | ExecutionException e) {
            System.err.println("❌ Unexpected error: " + e.getMessage());
        }

        System.out.println();
    }

    public void allOfExample() {
        System.out.println("=== AllOf Example (Wait for Multiple Tasks) ===");

        // Create multiple tasks with different durations
        CompletableFuture<String> task1 = CompletableFuture.supplyAsync(() -> {
            try {
                System.out.println("📋 Task 1 starting (1s)");
                Thread.sleep(1000);
            } catch (InterruptedException e) {}
            System.out.println("✅ Task 1 done");
            return "Result1";
        });

        CompletableFuture<String> task2 = CompletableFuture.supplyAsync(() -> {
            try {
                System.out.println("📋 Task 2 starting (1.5s)");
                Thread.sleep(1500);
            } catch (InterruptedException e) {}
            System.out.println("✅ Task 2 done");
            return "Result2";
        });

        CompletableFuture<String> task3 = CompletableFuture.supplyAsync(() -> {
            try {
                System.out.println("📋 Task 3 starting (0.8s)");
                Thread.sleep(800);
            } catch (InterruptedException e) {}
            System.out.println("✅ Task 3 done");
            return "Result3";
        });

        // Wait for ALL tasks to complete
        CompletableFuture<Void> allTasks = CompletableFuture.allOf(task1, task2, task3);

        allTasks.thenRun(() -> {
            System.out.println("🎉 All tasks completed!");
            try {
                // Now we can safely get all results
                List<String> results = Arrays.asList(
                        task1.get(), task2.get(), task3.get()
                );
                System.out.println("📊 All results: " + results);
            } catch (InterruptedException | ExecutionException e) {
                System.err.println("❌ Error getting results: " + e.getMessage());
            }
        });

        try {
            System.out.println("⏳ Waiting for all tasks...");
            allTasks.get();
        } catch (InterruptedException | ExecutionException e) {
            System.err.println("❌ Error: " + e.getMessage());
        }

        System.out.println();
    }

    public void anyOfExample() {
        System.out.println("=== AnyOf Example (First to Complete Wins) ===");

        CompletableFuture<String> slowTask = CompletableFuture.supplyAsync(() -> {
            try {
                System.out.println("🐌 Slow task starting (3s)");
                Thread.sleep(3000);
            } catch (InterruptedException e) {}
            System.out.println("✅ Slow task done");
            return "Slow result";
        });

        CompletableFuture<String> mediumTask = CompletableFuture.supplyAsync(() -> {
            try {
                System.out.println("🚶 Medium task starting (2s)");
                Thread.sleep(2000);
            } catch (InterruptedException e) {}
            System.out.println("✅ Medium task done");
            return "Medium result";
        });

        CompletableFuture<String> fastTask = CompletableFuture.supplyAsync(() -> {
            try {
                System.out.println("🏃 Fast task starting (1s)");
                Thread.sleep(1000);
            } catch (InterruptedException e) {}
            System.out.println("✅ Fast task done");
            return "Fast result";
        });

        // Get result from whichever completes first
        CompletableFuture<Object> firstResult = CompletableFuture.anyOf(slowTask, mediumTask, fastTask);

        try {
            System.out.println("⏳ Waiting for first task to complete...");
            Object result = firstResult.get();
            System.out.println("🏆 First result: " + result);
        } catch (InterruptedException | ExecutionException e) {
            System.err.println("❌ Error: " + e.getMessage());
        }

        System.out.println();
    }

    public static void main(String[] args) {
        CompletableFutureExample example = new CompletableFutureExample();

        System.out.println("🚀 Starting CompletableFuture Demonstrations\n");

        try {
            example.basicCompletableFuture();
            example.chainedOperations();
            example.combiningFutures();
            example.exceptionHandling();
            example.allOfExample();
            example.anyOfExample();

        } catch (Exception e) {
            System.err.println("❌ Unexpected error in main: " + e.getMessage());
        }

        System.out.println("🏁 All demonstrations completed!");

        // Give background threads time to finish before program exits
        try {
            Thread.sleep(500);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }
}

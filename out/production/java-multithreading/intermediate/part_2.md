# Java Concurrency Part 2: Intermediate Level - Thread Pools

## Table of Contents
1. [ExecutorService and Executors](#executorservice-and-executors)
2. [ThreadPoolExecutor Configuration](#threadpoolexecutor-configuration)
3. [ScheduledExecutorService](#scheduledexecutorservice)
4. [Callable and Future](#callable-and-future)

---

## ExecutorService and Executors

### Theory

**Thread pools** manage a collection of worker threads, reusing them to execute multiple tasks. This approach is more efficient than creating new threads for each task.

**Key Benefits:**
- **Resource management**: Controls number of concurrent threads
- **Performance**: Eliminates thread creation overhead
- **Lifecycle management**: Centralized thread lifecycle control
- **Task queuing**: Handles task overflow gracefully

**ExecutorService** is the main interface for thread pool management, while **Executors** provides factory methods for common thread pool types.

### Code Examples

#### Basic ExecutorService Usage
```java
import java.util.concurrent.*;
import java.util.List;
import java.util.ArrayList;

public class ExecutorServiceBasics {
    
    public void demonstrateBasicUsage() {
        // Create different types of thread pools
        ExecutorService fixedPool = Executors.newFixedThreadPool(4);
        ExecutorService cachedPool = Executors.newCachedThreadPool();
        ExecutorService singlePool = Executors.newSingleThreadExecutor();
        
        try {
            // Submit tasks
            for (int i = 0; i < 10; i++) {
                final int taskId = i;
                fixedPool.submit(() -> {
                    System.out.println("Task " + taskId + " executed by " + 
                                     Thread.currentThread().getName());
                    try {
                        Thread.sleep(1000);
                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                    }
                });
            }
            
        } finally {
            // Always shutdown executors
            shutdownExecutor(fixedPool);
            shutdownExecutor(cachedPool);
            shutdownExecutor(singlePool);
        }
    }
    
    private void shutdownExecutor(ExecutorService executor) {
        executor.shutdown();
        try {
            if (!executor.awaitTermination(60, TimeUnit.SECONDS)) {
                executor.shutdownNow();
            }
        } catch (InterruptedException e) {
            executor.shutdownNow();
            Thread.currentThread().interrupt();
        }
    }
}
```

#### Different Thread Pool Types Comparison
```java
public class ThreadPoolComparison {
    
    public void compareThreadPools() throws InterruptedException {
        System.out.println("=== Fixed Thread Pool ===");
        testThreadPool(Executors.newFixedThreadPool(2), "Fixed");
        
        System.out.println("\n=== Cached Thread Pool ===");
        testThreadPool(Executors.newCachedThreadPool(), "Cached");
        
        System.out.println("\n=== Single Thread Pool ===");
        testThreadPool(Executors.newSingleThreadExecutor(), "Single");
        
        System.out.println("\n=== Work Stealing Pool ===");
        testThreadPool(Executors.newWorkStealingPool(), "WorkStealing");
    }
    
    private void testThreadPool(ExecutorService executor, String type) 
            throws InterruptedException {
        long startTime = System.currentTimeMillis();
        CountDownLatch latch = new CountDownLatch(6);
        
        for (int i = 0; i < 6; i++) {
            final int taskId = i;
            executor.submit(() -> {
                System.out.println(type + " - Task " + taskId + " on " + 
                                 Thread.currentThread().getName());
                try {
                    Thread.sleep(500);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                } finally {
                    latch.countDown();
                }
            });
        }
        
        latch.await();
        long endTime = System.currentTimeMillis();
        System.out.println(type + " execution time: " + (endTime - startTime) + "ms");
        
        executor.shutdown();
    }
}
```

#### Advanced Task Management
```java
import java.util.concurrent.*;
import java.util.List;
import java.util.ArrayList;

public class AdvancedTaskManagement {
    private final ExecutorService executor = Executors.newFixedThreadPool(4);
    
    public void submitMultipleTasks() throws InterruptedException, ExecutionException {
        List<Future<String>> futures = new ArrayList<>();
        
        // Submit multiple tasks
        for (int i = 0; i < 5; i++) {
            final int taskId = i;
            Future<String> future = executor.submit(() -> {
                Thread.sleep(1000 + (taskId * 500));
                return "Task " + taskId + " completed";
            });
            futures.add(future);
        }
        
        // Process results as they complete
        for (Future<String> future : futures) {
            try {
                String result = future.get(3, TimeUnit.SECONDS);
                System.out.println("Result: " + result);
            } catch (TimeoutException e) {
                System.out.println("Task timed out");
                future.cancel(true); // Interrupt if running
            }
        }
        
        executor.shutdown();
    }
    
    public void invokeAllExample() throws InterruptedException {
        List<Callable<String>> tasks = new ArrayList<>();
        
        for (int i = 0; i < 3; i++) {
            final int taskId = i;
            tasks.add(() -> {
                Thread.sleep(1000);
                return "Batch task " + taskId + " completed";
            });
        }
        
        // Execute all tasks and wait for completion
        List<Future<String>> results = executor.invokeAll(tasks, 5, TimeUnit.SECONDS);
        
        for (Future<String> result : results) {
            try {
                System.out.println(result.get());
            } catch (ExecutionException e) {
                System.out.println("Task failed: " + e.getCause());
            }
        }
        
        executor.shutdown();
    }
    
    public void invokeAnyExample() throws InterruptedException, ExecutionException {
        List<Callable<String>> tasks = new ArrayList<>();
        
        tasks.add(() -> {
            Thread.sleep(2000);
            return "Slow task completed";
        });
        
        tasks.add(() -> {
            Thread.sleep(500);
            return "Fast task completed";
        });
        
        tasks.add(() -> {
            Thread.sleep(1000);
            return "Medium task completed";
        });
        
        // Execute and return first completed result
        String result = executor.invokeAny(tasks, 3, TimeUnit.SECONDS);
        System.out.println("First completed: " + result);
        
        executor.shutdown();
    }
}
```

---

## ThreadPoolExecutor Configuration

### Theory

**ThreadPoolExecutor** is the core implementation of ExecutorService, providing fine-grained control over thread pool behavior.

**Key Parameters:**
- **corePoolSize**: Minimum number of threads to keep alive
- **maximumPoolSize**: Maximum number of threads allowed
- **keepAliveTime**: Time to keep excess threads alive
- **workQueue**: Queue to hold tasks before execution
- **threadFactory**: Factory for creating new threads
- **rejectedExecutionHandler**: Handler for rejected tasks

### Code Examples

#### Custom ThreadPoolExecutor Configuration
```java
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;

public class CustomThreadPoolExecutor {
    
    public ThreadPoolExecutor createCustomPool() {
        return new ThreadPoolExecutor(
            2,                              // corePoolSize
            4,                              // maximumPoolSize  
            60L,                            // keepAliveTime
            TimeUnit.SECONDS,               // time unit
            new LinkedBlockingQueue<>(10),  // workQueue
            new CustomThreadFactory("CustomPool"), // threadFactory
            new CustomRejectionHandler()    // rejectionHandler
        );
    }
    
    static class CustomThreadFactory implements ThreadFactory {
        private final AtomicInteger threadNumber = new AtomicInteger(1);
        private final String namePrefix;
        
        CustomThreadFactory(String namePrefix) {
            this.namePrefix = namePrefix;
        }
        
        @Override
        public Thread newThread(Runnable r) {
            Thread thread = new Thread(r, namePrefix + "-thread-" + 
                                     threadNumber.getAndIncrement());
            thread.setDaemon(false);
            thread.setPriority(Thread.NORM_PRIORITY);
            
            // Custom uncaught exception handler
            thread.setUncaughtExceptionHandler((t, e) -> {
                System.err.println("Thread " + t.getName() + 
                                 " threw exception: " + e.getMessage());
                e.printStackTrace();
            });
            
            return thread;
        }
    }
    
    static class CustomRejectionHandler implements RejectedExecutionHandler {
        @Override
        public void rejectedExecution(Runnable r, ThreadPoolExecutor executor) {
            System.err.println("Task rejected: " + r.toString());
            
            // Custom rejection strategies
            if (!executor.isShutdown()) {
                // Try to add to queue with timeout
                try {
                    if (executor.getQueue().offer(r, 1, TimeUnit.SECONDS)) {
                        System.out.println("Task queued after rejection");
                    } else {
                        System.err.println("Task completely rejected");
                        // Could log to external system, send to DLQ, etc.
                    }
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
            }
        }
    }
}
```

#### Thread Pool Monitoring
```java
public class ThreadPoolMonitor {
    private final ThreadPoolExecutor executor;
    private final ScheduledExecutorService monitor;
    
    public ThreadPoolMonitor() {
        this.executor = new ThreadPoolExecutor(
            2, 10, 60L, TimeUnit.SECONDS,
            new ArrayBlockingQueue<>(20)
        );
        
        this.monitor = Executors.newScheduledThreadPool(1);
        startMonitoring();
    }
    
    private void startMonitoring() {
        monitor.scheduleAtFixedRate(() -> {
            System.out.println("=== Thread Pool Stats ===");
            System.out.println("Active threads: " + executor.getActiveCount());
            System.out.println("Core pool size: " + executor.getCorePoolSize());
            System.out.println("Pool size: " + executor.getPoolSize());
            System.out.println("Largest pool size: " + executor.getLargestPoolSize());
            System.out.println("Task count: " + executor.getTaskCount());
            System.out.println("Completed tasks: " + executor.getCompletedTaskCount());
            System.out.println("Queue size: " + executor.getQueue().size());
            System.out.println("Is shutdown: " + executor.isShutdown());
            System.out.println("Is terminated: " + executor.isTerminated());
            System.out.println("========================");
        }, 0, 5, TimeUnit.SECONDS);
    }
    
    public void submitTask(Runnable task) {
        executor.submit(task);
    }
    
    public void shutdown() {
        executor.shutdown();
        monitor.shutdown();
    }
}
```

#### Different Queue Types Impact
```java
public class QueueTypeComparison {
    
    public void compareQueueTypes() {
        // Unbounded queue - tasks never rejected
        ThreadPoolExecutor unbounded = new ThreadPoolExecutor(
            2, 4, 60L, TimeUnit.SECONDS,
            new LinkedBlockingQueue<>() // Unbounded
        );
        
        // Bounded queue - tasks may be rejected
        ThreadPoolExecutor bounded = new ThreadPoolExecutor(
            2, 4, 60L, TimeUnit.SECONDS,
            new ArrayBlockingQueue<>(5), // Bounded to 5
            new ThreadPoolExecutor.CallerRunsPolicy()
        );
        
        // Direct handoff - no queuing
        ThreadPoolExecutor direct = new ThreadPoolExecutor(
            2, 10, 60L, TimeUnit.SECONDS,
            new SynchronousQueue<>(), // Direct handoff
            new ThreadPoolExecutor.AbortPolicy()
        );
        
        // Priority queue
        ThreadPoolExecutor priority = new ThreadPoolExecutor(
            2, 4, 60L, TimeUnit.SECONDS,
            new PriorityBlockingQueue<>()
        );
        
        // Test each configuration
        testConfiguration("Unbounded", unbounded);
        testConfiguration("Bounded", bounded);
        testConfiguration("Direct", direct);
        testConfiguration("Priority", priority);
    }
    
    private void testConfiguration(String name, ThreadPoolExecutor executor) {
        System.out.println("Testing " + name + " configuration...");
        
        for (int i = 0; i < 15; i++) {
            final int taskId = i;
                            try {
                    executor.submit(() -> {
                        System.out.println(name + " - Task " + taskId + 
                                         " on " + Thread.currentThread().getName());
                        try {
                            Thread.sleep(500);
                        } catch (InterruptedException e) {
                            Thread.currentThread().interrupt();
                        }
                    });
                } catch (RejectedExecutionException e) {
                    System.out.println(name + " - Task " + taskId + " rejected");
                }
            }
            
            try {
                Thread.sleep(100);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }
        
        executor.shutdown();
        System.out.println(name + " test completed\n");
    }
}
```

---

## ScheduledExecutorService

### Theory

**ScheduledExecutorService** extends ExecutorService to support delayed and periodic task execution.

**Key Methods:**
- **schedule()**: Execute task after delay
- **scheduleAtFixedRate()**: Execute repeatedly with fixed rate
- **scheduleWithFixedDelay()**: Execute repeatedly with fixed delay between completions

**Use Cases:**
- Periodic maintenance tasks
- Delayed execution
- Timeout mechanisms
- Heartbeat systems

### Code Examples

#### Basic Scheduled Tasks
```java
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.concurrent.*;

public class ScheduledTasksExample {
    private final ScheduledExecutorService scheduler = 
        Executors.newScheduledThreadPool(3);
    
    public void demonstrateScheduledTasks() {
        // One-time delayed task
        ScheduledFuture<?> delayedTask = scheduler.schedule(() -> {
            System.out.println("Delayed task executed at " + getCurrentTime());
        }, 2, TimeUnit.SECONDS);
        
        // Fixed rate execution - starts every 1 second regardless of execution time
        ScheduledFuture<?> fixedRateTask = scheduler.scheduleAtFixedRate(() -> {
            System.out.println("Fixed rate task at " + getCurrentTime());
            try {
                Thread.sleep(1500); // Takes longer than interval
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }, 1, 1, TimeUnit.SECONDS);
        
        // Fixed delay execution - waits 1 second after completion
        ScheduledFuture<?> fixedDelayTask = scheduler.scheduleWithFixedDelay(() -> {
            System.out.println("Fixed delay task at " + getCurrentTime());
            try {
                Thread.sleep(500);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }, 1, 1, TimeUnit.SECONDS);
        
        // Cancel tasks after 10 seconds
        scheduler.schedule(() -> {
            fixedRateTask.cancel(false);
            fixedDelayTask.cancel(false);
            System.out.println("Cancelled periodic tasks at " + getCurrentTime());
        }, 10, TimeUnit.SECONDS);
    }
    
    private String getCurrentTime() {
        return LocalDateTime.now().format(DateTimeFormatter.ofPattern("HH:mm:ss.SSS"));
    }
    
    public void shutdown() {
        scheduler.shutdown();
    }
}
```

#### Scheduled Task with Return Values
```java
public class ScheduledCallableExample {
    private final ScheduledExecutorService scheduler = 
        Executors.newScheduledThreadPool(2);
    
    public void scheduleCallableTasks() {
        // Schedule a Callable that returns a value
        ScheduledFuture<String> future = scheduler.schedule(() -> {
            Thread.sleep(1000);
            return "Scheduled computation result: " + Math.random();
        }, 2, TimeUnit.SECONDS);
        
        try {
            String result = future.get(5, TimeUnit.SECONDS);
            System.out.println("Result: " + result);
        } catch (InterruptedException | ExecutionException | TimeoutException e) {
            System.err.println("Task failed: " + e.getMessage());
        }
        
        scheduler.shutdown();
    }
}
```

#### Real-world Scheduled Task System
```java
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;

public class ScheduledTaskManager {
    private final ScheduledExecutorService scheduler = 
        Executors.newScheduledThreadPool(5);
    private final AtomicBoolean running = new AtomicBoolean(true);
    
    public void startSystemTasks() {
        startHealthCheck();
        startMetricsCollection();
        startCleanupTask();
        startHeartbeat();
    }
    
    private void startHealthCheck() {
        scheduler.scheduleWithFixedDelay(() -> {
            if (!running.get()) return;
            
            System.out.println("=== Health Check ===");
            // Check system resources
            Runtime runtime = Runtime.getRuntime();
            long totalMemory = runtime.totalMemory();
            long freeMemory = runtime.freeMemory();
            long usedMemory = totalMemory - freeMemory;
            
            System.out.printf("Memory: %.2f%% used%n", 
                            (double) usedMemory / totalMemory * 100);
            
            if (usedMemory > totalMemory * 0.8) {
                System.out.println("WARNING: High memory usage detected!");
                // Could trigger alerts, garbage collection, etc.
            }
        }, 0, 30, TimeUnit.SECONDS);
    }
    
    private void startMetricsCollection() {
        AtomicLong requestCount = new AtomicLong(0);
        
        scheduler.scheduleAtFixedRate(() -> {
            if (!running.get()) return;
            
            long count = requestCount.incrementAndGet();
            System.out.println("Metrics: Processed " + count + " requests");
            
            // Simulate varying load
            if (Math.random() > 0.7) {
                System.out.println("High load detected - scaling up");
            }
        }, 10, 10, TimeUnit.SECONDS);
    }
    
    private void startCleanupTask() {
        scheduler.scheduleWithFixedDelay(() -> {
            if (!running.get()) return;
            
            System.out.println("Running cleanup tasks...");
            // Clean temporary files, expired cache entries, etc.
            try {
                Thread.sleep(2000); // Simulate cleanup work
                System.out.println("Cleanup completed");
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }, 1, 60, TimeUnit.SECONDS); // Run every minute after completion
    }
    
    private void startHeartbeat() {
        scheduler.scheduleAtFixedRate(() -> {
            if (!running.get()) return;
            
            System.out.println("♥ Heartbeat - " + getCurrentTime());
            // Send heartbeat to monitoring system
        }, 5, 5, TimeUnit.SECONDS);
    }
    
    public void gracefulShutdown() {
        System.out.println("Initiating graceful shutdown...");
        running.set(false);
        
        scheduler.shutdown();
        try {
            if (!scheduler.awaitTermination(30, TimeUnit.SECONDS)) {
                System.out.println("Force shutdown initiated");
                scheduler.shutdownNow();
            }
        } catch (InterruptedException e) {
            scheduler.shutdownNow();
            Thread.currentThread().interrupt();
        }
        
        System.out.println("Shutdown completed");
    }
    
    private String getCurrentTime() {
        return LocalDateTime.now().format(DateTimeFormatter.ofPattern("HH:mm:ss"));
    }
}
```

---

## Callable and Future

### Theory

**Callable** is similar to Runnable but can return a value and throw checked exceptions. **Future** represents the result of an asynchronous computation.

**Key Concepts:**
- **Callable<V>**: Task that returns a result of type V
- **Future<V>**: Handle to access result asynchronously
- **get()**: Blocking call to retrieve result
- **cancel()**: Attempt to cancel task execution
- **CompletableFuture**: Enhanced Future with composition capabilities

### Code Examples

#### Basic Callable and Future
```java
import java.util.concurrent.*;
import java.util.ArrayList;
import java.util.List;

public class CallableFutureExample {
    private final ExecutorService executor = Executors.newFixedThreadPool(4);
    
    public void basicCallableExample() {
        // Create a Callable that returns a computed value
        Callable<Integer> computation = () -> {
            Thread.sleep(2000);
            return 42 * 1337;
        };
        
        // Submit and get Future
        Future<Integer> future = executor.submit(computation);
        
        try {
            // Non-blocking check
            if (!future.isDone()) {
                System.out.println("Computation in progress...");
            }
            
            // Blocking call with timeout
            Integer result = future.get(3, TimeUnit.SECONDS);
            System.out.println("Computation result: " + result);
            
        } catch (InterruptedException | ExecutionException | TimeoutException e) {
            System.err.println("Computation failed: " + e.getMessage());
            future.cancel(true); // Interrupt if running
        }
        
        executor.shutdown();
    }
    
    public void multipleCallablesExample() throws InterruptedException {
        List<Callable<String>> tasks = new ArrayList<>();
        
        // Create multiple computational tasks
        for (int i = 0; i < 5; i++) {
            final int taskId = i;
            tasks.add(() -> {
                Thread.sleep(1000 + (taskId * 500));
                if (taskId == 3) {
                    throw new RuntimeException("Task " + taskId + " failed!");
                }
                return "Task " + taskId + " completed successfully";
            });
        }
        
        // Submit all tasks
        List<Future<String>> futures = new ArrayList<>();
        for (Callable<String> task : tasks) {
            futures.add(executor.submit(task));
        }
        
        // Process results as they become available
        for (int i = 0; i < futures.size(); i++) {
            Future<String> future = futures.get(i);
            try {
                String result = future.get();
                System.out.println("Success: " + result);
            } catch (ExecutionException e) {
                System.err.println("Task " + i + " failed: " + e.getCause().getMessage());
            }
        }
        
        executor.shutdown();
    }
}
```

#### CompletableFuture Advanced Usage
```java
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;

public class CompletableFutureExample {
    
    public void basicCompletableFuture() {
        // Create and complete a future manually
        CompletableFuture<String> future = new CompletableFuture<>();
        
        // Complete it from another thread
        CompletableFuture.runAsync(() -> {
            try {
                Thread.sleep(1000);
                future.complete("Hello from CompletableFuture!");
            } catch (InterruptedException e) {
                future.completeExceptionally(e);
            }
        });
        
        // Get result
        try {
            String result = future.get();
            System.out.println(result);
        } catch (InterruptedException | ExecutionException e) {
            System.err.println("Future failed: " + e.getMessage());
        }
    }
    
    public void chainedOperations() {
        CompletableFuture<Integer> future = CompletableFuture
            .supplyAsync(() -> {
                System.out.println("Step 1: Initial computation");
                return 10;
            })
            .thenApply(result -> {
                System.out.println("Step 2: Transform " + result);
                return result * 2;
            })
            .thenApply(result -> {
                System.out.println("Step 3: Add to " + result);
                return result + 5;
            })
            .thenCompose(result -> {
                System.out.println("Step 4: Compose with another async operation");
                return CompletableFuture.supplyAsync(() -> result * 10);
            });
        
        try {
            Integer finalResult = future.get();
            System.out.println("Final result: " + finalResult);
        } catch (InterruptedException | ExecutionException e) {
            System.err.println("Chain failed: " + e.getMessage());
        }
    }
    
    public void combiningFutures() {
        CompletableFuture<String> future1 = CompletableFuture.supplyAsync(() -> {
            try { Thread.sleep(1000); } catch (InterruptedException e) {}
            return "Hello";
        });
        
        CompletableFuture<String> future2 = CompletableFuture.supplyAsync(() -> {
            try { Thread.sleep(2000); } catch (InterruptedException e) {}
            return "World";
        });
        
        // Combine two futures
        CompletableFuture<String> combined = future1.thenCombine(future2, 
            (result1, result2) -> result1 + " " + result2 + "!");
        
        // Handle both success and failure
        combined.whenComplete((result, throwable) -> {
            if (throwable != null) {
                System.err.println("Combined operation failed: " + throwable.getMessage());
            } else {
                System.out.println("Combined result: " + result);
            }
        });
        
        try {
            combined.get(); // Wait for completion
        } catch (InterruptedException | ExecutionException e) {
            System.err.println("Error: " + e.getMessage());
        }
    }
    
    public void exceptionHandling() {
        CompletableFuture<String> future = CompletableFuture
            .supplyAsync(() -> {
                if (Math.random() > 0.5) {
                    throw new RuntimeException("Random failure!");
                }
                return "Success!";
            })
            .exceptionally(throwable -> {
                System.err.println("Handling exception: " + throwable.getMessage());
                return "Recovered from failure";
            })
            .thenApply(result -> {
                System.out.println("Processing: " + result);
                return result.toUpperCase();
            });
        
        try {
            String result = future.get();
            System.out.println("Final result: " + result);
        } catch (InterruptedException | ExecutionException e) {
            System.err.println("Unexpected error: " + e.getMessage());
        }
    }
}
```

#### Future with Timeout and Cancellation
```java
public class FutureTimeoutExample {
    private final ExecutorService executor = Executors.newCachedThreadPool();
    
    public void demonstrateTimeoutAndCancellation() {
        // Create a long-running task
        Future<String> longTask = executor.submit(() -> {
            for (int i = 0; i < 10; i++) {
                if (Thread.currentThread().isInterrupted()) {
                    System.out.println("Task was interrupted");
                    return "Task interrupted at step " + i;
                }
                
                try {
                    Thread.sleep(1000);
                    System.out.println("Step " + i + " completed");
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    return "Task interrupted during sleep at step " + i;
                }
            }
            return "Task completed successfully";
        });
        
        try {
            // Try to get result with timeout
            String result = longTask.get(3, TimeUnit.SECONDS);
            System.out.println("Result: " + result);
            
        } catch (TimeoutException e) {
            System.out.println("Task timed out, attempting cancellation...");
            
            boolean cancelled = longTask.cancel(true); // true = interrupt if running
            System.out.println("Cancellation successful: " + cancelled);
            
            // Wait a bit and check final status
            try {
                Thread.sleep(1000);
                System.out.println("Task cancelled: " + longTask.isCancelled());
                System.out.println("Task done: " + longTask.isDone());
            } catch (InterruptedException ie) {
                Thread.currentThread().interrupt();
            }
            
        } catch (InterruptedException | ExecutionException e) {
            System.err.println("Task execution error: " + e.getMessage());
        }
        
        executor.shutdown();
    }
}
```

---

## Practice Questions

### Conceptual Questions
1. **What are the advantages of using thread pools over creating new threads for each task?**
2. **Explain the difference between scheduleAtFixedRate() and scheduleWithFixedDelay().**
3. **What happens when you submit more tasks than a ThreadPoolExecutor can handle?**
4. **How does CompletableFuture improve upon the basic Future interface?**
5. **When would you use a cached thread pool vs a fixed thread pool?**

### Coding Challenges
1. **Create a custom ThreadPoolExecutor that logs task execution times**
2. **Implement a retry mechanism using ScheduledExecutorService**
3. **Build a batch processing system using Callable and Future**
4. **Design a timeout service using CompletableFuture**
5. **Create a load balancing executor that distributes tasks based on thread availability**

### Follow-up Questions
1. **How do you handle thread pool shutdown gracefully in a production system?**
2. **What are the trade-offs between different queue types in ThreadPoolExecutor?**
3. **How can you monitor and tune thread pool performance?**
4. **What strategies can you use to prevent thread pool exhaustion?**
5. **How do you handle exceptions in scheduled tasks to prevent them from stopping?**

---

## Key Takeaways
- ExecutorService provides managed thread pools for better resource utilization
- ThreadPoolExecutor offers fine-grained control over pool behavior
- ScheduledExecutorService enables delayed and periodic task execution
- Callable provides a way to return values from tasks
- Future represents asynchronous computation results
- CompletableFuture enables functional composition of async operations
- Always shutdown executors properly to prevent resource leaks
- Choose the right queue type based on your task submission patterns
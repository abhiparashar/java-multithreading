package basic;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;

class FixedThreadPoolTest {
    private int numberThreads = 0;

    FixedThreadPoolTest(int numberThreads) {
        this.numberThreads = numberThreads;
    }

    public void runTasks() throws ExecutionException, InterruptedException {
        ExecutorService executor = Executors.newFixedThreadPool(numberThreads);

        // ❌ WRONG: You used Callable with sleep - not educational
        // ✅ FIXED: Use ComputationTask to show actual work
        List<Future<?>> futures = new ArrayList<>();

        long startTime = System.currentTimeMillis(); // ❌ MISSING: No timing in your code

        // Submit 8 tasks to see queuing behavior with 3 threads
        for (int i = 0; i < 8; i++) {
            futures.add(executor.submit(new ComputationTask(i, 1000000)));
        }

        // Wait for all tasks to complete
        for (Future<?> future : futures) {
            future.get();
        }

        long endTime = System.currentTimeMillis(); // ❌ MISSING: No performance measurement
        System.out.println("Fixed Pool Total Time: " + (endTime - startTime) + "ms");

        executor.shutdown();
    }
}

class CachedThreadPoolTest {
    public void runTasks() throws ExecutionException, InterruptedException {
        ExecutorService executor = Executors.newCachedThreadPool();

        // ❌ WRONG: You used same sleep pattern as fixed pool
        // ✅ FIXED: Use ComputationTask for consistent comparison
        List<Future<?>> futures = new ArrayList<>();

        long startTime = System.currentTimeMillis(); // ❌ MISSING in your code

        // Submit 5 tasks quickly to show thread creation
        for (int i = 0; i < 5; i++) {
            futures.add(executor.submit(new ComputationTask(i, 500000)));
        }

        // Wait for results
        for (Future<?> future : futures) {
            future.get();
        }

        long endTime = System.currentTimeMillis(); // ❌ MISSING in your code
        System.out.println("Cached Pool Total Time: " + (endTime - startTime) + "ms");

        executor.shutdown();
    }
}

class CustomThreadFactory implements ThreadFactory {
    private final AtomicInteger threadNumber = new AtomicInteger(1);
    @Override
    public Thread newThread(Runnable r) {
        Thread thread = new Thread(r, "CustomPool-" + threadNumber.getAndIncrement());
        thread.setDaemon(false);
        return thread;
    }
}

class ComputationTask implements Runnable {
    private static final AtomicInteger taskCounter = new AtomicInteger(0);
    private final int taskId;
    private final long computationSize;

    // ❌ BUG in your code: Constructor ignored passed taskId parameter
    ComputationTask(int taskId, long computationSize) {
        this.taskId = taskId; // ✅ FIXED: Use passed ID, don't overwrite with counter
        this.computationSize = computationSize;
    }

    @Override
    public void run() {
        long startTime = System.currentTimeMillis();
        String threadName = Thread.currentThread().getName();
        System.out.println("Task " + taskId + " started on " + threadName);

        // Simulate CPU-intensive work
        double sum = 0;
        for (long i = 0; i < computationSize; i++) {
            sum = sum + Math.sqrt(i);
        }

        long endTime = System.currentTimeMillis();
        System.out.println("Task " + taskId + " completed on " + threadName +
                " in " + (endTime - startTime) + "ms, Result: " + sum);
    }
}

public class ThreadPoolDemo {
    public static void main(String[] args) throws ExecutionException, InterruptedException {
        System.out.println("=== FIXED THREAD POOL TEST ===");
        FixedThreadPoolTest fixedThreadPool = new FixedThreadPoolTest(3);
        fixedThreadPool.runTasks();

        System.out.println("\n=== CACHED THREAD POOL TEST ===");
        CachedThreadPoolTest cachedThreadPool = new CachedThreadPoolTest();
        cachedThreadPool.runTasks();

        System.out.println("\n=== CUSTOM THREAD POOL TEST ===");
        ThreadPoolExecutor customPool = new ThreadPoolExecutor(
                2,                              // corePoolSize
                4,                              // maximumPoolSize
                60L, TimeUnit.SECONDS,         // keepAliveTime
                new LinkedBlockingQueue<>(3),   // workQueue with capacity 3
                new CustomThreadFactory(),      // custom thread factory
                new ThreadPoolExecutor.CallerRunsPolicy() // rejection policy
        );

        // ❌ WRONG: You used Callable with sleep again - inconsistent!
        // ✅ FIXED: Use ComputationTask consistently across all pools
        List<Future<?>> futureList = new ArrayList<>();

        long startTime = System.currentTimeMillis(); // ❌ MISSING in your code

        for (int i = 0; i < 10; i++) {
            futureList.add(customPool.submit(new ComputationTask(i, 800000)));

            // ✅ GOOD: You added this monitoring - keep it!
            System.out.println("After submitting task " + i +
                    " - Active: " + customPool.getActiveCount() +
                    ", Queue: " + customPool.getQueue().size());
        }

        // Wait for all tasks
        for (Future<?> future : futureList) {
            future.get();
        }

        long endTime = System.currentTimeMillis(); // ❌ MISSING in your code
        System.out.println("Custom Pool Total Time: " + (endTime - startTime) + "ms");

        customPool.shutdown();
    }
}
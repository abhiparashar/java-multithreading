/**
* EASY LEVEL MULTITHREADING SOLUTIONS
* Complete implementations with detailed explanations and follow-up questions
  */

// ============================================================================
// PROBLEM 1: Basic Thread Creation and Management
// ============================================================================

import java.util.concurrent.*;

// Method 1: Extending Thread class
class CustomThread extends Thread {
private final String taskName;

    public CustomThread(String taskName) {
        super(taskName); // Set thread name
        this.taskName = taskName;
    }
    
    @Override
    public void run() {
        try {
            for (int i = 0; i < 5; i++) {
                System.out.println(taskName + " executing step " + i + 
                                 " on thread: " + Thread.currentThread().getName());
                Thread.sleep(1000); // Simulate work
            }
        } catch (InterruptedException e) {
            System.out.println(taskName + " was interrupted");
            Thread.currentThread().interrupt(); // Restore interrupt status
        }
    }
}

// Method 2: Implementing Runnable interface
class RunnableTask implements Runnable {
private final String taskName;

    public RunnableTask(String taskName) {
        this.taskName = taskName;
    }
    
    @Override
    public void run() {
        try {
            for (int i = 0; i < 5; i++) {
                System.out.println(taskName + " executing step " + i + 
                                 " on thread: " + Thread.currentThread().getName());
                Thread.sleep(800);
            }
        } catch (InterruptedException e) {
            System.out.println(taskName + " was interrupted");
            Thread.currentThread().interrupt();
        }
    }
}

// Method 3: Implementing Callable interface (returns result)
class CallableTask implements Callable<String> {
private final String taskName;
private final int workAmount;

    public CallableTask(String taskName, int workAmount) {
        this.taskName = taskName;
        this.workAmount = workAmount;
    }
    
    @Override
    public String call() throws Exception {
        int sum = 0;
        for (int i = 0; i < workAmount; i++) {
            sum += i;
            Thread.sleep(100); // Simulate computation
        }
        return taskName + " completed with result: " + sum;
    }
}

public class ThreadCreationDemo {
public static void main(String[] args) throws Exception {
System.out.println("=== Thread Creation Methods Demo ===\n");

        // Method 1: Using Thread class
        CustomThread thread1 = new CustomThread("CustomThread-Task");
        thread1.start();
        
        // Method 2: Using Runnable
        Thread thread2 = new Thread(new RunnableTask("Runnable-Task"), "RunnableWorker");
        thread2.start();
        
        // Method 3: Using Callable with ExecutorService
        ExecutorService executor = Executors.newSingleThreadExecutor();
        Future<String> future = executor.submit(new CallableTask("Callable-Task", 10));
        
        // Wait for callable result
        String result = future.get(); // This blocks until result is available
        System.out.println("Callable result: " + result);
        
        // Proper cleanup
        executor.shutdown();
        
        // Wait for all threads to complete
        thread1.join();
        thread2.join();
        
        System.out.println("All threads completed!");
    }
}

// ============================================================================
// PROBLEM 2: Simple Producer-Consumer
// ============================================================================

import java.util.*;
import java.util.concurrent.locks.*;

class SimpleProducerConsumer {
private final List<Integer> buffer = new ArrayList<>();
private final int capacity = 5;
private final Object lock = new Object();

    // Producer thread
    class Producer implements Runnable {
        @Override
        public void run() {
            int value = 0;
            while (true) {
                try {
                    synchronized (lock) {
                        // Wait while buffer is full
                        while (buffer.size() >= capacity) {
                            System.out.println("Buffer full, producer waiting...");
                            lock.wait();
                        }
                        
                        // Produce item
                        buffer.add(value);
                        System.out.println("Produced: " + value + ", Buffer size: " + buffer.size());
                        value++;
                        
                        // Notify waiting consumers
                        lock.notifyAll();
                    }
                    Thread.sleep(1000); // Simulate production time
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    break;
                }
            }
        }
    }
    
    // Consumer thread
    class Consumer implements Runnable {
        private final String consumerName;
        
        public Consumer(String name) {
            this.consumerName = name;
        }
        
        @Override
        public void run() {
            while (true) {
                try {
                    synchronized (lock) {
                        // Wait while buffer is empty
                        while (buffer.isEmpty()) {
                            System.out.println(consumerName + " waiting, buffer empty...");
                            lock.wait();
                        }
                        
                        // Consume item
                        int value = buffer.remove(0);
                        System.out.println(consumerName + " consumed: " + value + 
                                         ", Buffer size: " + buffer.size());
                        
                        // Notify waiting producers
                        lock.notifyAll();
                    }
                    Thread.sleep(1500); // Simulate consumption time
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    break;
                }
            }
        }
    }
    
    public void start() {
        Thread producer = new Thread(new Producer(), "Producer");
        Thread consumer1 = new Thread(new Consumer("Consumer-1"), "Consumer-1");
        Thread consumer2 = new Thread(new Consumer("Consumer-2"), "Consumer-2");
        
        producer.start();
        consumer1.start();
        consumer2.start();
    }
    
    public static void main(String[] args) {
        new SimpleProducerConsumer().start();
    }
}

// ============================================================================
// PROBLEM 3: Thread Pool Basics
// ============================================================================

import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;

class ThreadPoolDemo {
private static final AtomicInteger taskCounter = new AtomicInteger(0);

    // Computational task that simulates CPU-intensive work
    static class ComputationTask implements Runnable {
        private final int taskId;
        private final long computationSize;
        
        public ComputationTask(long computationSize) {
            this.taskId = taskCounter.incrementAndGet();
            this.computationSize = computationSize;
        }
        
        @Override
        public void run() {
            long startTime = System.currentTimeMillis();
            String threadName = Thread.currentThread().getName();
            
            System.out.println("Task " + taskId + " started on " + threadName);
            
            // Simulate CPU-intensive work
            long sum = 0;
            for (long i = 0; i < computationSize; i++) {
                sum += Math.sqrt(i);
            }
            
            long endTime = System.currentTimeMillis();
            System.out.println("Task " + taskId + " completed on " + threadName + 
                             " in " + (endTime - startTime) + "ms, Result: " + sum);
        }
    }
    
    public static void demonstrateThreadPools() throws InterruptedException {
        System.out.println("=== Thread Pool Demonstration ===\n");
        
        // 1. Fixed Thread Pool
        System.out.println("1. Fixed Thread Pool (3 threads):");
        ExecutorService fixedPool = Executors.newFixedThreadPool(3);
        
        // Submit 8 tasks to see queuing behavior
        for (int i = 0; i < 8; i++) {
            fixedPool.submit(new ComputationTask(1000000));
        }
        
        fixedPool.shutdown();
        fixedPool.awaitTermination(30, TimeUnit.SECONDS);
        
        System.out.println("\n" + "=".repeat(50) + "\n");
        
        // 2. Cached Thread Pool
        System.out.println("2. Cached Thread Pool:");
        ExecutorService cachedPool = Executors.newCachedThreadPool();
        
        for (int i = 0; i < 5; i++) {
            cachedPool.submit(new ComputationTask(500000));
        }
        
        cachedPool.shutdown();
        cachedPool.awaitTermination(30, TimeUnit.SECONDS);
        
        System.out.println("\n" + "=".repeat(50) + "\n");
        
        // 3. Custom ThreadPoolExecutor with monitoring
        System.out.println("3. Custom ThreadPoolExecutor:");
        ThreadPoolExecutor customPool = new ThreadPoolExecutor(
            2,                              // corePoolSize
            4,                              // maximumPoolSize
            60L, TimeUnit.SECONDS,         // keepAliveTime
            new LinkedBlockingQueue<>(3),   // workQueue with capacity 3
            new ThreadFactory() {           // custom thread factory
                private final AtomicInteger threadNumber = new AtomicInteger(1);
                @Override
                public Thread newThread(Runnable r) {
                    Thread t = new Thread(r, "CustomPool-" + threadNumber.getAndIncrement());
                    t.setDaemon(false);
                    return t;
                }
            },
            new ThreadPoolExecutor.CallerRunsPolicy() // rejection policy
        );
        
        // Monitor pool statistics
        for (int i = 0; i < 10; i++) {
            customPool.submit(new ComputationTask(800000));
            
            System.out.println("Pool stats - Active: " + customPool.getActiveCount() + 
                             ", Queue: " + customPool.getQueue().size() + 
                             ", Completed: " + customPool.getCompletedTaskCount());
        }
        
        customPool.shutdown();
        customPool.awaitTermination(30, TimeUnit.SECONDS);
    }
    
    public static void main(String[] args) throws InterruptedException {
        demonstrateThreadPools();
    }
}

// ============================================================================
// PROBLEM 4: Atomic Operations
// ============================================================================

import java.util.concurrent.atomic.AtomicInteger;

class CounterComparison {

    // Thread-unsafe counter
    static class UnsafeCounter {
        private int count = 0;
        
        public void increment() {
            count++; // Not atomic! Read-modify-write operation
        }
        
        public int getCount() {
            return count;
        }
    }
    
    // Synchronized counter
    static class SynchronizedCounter {
        private int count = 0;
        
        public synchronized void increment() {
            count++;
        }
        
        public synchronized int getCount() {
            return count;
        }
    }
    
    // Atomic counter
    static class AtomicCounter {
        private final AtomicInteger count = new AtomicInteger(0);
        
        public void increment() {
            count.incrementAndGet();
        }
        
        public int getCount() {
            return count.get();
        }
        
        // Advanced atomic operation
        public boolean incrementIfLessThan(int threshold) {
            while (true) {
                int current = count.get();
                if (current >= threshold) {
                    return false;
                }
                if (count.compareAndSet(current, current + 1)) {
                    return true;
                }
                // If CAS failed, retry (another thread modified the value)
            }
        }
    }
    
    // Test runner for different counter implementations
    static class CounterTest {
        private static final int NUM_THREADS = 10;
        private static final int INCREMENTS_PER_THREAD = 100000;
        
        public static void testCounter(String name, Runnable incrementOperation, 
                                     java.util.function.Supplier<Integer> getCount) 
                                     throws InterruptedException {
            
            Thread[] threads = new Thread[NUM_THREADS];
            long startTime = System.nanoTime();
            
            // Create and start threads
            for (int i = 0; i < NUM_THREADS; i++) {
                threads[i] = new Thread(() -> {
                    for (int j = 0; j < INCREMENTS_PER_THREAD; j++) {
                        incrementOperation.run();
                    }
                });
                threads[i].start();
            }
            
            // Wait for all threads to complete
            for (Thread thread : threads) {
                thread.join();
            }
            
            long endTime = System.nanoTime();
            double duration = (endTime - startTime) / 1_000_000.0; // Convert to milliseconds
            
            int finalCount = getCount.get();
            int expectedCount = NUM_THREADS * INCREMENTS_PER_THREAD;
            
            System.out.printf("%-20s: Final count = %,d (expected %,d), " +
                            "Time = %.2f ms, Correct = %s%n",
                            name, finalCount, expectedCount, duration, 
                            finalCount == expectedCount ? "YES" : "NO");
        }
    }
    
    public static void main(String[] args) throws InterruptedException {
        System.out.println("=== Counter Implementation Comparison ===\n");
        System.out.println("Running " + CounterTest.NUM_THREADS + " threads, " + 
                         CounterTest.INCREMENTS_PER_THREAD + " increments each\n");
        
        // Test unsafe counter
        UnsafeCounter unsafeCounter = new UnsafeCounter();
        CounterTest.testCounter("Unsafe Counter", 
                               unsafeCounter::increment, 
                               unsafeCounter::getCount);
        
        // Test synchronized counter
        SynchronizedCounter syncCounter = new SynchronizedCounter();
        CounterTest.testCounter("Synchronized Counter", 
                               syncCounter::increment, 
                               syncCounter::getCount);
        
        // Test atomic counter
        AtomicCounter atomicCounter = new AtomicCounter();
        CounterTest.testCounter("Atomic Counter", 
                               atomicCounter::increment, 
                               atomicCounter::getCount);
        
        // Demonstrate advanced atomic operation
        System.out.println("\n=== Advanced Atomic Operations ===");
        AtomicCounter advancedCounter = new AtomicCounter();
        
        // Multiple threads trying to increment only if less than threshold
        Thread[] threads = new Thread[5];
        for (int i = 0; i < 5; i++) {
            final int threadId = i;
            threads[i] = new Thread(() -> {
                for (int j = 0; j < 20; j++) {
                    boolean success = advancedCounter.incrementIfLessThan(50);
                    if (success) {
                        System.out.println("Thread " + threadId + " incremented to " + 
                                         advancedCounter.getCount());
                    } else {
                        System.out.println("Thread " + threadId + " failed - threshold reached");
                        break;
                    }
                    try {
                        Thread.sleep(10);
                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                        break;
                    }
                }
            });
            threads[i].start();
        }
        
        for (Thread thread : threads) {
            thread.join();
        }
        
        System.out.println("Final count with threshold: " + advancedCounter.getCount());
    }
}

// ============================================================================
// PROBLEM 5: Simple Deadlock Detection
// ============================================================================

class DeadlockDemo {
private final Object lock1 = new Object();
private final Object lock2 = new Object();

    // Method that acquires locks in order: lock1 -> lock2
    public void method1() {
        synchronized (lock1) {
            System.out.println(Thread.currentThread().getName() + " acquired lock1");
            
            try {
                Thread.sleep(100); // Increase chance of deadlock
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                return;
            }
            
            System.out.println(Thread.currentThread().getName() + " waiting for lock2");
            synchronized (lock2) {
                System.out.println(Thread.currentThread().getName() + " acquired lock2");
                // Do some work
            }
        }
    }
    
    // Method that acquires locks in reverse order: lock2 -> lock1 (DEADLOCK!)
    public void method2() {
        synchronized (lock2) {
            System.out.println(Thread.currentThread().getName() + " acquired lock2");
            
            try {
                Thread.sleep(100);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                return;
            }
            
            System.out.println(Thread.currentThread().getName() + " waiting for lock1");
            synchronized (lock1) {
                System.out.println(Thread.currentThread().getName() + " acquired lock1");
                // Do some work
            }
        }
    }
    
    // Fixed version - consistent lock ordering
    public void method1Fixed() {
        acquireLocksInOrder("method1Fixed");
    }
    
    public void method2Fixed() {
        acquireLocksInOrder("method2Fixed");
    }
    
    private void acquireLocksInOrder(String methodName) {
        // Always acquire locks in the same order to prevent deadlock
        synchronized (lock1) {
            System.out.println(Thread.currentThread().getName() + 
                             " (" + methodName + ") acquired lock1");
            
            try {
                Thread.sleep(50);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                return;
            }
            
            synchronized (lock2) {
                System.out.println(Thread.currentThread().getName() + 
                                 " (" + methodName + ") acquired lock2");
                // Do some work
                try {
                    Thread.sleep(50);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
            }
        }
    }
    
    public static void demonstrateDeadlock() {
        System.out.println("=== Deadlock Demonstration ===");
        System.out.println("This will likely cause a deadlock (wait 10 seconds)...\n");
        
        DeadlockDemo demo = new DeadlockDemo();
        
        Thread thread1 = new Thread(() -> {
            for (int i = 0; i < 5; i++) {
                demo.method1();
                try {
                    Thread.sleep(200);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    break;
                }
            }
        }, "Thread-1");
        
        Thread thread2 = new Thread(() -> {
            for (int i = 0; i < 5; i++) {
                demo.method2();
                try {
                    Thread.sleep(200);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    break;
                }
            }
        }, "Thread-2");
        
        thread1.start();
        thread2.start();
        
        // Wait for a maximum of 10 seconds
        try {
            thread1.join(10000);
            thread2.join(10000);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
        
        if (thread1.isAlive() || thread2.isAlive()) {
            System.out.println("\n*** DEADLOCK DETECTED! ***");
            System.out.println("Threads are still running after timeout.");
            
            // Force stop the threads (not recommended in production)
            thread1.interrupt();
            thread2.interrupt();
        } else {
            System.out.println("No deadlock occurred (lucky timing!)");
        }
    }
    
    public static void demonstrateDeadlockFix() {
        System.out.println("\n=== Deadlock Fix Demonstration ===");
        System.out.println("Using consistent lock ordering...\n");
        
        DeadlockDemo demo = new DeadlockDemo();
        
        Thread thread1 = new Thread(() -> {
            for (int i = 0; i < 3; i++) {
                demo.method1Fixed();
            }
        }, "FixedThread-1");
        
        Thread thread2 = new Thread(() -> {
            for (int i = 0; i < 3; i++) {
                demo.method2Fixed();
            }
        }, "FixedThread-2");
        
        thread1.start();
        thread2.start();
        
        try {
            thread1.join();
            thread2.join();
            System.out.println("Both threads completed successfully - no deadlock!");
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }
    
    public static void main(String[] args) {
        demonstrateDeadlock();
        demonstrateDeadlockFix();
    }
}

/*
* FOLLOW-UP QUESTIONS FOR EASY LEVEL:
*
* PROBLEM 1 - Thread Creation:
* 1. Why is implementing Runnable preferred over extending Thread?
* 2. What happens if you call run() instead of start()?
* 3. How do you handle exceptions in different thread creation methods?
* 4. When should you use Callable vs Runnable?
* 5. What's the difference between daemon and user threads?
*
* PROBLEM 2 - Producer-Consumer:
* 1. Why do we use while loop instead of if for wait conditions?
* 2. What happens if we use notify() instead of notifyAll()?
* 3. How would you implement this using BlockingQueue?
* 4. What are the performance implications of synchronized blocks?
* 5. How would you handle multiple producers and consumers?
*
* PROBLEM 3 - Thread Pools:
* 1. What's the difference between corePoolSize and maximumPoolSize?
* 2. When would a RejectedExecutionException be thrown?
* 3. How do different queue types affect pool behavior?
* 4. Why is newFixedThreadPool dangerous for servers?
* 5. How do you properly shutdown a thread pool?
*
* PROBLEM 4 - Atomic Operations:
* 1. Why are atomic operations lock-free but not wait-free?
* 2. What is the ABA problem and how do you solve it?
* 3. When would you choose synchronized over atomic operations?
* 4. How do atomic operations work at the CPU level?
* 5. What are memory ordering guarantees of atomic operations?
*
* PROBLEM 5 - Deadlock:
* 1. What are the four conditions necessary for deadlock?
* 2. How do you detect deadlock at runtime?
* 3. What's the difference between deadlock and livelock?
* 4. How does lock timeout help prevent deadlock?
* 5. What are other deadlock prevention strategies besides lock ordering?
     */
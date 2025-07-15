# Java Multithreading Mastery Guide 🚀

## Table of Contents
1. [Foundation Concepts](#foundation-concepts)
2. [Basic Threading](#basic-threading)
3. [Thread Synchronization](#thread-synchronization)
4. [Advanced Concurrency](#advanced-concurrency)
5. [CompletableFuture Deep Dive](#completablefuture-deep-dive)
6. [Practical Projects](#practical-projects)
7. [Best Practices](#best-practices)

---

## Foundation Concepts

### What is Multithreading?
Think of your computer as a restaurant kitchen:
- **Single-threaded**: One chef doing everything sequentially
- **Multi-threaded**: Multiple chefs working simultaneously on different dishes

```java
// This is single-threaded - everything happens one after another
public class SingleThreaded {
    public static void main(String[] args) {
        cookPasta();    // Takes 10 minutes
        makeSalad();    // Takes 5 minutes
        bakeBread();    // Takes 15 minutes
        // Total: 30 minutes
    }
}

// This is multi-threaded - things happen simultaneously
public class MultiThreaded {
    public static void main(String[] args) {
        Thread chef1 = new Thread(() -> cookPasta());
        Thread chef2 = new Thread(() -> makeSalad());
        Thread chef3 = new Thread(() -> bakeBread());
        
        chef1.start();
        chef2.start();
        chef3.start();
        // Total: 15 minutes (longest task)
    }
}
```

### Key Terms You Must Know
- **Thread**: A lightweight sub-process that can run concurrently
- **Process**: A program in execution (your entire Java application)
- **Concurrency**: Multiple threads making progress (not necessarily simultaneously)
- **Parallelism**: Multiple threads executing simultaneously on different CPU cores

---

## Basic Threading

### Method 1: Extending Thread Class
```java
class thread.MyThread extends Thread {
    private String taskName;
    
    public thread.MyThread(String taskName) {
        this.taskName = taskName;
    }
    
    @Override
    public void run() {
        for (int i = 1; i <= 5; i++) {
            System.out.println(taskName + " - Step " + i);
            try {
                Thread.sleep(1000); // Sleep for 1 second
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }
    }
}

public class thread.ThreadExample1 {
    public static void main(String[] args) {
        thread.MyThread thread1 = new thread.MyThread("Task-A");
        thread.MyThread thread2 = new thread.MyThread("Task-B");
        
        thread1.start(); // Don't call run() directly!
        thread2.start();
    }
}
```

### Method 2: Implementing Runnable Interface (Preferred)
```java
class MyTask implements Runnable {
    private String taskName;
    
    public MyTask(String taskName) {
        this.taskName = taskName;
    }
    
    @Override
    public void run() {
        for (int i = 1; i <= 5; i++) {
            System.out.println(taskName + " - Step " + i);
            try {
                Thread.sleep(1000);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                return;
            }
        }
    }
}

public class thread.RunnableExample {
    public static void main(String[] args) {
        Thread thread1 = new Thread(new MyTask("Download"));
        Thread thread2 = new Thread(new MyTask("Upload"));
        
        thread1.start();
        thread2.start();
    }
}
```

### Method 3: Using Lambda Expressions (Modern Way)
```java
public class LambdaThreads {
    public static void main(String[] args) {
        // Simple lambda thread
        Thread thread1 = new Thread(() -> {
            for (int i = 1; i <= 5; i++) {
                System.out.println("Lambda Thread - " + i);
                try {
                    Thread.sleep(1000);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    return;
                }
            }
        });
        
        thread1.start();
    }
}
```

### Thread Lifecycle
```java
public class thread.ThreadLifecycle {
    public static void main(String[] args) throws InterruptedException {
        Thread thread = new Thread(() -> {
            System.out.println("Thread is running");
            try {
                Thread.sleep(2000);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
            System.out.println("Thread finished");
        });
        
        System.out.println("State: " + thread.getState()); // NEW
        
        thread.start();
        System.out.println("State: " + thread.getState()); // RUNNABLE
        
        Thread.sleep(100);
        System.out.println("State: " + thread.getState()); // TIMED_WAITING
        
        thread.join(); // Wait for thread to finish
        System.out.println("State: " + thread.getState()); // TERMINATED
    }
}
```

---

## Thread Synchronization

### The Problem: Race Conditions
```java
class Counter {
    private int count = 0;
    
    // This is NOT thread-safe!
    public void increment() {
        count++; // This is actually 3 operations: read, increment, write
    }
    
    public int getCount() {
        return count;
    }
}

public class RaceConditionExample {
    public static void main(String[] args) throws InterruptedException {
        Counter counter = new Counter();
        
        // Create 1000 threads, each incrementing the counter
        Thread[] threads = new Thread[1000];
        for (int i = 0; i < 1000; i++) {
            threads[i] = new Thread(() -> {
                for (int j = 0; j < 1000; j++) {
                    counter.increment();
                }
            });
        }
        
        // Start all threads
        for (Thread thread : threads) {
            thread.start();
        }
        
        // Wait for all threads to finish
        for (Thread thread : threads) {
            thread.join();
        }
        
        System.out.println("Final count: " + counter.getCount());
        // Expected: 1,000,000 but you'll get less due to race condition!
    }
}
```

### Solution 1: Synchronized Methods
```java
class SynchronizedCounter {
    private int count = 0;
    
    // This is thread-safe!
    public synchronized void increment() {
        count++;
    }
    
    public synchronized int getCount() {
        return count;
    }
}
```

### Solution 2: Synchronized Blocks
```java
class BlockSynchronizedCounter {
    private int count = 0;
    private final Object lock = new Object();
    
    public void increment() {
        synchronized (lock) {
            count++;
        }
    }
    
    public int getCount() {
        synchronized (lock) {
            return count;
        }
    }
}
```

### Solution 3: ReentrantLock (More Flexible)
```java
import java.util.concurrent.locks.ReentrantLock;

class LockCounter {
    private int count = 0;
    private final ReentrantLock lock = new ReentrantLock();
    
    public void increment() {
        lock.lock();
        try {
            count++;
        } finally {
            lock.unlock(); // Always unlock in finally block!
        }
    }
    
    public int getCount() {
        lock.lock();
        try {
            return count;
        } finally {
            lock.unlock();
        }
    }
}
```

### Producer-Consumer Pattern
```java
import java.util.LinkedList;
import java.util.Queue;

class ProducerConsumerExample {
    private Queue<Integer> queue = new LinkedList<>();
    private final int MAX_SIZE = 5;
    private final Object lock = new Object();
    
    public void produce(int item) throws InterruptedException {
        synchronized (lock) {
            while (queue.size() == MAX_SIZE) {
                System.out.println("Queue is full, producer waiting...");
                lock.wait(); // Wait until space is available
            }
            
            queue.offer(item);
            System.out.println("Produced: " + item);
            lock.notifyAll(); // Wake up consumers
        }
    }
    
    public int consume() throws InterruptedException {
        synchronized (lock) {
            while (queue.isEmpty()) {
                System.out.println("Queue is empty, consumer waiting...");
                lock.wait(); // Wait until item is available
            }
            
            int item = queue.poll();
            System.out.println("Consumed: " + item);
            lock.notifyAll(); // Wake up producers
            return item;
        }
    }
    
    public static void main(String[] args) {
        ProducerConsumerExample example = new ProducerConsumerExample();
        
        // Producer thread
        Thread producer = new Thread(() -> {
            try {
                for (int i = 1; i <= 10; i++) {
                    example.produce(i);
                    Thread.sleep(100);
                }
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        });
        
        // Consumer thread
        Thread consumer = new Thread(() -> {
            try {
                for (int i = 1; i <= 10; i++) {
                    example.consume();
                    Thread.sleep(200);
                }
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        });
        
        producer.start();
        consumer.start();
    }
}
```

---

## Advanced Concurrency

### Thread Pool with ExecutorService
```java
import java.util.concurrent.*;

public class ThreadPoolExample {
    public static void main(String[] args) {
        // Create a fixed thread pool with 3 threads
        ExecutorService executor = Executors.newFixedThreadPool(3);
        
        // Submit 10 tasks
        for (int i = 1; i <= 10; i++) {
            final int taskId = i;
            executor.submit(() -> {
                System.out.println("Task " + taskId + " started by " + 
                    Thread.currentThread().getName());
                try {
                    Thread.sleep(2000); // Simulate work
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
                System.out.println("Task " + taskId + " completed");
            });
        }
        
        executor.shutdown(); // Stop accepting new tasks
        
        try {
            if (!executor.awaitTermination(60, TimeUnit.SECONDS)) {
                executor.shutdownNow();
            }
        } catch (InterruptedException e) {
            executor.shutdownNow();
        }
    }
}
```

### Callable and Future
```java
import java.util.concurrent.*;

class FactorialCalculator implements Callable<Long> {
    private final int number;
    
    public FactorialCalculator(int number) {
        this.number = number;
    }
    
    @Override
    public Long call() throws Exception {
        long result = 1;
        for (int i = 1; i <= number; i++) {
            result *= i;
            Thread.sleep(100); // Simulate heavy computation
        }
        return result;
    }
}

public class CallableFutureExample {
    public static void main(String[] args) throws Exception {
        ExecutorService executor = Executors.newFixedThreadPool(2);
        
        // Submit tasks that return values
        Future<Long> future1 = executor.submit(new FactorialCalculator(5));
        Future<Long> future2 = executor.submit(new FactorialCalculator(7));
        
        // Do other work while calculations are running
        System.out.println("Calculations started...");
        
        // Get results (this will block until computation is complete)
        Long result1 = future1.get();
        Long result2 = future2.get();
        
        System.out.println("Factorial of 5: " + result1);
        System.out.println("Factorial of 7: " + result2);
        
        executor.shutdown();
    }
}
```

### Concurrent Collections
```java
import java.util.concurrent.*;
import java.util.*;

public class ConcurrentCollectionsExample {
    public static void main(String[] args) throws InterruptedException {
        // Thread-safe collections
        ConcurrentHashMap<String, Integer> concurrentMap = new ConcurrentHashMap<>();
        CopyOnWriteArrayList<String> concurrentList = new CopyOnWriteArrayList<>();
        BlockingQueue<String> blockingQueue = new LinkedBlockingQueue<>();
        
        ExecutorService executor = Executors.newFixedThreadPool(5);
        
        // Multiple threads adding to concurrent collections
        for (int i = 0; i < 5; i++) {
            final int threadId = i;
            executor.submit(() -> {
                concurrentMap.put("Thread-" + threadId, threadId);
                concurrentList.add("Item-" + threadId);
                try {
                    blockingQueue.put("Message-" + threadId);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
            });
        }
        
        Thread.sleep(1000); // Let threads finish
        
        System.out.println("Concurrent Map: " + concurrentMap);
        System.out.println("Concurrent List: " + concurrentList);
        System.out.println("Blocking Queue: " + blockingQueue);
        
        executor.shutdown();
    }
}
```

---

## CompletableFuture Deep Dive

### What is CompletableFuture?
CompletableFuture is like a smart contract for asynchronous operations. It represents a future result that will be available at some point, and you can chain operations on it.

```java
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

public class CompletableFutureBasics {
    public static void main(String[] args) throws Exception {
        // Creating a CompletableFuture
        CompletableFuture<String> future = CompletableFuture.supplyAsync(() -> {
            try {
                Thread.sleep(2000);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
            return "Hello from CompletableFuture!";
        });
        
        // This runs immediately, doesn't wait
        System.out.println("This prints immediately");
        
        // This waits for the result
        String result = future.get();
        System.out.println(result);
    }
}
```

### Chaining Operations
```java
import java.util.concurrent.CompletableFuture;

public class CompletableFutureChaining {
    public static void main(String[] args) throws Exception {
        CompletableFuture<String> future = CompletableFuture
            .supplyAsync(() -> {
                System.out.println("Step 1: Fetching user data...");
                return "userId123";
            })
            .thenApply(userId -> {
                System.out.println("Step 2: Processing user: " + userId);
                return "User: John Doe";
            })
            .thenApply(userName -> {
                System.out.println("Step 3: Formatting: " + userName);
                return userName.toUpperCase();
            })
            .thenCompose(formattedName -> {
                // thenCompose is for operations that return CompletableFuture
                return CompletableFuture.supplyAsync(() -> {
                    System.out.println("Step 4: Saving to database: " + formattedName);
                    return "Saved: " + formattedName;
                });
            });
        
        System.out.println("Final result: " + future.get());
    }
}
```

### Combining Multiple Futures
```java
import java.util.concurrent.CompletableFuture;

public class CombiningFutures {
    public static void main(String[] args) throws Exception {
        CompletableFuture<String> future1 = CompletableFuture.supplyAsync(() -> {
            sleep(1000);
            return "Data from API 1";
        });
        
        CompletableFuture<String> future2 = CompletableFuture.supplyAsync(() -> {
            sleep(1500);
            return "Data from API 2";
        });
        
        CompletableFuture<String> future3 = CompletableFuture.supplyAsync(() -> {
            sleep(800);
            return "Data from API 3";
        });
        
        // Combine all results
        CompletableFuture<String> combinedFuture = future1
            .thenCombine(future2, (result1, result2) -> result1 + " + " + result2)
            .thenCombine(future3, (combined, result3) -> combined + " + " + result3);
        
        System.out.println("Combined result: " + combinedFuture.get());
        
        // Or use allOf for multiple futures
        CompletableFuture<Void> allFutures = CompletableFuture.allOf(future1, future2, future3);
        allFutures.thenRun(() -> {
            System.out.println("All futures completed!");
        }).get();
    }
    
    private static void sleep(int millis) {
        try {
            Thread.sleep(millis);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
        
        long endTime = System.currentTimeMillis();
        
        // Print contention statistics
        ThreadInfo[] threadInfos = threadBean.getThreadInfo(threadBean.getAllThreadIds());
        long totalBlockedTime = 0;
        long totalWaitedTime = 0;
        
        for (ThreadInfo info : threadInfos) {
            if (info != null) {
                totalBlockedTime += info.getBlockedTime();
                totalWaitedTime += info.getWaitedTime();
            }
        }
        
        System.out.println("Total execution time: " + (endTime - startTime) + "ms");
        System.out.println("Total blocked time: " + totalBlockedTime + "ms");
        System.out.println("Total waited time: " + totalWaitedTime + "ms");
        System.out.println("Contention ratio: " + 
            (double)(totalBlockedTime + totalWaitedTime) / (endTime - startTime));
    }
}
```

---

### Scenario 3: Memory Consistency and Visibility
**Question:** Explain the happens-before relationship and how it affects multithreaded programs.

**Answer:**
```java
// Problem: Visibility issue without proper synchronization
class VisibilityProblem {
    private boolean flag = false;
    private int value = 0;
    
    // Thread 1
    public void writer() {
        value = 42;
        flag = true; // Without synchronization, this might not be visible to reader
    }
    
    // Thread 2
    public void reader() {
        if (flag) {
            System.out.println(value); // Might print 0 instead of 42!
        }
    }
}

// Solution 1: Using volatile (establishes happens-before)
class VolatileSolution {
    private volatile boolean flag = false; // volatile establishes happens-before
    private int value = 0;
    
    public void writer() {
        value = 42;
        flag = true; // All writes before this are visible to readers
    }
    
    public void reader() {
        if (flag) { // Reading volatile establishes happens-before
            System.out.println(value); // Guaranteed to see 42
        }
    }
}

// Solution 2: Using synchronization
class SynchronizationSolution {
    private boolean flag = false;
    private int value = 0;
    private final Object lock = new Object();
    
    public void writer() {
        synchronized (lock) {
            value = 42;
            flag = true;
        }
    }
    
    public void reader() {
        synchronized (lock) {
            if (flag) {
                System.out.println(value);
            }
        }
    }
}

// Solution 3: Using atomic operations
class AtomicSolution {
    private final AtomicBoolean flag = new AtomicBoolean(false);
    private final AtomicInteger value = new AtomicInteger(0);
    
    public void writer() {
        value.set(42);
        flag.set(true); // Atomic operations establish happens-before
    }
    
    public void reader() {
        if (flag.get()) {
            System.out.println(value.get());
        }
    }
}

// Demonstrating happens-before with CountDownLatch
class HappensBefore {
    private String message;
    private final CountDownLatch latch = new CountDownLatch(1);
    
    public void writer() {
        message = "Hello World"; // Write
        latch.countDown(); // Happens-before await() returns
    }
    
    public void reader() throws InterruptedException {
        latch.await(); // Waits for countDown()
        System.out.println(message); // Guaranteed to see "Hello World"
    }
    
    public static void main(String[] args) throws InterruptedException {
        HappensBefore demo = new HappensBefore();
        
        Thread writerThread = new Thread(demo::writer);
        Thread readerThread = new Thread(() -> {
            try {
                demo.reader();
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        });
        
        readerThread.start();
        Thread.sleep(100); // Ensure reader is waiting
        writerThread.start();
        
        writerThread.join();
        readerThread.join();
    }
}
```

**Follow-up:** What are the different ways to establish happens-before relationships in Java?
**Answer:**
1. **Synchronized blocks/methods**
2. **Volatile variables**
3. **Thread start/join**
4. **CountDownLatch, Semaphore, etc.**
5. **CompletableFuture completion**
6. **Final fields in constructors**

---

## Behavioral Interview Questions

### Q13: Tell me about a time when you had to debug a complex multithreading issue.
**Sample Answer Structure:**
```
SITUATION: "In our high-traffic e-commerce application, we started seeing intermittent 
data corruption in our shopping cart service during peak hours."

TASK: "I needed to identify and fix the root cause of the data inconsistency that was 
affecting customer orders."

ACTION: 
1. "First, I reproduced the issue in a controlled environment with high concurrency"
2. "Used thread dumps and profiling tools to identify contention points"
3. "Found that multiple threads were modifying shared cart state without proper synchronization"
4. "Implemented a solution using ConcurrentHashMap and atomic operations"
5. "Added comprehensive logging and monitoring to prevent future issues"

RESULT: "The data corruption was eliminated, and we saw a 30% improvement in 
cart service performance due to reduced lock contention."
```

### Q14: How do you approach designing a multithreaded system from scratch?
**Answer Framework:**
```java
// Step 1: Identify concurrency requirements
class SystemDesignApproach {
    
    // Step 2: Define thread safety boundaries
    public void defineThreadSafety() {
        // - Identify shared state
        // - Determine which operations need to be atomic
        // - Choose appropriate synchronization mechanisms
    }
    
    // Step 3: Design for scalability
    public void designForScalability() {
        // - Use thread pools instead of creating threads manually
        // - Implement back-pressure mechanisms
        // - Design stateless components when possible
    }
    
    // Step 4: Handle failures gracefully
    public void handleFailures() {
        // - Implement circuit breakers
        // - Use timeouts for all blocking operations
        // - Design for graceful degradation
    }
    
    // Step 5: Monitor and observe
    public void addObservability() {
        // - Add metrics for thread pool utilization
        // - Monitor contention and lock wait times
        // - Implement health checks
    }
}
```

---

## Quick Reference - Interview Preparation Checklist

### Core Concepts You Must Know ✅
- [ ] **Thread vs Process difference**
- [ ] **Thread creation methods (Thread, Runnable, Callable)**
- [ ] **Thread lifecycle and states**
- [ ] **Synchronization mechanisms (synchronized, locks, atomic)**
- [ ] **Producer-Consumer pattern**
- [ ] **Deadlock detection and prevention**
- [ ] **Thread pools and ExecutorService**
- [ ] **CompletableFuture and async programming**
- [ ] **Concurrent collections**
- [ ] **Memory model and happens-before**

### Code Patterns to Practice 🔄
```java
// 1. Basic synchronization
synchronized (lock) {
    // critical section
}

// 2. Double-checked locking
if (instance == null) {
    synchronized (Class.class) {
        if (instance == null) {
            instance = new Class();
        }
    }
}

// 3. Producer-Consumer with wait/notify
while (condition) {
    lock.wait();
}
// work
lock.notifyAll();

// 4. CompletableFuture chaining
CompletableFuture.supplyAsync(() -> getData())
    .thenApply(data -> process(data))
    .thenAccept(result -> save(result));

// 5. Custom thread pool
ThreadPoolExecutor executor = new ThreadPoolExecutor(
    coreSize, maxSize, keepAlive, unit, workQueue, factory, handler);
```

### Common Mistakes to Avoid ❌
1. **Calling run() instead of start()**
2. **Not handling InterruptedException properly**
3. **Using synchronized on wrong objects**
4. **Forgetting volatile for flags**
5. **Creating too many threads**
6. **Not shutting down ExecutorService**
7. **Race conditions in lazy initialization**
8. **Deadlocks due to lock ordering**

### Performance Optimization Tips 🚀
1. **Use concurrent collections over synchronized collections**
2. **Prefer atomic operations for simple updates**
3. **Minimize critical section size**
4. **Use thread-local storage to avoid contention**
5. **Implement proper back-pressure mechanisms**
6. **Use lock-free algorithms when possible**
7. **Profile before optimizing**

---

## Final Advice for FAANG Interviews

### What Interviewers Look For:
1. **Deep understanding** of concurrency concepts
2. **Ability to write correct, thread-safe code**
3. **System design thinking** for scalable solutions
4. **Debugging skills** for complex issues
5. **Performance awareness** and optimization techniques

### How to Stand Out:
1. **Explain trade-offs** between different approaches
2. **Discuss scalability** and performance implications
3. **Mention real-world considerations** (monitoring, error handling)
4. **Show knowledge of modern patterns** (reactive programming, async)
5. **Demonstrate testing strategies** for concurrent code

### Last-Minute Preparation:
1. **Practice coding** the core patterns without IDE
2. **Prepare examples** from your experience with multithreading
3. **Review Java Memory Model** and happens-before relationships
4. **Understand when to use** different concurrency utilities
5. **Be ready to design** systems with specific requirements

Remember: **Multithreading mastery comes from understanding both the theory and practical implications. Practice building real systems, not just solving isolated problems!** 🎯

---

## Additional FAANG-Style Questions

### Q15: Implement a thread-safe bounded buffer with blocking operations.
**Answer:**
```java
import java.util.concurrent.locks.*;

class BoundedBuffer<T> {
    private final T[] buffer;
    private final int capacity;
    private int count = 0;
    private int putIndex = 0;
    private int takeIndex = 0;
    
    private final ReentrantLock lock = new ReentrantLock();
    private final Condition notEmpty = lock.newCondition();
    private final Condition notFull = lock.newCondition();
    
    @SuppressWarnings("unchecked")
    public BoundedBuffer(int capacity) {
        this.capacity = capacity;
        this.buffer = (T[]) new Object[capacity];
    }
    
    public void put(T item) throws InterruptedException {
        lock.lock();
        try {
            while (count == capacity) {
                notFull.await(); // Wait until buffer is not full
            }
            
            buffer[putIndex] = item;
            putIndex = (putIndex + 1) % capacity;
            count++;
            
            notEmpty.signal(); // Signal waiting consumers
        } finally {
            lock.unlock();
        }
    }
    
    public T take() throws InterruptedException {
        lock.lock();
        try {
            while (count == 0) {
                notEmpty.await(); // Wait until buffer is not empty
            }
            
            T item = buffer[takeIndex];
            buffer[takeIndex] = null; // Help GC
            takeIndex = (takeIndex + 1) % capacity;
            count--;
            
            notFull.signal(); // Signal waiting producers
            return item;
        } finally {
            lock.unlock();
        }
    }
    
    public int size() {
        lock.lock();
        try {
            return count;
        } finally {
            lock.unlock();
        }
    }
}
```

### Q16: Design a thread pool that can dynamically adjust its size based on load.
**Answer:**
```java
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;

class AdaptiveThreadPool {
    private final int minThreads;
    private final int maxThreads;
    private final BlockingQueue<Runnable> workQueue;
    private final AtomicInteger activeThreads = new AtomicInteger(0);
    private final AtomicInteger totalThreads = new AtomicInteger(0);
    private final ConcurrentHashMap<Long, WorkerThread> workers = new ConcurrentHashMap<>();
    private volatile boolean shutdown = false;
    
    // Load monitoring
    private final AtomicInteger queueSizeHistory = new AtomicInteger(0);
    private final ScheduledExecutorService monitor = Executors.newScheduledThreadPool(1);
    
    public AdaptiveThreadPool(int minThreads, int maxThreads, int queueCapacity) {
        this.minThreads = minThreads;
        this.maxThreads = maxThreads;
        this.workQueue = new LinkedBlockingQueue<>(queueCapacity);
        
        // Create minimum threads
        for (int i = 0; i < minThreads; i++) {
            createWorkerThread();
        }
        
        // Start monitoring thread
        monitor.scheduleAtFixedRate(this::adjustPoolSize, 5, 5, TimeUnit.SECONDS);
    }
    
    public void submit(Runnable task) {
        if (shutdown) {
            throw new RejectedExecutionException("ThreadPool is shutdown");
        }
        
        if (!workQueue.offer(task)) {
            throw new RejectedExecutionException("Queue is full");
        }
        
        // If all threads are busy and we can create more, do it
        if (activeThreads.get() == totalThreads.get() && 
            totalThreads.get() < maxThreads && 
            workQueue.size() > 0) {
            createWorkerThread();
        }
    }
    
    private void createWorkerThread() {
        WorkerThread worker = new WorkerThread();
        workers.put(worker.getId(), worker);
        totalThreads.incrementAndGet();
        worker.start();
    }
    
    private void adjustPoolSize() {
        int currentQueue = workQueue.size();
        int currentActive = activeThreads.get();
        int currentTotal = totalThreads.get();
        
        // Scale up if queue is growing and we have capacity
        if (currentQueue > 10 && currentActive == currentTotal && currentTotal < maxThreads) {
            createWorkerThread();
            System.out.println("Scaled UP: Total threads = " + totalThreads.get());
        }
        // Scale down if threads are idle and we're above minimum
        else if (currentQueue == 0 && currentActive < currentTotal / 2 && currentTotal > minThreads) {
            // Mark some threads for termination
            workers.values().stream()
                .filter(worker -> !worker.isActive())
                .findFirst()
                .ifPresent(WorkerThread::terminate);
        }
    }
    
    private class WorkerThread extends Thread {
        private volatile boolean active = false;
        private volatile boolean terminated = false;
        private long lastActiveTime = System.currentTimeMillis();
        
        @Override
        public void run() {
            try {
                while (!shutdown && !terminated) {
                    Runnable task = workQueue.poll(30, TimeUnit.SECONDS);
                    
                    if (task != null) {
                        active = true;
                        activeThreads.incrementAndGet();
                        lastActiveTime = System.currentTimeMillis();
                        
                        try {
                            task.run();
                        } catch (Exception e) {
                            System.err.println("Task execution failed: " + e.getMessage());
                        } finally {
                            activeThreads.decrementAndGet();
                            active = false;
                        }
                    } else {
                        // Check if this thread should be terminated due to inactivity
                        if (System.currentTimeMillis() - lastActiveTime > 60000 && 
                            totalThreads.get() > minThreads) {
                            terminated = true;
                        }
                    }
                }
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            } finally {
                workers.remove(this.getId());
                totalThreads.decrementAndGet();
                System.out.println("Worker thread terminated. Total threads = " + totalThreads.get());
            }
        }
        
        public boolean isActive() {
            return active;
        }
        
        public void terminate() {
            terminated = true;
        }
    }
    
    public void shutdown() {
        shutdown = true;
        monitor.shutdown();
        
        // Interrupt all worker threads
        workers.values().forEach(Thread::interrupt);
    }
    
    public String getStats() {
        return String.format("Active: %d, Total: %d, Queue: %d", 
            activeThreads.get(), totalThreads.get(), workQueue.size());
    }
}
```

### Q17: Implement a distributed lock using Java.
**Answer:**
```java
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

// Simplified distributed lock (simulation)
class DistributedLock {
    private final String lockKey;
    private final long leaseTime;
    private final TimeUnit timeUnit;
    
    // Simulated distributed storage
    private static final ConcurrentHashMap<String, LockEntry> globalLocks = new ConcurrentHashMap<>();
    private static final ScheduledExecutorService leaseRenewal = Executors.newScheduledThreadPool(2);
    
    private final AtomicReference<String> lockToken = new AtomicReference<>();
    private final AtomicBoolean isLocked = new AtomicBoolean(false);
    private ScheduledFuture<?> renewalTask;
    
    static class LockEntry {
        final String token;
        final long expirationTime;
        final String owner;
        
        LockEntry(String token, long expirationTime, String owner) {
            this.token = token;
            this.expirationTime = expirationTime;
            this.owner = owner;
        }
        
        boolean isExpired() {
            return System.currentTimeMillis() > expirationTime;
        }
    }
    
    public DistributedLock(String lockKey, long leaseTime, TimeUnit timeUnit) {
        this.lockKey = lockKey;
        this.leaseTime = leaseTime;
        this.timeUnit = timeUnit;
    }
    
    public boolean tryLock() {
        return tryLock(0, TimeUnit.MILLISECONDS);
    }
    
    public boolean tryLock(long waitTime, TimeUnit unit) {
        long deadline = System.currentTimeMillis() + unit.toMillis(waitTime);
        
        while (System.currentTimeMillis() < deadline) {
            if (acquireLock()) {
                startLeaseRenewal();
                return true;
            }
            
            try {
                Thread.sleep(50); // Retry interval
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                return false;
            }
        }
        
        return false;
    }
    
    private boolean acquireLock() {
        String token = generateToken();
        long expirationTime = System.currentTimeMillis() + timeUnit.toMillis(leaseTime);
        String owner = Thread.currentThread().getName();
        
        LockEntry newEntry = new LockEntry(token, expirationTime, owner);
        
        // Atomic compare-and-set operation
        LockEntry existing = globalLocks.putIfAbsent(lockKey, newEntry);
        
        if (existing == null) {
            // Successfully acquired lock
            lockToken.set(token);
            isLocked.set(true);
            return true;
        } else if (existing.isExpired()) {
            // Try to replace expired lock
            if (globalLocks.replace(lockKey, existing, newEntry)) {
                lockToken.set(token);
                isLocked.set(true);
                return true;
            }
        }
        
        return false;
    }
    
    private void startLeaseRenewal() {
        renewalTask = leaseRenewal.scheduleAtFixedRate(() -> {
            try {
                renewLease();
            } catch (Exception e) {
                System.err.println("Failed to renew lease: " + e.getMessage());
                unlock(); // Release lock if renewal fails
            }
        }, leaseTime / 3, leaseTime / 3, timeUnit);
    }
    
    private void renewLease() {
        if (!isLocked.get()) return;
        
        String currentToken = lockToken.get();
        if (currentToken == null) return;
        
        LockEntry current = globalLocks.get(lockKey);
        if (current != null && current.token.equals(currentToken)) {
            long newExpirationTime = System.currentTimeMillis() + timeUnit.toMillis(leaseTime);
            LockEntry renewed = new LockEntry(currentToken, newExpirationTime, current.owner);
            
            globalLocks.replace(lockKey, current, renewed);
            System.out.println("Lease renewed for key: " + lockKey);
        } else {
            // Lost the lock somehow
            unlock();
        }
    }
    
    public void unlock() {
        if (!isLocked.getAndSet(false)) return;
        
        if (renewalTask != null) {
            renewalTask.cancel(false);
        }
        
        String currentToken = lockToken.getAndSet(null);
        if (currentToken != null) {
            LockEntry current = globalLocks.get(lockKey);
            if (current != null && current.token.equals(currentToken)) {
                globalLocks.remove(lockKey, current);
                System.out.println("Lock released for key: " + lockKey);
            }
        }
    }
    
    private String generateToken() {
        return Thread.currentThread().getName() + "-" + System.nanoTime();
    }
    
    public static void main(String[] args) throws InterruptedException {
        // Test distributed lock
        ExecutorService executor = Executors.newFixedThreadPool(5);
        
        for (int i = 0; i < 5; i++) {
            final int threadId = i;
            executor.submit(() -> {
                DistributedLock lock = new DistributedLock("resource-1", 5, TimeUnit.SECONDS);
                
                try {
                    if (lock.tryLock(10, TimeUnit.SECONDS)) {
                        System.out.println("Thread " + threadId + " acquired lock");
                        
                        // Simulate work
                        Thread.sleep(3000);
                        
                        System.out.println("Thread " + threadId + " finished work");
                    } else {
                        System.out.println("Thread " + threadId + " failed to acquire lock");
                    }
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                } finally {
                    lock.unlock();
                }
            });
        }
        
        executor.shutdown();
        executor.awaitTermination(30, TimeUnit.SECONDS);
        leaseRenewal.shutdown();
    }
}
```

---

## System Design Questions

### Q18: Design a thread-safe cache that can handle millions of requests per second.
**Key Points to Cover:**
1. **Partitioning strategy** to reduce contention
2. **Lock-free operations** where possible
3. **Memory management** and GC considerations
4. **Monitoring and metrics**
5. **Cache warming strategies**

### Q19: How would you implement a rate limiter for a distributed system?
**Key Points to Cover:**
1. **Token bucket vs sliding window** algorithms
2. **Distributed coordination** (Redis, database)
3. **Handling network partitions**
4. **Performance under high load**
5. **Fair vs unfair rate limiting**

### Q20: Design a real-time notification system.
**Key Points to Cover:**
1. **Push vs pull** mechanisms
2. **Connection management** for millions of users
3. **Message ordering** and delivery guarantees
4. **Failure handling** and retry strategies
5. **Scaling considerations**

---

## Interview Tips & Tricks 🎯

### **During Coding Questions:**
1. **Start with brute force** then optimize
2. **Explain thread safety concerns** as you code
3. **Consider edge cases** (empty collections, null values)
4. **Discuss time/space complexity** of concurrent operations
5. **Mention testing strategies** for concurrent code

### **For System Design:**
1. **Ask clarifying questions** about scale and requirements
2. **Start with single-machine design** then scale out
3. **Identify bottlenecks** and how to address them
4. **Discuss trade-offs** between consistency and performance
5. **Consider operational aspects** (monitoring, debugging)

### **Red Flags to Avoid:**
- Creating threads manually instead of using thread pools
- Not handling InterruptedException properly
- Using synchronized on the wrong objects
- Ignoring memory visibility issues
- Not considering deadlock scenarios
- Poor exception handling in concurrent code

### **Impress Your Interviewer:**
- Mention specific Java concurrency utilities by name
- Discuss memory model implications
- Show knowledge of performance characteristics
- Explain how you would test concurrent code
- Demonstrate understanding of production considerations

---

## Final Study Plan 📚

### **Week 1: Fundamentals**
- [ ] Review all basic concepts
- [ ] Practice thread creation methods
- [ ] Implement producer-consumer pattern
- [ ] Understand synchronization mechanisms

### **Week 2: Intermediate Patterns**
- [ ] Master ExecutorService and thread pools
- [ ] Practice CompletableFuture operations
- [ ] Implement thread-safe data structures
- [ ] Study concurrent collections

### **Week 3: Advanced Topics**
- [ ] Fork-Join framework
- [ ] Custom synchronizers
- [ ] Memory model and happens-before
- [ ] Performance optimization techniques

### **Week 4: Interview Practice**
- [ ] Mock interviews with peers
- [ ] System design practice
- [ ] Review common mistake patterns
- [ ] Time yourself on coding problems

### **Day Before Interview:**
- [ ] Review quick reference guide
- [ ] Practice explaining concepts out loud
- [ ] Prepare questions to ask interviewer
- [ ] Get good rest! 😴

---

## Conclusion 🎉

You've now completed the most comprehensive Java multithreading guide available! This covers everything from basic concepts to advanced FAANG-level interview questions.

### **What You've Mastered:**
✅ **Core Threading Concepts** - Threads, synchronization, deadlocks  
✅ **Advanced Concurrency** - Thread pools, Fork-Join, CompletableFuture  
✅ **Real-World Applications** - Chat systems, rate limiters, caches  
✅ **Interview Excellence** - 20+ FAANG questions with detailed solutions  
✅ **System Design** - Scalable multithreaded architectures  
✅ **Best Practices** - Performance, debugging, testing strategies

### **Your Next Steps:**
1. **Practice the code examples** - Run them, modify them, break them!
2. **Build real projects** - Apply these concepts in actual applications
3. **Join the top 1%** - You now have the knowledge to excel in any Java role

Remember: **Multithreading mastery is not just about knowing the syntax - it's about understanding the deeper principles of concurrent programming and applying them to build robust, scalable systems.**

You're now ready to tackle any multithreading challenge that comes your way. Whether it's a FAANG interview, a complex production system, or designing the next generation of concurrent applications - you've got the skills to succeed! 🚀

**Happy coding, and may your threads never deadlock!** 😄
}
}
```

### Error Handling with CompletableFuture
```java
import java.util.concurrent.CompletableFuture;

public class CompletableFutureErrorHandling {
    public static void main(String[] args) throws Exception {
        CompletableFuture<String> future = CompletableFuture
            .supplyAsync(() -> {
                if (Math.random() > 0.5) {
                    throw new RuntimeException("Something went wrong!");
                }
                return "Success!";
            })
            .exceptionally(throwable -> {
                System.out.println("Error occurred: " + throwable.getMessage());
                return "Default value";
            })
            .thenApply(result -> {
                System.out.println("Processing: " + result);
                return result.toUpperCase();
            });
        
        System.out.println("Final result: " + future.get());
        
        // Using handle() for both success and error cases
        CompletableFuture<String> future2 = CompletableFuture
            .supplyAsync(() -> {
                throw new RuntimeException("Always fails!");
            })
            .handle((result, throwable) -> {
                if (throwable != null) {
                    return "Handled error: " + throwable.getMessage();
                }
                return "Success: " + result;
            });
        
        System.out.println("Future2 result: " + future2.get());
    }
}
```

---

## Practical Projects

### Project 1: Web Scraper with Thread Pool
```java
import java.util.concurrent.*;
import java.util.List;
import java.util.Arrays;

class WebScraper {
    private final ExecutorService executor;
    
    public WebScraper(int threadPoolSize) {
        this.executor = Executors.newFixedThreadPool(threadPoolSize);
    }
    
    public void scrapeUrls(List<String> urls) {
        List<Future<String>> futures = urls.stream()
            .map(url -> executor.submit(() -> scrapeUrl(url)))
            .toList();
        
        // Collect results
        for (int i = 0; i < futures.size(); i++) {
            try {
                String result = futures.get(i).get(5, TimeUnit.SECONDS);
                System.out.println("URL " + urls.get(i) + " scraped: " + result);
            } catch (TimeoutException e) {
                System.out.println("URL " + urls.get(i) + " timed out");
            } catch (Exception e) {
                System.out.println("URL " + urls.get(i) + " failed: " + e.getMessage());
            }
        }
    }
    
    private String scrapeUrl(String url) {
        // Simulate web scraping
        try {
            Thread.sleep((long) (Math.random() * 3000 + 1000));
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            return "Interrupted";
        }
        return "Content from " + url;
    }
    
    public void shutdown() {
        executor.shutdown();
    }
    
    public static void main(String[] args) {
        WebScraper scraper = new WebScraper(5);
        
        List<String> urls = Arrays.asList(
            "https://example1.com",
            "https://example2.com",
            "https://example3.com",
            "https://example4.com",
            "https://example5.com"
        );
        
        long startTime = System.currentTimeMillis();
        scraper.scrapeUrls(urls);
        long endTime = System.currentTimeMillis();
        
        System.out.println("Total time: " + (endTime - startTime) + "ms");
        scraper.shutdown();
    }
}
```

### Project 2: Real-time Chat Server
```java
import java.util.concurrent.*;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

class ChatServer {
    private final Set<ChatClient> clients = ConcurrentHashMap.newKeySet();
    private final ExecutorService executor = Executors.newCachedThreadPool();
    
    public void addClient(ChatClient client) {
        clients.add(client);
        System.out.println("Client connected: " + client.getName());
    }
    
    public void removeClient(ChatClient client) {
        clients.remove(client);
        System.out.println("Client disconnected: " + client.getName());
    }
    
    public void broadcastMessage(String message, ChatClient sender) {
        executor.submit(() -> {
            for (ChatClient client : clients) {
                if (client != sender) {
                    client.receiveMessage(message);
                }
            }
        });
    }
    
    public void shutdown() {
        executor.shutdown();
    }
}

class ChatClient {
    private final String name;
    private final ChatServer server;
    private final BlockingQueue<String> messageQueue = new LinkedBlockingQueue<>();
    
    public ChatClient(String name, ChatServer server) {
        this.name = name;
        this.server = server;
        server.addClient(this);
        
        // Start message processing thread
        new Thread(this::processMessages).start();
    }
    
    public void sendMessage(String message) {
        String formattedMessage = name + ": " + message;
        server.broadcastMessage(formattedMessage, this);
    }
    
    public void receiveMessage(String message) {
        messageQueue.offer(message);
    }
    
    private void processMessages() {
        try {
            while (true) {
                String message = messageQueue.take();
                System.out.println("[" + name + " received] " + message);
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }
    
    public String getName() {
        return name;
    }
}

public class ChatServerDemo {
    public static void main(String[] args) throws InterruptedException {
        ChatServer server = new ChatServer();
        
        ChatClient alice = new ChatClient("Alice", server);
        ChatClient bob = new ChatClient("Bob", server);
        ChatClient charlie = new ChatClient("Charlie", server);
        
        // Simulate chat messages
        alice.sendMessage("Hello everyone!");
        Thread.sleep(100);
        bob.sendMessage("Hi Alice!");
        Thread.sleep(100);
        charlie.sendMessage("Hey guys!");
        Thread.sleep(100);
        alice.sendMessage("How's everyone doing?");
        
        Thread.sleep(2000);
        server.shutdown();
    }
}
```

### Project 3: Parallel File Processing
```java
import java.util.concurrent.*;
import java.util.List;
import java.util.ArrayList;
import java.util.concurrent.atomic.AtomicLong;

class FileProcessor {
    private final ExecutorService executor;
    private final AtomicLong totalFilesProcessed = new AtomicLong(0);
    private final AtomicLong totalBytesProcessed = new AtomicLong(0);
    
    public FileProcessor(int threadPoolSize) {
        this.executor = Executors.newFixedThreadPool(threadPoolSize);
    }
    
    public CompletableFuture<Void> processFilesAsync(List<String> filePaths) {
        List<CompletableFuture<FileResult>> futures = filePaths.stream()
            .map(filePath -> CompletableFuture.supplyAsync(() -> processFile(filePath), executor))
            .toList();
        
        return CompletableFuture.allOf(futures.toArray(new CompletableFuture[0]))
            .thenAccept(v -> {
                futures.forEach(future -> {
                    try {
                        FileResult result = future.get();
                        totalFilesProcessed.incrementAndGet();
                        totalBytesProcessed.addAndGet(result.bytesProcessed);
                    } catch (Exception e) {
                        System.err.println("Error processing file: " + e.getMessage());
                    }
                });
            });
    }
    
    private FileResult processFile(String filePath) {
        // Simulate file processing
        try {
            Thread.sleep((long) (Math.random() * 2000 + 500));
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            return new FileResult(filePath, 0, false);
        }
        
        long bytesProcessed = (long) (Math.random() * 10000 + 1000);
        System.out.println("Processed file: " + filePath + " (" + bytesProcessed + " bytes)");
        return new FileResult(filePath, bytesProcessed, true);
    }
    
    public void printStatistics() {
        System.out.println("\n=== Processing Statistics ===");
        System.out.println("Total files processed: " + totalFilesProcessed.get());
        System.out.println("Total bytes processed: " + totalBytesProcessed.get());
    }
    
    public void shutdown() {
        executor.shutdown();
    }
}

class FileResult {
    final String filePath;
    final long bytesProcessed;
    final boolean success;
    
    public FileResult(String filePath, long bytesProcessed, boolean success) {
        this.filePath = filePath;
        this.bytesProcessed = bytesProcessed;
        this.success = success;
    }
}

public class ParallelFileProcessingDemo {
    public static void main(String[] args) throws Exception {
        FileProcessor processor = new FileProcessor(4);
        
        List<String> filePaths = List.of(
            "file1.txt", "file2.txt", "file3.txt", "file4.txt",
            "file5.txt", "file6.txt", "file7.txt", "file8.txt",
            "file9.txt", "file10.txt"
        );
        
        long startTime = System.currentTimeMillis();
        
        CompletableFuture<Void> processingFuture = processor.processFilesAsync(filePaths);
        
        // Do other work while files are being processed
        System.out.println("Files are being processed in parallel...");
        
        // Wait for all files to be processed
        processingFuture.get();
        
        long endTime = System.currentTimeMillis();
        
        processor.printStatistics();
        System.out.println("Total processing time: " + (endTime - startTime) + "ms");
        
        processor.shutdown();
    }
}
```

---

## Best Practices

### 1. Thread Safety Checklist
- **Immutable objects** are always thread-safe
- **Use concurrent collections** instead of synchronizing regular collections
- **Minimize shared mutable state**
- **Use atomic classes** for simple operations
- **Prefer high-level concurrency utilities** over low-level synchronization

### 2. Performance Tips
```java
// Good: Use thread pools instead of creating threads manually
ExecutorService executor = Executors.newFixedThreadPool(4);

// Bad: Creating new threads repeatedly
for (int i = 0; i < 1000; i++) {
    new Thread(() -> doWork()).start(); // Don't do this!
}

// Good: Use CompletableFuture for async operations
CompletableFuture.supplyAsync(() -> fetchData())
    .thenApply(data -> processData(data))
    .thenAccept(result -> saveResult(result));

// Good: Use concurrent collections
ConcurrentHashMap<String, String> map = new ConcurrentHashMap<>();

// Bad: Synchronizing regular collections
Map<String, String> map = Collections.synchronizedMap(new HashMap<>());
```

### 3. Common Pitfalls to Avoid
```java
// PITFALL 1: Calling run() instead of start()
Thread thread = new Thread(() -> doWork());
thread.run(); // Wrong! This runs in current thread
thread.start(); // Correct! This starts new thread

// PITFALL 2: Not handling InterruptedException properly
try {
    Thread.sleep(1000);
} catch (InterruptedException e) {
    // Wrong: Just swallowing the exception
    e.printStackTrace();
}

// Correct: Restore interrupt status
try {
    Thread.sleep(1000);
} catch (InterruptedException e) {
    Thread.currentThread().interrupt();
    return; // or throw RuntimeException
}

// PITFALL 3: Deadlock
synchronized (lock1) {
    synchronized (lock2) {
        // Thread 1 gets lock1, then lock2
    }
}
// Meanwhile, another thread...
synchronized (lock2) {
    synchronized (lock1) {
        // Thread 2 gets lock2, then tries lock1 - DEADLOCK!
    }
}
```

### 4. Testing Multithreaded Code
```java
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;

public class ThreadSafetyTest {
    @Test
    public void testCounterThreadSafety() throws InterruptedException {
        ThreadSafeCounter counter = new ThreadSafeCounter();
        int numThreads = 10;
        int incrementsPerThread = 1000;
        
        ExecutorService executor = Executors.newFixedThreadPool(numThreads);
        CountDownLatch latch = new CountDownLatch(numThreads);
        
        for (int i = 0; i < numThreads; i++) {
            executor.submit(() -> {
                try {
                    for (int j = 0; j < incrementsPerThread; j++) {
                        counter.increment();
                    }
                } finally {
                    latch.countDown();
                }
            });
        }
        
        latch.await(); // Wait for all threads to complete
        
        assertEquals(numThreads * incrementsPerThread, counter.getCount());
        
        executor.shutdown();
    }
}

class ThreadSafeCounter {
    private final AtomicInteger count = new AtomicInteger(0);
    
    public void increment() {
        count.incrementAndGet();
    }
    
    public int getCount() {
        return count.get();
    }
}
```

---

## Advanced Patterns and Techniques

### 1. Fork-Join Framework
```java
import java.util.concurrent.ForkJoinPool;
import java.util.concurrent.RecursiveTask;

class SumTask extends RecursiveTask<Long> {
    private final int[] array;
    private final int start;
    private final int end;
    private static final int THRESHOLD = 10000;
    
    public SumTask(int[] array, int start, int end) {
        this.array = array;
        this.start = start;
        this.end = end;
    }
    
    @Override
    protected Long compute() {
        if (end - start <= THRESHOLD) {
            // Base case: compute directly
            long sum = 0;
            for (int i = start; i < end; i++) {
                sum += array[i];
            }
            return sum;
        } else {
            // Divide and conquer
            int mid = (start + end) / 2;
            SumTask leftTask = new SumTask(array, start, mid);
            SumTask rightTask = new SumTask(array, mid, end);
            
            leftTask.fork(); // Execute asynchronously
            long rightResult = rightTask.compute(); // Execute in current thread
            long leftResult = leftTask.join(); // Wait for result
            
            return leftResult + rightResult;
        }
    }
}

public class ForkJoinExample {
    public static void main(String[] args) {
        int[] array = new int[1000000];
        for (int i = 0; i < array.length; i++) {
            array[i] = i + 1;
        }
        
        ForkJoinPool pool = new ForkJoinPool();
        SumTask task = new SumTask(array, 0, array.length);
        
        long startTime = System.currentTimeMillis();
        long result = pool.invoke(task);
        long endTime = System.currentTimeMillis();
        
        System.out.println("Sum: " + result);
        System.out.println("Time taken: " + (endTime - startTime) + "ms");
        
        pool.shutdown();
    }
}
```

### 2. Reactive Streams Pattern
```java
import java.util.concurrent.*;
import java.util.function.Consumer;

class ReactiveStream<T> {
    private final BlockingQueue<T> queue = new LinkedBlockingQueue<>();
    private final ExecutorService executor = Executors.newCachedThreadPool();
    private volatile boolean closed = false;
    
    public void publish(T item) {
        if (!closed) {
            queue.offer(item);
        }
    }
    
    public void subscribe(Consumer<T> consumer) {
        executor.submit(() -> {
            try {
                while (!closed || !queue.isEmpty()) {
                    T item = queue.poll(100, TimeUnit.MILLISECONDS);
                    if (item != null) {
                        consumer.accept(item);
                    }
                }
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        });
    }
    
    public void close() {
        closed = true;
        executor.shutdown();
    }
}

public class ReactiveStreamExample {
    public static void main(String[] args) throws InterruptedException {
        ReactiveStream<String> stream = new ReactiveStream<>();
        
        // Subscriber 1
        stream.subscribe(message -> 
            System.out.println("Subscriber 1 received: " + message));
        
        // Subscriber 2
        stream.subscribe(message -> 
            System.out.println("Subscriber 2 received: " + message));
        
        // Publisher
        for (int i = 1; i <= 5; i++) {
            stream.publish("Message " + i);
            Thread.sleep(1000);
        }
        
        Thread.sleep(2000);
        stream.close();
    }
}
```

### 3. Custom Thread Pool
```java
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;

public class CustomThreadPoolExample {
    public static void main(String[] args) {
        // Create custom thread pool with detailed configuration
        ThreadPoolExecutor executor = new ThreadPoolExecutor(
            2,                              // Core pool size
            4,                              // Maximum pool size
            60L,                            // Keep alive time
            TimeUnit.SECONDS,               // Time unit
            new LinkedBlockingQueue<>(10),  // Work queue
            new CustomThreadFactory(),      // Thread factory
            new CustomRejectionHandler()    // Rejection handler
        );
        
        // Submit tasks
        for (int i = 1; i <= 20; i++) {
            final int taskId = i;
            try {
                executor.submit(() -> {
                    System.out.println("Task " + taskId + " executing on " + 
                        Thread.currentThread().getName());
                    try {
                        Thread.sleep(2000);
                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                    }
                });
            } catch (RejectedExecutionException e) {
                System.out.println("Task " + taskId + " was rejected");
            }
        }
        
        // Monitor thread pool
        new Thread(() -> {
            while (!executor.isShutdown()) {
                System.out.println("Active threads: " + executor.getActiveCount() + 
                    ", Queue size: " + executor.getQueue().size());
                try {
                    Thread.sleep(1000);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    break;
                }
            }
        }).start();
        
        // Shutdown after some time
        try {
            Thread.sleep(10000);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
        
        executor.shutdown();
    }
}

class CustomThreadFactory implements ThreadFactory {
    private final AtomicInteger threadNumber = new AtomicInteger(1);
    private final String namePrefix = "CustomPool-thread-";
    
    @Override
    public Thread newThread(Runnable r) {
        Thread t = new Thread(r, namePrefix + threadNumber.getAndIncrement());
        t.setDaemon(false);
        t.setPriority(Thread.NORM_PRIORITY);
        return t;
    }
}

class CustomRejectionHandler implements RejectedExecutionHandler {
    @Override
    public void rejectedExecution(Runnable r, ThreadPoolExecutor executor) {
        System.out.println("Task rejected: " + r.toString());
        // Could implement fallback logic here
    }
}
```

### 4. Asynchronous Processing Pipeline
```java
import java.util.concurrent.*;
import java.util.function.Function;

class AsyncPipeline<T, R> {
    private final ExecutorService executor;
    
    public AsyncPipeline(ExecutorService executor) {
        this.executor = executor;
    }
    
    public <U> AsyncPipeline<T, U> then(Function<R, U> function) {
        return new AsyncPipeline<T, U>(executor) {
            @Override
            public CompletableFuture<U> process(T input) {
                return AsyncPipeline.this.process(input)
                    .thenApplyAsync(function, executor);
            }
        };
    }
    
    public CompletableFuture<R> process(T input) {
        // Override in subclasses
        return CompletableFuture.completedFuture(null);
    }
}

public class AsyncPipelineExample {
    public static void main(String[] args) throws Exception {
        ExecutorService executor = Executors.newFixedThreadPool(4);
        
        // Create processing pipeline
        AsyncPipeline<String, String> pipeline = new AsyncPipeline<String, String>(executor) {
            @Override
            public CompletableFuture<String> process(String input) {
                return CompletableFuture.supplyAsync(() -> {
                    System.out.println("Step 1: Parsing " + input);
                    return input.toUpperCase();
                }, executor);
            }
        }
        .then(data -> {
            System.out.println("Step 2: Validating " + data);
            return data + "_VALIDATED";
        })
        .then(data -> {
            System.out.println("Step 3: Processing " + data);
            return data + "_PROCESSED";
        })
        .then(data -> {
            System.out.println("Step 4: Saving " + data);
            return data + "_SAVED";
        });
        
        // Process multiple inputs
        String[] inputs = {"input1", "input2", "input3"};
        
        CompletableFuture<String>[] futures = new CompletableFuture[inputs.length];
        for (int i = 0; i < inputs.length; i++) {
            futures[i] = pipeline.process(inputs[i]);
        }
        
        // Wait for all to complete
        CompletableFuture.allOf(futures).get();
        
        for (CompletableFuture<String> future : futures) {
            System.out.println("Result: " + future.get());
        }
        
        executor.shutdown();
    }
}
```

---

## Performance Monitoring and Debugging

### 1. Thread Monitoring
```java
import java.lang.management.ManagementFactory;
import java.lang.management.ThreadMXBean;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

public class ThreadMonitor {
    private final ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(1);
    private final ThreadMXBean threadBean = ManagementFactory.getThreadMXBean();
    
    public void startMonitoring() {
        scheduler.scheduleAtFixedRate(() -> {
            System.out.println("=== Thread Statistics ===");
            System.out.println("Total threads: " + threadBean.getThreadCount());
            System.out.println("Peak threads: " + threadBean.getPeakThreadCount());
            System.out.println("Daemon threads: " + threadBean.getDaemonThreadCount());
            
            // Detect deadlocks
            long[] deadlockedThreads = threadBean.findDeadlockedThreads();
            if (deadlockedThreads != null) {
                System.out.println("DEADLOCK DETECTED! Threads: " + deadlockedThreads.length);
            }
            
            System.out.println("========================\n");
        }, 0, 5, TimeUnit.SECONDS);
    }
    
    public void stopMonitoring() {
        scheduler.shutdown();
    }
    
    public static void main(String[] args) throws InterruptedException {
        ThreadMonitor monitor = new ThreadMonitor();
        monitor.startMonitoring();
        
        // Create some threads to monitor
        for (int i = 0; i < 5; i++) {
            new Thread(() -> {
                try {
                    Thread.sleep(10000);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
            }).start();
        }
        
        Thread.sleep(15000);
        monitor.stopMonitoring();
    }
}
```

### 2. CompletableFuture Debugging
```java
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;

public class CompletableFutureDebugging {
    public static void main(String[] args) {
        CompletableFuture<String> future = CompletableFuture
            .supplyAsync(() -> {
                System.out.println("Thread: " + Thread.currentThread().getName());
                return "Hello";
            })
            .thenApply(result -> {
                System.out.println("Thread: " + Thread.currentThread().getName());
                if (result.equals("Hello")) {
                    throw new RuntimeException("Simulated error");
                }
                return result + " World";
            })
            .exceptionally(throwable -> {
                System.out.println("Error Thread: " + Thread.currentThread().getName());
                System.out.println("Error: " + throwable.getMessage());
                return "Error handled";
            })
            .whenComplete((result, throwable) -> {
                System.out.println("Completion Thread: " + Thread.currentThread().getName());
                if (throwable != null) {
                    System.out.println("Completed with error: " + throwable.getMessage());
                } else {
                    System.out.println("Completed successfully: " + result);
                }
            });
        
        try {
            String result = future.get();
            System.out.println("Final result: " + result);
        } catch (Exception e) {
            System.out.println("Exception: " + e.getMessage());
        }
    }
}
```

---

## Real-World Integration Examples

### 1. Spring Boot Async Configuration
```java
// Configuration class
@Configuration
@EnableAsync
public class AsyncConfig {
    
    @Bean(name = "taskExecutor")
    public Executor taskExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(4);
        executor.setMaxPoolSize(8);
        executor.setQueueCapacity(100);
        executor.setThreadNamePrefix("AsyncTask-");
        executor.initialize();
        return executor;
    }
}

// Service class
@Service
public class AsyncService {
    
    @Async("taskExecutor")
    public CompletableFuture<String> processAsync(String data) {
        try {
            // Simulate long-running task
            Thread.sleep(2000);
            return CompletableFuture.completedFuture("Processed: " + data);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            return CompletableFuture.failedFuture(e);
        }
    }
}
```

### 2. Database Connection Pool Pattern
```java
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;

class DatabaseConnection {
    private final String connectionId;
    private boolean inUse = false;
    
    public DatabaseConnection(String connectionId) {
        this.connectionId = connectionId;
    }
    
    public void executeQuery(String query) {
        System.out.println("Executing query on " + connectionId + ": " + query);
        try {
            Thread.sleep(1000); // Simulate query execution
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }
    
    public String getConnectionId() {
        return connectionId;
    }
    
    public boolean isInUse() {
        return inUse;
    }
    
    public void setInUse(boolean inUse) {
        this.inUse = inUse;
    }
}

class DatabaseConnectionPool {
    private final BlockingQueue<DatabaseConnection> pool;
    private final int maxPoolSize;
    
    public DatabaseConnectionPool(int maxPoolSize) {
        this.maxPoolSize = maxPoolSize;
        this.pool = new LinkedBlockingQueue<>();
        
        // Initialize pool
        for (int i = 1; i <= maxPoolSize; i++) {
            pool.offer(new DatabaseConnection("Connection-" + i));
        }
    }
    
    public DatabaseConnection getConnection() throws InterruptedException {
        return getConnection(5, TimeUnit.SECONDS);
    }
    
    public DatabaseConnection getConnection(long timeout, TimeUnit unit) throws InterruptedException {
        DatabaseConnection connection = pool.poll(timeout, unit);
        if (connection != null) {
            connection.setInUse(true);
        }
        return connection;
    }
    
    public void releaseConnection(DatabaseConnection connection) {
        if (connection != null && connection.isInUse()) {
            connection.setInUse(false);
            pool.offer(connection);
        }
    }
    
    public int getAvailableConnections() {
        return pool.size();
    }
}

public class DatabaseConnectionPoolExample {
    public static void main(String[] args) throws InterruptedException {
        DatabaseConnectionPool pool = new DatabaseConnectionPool(3);
        
        // Simulate multiple clients
        for (int i = 1; i <= 10; i++) {
            final int clientId = i;
            new Thread(() -> {
                try {
                    DatabaseConnection connection = pool.getConnection();
                    if (connection != null) {
                        System.out.println("Client " + clientId + " got " + connection.getConnectionId());
                        connection.executeQuery("SELECT * FROM users WHERE id = " + clientId);
                        pool.releaseConnection(connection);
                        System.out.println("Client " + clientId + " released " + connection.getConnectionId());
                    } else {
                        System.out.println("Client " + clientId + " couldn't get connection - timeout");
                    }
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
            }).start();
        }
        
        // Monitor pool status
        for (int i = 0; i < 10; i++) {
            Thread.sleep(1000);
            System.out.println("Available connections: " + pool.getAvailableConnections());
        }
    }
}
```

---

## Master-Level Challenge Project

### Distributed Task Scheduler
```java
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicLong;
import java.util.*;

class Task {
    private final String id;
    private final Runnable task;
    private final long scheduleTime;
    private final long period; // For recurring tasks
    private final boolean recurring;
    
    public Task(String id, Runnable task, long scheduleTime) {
        this.id = id;
        this.task = task;
        this.scheduleTime = scheduleTime;
        this.period = 0;
        this.recurring = false;
    }
    
    public Task(String id, Runnable task, long scheduleTime, long period) {
        this.id = id;
        this.task = task;
        this.scheduleTime = scheduleTime;
        this.period = period;
        this.recurring = true;
    }
    
    // Getters
    public String getId() { return id; }
    public Runnable getTask() { return task; }
    public long getScheduleTime() { return scheduleTime; }
    public long getPeriod() { return period; }
    public boolean isRecurring() { return recurring; }
    
    public Task getNextExecution() {
        if (!recurring) return null;
        return new Task(id, task, scheduleTime + period, period);
    }
}

class DistributedTaskScheduler {
    private final PriorityBlockingQueue<Task> taskQueue;
    private final ExecutorService workerPool;
    private final ScheduledExecutorService schedulerThread;
    private final AtomicLong tasksExecuted = new AtomicLong(0);
    private final Map<String, Task> taskRegistry = new ConcurrentHashMap<>();
    private volatile boolean running = true;
    
    public DistributedTaskScheduler(int workerThreads) {
        this.taskQueue = new PriorityBlockingQueue<>(100, 
            Comparator.comparingLong(Task::getScheduleTime));
        this.workerPool = Executors.newFixedThreadPool(workerThreads);
        this.schedulerThread = Executors.newSingleThreadScheduledExecutor();
        
        startScheduler();
    }
    
    private void startScheduler() {
        schedulerThread.scheduleAtFixedRate(() -> {
            try {
                while (running) {
                    Task task = taskQueue.peek();
                    if (task == null) {
                        break;
                    }
                    
                    if (task.getScheduleTime() <= System.currentTimeMillis()) {
                        taskQueue.poll();
                        executeTask(task);
                        
                        // Reschedule if recurring
                        if (task.isRecurring()) {
                            Task nextTask = task.getNextExecution();
                            if (nextTask != null) {
                                taskQueue.offer(nextTask);
                            }
                        }
                    } else {
                        break;
                    }
                }
            } catch (Exception e) {
                System.err.println("Scheduler error: " + e.getMessage());
            }
        }, 0, 100, TimeUnit.MILLISECONDS);
    }
    
    private void executeTask(Task task) {
        CompletableFuture.runAsync(() -> {
            try {
                System.out.println("Executing task: " + task.getId() + 
                    " at " + new Date());
                task.getTask().run();
                tasksExecuted.incrementAndGet();
                System.out.println("Completed task: " + task.getId());
            } catch (Exception e) {
                System.err.println("Task execution failed: " + task.getId() + 
                    " - " + e.getMessage());
            }
        }, workerPool);
    }
    
    public void scheduleTask(String id, Runnable task, long delayMs) {
        Task scheduledTask = new Task(id, task, System.currentTimeMillis() + delayMs);
        taskRegistry.put(id, scheduledTask);
        taskQueue.offer(scheduledTask);
        System.out.println("Scheduled task: " + id + " to run in " + delayMs + "ms");
    }
    
    public void scheduleRecurringTask(String id, Runnable task, long delayMs, long periodMs) {
        Task scheduledTask = new Task(id, task, System.currentTimeMillis() + delayMs, periodMs);
        taskRegistry.put(id, scheduledTask);
        taskQueue.offer(scheduledTask);
        System.out.println("Scheduled recurring task: " + id + 
            " to run in " + delayMs + "ms every " + periodMs + "ms");
    }
    
    public boolean cancelTask(String id) {
        Task task = taskRegistry.remove(id);
        if (task != null) {
            taskQueue.remove(task);
            System.out.println("Cancelled task: " + id);
            return true;
        }
        return false;
    }
    
    public void getStatistics() {
        System.out.println("\n=== Scheduler Statistics ===");
        System.out.println("Tasks executed: " + tasksExecuted.get());
        System.out.println("Pending tasks: " + taskQueue.size());
        System.out.println("Registered tasks: " + taskRegistry.size());
        System.out.println("============================\n");
    }
    
    public void shutdown() {
        running = false;
        schedulerThread.shutdown();
        workerPool.shutdown();
        
        try {
            if (!workerPool.awaitTermination(60, TimeUnit.SECONDS)) {
                workerPool.shutdownNow();
            }
        } catch (InterruptedException e) {
            workerPool.shutdownNow();
            Thread.currentThread().interrupt();
        }
    }
}

public class DistributedTaskSchedulerDemo {
    public static void main(String[] args) throws InterruptedException {
        DistributedTaskScheduler scheduler = new DistributedTaskScheduler(4);
        
        // Schedule one-time tasks
        scheduler.scheduleTask("backup-database", 
            () -> System.out.println("Backing up database..."), 2000);
        
        scheduler.scheduleTask("send-email", 
            () -> System.out.println("Sending email notification..."), 1000);
        
        scheduler.scheduleTask("cleanup-temp", 
            () -> System.out.println("Cleaning up temporary files..."), 3000);
        
        // Schedule recurring tasks
        scheduler.scheduleRecurringTask("health-check", 
            () -> System.out.println("Performing health check..."), 1000, 2000);
        
        scheduler.scheduleRecurringTask("log-rotation", 
            () -> System.out.println("Rotating log files..."), 5000, 10000);
        
        // Monitor for 30 seconds
        for (int i = 0; i < 6; i++) {
            Thread.sleep(5000);
            scheduler.getStatistics();
        }
        
        // Cancel a recurring task
        scheduler.cancelTask("health-check");
        
        // Monitor for another 10 seconds
        Thread.sleep(10000);
        scheduler.getStatistics();
        
        scheduler.shutdown();
        System.out.println("Scheduler shut down.");
    }
}
```

---

## Conclusion

You've now learned Java multithreading from basics to advanced level! Here's what you've mastered:

### ✅ **Foundation Level**
- Understanding threads, processes, and concurrency
- Creating threads using Thread class, Runnable interface, and lambda expressions
- Thread lifecycle and states

### ✅ **Intermediate Level**
- Synchronization with synchronized keyword, locks, and atomic classes
- Producer-consumer pattern with wait/notify
- Thread pools and ExecutorService
- Callable and Future for return values

### ✅ **Advanced Level**
- CompletableFuture for asynchronous programming
- Concurrent collections and thread-safe data structures
- Fork-Join framework for parallel processing
- Custom thread pools and rejection handling

### ✅ **Master Level**
- Reactive programming patterns
- Performance monitoring and debugging
- Real-world integration patterns
- Complex distributed systems

### 🚀 **Next Steps to Become Top 1%**
1. **Practice the projects** - Build each one and experiment with modifications
2. **Study performance** - Learn to profile and optimize multithreaded applications
3. **Explore frameworks** - Learn Spring Boot async, Akka, or RxJava
4. **Read source code** - Study how popular libraries handle concurrency
5. **Build complex systems** - Create your own thread-safe libraries

Remember: Multithreading is about **managing shared resources safely** while **maximizing performance**. Start with simple examples, understand the fundamentals, then gradually build more complex systems.

You're now equipped with the knowledge to handle any multithreading challenge in Java! 🎉

---

## FAANG Interview Questions & Answers 🎯

### EASY LEVEL QUESTIONS

#### Q1: What is the difference between a process and a thread?
**Answer:**
- **Process**: An independent program in execution with its own memory space
- **Thread**: A lightweight sub-process that shares memory with other threads in the same process

```java
// Example: Multiple threads in same process share this static variable
public class ProcessVsThread {
    static int sharedCounter = 0; // All threads can access this
    
    public static void main(String[] args) {
        Thread t1 = new Thread(() -> {
            for (int i = 0; i < 1000; i++) {
                sharedCounter++; // Accessing shared memory
            }
        });
        
        Thread t2 = new Thread(() -> {
            for (int i = 0; i < 1000; i++) {
                sharedCounter++; // Same shared memory
            }
        });
        
        t1.start();
        t2.start();
    }
}
```

**Follow-up:** How does JVM handle threads vs processes?
**Answer:** JVM creates OS-level threads (1:1 mapping). Each JVM process can have multiple threads that share heap memory but have separate stacks.

---

#### Q2: What are the different ways to create a thread in Java?
**Answer:**
```java
// Method 1: Extending Thread class
class thread.MyThread extends Thread {
    public void run() {
        System.out.println("Method 1: " + Thread.currentThread().getName());
    }
}

// Method 2: Implementing Runnable interface (Preferred)
class MyRunnable implements Runnable {
    public void run() {
        System.out.println("Method 2: " + Thread.currentThread().getName());
    }
}

// Method 3: Using Callable interface (returns value)
class MyCallable implements Callable<String> {
    public String call() {
        return "Method 3: " + Thread.currentThread().getName();
    }
}

public class ThreadCreationMethods {
    public static void main(String[] args) throws Exception {
        // Method 1
        new thread.MyThread().start();
        
        // Method 2
        new Thread(new MyRunnable()).start();
        
        // Method 3 (with lambda)
        Thread t3 = new Thread(() -> 
            System.out.println("Method 3 Lambda: " + Thread.currentThread().getName()));
        t3.start();
        
        // Method 4: Using ExecutorService with Callable
        ExecutorService executor = Executors.newSingleThreadExecutor();
        Future<String> future = executor.submit(new MyCallable());
        System.out.println(future.get());
        executor.shutdown();
    }
}
```

**Follow-up:** Why is implementing Runnable preferred over extending Thread?
**Answer:** Because Java supports single inheritance. If you extend Thread, you can't extend any other class. Runnable allows better separation of concerns and flexibility.

---

#### Q3: What is the difference between start() and run() methods?
**Answer:**
```java
public class StartVsRun {
    public static void main(String[] args) {
        Thread thread = new Thread(() -> {
            System.out.println("Thread name: " + Thread.currentThread().getName());
            System.out.println("Is main thread? " + Thread.currentThread().getName().equals("main"));
        });
        
        System.out.println("Calling run() directly:");
        thread.run(); // Executes in current thread (main)
        
        System.out.println("\nCalling start():");
        thread.start(); // Creates new thread and executes
    }
}
```

**Output:**
```
Calling run() directly:
Thread name: main
Is main thread? true

Calling start():
Thread name: Thread-0
Is main thread? false
```

**Follow-up:** What happens if you call start() twice on the same thread?
**Answer:** It throws `IllegalThreadStateException` because a thread can only be started once.

---

### MEDIUM LEVEL QUESTIONS

#### Q4: Explain the Producer-Consumer problem and implement it using wait() and notify().
**Answer:**
```java
import java.util.LinkedList;
import java.util.Queue;

class ProducerConsumer {
    private Queue<Integer> queue = new LinkedList<>();
    private final int CAPACITY = 5;
    private final Object lock = new Object();
    
    public void produce() {
        int value = 0;
        while (true) {
            synchronized (lock) {
                while (queue.size() == CAPACITY) {
                    try {
                        System.out.println("Queue full, producer waiting...");
                        lock.wait(); // Wait until space available
                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                        return;
                    }
                }
                
                queue.offer(++value);
                System.out.println("Produced: " + value + ", Queue size: " + queue.size());
                lock.notifyAll(); // Notify consumers
                
                try {
                    Thread.sleep(1000);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    return;
                }
            }
        }
    }
    
    public void consume() {
        while (true) {
            synchronized (lock) {
                while (queue.isEmpty()) {
                    try {
                        System.out.println("Queue empty, consumer waiting...");
                        lock.wait(); // Wait until item available
                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                        return;
                    }
                }
                
                int value = queue.poll();
                System.out.println("Consumed: " + value + ", Queue size: " + queue.size());
                lock.notifyAll(); // Notify producers
                
                try {
                    Thread.sleep(1500);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    return;
                }
            }
        }
    }
    
    public static void main(String[] args) {
        ProducerConsumer pc = new ProducerConsumer();
        
        Thread producer = new Thread(pc::produce);
        Thread consumer = new Thread(pc::consume);
        
        producer.start();
        consumer.start();
    }
}
```

**Follow-up Questions:**
1. **Why use while loop instead of if for wait condition?**
   **Answer:** To handle spurious wakeups. A thread might wake up even when the condition isn't met.

2. **What's the difference between notify() and notifyAll()?**
   **Answer:** `notify()` wakes up one waiting thread, `notifyAll()` wakes up all waiting threads.

3. **Implement the same using BlockingQueue:**
```java
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;

class ProducerConsumerWithBlockingQueue {
    private BlockingQueue<Integer> queue = new LinkedBlockingQueue<>(5);
    
    public void produce() {
        int value = 0;
        try {
            while (true) {
                queue.put(++value); // Blocks if queue is full
                System.out.println("Produced: " + value);
                Thread.sleep(1000);
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }
    
    public void consume() {
        try {
            while (true) {
                int value = queue.take(); // Blocks if queue is empty
                System.out.println("Consumed: " + value);
                Thread.sleep(1500);
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }
}
```

---

#### Q5: What is a deadlock? How can you prevent it?
**Answer:**
```java
// Example of Deadlock
class DeadlockExample {
    private static Object lock1 = new Object();
    private static Object lock2 = new Object();
    
    public static void main(String[] args) {
        Thread t1 = new Thread(() -> {
            synchronized (lock1) {
                System.out.println("T1: Acquired lock1");
                try { Thread.sleep(100); } catch (InterruptedException e) {}
                
                synchronized (lock2) { // Will wait for T2 to release lock2
                    System.out.println("T1: Acquired lock2");
                }
            }
        });
        
        Thread t2 = new Thread(() -> {
            synchronized (lock2) {
                System.out.println("T2: Acquired lock2");
                try { Thread.sleep(100); } catch (InterruptedException e) {}
                
                synchronized (lock1) { // Will wait for T1 to release lock1
                    System.out.println("T2: Acquired lock1");
                }
            }
        });
        
        t1.start();
        t2.start();
        // This will cause deadlock!
    }
}

// Solution 1: Lock Ordering
class DeadlockPrevention1 {
    private static Object lock1 = new Object();
    private static Object lock2 = new Object();
    
    public static void main(String[] args) {
        Thread t1 = new Thread(() -> {
            synchronized (lock1) { // Always acquire lock1 first
                synchronized (lock2) { // Then lock2
                    System.out.println("T1: Got both locks");
                }
            }
        });
        
        Thread t2 = new Thread(() -> {
            synchronized (lock1) { // Always acquire lock1 first
                synchronized (lock2) { // Then lock2
                    System.out.println("T2: Got both locks");
                }
            }
        });
        
        t1.start();
        t2.start();
    }
}

// Solution 2: Using tryLock with timeout
import java.util.concurrent.locks.ReentrantLock;
import java.util.concurrent.TimeUnit;

class DeadlockPrevention2 {
    private static ReentrantLock lock1 = new ReentrantLock();
    private static ReentrantLock lock2 = new ReentrantLock();
    
    public static void method1() {
        try {
            if (lock1.tryLock(1, TimeUnit.SECONDS)) {
                try {
                    if (lock2.tryLock(1, TimeUnit.SECONDS)) {
                        try {
                            System.out.println("Method1: Got both locks");
                        } finally {
                            lock2.unlock();
                        }
                    } else {
                        System.out.println("Method1: Couldn't get lock2");
                    }
                } finally {
                    lock1.unlock();
                }
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }
    
    public static void method2() {
        try {
            if (lock2.tryLock(1, TimeUnit.SECONDS)) {
                try {
                    if (lock1.tryLock(1, TimeUnit.SECONDS)) {
                        try {
                            System.out.println("Method2: Got both locks");
                        } finally {
                            lock1.unlock();
                        }
                    } else {
                        System.out.println("Method2: Couldn't get lock1");
                    }
                } finally {
                    lock2.unlock();
                }
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }
}
```

**Follow-up:** How can you detect deadlocks in Java?
**Answer:**
```java
import java.lang.management.ManagementFactory;
import java.lang.management.ThreadMXBean;

public class DeadlockDetection {
    public static void detectDeadlock() {
        ThreadMXBean threadBean = ManagementFactory.getThreadMXBean();
        long[] deadlockedThreads = threadBean.findDeadlockedThreads();
        
        if (deadlockedThreads != null) {
            System.out.println("Deadlock detected!");
            for (long threadId : deadlockedThreads) {
                System.out.println("Deadlocked thread ID: " + threadId);
            }
        } else {
            System.out.println("No deadlock detected");
        }
    }
}
```

---

#### Q6: Implement a Thread-Safe Singleton using double-checked locking.
**Answer:**
```java
// Incorrect implementation (not thread-safe)
class UnsafeSingleton {
    private static UnsafeSingleton instance;
    
    private UnsafeSingleton() {}
    
    public static UnsafeSingleton getInstance() {
        if (instance == null) { // Problem: Multiple threads can pass this check
            instance = new UnsafeSingleton();
        }
        return instance;
    }
}

// Correct implementation with double-checked locking
class ThreadSafeSingleton {
    private static volatile ThreadSafeSingleton instance; // volatile is crucial!
    
    private ThreadSafeSingleton() {}
    
    public static ThreadSafeSingleton getInstance() {
        if (instance == null) { // First check (no synchronization)
            synchronized (ThreadSafeSingleton.class) {
                if (instance == null) { // Second check (inside synchronized block)
                    instance = new ThreadSafeSingleton();
                }
            }
        }
        return instance;
    }
}

// Even better: Initialization-on-demand holder pattern
class BestSingleton {
    private BestSingleton() {}
    
    private static class SingletonHolder {
        private static final BestSingleton INSTANCE = new BestSingleton();
    }
    
    public static BestSingleton getInstance() {
        return SingletonHolder.INSTANCE; // Thread-safe by JVM class loading
    }
}

// Using enum (Joshua Bloch's recommendation)
enum EnumSingleton {
    INSTANCE;
    
    public void doSomething() {
        System.out.println("Doing something...");
    }
}

// Test thread safety
public class SingletonTest {
    public static void main(String[] args) {
        for (int i = 0; i < 10; i++) {
            new Thread(() -> {
                ThreadSafeSingleton instance = ThreadSafeSingleton.getInstance();
                System.out.println("Instance: " + instance.hashCode());
            }).start();
        }
    }
}
```

**Follow-up Questions:**
1. **Why is volatile keyword necessary in double-checked locking?**
   **Answer:** Without volatile, due to instruction reordering, another thread might see a partially constructed object.

2. **What are other ways to implement thread-safe singleton?**
   **Answer:** Synchronized method, static initialization, enum, initialization-on-demand holder pattern.

---

### HARD LEVEL QUESTIONS

#### Q7: Implement a custom Thread Pool from scratch.
**Answer:**
```java
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;

class CustomThreadPool {
    private final BlockingQueue<Runnable> taskQueue;
    private final Thread[] workerThreads;
    private volatile boolean isShutdown = false;
    private final AtomicInteger activeThreads = new AtomicInteger(0);
    
    public CustomThreadPool(int poolSize, int queueCapacity) {
        taskQueue = new LinkedBlockingQueue<>(queueCapacity);
        workerThreads = new Thread[poolSize];
        
        // Create and start worker threads
        for (int i = 0; i < poolSize; i++) {
            workerThreads[i] = new Thread(new Worker(), "Worker-" + i);
            workerThreads[i].start();
        }
    }
    
    public void submit(Runnable task) {
        if (isShutdown) {
            throw new IllegalStateException("ThreadPool is shutdown");
        }
        
        try {
            taskQueue.put(task);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new RuntimeException("Interrupted while submitting task", e);
        }
    }
    
    public void shutdown() {
        isShutdown = true;
        // Interrupt all worker threads
        for (Thread worker : workerThreads) {
            worker.interrupt();
        }
    }
    
    public boolean awaitTermination(long timeout, TimeUnit unit) throws InterruptedException {
        long startTime = System.currentTimeMillis();
        long timeoutMillis = unit.toMillis(timeout);
        
        for (Thread worker : workerThreads) {
            long remainingTime = timeoutMillis - (System.currentTimeMillis() - startTime);
            if (remainingTime <= 0) {
                return false;
            }
            worker.join(remainingTime);
            if (worker.isAlive()) {
                return false;
            }
        }
        return true;
    }
    
    public int getActiveThreadCount() {
        return activeThreads.get();
    }
    
    public int getQueueSize() {
        return taskQueue.size();
    }
    
    private class Worker implements Runnable {
        @Override
        public void run() {
            try {
                while (!isShutdown || !taskQueue.isEmpty()) {
                    Runnable task = taskQueue.poll(1, TimeUnit.SECONDS);
                    if (task != null) {
                        activeThreads.incrementAndGet();
                        try {
                            task.run();
                        } catch (Exception e) {
                            System.err.println("Task execution failed: " + e.getMessage());
                        } finally {
                            activeThreads.decrementAndGet();
                        }
                    }
                }
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
            System.out.println(Thread.currentThread().getName() + " terminated");
        }
    }
    
    public static void main(String[] args) throws InterruptedException {
        CustomThreadPool pool = new CustomThreadPool(3, 10);
        
        // Submit tasks
        for (int i = 1; i <= 10; i++) {
            final int taskId = i;
            pool.submit(() -> {
                System.out.println("Executing task " + taskId + " on " + 
                    Thread.currentThread().getName());
                try {
                    Thread.sleep(2000);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
                System.out.println("Completed task " + taskId);
            });
        }
        
        // Monitor progress
        for (int i = 0; i < 5; i++) {
            Thread.sleep(1000);
            System.out.println("Active threads: " + pool.getActiveThreadCount() + 
                ", Queue size: " + pool.getQueueSize());
        }
        
        pool.shutdown();
        boolean terminated = pool.awaitTermination(10, TimeUnit.SECONDS);
        System.out.println("Pool terminated: " + terminated);
    }
}
```

**Follow-up Questions:**
1. **How would you add rejection policies?**
2. **How would you implement core and maximum pool sizes?**
3. **How would you add thread timeout for idle threads?**

---

#### Q8: Design a Rate Limiter using multithreading concepts.
**Answer:**
```java
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;

// Token Bucket Rate Limiter
class TokenBucketRateLimiter {
    private final int capacity;
    private final int tokensPerSecond;
    private final AtomicInteger tokens;
    private final ScheduledExecutorService scheduler;
    
    public TokenBucketRateLimiter(int capacity, int tokensPerSecond) {
        this.capacity = capacity;
        this.tokensPerSecond = tokensPerSecond;
        this.tokens = new AtomicInteger(capacity);
        this.scheduler = Executors.newScheduledThreadPool(1);
        
        // Refill tokens periodically
        scheduler.scheduleAtFixedRate(this::refillTokens, 0, 1, TimeUnit.SECONDS);
    }
    
    private void refillTokens() {
        tokens.updateAndGet(current -> Math.min(capacity, current + tokensPerSecond));
        System.out.println("Refilled tokens. Current: " + tokens.get());
    }
    
    public boolean tryAcquire() {
        return tokens.updateAndGet(current -> current > 0 ? current - 1 : current) > 0;
    }
    
    public boolean tryAcquire(int permits) {
        return tokens.updateAndGet(current -> 
            current >= permits ? current - permits : current) >= permits;
    }
    
    public void acquire() throws InterruptedException {
        while (!tryAcquire()) {
            Thread.sleep(100); // Wait and retry
        }
    }
    
    public void shutdown() {
        scheduler.shutdown();
    }
    
    public int getAvailableTokens() {
        return tokens.get();
    }
}

// Sliding Window Rate Limiter
class SlidingWindowRateLimiter {
    private final int maxRequests;
    private final long windowSizeMs;
    private final ConcurrentLinkedQueue<Long> requestTimes;
    
    public SlidingWindowRateLimiter(int maxRequests, long windowSizeMs) {
        this.maxRequests = maxRequests;
        this.windowSizeMs = windowSizeMs;
        this.requestTimes = new ConcurrentLinkedQueue<>();
    }
    
    public synchronized boolean tryAcquire() {
        long now = System.currentTimeMillis();
        
        // Remove old requests outside the window
        while (!requestTimes.isEmpty() && 
               requestTimes.peek() < now - windowSizeMs) {
            requestTimes.poll();
        }
        
        if (requestTimes.size() < maxRequests) {
            requestTimes.offer(now);
            return true;
        }
        
        return false;
    }
    
    public int getCurrentRequests() {
        long now = System.currentTimeMillis();
        // Clean up old requests
        while (!requestTimes.isEmpty() && 
               requestTimes.peek() < now - windowSizeMs) {
            requestTimes.poll();
        }
        return requestTimes.size();
    }
}

// Rate Limiter Demo
public class RateLimiterDemo {
    public static void main(String[] args) throws InterruptedException {
        System.out.println("=== Token Bucket Rate Limiter Demo ===");
        TokenBucketRateLimiter tokenLimiter = new TokenBucketRateLimiter(5, 2);
        
        // Simulate multiple clients
        for (int i = 0; i < 3; i++) {
            final int clientId = i;
            new Thread(() -> {
                for (int j = 0; j < 10; j++) {
                    if (tokenLimiter.tryAcquire()) {
                        System.out.println("Client " + clientId + " - Request " + j + " ALLOWED");
                    } else {
                        System.out.println("Client " + clientId + " - Request " + j + " DENIED");
                    }
                    
                    try {
                        Thread.sleep(500);
                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                        return;
                    }
                }
            }).start();
        }
        
        Thread.sleep(15000);
        tokenLimiter.shutdown();
        
        System.out.println("\n=== Sliding Window Rate Limiter Demo ===");
        SlidingWindowRateLimiter windowLimiter = new SlidingWindowRateLimiter(3, 5000);
        
        for (int i = 0; i < 10; i++) {
            boolean allowed = windowLimiter.tryAcquire();
            System.out.println("Request " + i + ": " + (allowed ? "ALLOWED" : "DENIED") + 
                " (Current: " + windowLimiter.getCurrentRequests() + "/3)");
            Thread.sleep(1000);
        }
    }
}
```

**Follow-up Questions:**
1. **How would you implement distributed rate limiting?**
2. **What are the pros and cons of token bucket vs sliding window?**
3. **How would you handle burst traffic?**

---

#### Q9: Implement a concurrent HashMap from scratch (simplified version).
**Answer:**
```java
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.locks.ReentrantReadWriteLock;

class ConcurrentHashMapSimple<K, V> {
    private static final int DEFAULT_CAPACITY = 16;
    private static final double LOAD_FACTOR = 0.75;
    
    private volatile Node<K, V>[] table;
    private final AtomicInteger size = new AtomicInteger(0);
    private final ReentrantReadWriteLock[] locks;
    
    @SuppressWarnings("unchecked")
    public ConcurrentHashMapSimple() {
        this.table = (Node<K, V>[]) new Node[DEFAULT_CAPACITY];
        this.locks = new ReentrantReadWriteLock[DEFAULT_CAPACITY];
        for (int i = 0; i < DEFAULT_CAPACITY; i++) {
            locks[i] = new ReentrantReadWriteLock();
        }
    }
    
    static class Node<K, V> {
        final K key;
        volatile V value;
        volatile Node<K, V> next;
        
        Node(K key, V value, Node<K, V> next) {
            this.key = key;
            this.value = value;
            this.next = next;
        }
    }
    
    private int hash(K key) {
        return key == null ? 0 : Math.abs(key.hashCode() % table.length);
    }
    
    public V put(K key, V value) {
        int hash = hash(key);
        ReentrantReadWriteLock lock = locks[hash];
        
        lock.writeLock().lock();
        try {
            Node<K, V> head = table[hash];
            
            // Check if key already exists
            for (Node<K, V> node = head; node != null; node = node.next) {
                if (key == null ? node.key == null : key.equals(node.key)) {
                    V oldValue = node.value;
                    node.value = value;
                    return oldValue;
                }
            }
            
            // Add new node at the beginning
            table[hash] = new Node<>(key, value, head);
            size.incrementAndGet();
            
            // Check if resize is needed (simplified)
            if (size.get() > table.length * LOAD_FACTOR) {
                // In real implementation, you'd resize here
                System.out.println("Resize needed - current size: " + size.get());
            }
            
            return null;
        } finally {
            lock.writeLock().unlock();
        }
    }
    
    public V get(K key) {
        int hash = hash(key);
        ReentrantReadWriteLock lock = locks[hash];
        
        lock.readLock().lock();
        try {
            Node<K, V> head = table[hash];
            
            for (Node<K, V> node = head; node != null; node = node.next) {
                if (key == null ? node.key == null : key.equals(node.key)) {
                    return node.value;
                }
            }
            
            return null;
        } finally {
            lock.readLock().unlock();
        }
    }
    
    public V remove(K key) {
        int hash = hash(key);
        ReentrantReadWriteLock lock = locks[hash];
        
        lock.writeLock().lock();
        try {
            Node<K, V> head = table[hash];
            
            // If first node matches
            if (head != null && (key == null ? head.key == null : key.equals(head.key))) {
                table[hash] = head.next;
                size.decrementAndGet();
                return head.value;
            }
            
            // Search in the rest of the chain
            for (Node<K, V> node = head; node != null && node.next != null; node = node.next) {
                if (key == null ? node.next.key == null : key.equals(node.next.key)) {
                    V value = node.next.value;
                    node.next = node.next.next;
                    size.decrementAndGet();
                    return value;
                }
            }
            
            return null;
        } finally {
            lock.writeLock().unlock();
        }
    }
    
    public int size() {
        return size.get();
    }
    
    public boolean isEmpty() {
        return size.get() == 0;
    }
    
    // For testing
    public void printContents() {
        for (int i = 0; i < table.length; i++) {
            if (table[i] != null) {
                System.out.print("Bucket " + i + ": ");
                for (Node<K, V> node = table[i]; node != null; node = node.next) {
                    System.out.print(node.key + "=" + node.value + " ");
                }
                System.out.println();
            }
        }
    }
    
    public static void main(String[] args) throws InterruptedException {
        ConcurrentHashMapSimple<String, Integer> map = new ConcurrentHashMapSimple<>();
        
        // Test concurrent access
        Thread[] threads = new Thread[10];
        
        for (int i = 0; i < 10; i++) {
            final int threadId = i;
            threads[i] = new Thread(() -> {
                for (int j = 0; j < 100; j++) {
                    String key = "key" + (threadId * 100 + j);
                    map.put(key, threadId * 100 + j);
                    
                    // Occasionally read and remove
                    if (j % 10 == 0) {
                        Integer value = map.get(key);
                        System.out.println("Thread " + threadId + " read: " + key + "=" + value);
                        
                        if (j % 20 == 0) {
                            map.remove(key);
                        }
                    }
                }
            });
        }
        
        // Start all threads
        for (Thread thread : threads) {
            thread.start();
        }
        
        // Wait for completion
        for (Thread thread : threads) {
            thread.join();
        }
        
        System.out.println("Final size: " + map.size());
    }
}
```

**Follow-up Questions:**
1. **How would you implement resizing in a thread-safe manner?**
2. **What are the advantages of segment-based locking vs individual bucket locking?**
3. **How does Java 8's ConcurrentHashMap differ from previous versions?**

---

#### Q10: Design a thread-safe LRU Cache with TTL (Time To Live).
**Answer:**
```java
import java.util.concurrent.*;
import java.util.concurrent.locks.ReentrantReadWriteLock;

class LRUCacheWithTTL<K, V> {
    private final int capacity;
    private final long defaultTTL;
    private final ConcurrentHashMap<K, Node> cache;
    private final ReentrantReadWriteLock lock = new ReentrantReadWriteLock();
    private Node head, tail;
    private final ScheduledExecutorService cleanupExecutor;
    
    class Node {
        K key;
        V value;
        long expirationTime;
        Node prev, next;
        
        Node(K key, V value, long ttl) {
            this.key = key;
            this.value = value;
            this.expirationTime = System.currentTimeMillis() + ttl;
        }
        
        boolean isExpired() {
            return System.currentTimeMillis() > expirationTime;
        }
    }
    
    public LRUCacheWithTTL(int capacity, long defaultTTL) {
        this.capacity = capacity;
        this.defaultTTL = defaultTTL;
        this.cache = new ConcurrentHashMap<>();
        
        // Initialize dummy head and tail
        this.head = new Node(null, null, Long.MAX_VALUE);
        this.tail = new Node(null, null, Long.MAX_VALUE);
        head.next = tail;
        tail.prev = head;
        
        // Schedule cleanup task
        this.cleanupExecutor = Executors.newScheduledThreadPool(1);
        cleanupExecutor.scheduleAtFixedRate(this::cleanup, defaultTTL, defaultTTL, TimeUnit.MILLISECONDS);
    }
    
    public V get(K key) {
        Node node = cache.get(key);
        if (node == null || node.isExpired()) {
            if (node != null) {
                remove(key); // Remove expired node
            }
            return null;
        }
        
        // Move to head (most recently used)
        lock.writeLock().lock();
        try {
            moveToHead(node);
            return node.value;
        } finally {
            lock.writeLock().unlock();
        }
    }
    
    public void put(K key, V value) {
        put(key, value, defaultTTL);
    }
    
    public void put(K key, V value, long ttl) {
        Node existingNode = cache.get(key);
        
        lock.writeLock().lock();
        try {
            if (existingNode != null) {
                // Update existing node
                existingNode.value = value;
                existingNode.expirationTime = System.currentTimeMillis() + ttl;
                moveToHead(existingNode);
            } else {
                // Create new node
                Node newNode = new Node(key, value, ttl);
                
                if (cache.size() >= capacity) {
                    // Remove least recently used (tail.prev)
                    Node lru = tail.prev;
                    removeNode(lru);
                    cache.remove(lru.key);
                }
                
                cache.put(key, newNode);
                addToHead(newNode);
            }
        } finally {
            lock.writeLock().unlock();
        }
    }
    
    public V remove(K key) {
        Node node = cache.remove(key);
        if (node == null) {
            return null;
        }
        
        lock.writeLock().lock();
        try {
            removeNode(node);
            return node.value;
        } finally {
            lock.writeLock().unlock();
        }
    }
    
    private void addToHead(Node node) {
        node.prev = head;
        node.next = head.next;
        head.next.prev = node;
        head.next = node;
    }
    
    private void removeNode(Node node) {
        node.prev.next = node.next;
        node.next.prev = node.prev;
    }
    
    private void moveToHead(Node node) {
        removeNode(node);
        addToHead(node);
    }
    
    private void cleanup() {
        lock.writeLock().lock();
        try {
            cache.entrySet().removeIf(entry -> {
                if (entry.getValue().isExpired()) {
                    removeNode(entry.getValue());
                    return true;
                }
                return false;
            });
        } finally {
            lock.writeLock().unlock();
        }
    }
    
    public int size() {
        return cache.size();
    }
    
    public void shutdown() {
        cleanupExecutor.shutdown();
    }
    
    // For testing and monitoring
    public void printCache() {
        lock.readLock().lock();
        try {
            System.out.println("Cache contents (head to tail):");
            Node current = head.next;
            while (current != tail) {
                long remainingTTL = current.expirationTime - System.currentTimeMillis();
                System.out.println(current.key + "=" + current.value + 
                    " (TTL: " + remainingTTL + "ms)");
                current = current.next;
            }
            System.out.println("Size: " + cache.size() + "/" + capacity);
        } finally {
            lock.readLock().unlock();
        }
    }
    
    public static void main(String[] args) throws InterruptedException {
        LRUCacheWithTTL<String, Integer> cache = new LRUCacheWithTTL<>(3, 5000);
        
        // Test basic operations
        cache.put("A", 1);
        cache.put("B", 2);
        cache.put("C", 3);
        cache.printCache();
        
        // Access A to make it most recent
        System.out.println("\nAccessing A: " + cache.get("A"));
        cache.printCache();
        
        // Add D, should evict B (least recently used)
        cache.put("D", 4);
        System.out.println("\nAfter adding D:");
        cache.printCache();
        
        // Test TTL expiration
        System.out.println("\nAdding E with short TTL (2 seconds):");
        cache.put("E", 5, 2000);
        cache.printCache();
        
        Thread.sleep(3000);
        System.out.println("\nAfter 3 seconds (E should be expired):");
        System.out.println("Getting E: " + cache.get("E"));
        cache.printCache();
        
        // Test concurrent access
        System.out.println("\n=== Concurrent Access Test ===");
        ExecutorService executor = Executors.newFixedThreadPool(5);
        
        for (int i = 0; i < 5; i++) {
            final int threadId = i;
            executor.submit(() -> {
                for (int j = 0; j < 10; j++) {
                    String key = "T" + threadId + "_" + j;
                    cache.put(key, threadId * 10 + j);
                    
                    // Random access pattern
                    if (j % 3 == 0) {
                        cache.get("T" + threadId + "_" + (j - 2));
                    }
                    
                    try {
                        Thread.sleep(100);
                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                        return;
                    }
                }
            });
        }
        
        executor.shutdown();
        executor.awaitTermination(10, TimeUnit.SECONDS);
        
        System.out.println("\nFinal cache state:");
        cache.printCache();
        
        cache.shutdown();
    }
}
```

**Follow-up Questions:**
1. **How would you implement write-through vs write-back caching strategies?**
2. **How would you handle cache warming and cold starts?**
3. **What would be the impact of using WeakReferences for memory-sensitive caching?**

---

#### Q11: Implement a Fork-Join based parallel merge sort.
**Answer:**
```java
import java.util.concurrent.ForkJoinPool;
import java.util.concurrent.RecursiveAction;
import java.util.Arrays;
import java.util.Random;

class ParallelMergeSort extends RecursiveAction {
    private static final int THRESHOLD = 1000; // Switch to sequential below this size
    private final int[] array;
    private final int low, high;
    private final int[] temp;
    
    public ParallelMergeSort(int[] array, int low, int high, int[] temp) {
        this.array = array;
        this.low = low;
        this.high = high;
        this.temp = temp;
    }
    
    @Override
    protected void compute() {
        if (high - low <= THRESHOLD) {
            // Use sequential merge sort for small arrays
            sequentialMergeSort(array, low, high, temp);
        } else {
            // Divide and conquer using fork-join
            int mid = low + (high - low) / 2;
            
            ParallelMergeSort leftTask = new ParallelMergeSort(array, low, mid, temp);
            ParallelMergeSort rightTask = new ParallelMergeSort(array, mid + 1, high, temp);
            
            // Fork left task and compute right task in current thread
            leftTask.fork();
            rightTask.compute();
            leftTask.join(); // Wait for left task to complete
            
            // Merge the sorted halves
            merge(array, low, mid, high, temp);
        }
    }
    
    private void sequentialMergeSort(int[] arr, int low, int high, int[] temp) {
        if (low < high) {
            int mid = low + (high - low) / 2;
            sequentialMergeSort(arr, low, mid, temp);
            sequentialMergeSort(arr, mid + 1, high, temp);
            merge(arr, low, mid, high, temp);
        }
    }
    
    private void merge(int[] arr, int low, int mid, int high, int[] temp) {
        // Copy elements to temp array
        System.arraycopy(arr, low, temp, low, high - low + 1);
        
        int i = low;    // Pointer for left subarray
        int j = mid + 1; // Pointer for right subarray
        int k = low;    // Pointer for merged array
        
        // Merge temp arrays back into arr[low..high]
        while (i <= mid && j <= high) {
            if (temp[i] <= temp[j]) {
                arr[k++] = temp[i++];
            } else {
                arr[k++] = temp[j++];
            }
        }
        
        // Copy remaining elements
        while (i <= mid) {
            arr[k++] = temp[i++];
        }
        while (j <= high) {
            arr[k++] = temp[j++];
        }
    }
    
    public static void parallelMergeSort(int[] array) {
        if (array.length <= 1) return;
        
        int[] temp = new int[array.length];
        ForkJoinPool pool = new ForkJoinPool();
        
        try {
            ParallelMergeSort task = new ParallelMergeSort(array, 0, array.length - 1, temp);
            pool.invoke(task);
        } finally {
            pool.shutdown();
        }
    }
    
    public static void main(String[] args) {
        int[] sizes = {10000, 100000, 1000000};
        
        for (int size : sizes) {
            System.out.println("\n=== Testing with array size: " + size + " ===");
            
            // Generate random array
            int[] array = new Random().ints(size, 0, 1000000).toArray();
            int[] sequentialArray = array.clone();
            int[] parallelArray = array.clone();
            
            // Sequential merge sort
            long startTime = System.currentTimeMillis();
            Arrays.sort(sequentialArray);
            long sequentialTime = System.currentTimeMillis() - startTime;
            
            // Parallel merge sort
            startTime = System.currentTimeMillis();
            parallelMergeSort(parallelArray);
            long parallelTime = System.currentTimeMillis() - startTime;
            
            // Verify results are identical
            boolean identical = Arrays.equals(sequentialArray, parallelArray);
            
            System.out.println("Sequential time: " + sequentialTime + "ms");
            System.out.println("Parallel time: " + parallelTime + "ms");
            System.out.println("Speedup: " + (double) sequentialTime / parallelTime + "x");
            System.out.println("Results identical: " + identical);
            
            // Verify array is sorted
            boolean sorted = true;
            for (int i = 1; i < parallelArray.length; i++) {
                if (parallelArray[i] < parallelArray[i - 1]) {
                    sorted = false;
                    break;
                }
            }
            System.out.println("Array is sorted: " + sorted);
        }
    }
}
```

**Follow-up Questions:**
1. **What's the optimal threshold for switching from parallel to sequential?**
2. **How would you implement parallel quick sort using Fork-Join?**
3. **What are the trade-offs between Fork-Join and ExecutorService?**

---

#### Q12: Design a thread-safe publish-subscribe system.
**Answer:**
```java
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Consumer;

interface Message {
    String getTopic();
    Object getPayload();
    long getTimestamp();
}

class SimpleMessage implements Message {
    private final String topic;
    private final Object payload;
    private final long timestamp;
    
    public SimpleMessage(String topic, Object payload) {
        this.topic = topic;
        this.payload = payload;
        this.timestamp = System.currentTimeMillis();
    }
    
    @Override
    public String getTopic() { return topic; }
    
    @Override
    public Object getPayload() { return payload; }
    
    @Override
    public long getTimestamp() { return timestamp; }
    
    @Override
    public String toString() {
        return "Message{topic='" + topic + "', payload=" + payload + ", timestamp=" + timestamp + "}";
    }
}

interface Subscriber {
    String getId();
    void onMessage(Message message);
    boolean isActive();
}

class SimpleSubscriber implements Subscriber {
    private final String id;
    private final Consumer<Message> messageHandler;
    private final AtomicBoolean active = new AtomicBoolean(true);
    
    public SimpleSubscriber(String id, Consumer<Message> messageHandler) {
        this.id = id;
        this.messageHandler = messageHandler;
    }
    
    @Override
    public String getId() { return id; }
    
    @Override
    public void onMessage(Message message) {
        if (active.get()) {
            try {
                messageHandler.accept(message);
            } catch (Exception e) {
                System.err.println("Error processing message in subscriber " + id + ": " + e.getMessage());
            }
        }
    }
    
    @Override
    public boolean isActive() { return active.get(); }
    
    public void deactivate() { active.set(false); }
}

class PubSubSystem {
    private final ConcurrentHashMap<String, CopyOnWriteArraySet<Subscriber>> topicSubscribers;
    private final ExecutorService messageDeliveryExecutor;
    private final ExecutorService maintenanceExecutor;
    private final BlockingQueue<Message> messageQueue;
    private final AtomicBoolean running = new AtomicBoolean(true);
    private final Thread messageProcessor;
    
    public PubSubSystem(int deliveryThreads) {
        this.topicSubscribers = new ConcurrentHashMap<>();
        this.messageDeliveryExecutor = Executors.newFixedThreadPool(deliveryThreads);
        this.maintenanceExecutor = Executors.newScheduledThreadPool(1);
        this.messageQueue = new LinkedBlockingQueue<>();
        
        // Start message processing thread
        this.messageProcessor = new Thread(this::processMessages, "MessageProcessor");
        this.messageProcessor.start();
        
        // Schedule maintenance task to cleanup inactive subscribers
        ((ScheduledExecutorService) maintenanceExecutor).scheduleAtFixedRate(
            this::cleanupInactiveSubscribers, 30, 30, TimeUnit.SECONDS);
    }
    
    public void subscribe(String topic, Subscriber subscriber) {
        topicSubscribers.computeIfAbsent(topic, k -> new CopyOnWriteArraySet<>()).add(subscriber);
        System.out.println("Subscriber " + subscriber.getId() + " subscribed to topic: " + topic);
    }
    
    public void unsubscribe(String topic, Subscriber subscriber) {
        CopyOnWriteArraySet<Subscriber> subscribers = topicSubscribers.get(topic);
        if (subscribers != null) {
            subscribers.remove(subscriber);
            if (subscribers.isEmpty()) {
                topicSubscribers.remove(topic);
            }
            System.out.println("Subscriber " + subscriber.getId() + " unsubscribed from topic: " + topic);
        }
    }
    
    public void publish(Message message) {
        if (running.get()) {
            try {
                messageQueue.put(message);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                throw new RuntimeException("Interrupted while publishing message", e);
            }
        }
    }
    
    public void publish(String topic, Object payload) {
        publish(new SimpleMessage(topic, payload));
    }
    
    private void processMessages() {
        while (running.get() || !messageQueue.isEmpty()) {
            try {
                Message message = messageQueue.poll(1, TimeUnit.SECONDS);
                if (message != null) {
                    deliverMessage(message);
                }
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                break;
            }
        }
    }
    
    private void deliverMessage(Message message) {
        CopyOnWriteArraySet<Subscriber> subscribers = topicSubscribers.get(message.getTopic());
        if (subscribers != null && !subscribers.isEmpty()) {
            CompletableFuture[] deliveryFutures = subscribers.stream()
                .filter(Subscriber::isActive)
                .map(subscriber -> CompletableFuture.runAsync(
                    () -> {
                        try {
                            subscriber.onMessage(message);
                        } catch (Exception e) {
                            System.err.println("Error delivering message to subscriber " + 
                                subscriber.getId() + ": " + e.getMessage());
                        }
                    }, messageDeliveryExecutor))
                .toArray(CompletableFuture[]::new);
            
            // Optional: Wait for all deliveries (with timeout)
            CompletableFuture.allOf(deliveryFutures)
                .orTimeout(5, TimeUnit.SECONDS)
                .exceptionally(ex -> {
                    System.err.println("Some message deliveries timed out");
                    return null;
                });
        }
    }
    
    private void cleanupInactiveSubscribers() {
        topicSubscribers.forEach((topic, subscribers) -> {
            subscribers.removeIf(subscriber -> !subscriber.isActive());
            if (subscribers.isEmpty()) {
                topicSubscribers.remove(topic);
            }
        });
    }
    
    public int getSubscriberCount(String topic) {
        CopyOnWriteArraySet<Subscriber> subscribers = topicSubscribers.get(topic);
        return subscribers != null ? subscribers.size() : 0;
    }
    
    public int getTotalSubscribers() {
        return topicSubscribers.values().stream()
            .mapToInt(CopyOnWriteArraySet::size)
            .sum();
    }
    
    public int getQueueSize() {
        return messageQueue.size();
    }
    
    public void shutdown() {
        running.set(false);
        
        try {
            messageProcessor.join(5000);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
        
        messageDeliveryExecutor.shutdown();
        maintenanceExecutor.shutdown();
        
        try {
            if (!messageDeliveryExecutor.awaitTermination(10, TimeUnit.SECONDS)) {
                messageDeliveryExecutor.shutdownNow();
            }
            if (!maintenanceExecutor.awaitTermination(5, TimeUnit.SECONDS)) {
                maintenanceExecutor.shutdownNow();
            }
        } catch (InterruptedException e) {
            messageDeliveryExecutor.shutdownNow();
            maintenanceExecutor.shutdownNow();
            Thread.currentThread().interrupt();
        }
    }
    
    public static void main(String[] args) throws InterruptedException {
        PubSubSystem pubSub = new PubSubSystem(4);
        
        // Create subscribers
        Subscriber newsSubscriber = new SimpleSubscriber("NewsReader", 
            message -> System.out.println("📰 News: " + message.getPayload()));
        
        Subscriber sportsSubscriber = new SimpleSubscriber("SportsReader", 
            message -> {
                System.out.println("⚽ Sports: " + message.getPayload());
                // Simulate processing time
                try { Thread.sleep(100); } catch (InterruptedException e) {}
            });
        
        Subscriber techSubscriber = new SimpleSubscriber("TechReader", 
            message -> System.out.println("💻 Tech: " + message.getPayload()));
        
        // Subscribe to topics
        pubSub.subscribe("news", newsSubscriber);
        pubSub.subscribe("sports", sportsSubscriber);
        pubSub.subscribe("tech", techSubscriber);
        pubSub.subscribe("news", techSubscriber); // Tech reader also reads news
        
        // Publish messages
        pubSub.publish("news", "Breaking: Major earthquake hits California");
        pubSub.publish("sports", "World Cup final scheduled for next week");
        pubSub.publish("tech", "New AI breakthrough announced");
        pubSub.publish("news", "Stock market reaches all-time high");
        
        // Simulate concurrent publishers
        ExecutorService publishers = Executors.newFixedThreadPool(3);
        
        for (int i = 0; i < 3; i++) {
            final int publisherId = i;
            publishers.submit(() -> {
                for (int j = 0; j < 5; j++) {
                    String[] topics = {"news", "sports", "tech"};
                    String topic = topics[j % topics.length];
                    pubSub.publish(topic, "Message " + j + " from Publisher " + publisherId);
                    
                    try {
                        Thread.sleep(500);
                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                        return;
                    }
                }
            });
        }
        
        // Monitor system
        for (int i = 0; i < 10; i++) {
            Thread.sleep(1000);
            System.out.println("--- Stats: Total subscribers: " + pubSub.getTotalSubscribers() + 
                ", Queue size: " + pubSub.getQueueSize() + " ---");
        }
        
        publishers.shutdown();
        publishers.awaitTermination(10, TimeUnit.SECONDS);
        
        Thread.sleep(2000); // Let remaining messages process
        pubSub.shutdown();
        
        System.out.println("PubSub system shut down");
    }
}
```

**Follow-up Questions:**
1. **How would you implement message persistence and replay capabilities?**
2. **How would you handle subscriber failures and implement retry mechanisms?**
3. **How would you scale this system across multiple nodes?**

---

## Advanced Interview Scenarios

### Scenario 1: System Design - Design a real-time chat application
**Question:** Design the threading model for a chat application that needs to handle 10,000 concurrent users.

**Answer:**
```java
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicLong;

class ChatServer {
    // Connection handling
    private final ExecutorService connectionAcceptor = Executors.newCachedThreadPool();
    
    // Message processing pipeline
    private final ExecutorService messageProcessor = Executors.newFixedThreadPool(
        Runtime.getRuntime().availableProcessors());
    
    // Message broadcasting
    private final ExecutorService messageBroadcaster = Executors.newFixedThreadPool(
        Runtime.getRuntime().availableProcessors() * 2);
    
    // Room management
    private final ConcurrentHashMap<String, ChatRoom> rooms = new ConcurrentHashMap<>();
    
    // Connection pool for database operations
    private final ExecutorService dbExecutor = Executors.newFixedThreadPool(20);
    
    // Metrics
    private final AtomicLong activeConnections = new AtomicLong(0);
    private final AtomicLong messagesProcessed = new AtomicLong(0);
    
    // Rate limiting per user
    private final ConcurrentHashMap<String, RateLimiter> userRateLimiters = new ConcurrentHashMap<>();
    
    public void handleNewConnection(ClientConnection connection) {
        connectionAcceptor.submit(() -> {
            try {
                activeConnections.incrementAndGet();
                setupClientConnection(connection);
            } catch (Exception e) {
                System.err.println("Error handling connection: " + e.getMessage());
            } finally {
                activeConnections.decrementAndGet();
            }
        });
    }
    
    public void processMessage(ChatMessage message) {
        messageProcessor.submit(() -> {
            try {
                // Rate limiting check
                RateLimiter rateLimiter = userRateLimiters.computeIfAbsent(
                    message.getUserId(), k -> new RateLimiter(100, 60000)); // 100 messages per minute
                
                if (!rateLimiter.tryAcquire()) {
                    // Reject message due to rate limiting
                    return;
                }
                
                // Validate and process message
                if (validateMessage(message)) {
                    // Store in database asynchronously
                    storeMessageAsync(message);
                    
                    // Broadcast to room members
                    broadcastMessage(message);
                    
                    messagesProcessed.incrementAndGet();
                }
            } catch (Exception e) {
                System.err.println("Error processing message: " + e.getMessage());
            }
        });
    }
    
    private void broadcastMessage(ChatMessage message) {
        ChatRoom room = rooms.get(message.getRoomId());
        if (room != null) {
            messageBroadcaster.submit(() -> {
                room.broadcastMessage(message);
            });
        }
    }
    
    // Additional methods...
}

class ChatRoom {
    private final String roomId;
    private final ConcurrentHashMap<String, ClientConnection> members = new ConcurrentHashMap<>();
    private final BlockingQueue<ChatMessage> messageQueue = new LinkedBlockingQueue<>();
    private final Thread messageDispatcher;
    
    public ChatRoom(String roomId) {
        this.roomId = roomId;
        this.messageDispatcher = new Thread(this::dispatchMessages);
        this.messageDispatcher.start();
    }
    
    private void dispatchMessages() {
        while (!Thread.currentThread().isInterrupted()) {
            try {
                ChatMessage message = messageQueue.take();
                
                // Parallel broadcast to all members
                CompletableFuture[] broadcasts = members.values().stream()
                    .map(connection -> CompletableFuture.runAsync(() -> 
                        connection.sendMessage(message)))
                    .toArray(CompletableFuture[]::new);
                
                CompletableFuture.allOf(broadcasts)
                    .orTimeout(1, TimeUnit.SECONDS)
                    .exceptionally(ex -> {
                        System.err.println("Some broadcasts failed in room " + roomId);
                        return null;
                    });
                    
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                break;
            }
        }
    }
    
    public void broadcastMessage(ChatMessage message) {
        messageQueue.offer(message);
    }
}
```

**Key Design Decisions:**
1. **Separate thread pools** for different concerns (connection handling, message processing, broadcasting)
2. **Non-blocking message queues** to prevent one slow client from affecting others
3. **Rate limiting** per user to prevent spam
4. **Async database operations** to avoid blocking message flow
5. **Parallel broadcasting** within rooms using CompletableFuture

---

### Scenario 2: Performance Optimization
**Question:** Your application has a performance bottleneck where multiple threads are contending for a shared resource. How would you diagnose and fix it?

**Answer:**
```java
// Problem: Heavy contention on synchronized block
class ProblematicCounter {
    private long count = 0;
    
    public synchronized void increment() {
        count++; // All threads block here
        doExpensiveOperation(); // Makes contention worse
    }
    
    private void doExpensiveOperation() {
        // Simulate expensive work
        try { Thread.sleep(10); } catch (InterruptedException e) {}
    }
}

// Solution 1: Reduce critical section size
class ImprovedCounter1 {
    private long count = 0;
    
    public void increment() {
        doExpensiveOperation(); // Move outside critical section
        
        synchronized (this) {
            count++; // Minimal critical section
        }
    }
}

// Solution 2: Use lock-free operations
import java.util.concurrent.atomic.AtomicLong;

class ImprovedCounter2 {
    private final AtomicLong count = new AtomicLong(0);
    
    public void increment() {
        doExpensiveOperation();
        count.incrementAndGet(); // Lock-free
    }
}

// Solution 3: Use thread-local aggregation
class ImprovedCounter3 {
    private final ThreadLocal<Long> localCount = ThreadLocal.withInitial(() -> 0L);
    private final AtomicLong globalCount = new AtomicLong(0);
    private final ScheduledExecutorService aggregator = Executors.newScheduledThreadPool(1);
    
    public ImprovedCounter3() {
        // Periodically aggregate thread-local counters
        aggregator.scheduleAtFixedRate(this::aggregateLocalCounts, 1, 1, TimeUnit.SECONDS);
    }
    
    public void increment() {
        localCount.set(localCount.get() + 1);
    }
    
    private void aggregateLocalCounts() {
        // This is a simplified version - real implementation would need
        // to track all ThreadLocal instances
        // For demonstration purposes only
    }
    
    public long getCount() {
        return globalCount.get();
    }
}

// Diagnostic Tools
import java.lang.management.ManagementFactory;
import java.lang.management.ThreadMXBean;
import java.lang.management.ThreadInfo;

class ContentionDiagnostics {
    public static void measureContention() {
        ThreadMXBean threadBean = ManagementFactory.getThreadMXBean();
        
        // Enable thread contention monitoring
        if (threadBean.isThreadContentionMonitoringSupported()) {
            threadBean.setThreadContentionMonitoringEnabled(true);
        }
        
        // Run test with problematic counter
        System.out.println("=== Testing Problematic Counter ===");
        ProblematicCounter counter = new ProblematicCounter();
        ExecutorService executor = Executors.newFixedThreadPool(10);
        
        long startTime = System.currentTimeMillis();
        
        for (int i = 0; i < 10; i++) {
            executor.submit(() -> {
                for (int j = 0; j < 1000; j++) {
                    counter.increment();
                }
            });
        }
        
        executor.shutdown();
        try {
            executor.awaitTermination(30, TimeUnit.SECONDS);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
        
        long endTime = System.currentTimeMillis();
        
        // Print contention statistics
        printContentionStats(threadBean, startTime, endTime, "Problematic Counter");
        
        // Test improved version
        System.out.println("\n=== Testing Improved Counter ===");
        ImprovedCounter2 improvedCounter = new ImprovedCounter2();
        ExecutorService executor2 = Executors.newFixedThreadPool(10);
        
        startTime = System.currentTimeMillis();
        
        for (int i = 0; i < 10; i++) {
            executor2.submit(() -> {
                for (int j = 0; j < 1000; j++) {
                    improvedCounter.increment();
                }
            });
        }
        
        executor2.shutdown();
        try {
            executor2.awaitTermination(30, TimeUnit.SECONDS);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
        
        endTime = System.currentTimeMillis();
        printContentionStats(threadBean, startTime, endTime, "Improved Counter");
    }
    
    private static void printContentionStats(ThreadMXBean threadBean, long startTime, long endTime, String testName) {
        ThreadInfo[] threadInfos = threadBean.getThreadInfo(threadBean.getAllThreadIds());
        long totalBlockedTime = 0;
        long totalWaitedTime = 0;
        long totalBlockedCount = 0;
        long totalWaitedCount = 0;
        
        for (ThreadInfo info : threadInfos) {
            if (info != null && info.getThreadName().contains("pool")) {
                totalBlockedTime += info.getBlockedTime();
                totalWaitedTime += info.getWaitedTime();
                totalBlockedCount += info.getBlockedCount();
                totalWaitedCount += info.getWaitedCount();
            }
        }
        
        System.out.println("--- " + testName + " Results ---");
        System.out.println("Total execution time: " + (endTime - startTime) + "ms");
        System.out.println("Total blocked time: " + totalBlockedTime + "ms");
        System.out.println("Total waited time: " + totalWaitedTime + "ms");
        System.out.println("Total blocked count: " + totalBlockedCount);
        System.out.println("Total waited count: " + totalWaitedCount);
        
        if ((endTime - startTime) > 0) {
            double contentionRatio = (double)(totalBlockedTime + totalWaitedTime) / (endTime - startTime);
            System.out.println("Contention ratio: " + String.format("%.2f", contentionRatio));
        }
    }
    
    // Method to detect deadlocks
    public static void detectDeadlocks() {
        ThreadMXBean threadBean = ManagementFactory.getThreadMXBean();
        long[] deadlockedThreads = threadBean.findDeadlockedThreads();
        
        if (deadlockedThreads != null) {
            System.out.println("DEADLOCK DETECTED!");
            ThreadInfo[] threadInfos = threadBean.getThreadInfo(deadlockedThreads);
            
            for (ThreadInfo threadInfo : threadInfos) {
                System.out.println("Deadlocked thread: " + threadInfo.getThreadName());
                System.out.println("Lock name: " + threadInfo.getLockName());
                System.out.println("Lock owner: " + threadInfo.getLockOwnerName());
                
                StackTraceElement[] stackTrace = threadInfo.getStackTrace();
                for (StackTraceElement element : stackTrace) {
                    System.out.println("\tat " + element);
                }
                System.out.println();
            }
        } else {
            System.out.println("No deadlock detected");
        }
    }
    
    // Method to monitor thread pool health
    public static void monitorThreadPool(ThreadPoolExecutor executor, String poolName) {
        System.out.println("=== " + poolName + " Thread Pool Stats ===");
        System.out.println("Active threads: " + executor.getActiveCount());
        System.out.println("Pool size: " + executor.getPoolSize());
        System.out.println("Core pool size: " + executor.getCorePoolSize());
        System.out.println("Maximum pool size: " + executor.getMaximumPoolSize());
        System.out.println("Queue size: " + executor.getQueue().size());
        System.out.println("Completed tasks: " + executor.getCompletedTaskCount());
        System.out.println("Total tasks: " + executor.getTaskCount());
        System.out.println("Is shutdown: " + executor.isShutdown());
        System.out.println("Is terminated: " + executor.isTerminated());
    }
    
    public static void main(String[] args) {
        measureContention();
        
        System.out.println("\n=== Deadlock Detection Test ===");
        detectDeadlocks();
        
        System.out.println("\n=== Thread Pool Monitoring Test ===");
        ThreadPoolExecutor testPool = new ThreadPoolExecutor(
            2, 4, 60L, TimeUnit.SECONDS,
            new LinkedBlockingQueue<>(10),
            new ThreadFactory() {
                private int threadNumber = 0;
                public Thread newThread(Runnable r) {
                    return new Thread(r, "TestPool-" + (++threadNumber));
                }
            }
        );
        
        // Submit some tasks
        for (int i = 0; i < 5; i++) {
            testPool.submit(() -> {
                try {
                    Thread.sleep(2000);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
            });
        }
        
        monitorThreadPool(testPool, "Test");
        
        testPool.shutdown();
        try {
            testPool.awaitTermination(10, TimeUnit.SECONDS);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
        
        System.out.println("\nAfter shutdown:");
        monitorThreadPool(testPool, "Test");
    }
}
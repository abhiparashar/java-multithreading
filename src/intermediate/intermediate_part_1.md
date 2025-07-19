# Java Concurrency Part 1: Intermediate Level - Advanced Synchronization

## Table of Contents
1. [ReentrantLock and ReadWriteLock](#reentrantlock-and-readwritelock)
2. [wait(), notify(), notifyAll()](#wait-notify-notifyall)
3. [Producer-Consumer Pattern](#producer-consumer-pattern)
4. [Deadlock Detection and Prevention](#deadlock-detection-and-prevention)

---

## ReentrantLock and ReadWriteLock

### Theory

**ReentrantLock** is a more flexible alternative to synchronized blocks, providing the same basic behavior but with extended capabilities.

**Key Features:**
- **Reentrancy**: Same thread can acquire the lock multiple times
- **Interruptibility**: Threads waiting for lock can be interrupted
- **Try-lock mechanism**: Non-blocking lock attempts
- **Fairness policy**: Can ensure FIFO ordering of waiting threads
- **Condition variables**: Multiple wait/notify queues per lock

**ReadWriteLock** allows multiple concurrent readers but exclusive writers, improving performance for read-heavy scenarios.

### Code Examples

#### Basic ReentrantLock Usage
```java
import java.util.concurrent.locks.ReentrantLock;

public class ReentrantLockExample {
    private final ReentrantLock lock = new ReentrantLock();
    private int count = 0;
    
    public void increment() {
        lock.lock();
        try {
            count++;
            // Reentrant - same thread can acquire again
            if (count % 10 == 0) {
                logMilestone(); // This method also uses the same lock
            }
        } finally {
            lock.unlock();
        }
    }
    
    public void logMilestone() {
        lock.lock(); // Reentrant acquisition
        try {
            System.out.println("Milestone reached: " + count);
        } finally {
            lock.unlock();
        }
    }
    
    public boolean tryIncrement() {
        if (lock.tryLock()) {
            try {
                count++;
                return true;
            } finally {
                lock.unlock();
            }
        }
        return false; // Couldn't acquire lock
    }
}
```

#### ReadWriteLock Implementation
```java
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import java.util.*;

public class ReadWriteLockCache<K, V> {
    private final Map<K, V> cache = new HashMap<>();
    private final ReadWriteLock lock = new ReentrantReadWriteLock();
    
    public V get(K key) {
        lock.readLock().lock();
        try {
            return cache.get(key);
        } finally {
            lock.readLock().unlock();
        }
    }
    
    public void put(K key, V value) {
        lock.writeLock().lock();
        try {
            cache.put(key, value);
        } finally {
            lock.writeLock().unlock();
        }
    }
    
    public void clear() {
        lock.writeLock().lock();
        try {
            cache.clear();
        } finally {
            lock.writeLock().unlock();
        }
    }
    
    public int size() {
        lock.readLock().lock();
        try {
            return cache.size();
        } finally {
            lock.readLock().unlock();
        }
    }
}
```

#### Condition Variables with ReentrantLock
```java
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.ReentrantLock;

public class BoundedBuffer<T> {
    private final Object[] items;
    private int count, putIndex, takeIndex;
    private final ReentrantLock lock = new ReentrantLock();
    private final Condition notEmpty = lock.newCondition();
    private final Condition notFull = lock.newCondition();
    
    public BoundedBuffer(int capacity) {
        items = new Object[capacity];
    }
    
    public void put(T item) throws InterruptedException {
        lock.lock();
        try {
            while (count == items.length) {
                notFull.await(); // Wait until buffer is not full
            }
            items[putIndex] = item;
            putIndex = (putIndex + 1) % items.length;
            count++;
            notEmpty.signal(); // Signal that buffer is not empty
        } finally {
            lock.unlock();
        }
    }
    
    @SuppressWarnings("unchecked")
    public T take() throws InterruptedException {
        lock.lock();
        try {
            while (count == 0) {
                notEmpty.await(); // Wait until buffer is not empty
            }
            T item = (T) items[takeIndex];
            items[takeIndex] = null;
            takeIndex = (takeIndex + 1) % items.length;
            count--;
            notFull.signal(); // Signal that buffer is not full
            return item;
        } finally {
            lock.unlock();
        }
    }
}
```

---

## wait(), notify(), notifyAll()

### Theory

These methods are part of Java's intrinsic locking mechanism and must be called within synchronized blocks/methods.

**Key Concepts:**
- **wait()**: Releases lock and waits until notified
- **notify()**: Wakes up one waiting thread
- **notifyAll()**: Wakes up all waiting threads
- **Spurious wakeups**: wait() can return without notification

### Code Examples

#### Basic wait/notify Pattern
```java
public class WaitNotifyExample {
    private boolean condition = false;
    private final Object lock = new Object();
    
    public void waitForCondition() throws InterruptedException {
        synchronized(lock) {
            while (!condition) { // Always use while loop for spurious wakeups
                System.out.println("Waiting for condition...");
                lock.wait();
            }
            System.out.println("Condition met!");
        }
    }
    
    public void setCondition() {
        synchronized(lock) {
            condition = true;
            lock.notifyAll(); // Wake up all waiting threads
        }
    }
}
```

#### Producer-Consumer with wait/notify
```java
import java.util.*;

public class ProducerConsumerWaitNotify {
    private final Queue<Integer> queue = new LinkedList<>();
    private final int capacity = 10;
    private final Object lock = new Object();
    
    public void produce(int item) throws InterruptedException {
        synchronized(lock) {
            while (queue.size() == capacity) {
                System.out.println("Queue full, producer waiting...");
                lock.wait();
            }
            queue.offer(item);
            System.out.println("Produced: " + item);
            lock.notifyAll(); // Notify consumers
        }
    }
    
    public int consume() throws InterruptedException {
        synchronized(lock) {
            while (queue.isEmpty()) {
                System.out.println("Queue empty, consumer waiting...");
                lock.wait();
            }
            int item = queue.poll();
            System.out.println("Consumed: " + item);
            lock.notifyAll(); // Notify producers
            return item;
        }
    }
}
```

---

## Producer-Consumer Pattern

### Theory

The Producer-Consumer pattern is a classic concurrency design pattern where:
- **Producers** generate data and put it in a buffer
- **Consumers** take data from the buffer and process it
- **Buffer** acts as a synchronization point between producers and consumers

**Benefits:**
- Decouples producers from consumers
- Allows different processing rates
- Provides natural load balancing

### Code Examples

#### Thread-Safe Producer-Consumer with BlockingQueue
```java
import java.util.concurrent.*;

public class ProducerConsumerBlockingQueue {
    private final BlockingQueue<String> queue = new ArrayBlockingQueue<>(10);
    
    class Producer implements Runnable {
        @Override
        public void run() {
            try {
                for (int i = 0; i < 20; i++) {
                    String item = "Item-" + i;
                    queue.put(item); // Blocks if queue is full
                    System.out.println("Produced: " + item);
                    Thread.sleep(100);
                }
                queue.put("POISON_PILL"); // Signal end of production
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }
    }
    
    class Consumer implements Runnable {
        @Override
        public void run() {
            try {
                while (true) {
                    String item = queue.take(); // Blocks if queue is empty
                    if ("POISON_PILL".equals(item)) {
                        queue.put(item); // Put back for other consumers
                        break;
                    }
                    System.out.println("Consumed: " + item);
                    Thread.sleep(200);
                }
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }
    }
    
    public void start() {
        ExecutorService executor = Executors.newFixedThreadPool(4);
        
        // Start 1 producer and 3 consumers
        executor.submit(new Producer());
        executor.submit(new Consumer());
        executor.submit(new Consumer());
        executor.submit(new Consumer());
        
        executor.shutdown();
    }
}
```

#### Advanced Producer-Consumer with Priority
```java
import java.util.concurrent.*;
import java.util.Comparator;

public class PriorityProducerConsumer {
    private final PriorityBlockingQueue<Task> queue = 
        new PriorityBlockingQueue<>(10, Comparator.comparing(Task::getPriority).reversed());
    
    static class Task {
        private final String name;
        private final int priority;
        
        public Task(String name, int priority) {
            this.name = name;
            this.priority = priority;
        }
        
        public int getPriority() { return priority; }
        public String getName() { return name; }
        
        @Override
        public String toString() {
            return name + "(Priority: " + priority + ")";
        }
    }
    
    class PriorityProducer implements Runnable {
        @Override
        public void run() {
            try {
                queue.put(new Task("Low Priority Task", 1));
                queue.put(new Task("High Priority Task", 10));
                queue.put(new Task("Medium Priority Task", 5));
                queue.put(new Task("Critical Task", 20));
            } catch (Exception e) {
                Thread.currentThread().interrupt();
            }
        }
    }
    
    class PriorityConsumer implements Runnable {
        @Override
        public void run() {
            try {
                while (!Thread.currentThread().isInterrupted()) {
                    Task task = queue.take();
                    System.out.println("Processing: " + task);
                    Thread.sleep(1000); // Simulate processing time
                }
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }
    }
}
```

---

## Deadlock Detection and Prevention

### Theory

**Deadlock** occurs when two or more threads are blocked forever, waiting for each other to release resources.

**Four Necessary Conditions for Deadlock:**
1. **Mutual Exclusion**: Resources cannot be shared
2. **Hold and Wait**: Thread holds resources while waiting for others
3. **No Preemption**: Resources cannot be forcibly taken
4. **Circular Wait**: Circular chain of threads waiting for resources

**Prevention Strategies:**
- **Lock Ordering**: Always acquire locks in the same order
- **Lock Timeout**: Use tryLock with timeout
- **Lock-free algorithms**: Use atomic operations
- **Detect and recover**: Monitor for deadlocks and break them

### Code Examples

#### Deadlock Example
```java
public class DeadlockExample {
    private final Object lock1 = new Object();
    private final Object lock2 = new Object();
    
    public void method1() {
        synchronized(lock1) {
            System.out.println("Thread " + Thread.currentThread().getId() + " acquired lock1");
            try { Thread.sleep(100); } catch (InterruptedException e) {}
            
            synchronized(lock2) { // This can cause deadlock
                System.out.println("Thread " + Thread.currentThread().getId() + " acquired lock2");
            }
        }
    }
    
    public void method2() {
        synchronized(lock2) {
            System.out.println("Thread " + Thread.currentThread().getId() + " acquired lock2");
            try { Thread.sleep(100); } catch (InterruptedException e) {}
            
            synchronized(lock1) { // This can cause deadlock
                System.out.println("Thread " + Thread.currentThread().getId() + " acquired lock1");
            }
        }
    }
}
```

#### Deadlock Prevention - Lock Ordering
```java
public class DeadlockPrevention {
    private final Object lock1 = new Object();
    private final Object lock2 = new Object();
    
    // Assign consistent ordering to locks
    private final Object firstLock = System.identityHashCode(lock1) < System.identityHashCode(lock2) ? lock1 : lock2;
    private final Object secondLock = System.identityHashCode(lock1) < System.identityHashCode(lock2) ? lock2 : lock1;
    
    public void safeMethod1() {
        synchronized(firstLock) {
            synchronized(secondLock) {
                // Safe - always acquire locks in same order
                System.out.println("Method1 executed safely");
            }
        }
    }
    
    public void safeMethod2() {
        synchronized(firstLock) {
            synchronized(secondLock) {
                // Safe - always acquire locks in same order
                System.out.println("Method2 executed safely");
            }
        }
    }
}
```

#### Deadlock Prevention - Timeout Approach
```java
import java.util.concurrent.locks.ReentrantLock;
import java.util.concurrent.TimeUnit;

public class TimeoutDeadlockPrevention {
    private final ReentrantLock lock1 = new ReentrantLock();
    private final ReentrantLock lock2 = new ReentrantLock();
    
    public boolean safeTransfer() throws InterruptedException {
        boolean acquired1 = false, acquired2 = false;
        
        try {
            acquired1 = lock1.tryLock(1, TimeUnit.SECONDS);
            if (!acquired1) return false;
            
            acquired2 = lock2.tryLock(1, TimeUnit.SECONDS);
            if (!acquired2) return false;
            
            // Perform the operation
            System.out.println("Transfer completed successfully");
            return true;
            
        } finally {
            if (acquired2) lock2.unlock();
            if (acquired1) lock1.unlock();
        }
    }
}
```

#### Deadlock Detection Utility
```java
import java.lang.management.ManagementFactory;
import java.lang.management.ThreadMXBean;
import java.lang.management.ThreadInfo;

public class DeadlockDetector {
    private final ThreadMXBean threadBean = ManagementFactory.getThreadMXBean();
    
    public void detectDeadlock() {
        long[] deadlocked = threadBean.findDeadlockedThreads();
        
        if (deadlocked != null) {
            ThreadInfo[] threadInfos = threadBean.getThreadInfo(deadlocked);
            System.out.println("Deadlock detected!");
            
            for (ThreadInfo threadInfo : threadInfos) {
                System.out.println("Thread: " + threadInfo.getThreadName());
                System.out.println("Locked on: " + threadInfo.getLockName());
                System.out.println("Owned by: " + threadInfo.getLockOwnerName());
            }
        } else {
            System.out.println("No deadlock detected");
        }
    }
    
    public void startMonitoring() {
        ScheduledExecutorService executor = Executors.newScheduledThreadPool(1);
        executor.scheduleAtFixedRate(this::detectDeadlock, 0, 5, TimeUnit.SECONDS);
    }
}
```

---

## Practice Questions

### Conceptual Questions
1. **What are the main advantages of ReentrantLock over synchronized blocks?**
2. **When would you use ReadWriteLock instead of ReentrantLock?**
3. **Explain the difference between notify() and notifyAll(). When would you use each?**
4. **What is a spurious wakeup and why should you always use while loops with wait()?**
5. **Describe the four conditions necessary for deadlock to occur.**

### Coding Challenges
1. **Implement a thread-safe counter using ReentrantLock with fair/unfair policies**
2. **Create a ReadWriteLock-based cache that supports cache expiration**
3. **Build a multi-producer, multi-consumer system using wait/notify**
4. **Design a deadlock-free bank transfer system**
5. **Implement a priority-based producer-consumer pattern**

### Follow-up Questions
1. **How does ReentrantLock handle thread interruption differently from synchronized?**
2. **What happens if you call notify() without holding the monitor lock?**
3. **How can you implement a timeout mechanism for producer-consumer operations?**
4. **What strategies can you use to avoid deadlocks in complex systems with multiple locks?**
5. **How does the fairness parameter in ReentrantLock affect performance and behavior?**

---

## Key Takeaways
- ReentrantLock provides more flexibility than synchronized but requires explicit unlock
- ReadWriteLock optimizes for read-heavy scenarios
- Always use while loops with wait() to handle spurious wakeups
- Producer-Consumer pattern decouples data generation from consumption
- Deadlock prevention is easier than deadlock recovery
- Lock ordering is the most effective deadlock prevention strategy
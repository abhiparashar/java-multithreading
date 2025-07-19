# Java Concurrency Part 5C: Advanced Level - Phaser & StampedLock

## Table of Contents
1. [Phaser for Multi-Phase Coordination](#phaser-for-multi-phase-coordination)
2. [StampedLock with Optimistic Reading](#stampedlock-with-optimistic-reading)

---

## Phaser for Multi-Phase Coordination

### Theory

**Phaser** is a more flexible alternative to CountDownLatch and CyclicBarrier, supporting dynamic registration and multi-phase synchronization.

**Key Features:**
- **Dynamic registration**: Parties can register/deregister dynamically
- **Multi-phase**: Supports multiple phases of execution
- **Hierarchical**: Can be arranged in trees for scalability
- **Termination**: Automatically terminates when no parties remain

### Code Examples

#### Complete Phaser Implementation
```java
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.List;
import java.util.ArrayList;

public class PhaserDemo {
    
    // Example 1: Basic multi-phase coordination
    public void demonstrateBasicPhaser() throws InterruptedException {
        System.out.println("=== Basic Phaser Demo ===");
        
        int numWorkers = 4;
        Phaser phaser = new Phaser(numWorkers);
        
        ExecutorService executor = Executors.newFixedThreadPool(numWorkers);
        
        for (int i = 0; i < numWorkers; i++) {
            final int workerId = i;
            executor.submit(() -> {
                try {
                    // Phase 1: Initialization
                    System.out.println("Worker " + workerId + " initializing...");
                    Thread.sleep(100 + workerId * 50);
                    System.out.println("Worker " + workerId + " finished initialization");
                    phaser.arriveAndAwaitAdvance(); // Wait for all workers to finish phase 1
                    
                    // Phase 2: Processing
                    System.out.println("Worker " + workerId + " processing...");
                    Thread.sleep(200 + workerId * 30);
                    System.out.println("Worker " + workerId + " finished processing");
                    phaser.arriveAndAwaitAdvance(); // Wait for all workers to finish phase 2
                    
                    // Phase 3: Cleanup
                    System.out.println("Worker " + workerId + " cleaning up...");
                    Thread.sleep(50);
                    System.out.println("Worker " + workerId + " finished cleanup");
                    phaser.arriveAndDeregister(); // Deregister from phaser
                    
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
            });
        }
        
        executor.shutdown();
        executor.awaitTermination(5, TimeUnit.SECONDS);
        
        System.out.println("All workers completed. Final phase: " + phaser.getPhase());
    }
    
    // Example 2: Dynamic phaser with variable participants
    public void demonstrateDynamicPhaser() throws InterruptedException {
        System.out.println("\n=== Dynamic Phaser Demo ===");
        
        Phaser phaser = new Phaser(1); // Start with main thread registered
        
        // Dynamically add workers
        for (int i = 0; i < 3; i++) {
            final int workerId = i;
            
            new Thread(() -> {
                phaser.register(); // Dynamically register
                
                for (int phase = 0; phase < 3; phase++) {
                    System.out.println("Worker " + workerId + " working on phase " + phase);
                    try {
                        Thread.sleep(100);
                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                        return;
                    }
                    
                    phaser.arriveAndAwaitAdvance();
                }
                
                phaser.arriveAndDeregister(); // Deregister when done
                System.out.println("Worker " + workerId + " completed all phases");
            }).start();
            
            Thread.sleep(50); // Stagger worker starts
        }
        
        // Main thread participates in synchronization
        for (int phase = 0; phase < 3; phase++) {
            System.out.println("Main thread: Starting phase " + phase);
            phaser.arriveAndAwaitAdvance();
            System.out.println("Main thread: Phase " + phase + " completed by all");
        }
        
        phaser.arriveAndDeregister(); // Main thread deregisters
    }
    
    // Example 3: Hierarchical phaser for large-scale coordination
    public void demonstrateHierarchicalPhaser() throws InterruptedException {
        System.out.println("\n=== Hierarchical Phaser Demo ===");
        
        // Root phaser
        Phaser root = new Phaser();
        
        // Child phasers for different groups
        Phaser group1 = new Phaser(root, 3); // 3 workers in group 1
        Phaser group2 = new Phaser(root, 2); // 2 workers in group 2
        
        ExecutorService executor = Executors.newFixedThreadPool(5);
        CountDownLatch allDone = new CountDownLatch(5);
        
        // Group 1 workers
        for (int i = 0; i < 3; i++) {
            final int workerId = i;
            executor.submit(() -> {
                try {
                    for (int phase = 0; phase < 2; phase++) {
                        System.out.println("Group1 Worker " + workerId + " phase " + phase);
                        Thread.sleep(100);
                        group1.arriveAndAwaitAdvance();
                    }
                    group1.arriveAndDeregister();
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                } finally {
                    allDone.countDown();
                }
            });
        }
        
        // Group 2 workers
        for (int i = 0; i < 2; i++) {
            final int workerId = i;
            executor.submit(() -> {
                try {
                    for (int phase = 0; phase < 2; phase++) {
                        System.out.println("Group2 Worker " + workerId + " phase " + phase);
                        Thread.sleep(150);
                        group2.arriveAndAwaitAdvance();
                    }
                    group2.arriveAndDeregister();
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                } finally {
                    allDone.countDown();
                }
            });
        }
        
        allDone.await();
        executor.shutdown();
        
        System.out.println("Hierarchical coordination completed");
    }
    
    // Example 4: Phaser with custom termination condition
    static class CustomPhaser extends Phaser {
        private final int maxPhases;
        
        public CustomPhaser(int parties, int maxPhases) {
            super(parties);
            this.maxPhases = maxPhases;
        }
        
        @Override
        protected boolean onAdvance(int phase, int registeredParties) {
            System.out.println("Phase " + phase + " completed with " + registeredParties + " parties");
            return phase >= maxPhases - 1 || registeredParties == 0;
        }
    }
    
    public void demonstrateCustomPhaser() throws InterruptedException {
        System.out.println("\n=== Custom Phaser Demo ===");
        
        int maxPhases = 3;
        CustomPhaser phaser = new CustomPhaser(2, maxPhases);
        
        Thread worker1 = new Thread(() -> {
            for (int i = 0; i < maxPhases; i++) {
                System.out.println("Worker1 executing phase " + i);
                try {
                    Thread.sleep(100);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    return;
                }
                phaser.arriveAndAwaitAdvance();
            }
            phaser.arriveAndDeregister();
        });
        
        Thread worker2 = new Thread(() -> {
            for (int i = 0; i < maxPhases; i++) {
                System.out.println("Worker2 executing phase " + i);
                try {
                    Thread.sleep(150);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    return;
                }
                phaser.arriveAndAwaitAdvance();
            }
            phaser.arriveAndDeregister();
        });
        
        worker1.start();
        worker2.start();
        
        worker1.join();
        worker2.join();
        
        System.out.println("Custom phaser completed, terminated: " + phaser.isTerminated());
    }
    
    // Example 5: Real-world data processing pipeline
    public void demonstrateDataProcessingPipeline() throws InterruptedException {
        System.out.println("\n=== Data Processing Pipeline Demo ===");
        
        int numWorkers = 4;
        int numPhases = 3;
        Phaser phaser = new Phaser(numWorkers);
        
        // Simulated data batches
        List<String>[] dataBatches = new List[numWorkers];
        for (int i = 0; i < numWorkers; i++) {
            dataBatches[i] = new ArrayList<>();
            for (int j = 0; j < 5; j++) {
                dataBatches[i].add("Data-Worker" + i + "-Item" + j);
            }
        }
        
        ExecutorService executor = Executors.newFixedThreadPool(numWorkers);
        
        for (int i = 0; i < numWorkers; i++) {
            final int workerId = i;
            final List<String> workerData = dataBatches[i];
            
            executor.submit(() -> {
                try {
                    List<String> processedData = new ArrayList<>(workerData);
                    
                    for (int phase = 0; phase < numPhases; phase++) {
                        // Process data for current phase
                        for (int j = 0; j < processedData.size(); j++) {
                            processedData.set(j, processedData.get(j) + "-Phase" + phase);
                        }
                        
                        System.out.println("Worker " + workerId + " completed phase " + phase + 
                                         ", processed " + processedData.size() + " items");
                        
                        // Wait for all workers to complete this phase
                        phaser.arriveAndAwaitAdvance();
                        
                        // Phase-specific processing
                        if (phase == 1) {
                            // Simulate filtering in phase 1
                            processedData = processedData.subList(0, Math.max(1, processedData.size() / 2));
                            System.out.println("Worker " + workerId + " filtered data, remaining: " + 
                                             processedData.size());
                        }
                    }
                    
                    System.out.println("Worker " + workerId + " final results: " + processedData);
                    
                } catch (Exception e) {
                    System.err.println("Worker " + workerId + " failed: " + e.getMessage());
                } finally {
                    phaser.arriveAndDeregister();
                }
            });
        }
        
        executor.shutdown();
        executor.awaitTermination(10, TimeUnit.SECONDS);
        
        System.out.println("Data processing pipeline completed");
    }
}
```

---

## StampedLock with Optimistic Reading

### Theory

**StampedLock** is a capability-based lock with three modes: writing, reading, and optimistic reading. It's designed for scenarios where reads greatly outnumber writes.

**Key Features:**
- **Optimistic reading**: Non-blocking reads that validate later
- **Lock conversion**: Can upgrade/downgrade between lock modes
- **Better performance**: Often outperforms ReadWriteLock for read-heavy scenarios
- **No reentrance**: Not reentrant like other locks

### Code Examples

#### Complete StampedLock Implementation
```java
import java.util.concurrent.locks.StampedLock;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

public class StampedLockDemo {
    
    // Example 1: Basic optimistic reading
    static class Point {
        private final StampedLock sl = new StampedLock();
        private double x, y;
        
        public void write(double newX, double newY) {
            long stamp = sl.writeLock();
            try {
                x = newX;
                y = newY;
            } finally {
                sl.unlockWrite(stamp);
            }
        }
        
        public double distanceFromOrigin() {
            long stamp = sl.tryOptimisticRead(); // Try optimistic read
            double curX = x, curY = y;
            
            if (!sl.validate(stamp)) { // Check if no writes occurred
                // Optimistic read failed, fall back to read lock
                stamp = sl.readLock();
                try {
                    curX = x;
                    curY = y;
                } finally {
                    sl.unlockRead(stamp);
                }
            }
            
            return Math.sqrt(curX * curX + curY * curY);
        }
        
        public double distanceFromOriginWithUpgrade() {
            long stamp = sl.readLock();
            try {
                double curX = x, curY = y;
                
                // Try to upgrade to write lock if we need to modify
                if (curX == 0.0 && curY == 0.0) {
                    long writeStamp = sl.tryConvertToWriteLock(stamp);
                    if (writeStamp != 0L) {
                        stamp = writeStamp;
                        x = 1.0; // Modify under write lock
                        y = 1.0;
                        curX = x;
                        curY = y;
                    } else {
                        sl.unlockRead(stamp);
                        stamp = sl.writeLock();
                        x = 1.0;
                        y = 1.0;
                        curX = x;
                        curY = y;
                    }
                }
                
                return Math.sqrt(curX * curX + curY * curY);
            } finally {
                sl.unlock(stamp);
            }
        }
    }
    
    public void demonstrateStampedLock() throws InterruptedException {
        System.out.println("=== StampedLock Demo ===");
        
        Point point = new Point();
        ExecutorService executor = Executors.newFixedThreadPool(6);
        CountDownLatch latch = new CountDownLatch(6);
        
        // Writers
        for (int i = 0; i < 2; i++) {
            final int writerId = i;
            executor.submit(() -> {
                try {
                    for (int j = 0; j < 10; j++) {
                        point.write(writerId + j, writerId + j + 1);
                        System.out.println("Writer " + writerId + " wrote (" + 
                                         (writerId + j) + ", " + (writerId + j + 1) + ")");
                        Thread.sleep(50);
                    }
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                } finally {
                    latch.countDown();
                }
            });
        }
        
        // Readers using optimistic reading
        for (int i = 0; i < 4; i++) {
            final int readerId = i;
            executor.submit(() -> {
                try {
                    for (int j = 0; j < 20; j++) {
                        double distance = point.distanceFromOrigin();
                        System.out.println("Reader " + readerId + " distance: " + distance);
                        Thread.sleep(25);
                    }
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                } finally {
                    latch.countDown();
                }
            });
        }
        
        latch.await();
        executor.shutdown();
        
        System.out.println("StampedLock demo completed");
    }
    
    // Example 2: Performance comparison between StampedLock and ReadWriteLock
    static class PerformanceComparison {
        private final StampedLock stampedLock = new StampedLock();
        private final java.util.concurrent.locks.ReadWriteLock rwLock = 
            new java.util.concurrent.locks.ReentrantReadWriteLock();
        
        private volatile int data = 0;
        
        // StampedLock version
        public int readWithStampedLock() {
            long stamp = stampedLock.tryOptimisticRead();
            int currentData = data;
            
            if (!stampedLock.validate(stamp)) {
                stamp = stampedLock.readLock();
                try {
                    currentData = data;
                } finally {
                    stampedLock.unlockRead(stamp);
                }
            }
            
            return currentData;
        }
        
        public void writeWithStampedLock(int newData) {
            long stamp = stampedLock.writeLock();
            try {
                data = newData;
            } finally {
                stampedLock.unlockWrite(stamp);
            }
        }
        
        // ReadWriteLock version
        public int readWithReadWriteLock() {
            rwLock.readLock().lock();
            try {
                return data;
            } finally {
                rwLock.readLock().unlock();
            }
        }
        
        public void writeWithReadWriteLock(int newData) {
            rwLock.writeLock().lock();
            try {
                data = newData;
            } finally {
                rwLock.writeLock().unlock();
            }
        }
    }
    
    public void comparePerformance() throws InterruptedException {
        System.out.println("\n=== StampedLock vs ReadWriteLock Performance ===");
        
        PerformanceComparison comparison = new PerformanceComparison();
        int numReaders = 8;
        int numWriters = 2;
        int operations = 100000;
        
        // Test StampedLock
        long stampedTime = testLockPerformance("StampedLock", comparison, numReaders, numWriters, operations, true);
        
        // Test ReadWriteLock
        long rwTime = testLockPerformance("ReadWriteLock", comparison, numReaders, numWriters, operations, false);
        
        System.out.printf("Performance comparison: StampedLock=%.2fms, ReadWriteLock=%.2fms%n",
                        stampedTime / 1_000_000.0, rwTime / 1_000_000.0);
        System.out.printf("StampedLock speedup: %.2fx%n", (double) rwTime / stampedTime);
    }
    
    private long testLockPerformance(String lockType, PerformanceComparison comparison,
                                   int numReaders, int numWriters, int operations, boolean useStamped) 
            throws InterruptedException {
        
        ExecutorService executor = Executors.newFixedThreadPool(numReaders + numWriters);
        CountDownLatch latch = new CountDownLatch(numReaders + numWriters);
        
        long startTime = System.nanoTime();
        
        // Start readers
        for (int i = 0; i < numReaders; i++) {
            executor.submit(() -> {
                try {
                    for (int j = 0; j < operations; j++) {
                        if (useStamped) {
                            comparison.readWithStampedLock();
                        } else {
                            comparison.readWithReadWriteLock();
                        }
                    }
                } finally {
                    latch.countDown();
                }
            });
        }
        
        // Start writers
        for (int i = 0; i < numWriters; i++) {
            final int writerId = i;
            executor.submit(() -> {
                try {
                    for (int j = 0; j < operations / 10; j++) { // Fewer writes
                        if (useStamped) {
                            comparison.writeWithStampedLock(writerId * 1000 + j);
                        } else {
                            comparison.writeWithReadWriteLock(writerId * 1000 + j);
                        }
                    }
                } finally {
                    latch.countDown();
                }
            });
        }
        
        latch.await();
        long endTime = System.nanoTime();
        
        executor.shutdown();
        return endTime - startTime;
    }
    
    // Example 3: Cache implementation using StampedLock
    static class StampedCache<K, V> {
        private final java.util.Map<K, V> cache = new java.util.HashMap<>();
        private final StampedLock lock = new StampedLock();
        
        public V get(K key) {
            // Try optimistic read first
            long stamp = lock.tryOptimisticRead();
            V value = cache.get(key);
            
            if (!lock.validate(stamp)) {
                // Fall back to read lock
                stamp = lock.readLock();
                try {
                    value = cache.get(key);
                } finally {
                    lock.unlockRead(stamp);
                }
            }
            
            return value;
        }
        
        public V put(K key, V value) {
            long stamp = lock.writeLock();
            try {
                return cache.put(key, value);
            } finally {
                lock.unlockWrite(stamp);
            }
        }
        
        public V remove(K key) {
            long stamp = lock.writeLock();
            try {
                return cache.remove(key);
            } finally {
                lock.unlockWrite(stamp);
            }
        }
        
        public int size() {
            long stamp = lock.tryOptimisticRead();
            int size = cache.size();
            
            if (!lock.validate(stamp)) {
                stamp = lock.readLock();
                try {
                    size = cache.size();
                } finally {
                    lock.unlockRead(stamp);
                }
            }
            
            return size;
        }
        
        public void clear() {
            long stamp = lock.writeLock();
            try {
                cache.clear();
            } finally {
                lock.unlockWrite(stamp);
            }
        }
    }
    
    public void demonstrateStampedCache() throws InterruptedException {
        System.out.println("\n=== Stamped Cache Demo ===");
        
        StampedCache<String, String> cache = new StampedCache<>();
        ExecutorService executor = Executors.newFixedThreadPool(8);
        CountDownLatch latch = new CountDownLatch(8);
        AtomicLong readCount = new AtomicLong(0);
        AtomicLong writeCount = new AtomicLong(0);
        
        // Writers
        for (int i = 0; i < 2; i++) {
            final int writerId = i;
            executor.submit(() -> {
                try {
                    for (int j = 0; j < 50; j++) {
                        String key = "key" + (writerId * 50 + j);
                        String value = "value" + (writerId * 50 + j);
                        cache.put(key, value);
                        writeCount.incrementAndGet();
                        
                        if (j % 10 == 0) {
                            System.out.println("Writer " + writerId + " wrote " + key + 
                                             ", cache size: " + cache.size());
                        }
                        
                        Thread.sleep(20);
                    }
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                } finally {
                    latch.countDown();
                }
            });
        }
        
        // Readers
        for (int i = 0; i < 6; i++) {
            final int readerId = i;
            executor.submit(() -> {
                try {
                    for (int j = 0; j < 200; j++) {
                        String key = "key" + (j % 100);
                        String value = cache.get(key);
                        readCount.incrementAndGet();
                        
                        if (j % 50 == 0) {
                            System.out.println("Reader " + readerId + " read " + key + 
                                             " -> " + value);
                        }
                        
                        Thread.sleep(5);
                    }
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                } finally {
                    latch.countDown();
                }
            });
        }
        
        latch.await();
        
        System.out.println("Cache operations completed:");
        System.out.println("Total writes: " + writeCount.get());
        System.out.println("Total reads: " + readCount.get());
        System.out.println("Final cache size: " + cache.size());
        
        executor.shutdown();
    }
}
```

---

## Practice Questions

### Conceptual Questions
1. **How does Phaser differ from CountDownLatch and CyclicBarrier in terms of flexibility?**
2. **What are the advantages of StampedLock's optimistic reading over traditional ReadWriteLock?**
3. **When would you use hierarchical Phaser organization?**
4. **How does StampedLock handle lock conversion between modes?**
5. **What are the trade-offs between optimistic reading and traditional locking?**

### Coding Challenges
1. **Design a multi-phase data processing pipeline using Phaser**
2. **Build a performance-optimized cache using StampedLock**
3. **Implement a dynamic workflow coordination system with Phaser**
4. **Create a high-throughput reader-writer system using StampedLock**
5. **Design a parallel algorithm that requires phase synchronization**

### Follow-up Questions
1. **How do you handle exceptions in Phaser-coordinated workflows?**
2. **What happens to optimistic reads during heavy write activity in StampedLock?**
3. **How do you choose the right number of phases in a Phaser-based system?**
4. **When should you fall back from optimistic reading to regular locking?**
5. **How do you monitor and debug Phaser-based coordination issues?**

---

## Key Takeaways

### Phaser
- **More flexible** than CountDownLatch and CyclicBarrier
- **Dynamic registration** allows parties to join/leave during execution
- **Multi-phase coordination** supports complex workflows
- **Hierarchical structure** provides scalability for large systems
- **Custom termination** conditions via onAdvance() method

### StampedLock
- **Optimistic reading** often outperforms ReadWriteLock significantly
- **Lock conversion** provides flexibility between modes
- **Not reentrant** - different usage pattern than other locks
- **Best for read-heavy** scenarios with occasional writes
- **Validation required** for optimistic reads to ensure consistency

### Best Practices
- Use Phaser for complex multi-stage coordination workflows
- Choose StampedLock for high-performance read-heavy scenarios
- Always validate optimistic reads in StampedLock
- Handle dynamic registration carefully in Phaser
- Consider hierarchical Phaser organization for large-scale systems
- Monitor performance improvements when migrating from ReadWriteLock to StampedLock

These advanced synchronization primitives provide powerful tools for building sophisticated coordination mechanisms in high-performance concurrent applications.
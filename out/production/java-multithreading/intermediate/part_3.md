# Java Concurrency Part 3: Intermediate Level - Collections & Communication

## Table of Contents
1. [Thread-Safe Collections](#thread-safe-collections)
2. [BlockingQueue Implementations](#blockingqueue-implementations)
3. [Vector vs ArrayList Thread Safety](#vector-vs-arraylist-thread-safety)
4. [Synchronization Utilities](#synchronization-utilities)

---

## Thread-Safe Collections

### Theory

**Thread-safe collections** provide concurrent access without external synchronization. Java offers several approaches:

1. **Synchronized Collections**: Legacy approach using synchronized wrappers
2. **Concurrent Collections**: Modern high-performance implementations
3. **Copy-on-Write Collections**: Optimized for read-heavy scenarios

**Key Collections:**
- **ConcurrentHashMap**: High-performance concurrent map
- **ConcurrentLinkedQueue**: Lock-free queue implementation
- **CopyOnWriteArrayList**: Thread-safe list for read-heavy scenarios
- **ConcurrentSkipListMap**: Concurrent sorted map

### Code Examples

#### ConcurrentHashMap Usage
```java
import java.util.concurrent.*;
import java.util.concurrent.atomic.LongAdder;
import java.util.*;

public class ConcurrentHashMapExample {
    private final ConcurrentHashMap<String, Integer> wordCount = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<String, LongAdder> statisticsMap = new ConcurrentHashMap<>();
    
    public void demonstrateBasicOperations() {
        // Basic thread-safe operations
        wordCount.put("hello", 1);
        wordCount.put("world", 1);
        
        // Atomic operations
        wordCount.compute("hello", (key, val) -> val == null ? 1 : val + 1);
        wordCount.computeIfAbsent("java", k -> 0);
        wordCount.computeIfPresent("world", (key, val) -> val + 1);
        
        // Atomic replacement
        wordCount.replace("hello", 2, 10); // Replace only if current value is 2
        
        // Merge operation (useful for counting)
        wordCount.merge("concurrent", 1, Integer::sum);
        
        System.out.println("Word counts: " + wordCount);
    }
    
    public void concurrentWordCounter(List<String> words) {
        ExecutorService executor = Executors.newFixedThreadPool(4);
        CountDownLatch latch = new CountDownLatch(words.size());
        
        for (String word : words) {
            executor.submit(() -> {
                try {
                    // Thread-safe increment using merge
                    wordCount.merge(word, 1, Integer::sum);
                } finally {
                    latch.countDown();
                }
            });
        }
        
        try {
            latch.await();
            System.out.println("Final word counts: " + wordCount);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
        
        executor.shutdown();
    }
    
    public void statisticsTracker() {
        // Using LongAdder for better concurrent performance
        ExecutorService executor = Executors.newFixedThreadPool(8);
        
        for (int i = 0; i < 1000; i++) {
            final String category = "category" + (i % 5);
            executor.submit(() -> {
                statisticsMap.computeIfAbsent(category, k -> new LongAdder()).increment();
            });
        }
        
        executor.shutdown();
        try {
            executor.awaitTermination(5, TimeUnit.SECONDS);
            
            // Print statistics
            statisticsMap.forEach((category, adder) -> 
                System.out.println(category + ": " + adder.sum()));
                
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }
    
    public void bulkOperations() {
        ConcurrentHashMap<String, Integer> map = new ConcurrentHashMap<>();
        
        // Populate map
        for (int i = 0; i < 1000; i++) {
            map.put("key" + i, i);
        }
        
        // Parallel operations (Java 8+)
        long evenCount = map.values().parallelStream()
                                   .filter(value -> value % 2 == 0)
                                   .count();
        
        // Bulk operations with threshold
        int threshold = 100; // Minimum elements to parallelize
        
        // Search
        String result = map.search(threshold, (key, value) -> 
            value > 500 ? key : null);
        
        // Reduce
        Integer maxValue = map.reduce(threshold, (key, value) -> value,
                                    Integer::max);
        
        // ForEach with threshold
        map.forEach(threshold, (key, value) -> {
            if (value % 100 == 0) {
                System.out.println("Milestone: " + key + " = " + value);
            }
        });
        
        System.out.println("Even numbers: " + evenCount);
        System.out.println("First key > 500: " + result);
        System.out.println("Max value: " + maxValue);
    }
}
```

#### CopyOnWriteArrayList Usage
```java
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.List;

public class CopyOnWriteExample {
    private final CopyOnWriteArrayList<String> eventLog = new CopyOnWriteArrayList<>();
    private final CopyOnWriteArrayList<EventListener> listeners = new CopyOnWriteArrayList<>();
    
    interface EventListener {
        void onEvent(String event);
    }
    
    public void demonstrateCopyOnWrite() {
        // Add initial data
        eventLog.add("Application started");
        eventLog.add("User logged in");
        
        ExecutorService executor = Executors.newFixedThreadPool(6);
        
        // Multiple readers (common scenario for CopyOnWrite)
        for (int i = 0; i < 4; i++) {
            final int readerId = i;
            executor.submit(() -> {
                for (int j = 0; j < 10; j++) {
                    // Safe iteration during concurrent modifications
                    System.out.println("Reader " + readerId + " sees " + 
                                     eventLog.size() + " events");
                    
                    // Safe enhanced for loop
                    for (String event : eventLog) {
                        System.out.println("  " + event);
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
        
        // Occasional writers
        for (int i = 0; i < 2; i++) {
            final int writerId = i;
            executor.submit(() -> {
                for (int j = 0; j < 5; j++) {
                    String event = "Event from writer " + writerId + "-" + j;
                    eventLog.add(event);
                    System.out.println("Writer " + writerId + " added: " + event);
                    
                    try {
                        Thread.sleep(500);
                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                        return;
                    }
                }
            });
        }
        
        executor.shutdown();
    }
    
    public void observerPattern() {
        // Add listeners
        listeners.add(event -> System.out.println("Logger: " + event));
        listeners.add(event -> System.out.println("Metrics: Event recorded"));
        listeners.add(event -> {
            if (event.contains("ERROR")) {
                System.out.println("Alert: Error detected!");
            }
        });
        
        ExecutorService executor = Executors.newFixedThreadPool(3);
        
        // Event generator
        executor.submit(() -> {
            String[] events = {"User action", "ERROR: Failed to save", "Data processed"};
            for (String event : events) {
                fireEvent(event);
                try {
                    Thread.sleep(1000);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    return;
                }
            }
        });
        
        // Dynamic listener management
        executor.submit(() -> {
            try {
                Thread.sleep(1500);
                listeners.add(event -> System.out.println("New listener: " + event));
                System.out.println("Added new listener");
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        });
        
        executor.shutdown();
    }
    
    private void fireEvent(String event) {
        // Safe iteration even if listeners list is modified during iteration
        for (EventListener listener : listeners) {
            try {
                listener.onEvent(event);
            } catch (Exception e) {
                System.err.println("Listener failed: " + e.getMessage());
            }
        }
    }
}
```

#### Performance Comparison
```java
import java.util.*;
import java.util.concurrent.*;

public class CollectionPerformanceComparison {
    
    public void compareMapPerformance() {
        int numThreads = 8;
        int operationsPerThread = 10000;
        
        // Test different map implementations
        Map<String, Integer> hashMap = new HashMap<>();
        Map<String, Integer> syncMap = Collections.synchronizedMap(new HashMap<>());
        ConcurrentHashMap<String, Integer> concurrentMap = new ConcurrentHashMap<>();
        
        System.out.println("Map Performance Comparison:");
        
        // Note: HashMap will likely produce incorrect results due to race conditions
        testMapPerformance("HashMap (unsafe)", hashMap, numThreads, operationsPerThread);
        testMapPerformance("Synchronized HashMap", syncMap, numThreads, operationsPerThread);
        testMapPerformance("ConcurrentHashMap", concurrentMap, numThreads, operationsPerThread);
    }
    
    private void testMapPerformance(String name, Map<String, Integer> map, 
                                  int numThreads, int operationsPerThread) {
        ExecutorService executor = Executors.newFixedThreadPool(numThreads);
        CountDownLatch latch = new CountDownLatch(numThreads);
        
        long startTime = System.nanoTime();
        
        for (int i = 0; i < numThreads; i++) {
            final int threadId = i;
            executor.submit(() -> {
                try {
                    for (int j = 0; j < operationsPerThread; j++) {
                        String key = "key" + ((threadId * operationsPerThread) + j);
                        map.put(key, j);
                        map.get(key);
                        
                        if (j % 2 == 0) {
                            map.remove(key);
                        }
                    }
                } finally {
                    latch.countDown();
                }
            });
        }
        
        try {
            latch.await();
            long endTime = System.nanoTime();
            
            double durationMs = (endTime - startTime) / 1_000_000.0;
            System.out.printf("%s: %.2f ms, Final size: %d%n", 
                            name, durationMs, map.size());
                            
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
        
        executor.shutdown();
        map.clear();
    }
    
    public void compareListPerformance() {
        int numReaders = 10;
        int numWriters = 2;
        int iterations = 1000;
        
        List<String> syncList = Collections.synchronizedList(new ArrayList<>());
        CopyOnWriteArrayList<String> cowList = new CopyOnWriteArrayList<>();
        
        System.out.println("\nList Performance Comparison:");
        testListPerformance("Synchronized ArrayList", syncList, numReaders, numWriters, iterations);
        testListPerformance("CopyOnWriteArrayList", cowList, numReaders, numWriters, iterations);
    }
    
    private void testListPerformance(String name, List<String> list, 
                                   int numReaders, int numWriters, int iterations) {
        ExecutorService executor = Executors.newFixedThreadPool(numReaders + numWriters);
        CountDownLatch latch = new CountDownLatch(numReaders + numWriters);
        
        // Pre-populate
        for (int i = 0; i < 100; i++) {
            list.add("Initial-" + i);
        }
        
        long startTime = System.nanoTime();
        
        // Start readers
        for (int i = 0; i < numReaders; i++) {
            final int readerId = i;
            executor.submit(() -> {
                try {
                    for (int j = 0; j < iterations; j++) {
                        // Read operations
                        int size = list.size();
                        if (size > 0) {
                            String item = list.get(size / 2);
                            // Simulate processing
                            item.length();
                        }
                        
                        // Iterate (this is where CopyOnWrite shines)
                        for (String item : list) {
                            item.hashCode();
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
                    for (int j = 0; j < iterations / 10; j++) { // Fewer writes
                        list.add("Writer-" + writerId + "-" + j);
                        if (list.size() > 150) {
                            list.remove(0); // Remove oldest
                        }
                    }
                } finally {
                    latch.countDown();
                }
            });
        }
        
        try {
            latch.await();
            long endTime = System.nanoTime();
            
            double durationMs = (endTime - startTime) / 1_000_000.0;
            System.out.printf("%s: %.2f ms, Final size: %d%n", 
                            name, durationMs, list.size());
                            
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
        
        executor.shutdown();
        list.clear();
    }
}
```

---

## BlockingQueue Implementations

### Theory

**BlockingQueue** is a thread-safe queue that supports operations that wait for the queue to become non-empty when retrieving elements, and wait for space to become available when storing elements.

**Key Implementations:**
- **ArrayBlockingQueue**: Bounded queue backed by array
- **LinkedBlockingQueue**: Optionally bounded queue backed by linked nodes
- **PriorityBlockingQueue**: Unbounded priority queue
- **SynchronousQueue**: Queue with no internal capacity
- **DelayQueue**: Queue of delayed elements
- **LinkedTransferQueue**: Unbounded queue with transfer capabilities

### Code Examples

#### Different BlockingQueue Types
```java
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.*;

public class BlockingQueueComparison {
    
    public void demonstrateArrayBlockingQueue() throws InterruptedException {
        ArrayBlockingQueue<String> queue = new ArrayBlockingQueue<>(3);
        
        System.out.println("=== ArrayBlockingQueue Demo ===");
        
        // Producer
        Thread producer = new Thread(() -> {
            try {
                for (int i = 1; i <= 5; i++) {
                    String item = "Item-" + i;
                    System.out.println("Producing: " + item);
                    queue.put(item); // Blocks when queue is full
                    System.out.println("Produced: " + item + ", Queue size: " + queue.size());
                }
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        });
        
        // Consumer (starts after a delay)
        Thread consumer = new Thread(() -> {
            try {
                Thread.sleep(2000); // Delay to show blocking behavior
                
                while (!queue.isEmpty() || !Thread.currentThread().isInterrupted()) {
                    String item = queue.poll(1, TimeUnit.SECONDS);
                    if (item != null) {
                        System.out.println("Consumed: " + item + ", Queue size: " + queue.size());
                        Thread.sleep(500);
                    } else {
                        break; // Timeout reached
                    }
                }
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        });
        
        producer.start();
        consumer.start();
        
        producer.join();
        consumer.join();
    }
    
    public void demonstratePriorityBlockingQueue() throws InterruptedException {
        PriorityBlockingQueue<Task> queue = new PriorityBlockingQueue<>();
        
        System.out.println("\n=== PriorityBlockingQueue Demo ===");
        
        // Producer adding tasks with different priorities
        Thread producer = new Thread(() -> {
            try {
                queue.put(new Task("Low priority task", 1));
                queue.put(new Task("High priority task", 10));
                queue.put(new Task("Medium priority task", 5));
                queue.put(new Task("Critical task", 20));
                queue.put(new Task("Another low task", 2));
                
                System.out.println("All tasks added to priority queue");
            } catch (Exception e) {
                Thread.currentThread().interrupt();
            }
        });
        
        // Consumer processing by priority
        Thread consumer = new Thread(() -> {
            try {
                Thread.sleep(500); // Let producer add all tasks
                
                while (!queue.isEmpty()) {
                    Task task = queue.take();
                    System.out.println("Processing: " + task);
                    Thread.sleep(200);
                }
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        });
        
        producer.start();
        consumer.start();
        
        producer.join();
        consumer.join();
    }
    
    public void demonstrateSynchronousQueue() throws InterruptedException {
        SynchronousQueue<String> queue = new SynchronousQueue<>();
        
        System.out.println("\n=== SynchronousQueue Demo ===");
        
        // Producer
        Thread producer = new Thread(() -> {
            try {
                for (int i = 1; i <= 3; i++) {
                    String item = "Item-" + i;
                    System.out.println("Trying to hand off: " + item);
                    queue.put(item); // Blocks until consumer takes it
                    System.out.println("Successfully handed off: " + item);
                }
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        });
        
        // Consumer
        Thread consumer = new Thread(() -> {
            try {
                for (int i = 1; i <= 3; i++) {
                    Thread.sleep(1000); // Simulate processing time
                    String item = queue.take(); // Direct handoff
                    System.out.println("Received: " + item);
                }
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        });
        
        producer.start();
        consumer.start();
        
        producer.join();
        consumer.join();
    }
    
    public void demonstrateDelayQueue() throws InterruptedException {
        DelayQueue<DelayedTask> queue = new DelayQueue<>();
        
        System.out.println("\n=== DelayQueue Demo ===");
        
        long now = System.currentTimeMillis();
        
        // Add tasks with different delays
        queue.put(new DelayedTask("Task 1", now + 3000)); // 3 seconds
        queue.put(new DelayedTask("Task 2", now + 1000)); // 1 second  
        queue.put(new DelayedTask("Task 3", now + 2000)); // 2 seconds
        
        System.out.println("Added 3 delayed tasks");
        
        // Consumer will receive tasks in delay order (shortest delay first)
        Thread consumer = new Thread(() -> {
            try {
                while (queue.size() > 0) {
                    DelayedTask task = queue.take(); // Blocks until delay expires
                    System.out.println("Executed: " + task + " at " + 
                                     new Date(System.currentTimeMillis()));
                }
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        });
        
        consumer.start();
        consumer.join();
    }
    
    // Helper classes
    static class Task implements Comparable<Task> {
        private final String name;
        private final int priority;
        
        public Task(String name, int priority) {
            this.name = name;
            this.priority = priority;
        }
        
        @Override
        public int compareTo(Task other) {
            // Higher priority first (reverse order)
            return Integer.compare(other.priority, this.priority);
        }
        
        @Override
        public String toString() {
            return name + " (Priority: " + priority + ")";
        }
    }
    
    static class DelayedTask implements Delayed {
        private final String name;
        private final long executeTime;
        
        public DelayedTask(String name, long executeTime) {
            this.name = name;
            this.executeTime = executeTime;
        }
        
        @Override
        public long getDelay(TimeUnit unit) {
            long remaining = executeTime - System.currentTimeMillis();
            return unit.convert(remaining, TimeUnit.MILLISECONDS);
        }
        
        @Override
        public int compareTo(Delayed other) {
            return Long.compare(this.executeTime, ((DelayedTask) other).executeTime);
        }
        
        @Override
        public String toString() {
            return name;
        }
    }
}
```

#### Producer-Consumer with Different Queues
```java
public class QueueBasedProducerConsumer {
    
    public void testWithDifferentQueues() throws InterruptedException {
        System.out.println("Testing different queue implementations:");
        
        // Test with bounded queue
        testProducerConsumer("ArrayBlockingQueue", 
                           new ArrayBlockingQueue<>(5), 3, 2);
        
        // Test with unbounded queue
        testProducerConsumer("LinkedBlockingQueue", 
                           new LinkedBlockingQueue<>(), 3, 2);
        
        // Test with synchronous handoff
        testProducerConsumer("SynchronousQueue", 
                           new SynchronousQueue<>(), 2, 2);
    }
    
    private void testProducerConsumer(String queueType, BlockingQueue<String> queue,
                                    int numProducers, int numConsumers) 
            throws InterruptedException {
        
        System.out.println("\n--- Testing " + queueType + " ---");
        
        ExecutorService executor = Executors.newCachedThreadPool();
        CountDownLatch producerLatch = new CountDownLatch(numProducers);
        CountDownLatch consumerLatch = new CountDownLatch(numConsumers);
        AtomicInteger itemCount = new AtomicInteger(0);
        
        // Start producers
        for (int i = 0; i < numProducers; i++) {
            final int producerId = i;
            executor.submit(() -> {
                try {
                    for (int j = 0; j < 5; j++) {
                        String item = "P" + producerId + "-Item" + j;
                        
                        long startTime = System.currentTimeMillis();
                        queue.put(item);
                        long endTime = System.currentTimeMillis();
                        
                        System.out.printf("[%s] Produced %s (took %dms, queue size: %d)%n",
                                        queueType, item, endTime - startTime, queue.size());
                        
                        Thread.sleep(100);
                    }
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                } finally {
                    producerLatch.countDown();
                }
            });
        }
        
        // Start consumers after a small delay
        Thread.sleep(50);
        
        for (int i = 0; i < numConsumers; i++) {
            final int consumerId = i;
            executor.submit(() -> {
                try {
                    while (true) {
                        String item = queue.poll(2, TimeUnit.SECONDS);
                        if (item == null) {
                            // Check if all producers are done
                            if (producerLatch.getCount() == 0 && queue.isEmpty()) {
                                break;
                            }
                            continue;
                        }
                        
                        int count = itemCount.incrementAndGet();
                        System.out.printf("[%s] Consumer %d got %s (#%d, queue size: %d)%n",
                                        queueType, consumerId, item, count, queue.size());
                        
                        Thread.sleep(150); // Simulate processing
                    }
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                } finally {
                    consumerLatch.countDown();
                }
            });
        }
        
        // Wait for completion
        producerLatch.await(10, TimeUnit.SECONDS);
        consumerLatch.await(10, TimeUnit.SECONDS);
        
        System.out.println(queueType + " test completed. Total items processed: " + 
                         itemCount.get());
        
        executor.shutdown();
    }
}
```

---

## Vector vs ArrayList Thread Safety

### Theory

**Vector** and **ArrayList** are both dynamic arrays, but they differ significantly in thread safety:

**Vector:**
- Thread-safe (synchronized methods)
- Legacy class from JDK 1.0
- Performance overhead due to synchronization
- All methods are synchronized

**ArrayList:**
- Not thread-safe
- Better performance for single-threaded use
- Requires external synchronization for concurrent access
- Can be wrapped with Collections.synchronizedList()

### Code Examples

#### Demonstrating Thread Safety Issues
```java
import java.util.*;
import java.util.concurrent.*;

public class VectorVsArrayListComparison {
    
    public void demonstrateThreadSafetyIssues() throws InterruptedException {
        System.out.println("=== Thread Safety Comparison ===");
        
        // Test ArrayList (unsafe)
        testListConcurrency("ArrayList (Unsafe)", new ArrayList<>(), false);
        
        // Test Vector (safe but slow)
        testListConcurrency("Vector (Safe)", new Vector<>(), true);
        
        // Test Synchronized ArrayList
        testListConcurrency("Synchronized ArrayList", 
                          Collections.synchronizedList(new ArrayList<>()), true);
        
        // Test CopyOnWriteArrayList
        testListConcurrency("CopyOnWriteArrayList", 
                          new CopyOnWriteArrayList<>(), true);
    }
    
    private void testListConcurrency(String listType, List<Integer> list, 
                                   boolean expectCorrectResult) throws InterruptedException {
        int numThreads = 10;
        int operationsPerThread = 1000;
        int expectedSize = numThreads * operationsPerThread;
        
        ExecutorService executor = Executors.newFixedThreadPool(numThreads);
        CountDownLatch latch = new CountDownLatch(numThreads);
        
        long startTime = System.nanoTime();
        
        // Each thread adds numbers
        for (int i = 0; i < numThreads; i++) {
            final int threadId = i;
            executor.submit(() -> {
                try {
                    for (int j = 0; j < operationsPerThread; j++) {
                        list.add(threadId * operationsPerThread + j);
                    }
                } catch (Exception e) {
                    System.err.println("Error in " + listType + ": " + e.getMessage());
                } finally {
                    latch.countDown();
                }
            });
        }
        
        latch.await();
        long endTime = System.nanoTime();
        
        double durationMs = (endTime - startTime) / 1_000_000.0;
        int actualSize = list.size();
        boolean isCorrect = actualSize == expectedSize;
        
        System.out.printf("%s: %.2f ms, Size: %d/%d %s%n", 
                        listType, durationMs, actualSize, expectedSize,
                        isCorrect ? "✓" : "✗");
        
        if (!expectCorrectResult && !isCorrect) {
            System.out.println("  ^ Expected incorrect result due to race conditions");
        } else if (expectCorrectResult && !isCorrect) {
            System.out.println("  ^ Unexpected incorrect result!");
        }
        
        executor.shutdown();
        list.clear();
    }
    
    public void demonstrateCompoundOperationIssues() throws InterruptedException {
        System.out.println("\n=== Compound Operation Issues ===");
        
        Vector<Integer> vector = new Vector<>();
        List<Integer> syncList = Collections.synchronizedList(new ArrayList<>());
        
        // Even Vector has issues with compound operations
        testCompoundOperations("Vector", vector);
        testCompoundOperations("Synchronized ArrayList", syncList);
    }
    
    private void testCompoundOperations(String listType, List<Integer> list) 
            throws InterruptedException {
        // Pre-populate
        for (int i = 0; i < 100; i++) {
            list.add(i);
        }
        
        int numThreads = 5;
        ExecutorService executor = Executors.newFixedThreadPool(numThreads);
        CountDownLatch latch = new CountDownLatch(numThreads);
        AtomicInteger errors = new AtomicInteger(0);
        
        for (int i = 0; i < numThreads; i++) {
            executor.submit(() -> {
                try {
                    for (int j = 0; j < 100; j++) {
                        // Compound operation: check-then-act
                        // This is NOT atomic even with Vector/synchronized list
                        if (!list.isEmpty()) {
                            try {
                                Integer value = list.remove(0);
                                // Process value...
                            } catch (IndexOutOfBoundsException e) {
                                errors.incrementAndGet();
                            }
                        }
                        
                        // Another compound operation
                        if (list.size() < 50) {
                            list.add((int) (Math.random() * 1000));
                        }
                    }
                } finally {
                    latch.countDown();
                }
            });
        }
        
        latch.await();
        
        System.out.printf("%s compound operations - Errors: %d, Final size: %d%n",
                        listType, errors.get(), list.size());
        
        executor.shutdown();
    }
    
    public void properCompoundOperations() {
        System.out.println("\n=== Proper Compound Operations ===");
        
        Vector<Integer> vector = new Vector<>();
        List<Integer> syncList = Collections.synchronizedList(new ArrayList<>());
        
        // Pre-populate
        for (int i = 0; i < 100; i++) {
            vector.add(i);
            syncList.add(i);
        }
        
        // Proper synchronization for compound operations
        
        // Vector - synchronized on the vector itself
        synchronized (vector) {
            if (!vector.isEmpty()) {
                Integer value = vector.remove(0);
                System.out.println("Safely removed from Vector: " + value);
            }
        }
        
        // Synchronized List - synchronized on the list itself
        synchronized (syncList) {
            if (!syncList.isEmpty()) {
                Integer value = syncList.remove(0);
                System.out.println("Safely removed from Synchronized List: " + value);
            }
        }
        
        // Iterator safety
        demonstrateIteratorSafety(vector, syncList);
    }
    
    private void demonstrateIteratorSafety(Vector<Integer> vector, List<Integer> syncList) {
        System.out.println("\n--- Iterator Safety ---");
        
        // Vector iterator is NOT thread-safe
        try {
            synchronized (vector) {
                Iterator<Integer> it = vector.iterator();
                while (it.hasNext()) {
                    Integer value = it.next();
                    if (value % 10 == 0) {
                        it.remove(); // Safe within synchronized block
                    }
                }
            }
            System.out.println("Vector iterator completed safely");
        } catch (ConcurrentModificationException e) {
            System.out.println("Vector iterator failed: " + e.getMessage());
        }
        
        // Synchronized List iterator
        try {
            synchronized (syncList) {
                Iterator<Integer> it = syncList.iterator();
                while (it.hasNext()) {
                    Integer value = it.next();
                    if (value % 10 == 0) {
                        it.remove(); // Safe within synchronized block
                    }
                }
            }
            System.out.println("Synchronized List iterator completed safely");
        } catch (ConcurrentModificationException e) {
            System.out.println("Synchronized List iterator failed: " + e.getMessage());
        }
    }
}
```

---

## Synchronization Utilities

### Theory

Java provides several high-level synchronization utilities that simplify common coordination patterns:

**CountDownLatch**: Allows threads to wait for a set of operations to complete
**CyclicBarrier**: Synchronization point where threads wait for each other
**Semaphore**: Controls access to a resource with limited permits
**Exchanger**: Synchronization point for exchanging data between two threads

### Code Examples

#### CountDownLatch Usage
```java
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;

public class CountDownLatchExample {
    
    public void basicCountDownLatch() throws InterruptedException {
        int numWorkers = 5;
        CountDownLatch startSignal = new CountDownLatch(1);
        CountDownLatch doneSignal = new CountDownLatch(numWorkers);
        
        ExecutorService executor = Executors.newFixedThreadPool(numWorkers);
        
        // Start workers
        for (int i = 0; i < numWorkers; i++) {
            final int workerId = i;
            executor.submit(() -> {
                try {
                    // Wait for start signal
                    startSignal.await();
                    System.out.println("Worker " + workerId + " started");
                    
                    // Do work
                    Thread.sleep(1000 + (workerId * 200));
                    System.out.println("Worker " + workerId + " completed");
                    
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                } finally {
                    doneSignal.countDown(); // Signal completion
                }
            });
        }
        
        System.out.println("All workers created, starting work...");
        startSignal.countDown(); // Release all workers
        
        // Wait for all workers to complete
        doneSignal.await();
        System.out.println("All workers completed!");
        
        executor.shutdown();
    }
    
    public void realWorldExample() throws InterruptedException {
        System.out.println("\n=== Service Startup Coordination ===");
        
        CountDownLatch serviceReady = new CountDownLatch(3);
        CountDownLatch shutdownSignal = new CountDownLatch(1);
        
        // Database service
        Thread dbService = new Thread(() -> {
            try {
                System.out.println("Starting database service...");
                Thread.sleep(2000); // Simulate startup time
                System.out.println("Database service ready");
                serviceReady.countDown();
                
                // Wait for shutdown signal
                shutdownSignal.await();
                System.out.println("Database service shutting down");
                
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        });
        
        // Cache service
        Thread cacheService = new Thread(() -> {
            try {
                System.out.println("Starting cache service...");
                Thread.sleep(1500);
                System.out.println("Cache service ready");
                serviceReady.countDown();
                
                shutdownSignal.await();
                System.out.println("Cache service shutting down");
                
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        });
        
        // Web service
        Thread webService = new Thread(() -> {
            try {
                System.out.println("Starting web service...");
                Thread.sleep(1000);
                System.out.println("Web service ready");
                serviceReady.countDown();
                
                shutdownSignal.await();
                System.out.println("Web service shutting down");
                
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        });
        
        // Start all services
        dbService.start();
        cacheService.start();
        webService.start();
        
        // Wait for all services to be ready
        serviceReady.await();
        System.out.println("🚀 All services ready! Application started.");
        
        // Simulate running for a while
        Thread.sleep(2000);
        
        // Initiate shutdown
        System.out.println("Initiating graceful shutdown...");
        shutdownSignal.countDown();
        
        // Wait for all services to shut down
        dbService.join();
        cacheService.join();
        webService.join();
        
        System.out.println("Application shutdown completed");
    }
}
```

#### CyclicBarrier Usage
```java
public class CyclicBarrierExample {
    
    public void basicCyclicBarrier() throws InterruptedException {
        int numThreads = 4;
        CyclicBarrier barrier = new CyclicBarrier(numThreads, () -> {
            System.out.println("🎯 All threads reached the barrier!");
        });
        
        ExecutorService executor = Executors.newFixedThreadPool(numThreads);
        
        for (int i = 0; i < numThreads; i++) {
            final int threadId = i;
            executor.submit(() -> {
                try {
                    // Phase 1
                    System.out.println("Thread " + threadId + " doing phase 1");
                    Thread.sleep(1000 + (threadId * 200));
                    System.out.println("Thread " + threadId + " finished phase 1");
                    
                    barrier.await(); // Wait for all threads
                    
                    // Phase 2
                    System.out.println("Thread " + threadId + " doing phase 2");
                    Thread.sleep(500 + (threadId * 100));
                    System.out.println("Thread " + threadId + " finished phase 2");
                    
                } catch (InterruptedException | BrokenBarrierException e) {
                    System.err.println("Thread " + threadId + " interrupted: " + e.getMessage());
                }
            });
        }
        
        executor.shutdown();
        executor.awaitTermination(10, TimeUnit.SECONDS);
    }
    
    public void matrixMultiplicationExample() throws InterruptedException {
        System.out.println("\n=== Parallel Matrix Multiplication ===");
        
        int size = 4;
        int numThreads = 4;
        int[][] matrixA = generateMatrix(size);
        int[][] matrixB = generateMatrix(size);
        int[][] result = new int[size][size];
        
        CyclicBarrier barrier = new CyclicBarrier(numThreads, () -> {
            System.out.println("All threads completed their portion");
        });
        
        ExecutorService executor = Executors.newFixedThreadPool(numThreads);
        
        // Divide work among threads
        int rowsPerThread = size / numThreads;
        
        for (int t = 0; t < numThreads; t++) {
            final int startRow = t * rowsPerThread;
            final int endRow = (t == numThreads - 1) ? size : startRow + rowsPerThread;
            final int threadId = t;
            
            executor.submit(() -> {
                try {
                    System.out.println("Thread " + threadId + " processing rows " + 
                                     startRow + " to " + (endRow - 1));
                    
                    // Matrix multiplication for assigned rows
                    for (int i = startRow; i < endRow; i++) {
                        for (int j = 0; j < size; j++) {
                            for (int k = 0; k < size; k++) {
                                result[i][j] += matrixA[i][k] * matrixB[k][j];
                            }
                        }
                    }
                    
                    System.out.println("Thread " + threadId + " completed");
                    barrier.await(); // Wait for all threads to complete
                    
                    // After barrier, thread 0 prints the result
                    if (threadId == 0) {
                        System.out.println("Final result matrix:");
                        printMatrix(result);
                    }
                    
                } catch (InterruptedException | BrokenBarrierException e) {
                    System.err.println("Thread " + threadId + " error: " + e.getMessage());
                }
            });
        }
        
        executor.shutdown();
        executor.awaitTermination(10, TimeUnit.SECONDS);
    }
    
    private int[][] generateMatrix(int size) {
        int[][] matrix = new int[size][size];
        for (int i = 0; i < size; i++) {
            for (int j = 0; j < size; j++) {
                matrix[i][j] = (int) (Math.random() * 10);
            }
        }
        return matrix;
    }
    
    private void printMatrix(int[][] matrix) {
        for (int[] row : matrix) {
            System.out.println(Arrays.toString(row));
        }
    }
}
```

#### Semaphore Usage
```java
public class SemaphoreExample {
    
    public void basicSemaphore() throws InterruptedException {
        // Semaphore with 3 permits
        Semaphore semaphore = new Semaphore(3);
        ExecutorService executor = Executors.newFixedThreadPool(6);
        
        for (int i = 0; i < 6; i++) {
            final int taskId = i;
            executor.submit(() -> {
                try {
                    System.out.println("Task " + taskId + " waiting for permit...");
                    semaphore.acquire(); // Acquire permit
                    
                    System.out.println("Task " + taskId + " acquired permit, working...");
                    Thread.sleep(2000); // Simulate work
                    System.out.println("Task " + taskId + " completed work");
                    
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                } finally {
                    semaphore.release(); // Always release permit
                    System.out.println("Task " + taskId + " released permit");
                }
            });
        }
        
        executor.shutdown();
        executor.awaitTermination(15, TimeUnit.SECONDS);
    }
    
    public void resourcePoolExample() throws InterruptedException {
        System.out.println("\n=== Database Connection Pool ===");
        
        DatabaseConnectionPool pool = new DatabaseConnectionPool(3);
        ExecutorService executor = Executors.newFixedThreadPool(8);
        
        for (int i = 0; i < 8; i++) {
            final int userId = i;
            executor.submit(() -> {
                Connection conn = null;
                try {
                    conn = pool.getConnection();
                    System.out.println("User " + userId + " got connection: " + conn);
                    
                    // Simulate database work
                    Thread.sleep(1000 + (userId % 3) * 500);
                    System.out.println("User " + userId + " completed database work");
                    
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                } finally {
                    if (conn != null) {
                        pool.releaseConnection(conn);
                    }
                }
            });
        }
        
        executor.shutdown();
        executor.awaitTermination(15, TimeUnit.SECONDS);
        
        System.out.println("Pool status: " + pool.getAvailableConnections() + 
                         " connections available");
    }
    
    // Mock database connection
    static class Connection {
        private final int id;
        
        public Connection(int id) {
            this.id = id;
        }
        
        @Override
        public String toString() {
            return "Connection-" + id;
        }
    }
    
    static class DatabaseConnectionPool {
        private final Semaphore semaphore;
        private final Queue<Connection> connections;
        
        public DatabaseConnectionPool(int poolSize) {
            this.semaphore = new Semaphore(poolSize);
            this.connections = new ConcurrentLinkedQueue<>();
            
            // Initialize connection pool
            for (int i = 0; i < poolSize; i++) {
                connections.offer(new Connection(i));
            }
        }
        
        public Connection getConnection() throws InterruptedException {
            semaphore.acquire(); // Wait for available connection
            return connections.poll(); // Get connection from pool
        }
        
        public void releaseConnection(Connection conn) {
            connections.offer(conn); // Return connection to pool
            semaphore.release(); // Release permit
        }
        
        public int getAvailableConnections() {
            return semaphore.availablePermits();
        }
    }
}
```

#### Exchanger Usage
```java
public class ExchangerExample {
    
    public void basicExchanger() throws InterruptedException {
        Exchanger<String> exchanger = new Exchanger<>();
        
        Thread producer = new Thread(() -> {
            try {
                for (int i = 1; i <= 3; i++) {
                    String data = "Data-" + i;
                    System.out.println("Producer created: " + data);
                    
                    // Exchange data with consumer
                    String received = exchanger.exchange(data);
                    System.out.println("Producer received back: " + received);
                    
                    Thread.sleep(1000);
                }
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        });
        
        Thread consumer = new Thread(() -> {
            try {
                for (int i = 1; i <= 3; i++) {
                    String confirmation = "Processed-" + i;
                    
                    // Exchange confirmation with producer
                    String received = exchanger.exchange(confirmation);
                    System.out.println("Consumer received: " + received);
                    System.out.println("Consumer sent back: " + confirmation);
                    
                    Thread.sleep(500);
                }
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        });
        
        producer.start();
        consumer.start();
        
        producer.join();
        consumer.join();
    }
    
    public void bufferExchangeExample() throws InterruptedException {
        System.out.println("\n=== Buffer Exchange Example ===");
        
        Exchanger<List<String>> exchanger = new Exchanger<>();
        
        // Producer with buffer
        Thread producer = new Thread(() -> {
            List<String> producerBuffer = new ArrayList<>();
            
            try {
                for (int round = 1; round <= 3; round++) {
                    // Fill buffer
                    for (int i = 1; i <= 5; i++) {
                        String item = "Round" + round + "-Item" + i;
                        producerBuffer.add(item);
                        System.out.println("Produced: " + item);
                        Thread.sleep(100);
                    }
                    
                    System.out.println("Producer buffer full, exchanging...");
                    
                    // Exchange full buffer for empty one
                    producerBuffer = exchanger.exchange(producerBuffer);
                    System.out.println("Producer got new buffer, size: " + producerBuffer.size());
                }
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        });
        
        // Consumer with buffer
        Thread consumer = new Thread(() -> {
            List<String> consumerBuffer = new ArrayList<>();
            
            try {
                for (int round = 1; round <= 3; round++) {
                    System.out.println("Consumer waiting for full buffer...");
                    
                    // Exchange empty buffer for full one
                    consumerBuffer = exchanger.exchange(consumerBuffer);
                    System.out.println("Consumer got buffer with " + consumerBuffer.size() + " items");
                    
                    // Process items
                    for (String item : consumerBuffer) {
                        System.out.println("Processing: " + item);
                        Thread.sleep(200);
                    }
                    
                    consumerBuffer.clear(); // Empty the buffer
                    System.out.println("Consumer finished processing round " + round);
                }
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        });
        
        producer.start();
        consumer.start();
        
        producer.join();
        consumer.join();
    }
    
    public void timeoutExchangeExample() {
        System.out.println("\n=== Exchange with Timeout ===");
        
        Exchanger<String> exchanger = new Exchanger<>();
        
        Thread fastThread = new Thread(() -> {
            try {
                String data = "Fast data";
                System.out.println("Fast thread trying to exchange: " + data);
                
                String received = exchanger.exchange(data, 2, TimeUnit.SECONDS);
                System.out.println("Fast thread received: " + received);
                
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            } catch (TimeoutException e) {
                System.out.println("Fast thread timed out waiting for exchange");
            }
        });
        
        Thread slowThread = new Thread(() -> {
            try {
                Thread.sleep(3000); // Will cause timeout
                
                String data = "Slow data";
                System.out.println("Slow thread trying to exchange: " + data);
                
                String received = exchanger.exchange(data, 1, TimeUnit.SECONDS);
                System.out.println("Slow thread received: " + received);
                
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            } catch (TimeoutException e) {
                System.out.println("Slow thread timed out waiting for exchange");
            }
        });
        
        fastThread.start();
        slowThread.start();
        
        try {
            fastThread.join();
            slowThread.join();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }
}
```

---

## Practice Questions

### Conceptual Questions
1. **When would you choose ConcurrentHashMap over Collections.synchronizedMap()?**
2. **Explain the trade-offs between CopyOnWriteArrayList and Collections.synchronizedList().**
3. **What are the differences between ArrayBlockingQueue and LinkedBlockingQueue?**
4. **When would you use CountDownLatch vs CyclicBarrier?**
5. **Why might Vector not be sufficient for thread-safe compound operations?**

### Coding Challenges
1. **Implement a thread-safe LRU cache using ConcurrentHashMap**
2. **Create a rate limiter using Semaphore**
3. **Build a parallel merge sort using CyclicBarrier**
4. **Design a producer-consumer system with priority using PriorityBlockingQueue**
5. **Implement a data pipeline using Exchanger for buffer swapping**

### Follow-up Questions
1. **How do concurrent collections handle resize operations differently than synchronized collections?**
2. **What happens to threads waiting on a CyclicBarrier if one thread throws an exception?**
3. **How can you implement fairness in Semaphore and what are the trade-offs?**
4. **What are the memory consistency effects of operations on concurrent collections?**
5. **How do you handle cleanup when using synchronization utilities in a service that needs graceful shutdown?**

---

## Key Takeaways
- **ConcurrentHashMap** provides better performance than synchronized HashMap for concurrent access
- **CopyOnWriteArrayList** is optimal for read-heavy scenarios with infrequent writes
- **Vector** and synchronized collections still require external synchronization for compound operations
- **BlockingQueue** implementations serve different use cases based on capacity and ordering requirements
- **CountDownLatch** is for one-time events, **CyclicBarrier** for repeated synchronization points
- **Semaphore** controls resource access, **Exchanger** enables safe data exchange between two threads
- Choose the right collection based on read/write patterns and performance requirements
- Always consider compound operations when designing thread-safe systems
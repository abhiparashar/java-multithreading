# Java Memory Model Guide - Part 8: Volatile vs Synchronized Performance Comparisons

## Introduction to Performance Characteristics

Understanding the performance implications of volatile vs synchronized is crucial for making informed decisions about synchronization mechanisms in concurrent Java applications. This guide provides comprehensive benchmarks and analysis.

## Fundamental Performance Differences

### Memory Barrier Costs
```java
import java.util.concurrent.TimeUnit;

public class MemoryBarrierCosts {
    private int plainField = 0;
    private volatile int volatileField = 0;
    private final Object lockObject = new Object();
    
    // Benchmark: Plain field access
    public void benchmarkPlainAccess(int iterations) {
        long startTime = System.nanoTime();
        
        for (int i = 0; i < iterations; i++) {
            plainField = i;
            int temp = plainField;
        }
        
        long endTime = System.nanoTime();
        printResults("Plain field access", iterations, startTime, endTime);
    }
    
    // Benchmark: Volatile field access
    public void benchmarkVolatileAccess(int iterations) {
        long startTime = System.nanoTime();
        
        for (int i = 0; i < iterations; i++) {
            volatileField = i;  // Store barrier
            int temp = volatileField;  // Load barrier
        }
        
        long endTime = System.nanoTime();
        printResults("Volatile field access", iterations, startTime, endTime);
    }
    
    // Benchmark: Synchronized access
    public void benchmarkSynchronizedAccess(int iterations) {
        long startTime = System.nanoTime();
        
        for (int i = 0; i < iterations; i++) {
            synchronized (lockObject) {
                plainField = i;
                int temp = plainField;
            }
        }
        
        long endTime = System.nanoTime();
        printResults("Synchronized access", iterations, startTime, endTime);
    }
    
    private void printResults(String testName, int iterations, long startTime, long endTime) {
        long totalTime = endTime - startTime;
        double avgTimeNs = (double) totalTime / iterations;
        
        System.out.printf("%-25s: %10d iterations, %10.2f ns/operation, %8.2f ms total%n",
            testName, iterations, avgTimeNs, totalTime / 1_000_000.0);
    }
    
    public static void main(String[] args) {
        MemoryBarrierCosts benchmark = new MemoryBarrierCosts();
        int iterations = 1_000_000;
        
        System.out.println("Memory Barrier Performance Comparison:");
        System.out.println("=====================================");
        
        // Warmup
        for (int i = 0; i < 5; i++) {
            benchmark.benchmarkPlainAccess(iterations / 10);
            benchmark.benchmarkVolatileAccess(iterations / 10);
            benchmark.benchmarkSynchronizedAccess(iterations / 10);
        }
        
        // Actual benchmarks
        benchmark.benchmarkPlainAccess(iterations);
        benchmark.benchmarkVolatileAccess(iterations);
        benchmark.benchmarkSynchronizedAccess(iterations);
    }
}
```

### Read vs Write Performance
```java
public class ReadWritePerformance {
    private volatile int volatileCounter = 0;
    private int synchronizedCounter = 0;
    private final Object lock = new Object();
    
    // Volatile read benchmark
    public void benchmarkVolatileReads(int iterations) {
        long startTime = System.nanoTime();
        int sum = 0;
        
        for (int i = 0; i < iterations; i++) {
            sum += volatileCounter;  // Volatile read
        }
        
        long endTime = System.nanoTime();
        printResults("Volatile reads", iterations, startTime, endTime);
        System.out.println("Sum (to prevent optimization): " + sum);
    }
    
    // Volatile write benchmark
    public void benchmarkVolatileWrites(int iterations) {
        long startTime = System.nanoTime();
        
        for (int i = 0; i < iterations; i++) {
            volatileCounter = i;  // Volatile write
        }
        
        long endTime = System.nanoTime();
        printResults("Volatile writes", iterations, startTime, endTime);
    }
    
    // Synchronized read benchmark
    public void benchmarkSynchronizedReads(int iterations) {
        long startTime = System.nanoTime();
        int sum = 0;
        
        for (int i = 0; i < iterations; i++) {
            synchronized (lock) {
                sum += synchronizedCounter;  // Synchronized read
            }
        }
        
        long endTime = System.nanoTime();
        printResults("Synchronized reads", iterations, startTime, endTime);
        System.out.println("Sum (to prevent optimization): " + sum);
    }
    
    // Synchronized write benchmark
    public void benchmarkSynchronizedWrites(int iterations) {
        long startTime = System.nanoTime();
        
        for (int i = 0; i < iterations; i++) {
            synchronized (lock) {
                synchronizedCounter = i;  // Synchronized write
            }
        }
        
        long endTime = System.nanoTime();
        printResults("Synchronized writes", iterations, startTime, endTime);
    }
    
    private void printResults(String testName, int iterations, long startTime, long endTime) {
        long totalTime = endTime - startTime;
        double avgTimeNs = (double) totalTime / iterations;
        
        System.out.printf("%-25s: %10.2f ns/operation%n", testName, avgTimeNs);
    }
    
    public static void main(String[] args) {
        ReadWritePerformance benchmark = new ReadWritePerformance();
        int iterations = 10_000_000;
        
        System.out.println("Read/Write Performance Comparison:");
        System.out.println("==================================");
        
        // Warmup
        for (int i = 0; i < 3; i++) {
            benchmark.benchmarkVolatileReads(iterations / 100);
            benchmark.benchmarkVolatileWrites(iterations / 100);
            benchmark.benchmarkSynchronizedReads(iterations / 100);
            benchmark.benchmarkSynchronizedWrites(iterations / 100);
        }
        
        // Actual benchmarks
        benchmark.benchmarkVolatileReads(iterations);
        benchmark.benchmarkVolatileWrites(iterations);
        benchmark.benchmarkSynchronizedReads(iterations);
        benchmark.benchmarkSynchronizedWrites(iterations);
    }
}
```

## Contention Scenarios

### Low Contention Performance
```java
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class LowContentionBenchmark {
    private volatile long volatileCounter = 0;
    private long synchronizedCounter = 0;
    private final Object lock = new Object();
    
    public void benchmarkLowContention(int numThreads, int operationsPerThread) 
            throws InterruptedException {
        
        System.out.println("Low Contention Benchmark:");
        System.out.println("Threads: " + numThreads + ", Operations per thread: " + operationsPerThread);
        
        // Test volatile
        benchmarkVolatileContention(numThreads, operationsPerThread);
        
        // Test synchronized
        benchmarkSynchronizedContention(numThreads, operationsPerThread);
    }
    
    private void benchmarkVolatileContention(int numThreads, int operationsPerThread) 
            throws InterruptedException {
        
        ExecutorService executor = Executors.newFixedThreadPool(numThreads);
        CountDownLatch latch = new CountDownLatch(numThreads);
        volatileCounter = 0;
        
        long startTime = System.nanoTime();
        
        for (int t = 0; t < numThreads; t++) {
            final int threadId = t;
            executor.submit(() -> {
                try {
                    // Each thread works on different cache lines to reduce contention
                    for (int i = 0; i < operationsPerThread; i++) {
                        // Read operation (most common in low contention)
                        long temp = volatileCounter;
                        
                        // Occasional write (simulating low contention)
                        if (i % 100 == threadId) {
                            volatileCounter = temp + 1;
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
        
        long totalOps = (long) numThreads * operationsPerThread;
        double avgTimeNs = (double) (endTime - startTime) / totalOps;
        
        System.out.printf("Volatile (low contention): %10.2f ns/operation%n", avgTimeNs);
    }
    
    private void benchmarkSynchronizedContention(int numThreads, int operationsPerThread) 
            throws InterruptedException {
        
        ExecutorService executor = Executors.newFixedThreadPool(numThreads);
        CountDownLatch latch = new CountDownLatch(numThreads);
        synchronizedCounter = 0;
        
        long startTime = System.nanoTime();
        
        for (int t = 0; t < numThreads; t++) {
            final int threadId = t;
            executor.submit(() -> {
                try {
                    for (int i = 0; i < operationsPerThread; i++) {
                        synchronized (lock) {
                            // Read operation
                            long temp = synchronizedCounter;
                            
                            // Occasional write
                            if (i % 100 == threadId) {
                                synchronizedCounter = temp + 1;
                            }
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
        
        long totalOps = (long) numThreads * operationsPerThread;
        double avgTimeNs = (double) (endTime - startTime) / totalOps;
        
        System.out.printf("Synchronized (low contention): %10.2f ns/operation%n", avgTimeNs);
    }
    
    public static void main(String[] args) throws InterruptedException {
        LowContentionBenchmark benchmark = new LowContentionBenchmark();
        
        // Test different thread counts
        int[] threadCounts = {1, 2, 4, 8};
        int operationsPerThread = 1_000_000;
        
        for (int threads : threadCounts) {
            benchmark.benchmarkLowContention(threads, operationsPerThread);
            System.out.println();
        }
    }
}
```

### High Contention Performance
```java
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicLong;

public class HighContentionBenchmark {
    private volatile long volatileCounter = 0;
    private long synchronizedCounter = 0;
    private final AtomicLong atomicCounter = new AtomicLong(0);
    private final Object lock = new Object();
    
    public void benchmarkHighContention(int numThreads, int operationsPerThread) 
            throws InterruptedException {
        
        System.out.println("High Contention Benchmark:");
        System.out.println("Threads: " + numThreads + ", Operations per thread: " + operationsPerThread);
        
        // Test volatile (will show poor performance due to cache coherency traffic)
        benchmarkVolatileHighContention(numThreads, operationsPerThread);
        
        // Test synchronized
        benchmarkSynchronizedHighContention(numThreads, operationsPerThread);
        
        // Test atomic for comparison
        benchmarkAtomicHighContention(numThreads, operationsPerThread);
    }
    
    private void benchmarkVolatileHighContention(int numThreads, int operationsPerThread) 
            throws InterruptedException {
        
        ExecutorService executor = Executors.newFixedThreadPool(numThreads);
        CountDownLatch latch = new CountDownLatch(numThreads);
        volatileCounter = 0;
        
        long startTime = System.nanoTime();
        
        for (int t = 0; t < numThreads; t++) {
            executor.submit(() -> {
                try {
                    for (int i = 0; i < operationsPerThread; i++) {
                        // High contention: every operation is a write
                        volatileCounter = volatileCounter + 1; // Race condition!
                    }
                } finally {
                    latch.countDown();
                }
            });
        }
        
        latch.await();
        long endTime = System.nanoTime();
        
        executor.shutdown();
        
        long totalOps = (long) numThreads * operationsPerThread;
        double avgTimeNs = (double) (endTime - startTime) / totalOps;
        
        System.out.printf("Volatile (high contention): %10.2f ns/operation, final value: %d%n", 
            avgTimeNs, volatileCounter);
    }
    
    private void benchmarkSynchronizedHighContention(int numThreads, int operationsPerThread) 
            throws InterruptedException {
        
        ExecutorService executor = Executors.newFixedThreadPool(numThreads);
        CountDownLatch latch = new CountDownLatch(numThreads);
        synchronizedCounter = 0;
        
        long startTime = System.nanoTime();
        
        for (int t = 0; t < numThreads; t++) {
            executor.submit(() -> {
                try {
                    for (int i = 0; i < operationsPerThread; i++) {
                        synchronized (lock) {
                            synchronizedCounter = synchronizedCounter + 1; // Thread-safe
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
        
        long totalOps = (long) numThreads * operationsPerThread;
        double avgTimeNs = (double) (endTime - startTime) / totalOps;
        
        System.out.printf("Synchronized (high contention): %10.2f ns/operation, final value: %d%n", 
            avgTimeNs, synchronizedCounter);
    }
    
    private void benchmarkAtomicHighContention(int numThreads, int operationsPerThread) 
            throws InterruptedException {
        
        ExecutorService executor = Executors.newFixedThreadPool(numThreads);
        CountDownLatch latch = new CountDownLatch(numThreads);
        atomicCounter.set(0);
        
        long startTime = System.nanoTime();
        
        for (int t = 0; t < numThreads; t++) {
            executor.submit(() -> {
                try {
                    for (int i = 0; i < operationsPerThread; i++) {
                        atomicCounter.incrementAndGet(); // Lock-free but high contention
                    }
                } finally {
                    latch.countDown();
                }
            });
        }
        
        latch.await();
        long endTime = System.nanoTime();
        
        executor.shutdown();
        
        long totalOps = (long) numThreads * operationsPerThread;
        double avgTimeNs = (double) (endTime - startTime) / totalOps;
        
        System.out.printf("Atomic (high contention): %10.2f ns/operation, final value: %d%n", 
            avgTimeNs, atomicCounter.get());
    }
    
    public static void main(String[] args) throws InterruptedException {
        HighContentionBenchmark benchmark = new HighContentionBenchmark();
        
        // Test different thread counts under high contention
        int[] threadCounts = {2, 4, 8, 16};
        int operationsPerThread = 100_000;
        
        for (int threads : threadCounts) {
            benchmark.benchmarkHighContention(threads, operationsPerThread);
            System.out.println();
        }
    }
}
```

## Cache Line Effects and False Sharing

### False Sharing Impact
```java
public class FalseSharingBenchmark {
    // Padded volatile fields to prevent false sharing
    static class PaddedVolatile {
        public long p1, p2, p3, p4, p5, p6, p7; // Padding
        public volatile long value = 0;
        public long p8, p9, p10, p11, p12, p13, p14; // Padding
    }
    
    // Non-padded volatile fields (will cause false sharing)
    static class UnpaddedVolatile {
        public volatile long value = 0;
    }
    
    private static final int NUM_THREADS = 4;
    private static final int OPERATIONS_PER_THREAD = 1_000_000;
    
    // Array of padded volatiles
    private static final PaddedVolatile[] paddedArray = new PaddedVolatile[NUM_THREADS];
    
    // Array of unpadded volatiles (false sharing)
    private static final UnpaddedVolatile[] unpaddedArray = new UnpaddedVolatile[NUM_THREADS];
    
    static {
        for (int i = 0; i < NUM_THREADS; i++) {
            paddedArray[i] = new PaddedVolatile();
            unpaddedArray[i] = new UnpaddedVolatile();
        }
    }
    
    public static void benchmarkFalseSharing() throws InterruptedException {
        System.out.println("False Sharing Benchmark:");
        System.out.println("========================");
        
        // Test without false sharing (padded)
        benchmarkPadded();
        
        // Test with false sharing (unpadded)
        benchmarkUnpadded();
    }
    
    private static void benchmarkPadded() throws InterruptedException {
        CountDownLatch latch = new CountDownLatch(NUM_THREADS);
        long startTime = System.nanoTime();
        
        for (int t = 0; t < NUM_THREADS; t++) {
            final int threadIndex = t;
            new Thread(() -> {
                try {
                    for (int i = 0; i < OPERATIONS_PER_THREAD; i++) {
                        paddedArray[threadIndex].value = i;
                    }
                } finally {
                    latch.countDown();
                }
            }).start();
        }
        
        latch.await();
        long endTime = System.nanoTime();
        
        long totalOps = (long) NUM_THREADS * OPERATIONS_PER_THREAD;
        double avgTimeNs = (double) (endTime - startTime) / totalOps;
        
        System.out.printf("Padded volatiles (no false sharing): %10.2f ns/operation%n", avgTimeNs);
    }
    
    private static void benchmarkUnpadded() throws InterruptedException {
        CountDownLatch latch = new CountDownLatch(NUM_THREADS);
        long startTime = System.nanoTime();
        
        for (int t = 0; t < NUM_THREADS; t++) {
            final int threadIndex = t;
            new Thread(() -> {
                try {
                    for (int i = 0; i < OPERATIONS_PER_THREAD; i++) {
                        unpaddedArray[threadIndex].value = i;
                    }
                } finally {
                    latch.countDown();
                }
            }).start();
        }
        
        latch.await();
        long endTime = System.nanoTime();
        
        long totalOps = (long) NUM_THREADS * OPERATIONS_PER_THREAD;
        double avgTimeNs = (double) (endTime - startTime) / totalOps;
        
        System.out.printf("Unpadded volatiles (false sharing): %10.2f ns/operation%n", avgTimeNs);
    }
    
    public static void main(String[] args) throws InterruptedException {
        // Warmup
        for (int i = 0; i < 3; i++) {
            benchmarkFalseSharing();
        }
        
        System.out.println("\nActual benchmark results:");
        benchmarkFalseSharing();
    }
}
```

## Real-World Use Case Comparisons

### Producer-Consumer Scenarios
```java
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.CountDownLatch;

public class ProducerConsumerBenchmark {
    private static final int NUM_ITEMS = 1_000_000;
    private static final int BUFFER_SIZE = 1000;
    
    // Volatile-based implementation
    static class VolatileBuffer {
        private final int[] buffer = new int[BUFFER_SIZE];
        private volatile int writeIndex = 0;
        private volatile int readIndex = 0;
        private volatile int count = 0;
        
        public boolean offer(int item) {
            if (count >= BUFFER_SIZE) return false;
            
            buffer[writeIndex] = item;
            writeIndex = (writeIndex + 1) % BUFFER_SIZE;
            count++; // Race condition possible!
            return true;
        }
        
        public Integer poll() {
            if (count <= 0) return null;
            
            int item = buffer[readIndex];
            readIndex = (readIndex + 1) % BUFFER_SIZE;
            count--; // Race condition possible!
            return item;
        }
    }
    
    // Synchronized-based implementation
    static class SynchronizedBuffer {
        private final int[] buffer = new int[BUFFER_SIZE];
        private int writeIndex = 0;
        private int readIndex = 0;
        private int count = 0;
        private final Object lock = new Object();
        
        public boolean offer(int item) {
            synchronized (lock) {
                if (count >= BUFFER_SIZE) return false;
                
                buffer[writeIndex] = item;
                writeIndex = (writeIndex + 1) % BUFFER_SIZE;
                count++;
                return true;
            }
        }
        
        public Integer poll() {
            synchronized (lock) {
                if (count <= 0) return null;
                
                int item = buffer[readIndex];
                readIndex = (readIndex + 1) % BUFFER_SIZE;
                count--;
                return item;
            }
        }
    }
    
    public static void benchmarkProducerConsumer() throws InterruptedException {
        System.out.println("Producer-Consumer Benchmark:");
        System.out.println("============================");
        
        // Test volatile implementation
        benchmarkVolatileImplementation();
        
        // Test synchronized implementation
        benchmarkSynchronizedImplementation();
        
        // Test BlockingQueue for comparison
        benchmarkBlockingQueueImplementation();
    }
    
    private static void benchmarkVolatileImplementation() throws InterruptedException {
        VolatileBuffer buffer = new VolatileBuffer();
        CountDownLatch latch = new CountDownLatch(2);
        long startTime = System.nanoTime();
        
        // Producer thread
        Thread producer = new Thread(() -> {
            try {
                for (int i = 0; i < NUM_ITEMS; i++) {
                    while (!buffer.offer(i)) {
                        Thread.yield(); // Busy wait
                    }
                }
            } finally {
                latch.countDown();
            }
        });
        
        // Consumer thread
        Thread consumer = new Thread(() -> {
            try {
                int consumed = 0;
                while (consumed < NUM_ITEMS) {
                    Integer item = buffer.poll();
                    if (item != null) {
                        consumed++;
                    } else {
                        Thread.yield(); // Busy wait
                    }
                }
            } finally {
                latch.countDown();
            }
        });
        
        producer.start();
        consumer.start();
        
        latch.await();
        long endTime = System.nanoTime();
        
        double avgTimeNs = (double) (endTime - startTime) / NUM_ITEMS;
        System.out.printf("Volatile implementation: %10.2f ns/item%n", avgTimeNs);
    }
    
    private static void benchmarkSynchronizedImplementation() throws InterruptedException {
        SynchronizedBuffer buffer = new SynchronizedBuffer();
        CountDownLatch latch = new CountDownLatch(2);
        long startTime = System.nanoTime();
        
        // Producer thread
        Thread producer = new Thread(() -> {
            try {
                for (int i = 0; i < NUM_ITEMS; i++) {
                    while (!buffer.offer(i)) {
                        Thread.yield();
                    }
                }
            } finally {
                latch.countDown();
            }
        });
        
        // Consumer thread
        Thread consumer = new Thread(() -> {
            try {
                int consumed = 0;
                while (consumed < NUM_ITEMS) {
                    Integer item = buffer.poll();
                    if (item != null) {
                        consumed++;
                    } else {
                        Thread.yield();
                    }
                }
            } finally {
                latch.countDown();
            }
        });
        
        producer.start();
        consumer.start();
        
        latch.await();
        long endTime = System.nanoTime();
        
        double avgTimeNs = (double) (endTime - startTime) / NUM_ITEMS;
        System.out.printf("Synchronized implementation: %10.2f ns/item%n", avgTimeNs);
    }
    
    private static void benchmarkBlockingQueueImplementation() throws InterruptedException {
        BlockingQueue<Integer> queue = new LinkedBlockingQueue<>(BUFFER_SIZE);
        CountDownLatch latch = new CountDownLatch(2);
        long startTime = System.nanoTime();
        
        // Producer thread
        Thread producer = new Thread(() -> {
            try {
                for (int i = 0; i < NUM_ITEMS; i++) {
                    queue.offer(i);
                }
            } finally {
                latch.countDown();
            }
        });
        
        // Consumer thread
        Thread consumer = new Thread(() -> {
            try {
                for (int i = 0; i < NUM_ITEMS; i++) {
                    queue.poll();
                }
            } finally {
                latch.countDown();
            }
        });
        
        producer.start();
        consumer.start();
        
        latch.await();
        long endTime = System.nanoTime();
        
        double avgTimeNs = (double) (endTime - startTime) / NUM_ITEMS;
        System.out.printf("BlockingQueue implementation: %10.2f ns/item%n", avgTimeNs);
    }
    
    public static void main(String[] args) throws InterruptedException {
        benchmarkProducerConsumer();
    }
}
```

## Memory Ordering Performance Impact

### Sequential Consistency vs Relaxed Ordering
```java
import java.util.concurrent.CountDownLatch;

public class MemoryOrderingBenchmark {
    private volatile int volatileX = 0;
    private volatile int volatileY = 0;
    private int plainX = 0;
    private int plainY = 0;
    private final Object lockObject = new Object();
    
    public void benchmarkMemoryOrdering() throws InterruptedException {
        System.out.println("Memory Ordering Performance Impact:");
        System.out.println("===================================");
        
        // Test volatile operations (sequential consistency)
        benchmarkVolatileOrdering();
        
        // Test synchronized operations (sequential consistency)
        benchmarkSynchronizedOrdering();
        
        // Test plain operations (relaxed ordering)
        benchmarkPlainOrdering();
    }
    
    private void benchmarkVolatileOrdering() throws InterruptedException {
        final int iterations = 1_000_000;
        CountDownLatch latch = new CountDownLatch(2);
        
        long startTime = System.nanoTime();
        
        // Thread 1: Write to volatileX, read from volatileY
        Thread t1 = new Thread(() -> {
            try {
                for (int i = 0; i < iterations; i++) {
                    volatileX = i;           // Store barrier
                    int temp = volatileY;    // Load barrier
                }
            } finally {
                latch.countDown();
            }
        });
        
        // Thread 2: Write to volatileY, read from volatileX
        Thread t2 = new Thread(() -> {
            try {
                for (int i = 0; i < iterations; i++) {
                    volatileY = i;           // Store barrier
                    int temp = volatileX;    // Load barrier
                }
            } finally {
                latch.countDown();
            }
        });
        
        t1.start();
        t2.start();
        
        latch.await();
        long endTime = System.nanoTime();
        
        long totalOps = iterations * 4; // 2 writes + 2 reads per iteration
        double avgTimeNs = (double) (endTime - startTime) / totalOps;
        
        System.out.printf("Volatile ordering: %10.2f ns/operation%n", avgTimeNs);
    }
    
    private void benchmarkSynchronizedOrdering() throws InterruptedException {
        final int iterations = 1_000_000;
        CountDownLatch latch = new CountDownLatch(2);
        
        long startTime = System.nanoTime();
        
        // Thread 1: Write to plainX, read from plainY (synchronized)
        Thread t1 = new Thread(() -> {
            try {
                for (int i = 0; i < iterations; i++) {
                    synchronized (lockObject) {
                        plainX = i;
                        int temp = plainY;
                    }
                }
            } finally {
                latch.countDown();
            }
        });
        
        // Thread 2: Write to plainY, read from plainX (synchronized)
        Thread t2 = new Thread(() -> {
            try {
                for (int i = 0; i < iterations; i++) {
                    synchronized (lockObject) {
                        plainY = i;
                        int temp = plainX;
                    }
                }
            } finally {
                latch.countDown();
            }
        });
        
        t1.start();
        t2.start();
        
        latch.await();
        long endTime = System.nanoTime();
        
        long totalOps = iterations * 4; // 2 writes + 2 reads per iteration
        double avgTimeNs = (double) (endTime - startTime) / totalOps;
        
        System.out.printf("Synchronized ordering: %10.2f ns/operation%n", avgTimeNs);
    }
    
    private void benchmarkPlainOrdering() throws InterruptedException {
        final int iterations = 1_000_000;
        CountDownLatch latch = new CountDownLatch(2);
        
        long startTime = System.nanoTime();
        
        // Thread 1: Write to plainX, read from plainY (no synchronization)
        Thread t1 = new Thread(() -> {
            try {
                for (int i = 0; i < iterations; i++) {
                    plainX = i;              // No barriers
                    int temp = plainY;       // No barriers
                }
            } finally {
                latch.countDown();
            }
        });
        
        // Thread 2: Write to plainY, read from plainX (no synchronization)
        Thread t2 = new Thread(() -> {
            try {
                for (int i = 0; i < iterations; i++) {
                    plainY = i;              // No barriers
                    int temp = plainX;       // No barriers
                }
            } finally {
                latch.countDown();
            }
        });
        
        t1.start();
        t2.start();
        
        latch.await();
        long endTime = System.nanoTime();
        
        long totalOps = iterations * 4; // 2 writes + 2 reads per iteration
        double avgTimeNs = (double) (endTime - startTime) / totalOps;
        
        System.out.printf("Plain ordering: %10.2f ns/operation%n", avgTimeNs);
        System.out.println("Note: Plain operations are unsafe for inter-thread communication!");
    }
    
    public static void main(String[] args) throws InterruptedException {
        MemoryOrderingBenchmark benchmark = new MemoryOrderingBenchmark();
        
        // Warmup
        for (int i = 0; i < 3; i++) {
            benchmark.benchmarkMemoryOrdering();
        }
        
        System.out.println("\nFinal benchmark results:");
        benchmark.benchmarkMemoryOrdering();
    }
}
```

## Performance Decision Matrix

### Choosing the Right Synchronization Mechanism
```java
public class SynchronizationDecisionMatrix {
    
    /**
     * Performance and Use Case Analysis
     * 
     * VOLATILE:
     * ✅ Best for: Simple flags, status indicators, single-writer scenarios
     * ✅ Performance: Excellent for reads, good for writes (no lock overhead)
     * ❌ Limitations: No atomicity for compound operations, poor for high write contention
     * 
     * SYNCHRONIZED:
     * ✅ Best for: Compound operations, multiple fields, blocking coordination
     * ✅ Performance: Good under moderate contention, excellent atomicity guarantees
     * ❌ Limitations: Lock overhead, potential for deadlock, thread blocking
     * 
     * ATOMIC CLASSES:
     * ✅ Best for: High contention counters, lock-free algorithms
     * ✅ Performance: Excellent under high contention, no blocking
     * ❌ Limitations: Limited to specific operations, memory overhead
     */
    
    public static void printDecisionMatrix() {
        System.out.println("SYNCHRONIZATION MECHANISM DECISION MATRIX");
        System.out.println("=========================================");
        System.out.println();
        
        printScenario("Single boolean flag (shutdown signal)", 
            "VOLATILE", "Lowest overhead, immediate visibility");
        
        printScenario("High-frequency counter with multiple writers", 
            "ATOMIC", "Lock-free, scales with contention");
        
        printScenario("Updating multiple related fields atomically", 
            "SYNCHRONIZED", "Ensures atomicity across multiple operations");
        
        printScenario("Rare writes, frequent reads", 
            "VOLATILE", "Read performance is excellent");
        
        printScenario("Producer-consumer with complex state", 
            "SYNCHRONIZED + wait/notify", "Blocking coordination available");
        
        printScenario("High-contention single-field updates", 
            "ATOMIC", "Best performance under contention");
        
        printScenario("Cache invalidation notifications", 
            "VOLATILE", "Immediate visibility, minimal overhead");
        
        printScenario("Bank account balance updates", 
            "SYNCHRONIZED", "Compound operations need atomicity");
    }
    
    private static void printScenario(String scenario, String recommendation, String reason) {
        System.out.printf("Scenario: %s%n", scenario);
        System.out.printf("  Recommended: %s%n", recommendation);
        System.out.printf("  Reason: %s%n", reason);
        System.out.println();
    }
    
    public static void main(String[] args) {
        printDecisionMatrix();
    }
}
```

## Comprehensive Performance Summary

### Benchmark Results Analysis
```java
public class PerformanceSummary {
    
    public static void printPerformanceSummary() {
        System.out.println("VOLATILE vs SYNCHRONIZED PERFORMANCE SUMMARY");
        System.out.println("============================================");
        System.out.println();
        
        System.out.println("📊 TYPICAL PERFORMANCE CHARACTERISTICS:");
        System.out.println("----------------------------------------");
        System.out.println("Operation Type           | Volatile    | Synchronized | Ratio");
        System.out.println("-------------------------|-------------|--------------|-------");
        System.out.println("Single read              | 1.0x        | 3-5x         | 3-5x slower");
        System.out.println("Single write             | 1.5x        | 3-5x         | 2-3x slower");
        System.out.println("Read under contention    | 1.0x        | 5-10x        | 5-10x slower");
        System.out.println("Write under contention   | 10-50x      | 5-10x        | Volatile worse!");
        System.out.println("Compound operations      | N/A         | 3-5x         | Synchronized only");
        System.out.println();
        
        System.out.println("🎯 WHEN TO USE EACH:");
        System.out.println("-------------------");
        System.out.println("Use VOLATILE when:");
        System.out.println("  • Single field updates");
        System.out.println("  • Mostly reads, few writes");
        System.out.println("  • Simple state flags");
        System.out.println("  • Low write contention");
        System.out.println();
        
        System.out.println("Use SYNCHRONIZED when:");
        System.out.println("  • Multiple field updates");
        System.out.println("  • Compound operations (read-modify-write)");
        System.out.println("  • High write contention");
        System.out.println("  • Need blocking semantics");
        System.out.println();
        
        System.out.println("⚠️  PERFORMANCE PITFALLS:");
        System.out.println("-------------------------");
        System.out.println("VOLATILE pitfalls:");
        System.out.println("  • False sharing in arrays");
        System.out.println("  • High write contention causes cache ping-ponging");
        System.out.println("  • Cannot provide atomicity for compound operations");
        System.out.println();
        
        System.out.println("SYNCHRONIZED pitfalls:");
        System.out.println("  • Lock overhead on every access");
        System.out.println("  • Thread blocking and context switching");
        System.out.println("  • Potential for deadlock");
        System.out.println("  • Poor scalability under high contention");
        System.out.println();
        
        System.out.println("🚀 OPTIMIZATION TIPS:");
        System.out.println("--------------------");
        System.out.println("For VOLATILE:");
        System.out.println("  • Use padding to prevent false sharing");
        System.out.println("  • Prefer single-writer patterns");
        System.out.println("  • Combine with atomic classes for complex operations");
        System.out.println();
        
        System.out.println("For SYNCHRONIZED:");
        System.out.println("  • Keep critical sections short");
        System.out.println("  • Consider ReadWriteLock for read-heavy workloads");
        System.out.println("  • Use separate locks for independent data");
        System.out.println("  • Consider Lock-free alternatives for high contention");
    }
    
    public static void main(String[] args) {
        printPerformanceSummary();
    }
}
```

## Summary

This comprehensive performance analysis reveals that the choice between volatile and synchronized depends heavily on your specific use case:

**Key Performance Insights:**

1. **Volatile excels at**: Simple reads, low contention scenarios, single-field updates
2. **Synchronized excels at**: Compound operations, high write contention, blocking coordination
3. **Performance varies dramatically** based on contention levels and access patterns
4. **Memory barriers and cache coherency** significantly impact performance

**Decision Guidelines:**

- Use **volatile** for simple coordination and status flags
- Use **synchronized** for atomicity and complex state management
- Consider **atomic classes** for high-contention single-field operations
- Always measure performance in your specific application context

The next part will cover memory model testing strategies and tools to help you validate your concurrent code's correctness.
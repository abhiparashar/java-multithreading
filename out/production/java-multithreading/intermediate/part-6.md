# Java Concurrency Part 6: Memory Model Deep Dive - Completing Memory Visibility

## Table of Contents
1. [Completing Memory Visibility Issues](#completing-memory-visibility-issues)
2. [Advanced Memory Model Scenarios](#advanced-memory-model-scenarios)
3. [Practical Memory Model Debugging](#practical-memory-model-debugging)
4. [JIT Compiler Optimizations and Memory Model](#jit-compiler-optimizations-and-memory-model)
5. [Best Practices for Memory Visibility](#best-practices-for-memory-visibility)

---

## Completing Memory Visibility Issues

### Demonstrating Memory Visibility Issues (Continuation)

```java
import java.util.concurrent.TimeUnit;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicInteger;

public class MemoryVisibilityDemo {
    
    // Example 1: Classic visibility problem
    static class VisibilityProblem {
        private boolean flag = false;
        private int value = 0;
        
        public void writer() {
            value = 42;          // Write 1
            flag = true;         // Write 2
        }
        
        public void reader() {
            if (flag) {          // Read 1
                System.out.println("Value: " + value); // Read 2
                // Without proper synchronization, this might print 0!
            }
        }
    }
    
    // Example 2: Volatile fixes visibility
    static class VolatileSolution {
        private volatile boolean flag = false;
        private int value = 0;
        
        public void writer() {
            value = 42;          // Happens-before volatile write
            flag = true;         // Volatile write
        }
        
        public void reader() {
            if (flag) {          // Volatile read
                System.out.println("Value: " + value); // Guaranteed to see 42
            }
        }
    }
    
    public void demonstrateVisibilityProblem() throws InterruptedException {
        System.out.println("=== Memory Visibility Problem ===");
        
        for (int iteration = 0; iteration < 10; iteration++) {
            VisibilityProblem problem = new VisibilityProblem();
            volatile boolean[] readerDone = {false};
            
            Thread writer = new Thread(() -> {
                try {
                    Thread.sleep(1); // Small delay
                    problem.writer();
                    System.out.println("Iteration " + iteration + ": Writer completed");
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
            });
            
            Thread reader = new Thread(() -> {
                long startTime = System.nanoTime();
                while (!readerDone[0]) {
                    problem.reader();
                    
                    // Timeout to avoid infinite loop
                    if (System.nanoTime() - startTime > TimeUnit.SECONDS.toNanos(1)) {
                        System.out.println("Iteration " + iteration + ": Reader timed out - visibility issue!");
                        break;
                    }
                }
            });
            
            reader.start();
            writer.start();
            
            writer.join();
            readerDone[0] = true;
            reader.join();
        }
    }
    
    public void demonstrateVolatileSolution() throws InterruptedException {
        System.out.println("\n=== Volatile Solution ===");
        
        for (int iteration = 0; iteration < 5; iteration++) {
            VolatileSolution solution = new VolatileSolution();
            volatile boolean[] readerDone = {false};
            
            Thread writer = new Thread(() -> {
                try {
                    Thread.sleep(1);
                    solution.writer();
                    System.out.println("Iteration " + iteration + ": Writer completed");
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
            });
            
            Thread reader = new Thread(() -> {
                long startTime = System.nanoTime();
                while (!readerDone[0]) {
                    solution.reader();
                    
                    if (System.nanoTime() - startTime > TimeUnit.SECONDS.toNanos(1)) {
                        System.out.println("Iteration " + iteration + ": Reader timed out");
                        break;
                    }
                }
            });
            
            reader.start();
            writer.start();
            
            writer.join();
            readerDone[0] = true;
            reader.join();
        }
    }
    
    // Example 3: Reordering demonstration
    static class ReorderingDemo {
        private int x = 0, y = 0;
        private int a = 0, b = 0;
        
        public void reset() {
            x = y = a = b = 0;
        }
        
        public void thread1() {
            a = 1;    // Write 1
            x = b;    // Read 1
        }
        
        public void thread2() {
            b = 1;    // Write 2
            y = a;    // Read 2
        }
        
        public int[] getResults() {
            return new int[]{x, y};
        }
    }
    
    public void demonstrateReordering() throws InterruptedException {
        System.out.println("\n=== Instruction Reordering Demo ===");
        
        ReorderingDemo demo = new ReorderingDemo();
        int impossibleResults = 0;
        int totalRuns = 100000;
        
        for (int i = 0; i < totalRuns; i++) {
            demo.reset();
            
            Thread t1 = new Thread(demo::thread1);
            Thread t2 = new Thread(demo::thread2);
            
            t1.start();
            t2.start();
            
            t1.join();
            t2.join();
            
            int[] results = demo.getResults();
            
            // The "impossible" result: x=0, y=0
            // This can happen due to instruction reordering!
            if (results[0] == 0 && results[1] == 0) {
                impossibleResults++;
            }
        }
        
        System.out.printf("Out of %d runs, %d resulted in x=0, y=0 (%.3f%%)%n", 
                        totalRuns, impossibleResults, 
                        (double) impossibleResults / totalRuns * 100);
        
        if (impossibleResults > 0) {
            System.out.println("This demonstrates that instruction reordering can occur!");
        }
    }
    
    // Example 4: Complex visibility scenarios
    static class ComplexVisibilityScenarios {
        private int sharedData = 0;
        private volatile boolean volatileFlag = false;
        private final Object lock = new Object();
        
        // Scenario 1: Volatile providing happens-before for other variables
        public void volatileHappensBefore() throws InterruptedException {
            System.out.println("\n--- Volatile Happens-Before Scenario ---");
            
            Thread writer = new Thread(() -> {
                sharedData = 100;        // Regular write
                volatileFlag = true;     // Volatile write (happens-before subsequent volatile reads)
            });
            
            Thread reader = new Thread(() -> {
                while (!volatileFlag) {  // Volatile read
                    Thread.yield();
                }
                // Due to happens-before, we're guaranteed to see sharedData = 100
                System.out.println("Reader sees sharedData: " + sharedData);
            });
            
            writer.start();
            reader.start();
            
            writer.join();
            reader.join();
            
            // Reset
            sharedData = 0;
            volatileFlag = false;
        }
        
        // Scenario 2: Synchronized providing happens-before
        public void synchronizedHappensBefore() throws InterruptedException {
            System.out.println("\n--- Synchronized Happens-Before Scenario ---");
            
            Thread writer = new Thread(() -> {
                synchronized (lock) {
                    sharedData = 200;    // Write inside synchronized block
                }
            });
            
            Thread reader = new Thread(() -> {
                try {
                    Thread.sleep(10); // Ensure writer goes first
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
                
                synchronized (lock) {
                    // Due to happens-before, we're guaranteed to see sharedData = 200
                    System.out.println("Reader sees sharedData: " + sharedData);
                }
            });
            
            writer.start();
            reader.start();
            
            writer.join();
            reader.join();
            
            // Reset
            sharedData = 0;
        }
        
        // Scenario 3: Demonstrating what happens WITHOUT proper synchronization
        public void noSynchronization() throws InterruptedException {
            System.out.println("\n--- No Synchronization Scenario ---");
            
            AtomicInteger visibilityIssues = new AtomicInteger(0);
            int iterations = 1000;
            
            for (int i = 0; i < iterations; i++) {
                sharedData = 0; // Reset
                volatile boolean writerDone = false;
                
                Thread writer = new Thread(() -> {
                    sharedData = 300;    // Regular write, no synchronization
                });
                
                Thread reader = new Thread(() -> {
                    // Busy wait to give writer a chance
                    long start = System.nanoTime();
                    while (System.nanoTime() - start < 1_000_000) { // 1ms
                        if (sharedData == 300) {
                            return; // Saw the update
                        }
                    }
                    visibilityIssues.incrementAndGet();
                });
                
                writer.start();
                reader.start();
                
                writer.join();
                reader.join();
            }
            
            System.out.printf("Visibility issues in %d/%d iterations (%.1f%%)%n",
                            visibilityIssues.get(), iterations,
                            (double) visibilityIssues.get() / iterations * 100);
        }
    }
    
    // Example 5: Final field initialization safety
    static class FinalFieldSafety {
        private final int finalValue;
        private int regularValue;
        
        public FinalFieldSafety(int value) {
            this.finalValue = value;
            this.regularValue = value;
        }
        
        public int getFinalValue() {
            return finalValue;
        }
        
        public int getRegularValue() {
            return regularValue;
        }
    }
    
    public void demonstrateFinalFieldSafety() throws InterruptedException {
        System.out.println("\n=== Final Field Safety Demo ===");
        
        ExecutorService executor = Executors.newFixedThreadPool(4);
        CountDownLatch latch = new CountDownLatch(100);
        AtomicInteger finalFieldIssues = new AtomicInteger(0);
        AtomicInteger regularFieldIssues = new AtomicInteger(0);
        
        for (int i = 0; i < 100; i++) {
            executor.submit(() -> {
                try {
                    // Create object in one thread
                    FinalFieldSafety obj = new FinalFieldSafety(42);
                    
                    // Pass reference to another thread without synchronization
                    Thread reader = new Thread(() -> {
                        // Final fields are guaranteed to be visible
                        if (obj.getFinalValue() != 42) {
                            finalFieldIssues.incrementAndGet();
                        }
                        
                        // Regular fields might not be visible
                        if (obj.getRegularValue() != 42) {
                            regularFieldIssues.incrementAndGet();
                        }
                    });
                    
                    reader.start();
                    reader.join();
                    
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                } finally {
                    latch.countDown();
                }
            });
        }
        
        latch.await();
        executor.shutdown();
        
        System.out.printf("Final field visibility issues: %d/100%n", finalFieldIssues.get());
        System.out.printf("Regular field visibility issues: %d/100%n", regularFieldIssues.get());
        System.out.println("Final fields provide initialization safety guarantees!");
    }
}
```

---

## Advanced Memory Model Scenarios

### Complex Memory Ordering Patterns

```java
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

public class AdvancedMemoryModelScenarios {
    
    // Example 1: Publication idiom and safe initialization
    static class SafePublicationExample {
        private final int[] data;
        private volatile boolean published = false;
        
        public SafePublicationExample(int size) {
            data = new int[size];
            // Initialize data
            for (int i = 0; i < size; i++) {
                data[i] = i * i;
            }
            // Don't set published flag here - let caller do it
        }
        
        public void publish() {
            published = true; // Volatile write creates happens-before
        }
        
        public int[] getData() {
            if (published) { // Volatile read
                return data; // Guaranteed to see fully initialized array
            }
            return null;
        }
        
        public boolean isPublished() {
            return published;
        }
    }
    
    // Example 2: Unsafe publication (DON'T do this)
    static class UnsafePublicationExample {
        private int[] data;
        private boolean ready = false; // NOT volatile!
        
        public UnsafePublicationExample(int size) {
            data = new int[size];
            for (int i = 0; i < size; i++) {
                data[i] = i * i;
            }
            ready = true; // Race condition!
        }
        
        public int[] getData() {
            if (ready) {
                return data; // Might see partially initialized array!
            }
            return null;
        }
    }
    
    public void demonstratePublicationIdioms() throws InterruptedException {
        System.out.println("=== Publication Idiom Demo ===");
        
        // Safe publication
        System.out.println("--- Safe Publication ---");
        for (int i = 0; i < 10; i++) {
            SafePublicationExample safeExample = new SafePublicationExample(1000);
            
            Thread publisher = new Thread(safeExample::publish);
            Thread consumer = new Thread(() -> {
                while (!safeExample.isPublished()) {
                    Thread.yield();
                }
                int[] data = safeExample.getData();
                if (data != null) {
                    // Verify data integrity
                    boolean valid = true;
                    for (int j = 0; j < data.length; j++) {
                        if (data[j] != j * j) {
                            valid = false;
                            break;
                        }
                    }
                    System.out.println("Safe publication iteration " + i + ": " + 
                                     (valid ? "✓ Valid" : "✗ Invalid"));
                }
            });
            
            publisher.start();
            consumer.start();
            
            publisher.join();
            consumer.join();
        }
        
        // Unsafe publication (commented out as it's dangerous)
        System.out.println("--- Unsafe Publication (Potential Issues) ---");
        System.out.println("Unsafe publication can lead to seeing partially initialized objects!");
    }
    
    // Example 3: Double-checked locking evolution
    static class DoubleCheckedLockingEvolution {
        
        // Broken version (DON'T use)
        static class BrokenSingleton {
            private static BrokenSingleton instance;
            
            public static BrokenSingleton getInstance() {
                if (instance == null) {
                    synchronized (BrokenSingleton.class) {
                        if (instance == null) {
                            instance = new BrokenSingleton(); // Can be partially visible!
                        }
                    }
                }
                return instance;
            }
        }
        
        // Correct version with volatile
        static class CorrectSingleton {
            private static volatile CorrectSingleton instance;
            
            public static CorrectSingleton getInstance() {
                if (instance == null) {
                    synchronized (CorrectSingleton.class) {
                        if (instance == null) {
                            instance = new CorrectSingleton(); // Volatile ensures safe publication
                        }
                    }
                }
                return instance;
            }
        }
        
        // Initialization-on-demand holder (best approach)
        static class HolderSingleton {
            private static class Holder {
                static final HolderSingleton instance = new HolderSingleton();
            }
            
            public static HolderSingleton getInstance() {
                return Holder.instance; // JVM guarantees safe initialization
            }
        }
        
        // Enum singleton (also safe)
        enum EnumSingleton {
            INSTANCE;
            
            public void doSomething() {
                // Enum instances are thread-safe by default
            }
        }
    }
    
    // Example 4: Memory model with collections
    static class CollectionMemoryModel {
        private final AtomicReference<String[]> arrayRef = new AtomicReference<>();
        
        public void updateArray(String[] newArray) {
            arrayRef.set(newArray); // Atomic operation provides happens-before
        }
        
        public String[] getArray() {
            return arrayRef.get(); // Atomic operation
        }
        
        public void demonstrateAtomicReference() throws InterruptedException {
            System.out.println("\n=== Atomic Reference Memory Model ===");
            
            String[] initialArray = {"A", "B", "C"};
            arrayRef.set(initialArray);
            
            Thread updater = new Thread(() -> {
                for (int i = 0; i < 5; i++) {
                    String[] newArray = {"X" + i, "Y" + i, "Z" + i};
                    updateArray(newArray);
                    System.out.println("Updated array to: " + java.util.Arrays.toString(newArray));
                    
                    try {
                        Thread.sleep(100);
                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                        break;
                    }
                }
            });
            
            Thread reader = new Thread(() -> {
                for (int i = 0; i < 10; i++) {
                    String[] currentArray = getArray();
                    System.out.println("Read array: " + java.util.Arrays.toString(currentArray));
                    
                    try {
                        Thread.sleep(50);
                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                        break;
                    }
                }
            });
            
            updater.start();
            reader.start();
            
            updater.join();
            reader.join();
        }
    }
}
```

---

## Practical Memory Model Debugging

### Tools and Techniques for Debugging Memory Visibility Issues

```java
import java.lang.management.ManagementFactory;
import java.lang.management.ThreadMXBean;
import java.util.concurrent.atomic.AtomicLong;

public class MemoryModelDebugging {
    
    // Example 1: Detecting visibility issues
    static class VisibilityDetector {
        private int counter = 0;
        private volatile boolean stopFlag = false;
        private final AtomicLong iterations = new AtomicLong(0);
        
        public void runDetection() throws InterruptedException {
            System.out.println("=== Visibility Issue Detection ===");
            
            Thread writer = new Thread(() -> {
                while (!stopFlag) {
                    counter++;
                    iterations.incrementAndGet();
                    
                    // Occasionally yield to increase chance of visibility issues
                    if (counter % 1000 == 0) {
                        Thread.yield();
                    }
                }
            });
            
            Thread reader = new Thread(() -> {
                int lastSeenValue = 0;
                int staleness = 0;
                
                while (!stopFlag) {
                    int currentValue = counter;
                    
                    if (currentValue == lastSeenValue) {
                        staleness++;
                    } else {
                        if (staleness > 0) {
                            System.out.println("Staleness detected: value stuck at " + 
                                             lastSeenValue + " for " + staleness + " reads");
                        }
                        lastSeenValue = currentValue;
                        staleness = 0;
                    }
                    
                    // Small delay to allow writer to progress
                    try {
                        Thread.sleep(1);
                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                        break;
                    }
                }
            });
            
            writer.start();
            reader.start();
            
            // Run for 2 seconds
            Thread.sleep(2000);
            stopFlag = true;
            
            writer.join();
            reader.join();
            
            System.out.println("Total iterations: " + iterations.get());
            System.out.println("Final counter value: " + counter);
        }
    }
    
    // Example 2: JVM flags for memory model debugging
    static class JVMFlagsHelper {
        public static void printRelevantJVMFlags() {
            System.out.println("\n=== Relevant JVM Flags for Memory Model Debugging ===");
            System.out.println("Use these flags to help debug memory visibility issues:");
            System.out.println();
            System.out.println("1. -XX:+PrintGCDetails");
            System.out.println("   Shows GC activity that might affect memory visibility");
            System.out.println();
            System.out.println("2. -XX:+TraceClassLoading");
            System.out.println("   Shows class loading which affects initialization safety");
            System.out.println();
            System.out.println("3. -XX:+PrintCompilation");
            System.out.println("   Shows JIT compilation which can affect memory ordering");
            System.out.println();
            System.out.println("4. -XX:+UnlockDiagnosticVMOptions -XX:+LogVMOutput");
            System.out.println("   Enables diagnostic output");
            System.out.println();
            System.out.println("5. -XX:+UseG1GC (or other GC)");
            System.out.println("   Different GCs have different memory barrier characteristics");
            System.out.println();
            
            // Get current JVM info
            System.out.println("Current JVM Information:");
            System.out.println("Java Version: " + System.getProperty("java.version"));
            System.out.println("JVM Name: " + System.getProperty("java.vm.name"));
            System.out.println("Available Processors: " + Runtime.getRuntime().availableProcessors());
            
            ThreadMXBean threadMX = ManagementFactory.getThreadMXBean();
            System.out.println("Current Thread Count: " + threadMX.getThreadCount());
        }
    }
    
    // Example 3: Memory model stress testing
    static class MemoryModelStressTest {
        private volatile boolean testRunning = true;
        private final AtomicLong successfulReads = new AtomicLong(0);
        private final AtomicLong failedReads = new AtomicLong(0);
        
        // Test object with potential visibility issues
        static class TestData {
            int value1;
            volatile int value2;
            int value3;
            
            void update(int newValue) {
                value1 = newValue;
                value2 = newValue; // Volatile provides memory barrier
                value3 = newValue;
            }
            
            boolean isConsistent() {
                int v1 = value1;
                int v2 = value2;
                int v3 = value3;
                return v1 == v2 && v2 == v3;
            }
        }
        
        public void runStressTest() throws InterruptedException {
            System.out.println("\n=== Memory Model Stress Test ===");
            
            TestData testData = new TestData();
            int numWriters = 2;
            int numReaders = 4;
            
            // Start writers
            Thread[] writers = new Thread[numWriters];
            for (int i = 0; i < numWriters; i++) {
                final int writerId = i;
                writers[i] = new Thread(() -> {
                    int value = writerId * 1000;
                    while (testRunning) {
                        testData.update(value++);
                        
                        // Occasionally pause to create opportunities for visibility issues
                        if (value % 100 == 0) {
                            Thread.yield();
                        }
                    }
                });
                writers[i].start();
            }
            
            // Start readers
            Thread[] readers = new Thread[numReaders];
            for (int i = 0; i < numReaders; i++) {
                readers[i] = new Thread(() -> {
                    while (testRunning) {
                        if (testData.isConsistent()) {
                            successfulReads.incrementAndGet();
                        } else {
                            failedReads.incrementAndGet();
                            System.out.println("Inconsistent read detected: v1=" + testData.value1 + 
                                             ", v2=" + testData.value2 + ", v3=" + testData.value3);
                        }
                        
                        // Small delay between reads
                        try {
                            Thread.sleep(0, 1000); // 1 microsecond
                        } catch (InterruptedException e) {
                            Thread.currentThread().interrupt();
                            break;
                        }
                    }
                });
                readers[i].start();
            }
            
            // Run test for 3 seconds
            Thread.sleep(3000);
            testRunning = false;
            
            // Wait for all threads to complete
            for (Thread writer : writers) {
                writer.join();
            }
            for (Thread reader : readers) {
                reader.join();
            }
            
            long total = successfulReads.get() + failedReads.get();
            System.out.printf("Stress test results:%n");
            System.out.printf("Total reads: %d%n", total);
            System.out.printf("Successful reads: %d (%.2f%%)%n", 
                            successfulReads.get(), 
                            (double) successfulReads.get() / total * 100);
            System.out.printf("Failed reads: %d (%.2f%%)%n", 
                            failedReads.get(), 
                            (double) failedReads.get() / total * 100);
            
            if (failedReads.get() > 0) {
                System.out.println("⚠️  Memory visibility issues detected!");
                System.out.println("This demonstrates the importance of proper synchronization.");
            } else {
                System.out.println("✅ No visibility issues detected in this run.");
                System.out.println("Note: Visibility issues are non-deterministic and may not always appear.");
            }
        }
    }
    
    // Example 4: Performance impact of different synchronization mechanisms
    static class SynchronizationPerformanceComparison {
        private int unsynchronizedCounter = 0;
        private volatile int volatileCounter = 0;
        private int synchronizedCounter = 0;
        private final AtomicInteger atomicCounter = new AtomicInteger(0);
        private final Object lock = new Object();
        
        public void comparePerformance() throws InterruptedException {
            System.out.println("\n=== Synchronization Performance Comparison ===");
            
            int iterations = 1_000_000;
            int numThreads = 4;
            
            // Test unsynchronized access (unsafe but fast)
            long unsyncTime = testUnsynchronized(iterations, numThreads);
            
            // Test volatile access
            long volatileTime = testVolatile(iterations, numThreads);
            
            // Test synchronized access
            long syncTime = testSynchronized(iterations, numThreads);
            
            // Test atomic access
            long atomicTime = testAtomic(iterations, numThreads);
            
            System.out.printf("Performance results (lower is better):%n");
            System.out.printf("Unsynchronized: %.2f ms (unsafe!)%n", unsyncTime / 1_000_000.0);
            System.out.printf("Volatile: %.2f ms%n", volatileTime / 1_000_000.0);
            System.out.printf("Synchronized: %.2f ms%n", syncTime / 1_000_000.0);
            System.out.printf("Atomic: %.2f ms%n", atomicTime / 1_000_000.0);
            
            System.out.printf("%nRelative performance (vs unsynchronized):%n");
            System.out.printf("Volatile: %.2fx slower%n", (double) volatileTime / unsyncTime);
            System.out.printf("Synchronized: %.2fx slower%n", (double) syncTime / unsyncTime);
            System.out.printf("Atomic: %.2fx slower%n", (double) atomicTime / unsyncTime);
        }
        
        private long testUnsynchronized(int iterations, int numThreads) throws InterruptedException {
            unsynchronizedCounter = 0;
            CountDownLatch latch = new CountDownLatch(numThreads);
            
            long startTime = System.nanoTime();
            
            for (int i = 0; i < numThreads; i++) {
                new Thread(() -> {
                    for (int j = 0; j < iterations / numThreads; j++) {
                        unsynchronizedCounter++; // Race condition!
                    }
                    latch.countDown();
                }).start();
            }
            
            latch.await();
            long endTime = System.nanoTime();
            
            System.out.printf("Unsynchronized final value: %d (expected: %d)%n", 
                            unsynchronizedCounter, iterations);
            
            return endTime - startTime;
        }
        
        private long testVolatile(int iterations, int numThreads) throws InterruptedException {
            volatileCounter = 0;
            CountDownLatch latch = new CountDownLatch(numThreads);
            
            long startTime = System.nanoTime();
            
            for (int i = 0; i < numThreads; i++) {
                new Thread(() -> {
                    for (int j = 0; j < iterations / numThreads; j++) {
                        volatileCounter++; // Still has race condition, but visibility guaranteed
                    }
                    latch.countDown();
                }).start();
            }
            
            latch.await();
            long endTime = System.nanoTime();
            
            System.out.printf("Volatile final value: %d (expected: %d)%n", 
                            volatileCounter, iterations);
            
            return endTime - startTime;
        }
        
        private long testSynchronized(int iterations, int numThreads) throws InterruptedException {
            synchronizedCounter = 0;
            CountDownLatch latch = new CountDownLatch(numThreads);
            
            long startTime = System.nanoTime();
            
            for (int i = 0; i < numThreads; i++) {
                new Thread(() -> {
                    for (int j = 0; j < iterations / numThreads; j++) {
                        synchronized (lock) {
                            synchronizedCounter++; // Thread-safe
                        }
                    }
                    latch.countDown();
                }).start();
            }
            
            latch.await();
            long endTime = System.nanoTime();
            
            System.out.printf("Synchronized final value: %d (expected: %d)%n", 
                            synchronizedCounter, iterations);
            
            return endTime - startTime;
        }
        
        private long testAtomic(int iterations, int numThreads) throws InterruptedException {
            atomicCounter.set(0);
            CountDownLatch latch = new CountDownLatch(numThreads);
            
            long startTime = System.nanoTime();
            
            for (int i = 0; i < numThreads; i++) {
                new Thread(() -> {
                    for (int j = 0; j < iterations / numThreads; j++) {
                        atomicCounter.incrementAndGet(); // Thread-safe and lock-free
                    }
                    latch.countDown();
                }).start();
            }
            
            latch.await();
            long endTime = System.nanoTime();
            
            System.out.printf("Atomic final value: %d (expected: %d)%n", 
                            atomicCounter.get(), iterations);
            
            return endTime - startTime;
        }
    }
}
```

---

## JIT Compiler Optimizations and Memory Model

### How JIT Compiler Affects Memory Visibility

```java
import java.util.concurrent.TimeUnit;

public class JITCompilerEffects {
    
    // Example 1: JIT compiler optimizations and visibility
    static class JITOptimizationDemo {
        private boolean flag = false;
        private int counter = 0;
        
        public void demonstrateJITEffects() {
            System.out.println("=== JIT Compiler Effects on Memory Model ===");
            
            // This method might get optimized by JIT
            Thread writerThread = new Thread(() -> {
                try {
                    Thread.sleep(1000); // Give reader time to start
                    
                    System.out.println("Writer: Setting flag to true");
                    flag = true;
                    
                    // Continue updating counter
                    for (int i = 0; i < 1000000; i++) {
                        counter++;
                    }
                    
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
            });
            
            Thread readerThread = new Thread(() -> {
                System.out.println("Reader: Waiting for flag...");
                
                // This loop might get optimized by JIT to not reload 'flag'
                // Without volatile, JIT might hoist the flag read out of the loop
                long startTime = System.nanoTime();
                while (!flag) {
                    // Empty loop - JIT might optimize this
                    
                    // Timeout to prevent infinite loop
                    if (System.nanoTime() - startTime > TimeUnit.SECONDS.toNanos(5)) {
                        System.out.println("Reader: Timeout! Flag never became true (JIT optimization?)");
                        return;
                    }
                }
                
                System.out.println("Reader: Flag became true! Counter value: " + counter);
            });
            
            readerThread.start();
            writerThread.start();
            
            try {
                writerThread.join();
                readerThread.join();
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }
        
        // Version with proper volatile to prevent JIT optimization
        public void demonstrateVolatileWithJIT() {
            System.out.println("\n--- With Volatile (JIT-safe) ---");
            
            VolatileVersion volatileDemo = new VolatileVersion();
            
            Thread writerThread = new Thread(() -> {
                try {
                    Thread.sleep(1000);
                    System.out.println("Writer: Setting volatile flag to true");
                    volatileDemo.setFlag(true);
                    
                    for (int i = 0; i < 1000000; i++) {
                        volatileDemo.incrementCounter();
                    }
                    
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
            });
            
            Thread readerThread = new Thread(() -> {
                System.out.println("Reader: Waiting for volatile flag...");
                
                long startTime = System.nanoTime();
                while (!volatileDemo.getFlag()) {
                    // Volatile read prevents JIT from optimizing this away
                    
                    if (System.nanoTime() - startTime > TimeUnit.SECONDS.toNanos(5)) {
                        System.out.println("Reader: Timeout! This shouldn't happen with volatile.");
                        return;
                    }
                }
                
                System.out.println("Reader: Volatile flag became true! Counter: " + 
                                 volatileDemo.getCounter());
            });
            
            readerThread.start();
            writerThread.start();
            
            try {
                writerThread.join();
                readerThread.join();
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }
        
        static class VolatileVersion {
            private volatile boolean flag = false;
            private volatile int counter = 0;
            
            public void setFlag(boolean value) { flag = value; }
            public boolean getFlag() { return flag; }
            public void incrementCounter() { counter++; }
            public int getCounter() { return counter; }
        }
    }
    
    // Example 2: Escape analysis and memory barriers
    static class EscapeAnalysisDemo {
        
        // Object that doesn't escape - JIT can optimize
        static class LocalObject {
            int value;
            
            LocalObject(int value) {
                this.value = value;
            }
            
            void increment() {
                value++;
            }
            
            int getValue() {
                return value;
            }
        }
        
        public void demonstrateEscapeAnalysis() {
            System.out.println("\n=== Escape Analysis Effects ===");
            
            long startTime = System.nanoTime();
            
            // Local objects that don't escape - JIT can stack-allocate
            for (int i = 0; i < 1_000_000; i++) {
                LocalObject obj = new LocalObject(i);
                obj.increment();
                obj.increment();
                int result = obj.getValue();
                // obj doesn't escape this method, so JIT can optimize heavily
            }
            
            long endTime = System.nanoTime();
            System.out.printf("Local object operations: %.2f ms%n", 
                            (endTime - startTime) / 1_000_000.0);
            
            // Compare with objects that DO escape
            startTime = System.nanoTime();
            java.util.List<LocalObject> escapingObjects = new java.util.ArrayList<>();
            
            for (int i = 0; i < 1_000_000; i++) {
                LocalObject obj = new LocalObject(i);
                obj.increment();
                obj.increment();
                escapingObjects.add(obj); // Object escapes!
            }
            
            endTime = System.nanoTime();
            System.out.printf("Escaping object operations: %.2f ms%n", 
                            (endTime - startTime) / 1_000_000.0);
            
            System.out.println("Escape analysis allows JIT to optimize non-escaping objects");
        }
    }
    
    // Example 3: Method inlining and memory ordering
    static class MethodInliningDemo {
        private volatile int volatileField = 0;
        private int regularField = 0;
        
        // Small method that will likely be inlined
        private void updateFields(int value) {
            regularField = value;
            volatileField = value; // Memory barrier
        }
        
        // Method that's too large to inline
        private void updateFieldsComplex(int value) {
            // Simulate complex logic
            for (int i = 0; i < 100; i++) {
                Math.sqrt(i * value);
            }
            
            regularField = value;
            volatileField = value;
            
            // More complex logic
            for (int i = 0; i < 100; i++) {
                Math.log(i + value);
            }
        }
        
        public void demonstrateInlining() throws InterruptedException {
            System.out.println("\n=== Method Inlining Effects ===");
            
            Thread writer = new Thread(() -> {
                for (int i = 0; i < 100000; i++) {
                    if (i % 2 == 0) {
                        updateFields(i); // Likely inlined
                    } else {
                        updateFieldsComplex(i); // Less likely to be inlined
                    }
                }
            });
            
            Thread reader = new Thread(() -> {
                int lastSeen = -1;
                while (volatileField < 99999) {
                    int current = volatileField;
                    if (current != lastSeen) {
                        // Due to inlining, we might see different behavior patterns
                        System.out.printf("Volatile: %d, Regular: %d%n", current, regularField);
                        lastSeen = current;
                    }
                    Thread.yield();
                }
            });
            
            writer.start();
            reader.start();
            
            writer.join();
            reader.join();
            
            System.out.println("Method inlining can affect memory barrier placement");
        }
    }
}
```

---

## Best Practices for Memory Visibility

### Guidelines and Patterns for Safe Memory Visibility

```java
import java.util.concurrent.atomic.AtomicReference;
import java.util.concurrent.locks.ReentrantReadWriteLock;

public class MemoryVisibilityBestPractices {
    
    // Best Practice 1: Prefer immutable objects
    static class ImmutableExample {
        private final int value;
        private final String name;
        private final long timestamp;
        
        public ImmutableExample(int value, String name) {
            this.value = value;
            this.name = name;
            this.timestamp = System.currentTimeMillis();
        }
        
        public int getValue() { return value; }
        public String getName() { return name; }
        public long getTimestamp() { return timestamp; }
        
        // Create new instance for "modifications"
        public ImmutableExample withValue(int newValue) {
            return new ImmutableExample(newValue, this.name);
        }
        
        @Override
        public String toString() {
            return String.format("ImmutableExample{value=%d, name='%s', timestamp=%d}", 
                               value, name, timestamp);
        }
    }
    
    // Safe publication of immutable objects
    static class ImmutableObjectManager {
        private volatile ImmutableExample current;
        
        public void updateSafely(int newValue, String newName) {
            // Create new immutable instance
            ImmutableExample newInstance = new ImmutableExample(newValue, newName);
            
            // Safely publish via volatile reference
            current = newInstance;
        }
        
        public ImmutableExample getCurrent() {
            return current; // Safe to return due to immutability
        }
    }
    
    // Best Practice 2: Use proper synchronization patterns
    static class SynchronizationPatterns {
        
        // Pattern 1: Volatile for simple flags and counters
        private volatile boolean shutdownRequested = false;
        private volatile int statusCode = 0;
        
        public void requestShutdown() {
            shutdownRequested = true;
        }
        
        public boolean isShutdownRequested() {
            return shutdownRequested;
        }
        
        // Pattern 2: Synchronized methods for compound operations
        private int balance = 0;
        
        public synchronized void deposit(int amount) {
            balance += amount; // Compound operation: read + modify + write
        }
        
        public synchronized void withdraw(int amount) {
            if (balance >= amount) {
                balance -= amount;
            }
        }
        
        public synchronized int getBalance() {
            return balance;
        }
        
        // Pattern 3: Read-write locks for read-heavy scenarios
        private final ReentrantReadWriteLock rwLock = new ReentrantReadWriteLock();
        private String data = "initial";
        
        public String readData() {
            rwLock.readLock().lock();
            try {
                return data; // Multiple readers can access simultaneously
            } finally {
                rwLock.readLock().unlock();
            }
        }
        
        public void updateData(String newData) {
            rwLock.writeLock().lock();
            try {
                data = newData; // Exclusive write access
            } finally {
                rwLock.writeLock().unlock();
            }
        }
    }
    
    // Best Practice 3: Atomic operations for lock-free programming
    static class AtomicPatterns {
        private final AtomicReference<Node> head = new AtomicReference<>();
        
        static class Node {
            final String data;
            final Node next;
            
            Node(String data, Node next) {
                this.data = data;
                this.next = next;
            }
        }
        
        // Lock-free stack operations
        public void push(String data) {
            Node newNode = new Node(data, null);
            Node currentHead;
            
            do {
                currentHead = head.get();
                newNode.next = currentHead;
            } while (!head.compareAndSet(currentHead, newNode));
        }
        
        public String pop() {
            Node currentHead;
            Node newHead;
            
            do {
                currentHead = head.get();
                if (currentHead == null) {
                    return null;
                }
                newHead = currentHead.next;
            } while (!head.compareAndSet(currentHead, newHead));
            
            return currentHead.data;
        }
    }
    
    // Best Practice 4: Safe initialization patterns
    static class SafeInitializationPatterns {
        
        // Pattern 1: Lazy initialization with double-checked locking
        private static volatile SafeInitializationPatterns instance;
        
        public static SafeInitializationPatterns getInstance() {
            if (instance == null) {
                synchronized (SafeInitializationPatterns.class) {
                    if (instance == null) {
                        instance = new SafeInitializationPatterns();
                    }
                }
            }
            return instance;
        }
        
        // Pattern 2: Initialization-on-demand holder
        private static class LazyHolder {
            static final String EXPENSIVE_RESOURCE = initializeExpensiveResource();
            
            private static String initializeExpensiveResource() {
                // Simulate expensive initialization
                try {
                    Thread.sleep(100);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
                return "Expensive Resource Initialized";
            }
        }
        
        public static String getExpensiveResource() {
            return LazyHolder.EXPENSIVE_RESOURCE; // Thread-safe lazy initialization
        }
        
        // Pattern 3: Safe publication via final fields
        private final int[] safeArray;
        private final String safeString;
        
        public SafeInitializationPatterns() {
            safeArray = new int[]{1, 2, 3, 4, 5};
            safeString = "safely initialized";
            // Final fields provide initialization safety guarantees
        }
        
        public int[] getSafeArray() {
            return safeArray.clone(); // Return defensive copy
        }
        
        public String getSafeString() {
            return safeString; // Strings are immutable, safe to return
        }
    }
    
    // Best Practice 5: Error handling with memory visibility
    static class ErrorHandlingPatterns {
        private volatile String lastError = null;
        private volatile boolean hasError = false;
        
        public void performOperation() {
            try {
                // Simulate some operation that might fail
                if (Math.random() < 0.3) {
                    throw new RuntimeException("Random failure");
                }
                
                // Clear error state on success
                clearError();
                
            } catch (Exception e) {
                setError("Operation failed: " + e.getMessage());
            }
        }
        
        private void setError(String errorMessage) {
            lastError = errorMessage;  // Set message first
            hasError = true;          // Then set flag (volatile write)
        }
        
        private void clearError() {
            hasError = false;         // Clear flag first
            lastError = null;         // Then clear message
        }
        
        public boolean hasError() {
            return hasError;          // Volatile read
        }
        
        public String getLastError() {
            if (hasError) {           // Check flag first
                return lastError;     // Then read message
            }
            return null;
        }
    }
    
    // Example usage and testing
    public void demonstrateBestPractices() throws InterruptedException {
        System.out.println("=== Memory Visibility Best Practices Demo ===");
        
        // Test immutable objects
        System.out.println("--- Immutable Objects ---");
        ImmutableObjectManager manager = new ImmutableObjectManager();
        
        Thread updater = new Thread(() -> {
            for (int i = 0; i < 5; i++) {
                manager.updateSafely(i * 10, "Name" + i);
                System.out.println("Updated to: " + manager.getCurrent());
                
                try {
                    Thread.sleep(200);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    break;
                }
            }
        });
        
        Thread reader = new Thread(() -> {
            for (int i = 0; i < 10; i++) {
                ImmutableExample current = manager.getCurrent();
                if (current != null) {
                    System.out.println("Read: " + current);
                }
                
                try {
                    Thread.sleep(100);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    break;
                }
            }
        });
        
        updater.start();
        reader.start();
        
        updater.join();
        reader.join();
        
        // Test atomic operations
        System.out.println("\n--- Atomic Operations ---");
        AtomicPatterns atomicStack = new AtomicPatterns();
        
        // Push some items
        atomicStack.push("First");
        atomicStack.push("Second");
        atomicStack.push("Third");
        
        // Pop items
        String item;
        while ((item = atomicStack.pop()) != null) {
            System.out.println("Popped: " + item);
        }
        
        // Test safe initialization
        System.out.println("\n--- Safe Initialization ---");
        System.out.println("Expensive resource: " + 
                         SafeInitializationPatterns.getExpensiveResource());
        
        // Test error handling
        System.out.println("\n--- Error Handling ---");
        ErrorHandlingPatterns errorHandler = new ErrorHandlingPatterns();
        
        for (int i = 0; i < 5; i++) {
            errorHandler.performOperation();
            
            if (errorHandler.hasError()) {
                System.out.println("Error occurred: " + errorHandler.getLastError());
            } else {
                System.out.println("Operation " + i + " succeeded");
            }
        }
    }
}
```

---

## Practice Questions

### Conceptual Questions
1. **What is the difference between program order and execution order in the Java Memory Model?**
2. **How does volatile differ from synchronized in terms of memory barriers provided?**
3. **Explain why double-checked locking was broken before Java 5 and how volatile fixed it.**
4. **What role does the JIT compiler play in memory visibility issues?**
5. **How do final fields provide initialization safety guarantees?**

### Debugging Scenarios
1. **You have a program where one thread sets a flag and another thread never sees the change. How would you debug this?**
2. **A singleton implementation sometimes returns null even after initialization. What could be wrong?**
3. **Your application shows different behavior in development vs production. How might JIT compilation be involved?**
4. **Performance degrades significantly when adding synchronization. What alternatives could you consider?**

### Design Challenges
1. **Design a thread-safe cache that provides fast reads without locks**
2. **Implement a producer-consumer system with minimal memory barriers**
3. **Create a configuration manager that safely publishes updates to multiple readers**
4. **Build a monitoring system that tracks memory visibility issues in real-time**

---

## Key Takeaways

- **Memory visibility** is not guaranteed without proper synchronization
- **Volatile** provides visibility and ordering guarantees but not atomicity
- **JIT compiler optimizations** can affect memory visibility in unexpected ways
- **Immutable objects** eliminate many memory visibility concerns
- **Proper synchronization patterns** are essential for correct concurrent programs
- **Performance trade-offs** exist between different synchronization mechanisms
- **Debugging memory visibility issues** requires understanding of underlying hardware and JVM behavior
- **Safe publication** patterns ensure objects are properly visible to other threads
- **Final fields** provide special initialization safety guarantees
- **Testing for memory visibility** issues requires stress testing and understanding of non-deterministic behavior

The Java Memory Model is complex, but understanding these fundamentals will help you write correct and efficient concurrent programs. Always prefer higher-level concurrency utilities when possible, and use explicit synchronization carefully when low-level control is needed.
# Java Memory Model Guide - Part 9: Memory Model Testing Strategies and Tools

## Introduction to Memory Model Testing

Testing concurrent code for memory model violations is challenging because issues are often non-deterministic and may only appear under specific conditions. This guide covers comprehensive strategies and tools for detecting memory visibility problems.

## Understanding the Challenge

### Why Memory Model Bugs Are Hard to Detect
```java
public class MemoryModelTestingChallenges {
    private boolean flag = false;
    private int data = 0;
    
    // Bug may not manifest in simple tests
    public void demonstrateHiddenBug() throws InterruptedException {
        Thread writer = new Thread(() -> {
            data = 42;        // Write data first
            flag = true;      // Then signal completion
        });
        
        Thread reader = new Thread(() -> {
            while (!flag) {
                Thread.yield(); // Busy wait for flag
            }
            // May read stale data = 0 instead of 42!
            System.out.println("Data read: " + data);
        });
        
        // This test might pass 99.9% of the time
        writer.start();
        reader.start();
        
        writer.join();
        reader.join();
    }
    
    // Stress test to increase chance of finding bugs
    public void stressTest() throws InterruptedException {
        final int iterations = 100_000;
        int bugCount = 0;
        
        for (int i = 0; i < iterations; i++) {
            flag = false;
            data = 0;
            
            final int expectedData = i + 1;
            boolean[] bugDetected = {false};
            CountDownLatch latch = new CountDownLatch(2);
            
            Thread writer = new Thread(() -> {
                try {
                    data = expectedData;
                    flag = true;
                } finally {
                    latch.countDown();
                }
            });
            
            Thread reader = new Thread(() -> {
                try {
                    while (!flag) {
                        Thread.yield();
                    }
                    if (data != expectedData) {
                        bugDetected[0] = true;
                    }
                } finally {
                    latch.countDown();
                }
            });
            
            writer.start();
            reader.start();
            
            latch.await();
            
            if (bugDetected[0]) {
                bugCount++;
            }
        }
        
        System.out.println("Memory model violations detected: " + bugCount + "/" + iterations);
        System.out.println("Bug rate: " + (bugCount * 100.0 / iterations) + "%");
    }
    
    public static void main(String[] args) throws InterruptedException {
        MemoryModelTestingChallenges demo = new MemoryModelTestingChallenges();
        
        System.out.println("Running stress test for memory model violations:");
        demo.stressTest();
    }
}
```

## Systematic Testing Approaches

### State-Based Testing
```java
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.CyclicBarrier;
import java.util.concurrent.atomic.AtomicInteger;

public class StateBasedTesting {
    
    // Test subject: Double-checked locking pattern
    public static class SingletonWithBug {
        private static Object instance;
        
        public static Object getInstance() {
            if (instance == null) {           // First check (not synchronized)
                synchronized (SingletonWithBug.class) {
                    if (instance == null) {   // Second check (synchronized)
                        instance = new Object(); // Bug: not volatile!
                    }
                }
            }
            return instance;
        }
    }
    
    // Correct version with volatile
    public static class SingletonCorrect {
        private static volatile Object instance;
        
        public static Object getInstance() {
            if (instance == null) {
                synchronized (SingletonCorrect.class) {
                    if (instance == null) {
                        instance = new Object();
                    }
                }
            }
            return instance;
        }
    }
    
    public void testDoubleCheckedLocking() throws InterruptedException {
        final int numThreads = 10;
        final int iterations = 10_000;
        
        System.out.println("Testing Double-Checked Locking Pattern:");
        System.out.println("======================================");
        
        // Test buggy version
        testSingletonImplementation("Buggy (non-volatile)", 
            SingletonWithBug::getInstance, numThreads, iterations);
        
        // Test correct version
        testSingletonImplementation("Correct (volatile)", 
            SingletonCorrect::getInstance, numThreads, iterations);
    }
    
    private void testSingletonImplementation(String name, 
            java.util.function.Supplier<Object> getInstance,
            int numThreads, int iterations) throws InterruptedException {
        
        AtomicInteger nullCount = new AtomicInteger(0);
        AtomicInteger uniqueInstances = new AtomicInteger(0);
        java.util.Set<Object> seenInstances = java.util.concurrent.ConcurrentHashMap.newKeySet();
        
        for (int iter = 0; iter < iterations; iter++) {
            // Reset singleton for each iteration
            resetSingleton(name.contains("Buggy"));
            
            CountDownLatch latch = new CountDownLatch(numThreads);
            CyclicBarrier barrier = new CyclicBarrier(numThreads);
            
            for (int t = 0; t < numThreads; t++) {
                new Thread(() -> {
                    try {
                        barrier.await(); // Synchronize start
                        Object instance = getInstance.get();
                        
                        if (instance == null) {
                            nullCount.incrementAndGet();
                        } else {
                            seenInstances.add(instance);
                        }
                    } catch (Exception e) {
                        e.printStackTrace();
                    } finally {
                        latch.countDown();
                    }
                }).start();
            }
            
            latch.await();
        }
        
        System.out.printf("%s: Null instances: %d, Unique instances: %d%n", 
            name, nullCount.get(), seenInstances.size());
        
        if (seenInstances.size() > 1) {
            System.out.println("  ❌ MEMORY MODEL VIOLATION: Multiple instances created!");
        } else if (nullCount.get() > 0) {
            System.out.println("  ❌ MEMORY MODEL VIOLATION: Null instances observed!");
        } else {
            System.out.println("  ✅ No violations detected in this run");
        }
    }
    
    private void resetSingleton(boolean isBuggy) {
        try {
            if (isBuggy) {
                java.lang.reflect.Field field = SingletonWithBug.class.getDeclaredField("instance");
                field.setAccessible(true);
                field.set(null, null);
            } else {
                java.lang.reflect.Field field = SingletonCorrect.class.getDeclaredField("instance");
                field.setAccessible(true);
                field.set(null, null);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
    
    public static void main(String[] args) throws InterruptedException {
        StateBasedTesting test = new StateBasedTesting();
        test.testDoubleCheckedLocking();
    }
}
```

### Property-Based Testing
```java
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

public class PropertyBasedTesting {
    
    // Test subject: A counter with potential race conditions
    public static class UnsafeCounter {
        private int count = 0;
        
        public void increment() {
            count++; // Race condition!
        }
        
        public int getCount() {
            return count;
        }
    }
    
    public static class VolatileCounter {
        private volatile int count = 0;
        
        public void increment() {
            count++; // Still has race condition!
        }
        
        public int getCount() {
            return count;
        }
    }
    
    public static class SynchronizedCounter {
        private int count = 0;
        
        public synchronized void increment() {
            count++;
        }
        
        public synchronized int getCount() {
            return count;
        }
    }
    
    public static class AtomicCounter {
        private final AtomicInteger count = new AtomicInteger(0);
        
        public void increment() {
            count.incrementAndGet();
        }
        
        public int getCount() {
            return count.get();
        }
    }
    
    // Property: Total increments should equal final count
    public void testCounterProperty() throws InterruptedException {
        final int numThreads = 8;
        final int incrementsPerThread = 10_000;
        final int expectedTotal = numThreads * incrementsPerThread;
        
        System.out.println("Property-Based Testing: Counter Correctness");
        System.out.println("===========================================");
        
        testCounterImplementation("Unsafe", new UnsafeCounter(), 
            numThreads, incrementsPerThread, expectedTotal);
        
        testCounterImplementation("Volatile", new VolatileCounter(), 
            numThreads, incrementsPerThread, expectedTotal);
        
        testCounterImplementation("Synchronized", new SynchronizedCounter(), 
            numThreads, incrementsPerThread, expectedTotal);
        
        testCounterImplementation("Atomic", new AtomicCounter(), 
            numThreads, incrementsPerThread, expectedTotal);
    }
    
    private void testCounterImplementation(String name, Object counter,
            int numThreads, int incrementsPerThread, int expectedTotal) 
            throws InterruptedException {
        
        CountDownLatch latch = new CountDownLatch(numThreads);
        AtomicLong actualIncrements = new AtomicLong(0);
        
        long startTime = System.nanoTime();
        
        for (int t = 0; t < numThreads; t++) {
            new Thread(() -> {
                try {
                    for (int i = 0; i < incrementsPerThread; i++) {
                        if (counter instanceof UnsafeCounter) {
                            ((UnsafeCounter) counter).increment();
                        } else if (counter instanceof VolatileCounter) {
                            ((VolatileCounter) counter).increment();
                        } else if (counter instanceof SynchronizedCounter) {
                            ((SynchronizedCounter) counter).increment();
                        } else if (counter instanceof AtomicCounter) {
                            ((AtomicCounter) counter).increment();
                        }
                        actualIncrements.incrementAndGet();
                    }
                } finally {
                    latch.countDown();
                }
            }).start();
        }
        
        latch.await();
        long endTime = System.nanoTime();
        
        int finalCount = 0;
        if (counter instanceof UnsafeCounter) {
            finalCount = ((UnsafeCounter) counter).getCount();
        } else if (counter instanceof VolatileCounter) {
            finalCount = ((VolatileCounter) counter).getCount();
        } else if (counter instanceof SynchronizedCounter) {
            finalCount = ((SynchronizedCounter) counter).getCount();
        } else if (counter instanceof AtomicCounter) {
            finalCount = ((AtomicCounter) counter).getCount();
        }
        
        double timeMs = (endTime - startTime) / 1_000_000.0;
        
        System.out.printf("%-12s: Expected: %8d, Actual: %8d, Time: %6.1f ms", 
            name, expectedTotal, finalCount, timeMs);
        
        if (finalCount == expectedTotal) {
            System.out.println(" ✅");
        } else {
            System.out.printf(" ❌ (Lost %d updates)%n", expectedTotal - finalCount);
        }
    }
    
    // Property: Monotonic progression test
    public void testMonotonicProperty() throws InterruptedException {
        System.out.println("\nMonotonic Progression Test:");
        System.out.println("===========================");
        
        AtomicInteger counter = new AtomicInteger(0);
        volatile boolean stop = false;
        AtomicInteger violations = new AtomicInteger(0);
        
        // Writer thread
        Thread writer = new Thread(() -> {
            for (int i = 1; i <= 100_000; i++) {
                counter.set(i);
                if (ThreadLocalRandom.current().nextInt(1000) == 0) {
                    Thread.yield(); // Occasional yield
                }
            }
            stop = true;
        });
        
        // Reader thread - checks for monotonic increases
        Thread reader = new Thread(() -> {
            int lastValue = 0;
            while (!stop) {
                int currentValue = counter.get();
                if (currentValue < lastValue && currentValue != 0) {
                    violations.incrementAndGet();
                }
                lastValue = currentValue;
            }
        });
        
        writer.start();
        reader.start();
        
        writer.join();
        reader.join();
        
        if (violations.get() == 0) {
            System.out.println("✅ No monotonic violations detected");
        } else {
            System.out.println("❌ Monotonic violations: " + violations.get());
        }
    }
    
    public static void main(String[] args) throws InterruptedException {
        PropertyBasedTesting test = new PropertyBasedTesting();
        test.testCounterProperty();
        test.testMonotonicProperty();
    }
}
```

## JVM and Tool-Based Testing

### Using JVM Flags for Memory Model Testing
```java
public class JVMFlagTesting {
    
    /*
     * Key JVM flags for memory model testing:
     * 
     * -XX:+StressLCM              // Stress local code motion
     * -XX:+StressGCM              // Stress global code motion  
     * -XX:+StressCCP              // Stress conditional constant propagation
     * -XX:+StressIGVN             // Stress iterative global value numbering
     * -XX:StressSeed=<number>     // Set stress testing seed
     * 
     * -Xint                       // Interpreter only (no optimizations)
     * -Xcomp                      // Compile everything (aggressive opts)
     * 
     * -XX:+PrintCompilation       // Show compilation events
     * -XX:+UnlockDiagnosticVMOptions -XX:+PrintInlining // Show inlining
     * 
     * -XX:+UseG1GC                // Different GC can affect timing
     * -XX:+UseParallelGC
     * -XX:+UseSerialGC
     */
    
    private boolean flag = false;
    private int data = 0;
    
    public void runMemoryModelTest() throws InterruptedException {
        System.out.println("JVM Flag Testing for Memory Model Issues");
        System.out.println("Run with different JVM flags to stress test:");
        System.out.println("  java -XX:+StressLCM -XX:+StressGCM YourClass");
        System.out.println("  java -Xint YourClass");
        System.out.println("  java -Xcomp YourClass");
        System.out.println();
        
        final int iterations = 50_000;
        int violations = 0;
        
        for (int i = 0; i < iterations; i++) {
            flag = false;
            data = 0;
            
            CountDownLatch latch = new CountDownLatch(2);
            final int expectedValue = i + 1;
            boolean[] violationDetected = {false};
            
            Thread writer = new Thread(() -> {
                try {
                    data = expectedValue;
                    flag = true;
                } finally {
                    latch.countDown();
                }
            });
            
            Thread reader = new Thread(() -> {
                try {
                    while (!flag) {
                        // Busy wait - vulnerable to compiler optimizations
                    }
                    if (data != expectedValue) {
                        violationDetected[0] = true;
                    }
                } finally {
                    latch.countDown();
                }
            });
            
            writer.start();
            reader.start();
            
            latch.await();
            
            if (violationDetected[0]) {
                violations++;
            }
        }
        
        System.out.printf("Violations detected: %d/%d (%.2f%%)%n", 
            violations, iterations, violations * 100.0 / iterations);
        
        // Print JVM information
        System.out.println("\nJVM Information:");
        System.out.println("  Java version: " + System.getProperty("java.version"));
        System.out.println("  JVM name: " + System.getProperty("java.vm.name"));
        System.out.println("  JVM vendor: " + System.getProperty("java.vm.vendor"));
        
        // Check if compilation monitoring is available
        var compilationBean = java.lang.management.ManagementFactory.getCompilationMXBean();
        if (compilationBean != null) {
            System.out.println("  JIT compilation time: " + 
                compilationBean.getTotalCompilationTime() + " ms");
        }
    }
    
    public static void main(String[] args) throws InterruptedException {
        JVMFlagTesting test = new JVMFlagTesting();
        test.runMemoryModelTest();
    }
}
```

### Thread Sanitizer-Style Testing
```java
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

public class ThreadSanitizerStyleTesting {
    
    // Data race detector implementation
    public static class DataRaceDetector {
        private final ConcurrentHashMap<String, ThreadAccess> lastAccess = new ConcurrentHashMap<>();
        
        private static class ThreadAccess {
            final long threadId;
            final boolean isWrite;
            final long timestamp;
            final StackTraceElement[] stackTrace;
            
            ThreadAccess(long threadId, boolean isWrite, StackTraceElement[] stackTrace) {
                this.threadId = threadId;
                this.isWrite = isWrite;
                this.timestamp = System.nanoTime();
                this.stackTrace = stackTrace;
            }
        }
        
        public void recordAccess(String fieldName, boolean isWrite) {
            if (!isMonitoringEnabled()) return;
            
            long currentThread = Thread.currentThread().getId();
            StackTraceElement[] stackTrace = Thread.currentThread().getStackTrace();
            ThreadAccess currentAccess = new ThreadAccess(currentThread, isWrite, stackTrace);
            
            ThreadAccess previous = lastAccess.put(fieldName, currentAccess);
            
            if (previous != null && previous.threadId != currentThread) {
                // Different threads accessing the same field
                if (isWrite || previous.isWrite) {
                    // At least one write operation - potential race
                    reportDataRace(fieldName, previous, currentAccess);
                }
            }
        }
        
        private void reportDataRace(String fieldName, ThreadAccess previous, ThreadAccess current) {
            System.err.println("POTENTIAL DATA RACE DETECTED:");
            System.err.println("Field: " + fieldName);
            System.err.println("Previous access:");
            System.err.println("  Thread: " + previous.threadId + 
                " (" + (previous.isWrite ? "WRITE" : "READ") + ")");
            System.err.println("  Stack trace: " + java.util.Arrays.toString(previous.stackTrace));
            System.err.println("Current access:");
            System.err.println("  Thread: " + current.threadId + 
                " (" + (current.isWrite ? "WRITE" : "READ") + ")");
            System.err.println("  Stack trace: " + java.util.Arrays.toString(current.stackTrace));
            System.err.println();
        }
        
        private boolean isMonitoringEnabled() {
            return true; // In real implementation, might be configurable
        }
    }
    
    // Test subject with instrumented field access
    public static class InstrumentedClass {
        private static final DataRaceDetector detector = new DataRaceDetector();
        private int unsafeField = 0;
        private volatile int safeField = 0;
        
        public void setUnsafeField(int value) {
            detector.recordAccess("unsafeField", true);
            this.unsafeField = value;
        }
        
        public int getUnsafeField() {
            detector.recordAccess("unsafeField", false);
            return this.unsafeField;
        }
        
        public void setSafeField(int value) {
            detector.recordAccess("safeField", true);
            this.safeField = value;
        }
        
        public int getSafeField() {
            detector.recordAccess("safeField", false);
            return this.safeField;
        }
    }
    
    public void testDataRaceDetection() throws InterruptedException {
        System.out.println("Thread Sanitizer-Style Data Race Detection:");
        System.out.println("==========================================");
        
        InstrumentedClass testObject = new InstrumentedClass();
        final int numThreads = 4;
        final int operationsPerThread = 1000;
        
        CountDownLatch latch = new CountDownLatch(numThreads);
        
        for (int t = 0; t < numThreads; t++) {
            final int threadIndex = t;
            new Thread(() -> {
                try {
                    for (int i = 0; i < operationsPerThread; i++) {
                        // Generate mix of reads and writes
                        if (i % 2 == 0) {
                            testObject.setUnsafeField(threadIndex * 1000 + i);
                            testObject.setSafeField(threadIndex * 1000 + i);
                        } else {
                            testObject.getUnsafeField();
                            testObject.getSafeField();
                        }
                        
                        // Add some randomness to increase chance of races
                        if (i % 100 == 0) {
                            Thread.yield();
                        }
                    }
                } finally {
                    latch.countDown();
                }
            }, "TestThread-" + threadIndex).start();
        }
        
        latch.await();
        System.out.println("Data race detection test completed.");
    }
    
    public static void main(String[] args) throws InterruptedException {
        ThreadSanitizerStyleTesting test = new ThreadSanitizerStyleTesting();
        test.testDataRaceDetection();
    }
}
```

## Litmus Testing

### Classic Memory Model Litmus Tests
```java
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicInteger;

public class LitmusTests {
    
    // Litmus Test 1: Store Buffering (SB)
    public static class StoreBufferingTest {
        private volatile int x = 0;
        private volatile int y = 0;
        
        public void runTest(int iterations) throws InterruptedException {
            AtomicInteger case00 = new AtomicInteger(0); // r1=0, r2=0
            AtomicInteger case01 = new AtomicInteger(0); // r1=0, r2=1
            AtomicInteger case10 = new AtomicInteger(0); // r1=1, r2=0
            AtomicInteger case11 = new AtomicInteger(0); // r1=1, r2=1
            
            for (int i = 0; i < iterations; i++) {
                x = 0;
                y = 0;
                
                CountDownLatch latch = new CountDownLatch(2);
                int[] results = new int[2];
                
                // Thread 1: x = 1; r1 = y
                Thread t1 = new Thread(() -> {
                    try {
                        x = 1;
                        results[0] = y; // r1
                    } finally {
                        latch.countDown();
                    }
                });
                
                // Thread 2: y = 1; r2 = x  
                Thread t2 = new Thread(() -> {
                    try {
                        y = 1;
                        results[1] = x; // r2
                    } finally {
                        latch.countDown();
                    }
                });
                
                t1.start();
                t2.start();
                latch.await();
                
                // Categorize results
                if (results[0] == 0 && results[1] == 0) case00.incrementAndGet();
                else if (results[0] == 0 && results[1] == 1) case01.incrementAndGet();
                else if (results[0] == 1 && results[1] == 0) case10.incrementAndGet();
                else if (results[0] == 1 && results[1] == 1) case11.incrementAndGet();
            }
            
            System.out.println("Store Buffering Test Results (volatile):");
            System.out.printf("  r1=0,r2=0: %6d (%5.1f%%)%n", case00.get(), 
                case00.get() * 100.0 / iterations);
            System.out.printf("  r1=0,r2=1: %6d (%5.1f%%)%n", case01.get(), 
                case01.get() * 100.0 / iterations);
            System.out.printf("  r1=1,r2=0: %6d (%5.1f%%)%n", case10.get(), 
                case10.get() * 100.0 / iterations);
            System.out.printf("  r1=1,r2=1: %6d (%5.1f%%)%n", case11.get(), 
                case11.get() * 100.0 / iterations);
            
            // In Java with volatile, r1=0,r2=0 should be impossible
            if (case00.get() > 0) {
                System.out.println("  ❌ VIOLATION: r1=0,r2=0 should be impossible with volatile!");
            } else {
                System.out.println("  ✅ No sequential consistency violations detected");
            }
        }
    }
    
    // Litmus Test 2: Load Buffering (LB)
    public static class LoadBufferingTest {
        private volatile int x = 0;
        private volatile int y = 0;
        
        public void runTest(int iterations) throws InterruptedException {
            AtomicInteger case00 = new AtomicInteger(0);
            AtomicInteger case01 = new AtomicInteger(0);
            AtomicInteger case10 = new AtomicInteger(0);
            AtomicInteger case11 = new AtomicInteger(0);
            
            for (int i = 0; i < iterations; i++) {
                x = 0;
                y = 0;
                
                CountDownLatch latch = new CountDownLatch(2);
                int[] results = new int[2];
                
                // Thread 1: r1 = x; y = 1
                Thread t1 = new Thread(() -> {
                    try {
                        results[0] = x; // r1
                        y = 1;
                    } finally {
                        latch.countDown();
                    }
                });
                
                // Thread 2: r2 = y; x = 1
                Thread t2 = new Thread(() -> {
                    try {
                        results[1] = y; // r2
                        x = 1;
                    } finally {
                        latch.countDown();
                    }
                });
                
                t1.start();
                t2.start();
                latch.await();
                
                if (results[0] == 0 && results[1] == 0) case00.incrementAndGet();
                else if (results[0] == 0 && results[1] == 1) case01.incrementAndGet();
                else if (results[0] == 1 && results[1] == 0) case10.incrementAndGet();
                else if (results[0] == 1 && results[1] == 1) case11.incrementAndGet();
            }
            
            System.out.println("\nLoad Buffering Test Results (volatile):");
            System.out.printf("  r1=0,r2=0: %6d (%5.1f%%)%n", case00.get(), 
                case00.get() * 100.0 / iterations);
            System.out.printf("  r1=0,r2=1: %6d (%5.1f%%)%n", case01.get(), 
                case01.get() * 100.0 / iterations);
            System.out.printf("  r1=1,r2=0: %6d (%5.1f%%)%n", case10.get(), 
                case10.get() * 100.0 / iterations);
            System.out.printf("  r1=1,r2=1: %6d (%5.1f%%)%n", case11.get(), 
                case11.get() * 100.0 / iterations);
            
            if (case11.get() > 0) {
                System.out.println("  ❌ POTENTIAL VIOLATION: r1=1,r2=1 observed");
            } else {
                System.out.println("  ✅ No load buffering violations detected");
            }
        }
    }
    
    // Litmus Test 3: Message Passing (MP)
    public static class MessagePassingTest {
        private int data = 0;      // Plain field
        private volatile boolean flag = false; // Volatile flag
        
        public void runTest(int iterations) throws InterruptedException {
            AtomicInteger correctReads = new AtomicInteger(0);
            AtomicInteger staleReads = new AtomicInteger(0);
            
            for (int i = 0; i < iterations; i++) {
                data = 0;
                flag = false;
                
                CountDownLatch latch = new CountDownLatch(2);
                final int expectedData = i + 1;
                boolean[] isStale = {false};
                
                // Publisher thread
                Thread publisher = new Thread(() -> {
                    try {
                        data = expectedData; // Write data first
                        flag = true;         // Then signal (volatile write)
                    } finally {
                        latch.countDown();
                    }
                });
                
                // Subscriber thread
                Thread subscriber = new Thread(() -> {
                    try {
                        while (!flag) {      // Wait for volatile flag
                            // Busy wait
                        }
                        // Should see the data write due to happens-before
                        if (data != expectedData) {
                            isStale[0] = true;
                        }
                    } finally {
                        latch.countDown();
                    }
                });
                
                publisher.start();
                subscriber.start();
                latch.await();
                
                if (isStale[0]) {
                    staleReads.incrementAndGet();
                } else {
                    correctReads.incrementAndGet();
                }
            }
            
            System.out.println("\nMessage Passing Test Results:");
            System.out.printf("  Correct reads: %6d (%5.1f%%)%n", correctReads.get(), 
                correctReads.get() * 100.0 / iterations);
            System.out.printf("  Stale reads:   %6d (%5.1f%%)%n", staleReads.get(), 
                staleReads.get() * 100.0 / iterations);
            
            if (staleReads.get() > 0) {
                System.out.println("  ❌ VIOLATION: Stale reads detected - happens-before violated!");
            } else {
                System.out.println("  ✅ All reads consistent with happens-before ordering");
            }
        }
    }
    
    public static void main(String[] args) throws InterruptedException {
        final int iterations = 100_000;
        
        System.out.println("Litmus Tests for Java Memory Model:");
        System.out.println("===================================");
        
        StoreBufferingTest sb = new StoreBufferingTest();
        sb.runTest(iterations);
        
        LoadBufferingTest lb = new LoadBufferingTest();
        lb.runTest(iterations);
        
        MessagePassingTest mp = new MessagePassingTest();
        mp.runTest(iterations);
    }
}
```

## Automated Testing Tools and Frameworks

### JCStress Integration
```java
/*
 * JCStress (Java Concurrency Stress) is the gold standard for testing
 * concurrent Java code. Here's how to integrate it:
 * 
 * 1. Add JCStress dependency:
 *    <dependency>
 *        <groupId>org.openjdk.jcstress</groupId>
 *        <artifactId>jcstress-core</artifactId>
 *        <version>0.16</version>
 *    </dependency>
 * 
 * 2. Create test classes using JCStress annotations
 */

import org.openjdk.jcstress.annotations.*;
import org.openjdk.jcstress.infra.results.II_Result;
import org.openjdk.jcstress.infra.results.I_Result;

public class JCStressExamples {
    
    // Example 1: Testing volatile semantics
    @JCStressTest
    @Outcome(id = "0, 0", expect = Expect.FORBIDDEN, desc = "Sequential consistency violation")
    @Outcome(id = "0, 1", expect = Expect.ACCEPTABLE, desc = "T2 runs first")
    @Outcome(id = "1, 0", expect = Expect.ACCEPTABLE, desc = "T1 runs first")  
    @Outcome(id = "1, 1", expect = Expect.ACCEPTABLE, desc = "Both complete")
    @State
    public static class VolatileStoreBuffering {
        volatile int x = 0;
        volatile int y = 0;
        
        @Actor
        public void actor1(II_Result r) {
            x = 1;
            r.r1 = y;
        }
        
        @Actor  
        public void actor2(II_Result r) {
            y = 1;
            r.r2 = x;
        }
    }
    
    // Example 2: Testing plain field races
    @JCStressTest
    @Outcome(id = "0", expect = Expect.ACCEPTABLE_INTERESTING, desc = "Lost update - race condition")
    @Outcome(expect = Expect.ACCEPTABLE, desc = "Normal result")
    @State
    public static class PlainFieldRace {
        int counter = 0;
        
        @Actor
        public void actor1() {
            counter++;
        }
        
        @Actor
        public void actor2() {
            counter++;
        }
        
        @Arbiter
        public void arbiter(I_Result r) {
            r.r1 = counter;
        }
    }
    
    // Example 3: Testing happens-before relationships  
    @JCStressTest
    @Outcome(id = "0", expect = Expect.FORBIDDEN, desc = "Happens-before violation")
    @Outcome(id = "42", expect = Expect.ACCEPTABLE, desc = "Correct value observed")
    @State
    public static class HappensBeforeTest {
        int data = 0;
        volatile boolean ready = false;
        
        @Actor
        public void publisher() {
            data = 42;      // Write data
            ready = true;   // Signal ready (volatile write)
        }
        
        @Actor
        public void subscriber(I_Result r) {
            if (ready) {    // Check ready (volatile read)
                r.r1 = data; // Should see data = 42 due to happens-before
            }
        }
    }
}
```

### Custom Testing Framework
```java
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.CyclicBarrier;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Consumer;

public class CustomMemoryModelTestFramework {
    
    public static class TestCase {
        private final String name;
        private final Runnable setup;
        private final Runnable[] actors;
        private final Consumer<TestResult> validator;
        
        public TestCase(String name, Runnable setup, Consumer<TestResult> validator, Runnable... actors) {
            this.name = name;
            this.setup = setup;
            this.actors = actors;
            this.validator = validator;
        }
        
        public String getName() { return name; }
        public Runnable getSetup() { return setup; }
        public Runnable[] getActors() { return actors; }
        public Consumer<TestResult> getValidator() { return validator; }
    }
    
    public static class TestResult {
        private final int iteration;
        private final Object[] actorResults;
        private final Object finalState;
        
        public TestResult(int iteration, Object[] actorResults, Object finalState) {
            this.iteration = iteration;
            this.actorResults = actorResults;
            this.finalState = finalState;
        }
        
        public int getIteration() { return iteration; }
        public Object[] getActorResults() { return actorResults; }
        public Object getFinalState() { return finalState; }
    }
    
    public static class TestRunner {
        private final int iterations;
        private final boolean enableRandomization;
        
        public TestRunner(int iterations, boolean enableRandomization) {
            this.iterations = iterations;
            this.enableRandomization = enableRandomization;
        }
        
        public void runTest(TestCase testCase) throws InterruptedException {
            System.out.println("Running test: " + testCase.getName());
            System.out.println("Iterations: " + iterations);
            
            AtomicInteger violationCount = new AtomicInteger(0);
            
            for (int i = 0; i < iterations; i++) {
                // Setup phase
                testCase.getSetup().run();
                
                // Actor execution phase
                Object[] results = executeActors(testCase.getActors());
                
                // Validation phase
                TestResult result = new TestResult(i, results, null);
                try {
                    testCase.getValidator().accept(result);
                } catch (AssertionError e) {
                    violationCount.incrementAndGet();
                    if (violationCount.get() <= 5) { // Log first few violations
                        System.err.println("Violation in iteration " + i + ": " + e.getMessage());
                    }
                }
            }
            
            int violations = violationCount.get();
            System.out.printf("Test completed: %d violations out of %d iterations (%.2f%%)%n",
                violations, iterations, violations * 100.0 / iterations);
            
            if (violations == 0) {
                System.out.println("✅ Test passed - no violations detected");
            } else {
                System.out.println("❌ Test failed - violations detected");
            }
            System.out.println();
        }
        
        private Object[] executeActors(Runnable[] actors) throws InterruptedException {
            CountDownLatch latch = new CountDownLatch(actors.length);
            CyclicBarrier barrier = new CyclicBarrier(actors.length);
            Object[] results = new Object[actors.length];
            
            for (int i = 0; i < actors.length; i++) {
                final int actorIndex = i;
                new Thread(() -> {
                    try {
                        // Add randomization to increase chance of finding races
                        if (enableRandomization && ThreadLocalRandom.current().nextBoolean()) {
                            Thread.yield();
                        }
                        
                        barrier.await(); // Synchronize start
                        actors[actorIndex].run();
                        
                    } catch (Exception e) {
                        e.printStackTrace();
                    } finally {
                        latch.countDown();
                    }
                }).start();
            }
            
            latch.await();
            return results;
        }
    }
    
    // Example test cases using the framework
    public static void demonstrateFramework() throws InterruptedException {
        TestRunner runner = new TestRunner(50_000, true);
        
        // Test case 1: Volatile store buffering
        TestCase volatileTest = createVolatileStoreBufferingTest();
        runner.runTest(volatileTest);
        
        // Test case 2: Plain field race condition
        TestCase raceTest = createRaceConditionTest();
        runner.runTest(raceTest);
        
        // Test case 3: Message passing test
        TestCase messageTest = createMessagePassingTest();
        runner.runTest(messageTest);
    }
    
    private static TestCase createVolatileStoreBufferingTest() {
        return new TestCase(
            "Volatile Store Buffering",
            () -> {
                // Setup is handled in actors
            },
            (result) -> {
                // No specific validation - just observe outcomes
            },
            () -> {
                // Actor 1: x = 1; r1 = y
                // Implementation would need shared state
            },
            () -> {
                // Actor 2: y = 1; r2 = x  
                // Implementation would need shared state
            }
        );
    }
    
    private static TestCase createRaceConditionTest() {
        AtomicInteger sharedCounter = new AtomicInteger(0);
        
        return new TestCase(
            "Race Condition Test",
            () -> sharedCounter.set(0),
            (result) -> {
                int finalValue = sharedCounter.get();
                if (finalValue != 2) {
                    throw new AssertionError("Expected 2, got " + finalValue);
                }
            },
            () -> sharedCounter.incrementAndGet(),
            () -> sharedCounter.incrementAndGet()
        );
    }
    
    private static TestCase createMessagePassingTest() {
        // Would need shared state implementation
        return new TestCase(
            "Message Passing Test",
            () -> {
                // Reset shared state
            },
            (result) -> {
                // Validate happens-before relationship
            },
            () -> {
                // Publisher actor
            },
            () -> {
                // Subscriber actor
            }
        );
    }
    
    public static void main(String[] args) throws InterruptedException {
        System.out.println("Custom Memory Model Test Framework Demo:");
        System.out.println("=======================================");
        demonstrateFramework();
    }
}
```

## Debugging Memory Model Issues

### Systematic Debugging Approach
```java
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicReference;

public class MemoryModelDebugging {
    
    // Example: Debugging a subtle publication problem
    public static class ConfigManager {
        private Config config;  // Not volatile - potential problem!
        
        public void updateConfig(Config newConfig) {
            this.config = newConfig;  // Unsafe publication
        }
        
        public Config getConfig() {
            return this.config;  // May return null or partially constructed object
        }
    }
    
    public static class Config {
        private final String serverUrl;
        private final int timeout;
        private final boolean debugMode;
        
        public Config(String serverUrl, int timeout, boolean debugMode) {
            this.serverUrl = serverUrl;
            this.timeout = timeout;
            this.debugMode = debugMode;
        }
        
        public String getServerUrl() { return serverUrl; }
        public int getTimeout() { return timeout; }
        public boolean isDebugMode() { return debugMode; }
    }
    
    // Debugging technique 1: Add logging and assertions
    public static class DebuggingConfigManager {
        private volatile Config config;  // Fixed: made volatile
        private final AtomicReference<String> lastUpdate = new AtomicReference<>();
        
        public void updateConfig(Config newConfig) {
            if (newConfig == null) {
                throw new IllegalArgumentException("Config cannot be null");
            }
            
            // Log the update
            String updateInfo = String.format("Thread %d updating config at %d", 
                Thread.currentThread().getId(), System.nanoTime());
            lastUpdate.set(updateInfo);
            
            this.config = newConfig;
            
            // Verify the update took effect
            Config verify = this.config;
            if (verify != newConfig) {
                System.err.println("WARNING: Config update verification failed!");
            }
        }
        
        public Config getConfig() {
            Config result = this.config;
            
            // Add assertion to catch null reads
            if (result == null) {
                String lastUpdateInfo = lastUpdate.get();
                System.err.println("WARNING: Read null config. Last update: " + lastUpdateInfo);
            }
            
            return result;
        }
    }
    
    // Debugging technique 2: State validation
    public static class ValidatingConfigManager {
        private volatile Config config;
        private volatile long configVersion = 0;
        
        public void updateConfig(Config newConfig) {
            long newVersion = configVersion + 1;
            this.config = newConfig;
            this.configVersion = newVersion;  // Should be visible after config
        }
        
        public Config getConfig() {
            long versionBefore = configVersion;
            Config result = config;
            long versionAfter = configVersion;
            
            // Detect if update happened during read
            if (versionBefore != versionAfter) {
                System.err.println("WARNING: Concurrent update detected during read");
            }
            
            // Detect if config is null but version > 0
            if (result == null && versionAfter > 0) {
                System.err.println("ERROR: Config is null but version is " + versionAfter);
            }
            
            return result;
        }
    }
    
    // Test to demonstrate debugging
    public void demonstrateDebugging() throws InterruptedException {
        System.out.println("Memory Model Debugging Demonstration:");
        System.out.println("====================================");
        
        // Test problematic version
        testConfigManager("Problematic", new ConfigManager());
        
        // Test debugging version
        testConfigManager("Debug", new DebuggingConfigManager());
        
        // Test validating version
        testConfigManager("Validating", new ValidatingConfigManager());
    }
    
    private void testConfigManager(String name, Object manager) throws InterruptedException {
        System.out.println("\nTesting " + name + " ConfigManager:");
        
        final int iterations = 10_000;
        AtomicReference<Exception> caughtException = new AtomicReference<>();
        CountDownLatch latch = new CountDownLatch(2);
        
        // Writer thread
        Thread writer = new Thread(() -> {
            try {
                for (int i = 0; i < iterations; i++) {
                    Config newConfig = new Config("server" + i, 1000 + i, i % 2 == 0);
                    
                    if (manager instanceof ConfigManager) {
                        ((ConfigManager) manager).updateConfig(newConfig);
                    } else if (manager instanceof DebuggingConfigManager) {
                        ((DebuggingConfigManager) manager).updateConfig(newConfig);
                    } else if (manager instanceof ValidatingConfigManager) {
                        ((ValidatingConfigManager) manager).updateConfig(newConfig);
                    }
                    
                    if (i % 1000 == 0) {
                        Thread.yield(); // Occasional yield
                    }
                }
            } catch (Exception e) {
                caughtException.set(e);
            } finally {
                latch.countDown();
            }
        });
        
        // Reader thread
        Thread reader = new Thread(() -> {
            try {
                int nullReads = 0;
                int validReads = 0;
                
                for (int i = 0; i < iterations; i++) {
                    Config config = null;
                    
                    if (manager instanceof ConfigManager) {
                        config = ((ConfigManager) manager).getConfig();
                    } else if (manager instanceof DebuggingConfigManager) {
                        config = ((DebuggingConfigManager) manager).getConfig();
                    } else if (manager instanceof ValidatingConfigManager) {
                        config = ((ValidatingConfigManager) manager).getConfig();
                    }
                    
                    if (config == null) {
                        nullReads++;
                    } else {
                        validReads++;
                        // Validate config fields
                        try {
                            String url = config.getServerUrl();
                            int timeout = config.getTimeout();
                            boolean debug = config.isDebugMode();
                            
                            if (url == null) {
                                System.err.println("ERROR: Config has null serverUrl");
                            }
                        } catch (Exception e) {
                            System.err.println("ERROR: Exception reading config fields: " + e);
                        }
                    }
                    
                    if (i % 1000 == 0) {
                        Thread.yield();
                    }
                }
                
                System.out.printf("  Null reads: %d, Valid reads: %d%n", nullReads, validReads);
                if (nullReads > 0) {
                    System.out.printf("  ❌ %d null reads detected%n", nullReads);
                } else {
                    System.out.println("  ✅ No null reads detected");
                }
                
            } catch (Exception e) {
                caughtException.set(e);
            } finally {
                latch.countDown();
            }
        });
        
        writer.start();
        reader.start();
        
        latch.await();
        
        if (caughtException.get() != null) {
            System.err.println("Exception during test: " + caughtException.get());
        }
    }
    
    public static void main(String[] args) throws InterruptedException {
        MemoryModelDebugging demo = new MemoryModelDebugging();
        demo.demonstrateDebugging();
    }
}
```

## Best Practices for Memory Model Testing

### Testing Strategy Summary
```java
public class TestingBestPractices {
    
    /**
     * COMPREHENSIVE TESTING STRATEGY FOR MEMORY MODEL ISSUES
     * ====================================================
     * 
     * 1. UNIT TESTING LEVEL:
     *    - Use JCStress for low-level memory model testing
     *    - Test individual synchronization primitives
     *    - Verify happens-before relationships
     * 
     * 2. INTEGRATION TESTING LEVEL:
     *    - Test component interactions under load
     *    - Verify data consistency across components
     *    - Test with realistic workloads
     * 
     * 3. STRESS TESTING LEVEL:
     *    - Run with high thread counts
     *    - Use different JVM flags (-Xint, -Xcomp, stress flags)
     *    - Test on different hardware architectures
     * 
     * 4. PRODUCTION-LIKE TESTING:
     *    - Test with production garbage collectors
     *    - Use production-like data volumes
     *    - Test under realistic contention scenarios
     */
    
    public static void printTestingChecklist() {
        System.out.println("MEMORY MODEL TESTING CHECKLIST");
        System.out.println("==============================");
        System.out.println();
        
        System.out.println("✓ Pre-Testing Setup:");
        System.out.println("  □ Identify shared mutable state");
        System.out.println("  □ Document synchronization strategy");
        System.out.println("  □ Identify critical sections");
        System.out.println("  □ List happens-before relationships");
        System.out.println();
        
        System.out.println("✓ Test Design:");
        System.out.println("  □ Create litmus tests for critical paths");
        System.out.println("  □ Design property-based tests");
        System.out.println("  □ Plan stress test scenarios");
        System.out.println("  □ Identify invariants to validate");
        System.out.println();
        
        System.out.println("✓ Test Execution:");
        System.out.println("  □ Run with multiple JVM configurations");
        System.out.println("  □ Test with different thread counts");
        System.out.println("  □ Use different hardware configurations");
        System.out.println("  □ Test with various GC settings");
        System.out.println();
        
        System.out.println("✓ Result Analysis:");
        System.out.println("  □ Track violation rates over time");
        System.out.println("  □ Analyze patterns in failures");
        System.out.println("  □ Correlate failures with system conditions");
        System.out.println("  □ Document failure scenarios");
        System.out.println();
        
        System.out.println("✓ Tools and Frameworks:");
        System.out.println("  □ JCStress for memory model testing");
        System.out.println("  □ Custom property-based test framework");
        System.out.println("  □ Thread sanitizer-style detection");
        System.out.println("  □ Monitoring and logging infrastructure");
    }
    
    public static void main(String[] args) {
        printTestingChecklist();
    }
}
```

## Summary

Effective memory model testing requires a multi-layered approach:

**Key Testing Strategies:**

1. **Litmus Tests**: Verify fundamental memory model guarantees
2. **Property-Based Testing**: Validate high-level correctness properties
3. **Stress Testing**: Find race conditions through intensive execution
4. **State Validation**: Detect inconsistent intermediate states

**Essential Tools:**

1. **JCStress**: Industry-standard memory model testing framework
2. **JVM Flags**: Control optimization and stress compilation
3. **Custom Frameworks**: Test application-specific scenarios
4. **Monitoring**: Track violations and patterns over time

**Best Practices:**

- Test across different JVM configurations and hardware
- Use systematic debugging approaches with logging and assertions
- Validate both safety (correctness) and liveness (progress) properties
- Combine multiple testing approaches for comprehensive coverage

The next part will cover production debugging of memory visibility issues to help you handle real-world problems.
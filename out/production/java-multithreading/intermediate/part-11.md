# Java Memory Model Guide - Part 11: Advanced Production Tools and Emergency Response

## Production Debugging Tools

### Custom Memory Model Profiler
```java
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.ConcurrentHashMap;
import java.util.Map;
import java.util.HashMap;

public class MemoryModelProfiler {
    
    public static class FieldAccessProfiler {
        private final Map<String, FieldStats> fieldStats = new ConcurrentHashMap<>();
        private volatile boolean profilingEnabled = false;
        
        public static class FieldStats {
            final AtomicLong readCount = new AtomicLong();
            final AtomicLong writeCount = new AtomicLong();
            final AtomicLong contentionCount = new AtomicLong();
            volatile long lastAccessTime = 0;
            volatile long lastWriteTime = 0;
            final Map<Long, Long> threadAccessCounts = new ConcurrentHashMap<>();
            
            void recordRead() {
                readCount.incrementAndGet();
                lastAccessTime = System.nanoTime();
                long threadId = Thread.currentThread().getId();
                threadAccessCounts.merge(threadId, 1L, Long::sum);
            }
            
            void recordWrite() {
                writeCount.incrementAndGet();
                lastWriteTime = System.nanoTime();
                lastAccessTime = System.nanoTime();
                long threadId = Thread.currentThread().getId();
                threadAccessCounts.merge(threadId, 1L, Long::sum);
            }
            
            void recordContention() {
                contentionCount.incrementAndGet();
            }
        }
        
        public void enableProfiling() {
            profilingEnabled = true;
            System.out.println("Memory model profiling enabled");
        }
        
        public void disableProfiling() {
            profilingEnabled = false;
            System.out.println("Memory model profiling disabled");
        }
        
        public void recordFieldRead(String fieldName) {
            if (!profilingEnabled) return;
            
            fieldStats.computeIfAbsent(fieldName, k -> new FieldStats()).recordRead();
        }
        
        public void recordFieldWrite(String fieldName) {
            if (!profilingEnabled) return;
            
            fieldStats.computeIfAbsent(fieldName, k -> new FieldStats()).recordWrite();
        }
        
        public void recordFieldContention(String fieldName) {
            if (!profilingEnabled) return;
            
            fieldStats.computeIfAbsent(fieldName, k -> new FieldStats()).recordContention();
        }
        
        public void printProfile() {
            System.out.println("Field Access Profile:");
            System.out.println("====================");
            
            fieldStats.forEach((fieldName, stats) -> {
                System.out.printf("Field: %s%n", fieldName);
                System.out.printf("  Reads: %d, Writes: %d%n", 
                    stats.readCount.get(), stats.writeCount.get());
                System.out.printf("  Contention events: %d%n", stats.contentionCount.get());
                System.out.printf("  Read/Write ratio: %.2f%n", 
                    (double) stats.readCount.get() / Math.max(1, stats.writeCount.get()));
                System.out.printf("  Active threads: %d%n", stats.threadAccessCounts.size());
                
                // Calculate access pattern
                long totalAccesses = stats.readCount.get() + stats.writeCount.get();
                if (totalAccesses > 100) {
                    double contentionRate = (double) stats.contentionCount.get() / totalAccesses;
                    if (contentionRate > 0.1) {
                        System.out.println("  ⚠️  HIGH CONTENTION detected!");
                    }
                }
                
                System.out.println();
            });
        }
        
        public void detectHotspots() {
            System.out.println("Memory Model Hotspot Analysis:");
            System.out.println("==============================");
            
            // Find fields with high contention
            fieldStats.entrySet().stream()
                .filter(entry -> entry.getValue().contentionCount.get() > 0)
                .sorted((e1, e2) -> Long.compare(
                    e2.getValue().contentionCount.get(),
                    e1.getValue().contentionCount.get()))
                .limit(5)
                .forEach(entry -> {
                    String field = entry.getKey();
                    FieldStats stats = entry.getValue();
                    System.out.printf("  %s: %d contention events%n", 
                        field, stats.contentionCount.get());
                });
            
            // Find fields with unbalanced access patterns
            System.out.println("\nUnbalanced Access Patterns:");
            fieldStats.entrySet().stream()
                .filter(entry -> {
                    FieldStats stats = entry.getValue();
                    long reads = stats.readCount.get();
                    long writes = stats.writeCount.get();
                    return writes > 0 && (reads / (double) writes > 100 || writes / (double) reads > 100);
                })
                .forEach(entry -> {
                    String field = entry.getKey();
                    FieldStats stats = entry.getValue();
                    double ratio = (double) stats.readCount.get() / Math.max(1, stats.writeCount.get());
                    System.out.printf("  %s: R/W ratio = %.1f%n", field, ratio);
                });
        }
    }
    
    // Example of instrumented class using the profiler
    public static class ProfiledCache {
        private static final FieldAccessProfiler profiler = new FieldAccessProfiler();
        
        private volatile boolean enabled = true;
        private final Map<String, Object> cache = new ConcurrentHashMap<>();
        private long hitCount = 0;   // Not thread-safe for demonstration
        private long missCount = 0;  // Not thread-safe for demonstration
        
        static {
            profiler.enableProfiling();
        }
        
        public void put(String key, Object value) {
            profiler.recordFieldRead("enabled");
            if (!enabled) return;
            
            cache.put(key, value);
        }
        
        public Object get(String key) {
            profiler.recordFieldRead("enabled");
            if (!enabled) return null;
            
            Object value = cache.get(key);
            
            if (value != null) {
                profiler.recordFieldWrite("hitCount");
                hitCount++;  // Race condition - will cause contention
            } else {
                profiler.recordFieldWrite("missCount");
                missCount++; // Race condition - will cause contention
            }
            
            return value;
        }
        
        public void disable() {
            profiler.recordFieldWrite("enabled");
            this.enabled = false;
        }
        
        public void enable() {
            profiler.recordFieldWrite("enabled");
            this.enabled = true;
        }
        
        public void printStats() {
            profiler.recordFieldRead("hitCount");
            profiler.recordFieldRead("missCount");
            
            System.out.printf("Cache stats: %d hits, %d misses%n", hitCount, missCount);
            
            // Print profiling results
            profiler.printProfile();
            profiler.detectHotspots();
        }
    }
    
    public static void demonstrateProfiling() throws InterruptedException {
        System.out.println("Memory Model Profiling Demo:");
        System.out.println("===========================");
        
        ProfiledCache cache = new ProfiledCache();
        
        // Create multiple threads that will cause contention
        Thread[] threads = new Thread[5];
        for (int i = 0; i < threads.length; i++) {
            final int threadIndex = i;
            threads[i] = new Thread(() -> {
                for (int j = 0; j < 1000; j++) {
                    // Mix of operations that will cause field access
                    cache.put("key" + (j % 10), "value" + j);
                    cache.get("key" + (j % 15)); // Some misses
                    
                    if (j % 100 == 0) {
                        if (threadIndex == 0) {
                            cache.disable();
                        } else if (threadIndex == 1) {
                            cache.enable();
                        }
                    }
                }
            }, "CacheWorker-" + i);
        }
        
        // Start all threads
        for (Thread thread : threads) {
            thread.start();
        }
        
        // Wait for completion
        for (Thread thread : threads) {
            thread.join();
        }
        
        // Print profiling results
        cache.printStats();
    }
    
    public static void main(String[] args) throws InterruptedException {
        demonstrateProfiling();
    }
}
```

## Memory Barrier Analysis Tools

### Runtime Memory Barrier Detection
```java
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.ConcurrentLinkedQueue;

public class MemoryBarrierAnalysis {
    
    public static class MemoryBarrierDetector {
        private final ConcurrentLinkedQueue<BarrierEvent> events = new ConcurrentLinkedQueue<>();
        private final AtomicLong eventCounter = new AtomicLong();
        
        public enum BarrierType {
            LOAD_LOAD, LOAD_STORE, STORE_LOAD, STORE_STORE,
            FULL_BARRIER, ACQUIRE, RELEASE
        }
        
        public static class BarrierEvent {
            final long eventId;
            final long threadId;
            final BarrierType type;
            final long timestamp;
            final String location;
            
            BarrierEvent(long eventId, long threadId, BarrierType type, String location) {
                this.eventId = eventId;
                this.threadId = threadId;
                this.type = type;
                this.timestamp = System.nanoTime();
                this.location = location;
            }
            
            @Override
            public String toString() {
                return String.format("[%d] T%d %s at %s (time: %d)", 
                    eventId, threadId, type, location, timestamp);
            }
        }
        
        public void recordBarrier(BarrierType type, String location) {
            long eventId = eventCounter.incrementAndGet();
            long threadId = Thread.currentThread().getId();
            
            BarrierEvent event = new BarrierEvent(eventId, threadId, type, location);
            events.offer(event);
            
            // Keep reasonable buffer size
            while (events.size() > 1000) {
                events.poll();
            }
        }
        
        public void analyzeBarrierPatterns() {
            BarrierEvent[] eventArray = events.toArray(new BarrierEvent[0]);
            
            System.out.println("Memory Barrier Analysis:");
            System.out.println("=======================");
            
            // Count barrier types
            Map<BarrierType, Integer> barrierCounts = new HashMap<>();
            for (BarrierEvent event : eventArray) {
                barrierCounts.merge(event.type, 1, Integer::sum);
            }
            
            System.out.println("Barrier Type Distribution:");
            barrierCounts.forEach((type, count) -> 
                System.out.printf("  %s: %d occurrences%n", type, count));
            
            // Analyze barrier clustering
            analyzeBarrierClustering(eventArray);
            
            // Detect potential optimizations
            detectOptimizationOpportunities(eventArray);
        }
        
        private void analyzeBarrierClustering(BarrierEvent[] events) {
            System.out.println("\nBarrier Clustering Analysis:");
            
            int clusteredBarriers = 0;
            for (int i = 1; i < events.length; i++) {
                long timeDiff = events[i].timestamp - events[i-1].timestamp;
                if (timeDiff < 1000000) { // Within 1ms
                    clusteredBarriers++;
                }
            }
            
            double clusteringRate = (double) clusteredBarriers / events.length;
            System.out.printf("  Clustered barriers: %d (%.1f%%)%n", 
                clusteredBarriers, clusteringRate * 100);
            
            if (clusteringRate > 0.3) {
                System.out.println("  ⚠️  High barrier clustering detected - potential optimization opportunity");
            }
        }
        
        private void detectOptimizationOpportunities(BarrierEvent[] events) {
            System.out.println("\nOptimization Opportunities:");
            
            // Look for redundant full barriers
            int redundantFullBarriers = 0;
            for (int i = 1; i < events.length; i++) {
                if (events[i].type == BarrierType.FULL_BARRIER && 
                    events[i-1].type == BarrierType.FULL_BARRIER &&
                    events[i].threadId == events[i-1].threadId) {
                    redundantFullBarriers++;
                }
            }
            
            if (redundantFullBarriers > 0) {
                System.out.printf("  Found %d potentially redundant full barriers%n", redundantFullBarriers);
            }
            
            // Look for acquire/release pairs that could be optimized
            int acquireReleasePairs = 0;
            for (int i = 1; i < events.length; i++) {
                if (events[i].type == BarrierType.RELEASE && 
                    events[i-1].type == BarrierType.ACQUIRE &&
                    events[i].threadId == events[i-1].threadId) {
                    acquireReleasePairs++;
                }
            }
            
            System.out.printf("  Acquire-Release pairs: %d%n", acquireReleasePairs);
        }
        
        public void printRecentEvents(int count) {
            BarrierEvent[] eventArray = events.toArray(new BarrierEvent[0]);
            int start = Math.max(0, eventArray.length - count);
            
            System.out.println("Recent Memory Barrier Events:");
            for (int i = start; i < eventArray.length; i++) {
                System.out.println("  " + eventArray[i]);
            }
        }
    }
    
    // Example class that instruments memory barriers
    public static class InstrumentedSynchronization {
        private static final MemoryBarrierDetector detector = new MemoryBarrierDetector();
        
        private volatile boolean flag = false;
        private int data = 0;
        private final Object lock = new Object();
        
        // Volatile operations with barrier detection
        public void setFlag(boolean value) {
            detector.recordBarrier(BarrierType.RELEASE, "setFlag");
            this.flag = value; // Volatile write - release barrier
        }
        
        public boolean getFlag() {
            detector.recordBarrier(BarrierType.ACQUIRE, "getFlag");
            return this.flag; // Volatile read - acquire barrier
        }
        
        // Synchronized operations with barrier detection
        public void setDataSynchronized(int value) {
            synchronized (lock) {
                detector.recordBarrier(BarrierType.FULL_BARRIER, "synchronized-enter");
                this.data = value;
                detector.recordBarrier(BarrierType.FULL_BARRIER, "synchronized-exit");
            }
        }
        
        public int getDataSynchronized() {
            synchronized (lock) {
                detector.recordBarrier(BarrierType.FULL_BARRIER, "synchronized-enter");
                int result = this.data;
                detector.recordBarrier(BarrierType.FULL_BARRIER, "synchronized-exit");
                return result;
            }
        }
        
        // Compound operation demonstrating multiple barriers
        public void updateBoth(int newData, boolean newFlag) {
            setDataSynchronized(newData);
            setFlag(newFlag);
        }
        
        public void printBarrierAnalysis() {
            detector.printRecentEvents(20);
            detector.analyzeBarrierPatterns();
        }
    }
    
    public static void demonstrateBarrierAnalysis() throws InterruptedException {
        System.out.println("Memory Barrier Analysis Demo:");
        System.out.println("============================");
        
        InstrumentedSynchronization sync = new InstrumentedSynchronization();
        
        // Create threads that will generate various barrier patterns
        Thread[] threads = new Thread[3];
        
        // Thread 1: Frequent volatile operations
        threads[0] = new Thread(() -> {
            for (int i = 0; i < 50; i++) {
                sync.setFlag(i % 2 == 0);
                sync.getFlag();
                try {
                    Thread.sleep(10);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    break;
                }
            }
        }, "VolatileWorker");
        
        // Thread 2: Synchronized operations
        threads[1] = new Thread(() -> {
            for (int i = 0; i < 30; i++) {
                sync.setDataSynchronized(i * 10);
                sync.getDataSynchronized();
                try {
                    Thread.sleep(15);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    break;
                }
            }
        }, "SynchronizedWorker");
        
        // Thread 3: Mixed operations
        threads[2] = new Thread(() -> {
            for (int i = 0; i < 20; i++) {
                sync.updateBoth(i, i % 3 == 0);
                try {
                    Thread.sleep(25);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    break;
                }
            }
        }, "MixedWorker");
        
        // Start all threads
        for (Thread thread : threads) {
            thread.start();
        }
        
        // Wait for completion
        for (Thread thread : threads) {
            thread.join();
        }
        
        // Analyze the barrier patterns
        sync.printBarrierAnalysis();
    }
    
    public static void main(String[] args) throws InterruptedException {
        demonstrateBarrierAnalysis();
    }
}
```

## Root Cause Analysis Framework

### Systematic Problem Investigation
```java
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.CountDownLatch;
import java.util.List;
import java.util.ArrayList;
import java.util.Date;

public class MemoryModelRootCauseAnalysis {
    
    public static class ProblemReportCollector {
        private final List<ProblemReport> reports = new ArrayList<>();
        
        public static class ProblemReport {
            final String description;
            final Severity severity;
            final long timestamp;
            final String threadName;
            final StackTraceElement[] stackTrace;
            final Map<String, Object> context;
            
            public enum Severity {
                LOW, MEDIUM, HIGH, CRITICAL
            }
            
            public ProblemReport(String description, Severity severity, Map<String, Object> context) {
                this.description = description;
                this.severity = severity;
                this.timestamp = System.currentTimeMillis();
                this.threadName = Thread.currentThread().getName();
                this.stackTrace = Thread.currentThread().getStackTrace();
                this.context = new HashMap<>(context);
            }
            
            @Override
            public String toString() {
                return String.format("[%s] %s - %s (Thread: %s)", 
                    severity, new Date(timestamp), description, threadName);
            }
        }
        
        public synchronized void reportProblem(String description, 
                ProblemReport.Severity severity, Map<String, Object> context) {
            ProblemReport report = new ProblemReport(description, severity, context);
            reports.add(report);
            
            if (severity == ProblemReport.Severity.CRITICAL) {
                System.err.println("CRITICAL ISSUE: " + report);
            }
        }
        
        public synchronized void analyzeReports() {
            System.out.println("Problem Report Analysis:");
            System.out.println("=======================");
            
            // Group by severity
            Map<ProblemReport.Severity, Long> severityCounts = reports.stream()
                .collect(java.util.stream.Collectors.groupingBy(
                    r -> r.severity, java.util.stream.Collectors.counting()));
            
            severityCounts.forEach((severity, count) ->
                System.out.printf("  %s: %d reports%n", severity, count));
            
            // Find patterns in descriptions
            Map<String, Long> descriptionPatterns = reports.stream()
                .map(r -> r.description.split(" ")[0]) // First word
                .collect(java.util.stream.Collectors.groupingBy(
                    w -> w, java.util.stream.Collectors.counting()));
            
            System.out.println("\nCommon Issue Patterns:");
            descriptionPatterns.entrySet().stream()
                .sorted(Map.Entry.<String, Long>comparingByValue().reversed())
                .limit(5)
                .forEach(entry -> System.out.printf("  %s: %d occurrences%n", 
                    entry.getKey(), entry.getValue()));
            
            // Analyze thread patterns
            Map<String, Long> threadPatterns = reports.stream()
                .collect(java.util.stream.Collectors.groupingBy(
                    r -> r.threadName, java.util.stream.Collectors.counting()));
            
            System.out.println("\nThread Issue Distribution:");
            threadPatterns.forEach((thread, count) ->
                System.out.printf("  %s: %d issues%n", thread, count));
        }
        
        public List<ProblemReport> getReportsBySeverity(ProblemReport.Severity severity) {
            return reports.stream()
                .filter(r -> r.severity == severity)
                .collect(java.util.stream.Collectors.toList());
        }
    }
    
    // Test case that generates various memory model issues
    public static class ProblematicSystem {
        private final ProblemReportCollector collector = new ProblemReportCollector();
        
        // Various problematic patterns
        private boolean shutdownFlag = false;  // Should be volatile
        private volatile boolean configChanged = false;
        private Config config = new Config("default");  // Unsafe publication
        private int counter = 0;  // Race condition
        
        private static class Config {
            final String value;
            Config(String value) { this.value = value; }
        }
        
        public void updateConfig(String newValue) {
            Map<String, Object> context = new HashMap<>();
            context.put("oldValue", config.value);
            context.put("newValue", newValue);
            
            try {
                config = new Config(newValue);  // Unsafe publication
                configChanged = true;
                
            } catch (Exception e) {
                collector.reportProblem("Configuration update failed", 
                    ProblemReport.Severity.HIGH, context);
            }
        }
        
        public String getConfigValue() {
            Config current = config;  // May see partially constructed object
            
            if (current == null) {
                Map<String, Object> context = new HashMap<>();
                context.put("configChanged", configChanged);
                collector.reportProblem("Null configuration observed", 
                    ProblemReport.Severity.CRITICAL, context);
                return "ERROR";
            }
            
            try {
                return current.value;
            } catch (Exception e) {
                Map<String, Object> context = new HashMap<>();
                context.put("exception", e.getMessage());
                collector.reportProblem("Configuration access failed", 
                    ProblemReport.Severity.HIGH, context);
                return "ERROR";
            }
        }
        
        public void incrementCounter() {
            int oldValue = counter;
            counter = oldValue + 1;  // Race condition
            
            // Detect lost updates
            if (counter != oldValue + 1) {
                Map<String, Object> context = new HashMap<>();
                context.put("expected", oldValue + 1);
                context.put("actual", counter);
                collector.reportProblem("Counter increment lost", 
                    ProblemReport.Severity.MEDIUM, context);
            }
        }
        
        public void shutdown() {
            shutdownFlag = true;  // May not be visible to other threads
        }
        
        public void workerLoop() {
            int iterations = 0;
            long startTime = System.currentTimeMillis();
            
            while (!shutdownFlag) {  // May not see shutdown
                incrementCounter();
                getConfigValue();
                
                iterations++;
                
                // Detect if shutdown is taking too long
                if (iterations % 1000 == 0) {
                    long elapsed = System.currentTimeMillis() - startTime;
                    if (elapsed > 5000) { // More than 5 seconds
                        Map<String, Object> context = new HashMap<>();
                        context.put("iterations", iterations);
                        context.put("elapsed", elapsed);
                        collector.reportProblem("Shutdown flag not visible", 
                            ProblemReport.Severity.HIGH, context);
                        break; // Force exit
                    }
                }
                
                try {
                    Thread.sleep(1);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    break;
                }
            }
        }
        
        public void runSystemTest() throws InterruptedException {
            System.out.println("Running Problematic System Test:");
            System.out.println("================================");
            
            CountDownLatch latch = new CountDownLatch(3);
            
            // Worker thread 1
            Thread worker1 = new Thread(() -> {
                try {
                    workerLoop();
                } finally {
                    latch.countDown();
                }
            }, "Worker-1");
            
            // Worker thread 2
            Thread worker2 = new Thread(() -> {
                try {
                    workerLoop();
                } finally {
                    latch.countDown();
                }
            }, "Worker-2");
            
            // Configuration updater thread
            Thread configUpdater = new Thread(() -> {
                try {
                    for (int i = 0; i < 10; i++) {
                        updateConfig("config-" + i);
                        Thread.sleep(100);
                    }
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                } finally {
                    latch.countDown();
                }
            }, "ConfigUpdater");
            
            worker1.start();
            worker2.start();
            configUpdater.start();
            
            // Let it run for a bit
            Thread.sleep(2000);
            
            // Initiate shutdown
            shutdown();
            
            // Wait for completion (with timeout)
            if (!latch.await(10, java.util.concurrent.TimeUnit.SECONDS)) {
                System.err.println("System did not shutdown properly!");
                
                // Force interrupt
                worker1.interrupt();
                worker2.interrupt();
                configUpdater.interrupt();
            }
            
            // Analyze collected problems
            collector.analyzeReports();
            
            // Print critical issues
            List<ProblemReport> criticalIssues = 
                collector.getReportsBySeverity(ProblemReport.Severity.CRITICAL);
            
            if (!criticalIssues.isEmpty()) {
                System.out.println("\nCritical Issues Detected:");
                criticalIssues.forEach(report -> System.out.println("  " + report));
            }
        }
    }
    
    public static void main(String[] args) throws InterruptedException {
        ProblematicSystem system = new ProblematicSystem();
        system.runSystemTest();
    }
}
```
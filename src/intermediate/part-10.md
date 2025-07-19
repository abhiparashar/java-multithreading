# Java Memory Model Guide - Part 10: Production Debugging of Memory Visibility Issues

## Introduction to Production Memory Model Debugging

Debugging memory visibility issues in production is challenging because these problems are often intermittent, environment-dependent, and may not reproduce in development. This guide provides systematic approaches for identifying, diagnosing, and resolving memory model issues in live systems.

## Recognizing Memory Visibility Symptoms

### Common Production Symptoms
```java
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.ConcurrentHashMap;
import java.time.Instant;
import java.util.Map;
import java.util.HashMap;

public class MemoryVisibilitySymptoms {
    
    // Symptom 1: Stale configuration reads
    public static class ConfigurationManager {
        private Config currentConfig = new Config("default", 5000, false);
        private volatile long configVersion = 0;
        
        // Symptom: New configuration not visible to all threads
        public void updateConfiguration(Config newConfig) {
            System.out.println("Updating config at " + Instant.now());
            this.currentConfig = newConfig;  // NOT VOLATILE - Problem!
            this.configVersion++;            // This IS volatile
        }
        
        public Config getCurrentConfig() {
            return currentConfig;  // May return stale config
        }
        
        public long getConfigVersion() {
            return configVersion;
        }
    }
    
    // Symptom 2: Inconsistent cache state
    public static class MemoryVisibilityCache {
        private final ConcurrentHashMap<String, CacheEntry> cache = new ConcurrentHashMap<>();
        private boolean cacheEnabled = true;  // NOT VOLATILE - Problem!
        private long totalHits = 0;           // NOT VOLATILE - Problem!
        private long totalMisses = 0;         // NOT VOLATILE - Problem!
        
        public void put(String key, Object value) {
            if (cacheEnabled) {  // May read stale value
                cache.put(key, new CacheEntry(value, System.currentTimeMillis()));
            }
        }
        
        public Object get(String key) {
            if (!cacheEnabled) {  // May read stale value
                return null;
            }
            
            CacheEntry entry = cache.get(key);
            if (entry != null) {
                totalHits++;  // Race condition - lost updates
                return entry.value;
            } else {
                totalMisses++;  // Race condition - lost updates
                return null;
            }
        }
        
        public void disableCache() {
            System.out.println("Disabling cache at " + Instant.now());
            cacheEnabled = false;  // May not be visible to other threads
        }
        
        public void printStats() {
            // May print inconsistent/stale statistics
            System.out.printf("Cache stats - Hits: %d, Misses: %d, Enabled: %b%n",
                totalHits, totalMisses, cacheEnabled);
        }
        
        private static class CacheEntry {
            final Object value;
            final long timestamp;
            
            CacheEntry(Object value, long timestamp) {
                this.value = value;
                this.timestamp = timestamp;
            }
        }
    }
    
    // Symptom 3: Shutdown flag not respected
    public static class WorkerThread extends Thread {
        private boolean shouldStop = false;  // NOT VOLATILE - Problem!
        private final String workerName;
        
        public WorkerThread(String name) {
            this.workerName = name;
            setDaemon(false);
        }
        
        @Override
        public void run() {
            int workCount = 0;
            
            while (!shouldStop) {  // May never see shouldStop = true
                try {
                    // Simulate work
                    Thread.sleep(100);
                    workCount++;
                    
                    if (workCount % 50 == 0) {
                        System.out.println(workerName + " processed " + workCount + " items");
                    }
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    break;
                }
            }
            
            System.out.println(workerName + " stopped after processing " + workCount + " items");
        }
        
        public void requestStop() {
            System.out.println("Requesting stop for " + workerName + " at " + Instant.now());
            shouldStop = true;  // May not be visible to worker thread
        }
    }
    
    private static class Config {
        final String serverUrl;
        final int timeout;
        final boolean debugMode;
        
        Config(String serverUrl, int timeout, boolean debugMode) {
            this.serverUrl = serverUrl;
            this.timeout = timeout;
            this.debugMode = debugMode;
        }
    }
    
    public static void demonstrateSymptoms() throws InterruptedException {
        System.out.println("Demonstrating Memory Visibility Symptoms:");
        System.out.println("=========================================");
        
        // Symptom 1: Configuration not updating
        demonstrateConfigurationProblem();
        
        // Symptom 2: Cache inconsistency  
        demonstrateCacheProblem();
        
        // Symptom 3: Shutdown not working
        demonstrateShutdownProblem();
    }
    
    private static void demonstrateConfigurationProblem() throws InterruptedException {
        System.out.println("\n1. Configuration Update Problem:");
        
        ConfigurationManager manager = new ConfigurationManager();
        
        // Reader thread that polls configuration
        Thread reader = new Thread(() -> {
            Config lastConfig = null;
            for (int i = 0; i < 100; i++) {
                Config current = manager.getCurrentConfig();
                long version = manager.getConfigVersion();
                
                if (lastConfig != current) {
                    System.out.printf("  Reader saw config change: %s (version %d)%n", 
                        current.serverUrl, version);
                    lastConfig = current;
                }
                
                try {
                    Thread.sleep(50);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    break;
                }
            }
        });
        
        reader.start();
        Thread.sleep(100);
        
        // Update configuration
        manager.updateConfiguration(new Config("newserver.com", 3000, true));
        
        reader.join();
    }
    
    private static void demonstrateCacheProblem() throws InterruptedException {
        System.out.println("\n2. Cache Inconsistency Problem:");
        
        MemoryVisibilityCache cache = new MemoryVisibilityCache();
        
        // Start multiple threads using the cache
        Thread[] workers = new Thread[3];
        for (int i = 0; i < workers.length; i++) {
            final int workerId = i;
            workers[i] = new Thread(() -> {
                for (int j = 0; j < 20; j++) {
                    cache.put("key" + j, "value" + j + "_worker" + workerId);
                    cache.get("key" + (j % 10));
                    
                    try {
                        Thread.sleep(10);
                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                        break;
                    }
                }
            });
            workers[i].start();
        }
        
        Thread.sleep(100);
        cache.disableCache();  // This may not be visible to all workers
        
        for (Thread worker : workers) {
            worker.join();
        }
        
        cache.printStats();  // May show inconsistent statistics
    }
    
    private static void demonstrateShutdownProblem() throws InterruptedException {
        System.out.println("\n3. Shutdown Flag Problem:");
        
        WorkerThread worker1 = new WorkerThread("Worker-1");
        WorkerThread worker2 = new WorkerThread("Worker-2");
        
        worker1.start();
        worker2.start();
        
        Thread.sleep(500);
        
        // Request shutdown
        worker1.requestStop();
        worker2.requestStop();
        
        // Wait for shutdown (may timeout if flag not visible)
        worker1.join(2000);
        worker2.join(2000);
        
        if (worker1.isAlive()) {
            System.out.println("❌ Worker-1 did not stop - memory visibility issue!");
            worker1.interrupt();
        }
        
        if (worker2.isAlive()) {
            System.out.println("❌ Worker-2 did not stop - memory visibility issue!");
            worker2.interrupt();
        }
    }
    
    public static void main(String[] args) throws InterruptedException {
        demonstrateSymptoms();
    }
}
```

## Diagnostic Techniques

### Runtime Monitoring and Detection
```java
import java.lang.management.ManagementFactory;
import java.lang.management.MemoryMXBean;
import java.lang.management.ThreadMXBean;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.ConcurrentHashMap;

public class RuntimeDiagnostics {
    
    // Memory visibility detector using heartbeat pattern
    public static class MemoryVisibilityDetector {
        private volatile long writerHeartbeat = 0;
        private final AtomicLong readerHeartbeat = new AtomicLong(0);
        private final Map<Long, Long> threadLastSeen = new ConcurrentHashMap<>();
        private volatile boolean monitoring = true;
        
        public void startMonitoring() {
            // Writer thread - updates heartbeat
            Thread writer = new Thread(() -> {
                while (monitoring) {
                    writerHeartbeat = System.currentTimeMillis();
                    try {
                        Thread.sleep(100);
                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                        break;
                    }
                }
            }, "MemoryVisibility-Writer");
            writer.setDaemon(true);
            writer.start();
            
            // Monitor thread - checks for visibility delays
            Thread monitor = new Thread(() -> {
                while (monitoring) {
                    long currentTime = System.currentTimeMillis();
                    long lastWrite = writerHeartbeat;
                    long delay = currentTime - lastWrite;
                    
                    if (delay > 1000) { // More than 1 second delay
                        System.err.printf("WARNING: Memory visibility delay detected: %d ms%n", delay);
                    }
                    
                    try {
                        Thread.sleep(500);
                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                        break;
                    }
                }
            }, "MemoryVisibility-Monitor");
            monitor.setDaemon(true);
            monitor.start();
        }
        
        public void recordReaderHeartbeat(long threadId) {
            long currentTime = System.currentTimeMillis();
            threadLastSeen.put(threadId, currentTime);
            readerHeartbeat.set(currentTime);
        }
        
        public void stopMonitoring() {
            monitoring = false;
        }
        
        public void printReport() {
            System.out.println("Memory Visibility Report:");
            System.out.println("Writer heartbeat: " + writerHeartbeat);
            System.out.println("Reader heartbeat: " + readerHeartbeat.get());
            System.out.println("Active threads: " + threadLastSeen.size());
            
            long now = System.currentTimeMillis();
            threadLastSeen.forEach((threadId, lastSeen) -> {
                long staleness = now - lastSeen;
                if (staleness > 5000) {
                    System.err.printf("  Thread %d: Last seen %d ms ago (STALE)%n", threadId, staleness);
                } else {
                    System.out.printf("  Thread %d: Last seen %d ms ago%n", threadId, staleness);
                }
            });
        }
    }
    
    // JVM-level diagnostics
    public static class JVMDiagnostics {
        private final MemoryMXBean memoryBean;
        private final ThreadMXBean threadBean;
        
        public JVMDiagnostics() {
            this.memoryBean = ManagementFactory.getMemoryMXBean();
            this.threadBean = ManagementFactory.getThreadMXBean();
        }
        
        public void printSystemState() {
            System.out.println("JVM Diagnostic Information:");
            System.out.println("==========================");
            
            // Memory information
            long heapUsed = memoryBean.getHeapMemoryUsage().getUsed();
            long heapMax = memoryBean.getHeapMemoryUsage().getMax();
            long nonHeapUsed = memoryBean.getNonHeapMemoryUsage().getUsed();
            
            System.out.printf("Heap: %d MB used / %d MB max%n", 
                heapUsed / 1024 / 1024, heapMax / 1024 / 1024);
            System.out.printf("Non-Heap: %d MB used%n", nonHeapUsed / 1024 / 1024);
            
            // Thread information
            int threadCount = threadBean.getThreadCount();
            int daemonCount = threadBean.getDaemonThreadCount();
            long totalStarted = threadBean.getTotalStartedThreadCount();
            
            System.out.printf("Threads: %d active (%d daemon), %d total started%n",
                threadCount, daemonCount, totalStarted);
            
            // GC information
            System.out.println("GC Count: " + memoryBean.getObjectPendingFinalizationCount());
            
            // Compilation information
            var compilationBean = ManagementFactory.getCompilationMXBean();
            if (compilationBean != null) {
                System.out.println("JIT Compilation Time: " + 
                    compilationBean.getTotalCompilationTime() + " ms");
            }
        }
        
        public void checkForDeadlocks() {
            long[] deadlockedThreads = threadBean.findDeadlockedThreads();
            if (deadlockedThreads != null) {
                System.err.println("DEADLOCK DETECTED!");
                for (long threadId : deadlockedThreads) {
                    var threadInfo = threadBean.getThreadInfo(threadId);
                    System.err.println("Deadlocked thread: " + threadInfo.getThreadName());
                }
            }
        }
        
        public void detectHighCPUThreads() {
            if (!threadBean.isThreadCpuTimeSupported()) {
                System.out.println("Thread CPU time monitoring not supported");
                return;
            }
            
            long[] allThreadIds = threadBean.getAllThreadIds();
            System.out.println("High CPU Usage Threads:");
            
            for (long threadId : allThreadIds) {
                long cpuTime = threadBean.getThreadCpuTime(threadId);
                if (cpuTime > 1_000_000_000L) { // More than 1 second CPU time
                    var threadInfo = threadBean.getThreadInfo(threadId);
                    if (threadInfo != null) {
                        System.out.printf("  %s: %d ms CPU time%n", 
                            threadInfo.getThreadName(), cpuTime / 1_000_000);
                    }
                }
            }
        }
    }
    
    public static void main(String[] args) throws InterruptedException {
        System.out.println("Runtime Diagnostics Demo:");
        System.out.println("========================");
        
        MemoryVisibilityDetector detector = new MemoryVisibilityDetector();
        JVMDiagnostics jvmDiag = new JVMDiagnostics();
        
        detector.startMonitoring();
        
        // Simulate some work with potential memory visibility issues
        for (int i = 0; i < 10; i++) {
            detector.recordReaderHeartbeat(Thread.currentThread().getId());
            Thread.sleep(200);
        }
        
        detector.printReport();
        jvmDiag.printSystemState();
        jvmDiag.checkForDeadlocks();
        jvmDiag.detectHighCPUThreads();
        
        detector.stopMonitoring();
    }
}
```

## Application-Level Logging and Tracing

### Enhanced Logging for Memory Model Debugging
```java
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.time.Instant;
import java.util.Date;

public class MemoryModelLogging {
    
    // Enhanced logging for memory model debugging
    public static class MemoryModelLogger {
        private final ConcurrentLinkedQueue<LogEntry> logBuffer = new ConcurrentLinkedQueue<>();
        private final AtomicLong sequenceNumber = new AtomicLong(0);
        private volatile boolean loggingEnabled = true;
        
        public static class LogEntry {
            final long sequence;
            final long threadId;
            final String threadName;
            final Instant timestamp;
            final String operation;
            final String details;
            final StackTraceElement[] stackTrace;
            
            LogEntry(long sequence, long threadId, String threadName, 
                    String operation, String details, StackTraceElement[] stackTrace) {
                this.sequence = sequence;
                this.threadId = threadId;
                this.threadName = threadName;
                this.timestamp = Instant.now();
                this.operation = operation;
                this.details = details;
                this.stackTrace = stackTrace;
            }
            
            @Override
            public String toString() {
                return String.format("[%d] %s T%d(%s): %s - %s",
                    sequence, timestamp.toString(), threadId, threadName, operation, details);
            }
        }
        
        public void logMemoryOperation(String operation, String details) {
            if (!loggingEnabled) return;
            
            Thread current = Thread.currentThread();
            StackTraceElement[] stack = current.getStackTrace();
            
            LogEntry entry = new LogEntry(
                sequenceNumber.incrementAndGet(),
                current.getId(),
                current.getName(),
                operation,
                details,
                stack
            );
            
            logBuffer.offer(entry);
            
            // Keep buffer size manageable
            while (logBuffer.size() > 10000) {
                logBuffer.poll();
            }
        }
        
        public void printRecentEntries(int count) {
            System.out.println("Recent Memory Model Operations:");
            System.out.println("==============================");
            
            LogEntry[] entries = logBuffer.toArray(new LogEntry[0]);
            int start = Math.max(0, entries.length - count);
            
            for (int i = start; i < entries.length; i++) {
                System.out.println(entries[i]);
            }
        }
        
        public void analyzePatterns() {
            LogEntry[] entries = logBuffer.toArray(new LogEntry[0]);
            
            System.out.println("\nPattern Analysis:");
            System.out.println("================");
            
            // Analyze thread interleaving
            analyzeThreadInterleaving(entries);
            
            // Analyze timing patterns
            analyzeTimingPatterns(entries);
            
            // Analyze operation sequences
            analyzeOperationSequences(entries);
        }
        
        private void analyzeThreadInterleaving(LogEntry[] entries) {
            System.out.println("Thread Interleaving Pattern:");
            
            long lastThreadId = -1;
            int switchCount = 0;
            
            for (LogEntry entry : entries) {
                if (lastThreadId != -1 && lastThreadId != entry.threadId) {
                    switchCount++;
                }
                lastThreadId = entry.threadId;
            }
            
            System.out.printf("  Thread switches: %d in %d operations%n", switchCount, entries.length);
            System.out.printf("  Switch rate: %.2f%%n", (switchCount * 100.0) / entries.length);
        }
        
        private void analyzeTimingPatterns(LogEntry[] entries) {
            System.out.println("Timing Pattern Analysis:");
            
            if (entries.length < 2) return;
            
            long minGap = Long.MAX_VALUE;
            long maxGap = 0;
            long totalGap = 0;
            int gapCount = 0;
            
            for (int i = 1; i < entries.length; i++) {
                long gap = entries[i].timestamp.toEpochMilli() - entries[i-1].timestamp.toEpochMilli();
                minGap = Math.min(minGap, gap);
                maxGap = Math.max(maxGap, gap);
                totalGap += gap;
                gapCount++;
            }
            
            System.out.printf("  Min gap: %d ms, Max gap: %d ms, Avg gap: %.2f ms%n",
                minGap, maxGap, (double) totalGap / gapCount);
            
            if (maxGap > 1000) {
                System.out.println("  ⚠️  Large timing gaps detected - possible blocking or GC");
            }
        }
        
        private void analyzeOperationSequences(LogEntry[] entries) {
            System.out.println("Operation Sequence Analysis:");
            
            Map<String, Integer> operationCounts = new HashMap<>();
            for (LogEntry entry : entries) {
                operationCounts.merge(entry.operation, 1, Integer::sum);
            }
            
            operationCounts.forEach((op, count) -> 
                System.out.printf("  %s: %d occurrences%n", op, count));
        }
    }
    
    // Example class with instrumented memory operations
    public static class InstrumentedSharedState {
        private static final MemoryModelLogger logger = new MemoryModelLogger();
        
        private volatile boolean flag = false;
        private int data = 0;
        private volatile long timestamp = 0;
        
        public void updateData(int newValue) {
            logger.logMemoryOperation("WRITE_DATA", "value=" + newValue);
            this.data = newValue;
            
            logger.logMemoryOperation("WRITE_TIMESTAMP", "ts=" + System.currentTimeMillis());
            this.timestamp = System.currentTimeMillis();
            
            logger.logMemoryOperation("WRITE_FLAG", "flag=true");
            this.flag = true;
        }
        
        public int readData() {
            logger.logMemoryOperation("READ_FLAG", "flag=" + flag);
            if (!flag) {
                return -1; // Not ready
            }
            
            logger.logMemoryOperation("READ_DATA", "data=" + data);
            int result = this.data;
            
            logger.logMemoryOperation("READ_TIMESTAMP", "ts=" + timestamp);
            long ts = this.timestamp;
            
            // Validate consistency
            long now = System.currentTimeMillis();
            if (now - ts > 10000) { // More than 10 seconds old
                logger.logMemoryOperation("STALE_READ", "age=" + (now - ts) + "ms");
            }
            
            return result;
        }
        
        public void reset() {
            logger.logMemoryOperation("RESET_START", "");
            this.flag = false;
            this.data = 0;
            this.timestamp = 0;
            logger.logMemoryOperation("RESET_END", "");
        }
        
        public void printDiagnostics() {
            logger.printRecentEntries(50);
            logger.analyzePatterns();
        }
    }
    
    public static void demonstrateLogging() throws InterruptedException {
        System.out.println("Memory Model Logging Demo:");
        System.out.println("=========================");
        
        InstrumentedSharedState state = new InstrumentedSharedState();
        
        // Writer thread
        Thread writer = new Thread(() -> {
            for (int i = 0; i < 10; i++) {
                state.updateData(i * 100);
                try {
                    Thread.sleep(100);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    break;
                }
            }
        }, "Writer");
        
        // Reader thread
        Thread reader = new Thread(() -> {
            for (int i = 0; i < 20; i++) {
                int data = state.readData();
                if (data != -1) {
                    System.out.println("Read data: " + data);
                }
                try {
                    Thread.sleep(50);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    break;
                }
            }
        }, "Reader");
        
        writer.start();
        reader.start();
        
        writer.join();
        reader.join();
        
        System.out.println("\nDiagnostic Analysis:");
        state.printDiagnostics();
    }
    
    public static void main(String[] args) throws InterruptedException {
        demonstrateLogging();
    }
}
```
package basic;

import java.util.concurrent.atomic.AtomicInteger;

class UnsafeCounter{
    private int count = 0;

    public void incrementCount(){
        count++;
    }

    public int getCount(){
        return count;
    }
}

class SynchronizedCounter{
    private int count = 0;

    public synchronized void incrementCount(){
        count++;
    }

    public synchronized int getCount() {
        return count;
    }
}

class AtomicCounter {
    private final AtomicInteger count = new AtomicInteger(0);

    public void incrementCount(){
        count.getAndIncrement();
    }

    public int getCount() {
        return count.get();
    }

    // Advanced atomic operation
    public boolean incrementIfLessThan(int threashold){
        while (true){
            int current = count.get();
            if(current>=threashold){
                return false;
            }
            if(count.compareAndSet(current, current+1)){
                return true;
            }
            // If CAS failed, retry (another thread modified the value)
        }
    }
}

public class CounterComparison {
   static class CounterTest{
        private static final int NUM_THREADS = 10;
        private static final int INCREMENTS_PER_THREAD = 100000;

        private static void testCounter(String name,Runnable incrementOperation,java.util.function.Supplier<Integer> getCount) throws InterruptedException {
            Thread[]threads = new Thread[NUM_THREADS];
            long startTime = System.currentTimeMillis();

            // Create and start threads
            for (int i = 0; i < threads.length; i++) {
                threads[i] = new Thread(()->{
                    for (int j = 0; j < INCREMENTS_PER_THREAD; j++) {
                        incrementOperation.run();
                    }
                });
                threads[i].start();
            }

            // Wait for all threads to complete
            for (Thread thread:threads){
                thread.join();
            }
            long endTime = System.currentTimeMillis();
            double duration = (endTime - startTime);

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
        CounterTest.testCounter("Unsafe Counter", unsafeCounter::incrementCount, unsafeCounter::getCount);

        // Test synchronized counter
        SynchronizedCounter synchronizedCounter = new SynchronizedCounter();
        CounterTest.testCounter("Synchronized Counter",synchronizedCounter::incrementCount,synchronizedCounter::getCount);

        // Test atomic counter
        AtomicCounter atomicCounter = new AtomicCounter();
        CounterTest.testCounter("Atomic Counter",atomicCounter::incrementCount,atomicCounter::getCount );

        // Demonstrate advanced atomic operation
        System.out.println("\n=== Advanced Atomic Operations ===");
        AtomicCounter advancedCounter = new AtomicCounter();

        // Multiple threads trying to increment only if less than threshold
        Thread[]threads = new Thread[5];
        for (int i = 0; i < threads.length; i++) {
            final int threadId = i;
            threads[i] = new Thread(()->{
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
                    }                }
            });
            threads[i].start();
        }
        for (Thread thread : threads) {
            thread.join();
        }
        System.out.println("Final count with threshold: " + advancedCounter.getCount());
    }
}

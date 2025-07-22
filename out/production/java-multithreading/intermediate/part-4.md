# Java Concurrency Part 4: Advanced Level - Lock-Free Programming

## Table of Contents
1. [Atomic Classes](#atomic-classes)
2. [Compare-and-Swap (CAS) Operations](#compare-and-swap-cas-operations)
3. [ABA Problem and Solutions](#aba-problem-and-solutions)
4. [AtomicStampedReference](#atomicstampedreference)

---

## Atomic Classes

### Theory

**Atomic classes** provide lock-free, thread-safe operations on single variables using low-level atomic hardware primitives. They are faster than synchronized blocks for simple operations and are immune to priority inversion and deadlock.

**Key Atomic Classes:**
- **AtomicInteger, AtomicLong, AtomicBoolean**: Basic atomic primitives
- **AtomicReference**: Atomic object references
- **AtomicIntegerArray, AtomicLongArray, AtomicReferenceArray**: Atomic arrays
- **AtomicMarkableReference**: Reference with boolean mark
- **AtomicStampedReference**: Reference with version stamp
- **LongAdder, DoubleAdder**: High-performance counters for concurrent updates

### Code Examples

#### Basic Atomic Operations
```java
import java.util.concurrent.atomic.*;
import java.util.concurrent.*;

public class AtomicBasicsExample {
    private final AtomicInteger counter = new AtomicInteger(0);
    private final AtomicLong totalSum = new AtomicLong(0);
    private final AtomicBoolean isReady = new AtomicBoolean(false);
    private final AtomicReference<String> message = new AtomicReference<>("Initial");
    
    public void demonstrateBasicOperations() {
        System.out.println("=== Basic Atomic Operations ===");
        
        // AtomicInteger operations
        System.out.println("Initial counter: " + counter.get());
        
        int newValue = counter.incrementAndGet();
        System.out.println("After increment: " + newValue);
        
        int oldValue = counter.getAndAdd(5);
        System.out.println("Added 5, old value: " + oldValue + ", new value: " + counter.get());
        
        boolean success = counter.compareAndSet(6, 10);
        System.out.println("CAS 6->10 success: " + success + ", value: " + counter.get());
        
        // AtomicLong operations
        totalSum.addAndGet(100);
        System.out.println("Total sum: " + totalSum.get());
        
        // AtomicBoolean operations
        boolean wasReady = isReady.getAndSet(true);
        System.out.println("Was ready: " + wasReady + ", now ready: " + isReady.get());
        
        // AtomicReference operations
        String oldMessage = message.getAndSet("Updated message");
        System.out.println("Old message: " + oldMessage + ", new: " + message.get());
    }
    
    public void concurrentCounterTest() throws InterruptedException {
        System.out.println("\n=== Concurrent Counter Test ===");
        
        AtomicInteger atomicCounter = new AtomicInteger(0);
        int numThreads = 10;
        int incrementsPerThread = 1000;
        
        ExecutorService executor = Executors.newFixedThreadPool(numThreads);
        CountDownLatch latch = new CountDownLatch(numThreads);
        
        long startTime = System.nanoTime();
        
        for (int i = 0; i < numThreads; i++) {
            executor.submit(() -> {
                try {
                    for (int j = 0; j < incrementsPerThread; j++) {
                        atomicCounter.incrementAndGet();
                    }
                } finally {
                    latch.countDown();
                }
            });
        }
        
        latch.await();
        long endTime = System.nanoTime();
        
        int expectedValue = numThreads * incrementsPerThread;
        int actualValue = atomicCounter.get();
        
        System.out.printf("Expected: %d, Actual: %d, Correct: %s%n", 
                        expectedValue, actualValue, expectedValue == actualValue);
        System.out.printf("Time taken: %.2f ms%n", (endTime - startTime) / 1_000_000.0);
        
        executor.shutdown();
    }
    
    public void atomicReferenceExample() throws InterruptedException {
        System.out.println("\n=== AtomicReference Example ===");
        
        AtomicReference<Node> head = new AtomicReference<>(null);
        ExecutorService executor = Executors.newFixedThreadPool(5);
        CountDownLatch latch = new CountDownLatch(5);
        
        // Multiple threads adding to a lock-free stack
        for (int i = 0; i < 5; i++) {
            final int value = i;
            executor.submit(() -> {
                try {
                    pushToStack(head, value);
                    System.out.println("Thread pushed: " + value);
                } finally {
                    latch.countDown();
                }
            });
        }
        
        latch.await();
        
        // Print stack contents
        System.out.print("Stack contents: ");
        Node current = head.get();
        while (current != null) {
            System.out.print(current.value + " ");
            current = current.next;
        }
        System.out.println();
        
        executor.shutdown();
    }
    
    private void pushToStack(AtomicReference<Node> head, int value) {
        Node newNode = new Node(value);
        Node currentHead;
        
        do {
            currentHead = head.get();
            newNode.next = currentHead;
        } while (!head.compareAndSet(currentHead, newNode));
    }
    
    static class Node {
        final int value;
        Node next;
        
        Node(int value) {
            this.value = value;
        }
    }
}
```

#### Advanced Atomic Operations
```java
import java.util.concurrent.atomic.AtomicReferenceFieldUpdater;
import java.util.concurrent.atomic.AtomicIntegerFieldUpdater;

public class AdvancedAtomicOperations {
    
    // Using AtomicFieldUpdater for memory efficiency
    private volatile int status;
    private volatile Node nextNode;
    
    private static final AtomicIntegerFieldUpdater<AdvancedAtomicOperations> STATUS_UPDATER =
        AtomicIntegerFieldUpdater.newUpdater(AdvancedAtomicOperations.class, "status");
    
    private static final AtomicReferenceFieldUpdater<AdvancedAtomicOperations, Node> NODE_UPDATER =
        AtomicReferenceFieldUpdater.newUpdater(AdvancedAtomicOperations.class, Node.class, "nextNode");
    
    public void demonstrateFieldUpdaters() {
        System.out.println("=== Field Updater Example ===");
        
        // Using field updater instead of AtomicInteger (saves memory)
        System.out.println("Initial status: " + status);
        
        boolean success = STATUS_UPDATER.compareAndSet(this, 0, 1);
        System.out.println("CAS status 0->1: " + success + ", new status: " + status);
        
        int oldStatus = STATUS_UPDATER.getAndAdd(this, 5);
        System.out.println("Added 5, old: " + oldStatus + ", new: " + status);
        
        // Node field updater
        Node oldNode = NODE_UPDATER.getAndSet(this, new Node(42));
        System.out.println("Set new node, old was null: " + (oldNode == null));
        System.out.println("Current node value: " + nextNode.value);
    }
    
    public void atomicArrayExample() {
        System.out.println("\n=== Atomic Array Example ===");
        
        AtomicIntegerArray atomicArray = new AtomicIntegerArray(10);
        
        // Initialize array
        for (int i = 0; i < atomicArray.length(); i++) {
            atomicArray.set(i, i * 10);
        }
        
        System.out.println("Initial array:");
        printAtomicArray(atomicArray);
        
        // Concurrent updates
        ExecutorService executor = Executors.newFixedThreadPool(4);
        CountDownLatch latch = new CountDownLatch(4);
        
        for (int t = 0; t < 4; t++) {
            final int threadId = t;
            executor.submit(() -> {
                try {
                    for (int i = 0; i < atomicArray.length(); i++) {
                        if (i % 4 == threadId) {
                            // Each thread updates specific indices
                            int oldValue = atomicArray.getAndUpdate(i, val -> val + threadId + 1);
                            System.out.println("Thread " + threadId + " updated index " + i + 
                                             ": " + oldValue + " -> " + atomicArray.get(i));
                        }
                    }
                } finally {
                    latch.countDown();
                }
            });
        }
        
        try {
            latch.await();
            System.out.println("Final array:");
            printAtomicArray(atomicArray);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
        
        executor.shutdown();
    }
    
    private void printAtomicArray(AtomicIntegerArray array) {
        for (int i = 0; i < array.length(); i++) {
            System.out.print(array.get(i) + " ");
        }
        System.out.println();
    }
    
    public void longAdderVsAtomicLong() throws InterruptedException {
        System.out.println("\n=== LongAdder vs AtomicLong Performance ===");
        
        int numThreads = 8;
        int operationsPerThread = 100000;
        
        // Test AtomicLong
        AtomicLong atomicLong = new AtomicLong(0);
        long atomicTime = testCounter("AtomicLong", numThreads, operationsPerThread, 
                                    () -> atomicLong.incrementAndGet(), 
                                    () -> atomicLong.get());
        
        // Test LongAdder
        LongAdder longAdder = new LongAdder();
        long adderTime = testCounter("LongAdder", numThreads, operationsPerThread,
                                   () -> { longAdder.increment(); return 0L; },
                                   () -> longAdder.sum());
        
        System.out.printf("Performance improvement: %.2fx%n", 
                        (double) atomicTime / adderTime);
    }
    
    private long testCounter(String name, int numThreads, int operationsPerThread,
                           Supplier<Long> operation, Supplier<Long> reader) 
            throws InterruptedException {
        
        ExecutorService executor = Executors.newFixedThreadPool(numThreads);
        CountDownLatch latch = new CountDownLatch(numThreads);
        
        long startTime = System.nanoTime();
        
        for (int i = 0; i < numThreads; i++) {
            executor.submit(() -> {
                try {
                    for (int j = 0; j < operationsPerThread; j++) {
                        operation.get();
                    }
                } finally {
                    latch.countDown();
                }
            });
        }
        
        latch.await();
        long endTime = System.nanoTime();
        
        long duration = endTime - startTime;
        long finalValue = reader.get();
        long expected = (long) numThreads * operationsPerThread;
        
        System.out.printf("%s: %.2f ms, Value: %d (expected: %d), Correct: %s%n",
                        name, duration / 1_000_000.0, finalValue, expected, 
                        finalValue == expected);
        
        executor.shutdown();
        return duration;
    }
}
```

---

## Compare-and-Swap (CAS) Operations

### Theory

**Compare-and-Swap (CAS)** is an atomic instruction that compares the contents of a memory location with a given value and, only if they are the same, modifies the contents to a new value.

**CAS Signature**: `boolean compareAndSet(expectedValue, newValue)`

**Key Properties:**
- **Atomic**: Operation cannot be interrupted
- **Lock-free**: No blocking or waiting
- **ABA-prone**: Susceptible to ABA problem
- **Memory barriers**: Provides happens-before guarantees

### Code Examples

#### CAS-based Data Structures
```java
import java.util.concurrent.atomic.AtomicReference;

public class CASDataStructures {
    
    // Lock-free stack implementation
    public static class LockFreeStack<T> {
        private final AtomicReference<Node<T>> top = new AtomicReference<>(null);
        
        private static class Node<T> {
            final T data;
            final Node<T> next;
            
            Node(T data, Node<T> next) {
                this.data = data;
                this.next = next;
            }
        }
        
        public void push(T item) {
            Node<T> newNode = new Node<>(item, null);
            Node<T> currentTop;
            
            do {
                currentTop = top.get();
                newNode.next = currentTop;
            } while (!top.compareAndSet(currentTop, newNode));
        }
        
        public T pop() {
            Node<T> currentTop;
            Node<T> newTop;
            
            do {
                currentTop = top.get();
                if (currentTop == null) {
                    return null; // Stack is empty
                }
                newTop = currentTop.next;
            } while (!top.compareAndSet(currentTop, newTop));
            
            return currentTop.data;
        }
        
        public boolean isEmpty() {
            return top.get() == null;
        }
        
        public int size() {
            int count = 0;
            Node<T> current = top.get();
            while (current != null) {
                count++;
                current = current.next;
            }
            return count;
        }
    }
    
    // Lock-free counter with retry logic
    public static class LockFreeCounter {
        private final AtomicInteger count = new AtomicInteger(0);
        private final AtomicInteger casFailures = new AtomicInteger(0);
        
        public int increment() {
            int current;
            int updated;
            
            do {
                current = count.get();
                updated = current + 1;
                
                // Track CAS failures for monitoring
                if (!count.compareAndSet(current, updated)) {
                    casFailures.incrementAndGet();
                }
            } while (!count.compareAndSet(current, updated));
            
            return updated;
        }
        
        public int decrement() {
            int current;
            int updated;
            
            do {
                current = count.get();
                updated = current - 1;
            } while (!count.compareAndSet(current, updated));
            
            return updated;
        }
        
        public int get() {
            return count.get();
        }
        
        public int getCasFailures() {
            return casFailures.get();
        }
        
        public void reset() {
            count.set(0);
            casFailures.set(0);
        }
    }
    
    public void demonstrateLockFreeStack() throws InterruptedException {
        System.out.println("=== Lock-Free Stack Demo ===");
        
        LockFreeStack<Integer> stack = new LockFreeStack<>();
        ExecutorService executor = Executors.newFixedThreadPool(4);
        CountDownLatch latch = new CountDownLatch(4);
        
        // Two producer threads
        for (int i = 0; i < 2; i++) {
            final int producerId = i;
            executor.submit(() -> {
                try {
                    for (int j = 0; j < 5; j++) {
                        int value = producerId * 10 + j;
                        stack.push(value);
                        System.out.println("Producer " + producerId + " pushed: " + value);
                        Thread.sleep(50);
                    }
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                } finally {
                    latch.countDown();
                }
            });
        }
        
        // Two consumer threads
        for (int i = 0; i < 2; i++) {
            final int consumerId = i;
            executor.submit(() -> {
                try {
                    Thread.sleep(100); // Let producers add some items
                    
                    for (int j = 0; j < 3; j++) {
                        Integer value = stack.pop();
                        if (value != null) {
                            System.out.println("Consumer " + consumerId + " popped: " + value);
                        } else {
                            System.out.println("Consumer " + consumerId + " found empty stack");
                        }
                        Thread.sleep(100);
                    }
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                } finally {
                    latch.countDown();
                }
            });
        }
        
        latch.await();
        
        System.out.println("Final stack size: " + stack.size());
        System.out.println("Remaining items:");
        Integer item;
        while ((item = stack.pop()) != null) {
            System.out.println("  " + item);
        }
        
        executor.shutdown();
    }
    
    public void demonstrateCASContention() throws InterruptedException {
        System.out.println("\n=== CAS Contention Analysis ===");
        
        LockFreeCounter counter = new LockFreeCounter();
        int numThreads = 8;
        int operationsPerThread = 10000;
        
        ExecutorService executor = Executors.newFixedThreadPool(numThreads);
        CountDownLatch latch = new CountDownLatch(numThreads);
        
        long startTime = System.nanoTime();
        
        for (int i = 0; i < numThreads; i++) {
            executor.submit(() -> {
                try {
                    for (int j = 0; j < operationsPerThread; j++) {
                        if (j % 2 == 0) {
                            counter.increment();
                        } else {
                            counter.decrement();
                        }
                    }
                } finally {
                    latch.countDown();
                }
            });
        }
        
        latch.await();
        long endTime = System.nanoTime();
        
        System.out.printf("Final counter value: %d%n", counter.get());
        System.out.printf("CAS failures: %d%n", counter.getCasFailures());
        System.out.printf("Total operations: %d%n", numThreads * operationsPerThread);
        System.out.printf("Failure rate: %.2f%%%n", 
                        (double) counter.getCasFailures() / (numThreads * operationsPerThread) * 100);
        System.out.printf("Time taken: %.2f ms%n", (endTime - startTime) / 1_000_000.0);
        
        executor.shutdown();
    }
}
```

#### CAS with Backoff Strategy
```java
import java.util.concurrent.ThreadLocalRandom;

public class CASWithBackoff {
    
    public static class BackoffCounter {
        private final AtomicInteger count = new AtomicInteger(0);
        private final AtomicInteger attempts = new AtomicInteger(0);
        private final AtomicInteger successes = new AtomicInteger(0);
        
        public int incrementWithBackoff() {
            int backoff = 1;
            int current, updated;
            
            while (true) {
                attempts.incrementAndGet();
                current = count.get();
                updated = current + 1;
                
                if (count.compareAndSet(current, updated)) {
                    successes.incrementAndGet();
                    return updated;
                }
                
                // Exponential backoff with randomization
                try {
                    int delay = ThreadLocalRandom.current().nextInt(backoff);
                    if (delay > 0) {
                        Thread.sleep(delay);
                    }
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    return count.get();
                }
                
                backoff = Math.min(backoff * 2, 16); // Cap at 16ms
            }
        }
        
        public int get() { return count.get(); }
        public int getAttempts() { return attempts.get(); }
        public int getSuccesses() { return successes.get(); }
        
        public void reset() {
            count.set(0);
            attempts.set(0);
            successes.set(0);
        }
    }
    
    public void compareBackoffStrategies() throws InterruptedException {
        System.out.println("=== Backoff Strategy Comparison ===");
        
        int numThreads = 8;
        int operationsPerThread = 1000;
        
        // Test without backoff
        LockFreeCounter simpleCounter = new LockFreeCounter();
        long simpleTime = testCounter("Simple CAS", simpleCounter, numThreads, operationsPerThread);
        
        // Test with backoff
        BackoffCounter backoffCounter = new BackoffCounter();
        long backoffTime = testBackoffCounter("CAS with Backoff", backoffCounter, numThreads, operationsPerThread);
        
        System.out.printf("Performance comparison: Simple=%.2fms, Backoff=%.2fms%n", 
                        simpleTime / 1_000_000.0, backoffTime / 1_000_000.0);
    }
    
    private long testCounter(String name, LockFreeCounter counter, int numThreads, int operations) 
            throws InterruptedException {
        ExecutorService executor = Executors.newFixedThreadPool(numThreads);
        CountDownLatch latch = new CountDownLatch(numThreads);
        
        long startTime = System.nanoTime();
        
        for (int i = 0; i < numThreads; i++) {
            executor.submit(() -> {
                try {
                    for (int j = 0; j < operations; j++) {
                        counter.increment();
                    }
                } finally {
                    latch.countDown();
                }
            });
        }
        
        latch.await();
        long endTime = System.nanoTime();
        
        System.out.printf("%s - Value: %d, CAS failures: %d%n", 
                        name, counter.get(), counter.getCasFailures());
        
        executor.shutdown();
        return endTime - startTime;
    }
    
    private long testBackoffCounter(String name, BackoffCounter counter, int numThreads, int operations) 
            throws InterruptedException {
        ExecutorService executor = Executors.newFixedThreadPool(numThreads);
        CountDownLatch latch = new CountDownLatch(numThreads);
        
        long startTime = System.nanoTime();
        
        for (int i = 0; i < numThreads; i++) {
            executor.submit(() -> {
                try {
                    for (int j = 0; j < operations; j++) {
                        counter.incrementWithBackoff();
                    }
                } finally {
                    latch.countDown();
                }
            });
        }
        
        latch.await();
        long endTime = System.nanoTime();
        
        System.out.printf("%s - Value: %d, Attempts: %d, Success rate: %.2f%%%n", 
                        name, counter.get(), counter.getAttempts(),
                        (double) counter.getSuccesses() / counter.getAttempts() * 100);
        
        executor.shutdown();
        return endTime - startTime;
    }
}
```

---

## ABA Problem and Solutions

### Theory

The **ABA Problem** occurs when a value is changed from A to B and then back to A during a CAS operation. The CAS succeeds even though intermediate changes occurred, potentially leading to inconsistent state.

**Problem Scenario:**
1. Thread 1 reads value A
2. Thread 2 changes A to B, does work, changes B back to A
3. Thread 1's CAS succeeds thinking nothing changed

**Solutions:**
- **AtomicStampedReference**: Adds version stamp to detect changes
- **AtomicMarkableReference**: Adds boolean mark for simple flagging
- **Hazard Pointers**: Advanced technique for memory management

### Code Examples

#### Demonstrating the ABA Problem
```java
import java.util.concurrent.atomic.AtomicReference;
import java.util.concurrent.CountDownLatch;

public class ABAProblemDemo {
    
    static class Node {
        final String data;
        volatile Node next;
        
        Node(String data) {
            this.data = data;
        }
        
        @Override
        public String toString() {
            return data;
        }
    }
    
    static class ProblematicStack {
        private final AtomicReference<Node> top = new AtomicReference<>();
        
        public void push(String data) {
            Node newNode = new Node(data);
            Node currentTop;
            
            do {
                currentTop = top.get();
                newNode.next = currentTop;
            } while (!top.compareAndSet(currentTop, newNode));
        }
        
        public String pop() {
            Node currentTop;
            Node newTop;
            
            do {
                currentTop = top.get();
                if (currentTop == null) {
                    return null;
                }
                newTop = currentTop.next;
                
                // Simulate delay that allows ABA to occur
                try {
                    Thread.sleep(10);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    return null;
                }
                
            } while (!top.compareAndSet(currentTop, newTop));
            
            return currentTop.data;
        }
        
        public void printStack() {
            Node current = top.get();
            System.out.print("Stack: ");
            while (current != null) {
                System.out.print(current.data + " ");
                current = current.next;
            }
            System.out.println();
        }
    }
    
    public void demonstrateABAProblem() throws InterruptedException {
        System.out.println("=== ABA Problem Demonstration ===");
        
        ProblematicStack stack = new ProblematicStack();
        
        // Initial setup: push A, B, C
        stack.push("C");
        stack.push("B");
        stack.push("A");
        
        System.out.println("Initial state:");
        stack.printStack();
        
        CountDownLatch latch = new CountDownLatch(2);
        
        // Thread 1: Slow pop operation
        Thread thread1 = new Thread(() -> {
            try {
                System.out.println("Thread 1: Starting slow pop...");
                String popped = stack.pop();
                System.out.println("Thread 1: Popped " + popped);
            } finally {
                latch.countDown();
            }
        });
        
        // Thread 2: Quick operations causing ABA
        Thread thread2 = new Thread(() -> {
            try {
                Thread.sleep(5); // Let thread1 start
                
                System.out.println("Thread 2: Popping A and B");
                String first = stack.pop();  // Should pop A
                String second = stack.pop(); // Should pop B
                System.out.println("Thread 2: Popped " + first + " and " + second);
                
                System.out.println("Thread 2: Pushing A back");
                stack.push("A"); // A is back at top - ABA!
                
                stack.printStack();
                
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            } finally {
                latch.countDown();
            }
        });
        
        thread1.start();
        thread2.start();
        
        latch.await();
        
        System.out.println("Final state:");
        stack.printStack();
        
        System.out.println("Problem: Thread 1's CAS succeeded even though stack changed!");
    }
}
```

#### Solutions to ABA Problem
```java
import java.util.concurrent.atomic.AtomicStampedReference;
import java.util.concurrent.atomic.AtomicMarkableReference;

public class ABASolutions {
    
    // Solution 1: Using AtomicStampedReference
    static class StampedStack {
        private final AtomicStampedReference<Node> top = 
            new AtomicStampedReference<>(null, 0);
        
        public void push(String data) {
            Node newNode = new Node(data);
            int[] stampHolder = new int[1];
            Node currentTop;
            
            do {
                currentTop = top.get(stampHolder);
                newNode.next = currentTop;
            } while (!top.compareAndSet(currentTop, newNode, 
                                      stampHolder[0], stampHolder[0] + 1));
        }
        
        public String pop() {
            int[] stampHolder = new int[1];
            Node currentTop;
            Node newTop;
            
            do {
                currentTop = top.get(stampHolder);
                if (currentTop == null) {
                    return null;
                }
                newTop = currentTop.next;
                
                // Simulate delay
                try {
                    Thread.sleep(10);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    return null;
                }
                
            } while (!top.compareAndSet(currentTop, newTop, 
                                      stampHolder[0], stampHolder[0] + 1));
            
            return currentTop.data;
        }
        
        public void printStack() {
            int[] stampHolder = new int[1];
            Node current = top.get(stampHolder);
            System.out.print("StampedStack (stamp=" + stampHolder[0] + "): ");
            while (current != null) {
                System.out.print(current.data + " ");
                current = current.next;
            }
            System.out.println();
        }
    }
    
    // Solution 2: Using AtomicMarkableReference for simple flagging
    static class MarkableNode {
        final String data;
        final AtomicMarkableReference<MarkableNode> next;
        
        MarkableNode(String data) {
            this.data = data;
            this.next = new AtomicMarkableReference<>(null, false);
        }
        
        @Override
        public String toString() {
            return data;
        }
    }
    
    static class MarkableStack {
        private final AtomicMarkableReference<MarkableNode> top = 
            new AtomicMarkableReference<>(null, false);
        
        public void push(String data) {
            MarkableNode newNode = new MarkableNode(data);
            boolean[] markHolder = new boolean[1];
            MarkableNode currentTop;
            
            do {
                currentTop = top.get(markHolder);
                newNode.next.set(currentTop, false);
            } while (!top.compareAndSet(currentTop, newNode, 
                                      markHolder[0], false));
        }
        
        public String pop() {
            boolean[] markHolder = new boolean[1];
            MarkableNode currentTop;
            MarkableNode newTop;
            
            do {
                currentTop = top.get(markHolder);
                if (currentTop == null || markHolder[0]) {
                    return null; // Empty or marked for deletion
                }
                
                boolean[] nextMarkHolder = new boolean[1];
                newTop = currentTop.next.get(nextMarkHolder);
                
                // Try to mark current node as deleted first
                if (!currentTop.next.compareAndSet(newTop, newTop, false, true)) {
                    continue; // Someone else modified it
                }
                
                // Simulate delay
                try {
                    Thread.sleep(10);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    return null;
                }
                
            } while (!top.compareAndSet(currentTop, newTop, false, false));
            
            return currentTop.data;
        }
        
        public void printStack() {
            boolean[] markHolder = new boolean[1];
            MarkableNode current = top.get(markHolder);
            System.out.print("MarkableStack: ");
            while (current != null) {
                boolean[] nextMarkHolder = new boolean[1];
                MarkableNode next = current.next.get(nextMarkHolder);
                System.out.print(current.data + (nextMarkHolder[0] ? "*" : "") + " ");
                current = next;
            }
            System.out.println();
        }
    }
    
    public void demonstrateStampedSolution() throws InterruptedException {
        System.out.println("\n=== Stamped Reference Solution ===");
        
        StampedStack stack = new StampedStack();
        
        // Setup
        stack.push("C");
        stack.push("B"); 
        stack.push("A");
        
        System.out.println("Initial state:");
        stack.printStack();
        
        CountDownLatch latch = new CountDownLatch(2);
        
        // Thread 1: Slow pop
        Thread thread1 = new Thread(() -> {
            try {
                System.out.println("Thread 1: Starting slow pop...");
                String popped = stack.pop();
                System.out.println("Thread 1: Popped " + popped);
            } finally {
                latch.countDown();
            }
        });
        
        // Thread 2: ABA attempt
        Thread thread2 = new Thread(() -> {
            try {
                Thread.sleep(5);
                
                System.out.println("Thread 2: Attempting ABA sequence");
                String first = stack.pop();
                String second = stack.pop();
                System.out.println("Thread 2: Popped " + first + " and " + second);
                
                stack.push("A"); // Try to create ABA
                System.out.println("Thread 2: Pushed A back");
                
                stack.printStack();
                
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            } finally {
                latch.countDown();
            }
        });
        
        thread1.start();
        thread2.start();
        
        latch.await();
        
        System.out.println("Final state:");
        stack.printStack();
        System.out.println("Solution: Stamp prevents ABA problem!");
    }
    
    public void demonstrateMarkableSolution() throws InterruptedException {
        System.out.println("\n=== Markable Reference Solution ===");
        
        MarkableStack stack = new MarkableStack();
        
        // Setup
        stack.push("C");
        stack.push("B");
        stack.push("A");
        
        System.out.println("Initial state:");
        stack.printStack();
        
        // Demonstrate marking mechanism
        Thread popThread = new Thread(() -> {
            String popped = stack.pop();
            System.out.println("Popped: " + popped);
            stack.printStack();
        });
        
        popThread.start();
        popThread.join();
        
        System.out.println("Markable reference helps track node states during operations");
    }
    
    // Performance comparison
    public void compareABASolutions() throws InterruptedException {
        System.out.println("\n=== ABA Solution Performance Comparison ===");
        
        int operations = 10000;
        int numThreads = 4;
        
        // Test regular atomic reference
        long regularTime = testStackPerformance("Regular AtomicReference", 
                                               () -> new ProblematicStack(), operations, numThreads);
        
        // Test stamped reference
        long stampedTime = testStackPerformance("AtomicStampedReference", 
                                               () -> new StampedStack(), operations, numThreads);
        
        System.out.printf("Performance overhead of stamping: %.2fx%n", 
                        (double) stampedTime / regularTime);
    }
    
    private long testStackPerformance(String name, Supplier<Object> stackSupplier, 
                                    int operations, int numThreads) throws InterruptedException {
        Object stack = stackSupplier.get();
        ExecutorService executor = Executors.newFixedThreadPool(numThreads);
        CountDownLatch latch = new CountDownLatch(numThreads);
        
        long startTime = System.nanoTime();
        
        for (int i = 0; i < numThreads; i++) {
            executor.submit(() -> {
                try {
                    for (int j = 0; j < operations / numThreads; j++) {
                        if (stack instanceof ProblematicStack) {
                            ProblematicStack ps = (ProblematicStack) stack;
                            ps.push("Item" + j);
                            ps.pop();
                        } else if (stack instanceof StampedStack) {
                            StampedStack ss = (StampedStack) stack;
                            ss.push("Item" + j);
                            ss.pop();
                        }
                    }
                } finally {
                    latch.countDown();
                }
            });
        }
        
        latch.await();
        long endTime = System.nanoTime();
        
        long duration = endTime - startTime;
        System.out.printf("%s: %.2f ms%n", name, duration / 1_000_000.0);
        
        executor.shutdown();
        return duration;
    }
}
```

---

## AtomicStampedReference

### Theory

**AtomicStampedReference** combines an object reference with an integer "stamp" to detect if the reference has been modified even if it's changed back to the original value.

**Key Features:**
- **Reference + Stamp**: Atomic operations on both values
- **ABA Prevention**: Stamp changes detect intermediate modifications
- **Version Control**: Useful for optimistic locking patterns
- **Memory Overhead**: Additional memory for stamp storage

### Code Examples

#### Comprehensive AtomicStampedReference Usage
```java
import java.util.concurrent.atomic.AtomicStampedReference;

public class AtomicStampedReferenceExample {
    
    // Versioned data structure
    static class VersionedData {
        private final String value;
        private final long timestamp;
        
        public VersionedData(String value) {
            this.value = value;
            this.timestamp = System.currentTimeMillis();
        }
        
        public String getValue() { return value; }
        public long getTimestamp() { return timestamp; }
        
        @Override
        public String toString() {
            return String.format("%s@%d", value, timestamp);
        }
    }
    
    static class VersionedCache {
        private final AtomicStampedReference<VersionedData> data = 
            new AtomicStampedReference<>(null, 0);
        
        public boolean updateData(String newValue, int expectedStamp) {
            VersionedData newData = new VersionedData(newValue);
            int[] stampHolder = new int[1];
            VersionedData currentData = data.get(stampHolder);
            
            if (stampHolder[0] != expectedStamp) {
                System.out.println("Update failed: stamp mismatch. Expected: " + 
                                 expectedStamp + ", actual: " + stampHolder[0]);
                return false;
            }
            
            boolean success = data.compareAndSet(currentData, newData, 
                                               expectedStamp, expectedStamp + 1);
            
            if (success) {
                System.out.println("Updated data to: " + newData + 
                                 " with stamp: " + (expectedStamp + 1));
            } else {
                System.out.println("Update failed due to concurrent modification");
            }
            
            return success;
        }
        
        public VersionedData getData() {
            int[] stampHolder = new int[1];
            VersionedData currentData = data.get(stampHolder);
            System.out.println("Current data: " + currentData + 
                             " with stamp: " + stampHolder[0]);
            return currentData;
        }
        
        public int getCurrentStamp() {
            int[] stampHolder = new int[1];
            data.get(stampHolder);
            return stampHolder[0];
        }
        
        public boolean attemptWeakUpdate(String newValue) {
            // Weak update: get current stamp and try to update
            int currentStamp = getCurrentStamp();
            return updateData(newValue, currentStamp);
        }
    }
    
    public void demonstrateBasicOperations() {
        System.out.println("=== AtomicStampedReference Basic Operations ===");
        
        VersionedCache cache = new VersionedCache();
        
        // Initial state
        cache.getData();
        
        // First update
        boolean success1 = cache.updateData("Initial Value", 0);
        System.out.println("First update success: " + success1);
        
        // Get current state
        cache.getData();
        
        // Update with correct stamp
        boolean success2 = cache.updateData("Updated Value", 1);
        System.out.println("Second update success: " + success2);
        
        // Try update with wrong stamp
        boolean success3 = cache.updateData("Wrong Stamp Update", 1);
        System.out.println("Wrong stamp update success: " + success3);
        
        // Weak update (automatically gets current stamp)
        boolean success4 = cache.attemptWeakUpdate("Weak Update");
        System.out.println("Weak update success: " + success4);
        
        cache.getData();
    }
    
    public void demonstrateConcurrentUpdates() throws InterruptedException {
        System.out.println("\n=== Concurrent Updates with Stamped Reference ===");
        
        VersionedCache cache = new VersionedCache();
        cache.updateData("Initial", 0);
        
        ExecutorService executor = Executors.newFixedThreadPool(4);
        CountDownLatch latch = new CountDownLatch(4);
        AtomicInteger successCount = new AtomicInteger(0);
        
        // Multiple threads trying to update
        for (int i = 0; i < 4; i++) {
            final int threadId = i;
            executor.submit(() -> {
                try {
                    for (int j = 0; j < 5; j++) {
                        // Strategy: retry with current stamp until success
                        boolean updated = false;
                        int retries = 0;
                        
                        while (!updated && retries < 10) {
                            int currentStamp = cache.getCurrentStamp();
                            String newValue = "Thread" + threadId + "-Update" + j;
                            updated = cache.updateData(newValue, currentStamp);
                            
                            if (updated) {
                                successCount.incrementAndGet();
                            } else {
                                retries++;
                                // Small delay before retry
                                try {
                                    Thread.sleep(1);
                                } catch (InterruptedException e) {
                                    Thread.currentThread().interrupt();
                                    return;
                                }
                            }
                        }
                        
                        if (!updated) {
                            System.out.println("Thread " + threadId + 
                                             " gave up after " + retries + " retries");
                        }
                    }
                } finally {
                    latch.countDown();
                }
            });
        }
        
        latch.await();
        
        System.out.println("Total successful updates: " + successCount.get());
        System.out.println("Final state:");
        cache.getData();
        
        executor.shutdown();
    }
    
    // Optimistic locking pattern with AtomicStampedReference
    static class OptimisticCounter {
        private final AtomicStampedReference<Integer> counter = 
            new AtomicStampedReference<>(0, 0);
        
        public boolean increment() {
            int[] stampHolder = new int[1];
            Integer currentValue;
            
            do {
                currentValue = counter.get(stampHolder);
                Integer newValue = currentValue + 1;
                
                if (counter.compareAndSet(currentValue, newValue, 
                                        stampHolder[0], stampHolder[0] + 1)) {
                    return true;
                }
                // Retry with updated stamp
            } while (true);
        }
        
        public boolean add(int delta) {
            int[] stampHolder = new int[1];
            Integer currentValue;
            int attempts = 0;
            
            do {
                currentValue = counter.get(stampHolder);
                Integer newValue = currentValue + delta;
                
                if (counter.compareAndSet(currentValue, newValue, 
                                        stampHolder[0], stampHolder[0] + 1)) {
                    return true;
                }
                
                attempts++;
                if (attempts > 1000) {
                    System.out.println("Too many attempts, giving up");
                    return false;
                }
            } while (true);
        }
        
        public int getValue() {
            return counter.getReference();
        }
        
        public int getStamp() {
            int[] stampHolder = new int[1];
            counter.get(stampHolder);
            return stampHolder[0];
        }
    }
    
    public void demonstrateOptimisticLocking() throws InterruptedException {
        System.out.println("\n=== Optimistic Locking Pattern ===");
        
        OptimisticCounter counter = new OptimisticCounter();
        int numThreads = 6;
        int operationsPerThread = 1000;
        
        ExecutorService executor = Executors.newFixedThreadPool(numThreads);
        CountDownLatch latch = new CountDownLatch(numThreads);
        
        long startTime = System.nanoTime();
        
        for (int i = 0; i < numThreads; i++) {
            final int threadId = i;
            executor.submit(() -> {
                try {
                    for (int j = 0; j < operationsPerThread; j++) {
                        if (j % 10 == 0) {
                            counter.add(10); // Larger increment occasionally
                        } else {
                            counter.increment();
                        }
                    }
                    System.out.println("Thread " + threadId + " completed");
                } finally {
                    latch.countDown();
                }
            });
        }
        
        latch.await();
        long endTime = System.nanoTime();
        
        int expectedValue = numThreads * operationsPerThread + 
                           numThreads * (operationsPerThread / 10) * 9; // +9 for each add(10)
        
        System.out.printf("Final value: %d (expected: %d)%n", 
                        counter.getValue(), expectedValue);
        System.out.printf("Final stamp: %d%n", counter.getStamp());
        System.out.printf("Time taken: %.2f ms%n", (endTime - startTime) / 1_000_000.0);
        System.out.printf("Correct result: %s%n", 
                        counter.getValue() == expectedValue ? "✓" : "✗");
        
        executor.shutdown();
    }
}
```

---

## Practice Questions

### Conceptual Questions
1. **What are the advantages of atomic classes over synchronized blocks for simple operations?**
2. **Explain how Compare-and-Swap (CAS) provides thread safety without locking.**
3. **What is the ABA problem and why is it significant in lock-free programming?**
4. **When would you choose AtomicStampedReference over AtomicReference?**
5. **How do LongAdder and DoubleAdder achieve better performance than AtomicLong?**

### Coding Challenges
1. **Implement a lock-free queue using AtomicReference**
2. **Create a non-blocking cache with TTL using AtomicStampedReference**
3. **Build a lock-free binary search tree**
4. **Design a wait-free counter that handles overflow**
5. **Implement a lock-free hash table with linear probing**

### Follow-up Questions
1. **How do atomic field updaters help reduce memory overhead?**
2. **What are the memory ordering guarantees provided by atomic operations?**
3. **How can you implement exponential backoff for CAS operations?**
4. **What are hazard pointers and how do they solve memory reclamation in lock-free structures?**
5. **How do you handle the trade-off between lock-free performance and code complexity?**

---

## Key Takeaways
- **Atomic classes** provide lock-free thread safety for simple operations
- **CAS operations** are the foundation of lock-free programming
- **ABA problem** can cause subtle bugs in lock-free algorithms
- **AtomicStampedReference** prevents ABA by adding version information
- **Lock-free programming** requires careful consideration of memory ordering and edge cases
- **Performance benefits** come at the cost of increased complexity
- **Choose wisely** between locks and lock-free based on contention patterns and requirements
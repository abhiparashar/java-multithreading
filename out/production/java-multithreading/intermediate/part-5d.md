# Java Concurrency Part 5D: Advanced Level - Custom Synchronizers using AQS

## Table of Contents
1. [Custom Synchronizers using AbstractQueuedSynchronizer](#custom-synchronizers-using-abstractqueuedsynchronizer)

---

## Custom Synchronizers using AbstractQueuedSynchronizer

### Theory

**AbstractQueuedSynchronizer (AQS)** is the foundation for building custom synchronization primitives. It provides a framework for blocking locks and synchronizers that rely on FIFO wait queues.

**Key Concepts:**
- **State**: Synchronization state (atomic integer)
- **Acquire/Release**: Template methods for obtaining/releasing synchronization
- **Exclusive vs Shared**: Support for both exclusive and shared modes
- **Condition queues**: Support for condition-based waiting

### Code Examples

#### Complete AQS Implementation
```java
import java.util.concurrent.locks.AbstractQueuedSynchronizer;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class CustomSynchronizers {
    
    // Example 1: Boolean latch using AQS
    static class BooleanLatch {
        private final Sync sync = new Sync();
        
        private static class Sync extends AbstractQueuedSynchronizer {
            @Override
            protected int tryAcquireShared(int ignore) {
                return (getState() == 1) ? 1 : -1;
            }
            
            @Override
            protected boolean tryReleaseShared(int ignore) {
                setState(1);
                return true;
            }
        }
        
        public boolean isSignalled() {
            return sync.getState() == 1;
        }
        
        public void signal() {
            sync.releaseShared(1);
        }
        
        public void await() throws InterruptedException {
            sync.acquireSharedInterruptibly(1);
        }
        
        public boolean await(long time, TimeUnit unit) throws InterruptedException {
            return sync.tryAcquireSharedNanos(1, unit.toNanos(time));
        }
    }
    
    // Example 2: Counting semaphore using AQS
    static class CustomSemaphore {
        private final Sync sync;
        
        public CustomSemaphore(int permits) {
            this.sync = new Sync(permits);
        }
        
        private static class Sync extends AbstractQueuedSynchronizer {
            Sync(int permits) {
                setState(permits);
            }
            
            @Override
            protected int tryAcquireShared(int acquires) {
                for (;;) {
                    int available = getState();
                    int remaining = available - acquires;
                    if (remaining < 0 || compareAndSetState(available, remaining)) {
                        return remaining;
                    }
                }
            }
            
            @Override
            protected boolean tryReleaseShared(int releases) {
                for (;;) {
                    int current = getState();
                    int next = current + releases;
                    if (compareAndSetState(current, next)) {
                        return true;
                    }
                }
            }
        }
        
        public void acquire() throws InterruptedException {
            sync.acquireSharedInterruptibly(1);
        }
        
        public void acquire(int permits) throws InterruptedException {
            sync.acquireSharedInterruptibly(permits);
        }
        
        public boolean tryAcquire() {
            return sync.tryAcquireShared(1) >= 0;
        }
        
        public boolean tryAcquire(long timeout, TimeUnit unit) throws InterruptedException {
            return sync.tryAcquireSharedNanos(1, unit.toNanos(timeout));
        }
        
        public void release() {
            sync.releaseShared(1);
        }
        
        public void release(int permits) {
            sync.releaseShared(permits);
        }
        
        public int availablePermits() {
            return sync.getState();
        }
    }
    
    // Example 3: Exclusive lock using AQS
    static class CustomMutex {
        private final Sync sync = new Sync();
        
        private static class Sync extends AbstractQueuedSynchronizer {
            @Override
            protected boolean tryAcquire(int ignore) {
                return compareAndSetState(0, 1);
            }
            
            @Override
            protected boolean tryRelease(int ignore) {
                setState(0);
                return true;
            }
            
            @Override
            protected boolean isHeldExclusively() {
                return getState() == 1;
            }
        }
        
        public void lock() {
            sync.acquire(1);
        }
        
        public void lockInterruptibly() throws InterruptedException {
            sync.acquireInterruptibly(1);
        }
        
        public boolean tryLock() {
            return sync.tryAcquire(1);
        }
        
        public boolean tryLock(long timeout, TimeUnit unit) throws InterruptedException {
            return sync.tryAcquireNanos(1, unit.toNanos(timeout));
        }
        
        public void unlock() {
            sync.release(1);
        }
        
        public boolean isLocked() {
            return sync.isHeldExclusively();
        }
    }
    
    // Example 4: Barrier using AQS
    static class CustomBarrier {
        private final Sync sync;
        private final int parties;
        
        public CustomBarrier(int parties) {
            this.parties = parties;
            this.sync = new Sync(parties);
        }
        
        private static class Sync extends AbstractQueuedSynchronizer {
            private final int parties;
            
            Sync(int parties) {
                this.parties = parties;
                setState(parties);
            }
            
            @Override
            protected int tryAcquireShared(int ignore) {
                for (;;) {
                    int current = getState();
                    int next = current - 1;
                    
                    if (compareAndSetState(current, next)) {
                        if (next == 0) {
                            // Last thread - reset barrier and release all
                            setState(parties);
                            return parties;
                        } else {
                            // Not last thread - wait
                            return -1;
                        }
                    }
                }
            }
            
            @Override
            protected boolean tryReleaseShared(int ignore) {
                return true;
            }
        }
        
        public void await() throws InterruptedException {
            sync.acquireSharedInterruptibly(1);
        }
        
        public boolean await(long timeout, TimeUnit unit) throws InterruptedException {
            return sync.tryAcquireSharedNanos(1, unit.toNanos(timeout));
        }
        
        public int getParties() {
            return parties;
        }
        
        public int getNumberWaiting() {
            return parties - sync.getState();
        }
    }
    
    // Example 5: Resource pool using AQS
    static class ResourcePool<T> {
        private final Sync sync;
        private final T[] resources;
        private final boolean[] available;
        
        @SuppressWarnings("unchecked")
        public ResourcePool(java.util.Collection<T> resources) {
            this.resources = (T[]) resources.toArray();
            this.available = new boolean[this.resources.length];
            java.util.Arrays.fill(available, true);
            this.sync = new Sync(this.resources.length);
        }
        
        private class Sync extends AbstractQueuedSynchronizer {
            Sync(int permits) {
                setState(permits);
            }
            
            @Override
            protected int tryAcquireShared(int acquires) {
                for (;;) {
                    int available = getState();
                    int remaining = available - acquires;
                    if (remaining < 0 || compareAndSetState(available, remaining)) {
                        return remaining;
                    }
                }
            }
            
            @Override
            protected boolean tryReleaseShared(int releases) {
                for (;;) {
                    int current = getState();
                    int next = current + releases;
                    if (compareAndSetState(current, next)) {
                        return true;
                    }
                }
            }
        }
        
        public T acquire() throws InterruptedException {
            sync.acquireSharedInterruptibly(1);
            return getResource();
        }
        
        public T acquire(long timeout, TimeUnit unit) throws InterruptedException {
            if (sync.tryAcquireSharedNanos(1, unit.toNanos(timeout))) {
                return getResource();
            }
            return null;
        }
        
        public void release(T resource) {
            if (returnResource(resource)) {
                sync.releaseShared(1);
            }
        }
        
        private synchronized T getResource() {
            for (int i = 0; i < available.length; i++) {
                if (available[i]) {
                    available[i] = false;
                    return resources[i];
                }
            }
            throw new IllegalStateException("No resources available");
        }
        
        private synchronized boolean returnResource(T resource) {
            for (int i = 0; i < resources.length; i++) {
                if (resources[i].equals(resource) && !available[i]) {
                    available[i] = true;
                    return true;
                }
            }
            return false;
        }
        
        public int availableResources() {
            return sync.getState();
        }
    }
    
    public void demonstrateCustomSynchronizers() throws InterruptedException {
        System.out.println("=== Custom Synchronizers Demo ===");
        
        // Test BooleanLatch
        System.out.println("--- Boolean Latch Test ---");
        BooleanLatch latch = new BooleanLatch();
        ExecutorService executor = Executors.newFixedThreadPool(6);
        
        // Start waiting threads
        for (int i = 0; i < 3; i++) {
            final int threadId = i;
            executor.submit(() -> {
                try {
                    System.out.println("Thread " + threadId + " waiting for signal...");
                    latch.await();
                    System.out.println("Thread " + threadId + " received signal!");
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
            });
        }
        
        // Signal after delay
        executor.submit(() -> {
            try {
                Thread.sleep(2000);
                System.out.println("Signaling all waiting threads...");
                latch.signal();
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        });
        
        Thread.sleep(3000);
        
        // Test CustomSemaphore
        System.out.println("\n--- Custom Semaphore Test ---");
        CustomSemaphore semaphore = new CustomSemaphore(2);
        CountDownLatch semaphoreTest = new CountDownLatch(4);
        
        for (int i = 0; i < 4; i++) {
            final int taskId = i;
            executor.submit(() -> {
                try {
                    System.out.println("Task " + taskId + " trying to acquire permit...");
                    semaphore.acquire();
                    System.out.println("Task " + taskId + " acquired permit, working...");
                    Thread.sleep(1000);
                    System.out.println("Task " + taskId + " releasing permit");
                    semaphore.release();
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                } finally {
                    semaphoreTest.countDown();
                }
            });
        }
        
        semaphoreTest.await();
        
        // Test CustomMutex
        System.out.println("\n--- Custom Mutex Test ---");
        CustomMutex mutex = new CustomMutex();
        CountDownLatch mutexTest = new CountDownLatch(3);
        
        for (int i = 0; i < 3; i++) {
            final int threadId = i;
            executor.submit(() -> {
                try {
                    System.out.println("Thread " + threadId + " trying to acquire mutex...");
                    mutex.lock();
                    System.out.println("Thread " + threadId + " acquired mutex");
                    Thread.sleep(500);
                    System.out.println("Thread " + threadId + " releasing mutex");
                    mutex.unlock();
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                } finally {
                    mutexTest.countDown();
                }
            });
        }
        
        mutexTest.await();
        
        // Test CustomBarrier
        System.out.println("\n--- Custom Barrier Test ---");
        CustomBarrier barrier = new CustomBarrier(3);
        CountDownLatch barrierTest = new CountDownLatch(3);
        
        for (int i = 0; i < 3; i++) {
            final int threadId = i;
            executor.submit(() -> {
                try {
                    System.out.println("Thread " + threadId + " working...");
                    Thread.sleep(1000 + threadId * 200);
                    System.out.println("Thread " + threadId + " waiting at barrier");
                    barrier.await();
                    System.out.println("Thread " + threadId + " passed barrier");
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                } finally {
                    barrierTest.countDown();
                }
            });
        }
        
        barrierTest.await();
        
        // Test ResourcePool
        System.out.println("\n--- Resource Pool Test ---");
        java.util.List<String> resourceList = java.util.Arrays.asList("Resource1", "Resource2", "Resource3");
        ResourcePool<String> resourcePool = new ResourcePool<>(resourceList);
        CountDownLatch resourceTest = new CountDownLatch(5);
        
        for (int i = 0; i < 5; i++) {
            final int taskId = i;
            executor.submit(() -> {
                try {
                    System.out.println("Task " + taskId + " trying to acquire resource...");
                    String resource = resourcePool.acquire(2, TimeUnit.SECONDS);
                    
                    if (resource != null) {
                        System.out.println("Task " + taskId + " acquired: " + resource);
                        Thread.sleep(1000);
                        resourcePool.release(resource);
                        System.out.println("Task " + taskId + " released: " + resource);
                    } else {
                        System.out.println("Task " + taskId + " timed out waiting for resource");
                    }
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                } finally {
                    resourceTest.countDown();
                }
            });
        }
        
        resourceTest.await();
        executor.shutdown();
        
        System.out.println("Custom synchronizers demo completed");
    }
}
```

---

## Practice Questions

### Conceptual Questions
1. **When would you implement a custom synchronizer using AbstractQueuedSynchronizer instead of using existing primitives?**
2. **What are the key differences between exclusive and shared modes in AQS?**
3. **How does AQS handle thread interruption and what are the implications for custom synchronizers?**
4. **What is the role of the state variable in AQS and how should it be managed?**
5. **How do you implement timeout support in custom synchronizers built with AQS?**

### Coding Challenges
1. **Implement a custom ReadWriteLock using AQS that supports fairness**
2. **Create a resource pool synchronizer with different priority levels**
3. **Build a custom synchronizer that supports both blocking and non-blocking operations**
4. **Design a synchronizer for producer-consumer scenarios with bounded capacity**
5. **Implement a custom barrier that supports dynamic registration like Phaser**

### Advanced Scenarios
1. **Design a distributed locking mechanism using AQS as the foundation**
2. **Create a synchronizer for coordinating multiple phases of work**
3. **Build a custom synchronizer that integrates with monitoring and observability systems**
4. **Implement a synchronizer that supports hierarchical resource allocation**
5. **Design a custom coordination primitive for reactive systems**

### Follow-up Questions
1. **How do you handle spurious wakeups in custom synchronizers and why are they important?**
2. **What are the memory consistency guarantees provided by AQS operations?**
3. **How do you debug and troubleshoot issues in custom synchronizers built with AQS?**
4. **What performance considerations should guide the design of custom synchronizers?**
5. **How do you test custom synchronizers for correctness under high concurrency?**

---

## Key Takeaways

### AbstractQueuedSynchronizer (AQS)
- **Foundation for most Java synchronization primitives** including ReentrantLock, CountDownLatch, Semaphore
- **Template method pattern** simplifies implementation by providing the queuing and blocking framework
- **State management** is the core responsibility of the custom synchronizer implementation
- **Supports both exclusive and shared modes** to cover different synchronization scenarios
- **Built-in support for interruption and timeouts** makes it suitable for production use

### Custom Synchronizer Design
- **Identify the synchronization semantics** needed before choosing AQS vs other approaches
- **State representation** should be carefully designed to support all required operations atomically
- **Fairness considerations** can be implemented through careful ordering of thread wakeups
- **Performance characteristics** depend heavily on the specific implementation of tryAcquire/tryRelease methods
- **Error handling and edge cases** must be thoroughly considered and tested

### Implementation Best Practices
- **Keep state transitions simple** and atomic to avoid race conditions
- **Document the synchronization contract** clearly for users of the custom synchronizer
- **Test extensively** under high concurrency to verify correctness
- **Consider using existing primitives** before implementing custom solutions
- **Profile performance** to ensure the custom implementation provides the expected benefits

### Common Use Cases
- **Resource pooling** with custom allocation strategies
- **Application-specific coordination** that doesn't map well to existing primitives
- **Performance optimization** for specific access patterns
- **Integration with external systems** that require custom signaling protocols
- **Complex state machines** that need fine-grained control over thread coordination

Custom synchronizers built with AQS provide ultimate flexibility for application-specific coordination needs, but should be implemented with careful consideration of the complexity they introduce to the system.
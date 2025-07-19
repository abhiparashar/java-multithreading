# Java Concurrency Part 5B: Advanced Level - Reactive Programming & ThreadLocal

## Table of Contents
1. [Reactive Programming Basics](#reactive-programming-basics)
2. [Thread-Local Storage](#thread-local-storage)

---

## Reactive Programming Basics

### Theory

**Reactive Programming** is a programming paradigm focused on asynchronous data streams and the propagation of change. It's based on the Observer pattern but provides powerful operators for composing and transforming streams.

**Key Concepts:**
- **Streams**: Sequences of events over time
- **Observers**: React to emitted events
- **Operators**: Transform, filter, combine streams
- **Backpressure**: Handle fast producers and slow consumers
- **Non-blocking**: Asynchronous by design

### Code Examples

#### Complete Reactive Streams Implementation
```java
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.List;
import java.util.ArrayList;
import java.util.Date;
import java.util.concurrent.CopyOnWriteArrayList;

public class ReactiveBasics {
    
    // Simple reactive stream implementation
    interface Observable<T> {
        void subscribe(Observer<T> observer);
    }
    
    interface Observer<T> {
        void onNext(T item);
        void onError(Throwable error);
        void onComplete();
    }
    
    // Basic observable implementation
    static class SimpleObservable<T> implements Observable<T> {
        private final List<T> items;
        
        public SimpleObservable(List<T> items) {
            this.items = new ArrayList<>(items);
        }
        
        @Override
        public void subscribe(Observer<T> observer) {
            try {
                for (T item : items) {
                    observer.onNext(item);
                }
                observer.onComplete();
            } catch (Exception e) {
                observer.onError(e);
            }
        }
        
        // Factory methods
        public static <T> Observable<T> fromList(List<T> items) {
            return new SimpleObservable<>(items);
        }
        
        public static Observable<Integer> range(int start, int count) {
            List<Integer> items = new ArrayList<>();
            for (int i = 0; i < count; i++) {
                items.add(start + i);
            }
            return new SimpleObservable<>(items);
        }
    }
    
    // Async observable that emits items over time
    static class AsyncObservable<T> implements Observable<T> {
        private final List<T> items;
        private final int delayMs;
        private final ScheduledExecutorService scheduler;
        
        public AsyncObservable(List<T> items, int delayMs) {
            this.items = new ArrayList<>(items);
            this.delayMs = delayMs;
            this.scheduler = Executors.newScheduledThreadPool(1);
        }
        
        @Override
        public void subscribe(Observer<T> observer) {
            AtomicBoolean cancelled = new AtomicBoolean(false);
            
            // Schedule emission of items
            for (int i = 0; i < items.size(); i++) {
                final int index = i;
                scheduler.schedule(() -> {
                    if (!cancelled.get()) {
                        observer.onNext(items.get(index));
                        
                        // Complete after last item
                        if (index == items.size() - 1) {
                            observer.onComplete();
                            scheduler.shutdown();
                        }
                    }
                }, delayMs * (i + 1), TimeUnit.MILLISECONDS);
            }
        }
    }
    
    // Observable with operators
    static class OperatorObservable<T> implements Observable<T> {
        private final Observable<T> source;
        
        public OperatorObservable(Observable<T> source) {
            this.source = source;
        }
        
        public <R> Observable<R> map(Function<T, R> mapper) {
            return new Observable<R>() {
                @Override
                public void subscribe(Observer<R> observer) {
                    source.subscribe(new Observer<T>() {
                        @Override
                        public void onNext(T item) {
                            try {
                                R mapped = mapper.apply(item);
                                observer.onNext(mapped);
                            } catch (Exception e) {
                                observer.onError(e);
                            }
                        }
                        
                        @Override
                        public void onError(Throwable error) {
                            observer.onError(error);
                        }
                        
                        @Override
                        public void onComplete() {
                            observer.onComplete();
                        }
                    });
                }
            };
        }
        
        public Observable<T> filter(Predicate<T> predicate) {
            return new Observable<T>() {
                @Override
                public void subscribe(Observer<T> observer) {
                    source.subscribe(new Observer<T>() {
                        @Override
                        public void onNext(T item) {
                            try {
                                if (predicate.test(item)) {
                                    observer.onNext(item);
                                }
                            } catch (Exception e) {
                                observer.onError(e);
                            }
                        }
                        
                        @Override
                        public void onError(Throwable error) {
                            observer.onError(error);
                        }
                        
                        @Override
                        public void onComplete() {
                            observer.onComplete();
                        }
                    });
                }
            };
        }
        
        public Observable<T> take(int count) {
            return new Observable<T>() {
                @Override
                public void subscribe(Observer<T> observer) {
                    source.subscribe(new Observer<T>() {
                        private int emitted = 0;
                        
                        @Override
                        public void onNext(T item) {
                            if (emitted < count) {
                                observer.onNext(item);
                                emitted++;
                                
                                if (emitted == count) {
                                    observer.onComplete();
                                }
                            }
                        }
                        
                        @Override
                        public void onError(Throwable error) {
                            observer.onError(error);
                        }
                        
                        @Override
                        public void onComplete() {
                            if (emitted < count) {
                                observer.onComplete();
                            }
                        }
                    });
                }
            };
        }
    }
    
    public void demonstrateBasicObservable() {
        System.out.println("=== Basic Observable Demo ===");
        
        List<String> data = List.of("Apple", "Banana", "Cherry", "Date", "Elderberry");
        Observable<String> observable = SimpleObservable.fromList(data);
        
        observable.subscribe(new Observer<String>() {
            @Override
            public void onNext(String item) {
                System.out.println("Received: " + item);
            }
            
            @Override
            public void onError(Throwable error) {
                System.err.println("Error: " + error.getMessage());
            }
            
            @Override
            public void onComplete() {
                System.out.println("Stream completed");
            }
        });
    }
    
    public void demonstrateAsyncObservable() throws InterruptedException {
        System.out.println("\n=== Async Observable Demo ===");
        
        List<Integer> numbers = List.of(1, 2, 3, 4, 5);
        Observable<Integer> asyncObservable = new AsyncObservable<>(numbers, 200);
        
        CountDownLatch latch = new CountDownLatch(1);
        
        asyncObservable.subscribe(new Observer<Integer>() {
            @Override
            public void onNext(Integer item) {
                System.out.println("Async received: " + item + " at " + 
                                 System.currentTimeMillis());
            }
            
            @Override
            public void onError(Throwable error) {
                System.err.println("Async error: " + error.getMessage());
                latch.countDown();
            }
            
            @Override
            public void onComplete() {
                System.out.println("Async stream completed");
                latch.countDown();
            }
        });
        
        latch.await(5, TimeUnit.SECONDS);
    }
    
    public void demonstrateOperators() {
        System.out.println("\n=== Observable Operators Demo ===");
        
        Observable<Integer> numbers = SimpleObservable.range(1, 10);
        
        // Chain operators
        OperatorObservable<Integer> operatorObservable = new OperatorObservable<>(numbers);
        
        Observable<String> result = operatorObservable
            .filter(n -> n % 2 == 0)           // Only even numbers
            .map(n -> n * n)                   // Square them
            .take(3)                           // Take first 3
            .map(n -> "Square: " + n);         // Convert to string
        
        result.subscribe(new Observer<String>() {
            @Override
            public void onNext(String item) {
                System.out.println("Operator result: " + item);
            }
            
            @Override
            public void onError(Throwable error) {
                System.err.println("Operator error: " + error.getMessage());
            }
            
            @Override
            public void onComplete() {
                System.out.println("Operator chain completed");
            }
        });
    }
    
    // Event stream simulation
    static class EventStream {
        private final List<Observer<Event>> observers = new CopyOnWriteArrayList<>();
        private final ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(2);
        private volatile boolean running = false;
        
        static class Event {
            final String type;
            final String data;
            final long timestamp;
            
            Event(String type, String data) {
                this.type = type;
                this.data = data;
                this.timestamp = System.currentTimeMillis();
            }
            
            @Override
            public String toString() {
                return String.format("Event{type='%s', data='%s', timestamp=%d}", 
                                   type, data, timestamp);
            }
        }
        
        public void subscribe(Observer<Event> observer) {
            observers.add(observer);
        }
        
        public void start() {
            if (running) return;
            running = true;
            
            // Simulate user events
            scheduler.scheduleAtFixedRate(() -> {
                Event event = new Event("USER_ACTION", "User clicked button " + 
                                       (int)(Math.random() * 100));
                notifyObservers(event);
            }, 0, 500, TimeUnit.MILLISECONDS);
            
            // Simulate system events
            scheduler.scheduleAtFixedRate(() -> {
                Event event = new Event("SYSTEM", "Memory usage: " + 
                                       (int)(Math.random() * 100) + "%");
                notifyObservers(event);
            }, 100, 1000, TimeUnit.MILLISECONDS);
        }
        
        public void stop() {
            running = false;
            scheduler.shutdown();
            observers.forEach(observer -> observer.onComplete());
        }
        
        private void notifyObservers(Event event) {
            observers.forEach(observer -> {
                try {
                    observer.onNext(event);
                } catch (Exception e) {
                    observer.onError(e);
                }
            });
        }
    }
    
    public void demonstrateEventStream() throws InterruptedException {
        System.out.println("\n=== Event Stream Demo ===");
        
        EventStream eventStream = new EventStream();
        
        // Subscribe to all events
        eventStream.subscribe(new Observer<EventStream.Event>() {
            @Override
            public void onNext(EventStream.Event event) {
                System.out.println("All events: " + event);
            }
            
            @Override
            public void onError(Throwable error) {
                System.err.println("Event stream error: " + error.getMessage());
            }
            
            @Override
            public void onComplete() {
                System.out.println("Event stream completed");
            }
        });
        
        // Subscribe to only user events
        eventStream.subscribe(new Observer<EventStream.Event>() {
            @Override
            public void onNext(EventStream.Event event) {
                if ("USER_ACTION".equals(event.type)) {
                    System.out.println("User event filter: " + event);
                }
            }
            
            @Override
            public void onError(Throwable error) {}
            
            @Override
            public void onComplete() {
                System.out.println("User event filter completed");
            }
        });
        
        eventStream.start();
        Thread.sleep(3000);
        eventStream.stop();
        
        Thread.sleep(100); // Let completion messages print
    }
}
```

---

## Thread-Local Storage

### Theory

**ThreadLocal** provides thread-local variables where each thread has its own independent copy of the variable. This eliminates the need for synchronization when the data doesn't need to be shared between threads.

**Key Concepts:**
- **Thread isolation**: Each thread has its own copy
- **No synchronization needed**: No race conditions
- **Memory leaks**: Must be careful to clean up
- **Inheritance**: InheritableThreadLocal passes values to child threads

### Code Examples

#### Complete ThreadLocal Implementation
```java
import java.util.concurrent.atomic.AtomicInteger;
import java.util.Random;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicLong;

public class ThreadLocalDemo {
    
    // Example 1: Basic ThreadLocal usage
    private static final ThreadLocal<Integer> threadLocalValue = new ThreadLocal<Integer>() {
        @Override
        protected Integer initialValue() {
            return 0; // Default initial value
        }
    };
    
    // Modern way using Supplier (Java 8+)
    private static final ThreadLocal<Random> threadLocalRandom = 
        ThreadLocal.withInitial(() -> new Random());
    
    public void demonstrateBasicThreadLocal() throws InterruptedException {
        System.out.println("=== Basic ThreadLocal Demo ===");
        
        ExecutorService executor = Executors.newFixedThreadPool(3);
        CountDownLatch latch = new CountDownLatch(3);
        
        for (int i = 0; i < 3; i++) {
            final int threadId = i;
            executor.submit(() -> {
                try {
                    // Each thread gets its own copy
                    System.out.println("Thread " + threadId + " initial value: " + 
                                     threadLocalValue.get());
                    
                    // Modify the thread-local value
                    threadLocalValue.set(threadId * 10);
                    
                    // Generate some random numbers using thread-local Random
                    Random random = threadLocalRandom.get();
                    for (int j = 0; j < 3; j++) {
                        int randomValue = random.nextInt(100);
                        threadLocalValue.set(threadLocalValue.get() + randomValue);
                        System.out.println("Thread " + threadId + " added " + randomValue + 
                                         ", new value: " + threadLocalValue.get());
                        
                        Thread.sleep(100);
                    }
                    
                    System.out.println("Thread " + threadId + " final value: " + 
                                     threadLocalValue.get());
                    
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                } finally {
                    // Important: Clean up to avoid memory leaks
                    threadLocalValue.remove();
                    threadLocalRandom.remove();
                    latch.countDown();
                }
            });
        }
        
        latch.await();
        executor.shutdown();
    }
    
    // Example 2: Thread-local context for request processing
    static class RequestContext {
        private final String requestId;
        private final String userId;
        private final long startTime;
        
        public RequestContext(String requestId, String userId) {
            this.requestId = requestId;
            this.userId = userId;
            this.startTime = System.currentTimeMillis();
        }
        
        public String getRequestId() { return requestId; }
        public String getUserId() { return userId; }
        public long getStartTime() { return startTime; }
        public long getElapsedTime() { return System.currentTimeMillis() - startTime; }
        
        @Override
        public String toString() {
            return String.format("RequestContext{requestId='%s', userId='%s', elapsed=%dms}", 
                               requestId, userId, getElapsedTime());
        }
    }
    
    static class RequestContextHolder {
        private static final ThreadLocal<RequestContext> context = new ThreadLocal<>();
        
        public static void setContext(RequestContext requestContext) {
            context.set(requestContext);
        }
        
        public static RequestContext getContext() {
            return context.get();
        }
        
        public static void clear() {
            context.remove();
        }
        
        public static String getCurrentRequestId() {
            RequestContext ctx = getContext();
            return ctx != null ? ctx.getRequestId() : "unknown";
        }
        
        public static String getCurrentUserId() {
            RequestContext ctx = getContext();
            return ctx != null ? ctx.getUserId() : "anonymous";
        }
    }
    
    // Service classes that use the request context
    static class UserService {
        public String getUserProfile() {
            String userId = RequestContextHolder.getCurrentUserId();
            String requestId = RequestContextHolder.getCurrentRequestId();
            
            System.out.println("UserService.getUserProfile() - RequestId: " + requestId + 
                             ", UserId: " + userId);
            
            // Simulate processing
            try {
                Thread.sleep(50);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
            
            return "Profile for user " + userId;
        }
    }
    
    static class OrderService {
        public String getOrders() {
            String userId = RequestContextHolder.getCurrentUserId();
            String requestId = RequestContextHolder.getCurrentRequestId();
            
            System.out.println("OrderService.getOrders() - RequestId: " + requestId + 
                             ", UserId: " + userId);
            
            try {
                Thread.sleep(75);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
            
            return "Orders for user " + userId;
        }
    }
    
    static class RequestProcessor {
        private final UserService userService = new UserService();
        private final OrderService orderService = new OrderService();
        
        public void processRequest(String requestId, String userId) {
            try {
                // Set up request context
                RequestContext context = new RequestContext(requestId, userId);
                RequestContextHolder.setContext(context);
                
                System.out.println("Processing request: " + context);
                
                // Process the request - services automatically get context
                String userProfile = userService.getUserProfile();
                String orders = orderService.getOrders();
                
                System.out.println("Request " + requestId + " completed - Profile: " + 
                                 userProfile + ", Orders: " + orders);
                System.out.println("Total time: " + context.getElapsedTime() + "ms");
                
            } finally {
                // Always clean up the context
                RequestContextHolder.clear();
            }
        }
    }
    
    public void demonstrateRequestContext() throws InterruptedException {
        System.out.println("\n=== Request Context Demo ===");
        
        RequestProcessor processor = new RequestProcessor();
        ExecutorService executor = Executors.newFixedThreadPool(3);
        CountDownLatch latch = new CountDownLatch(5);
        
        // Simulate multiple concurrent requests
        String[] userIds = {"user1", "user2", "user3", "user1", "user2"};
        
        for (int i = 0; i < userIds.length; i++) {
            final String requestId = "req-" + (i + 1);
            final String userId = userIds[i];
            
            executor.submit(() -> {
                try {
                    processor.processRequest(requestId, userId);
                } finally {
                    latch.countDown();
                }
            });
        }
        
        latch.await();
        executor.shutdown();
    }
    
    // Example 3: InheritableThreadLocal for parent-child thread communication
    private static final InheritableThreadLocal<String> inheritableContext = 
        new InheritableThreadLocal<String>() {
            @Override
            protected String initialValue() {
                return "default-context";
            }
            
            @Override
            protected String childValue(String parentValue) {
                return parentValue + "-child";
            }
        };
    
    public void demonstrateInheritableThreadLocal() throws InterruptedException {
        System.out.println("\n=== InheritableThreadLocal Demo ===");
        
        // Set value in main thread
        inheritableContext.set("main-thread-context");
        System.out.println("Main thread context: " + inheritableContext.get());
        
        // Create child thread
        Thread childThread = new Thread(() -> {
            System.out.println("Child thread inherited context: " + inheritableContext.get());
            
            // Modify in child thread
            inheritableContext.set(inheritableContext.get() + "-modified");
            System.out.println("Child thread modified context: " + inheritableContext.get());
            
            // Create grandchild thread
            Thread grandchildThread = new Thread(() -> {
                System.out.println("Grandchild thread inherited context: " + 
                                 inheritableContext.get());
            });
            
            grandchildThread.start();
            try {
                grandchildThread.join();
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        });
        
        childThread.start();
        childThread.join();
        
        // Check main thread context (unchanged)
        System.out.println("Main thread context after child modification: " + 
                         inheritableContext.get());
        
        inheritableContext.remove();
    }
    
    // Example 4: Performance comparison with and without ThreadLocal
    static class PerformanceComparison {
        private static final ThreadLocal<StringBuilder> threadLocalBuilder = 
            ThreadLocal.withInitial(() -> new StringBuilder());
        
        public String buildStringWithThreadLocal(int iterations) {
            StringBuilder builder = threadLocalBuilder.get();
            builder.setLength(0); // Reset
            
            for (int i = 0; i < iterations; i++) {
                builder.append("Item ").append(i).append(", ");
            }
            
            return builder.toString();
        }
        
        public String buildStringWithNewInstance(int iterations) {
            StringBuilder builder = new StringBuilder();
            
            for (int i = 0; i < iterations; i++) {
                builder.append("Item ").append(i).append(", ");
            }
            
            return builder.toString();
        }
    }
    
    public void performanceComparison() throws InterruptedException {
        System.out.println("\n=== ThreadLocal Performance Comparison ===");
        
        PerformanceComparison comparison = new PerformanceComparison();
        int numThreads = 4;
        int iterations = 10000;
        int stringBuilderIterations = 1000;
        
        ExecutorService executor = Executors.newFixedThreadPool(numThreads);
        
        // Test with ThreadLocal
        long startTime = System.nanoTime();
        CountDownLatch threadLocalLatch = new CountDownLatch(numThreads);
        
        for (int i = 0; i < numThreads; i++) {
            executor.submit(() -> {
                try {
                    for (int j = 0; j < iterations; j++) {
                        comparison.buildStringWithThreadLocal(stringBuilderIterations);
                    }
                } finally {
                    threadLocalLatch.countDown();
                }
            });
        }
        
        threadLocalLatch.await();
        long threadLocalTime = System.nanoTime() - startTime;
        
        // Test with new instances
        startTime = System.nanoTime();
        CountDownLatch newInstanceLatch = new CountDownLatch(numThreads);
        
        for (int i = 0; i < numThreads; i++) {
            executor.submit(() -> {
                try {
                    for (int j = 0; j < iterations; j++) {
                        comparison.buildStringWithNewInstance(stringBuilderIterations);
                    }
                } finally {
                    newInstanceLatch.countDown();
                }
            });
        }
        
        newInstanceLatch.await();
        long newInstanceTime = System.nanoTime() - startTime;
        
        System.out.printf("ThreadLocal approach: %.2f ms%n", threadLocalTime / 1_000_000.0);
        System.out.printf("New instance approach: %.2f ms%n", newInstanceTime / 1_000_000.0);
        System.out.printf("Performance improvement: %.2fx%n", 
                        (double) newInstanceTime / threadLocalTime);
        
        executor.shutdown();
    }
}
```

---

## Practice Questions

### Conceptual Questions
1. **What are the key principles of reactive programming and when should it be used?**
2. **When should you use ThreadLocal and what are the potential memory leak issues?**
3. **How does backpressure work in reactive streams?**
4. **What's the difference between ThreadLocal and InheritableThreadLocal?**
5. **How do reactive streams handle error propagation?**

### Coding Challenges
1. **Build a reactive stream processor that handles backpressure**
2. **Design a ThreadLocal-based transaction management system**
3. **Create a reactive event bus for decoupled communication**
4. **Implement a ThreadLocal-based logging context system**
5. **Build a reactive data processing pipeline with error handling**

### Follow-up Questions
1. **How do you implement backpressure in a reactive system?**
2. **What are the best practices for using ThreadLocal in web applications?**
3. **How do you prevent memory leaks when using ThreadLocal in application servers?**
4. **What are the trade-offs between reactive programming and traditional async approaches?**
5. **How do you handle cleanup in reactive streams when subscribers disconnect?**

---

## Key Takeaways

### Reactive Programming
- **Event-driven** programming model for handling streams
- **Operators** provide powerful transformation capabilities
- **Backpressure** handling is essential for production systems
- **Non-blocking** by design improves scalability
- **Error handling** can be done at multiple levels in the stream

### ThreadLocal
- **Thread isolation** eliminates synchronization needs
- **Memory leaks** are a serious concern - always clean up
- **Request context** pattern is very useful in web applications
- **InheritableThreadLocal** passes values to child threads
- **Performance benefits** come from avoiding object creation

### Best Practices
- Use reactive programming for event-driven, async data processing
- Always clean up ThreadLocal variables to prevent memory leaks
- Consider using ThreadLocal for request-scoped data in web applications
- Implement proper error handling in reactive streams
- Test reactive systems under load to ensure backpressure handling works correctly
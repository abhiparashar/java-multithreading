# Java Concurrency Part 5: Advanced Level - Fork-Join Framework & CompletableFuture

## Table of Contents
1. [Fork-Join Framework](#fork-join-framework)
2. [CompletableFuture and Async Programming](#completablefuture-and-async-programming)

---

## Fork-Join Framework

### Theory

The **Fork-Join Framework** is designed for recursive divide-and-conquer algorithms. It uses work-stealing to efficiently distribute tasks across multiple threads.

**Key Concepts:**
- **Work Stealing**: Idle threads steal tasks from busy threads' queues
- **Fork**: Split task into smaller subtasks
- **Join**: Wait for subtasks to complete and combine results
- **ForkJoinPool**: Thread pool optimized for fork-join tasks
- **RecursiveTask**: Returns a result
- **RecursiveAction**: Performs work without returning result

### Code Examples

#### Complete Fork-Join Implementation
```java
import java.util.concurrent.*;
import java.util.Arrays;

public class ForkJoinDemo {
    
    // Example 1: Recursive sum calculation
    static class SumTask extends RecursiveTask<Long> {
        private static final int THRESHOLD = 1000;
        private final int[] array;
        private final int start;
        private final int end;
        
        public SumTask(int[] array, int start, int end) {
            this.array = array;
            this.start = start;
            this.end = end;
        }
        
        @Override
        protected Long compute() {
            if (end - start <= THRESHOLD) {
                // Base case: compute directly
                long sum = 0;
                for (int i = start; i < end; i++) {
                    sum += array[i];
                }
                return sum;
            } else {
                // Recursive case: split the task
                int mid = start + (end - start) / 2;
                
                SumTask leftTask = new SumTask(array, start, mid);
                SumTask rightTask = new SumTask(array, mid, end);
                
                // Fork the left task (async)
                leftTask.fork();
                
                // Compute right task in current thread
                Long rightResult = rightTask.compute();
                
                // Join the left task (wait for completion)
                Long leftResult = leftTask.join();
                
                return leftResult + rightResult;
            }
        }
    }
    
    // Example 2: Parallel merge sort
    static class MergeSortTask extends RecursiveAction {
        private static final int THRESHOLD = 100;
        private final int[] array;
        private final int[] temp;
        private final int start;
        private final int end;
        
        public MergeSortTask(int[] array, int[] temp, int start, int end) {
            this.array = array;
            this.temp = temp;
            this.start = start;
            this.end = end;
        }
        
        @Override
        protected void compute() {
            if (end - start <= THRESHOLD) {
                // Base case: use sequential sort
                Arrays.sort(array, start, end);
            } else {
                // Recursive case: divide and conquer
                int mid = start + (end - start) / 2;
                
                MergeSortTask leftTask = new MergeSortTask(array, temp, start, mid);
                MergeSortTask rightTask = new MergeSortTask(array, temp, mid, end);
                
                // Fork both tasks
                invokeAll(leftTask, rightTask);
                
                // Merge the sorted halves
                merge(start, mid, end);
            }
        }
        
        private void merge(int start, int mid, int end) {
            // Copy to temp array
            System.arraycopy(array, start, temp, start, end - start);
            
            int i = start;
            int j = mid;
            int k = start;
            
            // Merge back to original array
            while (i < mid && j < end) {
                if (temp[i] <= temp[j]) {
                    array[k++] = temp[i++];
                } else {
                    array[k++] = temp[j++];
                }
            }
            
            // Copy remaining elements
            while (i < mid) {
                array[k++] = temp[i++];
            }
            while (j < end) {
                array[k++] = temp[j++];
            }
        }
    }
    
    // Example 3: Matrix multiplication using fork-join
    static class MatrixMultiplyTask extends RecursiveTask<int[][]> {
        private static final int THRESHOLD = 64;
        private final int[][] a;
        private final int[][] b;
        private final int startRow;
        private final int endRow;
        
        public MatrixMultiplyTask(int[][] a, int[][] b, int startRow, int endRow) {
            this.a = a;
            this.b = b;
            this.startRow = startRow;
            this.endRow = endRow;
        }
        
        @Override
        protected int[][] compute() {
            if (endRow - startRow <= THRESHOLD) {
                return computeDirectly();
            } else {
                int mid = startRow + (endRow - startRow) / 2;
                
                MatrixMultiplyTask upperTask = new MatrixMultiplyTask(a, b, startRow, mid);
                MatrixMultiplyTask lowerTask = new MatrixMultiplyTask(a, b, mid, endRow);
                
                upperTask.fork();
                int[][] lowerResult = lowerTask.compute();
                int[][] upperResult = upperTask.join();
                
                // Combine results
                int[][] result = new int[endRow - startRow][b[0].length];
                System.arraycopy(upperResult, 0, result, 0, upperResult.length);
                System.arraycopy(lowerResult, 0, result, upperResult.length, lowerResult.length);
                
                return result;
            }
        }
        
        private int[][] computeDirectly() {
            int[][] result = new int[endRow - startRow][b[0].length];
            
            for (int i = startRow; i < endRow; i++) {
                for (int j = 0; j < b[0].length; j++) {
                    for (int k = 0; k < a[0].length; k++) {
                        result[i - startRow][j] += a[i][k] * b[k][j];
                    }
                }
            }
            
            return result;
        }
    }
    
    public void demonstrateForkJoin() {
        System.out.println("=== Fork-Join Framework Demo ===");
        
        // Test parallel sum
        int[] array = new int[100_000];
        for (int i = 0; i < array.length; i++) {
            array[i] = i + 1;
        }
        
        ForkJoinPool pool = new ForkJoinPool();
        
        // Sequential sum for comparison
        long sequentialStart = System.nanoTime();
        long sequentialSum = 0;
        for (int value : array) {
            sequentialSum += value;
        }
        long sequentialTime = System.nanoTime() - sequentialStart;
        
        // Parallel sum using fork-join
        long parallelStart = System.nanoTime();
        SumTask task = new SumTask(array, 0, array.length);
        Long parallelSum = pool.invoke(task);
        long parallelTime = System.nanoTime() - parallelStart;
        
        System.out.printf("Sequential sum: %d (%.2f ms)%n", 
                        sequentialSum, sequentialTime / 1_000_000.0);
        System.out.printf("Parallel sum: %d (%.2f ms)%n", 
                        parallelSum, parallelTime / 1_000_000.0);
        System.out.printf("Speedup: %.2fx%n", 
                        (double) sequentialTime / parallelTime);
        
        pool.shutdown();
    }
    
    public void demonstrateParallelMergeSort() {
        System.out.println("\n=== Parallel Merge Sort Demo ===");
        
        int[] array = new int[50_000];
        for (int i = 0; i < array.length; i++) {
            array[i] = (int) (Math.random() * 100_000);
        }
        
        // Copy for sequential sort
        int[] sequentialArray = array.clone();
        int[] parallelArray = array.clone();
        
        ForkJoinPool pool = new ForkJoinPool();
        
        // Sequential sort
        long sequentialStart = System.nanoTime();
        Arrays.sort(sequentialArray);
        long sequentialTime = System.nanoTime() - sequentialStart;
        
        // Parallel sort
        long parallelStart = System.nanoTime();
        int[] temp = new int[parallelArray.length];
        MergeSortTask sortTask = new MergeSortTask(parallelArray, temp, 0, parallelArray.length);
        pool.invoke(sortTask);
        long parallelTime = System.nanoTime() - parallelStart;
        
        // Verify correctness
        boolean correct = Arrays.equals(sequentialArray, parallelArray);
        
        System.out.printf("Sequential sort: %.2f ms%n", sequentialTime / 1_000_000.0);
        System.out.printf("Parallel sort: %.2f ms%n", parallelTime / 1_000_000.0);
        System.out.printf("Speedup: %.2fx%n", (double) sequentialTime / parallelTime);
        System.out.printf("Correct: %s%n", correct);
        
        pool.shutdown();
    }
    
    public void demonstrateMatrixMultiplication() {
        System.out.println("\n=== Parallel Matrix Multiplication ===");
        
        int size = 500;
        int[][] matrixA = generateRandomMatrix(size, size);
        int[][] matrixB = generateRandomMatrix(size, size);
        
        ForkJoinPool pool = new ForkJoinPool();
        
        long start = System.nanoTime();
        MatrixMultiplyTask task = new MatrixMultiplyTask(matrixA, matrixB, 0, size);
        int[][] result = pool.invoke(task);
        long end = System.nanoTime();
        
        System.out.printf("Matrix multiplication (%dx%d): %.2f ms%n", 
                        size, size, (end - start) / 1_000_000.0);
        System.out.printf("Result matrix size: %dx%d%n", 
                        result.length, result[0].length);
        
        pool.shutdown();
    }
    
    private int[][] generateRandomMatrix(int rows, int cols) {
        int[][] matrix = new int[rows][cols];
        for (int i = 0; i < rows; i++) {
            for (int j = 0; j < cols; j++) {
                matrix[i][j] = (int) (Math.random() * 100);
            }
        }
        return matrix;
    }
}
```

---

## CompletableFuture and Async Programming

### Theory

**CompletableFuture** provides a powerful API for asynchronous programming, allowing composition of async operations without blocking threads.

**Key Features:**
- **Non-blocking**: Operations don't block calling thread
- **Composable**: Chain operations with thenApply, thenCompose, etc.
- **Exception handling**: Handle errors with exceptionally, handle
- **Combination**: Combine multiple futures with allOf, anyOf
- **Custom executors**: Control which thread pool executes operations

### Code Examples

#### Complete CompletableFuture Implementation
```java
import java.util.concurrent.*;
import java.util.List;
import java.util.ArrayList;
import java.util.stream.Collectors;
import java.util.function.Supplier;

public class CompletableFutureAdvanced {
    
    // Simulated services
    static class UserService {
        public static CompletableFuture<User> getUserById(String id) {
            return CompletableFuture.supplyAsync(() -> {
                simulateDelay(100);
                return new User(id, "User" + id, "user" + id + "@example.com");
            });
        }
    }
    
    static class OrderService {
        public static CompletableFuture<List<Order>> getOrdersByUserId(String userId) {
            return CompletableFuture.supplyAsync(() -> {
                simulateDelay(200);
                List<Order> orders = new ArrayList<>();
                for (int i = 1; i <= 3; i++) {
                    orders.add(new Order("order" + userId + i, userId, 100.0 * i));
                }
                return orders;
            });
        }
    }
    
    static class PaymentService {
        public static CompletableFuture<Payment> getPaymentByOrderId(String orderId) {
            return CompletableFuture.supplyAsync(() -> {
                simulateDelay(150);
                return new Payment("pay" + orderId, orderId, "COMPLETED");
            });
        }
    }
    
    // Data classes
    static class User {
        final String id, name, email;
        User(String id, String name, String email) {
            this.id = id; this.name = name; this.email = email;
        }
        @Override
        public String toString() {
            return String.format("User{id='%s', name='%s', email='%s'}", id, name, email);
        }
    }
    
    static class Order {
        final String id, userId;
        final double amount;
        Order(String id, String userId, double amount) {
            this.id = id; this.userId = userId; this.amount = amount;
        }
        @Override
        public String toString() {
            return String.format("Order{id='%s', userId='%s', amount=%.2f}", id, userId, amount);
        }
    }
    
    static class Payment {
        final String id, orderId, status;
        Payment(String id, String orderId, String status) {
            this.id = id; this.orderId = orderId; this.status = status;
        }
        @Override
        public String toString() {
            return String.format("Payment{id='%s', orderId='%s', status='%s'}", id, orderId, status);
        }
    }
    
    static class UserOrderSummary {
        final User user;
        final List<Order> orders;
        final List<Payment> payments;
        final double totalAmount;
        
        UserOrderSummary(User user, List<Order> orders, List<Payment> payments) {
            this.user = user;
            this.orders = orders;
            this.payments = payments;
            this.totalAmount = orders.stream().mapToDouble(o -> o.amount).sum();
        }
        
        @Override
        public String toString() {
            return String.format("UserOrderSummary{user=%s, orders=%d, payments=%d, total=%.2f}", 
                               user.name, orders.size(), payments.size(), totalAmount);
        }
    }
    
    private static void simulateDelay(int millis) {
        try {
            Thread.sleep(millis);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }
    
    public void demonstrateBasicComposition() {
        System.out.println("=== Basic CompletableFuture Composition ===");
        
        String userId = "123";
        
        CompletableFuture<UserOrderSummary> summaryFuture = UserService.getUserById(userId)
            .thenCompose(user -> 
                OrderService.getOrdersByUserId(user.id)
                    .thenCompose(orders -> {
                        // Get payments for all orders
                        List<CompletableFuture<Payment>> paymentFutures = orders.stream()
                            .map(order -> PaymentService.getPaymentByOrderId(order.id))
                            .collect(Collectors.toList());
                        
                        // Combine all payment futures
                        CompletableFuture<List<Payment>> allPayments = CompletableFuture.allOf(
                            paymentFutures.toArray(new CompletableFuture[0])
                        ).thenApply(v -> 
                            paymentFutures.stream()
                                .map(CompletableFuture::join)
                                .collect(Collectors.toList())
                        );
                        
                        // Combine user, orders, and payments
                        return allPayments.thenApply(payments -> 
                            new UserOrderSummary(user, orders, payments));
                    })
            );
        
        try {
            UserOrderSummary summary = summaryFuture.get(5, TimeUnit.SECONDS);
            System.out.println("Summary: " + summary);
        } catch (InterruptedException | ExecutionException | TimeoutException e) {
            System.err.println("Failed to get summary: " + e.getMessage());
        }
    }
    
    public void demonstrateExceptionHandling() {
        System.out.println("\n=== Exception Handling ===");
        
        CompletableFuture<String> future = CompletableFuture
            .supplyAsync(() -> {
                if (Math.random() > 0.5) {
                    throw new RuntimeException("Random failure!");
                }
                return "Success!";
            })
            .exceptionally(throwable -> {
                System.out.println("Handling exception: " + throwable.getMessage());
                return "Recovered from failure";
            })
            .thenApply(result -> {
                System.out.println("Processing: " + result);
                return result.toUpperCase();
            });
        
        try {
            String result = future.get();
            System.out.println("Final result: " + result);
        } catch (InterruptedException | ExecutionException e) {
            System.err.println("Unexpected error: " + e.getMessage());
        }
    }
    
    public void demonstrateTimeoutAndCancellation() {
        System.out.println("\n=== Timeout and Cancellation ===");
        
        CompletableFuture<String> slowTask = CompletableFuture.supplyAsync(() -> {
            try {
                Thread.sleep(3000);
                return "Slow task completed";
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                return "Task interrupted";
            }
        });
        
        // Simulate timeout behavior
        CompletableFuture<String> timeoutFuture = slowTask
            .applyToEither(
                CompletableFuture.supplyAsync(() -> {
                    try {
                        Thread.sleep(1000);
                        throw new RuntimeException(new TimeoutException("Task timed out"));
                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                        throw new RuntimeException(e);
                    }
                }),
                result -> result
            )
            .exceptionally(throwable -> {
                if (throwable.getCause() instanceof TimeoutException) {
                    return "Task timed out";
                }
                return "Task failed: " + throwable.getMessage();
            });
        
        try {
            String result = timeoutFuture.get();
            System.out.println("Result: " + result);
        } catch (InterruptedException | ExecutionException e) {
            System.err.println("Error: " + e.getMessage());
        }
        
        // Manual cancellation example
        CompletableFuture<String> cancellableTask = CompletableFuture.supplyAsync(() -> {
            for (int i = 0; i < 100; i++) {
                if (Thread.currentThread().isInterrupted()) {
                    return "Task was cancelled";
                }
                try {
                    Thread.sleep(50);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    return "Task interrupted during sleep";
                }
            }
            return "Task completed normally";
        });
        
        // Cancel after 200ms
        ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(1);
        scheduler.schedule(() -> cancellableTask.cancel(true), 200, TimeUnit.MILLISECONDS);
        
        try {
            String result = cancellableTask.get();
            System.out.println("Cancellable task result: " + result);
        } catch (CancellationException e) {
            System.out.println("Task was cancelled");
        } catch (InterruptedException | ExecutionException e) {
            System.err.println("Error: " + e.getMessage());
        }
        
        scheduler.shutdown();
    }
    
    public void demonstrateParallelProcessing() {
        System.out.println("\n=== Parallel Processing ===");
        
        List<String> userIds = List.of("1", "2", "3", "4", "5");
        
        long start = System.currentTimeMillis();
        
        // Process all users in parallel
        List<CompletableFuture<UserOrderSummary>> futures = userIds.stream()
            .map(userId -> 
                UserService.getUserById(userId)
                    .thenCombine(
                        OrderService.getOrdersByUserId(userId),
                        (user, orders) -> {
                            // For simplicity, create dummy payments
                            List<Payment> payments = orders.stream()
                                .map(order -> new Payment("pay" + order.id, order.id, "COMPLETED"))
                                .collect(Collectors.toList());
                            return new UserOrderSummary(user, orders, payments);
                        }
                    )
            )
            .collect(Collectors.toList());
        
        // Wait for all to complete
        CompletableFuture<List<UserOrderSummary>> allSummaries = CompletableFuture.allOf(
            futures.toArray(new CompletableFuture[0])
        ).thenApply(v -> 
            futures.stream()
                .map(CompletableFuture::join)
                .collect(Collectors.toList())
        );
        
        try {
            List<UserOrderSummary> summaries = allSummaries.get();
            long end = System.currentTimeMillis();
            
            System.out.printf("Processed %d users in %d ms%n", summaries.size(), end - start);
            summaries.forEach(System.out::println);
            
            double totalAmount = summaries.stream()
                .mapToDouble(s -> s.totalAmount)
                .sum();
            System.out.printf("Total amount across all users: %.2f%n", totalAmount);
            
        } catch (InterruptedException | ExecutionException e) {
            System.err.println("Failed to process users: " + e.getMessage());
        }
    }
    
    public void demonstrateCustomExecutor() {
        System.out.println("\n=== Custom Executor ===");
        
        // Create custom executor for I/O operations
        ExecutorService ioExecutor = Executors.newFixedThreadPool(10, r -> {
            Thread t = new Thread(r, "IO-Thread");
            t.setDaemon(true);
            return t;
        });
        
        // Create custom executor for CPU-intensive operations
        ExecutorService cpuExecutor = Executors.newFixedThreadPool(
            Runtime.getRuntime().availableProcessors(), r -> {
                Thread t = new Thread(r, "CPU-Thread");
                t.setDaemon(true);
                return t;
            });
        
        CompletableFuture<String> result = CompletableFuture
            .supplyAsync(() -> {
                System.out.println("I/O operation on: " + Thread.currentThread().getName());
                simulateDelay(100);
                return "Data from database";
            }, ioExecutor)
            .thenApplyAsync(data -> {
                System.out.println("CPU operation on: " + Thread.currentThread().getName());
                // Simulate CPU-intensive processing
                return data.toUpperCase() + " PROCESSED";
            }, cpuExecutor)
            .thenApplyAsync(processedData -> {
                System.out.println("Final I/O on: " + Thread.currentThread().getName());
                simulateDelay(50);
                return "Saved: " + processedData;
            }, ioExecutor);
        
        try {
            String finalResult = result.get();
            System.out.println("Final result: " + finalResult);
        } catch (InterruptedException | ExecutionException e) {
            System.err.println("Error: " + e.getMessage());
        }
        
        ioExecutor.shutdown();
        cpuExecutor.shutdown();
    }
}
```

---

## Practice Questions

### Conceptual Questions
1. **What are the advantages of Fork-Join framework over traditional thread pools for recursive algorithms?**
2. **How does CompletableFuture improve upon traditional Future interface for async programming?**
3. **When should you use RecursiveTask vs RecursiveAction in Fork-Join framework?**
4. **What is work-stealing and how does it improve performance in Fork-Join pools?**
5. **How do you handle exceptions in CompletableFuture chains?**

### Coding Challenges
1. **Implement a parallel quicksort using Fork-Join framework**
2. **Create a CompletableFuture-based HTTP client with retry logic**
3. **Build a parallel file processing system using Fork-Join**
4. **Design an async cache using CompletableFuture**
5. **Implement parallel image processing using Fork-Join**

### Follow-up Questions
1. **How do you tune the threshold value in Fork-Join tasks for optimal performance?**
2. **What strategies can you use to prevent CompletableFuture chains from blocking threads?**
3. **How do you handle timeouts in CompletableFuture without Java 9+ orTimeout method?**
4. **What are the memory implications of Fork-Join's work-stealing queues?**
5. **How do you choose between different CompletableFuture composition methods (thenApply vs thenCompose)?**

---

## Key Takeaways

### Fork-Join Framework
- **Best suited** for recursive divide-and-conquer algorithms
- **Work-stealing** provides automatic load balancing across threads
- **Threshold tuning** is crucial for performance optimization
- **Exception handling** requires careful consideration in recursive tasks
- **ForkJoinPool** can be shared across multiple fork-join tasks

### CompletableFuture
- **Powerful composition** of asynchronous operations without blocking
- **Exception handling** can be done at any stage in the chain
- **Custom executors** provide fine-grained control over execution
- **Parallel processing** is easy with allOf() and anyOf() methods
- **Non-blocking** design improves resource utilization

### Best Practices
- Choose Fork-Join for CPU-intensive recursive algorithms
- Use CompletableFuture for I/O-bound async operations
- Always handle exceptions appropriately in async chains
- Consider using custom executors for different types of work
- Test performance with realistic workloads and data sizes
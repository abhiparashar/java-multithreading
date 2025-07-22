package intermediate.intermedidiate_prac;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.*;

public class CallableFutureExample {
    private final ExecutorService executor = Executors.newFixedThreadPool(4);
    public static void main(String[] args) throws ExecutionException, InterruptedException, TimeoutException {
        CallableFutureExample callableFutureExample = new CallableFutureExample();
        callableFutureExample.basicCallableExample();
        callableFutureExample.multipleCallablesExample();
    }

    public void basicCallableExample() throws InterruptedException, ExecutionException, TimeoutException {
        System.out.println("=== Basic Callable and Future Example ===");

        // Create a task that computes and returns a value
        Callable<Integer>computation = ()->{
            System.out.println("🧮 Starting expensive computation on " +
                    Thread.currentThread().getName());
            // Simulate expensive work
            Thread.sleep(3000);
            // Return computed result
            int result = 42 * 1337;
            System.out.println("✅ Computation completed: " + result);
            return result;
        };

        // Submit task and get a Future (like a receipt)
        Future<Integer> future = executor.submit(computation);

        // You can do other work while computation runs
        for (int i = 0; i < 5; i++) {
            System.out.println("   Other work step " + i);
            try{
                Thread.sleep(1000);
            }catch (InterruptedException e){
                Thread.currentThread().interrupt();
            }
        }

        try{
            if(!future.isDone()){
                System.out.println("⏳ Computation still in progress...");
            }
            // Get result (blocking - will wait if needed)
            System.out.println("🎯 Getting result...");
            Integer result = future.get(5, TimeUnit.SECONDS);
            System.out.println("🎉 Final result: " + result);
        }catch (InterruptedException  e){
            Thread.currentThread().interrupt();
        }catch (ExecutionException e){
            System.err.println("❌ Computation failed: " + e.getCause().getMessage());
        }catch (TimeoutException e){
            System.err.println("⏰ Computation took too long, cancelling...");
            future.cancel(true);
        }
        executor.shutdown();
    }

    private void multipleCallablesExample(){
        System.out.println("\n=== Multiple Callables Example ===");
        List<Callable<String>>tasks  = new ArrayList<>();
        // Create multiple computational tasks with different behaviors
        for (int i = 0; i < 5; i++) {
            final int taskId = i;
            tasks.add(()->{
                System.out.println("📊 Task " + taskId + " starting on " +
                        Thread.currentThread().getName());

                // Different execution times
                int workTime = 1000 + (taskId * 500);
                Thread.sleep(workTime);
                // Task 3 will fail to demonstrate error handling
                if(taskId==3){
                    throw new RuntimeException("Task " + taskId + " encountered an error!");
                }
                String result = "Task " + taskId + " completed successfully in " + workTime + "ms";
                System.out.println("✅ " + result);
                return result;
            });
        }

        System.out.println("📋 Submitting " + tasks.size() + " tasks...");
        // Submit all tasks and collect Futures
        List<Future<String>> futures = new ArrayList<>();
        for (Callable<String> task : tasks) {
            futures.add(executor.submit(task));
        }

        // System.out.println("⏳ Waiting for results...");
        // Process results as they become available
        for (int i = 0; i < futures.size(); i++) {
            Future<String> future = futures.get(i);
            try{
                String result = future.get();
                System.out.println("🎯 Success: " + result);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            } catch (ExecutionException e) {
                System.err.println("❌ Task " + i + " failed: " + e.getCause().getMessage());
            }
        }

        executor.shutdown();
    }
}

package intermediate.intermedidiate_prac;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.*;

public class AdvancedTaskManagement {
    private final ExecutorService executor = Executors.newFixedThreadPool(4);
    public static void main(String[] args) throws InterruptedException, ExecutionException, TimeoutException {
        AdvancedTaskManagement atm = new AdvancedTaskManagement();
        atm.submitMultipleTasksWithResults();
        atm.invokeAllExample();
        atm.invokeAnyExample();
    }

    public void submitMultipleTasksWithResults() throws ExecutionException, InterruptedException, TimeoutException {
        System.out.println("=== Submitting Multiple Tasks and Getting Results ===");
        List<Future<String>>futures = new ArrayList<>();
        for (int i = 1; i <= 5; i++) {
            final int taskId = i;
            // submit() returns a Future - think "receipt for future result"
            Future<String>future = executor.submit(()->{
                int workTime = 1000 + (taskId * 500);
                Thread.sleep(workTime);
                return "Task " + taskId + " completed after " + workTime + "ms";
            });
            futures.add(future);
            System.out.println("Task " + taskId + " submitted (will take " +
                    (1000 + taskId * 500) + "ms)");
        }

        System.out.println("\nWaiting for results...");

        for (int i = 0; i < futures.size(); i++) {
            Future<String>future = futures.get(i);
            try{
                String result = future.get(3, TimeUnit.SECONDS);
                System.out.println("✓ " + result);
            }finally {
                System.out.println("✗ Task " + i + " timed out");
                future.cancel(true);
            }
        }
    }

    public void invokeAllExample() throws InterruptedException, ExecutionException {
        System.out.println("\n=== Batch Processing - Wait for All ===");

        List<Callable<String>>callables = new ArrayList<>();
        for (int i = 1; i <=3; i++) {
            final int taskId = i;
            callables.add(()->{
                Thread.sleep(1000);
                return "Batch_id"+taskId+"completed";
            });
        }
        System.out.println("Submitting 3 tasks in batch...");
        List<Future<String>>results = executor.invokeAll(callables);

        System.out.println("All tasks completed! Processing results:");

        for (int i = 0; i < results.size(); i++) {
            try{
                String result = results.get(i).get();
                System.out.println("✓ " + result);
            } catch (ExecutionException e) {
                throw new RuntimeException(e);
            }
        }
        executor.shutdown();
    }

    public void invokeAnyExample() throws ExecutionException, InterruptedException, TimeoutException {
        System.out.println("\n=== Race Condition - First One Wins ===");
        List<Callable<String>>tasks = new ArrayList<>();
        tasks.add(()->{
            Thread.sleep(2000);
            return "Slow task completed (2 seconds)";
        });

        tasks.add(()->{
            Thread.sleep(500);
            return "Fast task completed (0.5 seconds)";
        });

        tasks.add(()->{
            Thread.sleep(1000);
            return "Medium task completed (1 second)";
        });

        System.out.println("Starting 3 tasks - fastest wins!");

        String result = executor.invokeAny(tasks,20, TimeUnit.SECONDS);

        System.out.println("🏆 Winner: " + result);

        executor.shutdown();
    }

}

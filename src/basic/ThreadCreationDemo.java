package basic;

import java.util.concurrent.*;

// Method 1: Extending Thread class
class CustomThread extends Thread {
   private final String taskName;

    CustomThread(String taskName) {
        this.taskName = taskName;
    }

   @Override
   public void run(){
           try{
               for (int i = 0; i < 5; i++) {
                   System.out.println(taskName + " executing step " + i +
                           " on thread: " + Thread.currentThread().getName());
                   Thread.sleep(1000);
               }
           }catch (InterruptedException e ){
               System.out.println(taskName + " was interrupted");
               Thread.currentThread().interrupt();
           }
   }
}

// Method 2: Using Runnable
class RunnableTask implements Runnable{
    private final String taskName;

    RunnableTask(String taskName) {
        this.taskName = taskName;
    }

    @Override
    public void run() {
        try{
            for (int i = 0; i < 5; i++) {
                System.out.println(taskName + " executing step " + i +
                        " on thread: " + Thread.currentThread().getName());
                Thread.sleep(800);
            }
        }catch(InterruptedException e){
            System.out.println(taskName + " was interrupted");
            Thread.currentThread().interrupt();
        }
    }
}

// Method 3: Implementing Callable interface (returns result)
class CallableTask implements Callable<String> {
    private final String taskName;
    private final int workAmount;

    CallableTask(String taskName, int workAmount) {
        this.taskName = taskName;
        this.workAmount = workAmount;
    }

    @Override
    public String call() throws Exception {
            int sum = 0;
            for (int i = 1; i <= workAmount; i++) {
               sum = sum+i;
               Thread.sleep(1000);
            }
            return taskName + " completed with result: " + sum;
    }
}

public class ThreadCreationDemo {
    public static void main(String[] args) throws ExecutionException, InterruptedException {
        System.out.println("=== Thread Creation Methods Demo ===\n");

        // Method 1: Using Thread class
        CustomThread thread1 = new CustomThread("CustomThread-Task");
        thread1.start();

        // Method 2: Using Runnable
        Thread thread2 = new Thread(new RunnableTask("Runnable-Task"), "RunnableWorker");
        thread2.start();

        // Method 3: Using Callable with ExecutorService
        ExecutorService executor = Executors.newSingleThreadExecutor();
        Future<String>future = executor.submit(new CallableTask("Callable-Task",10));
        String result = future.get();
        System.out.println("Callable result: " + result);
        executor.shutdown();

        // Wait for all threads to complete
        thread1.join();
        thread2.join();

        System.out.println("All threads completed!");
    }
}

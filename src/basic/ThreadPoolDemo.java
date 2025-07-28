package basic;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.*;

class FixedThreadPoolTest {
    private int numberThreads = 0;

    FixedThreadPoolTest(int numberThreads) {
        this.numberThreads = numberThreads;
    }

    public void runTasks() throws ExecutionException, InterruptedException {
        try{
            ExecutorService executor = Executors.newFixedThreadPool(numberThreads);

            //Creating a task
            Callable<String>callable = () -> {
                try{
                    for (int i = 0; i < 8; i++) {
                        System.out.println("Task step " + i + " on thread: " + Thread.currentThread().getName());
                        Thread.sleep(1000);
                    }
                }catch (InterruptedException e){
                    Thread.currentThread().interrupt();
                    return "Task was interrupted";
                }
                return "Task completed successfully";
            };

            List<Future<String>>futures = new ArrayList<>();
            for (int i = 0; i < 5; i++) {
               futures.add(executor.submit(callable));
            }

            for (int i = 0; i < futures.size(); i++) {
                String result = futures.get(i).get();
                System.out.println("result"+ result);
            }
            executor.shutdown();
        }catch (InterruptedException e){
            Thread.currentThread().interrupt();
        }
    }
}

class CachedThreadPoolTest{
    public void runTasks() throws ExecutionException, InterruptedException {
        ExecutorService executor = Executors.newCachedThreadPool();
        Callable<String>callable = ()->{
            try{
                for (int i = 0; i < 5; i++) {
                    System.out.println("Task step " + i + " on thread: " + Thread.currentThread().getName());
                    Thread.sleep(1000);
                }
            }catch (InterruptedException e){
                Thread.currentThread().interrupt();
                return "Task interrupted ";
            }
            return "Task completed successfully";
        };
        List<Future<String>>futures = new ArrayList<>();
        for (int i = 0; i < 5; i++) {
            Future<String> future = executor.submit(callable);
            futures.add(future);
        }
        //get result
        for (int i = 0; i < 5; i++) {
            String result = futures.get(i).get();
            System.out.println("Task " + (i+1) + " result: " + result);
        }
        executor.shutdown();
    }
}
public class ThreadPoolDemo {
    public static void main(String[] args) throws ExecutionException, InterruptedException {
//       FixedThreadPoolTest fixedThreadPool = new FixedThreadPoolTest(3);
//       fixedThreadPool.runTasks();
       CachedThreadPoolTest cachedThreadPool = new CachedThreadPoolTest();
       cachedThreadPool.runTasks();
    }
}

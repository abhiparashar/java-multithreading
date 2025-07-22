package intermediate.intermedidiate_prac;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

public class ExecutorServiceBasics {
    public static void main(String[] args) {
        // Create different types of thread pools
        ExecutorService fixedPool = Executors.newFixedThreadPool(4);
        ExecutorService cachedPool  = Executors.newCachedThreadPool();
        ExecutorService singlePool = Executors.newSingleThreadExecutor();

        try{
            for (int i = 1; i < 10; i++) {
                final int taskId = i;
                fixedPool.submit(()->{
                    try {
                        System.out.println("Task " + taskId + " executed by " +
                                Thread.currentThread().getName());
                        Thread.sleep(1000);
                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                    }
                });
            }
        }finally {
            shutdownExecutor(fixedPool);
            shutdownExecutor(cachedPool);
            shutdownExecutor(singlePool);
        }
    }

    public static void shutdownExecutor(ExecutorService  executorService){
        executorService.shutdown();
        try{
            if(!executorService.awaitTermination(30, TimeUnit.SECONDS)){
                executorService.shutdown();
            }
        }catch (InterruptedException e){
            executorService.shutdown();
            Thread.currentThread().interrupt();
        }
    }
}

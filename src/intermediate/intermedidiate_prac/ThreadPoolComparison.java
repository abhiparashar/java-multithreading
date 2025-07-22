package intermediate.intermedidiate_prac;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class ThreadPoolComparison {
    public static void main(String[] args) throws InterruptedException {
        System.out.println("Comparing different thread pool types");
        // Test each type with 6 tasks to see the difference
        testFixedthreadPool();
        testCacheTheadPool();
        testSingleTheradPool();
    }

    private static void testFixedthreadPool() throws InterruptedException {
        ExecutorService fixedPool = Executors.newFixedThreadPool(2);
        runTaskBatch(fixedPool,"Fixed");
        System.out.println("   → Notice: Only 2 threads used, others wait\n");
    }

    private static void testCacheTheadPool() throws InterruptedException {
        ExecutorService executorService = Executors.newCachedThreadPool();
        runTaskBatch(executorService, "Cached");
        System.out.println("   → Notice: Creates new threads for each task (up to limit)\n");
    }

    private static void testSingleTheradPool() throws InterruptedException {
        ExecutorService executorService = Executors.newSingleThreadExecutor();
        runTaskBatch(executorService,"Single");
        System.out.println("   → Notice: Only 1 thread, tasks run one by one\n");
    }

    private static void runTaskBatch(ExecutorService executorService, String type) throws InterruptedException {
        long startTime = System.currentTimeMillis();
        CountDownLatch latch = new CountDownLatch(6);
            for (int i = 0; i < 6; i++) {
                final int taskId = i;
                executorService.submit(()->{
                    System.out.println("   " + type + " - Task " + taskId +
                            " running on " + Thread.currentThread().getName());
                    try {
                        Thread.sleep(500);
                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                    }finally {
                        latch.countDown();
                    }
                });
            }
            latch.await();
            long endTime = System.currentTimeMillis();
        System.out.println("   " + type + " execution time: " +
                (endTime - startTime) + "ms");
        executorService.shutdown();
    }

}

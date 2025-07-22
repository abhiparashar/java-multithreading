package intermediate.intermedidiate_prac;

import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;

class CustomThreadFactory implements ThreadFactory {
    private final String namePrefix;
    private final AtomicInteger threadNumber = new AtomicInteger(0);
    CustomThreadFactory(String namePrefix) {
        this.namePrefix = namePrefix;
    }

    @Override
    public Thread newThread(Runnable r) {
        Thread thread = new Thread(r, namePrefix+"-worker-"+threadNumber.getAndIncrement());

        // Configure thread properties
        thread.setDaemon(false);
        thread.setPriority(Thread.NORM_PRIORITY);

        // Handle uncaught exceptions
        thread.setUncaughtExceptionHandler((t,e)->{
            System.err.println("💥 Thread " + t.getName() +
                    " had uncaught exception: " + e.getMessage());
        });
        System.out.println("👷 Created new thread: " + thread.getName());
        return thread;
    }
}

class CustomRejectionHandler implements RejectedExecutionHandler{

    @Override
    public void rejectedExecution(Runnable r, ThreadPoolExecutor executor) {
        System.err.println("🚫 Task rejected! Pool is full");
        System.err.println("   - Pool size: " + executor.getPoolSize());
        System.err.println("   - Queue size: " + executor.getQueue().size());

        // Strategy 1: Try to add to queue with timeout
        if(!executor.isShutdown()){
            try{
                boolean added = executor.getQueue().offer(r,1,TimeUnit.SECONDS);
                if(added){
                    System.out.println("✅ Task queued after rejection");
                }else {
                    System.err.println("❌ Task completely rejected");
                    // Could: log to file, send to dead letter queue, etc.
                }
            }catch (InterruptedException e){
                Thread.currentThread().interrupt();
            }
        }
    }
}
public class CustomThreadPoolExecutor {
    public static void main(String[] args) throws InterruptedException {
        CustomThreadPoolExecutor customThreadPoolExecutor = new CustomThreadPoolExecutor();
        customThreadPoolExecutor.demonstrateCustomPool();
    }

    public void demonstrateCustomPool() throws InterruptedException {
        System.out.println("=== Creating Custom Thread Pool ===");
        ThreadPoolExecutor customPool = new ThreadPoolExecutor(
                2,
                4,
                60L,
                TimeUnit.SECONDS,
                new LinkedBlockingQueue<>(10),
                new CustomThreadFactory("MyApp"),
                new CustomRejectionHandler()
        );

        System.out.println("Custom pool created with:");
        System.out.println("- Core threads: 2 (always alive)");
        System.out.println("- Max threads: 4 (when busy)");
        System.out.println("- Queue size: 10 (waiting tasks)");
        System.out.println("- Keep alive: 60 seconds\n");

        // Test the pool with different loads
        testLightLoad(customPool);
        testHeavyLoad(customPool);

        customPool.shutdown();
    }

    private void testLightLoad(ThreadPoolExecutor pool) throws InterruptedException {
        System.out.println("🟢 Testing Light Load (3 tasks):");

        for (int i = 0; i < 3; i++) {
            final int taskId = i;
            pool.submit(()->{
                System.out.println("Light task " + taskId + " on " +
                        Thread.currentThread().getName());
                  try{
                      Thread.sleep(1000);
                  } catch (Exception e) {
                      Thread.currentThread().interrupt();
                  }
              });
        }

        // Wait and check pool state
        try{
            Thread.sleep(2000);
        }catch (InterruptedException e){
            Thread.currentThread().interrupt();
        }
        printPoolStats(pool);
        System.out.println("→ Only core threads used for light load\n");
    }

    private void testHeavyLoad(ThreadPoolExecutor pool) {
        System.out.println("🔴 Testing Heavy Load (15 tasks):");

        for (int i = 0; i < 15; i++) {
            final int taskId = i;
            pool.submit(()->{
                System.out.println("Heavy task " + taskId + " on " +
                        Thread.currentThread().getName());
                try {
                    Thread.sleep(500);
                } catch (InterruptedException e) {
                   Thread.currentThread().interrupt();
                }
            });
        }

        //wait
        try{
            Thread.sleep(2000);
        }catch (InterruptedException e){
            Thread.currentThread().interrupt();
        }
        printPoolStats(pool);
        System.out.println("→ Extra threads created + queue used + some rejected\n");
    }

    private void printPoolStats(ThreadPoolExecutor pool){
        System.out.println("   Pool Stats:");
        System.out.println("   - Active threads: "+pool.getActiveCount());
        System.out.println("   - Pool Size: "+pool.getPoolSize());
        System.out.println("   - Queue size: "+pool.getQueue().size());
    }
}

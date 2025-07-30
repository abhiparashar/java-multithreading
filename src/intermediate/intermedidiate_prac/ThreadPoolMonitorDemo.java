package intermediate.intermedidiate_prac;

import java.util.concurrent.*;

class ThreadPoolMonitor{
    private final ThreadPoolExecutor executor;
    private final ScheduledThreadPoolExecutor monitor;

   public ThreadPoolMonitor() {
        this.executor = new ThreadPoolExecutor(2,10,60L, TimeUnit.SECONDS,new ArrayBlockingQueue<>(20));
        this.monitor = (ScheduledThreadPoolExecutor) Executors.newScheduledThreadPool(1);
        startMonitoring();
    }


    public void startMonitoring(){
        monitor.scheduleAtFixedRate(()->{
            System.out.println("=== Thread Pool Stats ===");
            System.out.println("Active threads: " + executor.getActiveCount());
            System.out.println("Core pool size: " + executor.getPoolSize());
            System.out.println("Pool size: " + executor.getPoolSize());
            System.out.println("Largest pool size: " + executor.getLargestPoolSize());
            System.out.println("Task count: " + executor.getTaskCount());
            System.out.println("Completed tasks: " + executor.getCompletedTaskCount());
            System.out.println("Queue size: " + executor.getQueue().size());
            System.out.println("Is shutdown: " + executor.isShutdown());
            System.out.println("Is terminated: " + executor.isTerminated());
        },0,5,TimeUnit.SECONDS);
    }

    public void submitTasks(Runnable task){
        executor.submit(task);
    }

    public void shutdown(){
        executor.shutdown();
        monitor.shutdown();
    }
}
public class ThreadPoolMonitorDemo {
    public static void main(String[] args) throws ExecutionException, InterruptedException {
        ThreadPoolMonitor threadPoolMonitor = new ThreadPoolMonitor();

        // Submit various tasks to see monitoring in action
        for (int i = 1; i <= 15; i++) {
            final int taskId = i;
            threadPoolMonitor.submitTasks(()->{
                try{
                    Thread.sleep(3000);
                }catch (InterruptedException e){
                    Thread.currentThread().interrupt();
                }
            });
            Thread.sleep(500);
        }

        // Let it run for a while to see stats
        Thread.sleep(30000);

        threadPoolMonitor.shutdown();
    }
}

package thread;

public class ThreadLifecycle {
    public static void main(String[] args) throws InterruptedException{
        Thread thread = new Thread(()->{
            System.out.println("Thread is running");
            try{
                Thread.sleep(1000);
            } catch (InterruptedException e) {
               Thread.currentThread().interrupt();
            }
            System.out.println("Thread finished");
        });

        System.out.println("State: " + thread.getState()); // NEW

        thread.start();
        System.out.println("State: "+ thread.getState()); //RUNNABLE

        Thread.sleep(100);
        System.out.println("State "+ thread.getState()); //TIMED_WAITING

        thread.join();
        System.out.println("State: " + thread.getState()); //TIMED_WAITING
    }
}

package basic;

public class DeadlockDemo {
    private final Object lock1 = new Object();
    private final Object lock2 = new Object();

    // Method that acquires locks in order: lock1 -> lock2
    public void method1(){
        synchronized (lock1){
            System.out.println(Thread.currentThread().getName() + " acquired lock1");
            try{
                Thread.sleep(100);
            }catch (InterruptedException e){
                Thread.currentThread().interrupt();
                return;
            }

            System.out.println(Thread.currentThread().getName() + " waiting for lock2");
            synchronized (lock2){
                System.out.println(Thread.currentThread().getName() + " acquired lock2");
            }
        }
    }

    // Method that acquires locks in reverse order: lock2 -> lock1 (DEADLOCK!)
    public void method2(){
        synchronized (lock2){
            System.out.println(Thread.currentThread().getName() + " acquired lock2");
            try{
                Thread.sleep(100);
            }catch (InterruptedException e){
                Thread.currentThread().interrupt();
                return;
            }

            System.out.println(Thread.currentThread().getName() + " waiting for lock1");
            synchronized (lock1){
                System.out.println(Thread.currentThread().getName() + " acquired lock1");
            }
        }
    }

    // Fixed version - consistent lock ordering
    public void method1Fixed(){
        acquireLocksInOrder("method1Fixed");
    }

    public void method2Fixed(){
        acquireLocksInOrder("method2Fixed");
    }

    private void acquireLocksInOrder(String methodName){
        synchronized (lock1){
            System.out.println(Thread.currentThread().getName() +
                    " (" + methodName + ") acquired lock1");
            try{
                Thread.sleep(50);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                return;
            }
            synchronized (lock2){
                System.out.println(Thread.currentThread().getName() +
                        " (" + methodName + ") acquired lock2");
                try{
                    Thread.sleep(50);
                }catch (InterruptedException e){
                    Thread.currentThread().interrupt();
                }
            }
        }
    }

    public static void demonstrateDeadlock(){
        System.out.println("=== Deadlock Demonstration ===");
        System.out.println("This will likely cause a deadlock (wait 10 seconds)...\n");

        DeadlockDemo deadlockDemo = new DeadlockDemo();
        Thread thread1 = new Thread(()->{
            for (int i = 0; i < 5; i++) {  // FIXED: Added loop
                deadlockDemo.method1();
                try{
                    Thread.sleep(200);
                }catch (InterruptedException e){
                    Thread.currentThread().interrupt();
                    break;  // FIXED: Added break to exit loop
                }
            }
        },"Thread-1");

        Thread thread2 = new Thread(()->{
            for (int i = 0; i < 5; i++) {  // FIXED: Added loop
                deadlockDemo.method2();  // FIXED: Actually call method2()!
                try{
                    Thread.sleep(200);
                }catch (InterruptedException e){
                    Thread.currentThread().interrupt();
                    break;  // FIXED: Added break to exit loop
                }
            }
        },"Thread-2");

        thread1.start();
        thread2.start();

        // Wait for a maximum of 10 seconds
        try{
            thread1.join(10000);
            thread2.join(10000);
        }catch (InterruptedException e){
            Thread.currentThread().interrupt();
        }

        if(thread1.isAlive()||thread2.isAlive()){
            System.out.println("\n*** DEADLOCK DETECTED! ***");
            System.out.println("Threads are still running after timeout.");

            // Force stop the threads (not recommended in production)
            thread1.interrupt();
            thread2.interrupt();
        }else {
            System.out.println("No deadlock occurred (lucky timing!)");
        }
    }

    public static void demonstrateDeadlockFix(){
        System.out.println("\n=== Deadlock Fix Demonstration ===");
        System.out.println("Using consistent lock ordering...\n");
        DeadlockDemo deadlockDemo = new DeadlockDemo();
        Thread thread1 = new Thread(()->{
            for (int i = 0; i < 3; i++) {
                deadlockDemo.method1Fixed();
            }
        },"FixedThread-1");

        Thread thread2 = new Thread(()->{
            for (int i = 0; i < 3; i++) {
                deadlockDemo.method2Fixed();
            }
        },"FixedThread-2");

        thread1.start();
        thread2.start();

        try{
            thread1.join();
            thread2.join();
            System.out.println("Both threads completed successfully - no deadlock!");
        }catch (InterruptedException e){
            Thread.currentThread().interrupt();
        }
    }

    public static void main(String[] args) {
        demonstrateDeadlock();
        demonstrateDeadlockFix();
    }
}
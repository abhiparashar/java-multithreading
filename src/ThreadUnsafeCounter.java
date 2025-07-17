public class ThreadUnsafeCounter {
    private int count = 0;

    public void incrementNumber(){
        count++;
    }

    public int getCount(){
      return count;
    }

    public static void main(String[] args) throws InterruptedException {
        ThreadUnsafeCounter counter = new ThreadUnsafeCounter();
        // 2 threads, each increments 1000 times
        Thread t1 = new Thread(()->{
            for (int i = 0; i < 1000; i++) {
                counter.incrementNumber();
            }
        });

        Thread t2 = new Thread(()->{
            for (int i = 0; i < 1000; i++) {
                counter.incrementNumber();
            }
        });

        t1.start();
        t2.start();

        t1.join();
        t2.join();

        System.out.println("Expected: 2000");
        System.out.println("Actual: " + counter.getCount());
    }
}

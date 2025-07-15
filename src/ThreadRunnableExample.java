class RunnableExample implements Runnable{

    @Override
    public void run() {
        for (int i = 0; i < 5; i++) {
            System.out.println("Thread " + Thread.currentThread().getId() + "is running" + i);
            try {
                Thread.sleep(1000);
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            }
        }
    }
}
public class ThreadRunnableExample {
    public static void main(String[] args) {
       RunnableExample runnableExample = new RunnableExample();
       Thread thread1 = new Thread(runnableExample);
       Thread thread2 = new Thread(runnableExample);
       thread1.start();
       thread2.start();
    }
}

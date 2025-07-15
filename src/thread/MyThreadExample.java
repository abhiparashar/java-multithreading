package thread;

public class MyThreadExample extends Thread {
    @Override
    public void run() {
            try {
                Thread.sleep(5000);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
    }

    public static void main(String[] args) throws InterruptedException {
        MyThreadExample t1 = new MyThreadExample();
        t1.start();
        t1.join();
        System.out.println("Hello Thread");
    }
}
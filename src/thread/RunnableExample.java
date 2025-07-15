package thread;

class MyRunnableThread implements Runnable{
    private final String taskName;

    public MyRunnableThread(String taskName){
        this.taskName = taskName;
    }

    @Override
    public void run() {
        for (int i = 1; i <=5; i++) {
            System.out.println(taskName+"_step"+i);
            try{
                Thread.sleep(1000);
            }catch (InterruptedException e){
                Thread.currentThread().interrupt();
            }
        }
    }
}

public class RunnableExample {
    public static void main(String[] args){
        Thread thread1 = new Thread(new MyRunnableThread("Download"));
        Thread thread2 = new Thread(new MyRunnableThread("Upload"));
        thread1.start();
        thread2.start();
    }
}

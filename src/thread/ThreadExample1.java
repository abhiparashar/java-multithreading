package thread;

//Method 1: Extending Thread Class
class MyThread extends Thread{
   private final String taskName;
   public MyThread(String taskName){
        this.taskName = taskName;
   }
   @Override
    public void run(){
       for (int i = 1; i <= 5; i++) {
            System.out.println(taskName + "-step" + i);
            try{
                Thread.sleep(1000);
            } catch (InterruptedException e) {
               Thread.currentThread().interrupt();
            }
       }
   }
}

public class ThreadExample1 {
    public static void main(String[] main){
        MyThread myThread1= new MyThread("Task-A");
        MyThread myThread2 = new MyThread("Task-B");
        myThread1.run(); //one at a time
        myThread2.run();
    }
}
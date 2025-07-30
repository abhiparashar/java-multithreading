package intermediate.intermedidiate_prac;

class CustomReadWriteLock{
    private int readers = 0;
    private  int writers = 0;
    private  int writeRequests = 0;
    private final Object lock = new Object();
    // Read lock acquisition

    public void lockRead() throws InterruptedException {
        synchronized (lock) {
            // Wait while there are writers or pending write requests
            while (writers > 0 || writeRequests > 0) {
                lock.wait();
            }
            readers++;
        }
    }

    public void unlockRead() {
        synchronized (lock) {
            readers--;
            if (readers == 0) {
                lock.notifyAll(); // Wake up waiting writers
            }
        }
    }

    // Write lock acquisition
    public void lockWrite(){
        synchronized (lock){
            while (readers>0){

            }
        }
    }
}
public class CustomReadWriteLockDemo_pending {
    public static void main(String[] args) {
        System.out.println("Hello world");
    }
}

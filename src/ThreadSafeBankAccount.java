public class ThreadSafeBankAccount {
    private  String accountNumber;
    private double balance;

    public ThreadSafeBankAccount(String accountNumber, double intitalBalance) {
        this.accountNumber = accountNumber;
        this.balance = intitalBalance;
    }


    // Synchronized to prevent concurrent access
    public synchronized boolean withDraw(double amount){
        System.out.println(Thread.currentThread().getName() + "Attempting to withdraw : " + amount);
        if(balance>=amount){
            // Simulate processing time
            try {
                Thread.sleep(100);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                return false;
            }
            balance = balance - amount;
            System.out.println(Thread.currentThread().getName() +
                    " withdrew $" + amount +
                    ", remaining balance: $" + balance);
            return true;
        }else {
            System.out.println(Thread.currentThread().getName()+"withdrew $"+ amount + ", remaining balance: $" + balance);
            return false;
        }
    }

    public synchronized void deposit(double amount){
        balance = balance + amount;
        System.out.println(Thread.currentThread().getName() +
                " deposited $" + amount +
                ", new balance: $" + balance);
    }

    public synchronized double getBalance(){
        return balance;
    }

    public static void main(String[] args) throws InterruptedException {
        ThreadSafeBankAccount threadSafeBankAccount = new ThreadSafeBankAccount("123-456-789", 1000.0);
        // Create multiple threads trying to withdraw
        Thread[] threads = new Thread[5];
        for (int i = 0; i < threads.length; i++) {
            threads[i] = new Thread(()->{
                threadSafeBankAccount.withDraw(300);
            });
        }

        // Start all threads
        for(Thread thread:threads){
            thread.start();
        }

        // Wait for all threads to complete
        for (Thread thread:threads){
            thread.join();
        }

        System.out.println("Final balance: $" + threadSafeBankAccount.getBalance());
    }
}

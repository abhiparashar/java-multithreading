package basic;

public class BankDemo {
    public static void main(String[] args) {
        AdvancedBankAccount account1 = new AdvancedBankAccount();
        AdvancedBankAccount account2 = new AdvancedBankAccount();

        // Multiple threads trying different operations
       Thread thread1 = new Thread(()->{
            account1.deposit(500);
            account1.deposit(600);
        });

       Thread thread2 = new Thread(()->{
            account1.tryDeposit(100);
            account1.tryWithdrawWithTimeout(50,2);
       });

       Thread thread3 = new Thread(()->{
           System.out.println("Account1 busy? " + account1.isAccountBusy());
           System.out.println("Waiting threads: " + account1.getWaitingThreads());
       });

        // Start all threads
        thread1.start();
        thread2.start();
        thread3.start();

        // Wait for completion
        try {
            thread1.join();
            thread2.join();
            thread3.join();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
        System.out.println("Final Account1 Balance: $" + account1.getBalance());
        System.out.println("Final Account2 Balance: $" + account2.getBalance());
    }
}

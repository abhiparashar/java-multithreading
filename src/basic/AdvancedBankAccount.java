package basic;

import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.ReentrantLock;

public class AdvancedBankAccount {
    private final ReentrantLock lock = new ReentrantLock();
    private double balance = 0.0;

    // Basic deposit with lock
    public void deposit(double amount){
        lock.lock();
        try{
            if(amount>0){
                balance = balance + amount;
                System.out.println("Deposited: $" + amount + ", New Balance: $" + balance);
                // REENTRANCY: Check for bonus - calls another method that needs same lock
                if(balance>=1000){
                    addBonus();
                }
            }
        }finally {
            lock.unlock(); // CRITICAL: Always unlock in finally
        }
    }

    // Basic withdraw with lock
    public void withdraw(double amount){
        lock.lock();
        try{
            if(amount >0 && amount<=balance){
                balance = balance - amount;
                System.out.println("Withdrew: $" + amount + ", New Balance: $" + balance);
            }else {
                System.out.println("Insufficient funds or invalid amount");
            }
        }finally {
            lock.unlock();
        }
    }

    // ADVANTAGE: Try deposit without waiting (impossible with synchronized)
    public boolean tryDeposit(double amount){
        if(lock.tryLock()){
            try {
                if(amount>0){
                    balance = balance + amount;
                    System.out.println("Quick Deposit: $" + amount + ", New Balance: $" + balance);
                    return true; // Success!
                }
            }finally {
                lock.unlock();
            }
        }
        System.out.println("Account busy, couldn't deposit $" + amount + " - try later");
        return false; // Couldn't get lock, but didn't wait
    }

    // ADVANTAGE: Try withdraw with timeout
    public boolean tryWithdrawWithTimeout(double amount, long timeoutSeconds ){
       try {
           if(lock.tryLock(timeoutSeconds, TimeUnit.SECONDS)){
              try{
                if(amount>0 && balance>= amount){
                    balance = balance - amount;
                    System.out.println("Timed Withdraw: $" + amount + ", New Balance: $" + balance);
                    return true;
                }else {
                    System.out.println("Insufficient funds");
                    return false;
                }
              }finally {
                  lock.unlock();
              }
           }else {
               System.out.println("Timeout: Couldn't withdraw $" + amount + " within " + timeoutSeconds + " seconds");
                return false;
           }
       } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            return false;
       }
    }

    // REENTRANCY: Method called from deposit() that also needs the same lock
    private void addBonus(){
        lock.lock(); // Same thread can acquire again - this is "reentrancy"
        try{
            double bonus = balance * 0.01; // 1% bonus
            balance = balance + bonus;
            System.out.println("🎉 Bonus added: $" + bonus + " (Balance >= $1000)");
        }finally {
            lock.unlock();
        }
    }

    // Get balance safely
    public double getBalance(){
      lock.lock();
      try {
          return balance;
      }finally {
          lock.unlock();
      }
    }

    // ADVANTAGE: Check if account is currently being used
    public boolean isAccountBusy(){
        return lock.isLocked();
    }

    // ADVANTAGE: See how many threads are waiting to use this account
    public double getWaitingThreads(){
        return lock.getQueueLength();
    }

    // Transfer with safety check
    public boolean transfer(AdvancedBankAccount toAccount, double amount){
        lock.lock();
        try {
            if(amount>0 && balance >= amount){
                balance -= amount;
                System.out.println("Transferring $" + amount + "...");
                toAccount.deposit(amount);
                System.out.println("Transfer completed!");
                return true;
            }else {
                System.out.println("Transfer failed: Insufficient funds");
                return false;
            }
        }finally {
            lock.unlock();
        }
    }
}

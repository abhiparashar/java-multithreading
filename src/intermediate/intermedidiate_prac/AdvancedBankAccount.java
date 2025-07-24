package intermediate.intermedidiate_prac;

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
}

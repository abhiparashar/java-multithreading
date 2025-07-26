package intermediate.intermedidiate_prac;

import java.util.ArrayDeque;
import java.util.Queue;
import java.util.concurrent.Semaphore;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.ReentrantLock;

class BoundedBuffer<T>{
    private final Queue<T> buffer;
    private final int capacity;

    BoundedBuffer(int capacity) {
        this.capacity = capacity;
        this.buffer = new ArrayDeque<>(capacity);
    }

    private final Object lock = new Object();

    public void put(T item) throws InterruptedException {
        synchronized (lock){
            while (buffer.size()==capacity){
                System.out.println(Thread.currentThread().getName() +
                        " waiting to put (buffer full)");
               lock.wait();
            }
            buffer.offer(item);
            System.out.println(Thread.currentThread().getName() +
                    " put: " + item + " (size: " + buffer.size() + ")");
            lock.notifyAll();
        }
    }

    public T take() throws InterruptedException {
        synchronized (lock){
            while (buffer.isEmpty()){
                System.out.println(Thread.currentThread().getName() +
                        " waiting to take (buffer empty)");
                lock.wait();
            }
            T item = buffer.poll();
            System.out.println(Thread.currentThread().getName() +
                    " took: " + item + " (size: " + buffer.size() + ")");
            lock.notifyAll();
            return item;
        }
    }

    public int size(){
        synchronized (lock){
            return buffer.size();
        }
    }
}

// Alternative implementation using ReentrantLock and Condition
class BoundedBufferWithConditions<T>{
    private final Queue<T>buffer;
    private final int capacity;

    BoundedBufferWithConditions(int capacity) {
        this.buffer = new ArrayDeque<>();
        this.capacity = capacity;
    }

    private final ReentrantLock lock = new ReentrantLock();
    private final Condition notFull = lock.newCondition();
    private final Condition notEmpty = lock.newCondition();

    public void put(T item) throws InterruptedException {
        lock.lock();
        try {
            while (buffer.size()==capacity){
              notFull.await();
            }
            buffer.offer(item);
            System.out.println(Thread.currentThread().getName() +
                    " put: " + item + " with Lock+Condition");
            notEmpty.signal();
        }finally {
            lock.unlock();
        }
    }

    public T take() throws InterruptedException {
        lock.lock();
        try{
            while (buffer.isEmpty()){
                notEmpty.await();
            }
            T item = buffer.poll();
            notFull.signal();
            System.out.println(Thread.currentThread().getName() +
                    " took: " + item + " with Lock+Condition");
            return item;
        }finally {
            lock.unlock();
        }
    }
}

// Alternative using Semaphore
class BoundedBufferWithSemaphore<T>{
    private final Queue<T> buffer;
    private final Semaphore items;
    private final Semaphore spaces;
    private final Object lock = new Object();

    BoundedBufferWithSemaphore(int capacity) {
        this.buffer = new ArrayDeque<>();
        this.items = new Semaphore(0);
        this.spaces = new Semaphore(capacity);
    }

    public void put(T item) throws InterruptedException {
        spaces.acquire();
        synchronized (lock){
            buffer.offer(item);
            System.out.println(Thread.currentThread().getName() +
                    " put: " + item + " with Semaphore");
        }
        items.release();
    }

    public T take() throws InterruptedException {
       items.acquire();
       T item;
       synchronized (lock){
           item = buffer.poll();
           System.out.println(Thread.currentThread().getName() +
                   " took: " + item + " with Semaphore");
       }
       spaces.release();
       return item;
    }
}
public class BoundedBufferDemo {
    public static void main(String[] args) throws InterruptedException {
        System.out.println("=== Bounded Buffer Implementations Demo ===\n");

        // Test basic wait/notify implementation
        testBasicBoundedBuffer();

        Thread.sleep(2000);

        // Test Lock+Condition implementation
        testConditionBoundedBuffer();

        Thread.sleep(2000);

        // Test Semaphore implementation
        testSemaphoreBoundedBuffer();
    }

    public static void testBasicBoundedBuffer() throws InterruptedException {
        System.out.println("--- Testing Basic Wait/Notify Implementation ---");
        BoundedBuffer<Integer> buffer = new BoundedBuffer<>(3);
        // Producer thread
        Thread producer = new Thread(()->{
            try{
                for (int i = 0; i <= 10; i++) {
                    buffer.put(i);
                    Thread.sleep(300);
                }
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        },"Producer");

        // Consumer thread
        Thread consumer = new Thread(()->{
            try{
                for (int i = 0; i <=10 ; i++) {
                    buffer.take();
                    Thread.sleep(500);
                }
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        },"Consumer");

        producer.start();
        consumer.start();

        producer.join();
        consumer.join();
    }

    public static void testConditionBoundedBuffer() throws InterruptedException {
        System.out.println("\n--- Testing Lock+Condition Implementation ---");
        BoundedBufferWithConditions<String> buffer = new BoundedBufferWithConditions<>(2);
        //Producer
        Thread producer = new Thread(()->{
            try{
                String[] items = {"A", "B", "C", "D", "E"};
                for (String item : items) {
                    buffer.put(item);
                    Thread.sleep(200);
                }
            } catch (InterruptedException e) {
               Thread.currentThread().interrupt();
            }
        },"ConditionProducer");

        Thread consumer = new Thread(()->{
            try{
                for (int i = 0; i < 5; i++) {
                    buffer.take();
                    Thread.sleep(400);
                }
            } catch (InterruptedException e) {
               Thread.currentThread().interrupt();
            }
        },"ConditionConsumer");

        producer.start();
        consumer.start();

        producer.join();
        consumer.join();
    }

    public static void testSemaphoreBoundedBuffer() throws InterruptedException {
        System.out.println("\n--- Testing Semaphore Implementation ---");
        BoundedBufferWithSemaphore<Character>buffer = new BoundedBufferWithSemaphore<>(2);
        //Producer
        Thread producer = new Thread(()->{
            try{
                char[] items = {'X', 'Y', 'Z'};
                for (char item : items){
                    buffer.put(item);
                    Thread.sleep(250);
                }
            } catch (InterruptedException e) {
               Thread.currentThread().interrupt();
            }
        },"SemaphoreProducer");

        //Consumer
        Thread consumer = new Thread(()->{
            try{
                for (int i = 0; i < 3; i++) {
                    buffer.take();
                    Thread.sleep(400);
                }
            } catch (InterruptedException e) {
               Thread.currentThread().interrupt();
            }
        },"SemaphoreConsumer");

        producer.start();
        consumer.start();

        producer.join();
        consumer.join();
    }
}

package basic;

import java.util.ArrayDeque;
import java.util.Queue;

class SimpleProducerConsumer{
    private final Queue<String>buffer;
    private final Object monitor = new Object();
    private final int capacity;

    SimpleProducerConsumer() {
        this.capacity = 5;
        this.buffer = new ArrayDeque<>(capacity);
    }

    public void put(String item){
        synchronized (monitor){
            try{
                while (buffer.size()==capacity){
                    System.out.println("Buffer full, producer waiting...");
                    monitor.wait();
                }
                buffer.offer(item);
                System.out.println("Produced: " + item + ", Buffer size: " + buffer.size());
                monitor.notifyAll();
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }
    }

    public String take() {
        synchronized (monitor){
            try{
                while (buffer.isEmpty()){
                    monitor.wait();
                }
                String item = buffer.poll();
                monitor.notifyAll();
                return item;
            }catch (InterruptedException e ){
                Thread.currentThread().interrupt();
                return null;
            }
        }
    }

    public int size() {
        return buffer.size();
    }
}

class Producer implements Runnable {
    private final SimpleProducerConsumer buffer;

    Producer(SimpleProducerConsumer buffer) {
        this.buffer = buffer;
    }

    @Override
    public void run() {
        try {
            String[] items = {"item1", "item2", "item3", "item4", "item5", "item6", "item7"};
            for (String item : items){
                buffer.put(item);
                Thread.sleep(1000); // Simulate production time
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }
}

class Consumer implements Runnable {
    private final SimpleProducerConsumer buffer;
    private final String name;

    Consumer(SimpleProducerConsumer buffer, String name) {  // Fixed: Added buffer parameter
        this.buffer = buffer;
        this.name = name;
    }

    @Override
    public void run() {
        try {
            for (int i = 0; i < 5; i++) {  // Fixed: Added limit to prevent infinite loop
                String item = buffer.take();
                if (item != null) {
                    System.out.println(name + " consumed: " + item + ", Buffer size: " + buffer.size());
                }
                Thread.sleep(1500); // Simulate consumption time
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }
}

public class SimpleProducerConsumerDemo {
    public static void main(String[] args) throws InterruptedException {
        SimpleProducerConsumer sharedBuffer = new SimpleProducerConsumer();  // Fixed: Create shared buffer

        Thread producer = new Thread(new Producer(sharedBuffer),"Producer");
        Thread consumer1 = new Thread(new Consumer(sharedBuffer, "Consumer-1"),"Consumer-1");
        Thread consumer2 = new Thread(new Consumer(sharedBuffer, "Consumer-2"),"Consumer-2");

        producer.start();
        consumer1.start();
        consumer2.start();

        producer.join();
        consumer1.join();
        consumer2.join();

        System.out.println("All threads completed!");
    }
}
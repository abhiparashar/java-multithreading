import java.util.LinkedList;
import java.util.Queue;

public class LogProcessingSystem {
    public static class LogBuffer{
        // Shared log buffer
        private final Queue<String>logs = new LinkedList<>();

        //Add log
        public synchronized void addLog(String logEntry) throws InterruptedException {
            int maxSize = 10;
            while (logs.size()>= maxSize){
                System.out.println("Buffer is full, waiting...");
                wait();
            }
            logs.offer(logEntry);
            System.out.println("📝 Added log: " + logEntry + " (Buffer size: " + logs.size() + ")");
            notifyAll();
        }

        //get log
        public synchronized String getLog() throws InterruptedException {
            while (logs.isEmpty()){
                System.out.println("No logs available, waiting...");
                wait();
            }
            String log = logs.poll();
            System.out.println("📤 Processing log: " + log + " (Buffer size: " + logs.size() + ")");
            notifyAll();
            return log;
        }
    }

    // Log producer (simulates application generating logs)
    static class LogProducer implements Runnable{
        private final LogBuffer buffer;
        private final String appName;

        LogProducer( LogBuffer buffer, String appName) {
            this.buffer = buffer;
            this.appName = appName;
        }

        @Override
        public void run() {
            try {
                for (int i = 0; i <=5 ; i++) {
                    String logEntry = "[" + appName + "] Log entry " + i + " at " + System.currentTimeMillis();
                    buffer.addLog(logEntry);
                    Thread.sleep(1000);
                }
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }
    }

    // Log consumer (simulates log processing service)
    static class LogConsumer implements Runnable{
        private final LogBuffer buffer;
        private final String processorName;

        LogConsumer(LogBuffer buffer, String processorName) {
            this.buffer = buffer;
            this.processorName = processorName;
        }

        @Override
        public void run() {
            try {
                for (int i = 0; i < 8; i++) {
                    String log = buffer.getLog();
                    Thread.sleep(1500);
                    System.out.println("✅ [" + processorName + "] Processed: " + log);
                }
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }
    }

    public static void main(String[] args) throws InterruptedException {
        LogBuffer logBuffer = new LogBuffer();
        // Create producers (different applications)
        Thread webApp = new Thread(new LogProducer(logBuffer, "WebApp"));
        Thread apiService = new Thread(new LogProducer(logBuffer, "ApiService"));

        // Create consumers (log processors)
        Thread processor1 = new Thread(new LogConsumer(logBuffer, "Processor-1"));
        Thread processor2 = new Thread(new LogConsumer(logBuffer, "Processor-2"));

        // Start all threads
        webApp.start();
        apiService.start();
        processor1.start();
        processor2.start();

        // Wait for completion
        webApp.join();
        apiService.join();
        processor1.join();
        processor2.join();

        System.out.println("🎉 Log processing completed!");
    }
}

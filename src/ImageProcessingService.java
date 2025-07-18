import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

public class ImageProcessingService {
    static class ImageProcessor implements Runnable{
        private final String imageName;
        private final String operation;

        ImageProcessor(String imageName, String operation) {
            this.imageName = imageName;
            this.operation = operation;
        }

        @Override
        public void run() {
            System.out.println("🖼️ Starting " + operation + " on " + imageName +
                    " [Thread: " + Thread.currentThread().getName() + "]");
            try {
                Thread.sleep(2000+ (int)(Math.random()*3000));
                System.out.println("✅ Completed " + operation + " on " + imageName);
            } catch (InterruptedException e) {
               Thread.currentThread().interrupt();
               System.out.println("❌ Processing interrupted for " + imageName);
            }
        }
    }
    public static void main(String[] args) throws InterruptedException {
        ExecutorService imageProcessor  = Executors.newFixedThreadPool(3);

        // List of images to process
        String[] images = {
                "vacation-photo1.jpg", "vacation-photo2.jpg", "vacation-photo3.jpg",
                "profile-pic.jpg", "document-scan.jpg", "screenshot.png",
                "wedding-photo1.jpg", "wedding-photo2.jpg"
        };

        String[] operations = {"resize", "compress", "watermark", "filter"};

        for (String image: images){
            String operation = operations[(int)(Math.random()*operations.length)];
            imageProcessor.submit(new ImageProcessor(image, operation));
        }

        // Monitor progress
        System.out.println("📊 All tasks submitted. Processing with 3 threads...");

        // Shutdown executor
        imageProcessor.shutdown();

        // Wait for all tasks to complete
        if(imageProcessor.awaitTermination(30, TimeUnit.SECONDS)){
            System.out.println("🎉 All images processed successfully!");
        }else {
            System.out.println("⏰ Some tasks didn't complete in time");
            imageProcessor.shutdown();
        }

    }
}

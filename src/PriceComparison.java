import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.*;

public class PriceComparison {
   public static class PriceFetcher implements Callable<Double> {
       private final String website;
       private final String product;


       public PriceFetcher(String website, String product) {
           this.website = website;
           this.product = product;
       }

       @Override
       public Double call() throws Exception {
           System.out.println("Fetching price from " + website);

           // Simulate network delay
           Thread.sleep(1000 + (int)(Math.random() * 2000));

           // Simulate random price
           double price = 100 + Math.random() * 200;
           System.out.println(website + " price: $" + String.format("%.2f", price));
           return price;
       }
   }
    public static void main(String[] args) throws Exception {
        String product = "iPhone 15";
        String[] websites = {"Amazon", "eBay", "BestBuy", "Walmart"};

        ExecutorService executorService = Executors.newFixedThreadPool(4);

        // Submit all price fetching tasks
        List<Future<Double>>futures = new ArrayList<>();

        for (String website : websites){
            Future<Double>future = executorService.submit(new PriceFetcher(website, product));
            futures.add(future);
        }

        // Collect results
        double minPrice = Double.MAX_VALUE;
        String bestWebsite = "";
        for (int i = 0; i < futures.size(); i++) {
            try {
                double price = futures.get(i).get(5, TimeUnit.SECONDS);
                if (price < minPrice) {
                    minPrice = price;
                    bestWebsite = websites[i];
                }
            } catch (TimeoutException e) {
                System.out.println(websites[i] + " timed out");
            }
        }
        System.out.println("\nBest price: $" + String.format("%.2f", minPrice) +
                " from " + bestWebsite);
        executorService.shutdown();
    }
}

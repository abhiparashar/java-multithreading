import java.util.Arrays;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

public class ECommerceOrderProcessor {

    // Simulate different service calls
    static class OrderService {
        static CompletableFuture<String> validateOrder(String orderId) {
            return CompletableFuture.supplyAsync(() -> {
                try {
                    System.out.println("🔍 Validating order: " + orderId);
                    Thread.sleep(1000);
                    return "Order validated: " + orderId;
                } catch (InterruptedException e) {
                    throw new RuntimeException(e);
                }
            });
        }

        static CompletableFuture<String> processPayment(String orderValidation) {
            return CompletableFuture.supplyAsync(() -> {
                try {
                    System.out.println("💳 Processing payment for validated order");
                    Thread.sleep(1500);
                    return "Payment processed for: " + orderValidation;
                } catch (InterruptedException e) {
                    throw new RuntimeException(e);
                }
            });
        }

        static CompletableFuture<String> updateInventory(String paymentInfo) {
            return CompletableFuture.supplyAsync(() -> {
                try {
                    System.out.println("📦 Updating inventory");
                    Thread.sleep(800);
                    return "Inventory updated: " + paymentInfo;
                } catch (InterruptedException e) {
                    throw new RuntimeException(e);
                }
            });
        }

        static CompletableFuture<String> sendConfirmation(String inventoryUpdate) {
            return CompletableFuture.supplyAsync(() -> {
                try {
                    System.out.println("📧 Sending confirmation email");
                    Thread.sleep(500);
                    return "Confirmation sent: " + inventoryUpdate;
                } catch (InterruptedException e) {
                    throw new RuntimeException(e);
                }
            });
        }
    }

    public static void main(String[] args) {
        String orderId = "ORD-12345";

        // Chain the entire order processing workflow
        CompletableFuture<String> orderProcessingFlow = OrderService.validateOrder(orderId)
                .thenCompose(OrderService::processPayment)
                .thenCompose(OrderService::updateInventory)
                .thenCompose(OrderService::sendConfirmation)
                .exceptionally(throwable -> {
                    System.err.println("❌ Order processing failed: " + throwable.getMessage());
                    return "Order processing failed for: " + orderId;
                });

        // Process multiple orders concurrently
        String[] orderIds = {"ORD-001", "ORD-002", "ORD-003"};

        CompletableFuture<Void> allOrders = CompletableFuture.allOf(
                Arrays.stream(orderIds)
                        .map(id -> OrderService.validateOrder(id)
                                .thenCompose(OrderService::processPayment)
                                .thenCompose(OrderService::updateInventory)
                                .thenCompose(OrderService::sendConfirmation))
                        .toArray(CompletableFuture[]::new)
        );

        // Wait for all orders to complete
        try {
            allOrders.get(30, TimeUnit.SECONDS);
            System.out.println("🎉 All orders processed successfully!");
        } catch (Exception e) {
            System.err.println("Some orders failed: " + e.getMessage());
        }
    }
}
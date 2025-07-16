import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

public class UserRegistrationFlow {
    // Simulate async operations
    static CompletableFuture<String>validateEmail(String email){
       return CompletableFuture.supplyAsync(()->{
            try {
                Thread.sleep(500);
                if(email.contains("@")){
                    return "Valid email: " + email;
                }else {
                    throw new RuntimeException("Invalid email");
                }
            } catch (InterruptedException e) {
                System.out.println("Thread interrupted");
                throw new RuntimeException(e);
            }
       });
    }

    static CompletableFuture<String>createUser(String validEmail){
        return CompletableFuture.supplyAsync(()->{
            try {
                Thread.sleep(1000); // Simulate database operation
                return "User created with ID: " + Math.abs(validEmail.hashCode());
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            }
        });
    }

    static CompletableFuture<String>sendWelcomeEmail(String userId){
        return CompletableFuture.supplyAsync(()->{
            try {
                Thread.sleep(300);
                return "Welcome email sent to user: " + userId;
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            }
        });
    }

    public static void main(String[] args) throws ExecutionException, InterruptedException, TimeoutException {
        String email = "user@example.com";
        CompletableFuture<String>registrationFlow = validateEmail(email)
                .thenCompose(UserRegistrationFlow::createUser)
                .thenCompose(UserRegistrationFlow::sendWelcomeEmail)
                .exceptionally(throwable->{
                    return "Registration failed: " + throwable.getMessage();
                });

        // Get final result
        try{
            String result = registrationFlow.get(5, TimeUnit.SECONDS);
            System.out.println("Registration result: " + result);
        } catch (RuntimeException e) {
            throw new RuntimeException(e);
        }
    }
}

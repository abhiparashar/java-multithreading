import java.sql.Connection;
import java.util.concurrent.BlockingDeque;
import java.util.concurrent.LinkedBlockingDeque;
import java.util.concurrent.atomic.AtomicInteger;

public class DatabaseConnectionPool {
    private final BlockingDeque<Connection>pool;
    private final int maxPoolSize;
    private final AtomicInteger activeConnections =  new AtomicInteger(0);
    public DatabaseConnectionPool(int maxPoolSize) {
        this.pool = new LinkedBlockingDeque<>(maxPoolSize);
        this.maxPoolSize = maxPoolSize;

        // Pre-create some connections
        for (int i = 0; i < Math.min(5, maxPoolSize); i++) {
            pool.offer(createConnection());
        }
    }

    private Connection createConnection() {
        // Simulate connection creation
        return new Connection("DB-Connection-" + System.currentTimeMillis());
    }


}

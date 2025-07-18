import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicReference;

public class WebsiteAnalytics {
    // Atomic counters for thread-safe statistics
    private final AtomicLong pageViews = new AtomicLong(0);
    private final AtomicLong uniqueVisitors = new AtomicLong(0);
    private final AtomicLong errorCount = new AtomicLong(0);
    private final AtomicReference<String>lastError = new AtomicReference<>("none");
}

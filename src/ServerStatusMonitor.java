public class ServerStatusMonitor {
    private volatile boolean serverRunning = true;
    private volatile String serverStatus = "STARTING";
    private volatile int activeConnections = 0;

    // Worker thread that monitors server
    class ServerWorker implements Runnable {

        @Override
        public void run() {
            while (serverRunning){
                try {
                    Thread.sleep(200);
                    // Update status based on some condition
                    if(activeConnections>100){
                        serverStatus = "OVERLOADED";
                    } else if (activeConnections > 0) {
                        serverStatus = "RUNNING";
                    }else {
                        serverStatus = "IDLE";
                    }
                    System.out.println("Server status: " + serverStatus +
                            " (Connections: " + activeConnections + ")");
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    break;
                }
            }
            serverStatus = "STOPPED";
            System.out.println("Server worker stopped");
        }
    }

    // Control methods
    public void startServer(){
        serverRunning = true;
        Thread worker = new Thread(new ServerWorker());
        worker.start();
        worker.start();
    }

    public void stopServer(){
        serverRunning = false;
    }

    public void simulateConnection(){
        activeConnections++;
    }

    public void simulateDisconnection(){
        if(activeConnections>0){
            activeConnections--;
        }
    }
}

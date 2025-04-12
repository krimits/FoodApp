// Worker.java
import java.io.*;
import java.net.*;
import java.util.ArrayList;

public class Worker {

    public static void main(String[] args) throws UnknownHostException {
        if (args.length != 1) {
            System.out.println("Usage: java Worker <port>");
            System.exit(1);
        }
        
        int port = Integer.parseInt(args[0]);

        ArrayList<Store> stores = new ArrayList<>();
        Object lock = new Object();

        new Worker().openServer(port, stores, lock);
    }

    ServerSocket providerSocket;
    Socket connection = null;

    void openServer(int port, ArrayList<Store> stores, Object lock) {
        try {
            providerSocket = new ServerSocket(port, 10);
            System.out.println("Worker started on port " + port);
            System.out.println("Waiting for connections from Master...");

            while (true) {
                connection = providerSocket.accept();
                System.out.println("Connection established with Master: " + connection.getInetAddress());

                Thread t = new WorkerActions(connection, stores, lock);
                t.start();
            }
        } catch (IOException ioException) {
            System.err.println("Error in worker server: " + ioException.getMessage());
            ioException.printStackTrace();
        } finally {
            try {
                if (providerSocket != null && !providerSocket.isClosed()) {
                    System.out.println("Closing worker server socket...");
                    providerSocket.close();
                }
            } catch (IOException ioException) {
                System.err.println("Error closing worker socket: " + ioException.getMessage());
                ioException.printStackTrace();
            }
        }
    }
}

// Master.java
import java.io.*;
import java.net.*;
import java.util.*;

public class Master {
    public static void main(String[] args) throws UnknownHostException {
        if (args.length < 2 || args.length % 2 != 0) {
            System.out.println("Usage: java Master <worker1_ip> <worker1_port> <worker2_ip> <worker2_port> ...");
            System.exit(1);
        }

        String[][] workers = new String[args.length/2][2];
        HashMap<Integer, ObjectOutputStream> connectionsOut = new HashMap<>();

        for (int i = 0; i < args.length/2; i++) {
            workers[i][0] = args[i*2];
            workers[i][1] = args[i*2 + 1];
            System.out.println("Worker " + (i+1) + " configured at " + workers[i][0] + ":" + workers[i][1]);
        }

        new Master().openServer(workers, connectionsOut);
    }

    ServerSocket providerSocket;
    Socket connection = null;
    int counterID = 0;

    void openServer(String[][] workers, HashMap<Integer, ObjectOutputStream> connectionsOut) {
        try {
            providerSocket = new ServerSocket(4321, 10);
            System.out.println("Master server started on port 4321");
            System.out.println("Waiting for connections...");

            while (true) {
                connection = providerSocket.accept();
                counterID++;
                System.out.println("Connection " + counterID + " established with: " + connection.getInetAddress());

                Thread t = new Actions(connection, workers, connectionsOut, counterID);
                t.start();
            }
        } catch (IOException ioException) {
            System.err.println("Error in master server: " + ioException.getMessage());
            ioException.printStackTrace();
        } finally {
            try {
                if (providerSocket != null && !providerSocket.isClosed()) {
                    System.out.println("Closing master server socket...");
                    providerSocket.close();
                }
            } catch (IOException ioException) {
                System.err.println("Error closing server socket: " + ioException.getMessage());
                ioException.printStackTrace();
            }
        }
    }
}
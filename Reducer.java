// Reducer.java
import java.io.*;
import java.net.*;
import java.util.ArrayList;
import java.util.Map;

public class Reducer {
    private static final int DEFAULT_PORT = 4325;
    
    public static void main(String[] args) throws UnknownHostException {
        int port = DEFAULT_PORT;
        
        if (args.length > 0) {
            try {
                port = Integer.parseInt(args[0]);
            } catch (NumberFormatException e) {
                System.err.println("Invalid port number. Using default port: " + DEFAULT_PORT);
            }
        }
        
        ArrayList<Integer> ids = new ArrayList<>();
        ArrayList<ArrayList<Store>> idFilters = new ArrayList<>();
        Object lock = new Object();
        
        ArrayList<Integer> ids1 = new ArrayList<>();
        ArrayList<Map<String, Integer>> idDict = new ArrayList<>();
        Object lock1 = new Object();

        new Reducer().openServer(port, ids, idFilters, lock, ids1, idDict, lock1);
    }

    ServerSocket providerSocket;
    Socket connection = null;

    void openServer(int port, ArrayList<Integer> ids, ArrayList<ArrayList<Store>> idFilters, 
                   Object lock, ArrayList<Integer> ids1, ArrayList<Map<String, Integer>> idDict, 
                   Object lock1) {
        try {
            providerSocket = new ServerSocket(port, 10);
            System.out.println("Reducer started on port " + port);
            System.out.println("Waiting for connections...");

            while (true) {
                connection = providerSocket.accept();
                System.out.println("Connection established with: " + connection.getInetAddress());

                Thread t = new ReducerActions(connection, ids, idFilters, lock, ids1, idDict, lock1);
                t.start();
            }
        } catch (IOException ioException) {
            System.err.println("Error in reducer server: " + ioException.getMessage());
            ioException.printStackTrace();
        } finally {
            try {
                if (providerSocket != null && !providerSocket.isClosed()) {
                    System.out.println("Closing reducer server socket...");
                    providerSocket.close();
                }
            } catch (IOException ioException) {
                System.err.println("Error closing reducer socket: " + ioException.getMessage());
                ioException.printStackTrace();
            }
        }
    }
}

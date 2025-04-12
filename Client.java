// Client.java
import java.io.*;
import java.net.Socket;
import java.util.*;

public class Client {
    private static final String SERVER_IP = "127.0.0.1";
    private static final int SERVER_PORT = 4321;
    
    public static void main(String[] args) {
        Scanner sc = new Scanner(System.in);

        while (true) {
            printMainMenu();
            String option = sc.nextLine().trim();

            switch(option) {
                case "1":
                    findNearbyStores(sc);
                    break;
                case "2":
                    filterStores(sc);
                    break;
                case "3":
                    purchaseProducts(sc);
                    break;
                case "4":
                    rateStore(sc);
                    break;
                case "5":
                    System.out.println("Goodbye!");
                    sc.close();
                    return;
                default:
                    System.out.println("Invalid option. Please try again.");
            }
        }
    }
    
    private static void printMainMenu() {
        System.out.println("\n===== FOOD DELIVERY APP =====");
        System.out.println("1. Stores near you");
        System.out.println("2. Filtering stores");
        System.out.println("3. Purchase products");
        System.out.println("4. Rate store");
        System.out.println("5. Exit");
        System.out.print("Choose an option: ");
    }
    
    private static void findNearbyStores(Scanner sc) {
        Socket requestSocket = null;
        ObjectOutputStream out = null;
        ObjectInputStream in = null;
        
        try {
            requestSocket = new Socket(SERVER_IP, SERVER_PORT);
            out = new ObjectOutputStream(requestSocket.getOutputStream());
            in = new ObjectInputStream(requestSocket.getInputStream());

            out.writeObject("client");
            out.flush();

            System.out.print("Enter your latitude: ");
            double lat = Double.parseDouble(sc.nextLine());

            System.out.print("Enter your longitude: ");
            double lon = Double.parseDouble(sc.nextLine());

            // Create MapReduceRequest with default filters
            MapReduceRequest request = new MapReduceRequest(
                    lat,
                    lon,
                    new ArrayList<>(), // No category filter
                    0,                // No minimum stars
                    "",               // No price filter
                    5.0               // 5km radius
            );

            out.writeObject(request);
            out.flush();

            System.out.println("Searching for stores nearby...");

            ArrayList<Store> results = (ArrayList<Store>) in.readObject();

            if (results.isEmpty()) {
                System.out.println("No nearby stores found within 5 km.");
            } else {
                System.out.println("\nNearby Stores:");
                for (Store store : results) {
                    System.out.println(store);
                    System.out.println("-----------");
                }
            }

            out.writeObject("Done");
            out.flush();

        } catch (Exception e) {
            System.err.println("Error connecting to server: " + e.getMessage());
            e.printStackTrace();
        } finally {
            closeConnections(requestSocket, out, in);
        }
    }
    
    private static void filterStores(Scanner sc) {
        Socket requestSocket = null;
        ObjectOutputStream out = null;
        ObjectInputStream in = null;
        
        try {
            requestSocket = new Socket(SERVER_IP, SERVER_PORT);
            out = new ObjectOutputStream(requestSocket.getOutputStream());
            in = new ObjectInputStream(requestSocket.getInputStream());

            out.writeObject("filter");
            out.flush();

            System.out.print("Enter your latitude: ");
            double latitude = Double.parseDouble(sc.nextLine());

            System.out.print("Enter your longitude: ");
            double longitude = Double.parseDouble(sc.nextLine());

            System.out.print("Enter food categories (comma-separated, e.g., pizza,burger): ");
            ArrayList<String> categories = new ArrayList<>(Arrays.asList(sc.nextLine().split("\\s*,\\s*")));

            System.out.print("Enter minimum stars (1-5): ");
            int minStars = Integer.parseInt(sc.nextLine());

            System.out.print("Enter price category ($, $$, $$$): ");
            String price = sc.nextLine();

            MapReduceRequest request = new MapReduceRequest(
                    latitude,
                    longitude,
                    categories,
                    minStars,
                    price,
                    5.0 // radius in km
            );

            out.writeObject(request);
            out.flush();

            System.out.println("Searching with filters...");

            ArrayList<Store> results = (ArrayList<Store>) in.readObject();

            if (results.isEmpty()) {
                System.out.println("No stores found matching your filters.");
            } else {
                System.out.println("\nFiltered Stores:");
                for (Store store : results) {
                    System.out.println(store);
                    System.out.println("-----------");
                }
            }

            out.writeObject("Done");
            out.flush();

        } catch (Exception e) {
            System.err.println("Error connecting to server: " + e.getMessage());
            e.printStackTrace();
        } finally {
            closeConnections(requestSocket, out, in);
        }
    }
    
    private static void purchaseProducts(Scanner sc) {
        Socket requestSocket = null;
        ObjectOutputStream out = null;
        ObjectInputStream in = null;
        
        try {
            requestSocket = new Socket(SERVER_IP, SERVER_PORT);
            out = new ObjectOutputStream(requestSocket.getOutputStream());
            in = new ObjectInputStream(requestSocket.getInputStream());

            out.writeObject("fetchProducts");
            out.flush();

            // Store info
            System.out.print("Enter store name you want to buy from: ");
            String storeName = sc.nextLine();

            out.writeObject(storeName);  // Send store name to fetch product list
            out.flush();

            ArrayList<Product> storeProducts = (ArrayList<Product>) in.readObject();

            if (storeProducts.isEmpty()) {
                System.out.println("No products available for this store.");
                closeConnections(requestSocket, out, in);
                return;
            }

            System.out.println("\nAvailable products:");
            for (Product p : storeProducts) {
                System.out.println("- " + p.getName() + " (" + p.getCategory() + ") - " + p.getPrice() + "€ | Available: " + p.getQuantity());
            }

            // Create new socket for purchase operation
            closeConnections(requestSocket, out, in);
            requestSocket = new Socket(SERVER_IP, SERVER_PORT);
            out = new ObjectOutputStream(requestSocket.getOutputStream());
            in = new ObjectInputStream(requestSocket.getInputStream());

            out.writeObject("purchase");
            out.flush();

            // Products to purchase
            ArrayList<Product> productsToBuy = new ArrayList<>();
            while (true) {
                System.out.print("Enter product name (or type 'done' to finish): ");
                String name = sc.nextLine();
                if (name.equalsIgnoreCase("done")) break;

                System.out.print("Enter quantity: ");
                int quantity = Integer.parseInt(sc.nextLine());

                // Create a product with only name and quantity
                // Set category and price to placeholders — Worker will fill them in
                productsToBuy.add(new Product(name, "", quantity, 0.0));
            }

            // Buyer info
            System.out.print("Enter your name: ");
            String customerName = sc.nextLine();

            System.out.print("Enter your email: ");
            String email = sc.nextLine();

            Purchase purchase = new Purchase(customerName, email, productsToBuy);

            out.writeObject(purchase);
            out.flush();

            // Response
            String response = (String) in.readObject();
            System.out.println("Server response: " + response);

        } catch (Exception e) {
            System.err.println("Error during purchase: " + e.getMessage());
            e.printStackTrace();
        } finally {
            closeConnections(requestSocket, out, in);
        }
    }
    
    private static void rateStore(Scanner sc) {
        Socket requestSocket = null;
        ObjectOutputStream out = null;
        ObjectInputStream in = null;
        
        try {
            requestSocket = new Socket(SERVER_IP, SERVER_PORT);
            out = new ObjectOutputStream(requestSocket.getOutputStream());
            in = new ObjectInputStream(requestSocket.getInputStream());

            out.writeObject("rate");
            out.flush();

            System.out.print("Enter store name to rate: ");
            String storeName = sc.nextLine();

            System.out.print("Enter rating (1 to 5): ");
            int rating = Integer.parseInt(sc.nextLine());

            while (rating < 1 || rating > 5) {
                System.out.print("Invalid rating. Please enter a number between 1 and 5: ");
                rating = Integer.parseInt(sc.nextLine());
            }

            out.writeObject(storeName);
            out.flush();
            out.writeObject(rating);
            out.flush();

            String response = (String) in.readObject();
            System.out.println("Server: " + response);

        } catch (Exception e) {
            System.err.println("Error rating store: " + e.getMessage());
            e.printStackTrace();
        } finally {
            closeConnections(requestSocket, out, in);
        }
    }
    
    private static void closeConnections(Socket socket, ObjectOutputStream out, ObjectInputStream in) {
        try {
            if (in != null) in.close();
            if (out != null) out.close();
            if (socket != null) socket.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
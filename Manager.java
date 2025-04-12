// Manager.java
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;

import java.io.*;
import java.net.*;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.text.ParseException;
import java.util.*;

public class Manager {
    private static final String SERVER_IP = "127.0.0.1";
    private static final int SERVER_PORT = 4321;
    
    public static void main(String[] args) throws ParseException, FileNotFoundException {
        Scanner sc = new Scanner(System.in);
        boolean running = true;

        while (running) {
            printMenu();
            String choice = sc.nextLine().trim();

            switch (choice) {
                case "1":
                    addStore(sc);
                    break;
                case "2":
                    addProduct(sc);
                    break;
                case "3":
                    removeProduct(sc);
                    break;
                case "4":
                    getSalesByStoreType(sc);
                    break;
                case "5":
                    getSalesByProductCategory(sc);
                    break;
                case "6":
                    System.out.println("Exiting Manager Application");
                    running = false;
                    break;
                default:
                    System.out.println("Invalid option. Please try again.");
            }
        }
        
        sc.close();
    }
    
    private static void printMenu() {
        System.out.println("\n===== STORE MANAGER =====");
        System.out.println("1. Add store");
        System.out.println("2. Add Product");
        System.out.println("3. Remove Product");
        System.out.println("4. Total sales by store type");
        System.out.println("5. Total sales by product category");
        System.out.println("6. Exit");
        System.out.print("Choose option (1-6): ");
    }
    
    private static void addStore(Scanner sc) {
        ArrayList<Store> stores = new ArrayList<>();

        System.out.println("Enter the path to the JSON file of the store:");
        String jsonPath = sc.nextLine();

        try (FileReader reader = new FileReader(jsonPath)) {
            JSONParser parser = new JSONParser();
            JSONArray jsonArray = (JSONArray) parser.parse(reader);

            for (Object obj : jsonArray) {
                JSONObject jsonObject = (JSONObject) obj;

                String name = (String) jsonObject.get("StoreName");
                double latitude = ((Number) jsonObject.get("Latitude")).doubleValue();
                double longitude = ((Number) jsonObject.get("Longitude")).doubleValue();
                String category = (String) jsonObject.get("FoodCategory");
                int stars = ((Number) jsonObject.get("Stars")).intValue();
                int reviews = ((Number) jsonObject.get("NoOfVotes")).intValue();
                String storeLogoPath = (String) jsonObject.get("StoreLogo");

                // Parse products
                ArrayList<Product> products = new ArrayList<>();
                JSONArray productsArray = (JSONArray) jsonObject.get("Products");
                for (Object prodObj : productsArray) {
                    JSONObject productJson = (JSONObject) prodObj;

                    String productName = (String) productJson.get("ProductName");
                    String productType = (String) productJson.get("ProductType");
                    int amount = ((Number) productJson.get("Available Amount")).intValue();
                    double productPrice = ((Number) productJson.get("Price")).doubleValue();

                    products.add(new Product(productName, productType, amount, productPrice));
                }

                Store s = new Store(name, latitude, longitude, category, stars, reviews, storeLogoPath, products);
                System.out.println(s);
                stores.add(s);
            }

            // Connect to the server and send the store(s)
            Socket requestSocket = null;
            ObjectOutputStream out = null;
            ObjectInputStream in = null;
            
            try {
                requestSocket = new Socket(SERVER_IP, SERVER_PORT);
                out = new ObjectOutputStream(requestSocket.getOutputStream());
                in = new ObjectInputStream(requestSocket.getInputStream());

                out.writeObject("manager");
                out.flush();

                out.writeObject(stores);
                out.flush();

                String response = (String) in.readObject();
                System.out.println("Server response: " + response);

            } catch (UnknownHostException e) {
                System.err.println("Unknown host: " + SERVER_IP);
            } catch (IOException | ClassNotFoundException e) {
                System.err.println("Error communicating with server: " + e.getMessage());
            } finally {
                closeConnections(requestSocket, out, in);
            }

        } catch (IOException e) {
            System.err.println("Error reading JSON file: " + e.getMessage());
        } catch (org.json.simple.parser.ParseException e) {
            System.err.println("Error parsing JSON: " + e.getMessage());
        }
    }
    
    private static void addProduct(Scanner sc) {
        String jsonPath = "store.json"; // Fixed JSON file path

        try (FileReader reader = new FileReader(jsonPath)) {
            JSONParser parser = new JSONParser();
            JSONArray jsonArray = (JSONArray) parser.parse(reader);

            System.out.println("Enter store name to add a product:");
            String storeName = sc.nextLine();
            Product product = null;
            boolean storeFound = false;

            for (Object obj : jsonArray) {
                JSONObject jsonObject = (JSONObject) obj;

                if (storeName.equalsIgnoreCase((String) jsonObject.get("StoreName"))) {
                    storeFound = true;
                    JSONArray productsArray = (JSONArray) jsonObject.get("Products");

                    System.out.println("Enter Product Name:");
                    String productName = sc.nextLine();
                    boolean productFound = false;

                    // Check if product already exists
                    for (Object productObj : productsArray) {
                        JSONObject productJson = (JSONObject) productObj;
                        String existingProductName = (String) productJson.get("ProductName");

                        if (productName.equalsIgnoreCase(existingProductName)) {
                            productFound = true;

                            System.out.println("Product already exists. How much would you like to add to the quantity?");
                            int additionalAmount = Integer.parseInt(sc.nextLine());
                            
                            int currentAmount = ((Number) productJson.get("Available Amount")).intValue();
                            double price = ((Number) productJson.get("Price")).doubleValue();
                            String productType = (String) productJson.get("ProductType");
                            
                            product = new Product(productName, productType, additionalAmount, price);
                            break;
                        }
                    }

                    if (!productFound) {
                        System.out.println("Enter Product Type:");
                        String productType = sc.nextLine();

                        System.out.println("Enter Available Amount:");
                        int amount = Integer.parseInt(sc.nextLine());

                        System.out.println("Enter Product Price:");
                        double productPrice = Double.parseDouble(sc.nextLine());

                        product = new Product(productName, productType, amount, productPrice);
                    }

                    // Send product to server
                    Socket requestSocket = null;
                    ObjectOutputStream out = null;
                    ObjectInputStream in = null;
                    
                    try {
                        requestSocket = new Socket(SERVER_IP, SERVER_PORT);
                        out = new ObjectOutputStream(requestSocket.getOutputStream());
                        in = new ObjectInputStream(requestSocket.getInputStream());

                        out.writeObject("product");
                        out.flush();

                        out.writeObject(storeName);
                        out.flush();

                        out.writeObject(product);
                        out.flush();

                        String response = (String) in.readObject();
                        System.out.println("Server response: " + response);

                    } catch (Exception e) {
                        System.err.println("Error communicating with server: " + e.getMessage());
                    } finally {
                        closeConnections(requestSocket, out, in);
                    }
                    
                    break;
                }
            }

            if (!storeFound) {
                System.out.println("Store not found.");
            }

        } catch (IOException e) {
            System.err.println("Error reading store.json: " + e.getMessage());
        } catch (org.json.simple.parser.ParseException e) {
            System.err.println("Error parsing JSON: " + e.getMessage());
        }
    }
    
    private static void removeProduct(Scanner sc) {
        String jsonPath = "store.json";
        Map<String, Integer> productQuantityMap = new HashMap<>();

        try (FileReader reader = new FileReader(jsonPath)) {
            JSONParser parser = new JSONParser();
            JSONArray jsonArray = (JSONArray) parser.parse(reader);

            System.out.println("Enter store name to remove a product:");
            String storeName = sc.nextLine();

            boolean storeFound = false;
            boolean productFound = false;

            for (Object obj : jsonArray) {
                JSONObject jsonObject = (JSONObject) obj;

                if (storeName.equalsIgnoreCase((String) jsonObject.get("StoreName"))) {
                    storeFound = true;
                    JSONArray productsArray = (JSONArray) jsonObject.get("Products");

                    System.out.println("Enter Product Name to remove:");
                    String productName = sc.nextLine();

                    System.out.println("1. Remove the product");
                    System.out.println("2. Decrease the quantity of the product");
                    System.out.print("Choose (1-2): ");
                    String option = sc.nextLine();

                    if (option.equals("1")) {
                        for (Object prodObj : productsArray) {
                            JSONObject productJson = (JSONObject) prodObj;

                            if (productName.equalsIgnoreCase((String) productJson.get("ProductName"))) {
                                productJson.put("Hidden", true);
                                productQuantityMap.put(productName, -1); // -1 code to hide
                                productFound = true;
                                break;
                            }
                        }
                    } else if (option.equals("2")) {
                        for (Object prodObj : productsArray) {
                            JSONObject productJson = (JSONObject) prodObj;

                            if (productName.equalsIgnoreCase((String) productJson.get("ProductName"))) {
                                productFound = true;
                                int currentQuantity = ((Number) productJson.get("Available Amount")).intValue();

                                System.out.println("Enter quantity to subtract:");
                                int subtractAmount = Integer.parseInt(sc.nextLine());

                                if (subtractAmount > currentQuantity) {
                                    System.out.println("Error: Not enough stock!");
                                    System.out.println("The available stock is: " + currentQuantity);
                                } else {
                                    int newQuantity = currentQuantity - subtractAmount;
                                    productJson.put("Available Amount", newQuantity);
                                    productQuantityMap.put(productName, newQuantity);
                                    System.out.println("Updated Quantity: " + newQuantity);
                                }
                                break;
                            }
                        }
                    }
                    
                    break;
                }
            }

            if (!storeFound) {
                System.out.println("Store not found.");
            } else if (!productFound) {
                System.out.println("Product not found.");
            } else {
                // Update JSON file
                try (FileWriter writer = new FileWriter(jsonPath)) {
                    writer.write(jsonArray.toJSONString());
                    writer.flush();
                    System.out.println("Changes saved to store.json.");
                }

                // Send update to server
                Socket requestSocket = null;
                ObjectOutputStream out = null;
                ObjectInputStream in = null;
                
                try {
                    requestSocket = new Socket(SERVER_IP, SERVER_PORT);
                    out = new ObjectOutputStream(requestSocket.getOutputStream());
                    in = new ObjectInputStream(requestSocket.getInputStream());

                    out.writeObject("remove");
                    out.flush();

                    out.writeObject(storeName);
                    out.flush();

                    out.writeObject(productQuantityMap);
                    out.flush();

                    String response = (String) in.readObject();
                    System.out.println("Server response: " + response);

                } catch (Exception e) {
                    System.err.println("Error communicating with server: " + e.getMessage());
                } finally {
                    closeConnections(requestSocket, out, in);
                }
            }

        } catch (IOException | org.json.simple.parser.ParseException e) {
            System.err.println("Error with JSON file: " + e.getMessage());
        }
    }
    
    private static void getSalesByStoreType(Scanner sc) {
        System.out.println("Enter the store type (e.g., pizzeria, burger):");
        String storeType = sc.nextLine();

        Socket requestSocket = null;
        ObjectOutputStream out = null;
        ObjectInputStream in = null;
        
        try {
            requestSocket = new Socket(SERVER_IP, SERVER_PORT);
            out = new ObjectOutputStream(requestSocket.getOutputStream());
            in = new ObjectInputStream(requestSocket.getInputStream());

            out.writeObject("storeType");
            out.flush();

            out.writeObject(storeType);
            out.flush();

            Map<String, Integer> result = (Map<String, Integer>) in.readObject();

            int total = 0;
            System.out.println("\nSales by Store for type: " + storeType);
            for (Map.Entry<String, Integer> entry : result.entrySet()) {
                if (!"total".equals(entry.getKey())) {  // Skip the "total" entry for our own calculation
                    System.out.println("• " + entry.getKey() + ": " + entry.getValue());
                    total += entry.getValue();
                }
            }
            System.out.println("Total Sales: " + total + "\n");

        } catch (Exception e) {
            System.err.println("Error communicating with server: " + e.getMessage());
        } finally {
            closeConnections(requestSocket, out, in);
        }
    }
    
    private static void getSalesByProductCategory(Scanner sc) {
        System.out.println("Enter the product category (e.g., pizza, salad, burger):");
        String productCategory = sc.nextLine();

        Socket requestSocket = null;
        ObjectOutputStream out = null;
        ObjectInputStream in = null;
        
        try {
            requestSocket = new Socket(SERVER_IP, SERVER_PORT);
            out = new ObjectOutputStream(requestSocket.getOutputStream());
            in = new ObjectInputStream(requestSocket.getInputStream());

            out.writeObject("productCategory");
            out.flush();

            out.writeObject(productCategory);
            out.flush();

            Map<String, Integer> result = (Map<String, Integer>) in.readObject();

            int total = 0;
            System.out.println("\nSales by Store for product category: " + productCategory);
            for (Map.Entry<String, Integer> entry : result.entrySet()) {
                if (!"total".equals(entry.getKey())) {  // Skip the "total" entry for our own calculation
                    System.out.println("• " + entry.getKey() + ": " + entry.getValue());
                    total += entry.getValue();
                }
            }
            System.out.println("Total Sales: " + total + "\n");

        } catch (Exception e) {
            System.err.println("Error communicating with server: " + e.getMessage());
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

// WorkerActions.java
import java.io.*;
import java.net.*;
import java.util.*;

public class WorkerActions extends Thread {
    ObjectInputStream in;
    ObjectOutputStream out;
    private final ArrayList<Store> stores;
    private final Object lock;
    private final Socket connection;

    public WorkerActions(Socket connection, ArrayList<Store> stores, Object lock) {
        this.connection = connection;
        this.stores = stores;
        this.lock = lock;
        try {
            out = new ObjectOutputStream(connection.getOutputStream());
            in = new ObjectInputStream(connection.getInputStream());
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void run() {
        try {
            String role = (String) in.readObject();

            if (role.equals("manager")) {
                // Λειτουργία προσθήκης καταστήματος (δεν αλλάζει)
                Store incomingStore = (Store) in.readObject();
                boolean storeExists = false;

                synchronized (lock) {
                    for (Store store : stores) {
                        if (store.getStoreName().equalsIgnoreCase(incomingStore.getStoreName())) {
                            storeExists = true;
                            break;
                        }
                    }

                    if (!storeExists) {
                        stores.add(incomingStore);
                        out.writeObject("Store(s) added successfully");
                    } else {
                        out.writeObject("Store already exists.");
                    }
                    out.flush();
                }
            } else if (role.equals("product")) {
                // Λειτουργία προσθήκης προϊόντος (δεν αλλάζει)
                String storeName = (String) in.readObject();
                Product newProduct = (Product) in.readObject();

                boolean storeFound = false;
                boolean productUpdated = false;

                synchronized (lock) {
                    for (Store store : stores) {
                        if (store.getStoreName().equalsIgnoreCase(storeName)) {
                            storeFound = true;

                            for (Product product : store.getProducts()) {
                                if (product.getName().equalsIgnoreCase(newProduct.getName())) {
                                    // Product exists: update quantity
                                    product.setQuantity(product.getQuantity() + newProduct.getQuantity());
                                    productUpdated = true;
                                    break;
                                }
                            }

                            if (!productUpdated) {
                                // Product does not exist: add new
                                store.getProducts().add(newProduct);
                            }

                            break; // exit after store is found and processed
                        }
                    }
                }

                if (!storeFound) {
                    out.writeObject("Store not found.");
                } else if (productUpdated) {
                    out.writeObject("Product quantity updated successfully.");
                } else {
                    out.writeObject("New product added successfully.");
                }
                out.flush();
            } else if (role.equals("remove")) {
                // Λειτουργία αφαίρεσης προϊόντος (δεν αλλάζει)
                String storeName = (String) in.readObject();
                Map<String, Integer> productQuantityMap = (Map<String, Integer>) in.readObject();
                boolean storeFound = false;

                synchronized (lock) {
                    for (Store store : stores) {
                        if (store.getStoreName().equalsIgnoreCase(storeName)) {
                            storeFound = true;

                            for (Product product : store.getProducts()) {
                                String productName = product.getName();

                                if (productQuantityMap.containsKey(productName)) {
                                    int newQty = productQuantityMap.get(productName);

                                    if (newQty == -1) {
                                        product.setStatus("hidden");
                                    } else {
                                        product.setQuantity(newQty);
                                        product.setStatus("visible");
                                    }
                                }
                            }
                            break;
                        }
                    }
                }

                if (storeFound) {
                    out.writeObject("Product(s) removed or updated successfully.");
                } else {
                    out.writeObject("Store not found.");
                }
                out.flush();
            } else if (role.equals("storeType")) {
                // Υλοποίηση MapReduce για συνολικές πωλήσεις ανά τύπο καταστήματος
                String requestedType = (String) in.readObject();
                
                // Εκτέλεση της Map φάσης (αντιστοίχιση καταστημάτων με τον τύπο με τις πωλήσεις τους)
                Map<String, Integer> mappedResults = mapStoreType(requestedType);
                
                // Αποστολή των αποτελεσμάτων της Map φάσης στο Master
                out.writeObject(mappedResults);
                out.flush();
            } else if (role.equals("productCategory")) {
                // Υλοποίηση MapReduce για συνολικές πωλήσεις ανά κατηγορία προϊόντος
                String requestedCategory = (String) in.readObject();
                
                // Εκτέλεση της Map φάσης (αντιστοίχιση καταστημάτων με πωλήσεις ανά κατηγορία προϊόντος)
                Map<String, Integer> mappedResults = mapProductCategory(requestedCategory);
                
                // Αποστολή των αποτελεσμάτων της Map φάσης στο Master
                out.writeObject(mappedResults);
                out.flush();
            } else if (role.equals("client") || role.equals("filter")) {
                // Υλοποίηση MapReduce για αναζήτηση καταστημάτων
                MapReduceRequest request = (MapReduceRequest) in.readObject();
                
                // Εκτέλεση της Map φάσης (φιλτράρισμα καταστημάτων βάσει κριτηρίων)
                List<Store> mappedResults = mapStores(request);
                
                // Αποστολή των αποτελεσμάτων της Map φάσης στο Master
                out.writeObject(new ArrayList<>(mappedResults));
                out.flush();
                
                // Προαιρετική επιβεβαίωση από τον client
                try {
                    String confirm = (String) in.readObject();
                    if (!"Done".equalsIgnoreCase(confirm)) {
                        System.out.println("Client did not confirm receipt of results.");
                    }
                } catch (IOException | ClassNotFoundException e) {
                    // Διαχείριση εξαίρεσης - ο client μπορεί να μην στείλει επιβεβαίωση
                }
            } else if (role.equals("fetchProducts")) {
                // Ανάκτηση προϊόντων για συγκεκριμένο κατάστημα
                String storeName = (String) in.readObject();
                ArrayList<Product> available = fetchAvailableProducts(storeName);
                out.writeObject(available);
                out.flush();
            } else if (role.equals("purchase")) {
                // Λειτουργία αγοράς
                Purchase purchase = (Purchase) in.readObject();
                processPurchase(purchase);
            } else if (role.equals("rate")) {
                // Λειτουργία αξιολόγησης
                String storeName = (String) in.readObject();
                int rating = (int) in.readObject();
                processRating(storeName, rating);
            }
        } catch (IOException | ClassNotFoundException e) {
            e.printStackTrace();
        } finally {
            try {
                if (in != null) in.close();
                if (out != null) out.close();
                if (connection != null && !connection.isClosed()) connection.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    /**
     * Map λειτουργία: Αντιστοίχιση καταστημάτων του συγκεκριμένου τύπου με τις πωλήσεις τους
     * 
     * @param storeType Ο τύπος καταστήματος (π.χ. "pizzeria")
     * @return Ένα Map που αντιστοιχίζει ονόματα καταστημάτων με το πλήθος πωλήσεων
     */
    private Map<String, Integer> mapStoreType(String storeType) {
        Map<String, Integer> result = new HashMap<>();
        
        synchronized (lock) {
            for (Store store : stores) {
                if (store.getCategory().equalsIgnoreCase(storeType)) {
                    // Για κάθε κατάστημα του ζητούμενου τύπου, υπολογίζουμε τις συνολικές πωλήσεις
                    int totalSold = 0;
                    
                    for (Purchase purchase : store.getPurchases()) {
                        for (Product p : purchase.getPurchasedProducts()) {
                            totalSold += p.getQuantity();  // Άθροισμα ποσοτήτων
                        }
                    }
                    
                    // Αντιστοίχιση ονόματος καταστήματος (κλειδί) με πωλήσεις (τιμή)
                    result.put(store.getStoreName(), totalSold);
                }
            }
        }
        
        return result;
    }
    
    /**
     * Map λειτουργία: Αντιστοίχιση καταστημάτων με πωλήσεις για συγκεκριμένη κατηγορία προϊόντος
     * 
     * @param productCategory Η κατηγορία προϊόντος (π.χ. "pizza")
     * @return Ένα Map που αντιστοιχίζει ονόματα καταστημάτων με το πλήθος πωλήσεων
     */
    private Map<String, Integer> mapProductCategory(String productCategory) {
        Map<String, Integer> result = new HashMap<>();
        
        synchronized (lock) {
            for (Store store : stores) {
                int totalCategorySales = 0;
                
                for (Purchase purchase : store.getPurchases()) {
                    for (Product product : purchase.getPurchasedProducts()) {
                        if (product.getCategory().equalsIgnoreCase(productCategory)) {
                            totalCategorySales += product.getQuantity();
                        }
                    }
                }
                
                if (totalCategorySales > 0) {
                    // Αντιστοίχιση ονόματος καταστήματος (κλειδί) με πωλήσεις συγκεκριμένης κατηγορίας (τιμή)
                    result.put(store.getStoreName(), totalCategorySales);
                }
            }
        }
        
        return result;
    }
    
    /**
     * Map λειτουργία: Φιλτράρισμα καταστημάτων βάσει κριτηρίων αναζήτησης
     * 
     * @param request Το αίτημα που περιέχει τα κριτήρια αναζήτησης
     * @return Λίστα καταστημάτων που ικανοποιούν τα κριτήρια
     */
    private List<Store> mapStores(MapReduceRequest request) {
        List<Store> result = new ArrayList<>();
        
        double userLat = request.getClientLatitude();
        double userLon = request.getClientLongitude();
        double radius = request.getRadius();
        
        ArrayList<String> categories = (ArrayList<String>) request.getFoodCategories();  
        int minStars = request.getMinStars();                  
        String price = request.getPriceCategory();             
        
        synchronized (lock) {
            for (Store store : stores) {
                // Υπολογισμός απόστασης με τον τύπο της Ευκλείδειας απόστασης
                double distance = Math.sqrt(Math.pow(userLat - store.getLatitude(), 2) + 
                                           Math.pow(userLon - store.getLongitude(), 2));
                
                // Έλεγχος αν το κατάστημα ικανοποιεί όλα τα κριτήρια
                boolean matchesDistance = distance <= radius;
                boolean matchesCategory = categories.isEmpty() || categories.contains(store.getCategory());
                boolean matchesStars = minStars == 0 || store.getStars() >= minStars;
                boolean matchesPrice = price.isEmpty() || store.calculatePriceCategory().equalsIgnoreCase(price);
                
                if (matchesDistance && matchesCategory && matchesStars && matchesPrice) {
                    result.add(store);
                }
            }
        }
        
        return result;
    }
    
    /**
     * Ανάκτηση διαθέσιμων προϊόντων για ένα συγκεκριμένο κατάστημα
     * 
     * @param storeName Το όνομα του καταστήματος
     * @return Λίστα με τα διαθέσιμα προϊόντα
     */
    private ArrayList<Product> fetchAvailableProducts(String storeName) {
        ArrayList<Product> available = new ArrayList<>();
        
        synchronized (lock) {
            for (Store store : stores) {
                if (store.getStoreName().equalsIgnoreCase(storeName)) {
                    for (Product product : store.getProducts()) {
                        if (product.getStatus().equalsIgnoreCase("visible") && product.getQuantity() > 0) {
                            available.add(product);
                        }
                    }
                    break;
                }
            }
        }
        
        return available;
    }
    
    /**
     * Επεξεργασία αγοράς
     * 
     * @param purchase Το αντικείμενο αγοράς
     */
    private void processPurchase(Purchase purchase) throws IOException {
        ArrayList<Product> requestedProducts = purchase.getPurchasedProducts();
        boolean storeFound = false;
        String storeName = "";
        
        synchronized (lock) {
            for (Store store : stores) {
                Map<String, Product> storeProductMap = new HashMap<>();
                for (Product p : store.getProducts()) {
                    storeProductMap.put(p.getName().toLowerCase(), p);
                }
                
                boolean allMatch = true;
                for (Product req : requestedProducts) {
                    Product available = storeProductMap.get(req.getName().toLowerCase());
                    if (available == null || available.getQuantity() < req.getQuantity() || 
                        !available.getStatus().equalsIgnoreCase("visible")) {
                        allMatch = false;
                        break;
                    }
                }
                
                if (allMatch) {
                    for (Product req : requestedProducts) {
                        Product prod = storeProductMap.get(req.getName().toLowerCase());
                        prod.setQuantity(prod.getQuantity() - req.getQuantity());
                        
                        // Συμπλήρωση κατηγορίας/τιμής (άδεια από τον client)
                        req.setCategory(prod.getCategory());
                        req.setPrice(prod.getPrice());
                    }
                    
                    store.getPurchases().add(purchase);
                    storeName = store.getStoreName();
                    storeFound = true;
                    break;
                }
            }
        }
        
        if (!storeFound) {
            out.writeObject("Purchase failed: No store has all requested items in sufficient quantity.");
        } else {
            out.writeObject("Purchase completed successfully at store: " + storeName);
        }
        
        out.flush();
    }
    
    /**
     * Επεξεργασία αξιολόγησης καταστήματος
     * 
     * @param storeName Το όνομα του καταστήματος
     * @param rating Η αξιολόγηση (1-5)
     */
    private void processRating(String storeName, int rating) throws IOException {
        boolean storeFound = false;
        
        synchronized (lock) {
            for (Store store : stores) {
                if (store.getStoreName().equalsIgnoreCase(storeName)) {
                    int oldStars = store.getStars();
                    int oldReviews = store.getNoOfReviews();
                    
                    int newReviews = oldReviews + 1;
                    int newAvg = (int) Math.round((oldStars * oldReviews + rating) / (double) newReviews);
                    
                    store.setStars(newAvg);
                    store.setNoOfReviews(newReviews);
                    
                    storeFound = true;
                    break;
                }
            }
        }
        
        if (storeFound) {
            out.writeObject("Rating submitted successfully.");
        } else {
            out.writeObject("Store not found.");
        }
        
        out.flush();
    }
}
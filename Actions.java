// Actions.java
import java.io.*;
import java.net.*;
import java.text.ParseException;
import java.util.*;

public class Actions extends Thread {
    ObjectInputStream in;
    ObjectOutputStream out;
    String[][] workers;
    HashMap<Integer, ObjectOutputStream> connectionsOut;
    int counterID;

    public Actions(Socket connection, String[][] workers, HashMap<Integer, ObjectOutputStream> connectionsOut, int counterID) {
        try {
            out = new ObjectOutputStream(connection.getOutputStream());
            in = new ObjectInputStream(connection.getInputStream());
            this.workers = workers;
            this.connectionsOut = connectionsOut;
            this.counterID = counterID;
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void run() {
        try {
            String role = (String) in.readObject();

            if (role.equals("manager")) {
                // Διαχείριση αιτήματος προσθήκης καταστήματος (δεν αλλάζει)
                ArrayList<Store> stores = (ArrayList<Store>) in.readObject();
                int successCount = 0;

                for (Store store : stores) {
                    Socket workerSocket = null;
                    ObjectOutputStream outWorker = null;
                    ObjectInputStream inWorker = null;

                    try {
                        // Hash-based assignment to worker
                        int workerId = Math.abs(store.getStoreName().hashCode()) % workers.length;
                        String workerIP = workers[workerId][0];
                        int workerPort = Integer.parseInt(workers[workerId][1]);

                        workerSocket = new Socket(workerIP, workerPort);
                        outWorker = new ObjectOutputStream(workerSocket.getOutputStream());
                        inWorker = new ObjectInputStream(workerSocket.getInputStream());

                        outWorker.writeObject("manager");
                        outWorker.flush();

                        outWorker.writeObject(store);
                        outWorker.flush();

                        String response = (String) inWorker.readObject();
                        if ("Store(s) added successfully".equals(response)) {
                            successCount++;
                        }

                    } catch (IOException | ClassNotFoundException e) {
                        e.printStackTrace();
                    } finally {
                        try {
                            if (inWorker != null) inWorker.close();
                            if (outWorker != null) outWorker.close();
                            if (workerSocket != null) workerSocket.close();
                        } catch (IOException e) {
                            e.printStackTrace();
                        }
                    }
                }

                if (successCount == stores.size()) {
                    out.writeObject("All stores added successfully");
                } else {
                    out.writeObject("Some stores failed to add");
                }
                out.flush();

            } else if (role.equals("product")) {
                // Διαχείριση αιτήματος προσθήκης προϊόντος (δεν αλλάζει)
                String storeName = (String) in.readObject();
                Product product = (Product) in.readObject();

                Socket workerSocket = null;
                ObjectOutputStream outWorker = null;
                ObjectInputStream inWorker = null;

                try {
                    int workerId = Math.abs(storeName.hashCode()) % workers.length;
                    String workerIP = workers[workerId][0];
                    int workerPort = Integer.parseInt(workers[workerId][1]);

                    workerSocket = new Socket(workerIP, workerPort);
                    outWorker = new ObjectOutputStream(workerSocket.getOutputStream());
                    inWorker = new ObjectInputStream(workerSocket.getInputStream());

                    outWorker.writeObject("product");
                    outWorker.flush();

                    outWorker.writeObject(storeName);
                    outWorker.flush();

                    outWorker.writeObject(product);
                    outWorker.flush();

                    String response = (String) inWorker.readObject();
                    out.writeObject(response);
                    out.flush();

                } catch (IOException | ClassNotFoundException e) {
                    e.printStackTrace();
                } finally {
                    try {
                        if (inWorker != null) inWorker.close();
                        if (outWorker != null) outWorker.close();
                        if (workerSocket != null) workerSocket.close();
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }

            } else if (role.equals("remove")) {
                // Διαχείριση αιτήματος αφαίρεσης προϊόντος (δεν αλλάζει)
                String storeName = (String) in.readObject();
                Map<String, Integer> productUpdates = (Map<String, Integer>) in.readObject();

                Socket workerSocket = null;
                ObjectOutputStream outWorker = null;
                ObjectInputStream inWorker = null;

                try {
                    int workerId = Math.abs(storeName.hashCode()) % workers.length;
                    String workerIP = workers[workerId][0];
                    int workerPort = Integer.parseInt(workers[workerId][1]);

                    workerSocket = new Socket(workerIP, workerPort);
                    outWorker = new ObjectOutputStream(workerSocket.getOutputStream());
                    inWorker = new ObjectInputStream(workerSocket.getInputStream());

                    outWorker.writeObject("remove");
                    outWorker.flush();

                    outWorker.writeObject(storeName);
                    outWorker.flush();

                    outWorker.writeObject(productUpdates);
                    outWorker.flush();

                    String response = (String) inWorker.readObject();
                    out.writeObject(response);
                    out.flush();

                } catch (IOException | ClassNotFoundException e) {
                    e.printStackTrace();
                } finally {
                    try {
                        if (inWorker != null) inWorker.close();
                        if (outWorker != null) outWorker.close();
                        if (workerSocket != null) workerSocket.close();
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }

            } else if (role.equals("storeType")) {
                // Υλοποίηση MapReduce για αναζήτηση πωλήσεων ανά τύπο καταστήματος
                String storeType = (String) in.readObject();
                
                // Map φάση: Συλλογή των αποτελεσμάτων από κάθε worker
                Map<String, List<Integer>> mappedResults = new HashMap<>();
                
                for (int i = 0; i < workers.length; i++) {
                    Socket workerSocket = null;
                    ObjectOutputStream outWorker = null;
                    ObjectInputStream inWorker = null;

                    try {
                        String workerIP = workers[i][0];
                        int workerPort = Integer.parseInt(workers[i][1]);

                        workerSocket = new Socket(workerIP, workerPort);
                        outWorker = new ObjectOutputStream(workerSocket.getOutputStream());
                        inWorker = new ObjectInputStream(workerSocket.getInputStream());

                        outWorker.writeObject("storeType");
                        outWorker.flush();

                        outWorker.writeObject(storeType);
                        outWorker.flush();

                        Map<String, Integer> partialResult = (Map<String, Integer>) inWorker.readObject();
                        
                        // Αποθήκευση των αποτελεσμάτων σε μια δομή για τη φάση Reduce
                        for (Map.Entry<String, Integer> entry : partialResult.entrySet()) {
                            String storeName = entry.getKey();
                            int sales = entry.getValue();
                            
                            if (!mappedResults.containsKey(storeName)) {
                                mappedResults.put(storeName, new ArrayList<>());
                            }
                            mappedResults.get(storeName).add(sales);
                        }

                    } catch (IOException | ClassNotFoundException e) {
                        e.printStackTrace();
                    } finally {
                        try {
                            if (inWorker != null) inWorker.close();
                            if (outWorker != null) outWorker.close();
                            if (workerSocket != null) workerSocket.close();
                        } catch (IOException e) {
                            e.printStackTrace();
                        }
                    }
                }
                
                // Reduce φάση: Συγχώνευση των αποτελεσμάτων ανά κατάστημα
                Map<String, Integer> finalResult = reduceStoreResults(mappedResults);
                
                // Αποστολή των τελικών αποτελεσμάτων
                out.writeObject(finalResult);
                out.flush();

            } else if (role.equals("productCategory")) {
                // Υλοποίηση MapReduce για αναζήτηση πωλήσεων ανά κατηγορία προϊόντος
                String productCategory = (String) in.readObject();
                
                // Map φάση: Συλλογή των αποτελεσμάτων από κάθε worker
                Map<String, List<Integer>> mappedResults = new HashMap<>();

                for (int i = 0; i < workers.length; i++) {
                    Socket workerSocket = null;
                    ObjectOutputStream outWorker = null;
                    ObjectInputStream inWorker = null;

                    try {
                        String workerIP = workers[i][0];
                        int workerPort = Integer.parseInt(workers[i][1]);

                        workerSocket = new Socket(workerIP, workerPort);
                        outWorker = new ObjectOutputStream(workerSocket.getOutputStream());
                        inWorker = new ObjectInputStream(workerSocket.getInputStream());

                        outWorker.writeObject("productCategory");
                        outWorker.flush();

                        outWorker.writeObject(productCategory);
                        outWorker.flush();

                        Map<String, Integer> partialResult = (Map<String, Integer>) inWorker.readObject();
                        
                        // Αποθήκευση των αποτελεσμάτων σε μια δομή για τη φάση Reduce
                        for (Map.Entry<String, Integer> entry : partialResult.entrySet()) {
                            String storeName = entry.getKey();
                            int sales = entry.getValue();
                            
                            if (!mappedResults.containsKey(storeName)) {
                                mappedResults.put(storeName, new ArrayList<>());
                            }
                            mappedResults.get(storeName).add(sales);
                        }

                    } catch (IOException | ClassNotFoundException e) {
                        e.printStackTrace();
                    } finally {
                        try {
                            if (inWorker != null) inWorker.close();
                            if (outWorker != null) outWorker.close();
                            if (workerSocket != null) workerSocket.close();
                        } catch (IOException e) {
                            e.printStackTrace();
                        }
                    }
                }
                
                // Reduce φάση: Συγχώνευση των αποτελεσμάτων ανά κατάστημα
                Map<String, Integer> finalResult = reduceStoreResults(mappedResults);
                
                // Αποστολή των τελικών αποτελεσμάτων
                out.writeObject(finalResult);
                out.flush();

            } else if (role.equals("client") || role.equals("filter")) {
                // Υλοποίηση MapReduce για αναζήτηση καταστημάτων
                MapReduceRequest request = (MapReduceRequest) in.readObject();
                
                // Map φάση: Συλλογή των αποτελεσμάτων από κάθε worker
                List<List<Store>> mappedResults = new ArrayList<>();
                
                for (int i = 0; i < workers.length; i++) {
                    Socket workerSocket = null;
                    ObjectOutputStream outWorker = null;
                    ObjectInputStream inWorker = null;

                    try {
                        String workerIP = workers[i][0];
                        int workerPort = Integer.parseInt(workers[i][1]);

                        workerSocket = new Socket(workerIP, workerPort);
                        outWorker = new ObjectOutputStream(workerSocket.getOutputStream());
                        inWorker = new ObjectInputStream(workerSocket.getInputStream());

                        // Αποστολή του τύπου αιτήματος και των παραμέτρων
                        outWorker.writeObject(role);  // Χρησιμοποιούμε το role για να διαφοροποιήσουμε client/filter
                        outWorker.flush();

                        outWorker.writeObject(request);
                        outWorker.flush();

                        // Λήψη των αποτελεσμάτων από τον worker
                        ArrayList<Store> workerResults = (ArrayList<Store>) inWorker.readObject();
                        mappedResults.add(workerResults);

                    } catch (IOException | ClassNotFoundException e) {
                        e.printStackTrace();
                    } finally {
                        try {
                            if (inWorker != null) inWorker.close();
                            if (outWorker != null) outWorker.close();
                            if (workerSocket != null) workerSocket.close();
                        } catch (IOException e) {
                            e.printStackTrace();
                        }
                    }
                }
                
                // Reduce φάση: Συγχώνευση των αποτελεσμάτων
                ArrayList<Store> finalResult = reduceStoreList(mappedResults);
                
                // Αποστολή των τελικών αποτελεσμάτων
                out.writeObject(finalResult);
                out.flush();

            } else if (role.equals("fetchProducts")) {
                // Υλοποίηση ανάκτησης προϊόντων για ένα συγκεκριμένο κατάστημα
                String storeName = (String) in.readObject();
                
                // Εύρεση του worker που διαχειρίζεται αυτό το κατάστημα
                int workerId = Math.abs(storeName.hashCode()) % workers.length;
                String workerIP = workers[workerId][0];
                int workerPort = Integer.parseInt(workers[workerId][1]);
                
                Socket workerSocket = null;
                ObjectOutputStream outWorker = null;
                ObjectInputStream inWorker = null;
                
                try {
                    workerSocket = new Socket(workerIP, workerPort);
                    outWorker = new ObjectOutputStream(workerSocket.getOutputStream());
                    inWorker = new ObjectInputStream(workerSocket.getInputStream());

                    outWorker.writeObject("fetchProducts");
                    outWorker.flush();

                    outWorker.writeObject(storeName);
                    outWorker.flush();

                    ArrayList<Product> results = (ArrayList<Product>) inWorker.readObject();
                    
                    // Αποστολή των αποτελεσμάτων στον client
                    out.writeObject(results);
                    out.flush();

                } catch (IOException | ClassNotFoundException e) {
                    e.printStackTrace();
                    // Σε περίπτωση σφάλματος, αποστέλλουμε μια κενή λίστα
                    out.writeObject(new ArrayList<Product>());
                    out.flush();
                } finally {
                    try {
                        if (inWorker != null) inWorker.close();
                        if (outWorker != null) outWorker.close();
                        if (workerSocket != null) workerSocket.close();
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }

            } else if (role.equals("purchase")) {
                // Υλοποίηση αγοράς προϊόντων
                Purchase pur = (Purchase) in.readObject();
                
                // Δοκιμάζουμε να αποστείλουμε την αγορά σε όλους τους workers
                // Μόνο αυτός που διαχειρίζεται τα σχετικά προϊόντα θα την επεξεργαστεί επιτυχώς
                String finalResult = null;
                boolean purchaseSucceeded = false;
                
                for (int i = 0; i < workers.length; i++) {
                    Socket workerSocket = null;
                    ObjectOutputStream outWorker = null;
                    ObjectInputStream inWorker = null;

                    try {
                        String workerIP = workers[i][0];
                        int workerPort = Integer.parseInt(workers[i][1]);

                        workerSocket = new Socket(workerIP, workerPort);
                        outWorker = new ObjectOutputStream(workerSocket.getOutputStream());
                        inWorker = new ObjectInputStream(workerSocket.getInputStream());

                        outWorker.writeObject("purchase");
                        outWorker.flush();

                        outWorker.writeObject(pur);
                        outWorker.flush();

                        String result = (String) inWorker.readObject();
                        
                        if (result.contains("successfully") || !result.contains("failed")) {
                            finalResult = result;
                            purchaseSucceeded = true;
                            break;
                        } else if (finalResult == null) {
                            finalResult = result;
                        }

                    } catch (IOException | ClassNotFoundException e) {
                        e.printStackTrace();
                    } finally {
                        try {
                            if (inWorker != null) inWorker.close();
                            if (outWorker != null) outWorker.close();
                            if (workerSocket != null) workerSocket.close();
                        } catch (IOException e) {
                            e.printStackTrace();
                        }
                    }
                }
                
                if (!purchaseSucceeded && finalResult == null) {
                    finalResult = "Purchase failed: Could not process the purchase with any store.";
                }
                
                out.writeObject(finalResult);
                out.flush();

            } else if (role.equals("rate")) {
                // Υλοποίηση αξιολόγησης καταστήματος
                String store = (String) in.readObject();
                int rating = (int) in.readObject();
                
                // Εύρεση του worker που διαχειρίζεται αυτό το κατάστημα
                int workerId = Math.abs(store.hashCode()) % workers.length;
                String workerIP = workers[workerId][0];
                int workerPort = Integer.parseInt(workers[workerId][1]);
                
                Socket workerSocket = null;
                ObjectOutputStream outWorker = null;
                ObjectInputStream inWorker = null;
                
                try {
                    workerSocket = new Socket(workerIP, workerPort);
                    outWorker = new ObjectOutputStream(workerSocket.getOutputStream());
                    inWorker = new ObjectInputStream(workerSocket.getInputStream());
                    
                    outWorker.writeObject("rate");
                    outWorker.flush();
                    
                    outWorker.writeObject(store);
                    outWorker.flush();
                    
                    outWorker.writeObject(rating);
                    outWorker.flush();
                    
                    String result = (String) inWorker.readObject();
                    out.writeObject(result);
                    out.flush();
                    
                } catch (IOException | ClassNotFoundException e) {
                    e.printStackTrace();
                    out.writeObject("Rating failed due to a connection error.");
                    out.flush();
                } finally {
                    try {
                        if (inWorker != null) inWorker.close();
                        if (outWorker != null) outWorker.close();
                        if (workerSocket != null) workerSocket.close();
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
            }

        } catch (IOException | ClassNotFoundException e) {
            e.printStackTrace();
        } finally {
            try {
                in.close();
                out.close();
            } catch (IOException ioException) {
                ioException.printStackTrace();
            }
        }
    }
    
    /**
     * Reduce λειτουργία: Συγχώνευση των αποτελεσμάτων πωλήσεων ανά κατάστημα
     * 
     * @param mappedResults Map με ονόματα καταστημάτων και λίστες από τιμές πωλήσεων
     * @return Map με τα συγχωνευμένα αποτελέσματα
     */
    private Map<String, Integer> reduceStoreResults(Map<String, List<Integer>> mappedResults) {
        Map<String, Integer> reducedResult = new HashMap<>();
        
        // Για κάθε κατάστημα, αθροίζουμε όλες τις τιμές πωλήσεων
        for (Map.Entry<String, List<Integer>> entry : mappedResults.entrySet()) {
            String storeName = entry.getKey();
            List<Integer> salesValues = entry.getValue();
            
            // Reduce: Άθροισμα όλων των τιμών για το συγκεκριμένο κατάστημα
            int totalSales = 0;
            for (int sales : salesValues) {
                totalSales += sales;
            }
            
            reducedResult.put(storeName, totalSales);
        }
        
        return reducedResult;
    }
    
    /**
     * Reduce λειτουργία: Συγχώνευση των λιστών καταστημάτων
     * 
     * @param mappedResults Λίστα από λίστες καταστημάτων
     * @return Συγχωνευμένη λίστα καταστημάτων χωρίς διπλότυπα
     */
    private ArrayList<Store> reduceStoreList(List<List<Store>> mappedResults) {
        Set<String> processedStoreNames = new HashSet<>();
        ArrayList<Store> reducedResult = new ArrayList<>();
        
        // Συγχώνευση όλων των αποτελεσμάτων, αποφεύγοντας τα διπλότυπα
        for (List<Store> workerResult : mappedResults) {
            for (Store store : workerResult) {
                if (!processedStoreNames.contains(store.getStoreName())) {
                    processedStoreNames.add(store.getStoreName());
                    reducedResult.add(store);
                }
            }
        }
        
        return reducedResult;
    }
}
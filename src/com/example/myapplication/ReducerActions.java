package com.example.myapplication;
import java.io.*;
import java.net.*;
import java.util.*;

public class ReducerActions extends Thread {
    private ObjectInputStream in;
    private ObjectOutputStream out;
    private Socket connection;





    public ReducerActions(Socket connection) {
        try {
            this.connection = connection;
            out = new ObjectOutputStream(connection.getOutputStream());
            in = new ObjectInputStream(connection.getInputStream());
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void run() {
        try {
            String role = (String) in.readObject();

            if (role.equals("client") || role.equals("filter")) {
                // Receive from master
                String overallClientId = (String) in.readObject();
                int totalBatches = (int) in.readObject(); // This will be 1 from the modified Master

                ArrayList<Store> merged = new ArrayList<>();
                Set<String> addedNames = new HashSet<>();

                for (int i = 0; i < totalBatches; i++) { // Loop will run once
                    // Receive from master
                    String requestIdFromMaster = (String) in.readObject(); // This is the clientId sent by Master for the batch
                    ArrayList<Store> storesInBatch = (ArrayList<Store>) in.readObject(); // This is the allStoresFromWorkers list

                    if (!(overallClientId.equals(requestIdFromMaster))) {
                        System.out.println("[Reducer] Mismatch: overallClientId (" + overallClientId + ") != requestIdFromMaster (" + requestIdFromMaster + "). Skipping batch.");
                        continue;
                    }
                    for (Store store : storesInBatch) { // merge all the stores from the batch
                        if (!addedNames.contains(store.getStoreName())) {
                            merged.add(store);
                            addedNames.add(store.getStoreName());
                        }
                    }
                }

                // Send to master
                out.writeObject(overallClientId);
                out.flush();
                out.writeObject(merged);
                out.flush();

            } else if (role.equals("storeType")) {
                // Receive from master
                int totalWorkers = (int) in.readObject();

                Map<String, Integer> merged = new HashMap<>();

                for (int i = 0; i < totalWorkers; i++) { // for all workers
                    // Receive from master
                    Map<String, Integer> partial = (Map<String, Integer>) in.readObject();

                    for (Map.Entry<String, Integer> entry : partial.entrySet()) { // merge all the partial results
                        merged.merge(entry.getKey(), entry.getValue(), Integer::sum);
                    }
                }

                // Send to master
                out.writeObject(merged);
                out.flush();


            }else if (role.equals("productCategory")) {
                // Receive from master
                int totalWorkers = (int) in.readObject();

                Map<String, Integer> merged = new HashMap<>();

                for (int i = 0; i < totalWorkers; i++) { // for all workers
                    // Receive from master
                    Map<String, Integer> partial = (Map<String, Integer>) in.readObject();

                    for (Map.Entry<String, Integer> entry : partial.entrySet()) { // merge all the partial results
                        merged.merge(entry.getKey(), entry.getValue(), Integer::sum);
                    }
                }

                // Send to master
                out.writeObject(merged);
                out.flush();
            }


        } catch (Exception e) {
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
}

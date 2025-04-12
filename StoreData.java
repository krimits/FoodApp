// StoreData.java
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;

import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class StoreData {
    /**
     * Parses a JSON file containing store information and returns a Store object
     * 
     * @param jsonFilePath Path to the JSON file
     * @return Store object populated with data from the JSON file
     * @throws IOException If the file cannot be read
     * @throws ParseException If the JSON is invalid
     */
    public static Store parseStoreJson(String jsonFilePath) throws IOException, ParseException {
        // Initialize JSON Parser
        JSONParser parser = new JSONParser();

        // Read and parse the JSON file
        try (FileReader reader = new FileReader(jsonFilePath)) {
            JSONObject jsonObject = (JSONObject) parser.parse(reader);

            // Extract Store details
            String storeName = (String) jsonObject.get("StoreName");
            double latitude = ((Number) jsonObject.get("Latitude")).doubleValue();
            double longitude = ((Number) jsonObject.get("Longitude")).doubleValue();
            String foodCategory = (String) jsonObject.get("FoodCategory");
            int stars = ((Number) jsonObject.get("Stars")).intValue();
            int noOfVotes = ((Number) jsonObject.get("NoOfVotes")).intValue();
            String storeLogoPath = (String) jsonObject.get("StoreLogo");

            // Extract Product details
            JSONArray productArray = (JSONArray) jsonObject.get("Products");
            ArrayList<Product> products = new ArrayList<>();
            for (Object productObj : productArray) {
                JSONObject productJson = (JSONObject) productObj;

                String productName = (String) productJson.get("ProductName");
                String productType = (String) productJson.get("ProductType");
                int availableAmount = ((Number) productJson.get("Available Amount")).intValue();
                double price = ((Number) productJson.get("Price")).doubleValue();

                Product product = new Product(productName, productType, availableAmount, price);
                products.add(product);
            }

            // Create and return the Store object
            return new Store(storeName, latitude, longitude, foodCategory, stars, noOfVotes, storeLogoPath, products);
        }
    }

    /**
     * Calculates the price category of a store based on its products
     * 
     * @param products List of products
     * @return Price category as a string ($, $$, or $$$)
     */
    public static String calculatePriceCategory(List<Product> products) {
        if (products == null || products.isEmpty()) {
            return "$";  // Default for empty list
        }
        
        double totalPrice = 0;
        int visibleProductCount = 0;
        
        for (Product product : products) {
            if (product.getStatus().equalsIgnoreCase("visible")) {
                totalPrice += product.getPrice();
                visibleProductCount++;
            }
        }
        
        if (visibleProductCount == 0) {
            return "$";  // Default if no visible products
        }
        
        double averagePrice = totalPrice / visibleProductCount;

        if (averagePrice <= 5) {
            return "$";
        } else if (averagePrice <= 15) {
            return "$$";
        } else {
            return "$$$";
        }
    }
}
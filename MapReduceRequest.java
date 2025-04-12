// MapReduceRequest.java
import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

public class MapReduceRequest implements Serializable {
    private static final long serialVersionUID = 1L;

    private double clientLatitude;
    private double clientLongitude;
    private ArrayList<String> foodCategories;
    private int minStars;
    private String priceCategory;
    private double radius;


    public MapReduceRequest() {
        // Default constructor with no parameters
        this.foodCategories = new ArrayList<>();
    }


    public MapReduceRequest(double clientLatitude, double clientLongitude, ArrayList<String> foodCategories, int minStars, String priceCategory, double radius) {
        this.clientLatitude = clientLatitude;
        this.clientLongitude = clientLongitude;
        this.foodCategories = foodCategories != null ? foodCategories : new ArrayList<>();
        this.minStars = minStars;
        this.priceCategory = priceCategory != null ? priceCategory : "";
        this.radius = radius > 0 ? radius : 5.0; // Default radius is 5km
    }


    public double getClientLatitude() {
        return clientLatitude;
    }

    public void setClientLatitude(double clientLatitude) {
        this.clientLatitude = clientLatitude;
    }

    public double getClientLongitude() {
        return clientLongitude;
    }

    public void setClientLongitude(double clientLongitude) {
        this.clientLongitude = clientLongitude;
    }

    public List<String> getFoodCategories() {
        return foodCategories;
    }

    public void setFoodCategories(ArrayList<String> foodCategories) {
        this.foodCategories = foodCategories != null ? foodCategories : new ArrayList<>();
    }

    public int getMinStars() {
        return minStars;
    }

    public void setMinStars(int minStars) {
        this.minStars = minStars;
    }

    public String getPriceCategory() {
        return priceCategory;
    }

    public void setPriceCategory(String priceCategory) {
        this.priceCategory = priceCategory != null ? priceCategory : "";
    }

    public double getRadius() {
        return radius;
    }

    public void setRadius(double radius) {
        this.radius = radius > 0 ? radius : 5.0;
    }

    @Override
    public String toString() {
        return "MapReduceRequest{" +
                "clientLatitude=" + clientLatitude +
                ", clientLongitude=" + clientLongitude +
                ", foodCategories=" + foodCategories +
                ", minStars=" + minStars +
                ", priceCategory='" + priceCategory + '\'' +
                ", radius=" + radius +
                '}';
    }
}

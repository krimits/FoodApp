// Product.java
import java.io.Serializable;

public class Product implements Serializable {
    private String name;
    private String category; // π.χ., "pizza", "burger", "drink"
    private double price;
    private int quantity; // Διαθέσιμη ποσότητα
    private String status; // e.g., "visible", "hidden"


    public Product(String name, String category, int quantity, double price) {
        this.name = name;
        this.category = category;
        this.price = price;
        this.quantity = quantity;
        this.status = "visible";
    }

    // Getters and Setters
    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getCategory() {
        return category;
    }

    public void setCategory(String category) {
        this.category = category;
    }

    public double getPrice() {
        return price;
    }

    public void setPrice(double price) {
        this.price = price;
    }

    public int getQuantity() {
        return quantity;
    }

    public void setQuantity(int quantity) {
        this.quantity = quantity;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }


    @Override
    public String toString() {
        return "Product Name: " + name +
                "\nCategory: " + category +
                "\nPrice: " + price +
                " €\nQuantity: " + quantity;
    }
}
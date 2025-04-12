// Purchase.java
import java.io.Serializable;
import java.util.*;

public class Purchase implements Serializable {
    private String customerName;
    private String customerEmail;
    private ArrayList<Product> purchasedProducts; // Λίστα προϊόντων που αγοράστηκαν
    private double totalPrice;
    private Date purchaseDate;

    public Purchase(String customerName, String customerEmail, ArrayList<Product> purchasedProducts) {
        this.customerName = customerName;
        this.customerEmail = customerEmail;
        this.purchasedProducts = purchasedProducts;
        this.totalPrice = calculateTotalPrice();
        this.purchaseDate = new Date(); // Current date/time when purchase is created
    }

    // Getters and Setters
    public String getCustomerName() {
        return customerName;
    }

    public void setCustomerName(String customerName) {
        this.customerName = customerName;
    }

    public String getCustomerEmail() {
        return customerEmail;
    }

    public void setCustomerEmail(String customerEmail) {
        this.customerEmail = customerEmail;
    }

    public ArrayList<Product> getPurchasedProducts() {
        return purchasedProducts;
    }

    public void setPurchasedProducts(ArrayList<Product> purchasedProducts) {
        this.purchasedProducts = purchasedProducts;
        this.totalPrice = calculateTotalPrice(); // Recalculate total when products change
    }

    public double getTotalPrice() {
        return totalPrice;
    }
    
    public Date getPurchaseDate() {
        return purchaseDate;
    }

    // Υπολογισμός του συνολικού κόστους της αγοράς
    private double calculateTotalPrice() {
        double total = 0.0;
        for (Product product : purchasedProducts) {
            total += product.getPrice() * product.getQuantity();
        }
        return total;
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append("Customer Name: ").append(customerName).append("\n");
        sb.append("Customer Email: ").append(customerEmail).append("\n");
        sb.append("Purchase Date: ").append(purchaseDate).append("\n");
        sb.append("Products:\n");
        
        for (Product product : purchasedProducts) {
            sb.append("- ").append(product.getName())
              .append(" (").append(product.getCategory()).append(")")
              .append(": ").append(product.getQuantity())
              .append(" x ").append(product.getPrice())
              .append("€ = ").append(product.getQuantity() * product.getPrice())
              .append("€\n");
        }
        
        sb.append("Total Price: ").append(totalPrice).append("€");
        return sb.toString();
    }
}
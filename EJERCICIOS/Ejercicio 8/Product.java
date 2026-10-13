package tecshop;

public class Product implements CartItem {
    private final String name;
    private final double basePrice;

    public Product(String name, double basePrice) {
        this.name = name;
        this.basePrice = basePrice;
    }

    @Override
    public double getPrice() {
        return basePrice;
    }

    @Override
    public String describe() {
        return name;
    }
}

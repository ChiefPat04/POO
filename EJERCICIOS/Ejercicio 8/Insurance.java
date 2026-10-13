package tecshop;

public class Insurance implements CartItem {
    private final CartItem item;

    public Insurance(CartItem item) {
        this.item = item;
    }

    @Override
    public double getPrice() {
        double p = item.getPrice();
        return p + p * 0.05;
    }

    @Override
    public String describe() {
        return item.describe() + " + insurance";
    }
}

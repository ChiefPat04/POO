package tecshop;

public class Express implements CartItem {
    private final CartItem item;

    public Express(CartItem item) {
        this.item = item;
    }

    @Override
    public double getPrice() {
        return item.getPrice() + 2500;
    }

    @Override
    public String describe() {
        return item.describe() + " + express";
    }
}

package tecshop;

public class GiftWrap implements CartItem {
    private final CartItem item;

    public GiftWrap(CartItem item) {
        this.item = item;
    }

    @Override
    public double getPrice() {
        return item.getPrice() + 1500;
    }

    @Override
    public String describe() {
        return item.describe() + " + gift wrap";
    }
}

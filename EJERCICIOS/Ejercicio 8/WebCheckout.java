package tecshop;

// Online store checkout.
public class WebCheckout {
    public void buy(Cart cart, String customer, String city) {
        new Checkout(new CorreosShipping(new CorreosApi()), "credit card").buy(cart, customer, city);
    }
}

package tecshop;

// In-store kiosk checkout.
public class KioskCheckout {
    public void buy(Cart cart, String customer, String city) {
        new Checkout(new LocalCourier(), "SINPE Movil").buy(cart, customer, city);
    }
}

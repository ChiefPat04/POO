package tecshop;

public class Checkout {
    private final ShippingProvider shipping;
    private final String paymentMethod;

    public Checkout(ShippingProvider shipping, String paymentMethod) {
        this.shipping = shipping;
        this.paymentMethod = paymentMethod;
    }

    public void buy(Cart cart, String customer, String city) {
        Inventory inventory = new Inventory();
        inventory.reserve(cart);

        TaxCalculator taxCalculator = new TaxCalculator();
        double subtotal = cart.getSubtotal();
        double tax = taxCalculator.tax(subtotal);

        double shippingCost = shipping.quote(cart.getWeightKg(), city);

        double total = subtotal + tax + shippingCost;
        PaymentGateway gateway = new PaymentGateway();
        gateway.charge(paymentMethod, total);

        InvoicePrinter printer = new InvoicePrinter();
        printer.print(customer, cart, tax, shippingCost, total);
    }
}

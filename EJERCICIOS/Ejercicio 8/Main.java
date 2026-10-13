package tecshop;

public class Main {
    public static void main(String[] args) {
        Cart webCart = new Cart(2.3);
        webCart.add(new Express(new GiftWrap(new Product("Laptop stand", 25000))));
        webCart.add(new Insurance(new Product("USB-C hub", 18000)));
        webCart.add(new Product("Notebook", 3500));
        new WebCheckout().buy(webCart, "Ana", "Cartago");

        System.out.println();

        Cart kioskCart = new Cart(0.8);
        kioskCart.add(new Insurance(new GiftWrap(new Product("Mechanical keyboard", 42000))));
        new KioskCheckout().buy(kioskCart, "Luis", "San Jose");
    }
}

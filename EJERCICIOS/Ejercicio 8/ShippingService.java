package tecshop;

public class ShippingService {
    private final LocalCourier local = new LocalCourier();
    private final ShippingProvider correos = new CorreosShipping(new CorreosApi());

    public double quote(String provider, double kg, String city) {
        if (provider.equals("local")) {
            return local.quote(kg, city);
        } else if (provider.equals("correos")) {
            return correos.quote(kg, city);
        }
        throw new IllegalArgumentException("Unknown provider: " + provider);
    }
}

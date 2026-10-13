package tecshop;

public class CorreosShipping implements ShippingProvider {
    private final CorreosApi api;

    public CorreosShipping(CorreosApi api) {
        this.api = api;
    }

    @Override
    public double quote(double kg, String city) {
        int gramos = (int) Math.round(kg * 1000);
        String codigoPostal;
        switch (city) {
            case "San Jose": codigoPostal = "10101"; break;
            case "Alajuela": codigoPostal = "20101"; break;
            case "Cartago":  codigoPostal = "30101"; break;
            case "Limon":    codigoPostal = "70101"; break;
            default: throw new IllegalArgumentException("Unknown city: " + city);
        }
        String raw = api.calcularTarifa(gramos, codigoPostal);
        return Double.parseDouble(raw.replace("₡", ""));
    }
}

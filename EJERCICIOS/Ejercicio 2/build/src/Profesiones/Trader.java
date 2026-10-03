package profesiones;

import personas.Persona;

public class Trader extends Persona {
    private String mercado;
    private double capitalDisponible;
    private int operacionesRealizadas;
    private double ganancias;

    public Trader(String nombre, byte edad, String mercado) {
        super(nombre, edad);
        this.mercado = mercado;
    }

    public String getMercado() { return mercado; }
    public void setMercado(String mercado) {
        if (mercado != null && !mercado.trim().isEmpty()) this.mercado = mercado;
    }
    public double getCapitalDisponible() { return capitalDisponible; }
    public void setCapitalDisponible(double capitalDisponible) {
        if (capitalDisponible >= 0 && Double.isFinite(capitalDisponible)) this.capitalDisponible = capitalDisponible;
    }
    public int getOperacionesRealizadas() { return operacionesRealizadas; }
    public void setOperacionesRealizadas(int operacionesRealizadas) {
        if (operacionesRealizadas >= 0) this.operacionesRealizadas = operacionesRealizadas;
    }
    public double getGanancias() { return ganancias; }
    public void setGanancias(double ganancias) {
        if (Double.isFinite(ganancias)) this.ganancias = ganancias;
    }

    @Override
    public void trabajar() {
        System.out.println(identidad() + " trabaja como trader en el mercado de " + mercado + ".");
    }

    public void analizarMercado() {
        System.out.println(getNombre() + " analiza el mercado de " + mercado + ".");
    }

    public boolean comprarActivo(String activo, double precio) {
        if (precio <= 0 || precio > capitalDisponible) {
            System.out.println("No se pudo comprar " + activo + ". Capital insuficiente.");
            return false;
        }
        capitalDisponible -= precio;
        operacionesRealizadas++;
        System.out.println(getNombre() + " compra " + activo + " por " + precio + ".");
        return true;
    }

    public void venderActivo(String activo, double precioVenta, double costoCompra) {
        if (precioVenta <= 0 || costoCompra < 0) {
            System.out.println("Los valores de la venta no son validos.");
            return;
        }
        capitalDisponible += precioVenta;
        ganancias += precioVenta - costoCompra;
        operacionesRealizadas++;
        System.out.println(getNombre() + " vende " + activo + " y obtiene una ganancia de "
                + (precioVenta - costoCompra) + ".");
    }
}
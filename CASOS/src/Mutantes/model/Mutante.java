package Mutantes.model;

import Mutantes.constants.ConstantesJuego;

public class Mutante {

    private final int id;
    private final String nombre;
    private int energia;
    private final int defensa;
    private boolean vivo;

    public Mutante(int id, String nombre, int defensa) {
        this.id = id;
        this.nombre = nombre;
        this.defensa = defensa;
        this.energia = ConstantesJuego.ENERGIA_INICIAL;
        this.vivo = true;
    }

    public synchronized void recibirDano(int cantidad) {
        if (!vivo) {
            return;
        }

        energia -= cantidad;

        if (energia <= 0) {
            energia = 0;
            vivo = false;
        }
    }

    public synchronized boolean estaVivo() {
        return vivo;
    }

    public synchronized int getEnergia() {
        return energia;
    }

    public int getId() {
        return id;
    }

    public String getNombre() {
        return nombre;
    }

    public int getDefensa() {
        return defensa;
    }
}
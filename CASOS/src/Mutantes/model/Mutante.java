package mutantes.model;

import mutantes.constants.ConstantesJuego;
import mutantes.model.poderes.PoderMutante;

public class Mutante {

    private final int id;
    private final String nombre;
    private int energia;
    private final int defensa;
    private final PoderMutante poder;
    private boolean vivo;

    public Mutante(int id, String nombre, int defensa, PoderMutante poder) {
        this.id = id;
        this.nombre = nombre;
        this.defensa = defensa;
        this.poder = poder;
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

    public synchronized void curar(int cantidad) {
        if (!vivo) {
            return;
        }

        energia += cantidad;
    }

    public void atacar(Mutante objetivo){
        poder.aplicarEfecto(this, objetivo);
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

    public PoderMutante getPoder() {
        return poder;
    }
}
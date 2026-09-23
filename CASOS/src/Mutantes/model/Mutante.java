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
    private boolean escudoActivo;
    private boolean invisible;

    public Mutante(int id, String nombre, int defensa, PoderMutante poder) {
        this.id = id;
        this.nombre = nombre;
        this.defensa = defensa;
        this.poder = poder;
        this.energia = ConstantesJuego.ENERGIA_INICIAL;
        this.vivo = true;
        this.escudoActivo = false;
        this.invisible = false;
    }

    public synchronized void recibirDano(int cantidad) {
        if (!vivo) {
            return;
        }

        if (invisible) {
            invisible = false;
            return;
        }

        int danoFinal = cantidad;
        if (escudoActivo) {
            danoFinal = cantidad / 2;
            escudoActivo = false;
        }

        energia -= danoFinal;

        if (energia <= 0) {
            energia = 0;
            vivo = false;
        }
    }

    public synchronized void recuperarEnergia(int cantidad) {
        if (!vivo) {
            return;
        }

        energia += cantidad;
        if (energia > ConstantesJuego.ENERGIA_INICIAL) {
            energia = ConstantesJuego.ENERGIA_INICIAL;
        }
    }

    public synchronized void activarEscudo() {
        escudoActivo = true;
    }

    public synchronized void activarInvisibilidad() {
        invisible = true;
    }

    public synchronized void atacar(Mutante objetivo) {
        if (!vivo || poder == null) {
            return;
        }

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
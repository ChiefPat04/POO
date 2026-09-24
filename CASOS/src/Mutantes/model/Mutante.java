package mutantes.model;

import java.util.concurrent.ThreadLocalRandom;
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

    private int posicionX;
    private int posicionY;
    private int velocidadX;
    private int velocidadY;

    public Mutante(int id, String nombre, int defensa, PoderMutante poder) {
        this.id = id;
        this.nombre = nombre;
        this.defensa = defensa;
        this.poder = poder;
        this.energia = ConstantesJuego.ENERGIA_INICIAL;
        this.vivo = true;
        this.escudoActivo = false;
        this.invisible = false;

        this.posicionX = 0;
        this.posicionY = 0;
        this.velocidadX = signoAleatorio() * magnitudVelocidadAleatoria();
        this.velocidadY = signoAleatorio() * magnitudVelocidadAleatoria();
     }

    private int signoAleatorio() {
        return ThreadLocalRandom.current().nextBoolean() ? 1 : -1;
    }

    private int magnitudVelocidadAleatoria() {
        int rango = ConstantesJuego.VELOCIDAD_MUTANTE_MAX - ConstantesJuego.VELOCIDAD_MUTANTE_MIN + 1;
        return ConstantesJuego.VELOCIDAD_MUTANTE_MIN + ThreadLocalRandom.current().nextInt(rango);
    }

    private volatile long ultimoGolpeMillis = 0;

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
        if (danoFinal > 0) {
            ultimoGolpeMillis = System.currentTimeMillis();
        }

        if (energia <= 0) {
            energia = 0;
            vivo = false;
        }
    }

    public long getUltimoGolpeMillis() {
        return ultimoGolpeMillis;
    }

    public synchronized void moverse (int limiteX, int limiteY) {
        if (!vivo) {
            return;
        }

        posiblementeCambiarRumbo();

        posicionX += velocidadX;
        posicionY += velocidadY;

        if (posicionX < 0 || posicionX > limiteX) {
            velocidadX = -velocidadX;
            posicionX = Math.max(0, Math.min(posicionX, limiteX));
        }

        if (posicionY < 0 || posicionY > limiteY) {
            velocidadY = -velocidadY;
            posicionY = Math.max(0, Math.min(posicionY, limiteY));
        }
    }

    private void posiblementeCambiarRumbo() {
        boolean cambiarRumbo = ThreadLocalRandom.current().nextDouble() < ConstantesJuego.PROBABILIDAD_CAMBIO_RUMBO;
        if (cambiarRumbo) {
            velocidadX = signoAleatorio() * magnitudVelocidadAleatoria();
            velocidadY = signoAleatorio() * magnitudVelocidadAleatoria();
        }
    }

    public synchronized void reposicionar(int nuevaX, int nuevaY){
        this.posicionX = nuevaX;
        this.posicionY = nuevaY;
    }

    public synchronized int getPosicionX() {
        return posicionX;
    }

    public synchronized int getPosicionY() {
        return posicionY;
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

    public AccionCombate decidirAccion() {
        boolean ataca = ThreadLocalRandom.current().nextBoolean();
        return ataca ? AccionCombate.ATACAR : AccionCombate.DEFENDER;
    }

    public synchronized void atacar(Mutante objetivo) {
        atacar(objetivo, false);
    }

    public synchronized void atacar(Mutante objetivo, boolean objetivoSeDefiende) {
        if (!vivo || poder == null) {
            return;
        }

        poder.aplicarEfecto(this, objetivo, objetivoSeDefiende);
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
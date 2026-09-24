package mutantes.game;

import java.util.Random;
import java.util.ArrayList;
import java.util.List;
import mutantes.constants.ConstantesJuego;
import mutantes.model.Mutante;
import mutantes.model.poderes.PoderEscudoEnergia;
import mutantes.model.poderes.PoderFuerza;
import mutantes.model.poderes.PoderInvisibilidad;
import mutantes.model.poderes.PoderMutante;
import mutantes.model.poderes.PoderRegeneracion;
import mutantes.model.poderes.PoderRoboEnergia;
import mutantes.model.poderes.PoderTeletransportacion;
import mutantes.model.poderes.PoderVelocidad;


public class CampoDeBatalla {

    private final int ancho;
    private final int alto;
    private final int radioDeteccion;
    private final Random random;
    private final List<ObservadorBatalla> observadores = new ArrayList<>();

    private Equipo equipoA; 
    private Equipo equipoB;

    public CampoDeBatalla(int ancho, int alto) {
        this.ancho = ancho;
        this.alto = alto;
        this.radioDeteccion = ConstantesJuego.RADIO_DETECCION_DEFAULT;
        this.random = new Random();
    }

    public void crearEquipos(int tamano){
        if (tamano < ConstantesJuego.TAMANO_EQUIPO || tamano > ConstantesJuego.TAMANO_EQUIPO_MAX) {
            throw new IllegalArgumentException("El tamaño del equipo debe estar entre " 
            + ConstantesJuego.TAMANO_EQUIPO + " y " + ConstantesJuego.TAMANO_EQUIPO_MAX);
        }

        equipoA = new Equipo(ColorEquipo.ROJO, "★");
        equipoB = new Equipo(ColorEquipo.AZUL, "☾");

        int idActual = 1;

        for (int i = 0; i < tamano; i++){
            Mutante mutante = generarMutanteAleatorio(idActual);
            ubicarEnZonaBase(mutante, true);
            equipoA.agregarMutante(mutante);
            idActual++;
        }

        for (int i = 0; i < tamano; i++){
            Mutante mutante = generarMutanteAleatorio(idActual);
            ubicarEnZonaBase(mutante, false);
            equipoB.agregarMutante(mutante);
            idActual++;
        }
    }

    private void ubicarEnZonaBase(Mutante mutante, boolean esEquipoA) {
        int x;
        if (esEquipoA) {
            x = valorAleatorioEntre(0, ConstantesJuego.ANCHO_ZONA_BASE);
        } else {
            x = valorAleatorioEntre(ancho - ConstantesJuego.ANCHO_ZONA_BASE, ancho);
        }

        int y = valorAleatorioEntre(0, alto);
        mutante.reposicionar(x, y);
    }

    public void agregarObservador(ObservadorBatalla observador) {
        observadores.add(observador);
    }

    public void notificarObservadores() {
        for (ObservadorBatalla observador : observadores) {
            observador.alActualizarEstado();
        }
    }

    private Mutante generarMutanteAleatorio(int id) {
        int defensa = valorAleatorioEntre(ConstantesJuego.DEFENSA_MIN, ConstantesJuego.DEFENSA_MAX);
        int danoInicial = valorAleatorioEntre(ConstantesJuego.DANO_MIN, ConstantesJuego.DANO_MAX);
        PoderMutante poder = generarPoderAleatorio(danoInicial);

        String nombre = "Mutante " + poder.getTipoEfecto() + " " + id;

        return new Mutante(id, nombre, defensa, poder);
    }

    private PoderMutante generarPoderAleatorio(int danoInicial) {
    int tipo = random.nextInt(7);

    switch (tipo) {
        case 0:
            return new PoderFuerza(danoInicial);
        case 1:
            return new PoderVelocidad(danoInicial);
        case 2:
            return new PoderRegeneracion(danoInicial);
        case 3:
            return new PoderEscudoEnergia(danoInicial);
        case 4:
            return new PoderInvisibilidad(danoInicial);
        case 5:
            return new PoderRoboEnergia(danoInicial);
        default:
            return new PoderTeletransportacion(danoInicial);
        }

    }

    private int valorAleatorioEntre(int minimo, int maximo){
        return minimo + random.nextInt(maximo - minimo +1);
    }

    public int[] getDimensiones() {
        return new int[] { ancho, alto };
    }

    public int getRadioDeteccion() {
        return radioDeteccion;
    }

    public Equipo getEquipoEnemigoDe(Mutante mutante) {
        if (equipoA.getMutantes().contains(mutante)) {
            return equipoB;
        }
        return equipoA;
    }

    public boolean hayGanador(){
        equipoA.actualizarMarcador();
        equipoB.actualizarMarcador();
        return equipoA.estaDerrotado() || equipoB.estaDerrotado();
    }

    public Equipo getGanador(){
        if (equipoA.estaDerrotado()){
            return equipoB;
        }
        if (equipoB.estaDerrotado()){
            return equipoA;
        }
        return null;
    }

    public Equipo getEquipoA() {
        return equipoA;
    }

    public Equipo getEquipoB() {
        return equipoB;
    }
}
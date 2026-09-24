package mutantes.constants;

public final class ConstantesJuego {

    private ConstantesJuego() {
    }

    public static final int ENERGIA_INICIAL = 100;
    public static final int DEFENSA_MIN = 1;
    public static final int DEFENSA_MAX = 3;

    public static final int DANO_MIN = 1;
    public static final int DANO_MAX = 3;
    public static final int DANO_MAX_PODER = 7;

    public static final int TAMANO_EQUIPO = 3;
    public static final int TAMANO_EQUIPO_MAX = 11;
    public static final int RADIO_DETECCION_DEFAULT = 100;
    //por ahora se queda en 100 para pruebas de combate.

    public static final int ANCHO_ZONA_BASE = 100;
    public static final int VELOCIDAD_MUTANTE_MIN = 2;
    public static final int VELOCIDAD_MUTANTE_MAX = 6;  
    public static final int INTERVALO_MOVIMIENTO_MS = 50;
    public static final int CANTIDAD_HILOS_POOL = 8;
    public static final double PROBABILIDAD_CAMBIO_RUMBO = 0.05;
    public static final int MULTIPLICADOR_LLAMARADA = 2;
    public static final int MULTIPLICADOR_EXPLOSIVO = 2;
    public static final double PORCENTAJE_DANO_VENENO = 0.15;
    
}
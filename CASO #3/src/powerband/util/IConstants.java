package powerband.util;

import java.util.List;

public interface IConstants {

    public static final String APP_NAME = "Power Band";

    // ------------------------------------------------------------ conexión SuperCollider
    public static final String SC_HOST = "127.0.0.1";
    public static final int SC_PORT = 57110;
    public static final int PORT_MIN = 0;
    public static final int PORT_MAX = 65535;
    public static final int FIRST_NODE_ID = 10000;
    public static final int ADD_TO_HEAD = 0;
    public static final int DEFAULT_GROUP = 1;

    // ------------------------------------------------------------ formato OSC
    public static final String OSC_S_NEW = "/s_new";
    public static final String OSC_N_SET = "/n_set";
    public static final int OSC_ALIGNMENT = 4;
    public static final char OSC_TYPE_TAG_PREFIX = ',';
    public static final char OSC_TAG_INT = 'i';
    public static final char OSC_TAG_FLOAT = 'f';
    public static final char OSC_TAG_STRING = 's';
    public static final int OSC_NULL_BYTE = 0;
    public static final int BYTE_SHIFT_24 = 24;
    public static final int BYTE_SHIFT_16 = 16;
    public static final int BYTE_SHIFT_8 = 8;
    /** defName, nodeId, addAction y target de /s_new. */
    public static final int S_NEW_FIXED_ARGS = 4;

    // ------------------------------------------------------------ synths (myband.sc)
    public static final String SYNTH_PIANO = "classicPiano";
    public static final String SYNTH_BASS = "stringBass";
    public static final String SYNTH_ORGAN = "organ70s";

    // ------------------------------------------------------------ parámetros de los synths
    public static final String PARAM_NOTE = "note";
    public static final String PARAM_DUR_MS = "durMs";
    public static final String PARAM_THIRD = "third";
    public static final String PARAM_AMP = "amp";
    public static final String PARAM_FREQ = "freq";
    public static final String PARAM_R1 = "r1";
    public static final String PARAM_R2 = "r2";
    public static final String PARAM_R3 = "r3";
    public static final String PARAM_A1 = "a1";
    public static final String PARAM_A2 = "a2";
    public static final String PARAM_A3 = "a3";
    public static final String PARAM_GATE = "gate";

    // ------------------------------------------------------------ valores musicales
    public static final int GATE_OFF = 0;
    public static final int THIRD_MAJOR = 4;
    public static final int THIRD_MINOR = 3;
    public static final int MIDI_A4 = 69;
    public static final double FREQ_A4 = 440.0;
    public static final double SEMITONES_PER_OCTAVE = 12.0;
    public static final double FREQ_BASE = 2.0;
    public static final int ORGAN_PARTIALS = 3;
    public static final int FIRST_PARTIAL = 0;
    public static final int SECOND_PARTIAL = 1;
    public static final int THIRD_PARTIAL = 2;
    public static final int UNUSED_INT = 0;
    public static final float UNUSED_FLOAT = 0f;

    // ------------------------------------------------------------ valores por defecto (iguales a myband.sc)
    public static final float DEFAULT_PIANO_AMP = 0.3f;
    public static final float DEFAULT_BASS_AMP = 0.5f;
    public static final float DEFAULT_ORGAN_AMP = 0.3f;
    /** Inmutables: List.of evita que alguien cambie los valores por defecto. */
    public static final List<Float> DEFAULT_ORGAN_RATIOS = List.of(1f, 2f, 3f);
    public static final List<Float> DEFAULT_ORGAN_AMPS = List.of(1f, 0.5f, 0.25f);

    // ------------------------------------------------------------ mensajes de error
    public static final String ERR_INVALID_PORT = "Puerto fuera de rango: ";
    public static final String ERR_UNKNOWN_HOST = "No se pudo resolver el host: ";
    public static final String ERR_SOCKET_CREATE = "No se pudo crear el socket UDP";
    public static final String ERR_SEND = "No se pudo enviar el mensaje OSC: ";
    public static final String ERR_UNSUPPORTED_OSC_TYPE = "Tipo OSC no soportado: ";
    public static final String ERR_NULL_EVENT = "El evento no puede ser null";
    public static final String ERR_NULL_COMMAND = "El comando no puede ser null";
    public static final String ERR_NULL_CLIENT = "El cliente OSC no puede ser null";
    public static final String ERR_WRONG_SYNTH = "El comando no acepta eventos del synth: ";
    public static final String ERR_WRONG_ACTION = "El comando no acepta la acción: ";
    public static final String ERR_ORGAN_PARTIALS = "El órgano necesita exactamente 3 ratios y 3 amplitudes";
}

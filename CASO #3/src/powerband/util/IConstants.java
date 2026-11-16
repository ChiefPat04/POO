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

    // ------------------------------------------------------------ archivos de grabaciones
    public static final String DATA_DIR = "data";
    public static final String MUSIC_FILE = DATA_DIR + "/recordings.music";
    public static final String LIST_FILE = DATA_DIR + "/recordings.list";
    public static final String FILE_MODE_READ = "r";
    public static final String FILE_MODE_READ_WRITE = "rw";

    // ------------------------------------------------------------ formato binario de las grabaciones
    /** Tamaño de todo registro de recordings.music, sea encabezado o comando. */
    public static final int RECORD_SIZE = 64;
    public static final int NAME_LENGTH = 32;
    /** Nombre (32) + fecha en ms (8). */
    public static final int LIST_RECORD_SIZE = 40;
    public static final int HEADER_PADDING = 15;
    public static final int COMMAND_PADDING = 13;
    public static final long FILE_START = 0L;
    public static final int RECORD_TYPE_HEADER = 1;
    public static final int RECORD_TYPE_COMMAND = 2;
    public static final int SYNTH_CODE_PIANO = 1;
    public static final int SYNTH_CODE_BASS = 2;
    public static final int SYNTH_CODE_ORGAN = 3;
    public static final int ACTION_CODE_TRIGGER = 1;
    public static final int ACTION_CODE_NOTE_ON = 2;
    public static final int ACTION_CODE_NOTE_OFF = 3;
    public static final char NAME_PAD_CHAR = ' ';
    public static final char NAME_MIN_CHAR = 32;
    public static final char NAME_MAX_CHAR = 126;
    public static final int PADDING_BYTE = 0;
    /** Posición de fin de una grabación que todavía no se ha cerrado. */
    public static final long PENDING_POSITION = -1L;
    /** Nombre del encabezado provisional (queda como 32 espacios en disco). */
    public static final String EMPTY_RECORDING_NAME = "";
    public static final long RECORDING_MIN_TIME_MS = 0L;

    // ------------------------------------------------------------ mensajes de error
    public static final String ERR_STORAGE_READ = "No se pudo leer el archivo: ";
    public static final String ERR_STORAGE_WRITE = "No se pudo escribir el archivo: ";
    public static final String ERR_UNEXPECTED_RECORD_TYPE = "Tipo de registro inesperado: ";
    public static final String ERR_UNKNOWN_SYNTH_CODE = "Código de synth desconocido: ";
    public static final String ERR_UNKNOWN_ACTION_CODE = "Código de acción desconocido: ";
    public static final String ERR_ALREADY_RECORDING = "Ya hay una grabación en curso o pendiente de nombre";
    public static final String ERR_NOT_RECORDING = "No hay una grabación en curso";
    public static final String ERR_NO_PENDING_RECORDING = "No hay una grabación pendiente de nombre";
    public static final String ERR_DUPLICATE_NAME = "Ya existe una grabación con el nombre: ";
    public static final String ERR_INVALID_NAME = "Nombre de grabación inválido (1 a 32 caracteres ASCII, sin espacios en los extremos): ";
    public static final String ERR_NULL_STORAGE = "El almacenamiento no puede ser null";
    public static final String ERR_NULL_INDEXER = "El indexador no puede ser null";
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

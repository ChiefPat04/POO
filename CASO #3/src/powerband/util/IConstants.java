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
    public static final String INDEX_FILE = DATA_DIR + "/recordings.index";
    /** Los 4 archivos temporales del Balanced Merge Sort de 2 vías. */
    public static final List<String> MERGE_TEMP_FILES =
            List.of("merge1.tmp", "merge2.tmp", "merge3.tmp", "merge4.tmp");
    public static final String FILE_MODE_READ = "r";
    public static final String FILE_MODE_READ_WRITE = "rw";

    // ------------------------------------------------------------ formato binario de las grabaciones
    /** Tamaño de todo registro de recordings.music, sea encabezado o comando. */
    public static final int RECORD_SIZE = 64;
    public static final int NAME_LENGTH = 32;
    /** Nombre (32) + fecha en ms (8). */
    public static final int LIST_RECORD_SIZE = 40;
    /** Registro de recordings.index: nombre (32) + posición del encabezado (8). */
    public static final int INDEX_RECORD_SIZE = 40;
    /** Balanced Merge Sort: tramos iniciales de 1 registro que se duplican en cada pasada. */
    public static final long MERGE_INITIAL_RUN = 1L;
    public static final long MERGE_RUN_GROWTH = 2L;
    /** Mezcla de 2 vías: 2 archivos de entrada y 2 de salida por pasada. */
    public static final int MERGE_WAYS = 2;
    /** Desplazamiento del segundo archivo dentro de un par (entradas o salidas). */
    public static final int MERGE_SECOND_OFFSET = 1;
    public static final long EMPTY_COUNT = 0L;
    /** Divisor para tomar el punto medio en la búsqueda binaria. */
    public static final long SEARCH_HALF = 2L;
    public static final long SEARCH_STEP = 1L;
    /** Resultado de compareTo cuando los dos nombres son iguales. */
    public static final int SAME_NAME = 0;
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

    // ------------------------------------------------------------ escala y rango del bajo
    public static final int BASS_MIN_NOTE = 28;
    public static final int BASS_MAX_NOTE = 55;
    /** Semitonos de cada grado de Do mayor desde Do. */
    public static final List<Integer> C_MAJOR_SCALE = List.of(0, 2, 4, 5, 7, 9, 11);
    public static final int OCTAVE_SEMITONES = 12;

    // ------------------------------------------------------------ control PS5 (DualSense)
    /** Do2: nota del paso 0 de la escala. */
    public static final int GAMEPAD_ROOT_NOTE = 36;
    public static final int GAMEPAD_INITIAL_STEP = 0;
    /** Mi1 = 28. */
    public static final int GAMEPAD_MIN_STEP = -5;
    /** Sol3 = 55. */
    public static final int GAMEPAD_MAX_STEP = 11;
    public static final int GAMEPAD_STICK_STEP = 1;
    public static final int GAMEPAD_OCTAVE_STEPS = 7;
    public static final int GAMEPAD_STEP_CROSS = 0;
    public static final int GAMEPAD_STEP_CIRCLE = 1;
    public static final int GAMEPAD_STEP_SQUARE = 2;
    public static final int GAMEPAD_STEP_TRIANGLE = 3;
    /** El stick da un paso al pasar este valor y se rearma al volver bajo el otro. */
    public static final float GAMEPAD_STICK_PUSH_THRESHOLD = 0.6f;
    public static final float GAMEPAD_STICK_RELEASE_THRESHOLD = 0.3f;
    /** Valor de un eje o gatillo en reposo. */
    public static final float GAMEPAD_AXIS_REST = 0f;
    public static final int GAMEPAD_MIN_DUR_MS = 300;
    public static final int GAMEPAD_MAX_DUR_MS = 2000;
    public static final long GAMEPAD_POLL_INTERVAL_MS = 10L;
    public static final long GAMEPAD_RECONNECT_WAIT_MS = 500L;
    public static final long GAMEPAD_STOP_TIMEOUT_MS = 1000L;
    public static final String GAMEPAD_THREAD_NAME = "powerband-gamepad";
    public static final int GAMEPAD_INDEX = 0;
    public static final int NO_GAMEPADS = 0;

    // ------------------------------------------------------------ captura de audio
    public static final float AUDIO_SAMPLE_RATE = 44100f;
    public static final int AUDIO_SAMPLE_BITS = 16;
    public static final int AUDIO_CHANNELS = 1;
    public static final int AUDIO_BYTES_PER_SAMPLE = 2;
    public static final boolean AUDIO_SIGNED = true;
    public static final boolean AUDIO_BIG_ENDIAN = false;
    /** Muestras por ventana de análisis: unos 46 ms a 44100 Hz. */
    public static final int AUDIO_BLOCK_SIZE = 2048;
    /** Divide una muestra de 16 bits para dejarla entre -1 y 1. */
    public static final float AUDIO_SAMPLE_SCALE = 32768f;
    public static final int FIRST_INDEX = 0;

    // ------------------------------------------------------------ detección de tono
    public static final float PITCH_MIN_FREQ = 75f;
    public static final float PITCH_MAX_FREQ = 2800f;
    /** Autocorrelación normalizada mínima para aceptar que la señal es periódica. */
    public static final float PITCH_CLARITY_THRESHOLD = 0.9f;
    /** Energía media por muestra bajo la cual la ventana se trata como silencio. */
    public static final float PITCH_MIN_ENERGY = 1e-6f;
    public static final float NO_PITCH = -1f;
    public static final int PITCH_FIRST_LAG = 1;
    public static final int PITCH_LAG_STEP = 1;
    public static final int PITCH_MAX_LAG_DIVISOR = 2;
    public static final double NO_CORRELATION = 0.0;
    public static final double NO_ENERGY = 0.0;
    public static final double PARABOLA_HALF = 0.5;
    public static final double PARABOLA_DOUBLE = 2.0;

    // ------------------------------------------------------------ micrófono -> órgano
    public static final int ORGAN_MIN_NOTE = 40;
    public static final int ORGAN_MAX_NOTE = 100;
    public static final int NO_NOTE = -1;
    /** Volumen RMS para encender; el de apagar es menor (histéresis). Ajustar con el micrófono real. */
    public static final float MIC_ON_THRESHOLD = 0.02f;
    public static final float MIC_OFF_THRESHOLD = 0.01f;
    /** Ventanas seguidas con la misma nota para encenderla. */
    public static final int MIC_STABLE_WINDOWS = 2;
    /** Ventanas seguidas de silencio para apagar. */
    public static final int MIC_OFF_WINDOWS = 3;
    public static final int MIC_NO_WINDOWS = 0;
    public static final float MIC_AMP_GAIN = 2f;
    public static final float MIC_MIN_AMP = 0.1f;
    public static final float MIC_MAX_AMP = 0.6f;
    public static final long MIC_RETRY_WAIT_MS = 200L;
    public static final long MIC_STOP_TIMEOUT_MS = 1000L;
    public static final String MIC_THREAD_NAME = "powerband-microphone";

    // ------------------------------------------------------------ webcam -> piano
    public static final int PIANO_MIN_NOTE = 48;
    public static final int PIANO_MAX_NOTE = 72;
    /** Do3: nota del paso 0 (abajo de la imagen). */
    public static final int PIANO_ROOT_NOTE = 48;
    /** Dos octavas de Do mayor: pasos 0..14 (Do3..Do5). */
    public static final int CAM_MAX_STEP = 14;
    /** Franjas de igual alto en la imagen: una por nota. */
    public static final int CAM_NOTE_BANDS = CAM_MAX_STEP + 1;
    /** Cuadrícula a la que se reduce cada cuadro antes de compararlo. */
    public static final int CAM_GRID_WIDTH = 64;
    public static final int CAM_GRID_HEIGHT = 48;
    /** Diferencia de gris (0..255) para contar un píxel como cambiado. Ajustar con la cámara real. */
    public static final int CAM_PIXEL_THRESHOLD = 30;
    /** Banda del raster gris. */
    public static final int CAM_GRAY_BAND = 0;
    /** Espejar la imagen para que la izquierda sea la del usuario frente a la cámara. */
    public static final boolean CAM_MIRROR = true;
    /** Desplazamiento al centro de una celda de la cuadrícula. */
    public static final float CAM_PIXEL_CENTER = 0.5f;
    /** Límites de una coordenada normalizada. */
    public static final float CAM_NORMALIZED_MIN = 0f;
    public static final float CAM_NORMALIZED_MAX = 1f;
    public static final float CAM_NO_MOTION = 0f;
    /** Fracción de la cuadrícula que cambió. Ajustar con la cámara, la luz y la distancia reales. */
    public static final float CAM_MIN_MOTION = 0.01f;
    public static final float CAM_REARM_MOTION = 0.02f;
    public static final float CAM_STRIKE_MOTION = 0.06f;
    /** Más que esto es un cambio de luz o de la cámara, no la mano. */
    public static final float CAM_MAX_MOTION = 0.5f;
    /** A la izquierda de esta x el acorde es mayor; a la derecha, menor. */
    public static final float CAM_SPLIT_X = 0.5f;
    public static final int CAM_DUR_MS = 1000;
    public static final float CAM_AMP_GAIN = 4f;
    public static final float CAM_MIN_AMP = 0.15f;
    public static final float CAM_MAX_AMP = 0.6f;
    public static final long CAM_POLL_INTERVAL_MS = 33L;
    public static final long CAM_RETRY_WAIT_MS = 500L;
    public static final long CAM_STOP_TIMEOUT_MS = 1000L;
    public static final String CAM_THREAD_NAME = "powerband-webcam";

    // ------------------------------------------------------------ reproductor
    public static final String PLAYER_THREAD_NAME = "powerband-player";
    public static final long PLAYER_STOP_TIMEOUT_MS = 1000L;
    public static final long PLAYER_NO_WAIT_MS = 0L;
    /** Margen aceptado entre el intervalo grabado y el reproducido (lo usan las pruebas). */
    public static final long PLAYER_TIMING_TOLERANCE_MS = 30L;

    // ------------------------------------------------------------ editor: instrumentos y estado
    public static final String LABEL_GAMEPAD = "Control PS5 → bajo";
    public static final String LABEL_MICROPHONE = "Micrófono → órgano";
    public static final String LABEL_WEBCAM = "Webcam → piano";
    public static final String STATUS_STARTED = "Iniciados: ";
    public static final String STATUS_FAILED = "Fallaron: ";
    public static final String STATUS_NONE = "ninguno";
    public static final String STATUS_SEPARATOR = ", ";
    public static final String STATUS_REASON_OPEN = " (";
    public static final String STATUS_REASON_CLOSE = ")";
    public static final String STATUS_PART_SEPARATOR = "  |  ";
    public static final String STATUS_LOADED = "Cargada: ";

    // ------------------------------------------------------------ editor: monitor y lista
    public static final String MONITOR_TIME_PATTERN = "HH:mm:ss.SSS";
    public static final String MONITOR_LINE_FORMAT = "%s  %-5s  %-8s  nota %3d  dur %s";
    public static final String MONITOR_NO_VALUE = "-";
    public static final String MONITOR_DUR_SUFFIX = " ms";
    public static final int MONITOR_MAX_LINES = 500;
    public static final int MONITOR_FONT_SIZE = 12;
    public static final String LIST_DATE_PATTERN = "yyyy-MM-dd HH:mm:ss";
    public static final String LIST_LINE_FORMAT = "%-32s  %s";
    public static final int NO_SELECTION = -1;

    // ------------------------------------------------------------ editor: ventana
    public static final String WINDOW_TITLE = APP_NAME;
    public static final String BTN_RECORD = "Grabar";
    public static final String BTN_STOP_RECORDING = "Detener grabación";
    public static final String BTN_LOAD = "Cargar";
    public static final String BTN_PLAY = "Reproducir";
    public static final String BTN_STOP_PLAYBACK = "Detener reproducción";
    public static final String TITLE_RECORDINGS = "Grabaciones";
    public static final String TITLE_MONITOR = "Monitor";
    public static final String DIALOG_NAME_TITLE = "Guardar grabación";
    public static final String DIALOG_NAME_PROMPT = "Nombre (máximo 32 caracteres, sin tildes ni ñ):";
    public static final String DIALOG_ERROR_TITLE = "Error";
    public static final String STATE_IDLE = "Listo";
    public static final String STATE_RECORDING = "Grabando…";
    public static final String STATE_NAMING = "Esperando nombre";
    public static final String STATE_PLAYING = "Reproduciendo…";
    public static final String LIST_READ_ERROR = "(no se pudo leer)";
    public static final String NEWLINE = "\n";
    public static final int WINDOW_WIDTH = 1000;
    public static final int WINDOW_HEIGHT = 600;
    public static final int LIST_PANEL_WIDTH = 360;
    public static final int UI_GAP = 8;
    public static final String MONO_FONT = java.awt.Font.MONOSPACED;
    public static final int EXIT_OK = 0;
    public static final int WINDOW_CLOSE_SIGNALS = 1;

    // ------------------------------------------------------------ mensajes de error
    public static final String ERR_INVALID_STATE = "Acción no permitida en el estado: ";
    public static final String ERR_NOTHING_LOADED = "No hay ninguna grabación cargada";
    public static final String ERR_NULL_EDITOR_DEPENDENCY = "El grabador, el reproductor, la biblioteca y el órgano no pueden ser null";
    public static final String ERR_SHUTDOWN_STEP = "Error al cerrar: ";
    public static final String ERR_SC_CONNECT = "No se pudo conectar con SuperCollider: ";
    public static final String ERR_NO_MICROPHONE = "No se pudo abrir el micrófono";
    public static final String ERR_MIC_ALREADY_STARTED = "El micrófono ya está iniciado";
    public static final String ERR_NULL_AUDIO_SOURCE = "La fuente de audio no puede ser null";
    public static final String ERR_MIC_PLAY = "No se pudo tocar la nota del micrófono: ";
    public static final String ERR_NO_WEBCAM = "No hay ninguna webcam disponible";
    public static final String ERR_WEBCAM_OPEN = "No se pudo abrir la webcam";
    public static final String ERR_WEBCAM_ALREADY_STARTED = "La webcam ya está iniciada";
    public static final String ERR_NULL_FRAME_SOURCE = "La fuente de cuadros no puede ser null";
    public static final String ERR_WEBCAM_PLAY = "No se pudo tocar el acorde de la webcam: ";
    public static final String ERR_NO_GAMEPAD = "No hay ningún control conectado";
    public static final String ERR_GAMEPAD_INIT = "No se pudo inicializar la lectura del control";
    public static final String ERR_GAMEPAD_ALREADY_STARTED = "El control ya está iniciado";
    public static final String ERR_NULL_READER = "El lector del control no puede ser null";
    public static final String ERR_GAMEPAD_PLAY = "No se pudo tocar la nota del control: ";
    public static final String ERR_STORAGE_READ = "No se pudo leer el archivo: ";
    public static final String ERR_STORAGE_WRITE = "No se pudo escribir el archivo: ";
    public static final String ERR_INDEX_SORT = "No se pudo ordenar el índice: ";
    public static final String ERR_INDEX_READ = "No se pudo leer el índice: ";
    public static final String ERR_INDEX_WRITE = "No se pudo escribir el índice: ";
    public static final String ERR_NULL_INDEX_FILE = "El archivo del índice y la carpeta de temporales no pueden ser null";
    public static final String ERR_TEMP_DELETE = "No se pudo borrar el archivo temporal: ";
    public static final String ERR_INDEX_OUT_OF_RANGE = "Posición fuera del índice: ";
    public static final String ERR_UNEXPECTED_RECORD_TYPE = "Tipo de registro inesperado: ";
    public static final String ERR_UNKNOWN_SYNTH_CODE = "Código de synth desconocido: ";
    public static final String ERR_UNKNOWN_ACTION_CODE = "Código de acción desconocido: ";
    public static final String ERR_ALREADY_RECORDING = "Ya hay una grabación en curso o pendiente de nombre";
    public static final String ERR_NOT_RECORDING = "No hay una grabación en curso";
    public static final String ERR_NO_PENDING_RECORDING = "No hay una grabación pendiente de nombre";
    public static final String ERR_RECORDING_NOT_FOUND = "No existe una grabación con el nombre: ";
    public static final String ERR_ALREADY_PLAYING = "Ya hay una reproducción en curso";
    public static final String ERR_PLAYER_SEND = "No se pudo enviar un comando de la grabación: ";
    public static final String ERR_PLAYER_READ = "No se pudo leer la grabación: ";
    public static final String ERR_NULL_HEADER = "El encabezado no puede ser null";
    public static final String ERR_NULL_LIBRARY_DEPENDENCY = "El almacenamiento y el índice no pueden ser null";
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

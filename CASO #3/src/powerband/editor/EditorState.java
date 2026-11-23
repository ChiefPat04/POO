package powerband.editor;

/**
 * Estado del editor. Decide qué acciones se permiten.
 */
public enum EditorState {
    IDLE,
    RECORDING,
    /** La grabación se detuvo y espera un nombre (o descartarse). */
    NAMING,
    PLAYING
}

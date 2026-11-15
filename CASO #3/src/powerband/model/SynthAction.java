package powerband.model;

/**
 * Acción de un evento: disparar (piano y bajo) o encender/apagar una nota (órgano).
 */
public enum SynthAction {
    TRIGGER,
    NOTE_ON,
    NOTE_OFF
}

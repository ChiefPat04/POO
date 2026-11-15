package powerband.command;

import powerband.model.SynthEvent;

/**
 * Observador de los comandos enviados a SuperCollider (lo usa el editor).
 * Se llama desde el hilo del instrumento que tocó la nota.
 */
public interface ISynthEventListener {

    void onSynthEvent(SynthEvent pEvent);
}

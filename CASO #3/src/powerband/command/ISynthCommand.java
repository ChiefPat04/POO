package powerband.command;

import powerband.exception.SynthConnectionException;
import powerband.model.SynthEvent;

/**
 * Implementación del patrón Bridge: sabe cómo invocar un synth en SuperCollider.
 * Los instrumentos (abstracción) y el reproductor la usan sin conocer el synth concreto.
 */
public interface ISynthCommand {

    /**
     * Envía el evento a su synth y lo notifica a los listeners.
     *
     * @throws IllegalArgumentException si el evento no corresponde a este comando
     */
    void execute(SynthEvent pEvent) throws SynthConnectionException;

    void addListener(ISynthEventListener pListener);

    void removeListener(ISynthEventListener pListener);
}

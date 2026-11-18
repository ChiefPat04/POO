package powerband.editor;

/**
 * Recibe el aviso de que una reproducción terminó, sola o porque se detuvo.
 * La ventana del editor (ft-8) lo usa para reactivar sus botones.
 */
public interface IPlaybackListener {

    void onPlaybackFinished(String pName);
}

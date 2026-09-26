package mutantes.game;
// Vive en game, no en ui: CampoDeBatalla (el sujeto del patron Observer)
// necesita esta interfaz para notificar observadores sin depender de la
// capa de interfaz grafica. Es ui quien la implementa desde aqui.
public interface ObservadorBatalla {
    void alActualizarEstado();
}
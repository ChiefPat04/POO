package mutantes.game;

public enum ColorEquipo {
    ROJO,
    AZUL
}

//Un enum de solo dos valores, porque el juego siempre tiene exactamente dos equipos. 
// Igual que con TipoEfecto, esto le va a servir después a la UI para decidir con qué java.awt.Color pintar cada bando, 
// sin que game tenga que saber nada de pintura.
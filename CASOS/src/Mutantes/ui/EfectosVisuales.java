package mutantes.ui;

import java.awt.Color;
import mutantes.model.poderes.TipoEfecto;

public final class EfectosVisuales {

    private EfectosVisuales() {
    }

    public static Color colorParaEfecto(TipoEfecto tipo) {
        switch (tipo) {
            case FUEGO: return Color.RED;
            case RAYO: return Color.CYAN;
            case REGENERACION: return Color.GREEN;
            case ESCUDO_ENERGIA: return Color.BLUE;
            case INVISIBILIDAD: return Color.LIGHT_GRAY;
            case ROBO_ENERGIA: return Color.MAGENTA;
            case TELETRANSPORTACION: return Color.ORANGE;
            case DESCARGA_ELECTRICA: return Color.YELLOW;
            case LLAMARADA: return new Color(255, 100, 0);
            case VENENO: return new Color(120, 200, 40);
            case EXPLOSION: return new Color(180, 0, 0);
            default: return Color.WHITE;
        }
    }

    public static String simboloParaEfecto(TipoEfecto tipo) {
        switch (tipo) {
            case FUEGO: return "F";
            case RAYO: return "R";
            case REGENERACION: return "+";
            case ESCUDO_ENERGIA: return "E";
            case INVISIBILIDAD: return "?";
            case ROBO_ENERGIA: return "-";
            case TELETRANSPORTACION: return ">";
            case DESCARGA_ELECTRICA: return "Z";
            case LLAMARADA: return "L";
            case VENENO: return "V";
            case EXPLOSION: return "X";
            default: return "";
        }
    }

    public static String nombreLegibleParaEfecto(TipoEfecto tipo) {
        switch (tipo) {
            case FUEGO: return "Fuerza";
            case RAYO: return "Velocidad";
            case REGENERACION: return "Regeneracion";
            case ESCUDO_ENERGIA: return "Escudo de Energia";
            case INVISIBILIDAD: return "Invisibilidad";
            case ROBO_ENERGIA: return "Robo de Energia";
            case TELETRANSPORTACION: return "Teletransportacion";
            case DESCARGA_ELECTRICA: return "Descarga Electrica";
            case LLAMARADA: return "Llamarada";
            case VENENO: return "Veneno";
            case EXPLOSION: return "Explosion";
            default: return "";
        }
    }

    public static String descripcionParaEfecto(TipoEfecto tipo) {
        switch (tipo) {
            case FUEGO: return "Ataque fisico basico, respeta la defensa del objetivo.";
            case RAYO: return "Ataque rapido basico, respeta la defensa del objetivo.";
            case REGENERACION: return "No ataca: recupera energia para su propio dueno.";
            case ESCUDO_ENERGIA: return "No ataca: activa un escudo que reduce a la mitad el proximo golpe recibido.";
            case INVISIBILIDAD: return "No ataca: se vuelve invisible, el siguiente golpe recibido no le hace nada.";
            case ROBO_ENERGIA: return "Ataca al enemigo y cura a su dueno con parte del dano hecho.";
            case TELETRANSPORTACION: return "Le permite reposicionarse en el campo de batalla.";
            case DESCARGA_ELECTRICA: return "Ataque de alto dano que ignora por completo la defensa del objetivo.";
            case LLAMARADA: return "Ataque de alto dano (el doble de lo normal), respeta la defensa del objetivo.";
            case VENENO: return "Ataque proporcional a la energia actual del objetivo: mas doloroso contra rivales sanos que contra los ya debilitados.";
            case EXPLOSION: return "Ataque de alto dano (el doble de lo normal) que ademas ignora la defensa del objetivo.";
            default: return "";
        }
    }
}
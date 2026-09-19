package mutantes.game;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import mutantes.model.Mutante;

public class Equipo {

    private final ColorEquipo color;
    private final String simbolo;
    private final List<Mutante> mutantes;
    private int vivos;
    private int muertos;

    public Equipo(ColorEquipo color, String simbolo) {
        this.color = color;
        this.simbolo = simbolo;
        this.mutantes = new ArrayList<>();
        this.vivos = 0;
        this.muertos = 0;
    }

    public void agregarMutante(Mutante mutante){
        mutantes.add(mutante);
        vivos++;
    }

    public synchronized void actualizarMarcador(){
        int vivosActuales = 0;

        for (Mutante mutante : mutantes) {
            if (mutante.estaVivo()) {
                vivosActuales++;
            }
        }

        this.vivos = vivosActuales;
        this.muertos = mutantes.size() - vivosActuales;
    }

    public synchronized boolean estaDerrotado() {
        return vivos == 0;
    }

    public List<Mutante> getMutantes() {
        return Collections.unmodifiableList(mutantes);
    }

    public ColorEquipo getColor() {
        return color;
    }

    public String getSimbolo() {
        return simbolo;
    }

    public int getVivos() {
        return vivos;
    }

    public int getMuertos() {
        return muertos;
    }
}

//List es una interfaz de Java (define "qué puede hacer" una lista: agregar, recorrer, etc.), y ArrayList es una implementación concreta de esa interfaz (el "cómo" lo hace por dentro).
//actualizarMarcador() recorre la lista en vez de llevar la cuenta a mano.
//Collections.unmodifiableList(...) envuelve la lista real en una versión de "solo lectura": quien la reciba puede recorrerla y leerla, pero si intenta modificarla, Java lanza un error inmediatamente. Así protegemos la única forma válida de agregar mutantes: pasar por agregarMutante().
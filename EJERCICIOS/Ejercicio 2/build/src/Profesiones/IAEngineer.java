package profesiones;

import personas.Persona;

public class IAEngineer extends Persona {
	// private protege los datos propios del ingeniero de IA.
    private String especialidad;
    private int proyectosDiseñados;
    private int aplicacionesCreadas;
    private double horasTrabajadas;


    public IAEngineer(String nombre, byte edad, String especialidad) {
        super(nombre, edad);
        this.especialidad = especialidad;
        this.proyectosDiseñados = 0;
        this.aplicacionesCreadas = 0;
        this.horasTrabajadas = 0;
    }

    // Getter public: permite consultar la especialidad desde otra clase.
    public String getEspecialidad() {
        return especialidad;
    }

    // Setter public: permite cambiar la especialidad de forma controlada.
    public void setEspecialidad(String especialidad) {
        if (especialidad != null && !especialidad.trim().isEmpty()) {
            this.especialidad = especialidad;
        }
    }

    // Getter y setter: consultan y actualizan el contador de proyectos.
    public int getProyectosDiseñados() {
        return proyectosDiseñados;
    }

    public void setProyectosDiseñados(int proyectosDiseñados) {
        if (proyectosDiseñados >= 0) {
            this.proyectosDiseñados = proyectosDiseñados;
        }
    }

    // Getter y setter: consultan y actualizan el contador de aplicaciones.
    public int getAplicacionesCreadas() {
        return aplicacionesCreadas;
    }

    public void setAplicacionesCreadas(int aplicacionesCreadas) {
        if (aplicacionesCreadas >= 0) {
            this.aplicacionesCreadas = aplicacionesCreadas;
        }
    }

    // Getter y setter: consultan y actualizan las horas acumuladas.
    public double getHorasTrabajadas() {
        return horasTrabajadas;
    }

    public void setHorasTrabajadas(double horasTrabajadas) {
        if (horasTrabajadas >= 0 && Double.isFinite(horasTrabajadas)) {
            this.horasTrabajadas = horasTrabajadas;
        }
    }

    @Override
    public void trabajar() {
        System.out.println(identidad() + " trabaja como ingeniero de IA en " + especialidad + ".");
    }

    // Accion public: cualquier parte del programa puede pedirle diseñar una IA.
    public void diseñarIA(String nombreProyecto, String tipoModelo, int cantidadDatos) {
        if (cantidadDatos <= 0) {
            System.out.println("El proyecto necesita una cantidad de datos mayor que cero.");
            return;
        }

        proyectosDiseñados++;
        System.out.println(getNombre() + " diseña la IA " + nombreProyecto
                + " usando un modelo de " + tipoModelo + " con " + cantidadDatos + " datos.");
    }

    public void crearAplicacion(String nombreAplicacion, int horasEstimadas) {
        if (horasEstimadas <= 0) {
            System.out.println("Las horas estimadas deben ser mayores que cero.");
            return;
        }

        aplicacionesCreadas++;
        horasTrabajadas += horasEstimadas;
        System.out.println(getNombre() + " crea la aplicacion " + nombreAplicacion
                + " en " + horasEstimadas + " horas.");
    }

    public void entrenarModelo() {
        System.out.println(getNombre() + " entrena un modelo de IA en " + especialidad
                + " durante " + horasTrabajadas + " horas.");
    }


    

}
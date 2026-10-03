package profesiones;

import personas.Persona;

public class Mecánico extends Persona {
	// private: los datos del mecanico quedan protegidos de cambios directos.
	private String especialidad;
	private int vehiculosReparados;
	private double horasTrabajadas;
	private double ingresos;

	public Mecánico(String nombre, byte edad, String especialidad) {
		super(nombre, edad);
		this.especialidad = especialidad;
		this.vehiculosReparados = 0;
		this.horasTrabajadas = 0;
		this.ingresos = 0;
	}

	// Getter public: consulta el tipo de especialidad del mecanico.
	public String getEspecialidad() {
		return especialidad;
	}

	// Setter public: cambia la especialidad de forma controlada.
	public void setEspecialidad(String especialidad) {
		if (especialidad != null && !especialidad.trim().isEmpty()) {
			this.especialidad = especialidad;
		}
	}

	// Getter y setter: consultan y actualizan los vehiculos reparados.
	public int getVehiculosReparados() {
		return vehiculosReparados;
	}

	public void setVehiculosReparados(int vehiculosReparados) {
		if (vehiculosReparados >= 0) {
			this.vehiculosReparados = vehiculosReparados;
		}
	}

	// Getter y setter: consultan y actualizan las horas trabajadas.
	public double getHorasTrabajadas() {
		return horasTrabajadas;
	}

	public void setHorasTrabajadas(double horasTrabajadas) {
		if (horasTrabajadas >= 0 && Double.isFinite(horasTrabajadas)) {
			this.horasTrabajadas = horasTrabajadas;
		}
	}

	// Getter y setter: consultan y actualizan los ingresos acumulados.
	public double getIngresos() {
		return ingresos;
	}

	public void setIngresos(double ingresos) {
		if (ingresos >= 0 && Double.isFinite(ingresos)) {
			this.ingresos = ingresos;
		}
	}

	@Override
	public void trabajar() {
		System.out.println(identidad() + " trabaja como mecanico especializado en " + especialidad + ".");
	}

	// Acciones public: representan las tareas propias del mecanico.
	public void repararVehiculo() {
		vehiculosReparados++;
		System.out.println(getNombre() + " repara un vehículo de tipo " + especialidad + ".");
	}

	public void diagnosticarVehiculo(String vehiculo, String problema) {
		System.out.println(getNombre() + " diagnostica " + vehiculo + ": " + problema + ".");
	}

	public boolean cobrarReparacion(double horas, double precioPorHora) {
		if (horas <= 0 || precioPorHora <= 0) {
			return false;
		}

		horasTrabajadas += horas;
		ingresos += horas * precioPorHora;
		System.out.println(getNombre() + " cobra " + (horas * precioPorHora)
				+ " por la reparacion.");
		return true;
	}
}

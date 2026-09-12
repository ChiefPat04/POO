# Juego de Batalla de Mutantes 

* **Curso:** Programación Orientada a Objetos (POO)
* **Institución:** Tecnológico de Costa Rica (TEC)
* **Autores:** Elian Montero y Patrick Zúñiga
* **Fecha de entrega:** Viernes 25 de septiembre de 2026

---

## 1. Descripción general

Este proyecto consiste en un juego de simulación automática de batallas entre dos equipos de mutantes, desarrollado en Java. 

El usuario solo debe ingresar la cantidad de mutantes por equipo (un número entre 3 y 11, igual para ambos bandos). A partir de ahí, el sistema genera los mutantes con atributos aleatorios, los coloca en sus zonas del tablero e inicia la simulación. La batalla ocurre de forma automática mediante hilos hasta que todos los integrantes de un equipo queden eliminados.

Al finalizar la partida, la interfaz permite reiniciar el juego y probar con un nuevo tamaño de equipo.

### Organización por Capas (MVC + Observer)

Para mantener el código ordenado y fácil de mantener, dividimos el proyecto en los siguientes paquetes:

* **Modelo (`model`):** Contiene la lógica individual de los mutantes, sus atributos (energía, defensa, posición), movimiento y el funcionamiento de sus poderes.
* **Juego (`game`):** Maneja el estado general de la partida de forma independiente a la interfaz. Administra los equipos, el tablero y los marcadores.
* **Control (`control`):** Se encarga de la concurrencia. Maneja los hilos de movimiento, detecta cuándo dos mutantes entran en su radio de visión y procesa los combates.
* **Interfaz Gráfica (`ui`):** Implementada con Swing. Muestra la pantalla de juego y se actualiza automáticamente escuchando los cambios del modelo mediante el patrón Observer.
* **Constantes (`constants`):** Interfaz `IConstants` donde centralizamos todos los valores fijos del sistema para no tener números quemados en el código.

---

## 2. Especificación de Clases

*Nota: Todos los atributos son privados a menos que se indique lo contrario. Para respetar la encapsulación, no agregamos setters para modificar la energía o las listas de equipos de forma arbitraria.*

---

### 2.1. Capa Modelo (`model`)

#### Enumeración `AccionCombate`
* **Valores:** `ATACAR`, `DEFENDER`.
* **Descripción:** Representa la decisión aleatoria que toma un mutante cuando se cruza con un enemigo.

#### Clase `Mutante`
* **Atributos:**
  * `nombre`: `String` (nombre o identificador del mutante).
  * `energia`: `int` (empieza en 100; si llega a 0 se elimina).
  * `defensa`: `int` (valor aleatorio entre 1 y 3).
  * `poder`: `PoderMutante` (el poder asignado al mutante).
  * `posicionX`, `posicionY`: `int` (coordenadas actuales en el mapa).
  * `limiteX`, `limiteY`: `int` (límites del mapa guardados desde la creación).
  * `radioDeteccion`: `int` (distancia para detectar a un enemigo).
  * `vivo`: `boolean` (indica si sigue activo en la partida).
* **Métodos principales:**
  * `Mutante(String nombre, int defensa, PoderMutante poder, int limiteX, int limiteY, int radioDeteccion)`: Constructor principal que recibe los límites y atributos base.
  * `moverse()`: Cambia la posición del mutante al azar sin salirse de los límites (`limiteX`, `limiteY`).
  * `decidirAccion()`: Retorna `AccionCombate` (`ATACAR` o `DEFENDER`) de forma aleatoria.
  * `atacar(Mutante objetivo)`: Aplica el poder sobre el mutante objetivo llamando a `poder.aplicarEfecto(objetivo)`.
  * `recibirDano(int cantidad)`: Resta vida según el daño recibido (mínimo 0) y actualiza el estado de `vivo`.
  * `defender()`: Devuelve el valor de defensa para reducir el daño del rival.
  * `estaVivo()`: Retorna `true` si la energía es mayor a 0.

#### Clase Abstracta `PoderMutante`
* **Atributos:**
  * `nombre`: `String`.
  * `dano`: `int` (daño base entre 1 y 3; sube hasta un máximo de 7).
* **Métodos principales:**
  * `PoderMutante(String nombre, int dano)`
  * `abstract void aplicarEfecto(Mutante objetivo)`: Define el efecto del ataque sobre el objetivo.
  * `aumentarDano()`: Incrementa el daño en +1 sin pasar del límite de 7.
  * `getDano()`: `int`.

#### Tipos de Poderes (`PoderMutante`)
* **`PoderFuerza`:** Realiza daño directo y potenciado contra el enemigo.
* **`PoderVelocidad`:** Modifica la frecuencia de movimiento o el alcance del mutante.
* **`PoderRegeneracion`:** Además de hacerle daño al rival, le recupera un porcentaje de energía al atacante.

---

### 2.2. Capa Juego (`game`)

#### Clase `Equipo`
* **Atributos:**
  * `color`: `java.awt.Color` (color asignado al equipo).
  * `simbolo`: `java.awt.image.BufferedImage` (imagen del escudo del equipo).
  * `mutantes`: `List<Mutante>` (lista de integrantes).
  * `vivos`: `int`.
  * `muertos`: `int`.
* **Métodos principales:**
  * `agregarMutante(Mutante m)`: Añade un nuevo mutante a la lista.
  * `actualizarMarcador()`: Recorre la lista y recalcula cuántos mutantes quedan vivos y cuántos murieron.
  * `estaDerrotado()`: Retorna `true` cuando ya no quedan mutantes vivos.

#### Clase `CampoDeBatalla`
*(Extiende de `java.util.Observable` para notificar cambios a la interfaz).*
* **Atributos:**
  * `ancho`, `alto`: `int`.
  * `equipoA`, `equipoB`: `Equipo`.
  * `radioDeteccionDefault`: `int`.
  * `velocidad`: `double`.
  * `enEjecucion`: `boolean`.
* **Métodos principales:**
  * `crearEquipos(int tamano)`: Genera los dos equipos según la cantidad elegida.
  * `iniciarJuego()`: Activa la simulación y pone el campo en marcha.
  * `hayGanador()`: Revisa si alguno de los dos equipos perdió a todos sus integrantes.
  * `getGanador()`: Devuelve el equipo que quedó en pie.
  * `notificarCambio()`: Llama a `setChanged()` y `notifyObservers()` para pedirle a la UI que se redibuje.

---

### 2.3. Capa Control (`control`)

#### Estrategia de Concurrencia (Opción A: Hilo por Mutante)
Para mantener el código simple de entender y depurar, cada mutante corre en su propio hilo independiente (`Thread`), mientras que la lógica de colisiones y peleas se coordina de forma centralizada en `GestorCombate`.

#### Clase `HiloMutante` (Implementa `java.lang.Runnable`)
* **Atributos:**
  * `mutante`: `Mutante`.
  * `campo`: `CampoDeBatalla`.
  * `gestor`: `GestorCombate`.
* **Métodos principales:**
  * `run()`: Bucle principal que mueve al mutante, revisa si hay enemigos cerca y le pide al gestor resolver la pelea si aplica. Se repite periódicamente mientras el juego siga activo y el mutante esté vivo.

#### Clase `GestorCombate`
* **Atributos:**
  * `campo`: `CampoDeBatalla`.
  * `paresEnContacto`: `Set<String>` (registro de mutantes peleando para evitar que sigan atacándose en cada ciclo mientras sigan cerca).
* **Métodos principales:**
  * `resolverEncuentro(Mutante mutante1, Mutante mutante2)`: Método `synchronized` que revisa las acciones de ambos (`ATACAR` o `DEFENDER`), calcula el daño final y actualiza las vidas sin problemas de concurrencia.

---

### 2.4. Capa UI (`ui`)

*(Se utiliza el patrón `java.util.Observer` estándar para refrescar la pantalla).*

#### Clase `VentanaBatalla` (Extiende `javax.swing.JFrame`)
* Ventana principal de la aplicación. Muestra los controles para iniciar una nueva partida, escoger la cantidad de mutantes y contiene los paneles del juego.

#### Clase `PanelCampoBatalla` (Extiende `javax.swing.JPanel`, Implementa `java.util.Observer`)
* **`update(Observable o, Object arg)`:** Recibe el aviso del modelo y ejecuta `repaint()` dentro del hilo de Swing (`SwingUtilities.invokeLater`).
* **`paintComponent(Graphics g)`:** Dibuja el terreno de juego, las posiciones de los mutantes con su respectivo color y las animaciones/efectos básicos de ataque.

#### Clase `PanelMarcador` (Extiende `javax.swing.JPanel`, Implementa `java.util.Observer`)
* **`update(Observable o, Object arg)`:** Actualiza las barras de vida de cada bando, el contador de vivos/muertos y muestra un mensaje emergente indicando cuál equipo ganó al terminar.

---

### 2.5. Constantes (`constants`)

#### Interfaz `IConstants`
Contiene los parámetros y configuraciones globales del juego para evitar hardcodear valores.


public interface IConstants {
    int ENERGIA_INICIAL = 100;
    int DEFENSA_MIN = 1;
    int DEFENSA_MAX = 3;
    int DANO_MIN = 1;
    int DANO_MAX = 3;
    int DANO_MAX_PODER = 7;
    
    int TAMANO_EQUIPO_MIN = 3;
    int TAMANO_EQUIPO_MAX = 11;
    
    int RADIO_DETECCION_DEFAULT = 40;
    int REFRESH_RATE_MS = 33; // Refresco de pantalla (~30 FPS)
    int VELOCIDAD_DEFAULT = 60;
}


## 3. Diagrama UML (PlantUML)
@startuml
skinparam classAttributeIconSize 0

package constants {
  interface IConstants
}

package model {
  enum AccionCombate {
    ATACAR
    DEFENDER
  }

  abstract class PoderMutante {
    -nombre: String
    -dano: int
    +aplicarEfecto(objetivo: Mutante)
    +aumentarDano()
    +getDano(): int
  }

  class PoderFuerza extends PoderMutante
  class PoderVelocidad extends PoderMutante
  class PoderRegeneracion extends PoderMutante

  class Mutante {
    -nombre: String
    -energia: int
    -defensa: int
    -poder: PoderMutante
    -posicionX: int
    -posicionY: int
    -limiteX: int
    -limiteY: int
    -radioDeteccion: int
    -vivo: boolean
    +moverse()
    +decidirAccion(): AccionCombate
    +atacar(objetivo: Mutante)
    +recibirDano(cantidad: int)
    +defender(): int
    +estaVivo(): boolean
  }
}

package game {
  class Equipo {
    -color: java.awt.Color
    -simbolo: java.awt.image.BufferedImage
    -mutantes: List<Mutante>
    -vivos: int
    -muertos: int
    +agregarMutante(m: Mutante)
    +actualizarMarcador()
    +estaDerrotado(): boolean
  }

  class CampoDeBatalla {
    -ancho: int
    -alto: int
    -velocidad: double
    -equipoA: Equipo
    -equipoB: Equipo
    -radioDeteccionDefault: int
    -enEjecucion: boolean
    +crearEquipos(tamano: int)
    +iniciarJuego()
    +hayGanador(): boolean
    +getGanador(): Equipo
    +notificarCambio()
  }
}

package control {
  class HiloMutante {
    -mutante: Mutante
    -campo: CampoDeBatalla
    -gestor: GestorCombate
    +run()
  }

  class GestorCombate {
    -campo: CampoDeBatalla
    -paresEnContacto: Set<String>
    +resolverEncuentro(mutante1: Mutante, mutante2: Mutante)
  }
}

package ui {
  class VentanaBatalla
  class PanelCampoBatalla
  class PanelMarcador
}

class "java.util.Observable" as Observable
interface "java.util.Observer" as Observer
interface "java.lang.Runnable" as Runnable
class "javax.swing.JFrame" as JFrame
class "javax.swing.JPanel" as JPanel

PoderMutante <|-- PoderFuerza
PoderMutante <|-- PoderVelocidad
PoderMutante <|-- PoderRegeneracion

Mutante "1" *-- "0..1" PoderMutante
Mutante ..> AccionCombate
Equipo "1" *-- "3..11" Mutante
CampoDeBatalla "1" *-- "2" Equipo
Observable <|-- CampoDeBatalla

Runnable <|.. HiloMutante
HiloMutante --> Mutante
HiloMutante --> CampoDeBatalla
HiloMutante --> GestorCombate
GestorCombate --> CampoDeBatalla

JFrame <|-- VentanaBatalla
JPanel <|-- PanelCampoBatalla
JPanel <|-- PanelMarcador
Observer <|.. PanelCampoBatalla
Observer <|.. PanelMarcador

VentanaBatalla *-- PanelCampoBatalla
VentanaBatalla *-- PanelMarcador
PanelCampoBatalla --> CampoDeBatalla
PanelMarcador --> CampoDeBatalla
Observable ..> Observer : notifica
@enduml

## 4 Decisiones de diseño

4.1 Zonas de inicio: Para evitar que los mutantes peleen apenas inicia la partida, los de un equipo aparecen en la mitad izquierda del mapa y los del otro equipo en la mitad derecha.

4.2 Muerte de mutantes: Cuando la energía de un mutante llega a 0, su hilo finaliza. El objeto se mantiene dibujado en la pantalla como eliminado y se descarta de la lógica de peleas.

4.3 Control de peleas repetidas: Cuando dos enemigos entran en el radioDeteccion, el GestorCombate guarda la pareja en paresEnContacto. Esto evita que sigan atacando en cada milisegundo mientras estén a la par; el ataque se procesa una vez por encuentro.

4.4 Sincronización: Usamos un hilo por mutante (HiloMutante) y delegamos los combates a GestorCombate, que utiliza métodos sincronizados (synchronized) para evitar problemas cuando dos hilos intenten modificar la energía al mismo tiempo.

```java
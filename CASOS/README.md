# Juego de Batalla de Mutantes 

**Curso:** Programación Orientada a Objetos  
**Instituto Tecnológico de Costa Rica**  
**Fecha de entrega final:** viernes 25 de septiembre  

**Autores:**  
Elian Montero  
Patrick Zúñiga  

---

## 1. Descripción general

El sistema implementa un juego automático de batalla de mutantes. El único dato que ingresa el usuario es el tamaño de los equipos (un valor entre 3 y 11, igual para ambos). A partir de ahí, el sistema genera mutantes aleatorios para dos equipos y ejecuta la batalla de forma completamente automática, sin intervención adicional del usuario, hasta que uno de los dos equipos pierde a todos sus mutantes.

El proyecto se organiza en cuatro capas con responsabilidades separadas:

| Capa | Paquete | Responsabilidad |
|---|---|---|
| Modelo | `model` | Representar mutantes, poderes, energía y defensa |
| Juego | `game` | Representar el campo de batalla, los equipos y el marcador |
| Control | `control` | Movimiento, detección de enemigos y resolución de combates (concurrente) |
| UI | `ui` | Dibujar el estado del juego en tiempo real (Swing + Observer + MVC) |

---

## 2. Especificación de clases

### 2.1 Capa Modelo (`model`)

**Clase `Mutante`**

* `- id: int` (Identificador único del mutante)
* `- nombre: String` (Nombre o identificador)
* `- energia: int` (Energía actual, inicia en `100`)
* `- defensa: int` (Capacidad de defensa, entre `1` y `3`)
* `- poder: PoderMutante` (El poder que porta el mutante)
* `- posicionX: int`, `- posicionY: int` (Posición actual en el campo)
* `- limiteX: int`, `- limiteY: int` (Límites del campo de batalla)
* `- radioDeteccion: int` (Radio dentro del cual detecta enemigos)
* `- vivo: boolean` (Estado de vida)

*Métodos principales:* `recibirDano(int cantidad)`, `defender()`, `atacar(Mutante objetivo)`, `estaVivo(): boolean`, `moverse()`.

**Clase abstracta `PoderMutante`**

* `- nombre: String` (Nombre del poder)
* `- dano: int` (Daño base, entre `1` y `3`, sube hasta un máximo de `7`)
* `- colorEfecto: java.awt.Color` (Color o identificador gráfico del ataque para la UI)

*Métodos principales:* `abstract aplicarEfecto(Mutante objetivo)`, `aumentarDano()`, `getColorEfecto()`.

Para cumplir con el uso de herencia y polimorfismo, `PoderMutante` es abstracta y se definen subclases concretas:

- `PoderFuerza`: aumenta el daño directo de ataque.
- `PoderVelocidad`: afecta la frecuencia de movimiento o el radio de detección.
- `PoderRegeneracion`: recupera parte de la energía propia con cada golpe exitoso.

Cada subclase sobrescribe `aplicarEfecto(objetivo)` con su propio comportamiento y define su propio `colorEfecto` para la diferenciación visual en la UI.

---

### 2.2 Capa Juego (`game`)

**Clase `Equipo`**

* `- color: java.awt.Color` (Color identificador del equipo)
* `- simbolo: java.awt.image.BufferedImage` (Escudo o símbolo del equipo)
* `- mutantes: List<Mutante>` (Mutantes del equipo)
* `- vivos: int`, `- muertos: int` (Marcador del equipo)

*Métodos principales:* `agregarMutante(Mutante m)`, `actualizarMarcador()`, `estaDerrotado(): boolean`.

**Clase `CampoDeBatalla`**

* `- ancho: int`, `- alto: int` (Dimensiones del campo)
* `- equipoA: Equipo`, `- equipoB: Equipo` (Los dos equipos en juego)
* `- radioDeteccionDefault: int` (Valor por defecto del radio de detección)

*Métodos principales:* `crearEquipos(int tamano)`, `getDimensiones(): int[]`, `hayGanador(): boolean`, `getGanador(): Equipo`.

---

### 2.3 Capa Control (`control`)

**Clase `HiloMutante` (implementa `Runnable`)**

* `- mutante: Mutante`
* `- campo: CampoDeBatalla`
* `- gestor: GestorCombate`

*Métodos principales:* `run()`, `detectarEnemigosEnRadio(): List<Mutante>`.

**Clase `GestorCombate`**

* `- campo: CampoDeBatalla`
* `- paresEnContacto: Set<String>`

*Método principal:* `resolverEncuentro(Mutante atacante, Mutante defensor)`.

---

### 2.4 Capa UI (`ui`)

**Interfaz `ObservadorBatalla`** (o `java.util.Observer`)
* `alActualizarEstado()`

**Clase `VentanaBatalla` (extiende `JFrame`)**
Contenedor principal de la aplicación; incluye la configuración de equipos y el botón de nueva partida.

**Clase `PanelCampoBatalla` (extiende `JPanel`, implementa `ObservadorBatalla`)**
Dibuja el campo y los mutantes vivos, coloreados según su equipo y resaltando el efecto visual según `colorEfecto`.

**Clase `PanelMarcador` (extiende `JPanel`, implementa `ObservadorBatalla`)**
Muestra energía, vivos/muertos por equipo y anuncia al ganador.

---

### 2.5 Constantes (`constants`)

**Interfaz `IConstants`**


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
    int REFRESH_RATE_MS = 33;
}


## 3 Diagrama UML (PlantUML)
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
    -colorEfecto: java.awt.Color
    +aplicarEfecto(objetivo: Mutante)
    +aumentarDano()
    +getColorEfecto(): java.awt.Color
  }

  class PoderFuerza extends PoderMutante
  class PoderVelocidad extends PoderMutante
  class PoderRegeneracion extends PoderMutante

  class Mutante {
    -id: int
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
    +recibirDano(cantidad: int)
    +defender(): int
    +atacar(objetivo: Mutante)
    +estaVivo(): boolean
    +moverse()
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
    -equipoA: Equipo
    -equipoB: Equipo
    -radioDeteccionDefault: int
    +crearEquipos(tamano: int)
    +getDimensiones(): int[]
    +hayGanador(): boolean
    +getGanador(): Equipo
  }
}

package control {
  class HiloMutante {
    -mutante: Mutante
    -campo: CampoDeBatalla
    -gestor: GestorCombate
    +run()
    +detectarEnemigosEnRadio(): List<Mutante>
  }

  class GestorCombate {
    -campo: CampoDeBatalla
    -paresEnContacto: Set<String>
    +resolverEncuentro(atacante: Mutante, defensor: Mutante)
  }
}

package ui {
  interface ObservadorBatalla {
    +alActualizarEstado()
  }

  class VentanaBatalla
  class PanelCampoBatalla
  class PanelMarcador
}

PoderMutante <|-- PoderFuerza
PoderMutante <|-- PoderVelocidad
PoderMutante <|-- PoderRegeneracion

Mutante "1" *-- "0..1" PoderMutante
Equipo "1" *-- "3..11" Mutante
CampoDeBatalla "1" *-- "2" Equipo

HiloMutante ..> Mutante
HiloMutante ..> CampoDeBatalla
HiloMutante ..> GestorCombate
GestorCombate ..> Mutante

ObservadorBatalla <|.. PanelCampoBatalla
ObservadorBatalla <|.. PanelMarcador
VentanaBatalla *-- PanelCampoBatalla
VentanaBatalla *-- PanelMarcador
@enduml
```java

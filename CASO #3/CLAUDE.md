# Project description
Power Band es un programa de creación e interacción musical. Desde Java se invocan 3 sintetizadores (synths) que corren en un servidor de SuperCollider (`scsynth`). Cada synth se controla con un dispositivo físico distinto (nunca teclado ni mouse). Un editor musical monitorea todos los comandos que se envían a SuperCollider y permite grabarlos, guardarlos en disco, cargarlos y reproducirlos de forma idéntica a como se tocaron.

Proyecto en pareja del curso de POO (Caso #3). El enunciado completo está en `docs/enunciado.md`.

# Tech stack
- Java (versión en `maven.compiler.release` de `pom.xml`)
- Maven project
- Java Swing para el UI del editor musical
- JUnit 5 para unit testing
- GitHub como control de versiones
- SuperCollider como servidor de sonido. Los synths están en `recursos/supercollider/myband.sc`.
- Comunicación con SuperCollider por OSC sobre UDP (puerto por defecto `57110`), usando `/s_new` y `/n_set`. La referencia del profesor es `recursos/supercollider/BandClient.java` (no se compila; es solo guía).

# Layout
- El entrypoint (`public static void main`) está en `src/powerband/PowerBandMain.java`.
- `src/powerband/instrument/` — instrumentos (abstracción del Bridge).
- `src/powerband/command/` — comandos de synth (implementación del Bridge).
- `src/powerband/sc/` — cliente OSC hacia SuperCollider.
- `src/powerband/editor/` — monitor, grabador y reproductor.
- `src/powerband/storage/` — archivos binarios, Balanced Merge Sort y búsqueda binaria.
- `src/powerband/model/` — clases de dominio (registros, encabezados, eventos).
- `src/powerband/ui/` — interfaz Swing del editor.
- `src/powerband/exception/` — excepciones personalizadas.
- `src/powerband/util/IConstants.java` — todas las constantes.
- Pruebas en `src/test/powerband/...`, con la misma estructura de paquetes.
- Archivos de datos generados en ejecución: `data/recordings.music`, `data/recordings.index`, `data/recordings.list`.
- Paquetes en minúscula, clases en `PascalCase`, métodos y campos en `camelCase`, constantes en `UPPER_SNAKE_CASE`.
- Un tipo público por archivo; el nombre del archivo coincide con el tipo.

# Synths (definidos en myband.sc)
- `classicPiano`: acorde a partir de una nota. Parámetros: `note` (MIDI), `durMs`, `third` (4 = mayor, 3 = menor), `amp`. Se dispara y termina solo.
- `stringBass`: nota de bajo. Parámetros: `note` (MIDI), `durMs`, `amp`. Se dispara y termina solo.
- `organ70s`: nota sostenida. `noteOn` con `freq` (desde la nota MIDI), `r1..r3` (ratios) y `a1..a3` (amplitudes). Suena hasta `noteOff` (`/n_set gate 0`).

# Instrumentos (dispositivos)
- Micrófono → `organ70s`. Entra violín, guitarra, ukelele o voz. Se detecta la nota (pitch) y el volumen; inicio de sonido = `noteOn`, silencio = `noteOff`.
- Control de PS5 (DualSense, USB o Bluetooth) → `stringBass`. Botones disparan notas, stick cambia la nota base, R2 controla `durMs`, L1/R1 cambian de octava.
- Webcam → `classicPiano`. Altura de la mano = nota, lado de la imagen = mayor o menor, movimiento brusco = disparo.

# Patrón Bridge (obligatorio)
- Abstracción: `Instrument` (clase abstracta) → `MicrophoneInstrument`, `GamepadInstrument`, `WebcamInstrument`. Lee el dispositivo y traduce sus valores en parámetros musicales.
- Implementación: `ISynthCommand` (interfaz) → `PianoCommand`, `BassCommand`, `OrganCommand`. Sabe cómo invocar su synth en SuperCollider.
- Cada `Instrument` tiene una referencia a un `ISynthCommand`. Las dos jerarquías evolucionan por separado: se puede cambiar qué synth controla un instrumento sin tocar su clase.
- El reproductor (`Player`) reutiliza los mismos `ISynthCommand` para reproducir una grabación, sin instrumentos.

# Editor musical
- Conoce en todo momento cada comando enviado a SuperCollider (patrón observador sobre los comandos).
- Grabar: al activar el modo grabación, cada comando se escribe en tiempo real en `recordings.music` con el instante relativo al inicio de la grabación. Al detener, se pide un nombre único; si ya existe, se rechaza y se pide otro.
- Cada grabación en `recordings.music` tiene un encabezado con nombre, posición de inicio y posición de fin dentro del archivo.
- `recordings.index`: registros de tamaño fijo (nombre de longitud fija + posición del encabezado), ordenados por nombre. Se actualiza al terminar cada grabación con Balanced Merge Sort sobre archivos en disco.
- `recordings.list`: nombre y fecha/hora de cada grabación, en orden cronológico.
- Cargar: muestra la lista cronológica desde `recordings.list`; con el nombre elegido se hace búsqueda binaria en disco sobre `recordings.index`.
- Reproducir: lee los comandos de la grabación y los envía respetando los intervalos grabados, para que suene idéntica.

# Reglas no negociables
- Todos los registros de `recordings.music` y de `recordings.index` tienen tamaño fijo y se escriben en binario.
- Prohibido ordenar o recorrer en memoria (arrays o colecciones) las grabaciones indexadas. Todo ocurre en disco.
- Ningún valor quemado en el código: todo va a `IConstants`.
- Uso intensivo de herencia, polimorfismo y encapsulamiento.
- Excepciones personalizadas para errores de dispositivos, conexión y almacenamiento.

# Specs de alto nivel para ciertas clases mínimas del model

SynthEvent
    - instante relativo (ms desde el inicio de la grabación)
    - synth invocado
    - acción (disparar, noteOn, noteOff)
    - nota MIDI
    - duración en ms
    - tercera (piano)
    - 3 ratios y 3 amplitudes (órgano)

RecordingHeader
    - nombre (longitud fija)
    - posición de inicio en recordings.music
    - posición de fin en recordings.music

IndexEntry
    - nombre (longitud fija)
    - posición del encabezado en recordings.music

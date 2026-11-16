---
id: ft-2
layer: backend
status: approved
---

# Micrófono → órgano

## General description

- `MicrophoneInstrument` (extiende `Instrument`) escucha el micrófono y toca `organ70s`: detecta la nota y el volumen, y envía `organOn` / `organOff`. Solo una nota encendida a la vez.
- La captura va detrás de `IAudioSource`. `MicrophoneSource` es la real (`javax.sound.sampled`, sin dependencias nuevas); las pruebas usan una falsa con señales generadas.
- `PitchDetector` es una clase aparte: frecuencia fundamental por autocorrelación, volumen RMS y conversión a nota MIDI.
- Un hilo propio lee bloques de `AUDIO_BLOCK_SIZE` muestras sin parar.
- Actualiza el `PowerBandMain` provisional para iniciar micrófono y control; si uno falla al iniciar, se informa y los demás siguen.

## Inputs

- Bloques de `AUDIO_BLOCK_SIZE` muestras `float` normalizadas (-1 a 1) desde `IAudioSource.read`.
- `ISynthCommand` (normalmente `OrganCommand`) e `IAudioSource`, por constructor.

## Outputs

- `SynthEvent.organOn(nota, amp)` y `SynthEvent.organOff(nota)` enviados con `play` al comando.
- `DeviceException` en `start()` si no hay micrófono o no se puede abrir.
- `IllegalStateException` si se llama `start()` con el instrumento ya iniciado.

## Test criteria

- `PitchDetector`:
  - Seno de 440 Hz → frecuencia 440 ± 1 % y nota 69.
  - Un seno en cada nota de 40 a 100 → esa misma nota.
  - Seno con armónicos (fundamental + 2.º + 3.º más débiles) → la nota de la fundamental.
  - La detección no depende del volumen: un seno de amplitud 0.01 da la misma nota.
  - Silencio y ruido blanco → `NO_PITCH`.
  - RMS: silencio = 0; seno de amplitud A = A / √2.
  - `toMidiNote` redondea al semitono más cercano (440 → 69, 452 → 69, 455 → 70).
- `MicrophoneInstrument`:
  - Volumen ≥ `MIC_ON_THRESHOLD` y la misma nota en `MIC_STABLE_WINDOWS` ventanas seguidas → un `organOn`. Con menos ventanas no suena.
  - Mientras la nota sigue igual no se repite el `organOn`.
  - `amp` = RMS × `MIC_AMP_GAIN`, limitado a `MIC_MIN_AMP`..`MIC_MAX_AMP`.
  - Si la nota estable cambia: `organOff` de la anterior y luego `organOn` de la nueva. Una nota distinta por menos de `MIC_STABLE_WINDOWS` ventanas no cambia nada.
  - Volumen < `MIC_OFF_THRESHOLD` durante `MIC_OFF_WINDOWS` ventanas seguidas → `organOff`. Un silencio más corto mantiene la nota.
  - Histéresis: con el volumen entre los dos umbrales la nota encendida sigue y no se enciende ninguna nueva.
  - Silencio, ruido, un tono bajo el umbral de encendido y notas fuera de `ORGAN_MIN_NOTE`..`ORGAN_MAX_NOTE` (40–100) nunca encienden nada.
  - En ningún momento hay más de una nota encendida.
  - `start()` con una fuente que falla en `open()` lanza `DeviceException` y no arranca el hilo.
  - `stop()` envía `organOff` de la nota encendida, detiene el hilo y cierra la fuente; llamarlo dos veces o sin `start()` no falla.
  - Si la fuente deja de entregar audio, se apaga la nota encendida y el hilo sigue vivo.

## Assets

- `MicrophoneInstrument extends Instrument`:
  - `MicrophoneInstrument(ISynthCommand pCommand, IAudioSource pSource)`
  - `void start() throws DeviceException`
  - `void stop()`
  - `void process(float[] pBlock) throws SynthConnectionException` — analiza una ventana; lo llama el hilo y también las pruebas.
- `IAudioSource`:
  - `void open() throws DeviceException`
  - `boolean read(float[] pBlock)` — llena el bloque completo (bloquea hasta tenerlo); `false` si no hay audio. Nunca lanza.
  - `void close()`
- `MicrophoneSource implements IAudioSource`: `TargetDataLine` con `AudioFormat(AUDIO_SAMPLE_RATE, AUDIO_SAMPLE_BITS, AUDIO_CHANNELS, signed, little-endian)`.
- `PitchDetector`:
  - `PitchDetector(float pSampleRate)`
  - `float detectFrequency(float[] pSamples)` — Hz, o `NO_PITCH` si la señal no es periódica.
  - `float rms(float[] pSamples)`
  - `int toMidiNote(float pFrequency)` — `round(MIDI_A4 + 12 · log2(f / FREQ_A4))`.
- `SynthEvent`: fábrica nueva `organOn(int pNote, float pAmp)` (ratios y amplitudes por defecto).
- Claves nuevas en `IConstants`:
  - Audio: `AUDIO_SAMPLE_RATE` (44100f), `AUDIO_SAMPLE_BITS` (16), `AUDIO_CHANNELS` (1), `AUDIO_BYTES_PER_SAMPLE` (2), `AUDIO_BLOCK_SIZE` (2048, unos 46 ms), `AUDIO_SAMPLE_SCALE` (32768f).
  - Tono: `PITCH_MIN_FREQ` (75f), `PITCH_MAX_FREQ` (2800f), `PITCH_CLARITY_THRESHOLD` (0.9f), `PITCH_MIN_ENERGY` (1e-6f), `NO_PITCH` (-1f).
  - Rango: `ORGAN_MIN_NOTE` (40), `ORGAN_MAX_NOTE` (100), `NO_NOTE` (-1).
  - Reglas: `MIC_ON_THRESHOLD` (0.02f), `MIC_OFF_THRESHOLD` (0.01f), `MIC_STABLE_WINDOWS` (2), `MIC_OFF_WINDOWS` (3), `MIC_AMP_GAIN` (2f), `MIC_MIN_AMP` (0.1f), `MIC_MAX_AMP` (0.6f).
  - Hilo: `MIC_RETRY_WAIT_MS` (200), `MIC_STOP_TIMEOUT_MS` (1000), `MIC_THREAD_NAME`.
  - Mensajes: `ERR_NO_MICROPHONE`, `ERR_MIC_ALREADY_STARTED`, `ERR_NULL_AUDIO_SOURCE`, `ERR_MIC_PLAY`, `ERR_INSTRUMENT_START`, `MSG_NO_INSTRUMENTS`.
- Los umbrales son valores iniciales: hay que ajustarlos con el micrófono y el instrumento reales.

## Implementation guidelines

- Archivos:
  - `src/powerband/instrument/MicrophoneInstrument.java`, `IAudioSource.java`, `MicrophoneSource.java`, `PitchDetector.java`
  - `src/powerband/model/SynthEvent.java` (fábrica `organOn(int, float)`)
  - `src/powerband/PowerBandMain.java`
  - `src/powerband/util/IConstants.java`
- Solo stdlib: `javax.sound.sampled` y `java.lang.Math`. `DeviceException` ya existe (ft-3).
- `PitchDetector.detectFrequency`, autocorrelación normalizada `r(d) = Σ x[i]·x[i+d] / √(Σ x[i]² · Σ x[i+d]²)`:
  1. Si la energía de la ventana es menor que `PITCH_MIN_ENERGY` → `NO_PITCH`.
  2. Desde el desfase 1, avanzar hasta el primer `r(d) < 0`. Así se salta la bajada inicial, donde `r` es alta sin que haya un período.
  3. Desde ahí, hasta el desfase `sampleRate / PITCH_MIN_FREQ`, tomar el **primer** máximo local con `r ≥ PITCH_CLARITY_THRESHOLD`. Tomar el primero evita errores de octava.
  4. Afinar el desfase con interpolación parabólica sobre los tres valores alrededor del máximo (en las notas agudas un desfase entero equivale a casi un semitono).
  5. `frecuencia = sampleRate / desfase`; si queda fuera de `PITCH_MIN_FREQ`..`PITCH_MAX_FREQ` o no hubo máximo → `NO_PITCH`.
- `MicrophoneInstrument.process` (`synchronized`), con `activeNote`, `candidateNote`, `candidateCount` y `quietCount`:
  1. `rms < MIC_OFF_THRESHOLD`: suma una ventana callada y borra la candidata; si hay nota encendida y ya van `MIC_OFF_WINDOWS`, `organOff`.
  2. `rms` entre los dos umbrales: reinicia la cuenta de silencio, borra la candidata y no hace nada más.
  3. `rms ≥ MIC_ON_THRESHOLD`: detecta la nota. Sin tono o fuera de 40–100, borra la candidata y mantiene lo que esté sonando. Con nota válida, cuenta ventanas seguidas; al llegar a `MIC_STABLE_WINDOWS`, si es distinta de la encendida: `organOff` de la anterior (si hay) y `organOn` de la nueva.
- El `amp` se fija al encender la nota; no se actualiza mientras suena.
- Hilo: daemon, con nombre `MIC_THREAD_NAME`. Ciclo: `read(bloque)`; si devuelve `true`, `process`; si devuelve `false`, apaga la nota encendida, espera `MIC_RETRY_WAIT_MS` y reintenta.
- El hilo no puede lanzar `SynthConnectionException`: la reporta en `System.err` con `ERR_MIC_PLAY` y sigue. Nunca `catch` vacío.
- `start()` y `stop()` usan un lock propio, distinto del de `process` (igual que `GamepadInstrument`), para que `stop()` no espere al hilo.
- `stop()`: baja la bandera `volatile`, interrumpe y hace `join(MIC_STOP_TIMEOUT_MS)`; luego `organOff` de la nota encendida y `close()` de la fuente.
- `MicrophoneSource`: `LineUnavailableException`, `IllegalArgumentException` y `SecurityException` se envuelven en `DeviceException` con la causa. `read` convierte cada par de bytes a `short` y lo divide entre `AUDIO_SAMPLE_SCALE`; si la línea entrega menos bytes de los pedidos devuelve `false`.
- En Windows, si el permiso de micrófono está apagado, la línea abre pero solo entrega silencio: no hay excepción.
- `PowerBandMain` (provisional, lo reemplaza ft-8):
  1. `OscClient` y los 3 comandos, como ahora.
  2. Arma la lista de `Instrument`: `GamepadInstrument(bass, ...)` y `MicrophoneInstrument(organ, new MicrophoneSource())`.
  3. Llama `start()` a cada uno; si lanza `PowerBandException`, imprime `ERR_INSTRUMENT_START` con el mensaje y sigue con el siguiente.
  4. Si ninguno inició, imprime `MSG_NO_INSTRUMENTS` y termina. Si no, espera Enter.
  5. En `finally`: `stop()` de los que iniciaron y `organ.allNotesOff()`.
- Parámetros con prefijo `p`; campos `private`; ningún literal fuera de `IConstants`.
- Fuera de alcance: elegir entre varios micrófonos, polifonía, cambiar el `amp` de una nota que ya suena, apagar la nota por ruido sin tono (solo se apaga por silencio), ventana del editor (ft-8).

## Business workflow

1. `start()`: `source.open()`; si falla, `DeviceException`. Si no, arranca el hilo.
2. El hilo lee un bloque y llama `process`.
3. `process` calcula el RMS y, si el volumen alcanza, la nota de la ventana.
4. Nota estable y volumen suficiente → `organOn`. Nota estable distinta → `organOff` + `organOn`. Silencio sostenido → `organOff`.
5. `stop()`: detiene el hilo, apaga la nota encendida y cierra el micrófono.

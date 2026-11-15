---
id: ft-1
layer: testing
status: approved
---

# Contratos Bridge y conexión SuperCollider

## General description

- Pruebas JUnit 5 que verifican el formato OSC, los mensajes de cada comando, la validación de eventos, la notificación a listeners y el Bridge, sin SuperCollider corriendo.
- En lugar de `scsynth`, un `DatagramSocket` local en puerto efímero recibe y decodifica los paquetes.

## Inputs

- `OscClient` apuntando a `127.0.0.1` y al puerto del receptor de prueba.
- `SynthEvent` construidos en cada prueba con las fábricas (`SynthEvent.piano(...)`, `bass(...)`, `organOn(...)`, `organOff(...)`).

## Outputs

- Resultados de `mvn test`.

## Test criteria

- `OscClientTest`
  - `encodesStringWithNullTerminatorAndPaddingToFourBytes`: `"/s_new"` (6 bytes) ocupa 8; un string de 4 bytes ocupa 8.
  - `encodesTypeTagsForIntFloatAndString`: args `(1, 2f, "x")` → tag `,ifs`.
  - `encodesIntsBigEndianAndFloatsAsIntBits`.
  - `rejectsUnsupportedArgumentType` → `IllegalArgumentException`.
  - `nodeIdsStartAtFirstNodeIdAndAreUniqueAcrossThreads`.
  - `throwsSynthConnectionExceptionForInvalidPort`: puerto fuera de rango (por ejemplo `-1`) → `SynthConnectionException`. No depende de DNS ni de red.
  - `throwsSynthConnectionExceptionForUnknownHost`: host con dominio reservado `.invalid` (RFC 6761) → `SynthConnectionException`. Puede tardar un poco sin internet; usar `assertTimeoutPreemptively` con un margen generoso.
- `SynthEventTest`
  - `pianoFactoryUsesDefaultAmpWhenNotGiven`.
  - `organOnFactoryUsesDefaultRatiosAndAmps`.
  - `organOnRejectsRatiosOrAmpsWithWrongLength` → `IllegalArgumentException`.
  - `withTimeMsReturnsNewEventAndKeepsOriginalUnchanged`.
- `PianoCommandTest`
  - Evento `piano(48, 1500, 3)` → `/s_new classicPiano <id> 0 1 note 48 durMs 1500 third 3 amp 0.3`; el listener recibe el evento con `timeMs > 0`.
  - `rejectsEventOfAnotherSynth`: un evento de bajo → `IllegalArgumentException`, no envía nada ni notifica.
- `BassCommandTest`
  - Evento `bass(36, 450)` → `/s_new stringBass <id> 0 1 note 36 durMs 450 amp 0.5`.
  - `rejectsEventOfAnotherSynth`.
- `OrganCommandTest`
  - `noteOnSendsFreqAmpRatiosAndAmps`: nota 69 → `freq` 440.0f, `amp` y ratios/amplitudes del evento.
  - `noteOffSendsGateZeroToSameNode`.
  - `noteOnOnActiveNoteReleasesPreviousNodeFirst`.
  - `noteOffOnInactiveNoteSendsNothingAndDoesNotNotify`.
  - `rejectsTriggerAction` → `IllegalArgumentException`.
  - `concurrentNoteOnAndNoteOffLeaveNoUntrackedNode`: varios hilos hacen `noteOn`/`noteOff` sobre la misma nota; al terminar se hace un último `noteOff`, y cada `nodeId` que recibió `/s_new` recibió también exactamente un `/n_set gate 0`.
- `SynthCommandListenerTest`: dos listeners reciben cada evento; un listener removido no recibe.
- `InstrumentTest`: un `Instrument` de prueba delega `play` al comando; tras `setCommand` el siguiente evento va al nuevo comando.
- Ninguna prueba depende de `scsynth`; todas pasan con `mvn test` sin red externa.

## Assets

- `src/test/powerband/command/helpers/OscReceiver.java`: `AutoCloseable`, abre `DatagramSocket(0)`, expone `getPort()` y `receive()` que devuelve el path y la lista de args decodificados (timeout corto para las pruebas de "no envía nada"). También `receiveAll()` para la prueba concurrente del órgano, que lee hasta agotar el timeout.
- `src/test/powerband/instrument/helpers/`: `FakeInstrument` (subclase mínima con `start`/`stop` vacíos que expone `play`) y `RecordingCommand` (`ISynthCommand` que guarda los eventos recibidos).

## Implementation guidelines

- JUnit 5 (`org.junit.jupiter`), ya en `pom.xml`; fuentes de prueba en `src/test/` (`testSourceDirectory`).
- Paquetes espejo: `src/test/powerband/sc/OscClientTest.java`, `src/test/powerband/model/SynthEventTest.java`, `src/test/powerband/command/*Test.java`, `src/test/powerband/instrument/InstrumentTest.java`.
- Clases `<ClassName>Test`; métodos que describen comportamiento.
- Receptores y clientes en try-with-resources.
- Valores de las pruebas desde `IConstants` cuando ya existan (nombres de synth, paths OSC, `FIRST_NODE_ID`, amps por defecto).
- Floats se comparan con una tolerancia pequeña.

## Business workflow

1. Abrir `OscReceiver` en puerto efímero.
2. Crear `OscClient("127.0.0.1", receiver.getPort())` y el comando bajo prueba; registrar un listener que acumula eventos.
3. Ejecutar el `SynthEvent`.
4. Decodificar el datagrama recibido y comparar path y args con el contrato.
5. Verificar los eventos notificados.
6. Cerrar cliente y receptor.

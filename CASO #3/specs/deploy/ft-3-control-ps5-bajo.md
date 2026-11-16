---
id: ft-3
layer: deploy
status: draft
---

# Control PS5 → bajo

## General description

- Agrega la librería Jamepad al `pom.xml` para leer el control DualSense.

## Inputs

- None

## Outputs

- `pom.xml` con la dependencia nueva; `mvn compile` y `mvn test` la descargan de Maven Central.

## Test criteria

- `mvn compile` resuelve `com.badlogicgames.jamepad:jamepad:2.30.0.0`.
- `mvn test` pasa sin control conectado (las pruebas no cargan la librería nativa).
- `mvn exec:java` arranca `PowerBandMain` con Jamepad en el classpath.

## Assets

- Dependencia (scope por defecto, `compile`):

  ```xml
  <dependency>
      <groupId>com.badlogicgames.jamepad</groupId>
      <artifactId>jamepad</artifactId>
      <version>2.30.0.0</version>
  </dependency>
  ```

- `2.30.0.0` es la última versión publicada en Maven Central (revisado el 2026-11-15). Trae `gdx-jnigen-loader` como dependencia transitiva y las librerías nativas de Windows, Linux y macOS dentro del jar.

## Implementation guidelines

- Archivo: `pom.xml`. No se cambia nada más del build.
- Es la única dependencia nueva: la stdlib de Java no lee controles.
- No hay que instalar SDL aparte ni copiar `.dll`: Jamepad extrae la nativa al iniciar.
- El control se conecta por USB o Bluetooth antes de arrancar; Windows debe verlo como control de juego.

## Business workflow

1. Agregar la dependencia a `<dependencies>` en `pom.xml`.
2. `mvn compile` para descargarla.
3. Con `scsynth` corriendo, `myband.sc` cargado y el control conectado: `mvn exec:java`.

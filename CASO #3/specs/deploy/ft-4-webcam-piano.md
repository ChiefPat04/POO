---
id: ft-4
layer: deploy
status: approved 
---

# Webcam → piano

## General description

- Agrega la librería Webcam Capture al `pom.xml` para leer la webcam.

## Inputs

- None

## Outputs

- `pom.xml` con la dependencia nueva; `mvn compile` y `mvn test` la descargan de Maven Central.

## Test criteria

- `mvn compile` resuelve `com.github.sarxos:webcam-capture:0.3.12`.
- `mvn test` pasa sin webcam (las pruebas no cargan la librería).
- `mvn exec:java` arranca `PowerBandMain` con la librería en el classpath y abre la cámara integrada en Windows.

## Assets

- Dependencia (scope por defecto, `compile`):

  ```xml
  <dependency>
      <groupId>com.github.sarxos</groupId>
      <artifactId>webcam-capture</artifactId>
      <version>0.3.12</version>
  </dependency>
  ```

- `0.3.12` es la última versión estable en Maven Central (enero de 2018, licencia MIT). Trae `bridj` (nativas de Windows, Linux y macOS dentro del jar) y `slf4j-api` como transitivas.

## Implementation guidelines

- Archivo: `pom.xml`. No se cambia nada más del build.
- Es la única dependencia nueva: la stdlib de Java no lee cámaras.
- Sin binding de SLF4J la librería imprime un aviso al arrancar (`No SLF4J providers were found`); es inofensivo y no se agrega otra dependencia para quitarlo.
- Riesgo a verificar con la cámara real: la librería es vieja y usa BridJ. Si en Windows con Java 17 falla al abrir (`UnsatisfiedLinkError` o la cámara no aparece), la alternativa es `org.openpnp:opencv` con `VideoCapture`; solo cambiaría `WebcamSource` y esta dependencia, no `IFrameSource` ni el instrumento.
- Windows debe tener activado el permiso de cámara para aplicaciones de escritorio, y ninguna otra aplicación (Teams, Zoom) debe estar usando la cámara.

## Business workflow

1. Agregar la dependencia a `<dependencies>` en `pom.xml`.
2. `mvn compile` para descargarla.
3. Con `scsynth` corriendo, `myband.sc` cargado y la cámara libre: `mvn exec:java`.

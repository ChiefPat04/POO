# Deploy

After you change this layer, update this file so the next run stays current.

Source: `pom.xml` (Maven build, `mainClass` = `powerband.PowerBandMain`), `readme.md`

## Implemented
- Maven build con `maven-compiler-plugin` que excluye `test/**` de la compilación principal, porque `sourceDirectory` es `src`.
- `exec-maven-plugin` para `mvn compile exec:java`.
- Surefire 3.5.2 para `mvn test`.
- Dependencia `com.badlogicgames.jamepad:jamepad:2.30.0.0` (control PS5, ft-3); trae las nativas de SDL dentro del jar.
- No hay archivos de pipeline CI.

## Build files
- `pom.xml`

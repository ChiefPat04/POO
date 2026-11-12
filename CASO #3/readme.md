# Power Band (Caso #3 - POO)

Programa de creación e interacción musical: 3 instrumentos físicos controlan 3 synths de SuperCollider, y un editor graba, guarda, carga y reproduce la música.

## Prerrequisitos

| Herramienta | Verificar |
|---|---|
| JDK (versión de `pom.xml`) | `java -version` |
| Apache Maven 3.9+ | `mvn -version` |
| SuperCollider | https://supercollider.github.io/downloads.html |
| Claude Code | `claude --version` |

## Preparar SuperCollider

1. Abrir `recursos/supercollider/myband.sc` en el IDE de SuperCollider.
2. Evaluar el bloque entre paréntesis (Ctrl+Enter).
3. Evaluar `~myband.load;` para iniciar el servidor y cargar los synths.

## Correr el programa

Desde esta carpeta (donde está `pom.xml`):

```powershell
mvn compile exec:java
```

## Pruebas

```powershell
mvn clean verify
```

## Flujo de trabajo con IA (spec driven development)

1. `/generate-feature-spec <descripción>` → crea `specs/<capa>/ft-N-*.md` en `draft`.
2. Revisar y editar la spec a mano, luego cambiar a `status: approved`.
3. `/develop-feature ft-N` → implementa y corre las pruebas.
4. `/update-current-development` → actualiza los mapas de `.claude/skills/`.

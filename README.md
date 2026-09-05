# Programación Orientada a Objetos

<div align="center">

### Aprender a modelar ideas como objetos

Repositorio académico para estudiar, practicar y aplicar los fundamentos de la **Programación Orientada a Objetos** con Java.

## Sobre la materia

La Programación Orientada a Objetos permite representar entidades del mundo real mediante clases, atributos y comportamientos. En este repositorio se construyen pequeños programas para comprender cómo organizar el código, reutilizarlo y hacer que sus componentes colaboren de forma clara.

La materia trabaja especialmente estos principios:

| Concepto | Qué representa |
| --- | --- |
| **Clases y objetos** | La definición de una entidad y sus instancias concretas. |
| **Encapsulamiento** | El control del acceso al estado interno mediante modificadores y métodos. |
| **Herencia** | La reutilización y especialización de una clase base. |
| **Polimorfismo** | La posibilidad de usar un tipo común y obtener comportamientos específicos. |
| **Abstracción** | La exposición de lo importante sin depender de los detalles internos. |
| **Interfaces** | Contratos que distintas clases pueden implementar de manera diferente. |
| **Paquetes** | La organización del código por responsabilidades. |

## Proyecto destacado: Programa Mutante

En [`EJERCICIOS`](EJERCICIOS/) se desarrolla un programa que reúne los conceptos principales de la materia:

- `Persona` funciona como clase base.
- `IAEngineer`, `Mecánico` y `Trader` heredan de `Persona`.
- `IPower` define el contrato común para los poderes mutantes.
- Cinco poderes implementan el mismo método con salidas diferentes.
- `quickstart` crea diez personas y demuestra el comportamiento polimórfico.
- Cada persona puede combinarse con un poder sin que su profesión quede atada a él.

Consulta la documentación detallada en el [README del ejercicio](EJERCICIOS/README.md) y el [diagrama de clases](EJERCICIOS/class-diagram.puml).


## Autor

**Patrick Zúñiga Arroyo**

Repositorio general para la materia de Programación Orientada a Objetos.

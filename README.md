# ReservaLab - Examen Final de Automatización de Pruebas

**Estudiante:** Edinson Ahumada Gallardo

**Asignatura:** Automatización de Pruebas

**Institución:** IPLACEX

[![CI/CD Examen Final](https://github.com/eahumadaed/examen-final-automatizacion-pruebas/actions/workflows/ci-cd.yml/badge.svg)](https://github.com/eahumadaed/examen-final-automatizacion-pruebas/actions/workflows/ci-cd.yml)

## Descripción

ReservaLab es una aplicación web Java para reservar cupos en talleres. El proyecto reúne control de versiones con GitFlow, construcción con Maven, pruebas unitarias, integración HTTP, una prueba de aceptación web, integración continua y despliegue Blue-Green con rollback.

El ejemplo se mantuvo pequeño a propósito: así resulta sencillo revisar las reglas de negocio, los casos de error y las condiciones que debe cumplir una versión antes de ser promovida.

## Cumplimiento de actividades

### Actividad 1 - Git y Maven

- Repositorio GitHub: <https://github.com/eahumadaed/examen-final-automatizacion-pruebas>
- Estrategia GitFlow con ramas `main`, `develop` y `feature/*`.
- Proyecto Maven para Java 17 configurado en `pom.xml`.
- Pruebas construidas con JUnit 5 y un navegador real.
- Historial con commits y merges que acredita el uso de ramas.

La explicación del flujo está en [`docs/GITFLOW.md`](docs/GITFLOW.md).

### Actividad 2 - Integración continua y pruebas

El archivo [`.github/workflows/ci-cd.yml`](.github/workflows/ci-cd.yml) define el pipeline solicitado. Se ejecuta con cada `push` a `main` o `develop`, en pull requests hacia `main` y también de forma manual.

| Tipo de prueba | Alcance | Cantidad |
| --- | --- | ---: |
| Unitaria | Reglas de negocio, validaciones e identificadores | 5 |
| Integración | Página, API REST, health check y errores HTTP | 4 |
| Aceptación | Reserva completa desde la interfaz web | 1 |

Etapas del pipeline:

1. `Build + unitarias + integración`: compila, ejecuta nueve pruebas y publica los reportes JUnit.
2. `Prueba de aceptación web`: levanta un ambiente temporal, recorre el flujo principal y guarda una captura.
3. `Deploy Blue-Green + rollback`: despliega dos versiones, conmuta de Blue a Green y valida el retorno a Blue.

Los reportes y evidencias quedan disponibles como artefactos descargables en cada ejecución de [GitHub Actions](https://github.com/eahumadaed/examen-final-automatizacion-pruebas/actions).

### Actividad 3 - Pipeline de despliegue

El archivo [`deployment/blue-green-deploy.ps1`](deployment/blue-green-deploy.ps1) administra dos slots:

- **Blue:** `http://localhost:8081`
- **Green:** `http://localhost:8082`

Cada versión se instala primero en el slot inactivo. El proceso consulta `/api/health` y solo actualiza el slot activo cuando recibe el estado `UP`. La versión anterior permanece encendida para permitir un rollback inmediato. El workflow demuestra el recorrido `Blue -> Green -> rollback a Blue` y publica las respuestas, los estados y el historial.

El procedimiento y los criterios de recuperación se detallan en [`docs/DEPLOYMENT.md`](docs/DEPLOYMENT.md).

## Estrategia de pruebas

La estrategia sigue una pirámide sencilla:

- Varias comprobaciones rápidas sobre la lógica en `ReservationServiceTest`.
- Menos pruebas de integración sobre el servidor y sus endpoints en `ReservaLabApplicationIT`.
- Un flujo crítico de usuario con navegador real en `ReservationAcceptanceTest`.

Las pruebas no dependen de datos externos ni de un orden determinado. El servidor de integración usa un puerto dinámico, la aceptación recibe la URL del ambiente mediante `acceptance.baseUrl` y todo lo necesario para reconstruir el proyecto está declarado en Maven.

## Ejecución local

Requisitos: Java 17 o superior, Maven 3.9+ y Chrome para la prueba de aceptación.

Compilar y ejecutar las pruebas unitarias y de integración:

```powershell
mvn clean verify
```

Iniciar la aplicación:

```powershell
mvn -DskipTests package
java -jar target/reserva-lab.jar --port=8080 --environment=local --color=blue --version=1.0.0
```

Con la aplicación activa en otra terminal, ejecutar la prueba de aceptación:

```powershell
mvn -Pacceptance verify -Dacceptance.baseUrl=http://localhost:8080
```

Probar el despliegue y el rollback:

```powershell
mvn -DskipTests package
./deployment/blue-green-deploy.ps1 deploy -Version 1.0.0
./deployment/blue-green-deploy.ps1 deploy -Version 1.1.0
./deployment/blue-green-deploy.ps1 status
./deployment/blue-green-deploy.ps1 rollback
./deployment/blue-green-deploy.ps1 cleanup
```

## Estructura relevante

```text
.github/workflows/ci-cd.yml                 Pipeline CI/CD
deployment/blue-green-deploy.ps1            Despliegue, health check y rollback
src/main/java/...                            Aplicación y lógica de negocio
src/main/resources/index.html                Interfaz web
src/test/java/.../*Test.java                 Pruebas unitarias
src/test/java/.../*IT.java                   Pruebas de integración
src/test/java/.../*AcceptanceTest.java       Prueba de aceptación web
docs/                                        Estrategia y evidencias
```

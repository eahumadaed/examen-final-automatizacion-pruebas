# ReservaLab - Examen Final de Automatizacion de Pruebas

**Estudiante:** Edinson Ahumada Gallardo  
**Asignatura:** Automatizacion de Pruebas  
**Institucion:** IPLACEX

[![CI/CD Examen Final](https://github.com/eahumadaed/examen-final-automatizacion-pruebas-edinson-ahumada/actions/workflows/ci-cd.yml/badge.svg)](https://github.com/eahumadaed/examen-final-automatizacion-pruebas-edinson-ahumada/actions/workflows/ci-cd.yml)

## Descripcion

ReservaLab es una aplicacion web Java que permite reservar cupos en talleres de automatizacion. El proyecto se construyo para demostrar un ciclo completo y verificable: control de versiones con GitFlow, compilacion Maven, pruebas unitarias, integracion HTTP, aceptacion con Selenium, integracion continua y despliegue Blue-Green con rollback.

El ejemplo es pequeno a proposito: permite revisar con claridad que se prueba, como falla una validacion y que condiciones debe cumplir un despliegue antes de ser promovido.

## Cumplimiento de actividades

### Actividad 1 - Git y Maven

- Repositorio GitHub: <https://github.com/eahumadaed/examen-final-automatizacion-pruebas-edinson-ahumada>
- Estrategia GitFlow con ramas `main`, `develop` y `feature/*`.
- Proyecto Maven para Java 17 configurado en `pom.xml`.
- Dependencias de pruebas: JUnit 5 y Selenium 4.
- Historial con commits y merges que evidencia el uso real de ramas.

La explicacion del flujo esta en [`docs/GITFLOW.md`](docs/GITFLOW.md).

### Actividad 2 - Integracion continua y pruebas

El archivo [`.github/workflows/ci-cd.yml`](.github/workflows/ci-cd.yml) implementa el pipeline equivalente solicitado en la pauta. Se ejecuta en cada `push` a `main` o `develop`, en pull requests hacia `main` y tambien de forma manual.

| Tipo de prueba | Herramienta | Alcance | Cantidad |
| --- | --- | --- | ---: |
| Unitaria | JUnit 5 | Reglas de negocio, validaciones e identificadores | 5 |
| Integracion | JUnit 5 + Java HttpClient | Pagina, API REST, health check y errores HTTP | 4 |
| Aceptacion | Selenium + Chrome headless | Reserva completa desde la interfaz web | 1 |

Stages del pipeline:

1. `Build + unitarias + integracion`: compila, ejecuta 9 pruebas y publica reportes JUnit.
2. `Acceptance tests Selenium`: levanta un ambiente temporal, automatiza el navegador y publica una captura.
3. `Deploy Blue-Green + rollback`: despliega dos versiones, conmuta de Blue a Green y valida el retorno a Blue.

Los reportes y evidencias quedan disponibles como artefactos descargables en cada ejecucion de [GitHub Actions](https://github.com/eahumadaed/examen-final-automatizacion-pruebas-edinson-ahumada/actions).

### Actividad 3 - Deployment pipeline

El script [`scripts/blue-green-deploy.ps1`](scripts/blue-green-deploy.ps1) administra dos slots:

- **Blue:** `http://localhost:8081`
- **Green:** `http://localhost:8082`

Cada despliegue se instala primero en el slot inactivo. El script consulta `/api/health` y solo actualiza el slot activo cuando recibe estado `UP`. La version anterior permanece encendida para permitir rollback inmediato. El workflow demuestra automaticamente `Blue -> Green -> rollback a Blue` y publica respuestas JSON, estados e historial.

El procedimiento y los criterios de recuperacion se detallan en [`docs/DEPLOYMENT.md`](docs/DEPLOYMENT.md).

## Estrategia de pruebas

La estrategia sigue una piramide simple:

- Muchas comprobaciones rapidas sobre la logica en `ReservationServiceTest`.
- Menos pruebas de integracion sobre el servidor y sus endpoints en `ReservaLabApplicationIT`.
- Un flujo critico de usuario con navegador real en `ReservationAcceptanceTest`.

Las pruebas no dependen de datos externos ni de un orden particular. El servidor de integracion usa un puerto dinamico; la aceptacion recibe la URL del ambiente mediante `acceptance.baseUrl`; y todo lo necesario para reconstruir el proyecto queda declarado en Maven.

## Ejecucion local

Requisitos: Java 17 o superior, Maven 3.9+ y Chrome para la prueba Selenium.

Compilar y ejecutar pruebas unitarias + integracion:

```powershell
mvn clean verify
```

Iniciar la aplicacion:

```powershell
mvn -DskipTests package
java -jar target/reserva-lab.jar --port=8080 --environment=local --color=blue --version=1.0.0
```

Con la aplicacion activa en otra terminal, ejecutar tambien aceptacion:

```powershell
mvn -Pacceptance verify -Dacceptance.baseUrl=http://localhost:8080
```

Probar el despliegue y rollback:

```powershell
mvn -DskipTests package
./scripts/blue-green-deploy.ps1 deploy -Version 1.0.0
./scripts/blue-green-deploy.ps1 deploy -Version 1.1.0
./scripts/blue-green-deploy.ps1 status
./scripts/blue-green-deploy.ps1 rollback
./scripts/blue-green-deploy.ps1 cleanup
```

## Estructura relevante

```text
.github/workflows/ci-cd.yml                Pipeline CI/CD
scripts/blue-green-deploy.ps1              Deploy, health check y rollback
src/main/java/...                           Aplicacion y logica de negocio
src/main/resources/index.html               Interfaz web
src/test/java/.../*Test.java                Pruebas unitarias
src/test/java/.../*IT.java                  Pruebas de integracion
src/test/java/.../*AcceptanceTest.java      Prueba Selenium
docs/                                       Estrategia y evidencias
Edinson_Ahumada.docx                        Informe formal para la plataforma
```

## Entregables

- Repositorio GitHub con flujo de ramas y `pom.xml`.
- Pipeline CI/CD versionado y ejecutado exitosamente.
- Pruebas unitarias, integracion y aceptacion automatizadas.
- Script Blue-Green con health check y rollback.
- Evidencias locales y remotas en `docs/evidencias`.
- Informe formal `Edinson_Ahumada.docx`, Arial 12 e interlineado 1,15.
- Paquete ZIP preparado para carga en la plataforma.

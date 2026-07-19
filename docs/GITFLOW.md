# Estrategia GitFlow

## Ramas

- `main`: contiene la version estable y entregable.
- `develop`: integra los avances que ya fueron revisados.
- `feature/automatizacion-pruebas`: incorpora JUnit, integracion HTTP y Selenium.
- `feature/pipeline-blue-green`: incorpora CI/CD, despliegue y rollback.

## Flujo aplicado

1. El proyecto base se crea en `main`.
2. Se abre `develop` desde la version estable.
3. Cada grupo de cambios se desarrolla en una rama `feature/*`.
4. Las features se integran en `develop` mediante merge explicito.
5. La version final se integra desde `develop` hacia `main`.

Este flujo separa el codigo estable del trabajo en curso y deja trazabilidad en el historial. Para una nueva funcionalidad se debe crear otra rama desde `develop`, abrir un pull request y ejecutar el pipeline antes de fusionarla.

## Comandos de referencia

```powershell
git switch develop
git switch -c feature/nueva-funcionalidad
git add .
git commit -m "Agrega nueva funcionalidad"
git switch develop
git merge --no-ff feature/nueva-funcionalidad
git switch main
git merge --no-ff develop
```


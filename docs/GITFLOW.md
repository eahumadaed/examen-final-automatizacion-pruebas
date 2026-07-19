# Estrategia GitFlow

## Ramas

- `main`: contiene la versión estable y entregable.
- `develop`: integra los avances que ya fueron revisados.
- `feature/automatizacion-pruebas`: incorpora las pruebas unitarias, de integración y de aceptación.
- `feature/pipeline-blue-green`: incorpora CI/CD, despliegue y rollback.
- `feature/ajustes-presentacion`: reúne las correcciones finales de documentación y evidencias.

## Flujo aplicado

1. El proyecto base se crea en `main`.
2. Se abre `develop` desde la versión estable.
3. Cada grupo de cambios se desarrolla en una rama `feature/*`.
4. Las ramas de funcionalidad se integran en `develop` mediante un merge explícito.
5. La versión final se integra desde `develop` hacia `main`.

Este flujo separa el código estable del trabajo en curso y mantiene la trazabilidad en el historial. Para una nueva funcionalidad se crea otra rama desde `develop`, se abre un pull request y se ejecuta el pipeline antes de fusionarla.

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

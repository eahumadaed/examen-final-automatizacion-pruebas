# Pipeline de despliegue Blue-Green

## Objetivo

El despliegue mantiene dos slots independientes: **Blue** en el puerto 8081 y **Green** en el puerto 8082. Solo se promueve una versión candidata cuando su endpoint `/api/health` responde con el estado `UP`. El slot anterior queda disponible para ejecutar un rollback inmediato.

## Etapas

1. El pipeline compila y ejecuta las pruebas unitarias y de integración.
2. Levanta un ambiente temporal y ejecuta la prueba de aceptación web.
3. Despliega la primera versión en Blue y valida su estado.
4. Despliega la versión candidata en Green y comprueba su estado antes de promoverla.
5. Ejecuta el rollback hacia Blue y vuelve a consultar el health check.
6. Publica logs, respuestas JSON e historial como artefactos de GitHub Actions.

## Uso local

```powershell
mvn clean package
./deployment/blue-green-deploy.ps1 deploy -Version 1.0.0
./deployment/blue-green-deploy.ps1 deploy -Version 1.1.0
./deployment/blue-green-deploy.ps1 status
./deployment/blue-green-deploy.ps1 rollback
./deployment/blue-green-deploy.ps1 cleanup
```

El directorio `.runtime` contiene el estado, los PID, los logs de cada slot y el historial de promociones. El slot activo no cambia si la versión candidata falla en el health check.

## Criterio de rollback

Se vuelve al slot anterior cuando una validación posterior al despliegue detecta errores funcionales, indisponibilidad o degradación. Como el proceso anterior permanece activo, el cambio de `current-url.txt` es inmediato y no requiere reconstruir el artefacto.

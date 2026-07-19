# Pipeline de despliegue Blue-Green

## Objetivo

El despliegue mantiene dos slots independientes: **Blue** en el puerto 8081 y **Green** en el puerto 8082. Solo se promueve un candidato cuando su endpoint `/api/health` responde `UP`. El slot anterior sigue disponible para ejecutar rollback inmediato.

## Etapas

1. El pipeline compila y ejecuta pruebas unitarias y de integracion.
2. Levanta un ambiente temporal y ejecuta la prueba de aceptacion con Selenium.
3. Despliega la primera version en Blue y valida su salud.
4. Despliega la version candidata en Green y valida su salud antes de promoverla.
5. Ejecuta rollback hacia Blue y vuelve a consultar el health check.
6. Publica logs, respuestas JSON e historial como artefacto de GitHub Actions.

## Uso local

```powershell
mvn clean package
./scripts/blue-green-deploy.ps1 deploy -Version 1.0.0
./scripts/blue-green-deploy.ps1 deploy -Version 1.1.0
./scripts/blue-green-deploy.ps1 status
./scripts/blue-green-deploy.ps1 rollback
./scripts/blue-green-deploy.ps1 cleanup
```

El directorio `.runtime` contiene el estado, los PID, logs por slot e historial de promociones. El script no cambia el slot activo si el candidato no supera el health check.

## Criterio de rollback

Se vuelve al slot anterior cuando una validacion posterior al despliegue detecta errores funcionales, indisponibilidad o degradacion. Como el proceso anterior permanece activo, el cambio de `current-url.txt` es inmediato y no requiere reconstruir el artefacto.

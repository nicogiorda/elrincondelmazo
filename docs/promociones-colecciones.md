# Promociones públicas y administración

Base: origin/main `14b1f24`. Rama: `audit/promotions-collections`.

## Contrato

- `GET /promotions/public`: público, solo lectura, devuelve PromotionResponse de promociones activas y vigentes. Inicio/fin inclusivos; fin nulo no expira. La entidad actual no tiene marca de campaña privada: todas las vigentes son publicables.
- Resto de `/promotions` y `/promotions/**`: ADMIN. Incluye listado/detalle, POST, PUT, PATCH y cualquier método futuro.
- Colecciones: se conserva el controller/service/repository/DTO existente. GET `/collections` y detalle públicos; escrituras restringidas. No se agregó CRUD.
- `PromotionService.applyPromotions(List<CartItem>, PaymentMethod)`: firma, cálculo y DTOs sin cambios. El método nuevo `getPublicPromotions()` es independiente.
- Administración usa transacciones de escritura; lecturas y mapeo de relaciones se ejecutan dentro de transacciones de solo lectura. La consulta pública y listado administrativo cargan collection/product con EntityGraph. El motor mantiene su consulta existente.
- Sin migraciones ni cambios de entidades.

## Pruebas ejecutadas el 19/10/2026

| Suite | Resultado |
| --- | --- |
| PromotionCheckoutIntegrationTests | 5/5 |
| PromotionQuantityTests | 19/19 |
| PromotionStackableTests | 26/26 |
| PromotionAdministrationIntegrationTests (nueva) | 15/15 |
| PromotionPublicTests (nueva) | 1/1 |

Las tres primeras se ejecutaron juntas (50); las nuevas después (16), todas sin errores ni omisiones. IntegrationTests usan PostgreSQL, JWT y Spring Security reales, `ddl-auto=none` y rollback de los fixtures. PromotionPublicTests usa MockMvc standalone con repositorio simulado para verificar la lista vacía sin inventar datos.

La nueva suite cubre anonimato, permisos, vigencia y límites exactos, POST/PUT/PATCH, lectura tras escritura, presencia de colecciones persistidas y validaciones existentes: nombre, rango de descuento, mínimos positivos, campos requeridos por tipo y fechas. No fue necesario cambiar esas validaciones.

Ejemplo de ejecución desde este worktree (configuración privada externa, nunca versionada):

```powershell
$maven = Get-ChildItem "$env:USERPROFILE/.m2/wrapper/dists/apache-maven-3.9.16" -Recurse -Filter mvn.cmd | Select-Object -First 1 -ExpandProperty FullName
& $maven '-Dtest=PromotionQuantityTests,PromotionStackableTests,PromotionCheckoutIntegrationTests,PromotionPublicTests,PromotionAdministrationIntegrationTests' '-Dspring.config.import=optional:file:../elrincondelmazo/application-secrets.properties' '-Dspring.jpa.hibernate.ddl-auto=none' test
```

Se utilizó Maven instalado porque el wrapper local no pudo resolver su arranque en PowerShell. El primer intento sin acceso de red no pudo obtener el POM padre; se repitió con acceso autorizado y pasó. No se cambió pom.xml ni se sustituyó la versión de Spring Boot.

## Coordinación

No requiere cambios del checkout. main ya contenía las correcciones de cantidad y acumulabilidad y sus pruebas. Integrar sobre main, no sobre el checkout antiguo de rama-vic.

El frontend complementario elimina las consultas propias del editor y usa esta lectura pública en Home. Su informe `docs/promociones-colecciones.md` incluye los archivos modificados, mediciones antes/después, 10 escenarios de navegador en desarrollo y producción y un smoke real contra este backend/PostgreSQL.

Pendiente externo: el catálogo global limita productos a 100; su responsable debe definir paginación/recarga compartida. No se cambió ProductServiceImpl, OrderServiceImpl ni ningún DTO de compra. Si el negocio necesita campañas privadas, debe acordarse un atributo de visibilidad; no existe en el modelo actual.

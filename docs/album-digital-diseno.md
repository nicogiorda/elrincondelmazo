# Mi Álbum — diseño funcional y técnico (v1)

Estado: propuesta de arquitectura; **sin implementación ni cambios de esquema**. Esta etapa es exclusivamente documental.

## Objetivo y reglas del producto

- Cada usuario autenticado tiene un álbum personal con cuatro secciones iniciales: **NBA**, **My Little Pony**, **Cars** y **Marvel**. Las secciones son contenedores, no restricciones de temática.
- Cada sección tiene páginas ordenadas de **9 posiciones** (índices 0 a 8). La primera página está disponible desde el inicio; al completar la última se habilita la siguiente. Las páginas creadas no desaparecen al quitar cartas.
- Se admiten cartas repetidas y colocar cualquier carta en cualquier sección. No hay unicidad por producto, nombre ni temática.
- Se puede agregar una carta desde un ítem de compra elegible o registrar una carta física con fotografía. Agregar desde una compra es optativo, nunca automático.
- Se puede mover, quitar y reemplazar una carta. Reemplazar requiere confirmación en la interfaz; el backend debe ser transaccional.
- El usuario elige si su álbum es PUBLIC o PRIVATE (por defecto PRIVATE). Los visitantes solo leen álbumes PUBLIC; el dueño puede leer y editar el suyo.
- Un registro manual **no verifica propiedad física**. Un registro desde pedido acredita asociación a una operación registrada, **no pago ni posesión actual**.

## Decisión pendiente antes de implementar compras

**¿Cuándo se permite agregar una carta de pedido y con qué etiqueta?** El checkout actual crea pedidos `PENDIENTE`, no procesa el pago. Propuesta segura: habilitar el distintivo «Compra confirmada» solo para estados que el negocio considere confirmados (p. ej. PAGO/ENTREGADO), y no marcar PENDIENTE como verificado. El usuario debe aprobar la regla exacta antes de la etapa de compras. No asumir cambios de estado automáticos.

## Convenciones observadas en el código

- Backend Java 21 / Spring Boot 4.1, rutas existentes `/orders`, `/products`, `/collections` (sin prefijo `/api` global).
- `@AuthenticationPrincipal User` permite resolver el propietario desde la sesión JWT. `SecurityConfig` requiere autenticación por defecto.
- `OrderItem` tiene `order`, `product`, `quantity`; `Product.type` incluye CARTA, SOBRE y LOOTBOX. Solo CARTA es elegible para alta desde compra; sobres y lootboxes no acreditan una carta individual.
- Existe `ImageStorageService` con subida a Supabase Storage y conversión WebP. Extenderlo con un método específico de álbum, sin reutilizar la ruta `products/` para fotos personales.
- El frontend tiene `src/Redux/store.js`, slices con `createAsyncThunk`, peticiones Axios y `AccountLayout.jsx`. Las imágenes de las cuatro colecciones ya existen en `src/assets/`.
- No agregar código de álbum a `App.jsx` más allá de una ruta/importación mínimos. Mantener las pantallas y lógica en archivos propios.

## Persistencia mínima

**album**: `id`, `user_id` UNIQUE NOT NULL FK users, `visibility` (PRIVATE/PUBLIC), `created_at`.

**album_section**: `id`, `album_id` FK, `collection_key` (NBA/CARS/MARVEL/MY_LITTLE_PONY), `page_count` >= 1, UNIQUE(`album_id`, `collection_key`). No reutilizar `Collection` de productos como propietario de las páginas.

**album_card**: `id`, `section_id` FK, `page_number` >= 1, `slot_index` entre 0 y 8, `name`, `image_url`, `origin` (MANUAL/PURCHASE), `order_item_id` nullable FK, `created_at`; UNIQUE(`section_id`, `page_number`, `slot_index`). Un mismo producto/nombre puede ocupar varias posiciones. Para PURCHASE, guardar instantánea de nombre/imagen para independizar el álbum de cambios en publicaciones.

La cantidad de ejemplares acreditados por un `OrderItem` no puede exceder `quantity` entre todos los registros PURCHASE que referencien ese ítem, incluso en secciones diferentes. Revalidar con transacción/bloqueo para evitar carreras. Las cartas MANUAL no cuentan contra ese cupo.

Se recomienda migración SQL versionada y revisada, sin operaciones destructivas. El proyecto tiene `spring.jpa.hibernate.ddl-auto=update` y una migración SQL previa en `db/migrations`; verificar el procedimiento de aplicación antes de cambiar esquema. No ejecutar migraciones sobre Supabase de producción sin aprobación.

## API propuesta (ajustable durante implementación)

| Método | Ruta | Acceso | Resultado |
|---|---|---|---|
| GET | `/albums/me` | autenticado | álbum, secciones, páginas y cartas |
| GET | `/albums/me/purchases` | autenticado | ítems CARTA elegibles y cantidades restantes |
| POST | `/albums/me/cards/manual` | autenticado | alta con `multipart/form-data`, sección/página/posición/nombre/foto |
| POST | `/albums/me/cards/purchase` | autenticado | alta con `orderItemId` y destino |
| PATCH | `/albums/me/cards/{cardId}` | propietario | movimiento/cambio de posición o datos permitidos |
| DELETE | `/albums/me/cards/{cardId}` | propietario | quitar del álbum; no afecta pedidos/publicaciones |
| PATCH | `/albums/me/visibility` | propietario | PUBLIC o PRIVATE |
| GET | `/albums/users/{userId}` | autenticado | solo álbum PUBLIC ajeno o álbum propio |

DTOs explícitos; nunca serializar entidades JPA directamente. El backend valida propiedad y permisos, no confía en `userId` del body. Errores: 400 para datos inválidos, 401 sin sesión, 403 para acceso a álbum privado, 404 para recursos inexistentes o ajenos cuando convenga no revelar existencia, 409 para conflicto de posición o cupo de compra.

**Páginas:** `page_count` es persistente. Al llenarse la última página se incrementa de manera transaccional, creando una página vacía. No generar páginas infinitas por simple consulta. El alta debe validar que la página existe; las páginas anteriores pueden quedar incompletas después de movimientos.

**Imágenes:** validar tamaño, formato y contenido en backend; generar nombre aleatorio y prefijo separado `albums/{userId}/...`; no aceptar URLs arbitrarias del cliente como sustituto de foto manual. Definir limpieza segura de archivos huérfanos si falla la persistencia. Para imágenes de pedidos, conservar referencia/snapshot y evaluar copia independiente si el almacenamiento de publicaciones permite borrar el objeto.

## Separación de responsabilidades

Backend:
- `controllers/AlbumController.java`: HTTP, autenticación y DTOs.
- `service/AlbumService.java` y `service/impl/AlbumServiceImpl.java`: reglas de negocio y transacciones.
- `repository/AlbumRepository.java`, `AlbumSectionRepository.java`, `AlbumCardRepository.java`: persistencia.
- `entity/Album.java`, `AlbumSection.java`, `AlbumCard.java`: entidades.
- `entity/dto/album/`: requests/responses; `enums/`: visibilidad, origen, clave de sección.
- Pruebas de integración de aislamiento, cupos, concurrencia y permisos.

Frontend:
- `src/services/albumApi.js`: **solo Axios**, sin `fetch`.
- `src/Redux/albumSlice.js`: `createAsyncThunk`, estados y reducers; registrar en store, sin JWT en acciones/estado.
- `src/hooks/useAlbum.js`: coordinación de sesión y acciones, solo si simplifica realmente el uso; no duplicar datos de Redux en estado local.
- `src/views/MyAlbum.jsx`, `src/views/PublicAlbum.jsx`: composición y rutas.
- `src/components/album/`: `AlbumSection.jsx`, `AlbumPage.jsx`, `AlbumSlot.jsx`, `AddCardModal.jsx`, `AlbumCardDetail.jsx`.
- `AccountLayout.jsx`: enlace «Mi álbum». `App.jsx`: solo integración de rutas.
- `useState` local para modal, página visible o posición seleccionada; Redux para datos remotos, carga y errores.

## Etapas y ramas independientes

1. **Diseño** — `feature/album-01-design` (backend, solo este documento). PR contra `main`.
2. **Backend núcleo** — `feature/album-02-backend-core` (entidades, repositorios, GET propio, posiciones/páginas, tests). Crear desde `main` después del merge de etapa 1.
3. **Backend entradas** — `feature/album-03-backend-cards` (foto manual, compras, límites, tests). Crear desde `main` actualizado tras merge de etapa 2.
4. **Backend privacidad** — `feature/album-04-backend-sharing` (visibilidad, consulta pública, tests de autorización).
5. **Frontend propio** — `feature/album-05-frontend-own` (Redux + Axios, páginas, altas, movimientos, tests).
6. **Frontend compartido** — `feature/album-06-frontend-sharing` (álbum público/privado, modo lectura, tests E2E).

Cada rama parte del `main` que contenga las etapas anteriores; PRs secuenciales, sin merges automáticos. Nunca modificar flujos de carrito, checkout, stock ni promociones salvo integración de lectura aprobada.

## Puertas de calidad antes de cada PR

- Revisión de diff, alcance y compatibilidad; pruebas nuevas para la funcionalidad de esa etapa.
- Backend: compilar y ejecutar tests Maven existentes y nuevos en entorno aislado con configuración de pruebas, sin credenciales de producción.
- Frontend: `npm ci`, `npm run lint`, `npm run build`, tests unitarios y browser relevantes; verificar en Network y Redux DevTools cuando haya UI.
- Seguridad: cuentas A/B, álbum privado, acceso ajeno, manipulación de IDs, límite de compra, archivos inválidos, sesiones vencidas.
- No declarar tests «aprobados» si no se ejecutaron; documentar limitaciones de entorno y bloquear PR funcional si no puede verificarse.
- Commits con un único author, **sin trailers Co-authored-by**; el author depende de la identidad de GitHub conectada.
- PR con resumen, archivos, pruebas ejecutadas, riesgos y pasos de verificación manual. El usuario realiza el merge.

## Criterios de aceptación final

Dos usuarios tienen álbumes independientes; pueden guardar fotos manuales, incorporar cartas CARTA elegibles de pedidos propios, repetir cartas y mezclarlas libremente en cualquier sección. Pueden mover/quitar/reemplazar sin afectar pedidos. Al completar nueve posiciones de la última página aparece una nueva página persistente. La privacidad se respeta en backend y la visita ajena es solo lectura. El frontend usa Redux Toolkit y Axios exclusivamente para HTTP. Todos los flujos existentes siguen pasando sus pruebas.

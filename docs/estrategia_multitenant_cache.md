# Estrategia de Multitenancy y Caching para el SaaS **API Function Registry**

> Última actualización: 2025-08-23  
> Autor: Equipo de Arquitectura

---

## 1. Objetivos de la Estrategia
1. Permitir que múltiples organizaciones (tenants) usen **una misma instancia de aplicación** y **una misma base de datos** de forma aislada y segura.  
2. Ofrecer **planes de pago** escalonados que controlen:
   - Cantidad de llamadas a funciones (API Calls).
   - Cantidad de definiciones de funciones almacenadas.
   - Estrategia de caché (memoria local vs. Redis distribuido).
3. Mantener **bajo TCO** (costo total de operación) creciendo linealmente con la cantidad de tenants y no con el número de instancias desplegadas.

---

## 2. Modelo de Planes de Pago

| Plan | Cache | Llamadas/mes | Funciones almacenadas | SLA |
|------|-------|--------------|-----------------------|-----|
| **Free**    | ConcurrentHashMap (local) | 1 000 | 25  | Best-Effort |
| **Startup** | ConcurrentHashMap (local) | 25 000 | 250 | 99 % |
| **Pro**     | Redis (Cluster de 1 nodo) | 250 000 | 2 500 | 99.5 % |
| **Enterprise** | Redis Cluster (3+ nodos) + persistencia AOF | Ilimitado* | 10 000 | 99.9 % |

\* Ilimitado sujeto a _fair-use_ y throttling dinámico.

---

## 3. Diseño Multitenant

### 3.1 Patrón “Single DB, Shared Schema”
- Única base de datos **PostgreSQL** compartida.
- Todas las tablas incluyen la columna `tenant_id` (`uuid`).
- **Ventajas**: 
  - Simplicidad operativa y bajo coste.
  - Posible sharding horizontal futuro usando `tenant_id`.
- **Riesgos**:
  - Contención en índices → Mitigar con particiones declarativas por rango de `tenant_id`.
  - Queries olvidadas sin filtro → Aplicar **Hibernate/JPA Multi-Tenant** + `Filter`.

### 3.2 Aislamiento en Capa de Datos
1. **Filtro Hibernate**  
   ```java
   @Filter(
       name = "tenantFilter",
       condition = "tenant_id = :tenantId"
   )
   ```
2. **Interceptor Web**  
   - Extrae `X-Tenant-ID` del request (o del JWT).
   - Valida que el `tenantId` pertenece al API Key/JWT.
   - Coloca el valor en `TenantContext` (ThreadLocal o Reactor Context).
3. **TenantIdentifierResolver** (Spring):  
   - Devuelve `TenantContext.getCurrentTenant()` para cada transacción JPA.
4. Test automático que falla si se ejecuta una query sin el parámetro `tenant_id`.

### 3.3 Datos Globales vs. Datos de Tenant
| Tipo de dato | Scope |
|--------------|-------|
| Catálogo de planes de pago | Global |
| Definición de API | Por Tenant |
| Ejecución / Log | Por Tenant |
| Métricas agregadas | Global (con tag `tenant_id`) |

---

## 4. Estrategia de Caching

La caché es un pilar fundamental para mantener la latencia baja y el costo operativo controlado.  
Cada plan de pago determina el tipo de `CacheManager`, el tiempo de vida (TTL) y la política de invalidación.

### 4.1 Selección Dinámica de `CacheManager`
| Plan | Implementación | TTL por defecto | Aislamiento | Observabilidad |
|------|---------------|-----------------|-------------|----------------|
| **Free** | `ConcurrentMapCacheManager` | 15 min | Prefijo `tenant_id` | Micrometer (local) |
| **Startup** | `ConcurrentMapCacheManager` + overflow disco (`Caffeine`) | 30 min | Prefijo `tenant_id` | Micrometer (local) |
| **Pro** | `RedisCacheManager` (1 nodo) | 1 h | Key-space `tenant_id:` | Micrometer + Redis INFO |
| **Enterprise** | `Redis Cluster` (3+ nodos, AOF) | 6 h | Key-space `tenant_id:` + ACL | Micrometer + Redis Enterprise |

### 4.2 Estrategia de Invalidación
1. **Time-Based (TTL)** – se aplica a todos los planes.  
2. **Write-Through** – `Pro` y `Enterprise` usan señales de dominio (Domain Events) para refrescar la caché tras un cambio de datos.  
3. **Smart Eviction** – `Enterprise` analiza ratios hit/miss y elimina solo claves frías (LRU + LFU híbrido).  
4. **Manual Flush** – endpoint `/admin/cache/flush?tenantId=…&region=…` disponible para planes `Pro` y `Enterprise`.

### 4.3 Precarga (Cache Warming)
Al iniciar la aplicación se precargan:
- Definiciones de funciones más utilizadas (top-100 por tenant).
- Configuración de planes y límites de rate-limit.

### 4.4 Métricas y Alertas
- `cache.hit`, `cache.miss`, `cache.evictions` etiquetadas por `tenant_id` y `plan`.
- Alerta si `cache.hit_rate < 50 %` durante 5 min en planes `Pro/Enterprise`.
- Dashboard Grafana “Multitenant Cache Overview”.

---

## 5. Estrategia de Manejo de Sesiones

Las sesiones representan conversaciones de un LLM dentro de un tenant; permiten compartir contexto y controlar límites de uso.

### 5.1 Almacenamiento según Plan
| Plan | Storage | TTL Sesión | Persistencia | Máx. sesiones activas |
|------|---------|-----------|--------------|-----------------------|
| **Free** | Memoria (Map) | 30 min | No | 10 |
| **Startup** | Memoria + disco (`Caffeine` tiered) | 2 h | Opcional | 50 |
| **Pro** | Redis Hash | 24 h | Sí | 500 |
| **Enterprise** | Redis Cluster Hash | 7 días | Sí (AOF) | Ilimitado |

### 5.2 Flujo de Creación y Resolución
1. El `Interceptor Web` extrae `sessionId` del request.  
2. `SessionService` selecciona el backend apropiado según el plan del tenant.  
3. Si la sesión no existe se crea con metadatos (`tenantId`, `createdAt`, `ttl`).  
4. En cada ejecución de función se actualiza `lastAccess` y contador de invocaciones.

### 5.3 Expiración y Limpieza
- **Local**: tarea `@Scheduled` cada 5 min elimina sesiones expiradas.
- **Redis**: se delega al propio TTL del key, sólo se limpian índices secundarios.

### 5.4 Monitorización
Métricas expuestas:
- `session.active{tenant_id,plan}`.
- `session.created.total` y `session.expired.total`.
Alertas si un tenant excede el 80 % de su límite de sesiones.

Con estas adiciones el documento cubre tanto la gestión avanzada de caché multitenant como una estrategia clara y escalable para controlar las sesiones de conversación de los LLM.

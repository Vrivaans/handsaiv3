# **API Function Registry - Documento de Requerimientos Funcionales**
## **1. Descripción General del Proyecto**
### **1.1 Propósito**
Construir un microservicio reactivo que permita a los Modelos de Lenguaje Grande (LLMs) descubrir y ejecutar funciones de APIs REST dinámicamente a través de una interfaz unificada. El sistema actúa como un **Function Calling Gateway** que abstrae la complejidad de múltiples APIs externas.
### **1.2 Propuesta de Valor Clave**
- **Descubrimiento Dinámico**: Los LLMs descubren funciones de APIs disponibles en tiempo de ejecución
- **Function Calling Estándar**: Formato compatible con OpenAI Function Calling para máxima interoperabilidad
- **Tolerancia a Fallos**: Manejo elegante de APIs no disponibles con logging completo
- **Recarga en Caliente**: Definiciones de funciones cacheadas en memoria, actualizadas sin reiniciar el servicio
- **Autenticación Unificada**: Un solo punto de configuración de credenciales para múltiples APIs

## **2. Arquitectura Técnica**
### **2.1 Stack Tecnológico**
- **Framework**: Spring Boot 3.2+
- **Java**: Java 21 LTS con Hilos Virtuales
- **Stack Reactivo**: Spring WebFlux con Hilos Virtuales
- **Compilación**: GraalVM con AOT (Fase 2)
- **Base de Datos**: PostgreSQL con Spring Data JPA
- **Seguridad**: Spring Security con API Keys
- **Build**: Maven con Native Build Tools
- **Adicionales**: Lombok, Spring DevTools

### **2.2 Features de Java 21**
- **Records**: Para todos los DTOs, requests y responses
- **Virtual Threads**: Para alta concurrencia sin blocking
- **Pattern Matching**: Para switch expressions y validaciones
- **Text Blocks**: Para templates SQL y JSON

### **2.3 Enfoque Reactivo**
- **Controladores**: Mono/Flux para respuestas HTTP
- **Servicios**: CompletableFuture con Hilos Virtuales
- **Base de Datos**: JPA con wrapping de Hilos Virtuales
- **Llamadas APIs**: WebClient reactivo
- **IMPORTANTE**: Controladores con mínima lógica, toda la lógica de negocio en servicios

## **3. Funcionalidades Principales**
### **3.1 Gestión de APIs**
**Como administrador del sistema, quiero:**
- Registrar APIs REST con configuración de endpoints, autenticación y esquemas de parámetros
- Habilitar/deshabilitar APIs sin reiniciar el servicio
- Ver estadísticas de uso y métricas de rendimiento

**Criterios de Aceptación FASE 1:**
- ✅ Definiciones de APIs almacenadas en PostgreSQL
- ✅ Cache simple en memoria (ConcurrentHashMap)
- ✅ Validación de conectividad al registrar APIs (health check)
- ✅ CRUD básico para gestión de APIs

### **3.2 Descubrimiento Dinámico de Funciones**
**Como LLM, quiero:**
- Solicitar funciones disponibles vía endpoint estandarizado
- Recibir esquemas compatibles con OpenAI Function Calling
- Obtener solo funciones de APIs habilitadas y saludables

**Criterios de Aceptación FASE 1:**
- ✅ `GET /api/functions/discover` retorna lista de funciones API
- ✅ Formato compatible con OpenAI Function Calling schema
- ✅ Tiempo de respuesta < 500ms (cache en memoria)
- ✅ Filtrado automático de APIs deshabilitadas

### **3.3 Ejecución de Funciones**
**Como LLM, quiero:**
- Ejecutar cualquier función descubierta con validación de parámetros
- Recibir formato de respuesta consistente
- Obtener mensajes de error significativos cuando las APIs fallan

**Criterios de Aceptación FASE 1:**
- ✅ `POST /api/functions/execute` para ejecución de funciones
- ✅ Validación de parámetros contra esquemas almacenados
- ✅ Timeout handling (30s configurable)
- ✅ Retry automático (3 intentos configurables)
- ✅ Formato de respuesta estandarizado

### **3.4 Logging y Monitoreo**
**Como operador del sistema, quiero:**
- Rastrear todas las ejecuciones con payloads completos
- Monitorear rendimiento y tasas de éxito por API
- Identificar patrones de fallo y APIs problemáticas

**Criterios de Aceptación:**
- ✅ Logging completo con precisión de milisegundos
- ✅ Retención de logs: 30 días
- ✅ Métricas de rendimiento por API
- ✅ Alertas automáticas para APIs con >10% de fallo

## **4. Especificaciones de API**
### **4.1 Descubrimiento de Funciones**
``` http
GET /api/functions/discover
```
**Respuesta:**
``` json
{
  "functions": [
    {
      "name": "get_weather",
      "description": "Obtener información del clima para una ubicación específica",
      "parameters": {
        "type": "object",
        "properties": {
          "location": {
            "type": "string", 
            "description": "Nombre de la ciudad"
          },
          "units": {
            "type": "string",
            "enum": ["metric", "imperial"],
            "description": "Unidades de medida"
          }
        },
        "required": ["location"]
      }
    }
  ],
  "totalCount": 1,
  "lastUpdated": "2024-01-15T10:30:00Z"
}
```
### **4.2 Ejecución de Funciones**
``` http
POST /api/functions/execute
```
**Petición:**
``` json
{
  "functionName": "get_weather",
  "parameters": {
    "location": "Buenos Aires",
    "units": "metric"
  },
  "sessionId": "conv_12345"
}
```
**Respuesta:**
``` json
{
  "success": true,
  "result": {
    "temperature": 22,
    "description": "Parcialmente nublado",
    "humidity": 65
  },
  "executionTimeMs": 850,
  "apiName": "weather_api"
}
```
### **4.3 Endpoints de Administración**
- `POST /admin/apis` - Registrar nueva API
- `PUT /admin/apis/{id}` - Actualizar API existente
- `DELETE /admin/apis/{id}` - Eliminar API
- `GET /admin/apis/health` - Estado de salud de todas las APIs
- `GET /admin/apis/stats` - Estadísticas de uso

## **5. Modelos de Datos**
### **5.1 Entidades Principales**
- **BaseModel**: Campos comunes (id, code, enabled, timestamps)
- **ApiDefinition**: Configuración y metadatos de APIs REST
- **FunctionParameter**: Esquemas de parámetros para function calling
- **FunctionExecutionLog**: Seguimiento de ejecución y métricas

### **5.2 Ejemplo de Entidad API**
``` java
@Entity
@Data
@NoArgsConstructor
@AllArgsConstructor
public class ApiDefinition extends BaseModel {
    private String name;
    private String description;
    private String baseUrl;
    private String endpointPath;
    private HttpMethodEnum httpMethod;
    private AuthenticationTypeEnum authType;
    private String authToken;
    private Integer timeoutSeconds = 30;
    private Integer retryAttempts = 3;
    private Boolean enabled = true;
    private Instant lastHealthCheck;
    
    @OneToMany(mappedBy = "apiDefinition", cascade = CascadeType.ALL)
    private List<FunctionParameter> parameters;
}
```
## **6. Estrategia de Implementación**
### **FASE 1: MVP Comercial (Semanas 1-4)**
**Objetivo**: Producto mínimo viable para primeros clientes
**Funcionalidades Core:**
- ✅ Modelos de datos: ApiDefinition, FunctionParameter, FunctionExecutionLog
- ✅ CRUD completo para gestión de APIs
- ✅ Endpoint de descubrimiento de funciones
- ✅ Endpoint de ejecución de funciones
- ✅ Validación y health checks
- ✅ Logging básico y manejo de errores

**Stack Simplificado:**
- Spring Boot 3.2+ (JVM tradicional)
- PostgreSQL + JPA
- Spring Security básico con API Keys
- Cache simple en memoria

**Criterios de Éxito:**
- ✅ 5+ APIs diferentes funcionando correctamente
- ✅ LLM ejecuta funciones en < 2 segundos
- ✅ Error handling elegante
- ✅ Primer cliente pagando

### **FASE 2: Optimización Enterprise (Semanas 5-8)**
**Objetivo**: Características enterprise y performance
**Nuevas Funcionalidades:**
- Virtual Threads completos
- GraalVM AOT compilation
- Cache event-driven con invalidación
- JWT + OAuth2 integration
- Métricas avanzadas y dashboard
- Multi-tenant support

## **7. Go-to-Market Strategy**
### **Fase 1 - Positioning**
- **Target**: Startups y equipos de desarrollo
- **Pitch**: _"API Function Gateway para LLMs - conecta tus APIs con ChatGPT en minutos"_
- **Pricing**: $200-500/mes
- **Casos de uso**: Bots de soporte, automatización interna

### **Ejemplo de Uso Real**
``` javascript
// En tu aplicación LLM
const functions = await fetch('/api/functions/discover');
const weatherFunction = functions.find(f => f.name === 'get_weather');

// El LLM decide llamar a la función
const result = await fetch('/api/functions/execute', {
  method: 'POST',
  body: JSON.stringify({
    functionName: 'get_weather',
    parameters: { location: 'Madrid' }
  })
});
```
## **8. Configuración Técnica**
### **8.1 Hilos Virtuales**
``` yaml
spring:
  threads:
    virtual:
      enabled: true
  task:
    execution:
      pool:
        virtual-threads: true
```
### **8.2 Seguridad**
``` yaml
security:
  api-key:
    header: "X-API-Key"
    required: true
  rate-limit:
    requests-per-minute: 100
```

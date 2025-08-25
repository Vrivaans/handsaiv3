# HandsAI v2 - Microservicio de Gestión de Herramientas de IA

## 🚀 Descripción

HandsAI v2 es un microservicio reactivo construido con Spring Boot 3.2+ y Java 21 que permite a los Modelos de Lenguaje Grande (LLMs) descubrir y ejecutar herramientas dinámicamente a través de una interfaz unificada. El sistema soporta APIs REST con descubrimiento dinámico, validación de parámetros y ejecución tolerante a fallos.

### 🎯 Características Principales

- **Descubrimiento Dinámico**: Los LLMs pueden descubrir herramientas disponibles en tiempo de ejecución
- **Interfaz Unificada**: Un solo endpoint para ejecutar cualquier herramienta registrada
- **Tolerancia a Fallos**: Manejo elegante de errores con logging completo
- **Caché Inteligente**: Definiciones de herramientas cacheadas en memoria para alta performance
- **Hilos Virtuales**: Aprovecha Java 21 para alta concurrencia sin bloqueo

## 🛠️ Stack Tecnológico

- **Framework**: Spring Boot 3.2+ con Spring WebFlux
- **Java**: Java 21 LTS con Virtual Threads
- **Base de Datos**: PostgreSQL con Spring Data JPA
- **Seguridad**: Spring Security con API Keys
- **Build**: Maven
- **Adicionales**: Lombok, Spring DevTools

## 📋 Requisitos Previos

- Java 21 LTS
- PostgreSQL 14+
- Maven 3.8+

## ⚡ Configuración y Arranque

1.  **Clonar el repositorio**

2.  **Configurar la base de datos**
    Abre el archivo `src/main/resources/application.properties` y ajusta las propiedades de conexión a tu base de datos PostgreSQL:

    ```properties
    spring.datasource.url=jdbc:postgresql://localhost:5432/handsai_db
    spring.datasource.username=tu_usuario
    spring.datasource.password=tu_contraseña
    spring.jpa.hibernate.ddl-auto=update
    ```

3.  **Construir y ejecutar la aplicación**
    Puedes ejecutar la aplicación usando el wrapper de Maven:
    ```bash
    ./mvnw spring-boot:run
    ```
    El servicio estará disponible en `http://localhost:8080`.

## 📖 API Endpoints

La API se divide en dos secciones principales: la API de Administración para gestionar las herramientas y la API Pública para que los LLMs las descubran y ejecuten.

### API de Administración (`/admin/tools/api`)

Estos endpoints se utilizan para gestionar el ciclo de vida de las `ApiTool`.

#### 1. Crear una Herramienta API

- **Endpoint**: `POST /admin/tools/api`
- **Descripción**: Registra una nueva herramienta de API en el sistema.
- **Request Body**:

  ```json
  {
    "name": "Servicio de Clima",
    "description": "Obtiene el clima actual para una ciudad específica.",
    "baseUrl": "https://api.weatherapi.com",
    "endpointPath": "/v1/current.json",
    "httpMethod": "GET",
    "parameters": [
      {
        "name": "q",
        "type": "STRING",
        "description": "Nombre de la ciudad",
        "required": true,
        "defaultValue": null
      },
      {
        "name": "key",
        "type": "STRING",
        "description": "API Key para el servicio de clima",
        "required": true,
        "defaultValue": null
      }
    ]
  }
  ```

- **Response Body (Ejemplo)**:

  ```json
  {
    "id": 1,
    "code": "a1b2c3d4",
    "name": "Servicio de Clima",
    "description": "Obtiene el clima actual para una ciudad específica.",
    "baseUrl": "https://api.weatherapi.com",
    "endpointPath": "/v1/current.json",
    "httpMethod": "GET",
    "enabled": true,
    "healthy": true,
    "lastHealthCheck": "2025-08-21T10:00:00Z",
    "parameters": [
      {
        "id": 1,
        "code": "p1o2i3u4",
        "name": "q",
        "type": "STRING",
        "description": "Nombre de la ciudad",
        "required": true,
        "defaultValue": null
      },
      {
        "id": 2,
        "code": "k1j2h3g4",
        "name": "key",
        "type": "STRING",
        "description": "API Key para el servicio de clima",
        "required": true,
        "defaultValue": null
      }
    ]
  }
  ```

#### 2. Actualizar una Herramienta API

- **Endpoint**: `PUT /admin/tools/api/{id}`
- **Descripción**: Actualiza los detalles de una herramienta existente.
- **Request Body**: `UpdateApiToolRequest` (similar al de creación, pero puede incluir el campo `enabled`).

#### 3. Obtener todas las Herramientas API

- **Endpoint**: `GET /admin/tools/api`
- **Descripción**: Devuelve una lista de todas las herramientas registradas.

#### 4. Obtener una Herramienta API por ID

- **Endpoint**: `GET /admin/tools/api/{id}`
- **Descripción**: Devuelve los detalles de una herramienta específica.

#### 5. Eliminar una Herramienta API

- **Endpoint**: `DELETE /admin/tools/api/{id}`
- **Descripción**: Elimina una herramienta del sistema.

### API Pública (`/api/tools`)

Estos endpoints están diseñados para ser consumidos por LLMs.

#### 1. Descubrir Herramientas

- **Endpoint**: `GET /api/tools/discover`
- **Descripción**: Devuelve una lista de todas las herramientas activas y saludables que un LLM puede utilizar.
- **Response Body (Ejemplo)**:

  ```json
  {
    "tools": [
      {
        "name": "Servicio de Clima",
        "description": "Obtiene el clima actual para una ciudad específica.",
        "type": "api_tool",
        "parameters": {
          "type": "object",
          "properties": {
            "q": {
              "type": "string",
              "description": "Nombre de la ciudad"
            },
            "key": {
              "type": "string",
              "description": "API Key para el servicio de clima"
            }
          },
          "required": ["q", "key"]
        }
      }
    ],
    "totalCount": 1,
    "lastUpdated": "2025-08-21T10:05:00Z"
  }
  ```

#### 2. Ejecutar una Herramienta

- **Endpoint**: `POST /api/tools/execute`
- **Descripción**: Ejecuta una herramienta específica con los parámetros proporcionados.
- **Request Body**:

  ```json
  {
    "toolName": "Servicio de Clima",
    "parameters": {
      "q": "Buenos Aires",
      "key": "YOUR_API_KEY"
    },
    "sessionId": "conv_12345"
  }
  ```

- **Response Body (Ejemplo)**:

  ```json
  {
    "success": true,
    "result": {
      "location": {
        "name": "Buenos Aires",
        "region": "Distrito Federal",
        "country": "Argentina"
      },
      "current": {
        "temp_c": 15.0,
        "condition": {
          "text": "Partly cloudy"
        }
      }
    },
    "executionTimeMs": 750,
    "toolType": "api_tool"
  }
  ```</llm-patch>

Especificación Técnica: Implementación MCP Server
📋 Objetivo
Adaptar el sistema actual de APIs para que sea compatible con el protocolo Model Context Protocol (MCP), manteniendo la funcionalidad existente.

🔧 Arquitectura Propuesta
Estado Actual
/api/tools/discover → JSON normal
/api/tools/execute  → JSON normal
Estado Final
/api/tools/discover → JSON normal (mantenido)
/api/tools/execute  → JSON normal (mantenido)
/mcp/tools/list     → JSON-RPC 2.0 (nuevo)--hecho
/mcp/tools/call     → JSON-RPC 2.0 (nuevo)--hecho
📑 Especificación de Endpoints MCP
1. GET /mcp/tools/list - Listado de Herramientas
   Propósito: Informar a los LLMs qué herramientas están disponibles

Flujo:

Recibir petición GET
Llamar a toolDiscoveryService.discoverTools()
Convertir respuesta al formato MCP
Devolver JSON-RPC 2.0
Request:

http
GET /mcp/tools/list
Content-Type: application/json
Response Exitosa:

json
{
"jsonrpc": "2.0",
"result": {
"tools": [
{
"name": "clima4",
"description": "Obtiene el clima actual para una ciudad específica.",
"inputSchema": {
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
]
}
}
Response Error:

json
{
"jsonrpc": "2.0",
"error": {
"code": -32603,
"message": "Internal error discovering tools"
}
}
2. POST /mcp/tools/call - Ejecutar Herramienta
   Propósito: Permitir a los LLMs ejecutar herramientas específicas

Flujo:

Recibir petición POST con formato MCP
Extraer name y arguments de los parámetros
Convertir a formato ToolExecuteRequest
Llamar a toolExecutionService.executeApiTool()
Convertir respuesta al formato MCP
Devolver JSON-RPC 2.0
Request:

json
{
"jsonrpc": "2.0",
"method": "tools/call",
"params": {
"name": "clima4",
"arguments": {
"q": "Buenos Aires",
"key": "abc123"
}
},
"id": "1"
}
Response Exitosa:

json
{
"jsonrpc": "2.0",
"result": {
"content": [
{
"type": "text",
"text": "{\"temperature\": 22, \"condition\": \"sunny\", \"city\": \"Buenos Aires\"}"
}
]
}
}
Response Error:

json
{
"jsonrpc": "2.0",
"error": {
"code": -32602,
"message": "Invalid params: missing required parameter 'q'"
}
}
🛠️ Tareas de Implementación
Fase 1: Crear Controller MCP
Crear McpController.java
Configurar mapping /mcp
Inyectar servicios existentes (ToolDiscoveryService, ToolExecutionService)
Fase 2: Implementar /mcp/tools/list
Endpoint GET /mcp/tools/list
Mapear ToolDiscoveryResponse → formato MCP
Cambiar parameters → inputSchema
Remover campos extras (totalCount, lastUpdated, type)
Envolver en JSON-RPC 2.0
Manejo de errores
Fase 3: Implementar /mcp/tools/call
Endpoint POST /mcp/tools/call
Parsear request MCP → ToolExecuteRequest
Ejecutar herramienta usando servicio existente
Mapear ToolExecuteResponse → formato MCP
Formatear resultado como content.text
Manejo de errores con códigos JSON-RPC
Fase 4: Testing
Test unitario para /mcp/tools/list
Test unitario para /mcp/tools/call
Test de integración con servicios existentes
Test de manejo de errores
Fase 5: Documentación
Documentar endpoints MCP
Guía de uso para conectar LLMs
Ejemplos de requests/responses
🎯 Criterios de Aceptación
Funcionalidad
✅ /mcp/tools/list devuelve herramientas en formato MCP
✅ /mcp/tools/call ejecuta herramientas correctamente
✅ Formato JSON-RPC 2.0 estricto
✅ Manejo de errores apropiado
✅ Endpoints existentes siguen funcionando sin cambios
Rendimiento
✅ Tiempo de respuesta similar a endpoints actuales
✅ Reutiliza servicios existentes (sin duplicar lógica)
Seguridad
✅ Validación de parámetros de entrada
✅ Manejo seguro de errores (no exponer stack traces)
📝 Transformaciones de Datos
Mapeo ToolDiscoveryResponse → MCP Tools
java
// Entrada (tu formato actual)
{
"name": "clima4",
"description": "...",
"type": "api_tool",           // ← ELIMINAR
"parameters": { ... }         // ← CAMBIAR por "inputSchema"
}

// Salida (formato MCP)
{
"name": "clima4",
"description": "...",
"inputSchema": { ... }        // ← RENOMBRADO
}
Mapeo MCP Request → ToolExecuteRequest
java
// Entrada MCP
{
"params": {
"name": "clima4",
"arguments": { "q": "BA", "key": "123" }
}
}

// Tu formato actual
{
"toolName": "clima4",
"parameters": { "q": "BA", "key": "123" }
}
🚨 Códigos de Error JSON-RPC 2.0
Código	Significado	Cuándo usar
-32700	Parse error	JSON malformado
-32600	Invalid Request	Request no válido
-32601	Method not found	Método no existe
-32602	Invalid params	Parámetros incorrectos
-32603	Internal error	Error del servidor
🔧 Configuración Adicional

java
@CrossOrigin(origins = "*")
@RestController
@RequestMapping("/mcp")
public class McpController { ... }

🎉 Resultado Final
Al completar esta implementación tendrás:

✅ Sistema actual funcionando sin cambios
✅ Compatibilidad completa con MCP para LLMs
✅ Claude y otros LLMs podrán conectarse y usar tus APIs
✅ Mantenimiento simplificado (un solo sistema, dos interfaces)

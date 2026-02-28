import { Injectable, inject } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';

export interface ApiTool {
    id?: number;
    name: string;
    code: string;
    description: string;
    providerId?: number;
    providerName?: string;
    baseUrl?: string;
    endpointPath: string;
    httpMethod: string;
    authenticationType?: string;
    apiKeyLocation?: string;
    apiKeyName?: string;
    apiKeyValue?: string;
    enabled: boolean;
    healthy?: boolean;
    isExportable?: boolean;
    parameters?: any[];
}

export interface AnalyticsSummary {
    totalExecutions: number;
    successfulExecutions: number;
    successRatePercentage: number;
    averageLatencyMs: number;
}

export interface ToolExecutionLog {
    id: number;
    toolName: string;
    sessionId: string;
    requestPayload: string;
    responsePayload: string;
    executionTimeMs: number;
    success: boolean;
    errorMessage: string;
    executedAt: string;
}

export interface PageResponse<T> {
    content: T[];
    totalElements: number;
    totalPages: number;
    size: number;
    number: number;
}

export interface McpTool {
    name: string;
    description: string;
    inputSchema: any;
}

export interface McpListResponse {
    result: {
        tools: McpTool[];
    };
}

export interface TaskMemory {
    id: number;
    title: string;
    description: string;
    status: string;
    priority: string;
    createdByAgent: string;
    createdAt: string;
    updatedAt?: string;
}

@Injectable({
    providedIn: 'root'
})
export class ApiService {
    private http = inject(HttpClient);

    // Tools guardadas en BD (Admin API)
    getStoredTools(): Observable<ApiTool[]> {
        return this.http.get<ApiTool[]>('/admin/tools/api');
    }

    // Tools activas disponibles para el agente (MCP API)
    getActiveTools(): Observable<McpListResponse> {
        return this.http.get<McpListResponse>('/mcp/tools/list');
    }

    // Registrar nueva herramienta en BD (Admin API)
    createApiTool(tool: ApiTool): Observable<ApiTool> {
        return this.http.post<ApiTool>('/admin/tools/api', tool);
    }

    // Actualizar herramienta existente en BD (Admin API)
    updateApiTool(id: number, tool: ApiTool): Observable<ApiTool> {
        return this.http.put<ApiTool>(`/admin/tools/api/${id}`, tool);
    }

    createApiToolsBatch(tools: ApiTool[]): Observable<any> {
        return this.http.post<any>('/admin/tools/api/batch', tools);
    }

    createApiProvider(provider: any): Observable<any> {
        return this.http.post<any>('/admin/providers', provider);
    }

    updateApiProvider(id: number, provider: any): Observable<any> {
        return this.http.put<any>(`/admin/providers/${id}`, provider);
    }

    getApiProviders(): Observable<any[]> {
        return this.http.get<any[]>('/admin/providers');
    }

    getExportableProviders(ids?: number[]): Observable<any[]> {
        let url = '/api/export/providers';
        if (ids && ids.length > 0) {
            url += `?ids=${ids.join(',')}`;
        }
        return this.http.get<any[]>(url);
    }

    importProviders(payload: any[]): Observable<any> {
        return this.http.post<any>('/api/import/providers', payload);
    }

    // Eliminar herramienta en BD (Admin API)
    deleteApiTool(id: number): Observable<void> {
        return this.http.delete<void>(`/admin/tools/api/${id}`);
    }

    // Analytics endpoints
    getAnalyticsSummary(days: number = 30): Observable<AnalyticsSummary> {
        return this.http.get<AnalyticsSummary>(`/admin/analytics/summary?days=${days}`);
    }

    getExecutionLogs(page: number = 0, size: number = 20): Observable<PageResponse<ToolExecutionLog>> {
        return this.http.get<PageResponse<ToolExecutionLog>>(`/admin/analytics/logs?page=${page}&size=${size}`);
    }

    // --- Native Memory Tasks ---

    getPendingTasks(): Observable<TaskMemory[]> {
        return this.http.get<TaskMemory[]>('/api/v1/memory/tasks');
    }

    getCompletedTasks(): Observable<TaskMemory[]> {
        return this.http.get<TaskMemory[]>('/api/v1/memory/tasks/completed');
    }

    deleteTask(id: number): Observable<void> {
        return this.http.delete<void>(`/api/v1/memory/tasks/${id}`);
    }
}

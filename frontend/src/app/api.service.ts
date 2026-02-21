import { Injectable, inject } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';

export interface ApiTool {
    id: number;
    name: string;
    description: string;
    enabled: boolean;
    healthy: boolean;
    code: string;
    baseUrl: string;
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
    createApiTool(toolData: any): Observable<ApiTool> {
        return this.http.post<ApiTool>('/admin/tools/api', toolData);
    }
}

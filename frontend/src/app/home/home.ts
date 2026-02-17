import { Component, OnInit, inject } from '@angular/core';
import { CommonModule, AsyncPipe } from '@angular/common';
import { ApiService, ApiTool, McpTool } from '../api.service';

@Component({
    selector: 'app-home',
    standalone: true,
    imports: [CommonModule, AsyncPipe],
    templateUrl: './home.html',
    styleUrl: '../app.scss'
})
export class HomeComponent implements OnInit {
    private apiService = inject(ApiService);

    storedTools: ApiTool[] = [];
    activeTools: McpTool[] = [];

    error: string | null = null;

    ngOnInit() {
        this.loadData();
    }

    loadData() {
        this.apiService.getStoredTools().subscribe({
            next: (tools) => this.storedTools = tools,
            error: (err) => console.error('Error fetching stored tools', err)
        });

        this.apiService.getActiveTools().subscribe({
            next: (response) => {
                if (response && response.result && response.result.tools) {
                    this.activeTools = response.result.tools;
                }
            },
            error: (err) => {
                console.error('Error fetching active tools', err);
                // Don't show error for active tools if backend is down, just log
            }
        });
    }
}

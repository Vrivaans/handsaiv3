import { Component, OnInit, inject } from '@angular/core';
import { CommonModule, AsyncPipe } from '@angular/common';
import { ApiService, ApiTool, McpTool } from '../api.service';

@Component({
    selector: 'app-home',
    standalone: true,
    imports: [CommonModule],
    templateUrl: './home.html',
    styleUrl: '../app.scss'
})
export class HomeComponent implements OnInit {
    private apiService = inject(ApiService);

    storedTools: ApiTool[] = [];
    activeTools: McpTool[] = [];

    error: string | null = null;
    showDeleteModal = false;
    toolToDelete: number | null = null;
    toolToDeleteName: string = '';

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

    deleteTool(id: number, name: string, event: Event) {
        event.preventDefault();
        event.stopPropagation();
        this.toolToDelete = id;
        this.toolToDeleteName = name;
        this.showDeleteModal = true;
    }

    closeDeleteModal() {
        this.showDeleteModal = false;
        this.toolToDelete = null;
        this.toolToDeleteName = '';
    }

    confirmDelete() {
        if (this.toolToDelete !== null) {
            this.apiService.deleteApiTool(this.toolToDelete).subscribe({
                next: () => {
                    this.loadData();
                    this.closeDeleteModal();
                },
                error: (err) => console.error('Error deleting tool', err)
            });
        }
    }
}

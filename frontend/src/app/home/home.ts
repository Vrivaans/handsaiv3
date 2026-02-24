import { Component, OnInit, inject } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormBuilder, FormGroup, ReactiveFormsModule, Validators, FormArray } from '@angular/forms';
import { ApiService, ApiTool, McpTool } from '../api.service';

@Component({
    selector: 'app-home',
    standalone: true,
    imports: [CommonModule, ReactiveFormsModule],
    templateUrl: './home.html',
    styleUrl: '../app.scss'
})
export class HomeComponent implements OnInit {
    private apiService = inject(ApiService);
    private fb = inject(FormBuilder);

    storedTools: ApiTool[] = [];
    activeTools: McpTool[] = [];

    error: string | null = null;

    // Delete Modal State
    showDeleteModal = false;
    toolToDelete: number | null = null;
    toolToDeleteName: string = '';

    // Edit Modal State
    showEditModal = false;
    toolToEdit: ApiTool | null = null;
    editForm: FormGroup;
    isSubmittingEdit = false;

    constructor() {
        this.editForm = this.fb.group({
            name: ['', Validators.required],
            code: [''],
            description: ['', Validators.required],
            endpointPath: ['', Validators.required],
            httpMethod: ['GET', Validators.required],
            enabled: [true],
            parameters: this.fb.array([])
        });
    }

    get parameters() {
        return this.editForm.get('parameters') as FormArray;
    }

    addParameter() {
        const parameterForm = this.fb.group({
            name: ['', Validators.required],
            type: ['STRING', Validators.required],
            description: ['', Validators.required],
            required: [true],
            defaultValue: ['']
        });
        this.parameters.push(parameterForm);
    }

    removeParameter(index: number) {
        this.parameters.removeAt(index);
    }

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

    // Edit Logic
    openEditModal(tool: ApiTool) {
        this.toolToEdit = tool;

        this.parameters.clear();
        if (tool.parameters && tool.parameters.length > 0) {
            tool.parameters.forEach(p => {
                this.parameters.push(this.fb.group({
                    id: [p.id],
                    name: [p.name, Validators.required],
                    type: [p.type || 'STRING', Validators.required],
                    description: [p.description || '', Validators.required],
                    required: [p.required !== undefined ? p.required : true],
                    defaultValue: [p.defaultValue || '']
                }));
            });
        }

        this.editForm.patchValue({
            name: tool.name,
            code: tool.code,
            description: tool.description,
            endpointPath: tool.endpointPath,
            httpMethod: tool.httpMethod,
            enabled: tool.enabled
        });
        this.showEditModal = true;
    }

    closeEditModal() {
        this.showEditModal = false;
        this.toolToEdit = null;
        this.editForm.reset();
        this.error = null;
    }

    saveEditChanges() {
        if (this.editForm.invalid || !this.toolToEdit || !this.toolToEdit.id) {
            this.editForm.markAllAsTouched();
            return;
        }

        this.isSubmittingEdit = true;
        this.error = null;

        const updatedData = {
            ...this.toolToEdit, // Preserve provider configs, api keys etc
            ...this.editForm.value
        };

        this.apiService.updateApiTool(this.toolToEdit.id, updatedData).subscribe({
            next: () => {
                this.isSubmittingEdit = false;
                this.loadData();
                this.closeEditModal();
            },
            error: (err) => {
                this.isSubmittingEdit = false;
                if (err.error && err.error.message) {
                    this.error = err.error.message;
                } else {
                    this.error = 'Ocurrió un error al actualizar la herramienta.';
                }
                console.error('Error updating tool', err);
            }
        });
    }
}

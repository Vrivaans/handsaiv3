import { Component } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormBuilder, FormGroup, ReactiveFormsModule, Validators, FormArray } from '@angular/forms';
import { ApiService } from '../api.service';
import { Router, RouterModule } from '@angular/router';
import { OnInit } from '@angular/core';

@Component({
    selector: 'app-tools',
    standalone: true,
    imports: [CommonModule, ReactiveFormsModule, RouterModule],
    templateUrl: './tools.html',
    styleUrl: './tools.scss'
})
export class ToolsComponent implements OnInit {
    toolForm: FormGroup;
    isSubmitting = false;
    successMessage = '';
    errorMessage = '';
    providers: any[] = [];

    // Edit Provider Modal State
    showEditProviderModal = false;
    providerToEdit: any | null = null;
    editProviderForm: FormGroup;
    isEditingProvider = false;
    editProviderError = '';

    constructor(private fb: FormBuilder, private apiService: ApiService, private router: Router) {
        this.toolForm = this.fb.group({
            name: ['', Validators.required],
            code: [''],
            enabled: [true],
            isExportable: [false],
            description: ['', Validators.required],
            providerId: [''],

            isCreatingProvider: [false],
            providerName: [''],
            providerCode: [''],
            baseUrl: [''],
            authenticationType: ['NONE'],
            apiKeyLocation: ['HEADER'],
            apiKeyName: [''],
            apiKeyValue: [''],
            providerIsExportable: [false],
            customHeaders: this.fb.array([]),

            endpointPath: ['', Validators.required],
            httpMethod: ['GET', Validators.required],
            parameters: this.fb.array([])
        });

        this.editProviderForm = this.fb.group({
            name: ['', Validators.required],
            code: [''],
            baseUrl: ['', Validators.required],
            authenticationType: ['NONE'],
            apiKeyLocation: ['HEADER'],
            apiKeyName: [''],
            apiKeyValue: [''],
            isExportable: [false],
            customHeaders: this.fb.array([])
        });
    }

    ngOnInit(): void {
        this.apiService.getApiProviders().subscribe({
            next: (data) => this.providers = data,
            error: (err) => console.error('Error fetching providers', err)
        });
    }

    get parameters() {
        return this.toolForm.get('parameters') as FormArray;
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

    // --- Custom Headers Helpers ---
    get customHeaders() {
        return this.toolForm.get('customHeaders') as FormArray;
    }

    addCustomHeader() {
        this.customHeaders.push(this.fb.group({
            key: ['', Validators.required],
            value: ['', Validators.required]
        }));
    }

    removeCustomHeader(index: number) {
        this.customHeaders.removeAt(index);
    }

    get editCustomHeaders() {
        return this.editProviderForm.get('customHeaders') as FormArray;
    }

    addEditCustomHeader() {
        this.editCustomHeaders.push(this.fb.group({
            key: ['', Validators.required],
            value: ['', Validators.required]
        }));
    }

    removeEditCustomHeader(index: number) {
        this.editCustomHeaders.removeAt(index);
    }

    // --- Provider Edit Logic ---
    openEditProviderModal() {
        const selectedId = this.toolForm.get('providerId')?.value;
        if (!selectedId) return;

        const provider = this.providers.find(p => p.id == selectedId);
        if (!provider) return;

        this.providerToEdit = provider;
        this.editProviderForm.patchValue({
            name: provider.name,
            code: provider.code,
            baseUrl: provider.baseUrl,
            authenticationType: provider.authenticationType,
            apiKeyLocation: provider.apiKeyLocation,
            apiKeyName: provider.apiKeyName,
            isExportable: provider.isExportable,
            apiKeyValue: '' // Reset secret field for security, require re-entry if needed
        });

        this.editCustomHeaders.clear();
        if (provider.customHeaders) {
            Object.keys(provider.customHeaders).forEach(k => {
                this.editCustomHeaders.push(this.fb.group({
                    key: [k, Validators.required],
                    value: [provider.customHeaders[k], Validators.required]
                }));
            });
        }

        this.showEditProviderModal = true;
    }

    closeEditProviderModal() {
        this.showEditProviderModal = false;
        this.providerToEdit = null;
        this.editProviderForm.reset();
        this.editProviderError = '';
        this.isEditingProvider = false;
    }

    saveProviderChanges() {
        if (this.editProviderForm.invalid || !this.providerToEdit) {
            this.editProviderForm.markAllAsTouched();
            return;
        }

        this.isEditingProvider = true;
        this.editProviderError = '';

        const payload = {
            ...this.editProviderForm.value,
            customHeaders: this.editCustomHeaders.value.reduce((acc: any, curr: any) => {
                if (curr.key && curr.value) acc[curr.key] = curr.value;
                return acc;
            }, {})
        };

        this.apiService.updateApiProvider(this.providerToEdit.id, payload).subscribe({
            next: (updatedProvider) => {
                this.isEditingProvider = false;
                this.ngOnInit(); // Refresh provider list
                this.closeEditProviderModal();
            },
            error: (err) => {
                this.isEditingProvider = false;
                this.editProviderError = err.error?.message || 'Error al actualizar el proveedor.';
                console.error('Error updating provider', err);
            }
        });
    }

    onSubmit() {
        if (this.toolForm.invalid) {
            this.toolForm.markAllAsTouched();
            return;
        }

        const formValue = this.toolForm.value;

        if (formValue.isCreatingProvider) {
            if (!formValue.providerName || !formValue.baseUrl) {
                this.errorMessage = 'Debe indicar el nombre y la URL base del nuevo proveedor.';
                return;
            }
        } else {
            if (!formValue.providerId) {
                this.errorMessage = 'Debe seleccionar un proveedor o crear uno nuevo.';
                return;
            }
        }

        this.isSubmitting = true;
        this.successMessage = '';
        this.errorMessage = '';

        if (formValue.isCreatingProvider) {
            const providerPayload = {
                name: formValue.providerName,
                code: formValue.providerCode,
                baseUrl: formValue.baseUrl,
                authenticationType: formValue.authenticationType,
                apiKeyLocation: formValue.apiKeyLocation,
                apiKeyName: formValue.apiKeyName,
                apiKeyValue: formValue.apiKeyValue,
                isExportable: formValue.providerIsExportable,
                customHeaders: formValue.customHeaders.reduce((acc: any, curr: any) => {
                    if (curr.key && curr.value) acc[curr.key] = curr.value;
                    return acc;
                }, {})
            };

            this.apiService.createApiProvider(providerPayload).subscribe({
                next: (providerResponse) => {
                    this.saveTool(providerResponse.id, formValue);
                },
                error: (err) => {
                    this.isSubmitting = false;
                    this.errorMessage = 'Hubo un error al crear el proveedor.';
                    console.error('Provider creation error', err);
                }
            });
        } else {
            this.saveTool(formValue.providerId, formValue);
        }
    }

    private saveTool(providerId: number, formValue: any) {
        const toolPayload = {
            ...formValue,
            providerId: providerId
        };

        this.apiService.createApiTool(toolPayload).subscribe({
            next: (response) => {
                this.isSubmitting = false;
                this.successMessage = 'Herramienta guardada con éxito.';
                this.toolForm.reset({ enabled: true, isExportable: false, providerIsExportable: false, httpMethod: 'GET', isCreatingProvider: false, authenticationType: 'NONE', apiKeyLocation: 'HEADER' });
                this.parameters.clear();
                this.customHeaders.clear();
                this.ngOnInit(); // refresh providers
                setTimeout(() => this.router.navigate(['/home']), 1500);
            },
            error: (error) => {
                this.isSubmitting = false;
                if (error.error && error.error.message) {
                    this.errorMessage = error.error.message;
                } else {
                    this.errorMessage = 'Hubo un error al guardar la herramienta.';
                }
                console.error('Error creating tool', error);
            }
        });
    }
}

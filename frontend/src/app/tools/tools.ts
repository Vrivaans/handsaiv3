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

    constructor(private fb: FormBuilder, private apiService: ApiService, private router: Router) {
        this.toolForm = this.fb.group({
            name: ['', Validators.required],
            code: [''],
            enabled: [true],
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

            endpointPath: ['', Validators.required],
            httpMethod: ['GET', Validators.required],
            parameters: this.fb.array([])
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
                apiKeyValue: formValue.apiKeyValue
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
                this.toolForm.reset({ enabled: true, httpMethod: 'GET', isCreatingProvider: false, authenticationType: 'NONE', apiKeyLocation: 'HEADER' });
                this.parameters.clear();
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

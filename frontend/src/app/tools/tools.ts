import { Component } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormBuilder, FormGroup, ReactiveFormsModule, Validators, FormArray } from '@angular/forms';
import { ApiService } from '../api.service';
import { Router, RouterModule } from '@angular/router';

@Component({
    selector: 'app-tools',
    standalone: true,
    imports: [CommonModule, ReactiveFormsModule, RouterModule],
    templateUrl: './tools.html',
    styleUrl: './tools.scss'
})
export class ToolsComponent {
    toolForm: FormGroup;
    isSubmitting = false;
    successMessage = '';
    errorMessage = '';

    constructor(private fb: FormBuilder, private apiService: ApiService, private router: Router) {
        this.toolForm = this.fb.group({
            name: ['', Validators.required],
            code: ['', Validators.required],
            enabled: [true],
            description: ['', Validators.required],
            baseUrl: ['', Validators.required],
            endpointPath: ['', Validators.required],
            httpMethod: ['GET', Validators.required],
            authenticationType: ['NONE', Validators.required],
            apiKeyLocation: ['HEADER'],
            apiKeyName: [''],
            apiKeyValue: [''],
            parameters: this.fb.array([])
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

        this.isSubmitting = true;
        this.successMessage = '';
        this.errorMessage = '';

        const formValue = this.toolForm.value;

        this.apiService.createApiTool(formValue).subscribe({
            next: (response) => {
                this.isSubmitting = false;
                this.successMessage = 'Herramienta guardada con éxito.';
                this.toolForm.reset({ enabled: true, httpMethod: 'GET', authenticationType: 'NONE', apiKeyLocation: 'HEADER' });
                this.parameters.clear();
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

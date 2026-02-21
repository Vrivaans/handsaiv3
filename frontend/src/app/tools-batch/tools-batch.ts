import { Component } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormBuilder, FormGroup, ReactiveFormsModule, Validators, FormArray, FormsModule } from '@angular/forms';
import { ApiService } from '../api.service';
import { Router, RouterModule } from '@angular/router';

@Component({
  selector: 'app-tools-batch',
  standalone: true,
  imports: [CommonModule, ReactiveFormsModule, RouterModule, FormsModule],
  templateUrl: './tools-batch.html',
  styleUrl: './tools-batch.scss'
})
export class ToolsBatchComponent {
  batchForm: FormGroup;
  isSubmitting = false;
  successMessage = '';
  errorMessage = '';

  openApiJsonText = '';
  importError = '';
  importSuccessMessage = '';

  constructor(private fb: FormBuilder, private apiService: ApiService, private router: Router) {
    this.batchForm = this.fb.group({
      baseUrl: ['', Validators.required],
      authenticationType: ['NONE', Validators.required],
      apiKeyLocation: ['HEADER'],
      apiKeyName: [''],
      apiKeyValue: [''],
      // Array of individual tool endpoints
      endpoints: this.fb.array([])
    });
  }

  get endpoints() {
    return this.batchForm.get('endpoints') as FormArray;
  }

  getParameters(endpointIndex: number) {
    return this.endpoints.at(endpointIndex).get('parameters') as FormArray;
  }

  addEndpoint(data?: any) {
    const endpointForm = this.fb.group({
      name: [data?.name || '', Validators.required],
      code: [data?.code || '', Validators.required],
      description: [data?.description || '', Validators.required],
      endpointPath: [data?.endpointPath || '', Validators.required],
      httpMethod: [data?.httpMethod || 'GET', Validators.required],
      parameters: this.fb.array([])
    });

    if (data && data.parameters) {
      const paramArray = endpointForm.get('parameters') as FormArray;
      data.parameters.forEach((p: any) => {
        paramArray.push(this.fb.group({
          name: [p.name, Validators.required],
          type: [p.type, Validators.required],
          description: [p.description || '', Validators.required],
          required: [p.required !== undefined ? p.required : true],
          defaultValue: [p.defaultValue || '']
        }));
      });
    }

    this.endpoints.push(endpointForm);
  }

  removeEndpoint(index: number) {
    this.endpoints.removeAt(index);
  }

  addParameter(endpointIndex: number) {
    const parameterForm = this.fb.group({
      name: ['', Validators.required],
      type: ['STRING', Validators.required],
      description: ['', Validators.required],
      required: [true],
      defaultValue: ['']
    });
    this.getParameters(endpointIndex).push(parameterForm);
  }

  removeParameter(endpointIndex: number, paramIndex: number) {
    this.getParameters(endpointIndex).removeAt(paramIndex);
  }

  importOpenApi() {
    this.importError = '';
    this.importSuccessMessage = '';

    if (!this.openApiJsonText.trim()) {
      this.importError = 'Pega un JSON válido primero.';
      return;
    }

    try {
      const spec = JSON.parse(this.openApiJsonText);

      // Try extracting base URL if present
      if (spec.servers && spec.servers.length > 0) {
        this.batchForm.patchValue({ baseUrl: spec.servers[0].url });
      }

      if (!spec.paths) {
        this.importError = 'No se encontraron "paths" en el JSON.';
        return;
      }

      let importCount = 0;

      // Clear existing if any
      while (this.endpoints.length !== 0) {
        this.endpoints.removeAt(0);
      }

      Object.keys(spec.paths).forEach(path => {
        const pathItem = spec.paths[path];

        ['get', 'post', 'put', 'delete'].forEach(method => {
          if (pathItem[method]) {
            const operation = pathItem[method];

            // Map OpenAPI params to our model
            const parsedParams: any[] = [];
            if (operation.parameters) {
              operation.parameters.forEach((param: any) => {
                let localType = 'STRING';
                if (param.schema && param.schema.type) {
                  if (param.schema.type === 'integer' || param.schema.type === 'number') localType = 'NUMBER';
                  if (param.schema.type === 'boolean') localType = 'BOOLEAN';
                }
                parsedParams.push({
                  name: param.name,
                  type: localType,
                  description: param.description || `Parameter ${param.name}`,
                  required: !!param.required
                });
              });
            }

            // Also look for requestBody for POST/PUT if needed (simplified representation as a param)
            if (operation.requestBody) {
              parsedParams.push({
                name: 'body',
                type: 'STRING',
                description: 'Request Body (JSON string)',
                required: true
              });
            }
            // Generate code safely
            const generatedCode = (operation.operationId)
              ? operation.operationId.replace(/[^a-zA-Z0-9-]/g, '-').toLowerCase()
              : `${method}-${path.replace(/[^a-zA-Z0-9]/g, '-')}`.toLowerCase();

            this.addEndpoint({
              name: operation.summary || operation.operationId || `${method.toUpperCase()} ${path}`,
              code: generatedCode,
              description: operation.description || operation.summary || `Llama al endpoint ${path}`,
              endpointPath: path,
              httpMethod: method.toUpperCase(),
              parameters: parsedParams
            });
            importCount++;
          }
        });
      });

      this.importSuccessMessage = `¡Éxito! Se detectaron y agregaron ${importCount} endpoints.`;
      this.openApiJsonText = ''; // Clear text area

    } catch (e: any) {
      this.importError = 'JSON inválido o estructura no reconocida: ' + e.message;
    }
  }

  onSubmit() {
    if (this.batchForm.invalid || this.endpoints.length === 0) {
      this.batchForm.markAllAsTouched();
      return;
    }

    this.isSubmitting = true;
    this.successMessage = '';
    this.errorMessage = '';

    const formValue = this.batchForm.value;
    const apiToolsToCreate: any[] = [];

    // Distribute the base config to each endpoint
    formValue.endpoints.forEach((ep: any) => {
      apiToolsToCreate.push({
        ...ep,
        baseUrl: formValue.baseUrl,
        authenticationType: formValue.authenticationType,
        apiKeyLocation: formValue.apiKeyLocation,
        apiKeyName: formValue.apiKeyName,
        apiKeyValue: formValue.apiKeyValue,
        enabled: true
      });
    });

    this.apiService.createApiToolsBatch(apiToolsToCreate).subscribe({
      next: (response) => {
        this.isSubmitting = false;
        this.successMessage = `¡Lote guardado! Se crearon ${apiToolsToCreate.length} herramientas con éxito.`;

        // Clear the form
        this.batchForm.reset({ authenticationType: 'NONE', apiKeyLocation: 'HEADER' });
        while (this.endpoints.length !== 0) {
          this.endpoints.removeAt(0);
        }
        setTimeout(() => this.router.navigate(['/home']), 1500);
      },
      error: (error) => {
        this.isSubmitting = false;
        if (error.error && error.error.message) {
          this.errorMessage = error.error.message;
        } else {
          this.errorMessage = 'Hubo un error al guardar el lote de herramientas.';
        }
        console.error('Error batch creating tools', error);
      }
    });
  }
}

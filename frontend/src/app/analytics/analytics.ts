import { Component, OnInit } from '@angular/core';
import { CommonModule, DatePipe } from '@angular/common';
import { ApiService, AnalyticsSummary, ToolExecutionLog } from '../api.service';

@Component({
  selector: 'app-analytics',
  standalone: true,
  imports: [CommonModule, DatePipe],
  templateUrl: './analytics.html',
  styleUrl: './analytics.scss'
})
export class AnalyticsComponent implements OnInit {
  summary: AnalyticsSummary | null = null;
  logs: ToolExecutionLog[] = [];

  currentPage: number = 0;
  pageSize: number = 20;
  totalPages: number = 0;
  totalElements: number = 0;
  isLoading: boolean = true;
  error: string | null = null;

  selectedLog: ToolExecutionLog | null = null;
  showModal: boolean = false;

  constructor(private apiService: ApiService) { }

  openLogDetails(log: ToolExecutionLog): void {
    this.selectedLog = log;
    this.showModal = true;
  }

  closeModal(): void {
    this.showModal = false;
    this.selectedLog = null;
  }

  ngOnInit(): void {
    this.loadData();
  }

  loadData(): void {
    this.isLoading = true;
    this.error = null;

    // Load Summary
    this.apiService.getAnalyticsSummary(30).subscribe({
      next: (summary) => {
        this.summary = summary;
      },
      error: (err) => {
        console.error('Error loading summary', err);
        this.error = 'No se pudieron cargar las analíticas principales.';
      }
    });

    // Load Logs Page
    this.loadLogsPage(0);
  }

  loadLogsPage(pageIndex: number): void {
    if (pageIndex < 0 || (this.totalPages > 0 && pageIndex >= this.totalPages)) {
      return;
    }

    this.apiService.getExecutionLogs(pageIndex, this.pageSize).subscribe({
      next: (pageData) => {
        this.logs = pageData.content;
        this.currentPage = pageData.number;
        this.totalPages = pageData.totalPages;
        this.totalElements = pageData.totalElements;
        this.isLoading = false;
      },
      error: (err) => {
        console.error('Error loading logs', err);
        this.error = 'No se pudo cargar el historial de ejecuciones.';
        this.isLoading = false;
      }
    });
  }

  nextPage(): void {
    this.loadLogsPage(this.currentPage + 1);
  }

  prevPage(): void {
    this.loadLogsPage(this.currentPage - 1);
  }

  formatPayload(payload: string): string {
    if (!payload || payload.trim() === '') return '-';
    try {
      // Just try to pretty print if it's JSON
      const parsed = JSON.parse(payload);
      return JSON.stringify(parsed, null, 2);
    } catch {
      return payload;
    }
  }
}

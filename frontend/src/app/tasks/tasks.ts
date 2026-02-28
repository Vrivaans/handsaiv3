import { Component, OnInit, inject } from '@angular/core';
import { CommonModule } from '@angular/common';
import { ApiService, TaskMemory } from '../api.service';

@Component({
  selector: 'app-tasks',
  standalone: true,
  imports: [CommonModule],
  templateUrl: './tasks.html',
  styleUrls: ['./tasks.scss']
})
export class Tasks implements OnInit {
  private apiService = inject(ApiService);

  tasks: TaskMemory[] = [];
  completedTasks: TaskMemory[] = [];
  loading = true;
  error: string | null = null;
  showCompleted = false;

  ngOnInit(): void {
    this.loadTasks();
  }

  loadTasks(): void {
    this.loading = true;
    this.error = null;

    this.apiService.getPendingTasks().subscribe({
      next: (data) => {
        this.tasks = data;
        this.loading = false;
      },
      error: (err) => {
        this.error = 'Failed to load tasks. Check your connection to the HandsAI server.';
        this.loading = false;
        console.error(err);
      }
    });

    this.apiService.getCompletedTasks().subscribe({
      next: (data) => {
        this.completedTasks = data;
      },
      error: (err) => {
        console.error('Failed to load completed tasks', err);
      }
    });
  }

  toggleCompleted(): void {
    this.showCompleted = !this.showCompleted;
  }

  deleteTask(id: number): void {
    if (confirm('Are you sure you want to delete this task?')) {
      this.apiService.deleteTask(id).subscribe({
        next: () => {
          this.tasks = this.tasks.filter(t => t.id !== id);
          this.completedTasks = this.completedTasks.filter(t => t.id !== id);
        },
        error: (err) => {
          console.error('Failed to delete task', err);
          alert('Failed to delete task');
        }
      });
    }
  }

  getPriorityColor(priority: string): string {
    switch (priority?.toUpperCase()) {
      case 'CRITICAL': return 'bg-red-500 text-white';
      case 'HIGH': return 'bg-orange-500 text-white';
      case 'MEDIUM': return 'bg-yellow-500 text-black';
      case 'LOW': return 'bg-blue-500 text-white';
      default: return 'bg-gray-500 text-white';
    }
  }

  getStatusColor(status: string): string {
    switch (status?.toUpperCase()) {
      case 'IN_PROGRESS': return 'bg-blue-100 text-blue-800 border-blue-200';
      case 'PENDING': return 'bg-gray-100 text-gray-800 border-gray-200';
      case 'COMPLETED': return 'bg-green-100 text-green-800 border-green-200';
      default: return 'bg-gray-100 text-gray-800 border-gray-200';
    }
  }
}

import { Component } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { Router } from '@angular/router';
import { AuthService } from '../../auth.service';

@Component({
  selector: 'app-login',
  standalone: true,
  imports: [CommonModule, FormsModule],
  templateUrl: './login.html',
  styleUrls: ['./login.scss']
})
export class LoginComponent {
  username = '';
  password = '';

  error = '';
  loading = false;

  locked = false;
  retryAfter = '';
  attemptsRemaining = -1;

  constructor(private authService: AuthService, private router: Router) { }

  onSubmit() {
    this.error = '';

    if (!this.username.trim() || !this.password) {
      this.error = 'Username and password are required';
      return;
    }

    this.loading = true;
    this.authService.login({ username: this.username, password: this.password }).subscribe({
      next: () => {
        this.loading = false;
        // Redirect to token management on successful login
        this.router.navigate(['/token']);
      },
      error: (err) => {
        this.loading = false;
        if (err.status === 429) {
          this.locked = true;
          this.error = 'Account is locked due to too many failed attempts.';
          if (err.error?.retryAfter) {
            const date = new Date(err.error.retryAfter);
            this.retryAfter = date.toLocaleTimeString();
          }
        } else if (err.status === 401) {
          this.error = 'Invalid credentials.';
          if (err.error?.attemptsRemaining !== undefined) {
            this.attemptsRemaining = err.error.attemptsRemaining;
          }
        } else {
          this.error = 'Login failed.';
        }
      }
    });
  }
}
